package com.trace.test.models;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("Failure Info Unit Tests")
class FailureInfoUnitTest {

    @Nested
    @DisplayName("Constructor and Builder")
    class ConstructorAndBuilder {

        @Test
        @DisplayName("should create FailureInfo with all required fields")
        void shouldCreateFailureInfoWithAllRequiredFields() {
            // Arrange
            String scenarioName = "User login with valid credentials";
            String failedStepText = "I click the login button";
            String stackTrace = "java.lang.AssertionError: Button not found";
            String sourceFilePath = "LoginStepDefinitions.java";
            int lineNumber = 25;

            // Act
            FailureInfo failureInfo = new FailureInfo(scenarioName, failedStepText, stackTrace,
                sourceFilePath, lineNumber, null, null, null, null, null, 0);

            // Assert
            assertThat(failureInfo).isNotNull();
            assertThat(failureInfo.getScenarioName()).isEqualTo(scenarioName);
            assertThat(failureInfo.getFailedStepText()).isEqualTo(failedStepText);
            assertThat(failureInfo.getStackTrace()).isEqualTo(stackTrace);
            assertThat(failureInfo.getSourceFilePath()).isEqualTo(sourceFilePath);
            assertThat(failureInfo.getLineNumber()).isEqualTo(lineNumber);
        }

        @Test
        @DisplayName("should create FailureInfo using builder pattern")
        void shouldCreateFailureInfoUsingBuilderPattern() {
            // Arrange
            String scenarioName = "User login with valid credentials";
            String failedStepText = "I click the login button";
            String stackTrace = "java.lang.AssertionError: Button not found";
            String sourceFilePath = "LoginStepDefinitions.java";
            int lineNumber = 25;

            // Act
            FailureInfo failureInfo = new FailureInfo.Builder()
                .withScenarioName(scenarioName)
                .withFailedStepText(failedStepText)
                .withStackTrace(stackTrace)
                .withSourceFilePath(sourceFilePath)
                .withLineNumber(lineNumber)
                .build();

            // Assert
            assertThat(failureInfo).isNotNull();
            assertThat(failureInfo.getScenarioName()).isEqualTo(scenarioName);
            assertThat(failureInfo.getFailedStepText()).isEqualTo(failedStepText);
            assertThat(failureInfo.getStackTrace()).isEqualTo(stackTrace);
            assertThat(failureInfo.getSourceFilePath()).isEqualTo(sourceFilePath);
            assertThat(failureInfo.getLineNumber()).isEqualTo(lineNumber);
        }

        @Test
        @DisplayName("should handle null optional fields gracefully")
        void shouldHandleNullOptionalFieldsGracefully() {
            // Arrange
            String scenarioName = "User login with valid credentials";
            String failedStepText = "I click the login button";

            // Act
            FailureInfo failureInfo = new FailureInfo.Builder()
                .withScenarioName(scenarioName)
                .withFailedStepText(failedStepText)
                .withStackTrace(null)
                .withSourceFilePath(null)
                .withLineNumber(-1)
                .withStepDefinitionInfo(null)
                .withGherkinScenarioInfo(null)
                .withExpectedValue(null)
                .withActualValue(null)
                .withErrorMessage(null)
                .withParsingTime(0)
                .build();

            // Assert
            assertThat(failureInfo).isNotNull();
            assertThat(failureInfo.getScenarioName()).isEqualTo(scenarioName);
            assertThat(failureInfo.getFailedStepText()).isEqualTo(failedStepText);
            assertThat(failureInfo.getStackTrace()).isNull();
            assertThat(failureInfo.getSourceFilePath()).isNull();
            assertThat(failureInfo.getLineNumber()).isEqualTo(-1);
            assertThat(failureInfo.getStepDefinitionInfo()).isNull();
            assertThat(failureInfo.getGherkinScenarioInfo()).isNull();
            assertThat(failureInfo.getExpectedValue()).isNull();
            assertThat(failureInfo.getActualValue()).isNull();
            assertThat(failureInfo.getErrorMessage()).isNull();
            assertThat(failureInfo.getParsingTime()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Core Failure Information")
    class CoreFailureInformation {

        @Test
        @DisplayName("should return correct scenario name")
        void shouldReturnCorrectScenarioName() {
            // Arrange
            String scenarioName = "User login with valid credentials";

            // Act
            FailureInfo failureInfo = new FailureInfo.Builder()
                .withScenarioName(scenarioName)
                .build();

            // Assert
            assertThat(failureInfo.getScenarioName()).isEqualTo(scenarioName);
        }

        @Test
        @DisplayName("should return correct failed step text")
        void shouldReturnCorrectFailedStepText() {
            // Arrange
            String failedStepText = "I click the login button";

            // Act
            FailureInfo failureInfo = new FailureInfo.Builder()
                .withFailedStepText(failedStepText)
                .build();

            // Assert
            assertThat(failureInfo.getFailedStepText()).isEqualTo(failedStepText);
        }

        @Test
        @DisplayName("should return correct stack trace")
        void shouldReturnCorrectStackTrace() {
            // Arrange
            String stackTrace = "java.lang.AssertionError: Button not found\n" +
                "at com.example.steps.LoginStepDefinitions.clickLoginButton(LoginStepDefinitions.java:25)";

            // Act
            FailureInfo failureInfo = new FailureInfo.Builder()
                .withStackTrace(stackTrace)
                .build();

            // Assert
            assertThat(failureInfo.getStackTrace()).isEqualTo(stackTrace);
        }

        @Test
        @DisplayName("should return correct source file path")
        void shouldReturnCorrectSourceFilePath() {
            // Arrange
            String sourceFilePath = "src/test/java/com/example/steps/LoginStepDefinitions.java";

            // Act
            FailureInfo failureInfo = new FailureInfo.Builder()
                .withSourceFilePath(sourceFilePath)
                .build();

            // Assert
            assertThat(failureInfo.getSourceFilePath()).isEqualTo(sourceFilePath);
        }

        @Test
        @DisplayName("should return correct line number")
        void shouldReturnCorrectLineNumber() {
            // Arrange
            int lineNumber = 25;

            // Act
            FailureInfo failureInfo = new FailureInfo.Builder()
                .withLineNumber(lineNumber)
                .build();

            // Assert
            assertThat(failureInfo.getLineNumber()).isEqualTo(lineNumber);
        }
    }

    @Nested
    @DisplayName("Structured Data")
    class StructuredData {

        @Test
        @DisplayName("should return correct step definition info")
        void shouldReturnCorrectStepDefinitionInfo() {
            // Arrange
            StepDefinitionInfo stepDefInfo = new StepDefinitionInfo.Builder()
                .withMethodName("clickLoginButton")
                .withClassName("LoginStepDefinitions")
                .withPackageName("com.example.steps")
                .withSourceFilePath("LoginStepDefinitions.java")
                .withLineNumber(25)
                .withStepPattern("^I click the (.*?) button$")
                .withParameters(Arrays.asList("buttonName"))
                .withMethodText("@When(\"^I click the (.*?) button$\") public void clickLoginButton(String buttonName) { ... }")
                .build();

            // Act
            FailureInfo failureInfo = new FailureInfo.Builder()
                .withStepDefinitionInfo(stepDefInfo)
                .build();

            // Assert
            assertThat(failureInfo.getStepDefinitionInfo()).isNotNull();
            assertThat(failureInfo.getStepDefinitionInfo().getMethodName()).isEqualTo("clickLoginButton");
            assertThat(failureInfo.getStepDefinitionInfo().getClassName()).isEqualTo("LoginStepDefinitions");
            assertThat(failureInfo.getStepDefinitionInfo().getPackageName()).isEqualTo("com.example.steps");
        }

        @Test
        @DisplayName("should return correct Gherkin scenario info")
        void shouldReturnCorrectGherkinScenarioInfo() {
            // Arrange
            GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
                .withScenarioName("User login with valid credentials")
                .withFeatureName("User Authentication")
                .withSourceFilePath("login.feature")
                .withLineNumber(5)
                .withSteps(Arrays.asList("Given I am on the login page", "When I click the login button", "Then I should be logged in"))
                .withTags(Arrays.asList("@smoke", "@login"))
                .withBackgroundSteps(Arrays.asList("Given the application is running"))
                .withDataTable(Arrays.asList("| username | password |", "| user1    | pass1    |"))
                .withIsScenarioOutline(false)
                .build();

            // Act
            FailureInfo failureInfo = new FailureInfo.Builder()
                .withGherkinScenarioInfo(scenarioInfo)
                .build();

            // Assert
            assertThat(failureInfo.getGherkinScenarioInfo()).isNotNull();
            assertThat(failureInfo.getGherkinScenarioInfo().getScenarioName()).isEqualTo("User login with valid credentials");
            assertThat(failureInfo.getGherkinScenarioInfo().getFeatureName()).isEqualTo("User Authentication");
            assertThat(failureInfo.getGherkinScenarioInfo().getSourceFilePath()).isEqualTo("login.feature");
        }

        @Test
        @DisplayName("should handle null structured data gracefully")
        void shouldHandleNullStructuredDataGracefully() {
            // Act
            FailureInfo failureInfo = new FailureInfo.Builder()
                .withStepDefinitionInfo(null)
                .withGherkinScenarioInfo(null)
                .build();

            // Assert
            assertThat(failureInfo.getStepDefinitionInfo()).isNull();
            assertThat(failureInfo.getGherkinScenarioInfo()).isNull();
        }
    }

    @Nested
    @DisplayName("Assertion Details")
    class AssertionDetails {

        @Test
        @DisplayName("should return correct expected value")
        void shouldReturnCorrectExpectedValue() {
            // Arrange
            String expectedValue = "true";

            // Act
            FailureInfo failureInfo = new FailureInfo.Builder()
                .withExpectedValue(expectedValue)
                .build();

            // Assert
            assertThat(failureInfo.getExpectedValue()).isEqualTo(expectedValue);
        }

        @Test
        @DisplayName("should return correct actual value")
        void shouldReturnCorrectActualValue() {
            // Arrange
            String actualValue = "false";

            // Act
            FailureInfo failureInfo = new FailureInfo.Builder()
                .withActualValue(actualValue)
                .build();

            // Assert
            assertThat(failureInfo.getActualValue()).isEqualTo(actualValue);
        }

        @Test
        @DisplayName("should return correct error message")
        void shouldReturnCorrectErrorMessage() {
            // Arrange
            String errorMessage = "Expected true but was false";

            // Act
            FailureInfo failureInfo = new FailureInfo.Builder()
                .withErrorMessage(errorMessage)
                .build();

            // Assert
            assertThat(failureInfo.getErrorMessage()).isEqualTo(errorMessage);
        }

        @Test
        @DisplayName("should handle null assertion details gracefully")
        void shouldHandleNullAssertionDetailsGracefully() {
            // Act
            FailureInfo failureInfo = new FailureInfo.Builder()
                .withExpectedValue(null)
                .withActualValue(null)
                .withErrorMessage(null)
                .build();

            // Assert
            assertThat(failureInfo.getExpectedValue()).isNull();
            assertThat(failureInfo.getActualValue()).isNull();
            assertThat(failureInfo.getErrorMessage()).isNull();
        }
    }

    @Nested
    @DisplayName("Parsing Metadata")
    class ParsingMetadata {

        @Test
        @DisplayName("should return correct parsing time")
        void shouldReturnCorrectParsingTime() {
            // Arrange
            long parsingTime = 150;

            // Act
            FailureInfo failureInfo = new FailureInfo.Builder()
                .withParsingTime(parsingTime)
                .build();

            // Assert
            assertThat(failureInfo.getParsingTime()).isEqualTo(parsingTime);
        }

        @Test
        @DisplayName("should handle zero parsing time")
        void shouldHandleZeroParsingTime() {
            // Arrange
            long parsingTime = 0;

            // Act
            FailureInfo failureInfo = new FailureInfo.Builder()
                .withParsingTime(parsingTime)
                .build();

            // Assert
            assertThat(failureInfo.getParsingTime()).isEqualTo(0);
        }

        @Test
        @DisplayName("should handle negative parsing time")
        void shouldHandleNegativeParsingTime() {
            // Arrange
            long parsingTime = -1;

            // Act
            FailureInfo failureInfo = new FailureInfo.Builder()
                .withParsingTime(parsingTime)
                .build();

            // Assert
            assertThat(failureInfo.getParsingTime()).isEqualTo(-1);
        }
    }

    @Nested
    @DisplayName("Builder Pattern")
    class BuilderPattern {

        @Test
        @DisplayName("should support method chaining")
        void shouldSupportMethodChaining() {
            // Act
            FailureInfo failureInfo = new FailureInfo.Builder()
                .withScenarioName("Test Scenario")
                .withFailedStepText("Test Step")
                .withStackTrace("Test Stack Trace")
                .withSourceFilePath("Test.java")
                .withLineNumber(10)
                .withExpectedValue("expected")
                .withActualValue("actual")
                .withErrorMessage("error")
                .withParsingTime(100)
                .build();

            // Assert
            assertThat(failureInfo).isNotNull();
            assertThat(failureInfo.getScenarioName()).isEqualTo("Test Scenario");
            assertThat(failureInfo.getFailedStepText()).isEqualTo("Test Step");
            assertThat(failureInfo.getStackTrace()).isEqualTo("Test Stack Trace");
            assertThat(failureInfo.getSourceFilePath()).isEqualTo("Test.java");
            assertThat(failureInfo.getLineNumber()).isEqualTo(10);
            assertThat(failureInfo.getExpectedValue()).isEqualTo("expected");
            assertThat(failureInfo.getActualValue()).isEqualTo("actual");
            assertThat(failureInfo.getErrorMessage()).isEqualTo("error");
            assertThat(failureInfo.getParsingTime()).isEqualTo(100);
        }

        @Test
        @DisplayName("should create multiple instances independently")
        void shouldCreateMultipleInstancesIndependently() {
            // Arrange
            FailureInfo.Builder builder = new FailureInfo.Builder();

            // Act
            FailureInfo failure1 = builder.withScenarioName("Scenario 1").build();
            FailureInfo failure2 = builder.withScenarioName("Scenario 2").build();

            // Assert
            assertThat(failure1.getScenarioName()).isEqualTo("Scenario 1");
            assertThat(failure2.getScenarioName()).isEqualTo("Scenario 2");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("should handle empty strings gracefully")
        void shouldHandleEmptyStringsGracefully() {
            // Act
            FailureInfo failureInfo = new FailureInfo.Builder()
                .withScenarioName("")
                .withFailedStepText("")
                .withStackTrace("")
                .withSourceFilePath("")
                .withExpectedValue("")
                .withActualValue("")
                .withErrorMessage("")
                .build();

            // Assert
            assertThat(failureInfo.getScenarioName()).isEmpty();
            assertThat(failureInfo.getFailedStepText()).isEmpty();
            assertThat(failureInfo.getStackTrace()).isEmpty();
            assertThat(failureInfo.getSourceFilePath()).isEmpty();
            assertThat(failureInfo.getExpectedValue()).isEmpty();
            assertThat(failureInfo.getActualValue()).isEmpty();
            assertThat(failureInfo.getErrorMessage()).isEmpty();
        }

        @Test
        @DisplayName("should handle very long text fields")
        void shouldHandleVeryLongTextFields() {
            // Arrange
            StringBuilder longText = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                longText.append("This is a very long text field that contains many characters ");
            }
            String veryLongText = longText.toString();

            // Act
            FailureInfo failureInfo = new FailureInfo.Builder()
                .withScenarioName(veryLongText)
                .withFailedStepText(veryLongText)
                .withStackTrace(veryLongText)
                .withErrorMessage(veryLongText)
                .build();

            // Assert
            assertThat(failureInfo.getScenarioName()).isEqualTo(veryLongText);
            assertThat(failureInfo.getFailedStepText()).isEqualTo(veryLongText);
            assertThat(failureInfo.getStackTrace()).isEqualTo(veryLongText);
            assertThat(failureInfo.getErrorMessage()).isEqualTo(veryLongText);
            assertThat(failureInfo.getScenarioName().length()).isGreaterThan(10000);
        }

        @Test
        @DisplayName("should handle special characters in text fields")
        void shouldHandleSpecialCharactersInTextFields() {
            // Arrange
            String specialChars = "Special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?\\`~";

            // Act
            FailureInfo failureInfo = new FailureInfo.Builder()
                .withScenarioName(specialChars)
                .withFailedStepText(specialChars)
                .withErrorMessage(specialChars)
                .build();

            // Assert
            assertThat(failureInfo.getScenarioName()).isEqualTo(specialChars);
            assertThat(failureInfo.getFailedStepText()).isEqualTo(specialChars);
            assertThat(failureInfo.getErrorMessage()).isEqualTo(specialChars);
        }

        @Test
        @DisplayName("should handle unicode characters in text fields")
        void shouldHandleUnicodeCharactersInTextFields() {
            // Arrange
            String unicodeText = "Unicode: café résumé naïve 测试 テスト";

            // Act
            FailureInfo failureInfo = new FailureInfo.Builder()
                .withScenarioName(unicodeText)
                .withFailedStepText(unicodeText)
                .withErrorMessage(unicodeText)
                .build();

            // Assert
            assertThat(failureInfo.getScenarioName()).isEqualTo(unicodeText);
            assertThat(failureInfo.getFailedStepText()).isEqualTo(unicodeText);
            assertThat(failureInfo.getErrorMessage()).isEqualTo(unicodeText);
        }

        @Test
        @DisplayName("should handle newlines in text fields")
        void shouldHandleNewlinesInTextFields() {
            // Arrange
            String textWithNewlines = "Line 1\nLine 2\nLine 3";

            // Act
            FailureInfo failureInfo = new FailureInfo.Builder()
                .withScenarioName(textWithNewlines)
                .withFailedStepText(textWithNewlines)
                .withErrorMessage(textWithNewlines)
                .build();

            // Assert
            assertThat(failureInfo.getScenarioName()).isEqualTo(textWithNewlines);
            assertThat(failureInfo.getFailedStepText()).isEqualTo(textWithNewlines);
            assertThat(failureInfo.getErrorMessage()).isEqualTo(textWithNewlines);
            assertThat(failureInfo.getScenarioName()).contains("\n");
        }

        @Test
        @DisplayName("should handle very large line numbers")
        void shouldHandleVeryLargeLineNumbers() {
            // Arrange
            int largeLineNumber = Integer.MAX_VALUE;

            // Act
            FailureInfo failureInfo = new FailureInfo.Builder()
                .withLineNumber(largeLineNumber)
                .build();

            // Assert
            assertThat(failureInfo.getLineNumber()).isEqualTo(Integer.MAX_VALUE);
        }

        @Test
        @DisplayName("should handle negative line numbers")
        void shouldHandleNegativeLineNumbers() {
            // Arrange
            int negativeLineNumber = -1;

            // Act
            FailureInfo failureInfo = new FailureInfo.Builder()
                .withLineNumber(negativeLineNumber)
                .build();

            // Assert
            assertThat(failureInfo.getLineNumber()).isEqualTo(-1);
        }
    }

    @Nested
    @DisplayName("Comprehensive Failure Info")
    class ComprehensiveFailureInfo {

        @Test
        @DisplayName("should create comprehensive failure info with all fields")
        void shouldCreateComprehensiveFailureInfoWithAllFields() {
            // Arrange
            StepDefinitionInfo stepDefInfo = new StepDefinitionInfo.Builder()
                .withMethodName("clickLoginButton")
                .withClassName("LoginStepDefinitions")
                .withPackageName("com.example.steps")
                .withSourceFilePath("LoginStepDefinitions.java")
                .withLineNumber(25)
                .withStepPattern("^I click the (.*?) button$")
                .withParameters(Arrays.asList("buttonName"))
                .withMethodText("@When(\"^I click the (.*?) button$\") public void clickLoginButton(String buttonName) { ... }")
                .build();

            GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
                .withScenarioName("User login with valid credentials")
                .withFeatureName("User Authentication")
                .withSourceFilePath("login.feature")
                .withLineNumber(5)
                .withSteps(Arrays.asList("Given I am on the login page", "When I click the login button", "Then I should be logged in"))
                .withTags(Arrays.asList("@smoke", "@login"))
                .withBackgroundSteps(Arrays.asList("Given the application is running"))
                .withDataTable(Arrays.asList("| username | password |", "| user1    | pass1    |"))
                .withIsScenarioOutline(false)
                .build();

            // Act
            FailureInfo failureInfo = new FailureInfo.Builder()
                .withScenarioName("User login with valid credentials")
                .withFailedStepText("I click the login button")
                .withStackTrace("java.lang.AssertionError: Button not found\nat com.example.steps.LoginStepDefinitions.clickLoginButton(LoginStepDefinitions.java:25)")
                .withSourceFilePath("src/test/java/com/example/steps/LoginStepDefinitions.java")
                .withLineNumber(25)
                .withStepDefinitionInfo(stepDefInfo)
                .withGherkinScenarioInfo(scenarioInfo)
                .withExpectedValue("true")
                .withActualValue("false")
                .withErrorMessage("Expected true but was false")
                .withParsingTime(150)
                .build();

            // Assert
            assertThat(failureInfo).isNotNull();
            assertThat(failureInfo.getScenarioName()).isEqualTo("User login with valid credentials");
            assertThat(failureInfo.getFailedStepText()).isEqualTo("I click the login button");
            assertThat(failureInfo.getStackTrace()).contains("java.lang.AssertionError: Button not found");
            assertThat(failureInfo.getSourceFilePath()).isEqualTo("src/test/java/com/example/steps/LoginStepDefinitions.java");
            assertThat(failureInfo.getLineNumber()).isEqualTo(25);
            assertThat(failureInfo.getStepDefinitionInfo()).isNotNull();
            assertThat(failureInfo.getStepDefinitionInfo().getMethodName()).isEqualTo("clickLoginButton");
            assertThat(failureInfo.getGherkinScenarioInfo()).isNotNull();
            assertThat(failureInfo.getGherkinScenarioInfo().getScenarioName()).isEqualTo("User login with valid credentials");
            assertThat(failureInfo.getExpectedValue()).isEqualTo("true");
            assertThat(failureInfo.getActualValue()).isEqualTo("false");
            assertThat(failureInfo.getErrorMessage()).isEqualTo("Expected true but was false");
            assertThat(failureInfo.getParsingTime()).isEqualTo(150);
        }
    }
}
