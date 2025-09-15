package com.trace.test.listeners;

import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsListener;
import com.intellij.execution.testframework.sm.runner.SMTestProxy;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.application.ApplicationManager;
import com.trace.chat.ui.TriagePanelToolWindowFactory;
import com.trace.test.extractors.GherkinScenarioExtractor;
import com.trace.test.extractors.StackTraceExtractor;
import com.trace.test.extractors.StepDefinitionExtractor;
import com.trace.test.models.FailureInfo;
import com.trace.test.models.StepDefinitionInfo;
import com.trace.test.models.GherkinScenarioInfo;
import com.trace.ai.prompts.InitialPromptFailureAnalysisService;
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
    private final InitialPromptFailureAnalysisService promptGenerationService;
    private final AISettings aiSettings;
    private AINetworkService aiNetworkService;
    
    // Stream capture for test output analysis (instance-based to allow proper cleanup)
    private final ConcurrentMap<SMTestProxy, ByteArrayOutputStream> testOutputStreams = new ConcurrentHashMap<>();
    private final ConcurrentMap<SMTestProxy, ByteArrayOutputStream> testErrorStreams = new ConcurrentHashMap<>();
    private static PrintStream originalOut;
    private static PrintStream originalErr;
    
    // Singleton instance for backward compatibility
    private static volatile CucumberTestExecutionListener instance;

    /**
     * Default constructor for IntelliJ plugin registration.
     * Creates a listener without project context for basic event handling.
     */
    public CucumberTestExecutionListener() {
        this.project = null;
        this.stackTraceExtractor = null;
        this.stepDefinitionExtractor = null;
        this.gherkinScenarioExtractor = null;
        this.promptGenerationService = new InitialPromptFailureAnalysisService();
        this.aiSettings = AISettings.getInstance();
        this.aiNetworkService = null; // Will be initialized when project is available
        instance = this;
        LOG.debug("CucumberTestExecutionListener created without project context");
    }

    /**
     * Constructor with project context for testing and production usage.
     * Initializes extractors for comprehensive failure analysis.
     * 
     * @param project The IntelliJ project context for file system access
     */
    public CucumberTestExecutionListener(Project project) {
        this.project = project;
        this.promptGenerationService = new InitialPromptFailureAnalysisService();
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
            if (LOG.isDebugEnabled()) {
                LOG.debug("Capturing test output for: " + test.getName());
            }
            TestOutputCaptureListener.captureTestOutputStatic(test, outputLine);
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
            TestOutputCaptureListener.captureTestOutputStatic(test, contextualOutput);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Captured contextual output for: " + test.getName() + " - " + context);
            }
        }
    }



    // Required interface methods - minimal implementation for unused events
    @Override
    public void onTestingStarted(SMTestProxy.SMRootTestProxy root) {
        LOG.info("Test run started: " + (root != null ? root.getName() : "null"));
        
        // Notify TriagePanel that a new test run has started
        if (project != null) {
            ApplicationManager.getApplication().invokeLater(() -> {
                try {
                    TriagePanelView triagePanel = getTriagePanelForProject(project);
                    if (triagePanel != null) {
                        triagePanel.onTestRunStarted();
                        LOG.info("TriagePanel notified of test run start");
                    } else {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("TriagePanel not found for project: " + project.getName());
                        }
                    }
                } catch (Exception e) {
                    LOG.error("Error notifying TriagePanel of test run start", e);
                }
            });
        } else {
            LOG.info("Project is null, cannot notify TriagePanel");
        }
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
            if (LOG.isDebugEnabled()) {
                LOG.debug("Test started: " + test.getName());
            }
            
            // Initialize output capture for this test
            TestOutputCaptureListener.captureTestOutputStatic(test, "Test started\n");
            TestUtilityHelper.setupTestOutputCaptureStatic(test);
        }
    }

    @Override
    public void onTestFinished(SMTestProxy test) {
        if (test != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Test finished: " + test.getName() + " (Status: " + test.getMagnitudeInfo() + ")");
            }
            
            // Capture any streams before finishing
            TestUtilityHelper.captureTestStreamsStatic(test);
            
            // Add final output marker
            TestOutputCaptureListener.captureTestOutputStatic(test, "Test finished\n");
        }
    }

    @Override
    public void onTestFailed(SMTestProxy test) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Test failed: " + (test != null ? test.getName() : "null"));
        }
        
        // Capture comprehensive test output for failed test
        if (test != null) {
            TestOutputCaptureListener.captureComprehensiveTestOutputStatic(test);
        }
        
        // Check if this is a Cucumber test
        if (TestUtilityHelper.isCucumberTest(test)) {
            LOG.info("Processing Cucumber test failure: " + test.getName());
            processFailedCucumberTest(test, project);
        }
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
                LOG.info("Project is null, cannot process test failure");
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
                LOG.error("StackTraceExtractor is null, cannot process");
                return;
            }
            
            // Extract basic failure information using the test proxy directly
            LOG.debug("Extracting failure info from test proxy...");
            FailureInfo basicFailureInfo = localStackTraceExtractor.extractFailureInfo(test);
            
            if (basicFailureInfo == null) {
                LOG.warn("Could not extract basic failure info");
                return;
            }
            
            if (LOG.isDebugEnabled()) {
                LOG.debug("Successfully extracted failure info for scenario: " + basicFailureInfo.getScenarioName());
            }
            
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
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Step definition extraction successful - Method: " + stepDefInfo.getMethodName());
                    }
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
            // Use Gherkin scenario name if available, otherwise fall back to basic scenario name
            String finalScenarioName = (scenarioInfo != null && scenarioInfo.getScenarioName() != null) 
                ? TestUtilityHelper.formatScenarioName(scenarioInfo.getScenarioName(), basicFailureInfo.getScenarioName(), scenarioInfo.isScenarioOutline()) 
                : basicFailureInfo.getScenarioName();
            
            FailureInfo enhancedFailureInfo = new FailureInfo.Builder()
                .withScenarioName(finalScenarioName)
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
        boolean failureWasProcessed = notifyTriagePanel(enhancedFailureInfo, currentProject);
        
        // Only trigger AI analysis if the failure was actually processed
        if (failureWasProcessed) {
            LOG.info("Failure was processed by TriagePanel - triggering AI analysis");
            triggerAIAnalysisIfConfigured(enhancedFailureInfo, currentProject);
        } else {
            LOG.info("Failure was ignored by TriagePanel (subsequent failure in same test run) - skipping AI analysis");
        }
            
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
     * @return true if the failure was processed, false if it was ignored
     */
    private boolean notifyTriagePanel(FailureInfo failureInfo, Project currentProject) {
        if (currentProject == null) {
            LOG.info("Project is null, cannot notify panel");
            return false;
        }
        
        if (LOG.isDebugEnabled()) {
            LOG.debug("Attempting to notify TriagePanel for project: " + currentProject.getName());
        }
        
        // Get the TriagePanel instance for this project
        TriagePanelView triagePanel = getTriagePanelForProject(currentProject);
        if (triagePanel == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("TriagePanel not found for project: " + currentProject.getName());
            }
            return false;
        }
        
        // Run on EDT to ensure thread safety and get the result
        final boolean[] wasProcessed = {false};
        try {
            ApplicationManager.getApplication().invokeAndWait(() -> {
                try {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Found TriagePanel, updating with failure info");
                    }
                    wasProcessed[0] = triagePanel.updateFailure(failureInfo);
                } catch (Exception e) {
                    LOG.error("Error notifying panel: " + e.getMessage(), e);
                    wasProcessed[0] = false;
                }
            });
        } catch (Exception e) {
            LOG.error("Error during EDT execution in notifyTriagePanel", e);
            wasProcessed[0] = false;
        }
        
        return wasProcessed[0];
    }
    
    /**
     * Gets the TriagePanel instance for the given project.
     * 
     * @param project The project to get the panel for
     * @return The TriagePanel instance or null if not found
     */
    private TriagePanelView getTriagePanelForProject(Project project) {
        return TriagePanelToolWindowFactory.getPanelForProjectStatic(project);
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
            LOG.info("Project is null, cannot trigger AI analysis");
            return;
        }
        
        // Master kill switch: TRACE OFF → do not trigger anything
        if (!aiSettings.isTraceEnabled()) {
            LOG.debug("TRACE is OFF - not triggering AI analysis");
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
        
        // NOTE: AI analysis is now handled directly by TriagePanelView to avoid duplication
        // The TriagePanelView will handle the AI analysis when updateFailure() is called
        LOG.info("AI analysis delegated to TriagePanel");
        LOG.info("Failure: " + failureInfo.getScenarioName());
        LOG.info("Failed Step: " + failureInfo.getFailedStepText());
        LOG.info("Error Message: " + failureInfo.getErrorMessage());
        
        // No longer trigger AI analysis here - it's handled by TriagePanelView
        // This prevents duplicate AI responses and token waste
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
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                TriagePanelView triagePanel = getTriagePanelForProject(currentProject);
                if (triagePanel != null) {
                    triagePanel.displayAIAnalysisResult(result);
                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("TriagePanel not found for project: " + currentProject.getName());
                    }
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
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                TriagePanelView triagePanel = getTriagePanelForProject(currentProject);
                if (triagePanel != null) {
                    triagePanel.displayAIAnalysisError(errorMessage);
                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("TriagePanel not found for project: " + currentProject.getName());
                    }
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
            LOG.error("Could not get current project: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Cleans up instance resources to prevent memory leaks and ensure consistent startup behavior.
     * 
     * <p>This method should be called during plugin shutdown or when resources need to be reset.
     * It clears all instance maps and closes any open streams to prevent memory leaks.</p>
     */
    public void cleanup() {
        LOG.info("Starting cleanup of CucumberTestExecutionListener instance resources");
        
        int streamsCleaned = 0;
        int errorStreamsCleaned = 0;
        
        try {
            // Close and clear test output streams
            for (ByteArrayOutputStream stream : testOutputStreams.values()) {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (Exception e) {
                        LOG.warn("Error closing test output stream: " + e.getMessage());
                    }
                }
                streamsCleaned++;
            }
            testOutputStreams.clear();
            
            // Close and clear test error streams
            for (ByteArrayOutputStream stream : testErrorStreams.values()) {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (Exception e) {
                        LOG.warn("Error closing test error stream: " + e.getMessage());
                    }
                }
                errorStreamsCleaned++;
            }
            testErrorStreams.clear();
            
            // Clear static PrintStream references to prevent memory leaks
            originalOut = null;
            originalErr = null;
            
            LOG.info("CucumberTestExecutionListener cleanup completed - cleared " + streamsCleaned + 
                    " output streams and " + errorStreamsCleaned + " error streams");
                    
        } catch (Exception e) {
            LOG.error("Error during CucumberTestExecutionListener cleanup: " + e.getMessage(), e);
        }
    }
    
    /**
     * Static wrapper for cleanup.
     */
    public static void cleanupStatic() {
        if (instance != null) {
            instance.cleanup();
            instance = null;
        }
    }
} 