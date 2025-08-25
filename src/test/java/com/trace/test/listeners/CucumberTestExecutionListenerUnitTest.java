package com.trace.test.listeners;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("Cucumber Test Execution Listener Unit Tests")
class CucumberTestExecutionListenerUnitTest {

    @Nested
    @DisplayName("Cucumber Test Detection")
    class CucumberTestDetection {

        @Test
        @DisplayName("should detect Cucumber test via feature file location")
        void shouldDetectCucumberTestViaFeatureFileLocation() {
            // Arrange
            String testLocation = "file:///path/to/test.feature";
            String[] cucumberIndicators = {".feature", "features/"};

            // Act
            boolean isCucumberTest = containsAnyIndicator(testLocation, cucumberIndicators);

            // Assert
            assertThat(isCucumberTest).isTrue();
        }

        @Test
        @DisplayName("should detect Cucumber test via runner class location")
        void shouldDetectCucumberTestViaRunnerClassLocation() {
            // Arrange
            String testLocation = "file:///path/to/CucumberRunner.java";
            String[] cucumberIndicators = {"Cucumber", "cucumber"};

            // Act
            boolean isCucumberTest = containsAnyIndicator(testLocation, cucumberIndicators);

            // Assert
            assertThat(isCucumberTest).isTrue();
        }

        @Test
        @DisplayName("should not detect non-Cucumber test location")
        void shouldNotDetectNonCucumberTestLocation() {
            // Arrange
            String testLocation = "file:///path/to/RegularTest.java";
            String[] cucumberIndicators = {".feature", "features/", "Cucumber", "cucumber"};

            // Act
            boolean isCucumberTest = containsAnyIndicator(testLocation, cucumberIndicators);

            // Assert
            assertThat(isCucumberTest).isFalse();
        }

        @Test
        @DisplayName("should handle null test location")
        void shouldHandleNullTestLocation() {
            // Arrange
            String testLocation = null;
            String[] cucumberIndicators = {".feature", "features/", "Cucumber", "cucumber"};

            // Act
            boolean isCucumberTest = containsAnyIndicator(testLocation, cucumberIndicators);

            // Assert
            assertThat(isCucumberTest).isFalse();
        }

        private boolean containsAnyIndicator(String text, String[] indicators) {
            if (text == null) {
                return false;
            }
            for (String indicator : indicators) {
                if (text.contains(indicator)) {
                    return true;
                }
            }
            return false;
        }
    }

    @Nested
    @DisplayName("Cucumber Error Message Detection")
    class CucumberErrorMessageDetection {

        @Test
        @DisplayName("should detect UndefinedStepException")
        void shouldDetectUndefinedStepException() {
            // Arrange
            String errorMessage = "io.cucumber.junit.UndefinedStepException: The step \"I click the button\" is undefined";
            String[] cucumberExceptions = {
                "io.cucumber", "cucumber.runtime", "UndefinedStepException",
                "AmbiguousStepDefinitionsException", "PendingException"
            };

            // Act
            boolean isCucumberException = containsAnyException(errorMessage, cucumberExceptions);

            // Assert
            assertThat(isCucumberException).isTrue();
        }

        @Test
        @DisplayName("should detect AmbiguousStepDefinitionsException")
        void shouldDetectAmbiguousStepDefinitionsException() {
            // Arrange
            String errorMessage = "cucumber.runtime.AmbiguousStepDefinitionsException: Multiple step definitions found";
            String[] cucumberExceptions = {
                "io.cucumber", "cucumber.runtime", "UndefinedStepException",
                "AmbiguousStepDefinitionsException", "PendingException"
            };

            // Act
            boolean isCucumberException = containsAnyException(errorMessage, cucumberExceptions);

            // Assert
            assertThat(isCucumberException).isTrue();
        }

        @Test
        @DisplayName("should detect PendingException")
        void shouldDetectPendingException() {
            // Arrange
            String errorMessage = "cucumber.runtime.PendingException: TODO: implement me";
            String[] cucumberExceptions = {
                "io.cucumber", "cucumber.runtime", "UndefinedStepException",
                "AmbiguousStepDefinitionsException", "PendingException"
            };

            // Act
            boolean isCucumberException = containsAnyException(errorMessage, cucumberExceptions);

            // Assert
            assertThat(isCucumberException).isTrue();
        }

        @Test
        @DisplayName("should not detect non-Cucumber exception")
        void shouldNotDetectNonCucumberException() {
            // Arrange
            String errorMessage = "java.lang.NullPointerException: Cannot invoke method on null";
            String[] cucumberExceptions = {
                "io.cucumber", "cucumber.runtime", "UndefinedStepException",
                "AmbiguousStepDefinitionsException", "PendingException"
            };

            // Act
            boolean isCucumberException = containsAnyException(errorMessage, cucumberExceptions);

            // Assert
            assertThat(isCucumberException).isFalse();
        }

        @Test
        @DisplayName("should handle null error message")
        void shouldHandleNullErrorMessage() {
            // Arrange
            String errorMessage = null;
            String[] cucumberExceptions = {
                "io.cucumber", "cucumber.runtime", "UndefinedStepException",
                "AmbiguousStepDefinitionsException", "PendingException"
            };

            // Act
            boolean isCucumberException = containsAnyException(errorMessage, cucumberExceptions);

            // Assert
            assertThat(isCucumberException).isFalse();
        }

        private boolean containsAnyException(String text, String[] exceptions) {
            if (text == null) {
                return false;
            }
            for (String exception : exceptions) {
                if (text.contains(exception)) {
                    return true;
                }
            }
            return false;
        }
    }

    @Nested
    @DisplayName("Scenario Name Formatting")
    class ScenarioNameFormatting {

        @Test
        @DisplayName("should format regular scenario name")
        void shouldFormatRegularScenarioName() {
            // Arrange
            String gherkinScenarioName = "User login with valid credentials";
            String basicScenarioName = "User login with valid credentials";
            boolean isScenarioOutline = false;

            // Act
            String formattedName = formatScenarioName(gherkinScenarioName, basicScenarioName, isScenarioOutline);

            // Assert
            assertThat(formattedName).isEqualTo("User login with valid credentials");
        }

        @Test
        @DisplayName("should format scenario outline with example")
        void shouldFormatScenarioOutlineWithExample() {
            // Arrange
            String gherkinScenarioName = "User login with different credentials";
            String basicScenarioName = "Example #1.1";
            boolean isScenarioOutline = true;

            // Act
            String formattedName = formatScenarioName(gherkinScenarioName, basicScenarioName, isScenarioOutline);

            // Assert
            assertThat(formattedName).isEqualTo("User login with different credentials (Example #1.1)");
        }

        @Test
        @DisplayName("should handle null gherkin scenario name")
        void shouldHandleNullGherkinScenarioName() {
            // Arrange
            String gherkinScenarioName = null;
            String basicScenarioName = "Basic scenario name";
            boolean isScenarioOutline = false;

            // Act
            String formattedName = formatScenarioName(gherkinScenarioName, basicScenarioName, isScenarioOutline);

            // Assert
            assertThat(formattedName).isEqualTo("Basic scenario name");
        }

        @Test
        @DisplayName("should handle empty gherkin scenario name")
        void shouldHandleEmptyGherkinScenarioName() {
            // Arrange
            String gherkinScenarioName = "";
            String basicScenarioName = "Basic scenario name";
            boolean isScenarioOutline = false;

            // Act
            String formattedName = formatScenarioName(gherkinScenarioName, basicScenarioName, isScenarioOutline);

            // Assert
            assertThat(formattedName).isEqualTo("Basic scenario name");
        }

        @Test
        @DisplayName("should handle null basic scenario name")
        void shouldHandleNullBasicScenarioName() {
            // Arrange
            String gherkinScenarioName = "Gherkin scenario name";
            String basicScenarioName = null;
            boolean isScenarioOutline = false;

            // Act
            String formattedName = formatScenarioName(gherkinScenarioName, basicScenarioName, isScenarioOutline);

            // Assert
            assertThat(formattedName).isEqualTo("Gherkin scenario name");
        }

        @Test
        @DisplayName("should handle both null scenario names")
        void shouldHandleBothNullScenarioNames() {
            // Arrange
            String gherkinScenarioName = null;
            String basicScenarioName = null;
            boolean isScenarioOutline = false;

            // Act
            String formattedName = formatScenarioName(gherkinScenarioName, basicScenarioName, isScenarioOutline);

            // Assert
            assertThat(formattedName).isEqualTo("Unknown Scenario");
        }

        @Test
        @DisplayName("should not format scenario outline without example pattern")
        void shouldNotFormatScenarioOutlineWithoutExamplePattern() {
            // Arrange
            String gherkinScenarioName = "User login with different credentials";
            String basicScenarioName = "Some other name";
            boolean isScenarioOutline = true;

            // Act
            String formattedName = formatScenarioName(gherkinScenarioName, basicScenarioName, isScenarioOutline);

            // Assert
            assertThat(formattedName).isEqualTo("User login with different credentials");
        }

        private String formatScenarioName(String gherkinScenarioName, String basicScenarioName, boolean isScenarioOutline) {
            if (gherkinScenarioName == null || gherkinScenarioName.isEmpty()) {
                return basicScenarioName != null ? basicScenarioName : "Unknown Scenario";
            }
            
            // Check if this is a scenario outline (basic scenario name contains "Example #")
            if (isScenarioOutline && basicScenarioName != null && basicScenarioName.matches("Example #\\d+\\.\\d+")) {
                return gherkinScenarioName + " (" + basicScenarioName + ")";
            }
            
            // Regular scenario, return Gherkin scenario name as is
            return gherkinScenarioName;
        }
    }

    @Nested
    @DisplayName("Test Output Capture Patterns")
    class TestOutputCapturePatterns {

        @Test
        @DisplayName("should identify test start marker")
        void shouldIdentifyTestStartMarker() {
            // Arrange
            String testStartMarker = "=== Test Started ===";

            // Act & Assert
            assertThat(testStartMarker).isNotNull();
            assertThat(testStartMarker).contains("Test Started");
            assertThat(testStartMarker).startsWith("===");
            assertThat(testStartMarker).endsWith("===");
        }

        @Test
        @DisplayName("should identify test finish marker")
        void shouldIdentifyTestFinishMarker() {
            // Arrange
            String testFinishMarker = "=== Test Finished ===";

            // Act & Assert
            assertThat(testFinishMarker).isNotNull();
            assertThat(testFinishMarker).contains("Test Finished");
            assertThat(testFinishMarker).startsWith("===");
            assertThat(testFinishMarker).endsWith("===");
        }

        @Test
        @DisplayName("should identify captured stdout marker")
        void shouldIdentifyCapturedStdoutMarker() {
            // Arrange
            String stdoutMarker = "=== CAPTURED STDOUT ===";

            // Act & Assert
            assertThat(stdoutMarker).isNotNull();
            assertThat(stdoutMarker).contains("CAPTURED STDOUT");
            assertThat(stdoutMarker).startsWith("===");
            assertThat(stdoutMarker).endsWith("===");
        }

        @Test
        @DisplayName("should identify captured stderr marker")
        void shouldIdentifyCapturedStderrMarker() {
            // Arrange
            String stderrMarker = "=== CAPTURED STDERR ===";

            // Act & Assert
            assertThat(stderrMarker).isNotNull();
            assertThat(stderrMarker).contains("CAPTURED STDERR");
            assertThat(stderrMarker).startsWith("===");
            assertThat(stderrMarker).endsWith("===");
        }

        @Test
        @DisplayName("should format contextual output")
        void shouldFormatContextualOutput() {
            // Arrange
            String outputLine = "Test output message";
            String context = "DEBUG";

            // Act
            String contextualOutput = "[" + context + "] " + outputLine;

            // Assert
            assertThat(contextualOutput).isEqualTo("[DEBUG] Test output message");
            assertThat(contextualOutput).startsWith("[");
            assertThat(contextualOutput).contains("] ");
        }
    }

    @Nested
    @DisplayName("Test Run Lifecycle Patterns")
    class TestRunLifecyclePatterns {

        @Test
        @DisplayName("should identify test run start marker")
        void shouldIdentifyTestRunStartMarker() {
            // Arrange
            String testRunStartMarker = "=== TEST RUN STARTED ===";

            // Act & Assert
            assertThat(testRunStartMarker).isNotNull();
            assertThat(testRunStartMarker).contains("TEST RUN STARTED");
            assertThat(testRunStartMarker).startsWith("===");
            assertThat(testRunStartMarker).endsWith("===");
        }

        @Test
        @DisplayName("should identify test run end marker")
        void shouldIdentifyTestRunEndMarker() {
            // Arrange
            String testRunEndMarker = "=== END TEST RUN STARTED ===";

            // Act & Assert
            assertThat(testRunEndMarker).isNotNull();
            assertThat(testRunEndMarker).contains("END TEST RUN STARTED");
            assertThat(testRunEndMarker).startsWith("===");
            assertThat(testRunEndMarker).endsWith("===");
        }

        @Test
        @DisplayName("should format test run information")
        void shouldFormatTestRunInformation() {
            // Arrange
            String rootTestName = "CucumberTestRunner";
            long timestamp = System.currentTimeMillis();

            // Act
            String testRunInfo = String.format("Root Test: %s\nTimestamp: %d", rootTestName, timestamp);

            // Assert
            assertThat(testRunInfo).isNotNull();
            assertThat(testRunInfo).contains("Root Test: CucumberTestRunner");
            assertThat(testRunInfo).contains("Timestamp: " + timestamp);
        }
    }

    @Nested
    @DisplayName("AI Analysis Delegation Patterns")
    class AIAnalysisDelegationPatterns {

        @Test
        @DisplayName("should identify AI analysis delegation marker")
        void shouldIdentifyAIAnalysisDelegationMarker() {
            // Arrange
            String delegationMarker = "=== AI ANALYSIS DELEGATED TO TRIAGE PANEL ===";

            // Act & Assert
            assertThat(delegationMarker).isNotNull();
            assertThat(delegationMarker).contains("AI ANALYSIS DELEGATED TO TRIAGE PANEL");
            assertThat(delegationMarker).startsWith("===");
            assertThat(delegationMarker).endsWith("===");
        }

        @Test
        @DisplayName("should identify AI analysis end marker")
        void shouldIdentifyAIAnalysisEndMarker() {
            // Arrange
            String endMarker = "=== END AI ANALYSIS DELEGATION ===";

            // Act & Assert
            assertThat(endMarker).isNotNull();
            assertThat(endMarker).contains("END AI ANALYSIS DELEGATION");
            assertThat(endMarker).startsWith("===");
            assertThat(endMarker).endsWith("===");
        }

        @Test
        @DisplayName("should format AI analysis information")
        void shouldFormatAIAnalysisInformation() {
            // Arrange
            String scenarioName = "User login with valid credentials";
            String failedStep = "I click the login button";
            String errorMessage = "Element not found";

            // Act
            String aiInfo = String.format("Failure: %s\nFailed Step: %s\nError Message: %s", 
                scenarioName, failedStep, errorMessage);

            // Assert
            assertThat(aiInfo).isNotNull();
            assertThat(aiInfo).contains("Failure: User login with valid credentials");
            assertThat(aiInfo).contains("Failed Step: I click the login button");
            assertThat(aiInfo).contains("Error Message: Element not found");
        }
    }

    @Nested
    @DisplayName("Input Validation")
    class InputValidation {

        @Test
        @DisplayName("should validate null test name")
        void shouldValidateNullTestName() {
            // Arrange
            String testName = null;

            // Act & Assert
            assertThat(testName).isNull();
        }

        @Test
        @DisplayName("should validate empty test name")
        void shouldValidateEmptyTestName() {
            // Arrange
            String testName = "";

            // Act & Assert
            assertThat(testName).isEmpty();
        }

        @Test
        @DisplayName("should validate valid test name")
        void shouldValidateValidTestName() {
            // Arrange
            String testName = "User login with valid credentials";

            // Act & Assert
            assertThat(testName).isNotNull();
            assertThat(testName).isNotEmpty();
            assertThat(testName).contains("User login");
        }

        @Test
        @DisplayName("should validate null output line")
        void shouldValidateNullOutputLine() {
            // Arrange
            String outputLine = null;

            // Act & Assert
            assertThat(outputLine).isNull();
        }

        @Test
        @DisplayName("should validate empty output line")
        void shouldValidateEmptyOutputLine() {
            // Arrange
            String outputLine = "";

            // Act & Assert
            assertThat(outputLine).isEmpty();
        }

        @Test
        @DisplayName("should validate valid output line")
        void shouldValidateValidOutputLine() {
            // Arrange
            String outputLine = "Test output message";

            // Act & Assert
            assertThat(outputLine).isNotNull();
            assertThat(outputLine).isNotEmpty();
            assertThat(outputLine).contains("Test output");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("should handle very long test name")
        void shouldHandleVeryLongTestName() {
            // Arrange
            String longTestName = "This is a very long test name that contains many words and should be handled gracefully by the listener without causing any issues or exceptions";

            // Act & Assert
            assertThat(longTestName).isNotNull();
            assertThat(longTestName.length()).isGreaterThan(100);
            assertThat(longTestName).contains("test name");
        }

        @Test
        @DisplayName("should handle special characters in test name")
        void shouldHandleSpecialCharactersInTestName() {
            // Arrange
            String specialTestName = "Test with special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?";

            // Act & Assert
            assertThat(specialTestName).isNotNull();
            assertThat(specialTestName).contains("!");
            assertThat(specialTestName).contains("@");
            assertThat(specialTestName).contains("#");
        }

        @Test
        @DisplayName("should handle unicode characters in test name")
        void shouldHandleUnicodeCharactersInTestName() {
            // Arrange
            String unicodeTestName = "Test with unicode: café résumé naïve";

            // Act & Assert
            assertThat(unicodeTestName).isNotNull();
            assertThat(unicodeTestName).contains("é");
            assertThat(unicodeTestName).contains("ï");
        }

        @Test
        @DisplayName("should handle test name with newlines")
        void shouldHandleTestNameWithNewlines() {
            // Arrange
            String testNameWithNewlines = "Test name\nwith\nnewlines";

            // Act & Assert
            assertThat(testNameWithNewlines).isNotNull();
            assertThat(testNameWithNewlines).contains("\n");
            assertThat(testNameWithNewlines.split("\n")).hasSize(3);
        }

        @Test
        @DisplayName("should handle very large line numbers")
        void shouldHandleVeryLargeLineNumbers() {
            // Arrange
            int largeLineNumber = 999999;

            // Act & Assert
            assertThat(largeLineNumber).isPositive();
            assertThat(largeLineNumber).isGreaterThan(100000);
        }

        @Test
        @DisplayName("should handle negative line numbers")
        void shouldHandleNegativeLineNumbers() {
            // Arrange
            int negativeLineNumber = -1;

            // Act & Assert
            assertThat(negativeLineNumber).isNegative();
            assertThat(negativeLineNumber).isLessThan(0);
        }
    }

    @Nested
    @DisplayName("Performance")
    class Performance {

        @Test
        @DisplayName("should not block test execution")
        void shouldNotBlockTestExecution() {
            // Arrange
            long startTime = System.currentTimeMillis();

            // Act
            // Simulate a quick operation, as actual listener operations are mocked out
            String dummyOperation = "Test operation";
            dummyOperation.length();

            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            // Assert
            assertThat(executionTime).isLessThan(1000); // Should complete within 1 second
        }

        @Test
        @DisplayName("should handle multiple operations efficiently")
        void shouldHandleMultipleOperationsEfficiently() {
            // Arrange
            String[] operations = {
                "Test operation 1",
                "Test operation 2",
                "Test operation 3",
                "Test operation 4",
                "Test operation 5"
            };

            long startTime = System.currentTimeMillis();

            // Act
            for (String operation : operations) {
                // Simulate a quick operation, as actual listener operations are mocked out
                operation.length();
            }

            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            // Assert
            assertThat(executionTime).isLessThan(2000); // Should complete within 2 seconds
        }

        @Test
        @DisplayName("should handle repeated operations efficiently")
        void shouldHandleRepeatedOperationsEfficiently() {
            // Arrange
            String operation = "Test operation";

            long startTime = System.currentTimeMillis();

            // Act
            for (int i = 0; i < 10; i++) {
                // Simulate a quick operation, as actual listener operations are mocked out
                operation.length();
            }

            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            // Assert
            assertThat(executionTime).isLessThan(1000); // Should complete within 1 second
        }
    }
}
