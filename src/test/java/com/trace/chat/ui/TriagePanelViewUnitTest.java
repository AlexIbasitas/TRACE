package com.trace.chat.ui;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.trace.test.models.FailureInfo;
import com.trace.test.models.GherkinScenarioInfo;
import com.trace.test.models.StepDefinitionInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;

/**
 * Unit tests for TriagePanelView analysis mode functionality.
 * 
 * <p>Tests the analysis mode toggle feature that allows users to switch
 * between Overview and Full Analysis modes for AI prompt generation.</p>
 * 
 * @author Alex Ibasitas
 * @version 1.0
 * @since 1.0
 */
@DisplayName("TriagePanelView Analysis Mode Tests")
public class TriagePanelViewUnitTest extends BasePlatformTestCase {
    
    private TriagePanelView triagePanelView;
    
    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        triagePanelView = new TriagePanelView(getProject());
    }
    
    @Test
    @DisplayName("Should initialize with Quick Overview mode by default")
    void shouldInitializeWithOverviewModeByDefault() {
        // Given: TriagePanelView is created
        
        // When: Getting the current analysis mode
        String currentMode = triagePanelView.getCurrentAnalysisMode();
        
        // Then: Should be Quick Overview by default
        assertEquals("Quick Overview", currentMode);
    }
    
    @Test
    @DisplayName("Should set analysis mode to Full Analysis")
    void shouldSetAnalysisModeToFullAnalysis() {
        // Given: TriagePanelView is created with default Quick Overview mode
        
        // When: Setting the analysis mode to Full Analysis
        triagePanelView.setCurrentAnalysisMode("Full Analysis");
        
        // Then: Should be set to Full Analysis
        assertEquals("Full Analysis", triagePanelView.getCurrentAnalysisMode());
    }
    
    @Test
    @DisplayName("Should set analysis mode back to Quick Overview")
    void shouldSetAnalysisModeBackToOverview() {
        // Given: TriagePanelView is created and set to Full Analysis
        triagePanelView.setCurrentAnalysisMode("Full Analysis");
        
        // When: Setting the analysis mode back to Quick Overview
        triagePanelView.setCurrentAnalysisMode("Quick Overview");
        
        // Then: Should be set to Quick Overview
        assertEquals("Quick Overview", triagePanelView.getCurrentAnalysisMode());
    }
    
    @Test
    @DisplayName("Should throw exception when setting null analysis mode")
    void shouldThrowExceptionWhenSettingNullAnalysisMode() {
        // Given: TriagePanelView is created
        
        // When/Then: Setting null should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            triagePanelView.setCurrentAnalysisMode(null);
        });
    }
    
    @Test
    @DisplayName("Should throw exception when setting invalid analysis mode")
    void shouldThrowExceptionWhenSettingInvalidAnalysisMode() {
        // Given: TriagePanelView is created
        
        // When/Then: Setting invalid mode should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            triagePanelView.setCurrentAnalysisMode("Invalid Mode");
        });
    }
    
    @Test
    @DisplayName("Should create valid failure info for testing")
    void shouldCreateValidFailureInfoForTesting() {
        // Given: Creating a test failure info
        FailureInfo failureInfo = createTestFailureInfo();
        
        // When: Checking the failure info properties
        
        // Then: Should have valid properties
        assertNotNull(failureInfo);
        assertEquals("Test Scenario", failureInfo.getScenarioName());
        assertEquals("Given I am on the test page", failureInfo.getFailedStepText());
        assertEquals("Expected: 'Test Title', Actual: 'Wrong Title'", failureInfo.getErrorMessage());
    }
    
    /**
     * Test the "First Failure Wins" feature.
     * Verifies that only the first failure of a test run gets analyzed.
     */
    @Test
    public void testFirstFailureWins() {
        // Create mock failure info
        FailureInfo firstFailure = new FailureInfo.Builder()
            .withScenarioName("Test Scenario 1")
            .withFailedStepText("I should see a link with text 'zoro'")
            .withErrorMessage("Element not found")
            .build();
        
        FailureInfo secondFailure = new FailureInfo.Builder()
            .withScenarioName("Test Scenario 2")
            .withFailedStepText("I should see a link with text 'A/B Testing'")
            .withErrorMessage("Element not found")
            .build();
        
        // Simulate new test run start
        triagePanelView.onTestRunStarted();
        
        // First failure should be analyzed
        triagePanelView.updateFailure(firstFailure);
        assertEquals("First failure should be analyzed", 1, triagePanelView.getChatHistory().size());
        
        // Second failure should be ignored
        triagePanelView.updateFailure(secondFailure);
        assertEquals("Second failure should be ignored", 1, triagePanelView.getChatHistory().size());
        
        // Simulate new test run start
        triagePanelView.onTestRunStarted();
        
        // Third failure should be analyzed (new test run)
        FailureInfo thirdFailure = new FailureInfo.Builder()
            .withScenarioName("Test Scenario 3")
            .withFailedStepText("I should see a link with text 'Checkboxes'")
            .withErrorMessage("Element not found")
            .build();
        
        triagePanelView.updateFailure(thirdFailure);
        assertEquals("Third failure should be analyzed in new test run", 1, triagePanelView.getChatHistory().size());
    }
    
    /**
     * Creates a test FailureInfo object for testing purposes.
     *
     * @return A test FailureInfo object
     */
    private FailureInfo createTestFailureInfo() {
        // Create Gherkin scenario info using Builder pattern
        GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
            .withFeatureName("Test Feature")
            .withScenarioName("Test Scenario")
            .withSteps(Arrays.asList(
                "Given I am on the test page",
                "When I check the page title",
                "Then the title should be \"Test Title\""
            ))
            .withTags(Arrays.asList("@test", "@ui"))
            .build();
        
        // Create step definition info using Builder pattern
        StepDefinitionInfo stepDefInfo = new StepDefinitionInfo.Builder()
            .withClassName("com.example.TestClass")
            .withMethodName("testPageTitle")
            .withStepPattern("I am on the test page")
            .withParameters(Arrays.asList())
            .withMethodText("@Given(\"I am on the test page\")\npublic void testPageTitle() {\n    // Test implementation\n}")
            .withSourceFilePath("src/test/java/com/example/TestClass.java")
            .withLineNumber(42)
            .build();
        
        // Create failure info using Builder pattern
        return new FailureInfo.Builder()
            .withScenarioName("Test Scenario")
            .withFailedStepText("Given I am on the test page")
            .withErrorMessage("Expected: 'Test Title', Actual: 'Wrong Title'")
            .withExpectedValue("Test Title")
            .withActualValue("Wrong Title")
            .withSourceFilePath("src/test/java/com/example/TestClass.java")
            .withLineNumber(42)
            .withStepDefinitionInfo(stepDefInfo)
            .withGherkinScenarioInfo(scenarioInfo)
            .withParsingTime(100L)
            .build();
    }
} 