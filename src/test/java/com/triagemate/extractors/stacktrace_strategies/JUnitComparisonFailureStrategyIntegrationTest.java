package com.triagemate.extractors.stacktrace_strategies;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.psi.PsiFile;
import com.triagemate.models.FailureInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for JUnitComparisonFailureStrategy with real PSI support.
 * 
 * <p>These tests require actual source files to be present in the test project
 * to properly test PSI enrichment functionality.</p>
 * 
 * <p><strong>IMPORTANT:</strong> Each test method must use unique file names to avoid
 * "File already exists" errors from the IntelliJ test framework. The test framework
 * maintains file state between test methods, so reusing the same filename will cause
 * failures. Consider using unique class names or cleaning up files between tests if
 * this becomes problematic.</p>
 */
@DisplayName("JUnitComparisonFailureStrategy Integration")
class JUnitComparisonFailureStrategyIntegrationTest extends BasePlatformTestCase {
    
    private JUnitComparisonFailureStrategy strategy;

    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        strategy = new JUnitComparisonFailureStrategy(getProject());
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
            "src/test/java/com/example/shouldEnrichWithPsiInformation_MyTest_JUnitComparison.java",
            """
            package com.example;
            
            import org.junit.Test;
            import static org.junit.Assert.assertEquals;
            
            public class MyTest {
                @Test
                public void testSomething() {
                    assertEquals("expected", "actual");
                }
            }
            """
        );
        
        String output = "org.junit.ComparisonFailure: expected:<expected> but was:<actual>\n" +
                       "    at com.example.MyTest.testSomething(MyTest.java:8)";
        
        FailureInfo info = strategy.parse(output);
        
        // Core parsing should work
        assertEquals("expected", info.getExpectedValue());
        assertEquals("actual", info.getActualValue());
        assertEquals("JUNIT_COMPARISON", info.getAssertionType());
        assertNotNull(info.getStackTrace());
        
        // PSI enrichment should work with real files
        assertNotNull("Source file path should be enriched", info.getSourceFilePath());
        assertTrue("Line number should be accurate", info.getLineNumber() > 0);
        assertNotNull("Step definition method should be enriched", info.getStepDefinitionMethod());
    }

    @Test
    @DisplayName("should handle PSI enrichment gracefully when files don't exist")
    void shouldHandlePsiEnrichmentGracefullyWhenFilesDontExist() {
        String output = "org.junit.ComparisonFailure: expected:<foo> but was:<bar>\n" +
                       "    at com.example.NonExistentClass.nonExistentMethod(NonExistentClass.java:42)";
        
        FailureInfo info = strategy.parse(output);
        
        // Core parsing should still work
        assertEquals("foo", info.getExpectedValue());
        assertEquals("bar", info.getActualValue());
        assertEquals("JUNIT_COMPARISON", info.getAssertionType());
        assertNotNull(info.getStackTrace());
        
        // PSI enrichment may fail, but shouldn't break parsing
        assertNotNull("Step definition method should not be null", info.getStepDefinitionMethod());
    }

    @Test
    @DisplayName("should handle complex expected/actual values with newlines")
    void shouldHandleComplexExpectedActualValuesWithNewlines() {
        String output = "org.junit.ComparisonFailure: expected:<Hello\nWorld> but was:<Hello>";
        
        FailureInfo info = strategy.parse(output);
        
        // Core parsing should work with complex values
        assertEquals("Hello\nWorld", info.getExpectedValue());
        assertEquals("Hello", info.getActualValue());
        assertEquals("JUNIT_COMPARISON", info.getAssertionType());
        assertNotNull(info.getStackTrace());
    }

    @Test
    @DisplayName("should extract stack trace information correctly")
    void shouldExtractStackTraceInformationCorrectly() {
        // Create a unique file for this test to avoid conflicts
        PsiFile javaFile = myFixture.addFileToProject(
            "src/test/java/com/example/shouldExtractStackTraceInformation_StackTraceTest_JUnitComparison.java",
            """
            package com.example;
            
            import org.junit.Test;
            import static org.junit.Assert.assertEquals;
            
            public class StackTraceTest {
                @Test
                public void testSomething() {
                    assertEquals("foo", "bar");
                }
            }
            """
        );
        
        String output = "org.junit.ComparisonFailure: expected:<foo> but was:<bar>\n" +
                       "    at com.example.StackTraceTest.testSomething(StackTraceTest.java:42)\n" +
                       "    at com.example.OtherClass.otherMethod(OtherClass.java:10)";
        
        FailureInfo info = strategy.parse(output);
        
        // Should extract the first user code line (not JUnit framework code)
        assertNotNull(info.getStepDefinitionMethod());
        assertTrue(info.getStepDefinitionMethod().contains("com.example.StackTraceTest.testSomething"));
        assertNotNull(info.getStackTrace());
        assertTrue(info.getStackTrace().contains("at com.example.StackTraceTest.testSomething(StackTraceTest.java:42)"));
    }
} 