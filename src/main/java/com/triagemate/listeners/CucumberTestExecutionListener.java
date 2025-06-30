package com.triagemate.listeners;

import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsAdapter;
import com.intellij.execution.testframework.sm.runner.SMTestProxy;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.triagemate.models.FailureInfo;
import com.triagemate.extractors.StackTraceExtractor;
import com.triagemate.extractors.StepDefinitionExtractor;
import com.triagemate.extractors.GherkinScenarioExtractor;
import com.triagemate.services.PromptFormatterService;

/**
 * Listener for Cucumber test execution events.
 * Detects test failures and triggers the extraction and processing pipeline.
 */
public class CucumberTestExecutionListener extends SMTRunnerEventsAdapter {
    private final Project project;
    private final StackTraceExtractor stackTraceExtractor;
    private final StepDefinitionExtractor stepDefinitionExtractor;
    private final GherkinScenarioExtractor gherkinScenarioExtractor;

    /**
     * Constructor for CucumberTestExecutionListener
     *
     * @param project The current IntelliJ project
     */
    public CucumberTestExecutionListener(Project project) {
        this.project = project;
        this.stackTraceExtractor = new StackTraceExtractor(project);
        this.stepDefinitionExtractor = new StepDefinitionExtractor(project);
        this.gherkinScenarioExtractor = new GherkinScenarioExtractor(project);
    }

    /**
     * Called when a test fails
     *
     * @param test The test proxy object representing the failed test
     */
    @Override
    public void onTestFailed(SMTestProxy test) {
        if (isCucumberTest(test)) {
            processFailedCucumberTest(test);
        }
    }

    /**
     * Determines if a test is a Cucumber test
     *
     * @param test The test proxy to check
     * @return true if the test is a Cucumber test, false otherwise
     */
    private boolean isCucumberTest(SMTestProxy test) {
        // Placeholder implementation - will need to check test metadata or name patterns
        // to determine if this is a Cucumber test
        return test.getName().contains("Scenario:");
    }

    /**
     * Process a failed Cucumber test by extracting relevant information
     * and passing it to the prompt formatter service
     *
     * @param test The failed test proxy
     */
    private void processFailedCucumberTest(SMTestProxy test) {
        try {
            // Extract information from the failed test
            String stackTrace = stackTraceExtractor.extractStackTrace(test);
            String failedStepText = stackTraceExtractor.extractFailedStepText(test);
            String scenarioName = test.getName().replace("Scenario: ", "");
            
            // Use PSI-based extractors to get code information
            String stepDefinitionMethod = stepDefinitionExtractor.extractStepDefinition(failedStepText);
            String sourceFilePath = stepDefinitionExtractor.getSourceFilePath();
            int lineNumber = stepDefinitionExtractor.getLineNumber();
            
            // Extract the Gherkin scenario
            String gherkinScenario = gherkinScenarioExtractor.extractScenario(scenarioName);
            
            // Create the failure info object
            FailureInfo failureInfo = new FailureInfo.Builder()
                .withScenarioName(scenarioName)
                .withFailedStepText(failedStepText)
                .withStackTrace(stackTrace)
                .withStepDefinitionMethod(stepDefinitionMethod)
                .withGherkinScenario(gherkinScenario)
                .withSourceFilePath(sourceFilePath)
                .withLineNumber(lineNumber)
                .build();
            
            // Format the information for analysis
            PromptFormatterService promptFormatter = project.getService(PromptFormatterService.class);
            String formattedPrompt = promptFormatter.formatPrompt(failureInfo);
            
            // TODO: Display the formatted prompt in the TriagePanel tool window
        } catch (Exception e) {
            // Log the error - will implement proper logging
            e.printStackTrace();
        }
    }
} 