package com.triagemate.ui;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.triagemate.models.FailureInfo;
import com.triagemate.models.StepDefinitionInfo;
import com.triagemate.models.GherkinScenarioInfo;

import java.util.List;

/**
 * Integration tests for TriagePanelView.
 * Tests the chat interface functionality and failure display with IntelliJ Platform integration.
 */
public class TriagePanelViewIntegrationTest extends BasePlatformTestCase {

    private TriagePanelView triagePanelView;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        triagePanelView = new TriagePanelView(getProject());
    }

    /**
     * Test that the TriagePanelView can be created successfully.
     */
    public void testTriagePanelViewCreation() {
        assertNotNull("TriagePanelView should be created", triagePanelView);
        assertNotNull("Content should be available", triagePanelView.getContent());
    }

    /**
     * Test that the panel can handle a failure update.
     */
    public void testUpdateFailure() {
        // Create a sample failure info
        FailureInfo failureInfo = createSampleFailureInfo();
        
        // Update the panel with the failure
        triagePanelView.updateFailure(failureInfo);
        
        // Verify the panel was updated (basic check)
        assertNotNull("Panel should still have content after update", triagePanelView.getContent());
    }

    /**
     * Test that the panel can handle null failure info gracefully.
     */
    public void testUpdateFailureWithNull() {
        // This should not throw an exception
        triagePanelView.updateFailure(null);
        
        // Verify the panel still has content
        assertNotNull("Panel should still have content after null update", triagePanelView.getContent());
    }

    /**
     * Test that the panel can handle multiple failure updates.
     */
    public void testMultipleFailureUpdates() {
        // Create multiple failure infos
        FailureInfo failure1 = createSampleFailureInfo("Test Scenario 1", "WebDriver Error");
        FailureInfo failure2 = createSampleFailureInfo("Test Scenario 2", "JUnit Error");
        
        // Update the panel multiple times
        triagePanelView.updateFailure(failure1);
        triagePanelView.updateFailure(failure2);
        
        // Verify the panel still has content
        assertNotNull("Panel should still have content after multiple updates", triagePanelView.getContent());
    }

    /**
     * Creates a sample FailureInfo for testing.
     */
    private FailureInfo createSampleFailureInfo() {
        return createSampleFailureInfo("Sample Scenario", "Sample Error");
    }

    /**
     * Creates a sample FailureInfo with custom values for testing.
     */
    private FailureInfo createSampleFailureInfo(String scenarioName, String errorType) {
        StepDefinitionInfo stepDefInfo = new StepDefinitionInfo(
            "givenUserIsOnHomePage",
            "StepDefinitions",
            "com.example",
            "StepDefinitions.java",
            42,
            "Given the user is on the home page",
            List.of("user"),
            "public void givenUserIsOnHomePage() { /* implementation */ }"
        );

        GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo(
            "Sample Feature",
            scenarioName,
            List.of("Given the user is on the home page", "When the user clicks the login button", "Then the user should see the login form"),
            List.of("@smoke", "@regression"),
            List.of(), // backgroundSteps
            List.of(), // dataTable
            "Feature: Sample Feature\nScenario: " + scenarioName + "\n  Given the user is on the home page\n  When the user clicks the login button\n  Then the user should see the login form",
            false, // isScenarioOutline
            "src/test/resources/features/sample.feature",
            10,
            "Feature: Sample Feature\nScenario: " + scenarioName + "\n  Given the user is on the home page\n  When the user clicks the login button\n  Then the user should see the login form"
        );

        return new FailureInfo.Builder()
            .withScenarioName(scenarioName)
            .withFailedStepText("Then the user should see the login form")
            .withStackTrace("java.lang.AssertionError: Expected element to be visible but it was not")
            .withSourceFilePath("src/test/java/com/example/StepDefinitions.java")
            .withLineNumber(42)
            .withStepDefinitionInfo(stepDefInfo)
            .withGherkinScenarioInfo(scenarioInfo)
            .withExpectedValue("true")
            .withActualValue("false")
            .withErrorMessage("Test failure occurred")
            .withErrorMessage("Element not found: #login-form")
            .withParsingTime(150)
            .build();
    }
} 