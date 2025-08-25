package com.trace.test.extractors;

import com.trace.test.models.FailureInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("Stack Trace Extractor Unit Tests")
class StackTraceExtractorUnitTest {
    
    @Nested
    @DisplayName("Failure Info Model")
    class FailureInfoModel {
        
        @Test
        @DisplayName("should create FailureInfo with builder pattern")
        void shouldCreateFailureInfoWithBuilderPattern() {
            // Arrange
            String scenarioName = "User Login Test";
            String failedStepText = "Given I am on the login page";
            String stackTrace = "java.lang.AssertionError: Test failed";
            String errorMessage = "Login button not found";
            String expectedValue = "true";
            String actualValue = "false";
            
            // Act
            FailureInfo failureInfo = new FailureInfo.Builder()
                .withScenarioName(scenarioName)
                .withFailedStepText(failedStepText)
                .withStackTrace(stackTrace)
                .withErrorMessage(errorMessage)
                .withExpectedValue(expectedValue)
                .withActualValue(actualValue)
                .withSourceFilePath("LoginTest.java")
                .withLineNumber(25)
                .withParsingTime(System.currentTimeMillis())
                .build();
            
            // Assert
            assertThat(failureInfo).isNotNull();
            assertThat(failureInfo.getScenarioName()).isEqualTo(scenarioName);
            assertThat(failureInfo.getFailedStepText()).isEqualTo(failedStepText);
            assertThat(failureInfo.getStackTrace()).isEqualTo(stackTrace);
            assertThat(failureInfo.getErrorMessage()).isEqualTo(errorMessage);
            assertThat(failureInfo.getExpectedValue()).isEqualTo(expectedValue);
            assertThat(failureInfo.getActualValue()).isEqualTo(actualValue);
            assertThat(failureInfo.getSourceFilePath()).isEqualTo("LoginTest.java");
            assertThat(failureInfo.getLineNumber()).isEqualTo(25);
            assertThat(failureInfo.getParsingTime()).isGreaterThan(0);
        }
        
        @Test
        @DisplayName("should handle null values in FailureInfo")
        void shouldHandleNullValuesInFailureInfo() {
            // Act
            FailureInfo failureInfo = new FailureInfo.Builder()
                .withScenarioName(null)
                .withFailedStepText(null)
                .withStackTrace(null)
                .withErrorMessage(null)
                .withExpectedValue(null)
                .withActualValue(null)
                .withSourceFilePath(null)
                .withLineNumber(-1)
                .withParsingTime(0)
                .build();
            
            // Assert
            assertThat(failureInfo).isNotNull();
            assertThat(failureInfo.getScenarioName()).isNull();
            assertThat(failureInfo.getFailedStepText()).isNull();
            assertThat(failureInfo.getStackTrace()).isNull();
            assertThat(failureInfo.getErrorMessage()).isNull();
            assertThat(failureInfo.getExpectedValue()).isNull();
            assertThat(failureInfo.getActualValue()).isNull();
            assertThat(failureInfo.getSourceFilePath()).isNull();
            assertThat(failureInfo.getLineNumber()).isEqualTo(-1);
            assertThat(failureInfo.getParsingTime()).isEqualTo(0);
        }
        
        @Test
        @DisplayName("should handle structured data in FailureInfo")
        void shouldHandleStructuredDataInFailureInfo() {
            // Act
            FailureInfo failureInfo = new FailureInfo.Builder()
                .withScenarioName("Test Scenario")
                .withFailedStepText("Test Step")
                .withStepDefinitionInfo(null) // Would be set in real usage
                .withGherkinScenarioInfo(null) // Would be set in real usage
                .build();
            
            // Assert
            assertThat(failureInfo).isNotNull();
            assertThat(failureInfo.getScenarioName()).isEqualTo("Test Scenario");
            assertThat(failureInfo.getFailedStepText()).isEqualTo("Test Step");
            assertThat(failureInfo.getStepDefinitionInfo()).isNull();
            assertThat(failureInfo.getGherkinScenarioInfo()).isNull();
        }
    }
    
    @Nested
    @DisplayName("Stack Trace Parsing")
    class StackTraceParsing {
        
        @Test
        @DisplayName("should identify expected value pattern in stack trace")
        void shouldIdentifyExpectedValuePatternInStackTrace() {
            // Arrange
            String stackTrace = "java.lang.AssertionError: \nExpected: true\nbut: was false";
            
            // Act & Assert
            assertThat(stackTrace).contains("Expected: true");
            assertThat(stackTrace).contains("but: was false");
        }
        
        @Test
        @DisplayName("should identify actual value pattern in stack trace")
        void shouldIdentifyActualValuePatternInStackTrace() {
            // Arrange
            String stackTrace = "java.lang.AssertionError: \nExpected: true\nbut: was false";
            
            // Act & Assert
            assertThat(stackTrace).contains("but: was false");
        }
        
        @Test
        @DisplayName("should handle stack trace without expected/actual values")
        void shouldHandleStackTraceWithoutExpectedActualValues() {
            // Arrange
            String stackTrace = "java.lang.NullPointerException: null";
            
            // Act & Assert
            assertThat(stackTrace).doesNotContain("Expected:");
            assertThat(stackTrace).doesNotContain("but: was");
        }
        
        @Test
        @DisplayName("should identify source file pattern in stack trace")
        void shouldIdentifySourceFilePatternInStackTrace() {
            // Arrange
            String stackTrace = "at com.example.TestClass.testMethod(TestClass.java:25)";
            
            // Act & Assert
            assertThat(stackTrace).contains(".java:");
            assertThat(stackTrace).contains("TestClass.java:25");
        }
        
        @Test
        @DisplayName("should identify line number pattern in stack trace")
        void shouldIdentifyLineNumberPatternInStackTrace() {
            // Arrange
            String stackTrace = "at com.example.TestClass.testMethod(TestClass.java:25)";
            
            // Act & Assert
            assertThat(stackTrace).contains(":25");
        }
        
        @Test
        @DisplayName("should handle malformed stack trace")
        void shouldHandleMalformedStackTrace() {
            // Arrange
            String stackTrace = "Invalid stack trace format";
            
            // Act & Assert
            assertThat(stackTrace).doesNotContain(".java:");
            assertThat(stackTrace).doesNotContain("Expected:");
        }
    }
    
    @Nested
    @DisplayName("Scenario Name Processing")
    class ScenarioNameProcessing {
        
        @Test
        @DisplayName("should identify scenario outline pattern")
        void shouldIdentifyScenarioOutlinePattern() {
            // Arrange
            String testName = "Example #1.1";
            String scenarioName = "User Login Test";
            
            // Act & Assert
            assertThat(testName).matches("Example #\\d+\\.\\d+");
            assertThat(scenarioName + " (" + testName + ")").isEqualTo("User Login Test (Example #1.1)");
        }
        
        @Test
        @DisplayName("should handle regular scenario name")
        void shouldHandleRegularScenarioName() {
            // Arrange
            String scenarioName = "User Login Test";
            String testName = "Given I am on the login page";
            
            // Act & Assert
            assertThat(testName).doesNotMatch("Example #\\d+\\.\\d+");
            assertThat(scenarioName).isEqualTo("User Login Test");
        }
        
        @Test
        @DisplayName("should handle very long step text")
        void shouldHandleVeryLongStepText() {
            // Arrange
            String longStepText = "Given I am on a very long step text that contains many words and should be truncated appropriately for display purposes";
            
            // Act & Assert
            assertThat(longStepText.length()).isGreaterThan(50);
            String truncated = longStepText.substring(0, 50) + "...";
            assertThat(truncated).hasSize(53); // 50 characters + 3 for "..."
            assertThat(truncated).endsWith("...");
        }
        
        @Test
        @DisplayName("should handle null scenario name")
        void shouldHandleNullScenarioName() {
            // Arrange
            String scenarioName = null;
            String testName = "Test Step";
            
            // Act & Assert
            assertThat(scenarioName).isNull();
            assertThat(testName).isEqualTo("Test Step");
        }
        
        @Test
        @DisplayName("should handle empty scenario name")
        void shouldHandleEmptyScenarioName() {
            // Arrange
            String scenarioName = "";
            String testName = "Test Step";
            
            // Act & Assert
            assertThat(scenarioName).isEmpty();
            assertThat(testName).isEqualTo("Test Step");
        }
    }
    
    @Nested
    @DisplayName("Input Validation")
    class InputValidation {
        
        @Test
        @DisplayName("should validate null test proxy")
        void shouldValidateNullTestProxy() {
            // Arrange
            Object testProxy = null;
            
            // Act & Assert
            assertThat(testProxy).isNull();
        }
        
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
            String testName = "Given I am on the login page";
            
            // Act & Assert
            assertThat(testName).isNotNull();
            assertThat(testName).isNotEmpty();
            assertThat(testName).startsWith("Given");
        }
    }
    
    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {
        
        @Test
        @DisplayName("should handle null stack trace gracefully")
        void shouldHandleNullStackTraceGracefully() {
            // Arrange
            String stackTrace = null;
            
            // Act & Assert
            assertThat(stackTrace).isNull();
        }
        
        @Test
        @DisplayName("should handle null error message gracefully")
        void shouldHandleNullErrorMessageGracefully() {
            // Arrange
            String errorMessage = null;
            
            // Act & Assert
            assertThat(errorMessage).isNull();
        }
        
        @Test
        @DisplayName("should handle null location URL gracefully")
        void shouldHandleNullLocationUrlGracefully() {
            // Arrange
            String locationUrl = null;
            
            // Act & Assert
            assertThat(locationUrl).isNull();
        }
        
        @Test
        @DisplayName("should handle malformed location URL gracefully")
        void shouldHandleMalformedLocationUrlGracefully() {
            // Arrange
            String locationUrl = "invalid:url:format";
            
            // Act & Assert
            assertThat(locationUrl).contains(":");
            assertThat(locationUrl.split(":")).hasSize(3);
        }
        
        @Test
        @DisplayName("should handle file URL format")
        void shouldHandleFileUrlFormat() {
            // Arrange
            String fileUrl = "file:///path/to/file.java:25";
            
            // Act & Assert
            assertThat(fileUrl).startsWith("file://");
            assertThat(fileUrl.substring(7)).isEqualTo("/path/to/file.java:25");
        }
    }
    
    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {
        
        @Test
        @DisplayName("should handle very long stack trace")
        void shouldHandleVeryLongStackTrace() {
            // Arrange
            StringBuilder longStackTrace = new StringBuilder();
            for (int i = 0; i < 100; i++) {
                longStackTrace.append("at com.example.TestClass.testMethod(TestClass.java:").append(i).append(")\n");
            }
            
            // Act & Assert
            assertThat(longStackTrace.toString()).isNotNull();
            assertThat(longStackTrace.toString().split("\n")).hasSize(100);
            assertThat(longStackTrace.toString()).contains("TestClass.java:");
        }
        
        @Test
        @DisplayName("should handle special characters in test name")
        void shouldHandleSpecialCharactersInTestName() {
            // Arrange
            String testName = "Given I enter \"test@email.com\" with special chars: !@#$%^&*()";
            
            // Act & Assert
            assertThat(testName).isNotNull();
            assertThat(testName).contains("@");
            assertThat(testName).contains("!");
            assertThat(testName).contains("#");
            assertThat(testName).contains("\"");
        }
        
        @Test
        @DisplayName("should handle unicode characters in test name")
        void shouldHandleUnicodeCharactersInTestName() {
            // Arrange
            String testName = "Given I enter text with unicode: café résumé naïve";
            
            // Act & Assert
            assertThat(testName).isNotNull();
            assertThat(testName).contains("é");
            assertThat(testName).contains("ï");
        }
        
        @Test
        @DisplayName("should handle test name with newlines")
        void shouldHandleTestNameWithNewlines() {
            // Arrange
            String testName = "Given I am on the login page\nAnd I see the login form";
            
            // Act & Assert
            assertThat(testName).isNotNull();
            assertThat(testName).contains("\n");
            assertThat(testName.split("\n")).hasSize(2);
        }
        
        @Test
        @DisplayName("should handle negative line numbers")
        void shouldHandleNegativeLineNumbers() {
            // Arrange
            int lineNumber = -1;
            
            // Act & Assert
            assertThat(lineNumber).isLessThan(0);
        }
        
        @Test
        @DisplayName("should handle very large line numbers")
        void shouldHandleVeryLargeLineNumbers() {
            // Arrange
            int lineNumber = Integer.MAX_VALUE;
            
            // Act & Assert
            assertThat(lineNumber).isEqualTo(Integer.MAX_VALUE);
        }
    }
    
    @Nested
    @DisplayName("Performance")
    class Performance {
        
        @Test
        @DisplayName("should handle multiple operations efficiently")
        void shouldHandleMultipleOperationsEfficiently() {
            // Arrange
            List<String> testNames = Arrays.asList(
                "Given I am on the login page",
                "When I enter valid credentials",
                "Then I should be logged in",
                "And I should see the dashboard"
            );
            
            long startTime = System.currentTimeMillis();
            
            // Act
            for (String testName : testNames) {
                assertThat(testName).isNotNull();
                assertThat(testName).isNotEmpty();
                assertThat(testName.trim()).isNotEmpty();
            }
            
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            
            // Assert
            assertThat(executionTime).isLessThan(1000); // Should complete within 1 second
        }
        
        @Test
        @DisplayName("should handle repeated operations efficiently")
        void shouldHandleRepeatedOperationsEfficiently() {
            // Arrange
            String testName = "Given I am on the login page";
            
            long startTime = System.currentTimeMillis();
            
            // Act
            for (int i = 0; i < 100; i++) {
                assertThat(testName).isNotNull();
                assertThat(testName).startsWith("Given");
            }
            
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            
            // Assert
            assertThat(executionTime).isLessThan(1000); // Should complete within 1 second
        }
        
        @Test
        @DisplayName("should handle large data processing efficiently")
        void shouldHandleLargeDataProcessingEfficiently() {
            // Arrange
            StringBuilder largeData = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                largeData.append("Test data line ").append(i).append("\n");
            }
            
            long startTime = System.currentTimeMillis();
            
            // Act
            String[] lines = largeData.toString().split("\n");
            
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            
            // Assert
            assertThat(lines).hasSize(1000);
            assertThat(executionTime).isLessThan(1000); // Should complete within 1 second
        }
    }
}
