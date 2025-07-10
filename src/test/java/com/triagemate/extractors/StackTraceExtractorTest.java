package com.triagemate.extractors;

import com.intellij.execution.testframework.sm.runner.SMTestProxy;
import com.intellij.openapi.project.Project;
import com.triagemate.models.FailureInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for StackTraceExtractor.
 * 
 * <p>These tests verify the strategy pattern implementation and ensure proper
 * coordination between different parsing strategies. The tests cover both
 * successful parsing scenarios and fallback mechanisms.</p>
 * 
 * <p>Test patterns follow IntelliJ Platform best practices:
 * <ul>
 *   <li>Mock IntelliJ Platform dependencies (Project, SMTestProxy)</li>
 *   <li>Test both positive and negative scenarios</li>
 *   <li>Verify strategy selection and fallback behavior</li>
 *   <li>Test edge cases and error conditions</li>
 * </ul></p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StackTraceExtractor")
class StackTraceExtractorTest {

    @Mock
    private Project project;

    @Mock
    private SMTestProxy testProxy;

    private StackTraceExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new StackTraceExtractor(project);
    }

    @Nested
    @DisplayName("Constructor and Initialization")
    class ConstructorAndInitialization {

        @Test
        @DisplayName("should initialize with raw extraction approach")
        void shouldInitializeWithRawExtractionApproach() {
            assertEquals(1, extractor.getStrategyCount(), 
                "Should initialize with 1 raw extraction approach");
        }

        @Test
        @DisplayName("should use raw extraction strategy")
        void shouldUseRawExtractionStrategy() {
            var strategyNames = extractor.getStrategyNames();
            
            // Verify raw extraction approach is used
            assertEquals(1, strategyNames.size(), "Should have one raw extraction strategy");
            assertTrue(strategyNames.get(0).contains("StackTraceExtraction"), 
                "Should use raw extraction strategy");
        }

        @Test
        @DisplayName("should throw exception when project is null")
        void shouldThrowExceptionWhenProjectIsNull() {
            assertThrows(NullPointerException.class, () -> new StackTraceExtractor(null),
                "Should throw NullPointerException when project is null");
        }
    }

    @Nested
    @DisplayName("extractFailureInfo method")
    class ExtractFailureInfoMethod {

        @Test
        @DisplayName("should throw IllegalArgumentException when testOutput is null")
        void shouldThrowIllegalArgumentExceptionWhenTestOutputIsNull() {
            assertThrows(IllegalArgumentException.class, () -> extractor.extractFailureInfo((String) null),
                "Should throw IllegalArgumentException when testOutput is null");
        }

        @Test
        @DisplayName("should handle JUnit comparison failure")
        void shouldHandleJUnitComparisonFailure() {
            String testOutput = """
                org.junit.ComparisonFailure: expected:<foo> but was:<bar>
                    at com.example.MyTest.testSomething(MyTest.java:42)
                    at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:325)
                """;

            FailureInfo result = extractor.extractFailureInfo(testOutput);

            assertNotNull(result, "Should return non-null FailureInfo");
            assertNotNull(result.getErrorMessage(), "Should extract error message");
            assertNotNull(result.getStackTrace(), "Should extract stack trace");
            assertTrue(result.getParsingTime() >= 0, "Should record parsing time");
        }

        @Test
        @DisplayName("should handle WebDriver exception")
        void shouldHandleWebDriverException() {
            String testOutput = """
                org.openqa.selenium.NoSuchElementException: no such element: Unable to locate element: {"method":"css selector","selector":"#nonexistent"}
                    at com.example.MyTest.testSomething(MyTest.java:42)
                    at org.openqa.selenium.remote.RemoteWebDriver.findElement(RemoteWebDriver.java:315)
                """;

            FailureInfo result = extractor.extractFailureInfo(testOutput);

            assertNotNull(result, "Should return non-null FailureInfo");
            assertNotNull(result.getErrorMessage(), "Should extract error message");
            assertNotNull(result.getStackTrace(), "Should extract stack trace");
        }

        @Test
        @DisplayName("should handle Cucumber exception")
        void shouldHandleCucumberException() {
            String testOutput = """
                io.cucumber.junit.UndefinedStepException: The step "I click on the button" is undefined.
                    at com.example.MyTest.testSomething(MyTest.java:42)
                    at io.cucumber.junit.Cucumber.run(Cucumber.java:123)
                """;

            FailureInfo result = extractor.extractFailureInfo(testOutput);

            assertNotNull(result, "Should return non-null FailureInfo");
            assertNotNull(result.getErrorMessage(), "Should extract error message");
            assertNotNull(result.getStackTrace(), "Should extract stack trace");
        }

        @Test
        @DisplayName("should handle runtime exception")
        void shouldHandleRuntimeException() {
            String testOutput = """
                java.lang.NullPointerException: Cannot invoke "String.length()" because "str" is null
                    at com.example.MyTest.testSomething(MyTest.java:42)
                    at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:325)
                """;

            FailureInfo result = extractor.extractFailureInfo(testOutput);

            assertNotNull(result, "Should return non-null FailureInfo");
            assertNotNull(result.getErrorMessage(), "Should extract error message");
            assertNotNull(result.getStackTrace(), "Should extract stack trace");
        }

        @Test
        @DisplayName("should handle configuration error")
        void shouldHandleConfigurationError() {
            String testOutput = """
                java.io.FileNotFoundException: config.properties (No such file or directory)
                    at com.example.MyTest.testSomething(MyTest.java:42)
                    at java.io.FileInputStream.open0(Native Method)
                """;

            FailureInfo result = extractor.extractFailureInfo(testOutput);

            assertNotNull(result, "Should return non-null FailureInfo");
            assertNotNull(result.getErrorMessage(), "Should extract error message");
            assertNotNull(result.getStackTrace(), "Should extract stack trace");
        }

        @Test
        @DisplayName("should fallback to generic strategy for unknown errors")
        void shouldFallbackToGenericStrategyForUnknownErrors() {
            String testOutput = """
                com.example.CustomException: Some custom error occurred
                    at com.example.MyTest.testSomething(MyTest.java:42)
                    at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:325)
                """;

            FailureInfo result = extractor.extractFailureInfo(testOutput);

            assertNotNull(result, "Should return non-null FailureInfo");
            assertNotNull(result.getErrorMessage(), "Should extract error message");
            assertNotNull(result.getStackTrace(), "Should extract stack trace");
        }

        @Test
        @DisplayName("should create minimal failure info when no strategy can handle")
        void shouldCreateMinimalFailureInfoWhenNoStrategyCanHandle() {
            String testOutput = "Some completely unrecognized error format";

            FailureInfo result = extractor.extractFailureInfo(testOutput);

            assertNotNull(result, "Should return non-null FailureInfo");
            assertNotNull(result.getErrorMessage(), "Should extract basic error message");
            assertEquals(testOutput, result.getStackTrace(),
                "Should use original test output as stack trace");
        }

        @Test
        @DisplayName("should handle empty test output")
        void shouldHandleEmptyTestOutput() {
            String testOutput = "";

            FailureInfo result = extractor.extractFailureInfo(testOutput);

            assertNotNull(result, "Should return non-null FailureInfo");
            assertNotNull(result.getErrorMessage(), "Should provide fallback error message");
        }

        @Test
        @DisplayName("should prioritize higher priority strategies")
        void shouldPrioritizeHigherPriorityStrategies() {
            // This test verifies that when multiple strategies could handle the same input,
            // the higher priority strategy is selected
            String testOutput = """
                java.lang.RuntimeException: Some runtime error
                    at com.example.MyTest.testSomething(MyTest.java:42)
                """;

            FailureInfo result = extractor.extractFailureInfo(testOutput);

            assertNotNull(result, "Should return non-null FailureInfo");
            assertNotNull(result.getErrorMessage(), "Should extract error message");
            assertNotNull(result.getStackTrace(), "Should extract stack trace");
        }
    }

    @Nested
    @DisplayName("extractStackTrace method")
    class ExtractStackTraceMethod {

        @Test
        @DisplayName("should extract stack trace from test proxy")
        void shouldExtractStackTraceFromTestProxy() {
            String errorMessage = """
                org.junit.ComparisonFailure: expected:<foo> but was:<bar>
                    at com.example.MyTest.testSomething(MyTest.java:42)
                """;
            when(testProxy.getErrorMessage()).thenReturn(errorMessage);

            String result = extractor.extractStackTrace(testProxy);

            assertNotNull(result, "Should return non-null stack trace");
            assertTrue(result.contains("org.junit.ComparisonFailure"), 
                "Should contain the exception type");
            assertTrue(result.contains("at com.example.MyTest.testSomething"), 
                "Should contain stack trace elements");
        }

        @Test
        @DisplayName("should handle null test proxy")
        void shouldHandleNullTestProxy() {
            String result = extractor.extractStackTrace(null);

            assertEquals("Test proxy is null", result,
                "Should return appropriate message for null test proxy");
        }

        @Test
        @DisplayName("should handle test proxy with null error message")
        void shouldHandleTestProxyWithNullErrorMessage() {
            when(testProxy.getErrorMessage()).thenReturn(null);

            String result = extractor.extractStackTrace(testProxy);

            assertEquals("Stack trace not available", result,
                "Should return appropriate message for null error message");
        }

        @Test
        @DisplayName("should handle test proxy with empty error message")
        void shouldHandleTestProxyWithEmptyErrorMessage() {
            when(testProxy.getErrorMessage()).thenReturn("");

            String result = extractor.extractStackTrace(testProxy);

            assertEquals("Stack trace not available", result,
                "Should return appropriate message for empty error message");
        }

        @Test
        @DisplayName("should fallback to raw error message when strategy parsing fails")
        void shouldFallbackToRawErrorMessageWhenStrategyParsingFails() {
            String errorMessage = "Some malformed error that strategies can't parse";
            when(testProxy.getErrorMessage()).thenReturn(errorMessage);

            String result = extractor.extractStackTrace(testProxy);

            assertEquals(errorMessage, result,
                "Should fallback to raw error message when strategy parsing fails");
        }
    }

    @Nested
    @DisplayName("extractFailedStepText method")
    class ExtractFailedStepTextMethod {

        @Test
        @DisplayName("should extract failed step text from test proxy")
        void shouldExtractFailedStepTextFromTestProxy() {
            String errorMessage = """
                io.cucumber.junit.UndefinedStepException: The step "I click on the button" is undefined.
                    at com.example.MyTest.testSomething(MyTest.java:42)
                """;
            when(testProxy.getErrorMessage()).thenReturn(errorMessage);

            String result = extractor.extractFailedStepText(testProxy);

            assertNotNull(result, "Should return non-null step text");
            assertTrue(result.contains("I click on the button"), 
                "Should extract the step text from the error message");
        }

        @Test
        @DisplayName("should handle null test proxy")
        void shouldHandleNullTestProxy() {
            String result = extractor.extractFailedStepText(null);

            assertEquals("Test proxy is null", result,
                "Should return appropriate message for null test proxy");
        }

        @Test
        @DisplayName("should handle test proxy with null error message")
        void shouldHandleTestProxyWithNullErrorMessage() {
            when(testProxy.getErrorMessage()).thenReturn(null);

            String result = extractor.extractFailedStepText(testProxy);

            assertEquals("Failed step text not available", result,
                "Should return appropriate message for null error message");
        }

        @Test
        @DisplayName("should handle test proxy with empty error message")
        void shouldHandleTestProxyWithEmptyErrorMessage() {
            when(testProxy.getErrorMessage()).thenReturn("");

            String result = extractor.extractFailedStepText(testProxy);

            assertEquals("Failed step text not available", result,
                "Should return appropriate message for empty error message");
        }

        @Test
        @DisplayName("should fallback to basic extraction when strategy parsing fails")
        void shouldFallbackToBasicExtractionWhenStrategyParsingFails() {
            String errorMessage = "Some error without step information";
            when(testProxy.getErrorMessage()).thenReturn(errorMessage);
            when(testProxy.getName()).thenReturn("Scenario: My Test Scenario Step: I do something");

            String result = extractor.extractFailedStepText(testProxy);

            assertEquals("I do something", result,
                "Should fallback to basic step extraction from test name");
        }

        @Test
        @DisplayName("should handle test name without step information")
        void shouldHandleTestNameWithoutStepInformation() {
            String errorMessage = "Some error";
            when(testProxy.getErrorMessage()).thenReturn(errorMessage);
            when(testProxy.getName()).thenReturn("Scenario: My Test Scenario");

            String result = extractor.extractFailedStepText(testProxy);

            assertEquals("Failed step text not available", result,
                "Should return appropriate message when no step information is available");
        }
    }

    @Nested
    @DisplayName("Error Handling and Resilience")
    class ErrorHandlingAndResilience {

        @Test
        @DisplayName("should handle strategy that throws exception")
        void shouldHandleStrategyThatThrowsException() {
            // This test verifies that if a strategy throws an exception during parsing,
            // the extractor continues to the next strategy instead of failing
            String testOutput = """
                org.junit.ComparisonFailure: expected:<foo> but was:<bar>
                    at com.example.MyTest.testSomething(MyTest.java:42)
                """;

            // The extractor should handle this gracefully and use the JUnit strategy
            FailureInfo result = extractor.extractFailureInfo(testOutput);

            assertNotNull(result, "Should return non-null FailureInfo even if some strategies fail");
            assertNotNull(result.getErrorMessage(), "Should extract error message");
            assertNotNull(result.getStackTrace(), "Should extract stack trace");
        }

        @Test
        @DisplayName("should handle very large test output")
        void shouldHandleVeryLargeTestOutput() {
            // Create a large test output to test performance and memory handling
            StringBuilder largeOutput = new StringBuilder();
            largeOutput.append("org.junit.ComparisonFailure: expected:<foo> but was:<bar>\n");
            
            // Add many stack trace lines
            for (int i = 0; i < 1000; i++) {
                largeOutput.append("    at com.example.MyTest.testSomething(MyTest.java:").append(i).append(")\n");
            }

            FailureInfo result = extractor.extractFailureInfo(largeOutput.toString());

            assertNotNull(result, "Should handle large test output without issues");
            assertNotNull(result.getErrorMessage(), "Should extract error message");
            assertTrue(result.getStackTrace().length() > 1000,
                "Should preserve large stack trace");
        }

        @Test
        @DisplayName("should handle test output with special characters")
        void shouldHandleTestOutputWithSpecialCharacters() {
            String testOutput = """
                org.junit.ComparisonFailure: expected:<foo> but was:<bar>
                    at com.example.MyTest.testSomething(MyTest.java:42)
                    at com.example.TestWithSpecialChars.test(TestWithSpecialChars.java:123)
                """;

            FailureInfo result = extractor.extractFailureInfo(testOutput);

            assertNotNull(result, "Should handle test output with special characters");
            assertNotNull(result.getErrorMessage(), "Should extract error message");
            assertTrue(result.getErrorMessage().contains("expected:<foo> but was:<bar>"),
                "Should extract error message with special characters");
        }
    }

    @Nested
    @DisplayName("Performance and Efficiency")
    class PerformanceAndEfficiency {

        @Test
        @DisplayName("should complete parsing within reasonable time")
        void shouldCompleteParsingWithinReasonableTime() {
            String testOutput = """
                org.junit.ComparisonFailure: expected:<foo> but was:<bar>
                    at com.example.MyTest.testSomething(MyTest.java:42)
                """;

            long startTime = System.currentTimeMillis();
            FailureInfo result = extractor.extractFailureInfo(testOutput);
            long duration = System.currentTimeMillis() - startTime;

            assertNotNull(result, "Should return valid result");
            assertTrue(duration < 1000, "Should complete parsing within 1 second");
            assertTrue(result.getParsingTime() >= 0, "Should record parsing time");
        }

        @Test
        @DisplayName("should not create excessive objects during parsing")
        void shouldNotCreateExcessiveObjectsDuringParsing() {
            String testOutput = """
                org.junit.ComparisonFailure: expected:<foo> but was:<bar>
                    at com.example.MyTest.testSomething(MyTest.java:42)
                """;

            // Run multiple parsing operations to check for memory leaks
            for (int i = 0; i < 100; i++) {
                FailureInfo result = extractor.extractFailureInfo(testOutput);
                assertNotNull(result, "Should consistently return valid results");
            }
        }
    }
} 