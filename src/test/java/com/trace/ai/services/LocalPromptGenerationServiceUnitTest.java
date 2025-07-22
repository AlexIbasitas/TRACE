package com.trace.ai.services;

import com.trace.ai.prompts.LocalPromptGenerationService;
import com.trace.test.models.FailureInfo;
import com.trace.test.models.GherkinScenarioInfo;
import com.trace.test.models.StepDefinitionInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LocalPromptGenerationService.
 * 
 * <p>These tests verify that the service correctly generates structured prompts
 * from FailureInfo data, including different prompt types and edge cases.</p>
 */
class LocalPromptGenerationServiceUnitTest {

    private LocalPromptGenerationService service;

    @BeforeEach
    void setUp() {
        service = new LocalPromptGenerationService();
    }

    @Test
    @DisplayName("should generate detailed prompt with minimal failure info")
    void shouldGenerateDetailedPromptWithMinimalFailureInfo() {
        // Arrange
        FailureInfo failureInfo = new FailureInfo.Builder()
            .withScenarioName("Login Test")
            .withFailedStepText("When user enters credentials")
            .withErrorMessage("Element not found")
            .build();

        // Act
        String prompt = service.generateDetailedPrompt(failureInfo);

        // Assert
        assertNotNull(prompt);
        assertTrue(prompt.contains("Login Test"));
        assertTrue(prompt.contains("When user enters credentials"));
        // Note: Error message and assertion type are no longer included in the optimized prompt
        assertTrue(prompt.contains("### Instruction ###"));
        assertTrue(prompt.contains("### Test Failure Context ###"));
        assertTrue(prompt.contains("### Error Details ###"));
        assertTrue(prompt.contains("### Code Context ###"));
        assertTrue(prompt.contains("### Analysis Request ###"));
        assertTrue(prompt.contains("Failure Analysis"));
        assertTrue(prompt.contains("Technical Details"));
        assertTrue(prompt.contains("Recommended Actions"));
        // Conditional sections should not appear for minimal data
        assertFalse(prompt.contains("### Gherkin Scenario ###"));
        assertFalse(prompt.contains("### Step Definition ###"));
    }

    @Test
    @DisplayName("should generate detailed prompt with complete failure info")
    void shouldGenerateDetailedPromptWithCompleteFailureInfo() {
        // Arrange
        GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
            .withFeatureName("Login Feature")
            .withScenarioName("Successful Login")
            .withSteps(Arrays.asList("Given user is on login page", "When user enters credentials", "Then user should be logged in"))
            .withTags(Arrays.asList("@smoke", "@login"))
            .build();

        StepDefinitionInfo stepDefInfo = new StepDefinitionInfo.Builder()
            .withClassName("LoginStepDefinitions")
            .withMethodName("userEntersCredentials")
            .withStepPattern("user enters credentials")
            .withParameters(Arrays.asList("username", "password"))
            .withMethodText("@When(\"user enters credentials\")\npublic void userEntersCredentials(String username, String password) { }")
            .withSourceFilePath("/src/test/java/LoginStepDefinitions.java")
            .withLineNumber(25)
            .build();

        FailureInfo failureInfo = new FailureInfo.Builder()
            .withScenarioName("Successful Login")
            .withFailedStepText("When user enters credentials")
            .withStackTrace("org.openqa.selenium.NoSuchElementException: Unable to locate element")
            .withSourceFilePath("/src/test/java/LoginStepDefinitions.java")
            .withLineNumber(25)
            .withStepDefinitionInfo(stepDefInfo)
            .withGherkinScenarioInfo(scenarioInfo)
            .withExpectedValue("true")
            .withActualValue("false")
            .withErrorMessage("Unable to locate element")
            .withParsingTime(150L)
            .build();

        // Act
        String prompt = service.generateDetailedPrompt(failureInfo);

        // Assert
        assertNotNull(prompt);
        assertTrue(prompt.contains("### Instruction ###"));
        assertTrue(prompt.contains("### Test Failure Context ###"));
        assertTrue(prompt.contains("Successful Login"));
        assertTrue(prompt.contains("When user enters credentials"));
        assertTrue(prompt.contains("### Error Details ###"));
        assertTrue(prompt.contains("**Expected Value:** true"));
        assertTrue(prompt.contains("**Actual Value:** false"));
        assertTrue(prompt.contains("### Gherkin Scenario ###"));
        assertTrue(prompt.contains("**Feature:** Login Feature"));
        assertTrue(prompt.contains("**Scenario:** Successful Login"));
        assertTrue(prompt.contains("@smoke, @login"));
        assertTrue(prompt.contains("Given user is on login page"));
        assertTrue(prompt.contains("Then user should be logged in"));
        assertTrue(prompt.contains("### Step Definition ###"));
        assertTrue(prompt.contains("**Class:** LoginStepDefinitions"));
        assertTrue(prompt.contains("**Method:** userEntersCredentials"));
        assertTrue(prompt.contains("**Pattern:** user enters credentials"));
        assertTrue(prompt.contains("username, password"));
        assertTrue(prompt.contains("**Implementation:**"));
        assertTrue(prompt.contains("```java"));
        assertTrue(prompt.contains("### Code Context ###"));
        assertTrue(prompt.contains("LoginStepDefinitions.java"));
        assertTrue(prompt.contains("### Analysis Request ###"));
        assertTrue(prompt.contains("Failure Analysis"));
        assertTrue(prompt.contains("Technical Details"));
        assertTrue(prompt.contains("Recommended Actions"));
    }

    @Test
    @DisplayName("should generate summary prompt for quick analysis")
    void shouldGenerateSummaryPromptForQuickAnalysis() {
        // Arrange
        FailureInfo failureInfo = new FailureInfo.Builder()
            .withScenarioName("Login Test")
            .withFailedStepText("When user enters credentials")
            .withErrorMessage("Element not found")
            .withExpectedValue("true")
            .withActualValue("false")
            .build();

        // Act
        String prompt = service.generateSummaryPrompt(failureInfo);

        // Assert
        assertNotNull(prompt);
        assertTrue(prompt.contains("### Instruction ###"));
        assertTrue(prompt.contains("### Context ###"));
        assertTrue(prompt.contains("### Output Format ###"));
        assertTrue(prompt.contains("Login Test"));
        assertTrue(prompt.contains("When user enters credentials"));
        assertTrue(prompt.contains("Element not found"));
        assertTrue(prompt.contains("**Expected:** true"));
        assertTrue(prompt.contains("**Actual:** false"));
        assertTrue(prompt.contains("**Issue:**"));
        assertTrue(prompt.contains("**Likely Cause:**"));
        assertTrue(prompt.contains("**Suggested Fix:**"));
    }

    @Test
    @DisplayName("should handle null failure info gracefully")
    void shouldHandleNullFailureInfoGracefully() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> service.generateSummaryPrompt(null));
        assertThrows(IllegalArgumentException.class, () -> service.generateDetailedPrompt(null));
    }

    @Test
    @DisplayName("should generate detailed prompt with assertion details")
    void shouldGenerateDetailedPromptWithAssertionDetails() {
        // Arrange
        FailureInfo failureInfo = new FailureInfo.Builder()
            .withScenarioName("Validation Test")
            .withFailedStepText("Then the result should be correct")
            .withErrorMessage("JUnit ComparisonFailure: expected:<true> but was:<false>")
            .withExpectedValue("true")
            .withActualValue("false")
            .build();

        // Act
        String prompt = service.generateDetailedPrompt(failureInfo);

        // Assert
        assertNotNull(prompt);
        assertTrue(prompt.contains("### Error Details ###"));
        assertTrue(prompt.contains("**Expected Value:** true"));
        assertTrue(prompt.contains("**Actual Value:** false"));
        // Note: Error message is no longer included in the optimized prompt structure
    }

    @Test
    @DisplayName("should generate prompt with cleaned stack trace")
    void shouldGeneratePromptWithCleanedStackTrace() {
        // Arrange
        String rawStackTrace = "=== SOURCE INFORMATION ===\n" +
                              "Primary source: stack trace\n" +
                              "Test name: I should see title\n" +
                              "Test location: file:///path/to/test.feature:8\n" +
                              "=== ERROR MESSAGE ===\n" +
                              "Step failed\n" +
                              "=== PRIMARY OUTPUT ===\n" +
                              "java.lang.AssertionError: \n" +
                              "Expected: is \"Welcome to the-internet delete me\"\n" +
                              "     but: was \"Welcome to the-internet\"\n" +
                              "\tat org.hamcrest.MatcherAssert.assertThat(MatcherAssert.java:20)\n" +
                              "\tat com.example.steps.HomePageTestStep.i_should_see_title_as(HomePageTestStep.java:28)";

        FailureInfo failureInfo = new FailureInfo.Builder()
            .withScenarioName("Element Test")
            .withFailedStepText("When I click the button")
            .withErrorMessage("Unable to locate element")
            .withStackTrace(rawStackTrace)
            .build();

        // Act
        String prompt = service.generateDetailedPrompt(failureInfo);

        // Assert
        assertNotNull(prompt);
        assertTrue(prompt.contains("### Error Details ###"));
        assertTrue(prompt.contains("**Stack Trace:**"));
        assertTrue(prompt.contains("```"));
        assertTrue(prompt.contains("java.lang.AssertionError"));
        assertTrue(prompt.contains("com.example.steps.HomePageTestStep.i_should_see_title_as"));
        // Should not contain the metadata that was cleaned
        assertFalse(prompt.contains("=== SOURCE INFORMATION ==="));
        assertFalse(prompt.contains("Primary source: stack trace"));
        assertFalse(prompt.contains("Test name:"));
        assertFalse(prompt.contains("Test location:"));
        assertFalse(prompt.contains("=== ERROR MESSAGE ==="));
        assertFalse(prompt.contains("Step failed"));
        assertFalse(prompt.contains("=== PRIMARY OUTPUT ==="));
    }

    @Test
    @DisplayName("should generate prompt with step definition information")
    void shouldGeneratePromptWithStepDefinitionInformation() {
        // Arrange
        StepDefinitionInfo stepDefInfo = new StepDefinitionInfo.Builder()
            .withClassName("MyStepDefinitions")
            .withMethodName("iClickTheButton")
            .withStepPattern("I click the button")
            .withParameters(Arrays.asList("buttonName"))
            .withMethodText("@When(\"I click the {string} button\")\npublic void iClickTheButton(String buttonName) { }")
            .withSourceFilePath("/src/test/java/MyStepDefinitions.java")
            .withLineNumber(30)
            .build();

        FailureInfo failureInfo = new FailureInfo.Builder()
            .withScenarioName("Button Test")
            .withFailedStepText("When I click the button")
            .withStepDefinitionInfo(stepDefInfo)
            .withSourceFilePath("/src/test/java/MyStepDefinitions.java")
            .withLineNumber(30)
            .build();

        // Act
        String prompt = service.generateDetailedPrompt(failureInfo);

        // Assert
        assertNotNull(prompt);
        assertTrue(prompt.contains("### Step Definition ###"));
        assertTrue(prompt.contains("**Class:** MyStepDefinitions"));
        assertTrue(prompt.contains("**Method:** iClickTheButton"));
        assertTrue(prompt.contains("**Pattern:** I click the button"));
        assertTrue(prompt.contains("buttonName"));
        assertTrue(prompt.contains("@When(\"I click the {string} button\")"));
        assertTrue(prompt.contains("**Implementation:**"));
        assertTrue(prompt.contains("```java"));
        assertTrue(prompt.contains("### Code Context ###"));
        assertTrue(prompt.contains("MyStepDefinitions.java"));
    }

    @Test
    @DisplayName("should generate prompt with scenario details")
    void shouldGeneratePromptWithScenarioDetails() {
        // Arrange
        GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
            .withFeatureName("Shopping Cart Feature")
            .withScenarioName("Add Item to Cart")
            .withSteps(Arrays.asList(
                "Given I am on the product page",
                "When I click add to cart",
                "Then the item should be in my cart"
            ))
            .withTags(Arrays.asList("@shopping", "@regression"))
            .build();

        FailureInfo failureInfo = new FailureInfo.Builder()
            .withScenarioName("Add Item to Cart")
            .withFailedStepText("When I click add to cart")
            .withGherkinScenarioInfo(scenarioInfo)
            .build();

        // Act
        String prompt = service.generateDetailedPrompt(failureInfo);

        // Assert
        assertNotNull(prompt);
        assertTrue(prompt.contains("### Test Failure Context ###"));
        assertTrue(prompt.contains("Add Item to Cart"));
        assertTrue(prompt.contains("When I click add to cart"));
        assertTrue(prompt.contains("### Gherkin Scenario ###"));
        assertTrue(prompt.contains("**Feature:** Shopping Cart Feature"));
        assertTrue(prompt.contains("**Scenario:** Add Item to Cart"));
        assertTrue(prompt.contains("@shopping, @regression"));
        assertTrue(prompt.contains("Given I am on the product page"));
        assertTrue(prompt.contains("Then the item should be in my cart"));
        assertTrue(prompt.contains("```gherkin"));
    }

    @Test
    @DisplayName("should handle missing optional fields gracefully")
    void shouldHandleMissingOptionalFieldsGracefully() {
        // Arrange
        FailureInfo failureInfo = new FailureInfo.Builder()
            .withScenarioName("Basic Test")
            .withFailedStepText("When something happens")
            .withErrorMessage("Something went wrong")
            .build();

        // Act
        String prompt = service.generateDetailedPrompt(failureInfo);

        // Assert
        assertNotNull(prompt);
        assertTrue(prompt.contains("### Test Failure Context ###"));
        assertTrue(prompt.contains("Basic Test"));
        assertTrue(prompt.contains("When something happens"));
        assertTrue(prompt.contains("### Error Details ###"));
        // Note: Error message is no longer included in the optimized prompt structure
        // Conditional sections should not appear when data is missing
        assertFalse(prompt.contains("### Gherkin Scenario ###"));
        assertFalse(prompt.contains("### Step Definition ###"));
        // Should not contain sections for missing data
        assertFalse(prompt.contains("**Expected Value:**"));
        assertFalse(prompt.contains("**Actual Value:**"));
    }

    @Test
    @DisplayName("should extract filename from full path")
    void shouldExtractFilenameFromFullPath() {
        // Arrange
        FailureInfo failureInfo = new FailureInfo.Builder()
            .withScenarioName("Test Scenario")
            .withFailedStepText("When something fails")
            .withErrorMessage("Test failed")
            .withSourceFilePath("/Users/alexibasitas/Projects/trace/src/test/java/MyTest.java")
            .withLineNumber(42)
            .build();

        // Act
        String prompt = service.generateDetailedPrompt(failureInfo);

        // Assert
        assertNotNull(prompt);
        assertTrue(prompt.contains("### Code Context ###"));
        assertTrue(prompt.contains("**Source File:** MyTest.java"));
        assertTrue(prompt.contains("**Line Number:** 42"));
        // Should not contain the full path
        assertFalse(prompt.contains("/Users/alexibasitas/Projects/trace/src/test/java/MyTest.java"));
    }
} 