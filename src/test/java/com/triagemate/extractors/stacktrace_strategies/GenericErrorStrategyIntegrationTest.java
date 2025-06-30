package com.triagemate.extractors.stacktrace_strategies;

import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.triagemate.models.FailureInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for GenericErrorStrategy (Part 1).
 *
 * <p>These tests verify the strategy works correctly with real PSI operations
 * and file system integration. Split into multiple classes to avoid temp directory nesting issues.</p>
 * 
 * <p><strong>Note:</strong> This class contains the first 4 tests. Additional tests are in:
 * - GenericErrorStrategyIntegrationTest2 (4 tests)
 * - GenericErrorStrategyIntegrationTest3 (4 tests)</p>
 */
class GenericErrorStrategyIntegrationTest extends BasePlatformTestCase {

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
    @DisplayName("should enrich with PSI information when source files exist")
    void shouldEnrichWithPsiInformationWhenSourceFilesExist() {
        // Create a real Java file in the test project with unique name
        PsiFile javaFile = myFixture.addFileToProject(
            "src/test/java/com/example/shouldEnrichWithPsiInformation_GenericErrorTest.java",
            """
            package com.example;
            
            import org.junit.Test;
            
            public class GenericErrorTest {
                @Test
                public void testSomething() {
                    throw new Exception("Some unexpected error occurred");
                }
            }
            """
        );

        String testOutput = """
            java.lang.Exception: Some unexpected error occurred
                at com.example.GenericErrorTest.testSomething(GenericErrorTest.java:8)
                at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:325)
            """;

        FailureInfo result = strategy.parse(testOutput);

        assertNotNull(result);
        assertEquals("Some unexpected error occurred", result.getErrorMessage());
        assertTrue(result.getStackTrace().contains("com.example.GenericErrorTest.testSomething"));
        assertTrue(result.getStackTrace().contains("GenericErrorTest.java:8"));
        assertEquals("GenericErrorStrategy", result.getParsingStrategy());
        assertTrue(result.getParsingTime() > 0);
    }

    @Test
    @DisplayName("should handle PSI enrichment gracefully when files don't exist")
    void shouldHandlePsiEnrichmentGracefullyWhenFilesDontExist() {
        String testOutput = """
            java.lang.Exception: Some unexpected error occurred
                at com.example.NonExistentTest.testSomething(NonExistentTest.java:42)
                at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:325)
            """;

        FailureInfo result = strategy.parse(testOutput);

        assertNotNull(result);
        assertEquals("Some unexpected error occurred", result.getErrorMessage());
        assertTrue(result.getStackTrace().contains("com.example.NonExistentTest.testSomething"));
        assertTrue(result.getStackTrace().contains("NonExistentTest.java:42"));
        assertEquals("GenericErrorStrategy", result.getParsingStrategy());
    }

    @Test
    @DisplayName("should extract stack trace information correctly")
    void shouldExtractStackTraceInformationCorrectly() {
        // Create a unique file for this test to avoid conflicts
        PsiFile javaFile = myFixture.addFileToProject(
            "src/test/java/com/example/shouldExtractStackTraceInformation_StackTraceTest.java",
            """
            package com.example;
            
            import org.junit.Test;
            
            public class StackTraceTest {
                @Test
                public void testSomething() {
                    throw new Exception("Stack trace test error");
                }
            }
            """
        );

        String testOutput = """
            java.lang.Exception: Stack trace test error
                at com.example.StackTraceTest.testSomething(StackTraceTest.java:8)
                at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:325)
            """;

        FailureInfo result = strategy.parse(testOutput);

        assertNotNull(result);
        assertEquals("Stack trace test error", result.getErrorMessage());
        assertTrue(result.getStackTrace().contains("com.example.StackTraceTest.testSomething"));
        assertTrue(result.getStackTrace().contains("StackTraceTest.java:8"));
        assertEquals("GenericErrorStrategy", result.getParsingStrategy());
    }

    @Test
    @DisplayName("should handle custom business exception with PSI enrichment")
    void shouldHandleCustomBusinessExceptionWithPsiEnrichment() {
        // Create a unique file for this test to avoid conflicts
        PsiFile javaFile = myFixture.addFileToProject(
            "src/test/java/com/example/shouldHandleCustomBusinessException_BusinessService.java",
            """
            package com.example;
            
            public class BusinessService {
                public void validate() {
                    throw new CustomBusinessException("Business rule violation");
                }
            }
            
            class CustomBusinessException extends Exception {
                public CustomBusinessException(String message) {
                    super(message);
                }
            }
            """
        );

        String testOutput = """
            com.example.CustomBusinessException: Business rule violation
                at com.example.BusinessService.validate(BusinessService.java:5)
                at com.example.MyTest.testBusinessLogic(MyTest.java:67)
            """;

        FailureInfo result = strategy.parse(testOutput);

        assertNotNull(result);
        assertEquals("Business rule violation", result.getErrorMessage());
        assertTrue(result.getStackTrace().contains("com.example.BusinessService.validate"));
        assertTrue(result.getStackTrace().contains("BusinessService.java:5"));
        assertEquals("GenericErrorStrategy", result.getParsingStrategy());
    }
} 