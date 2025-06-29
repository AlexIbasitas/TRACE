package com.triagemate.extractors.stacktrace_strategies;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ConfigurationErrorStrategyUnitTest {

    @Test
    void canHandle_shouldReturnTrueForKnownConfigurationErrors() {
        ConfigurationErrorStrategy strategy = new ConfigurationErrorStrategy(null);
        assertThat(strategy.canHandle("java.io.FileNotFoundException: config.properties (No such file or directory)"))
                .isTrue();
        assertThat(strategy.canHandle("java.lang.ClassNotFoundException: com.example.MissingClass"))
                .isTrue();
        assertThat(strategy.canHandle("Resource not found: missing.yaml"))
                .isTrue();
        assertThat(strategy.canHandle("Property loading error: missing.property"))
                .isTrue();
    }

    @Test
    void canHandle_shouldReturnFalseForNonConfigurationErrors() {
        ConfigurationErrorStrategy strategy = new ConfigurationErrorStrategy(null);
        assertThat(strategy.canHandle("java.lang.AssertionError: expected [true] but found [false]"))
                .isFalse();
        assertThat(strategy.canHandle(null)).isFalse();
        assertThat(strategy.canHandle("")).isFalse();
    }

    @Test
    void parseStackTraceElement_shouldParseValidLine() {
        ConfigurationErrorStrategy strategy = new ConfigurationErrorStrategy(null);
        // Use reflection to access package-private method for testability
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
        ConfigurationErrorStrategy strategy = new ConfigurationErrorStrategy(null);
        assertThat(strategy.parseStackTraceElement("not a stack trace line")).isNull();
    }

    @Test
    void extractFailedStepText_shouldExtractFileNameOrResource() throws Exception {
        ConfigurationErrorStrategy strategy = new ConfigurationErrorStrategy(null);
        // Use reflection to access private method for testability
        java.lang.reflect.Method method = ConfigurationErrorStrategy.class.getDeclaredMethod("extractFailedStepText", String.class);
        method.setAccessible(true);
        assertThat((String) method.invoke(strategy, "FileNotFoundException: config.properties (No such file or directory)"))
                .contains("config.properties");
        assertThat((String) method.invoke(strategy, "ClassNotFoundException: com.example.MissingClass"))
                .contains("com.example.MissingClass");
        assertThat((String) method.invoke(strategy, "BeanCreationException: Error creating bean 'myBean'"))
                .contains("myBean");
        assertThat((String) method.invoke(strategy, "JsonParseException: Unexpected character at line 1"))
                .contains("Unexpected character at line 1");
    }
} 