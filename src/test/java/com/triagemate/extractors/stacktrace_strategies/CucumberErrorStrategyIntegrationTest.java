package com.triagemate.extractors.stacktrace_strategies;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.psi.PsiFile;
import com.triagemate.models.FailureInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for CucumberErrorStrategy with real PSI support.
 * 
 * <p>These tests require actual source files to be present in the test project
 * to properly test PSI enrichment functionality.</p>
 */
@DisplayName("CucumberErrorStrategy Integration")
class CucumberErrorStrategyIntegrationTest extends BasePlatformTestCase {
    
    private CucumberErrorStrategy strategy;

    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        strategy = new CucumberErrorStrategy(getProject());
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
        // Create a real Java file with Cucumber step definitions in the test project
        PsiFile javaFile = myFixture.addFileToProject(
            "src/test/java/com/example/CucumberSteps.java",
            """
            package com.example;
            
            import io.cucumber.java.en.Given;
            import io.cucumber.java.en.When;
            import io.cucumber.java.en.Then;
            
            public class CucumberSteps {
                @Given("I am on the login page")
                public void iAmOnTheLoginPage() {
                    // implementation
                }
                
                @When("I click on the button")
                public void iClickOnTheButton() {
                    // implementation
                }
                
                @Then("I should see the dashboard")
                public void iShouldSeeTheDashboard() {
                    // implementation
                }
            }
            """
        );
        
        String output = "io.cucumber.junit.UndefinedStepException: The step \"I click on the button\" is undefined.\n" +
                       "    at com.example.CucumberSteps.iClickOnTheButton(CucumberSteps.java:15)";
        
        FailureInfo info = strategy.parse(output);
        
        // Core parsing should work
        assertEquals("UNDEFINED_STEP", info.getAssertionType());
        assertEquals("I click on the button", info.getFailedStepText());
        assertNotNull(info.getStackTrace());
        
        // PSI enrichment should work with real files
        assertNotNull("Source file path should be enriched", info.getSourceFilePath());
        assertTrue("Line number should be accurate", info.getLineNumber() > 0);
        assertNotNull("Step definition method should be enriched", info.getStepDefinitionMethod());
    }

    @Test
    @DisplayName("should handle PSI enrichment gracefully when files don't exist")
    void shouldHandlePsiEnrichmentGracefullyWhenFilesDontExist() {
        String output = "io.cucumber.junit.UndefinedStepException: The step \"I click on the button\" is undefined.\n" +
                       "    at com.example.NonExistentClass.nonExistentMethod(NonExistentClass.java:42)";
        
        FailureInfo info = strategy.parse(output);
        
        // Core parsing should still work
        assertEquals("UNDEFINED_STEP", info.getAssertionType());
        assertEquals("I click on the button", info.getFailedStepText());
        assertNotNull(info.getStackTrace());
        
        // PSI enrichment may fail, but shouldn't break parsing
        assertNotNull("Step definition method should not be null", info.getStepDefinitionMethod());
    }

    @Test
    @DisplayName("should handle ambiguous step definitions with PSI enrichment")
    void shouldHandleAmbiguousStepDefinitionsWithPsiEnrichment() {
        // Create a real Java file with ambiguous step definitions
        PsiFile javaFile = myFixture.addFileToProject(
            "src/test/java/com/example/AmbiguousSteps.java",
            """
            package com.example;
            
            import io.cucumber.java.en.When;
            
            public class AmbiguousSteps {
                @When("I click on the button")
                public void iClickOnTheButton1() {
                    // implementation 1
                }
                
                @When("I click on the button")
                public void iClickOnTheButton2() {
                    // implementation 2
                }
            }
            """
        );
        
        String output = "io.cucumber.junit.AmbiguousStepDefinitionsException: The step \"I click on the button\" matches multiple step definitions.\n" +
                       "    at com.example.AmbiguousSteps.iClickOnTheButton1(AmbiguousSteps.java:8)";
        
        FailureInfo info = strategy.parse(output);
        
        // Core parsing should work
        assertEquals("AMBIGUOUS_STEP", info.getAssertionType());
        assertEquals("I click on the button", info.getFailedStepText());
        assertNotNull(info.getStackTrace());
        
        // PSI enrichment should work with real files
        assertNotNull("Source file path should be enriched", info.getSourceFilePath());
        assertTrue("Line number should be accurate", info.getLineNumber() > 0);
        assertNotNull("Step definition method should be enriched", info.getStepDefinitionMethod());
    }

    @Test
    @DisplayName("should handle pending step exceptions with PSI enrichment")
    void shouldHandlePendingStepExceptionsWithPsiEnrichment() {
        // Create a real Java file with pending step definitions
        PsiFile javaFile = myFixture.addFileToProject(
            "src/test/java/com/example/PendingSteps.java",
            """
            package com.example;
            
            import io.cucumber.java.en.When;
            import io.cucumber.java.PendingException;
            
            public class PendingSteps {
                @When("I click on the button")
                public void iClickOnTheButton() {
                    throw new PendingException("This step is not implemented yet");
                }
            }
            """
        );
        
        String output = "io.cucumber.junit.PendingException: The step \"I click on the button\" is pending.\n" +
                       "    at com.example.PendingSteps.iClickOnTheButton(PendingSteps.java:9)";
        
        FailureInfo info = strategy.parse(output);
        
        // Core parsing should work
        assertEquals("PENDING_STEP", info.getAssertionType());
        assertEquals("I click on the button", info.getFailedStepText());
        assertNotNull(info.getStackTrace());
        
        // PSI enrichment should work with real files
        assertNotNull("Source file path should be enriched", info.getSourceFilePath());
        assertTrue("Line number should be accurate", info.getLineNumber() > 0);
        assertNotNull("Step definition method should be enriched", info.getStepDefinitionMethod());
    }

    @Test
    @DisplayName("should handle step definition not found with PSI enrichment")
    void shouldHandleStepDefinitionNotFoundWithPsiEnrichment() {
        // Create a real Java file without the specific step definition
        PsiFile javaFile = myFixture.addFileToProject(
            "src/test/java/com/example/IncompleteSteps.java",
            """
            package com.example;
            
            import io.cucumber.java.en.Given;
            
            public class IncompleteSteps {
                @Given("I am on the login page")
                public void iAmOnTheLoginPage() {
                    // implementation
                }
                // Missing step definition for "I click on the button"
            }
            """
        );
        
        String output = "Step definition not found for step 'I click on the button'\n" +
                       "    at com.example.IncompleteSteps.iAmOnTheLoginPage(IncompleteSteps.java:8)";
        
        FailureInfo info = strategy.parse(output);
        
        // Core parsing should work
        assertEquals("STEP_DEFINITION_NOT_FOUND", info.getAssertionType());
        assertEquals("Step definition not found for step: I click on the button", info.getFailedStepText());
        assertNotNull(info.getStackTrace());
        
        // PSI enrichment should work with real files
        assertNotNull("Source file path should be enriched", info.getSourceFilePath());
        assertTrue("Line number should be accurate", info.getLineNumber() > 0);
        assertNotNull("Step definition method should be enriched", info.getStepDefinitionMethod());
    }

    @Test
    @DisplayName("should handle complex step text with special characters")
    void shouldHandleComplexStepTextWithSpecialCharacters() {
        String output = "io.cucumber.junit.UndefinedStepException: The step \"I click on the 'Submit' button with text 'Click me!'\" is undefined.\n" +
                       "    at com.example.MyTest.testSomething(MyTest.java:42)";
        
        FailureInfo info = strategy.parse(output);
        
        // Core parsing should work with complex step text
        assertEquals("UNDEFINED_STEP", info.getAssertionType());
        assertEquals("I click on the 'Submit' button with text 'Click me!'", info.getFailedStepText());
        assertNotNull(info.getStackTrace());
    }

    @Test
    @DisplayName("should handle step text with newlines")
    void shouldHandleStepTextWithNewlines() {
        String output = "io.cucumber.junit.UndefinedStepException: The step \"I click on the button\nand wait for the page to load\" is undefined.\n" +
                       "    at com.example.MyTest.testSomething(MyTest.java:42)";
        
        FailureInfo info = strategy.parse(output);
        
        // Core parsing should work with multiline step text
        assertEquals("UNDEFINED_STEP", info.getAssertionType());
        assertEquals("I click on the button\nand wait for the page to load", info.getFailedStepText());
        assertNotNull(info.getStackTrace());
    }

    @Test
    @DisplayName("should extract step text from annotation values")
    void shouldExtractStepTextFromAnnotationValues() {
        // Create a real Java file with step definitions using annotation values
        PsiFile javaFile = myFixture.addFileToProject(
            "src/test/java/com/example/StepDefinitions.java",
            """
            package com.example;
            
            import io.cucumber.java.en.Given;
            import io.cucumber.java.en.When;
            
            public class StepDefinitions {
                @Given(value = "I am on the login page")
                public void iAmOnTheLoginPage() {
                    // implementation
                }
                
                @When("I click on the button")
                public void iClickOnTheButton() {
                    // implementation
                }
            }
            """
        );
        
        String output = "io.cucumber.junit.UndefinedStepException: The step \"I am on the login page\" is undefined.\n" +
                       "    at com.example.StepDefinitions.iAmOnTheLoginPage(StepDefinitions.java:8)";
        
        FailureInfo info = strategy.parse(output);
        
        // Core parsing should work
        assertEquals("UNDEFINED_STEP", info.getAssertionType());
        assertEquals("I am on the login page", info.getFailedStepText());
        assertNotNull(info.getStackTrace());
        
        // PSI enrichment should work with real files
        assertNotNull("Source file path should be enriched", info.getSourceFilePath());
        assertTrue("Line number should be accurate", info.getLineNumber() > 0);
        assertNotNull("Step definition method should be enriched", info.getStepDefinitionMethod());
    }
} 