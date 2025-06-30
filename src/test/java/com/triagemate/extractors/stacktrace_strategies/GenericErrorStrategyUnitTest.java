package com.triagemate.extractors.stacktrace_strategies;

import com.intellij.openapi.project.Project;
import com.triagemate.models.FailureInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for GenericErrorStrategy.
 * 
 * <p>These tests verify the strategy's ability to parse generic error types
 * that aren't handled by more specific strategies.</p>
 */
@ExtendWith(MockitoExtension.class)
class GenericErrorStrategyUnitTest {

    @Mock
    private Project project;

    private GenericErrorStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new GenericErrorStrategy(project);
    }

    @Test
    @DisplayName("should return correct strategy name")
    void shouldReturnCorrectStrategyName() {
        assertEquals("GenericErrorStrategy", strategy.getStrategyName());
    }

    @Test
    @DisplayName("should return correct priority")
    void shouldReturnCorrectPriority() {
        assertEquals(10, strategy.getPriority());
    }

    @Test
    @DisplayName("should handle generic exception with stack trace")
    void shouldHandleGenericExceptionWithStackTrace() {
        String testOutput = """
            java.lang.Exception: Some unexpected error occurred
                at com.example.MyTest.testSomething(MyTest.java:42)
                at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:325)
            """;

        assertTrue(strategy.canHandle(testOutput));
    }

    @Test
    @DisplayName("should handle custom exception with stack trace")
    void shouldHandleCustomExceptionWithStackTrace() {
        String testOutput = """
            com.example.CustomBusinessException: Business rule violation
                at com.example.BusinessService.validate(BusinessService.java:123)
                at com.example.MyTest.testBusinessLogic(MyTest.java:67)
            """;

        assertTrue(strategy.canHandle(testOutput));
    }

    @Test
    @DisplayName("should not handle null output")
    void shouldNotHandleNullOutput() {
        assertFalse(strategy.canHandle(null));
    }

    @Test
    @DisplayName("should not handle empty output")
    void shouldNotHandleEmptyOutput() {
        assertFalse(strategy.canHandle(""));
        assertFalse(strategy.canHandle("   "));
    }

    @Test
    @DisplayName("should not handle JUnit comparison failure")
    void shouldNotHandleJUnitComparisonFailure() {
        String testOutput = """
            org.junit.ComparisonFailure: expected:<foo> but was:<bar>
                at com.example.MyTest.testSomething(MyTest.java:42)
            """;

        assertFalse(strategy.canHandle(testOutput));
    }

    @Test
    @DisplayName("should not handle WebDriver exception")
    void shouldNotHandleWebDriverException() {
        String testOutput = """
            org.openqa.selenium.NoSuchElementException: no such element
                at com.example.MyTest.testSomething(MyTest.java:42)
            """;

        assertFalse(strategy.canHandle(testOutput));
    }

    @Test
    @DisplayName("should not handle Cucumber exception")
    void shouldNotHandleCucumberException() {
        String testOutput = """
            io.cucumber.junit.UndefinedStepException: The step "I click the button" is undefined
                at com.example.MyTest.testSomething(MyTest.java:42)
            """;

        assertFalse(strategy.canHandle(testOutput));
    }

    @Test
    @DisplayName("should not handle RuntimeException")
    void shouldNotHandleRuntimeException() {
        String testOutput = """
            java.lang.RuntimeException: Something went wrong
                at com.example.MyTest.testSomething(MyTest.java:42)
            """;

        assertFalse(strategy.canHandle(testOutput));
    }

    @Test
    @DisplayName("should handle stack trace without exception header")
    void shouldHandleStackTraceWithoutExceptionHeader() {
        String testOutput = """
                at com.example.MyTest.testSomething(MyTest.java:42)
                at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:325)
            """;

        assertTrue(strategy.canHandle(testOutput));
    }

    @Test
    @DisplayName("should parse generic exception correctly")
    void shouldParseGenericExceptionCorrectly() {
        String testOutput = """
            java.lang.Exception: Some unexpected error occurred
                at com.example.MyTest.testSomething(MyTest.java:42)
                at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:325)
            """;

        FailureInfo result = strategy.parse(testOutput);

        assertNotNull(result);
        assertEquals("Some unexpected error occurred", result.getErrorMessage());
        assertTrue(result.getStackTrace().contains("com.example.MyTest.testSomething"));
        assertTrue(result.getStackTrace().contains("MyTest.java:42"));
        assertEquals("GenericErrorStrategy", result.getParsingStrategy());
        assertTrue(result.getParsingTime() > 0);
    }

    @Test
    @DisplayName("should parse custom exception correctly")
    void shouldParseCustomExceptionCorrectly() {
        String testOutput = """
            com.example.CustomBusinessException: Business rule violation
                at com.example.BusinessService.validate(BusinessService.java:123)
                at com.example.MyTest.testBusinessLogic(MyTest.java:67)
            """;

        FailureInfo result = strategy.parse(testOutput);

        assertNotNull(result);
        assertEquals("Business rule violation", result.getErrorMessage());
        assertTrue(result.getStackTrace().contains("com.example.BusinessService.validate"));
        assertTrue(result.getStackTrace().contains("BusinessService.java:123"));
        assertEquals("GenericErrorStrategy", result.getParsingStrategy());
    }

    @Test
    @DisplayName("should parse exception without message")
    void shouldParseExceptionWithoutMessage() {
        String testOutput = """
            java.lang.Exception
                at com.example.MyTest.testSomething(MyTest.java:42)
            """;

        FailureInfo result = strategy.parse(testOutput);

        assertNotNull(result);
        assertEquals("Failed to parse generic error", result.getErrorMessage());
        assertEquals(testOutput, result.getStackTrace());
        assertEquals("GenericErrorStrategy", result.getParsingStrategy());
    }

    @Test
    @DisplayName("should parse stack trace without line numbers")
    void shouldParseStackTraceWithoutLineNumbers() {
        String testOutput = """
            java.lang.Exception: Some error
                at com.example.MyTest.testSomething(MyTest.java)
                at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java)
            """;

        FailureInfo result = strategy.parse(testOutput);

        assertNotNull(result);
        assertEquals("Some error", result.getErrorMessage());
        assertTrue(result.getStackTrace().contains("com.example.MyTest.testSomething"));
        assertTrue(result.getStackTrace().contains("MyTest.java"));
        assertEquals("GenericErrorStrategy", result.getParsingStrategy());
    }

    @Test
    @DisplayName("should parse complex stack trace with multiple frames")
    void shouldParseComplexStackTraceWithMultipleFrames() {
        String testOutput = """
            java.lang.Exception: Complex error scenario
                at com.example.ServiceLayer.process(ServiceLayer.java:100)
                at com.example.BusinessLogic.execute(BusinessLogic.java:50)
                at com.example.MyTest.testComplexScenario(MyTest.java:25)
                at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:325)
                at org.junit.runners.ParentRunner.access$000(ParentRunner.java:66)
            """;

        FailureInfo result = strategy.parse(testOutput);

        assertNotNull(result);
        assertEquals("Complex error scenario", result.getErrorMessage());
        assertTrue(result.getStackTrace().contains("com.example.ServiceLayer.process"));
        assertTrue(result.getStackTrace().contains("com.example.BusinessLogic.execute"));
        assertTrue(result.getStackTrace().contains("com.example.MyTest.testComplexScenario"));
        assertEquals("GenericErrorStrategy", result.getParsingStrategy());
    }

    @Test
    @DisplayName("should throw exception for null input")
    void shouldThrowExceptionForNullInput() {
        assertThrows(IllegalArgumentException.class, () -> strategy.parse(null));
    }

    @Test
    @DisplayName("should throw exception for empty input")
    void shouldThrowExceptionForEmptyInput() {
        assertThrows(IllegalArgumentException.class, () -> strategy.parse(""));
        assertThrows(IllegalArgumentException.class, () -> strategy.parse("   "));
    }

    @Test
    @DisplayName("should create fallback failure info when parsing fails")
    void shouldCreateFallbackFailureInfoWhenParsingFails() {
        String testOutput = "Invalid format that cannot be parsed";

        FailureInfo result = strategy.parse(testOutput);

        assertNotNull(result);
        assertEquals("Failed to parse generic error", result.getErrorMessage());
        assertEquals(testOutput, result.getStackTrace());
        assertEquals("GenericErrorStrategy", result.getParsingStrategy());
        assertTrue(result.getParsingTime() > 0);
    }

    @Test
    @DisplayName("should handle exception with special characters in message")
    void shouldHandleExceptionWithSpecialCharactersInMessage() {
        String testOutput = """
            java.lang.Exception: Error with special chars: !@#$%^&*()_+-=[]{}|;':",./<>?
                at com.example.MyTest.testSomething(MyTest.java:42)
            """;

        FailureInfo result = strategy.parse(testOutput);

        assertNotNull(result);
        assertEquals("Error with special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?", result.getErrorMessage());
        assertTrue(result.getStackTrace().contains("com.example.MyTest.testSomething"));
        assertEquals("GenericErrorStrategy", result.getParsingStrategy());
    }

    @Test
    @DisplayName("should handle exception with unicode characters")
    void shouldHandleExceptionWithUnicodeCharacters() {
        String testOutput = """
            java.lang.Exception: Error with unicode: 你好世界 🌍
                at com.example.MyTest.testSomething(MyTest.java:42)
            """;

        FailureInfo result = strategy.parse(testOutput);

        assertNotNull(result);
        assertEquals("Error with unicode: 你好世界 🌍", result.getErrorMessage());
        assertTrue(result.getStackTrace().contains("com.example.MyTest.testSomething"));
        assertEquals("GenericErrorStrategy", result.getParsingStrategy());
    }

    @Test
    @DisplayName("should handle exception with newlines in message")
    void shouldHandleExceptionWithNewlinesInMessage() {
        String testOutput = """
            java.lang.Exception: Error with
            multiple lines
            in message
                at com.example.MyTest.testSomething(MyTest.java:42)
            """;

        FailureInfo result = strategy.parse(testOutput);

        assertNotNull(result);
        assertEquals("Error with\nmultiple lines\nin message", result.getErrorMessage());
        assertTrue(result.getStackTrace().contains("com.example.MyTest.testSomething"));
        assertEquals("GenericErrorStrategy", result.getParsingStrategy());
    }
} 