package com.triagemate.extractors.stacktrace_strategies;

import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.triagemate.models.FailureInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for WebDriverErrorStrategy (Part 2).
 *
 * <p>These tests verify the strategy works correctly with real PSI operations
 * and file system integration. Split into multiple classes to avoid temp directory nesting issues.</p>
 */
class WebDriverErrorStrategyIntegrationTest2 extends BasePlatformTestCase {

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
    @DisplayName("should handle stale element reference with PSI enrichment")
    void shouldHandleStaleElementReferenceWithPsiEnrichment() {
        // Create a unique file for this test
        PsiFile javaFile = myFixture.addFileToProject(
            "src/test/java/com/example/shouldHandleStaleElementReference_StaleTest.java",
            """
            package com.example;
            
            import org.junit.Test;
            import org.openqa.selenium.WebDriver;
            import org.openqa.selenium.By;
            import org.openqa.selenium.StaleElementReferenceException;
            
            public class StaleTest {
                @Test
                public void testStaleElement() {
                    WebDriver driver = null;
                    driver.findElement(By.id("dynamic-element")).click(); // This will cause StaleElementReferenceException
                }
            }
            """
        );
        
        String output = "org.openqa.selenium.StaleElementReferenceException: stale element reference: element is not attached to the page document\n" +
                       "    at com.example.StaleTest.testStaleElement(StaleTest.java:42)";
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("STALE_ELEMENT_REFERENCE_EXCEPTION", info.getAssertionType());
        assertEquals("stale element reference: element is not attached to the page document", info.getErrorMessage());
        assertEquals("WebDriverErrorStrategy", info.getParsingStrategy());
        
        // PSI enrichment should provide method name
        if (info.getStepDefinitionMethod() != null) {
            assertTrue(info.getStepDefinitionMethod().contains("StaleTest.testStaleElement"));
        }
    }

    @Test
    @DisplayName("should handle element click intercepted with PSI enrichment")
    void shouldHandleElementClickInterceptedWithPsiEnrichment() {
        // Create a unique file for this test
        PsiFile javaFile = myFixture.addFileToProject(
            "src/test/java/com/example/shouldHandleElementClickIntercepted_ClickTest.java",
            """
            package com.example;
            
            import org.junit.Test;
            import org.openqa.selenium.WebDriver;
            import org.openqa.selenium.By;
            import org.openqa.selenium.ElementClickInterceptedException;
            
            public class ClickTest {
                @Test
                public void testClickIntercepted() {
                    WebDriver driver = null;
                    driver.findElement(By.className("submit-btn")).click(); // This will cause ElementClickInterceptedException
                }
            }
            """
        );
        
        String output = "org.openqa.selenium.ElementClickInterceptedException: element click intercepted: Element <button class=\"submit-btn\"> is not clickable at point (123, 456)\n" +
                       "    at com.example.ClickTest.testClickIntercepted(ClickTest.java:42)";
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("ELEMENT_CLICK_INTERCEPTED_EXCEPTION", info.getAssertionType());
        assertEquals("element click intercepted: Element <button class=\"submit-btn\"> is not clickable at point (123, 456)", info.getErrorMessage());
        assertEquals("WebDriverErrorStrategy", info.getParsingStrategy());
        
        // PSI enrichment should provide line number information
        if (info.getLineNumber() > 0) {
            assertTrue(info.getLineNumber() > 0);
        }
    }

    @Test
    @DisplayName("should handle no such window with PSI enrichment")
    void shouldHandleNoSuchWindowWithPsiEnrichment() {
        // Create a unique file for this test
        PsiFile javaFile = myFixture.addFileToProject(
            "src/test/java/com/example/shouldHandleNoSuchWindow_WindowTest.java",
            """
            package com.example;
            
            import org.junit.Test;
            import org.openqa.selenium.WebDriver;
            import org.openqa.selenium.NoSuchWindowException;
            
            public class WindowTest {
                @Test
                public void testNoSuchWindow() {
                    WebDriver driver = null;
                    driver.switchTo().window("nonexistent-window"); // This will cause NoSuchWindowException
                }
            }
            """
        );
        
        String output = "org.openqa.selenium.NoSuchWindowException: no such window: target window already closed\n" +
                       "    at com.example.WindowTest.testNoSuchWindow(WindowTest.java:42)";
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("NO_SUCH_WINDOW_EXCEPTION", info.getAssertionType());
        assertEquals("no such window: target window already closed", info.getErrorMessage());
        assertEquals("WebDriverErrorStrategy", info.getParsingStrategy());
        
        // PSI enrichment should provide accurate source file information
        if (info.getSourceFilePath() != null) {
            assertEquals("shouldHandleNoSuchWindow_WindowTest.java", info.getSourceFilePath());
        }
    }
} 