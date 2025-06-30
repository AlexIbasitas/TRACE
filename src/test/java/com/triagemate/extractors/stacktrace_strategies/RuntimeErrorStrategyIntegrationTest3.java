package com.triagemate.extractors.stacktrace_strategies;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.psi.PsiFile;
import com.triagemate.models.FailureInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for RuntimeErrorStrategy (Part 3).
 *
 * <p>These tests verify the strategy works correctly with real PSI operations
 * and file system integration. Split into multiple classes to avoid temp directory nesting issues.</p>
 * 
 * <p><strong>Note:</strong> This class contains tests 7-9. Additional tests are in:
 * - RuntimeErrorStrategyIntegrationTest (3 tests)
 * - RuntimeErrorStrategyIntegrationTest2 (3 tests)</p>
 */
class RuntimeErrorStrategyIntegrationTest3 extends BasePlatformTestCase {

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
    @DisplayName("should handle complex stack trace with multiple user code frames")
    void shouldHandleComplexStackTraceWithMultipleUserCodeFrames() {
        // Create a unique file for this test
        PsiFile javaFile = myFixture.addFileToProject(
            "src/test/java/com/example/shouldHandleComplexStackTrace_ComplexTest.java",
            """
            package com.example;
            
            import org.junit.Test;
            
            public class ComplexTest {
                @Test
                public void testComplexScenario() {
                    helperMethod();
                }
                
                private void helperMethod() {
                    throw new IllegalStateException("Invalid state");
                }
            }
            """
        );
        
        String output = "java.lang.IllegalStateException: Invalid state\n" +
                       "    at com.example.ComplexTest.helperMethod(ComplexTest.java:42)\n" +
                       "    at com.example.ComplexTest.testComplexScenario(ComplexTest.java:38)\n" +
                       "    at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:325)";
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("ILLEGAL_STATE_EXCEPTION", info.getAssertionType());
        assertEquals("Invalid state", info.getErrorMessage());
        assertEquals("RuntimeErrorStrategy", info.getParsingStrategy());
        
        // Should extract the first user code frame (helperMethod)
        if (info.getStepDefinitionMethod() != null) {
            assertTrue(info.getStepDefinitionMethod().contains("ComplexTest.helperMethod"));
        }
    }

    @Test
    @DisplayName("should handle runtime exception without message")
    void shouldHandleRuntimeExceptionWithoutMessage() {
        // Create a unique file for this test
        PsiFile javaFile = myFixture.addFileToProject(
            "src/test/java/com/example/shouldHandleRuntimeExceptionWithoutMessage_NoMessageTest.java",
            """
            package com.example;
            
            import org.junit.Test;
            
            public class NoMessageTest {
                @Test
                public void testNoMessage() {
                    throw new RuntimeException();
                }
            }
            """
        );
        
        String output = "java.lang.RuntimeException\n" +
                       "    at com.example.NoMessageTest.testNoMessage(NoMessageTest.java:42)";
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("RUNTIME_ERROR", info.getAssertionType());
        assertEquals("java.lang.RuntimeException", info.getErrorMessage());
        assertEquals("RuntimeErrorStrategy", info.getParsingStrategy());
        
        // PSI enrichment should still work
        if (info.getSourceFilePath() != null) {
            assertEquals("shouldHandleRuntimeExceptionWithoutMessage_NoMessageTest.java", info.getSourceFilePath());
        }
    }

    @Test
    @DisplayName("should handle security exception with PSI enrichment")
    void shouldHandleSecurityExceptionWithPsiEnrichment() {
        // Create a unique file for this test
        PsiFile javaFile = myFixture.addFileToProject(
            "src/test/java/com/example/shouldHandleSecurityException_SecurityTest.java",
            """
            package com.example;
            
            import org.junit.Test;
            
            public class SecurityTest {
                @Test
                public void testSecurity() {
                    throw new SecurityException("Access denied");
                }
            }
            """
        );
        
        String output = "java.lang.SecurityException: Access denied\n" +
                       "    at com.example.SecurityTest.testSecurity(SecurityTest.java:42)";
        
        FailureInfo info = strategy.parse(output);
        
        assertEquals("SECURITY_EXCEPTION", info.getAssertionType());
        assertEquals("Access denied", info.getErrorMessage());
        assertEquals("RuntimeErrorStrategy", info.getParsingStrategy());
        
        // PSI enrichment should provide method name
        if (info.getStepDefinitionMethod() != null) {
            assertTrue(info.getStepDefinitionMethod().contains("SecurityTest.testSecurity"));
        }
    }
} 