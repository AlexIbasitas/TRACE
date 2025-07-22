package com.trace.test.listeners;

import com.intellij.execution.testframework.sm.runner.SMTestProxy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TestOutputCaptureListener.
 * Tests the capture and retrieval of test output including full stack traces.
 */
@DisplayName("TestOutputCaptureListener")
class TestOutputCaptureListenerUnitTest {

    @Mock
    private SMTestProxy testProxy;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Clear any existing captured output before each test
        TestOutputCaptureListener.clearAllCapturedOutput();
    }

    @Test
    @DisplayName("should capture test output line")
    void shouldCaptureTestOutputLine() {
        // Given
        when(testProxy.getName()).thenReturn("TestScenario");
        String outputLine = "This is a test output line";

        // When
        TestOutputCaptureListener.captureTestOutput(testProxy, outputLine);

        // Then
        String capturedOutput = TestOutputCaptureListener.getCapturedOutput(testProxy);
        assertNotNull(capturedOutput);
        assertTrue(capturedOutput.contains(outputLine));
    }

    @Test
    @DisplayName("should capture multiple output lines")
    void shouldCaptureMultipleOutputLines() {
        // Given
        when(testProxy.getName()).thenReturn("TestScenario");
        String line1 = "First output line";
        String line2 = "Second output line";
        String line3 = "Third output line";

        // When
        TestOutputCaptureListener.captureTestOutput(testProxy, line1);
        TestOutputCaptureListener.captureTestOutput(testProxy, line2);
        TestOutputCaptureListener.captureTestOutput(testProxy, line3);

        // Then
        String capturedOutput = TestOutputCaptureListener.getCapturedOutput(testProxy);
        assertNotNull(capturedOutput);
        assertTrue(capturedOutput.contains(line1));
        assertTrue(capturedOutput.contains(line2));
        assertTrue(capturedOutput.contains(line3));
    }

    @Test
    @DisplayName("should capture full stack trace")
    void shouldCaptureFullStackTrace() {
        // Given
        when(testProxy.getName()).thenReturn("TestScenario");
        String stackTrace = """
            java.lang.AssertionError: Expected: true but: was false
                at org.junit.Assert.fail(Assert.java:88)
                at org.junit.Assert.assertTrue(Assert.java:41)
                at org.junit.Assert.assertTrue(Assert.java:52)
                at com.example.MyTest.testSomething(MyTest.java:25)
                at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
                at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
                at java.base/java.lang.reflect.Method.invoke(Method.java:568)
                at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:50)
                at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
                at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:47)
                at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
                at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:325)
                at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:78)
                at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:57)
                at org.junit.runners.ParentRunner$3.run(ParentRunner.java:290)
                at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:71)
                at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:288)
                at org.junit.runners.ParentRunner.access$000(ParentRunner.java:58)
                at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:268)
                at org.junit.runners.ParentRunner.run(ParentRunner.java:363)
                at org.junit.runners.Suite.runChild(Suite.java:128)
                at org.junit.runners.Suite.runChild(Suite.java:27)
                at org.junit.runners.ParentRunner$3.run(ParentRunner.java:290)
                at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:71)
                at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:288)
                at org.junit.runners.ParentRunner.access$000(ParentRunner.java:58)
                at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:268)
                at org.junit.runners.ParentRunner.run(ParentRunner.java:363)
                at org.junit.runner.JUnitCore.run(JUnitCore.java:137)
                at org.junit.runner.JUnitCore.runMain(JUnitCore.java:76)
                at org.junit.runner.JUnitCore.main(JUnitCore.java:45)
            """;

        // When
        TestOutputCaptureListener.captureTestOutput(testProxy, stackTrace);

        // Then
        String capturedOutput = TestOutputCaptureListener.getCapturedOutput(testProxy);
        assertNotNull(capturedOutput);
        assertTrue(capturedOutput.contains("java.lang.AssertionError"));
        assertTrue(capturedOutput.contains("at com.example.MyTest.testSomething(MyTest.java:25)"));
        assertTrue(capturedOutput.contains("at org.junit.Assert.assertTrue"));
    }

    @Test
    @DisplayName("should capture error output from test proxy")
    void shouldCaptureErrorOutputFromTestProxy() {
        // Given
        when(testProxy.getName()).thenReturn("TestScenario");
        String errorMessage = "java.lang.RuntimeException: Something went wrong\n" +
                            "at com.example.MyTest.testMethod(MyTest.java:10)";
        when(testProxy.getErrorMessage()).thenReturn(errorMessage);

        // When
        TestOutputCaptureListener.captureTestErrorOutput(testProxy);

        // Then
        String capturedOutput = TestOutputCaptureListener.getCapturedOutput(testProxy);
        assertNotNull(capturedOutput);
        assertTrue(capturedOutput.contains("=== FULL ERROR OUTPUT ==="));
        assertTrue(capturedOutput.contains("java.lang.RuntimeException: Something went wrong"));
    }

    @Test
    @DisplayName("should handle null test proxy gracefully")
    void shouldHandleNullTestProxyGracefully() {
        // When & Then - should not throw exception
        assertDoesNotThrow(() -> {
            TestOutputCaptureListener.captureTestOutput(null, "Some output");
            TestOutputCaptureListener.captureTestErrorOutput(null);
            TestOutputCaptureListener.getCapturedOutput(null);
        });
    }

    @Test
    @DisplayName("should handle null or empty output lines")
    void shouldHandleNullOrEmptyOutputLines() {
        // Given
        when(testProxy.getName()).thenReturn("TestScenario");

        // When
        TestOutputCaptureListener.captureTestOutput(testProxy, null);
        TestOutputCaptureListener.captureTestOutput(testProxy, "");
        TestOutputCaptureListener.captureTestOutput(testProxy, "   ");

        // Then
        String capturedOutput = TestOutputCaptureListener.getCapturedOutput(testProxy);
        // Should be null or empty since we only captured null/empty lines
        assertTrue(capturedOutput == null || capturedOutput.trim().isEmpty());
    }

    @Test
    @DisplayName("should clear captured output for specific test")
    void shouldClearCapturedOutputForSpecificTest() {
        // Given
        when(testProxy.getName()).thenReturn("TestScenario");
        TestOutputCaptureListener.captureTestOutput(testProxy, "Some output");

        // When
        TestOutputCaptureListener.clearCapturedOutput(testProxy);

        // Then
        String capturedOutput = TestOutputCaptureListener.getCapturedOutput(testProxy);
        assertNull(capturedOutput);
    }

    @Test
    @DisplayName("should clear all captured output")
    void shouldClearAllCapturedOutput() {
        // Given
        SMTestProxy test1 = mock(SMTestProxy.class);
        SMTestProxy test2 = mock(SMTestProxy.class);
        when(test1.getName()).thenReturn("Test1");
        when(test2.getName()).thenReturn("Test2");

        TestOutputCaptureListener.captureTestOutput(test1, "Output 1");
        TestOutputCaptureListener.captureTestOutput(test2, "Output 2");

        // When
        TestOutputCaptureListener.clearAllCapturedOutput();

        // Then
        assertNull(TestOutputCaptureListener.getCapturedOutput(test1));
        assertNull(TestOutputCaptureListener.getCapturedOutput(test2));
        assertEquals(0, TestOutputCaptureListener.getCapturedOutputCount());
    }

    @Test
    @DisplayName("should get correct captured output count")
    void shouldGetCorrectCapturedOutputCount() {
        // Given
        SMTestProxy test1 = mock(SMTestProxy.class);
        SMTestProxy test2 = mock(SMTestProxy.class);
        when(test1.getName()).thenReturn("Test1");
        when(test2.getName()).thenReturn("Test2");

        // When
        TestOutputCaptureListener.captureTestOutput(test1, "Output 1");
        TestOutputCaptureListener.captureTestOutput(test2, "Output 2");

        // Then
        assertEquals(2, TestOutputCaptureListener.getCapturedOutputCount());
    }

    @Test
    @DisplayName("should preserve line endings correctly")
    void shouldPreserveLineEndingsCorrectly() {
        // Given
        when(testProxy.getName()).thenReturn("TestScenario");
        String outputWithNewline = "Line 1\n";
        String outputWithoutNewline = "Line 2";

        // When
        TestOutputCaptureListener.captureTestOutput(testProxy, outputWithNewline);
        TestOutputCaptureListener.captureTestOutput(testProxy, outputWithoutNewline);

        // Then
        String capturedOutput = TestOutputCaptureListener.getCapturedOutput(testProxy);
        assertNotNull(capturedOutput);
        // Should have both lines, separated by a newline
        assertTrue(capturedOutput.contains("Line 1\n"));
        assertTrue(capturedOutput.contains("Line 2"));
        assertEquals("Line 1\nLine 2", capturedOutput);
    }
} 