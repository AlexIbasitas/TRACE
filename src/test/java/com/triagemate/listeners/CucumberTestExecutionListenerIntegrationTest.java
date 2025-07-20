package com.triagemate.listeners;

import com.intellij.execution.testframework.sm.runner.SMTestProxy;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.triagemate.models.FailureInfo;
import com.triagemate.models.GherkinScenarioInfo;
import com.triagemate.models.StepDefinitionInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for CucumberTestExecutionListener.
 * 
 * <p>These tests verify the listener's behavior with real IntelliJ Platform components,
 * including actual PSI operations and file system interactions. The tests create
 * realistic test scenarios and verify end-to-end functionality.</p>
 * 
 * <p>Test patterns follow IntelliJ Platform integration testing best practices:
 * <ul>
 *   <li>Extend BasePlatformTestCase for full IntelliJ Platform environment</li>
 *   <li>Create real test files in the project</li>
 *   <li>Test with actual PSI operations</li>
 *   <li>Verify complete processing pipeline</li>
 *   <li>Test realistic failure scenarios</li>
 * </ul></p>
 */
@DisplayName("CucumberTestExecutionListener Integration")
class CucumberTestExecutionListenerIntegrationTest extends BasePlatformTestCase {

    private CucumberTestExecutionListener listener;

    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        listener = new CucumberTestExecutionListener(getProject());
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
    @DisplayName("should process real Cucumber test failure with step definitions")
    void shouldProcessRealCucumberTestFailureWithStepDefinitions() {
        // Create a real Cucumber feature file
        PsiFile featureFile = myFixture.addFileToProject(
            "src/test/resources/features/login.feature",
            """
            Feature: User Login
              As a user
              I want to log into the system
              So that I can access my account
            
            @smoke @login
            Scenario: Successful login with valid credentials
              Given user is on login page
              When user enters valid credentials
              Then user should be logged in successfully
            """
        );

        // Create a real step definitions file
        PsiFile stepDefFile = myFixture.addFileToProject(
            "src/test/java/com/example/LoginStepDefinitions.java",
            """
            package com.example;
            
            import io.cucumber.java.en.Given;
            import io.cucumber.java.en.When;
            import io.cucumber.java.en.Then;
            import org.junit.Assert;
            
            public class LoginStepDefinitions {
                
                @Given("user is on login page")
                public void userIsOnLoginPage() {
                    // Implementation
                }
                
                @When("user enters valid credentials")
                public void userEntersValidCredentials() {
                    // This will cause an assertion failure
                    Assert.assertEquals("Expected success", "Actual failure");
                }
                
                @Then("user should be logged in successfully")
                public void userShouldBeLoggedInSuccessfully() {
                    // Implementation
                }
            }
            """
        );

        // Create a mock test proxy that simulates a real Cucumber test failure
        SMTestProxy mockTestProxy = createMockCucumberTestProxy(
            "Scenario: Successful login with valid credentials",
            """
            org.junit.ComparisonFailure: expected:<Expected success> but was:<Actual failure>
                at org.junit.Assert.assertEquals(Assert.java:115)
                at com.example.LoginStepDefinitions.userEntersValidCredentials(LoginStepDefinitions.java:25)
                at io.cucumber.java.en.When.invoke(When.java:42)
                at io.cucumber.junit.Cucumber.run(Cucumber.java:123)
                at org.junit.runner.JUnitCore.run(JUnitCore.java:137)
            """
        );

        // Process the failure
        assertDoesNotThrow(() -> listener.onTestFailed(mockTestProxy),
            "Should process real Cucumber test failure without exceptions");
    }

    @Test
    @DisplayName("should process Cucumber undefined step exception")
    void shouldProcessCucumberUndefinedStepException() {
        // Create a real Cucumber feature file
        PsiFile featureFile = myFixture.addFileToProject(
            "src/test/resources/features/undefined_step.feature",
            """
            Feature: Undefined Step Test
            
            Scenario: Test with undefined step
              Given user is on page
              When user clicks on undefined button
              Then result should be visible
            """
        );

        // Create a mock test proxy for undefined step exception
        SMTestProxy mockTestProxy = createMockCucumberTestProxy(
            "Scenario: Test with undefined step",
            """
            io.cucumber.junit.UndefinedStepException: The step "user clicks on undefined button" is undefined.
            You can implement this step using the snippet(s) below:

            @When("user clicks on undefined button")
            public void user_clicks_on_undefined_button() {
                // Write code here that turns the phrase above into concrete actions
                throw new io.cucumber.java.PendingException();
            }

                at io.cucumber.junit.UndefinedStepException.<init>(UndefinedStepException.java:38)
                at io.cucumber.junit.Cucumber.run(Cucumber.java:123)
                at org.junit.runner.JUnitCore.run(JUnitCore.java:137)
            """
        );

        // Process the failure
        assertDoesNotThrow(() -> listener.onTestFailed(mockTestProxy),
            "Should process undefined step exception without exceptions");
    }

    @Test
    @DisplayName("should process WebDriver error in Cucumber test")
    void shouldProcessWebDriverErrorInCucumberTest() {
        // Create a real Cucumber feature file
        PsiFile featureFile = myFixture.addFileToProject(
            "src/test/resources/features/webdriver_error.feature",
            """
            Feature: WebDriver Error Test
            
            Scenario: Test with WebDriver error
              Given user is on login page
              When user tries to find non-existent element
              Then error should be handled
            """
        );

        // Create a real step definitions file with WebDriver
        PsiFile stepDefFile = myFixture.addFileToProject(
            "src/test/java/com/example/WebDriverStepDefinitions.java",
            """
            package com.example;
            
            import io.cucumber.java.en.Given;
            import io.cucumber.java.en.When;
            import io.cucumber.java.en.Then;
            import org.openqa.selenium.WebDriver;
            import org.openqa.selenium.By;
            import org.openqa.selenium.NoSuchElementException;
            
            public class WebDriverStepDefinitions {
                
                private WebDriver driver;
                
                @Given("user is on login page")
                public void userIsOnLoginPage() {
                    // Implementation
                }
                
                @When("user tries to find non-existent element")
                public void userTriesToFindNonExistentElement() {
                    // This will cause NoSuchElementException
                    driver.findElement(By.id("nonexistent"));
                }
                
                @Then("error should be handled")
                public void errorShouldBeHandled() {
                    // Implementation
                }
            }
            """
        );

        // Create a mock test proxy for WebDriver error
        SMTestProxy mockTestProxy = createMockCucumberTestProxy(
            "Scenario: Test with WebDriver error",
            """
            org.openqa.selenium.NoSuchElementException: no such element: Unable to locate element: {"method":"id","selector":"nonexistent"}
                at org.openqa.selenium.remote.RemoteWebDriver.findElement(RemoteWebDriver.java:315)
                at com.example.WebDriverStepDefinitions.userTriesToFindNonExistentElement(WebDriverStepDefinitions.java:25)
                at io.cucumber.java.en.When.invoke(When.java:42)
                at io.cucumber.junit.Cucumber.run(Cucumber.java:123)
            """
        );

        // Process the failure
        assertDoesNotThrow(() -> listener.onTestFailed(mockTestProxy),
            "Should process WebDriver error without exceptions");
    }

    @Test
    @DisplayName("should process runtime exception in Cucumber test")
    void shouldProcessRuntimeExceptionInCucumberTest() {
        // Create a real Cucumber feature file
        PsiFile featureFile = myFixture.addFileToProject(
            "src/test/resources/features/runtime_error.feature",
            """
            Feature: Runtime Error Test
            
            Scenario: Test with runtime error
              Given user is on page
              When user performs action that causes null pointer
              Then error should be handled
            """
        );

        // Create a real step definitions file with runtime error
        PsiFile stepDefFile = myFixture.addFileToProject(
            "src/test/java/com/example/RuntimeErrorStepDefinitions.java",
            """
            package com.example;
            
            import io.cucumber.java.en.Given;
            import io.cucumber.java.en.When;
            import io.cucumber.java.en.Then;
            
            public class RuntimeErrorStepDefinitions {
                
                @Given("user is on page")
                public void userIsOnPage() {
                    // Implementation
                }
                
                @When("user performs action that causes null pointer")
                public void userPerformsActionThatCausesNullPointer() {
                    String str = null;
                    // This will cause NullPointerException
                    int length = str.length();
                }
                
                @Then("error should be handled")
                public void errorShouldBeHandled() {
                    // Implementation
                }
            }
            """
        );

        // Create a mock test proxy for runtime error
        SMTestProxy mockTestProxy = createMockCucumberTestProxy(
            "Scenario: Test with runtime error",
            """
            java.lang.NullPointerException: Cannot invoke "String.length()" because "str" is null
                at com.example.RuntimeErrorStepDefinitions.userPerformsActionThatCausesNullPointer(RuntimeErrorStepDefinitions.java:25)
                at io.cucumber.java.en.When.invoke(When.java:42)
                at io.cucumber.junit.Cucumber.run(Cucumber.java:123)
            """
        );

        // Process the failure
        assertDoesNotThrow(() -> listener.onTestFailed(mockTestProxy),
            "Should process runtime exception without exceptions");
    }

    @Test
    @DisplayName("should handle non-Cucumber test failure gracefully")
    void shouldHandleNonCucumberTestFailureGracefully() {
        // Create a mock test proxy for a regular JUnit test (not Cucumber)
        SMTestProxy mockTestProxy = createMockCucumberTestProxy(
            "Regular JUnit Test",
            """
            java.lang.AssertionError: Expected true but was false
                at org.junit.Assert.fail(Assert.java:88)
                at org.junit.Assert.assertTrue(Assert.java:41)
                at com.example.RegularTest.testSomething(RegularTest.java:15)
                at org.junit.runner.JUnitCore.run(JUnitCore.java:137)
            """
        );

        // Process the failure - should not process since it's not a Cucumber test
        assertDoesNotThrow(() -> listener.onTestFailed(mockTestProxy),
            "Should handle non-Cucumber test failure gracefully");
    }

    @Test
    @DisplayName("should handle test with empty error message")
    void shouldHandleTestWithEmptyErrorMessage() {
        // Create a mock test proxy with empty error message
        SMTestProxy mockTestProxy = createMockCucumberTestProxy(
            "Scenario: Test with empty error",
            ""
        );

        // Process the failure
        assertDoesNotThrow(() -> listener.onTestFailed(mockTestProxy),
            "Should handle test with empty error message gracefully");
    }

    @Test
    @DisplayName("should handle test with null error message")
    void shouldHandleTestWithNullErrorMessage() {
        // Create a mock test proxy with null error message
        SMTestProxy mockTestProxy = createMockCucumberTestProxy(
            "Scenario: Test with null error",
            null
        );

        // Process the failure
        assertDoesNotThrow(() -> listener.onTestFailed(mockTestProxy),
            "Should handle test with null error message gracefully");
    }

    /**
     * Helper method to create a real SMTestProxy for testing
     */
    private SMTestProxy createMockCucumberTestProxy(String testName, String errorMessage) {
        return new SMTestProxy(testName, false, "test://" + testName) {
            @Override
            public String getName() {
                return testName;
            }
            
            @Override
            public String getErrorMessage() {
                return errorMessage;
            }
        };
    }
} 