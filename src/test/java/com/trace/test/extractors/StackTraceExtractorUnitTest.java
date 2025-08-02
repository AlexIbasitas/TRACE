package com.trace.test.extractors;

import com.intellij.execution.testframework.sm.runner.SMTestProxy;
import com.intellij.openapi.project.Project;
import com.trace.test.models.FailureInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for StackTraceExtractor (pure logic only).
 * 
 * <p>These tests focus on logic that does not require IntelliJ PSI or file system.</p>
 */
@ExtendWith(MockitoExtension.class)
class StackTraceExtractorUnitTest {

    @Mock
    private Project mockProject;

    @Mock
    private SMTestProxy mockTestProxy;

    @Mock
    private SMTestProxy mockParentTestProxy;

    private StackTraceExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new StackTraceExtractor(mockProject);
    }

    @Test
    @DisplayName("should throw exception when project is null")
    void shouldThrowExceptionWhenProjectIsNull() {
        assertThatThrownBy(() -> new StackTraceExtractor(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Project cannot be null");
    }

    @Test
    @DisplayName("should throw exception when test proxy is null")
    void shouldThrowExceptionWhenTestProxyIsNull() {
        assertThatThrownBy(() -> extractor.extractFailureInfo(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Test cannot be null");
    }

    @Test
    @DisplayName("should return null when test proxy is null for step text extraction")
    void shouldReturnNullWhenTestProxyIsNullForStepTextExtraction() {
        String result = extractor.extractFailedStepText(null);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle null error message gracefully")
    void shouldHandleNullErrorMessageGracefully() {
        when(mockTestProxy.getName()).thenReturn("Scenario: User login");
        when(mockTestProxy.getErrorMessage()).thenReturn(null);
        when(mockTestProxy.getStacktrace()).thenReturn("java.lang.AssertionError: Test failed");
        when(mockTestProxy.getLocationUrl()).thenReturn(null);
        FailureInfo result = extractor.extractFailureInfo(mockTestProxy);
        assertThat(result).isNotNull();
        assertThat(result.getErrorMessage()).isEqualTo("Test failed");
    }

    @Test
    @DisplayName("should handle null stack trace gracefully")
    void shouldHandleNullStackTraceGracefully() {
        when(mockTestProxy.getName()).thenReturn("Scenario: User login");
        when(mockTestProxy.getErrorMessage()).thenReturn("Test failed");
        when(mockTestProxy.getStacktrace()).thenReturn(null);
        when(mockTestProxy.getLocationUrl()).thenReturn(null);
        FailureInfo result = extractor.extractFailureInfo(mockTestProxy);
        assertThat(result).isNotNull();
        assertThat(result.getStackTrace()).isEqualTo("Stack trace not available");
    }

    @Test
    @DisplayName("should create descriptive scenario name when no valid name found")
    void shouldCreateDescriptiveScenarioNameWhenNoValidNameFound() {
        when(mockTestProxy.getName()).thenReturn("I enter username");
        when(mockTestProxy.getErrorMessage()).thenReturn("Test failed");
        when(mockTestProxy.getStacktrace()).thenReturn("java.lang.AssertionError: Test failed");
        when(mockTestProxy.getLocationUrl()).thenReturn(null);
        when(mockTestProxy.getParent()).thenReturn(mockParentTestProxy);
        when(mockParentTestProxy.getName()).thenReturn("I enter username"); // Same as test name
        FailureInfo result = extractor.extractFailureInfo(mockTestProxy);
        assertThat(result).isNotNull();
        assertThat(result.getScenarioName()).isEqualTo("Cucumber Test - I enter username");
    }

    @Test
    @DisplayName("should handle null test name gracefully")
    void shouldHandleNullTestNameGracefully() {
        when(mockTestProxy.getName()).thenReturn(null);
        when(mockTestProxy.getErrorMessage()).thenReturn("Test failed");
        when(mockTestProxy.getStacktrace()).thenReturn("java.lang.AssertionError: Test failed");
        when(mockTestProxy.getLocationUrl()).thenReturn(null);
        FailureInfo result = extractor.extractFailureInfo(mockTestProxy);
        assertThat(result).isNotNull();
        assertThat(result.getScenarioName()).isEqualTo("Cucumber Test - Unknown Step");
        assertThat(result.getFailedStepText()).isNull();
    }

    @Test
    @DisplayName("should handle empty test name gracefully")
    void shouldHandleEmptyTestNameGracefully() {
        when(mockTestProxy.getName()).thenReturn("");
        when(mockTestProxy.getErrorMessage()).thenReturn("Test failed");
        when(mockTestProxy.getStacktrace()).thenReturn("java.lang.AssertionError: Test failed");
        when(mockTestProxy.getLocationUrl()).thenReturn(null);
        FailureInfo result = extractor.extractFailureInfo(mockTestProxy);
        assertThat(result).isNotNull();
        assertThat(result.getScenarioName()).isEqualTo("Cucumber Test - Unknown Step");
        assertThat(result.getFailedStepText()).isNull();
    }

    @Test
    @DisplayName("should handle whitespace-only test name gracefully")
    void shouldHandleWhitespaceOnlyTestNameGracefully() {
        when(mockTestProxy.getName()).thenReturn("   \n\t  ");
        when(mockTestProxy.getErrorMessage()).thenReturn("Test failed");
        when(mockTestProxy.getStacktrace()).thenReturn("java.lang.AssertionError: Test failed");
        when(mockTestProxy.getLocationUrl()).thenReturn(null);
        FailureInfo result = extractor.extractFailureInfo(mockTestProxy);
        assertThat(result).isNotNull();
        assertThat(result.getScenarioName()).isEqualTo("Cucumber Test -    \n\t  ");
        assertThat(result.getFailedStepText()).isEqualTo("   \n\t  ");
    }

    @Test
    @DisplayName("should create minimal failure info when extraction fails completely")
    void shouldCreateMinimalFailureInfoWhenExtractionFailsCompletely() {
        when(mockTestProxy.getName()).thenReturn("Test Name");
        when(mockTestProxy.getErrorMessage()).thenThrow(new RuntimeException("Complete failure"));
        FailureInfo result = extractor.extractFailureInfo(mockTestProxy);
        assertThat(result).isNotNull();
        assertThat(result.getScenarioName()).isEqualTo("Test Name");
        assertThat(result.getErrorMessage()).isEqualTo("Failed to extract detailed failure information");
        assertThat(result.getStackTrace()).isEqualTo("Stack trace extraction failed");
        assertThat(result.getParsingTime()).isGreaterThan(0);
    }

    /**
     * Test scenario name formatting for scenario outlines.
     * Verifies that scenario outline names include both title and example identifier.
     */
    @Test
    public void testScenarioOutlineNameFormatting() {
        // Test scenario outline formatting
        String scenarioName = "Verify Home Page has sample of correct links";
        String testName = "Example #1.1";
        String formattedName = extractor.formatScenarioName(scenarioName, testName);
        assertEquals("Verify Home Page has sample of correct links (Example #1.1)", formattedName);
        
        // Test regular scenario (no formatting)
        String regularScenarioName = "User login test";
        String regularTestName = "User login test";
        String regularFormattedName = extractor.formatScenarioName(regularScenarioName, regularTestName);
        assertEquals("User login test", regularFormattedName);
        
        // Test with null testName
        String nullTestNameFormatted = extractor.formatScenarioName(scenarioName, null);
        assertEquals("Verify Home Page has sample of correct links", nullTestNameFormatted);
        
        // Test with null scenarioName
        String nullScenarioFormatted = extractor.formatScenarioName(null, testName);
        assertEquals("Example #1.1", nullScenarioFormatted);
    }
} 