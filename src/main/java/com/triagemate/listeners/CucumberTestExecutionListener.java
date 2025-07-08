package com.triagemate.listeners;

import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsAdapter;
import com.intellij.execution.testframework.sm.runner.SMTestProxy;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.application.ApplicationManager;
import com.triagemate.models.FailureInfo;
import com.triagemate.models.StepDefinitionInfo;
import com.triagemate.models.GherkinScenarioInfo;
import com.triagemate.extractors.StackTraceExtractor;
import com.triagemate.extractors.StepDefinitionExtractor;
import com.triagemate.extractors.GherkinScenarioExtractor;
import com.triagemate.services.BackendCommunicationService;
import com.triagemate.services.PromptGenerationService;
import com.triagemate.ui.TriagePanelView;
import com.triagemate.ui.TriagePanelToolWindowFactory;

/**
 * Listener for Cucumber test execution events.
 * Detects test failures and triggers the extraction and processing pipeline.
 */
public class CucumberTestExecutionListener extends SMTRunnerEventsAdapter {
    private final Project project;
    private final StackTraceExtractor stackTraceExtractor;
    private final StepDefinitionExtractor stepDefinitionExtractor;
    private final GherkinScenarioExtractor gherkinScenarioExtractor;
    private final PromptGenerationService promptGenerationService;

    /**
     * Default constructor for IntelliJ plugin registration.
     * IntelliJ creates listeners using the default constructor.
     */
    public CucumberTestExecutionListener() {
        this.project = null;
        this.stackTraceExtractor = null;
        this.stepDefinitionExtractor = null;
        this.gherkinScenarioExtractor = null;
        this.promptGenerationService = null;
    }

    /**
     * Constructor for CucumberTestExecutionListener with project context.
     * Used for testing and manual instantiation.
     *
     * @param project The current IntelliJ project (can be null for testing)
     */
    public CucumberTestExecutionListener(Project project) {
        this.project = project;
        
        // Initialize extractors with defensive programming
        this.stackTraceExtractor = project != null ? new StackTraceExtractor(project) : null;
        this.stepDefinitionExtractor = project != null ? new StepDefinitionExtractor(project) : null;
        this.gherkinScenarioExtractor = project != null ? new GherkinScenarioExtractor(project) : null;
        
        // Get prompt generation service
        this.promptGenerationService = ServiceManager.getService(PromptGenerationService.class);
    }

    /**
     * Called when a test fails
     *
     * @param test The test proxy object representing the failed test
     */
    @Override
    public void onTestFailed(SMTestProxy test) {
        System.out.println("TriageMate: onTestFailed called for test: " + (test != null ? test.getName() : "null"));
        
        // Get the current project if not set
        Project currentProject = getCurrentProject();
        if (currentProject == null) {
            System.out.println("TriageMate: No current project found, skipping");
            return;
        }
        
        if (!isCucumberTest(test)) {
            System.out.println("TriageMate: Test is not a Cucumber test, skipping");
            return;
        }
        
        System.out.println("TriageMate: Processing Cucumber test failure");
        processFailedCucumberTest(test, currentProject);
    }

    /**
     * Determines if a test is a Cucumber test
     *
     * @param test The test proxy to check
     * @return true if the test is a Cucumber test, false otherwise
     */
    private boolean isCucumberTest(SMTestProxy test) {
        if (test == null) {
            return false;
        }
        
        // Check test name patterns (defensive programming - handle null values)
        String testName = test.getName();
        boolean hasCucumberName = testName != null && 
            (testName.contains("Scenario:") || testName.contains("Feature:"));
        
        // Check error message patterns (defensive programming - handle null values)
        String errorMessage = test.getErrorMessage();
        boolean hasCucumberError = errorMessage != null && errorMessage.contains("io.cucumber");
        
        return hasCucumberName || hasCucumberError;
    }

    /**
     * Process a failed Cucumber test by extracting relevant information
     * and generating AI prompts for analysis
     *
     * @param test The failed test proxy
     * @param currentProject The current project context
     */
    private void processFailedCucumberTest(SMTestProxy test, Project currentProject) {
        try {
            System.out.println("TriageMate: Starting to process failed Cucumber test");
            
            // Create extractors for this project if not already created
            StackTraceExtractor localStackTraceExtractor = stackTraceExtractor;
            StepDefinitionExtractor localStepDefinitionExtractor = stepDefinitionExtractor;
            GherkinScenarioExtractor localGherkinScenarioExtractor = gherkinScenarioExtractor;
            
            if (localStackTraceExtractor == null) {
                localStackTraceExtractor = new StackTraceExtractor(currentProject);
            }
            
            // Defensive programming: check if extractors are available
            if (localStackTraceExtractor == null) {
                System.out.println("TriageMate: StackTraceExtractor is null, cannot process");
                return;
            }
            
            // Extract basic failure information
            String testOutput = test.getErrorMessage();
            if (testOutput == null || testOutput.trim().isEmpty()) {
                System.out.println("TriageMate: No error message to process");
                return; // No error message to process
            }
            
            System.out.println("TriageMate: Extracting failure info from: " + testOutput.substring(0, Math.min(100, testOutput.length())) + "...");
            FailureInfo basicFailureInfo = localStackTraceExtractor.extractFailureInfo(testOutput);
            
            if (basicFailureInfo == null) {
                System.out.println("TriageMate: Could not extract basic failure info");
                return; // Could not extract basic failure info
            }
            
            System.out.println("TriageMate: Successfully extracted failure info for scenario: " + basicFailureInfo.getScenarioName());

            // Enhance with step definition information
            StepDefinitionInfo stepDefInfo = null;
            String sourceFilePath = basicFailureInfo.getSourceFilePath();
            int lineNumber = basicFailureInfo.getLineNumber();
            
            if (basicFailureInfo.getFailedStepText() != null) {
                if (localStepDefinitionExtractor == null) {
                    localStepDefinitionExtractor = new StepDefinitionExtractor(currentProject);
                }
                stepDefInfo = localStepDefinitionExtractor.extractStepDefinition(basicFailureInfo.getFailedStepText());
                if (stepDefInfo != null) {
                    // Use step definition info for better source location
                    sourceFilePath = stepDefInfo.getSourceFilePath();
                    lineNumber = stepDefInfo.getLineNumber();
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
                .withAssertionType(basicFailureInfo.getAssertionType())
                .withErrorMessage(basicFailureInfo.getErrorMessage())
                .withParsingStrategy(basicFailureInfo.getParsingStrategy())
                .withParsingTime(basicFailureInfo.getParsingTime())
                .build();
            
            // Notify the TriagePanel about the new failure
            System.out.println("TriageMate: Notifying TriagePanel about failure");
            notifyTriagePanel(enhancedFailureInfo, currentProject);
            
        } catch (Exception e) {
            // Log the error - will implement proper logging
            System.err.println("TriageMate: Error processing failed test: " + e.getMessage());
            e.printStackTrace();
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
            System.out.println("TriageMate: Project is null, cannot notify panel");
            return;
        }
        
        System.out.println("TriageMate: Attempting to notify TriagePanel for project: " + currentProject.getName());
        
        // Run on EDT to ensure thread safety
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                // Get the TriagePanel instance for this project
                TriagePanelView triagePanel = getTriagePanelForProject(currentProject);
                if (triagePanel != null) {
                    System.out.println("TriageMate: Found TriagePanel, updating with failure info");
                    triagePanel.updateFailure(failureInfo);
                } else {
                    System.out.println("TriageMate: TriagePanel not found for project: " + currentProject.getName());
                    System.out.println("TriageMate: Make sure the TriagePanel tool window is open");
                }
            } catch (Exception e) {
                // Log error but don't crash the listener
                System.err.println("TriageMate: Error notifying panel: " + e.getMessage());
                e.printStackTrace();
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
     * Displays the generated prompt in the UI.
     * 
     * @param prompt The generated prompt to display
     */
    private void displayPrompt(String prompt) {
        // TODO: Implement UI display in Phase 4
        // This will update the TriagePanel with the generated prompt
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
            System.out.println("TriageMate: Could not get current project: " + e.getMessage());
            return null;
        }
    }
} 