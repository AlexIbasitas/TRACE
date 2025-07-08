package com.triagemate.services;

import com.triagemate.models.FailureInfo;
import com.triagemate.models.GherkinScenarioInfo;
import com.triagemate.models.StepDefinitionInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

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
            .withAssertionType("WEBDRIVER_ERROR")
            .build();

        // Act
        String prompt = service.generateDetailedPrompt(failureInfo);

        // Assert
        assertNotNull(prompt);
        assertTrue(prompt.contains("Login Test"));
        assertTrue(prompt.contains("When user enters credentials"));
        assertTrue(prompt.contains("Element not found"));
        assertTrue(prompt.contains("WEBDRIVER_ERROR"));
        assertTrue(prompt.contains("Comprehensive Test Failure Analysis"));
        assertTrue(prompt.contains("Root Cause Analysis"));
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
            .withAssertionType("WEBDRIVER_ERROR")
            .withErrorMessage("Unable to locate element")
            .withParsingStrategy("WebDriverErrorStrategy")
            .withParsingTime(150L)
            .build();

        // Act
        String prompt = service.generateDetailedPrompt(failureInfo);

        // Assert
        assertNotNull(prompt);
        assertTrue(prompt.contains("Comprehensive Test Failure Analysis"));
        assertTrue(prompt.contains("Login Feature"));
        assertTrue(prompt.contains("Successful Login"));
        assertTrue(prompt.contains("@smoke @login"));
        assertTrue(prompt.contains("Given user is on login page"));
        assertTrue(prompt.contains("LoginStepDefinitions"));
        assertTrue(prompt.contains("userEntersCredentials"));
        assertTrue(prompt.contains("username, password"));
        assertTrue(prompt.contains("Unable to locate element"));
        assertTrue(prompt.contains("WebDriverErrorStrategy"));
        assertTrue(prompt.contains("150ms"));
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
        assertTrue(prompt.contains("Test Failure Summary"));
        assertTrue(prompt.contains("Login Test"));
        assertTrue(prompt.contains("When user enters credentials"));
        assertTrue(prompt.contains("Element not found"));
        assertTrue(prompt.contains("**Expected:** true"));
        assertTrue(prompt.contains("**Actual:** false"));
        assertTrue(prompt.contains("Likely cause of the failure"));
        assertTrue(prompt.contains("Suggested fix"));
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
            .withAssertionType("JUNIT_COMPARISON")
            .build();

        // Act
        String prompt = service.generateDetailedPrompt(failureInfo);

        // Assert
        assertNotNull(prompt);
        assertTrue(prompt.contains("**Expected Value:** true"));
        assertTrue(prompt.contains("**Actual Value:** false"));
        assertTrue(prompt.contains("JUNIT_COMPARISON"));
        assertTrue(prompt.contains("JUnit ComparisonFailure"));
    }

    @Test
    @DisplayName("should generate prompt with stack trace")
    void shouldGeneratePromptWithStackTrace() {
        // Arrange
        String stackTrace = "org.openqa.selenium.NoSuchElementException: Unable to locate element\n" +
                           "    at com.example.MyTest.testSomething(MyTest.java:42)\n" +
                           "    at org.openqa.selenium.remote.RemoteWebDriver.findElement(RemoteWebDriver.java:315)";

        FailureInfo failureInfo = new FailureInfo.Builder()
            .withScenarioName("Element Test")
            .withFailedStepText("When I click the button")
            .withErrorMessage("Unable to locate element")
            .withStackTrace(stackTrace)
            .build();

        // Act
        String prompt = service.generateDetailedPrompt(failureInfo);

        // Assert
        assertNotNull(prompt);
        assertTrue(prompt.contains("Stack Trace:"));
        assertTrue(prompt.contains("```"));
        assertTrue(prompt.contains("org.openqa.selenium.NoSuchElementException"));
        assertTrue(prompt.contains("com.example.MyTest.testSomething"));
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
        assertTrue(prompt.contains("MyStepDefinitions"));
        assertTrue(prompt.contains("iClickTheButton"));
        assertTrue(prompt.contains("I click the button"));
        assertTrue(prompt.contains("buttonName"));
        assertTrue(prompt.contains("@When(\"I click the {string} button\")"));
        assertTrue(prompt.contains("Method Implementation:"));
        assertTrue(prompt.contains("```java"));
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
        assertTrue(prompt.contains("Shopping Cart Feature"));
        assertTrue(prompt.contains("Add Item to Cart"));
        assertTrue(prompt.contains("@shopping @regression"));
        assertTrue(prompt.contains("Given I am on the product page"));
        assertTrue(prompt.contains("When I click add to cart"));
        assertTrue(prompt.contains("Then the item should be in my cart"));
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
        assertTrue(prompt.contains("Basic Test"));
        assertTrue(prompt.contains("When something happens"));
        assertTrue(prompt.contains("Something went wrong"));
        // Should not contain sections for missing data
        assertFalse(prompt.contains("Expected Value:"));
        assertFalse(prompt.contains("Actual Value:"));
        assertFalse(prompt.contains("Step Definition Class:"));
    }

    @Test
    @DisplayName("should include parsing metadata in detailed prompt")
    void shouldIncludeParsingMetadataInDetailedPrompt() {
        // Arrange
        FailureInfo failureInfo = new FailureInfo.Builder()
            .withScenarioName("Test Scenario")
            .withFailedStepText("When something fails")
            .withErrorMessage("Test failed")
            .withParsingStrategy("JUnitComparisonFailureStrategy")
            .withParsingTime(250L)
            .build();

        // Act
        String prompt = service.generateDetailedPrompt(failureInfo);

        // Assert
        assertNotNull(prompt);
        assertTrue(prompt.contains("**Parsing Strategy:** JUnitComparisonFailureStrategy"));
        assertTrue(prompt.contains("**Parsing Time:** 250ms"));
    }
} 