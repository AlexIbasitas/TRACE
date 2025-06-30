package com.triagemate.extractors.stacktrace_strategies;

import com.intellij.openapi.project.Project;
import com.triagemate.models.FailureInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for RuntimeErrorStrategy.
 * 
 * <p>These tests focus on the parsing logic without requiring PSI operations,
 * making them fast and reliable unit tests.</p>
 */
class RuntimeErrorStrategyUnitTest {

    @Mock
    private Project mockProject;
    
    private RuntimeErrorStrategy strategy;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        strategy = new RuntimeErrorStrategy(mockProject);
    }

    @Test
    @DisplayName("should handle null pointer exception")
    void shouldHandleNullPointerException() {
        String output = "java.lang.NullPointerException: Cannot invoke \"String.length()\" because \"str\" is null\n" +
                       "    at com.example.MyTest.testSomething(MyTest.java:42)";
        
        assertTrue(strategy.canHandle(output));
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("NULL_POINTER_EXCEPTION", info.getAssertionType());
        assertEquals("Cannot invoke \"String.length()\" because \"str\" is null", info.getErrorMessage());
        assertEquals("RuntimeErrorStrategy", info.getParsingStrategy());
        assertNotNull(info.getStackTrace());
    }

    @Test
    @DisplayName("should handle illegal argument exception")
    void shouldHandleIllegalArgumentException() {
        String output = "java.lang.IllegalArgumentException: Invalid argument provided\n" +
                       "    at com.example.MyTest.testSomething(MyTest.java:42)";
        
        assertTrue(strategy.canHandle(output));
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("ILLEGAL_ARGUMENT_EXCEPTION", info.getAssertionType());
        assertEquals("Invalid argument provided", info.getErrorMessage());
        assertEquals("RuntimeErrorStrategy", info.getParsingStrategy());
    }

    @Test
    @DisplayName("should handle illegal state exception")
    void shouldHandleIllegalStateException() {
        String output = "java.lang.IllegalStateException: Object is in invalid state\n" +
                       "    at com.example.MyTest.testSomething(MyTest.java:42)";
        
        assertTrue(strategy.canHandle(output));
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("ILLEGAL_STATE_EXCEPTION", info.getAssertionType());
        assertEquals("Object is in invalid state", info.getErrorMessage());
        assertEquals("RuntimeErrorStrategy", info.getParsingStrategy());
    }

    @Test
    @DisplayName("should handle array index out of bounds exception")
    void shouldHandleArrayIndexOutOfBoundsException() {
        String output = "java.lang.ArrayIndexOutOfBoundsException: Index 5 out of bounds for length 3\n" +
                       "    at com.example.MyTest.testSomething(MyTest.java:42)";
        
        assertTrue(strategy.canHandle(output));
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("ARRAY_INDEX_OUT_OF_BOUNDS_EXCEPTION", info.getAssertionType());
        assertEquals("Index 5 out of bounds for length 3", info.getErrorMessage());
        assertEquals("RuntimeErrorStrategy", info.getParsingStrategy());
    }

    @Test
    @DisplayName("should handle class cast exception")
    void shouldHandleClassCastException() {
        String output = "java.lang.ClassCastException: java.lang.String cannot be cast to java.lang.Integer\n" +
                       "    at com.example.MyTest.testSomething(MyTest.java:42)";
        
        assertTrue(strategy.canHandle(output));
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("CLASS_CAST_EXCEPTION", info.getAssertionType());
        assertEquals("java.lang.String cannot be cast to java.lang.Integer", info.getErrorMessage());
        assertEquals("RuntimeErrorStrategy", info.getParsingStrategy());
    }

    @Test
    @DisplayName("should handle number format exception")
    void shouldHandleNumberFormatException() {
        String output = "java.lang.NumberFormatException: For input string: \"abc\"\n" +
                       "    at com.example.MyTest.testSomething(MyTest.java:42)";
        
        assertTrue(strategy.canHandle(output));
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("NUMBER_FORMAT_EXCEPTION", info.getAssertionType());
        assertEquals("For input string: \"abc\"", info.getErrorMessage());
        assertEquals("RuntimeErrorStrategy", info.getParsingStrategy());
    }

    @Test
    @DisplayName("should handle index out of bounds exception")
    void shouldHandleIndexOutOfBoundsException() {
        String output = "java.lang.IndexOutOfBoundsException: Index: 10, Size: 5\n" +
                       "    at com.example.MyTest.testSomething(MyTest.java:42)";
        
        assertTrue(strategy.canHandle(output));
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("INDEX_OUT_OF_BOUNDS_EXCEPTION", info.getAssertionType());
        assertEquals("Index: 10, Size: 5", info.getErrorMessage());
        assertEquals("RuntimeErrorStrategy", info.getParsingStrategy());
    }

    @Test
    @DisplayName("should handle concurrent modification exception")
    void shouldHandleConcurrentModificationException() {
        String output = "java.util.ConcurrentModificationException: Collection was modified during iteration\n" +
                       "    at com.example.MyTest.testSomething(MyTest.java:42)";
        
        assertTrue(strategy.canHandle(output));
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("CONCURRENT_MODIFICATION_EXCEPTION", info.getAssertionType());
        assertEquals("Collection was modified during iteration", info.getErrorMessage());
        assertEquals("RuntimeErrorStrategy", info.getParsingStrategy());
    }

    @Test
    @DisplayName("should handle security exception")
    void shouldHandleSecurityException() {
        String output = "java.lang.SecurityException: Access denied\n" +
                       "    at com.example.MyTest.testSomething(MyTest.java:42)";
        
        assertTrue(strategy.canHandle(output));
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("SECURITY_EXCEPTION", info.getAssertionType());
        assertEquals("Access denied", info.getErrorMessage());
        assertEquals("RuntimeErrorStrategy", info.getParsingStrategy());
    }

    @Test
    @DisplayName("should handle generic runtime exception")
    void shouldHandleGenericRuntimeException() {
        String output = "java.lang.RuntimeException: Something went wrong\n" +
                       "    at com.example.MyTest.testSomething(MyTest.java:42)";
        
        assertTrue(strategy.canHandle(output));
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("RUNTIME_EXCEPTION", info.getAssertionType());
        assertEquals("Something went wrong", info.getErrorMessage());
        assertEquals("RuntimeErrorStrategy", info.getParsingStrategy());
    }

    @Test
    @DisplayName("should handle runtime exception with multi-line message")
    void shouldHandleRuntimeExceptionWithMultiLineMessage() {
        String output = "java.lang.RuntimeException: Something went wrong\n" +
                       "    Additional details on multiple lines\n" +
                       "    at com.example.MyTest.testSomething(MyTest.java:42)";
        
        assertTrue(strategy.canHandle(output));
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("RUNTIME_EXCEPTION", info.getAssertionType());
        assertEquals("Something went wrong\n    Additional details on multiple lines", info.getErrorMessage());
        assertEquals("RuntimeErrorStrategy", info.getParsingStrategy());
    }

    @Test
    @DisplayName("should handle runtime exception without message")
    void shouldHandleRuntimeExceptionWithoutMessage() {
        String output = "java.lang.RuntimeException\n" +
                       "    at com.example.MyTest.testSomething(MyTest.java:42)";
        
        assertTrue(strategy.canHandle(output));
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("RUNTIME_ERROR", info.getAssertionType());
        assertEquals("java.lang.RuntimeException", info.getErrorMessage());
        assertEquals("RuntimeErrorStrategy", info.getParsingStrategy());
    }

    @Test
    @DisplayName("should not handle non-runtime exceptions")
    void shouldNotHandleNonRuntimeExceptions() {
        String output = "org.junit.ComparisonFailure: expected:<foo> but was:<bar>\n" +
                       "    at com.example.MyTest.testSomething(MyTest.java:42)";
        
        assertFalse(strategy.canHandle(output));
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
    @DisplayName("should throw exception when parsing null output")
    void shouldThrowExceptionWhenParsingNullOutput() {
        assertThrows(IllegalArgumentException.class, () -> strategy.parse(null));
    }

    @Test
    @DisplayName("should create fallback failure info when parsing unrecognized output")
    void shouldCreateFallbackFailureInfoWhenParsingUnrecognizedOutput() {
        String output = "Some unrecognized error message";
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("RUNTIME_ERROR", info.getAssertionType());
        assertEquals("Runtime error: Some unrecognized error message", info.getErrorMessage());
        assertEquals("RuntimeErrorStrategy", info.getParsingStrategy());
        assertNotNull(info.getStackTrace());
    }

    @Test
    @DisplayName("should extract stack trace information correctly")
    void shouldExtractStackTraceInformationCorrectly() {
        String output = "java.lang.NullPointerException: Cannot invoke \"String.length()\" because \"str\" is null\n" +
                       "    at com.example.MyTest.testSomething(MyTest.java:42)\n" +
                       "    at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:325)";
        
        FailureInfo info = strategy.parse(output);
        
        assertNotNull(info.getStackTrace());
        assertTrue(info.getStackTrace().contains("java.lang.NullPointerException"));
        assertTrue(info.getStackTrace().contains("at com.example.MyTest.testSomething"));
    }

    @Test
    @DisplayName("should handle runtime exception with complex stack trace")
    void shouldHandleRuntimeExceptionWithComplexStackTrace() {
        String output = "java.lang.IllegalArgumentException: Invalid argument\n" +
                       "    at com.example.MyTest.testSomething(MyTest.java:42)\n" +
                       "    at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:325)\n" +
                       "    at org.junit.runners.ParentRunner.run(ParentRunner.java:363)\n" +
                       "    at org.junit.runner.JUnitCore.run(JUnitCore.java:137)";
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("ILLEGAL_ARGUMENT_EXCEPTION", info.getAssertionType());
        assertEquals("Invalid argument", info.getErrorMessage());
        assertNotNull(info.getStackTrace());
        assertTrue(info.getStackTrace().contains("at com.example.MyTest.testSomething"));
    }

    @Test
    @DisplayName("should return correct priority")
    void shouldReturnCorrectPriority() {
        assertEquals(80, strategy.getPriority());
    }

    @Test
    @DisplayName("should return correct strategy name")
    void shouldReturnCorrectStrategyName() {
        assertEquals("RuntimeErrorStrategy", strategy.getStrategyName());
    }

    @Test
    @DisplayName("should handle runtime exception with special characters in message")
    void shouldHandleRuntimeExceptionWithSpecialCharactersInMessage() {
        String output = "java.lang.RuntimeException: Error with \"quotes\" and 'apostrophes' and <brackets>\n" +
                       "    at com.example.MyTest.testSomething(MyTest.java:42)";
        
        assertTrue(strategy.canHandle(output));
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("RUNTIME_EXCEPTION", info.getAssertionType());
        assertEquals("Error with \"quotes\" and 'apostrophes' and <brackets>", info.getErrorMessage());
        assertEquals("RuntimeErrorStrategy", info.getParsingStrategy());
    }

    @Test
    @DisplayName("should handle runtime exception with unicode characters")
    void shouldHandleRuntimeExceptionWithUnicodeCharacters() {
        String output = "java.lang.RuntimeException: Error with unicode: 测试 テスト 테스트\n" +
                       "    at com.example.MyTest.testSomething(MyTest.java:42)";
        
        assertTrue(strategy.canHandle(output));
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("RUNTIME_EXCEPTION", info.getAssertionType());
        assertEquals("Error with unicode: 测试 テスト 테스트", info.getErrorMessage());
        assertEquals("RuntimeErrorStrategy", info.getParsingStrategy());
    }
} 