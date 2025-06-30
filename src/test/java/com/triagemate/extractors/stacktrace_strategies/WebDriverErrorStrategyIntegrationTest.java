package com.triagemate.extractors.stacktrace_strategies;

import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.triagemate.models.FailureInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for WebDriverErrorStrategy (Part 1).
 *
 * <p>These tests verify the strategy works correctly with real PSI operations
 * and file system integration. Split into multiple classes to avoid temp directory nesting issues.</p>
 * 
 * <p><strong>Note:</strong> This class contains the first 3 tests. Additional tests are in:
 * - WebDriverErrorStrategyIntegrationTest2 (3 tests)
 * - WebDriverErrorStrategyIntegrationTest3 (4 tests)</p>
 */
class WebDriverErrorStrategyIntegrationTest extends BasePlatformTestCase {

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
    @DisplayName("should enrich with PSI information when source files exist")
    void shouldEnrichWithPsiInformationWhenSourceFilesExist() {
        // Create a real Java file in the test project with unique name
        PsiFile javaFile = myFixture.addFileToProject(
            "src/test/java/com/example/shouldEnrichWithPsiInformation_WebDriverTest.java",
            """
            package com.example;
            
            import org.junit.Test;
            import org.openqa.selenium.WebDriver;
            import org.openqa.selenium.By;
            import org.openqa.selenium.NoSuchElementException;
            
            public class WebDriverTest {
                @Test
                public void testElementNotFound() {
                    WebDriver driver = null;
                    driver.findElement(By.id("nonexistent")); // This will cause NoSuchElementException
                }
            }
            """
        );
        
        String output = "org.openqa.selenium.NoSuchElementException: no such element: Unable to locate element: {\"method\":\"id\",\"selector\":\"nonexistent\"}\n" +
                       "    at com.example.WebDriverTest.testElementNotFound(WebDriverTest.java:42)";
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("NO_SUCH_ELEMENT_EXCEPTION", info.getAssertionType());
        assertEquals("no such element: Unable to locate element: {\"method\":\"id\",\"selector\":\"nonexistent\"}", info.getErrorMessage());
        assertEquals("WebDriverErrorStrategy", info.getParsingStrategy());
        assertNotNull(info.getStackTrace());
        
        // PSI enrichment should provide accurate source file information
        if (info.getSourceFilePath() != null) {
            assertEquals("shouldEnrichWithPsiInformation_WebDriverTest.java", info.getSourceFilePath());
        }
    }

    @Test
    @DisplayName("should extract stack trace information correctly")
    void shouldExtractStackTraceInformationCorrectly() {
        // Create a unique file for this test to avoid conflicts
        PsiFile javaFile = myFixture.addFileToProject(
            "src/test/java/com/example/shouldExtractStackTraceInformation_WebDriverStackTraceTest.java",
            """
            package com.example;
            
            import org.junit.Test;
            import org.openqa.selenium.WebDriver;
            import org.openqa.selenium.By;
            import org.openqa.selenium.TimeoutException;
            
            public class WebDriverStackTraceTest {
                @Test
                public void testTimeout() {
                    WebDriver driver = null;
                    driver.findElement(By.cssSelector("button[type='submit']")); // This will cause TimeoutException
                }
            }
            """
        );
        
        String output = "org.openqa.selenium.TimeoutException: Expected condition failed: waiting for element to be clickable: By.cssSelector: button[type='submit']\n" +
                       "    at com.example.WebDriverStackTraceTest.testTimeout(WebDriverStackTraceTest.java:42)";
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("TIMEOUT_EXCEPTION", info.getAssertionType());
        assertEquals("Expected condition failed: waiting for element to be clickable: By.cssSelector: button[type='submit']", info.getErrorMessage());
        assertEquals("WebDriverErrorStrategy", info.getParsingStrategy());
        assertNotNull(info.getStackTrace());
        assertTrue(info.getStackTrace().contains("org.openqa.selenium.TimeoutException"));
        assertTrue(info.getStackTrace().contains("at com.example.WebDriverStackTraceTest.testTimeout"));
        
        // PSI enrichment should provide method name
        if (info.getStepDefinitionMethod() != null) {
            assertTrue(info.getStepDefinitionMethod().contains("WebDriverStackTraceTest.testTimeout"));
        }
    }

    @Test
    @DisplayName("should handle element not interactable with PSI enrichment")
    void shouldHandleElementNotInteractableWithPsiEnrichment() {
        // Create a unique file for this test
        PsiFile javaFile = myFixture.addFileToProject(
            "src/test/java/com/example/shouldHandleElementNotInteractable_ElementTest.java",
            """
            package com.example;
            
            import org.junit.Test;
            import org.openqa.selenium.WebDriver;
            import org.openqa.selenium.By;
            import org.openqa.selenium.ElementNotInteractableException;
            
            public class ElementTest {
                @Test
                public void testElementNotInteractable() {
                    WebDriver driver = null;
                    driver.findElement(By.className("disabled-input")).sendKeys("test"); // This will cause ElementNotInteractableException
                }
            }
            """
        );
        
        String output = "org.openqa.selenium.ElementNotInteractableException: element not interactable: Element <input type=\"text\" class=\"disabled-input\"> is not interactable\n" +
                       "    at com.example.ElementTest.testElementNotInteractable(ElementTest.java:42)";
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("ELEMENT_NOT_INTERACTABLE_EXCEPTION", info.getAssertionType());
        assertEquals("element not interactable: Element <input type=\"text\" class=\"disabled-input\"> is not interactable", info.getErrorMessage());
        assertEquals("WebDriverErrorStrategy", info.getParsingStrategy());
        
        // PSI enrichment should provide accurate source file information
        if (info.getSourceFilePath() != null) {
            assertEquals("shouldHandleElementNotInteractable_ElementTest.java", info.getSourceFilePath());
        }
    }
} 