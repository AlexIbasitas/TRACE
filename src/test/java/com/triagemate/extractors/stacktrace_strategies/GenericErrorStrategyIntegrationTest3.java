package com.triagemate.extractors.stacktrace_strategies;

import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.triagemate.models.FailureInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for GenericErrorStrategy (Part 3).
 *
 * <p>These tests verify the strategy works correctly with real PSI operations
 * and file system integration. Split into multiple classes to avoid temp directory nesting issues.</p>
 * 
 * <p><strong>Note:</strong> This class contains tests 9-12. Additional tests are in:
 * - GenericErrorStrategyIntegrationTest (4 tests)
 * - GenericErrorStrategyIntegrationTest2 (4 tests)</p>
 */
class GenericErrorStrategyIntegrationTest3 extends BasePlatformTestCase {

    private GenericErrorStrategy strategy;

    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        strategy = new GenericErrorStrategy(getProject());
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            // test specific tear down calls (if any)
        } catch (Exception e) {
            addSuppressedException(e);
        } finally {
            super.tearDown();
        }
    }

    @Test
    @DisplayName("should handle edge case: empty stack trace")
    void shouldHandleEdgeCaseEmptyStackTrace() {
        String testOutput = "java.lang.Exception: Only a message, no stack trace";
        FailureInfo result = strategy.parse(testOutput);
        assertNotNull(result);
        assertEquals("Only a message, no stack trace", result.getErrorMessage());
        assertEquals("GenericErrorStrategy", result.getParsingStrategy());
        assertTrue(result.getStackTrace().isEmpty() || result.getStackTrace().equals(""));
    }

    @Test
    @DisplayName("should handle edge case: null input")
    void shouldHandleEdgeCaseNullInput() {
        FailureInfo result = strategy.parse(null);
        assertNotNull(result);
        assertEquals("Failed to parse generic error", result.getErrorMessage());
        assertEquals("GenericErrorStrategy", result.getParsingStrategy());
        assertTrue(result.getStackTrace().isEmpty() || result.getStackTrace().equals(""));
    }

    @Test
    @DisplayName("should handle edge case: completely malformed input")
    void shouldHandleEdgeCaseCompletelyMalformedInput() {
        String testOutput = "This is not a stack trace at all!";
        FailureInfo result = strategy.parse(testOutput);
        assertNotNull(result);
        assertEquals("Failed to parse generic error", result.getErrorMessage());
        assertEquals("GenericErrorStrategy", result.getParsingStrategy());
        assertTrue(result.getStackTrace().isEmpty() || result.getStackTrace().equals(""));
    }

    @Test
    @DisplayName("should handle edge case: multi-line message with no stack trace")
    void shouldHandleEdgeCaseMultiLineMessageNoStackTrace() {
        String testOutput = "java.lang.Exception: First line\nSecond line\nThird line";
        FailureInfo result = strategy.parse(testOutput);
        assertNotNull(result);
        assertEquals("First line", result.getErrorMessage());
        assertEquals("GenericErrorStrategy", result.getParsingStrategy());
        assertTrue(result.getStackTrace().isEmpty() || result.getStackTrace().equals(""));
    }
} 