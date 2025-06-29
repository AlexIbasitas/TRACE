package com.triagemate.extractors.stacktrace_strategies;

import com.triagemate.models.FailureInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CucumberErrorStrategy.
 * 
 * <p>These tests focus on pure logic without IntelliJ Platform dependencies.
 * They test the core parsing functionality and error handling.</p>
 */
@DisplayName("CucumberErrorStrategy Unit Tests")
class CucumberErrorStrategyUnitTest {
    
    private CucumberErrorStrategy strategy;

    @BeforeEach
    void setUp() {
        // Create strategy without project dependency for unit testing
        strategy = new CucumberErrorStrategy(null);
    }

    @Test
    @DisplayName("should handle undefined step exception")
    void shouldHandleUndefinedStepException() {
        String output = "io.cucumber.junit.UndefinedStepException: The step \"I click on the button\" is undefined.\n" +
                       "    at com.example.MyTest.testSomething(MyTest.java:42)";
        
        assertTrue(strategy.canHandle(output));
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("UNDEFINED_STEP", info.getAssertionType());
        assertEquals("I click on the button", info.getFailedStepText());
        assertEquals("The step \"I click on the button\" is undefined.", info.getErrorMessage());
        assertNotNull(info.getStackTrace());
        assertEquals("CucumberErrorStrategy", info.getParsingStrategy());
    }

    @Test
    @DisplayName("should handle ambiguous step definitions exception")
    void shouldHandleAmbiguousStepDefinitionsException() {
        String output = "io.cucumber.junit.AmbiguousStepDefinitionsException: The step \"I click on the button\" matches multiple step definitions.\n" +
                       "    at com.example.MyTest.testSomething(MyTest.java:42)";
        
        assertTrue(strategy.canHandle(output));
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("AMBIGUOUS_STEP", info.getAssertionType());
        assertEquals("I click on the button", info.getFailedStepText());
        assertEquals("The step \"I click on the button\" matches multiple step definitions.", info.getErrorMessage());
        assertNotNull(info.getStackTrace());
    }

    @Test
    @DisplayName("should handle pending exception")
    void shouldHandlePendingException() {
        String output = "io.cucumber.junit.PendingException: The step \"I click on the button\" is pending.\n" +
                       "    at com.example.MyTest.testSomething(MyTest.java:42)";
        
        assertTrue(strategy.canHandle(output));
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("PENDING_STEP", info.getAssertionType());
        assertEquals("I click on the button", info.getFailedStepText());
        assertEquals("The step \"I click on the button\" is pending.", info.getErrorMessage());
        assertNotNull(info.getStackTrace());
    }

    @Test
    @DisplayName("should handle cucumber exception")
    void shouldHandleCucumberException() {
        String output = "io.cucumber.junit.CucumberException: Something went wrong in Cucumber.\n" +
                       "    at com.example.MyTest.testSomething(MyTest.java:42)";
        
        assertTrue(strategy.canHandle(output));
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("CUCUMBER_EXCEPTION", info.getAssertionType());
        assertEquals("Cucumber step failed", info.getFailedStepText());
        assertEquals("Something went wrong in Cucumber.", info.getErrorMessage());
        assertNotNull(info.getStackTrace());
    }

    @Test
    @DisplayName("should handle step definition not found")
    void shouldHandleStepDefinitionNotFound() {
        String output = "Step definition not found for step 'I click on the button'\n" +
                       "    at com.example.MyTest.testSomething(MyTest.java:42)";
        
        assertTrue(strategy.canHandle(output));
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("STEP_DEFINITION_NOT_FOUND", info.getAssertionType());
        assertEquals("Step definition not found for step: I click on the button", info.getFailedStepText());
        assertNotNull(info.getStackTrace());
    }

    @Test
    @DisplayName("should handle generic cucumber error")
    void shouldHandleGenericCucumberError() {
        String output = "Some cucumber.runtime error occurred\n" +
                       "    at com.example.MyTest.testSomething(MyTest.java:42)";
        
        assertTrue(strategy.canHandle(output));
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("CUCUMBER_ERROR", info.getAssertionType());
        assertEquals("Cucumber step failed", info.getFailedStepText());
        assertNotNull(info.getStackTrace());
    }

    @Test
    @DisplayName("should not handle non-cucumber errors")
    void shouldNotHandleNonCucumberErrors() {
        String output = "java.lang.NullPointerException: null\n" +
                       "    at com.example.MyTest.testSomething(MyTest.java:42)";
        
        assertFalse(strategy.canHandle(output));
    }

    @Test
    @DisplayName("should not handle null or empty output")
    void shouldNotHandleNullOrEmptyOutput() {
        assertFalse(strategy.canHandle(null));
        assertFalse(strategy.canHandle(""));
        assertFalse(strategy.canHandle("   "));
    }

    @Test
    @DisplayName("should parse stack trace elements correctly")
    void shouldParseStackTraceElementsCorrectly() {
        String line = "at com.example.MyTest.testSomething(MyTest.java:42)";
        
        var element = strategy.parseStackTraceElement(line);
        
        assertNotNull(element);
        assertEquals("com.example.MyTest", element.getClassName());
        assertEquals("testSomething", element.getMethodName());
        assertEquals("MyTest.java", element.getFileName());
        assertEquals(42, element.getLineNumber());
    }

    @Test
    @DisplayName("should handle stack trace elements without line numbers")
    void shouldHandleStackTraceElementsWithoutLineNumbers() {
        String line = "at com.example.MyTest.testSomething(MyTest.java)";
        
        var element = strategy.parseStackTraceElement(line);
        
        assertNotNull(element);
        assertEquals("com.example.MyTest", element.getClassName());
        assertEquals("testSomething", element.getMethodName());
        assertEquals("MyTest.java", element.getFileName());
        assertEquals(-1, element.getLineNumber());
    }

    @Test
    @DisplayName("should handle stack trace elements with missing file info")
    void shouldHandleStackTraceElementsWithMissingFileInfo() {
        String line = "at com.example.MyTest.testSomething";
        
        var element = strategy.parseStackTraceElement(line);
        
        assertNull(element);
    }

    @Test
    @DisplayName("should handle malformed stack trace lines")
    void shouldHandleMalformedStackTraceLines() {
        assertNull(strategy.parseStackTraceElement("invalid line"));
        assertNull(strategy.parseStackTraceElement("at "));
        assertNull(strategy.parseStackTraceElement(""));
    }

    @Test
    @DisplayName("should extract step text from quoted strings")
    void shouldExtractStepTextFromQuotedStrings() {
        String output = "io.cucumber.junit.UndefinedStepException: The step 'I click on the button' is undefined.\n" +
                       "    at com.example.MyTest.testSomething(MyTest.java:42)";
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("I click on the button", info.getFailedStepText());
    }

    @Test
    @DisplayName("should handle double quoted step text")
    void shouldHandleDoubleQuotedStepText() {
        String output = "io.cucumber.junit.UndefinedStepException: The step \"I click on the button\" is undefined.\n" +
                       "    at com.example.MyTest.testSomething(MyTest.java:42)";
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("I click on the button", info.getFailedStepText());
    }

    @Test
    @DisplayName("should handle step text with special characters")
    void shouldHandleStepTextWithSpecialCharacters() {
        String output = "io.cucumber.junit.UndefinedStepException: The step \"I click on the 'Submit' button\" is undefined.\n" +
                       "    at com.example.MyTest.testSomething(MyTest.java:42)";
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("I click on the 'Submit' button", info.getFailedStepText());
    }

    @Test
    @DisplayName("should return fallback step text when no quotes found")
    void shouldReturnFallbackStepTextWhenNoQuotesFound() {
        String output = "io.cucumber.junit.CucumberException: Something went wrong\n" +
                       "    at com.example.MyTest.testSomething(MyTest.java:42)";
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("Cucumber step failed", info.getFailedStepText());
    }

    @Test
    @DisplayName("should handle null input in parse method")
    void shouldHandleNullInputInParseMethod() {
        assertThrows(IllegalArgumentException.class, () -> {
            strategy.parse(null);
        });
    }

    @Test
    @DisplayName("should return correct priority")
    void shouldReturnCorrectPriority() {
        assertEquals(85, strategy.getPriority());
    }

    @Test
    @DisplayName("should return correct strategy name")
    void shouldReturnCorrectStrategyName() {
        assertEquals("CucumberErrorStrategy", strategy.getStrategyName());
    }
} 