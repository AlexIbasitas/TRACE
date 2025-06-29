package com.triagemate.extractors.stacktrace_strategies;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class JUnitComparisonFailureStrategyUnitTest {

    @Test
    void canHandle_shouldReturnTrueForValidJUnitComparisonFailure() {
        JUnitComparisonFailureStrategy strategy = new JUnitComparisonFailureStrategy(null);
        assertThat(strategy.canHandle("org.junit.ComparisonFailure: expected:<foo> but was:<bar>"))
                .isTrue();
        assertThat(strategy.canHandle("org.junit.ComparisonFailure: expected:<true> but was:<false>"))
                .isTrue();
        assertThat(strategy.canHandle("org.junit.ComparisonFailure: expected:<Hello World> but was:<Hello>"))
                .isTrue();
    }

    @Test
    void canHandle_shouldReturnFalseForNonJUnitErrors() {
        JUnitComparisonFailureStrategy strategy = new JUnitComparisonFailureStrategy(null);
        assertThat(strategy.canHandle("java.lang.AssertionError: expected [true] but found [false]"))
                .isFalse();
        assertThat(strategy.canHandle("Some other error message"))
                .isFalse();
        assertThat(strategy.canHandle(null)).isFalse();
        assertThat(strategy.canHandle("")).isFalse();
    }

    @Test
    void canHandle_shouldReturnFalseForMalformedJUnitErrors() {
        JUnitComparisonFailureStrategy strategy = new JUnitComparisonFailureStrategy(null);
        assertThat(strategy.canHandle("org.junit.ComparisonFailure: expected foo but was bar"))
                .isFalse();
        assertThat(strategy.canHandle("org.junit.ComparisonFailure: expected:<foo> but was bar"))
                .isFalse();
        assertThat(strategy.canHandle("org.junit.ComparisonFailure: expected foo but was:<bar>"))
                .isFalse();
    }

    @Test
    void parseStackTraceElement_shouldParseValidLine() {
        JUnitComparisonFailureStrategy strategy = new JUnitComparisonFailureStrategy(null);
        String line = "at com.example.MyTest.testSomething(MyTest.java:42)";
        StackTraceElement element = strategy.parseStackTraceElement(line);
        assertThat(element).isNotNull();
        assertThat(element.getClassName()).isEqualTo("com.example.MyTest");
        assertThat(element.getMethodName()).isEqualTo("testSomething");
        assertThat(element.getFileName()).isEqualTo("MyTest.java");
        assertThat(element.getLineNumber()).isEqualTo(42);
    }

    @Test
    void parseStackTraceElement_shouldReturnNullForInvalidLine() {
        JUnitComparisonFailureStrategy strategy = new JUnitComparisonFailureStrategy(null);
        // Should return a StackTraceElement with null fileName and -1 lineNumber for missing info
        StackTraceElement element1 = strategy.parseStackTraceElement("not a stack trace line");
        assertThat(element1).isNull();
        StackTraceElement element2 = strategy.parseStackTraceElement("at com.example.MyTest.testSomething()");
        assertThat(element2).isNotNull();
        assertThat(element2.getFileName()).isNull();
        assertThat(element2.getLineNumber()).isEqualTo(-1);
        StackTraceElement element3 = strategy.parseStackTraceElement("at com.example.MyTest.testSomething(MyTest.java)");
        assertThat(element3).isNotNull();
        assertThat(element3.getFileName()).isEqualTo("MyTest.java");
        assertThat(element3.getLineNumber()).isEqualTo(-1);
    }

    @Test
    void parse_shouldExtractExpectedAndActualValues() {
        JUnitComparisonFailureStrategy strategy = new JUnitComparisonFailureStrategy(null);
        String output = "org.junit.ComparisonFailure: expected:<foo> but was:<bar>\n" +
                       "    at com.example.MyTest.testSomething(MyTest.java:42)";
        
        var failureInfo = strategy.parse(output);
        
        assertThat(failureInfo.getExpectedValue()).isEqualTo("foo");
        assertThat(failureInfo.getActualValue()).isEqualTo("bar");
        assertThat(failureInfo.getAssertionType()).isEqualTo("JUNIT_COMPARISON");
        assertThat(failureInfo.getErrorMessage()).contains("expected <foo> but was <bar>");
        assertThat(failureInfo.getParsingStrategy()).isEqualTo("JUnitComparisonFailureStrategy");
        assertThat(failureInfo.getStackTrace()).isEqualTo(output);
    }

    @Test
    void parse_shouldHandleComplexValues() {
        JUnitComparisonFailureStrategy strategy = new JUnitComparisonFailureStrategy(null);
        String output = "org.junit.ComparisonFailure: expected:<Hello\nWorld> but was:<Hello>";
        
        var failureInfo = strategy.parse(output);
        
        assertThat(failureInfo.getExpectedValue()).isEqualTo("Hello\nWorld");
        assertThat(failureInfo.getActualValue()).isEqualTo("Hello");
    }

    @Test
    void parse_shouldThrowExceptionForNullInput() {
        JUnitComparisonFailureStrategy strategy = new JUnitComparisonFailureStrategy(null);
        assertThatThrownBy(() -> strategy.parse(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("testOutput cannot be null");
    }

    @Test
    void parse_shouldThrowExceptionForNonMatchingInput() {
        JUnitComparisonFailureStrategy strategy = new JUnitComparisonFailureStrategy(null);
        assertThatThrownBy(() -> strategy.parse("Some other error message"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("No JUnit ComparisonFailure found in test output");
    }

    @Test
    void getPriority_shouldReturn100() {
        JUnitComparisonFailureStrategy strategy = new JUnitComparisonFailureStrategy(null);
        assertThat(strategy.getPriority()).isEqualTo(100);
    }

    @Test
    void getStrategyName_shouldReturnCorrectName() {
        JUnitComparisonFailureStrategy strategy = new JUnitComparisonFailureStrategy(null);
        assertThat(strategy.getStrategyName()).isEqualTo("JUnitComparisonFailureStrategy");
    }
} 