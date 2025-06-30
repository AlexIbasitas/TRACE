package com.triagemate.extractors;

import com.intellij.openapi.project.Project;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.triagemate.models.FailureInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for StackTraceExtractor.
 * 
 * <p>These tests verify the StackTraceExtractor works correctly with real IntelliJ Platform
 * components and handles real-world test failure scenarios. The tests use the actual
 * IntelliJ Platform test framework to ensure proper integration.</p>
 * 
 * <p>Test patterns follow established integration testing conventions:
 * <ul>
 *   <li>Extends BasePlatformTestCase for IntelliJ Platform integration</li>
 *   <li>Uses real project instances and PSI operations</li>
 *   <li>Tests with realistic test failure outputs</li>
 *   <li>Verifies end-to-end functionality</li>
 * </ul></p>
 */
@DisplayName("StackTraceExtractor Integration Tests")
class StackTraceExtractorIntegrationTest extends BasePlatformTestCase {

    private StackTraceExtractor extractor;

    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        extractor = new StackTraceExtractor(myFixture.getProject());
    }

    @Test
    @DisplayName("should extract JUnit comparison failure with real project context")
    void shouldExtractJUnitComparisonFailureWithRealProjectContext() {
        // Create a realistic JUnit comparison failure output
        String testOutput = """
            org.junit.ComparisonFailure: expected:<[Hello World]> but was:<[Hello, World]>
                at org.junit.Assert.assertEquals(Assert.java:117)
                at org.junit.Assert.assertEquals(Assert.java:144)
                at com.example.MyTest.testStringComparison(MyTest.java:42)
                at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
                at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
                at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
                at java.base/java.lang.reflect.Method.invoke(Method.java:568)
                at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
                at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
                at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
                at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
                at org.junit.runners.ParentRunner$3.run(ParentRunner.java:306)
                at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:71)
                at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:329)
                at org.junit.runners.ParentRunner.access$100(ParentRunner.java:66)
                at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:315)
                at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:71)
                at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:329)
                at org.junit.runners.ParentRunner.access$100(ParentRunner.java:66)
                at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:315)
                at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26)
                at org.junit.internal.runners.statements.RunAfters.evaluate(RunAfters.java:27)
                at org.junit.runners.ParentRunner.run(ParentRunner.java:415)
                at org.junit.runner.JUnitCore.run(JUnitCore.java:137)
                at com.intellij.junit4.JUnit4IdeaTestRunner.startRunnerWithArgs(JUnit4IdeaTestRunner.java:69)
                at com.intellij.rt.junit.IdeaTestRunner$Repeater.startRunnerWithArgs(IdeaTestRunner.java:33)
                at com.intellij.rt.junit.JUnitStarter.prepareStreamsAndStart(JUnitStarter.java:235)
                at com.intellij.rt.junit.JUnitStarter.main(JUnitStarter.java:54)
            """;

        FailureInfo result = extractor.extractFailureInfo(testOutput);

        assertNotNull("Should return non-null FailureInfo", result);
        assertEquals("Should use JUnit strategy for JUnit comparison failures", 
            "JUnitComparisonFailureStrategy", result.getParsingStrategy());
        assertNotNull("Should extract error message", result.getErrorMessage());
        assertTrue("Should extract the comparison details", 
            result.getErrorMessage().contains("expected <[Hello World]> but was <[Hello, World]>"));
        assertNotNull("Should extract stack trace", result.getStackTrace());
        assertTrue("Should identify the user code location", 
            result.getStackTrace().contains("at com.example.MyTest.testStringComparison"));
        assertTrue("Should record parsing time", result.getParsingTime() > 0);
    }

    @Test
    @DisplayName("should extract WebDriver exception with real project context")
    void shouldExtractWebDriverExceptionWithRealProjectContext() {
        // Create a realistic WebDriver exception output
        String testOutput = """
            org.openqa.selenium.NoSuchElementException: no such element: Unable to locate element: {"method":"css selector","selector":"#login-button"}
            For documentation on this error, please visit: https://www.seleniumhq.org/exceptions/no_such_element.html
            Build info: version: '4.15.0', revision: 'a5b7aab2b1'
            System info: host: 'localhost', ip: '127.0.0.1', os.name: 'Mac OS X', os.arch: 'x86_64', os.version: '10.15.7', java.version: '11.0.12'
            Driver info: org.openqa.selenium.chrome.ChromeDriver
            Capabilities {acceptInsecureCerts: false, browserName: chrome, browserVersion: 119.0.6045.105, chrome: {chromedriverVersion: 119.0.6045.105 (38c7255111b5..., userDataDir: /var/folders/...}, goog:chromeOptions: {debuggerAddress: localhost:...}, networkConnectionEnabled: false, pageLoadStrategy: normal, platformName: mac, proxy: Proxy(), se:cdp: ws://localhost:..., se:cdpVersion: 119.0.6045.105, setWindowRect: true, strictFileInteractability: false, timeouts: {implicit: 0, pageLoad: 300000, script: 30000}, unhandledPromptBehavior: dismiss and notify, webauthn:extension:credBlob: true, webauthn:extension:largeBlob: true, webauthn:extension:minPinLength: true, webauthn:extension:prf: true, webauthn:virtualAuthenticators: true}
            Session ID: 1234567890abcdef
                at org.openqa.selenium.remote.codec.w3c.W3CHttpResponseCodec.createException(W3CHttpResponseCodec.java:200)
                at org.openqa.selenium.remote.codec.w3c.W3CHttpResponseCodec.decode(W3CHttpResponseCodec.java:133)
                at org.openqa.selenium.remote.codec.w3c.W3CHttpResponseCodec.decode(W3CHttpResponseCodec.java:53)
                at org.openqa.selenium.remote.HttpCommandExecutor.execute(HttpCommandExecutor.java:184)
                at org.openqa.selenium.remote.service.DriverCommandExecutor.invokeExecute(DriverCommandExecutor.java:167)
                at org.openqa.selenium.remote.service.DriverCommandExecutor.execute(DriverCommandExecutor.java:142)
                at org.openqa.selenium.remote.RemoteWebDriver.execute(RemoteWebDriver.java:547)
                at org.openqa.selenium.remote.RemoteWebDriver.findElement(RemoteWebDriver.java:315)
                at org.openqa.selenium.remote.RemoteWebDriver.findElement(RemoteWebDriver.java:323)
                at org.openqa.selenium.support.pagefactory.DefaultElementLocator.findElement(DefaultElementLocator.java:69)
                at org.openqa.selenium.support.pagefactory.internal.LocatingElementHandler.invoke(LocatingElementHandler.java:38)
                at com.sun.proxy.$Proxy12.findElement(Unknown Source)
                at org.openqa.selenium.support.FindBy.findElement(FindBy.java:0)
                at com.example.MyTest.loginTest(MyTest.java:42)
                at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
                at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
                at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
                at java.base/java.lang.reflect.Method.invoke(Method.java:568)
                at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
                at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
                at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
                at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
                at org.junit.runners.ParentRunner$3.run(ParentRunner.java:306)
                at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:71)
                at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:329)
                at org.junit.runners.ParentRunner.access$100(ParentRunner.java:66)
                at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:315)
                at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:71)
                at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:329)
                at org.junit.runners.ParentRunner.access$100(ParentRunner.java:66)
                at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:315)
                at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26)
                at org.junit.internal.runners.statements.RunAfters.evaluate(RunAfters.java:27)
                at org.junit.runners.ParentRunner.run(ParentRunner.java:415)
                at org.junit.runner.JUnitCore.run(JUnitCore.java:137)
                at com.intellij.junit4.JUnit4IdeaTestRunner.startRunnerWithArgs(JUnit4IdeaTestRunner.java:69)
                at com.intellij.rt.junit.IdeaTestRunner$Repeater.startRunnerWithArgs(IdeaTestRunner.java:33)
                at com.intellij.rt.junit.JUnitStarter.prepareStreamsAndStart(JUnitStarter.java:235)
                at com.intellij.rt.junit.JUnitStarter.main(JUnitStarter.java:54)
            """;

        FailureInfo result = extractor.extractFailureInfo(testOutput);

        assertNotNull("Should return non-null FailureInfo", result);
        assertEquals("Should use WebDriver strategy for WebDriver exceptions", 
            "WebDriverErrorStrategy", result.getParsingStrategy());
        assertNotNull("Should extract error message", result.getErrorMessage());
        assertTrue("Should extract the WebDriver error message", 
            result.getErrorMessage().contains("no such element: Unable to locate element"));
        assertNotNull("Should extract stack trace", result.getStackTrace());
        assertTrue("Should identify the user code location", 
            result.getStackTrace().contains("at com.example.MyTest.loginTest"));
    }

    @Test
    @DisplayName("should extract Cucumber exception with real project context")
    void shouldExtractCucumberExceptionWithRealProjectContext() {
        // Create a realistic Cucumber exception output
        String testOutput = """
            io.cucumber.junit.UndefinedStepException: The step "I click on the login button" is undefined.
            You can implement this step using the snippet(s) below:

            @When("I click on the login button")
            public void i_click_on_the_login_button() {
                // Write code here that turns the phrase above into concrete actions
                throw new io.cucumber.java.PendingException();
            }

                at io.cucumber.junit.UndefinedStepException.<init>(UndefinedStepException.java:38)
                at io.cucumber.junit.Cucumber.run(Cucumber.java:123)
                at org.junit.runner.JUnitCore.run(JUnitCore.java:137)
                at com.intellij.junit4.JUnit4IdeaTestRunner.startRunnerWithArgs(JUnit4IdeaTestRunner.java:69)
                at com.intellij.rt.junit.IdeaTestRunner$Repeater.startRunnerWithArgs(IdeaTestRunner.java:33)
                at com.intellij.rt.junit.JUnitStarter.prepareStreamsAndStart(JUnitStarter.java:235)
                at com.intellij.rt.junit.JUnitStarter.main(JUnitStarter.java:54)
            """;

        FailureInfo result = extractor.extractFailureInfo(testOutput);

        assertNotNull("Should return non-null FailureInfo", result);
        assertEquals("Should use Cucumber strategy for Cucumber exceptions", 
            "CucumberErrorStrategy", result.getParsingStrategy());
        assertNotNull("Should extract failed step text", result.getFailedStepText());
        assertEquals("Should extract the exact step text", 
            "I click on the login button", result.getFailedStepText());
        assertNotNull("Should extract error message", result.getErrorMessage());
        assertTrue("Should extract the Cucumber error message", 
            result.getErrorMessage().contains("The step \"I click on the login button\" is undefined"));
    }

    @Test
    @DisplayName("should extract runtime exception with real project context")
    void shouldExtractRuntimeExceptionWithRealProjectContext() {
        // Create a realistic runtime exception output
        String testOutput = """
            java.lang.NullPointerException: Cannot invoke "String.length()" because "str" is null
                at com.example.MyTest.processString(MyTest.java:25)
                at com.example.MyTest.testStringProcessing(MyTest.java:42)
                at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
                at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
                at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
                at java.base/java.lang.reflect.Method.invoke(Method.java:568)
                at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
                at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
                at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
                at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
                at org.junit.runners.ParentRunner$3.run(ParentRunner.java:306)
                at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:71)
                at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:329)
                at org.junit.runners.ParentRunner.access$100(ParentRunner.java:66)
                at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:315)
                at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:71)
                at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:329)
                at org.junit.runners.ParentRunner.access$100(ParentRunner.java:66)
                at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:315)
                at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26)
                at org.junit.internal.runners.statements.RunAfters.evaluate(RunAfters.java:27)
                at org.junit.runners.ParentRunner.run(ParentRunner.java:415)
                at org.junit.runner.JUnitCore.run(JUnitCore.java:137)
                at com.intellij.junit4.JUnit4IdeaTestRunner.startRunnerWithArgs(JUnit4IdeaTestRunner.java:69)
                at com.intellij.rt.junit.IdeaTestRunner$Repeater.startRunnerWithArgs(IdeaTestRunner.java:33)
                at com.intellij.rt.junit.JUnitStarter.prepareStreamsAndStart(JUnitStarter.java:235)
                at com.intellij.rt.junit.JUnitStarter.main(JUnitStarter.java:54)
            """;

        FailureInfo result = extractor.extractFailureInfo(testOutput);

        assertNotNull("Should return non-null FailureInfo", result);
        assertEquals("Should use Runtime strategy for runtime exceptions", 
            "RuntimeErrorStrategy", result.getParsingStrategy());
        assertNotNull("Should extract error message", result.getErrorMessage());
        assertTrue("Should extract the runtime error message", 
            result.getErrorMessage().contains("Cannot invoke \"String.length()\" because \"str\" is null"));
        assertNotNull("Should extract stack trace", result.getStackTrace());
        assertTrue("Should identify the user code location", 
            result.getStackTrace().contains("at com.example.MyTest.processString"));
    }

    @Test
    @DisplayName("should extract configuration error with real project context")
    void shouldExtractConfigurationErrorWithRealProjectContext() {
        // Create a realistic configuration error output
        String testOutput = """
            java.io.FileNotFoundException: application.properties (No such file or directory)
                at java.base/java.io.FileInputStream.open0(Native Method)
                at java.base/java.io.FileInputStream.open(FileInputStream.java:196)
                at java.base/java.io.FileInputStream.<init>(FileInputStream.java:139)
                at java.base/java.io.FileInputStream.<init>(FileInputStream.java:103)
                at com.example.MyTest.loadConfiguration(MyTest.java:25)
                at com.example.MyTest.testConfigurationLoading(MyTest.java:42)
                at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
                at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
                at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
                at java.base/java.lang.reflect.Method.invoke(Method.java:568)
                at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
                at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
                at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
                at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
                at org.junit.runners.ParentRunner$3.run(ParentRunner.java:306)
                at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:71)
                at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:329)
                at org.junit.runners.ParentRunner.access$100(ParentRunner.java:66)
                at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:315)
                at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:71)
                at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:329)
                at org.junit.runners.ParentRunner.access$100(ParentRunner.java:66)
                at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:315)
                at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26)
                at org.junit.internal.runners.statements.RunAfters.evaluate(RunAfters.java:27)
                at org.junit.runners.ParentRunner.run(ParentRunner.java:415)
                at org.junit.runner.JUnitCore.run(JUnitCore.java:137)
                at com.intellij.junit4.JUnit4IdeaTestRunner.startRunnerWithArgs(JUnit4IdeaTestRunner.java:69)
                at com.intellij.rt.junit.IdeaTestRunner$Repeater.startRunnerWithArgs(IdeaTestRunner.java:33)
                at com.intellij.rt.junit.JUnitStarter.prepareStreamsAndStart(JUnitStarter.java:235)
                at com.intellij.rt.junit.JUnitStarter.main(JUnitStarter.java:54)
            """;

        FailureInfo result = extractor.extractFailureInfo(testOutput);

        assertNotNull("Should return non-null FailureInfo", result);
        assertEquals("Should use Configuration strategy for configuration errors", 
            "ConfigurationErrorStrategy", result.getParsingStrategy());
        assertNotNull("Should extract error message", result.getErrorMessage());
        assertTrue("Should extract the configuration error message", 
            result.getErrorMessage().contains("application.properties (No such file or directory)"));
        assertNotNull("Should extract stack trace", result.getStackTrace());
        assertTrue("Should identify the user code location", 
            result.getStackTrace().contains("at com.example.MyTest.loadConfiguration"));
    }

    @Test
    @DisplayName("should handle complex multi-strategy scenario")
    void shouldHandleComplexMultiStrategyScenario() {
        // Create a complex scenario that might trigger multiple strategies
        String testOutput = """
            Some runtime error occurred during test execution
            java.lang.RuntimeException: Some runtime error
                at com.example.MyTest.someMethod(MyTest.java:25)
                at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
                at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
                at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
                at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
                at org.junit.runners.ParentRunner$3.run(ParentRunner.java:306)
                at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:71)
                at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:329)
                at org.junit.runners.ParentRunner.access$100(ParentRunner.java:66)
                at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:315)
                at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:71)
                at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:329)
                at org.junit.runners.ParentRunner.access$100(ParentRunner.java:66)
                at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:315)
                at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26)
                at org.junit.internal.runners.statements.RunAfters.evaluate(RunAfters.java:27)
                at org.junit.runners.ParentRunner.run(ParentRunner.java:415)
                at org.junit.runner.JUnitCore.run(JUnitCore.java:137)
                at com.intellij.junit4.JUnit4IdeaTestRunner.startRunnerWithArgs(JUnit4IdeaTestRunner.java:69)
                at com.intellij.rt.junit.IdeaTestRunner$Repeater.startRunnerWithArgs(IdeaTestRunner.java:33)
                at com.intellij.rt.junit.JUnitStarter.prepareStreamsAndStart(JUnitStarter.java:235)
                at com.intellij.rt.junit.JUnitStarter.main(JUnitStarter.java:54)
            """;

        FailureInfo result = extractor.extractFailureInfo(testOutput);

        assertNotNull("Should return non-null FailureInfo", result);
        assertEquals("Should use Runtime strategy for runtime exceptions", 
            "RuntimeErrorStrategy", result.getParsingStrategy());
        assertNotNull("Should extract error message", result.getErrorMessage());
        assertTrue("Should extract the runtime error message", 
            result.getErrorMessage().contains("Some runtime error"));
    }

    @Test
    @DisplayName("should handle unknown error type with fallback")
    void shouldHandleUnknownErrorTypeWithFallback() {
        // Create an unknown error type that should fall back to GenericErrorStrategy
        String testOutput = """
            Business rule violation occurred
            com.example.BusinessRuleException: Business rule violation occurred
                at com.example.MyTest.validateBusinessRule(MyTest.java:25)
                at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
                at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
                at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
                at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
                at org.junit.runners.ParentRunner$3.run(ParentRunner.java:306)
                at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:71)
                at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:329)
                at org.junit.runners.ParentRunner.access$100(ParentRunner.java:66)
                at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:315)
                at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:71)
                at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:329)
                at org.junit.runners.ParentRunner.access$100(ParentRunner.java:66)
                at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:315)
                at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26)
                at org.junit.internal.runners.statements.RunAfters.evaluate(RunAfters.java:27)
                at org.junit.runners.ParentRunner.run(ParentRunner.java:415)
                at org.junit.runner.JUnitCore.run(JUnitCore.java:137)
                at com.intellij.junit4.JUnit4IdeaTestRunner.startRunnerWithArgs(JUnit4IdeaTestRunner.java:69)
                at com.intellij.rt.junit.IdeaTestRunner$Repeater.startRunnerWithArgs(IdeaTestRunner.java:33)
                at com.intellij.rt.junit.JUnitStarter.prepareStreamsAndStart(JUnitStarter.java:235)
                at com.intellij.rt.junit.JUnitStarter.main(JUnitStarter.java:54)
            """;

        FailureInfo result = extractor.extractFailureInfo(testOutput);

        assertNotNull("Should return non-null FailureInfo", result);
        assertEquals("Should use Generic strategy for unknown error types", 
            "GenericErrorStrategy", result.getParsingStrategy());
        assertNotNull("Should extract error message", result.getErrorMessage());
        assertTrue("Should extract the business rule error message", 
            result.getErrorMessage().contains("Business rule violation occurred"));
    }

    @Test
    @DisplayName("should handle malformed test output gracefully")
    void shouldHandleMalformedTestOutputGracefully() {
        // Create malformed test output that should still be handled
        String testOutput = "This is not a proper stack trace but should still be processed";

        FailureInfo result = extractor.extractFailureInfo(testOutput);

        assertNotNull("Should return non-null FailureInfo even for malformed input", result);
        assertEquals("Should use Generic strategy for malformed input", 
            "GenericErrorStrategy", result.getParsingStrategy());
        assertNotNull("Should provide fallback error message", result.getErrorMessage());
        assertTrue("Should provide appropriate fallback message", 
            result.getErrorMessage().contains("Failed to parse generic error"));
    }

    @Test
    @DisplayName("should maintain strategy priority order in real scenarios")
    void shouldMaintainStrategyPriorityOrderInRealScenarios() {
        // Test that strategy priority is maintained in real scenarios
        // This test verifies that the first matching strategy is used
        
        // Get the list of strategy names to verify priority order
        var strategyNames = extractor.getStrategyNames();

        // Verify the expected priority order (sorted by priority, highest first)
        assertTrue("JUnitComparisonFailureStrategy should be first (priority 100)", 
            strategyNames.get(0).contains("JUnitComparisonFailureStrategy"));
        assertTrue("CucumberErrorStrategy should be second (priority 85)", 
            strategyNames.get(1).contains("CucumberErrorStrategy"));
        assertTrue("RuntimeErrorStrategy should be third (priority 80)", 
            strategyNames.get(2).contains("RuntimeErrorStrategy"));
        assertTrue("ConfigurationErrorStrategy should be fourth (priority 75)", 
            strategyNames.get(3).contains("ConfigurationErrorStrategy"));
        assertTrue("WebDriverErrorStrategy should be fifth (priority 70)", 
            strategyNames.get(4).contains("WebDriverErrorStrategy"));
        assertTrue("GenericErrorStrategy should be last (priority 10)", 
            strategyNames.get(5).contains("GenericErrorStrategy"));
    }
} 