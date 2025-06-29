package com.triagemate.extractors.stacktrace_strategies;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.psi.PsiFile;
import com.triagemate.models.FailureInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for ConfigurationErrorStrategy with real PSI support.
 * 
 * <p>These tests require actual source files to be present in the test project
 * to properly test PSI enrichment functionality.</p>
 */
@DisplayName("ConfigurationErrorStrategy Integration")
class ConfigurationErrorStrategyIntegrationTest extends BasePlatformTestCase {
    
    private ConfigurationErrorStrategy strategy;

    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        strategy = new ConfigurationErrorStrategy(getProject());
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
        // Create a real Java file in the test project
        PsiFile javaFile = myFixture.addFileToProject(
            "src/test/java/com/example/MyTest.java",
            """
            package com.example;
            
            import org.junit.Test;
            import java.io.FileInputStream;
            
            public class MyTest {
                @Test
                public void testSomething() throws Exception {
                    new FileInputStream("config.properties");
                }
            }
            """
        );
        
        String output = "java.io.FileNotFoundException: config.properties (No such file or directory)\n" +
                       "    at com.example.MyTest.testSomething(MyTest.java:8)";
        
        FailureInfo info = strategy.parse(output);
        
        // Core parsing should work
        assertEquals("CONFIGURATION_ERROR", info.getAssertionType());
        assertEquals("config.properties", info.getFailedStepText());
        assertNotNull(info.getStackTrace());
        
        // PSI enrichment should work with real files
        assertNotNull("Source file path should be enriched", info.getSourceFilePath());
        assertTrue("Line number should be accurate", info.getLineNumber() > 0);
        assertNotNull("Step definition method should be enriched", info.getStepDefinitionMethod());
    }

    @Test
    @DisplayName("should handle PSI enrichment gracefully when files don't exist")
    void shouldHandlePsiEnrichmentGracefullyWhenFilesDontExist() {
        String output = "java.io.FileNotFoundException: config.properties (No such file or directory)\n" +
                       "    at com.example.NonExistentClass.nonExistentMethod(NonExistentClass.java:42)";
        
        FailureInfo info = strategy.parse(output);
        
        // Core parsing should still work
        assertEquals("CONFIGURATION_ERROR", info.getAssertionType());
        assertEquals("config.properties", info.getFailedStepText());
        assertNotNull(info.getStackTrace());
        
        // PSI enrichment may fail, but shouldn't break parsing
        assertNotNull("Step definition method should not be null", info.getStepDefinitionMethod());
    }
} 