/*
 * NOTE: This test class uses LightJavaCodeInsightFixtureTestCase and JUnit 3 style.
 * These tests must be run from the IDE using the IntelliJ Platform test runner.
 * They will not work with Gradle's test task.
 */
package com.triagemate.extractors;

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.psi.PsiFile;
import com.triagemate.models.StepDefinitionInfo;

public class StepDefinitionExtractorIntegrationTest extends LightJavaCodeInsightFixtureTestCase {

    private StepDefinitionExtractor extractor;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        addAnnotationStubs();
        extractor = new StepDefinitionExtractor(getProject());
    }

    private void addAnnotationStubs() {
        myFixture.addFileToProject("src/test/java/io/cucumber/java/en/Given.java", "package io.cucumber.java.en; public @interface Given { String value(); }");
        myFixture.addFileToProject("src/test/java/io/cucumber/java/en/When.java", "package io.cucumber.java.en; public @interface When { String value(); }");
        myFixture.addFileToProject("src/test/java/io/cucumber/java/en/Then.java", "package io.cucumber.java.en; public @interface Then { String value(); }");
        myFixture.addFileToProject("src/test/java/io/cucumber/java/en/And.java", "package io.cucumber.java.en; public @interface And { String value(); }");
        myFixture.addFileToProject("src/test/java/io/cucumber/java/en/But.java", "package io.cucumber.java.en; public @interface But { String value(); }");
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            String[] annotationFiles = {
                "src/test/java/io/cucumber/java/en/Given.java",
                "src/test/java/io/cucumber/java/en/When.java",
                "src/test/java/io/cucumber/java/en/Then.java",
                "src/test/java/io/cucumber/java/en/And.java",
                "src/test/java/io/cucumber/java/en/But.java"
            };
            for (String file : annotationFiles) {
                try {
                    var vFile = myFixture.findFileInTempDir(file);
                    if (vFile != null && vFile.exists()) {
                        vFile.delete(this);
                    }
                } catch (Exception e) {
                    // Ignore if file does not exist or cannot be deleted
                }
            }
        } catch (Exception e) {
            addSuppressedException(e);
        } finally {
            super.tearDown();
        }
    }

    public void testExtractStepDefinitionForBasicGivenStep() {
        PsiFile javaFile = myFixture.addFileToProject(
            "src/test/java/com/example/steps/LoginStepDefinitions.java",
            """
            package com.example.steps;
            
            import io.cucumber.java.en.Given;
            import io.cucumber.java.en.When;
            import io.cucumber.java.en.Then;
            
            public class LoginStepDefinitions {
                
                @Given(\"I am on the login page\")
                public void iAmOnTheLoginPage() {
                    // Implementation here
                    System.out.println(\"Navigating to login page\");
                }
                
                @When(\"I enter {string} in the username field\")
                public void iEnterInTheUsernameField(String username) {
                    // Implementation here
                    System.out.println(\"Entering username: \" + username);
                }
                
                @Then(\"I should see the dashboard\")
                public void iShouldSeeTheDashboard() {
                    // Implementation here
                    System.out.println(\"Verifying dashboard is displayed\");
                }
            }
            """
        );
        String failedStepText = "Given I am on the login page";
        StepDefinitionInfo result = extractor.extractStepDefinition(failedStepText);
        assertNotNull(result);
        assertEquals("iAmOnTheLoginPage", result.getMethodName());
        assertEquals("LoginStepDefinitions", result.getClassName());
        assertEquals("com.example.steps", result.getPackageName());
        assertEquals("LoginStepDefinitions.java", result.getSourceFilePath());
        assertTrue(result.getLineNumber() > 0);
        assertEquals("I am on the login page", result.getStepPattern());
        assertTrue(result.getParameters().isEmpty());
        assertNotNull(result.getMethodText());
        assertTrue(result.getMethodText().contains("iAmOnTheLoginPage"));
    }

    public void testExtractStepDefinitionForWhenStepWithParameters() {
        PsiFile javaFile = myFixture.addFileToProject(
            "src/test/java/com/example/steps/UserInputStepDefinitions.java",
            """
            package com.example.steps;
            
            import io.cucumber.java.en.When;
            
            public class UserInputStepDefinitions {
                
                @When(\"I enter {string} in the {string} field\")
                public void iEnterInTheField(String value, String fieldName) {
                    // Implementation here
                    System.out.println(\"Entering \" + value + \" in \" + fieldName + \" field\");
                }
                
                @When(\"I click on the {string} button\")
                public void iClickOnTheButton(String buttonText) {
                    // Implementation here
                    System.out.println(\"Clicking on \" + buttonText + \" button\");
                }
            }
            """
        );
        String failedStepText = "When I enter \"test@example.com\" in the email field";
        StepDefinitionInfo result = extractor.extractStepDefinition(failedStepText);
        assertNotNull(result);
        assertEquals("iEnterInTheField", result.getMethodName());
        assertEquals("UserInputStepDefinitions", result.getClassName());
        assertEquals("com.example.steps", result.getPackageName());
        assertEquals("UserInputStepDefinitions.java", result.getSourceFilePath());
        assertTrue(result.getLineNumber() > 0);
        assertEquals("I enter {string} in the {string} field", result.getStepPattern());
        assertEquals(2, result.getParameters().size());
        assertTrue(result.getParameters().contains("value"));
        assertTrue(result.getParameters().contains("fieldName"));
        assertNotNull(result.getMethodText());
    }

    public void testReturnNullForNonExistentStep() {
        PsiFile javaFile = myFixture.addFileToProject(
            "src/test/java/com/example/steps/SomeStepDefinitions.java",
            """
            package com.example.steps;
            
            import io.cucumber.java.en.Given;
            
            public class SomeStepDefinitions {
                
                @Given(\"I am on the home page\")
                public void iAmOnTheHomePage() {
                    // Implementation here
                }
            }
            """
        );
        String failedStepText = "Given I am on a page that does not exist";
        StepDefinitionInfo result = extractor.extractStepDefinition(failedStepText);
        assertNull(result);
    }
} 