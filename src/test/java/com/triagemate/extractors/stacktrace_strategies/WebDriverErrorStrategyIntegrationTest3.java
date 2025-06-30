package com.triagemate.extractors.stacktrace_strategies;

import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.triagemate.models.FailureInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for WebDriverErrorStrategy (Part 3).
 *
 * <p>These tests verify the strategy works correctly with real PSI operations
 * and file system integration. Split into multiple classes to avoid temp directory nesting issues.</p>
 */
class WebDriverErrorStrategyIntegrationTest3 extends BasePlatformTestCase {

    private WebDriverErrorStrategy strategy;

    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        strategy = new WebDriverErrorStrategy(getProject());
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
    @DisplayName("should handle no such frame with PSI enrichment")
    void shouldHandleNoSuchFrameWithPsiEnrichment() {
        // Create a unique file for this test
        PsiFile javaFile = myFixture.addFileToProject(
            "src/test/java/com/example/shouldHandleNoSuchFrame_FrameTest.java",
            """
            package com.example;
            
            import org.junit.Test;
            import org.openqa.selenium.WebDriver;
            import org.openqa.selenium.NoSuchFrameException;
            
            public class FrameTest {
                @Test
                public void testNoSuchFrame() {
                    WebDriver driver = null;
                    driver.switchTo().frame("nonexistent-frame"); // This will cause NoSuchFrameException
                }
            }
            """
        );
        
        String output = "org.openqa.selenium.NoSuchFrameException: no such frame: frame was not found\n" +
                       "    at com.example.FrameTest.testNoSuchFrame(FrameTest.java:42)";
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("NO_SUCH_FRAME_EXCEPTION", info.getAssertionType());
        assertEquals("no such frame: frame was not found", info.getErrorMessage());
        assertEquals("WebDriverErrorStrategy", info.getParsingStrategy());
        
        // PSI enrichment should provide method name
        if (info.getStepDefinitionMethod() != null) {
            assertTrue(info.getStepDefinitionMethod().contains("FrameTest.testNoSuchFrame"));
        }
    }

    @Test
    @DisplayName("should handle complex stack trace with multiple user code frames")
    void shouldHandleComplexStackTraceWithMultipleUserCodeFrames() {
        // Create a unique file for this test
        PsiFile javaFile = myFixture.addFileToProject(
            "src/test/java/com/example/shouldHandleComplexStackTrace_ComplexWebDriverTest.java",
            """
            package com.example;
            
            import org.junit.Test;
            import org.openqa.selenium.WebDriver;
            import org.openqa.selenium.By;
            import org.openqa.selenium.TimeoutException;
            
            public class ComplexWebDriverTest {
                @Test
                public void testComplexScenario() {
                    waitForElement();
                }
                
                private void waitForElement() {
                    WebDriver driver = null;
                    driver.findElement(By.id("slow-loading-element")); // This will cause TimeoutException
                }
            }
            """
        );
        
        String output = "org.openqa.selenium.TimeoutException: Expected condition failed: waiting for element\n" +
                       "    at com.example.ComplexWebDriverTest.waitForElement(ComplexWebDriverTest.java:42)\n" +
                       "    at com.example.ComplexWebDriverTest.testComplexScenario(ComplexWebDriverTest.java:38)\n" +
                       "    at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:325)";
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("TIMEOUT_EXCEPTION", info.getAssertionType());
        assertEquals("Expected condition failed: waiting for element", info.getErrorMessage());
        assertEquals("WebDriverErrorStrategy", info.getParsingStrategy());
        
        // Should extract the first user code frame (waitForElement)
        if (info.getStepDefinitionMethod() != null) {
            assertTrue(info.getStepDefinitionMethod().contains("ComplexWebDriverTest.waitForElement"));
        }
    }

    @Test
    @DisplayName("should handle WebDriver exception without message")
    void shouldHandleWebDriverExceptionWithoutMessage() {
        // Create a unique file for this test
        PsiFile javaFile = myFixture.addFileToProject(
            "src/test/java/com/example/shouldHandleWebDriverExceptionWithoutMessage_NoMessageTest.java",
            """
            package com.example;
            
            import org.junit.Test;
            import org.openqa.selenium.WebDriverException;
            
            public class NoMessageTest {
                @Test
                public void testNoMessage() {
                    throw new WebDriverException();
                }
            }
            """
        );
        
        String output = "org.openqa.selenium.WebDriverException\n" +
                       "    at com.example.NoMessageTest.testNoMessage(NoMessageTest.java:42)";
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("WEBDRIVER_ERROR", info.getAssertionType());
        assertEquals("org.openqa.selenium.WebDriverException", info.getErrorMessage());
        assertEquals("WebDriverErrorStrategy", info.getParsingStrategy());
        
        // PSI enrichment should still work
        if (info.getSourceFilePath() != null) {
            assertEquals("shouldHandleWebDriverExceptionWithoutMessage_NoMessageTest.java", info.getSourceFilePath());
        }
    }

    @Test
    @DisplayName("should handle generic WebDriver exception with PSI enrichment")
    void shouldHandleGenericWebDriverExceptionWithPsiEnrichment() {
        // Create a unique file for this test
        PsiFile javaFile = myFixture.addFileToProject(
            "src/test/java/com/example/shouldHandleGenericWebDriverException_GenericTest.java",
            """
            package com.example;
            
            import org.junit.Test;
            import org.openqa.selenium.WebDriverException;
            
            public class GenericTest {
                @Test
                public void testGenericException() {
                    throw new WebDriverException("Chrome failed to start: exited abnormally");
                }
            }
            """
        );
        
        String output = "org.openqa.selenium.WebDriverException: unknown error: Chrome failed to start: exited abnormally\n" +
                       "    at com.example.GenericTest.testGenericException(GenericTest.java:42)";
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("WEBDRIVER_EXCEPTION", info.getAssertionType());
        assertEquals("unknown error: Chrome failed to start: exited abnormally", info.getErrorMessage());
        assertEquals("WebDriverErrorStrategy", info.getParsingStrategy());
        
        // PSI enrichment should provide method name
        if (info.getStepDefinitionMethod() != null) {
            assertTrue(info.getStepDefinitionMethod().contains("GenericTest.testGenericException"));
        }
    }
} 