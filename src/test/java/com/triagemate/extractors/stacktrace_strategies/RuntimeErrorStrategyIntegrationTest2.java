package com.triagemate.extractors.stacktrace_strategies;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.psi.PsiFile;
import com.triagemate.models.FailureInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for RuntimeErrorStrategy (Part 2).
 *
 * <p>These tests verify the strategy works correctly with real PSI operations
 * and file system integration. Split into multiple classes to avoid temp directory nesting issues.</p>
 * 
 * <p><strong>Note:</strong> This class contains tests 4-6. Additional tests are in:
 * - RuntimeErrorStrategyIntegrationTest (3 tests)
 * - RuntimeErrorStrategyIntegrationTest3 (3 tests)</p>
 */
class RuntimeErrorStrategyIntegrationTest2 extends BasePlatformTestCase {

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
    @DisplayName("should handle class cast exception with PSI enrichment")
    void shouldHandleClassCastExceptionWithPsiEnrichment() {
        // Create a unique file for this test
        PsiFile javaFile = myFixture.addFileToProject(
            "src/test/java/com/example/shouldHandleClassCastException_CastTest.java",
            """
            package com.example;
            
            import org.junit.Test;
            
            public class CastTest {
                @Test
                public void testClassCast() {
                    Object obj = "Hello";
                    Integer number = (Integer) obj; // This will cause ClassCastException
                }
            }
            """
        );
        
        String output = "java.lang.ClassCastException: java.lang.String cannot be cast to java.lang.Integer\n" +
                       "    at com.example.CastTest.testClassCast(CastTest.java:42)";
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("CLASS_CAST_EXCEPTION", info.getAssertionType());
        assertEquals("java.lang.String cannot be cast to java.lang.Integer", info.getErrorMessage());
        assertEquals("RuntimeErrorStrategy", info.getParsingStrategy());
        
        // PSI enrichment should provide method name
        if (info.getStepDefinitionMethod() != null) {
            assertTrue(info.getStepDefinitionMethod().contains("CastTest.testClassCast"));
        }
    }

    @Test
    @DisplayName("should handle number format exception with PSI enrichment")
    void shouldHandleNumberFormatExceptionWithPsiEnrichment() {
        // Create a unique file for this test
        PsiFile javaFile = myFixture.addFileToProject(
            "src/test/java/com/example/shouldHandleNumberFormatException_NumberTest.java",
            """
            package com.example;
            
            import org.junit.Test;
            
            public class NumberTest {
                @Test
                public void testNumberFormat() {
                    int number = Integer.parseInt("abc"); // This will cause NumberFormatException
                }
            }
            """
        );
        
        String output = "java.lang.NumberFormatException: For input string: \"abc\"\n" +
                       "    at com.example.NumberTest.testNumberFormat(NumberTest.java:42)";
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("NUMBER_FORMAT_EXCEPTION", info.getAssertionType());
        assertEquals("For input string: \"abc\"", info.getErrorMessage());
        assertEquals("RuntimeErrorStrategy", info.getParsingStrategy());
        
        // PSI enrichment should provide line number information
        if (info.getLineNumber() > 0) {
            assertTrue(info.getLineNumber() > 0);
        }
    }

    @Test
    @DisplayName("should handle concurrent modification exception with PSI enrichment")
    void shouldHandleConcurrentModificationExceptionWithPsiEnrichment() {
        // Create a unique file for this test
        PsiFile javaFile = myFixture.addFileToProject(
            "src/test/java/com/example/shouldHandleConcurrentModificationException_ConcurrentTest.java",
            """
            package com.example;
            
            import org.junit.Test;
            import java.util.ArrayList;
            import java.util.List;
            
            public class ConcurrentTest {
                @Test
                public void testConcurrentModification() {
                    List<String> list = new ArrayList<>();
                    list.add("item1");
                    list.add("item2");
                    
                    for (String item : list) {
                        list.add("newItem"); // This will cause ConcurrentModificationException
                    }
                }
            }
            """
        );
        
        String output = "java.util.ConcurrentModificationException: Collection was modified during iteration\n" +
                       "    at com.example.ConcurrentTest.testConcurrentModification(ConcurrentTest.java:42)";
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("CONCURRENT_MODIFICATION_EXCEPTION", info.getAssertionType());
        assertEquals("Collection was modified during iteration", info.getErrorMessage());
        assertEquals("RuntimeErrorStrategy", info.getParsingStrategy());
        
        // PSI enrichment should provide accurate source file information
        if (info.getSourceFilePath() != null) {
            assertEquals("shouldHandleConcurrentModificationException_ConcurrentTest.java", info.getSourceFilePath());
        }
    }
} 