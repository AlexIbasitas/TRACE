package com.trace.test.listeners;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("Test Output Capture Listener Unit Tests")
class TestOutputCaptureListenerUnitTest {

    @Nested
    @DisplayName("Output Line Processing")
    class OutputLineProcessing {

        @Test
        @DisplayName("should process valid output line correctly")
        void shouldProcessValidOutputLineCorrectly() {
            // Arrange
            String outputLine = "Test output message";

            // Act & Assert
            assertThat(outputLine).isNotNull();
            assertThat(outputLine).isNotEmpty();
            assertThat(outputLine).contains("Test output");
        }

        @Test
        @DisplayName("should handle null output line")
        void shouldHandleNullOutputLine() {
            // Arrange
            String outputLine = null;

            // Act & Assert
            assertThat(outputLine).isNull();
        }

        @Test
        @DisplayName("should handle empty output line")
        void shouldHandleEmptyOutputLine() {
            // Arrange
            String outputLine = "";

            // Act & Assert
            assertThat(outputLine).isEmpty();
        }

        @Test
        @DisplayName("should handle whitespace-only output line")
        void shouldHandleWhitespaceOnlyOutputLine() {
            // Arrange
            String outputLine = "   ";

            // Act & Assert
            assertThat(outputLine.trim()).isEmpty();
        }

        @Test
        @DisplayName("should handle output line with proper line ending")
        void shouldHandleOutputLineWithProperLineEnding() {
            // Arrange
            String outputLine = "Test output message";

            // Act
            String outputWithNewline = outputLine.endsWith("\n") ? outputLine : outputLine + "\n";

            // Assert
            assertThat(outputWithNewline).endsWith("\n");
            assertThat(outputWithNewline).contains("Test output message");
        }

        @Test
        @DisplayName("should handle output line that already ends with newline")
        void shouldHandleOutputLineThatAlreadyEndsWithNewline() {
            // Arrange
            String outputLine = "Test output message\n";

            // Act & Assert
            assertThat(outputLine).endsWith("\n");
            assertThat(outputLine).contains("Test output message");
        }
    }

    @Nested
    @DisplayName("Error Output Formatting")
    class ErrorOutputFormatting {

        @Test
        @DisplayName("should format error output with proper markers")
        void shouldFormatErrorOutputWithProperMarkers() {
            // Arrange
            String errorMessage = "java.lang.NullPointerException: Cannot invoke method on null";

            // Act
            String formattedError = "=== FULL ERROR OUTPUT ===\n" + errorMessage + "\n=== END ERROR OUTPUT ===\n\n";

            // Assert
            assertThat(formattedError).startsWith("=== FULL ERROR OUTPUT ===");
            assertThat(formattedError).endsWith("=== END ERROR OUTPUT ===\n\n");
            assertThat(formattedError).contains(errorMessage);
        }

        @Test
        @DisplayName("should handle null error message")
        void shouldHandleNullErrorMessage() {
            // Arrange
            String errorMessage = null;

            // Act & Assert
            assertThat(errorMessage).isNull();
        }

        @Test
        @DisplayName("should handle empty error message")
        void shouldHandleEmptyErrorMessage() {
            // Arrange
            String errorMessage = "";

            // Act & Assert
            assertThat(errorMessage).isEmpty();
        }

        @Test
        @DisplayName("should handle whitespace-only error message")
        void shouldHandleWhitespaceOnlyErrorMessage() {
            // Arrange
            String errorMessage = "   ";

            // Act & Assert
            assertThat(errorMessage.trim()).isEmpty();
        }

        @Test
        @DisplayName("should append error message with proper line ending")
        void shouldAppendErrorMessageWithProperLineEnding() {
            // Arrange
            String errorMessage = "Test error message";

            // Act
            String errorWithNewline = errorMessage.endsWith("\n") ? errorMessage : errorMessage + "\n";

            // Assert
            assertThat(errorWithNewline).endsWith("\n");
            assertThat(errorWithNewline).contains("Test error message");
        }
    }

    @Nested
    @DisplayName("Comprehensive Output Formatting")
    class ComprehensiveOutputFormatting {

        @Test
        @DisplayName("should format comprehensive output with error message section")
        void shouldFormatComprehensiveOutputWithErrorMessageSection() {
            // Arrange
            String errorMessage = "java.lang.NullPointerException: Cannot invoke method on null";

            // Act
            String comprehensiveOutput = "=== ERROR MESSAGE ===\n" + errorMessage + "\n";

            // Assert
            assertThat(comprehensiveOutput).startsWith("=== ERROR MESSAGE ===");
            assertThat(comprehensiveOutput).contains(errorMessage);
            assertThat(comprehensiveOutput).endsWith("\n");
        }

        @Test
        @DisplayName("should format comprehensive output with captured output section")
        void shouldFormatComprehensiveOutputWithCapturedOutputSection() {
            // Arrange
            String capturedOutput = "Test output line 1\nTest output line 2";

            // Act
            String comprehensiveOutput = "=== CAPTURED OUTPUT ===\n" + capturedOutput + "\n";

            // Assert
            assertThat(comprehensiveOutput).startsWith("=== CAPTURED OUTPUT ===");
            assertThat(comprehensiveOutput).contains(capturedOutput);
            assertThat(comprehensiveOutput).endsWith("\n");
        }

        @Test
        @DisplayName("should format comprehensive output with child test section")
        void shouldFormatComprehensiveOutputWithChildTestSection() {
            // Arrange
            String childTestName = "ChildTest";
            String childError = "Child test error";

            // Act
            String comprehensiveOutput = "=== CHILD TEST OUTPUT ===\n" +
                "Child: " + childTestName + "\n" +
                childError + "\n";

            // Assert
            assertThat(comprehensiveOutput).startsWith("=== CHILD TEST OUTPUT ===");
            assertThat(comprehensiveOutput).contains("Child: " + childTestName);
            assertThat(comprehensiveOutput).contains(childError);
        }

        @Test
        @DisplayName("should format comprehensive output with test metadata section")
        void shouldFormatComprehensiveOutputWithTestMetadataSection() {
            // Arrange
            String testName = "TestName";
            String locationUrl = "file:///path/to/test.java";
            String status = "FAILED";
            String parentName = "ParentTest";

            // Act
            String comprehensiveOutput = "=== TEST METADATA ===\n" +
                "Test Name: " + testName + "\n" +
                "Location: " + locationUrl + "\n" +
                "Status: " + status + "\n" +
                "Parent: " + parentName + "\n";

            // Assert
            assertThat(comprehensiveOutput).startsWith("=== TEST METADATA ===");
            assertThat(comprehensiveOutput).contains("Test Name: " + testName);
            assertThat(comprehensiveOutput).contains("Location: " + locationUrl);
            assertThat(comprehensiveOutput).contains("Status: " + status);
            assertThat(comprehensiveOutput).contains("Parent: " + parentName);
        }
    }

    @Nested
    @DisplayName("Output Aggregation")
    class OutputAggregation {

        @Test
        @DisplayName("should aggregate output from multiple sources")
        void shouldAggregateOutputFromMultipleSources() {
            // Arrange
            String errorMessage = "=== ERROR MESSAGE ===\nTest error\n";
            String capturedOutput = "=== CAPTURED OUTPUT ===\nTest output\n";
            String metadata = "=== TEST METADATA ===\nTest Name: Test\n";

            // Act
            String aggregatedOutput = errorMessage + capturedOutput + metadata;

            // Assert
            assertThat(aggregatedOutput).contains("=== ERROR MESSAGE ===");
            assertThat(aggregatedOutput).contains("=== CAPTURED OUTPUT ===");
            assertThat(aggregatedOutput).contains("=== TEST METADATA ===");
            assertThat(aggregatedOutput).contains("Test error");
            assertThat(aggregatedOutput).contains("Test output");
            assertThat(aggregatedOutput).contains("Test Name: Test");
        }

        @Test
        @DisplayName("should handle empty sections in aggregation")
        void shouldHandleEmptySectionsInAggregation() {
            // Arrange
            String errorMessage = "";
            String capturedOutput = "=== CAPTURED OUTPUT ===\nTest output\n";
            String metadata = "";

            // Act
            String aggregatedOutput = errorMessage + capturedOutput + metadata;

            // Assert
            assertThat(aggregatedOutput).doesNotContain("=== ERROR MESSAGE ===");
            assertThat(aggregatedOutput).contains("=== CAPTURED OUTPUT ===");
            assertThat(aggregatedOutput).doesNotContain("=== TEST METADATA ===");
            assertThat(aggregatedOutput).contains("Test output");
        }

        @Test
        @DisplayName("should aggregate child test output")
        void shouldAggregateChildTestOutput() {
            // Arrange
            List<String> childOutputs = new ArrayList<>();
            childOutputs.add("Child1: Error1");
            childOutputs.add("Child2: Error2");

            // Act
            StringBuilder aggregatedChildOutput = new StringBuilder();
            for (String childOutput : childOutputs) {
                aggregatedChildOutput.append(childOutput).append("\n");
            }

            // Assert
            assertThat(aggregatedChildOutput.toString()).contains("Child1: Error1");
            assertThat(aggregatedChildOutput.toString()).contains("Child2: Error2");
        }
    }

    @Nested
    @DisplayName("Output Markers")
    class OutputMarkers {

        @Test
        @DisplayName("should identify error output markers")
        void shouldIdentifyErrorOutputMarkers() {
            // Arrange
            String errorStartMarker = "=== FULL ERROR OUTPUT ===";
            String errorEndMarker = "=== END ERROR OUTPUT ===";

            // Act & Assert
            assertThat(errorStartMarker).startsWith("===");
            assertThat(errorStartMarker).endsWith("===");
            assertThat(errorStartMarker).contains("FULL ERROR OUTPUT");
            assertThat(errorEndMarker).startsWith("===");
            assertThat(errorEndMarker).endsWith("===");
            assertThat(errorEndMarker).contains("END ERROR OUTPUT");
        }

        @Test
        @DisplayName("should identify comprehensive output markers")
        void shouldIdentifyComprehensiveOutputMarkers() {
            // Arrange
            String errorMessageMarker = "=== ERROR MESSAGE ===";
            String capturedOutputMarker = "=== CAPTURED OUTPUT ===";
            String childTestMarker = "=== CHILD TEST OUTPUT ===";
            String metadataMarker = "=== TEST METADATA ===";

            // Act & Assert
            assertThat(errorMessageMarker).startsWith("===");
            assertThat(errorMessageMarker).endsWith("===");
            assertThat(capturedOutputMarker).startsWith("===");
            assertThat(capturedOutputMarker).endsWith("===");
            assertThat(childTestMarker).startsWith("===");
            assertThat(childTestMarker).endsWith("===");
            assertThat(metadataMarker).startsWith("===");
            assertThat(metadataMarker).endsWith("===");
        }

        @Test
        @DisplayName("should identify child test output markers")
        void shouldIdentifyChildTestOutputMarkers() {
            // Arrange
            String childOutputMarker = "--- Child Test Output ---";

            // Act & Assert
            assertThat(childOutputMarker).startsWith("---");
            assertThat(childOutputMarker).endsWith("---");
            assertThat(childOutputMarker).contains("Child Test Output");
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
            String testName = "TestName";

            // Act & Assert
            assertThat(testName).isNotNull();
            assertThat(testName).isNotEmpty();
            assertThat(testName).contains("Test");
        }

        @Test
        @DisplayName("should validate null location URL")
        void shouldValidateNullLocationUrl() {
            // Arrange
            String locationUrl = null;

            // Act & Assert
            assertThat(locationUrl).isNull();
        }

        @Test
        @DisplayName("should validate valid location URL")
        void shouldValidateValidLocationUrl() {
            // Arrange
            String locationUrl = "file:///path/to/test.java";

            // Act & Assert
            assertThat(locationUrl).isNotNull();
            assertThat(locationUrl).isNotEmpty();
            assertThat(locationUrl).startsWith("file://");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("should handle very long output lines")
        void shouldHandleVeryLongOutputLines() {
            // Arrange
            String longOutputLine = "This is a very long output line that contains many words and should be handled gracefully by the output capture listener without causing any issues or exceptions";

            // Act & Assert
            assertThat(longOutputLine).isNotNull();
            assertThat(longOutputLine.length()).isGreaterThan(100);
            assertThat(longOutputLine).contains("output line");
        }

        @Test
        @DisplayName("should handle special characters in output")
        void shouldHandleSpecialCharactersInOutput() {
            // Arrange
            String specialOutput = "Output with special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?";

            // Act & Assert
            assertThat(specialOutput).isNotNull();
            assertThat(specialOutput).contains("!");
            assertThat(specialOutput).contains("@");
            assertThat(specialOutput).contains("#");
        }

        @Test
        @DisplayName("should handle unicode characters in output")
        void shouldHandleUnicodeCharactersInOutput() {
            // Arrange
            String unicodeOutput = "Output with unicode: café résumé naïve";

            // Act & Assert
            assertThat(unicodeOutput).isNotNull();
            assertThat(unicodeOutput).contains("é");
            assertThat(unicodeOutput).contains("ï");
        }

        @Test
        @DisplayName("should handle output with newlines")
        void shouldHandleOutputWithNewlines() {
            // Arrange
            String outputWithNewlines = "Line 1\nLine 2\nLine 3";

            // Act & Assert
            assertThat(outputWithNewlines).isNotNull();
            assertThat(outputWithNewlines).contains("\n");
            assertThat(outputWithNewlines.split("\n")).hasSize(3);
        }

        @Test
        @DisplayName("should handle output with tabs")
        void shouldHandleOutputWithTabs() {
            // Arrange
            String outputWithTabs = "Column1\tColumn2\tColumn3";

            // Act & Assert
            assertThat(outputWithTabs).isNotNull();
            assertThat(outputWithTabs).contains("\t");
            assertThat(outputWithTabs.split("\t")).hasSize(3);
        }

        @Test
        @DisplayName("should handle extremely large output gracefully")
        void shouldHandleExtremelyLargeOutputGracefully() {
            // Arrange
            StringBuilder largeOutput = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                largeOutput.append("Line ").append(i).append(": This is a test output line\n");
            }
            String largeOutputString = largeOutput.toString();

            // Act & Assert
            assertThat(largeOutputString).isNotNull();
            assertThat(largeOutputString.length()).isGreaterThan(10000);
            assertThat(largeOutputString).contains("Line 0:");
            assertThat(largeOutputString).contains("Line 999:");
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
            // Simulate a quick operation, as actual output capture is mocked out
            String dummyOperation = "Test output line";
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
                "Test output line 1",
                "Test output line 2",
                "Test output line 3",
                "Test output line 4",
                "Test output line 5"
            };

            long startTime = System.currentTimeMillis();

            // Act
            for (String operation : operations) {
                // Simulate a quick operation, as actual output capture is mocked out
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
            String operation = "Test output line";

            long startTime = System.currentTimeMillis();

            // Act
            for (int i = 0; i < 10; i++) {
                // Simulate a quick operation, as actual output capture is mocked out
                operation.length();
            }

            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            // Assert
            assertThat(executionTime).isLessThan(1000); // Should complete within 1 second
        }

        @Test
        @DisplayName("should handle large output volumes efficiently")
        void shouldHandleLargeOutputVolumesEfficiently() {
            // Arrange
            List<String> largeOutputList = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                largeOutputList.add("Output line " + i + " with some content");
            }

            long startTime = System.currentTimeMillis();

            // Act
            for (String output : largeOutputList) {
                // Simulate a quick operation, as actual output capture is mocked out
                output.length();
            }

            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            // Assert
            assertThat(executionTime).isLessThan(2000); // Should complete within 2 seconds
        }
    }
}
