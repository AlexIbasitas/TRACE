package com.trace.test.listeners;

import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsListener;
import com.intellij.execution.testframework.sm.runner.SMTestProxy;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.diagnostic.Logger;
import com.trace.chat.ui.TriagePanelToolWindowFactory;
import com.trace.test.extractors.GherkinScenarioExtractor;
import com.trace.test.extractors.StackTraceExtractor;
import com.trace.test.extractors.StepDefinitionExtractor;
import com.trace.test.models.FailureInfo;
import com.trace.test.models.StepDefinitionInfo;
import com.trace.test.models.GherkinScenarioInfo;
import com.trace.ai.prompts.LocalPromptGenerationService;
import com.trace.chat.ui.TriagePanelView;
import com.trace.ai.configuration.AISettings;
import com.trace.ai.services.AINetworkService;
import com.trace.ai.models.AIAnalysisResult;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Listener for Cucumber test execution events that captures test failures
 * and generates AI prompts for analysis.
 * 
 * <p>This listener implements the IntelliJ test runner events interface to
 * intercept test execution and extract failure information for Cucumber tests.
 * It coordinates with various extractors to gather comprehensive failure context
 * and updates the TriagePanel UI with the results.</p>
 * 
 * <p>The listener supports both unit testing (with null project) and production
 * usage (with project context) through different constructors.</p>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
public class CucumberTestExecutionListener implements SMTRunnerEventsListener {
    
    private static final Logger LOG = Logger.getInstance(CucumberTestExecutionListener.class);
    
    private final Project project;
    private final StackTraceExtractor stackTraceExtractor;
    private final StepDefinitionExtractor stepDefinitionExtractor;
    private final GherkinScenarioExtractor gherkinScenarioExtractor;
    private final LocalPromptGenerationService promptGenerationService;
    private final AISettings aiSettings;
    private AINetworkService aiNetworkService;
    
    // Stream capture for test output analysis
    private static final ConcurrentMap<SMTestProxy, ByteArrayOutputStream> testOutputStreams = new ConcurrentHashMap<>();
    private static final ConcurrentMap<SMTestProxy, ByteArrayOutputStream> testErrorStreams = new ConcurrentHashMap<>();
    private static PrintStream originalOut;
    private static PrintStream originalErr;

    /**
     * Default constructor for IntelliJ plugin registration.
     * Creates a listener without project context for basic event handling.
     */
    public CucumberTestExecutionListener() {
        this.project = null;
        this.stackTraceExtractor = null;
        this.stepDefinitionExtractor = null;
        this.gherkinScenarioExtractor = null;
        this.promptGenerationService = new LocalPromptGenerationService();
        this.aiSettings = AISettings.getInstance();
        this.aiNetworkService = null; // Will be initialized when project is available
    }

    /**
     * Constructor with project context for testing and production usage.
     * Initializes extractors for comprehensive failure analysis.
     * 
     * @param project The IntelliJ project context for file system access
     */
    public CucumberTestExecutionListener(Project project) {
        this.project = project;
        this.promptGenerationService = new LocalPromptGenerationService();
        this.aiSettings = AISettings.getInstance();
        
        // Initialize extractors and AI services only if project is available
        if (project != null) {
            this.stackTraceExtractor = new StackTraceExtractor(project);
            this.stepDefinitionExtractor = new StepDefinitionExtractor(project);
            this.gherkinScenarioExtractor = new GherkinScenarioExtractor(project);
            this.aiNetworkService = new AINetworkService(project);
        } else {
            this.stackTraceExtractor = null;
            this.stepDefinitionExtractor = null;
            this.gherkinScenarioExtractor = null;
            this.aiNetworkService = null;
        }
    }

    /**
     * Captures test output during execution.
     * Called by the test framework when tests produce output.
     *
     * @param test The test proxy object
     * @param outputLine The output line from the test
     */
    public void onTestOutput(SMTestProxy test, String outputLine) {
        if (test != null && outputLine != null) {
            LOG.debug("Capturing test output for: " + test.getName());
            TestOutputCaptureListener.captureTestOutput(test, outputLine);
        }
    }

    /**
     * Captures test output with additional context information.
     * 
     * @param test The test proxy object
     * @param outputLine The output line from the test
     * @param context Additional context information
     */
    public void captureTestOutputWithContext(SMTestProxy test, String outputLine, String context) {
        if (test != null && outputLine != null) {
            String contextualOutput = "[" + context + "] " + outputLine;
            TestOutputCaptureListener.captureTestOutput(test, contextualOutput);
            LOG.debug("Captured contextual output for: " + test.getName() + " - " + context);
        }
    }

    /**
     * Sets up output stream capture for a specific test.
     * Redirects System.out and System.err to capture test output.
     * 
     * @param test The test to capture output for
     */
    public static void setupTestOutputCapture(SMTestProxy test) {
        if (test == null) return;
        
        try {
            // Create capture streams for this test
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
            
            testOutputStreams.put(test, outputStream);
            testErrorStreams.put(test, errorStream);
            
            // Store original streams if not already stored
            if (originalOut == null) {
                originalOut = System.out;
                originalErr = System.err;
            }
            
            // Create custom print streams that capture output
            PrintStream capturedOut = new PrintStream(outputStream, true) {
                @Override
                public void write(byte[] buf, int off, int len) {
                    super.write(buf, off, len);
                    originalOut.write(buf, off, len); // Also write to original
                }
            };
            
            PrintStream capturedErr = new PrintStream(errorStream, true) {
                @Override
                public void write(byte[] buf, int off, int len) {
                    super.write(buf, off, len);
                    originalErr.write(buf, off, len); // Also write to original
                }
            };
            
            // Redirect system streams
            System.setOut(capturedOut);
            System.setErr(capturedErr);
            
            LOG.debug("Output capture started for test: " + test.getName());
            
        } catch (Exception e) {
            LOG.warn("Error setting up output capture: " + e.getMessage());
        }
    }

    /**
     * Captures and stores the output streams for a test.
     * Restores original streams and cleans up capture resources.
     * 
     * @param test The test to capture streams for
     */
    public static void captureTestStreams(SMTestProxy test) {
        if (test == null) return;
        
        try {
            ByteArrayOutputStream outputStream = testOutputStreams.get(test);
            ByteArrayOutputStream errorStream = testErrorStreams.get(test);
            
            if (outputStream != null) {
                String capturedOutput = outputStream.toString();
                if (!capturedOutput.trim().isEmpty()) {
                    TestOutputCaptureListener.captureTestOutput(test, "=== CAPTURED STDOUT ===\n" + capturedOutput);
                }
            }
            
            if (errorStream != null) {
                String capturedError = errorStream.toString();
                if (!capturedError.trim().isEmpty()) {
                    TestOutputCaptureListener.captureTestOutput(test, "=== CAPTURED STDERR ===\n" + capturedError);
                }
            }
            
            // Restore original streams
            if (originalOut != null) {
                System.setOut(originalOut);
            }
            if (originalErr != null) {
                System.setErr(originalErr);
            }
            
            // Clean up
            testOutputStreams.remove(test);
            testErrorStreams.remove(test);
            
        } catch (Exception e) {
            LOG.warn("Error capturing test streams: " + e.getMessage());
        }
    }

    // Required interface methods - minimal implementation for unused events
    @Override
    public void onTestingStarted(SMTestProxy.SMRootTestProxy root) {
        // Not needed for our use case
    }

    @Override
    public void onTestingFinished(SMTestProxy.SMRootTestProxy root) {
        // Not needed for our use case
    }

    @Override
    public void onTestsCountInSuite(int count) {
        // Not needed for our use case
    }

    @Override
    public void onTestIgnored(SMTestProxy test) {
        // Not needed for our use case
    }

    @Override
    public void onSuiteStarted(SMTestProxy suite) {
        // Not needed for our use case
    }

    @Override
    public void onSuiteFinished(SMTestProxy suite) {
        // Not needed for our use case
    }

    @Override
    public void onSuiteTreeNodeAdded(SMTestProxy test) {
        // Not needed for our use case
    }

    @Override
    public void onCustomProgressTestStarted() {
        // Not needed for our use case
    }

    @Override
    public void onCustomProgressTestFinished() {
        // Not needed for our use case
    }

    @Override
    public void onCustomProgressTestFailed() {
        // Not needed for our use case
    }

    @Override
    public void onCustomProgressTestsCategory(String categoryName, int count) {
        // Not needed for our use case
    }

    @Override
    public void onSuiteTreeStarted(SMTestProxy suite) {
        // Not needed for our use case
    }

    @Override
    public void onTestStarted(SMTestProxy test) {
        if (test != null) {
            LOG.debug("Test started: " + test.getName());
            
            // Initialize output capture for this test
            TestOutputCaptureListener.captureTestOutput(test, "=== Test Started ===\n");
            setupTestOutputCapture(test);
        }
    }

    @Override
    public void onTestFinished(SMTestProxy test) {
        if (test != null) {
            LOG.debug("Test finished: " + test.getName() + " (Status: " + test.getMagnitudeInfo() + ")");
            
            // Capture any streams before finishing
            captureTestStreams(test);
            
            // Add final output marker
            TestOutputCaptureListener.captureTestOutput(test, "=== Test Finished ===\n");
        }
    }

    @Override
    public void onTestFailed(SMTestProxy test) {
        LOG.debug("Test failed: " + (test != null ? test.getName() : "null"));
        
        // Capture comprehensive test output for failed test
        if (test != null) {
            TestOutputCaptureListener.captureComprehensiveTestOutput(test);
        }
        
        // Check if this is a Cucumber test
        if (isCucumberTest(test)) {
            LOG.info("Processing Cucumber test failure: " + test.getName());
            processFailedCucumberTest(test, project);
        }
    }

    /**
     * Determines if a test is a Cucumber test using multiple detection methods.
     *
     * @param test The test proxy to check
     * @return true if the test is a Cucumber test, false otherwise
     */
    private boolean isCucumberTest(SMTestProxy test) {
        if (test == null) {
            return false;
        }
        
        // Method 1: Check test location (most reliable)
        String testLocation = test.getLocationUrl();
        if (testLocation != null) {
            // Check if test is in a feature file
            if (testLocation.contains(".feature") || testLocation.contains("features/")) {
                LOG.debug("Detected Cucumber test via feature file location");
                return true;
            }
            
            // Check if test is in a Cucumber runner class
            if (testLocation.contains("Cucumber") || testLocation.contains("cucumber")) {
                LOG.debug("Detected Cucumber test via runner class location");
                return true;
            }
        }
        
        // Method 2: Check test hierarchy for Cucumber indicators
        SMTestProxy parent = test.getParent();
        while (parent != null) {
            String parentName = parent.getName();
            if (parentName != null && (
                parentName.contains("Cucumber") ||
                parentName.contains("Feature:") ||
                parentName.contains("Scenario:") ||
                parentName.contains("cucumber.runtime"))) {
                LOG.debug("Detected Cucumber test via parent hierarchy: " + parentName);
                return true;
            }
            parent = parent.getParent();
        }
        
        // Method 3: Check error message for Cucumber-specific exceptions (fallback)
        String errorMessage = test.getErrorMessage();
        if (errorMessage != null && (
            errorMessage.contains("io.cucumber") ||
            errorMessage.contains("cucumber.runtime") ||
            errorMessage.contains("UndefinedStepException") ||
            errorMessage.contains("AmbiguousStepDefinitionsException") ||
            errorMessage.contains("PendingException"))) {
            LOG.debug("Detected Cucumber test via error message");
            return true;
        }
        
        return false;
    }

    /**
     * Processes a failed Cucumber test by extracting relevant information
     * and generating AI prompts for analysis.
     *
     * @param test The failed test proxy
     * @param currentProject The current project context
     */
    private void processFailedCucumberTest(SMTestProxy test, Project currentProject) {
        try {
            // Check if we have a valid project for extraction
            if (currentProject == null) {
                LOG.warn("Project is null, cannot process test failure");
                return;
            }
            
            // Create extractors for this project if not already created
            StackTraceExtractor localStackTraceExtractor = stackTraceExtractor;
            StepDefinitionExtractor localStepDefinitionExtractor = stepDefinitionExtractor;
            GherkinScenarioExtractor localGherkinScenarioExtractor = gherkinScenarioExtractor;
            
            if (localStackTraceExtractor == null) {
                localStackTraceExtractor = new StackTraceExtractor(currentProject);
            }
            
            // Defensive programming: check if extractors are available
            if (localStackTraceExtractor == null) {
                LOG.warn("StackTraceExtractor is null, cannot process");
                return;
            }
            
            // Extract basic failure information using the test proxy directly
            LOG.debug("Extracting failure info from test proxy...");
            FailureInfo basicFailureInfo = localStackTraceExtractor.extractFailureInfo(test);
            
            if (basicFailureInfo == null) {
                LOG.warn("Could not extract basic failure info");
                return;
            }
            
            LOG.debug("Successfully extracted failure info for scenario: " + basicFailureInfo.getScenarioName());
            
            // Enhance with step definition information using stack trace-based extraction
            StepDefinitionInfo stepDefInfo = null;
            String sourceFilePath = basicFailureInfo.getSourceFilePath();
            int lineNumber = basicFailureInfo.getLineNumber();
            String stackTrace = basicFailureInfo.getStackTrace();
            
            if (stackTrace != null && basicFailureInfo.getFailedStepText() != null) {
                LOG.debug("Attempting step definition extraction from stack trace");
                
                // Create step definition extractor if needed
                final StepDefinitionExtractor finalStepDefinitionExtractor;
                if (localStepDefinitionExtractor == null) {
                    finalStepDefinitionExtractor = new StepDefinitionExtractor(currentProject);
                } else {
                    finalStepDefinitionExtractor = localStepDefinitionExtractor;
                }
                
                // Use stack trace-based extraction (most reliable approach)
                stepDefInfo = finalStepDefinitionExtractor.extractStepDefinition(stackTrace);
                
                if (stepDefInfo != null) {
                    LOG.debug("Step definition extraction successful - Method: " + stepDefInfo.getMethodName());
                    // Use step definition info for better source location
                    sourceFilePath = stepDefInfo.getSourceFilePath();
                    lineNumber = stepDefInfo.getLineNumber();
                } else {
                    LOG.debug("Step definition extraction failed - no step definition found");
                }
            }
            
            // Extract Gherkin scenario information
            GherkinScenarioInfo scenarioInfo = null;
            if (basicFailureInfo.getFailedStepText() != null) {
                if (localGherkinScenarioExtractor == null) {
                    localGherkinScenarioExtractor = new GherkinScenarioExtractor(currentProject);
                }
                scenarioInfo = localGherkinScenarioExtractor.extractScenarioInfo(
                    basicFailureInfo.getFailedStepText(), 
                    basicFailureInfo.getScenarioName()
                );
            }
            
            // Create enhanced failure info with rich structured data
            FailureInfo enhancedFailureInfo = new FailureInfo.Builder()
                .withScenarioName(basicFailureInfo.getScenarioName())
                .withFailedStepText(basicFailureInfo.getFailedStepText())
                .withStackTrace(basicFailureInfo.getStackTrace())
                .withSourceFilePath(sourceFilePath)
                .withLineNumber(lineNumber)
                .withStepDefinitionInfo(stepDefInfo)
                .withGherkinScenarioInfo(scenarioInfo)
                .withExpectedValue(basicFailureInfo.getExpectedValue())
                .withActualValue(basicFailureInfo.getActualValue())
                .withErrorMessage(basicFailureInfo.getErrorMessage())
                .withParsingTime(basicFailureInfo.getParsingTime())
                .build();
            
            // Notify the TriagePanel about the new failure
            LOG.debug("Notifying TriagePanel about failure");
            notifyTriagePanel(enhancedFailureInfo, currentProject);
            
            // Trigger AI analysis if configured and enabled
            triggerAIAnalysisIfConfigured(enhancedFailureInfo, currentProject);
            
        } catch (Exception e) {
            LOG.error("Error processing failed test: " + e.getMessage(), e);
        }
    }
    
    /**
     * Notifies the TriagePanel about a new test failure.
     * This method runs on the EDT to ensure thread safety.
     * 
     * @param failureInfo The failure information to display
     * @param currentProject The current project context
     */
    private void notifyTriagePanel(FailureInfo failureInfo, Project currentProject) {
        if (currentProject == null) {
            LOG.warn("Project is null, cannot notify panel");
            return;
        }
        
        LOG.debug("Attempting to notify TriagePanel for project: " + currentProject.getName());
        
        // Run on EDT to ensure thread safety
        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
            try {
                // Get the TriagePanel instance for this project
                TriagePanelView triagePanel = getTriagePanelForProject(currentProject);
                if (triagePanel != null) {
                    LOG.debug("Found TriagePanel, updating with failure info");
                    triagePanel.updateFailure(failureInfo);
                } else {
                    LOG.debug("TriagePanel not found for project: " + currentProject.getName());
                }
            } catch (Exception e) {
                LOG.error("Error notifying panel: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Gets the TriagePanel instance for the given project.
     * 
     * @param project The project to get the panel for
     * @return The TriagePanel instance or null if not found
     */
    private TriagePanelView getTriagePanelForProject(Project project) {
        return TriagePanelToolWindowFactory.getPanelForProject(project);
    }
    
    /**
     * Triggers AI analysis if the system is properly configured and auto-analysis is enabled.
     * This method runs asynchronously to avoid blocking the test execution flow.
     * 
     * @param failureInfo The failure information to analyze
     * @param currentProject The current project context
     */
    private void triggerAIAnalysisIfConfigured(FailureInfo failureInfo, Project currentProject) {
        if (failureInfo == null) {
            LOG.warn("FailureInfo is null, cannot trigger AI analysis");
            return;
        }
        
        if (currentProject == null) {
            LOG.warn("Project is null, cannot trigger AI analysis");
            return;
        }
        
        // Check if AI network service is available
        if (aiNetworkService == null) {
            LOG.warn("AI network service is null, cannot trigger AI analysis");
            return;
        }
        
        // Check if AI is configured and auto-analysis is enabled
        if (!aiSettings.isConfigured()) {
            LOG.debug("AI analysis not triggered: AI is not properly configured");
            return;
        }
        
        if (!aiSettings.isAutoAnalyzeEnabled()) {
            LOG.debug("AI analysis not triggered: Auto-analysis is disabled");
            return;
        }
        
        LOG.info("Triggering AI analysis for failure: " + failureInfo.getScenarioName());
        
        // Get the current analysis mode from the TriagePanel
        TriagePanelView triagePanel = getTriagePanelForProject(currentProject);
        String analysisMode = "Full Analysis"; // Default to full analysis
        if (triagePanel != null) {
            analysisMode = triagePanel.getCurrentAnalysisMode();
            LOG.info("Using analysis mode: " + analysisMode);
        } else {
            LOG.debug("TriagePanel not found, using default analysis mode: " + analysisMode);
        }
        
        // Perform AI analysis asynchronously to avoid blocking test execution
        CompletableFuture<AIAnalysisResult> analysisFuture = aiNetworkService.analyze(failureInfo, analysisMode);
        
        // Handle the analysis result
        analysisFuture.thenAccept(result -> {
            LOG.info("AI analysis callback triggered for: " + failureInfo.getScenarioName());
            if (result != null) {
                LOG.info("AI analysis completed successfully for: " + failureInfo.getScenarioName());
                LOG.info("Result analysis length: " + (result.getAnalysis() != null ? result.getAnalysis().length() : "null"));
                displayAIAnalysisResult(result, currentProject);
            } else {
                LOG.warn("AI analysis returned null result for: " + failureInfo.getScenarioName());
                displayAIAnalysisError("AI analysis returned no result", currentProject);
            }
        }).exceptionally(throwable -> {
            LOG.error("AI analysis failed for: " + failureInfo.getScenarioName(), throwable);
            displayAIAnalysisError("AI analysis failed: " + throwable.getMessage(), currentProject);
            return null;
        });
    }
    
    /**
     * Displays the AI analysis result in the TriagePanel.
     * This method runs on the EDT to ensure thread safety.
     * 
     * @param result The AI analysis result to display
     * @param currentProject The current project context
     */
    private void displayAIAnalysisResult(AIAnalysisResult result, Project currentProject) {
        if (result == null || currentProject == null) {
            return;
        }
        
        // Run on EDT to ensure thread safety
        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
            try {
                TriagePanelView triagePanel = getTriagePanelForProject(currentProject);
                if (triagePanel != null) {
                    triagePanel.displayAIAnalysisResult(result);
                } else {
                    LOG.debug("TriagePanel not found for project: " + currentProject.getName());
                }
            } catch (Exception e) {
                LOG.error("Error displaying AI analysis result: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Displays an AI analysis error in the TriagePanel.
     * This method runs on the EDT to ensure thread safety.
     * 
     * @param errorMessage The error message to display
     * @param currentProject The current project context
     */
    private void displayAIAnalysisError(String errorMessage, Project currentProject) {
        if (errorMessage == null || currentProject == null) {
            return;
        }
        
        // Run on EDT to ensure thread safety
        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
            try {
                TriagePanelView triagePanel = getTriagePanelForProject(currentProject);
                if (triagePanel != null) {
                    triagePanel.displayAIAnalysisError(errorMessage);
                } else {
                    LOG.debug("TriagePanel not found for project: " + currentProject.getName());
                }
            } catch (Exception e) {
                LOG.error("Error displaying AI analysis error: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Gets the current project from the application context.
     * 
     * @return The current project or null if not found
     */
    private Project getCurrentProject() {
        if (project != null) {
            return project;
        }
        
        // Try to get the current project from the application context
        try {
            return com.intellij.openapi.project.ProjectManager.getInstance().getOpenProjects()[0];
        } catch (Exception e) {
            LOG.warn("Could not get current project: " + e.getMessage());
            return null;
        }
    }
} 