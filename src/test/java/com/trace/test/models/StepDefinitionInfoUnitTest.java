package com.trace.test.models;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("Step Definition Info Unit Tests")
class StepDefinitionInfoUnitTest {

    @Nested
    @DisplayName("Constructor and Builder")
    class ConstructorAndBuilder {

        @Test
        @DisplayName("should create StepDefinitionInfo with all required fields")
        void shouldCreateStepDefinitionInfoWithAllRequiredFields() {
            // Arrange
            String methodName = "clickLoginButton";
            String className = "LoginStepDefinitions";
            String packageName = "com.example.steps";
            String sourceFilePath = "src/test/java/com/example/steps/LoginStepDefinitions.java";
            int lineNumber = 25;
            String stepPattern = "^I click the (.*?) button$";
            List<String> parameters = Arrays.asList("buttonName");
            String methodText = "@When(\"^I click the (.*?) button$\")\npublic void clickLoginButton(String buttonName) {\n    // Implementation\n}";

            // Act
            StepDefinitionInfo stepDef = new StepDefinitionInfo(methodName, className, packageName, sourceFilePath, lineNumber, stepPattern, parameters, methodText);

            // Assert
            assertThat(stepDef).isNotNull();
            assertThat(stepDef.getMethodName()).isEqualTo(methodName);
            assertThat(stepDef.getClassName()).isEqualTo(className);
            assertThat(stepDef.getPackageName()).isEqualTo(packageName);
            assertThat(stepDef.getSourceFilePath()).isEqualTo(sourceFilePath);
            assertThat(stepDef.getLineNumber()).isEqualTo(lineNumber);
            assertThat(stepDef.getStepPattern()).isEqualTo(stepPattern);
            assertThat(stepDef.getParameters()).isEqualTo(parameters);
            assertThat(stepDef.getMethodText()).isEqualTo(methodText);
        }

        @Test
        @DisplayName("should create StepDefinitionInfo using builder pattern")
        void shouldCreateStepDefinitionInfoUsingBuilderPattern() {
            // Arrange
            String methodName = "clickLoginButton";
            String className = "LoginStepDefinitions";
            String packageName = "com.example.steps";

            // Act
            StepDefinitionInfo stepDef = new StepDefinitionInfo.Builder()
                .withMethodName(methodName)
                .withClassName(className)
                .withPackageName(packageName)
                .withSourceFilePath("src/test/java/com/example/steps/LoginStepDefinitions.java")
                .withLineNumber(25)
                .withStepPattern("^I click the (.*?) button$")
                .withParameters(Arrays.asList("buttonName"))
                .withMethodText("@When(\"^I click the (.*?) button$\")\npublic void clickLoginButton(String buttonName) {\n    // Implementation\n}")
                .build();

            // Assert
            assertThat(stepDef).isNotNull();
            assertThat(stepDef.getMethodName()).isEqualTo(methodName);
            assertThat(stepDef.getClassName()).isEqualTo(className);
            assertThat(stepDef.getPackageName()).isEqualTo(packageName);
        }

        @Test
        @DisplayName("should handle null optional fields gracefully")
        void shouldHandleNullOptionalFieldsGracefully() {
            // Act
            StepDefinitionInfo stepDef = new StepDefinitionInfo.Builder()
                .withMethodName("clickLoginButton")
                .withClassName("LoginStepDefinitions")
                .withPackageName(null)
                .withSourceFilePath(null)
                .withLineNumber(-1)
                .withStepPattern(null)
                .withParameters(null)
                .withMethodText(null)
                .build();

            // Assert
            assertThat(stepDef).isNotNull();
            assertThat(stepDef.getMethodName()).isEqualTo("clickLoginButton");
            assertThat(stepDef.getClassName()).isEqualTo("LoginStepDefinitions");
            assertThat(stepDef.getPackageName()).isNull();
            assertThat(stepDef.getSourceFilePath()).isNull();
            assertThat(stepDef.getLineNumber()).isEqualTo(-1);
            assertThat(stepDef.getStepPattern()).isNull();
            assertThat(stepDef.getParameters()).isNull();
            assertThat(stepDef.getMethodText()).isNull();
        }

        @Test
        @DisplayName("should handle empty parameters list")
        void shouldHandleEmptyParametersList() {
            // Act
            StepDefinitionInfo stepDef = new StepDefinitionInfo.Builder()
                .withMethodName("clickLoginButton")
                .withClassName("LoginStepDefinitions")
                .withParameters(Arrays.asList())
                .build();

            // Assert
            assertThat(stepDef).isNotNull();
            assertThat(stepDef.getParameters()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Method Information")
    class MethodInformation {

        @Test
        @DisplayName("should return correct method name")
        void shouldReturnCorrectMethodName() {
            // Arrange
            String methodName = "clickLoginButton";

            // Act
            StepDefinitionInfo stepDef = new StepDefinitionInfo.Builder()
                .withMethodName(methodName)
                .build();

            // Assert
            assertThat(stepDef.getMethodName()).isEqualTo(methodName);
        }

        @Test
        @DisplayName("should return correct class name")
        void shouldReturnCorrectClassName() {
            // Arrange
            String className = "LoginStepDefinitions";

            // Act
            StepDefinitionInfo stepDef = new StepDefinitionInfo.Builder()
                .withClassName(className)
                .build();

            // Assert
            assertThat(stepDef.getClassName()).isEqualTo(className);
        }

        @Test
        @DisplayName("should return correct package name")
        void shouldReturnCorrectPackageName() {
            // Arrange
            String packageName = "com.example.steps";

            // Act
            StepDefinitionInfo stepDef = new StepDefinitionInfo.Builder()
                .withPackageName(packageName)
                .build();

            // Assert
            assertThat(stepDef.getPackageName()).isEqualTo(packageName);
        }

        @Test
        @DisplayName("should return correct source file path")
        void shouldReturnCorrectSourceFilePath() {
            // Arrange
            String sourceFilePath = "src/test/java/com/example/steps/LoginStepDefinitions.java";

            // Act
            StepDefinitionInfo stepDef = new StepDefinitionInfo.Builder()
                .withSourceFilePath(sourceFilePath)
                .build();

            // Assert
            assertThat(stepDef.getSourceFilePath()).isEqualTo(sourceFilePath);
        }

        @Test
        @DisplayName("should return correct line number")
        void shouldReturnCorrectLineNumber() {
            // Arrange
            int lineNumber = 25;

            // Act
            StepDefinitionInfo stepDef = new StepDefinitionInfo.Builder()
                .withLineNumber(lineNumber)
                .build();

            // Assert
            assertThat(stepDef.getLineNumber()).isEqualTo(lineNumber);
        }
    }

    @Nested
    @DisplayName("Step Definition Information")
    class StepDefinitionInformation {

        @Test
        @DisplayName("should return correct step pattern")
        void shouldReturnCorrectStepPattern() {
            // Arrange
            String stepPattern = "^I click the (.*?) button$";

            // Act
            StepDefinitionInfo stepDef = new StepDefinitionInfo.Builder()
                .withStepPattern(stepPattern)
                .build();

            // Assert
            assertThat(stepDef.getStepPattern()).isEqualTo(stepPattern);
        }

        @Test
        @DisplayName("should return correct parameters list")
        void shouldReturnCorrectParametersList() {
            // Arrange
            List<String> parameters = Arrays.asList("buttonName", "expectedText");

            // Act
            StepDefinitionInfo stepDef = new StepDefinitionInfo.Builder()
                .withParameters(parameters)
                .build();

            // Assert
            assertThat(stepDef.getParameters()).isEqualTo(parameters);
            assertThat(stepDef.getParameters()).hasSize(2);
            assertThat(stepDef.getParameters()).contains("buttonName");
            assertThat(stepDef.getParameters()).contains("expectedText");
        }

        @Test
        @DisplayName("should return correct method text")
        void shouldReturnCorrectMethodText() {
            // Arrange
            String methodText = "@When(\"^I click the (.*?) button$\")\npublic void clickLoginButton(String buttonName) {\n    // Implementation\n}";

            // Act
            StepDefinitionInfo stepDef = new StepDefinitionInfo.Builder()
                .withMethodText(methodText)
                .build();

            // Assert
            assertThat(stepDef.getMethodText()).isEqualTo(methodText);
            assertThat(stepDef.getMethodText()).contains("@When");
            assertThat(stepDef.getMethodText()).contains("public void clickLoginButton");
        }

        @Test
        @DisplayName("should handle null step pattern gracefully")
        void shouldHandleNullStepPatternGracefully() {
            // Act
            StepDefinitionInfo stepDef = new StepDefinitionInfo.Builder()
                .withStepPattern(null)
                .build();

            // Assert
            assertThat(stepDef.getStepPattern()).isNull();
        }

        @Test
        @DisplayName("should handle null method text gracefully")
        void shouldHandleNullMethodTextGracefully() {
            // Act
            StepDefinitionInfo stepDef = new StepDefinitionInfo.Builder()
                .withMethodText(null)
                .build();

            // Assert
            assertThat(stepDef.getMethodText()).isNull();
        }
    }

    @Nested
    @DisplayName("Parameter Handling")
    class ParameterHandling {

        @Test
        @DisplayName("should handle single parameter correctly")
        void shouldHandleSingleParameterCorrectly() {
            // Arrange
            List<String> parameters = Arrays.asList("buttonName");

            // Act
            StepDefinitionInfo stepDef = new StepDefinitionInfo.Builder()
                .withParameters(parameters)
                .build();

            // Assert
            assertThat(stepDef.getParameters()).hasSize(1);
            assertThat(stepDef.getParameters()).contains("buttonName");
        }

        @Test
        @DisplayName("should handle multiple parameters correctly")
        void shouldHandleMultipleParametersCorrectly() {
            // Arrange
            List<String> parameters = Arrays.asList("username", "password", "expectedResult");

            // Act
            StepDefinitionInfo stepDef = new StepDefinitionInfo.Builder()
                .withParameters(parameters)
                .build();

            // Assert
            assertThat(stepDef.getParameters()).hasSize(3);
            assertThat(stepDef.getParameters()).contains("username");
            assertThat(stepDef.getParameters()).contains("password");
            assertThat(stepDef.getParameters()).contains("expectedResult");
        }

        @Test
        @DisplayName("should handle empty parameters list")
        void shouldHandleEmptyParametersList() {
            // Act
            StepDefinitionInfo stepDef = new StepDefinitionInfo.Builder()
                .withParameters(Arrays.asList())
                .build();

            // Assert
            assertThat(stepDef.getParameters()).isEmpty();
        }

        @Test
        @DisplayName("should handle null parameters list")
        void shouldHandleNullParametersList() {
            // Act
            StepDefinitionInfo stepDef = new StepDefinitionInfo.Builder()
                .withParameters(null)
                .build();

            // Assert
            assertThat(stepDef.getParameters()).isNull();
        }
    }

    @Nested
    @DisplayName("Pattern Matching")
    class PatternMatching {

        @Test
        @DisplayName("should handle simple step patterns")
        void shouldHandleSimpleStepPatterns() {
            // Arrange
            String stepPattern = "^I am on the login page$";

            // Act
            StepDefinitionInfo stepDef = new StepDefinitionInfo.Builder()
                .withStepPattern(stepPattern)
                .build();

            // Assert
            assertThat(stepDef.getStepPattern()).isEqualTo(stepPattern);
            assertThat(stepDef.getStepPattern()).startsWith("^");
            assertThat(stepDef.getStepPattern()).endsWith("$");
        }

        @Test
        @DisplayName("should handle complex step patterns with parameters")
        void shouldHandleComplexStepPatternsWithParameters() {
            // Arrange
            String stepPattern = "^I enter (.*?) in the (.*?) field$";

            // Act
            StepDefinitionInfo stepDef = new StepDefinitionInfo.Builder()
                .withStepPattern(stepPattern)
                .withParameters(Arrays.asList("value", "fieldName"))
                .build();

            // Assert
            assertThat(stepDef.getStepPattern()).isEqualTo(stepPattern);
            assertThat(stepDef.getStepPattern()).contains("(.*?)");
            assertThat(stepDef.getParameters()).hasSize(2);
            assertThat(stepDef.getParameters()).contains("value");
            assertThat(stepDef.getParameters()).contains("fieldName");
        }

        @Test
        @DisplayName("should handle regex patterns correctly")
        void shouldHandleRegexPatternsCorrectly() {
            // Arrange
            String stepPattern = "^I should see (\\d+) items? in the list$";

            // Act
            StepDefinitionInfo stepDef = new StepDefinitionInfo.Builder()
                .withStepPattern(stepPattern)
                .withParameters(Arrays.asList("count"))
                .build();

            // Assert
            assertThat(stepDef.getStepPattern()).isEqualTo(stepPattern);
            assertThat(stepDef.getStepPattern()).contains("\\d+");
            assertThat(stepDef.getStepPattern()).contains("items?");
            assertThat(stepDef.getParameters()).hasSize(1);
            assertThat(stepDef.getParameters()).contains("count");
        }

        @Test
        @DisplayName("should handle escaped characters in patterns")
        void shouldHandleEscapedCharactersInPatterns() {
            // Arrange
            String stepPattern = "^I click the \\\"(.*?)\\\" button$";

            // Act
            StepDefinitionInfo stepDef = new StepDefinitionInfo.Builder()
                .withStepPattern(stepPattern)
                .withParameters(Arrays.asList("buttonText"))
                .build();

            // Assert
            assertThat(stepDef.getStepPattern()).isEqualTo(stepPattern);
            assertThat(stepDef.getStepPattern()).contains("\\\"");
            assertThat(stepDef.getParameters()).hasSize(1);
            assertThat(stepDef.getParameters()).contains("buttonText");
        }
    }

    @Nested
    @DisplayName("Builder Pattern")
    class BuilderPattern {

        @Test
        @DisplayName("should support method chaining")
        void shouldSupportMethodChaining() {
            // Act
            StepDefinitionInfo stepDef = new StepDefinitionInfo.Builder()
                .withMethodName("clickLoginButton")
                .withClassName("LoginStepDefinitions")
                .withPackageName("com.example.steps")
                .withSourceFilePath("src/test/java/com/example/steps/LoginStepDefinitions.java")
                .withLineNumber(25)
                .withStepPattern("^I click the (.*?) button$")
                .withParameters(Arrays.asList("buttonName"))
                .withMethodText("@When(\"^I click the (.*?) button$\")\npublic void clickLoginButton(String buttonName) {\n    // Implementation\n}")
                .build();

            // Assert
            assertThat(stepDef).isNotNull();
            assertThat(stepDef.getMethodName()).isEqualTo("clickLoginButton");
            assertThat(stepDef.getClassName()).isEqualTo("LoginStepDefinitions");
            assertThat(stepDef.getPackageName()).isEqualTo("com.example.steps");
            assertThat(stepDef.getSourceFilePath()).isEqualTo("src/test/java/com/example/steps/LoginStepDefinitions.java");
            assertThat(stepDef.getLineNumber()).isEqualTo(25);
            assertThat(stepDef.getStepPattern()).isEqualTo("^I click the (.*?) button$");
            assertThat(stepDef.getParameters()).hasSize(1);
            assertThat(stepDef.getMethodText()).contains("@When");
        }

        @Test
        @DisplayName("should create multiple instances independently")
        void shouldCreateMultipleInstancesIndependently() {
            // Arrange
            StepDefinitionInfo.Builder builder = new StepDefinitionInfo.Builder();

            // Act
            StepDefinitionInfo stepDef1 = builder.withMethodName("method1").withClassName("Class1").build();
            StepDefinitionInfo stepDef2 = builder.withMethodName("method2").withClassName("Class2").build();

            // Assert
            assertThat(stepDef1.getMethodName()).isEqualTo("method1");
            assertThat(stepDef1.getClassName()).isEqualTo("Class1");
            assertThat(stepDef2.getMethodName()).isEqualTo("method2");
            assertThat(stepDef2.getClassName()).isEqualTo("Class2");
        }
    }

    @Nested
    @DisplayName("Cucumber Annotations")
    class CucumberAnnotations {

        @Test
        @DisplayName("should handle Given annotation")
        void shouldHandleGivenAnnotation() {
            // Arrange
            String methodText = "@Given(\"^I am on the login page$\")\npublic void iAmOnTheLoginPage() {\n    // Implementation\n}";

            // Act
            StepDefinitionInfo stepDef = new StepDefinitionInfo.Builder()
                .withMethodText(methodText)
                .withStepPattern("^I am on the login page$")
                .build();

            // Assert
            assertThat(stepDef.getMethodText()).contains("@Given");
            assertThat(stepDef.getStepPattern()).isEqualTo("^I am on the login page$");
        }

        @Test
        @DisplayName("should handle When annotation")
        void shouldHandleWhenAnnotation() {
            // Arrange
            String methodText = "@When(\"^I click the login button$\")\npublic void iClickTheLoginButton() {\n    // Implementation\n}";

            // Act
            StepDefinitionInfo stepDef = new StepDefinitionInfo.Builder()
                .withMethodText(methodText)
                .withStepPattern("^I click the login button$")
                .build();

            // Assert
            assertThat(stepDef.getMethodText()).contains("@When");
            assertThat(stepDef.getStepPattern()).isEqualTo("^I click the login button$");
        }

        @Test
        @DisplayName("should handle Then annotation")
        void shouldHandleThenAnnotation() {
            // Arrange
            String methodText = "@Then(\"^I should be logged in$\")\npublic void iShouldBeLoggedIn() {\n    // Implementation\n}";

            // Act
            StepDefinitionInfo stepDef = new StepDefinitionInfo.Builder()
                .withMethodText(methodText)
                .withStepPattern("^I should be logged in$")
                .build();

            // Assert
            assertThat(stepDef.getMethodText()).contains("@Then");
            assertThat(stepDef.getStepPattern()).isEqualTo("^I should be logged in$");
        }

        @Test
        @DisplayName("should handle Step annotation")
        void shouldHandleStepAnnotation() {
            // Arrange
            String methodText = "@Step(\"^I enter (.*?) in the (.*?) field$\")\npublic void iEnterValueInField(String value, String fieldName) {\n    // Implementation\n}";

            // Act
            StepDefinitionInfo stepDef = new StepDefinitionInfo.Builder()
                .withMethodText(methodText)
                .withStepPattern("^I enter (.*?) in the (.*?) field$")
                .withParameters(Arrays.asList("value", "fieldName"))
                .build();

            // Assert
            assertThat(stepDef.getMethodText()).contains("@Step");
            assertThat(stepDef.getStepPattern()).isEqualTo("^I enter (.*?) in the (.*?) field$");
            assertThat(stepDef.getParameters()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("should handle empty strings gracefully")
        void shouldHandleEmptyStringsGracefully() {
            // Act
            StepDefinitionInfo stepDef = new StepDefinitionInfo.Builder()
                .withMethodName("")
                .withClassName("")
                .withPackageName("")
                .withSourceFilePath("")
                .withStepPattern("")
                .withMethodText("")
                .build();

            // Assert
            assertThat(stepDef.getMethodName()).isEmpty();
            assertThat(stepDef.getClassName()).isEmpty();
            assertThat(stepDef.getPackageName()).isEmpty();
            assertThat(stepDef.getSourceFilePath()).isEmpty();
            assertThat(stepDef.getStepPattern()).isEmpty();
            assertThat(stepDef.getMethodText()).isEmpty();
        }

        @Test
        @DisplayName("should handle very long method names")
        void shouldHandleVeryLongMethodNames() {
            // Arrange
            String longMethodName = "thisIsAVeryLongMethodNameThatContainsManyWordsAndShouldBeHandledGracefullyByTheStepDefinitionInfoModelWithoutCausingAnyIssuesOrExceptions";

            // Act
            StepDefinitionInfo stepDef = new StepDefinitionInfo.Builder()
                .withMethodName(longMethodName)
                .build();

            // Assert
            assertThat(stepDef.getMethodName()).isEqualTo(longMethodName);
            assertThat(stepDef.getMethodName().length()).isGreaterThan(100);
        }

        @Test
        @DisplayName("should handle special characters in method text")
        void shouldHandleSpecialCharactersInMethodText() {
            // Arrange
            String specialCharsText = "Method with special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?\\`~";

            // Act
            StepDefinitionInfo stepDef = new StepDefinitionInfo.Builder()
                .withMethodText(specialCharsText)
                .build();

            // Assert
            assertThat(stepDef.getMethodText()).isEqualTo(specialCharsText);
            assertThat(stepDef.getMethodText()).contains("!");
            assertThat(stepDef.getMethodText()).contains("@");
            assertThat(stepDef.getMethodText()).contains("#");
        }

        @Test
        @DisplayName("should handle negative line numbers")
        void shouldHandleNegativeLineNumbers() {
            // Arrange
            int negativeLineNumber = -1;

            // Act
            StepDefinitionInfo stepDef = new StepDefinitionInfo.Builder()
                .withLineNumber(negativeLineNumber)
                .build();

            // Assert
            assertThat(stepDef.getLineNumber()).isEqualTo(-1);
        }

        @Test
        @DisplayName("should handle unicode characters")
        void shouldHandleUnicodeCharacters() {
            // Arrange
            String unicodeText = "Method with unicode: café résumé naïve 测试 テスト";

            // Act
            StepDefinitionInfo stepDef = new StepDefinitionInfo.Builder()
                .withMethodName(unicodeText)
                .build();

            // Assert
            assertThat(stepDef.getMethodName()).isEqualTo(unicodeText);
            assertThat(stepDef.getMethodName()).contains("é");
            assertThat(stepDef.getMethodName()).contains("ï");
            assertThat(stepDef.getMethodName()).contains("测试");
        }
    }

    @Nested
    @DisplayName("Comprehensive Step Definition")
    class ComprehensiveStepDefinition {

        @Test
        @DisplayName("should create comprehensive step definition with all fields")
        void shouldCreateComprehensiveStepDefinitionWithAllFields() {
            // Arrange
            String methodName = "iEnterCredentialsAndClickLogin";
            String className = "LoginStepDefinitions";
            String packageName = "com.example.steps";
            String sourceFilePath = "src/test/java/com/example/steps/LoginStepDefinitions.java";
            int lineNumber = 45;
            String stepPattern = "^I enter (.*?) as username and (.*?) as password and click the login button$";
            List<String> parameters = Arrays.asList("username", "password");
            String methodText = "@When(\"^I enter (.*?) as username and (.*?) as password and click the login button$\")\n" +
                "public void iEnterCredentialsAndClickLogin(String username, String password) {\n" +
                "    // Enter username\n" +
                "    driver.findElement(By.id(\"username\")).sendKeys(username);\n" +
                "    // Enter password\n" +
                "    driver.findElement(By.id(\"password\")).sendKeys(password);\n" +
                "    // Click login button\n" +
                "    driver.findElement(By.id(\"login-button\")).click();\n" +
                "}";

            // Act
            StepDefinitionInfo stepDef = new StepDefinitionInfo.Builder()
                .withMethodName(methodName)
                .withClassName(className)
                .withPackageName(packageName)
                .withSourceFilePath(sourceFilePath)
                .withLineNumber(lineNumber)
                .withStepPattern(stepPattern)
                .withParameters(parameters)
                .withMethodText(methodText)
                .build();

            // Assert
            assertThat(stepDef).isNotNull();
            assertThat(stepDef.getMethodName()).isEqualTo(methodName);
            assertThat(stepDef.getClassName()).isEqualTo(className);
            assertThat(stepDef.getPackageName()).isEqualTo(packageName);
            assertThat(stepDef.getSourceFilePath()).isEqualTo(sourceFilePath);
            assertThat(stepDef.getLineNumber()).isEqualTo(lineNumber);
            assertThat(stepDef.getStepPattern()).isEqualTo(stepPattern);
            assertThat(stepDef.getParameters()).hasSize(2);
            assertThat(stepDef.getParameters()).contains("username");
            assertThat(stepDef.getParameters()).contains("password");
            assertThat(stepDef.getMethodText()).contains("@When");
            assertThat(stepDef.getMethodText()).contains("public void " + methodName);
            assertThat(stepDef.getMethodText()).contains("String username, String password");
            assertThat(stepDef.getMethodText()).contains("driver.findElement");
            assertThat(stepDef.getMethodText()).contains("sendKeys");
            assertThat(stepDef.getMethodText()).contains("click()");
        }
    }
}
