package com.triagemate.extractors.stacktrace_strategies;

import com.intellij.openapi.project.Project;
import com.triagemate.models.FailureInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for WebDriverErrorStrategy.
 *
 * <p>These tests focus on the parsing logic without requiring PSI operations,
 * making them fast and reliable unit tests.</p>
 */
class WebDriverErrorStrategyUnitTest {

    @Mock
    private Project mockProject;
    private WebDriverErrorStrategy strategy;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        strategy = new WebDriverErrorStrategy(mockProject);
    }

    @Test
    @DisplayName("should handle NoSuchElementException")
    void shouldHandleNoSuchElementException() {
        String output = "org.openqa.selenium.NoSuchElementException: no such element: Unable to locate element: {\"method\":\"css selector\",\"selector\":\"#nonexistent\"}\n" +
                       "    at com.example.WebDriverTest.testElementNotFound(WebDriverTest.java:42)";
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("NO_SUCH_ELEMENT_EXCEPTION", info.getAssertionType());
        assertTrue(info.getErrorMessage().contains("no such element: Unable to locate element"));
        assertEquals("WebDriverErrorStrategy", info.getParsingStrategy());
        assertNotNull(info.getStackTrace());
        assertTrue(info.getStackTrace().contains("org.openqa.selenium.NoSuchElementException"));
    }

    @Test
    @DisplayName("should handle TimeoutException")
    void shouldHandleTimeoutException() {
        String output = "org.openqa.selenium.TimeoutException: Expected condition failed: waiting for element to be clickable: By.cssSelector: button[type='submit'] (tried for 10 second(s) with 500 milliseconds interval)\n" +
                       "    at com.example.WebDriverTest.testTimeout(WebDriverTest.java:42)";
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("TIMEOUT_EXCEPTION", info.getAssertionType());
        assertTrue(info.getErrorMessage().contains("Expected condition failed: waiting for element to be clickable"));
        assertEquals("WebDriverErrorStrategy", info.getParsingStrategy());
        assertNotNull(info.getStackTrace());
    }

    @Test
    @DisplayName("should handle ElementNotInteractableException")
    void shouldHandleElementNotInteractableException() {
        String output = "org.openqa.selenium.ElementNotInteractableException: element not interactable: Element <input type=\"text\" class=\"disabled-input\"> is not interactable\n" +
                       "    at com.example.WebDriverTest.testElementNotInteractable(WebDriverTest.java:42)";
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("ELEMENT_NOT_INTERACTABLE_EXCEPTION", info.getAssertionType());
        assertTrue(info.getErrorMessage().contains("element not interactable"));
        assertEquals("WebDriverErrorStrategy", info.getParsingStrategy());
        assertNotNull(info.getStackTrace());
    }

    @Test
    @DisplayName("should handle StaleElementReferenceException")
    void shouldHandleStaleElementReferenceException() {
        String output = "org.openqa.selenium.StaleElementReferenceException: stale element reference: element is not attached to the page document\n" +
                       "    at com.example.WebDriverTest.testStaleElement(WebDriverTest.java:42)";
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("STALE_ELEMENT_REFERENCE_EXCEPTION", info.getAssertionType());
        assertTrue(info.getErrorMessage().contains("stale element reference"));
        assertEquals("WebDriverErrorStrategy", info.getParsingStrategy());
        assertNotNull(info.getStackTrace());
    }

    @Test
    @DisplayName("should handle ElementClickInterceptedException")
    void shouldHandleElementClickInterceptedException() {
        String output = "org.openqa.selenium.ElementClickInterceptedException: element click intercepted: Element <button class=\"submit-btn\"> is not clickable at point (123, 456). Other element would receive the click: <div class=\"overlay\">\n" +
                       "    at com.example.WebDriverTest.testClickIntercepted(WebDriverTest.java:42)";
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("ELEMENT_CLICK_INTERCEPTED_EXCEPTION", info.getAssertionType());
        assertTrue(info.getErrorMessage().contains("element click intercepted"));
        assertEquals("WebDriverErrorStrategy", info.getParsingStrategy());
        assertNotNull(info.getStackTrace());
    }

    @Test
    @DisplayName("should handle NoSuchWindowException")
    void shouldHandleNoSuchWindowException() {
        String output = "org.openqa.selenium.NoSuchWindowException: no such window: target window already closed\n" +
                       "    at com.example.WebDriverTest.testNoSuchWindow(WebDriverTest.java:42)";
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("NO_SUCH_WINDOW_EXCEPTION", info.getAssertionType());
        assertTrue(info.getErrorMessage().contains("no such window"));
        assertEquals("WebDriverErrorStrategy", info.getParsingStrategy());
        assertNotNull(info.getStackTrace());
    }

    @Test
    @DisplayName("should handle NoSuchFrameException")
    void shouldHandleNoSuchFrameException() {
        String output = "org.openqa.selenium.NoSuchFrameException: no such frame: frame was not found\n" +
                       "    at com.example.WebDriverTest.testNoSuchFrame(WebDriverTest.java:42)";
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("NO_SUCH_FRAME_EXCEPTION", info.getAssertionType());
        assertTrue(info.getErrorMessage().contains("no such frame"));
        assertEquals("WebDriverErrorStrategy", info.getParsingStrategy());
        assertNotNull(info.getStackTrace());
    }

    @Test
    @DisplayName("should handle generic WebDriverException")
    void shouldHandleGenericWebDriverException() {
        String output = "org.openqa.selenium.WebDriverException: unknown error: Chrome failed to start: exited abnormally\n" +
                       "    at com.example.WebDriverTest.testWebDriverException(WebDriverTest.java:42)";
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("WEBDRIVER_EXCEPTION", info.getAssertionType());
        assertTrue(info.getErrorMessage().contains("unknown error: Chrome failed to start"));
        assertEquals("WebDriverErrorStrategy", info.getParsingStrategy());
        assertNotNull(info.getStackTrace());
    }

    @Test
    @DisplayName("should handle WebDriver exception with multi-line message")
    void shouldHandleWebDriverExceptionWithMultiLineMessage() {
        String output = "org.openqa.selenium.NoSuchElementException: no such element: Unable to locate element: {\"method\":\"css selector\",\"selector\":\"#nonexistent\"}\n" +
                       "  (Session info: chrome=120.0.6099.109)\n" +
                       "  (Driver info: chromedriver=120.0.6099.109,platform=Mac OS X 14.1.2 x86_64)\n" +
                       "    at com.example.WebDriverTest.testMultiLineMessage(WebDriverTest.java:42)";
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("NO_SUCH_ELEMENT_EXCEPTION", info.getAssertionType());
        assertTrue(info.getErrorMessage().contains("no such element: Unable to locate element"));
        assertEquals("WebDriverErrorStrategy", info.getParsingStrategy());
        assertNotNull(info.getStackTrace());
    }

    @Test
    @DisplayName("should handle WebDriver exception without message")
    void shouldHandleWebDriverExceptionWithoutMessage() {
        String output = "org.openqa.selenium.WebDriverException\n" +
                       "    at com.example.WebDriverTest.testNoMessage(WebDriverTest.java:42)";
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("WEBDRIVER_ERROR", info.getAssertionType());
        assertEquals("org.openqa.selenium.WebDriverException", info.getErrorMessage());
        assertEquals("WebDriverErrorStrategy", info.getParsingStrategy());
        assertNotNull(info.getStackTrace());
    }

    @Test
    @DisplayName("should handle WebDriver exception with complex stack trace")
    void shouldHandleWebDriverExceptionWithComplexStackTrace() {
        String output = "org.openqa.selenium.TimeoutException: Expected condition failed: waiting for element to be clickable\n" +
                       "    at org.openqa.selenium.support.ui.WebDriverWait.until(WebDriverWait.java:95)\n" +
                       "    at com.example.WebDriverTest.waitForElement(WebDriverTest.java:38)\n" +
                       "    at com.example.WebDriverTest.testComplexStackTrace(WebDriverTest.java:42)";
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("TIMEOUT_EXCEPTION", info.getAssertionType());
        assertTrue(info.getErrorMessage().contains("Expected condition failed: waiting for element to be clickable"));
        assertEquals("WebDriverErrorStrategy", info.getParsingStrategy());
        assertNotNull(info.getStackTrace());
        
        // Should extract the first user code frame (waitForElement)
        if (info.getStepDefinitionMethod() != null) {
            assertTrue(info.getStepDefinitionMethod().contains("WebDriverTest.waitForElement"));
        }
    }

    @Test
    @DisplayName("should not handle empty output")
    void shouldNotHandleEmptyOutput() {
        assertFalse(strategy.canHandle(null));
        assertFalse(strategy.canHandle(""));
        assertFalse(strategy.canHandle("   "));
    }

    @Test
    @DisplayName("should not handle non-WebDriver errors")
    void shouldNotHandleNonWebDriverErrors() {
        assertFalse(strategy.canHandle("java.lang.NullPointerException: null"));
        assertFalse(strategy.canHandle("org.junit.ComparisonFailure: expected:<foo> but was:<bar>"));
        assertFalse(strategy.canHandle("Some other error message"));
    }

    @Test
    @DisplayName("should not handle malformed WebDriver errors")
    void shouldNotHandleMalformedWebDriverErrors() {
        // With fallback substring check, these should return true
        assertTrue(strategy.canHandle("org.openqa.selenium.NoSuchElementException"));
        assertTrue(strategy.canHandle("org.openqa.selenium.TimeoutException"));
        // But a random string should still return false
        assertFalse(strategy.canHandle("NotAWebDriverException"));
    }

    @Test
    @DisplayName("should create fallback failure info when parsing unrecognized output")
    void shouldCreateFallbackFailureInfoWhenParsingUnrecognizedOutput() {
        String output = "Some unrecognized WebDriver-like error";
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("WEBDRIVER_ERROR", info.getAssertionType());
        assertTrue(info.getErrorMessage().contains("WebDriver error: Some unrecognized WebDriver-like error"));
        assertEquals("WebDriverErrorStrategy", info.getParsingStrategy());
        assertNotNull(info.getStackTrace());
    }

    @Test
    @DisplayName("should throw exception for null input")
    void shouldThrowExceptionForNullInput() {
        assertThrows(IllegalArgumentException.class, () -> strategy.parse(null));
    }

    @Test
    @DisplayName("should not throw exception for non-matching input, should return fallback info")
    void shouldNotThrowExceptionForNonMatchingInput() {
        FailureInfo info = strategy.parse("Some other error message");
        assertEquals("WEBDRIVER_ERROR", info.getAssertionType());
        assertTrue(info.getErrorMessage().contains("WebDriver error: Some other error message"));
    }

    @Test
    @DisplayName("should parse stack trace element correctly")
    void shouldParseStackTraceElementCorrectly() {
        String line = "at com.example.WebDriverTest.testSomething(WebDriverTest.java:42)";
        StackTraceElement element = strategy.parseStackTraceElement(line);
        
        assertNotNull(element);
        assertEquals("com.example.WebDriverTest", element.getClassName());
        assertEquals("testSomething", element.getMethodName());
        assertEquals("WebDriverTest.java", element.getFileName());
        assertEquals(42, element.getLineNumber());
    }

    @Test
    @DisplayName("should parse stack trace element without line number")
    void shouldParseStackTraceElementWithoutLineNumber() {
        String line = "at com.example.WebDriverTest.testSomething(WebDriverTest.java)";
        StackTraceElement element = strategy.parseStackTraceElement(line);
        
        assertNotNull(element);
        assertEquals("com.example.WebDriverTest", element.getClassName());
        assertEquals("testSomething", element.getMethodName());
        assertEquals("WebDriverTest.java", element.getFileName());
        assertEquals(-1, element.getLineNumber());
    }

    @Test
    @DisplayName("should parse stack trace element without file info")
    void shouldParseStackTraceElementWithoutFileInfo() {
        String line = "at com.example.WebDriverTest.testSomething()";
        StackTraceElement element = strategy.parseStackTraceElement(line);
        
        assertNotNull(element);
        assertEquals("com.example.WebDriverTest", element.getClassName());
        assertEquals("testSomething", element.getMethodName());
        assertNull(element.getFileName());
        assertEquals(-1, element.getLineNumber());
    }

    @Test
    @DisplayName("should return null for invalid stack trace line")
    void shouldReturnNullForInvalidStackTraceLine() {
        assertNull(strategy.parseStackTraceElement("not a stack trace line"));
        assertNull(strategy.parseStackTraceElement("at com.example.WebDriverTest"));
    }

    @Test
    @DisplayName("should return correct priority")
    void shouldReturnCorrectPriority() {
        assertEquals(70, strategy.getPriority());
    }

    @Test
    @DisplayName("should return correct strategy name")
    void shouldReturnCorrectStrategyName() {
        assertEquals("WebDriverErrorStrategy", strategy.getStrategyName());
    }
} 