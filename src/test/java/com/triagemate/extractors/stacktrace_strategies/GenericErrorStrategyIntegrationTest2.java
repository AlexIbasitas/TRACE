package com.triagemate.extractors.stacktrace_strategies;

import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.triagemate.models.FailureInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for GenericErrorStrategy (Part 2).
 *
 * <p>These tests verify the strategy works correctly with real PSI operations
 * and file system integration. Split into multiple classes to avoid temp directory nesting issues.</p>
 * 
 * <p><strong>Note:</strong> This class contains tests 5-8. Additional tests are in:
 * - GenericErrorStrategyIntegrationTest (4 tests)
 * - GenericErrorStrategyIntegrationTest3 (4 tests)</p>
 */
class GenericErrorStrategyIntegrationTest2 extends BasePlatformTestCase {

    private GenericErrorStrategy strategy;

    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        strategy = new GenericErrorStrategy(getProject());
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
        // Create multiple files for this test
        PsiFile serviceFile = myFixture.addFileToProject(
            "src/test/java/com/example/shouldHandleComplexStackTrace_ServiceLayer.java",
            """
            package com.example;
            
            public class ServiceLayer {
                public void process() {
                    throw new Exception("Complex error scenario");
                }
            }
            """
        );

        PsiFile businessFile = myFixture.addFileToProject(
            "src/test/java/com/example/shouldHandleComplexStackTrace_BusinessLogic.java",
            """
            package com.example;
            
            public class BusinessLogic {
                public void execute() {
                    new ServiceLayer().process();
                }
            }
            """
        );

        PsiFile testFile = myFixture.addFileToProject(
            "src/test/java/com/example/shouldHandleComplexStackTrace_ComplexTest.java",
            """
            package com.example;
            
            import org.junit.Test;
            
            public class ComplexTest {
                @Test
                public void testComplexScenario() {
                    new BusinessLogic().execute();
                }
            }
            """
        );

        String testOutput = """
            java.lang.Exception: Complex error scenario
                at com.example.ServiceLayer.process(ServiceLayer.java:5)
                at com.example.BusinessLogic.execute(BusinessLogic.java:5)
                at com.example.ComplexTest.testComplexScenario(ComplexTest.java:8)
                at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:325)
            """;

        FailureInfo result = strategy.parse(testOutput);

        assertNotNull(result);
        assertEquals("Complex error scenario", result.getErrorMessage());
        assertTrue(result.getStackTrace().contains("com.example.ServiceLayer.process"));
        assertTrue(result.getStackTrace().contains("com.example.BusinessLogic.execute"));
        assertTrue(result.getStackTrace().contains("com.example.ComplexTest.testComplexScenario"));
        assertEquals("GenericErrorStrategy", result.getParsingStrategy());
    }

    @Test
    @DisplayName("should handle exception without message with PSI enrichment")
    void shouldHandleExceptionWithoutMessageWithPsiEnrichment() {
        // Create a unique file for this test to avoid conflicts
        PsiFile javaFile = myFixture.addFileToProject(
            "src/test/java/com/example/shouldHandleExceptionWithoutMessage_NoMessageTest.java",
            """
            package com.example;
            
            import org.junit.Test;
            
            public class NoMessageTest {
                @Test
                public void testSomething() {
                    throw new Exception();
                }
            }
            """
        );

        String testOutput = """
            java.lang.Exception
                at com.example.NoMessageTest.testSomething(NoMessageTest.java:8)
                at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:325)
            """;

        FailureInfo result = strategy.parse(testOutput);

        assertNotNull(result);
        assertNull(result.getErrorMessage());
        assertTrue(result.getStackTrace().contains("com.example.NoMessageTest.testSomething"));
        assertTrue(result.getStackTrace().contains("NoMessageTest.java:8"));
        assertEquals("GenericErrorStrategy", result.getParsingStrategy());
    }

    @Test
    @DisplayName("should handle stack trace without line numbers with PSI enrichment")
    void shouldHandleStackTraceWithoutLineNumbersWithPsiEnrichment() {
        // Create a unique file for this test to avoid conflicts
        PsiFile javaFile = myFixture.addFileToProject(
            "src/test/java/com/example/shouldHandleStackTraceWithoutLineNumbers_NoLineTest.java",
            """
            package com.example;
            
            import org.junit.Test;
            
            public class NoLineTest {
                @Test
                public void testSomething() {
                    throw new Exception("Some error");
                }
            }
            """
        );

        String testOutput = """
            java.lang.Exception: Some error
                at com.example.NoLineTest.testSomething(NoLineTest.java)
                at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java)
            """;

        FailureInfo result = strategy.parse(testOutput);

        assertNotNull(result);
        assertEquals("Some error", result.getErrorMessage());
        assertTrue(result.getStackTrace().contains("com.example.NoLineTest.testSomething"));
        assertTrue(result.getStackTrace().contains("NoLineTest.java"));
        assertEquals("GenericErrorStrategy", result.getParsingStrategy());
    }

    @Test
    @DisplayName("should handle malformed stack trace gracefully")
    void shouldHandleMalformedStackTraceGracefully() {
        String testOutput = """
            java.lang.Exception: Some error
                at com.example.MalformedTest.testSomething(MalformedTest.java:invalid)
                at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:325)
            """;

        FailureInfo result = strategy.parse(testOutput);

        assertNotNull(result);
        assertEquals("Some error", result.getErrorMessage());
        assertTrue(result.getStackTrace().contains("com.example.MalformedTest.testSomething"));
        assertEquals("GenericErrorStrategy", result.getParsingStrategy());
    }
} 