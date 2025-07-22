/*
 * NOTE: This test class uses LightJavaCodeInsightFixtureTestCase and JUnit 3 style.
 * These tests must be run from the IDE using the IntelliJ Platform test runner.
 * They will not work with Gradle's test task.
 */
package com.trace.test.extractors;

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.psi.PsiFile;
import com.trace.test.models.StepDefinitionInfo;

public class StepDefinitionExtractorIntegrationTest extends LightJavaCodeInsightFixtureTestCase {

    private StepDefinitionExtractor extractor;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        addAnnotationStubs();
        extractor = new StepDefinitionExtractor(myFixture.getProject());
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            // Add any additional cleanup here if needed
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
                
                @Given("I am on the login page")
                public void iAmOnTheLoginPage() {
                    // Implementation here
                    System.out.println("Navigating to login page");
                }
                
                @When("I enter {string} in the username field")
                public void iEnterInTheUsernameField(String username) {
                    // Implementation here
                    System.out.println("Entering username: " + username);
                }
                
                @Then("I should see the dashboard")
                public void iShouldSeeTheDashboard() {
                    // Implementation here
                    System.out.println("Verifying dashboard is displayed");
                }
            }
            """
        );
        
        // Test stack trace-based extraction for the Given method
        String stackTrace = """
            java.lang.AssertionError: Login page not found
                at com.example.steps.LoginStepDefinitions.iAmOnTheLoginPage(LoginStepDefinitions.java:12)
                at ✽.I am on the login page(file:///path/to/login.feature:3)
            """;
        
        StepDefinitionInfo result = extractor.extractStepDefinition(stackTrace);
        
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
                
                @When("I enter {string} in the {string} field")
                public void iEnterInTheField(String value, String fieldName) {
                    // Implementation here
                    System.out.println("Entering " + value + " in " + fieldName + " field");
                }
                
                @When("I click on the {string} button")
                public void iClickOnTheButton(String buttonText) {
                    // Implementation here
                    System.out.println("Clicking on " + buttonText + " button");
                }
            }
            """
        );
        
        // Test stack trace-based extraction for the first When method
        String stackTrace = """
            java.lang.AssertionError: Field not found
                at com.example.steps.UserInputStepDefinitions.iEnterInTheField(UserInputStepDefinitions.java:8)
                at ✽.I enter "testuser" in the "username" field(file:///path/to/input.feature:5)
            """;
        
        StepDefinitionInfo result = extractor.extractStepDefinition(stackTrace);
        
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
        assertTrue(result.getMethodText().contains("iEnterInTheField"));
    }

    public void testExtractStepDefinitionForThenStep() {
        PsiFile javaFile = myFixture.addFileToProject(
            "src/test/java/com/example/steps/HomePageTestStep.java",
            """
            package com.example.steps;
            
            import io.cucumber.java.en.Then;
            import org.junit.Assert;
            
            public class HomePageTestStep {
                
                @Then("I should see title as {string}")
                public void i_should_see_title_as(String expectedTitle) {
                    // Implementation here
                    String actualTitle = "Welcome to the-internet";
                    Assert.assertEquals(expectedTitle, actualTitle);
                }
                
                @Then("I should see a link with text {string}")
                public void i_should_see_a_link_with_text(String linkText) {
                    // Implementation here
                    System.out.println("Looking for link with text: " + linkText);
                }
            }
            """
        );
        
        // Test stack trace-based extraction for the Then method
        String stackTrace = """
            java.lang.AssertionError: 
            Expected: is "Welcome to the-internet delete me"
                 but: was "Welcome to the-internet"
                at org.hamcrest.MatcherAssert.assertThat(MatcherAssert.java:20)
                at org.hamcrest.MatcherAssert.assertThat(MatcherAssert.java:6)
                at com.example.steps.HomePageTestStep.i_should_see_title_as(HomePageTestStep.java:28)
                at ✽.I should see title as "Welcome to the-internet delete me"(file:///path/to/feature.feature:8)
            """;
        
        StepDefinitionInfo result = extractor.extractStepDefinition(stackTrace);
        
        assertNotNull(result);
        assertEquals("i_should_see_title_as", result.getMethodName());
        assertEquals("HomePageTestStep", result.getClassName());
        assertEquals("com.example.steps", result.getPackageName());
        assertEquals("HomePageTestStep.java", result.getSourceFilePath());
        assertTrue(result.getLineNumber() > 0);
        assertEquals("I should see title as {string}", result.getStepPattern());
        assertEquals(1, result.getParameters().size());
        assertTrue(result.getParameters().contains("expectedTitle"));
        assertNotNull(result.getMethodText());
        assertTrue(result.getMethodText().contains("i_should_see_title_as"));
    }

    public void testExtractStepDefinitionWithMultipleClassesInStackTrace() {
        PsiFile javaFile = myFixture.addFileToProject(
            "src/test/java/com/example/steps/LoginStepDefinitions.java",
            """
            package com.example.steps;
            
            import io.cucumber.java.en.Given;
            
            public class LoginStepDefinitions {
                
                @Given("I am on the login page")
                public void iAmOnTheLoginPage() {
                    // Implementation here
                    System.out.println("Navigating to login page");
                }
            }
            """
        );
        
        // Test stack trace with multiple classes - should find the first step definition class
        String stackTrace = """
            java.lang.AssertionError: Test failed
                at org.junit.Assert.fail(Assert.java:86)
                at com.example.utils.TestUtils.validate(TestUtils.java:15)
                at com.example.steps.LoginStepDefinitions.iAmOnTheLoginPage(LoginStepDefinitions.java:12)
                at ✽.I am on the login page(file:///path/to/login.feature:3)
            """;
        
        StepDefinitionInfo result = extractor.extractStepDefinition(stackTrace);
        
        assertNotNull(result);
        assertEquals("iAmOnTheLoginPage", result.getMethodName());
        assertEquals("LoginStepDefinitions", result.getClassName());
    }

    public void testExtractStepDefinitionWithNoStepDefinitionClasses() {
        // Test stack trace with no step definition classes
        String stackTrace = """
            java.lang.AssertionError: Test failed
                at org.junit.Assert.fail(Assert.java:86)
                at org.junit.Assert.assertTrue(Assert.java:41)
                at com.example.utils.TestUtils.validate(TestUtils.java:15)
            """;
        
        StepDefinitionInfo result = extractor.extractStepDefinition(stackTrace);
        
        assertNull(result);
    }

    public void testExtractStepDefinitionWithMalformedStackTrace() {
        // Test malformed stack trace
        String stackTrace = "This is not a valid stack trace";
        
        StepDefinitionInfo result = extractor.extractStepDefinition(stackTrace);
        
        assertNull(result);
    }

    private void addAnnotationStubs() {
        // Add stub files for Cucumber annotations
        myFixture.addFileToProject(
            "src/test/java/io/cucumber/java/en/Given.java",
            """
            package io.cucumber.java.en;
            
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;
            
            @Retention(RetentionPolicy.RUNTIME)
            @Target(ElementType.METHOD)
            public @interface Given {
                String value();
            }
            """
        );
        
        myFixture.addFileToProject(
            "src/test/java/io/cucumber/java/en/When.java",
            """
            package io.cucumber.java.en;
            
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;
            
            @Retention(RetentionPolicy.RUNTIME)
            @Target(ElementType.METHOD)
            public @interface When {
                String value();
            }
            """
        );
        
        myFixture.addFileToProject(
            "src/test/java/io/cucumber/java/en/Then.java",
            """
            package io.cucumber.java.en;
            
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;
            
            @Retention(RetentionPolicy.RUNTIME)
            @Target(ElementType.METHOD)
            public @interface Then {
                String value();
            }
            """
        );
    }
} 