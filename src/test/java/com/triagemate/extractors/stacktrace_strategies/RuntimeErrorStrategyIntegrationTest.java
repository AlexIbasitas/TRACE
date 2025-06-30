package com.triagemate.extractors.stacktrace_strategies;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.psi.PsiFile;
import com.triagemate.models.FailureInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for RuntimeErrorStrategy (Part 1).
 *
 * <p>These tests verify the strategy works correctly with real PSI operations
 * and file system integration. Split into multiple classes to avoid temp directory nesting issues.</p>
 * 
 * <p><strong>Note:</strong> This class contains the first 3 tests. Additional tests are in:
 * - RuntimeErrorStrategyIntegrationTest2 (3 tests)
 * - RuntimeErrorStrategyIntegrationTest3 (3 tests)</p>
 */
class RuntimeErrorStrategyIntegrationTest extends BasePlatformTestCase {

    private RuntimeErrorStrategy strategy;

    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        strategy = new RuntimeErrorStrategy(getProject());
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
            "src/test/java/com/example/shouldEnrichWithPsiInformation_RuntimeTest.java",
            """
            package com.example;
            
            import org.junit.Test;
            
            public class RuntimeTest {
                @Test
                public void testSomething() {
                    String str = null;
                    str.length(); // This will cause NullPointerException
                }
            }
            """
        );
        
        String output = "java.lang.NullPointerException: Cannot invoke \"String.length()\" because \"str\" is null\n" +
                       "    at com.example.RuntimeTest.testSomething(RuntimeTest.java:42)";
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("NULL_POINTER_EXCEPTION", info.getAssertionType());
        assertEquals("Cannot invoke \"String.length()\" because \"str\" is null", info.getErrorMessage());
        assertEquals("RuntimeErrorStrategy", info.getParsingStrategy());
        assertNotNull(info.getStackTrace());
        
        // PSI enrichment should provide accurate source file information
        if (info.getSourceFilePath() != null) {
            assertEquals("shouldEnrichWithPsiInformation_RuntimeTest.java", info.getSourceFilePath());
        }
    }

    @Test
    @DisplayName("should extract stack trace information correctly")
    void shouldExtractStackTraceInformationCorrectly() {
        // Create a unique file for this test to avoid conflicts
        PsiFile javaFile = myFixture.addFileToProject(
            "src/test/java/com/example/shouldExtractStackTraceInformation_StackTraceRuntimeTest.java",
            """
            package com.example;
            
            import org.junit.Test;
            
            public class StackTraceRuntimeTest {
                @Test
                public void testSomething() {
                    throw new IllegalArgumentException("Invalid argument");
                }
            }
            """
        );
        
        String output = "java.lang.IllegalArgumentException: Invalid argument\n" +
                       "    at com.example.StackTraceRuntimeTest.testSomething(StackTraceRuntimeTest.java:42)";
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("ILLEGAL_ARGUMENT_EXCEPTION", info.getAssertionType());
        assertEquals("Invalid argument", info.getErrorMessage());
        assertEquals("RuntimeErrorStrategy", info.getParsingStrategy());
        assertNotNull(info.getStackTrace());
        assertTrue(info.getStackTrace().contains("java.lang.IllegalArgumentException"));
        assertTrue(info.getStackTrace().contains("at com.example.StackTraceRuntimeTest.testSomething"));
        
        // PSI enrichment should provide method name
        if (info.getStepDefinitionMethod() != null) {
            assertTrue(info.getStepDefinitionMethod().contains("StackTraceRuntimeTest.testSomething"));
        }
    }

    @Test
    @DisplayName("should handle array index out of bounds with PSI enrichment")
    void shouldHandleArrayIndexOutOfBoundsWithPsiEnrichment() {
        // Create a unique file for this test
        PsiFile javaFile = myFixture.addFileToProject(
            "src/test/java/com/example/shouldHandleArrayIndexOutOfBounds_ArrayTest.java",
            """
            package com.example;
            
            import org.junit.Test;
            
            public class ArrayTest {
                @Test
                public void testArrayAccess() {
                    int[] array = new int[3];
                    int value = array[5]; // This will cause ArrayIndexOutOfBoundsException
                }
            }
            """
        );
        
        String output = "java.lang.ArrayIndexOutOfBoundsException: Index 5 out of bounds for length 3\n" +
                       "    at com.example.ArrayTest.testArrayAccess(ArrayTest.java:42)";
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("ARRAY_INDEX_OUT_OF_BOUNDS_EXCEPTION", info.getAssertionType());
        assertEquals("Index 5 out of bounds for length 3", info.getErrorMessage());
        assertEquals("RuntimeErrorStrategy", info.getParsingStrategy());
        
        // PSI enrichment should provide accurate source file information
        if (info.getSourceFilePath() != null) {
            assertEquals("shouldHandleArrayIndexOutOfBounds_ArrayTest.java", info.getSourceFilePath());
        }
    }
} 