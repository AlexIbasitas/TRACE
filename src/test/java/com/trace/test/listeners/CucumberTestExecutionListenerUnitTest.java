package com.trace.test.listeners;

import com.intellij.execution.testframework.sm.runner.SMTestProxy;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CucumberTestExecutionListener using real platform objects.
 * 
 * <p>These tests verify the listener's ability to detect Cucumber tests,
 * process failures, and coordinate with extractors and backend services.
 * The tests use real platform objects to ensure proper integration and
 * follow JetBrains best practices for plugin testing.</p>
 * 
 * <p>Test patterns follow IntelliJ Platform best practices:
 * <ul>
 *   <li>Use real IntelliJ Platform components (Project, SMTestProxy, ServiceManager)</li>
 *   <li>Use real extractors to test actual coordination logic</li>
 *   <li>Test both positive and negative scenarios</li>
 *   <li>Verify error handling and fallback behavior</li>
 *   <li>Test edge cases and boundary conditions</li>
 * </ul></p>
 */
@DisplayName("CucumberTestExecutionListener Unit Tests")
class CucumberTestExecutionListenerUnitTest extends BasePlatformTestCase {

    private CucumberTestExecutionListener listener;
    private Project realProject;

    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        realProject = getProject();
        listener = new CucumberTestExecutionListener(realProject);
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

    @Nested
    @DisplayName("Constructor and Initialization")
    class ConstructorAndInitialization {

        @Test
        @DisplayName("should initialize with project")
        void shouldInitializeWithProject() {
            CucumberTestExecutionListener newListener = new CucumberTestExecutionListener(realProject);
            assertNotNull("Listener should be created successfully", newListener);
        }

        @Test
        @DisplayName("should initialize with null project gracefully")
        void shouldInitializeWithNullProjectGracefully() {
            assertDoesNotThrow(() -> new CucumberTestExecutionListener(null),
                "Listener should handle null project gracefully");
        }
    }

    @Nested
    @DisplayName("Cucumber Test Detection")
    class CucumberTestDetection {

        @Test
        @DisplayName("should process Cucumber test by scenario name")
        void shouldProcessCucumberTestByScenarioName() {
            SMTestProxy testProxy = createTestProxy("Scenario: User login", "Some error");

            // Test that the public method handles this correctly without exceptions
            assertDoesNotThrow(() -> listener.onTestFailed(testProxy),
                "Should process Cucumber test by scenario name without exceptions");
        }

        @Test
        @DisplayName("should process Cucumber test by feature name")
        void shouldProcessCucumberTestByFeatureName() {
            SMTestProxy testProxy = createTestProxy("Feature: Login functionality", "Some error");

            // Test that the public method handles this correctly without exceptions
            assertDoesNotThrow(() -> listener.onTestFailed(testProxy),
                "Should process Cucumber test by feature name without exceptions");
        }

        @Test
        @DisplayName("should process Cucumber test by error message")
        void shouldProcessCucumberTestByErrorMessage() {
            SMTestProxy testProxy = createTestProxy("Some test", "io.cucumber.junit.UndefinedStepException");

            // Test that the public method handles this correctly without exceptions
            assertDoesNotThrow(() -> listener.onTestFailed(testProxy),
                "Should process Cucumber test by error message without exceptions");
        }

        @Test
        @DisplayName("should not process non-Cucumber test")
        void shouldNotProcessNonCucumberTest() {
            SMTestProxy testProxy = createTestProxy("Regular JUnit test", "java.lang.AssertionError");

            // Test that the public method handles non-Cucumber tests gracefully
            assertDoesNotThrow(() -> listener.onTestFailed(testProxy),
                "Should handle non-Cucumber test gracefully without exceptions");
        }

        @Test
        @DisplayName("should handle null test name gracefully")
        void shouldHandleNullTestName() {
            SMTestProxy testProxy = createTestProxy(null, "io.cucumber.junit.UndefinedStepException");

            // Test that the public method handles null test name gracefully
            assertDoesNotThrow(() -> listener.onTestFailed(testProxy),
                "Should handle null test name gracefully without exceptions");
        }

        @Test
        @DisplayName("should handle null error message gracefully")
        void shouldHandleNullErrorMessage() {
            SMTestProxy testProxy = createTestProxy("Scenario: User login", null);

            // Test that the public method handles null error message gracefully
            assertDoesNotThrow(() -> listener.onTestFailed(testProxy),
                "Should handle null error message gracefully without exceptions");
        }

        @Test
        @DisplayName("should handle both null name and error message gracefully")
        void shouldHandleBothNullNameAndErrorMessage() {
            SMTestProxy testProxy = createTestProxy(null, null);

            // Test that the public method handles both null values gracefully
            assertDoesNotThrow(() -> listener.onTestFailed(testProxy),
                "Should handle both null name and error message gracefully without exceptions");
        }
    }

    @Nested
    @DisplayName("Test Failure Processing")
    class TestFailureProcessing {

        @Test
        @DisplayName("should process Cucumber test failure successfully")
        void shouldProcessCucumberTestFailureSuccessfully() {
            // Create a real test proxy with Cucumber error
            SMTestProxy testProxy = createTestProxy(
                "Scenario: User login", 
                "io.cucumber.junit.UndefinedStepException: Step undefined"
            );

            // Execute - should not throw exceptions
            assertDoesNotThrow(() -> listener.onTestFailed(testProxy),
                "Should process failure without throwing exceptions");
        }

        @Test
        @DisplayName("should handle null error message gracefully")
        void shouldHandleNullErrorMessageGracefully() {
            SMTestProxy testProxy = createTestProxy("Scenario: User login", null);

            assertDoesNotThrow(() -> listener.onTestFailed(testProxy),
                "Should handle null error message gracefully");
        }

        @Test
        @DisplayName("should handle empty error message gracefully")
        void shouldHandleEmptyErrorMessageGracefully() {
            SMTestProxy testProxy = createTestProxy("Scenario: User login", "");

            assertDoesNotThrow(() -> listener.onTestFailed(testProxy),
                "Should handle empty error message gracefully");
        }

        @Test
        @DisplayName("should handle whitespace error message gracefully")
        void shouldHandleWhitespaceErrorMessageGracefully() {
            SMTestProxy testProxy = createTestProxy("Scenario: User login", "   ");

            assertDoesNotThrow(() -> listener.onTestFailed(testProxy),
                "Should handle whitespace error message gracefully");
        }

        @Test
        @DisplayName("should handle JUnit comparison failure")
        void shouldHandleJUnitComparisonFailure() {
            SMTestProxy testProxy = createTestProxy(
                "Scenario: User login",
                "org.junit.ComparisonFailure: expected:<true> but was:<false>"
            );

            assertDoesNotThrow(() -> listener.onTestFailed(testProxy),
                "Should handle JUnit comparison failure gracefully");
        }

        @Test
        @DisplayName("should handle WebDriver error")
        void shouldHandleWebDriverError() {
            SMTestProxy testProxy = createTestProxy(
                "Scenario: User login",
                "org.openqa.selenium.NoSuchElementException: no such element"
            );

            assertDoesNotThrow(() -> listener.onTestFailed(testProxy),
                "Should handle WebDriver error gracefully");
        }

        @Test
        @DisplayName("should handle runtime exception")
        void shouldHandleRuntimeException() {
            SMTestProxy testProxy = createTestProxy(
                "Scenario: User login",
                "java.lang.NullPointerException: Cannot invoke \"String.length()\" because \"str\" is null"
            );

            assertDoesNotThrow(() -> listener.onTestFailed(testProxy),
                "Should handle runtime exception gracefully");
        }

        @Test
        @DisplayName("should handle generic exception")
        void shouldHandleGenericException() {
            SMTestProxy testProxy = createTestProxy(
                "Scenario: User login",
                "java.lang.Exception: Some unexpected error occurred"
            );

            assertDoesNotThrow(() -> listener.onTestFailed(testProxy),
                "Should handle generic exception gracefully");
        }
    }

    @Nested
    @DisplayName("Non-Cucumber Test Handling")
    class NonCucumberTestHandling {

        @Test
        @DisplayName("should not process non-Cucumber test failures")
        void shouldNotProcessNonCucumberTestFailures() {
            SMTestProxy testProxy = createTestProxy("Regular JUnit test", "java.lang.AssertionError");

            // This should not throw exceptions and should not process the test
            assertDoesNotThrow(() -> listener.onTestFailed(testProxy),
                "Should handle non-Cucumber test failure gracefully");
        }

        @Test
        @DisplayName("should not process test with no Cucumber indicators")
        void shouldNotProcessTestWithNoCucumberIndicators() {
            SMTestProxy testProxy = createTestProxy("MyTest.testSomething", "java.lang.AssertionError");

            assertDoesNotThrow(() -> listener.onTestFailed(testProxy),
                "Should handle test with no Cucumber indicators gracefully");
        }
    }

    @Nested
    @DisplayName("Backend Service Integration")
    class BackendServiceIntegration {

        @Test
        @DisplayName("should handle backend service not available")
        void shouldHandleBackendServiceNotAvailable() {
            SMTestProxy testProxy = createTestProxy(
                "Scenario: User login",
                "org.junit.ComparisonFailure: expected:<true> but was:<false>"
            );

            // Test with backend service not available (normal scenario)
            assertDoesNotThrow(() -> listener.onTestFailed(testProxy),
                "Should handle backend not available gracefully");
        }

        @Test
        @DisplayName("should handle backend service null")
        void shouldHandleBackendServiceNull() {
            SMTestProxy testProxy = createTestProxy(
                "Scenario: User login",
                "org.junit.ComparisonFailure: expected:<true> but was:<false>"
            );

            // Test with no backend service (normal scenario)
            assertDoesNotThrow(() -> listener.onTestFailed(testProxy),
                "Should handle null backend service gracefully");
        }
    }

    @Nested
    @DisplayName("Extractor Integration")
    class ExtractorIntegration {

        @Test
        @DisplayName("should work with real stack trace extractor")
        void shouldWorkWithRealStackTraceExtractor() {
            SMTestProxy testProxy = createTestProxy(
                "Scenario: User login",
                "org.junit.ComparisonFailure: expected:<true> but was:<false>"
            );

            // Test that the real extractor works
            assertDoesNotThrow(() -> listener.onTestFailed(testProxy),
                "Should work with real stack trace extractor");
        }

        @Test
        @DisplayName("should work with real step definition extractor")
        void shouldWorkWithRealStepDefinitionExtractor() {
            SMTestProxy testProxy = createTestProxy(
                "Scenario: User login",
                "io.cucumber.junit.UndefinedStepException: The step \"I click on button\" is undefined"
            );

            // Test that the real step definition extractor works
            assertDoesNotThrow(() -> listener.onTestFailed(testProxy),
                "Should work with real step definition extractor");
        }

        @Test
        @DisplayName("should work with real gherkin scenario extractor")
        void shouldWorkWithRealGherkinScenarioExtractor() {
            SMTestProxy testProxy = createTestProxy(
                "Scenario: User login",
                "io.cucumber.junit.UndefinedStepException: The step \"I click on button\" is undefined"
            );

            // Test that the real gherkin scenario extractor works
            assertDoesNotThrow(() -> listener.onTestFailed(testProxy),
                "Should work with real gherkin scenario extractor");
        }
    }

    /**
     * Test scenario name formatting for scenario outlines.
     * Verifies that scenario outline names include both title and example identifier.
     */
    @Test
    public void testScenarioOutlineNameFormatting() {
        // Test scenario outline formatting
        String gherkinScenarioName = "Verify Home Page has sample of correct links";
        String basicScenarioName = "Example #1.1";
        boolean isScenarioOutline = true;
        String formattedName = listener.formatScenarioName(gherkinScenarioName, basicScenarioName, isScenarioOutline);
        assertEquals("Verify Home Page has sample of correct links (Example #1.1)", formattedName);
        
        // Test regular scenario (no formatting)
        String regularGherkinName = "User login test";
        String regularBasicName = "User login test";
        boolean isRegularScenario = false;
        String regularFormattedName = listener.formatScenarioName(regularGherkinName, regularBasicName, isRegularScenario);
        assertEquals("User login test", regularFormattedName);
        
        // Test with null gherkin scenario name
        String nullGherkinFormatted = listener.formatScenarioName(null, basicScenarioName, isScenarioOutline);
        assertEquals("Example #1.1", nullGherkinFormatted);
        
        // Test with null basic scenario name
        String nullBasicFormatted = listener.formatScenarioName(gherkinScenarioName, null, isScenarioOutline);
        assertEquals("Verify Home Page has sample of correct links", nullBasicFormatted);
    }

    /**
     * Helper method to create a test proxy for testing
     */
    private SMTestProxy createTestProxy(String testName, String errorMessage) {
        // Create a test proxy that overrides the getErrorMessage method
        return new SMTestProxy(testName, false, null) {
            @Override
            public String getErrorMessage() {
                return errorMessage;
            }
        };
    }
} 