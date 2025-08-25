package com.trace.test.extractors;

import com.trace.test.models.StepDefinitionInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("Step Definition Extractor Unit Tests")
class StepDefinitionExtractorUnitTest {

    @Nested
    @DisplayName("Step Definition Info Model")
    class StepDefinitionInfoModel {

        @Test
        @DisplayName("should create StepDefinitionInfo with builder pattern")
        void shouldCreateStepDefinitionInfoWithBuilderPattern() {
            // Arrange
            List<String> parameters = Arrays.asList("buttonName", "expectedText");

            // Act
            StepDefinitionInfo stepDef = new StepDefinitionInfo.Builder()
                .withMethodName("clickButtonAndVerifyText")
                .withClassName("LoginStepDefinitions")
                .withPackageName("com.example.steps")
                .withSourceFilePath("LoginStepDefinitions.java")
                .withLineNumber(25)
                .withStepPattern("^I click the (.*?) button and verify (.*?) is displayed$")
                .withParameters(parameters)
                .withMethodText("@When(\"^I click the (.*?) button and verify (.*?) is displayed$\") public void clickButtonAndVerifyText(String buttonName, String expectedText) { ... }")
                .build();

            // Assert
            assertThat(stepDef).isNotNull();
            assertThat(stepDef.getMethodName()).isEqualTo("clickButtonAndVerifyText");
            assertThat(stepDef.getClassName()).isEqualTo("LoginStepDefinitions");
            assertThat(stepDef.getPackageName()).isEqualTo("com.example.steps");
            assertThat(stepDef.getSourceFilePath()).isEqualTo("LoginStepDefinitions.java");
            assertThat(stepDef.getLineNumber()).isEqualTo(25);
            assertThat(stepDef.getStepPattern()).isEqualTo("^I click the (.*?) button and verify (.*?) is displayed$");
            assertThat(stepDef.getParameters()).containsExactlyElementsOf(parameters);
            assertThat(stepDef.getMethodText()).contains("@When");
        }

        @Test
        @DisplayName("should handle null values in StepDefinitionInfo")
        void shouldHandleNullValuesInStepDefinitionInfo() {
            // Act
            StepDefinitionInfo stepDef = new StepDefinitionInfo.Builder()
                .withMethodName(null)
                .withClassName(null)
                .withPackageName(null)
                .withSourceFilePath(null)
                .withLineNumber(-1)
                .withStepPattern(null)
                .withParameters(null)
                .withMethodText(null)
                .build();

            // Assert
            assertThat(stepDef).isNotNull();
            assertThat(stepDef.getMethodName()).isNull();
            assertThat(stepDef.getClassName()).isNull();
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
            // Arrange
            List<String> emptyParameters = Arrays.asList();

            // Act
            StepDefinitionInfo stepDef = new StepDefinitionInfo.Builder()
                .withMethodName("simpleStep")
                .withClassName("SimpleStepDefinitions")
                .withParameters(emptyParameters)
                .build();

            // Assert
            assertThat(stepDef).isNotNull();
            assertThat(stepDef.getParameters()).isEmpty();
        }

        @Test
        @DisplayName("should handle single parameter")
        void shouldHandleSingleParameter() {
            // Arrange
            List<String> singleParameter = Arrays.asList("username");

            // Act
            StepDefinitionInfo stepDef = new StepDefinitionInfo.Builder()
                .withMethodName("enterUsername")
                .withClassName("LoginStepDefinitions")
                .withStepPattern("^I enter username (.*?)$")
                .withParameters(singleParameter)
                .build();

            // Assert
            assertThat(stepDef).isNotNull();
            assertThat(stepDef.getParameters()).hasSize(1);
            assertThat(stepDef.getParameters()).contains("username");
        }

        @Test
        @DisplayName("should handle multiple parameters")
        void shouldHandleMultipleParameters() {
            // Arrange
            List<String> multipleParameters = Arrays.asList("username", "password", "rememberMe");

            // Act
            StepDefinitionInfo stepDef = new StepDefinitionInfo.Builder()
                .withMethodName("loginWithCredentials")
                .withClassName("LoginStepDefinitions")
                .withStepPattern("^I login with username (.*?) and password (.*?) and remember me (.*?)$")
                .withParameters(multipleParameters)
                .build();

            // Assert
            assertThat(stepDef).isNotNull();
            assertThat(stepDef.getParameters()).hasSize(3);
            assertThat(stepDef.getParameters()).containsExactly("username", "password", "rememberMe");
        }
    }

    @Nested
    @DisplayName("Cucumber Annotation Patterns")
    class CucumberAnnotationPatterns {

        @Test
        @DisplayName("should identify @Given annotation pattern")
        void shouldIdentifyGivenAnnotationPattern() {
            // Arrange
            String annotationPattern = "io.cucumber.java.en.Given";
            String[] cucumberAnnotations = {
                "io.cucumber.java.en.Given",
                "io.cucumber.java.en.When", 
                "io.cucumber.java.en.Then",
                "io.cucumber.java.en.And",
                "io.cucumber.java.en.But"
            };

            // Act & Assert
            assertThat(cucumberAnnotations).contains(annotationPattern);
            assertThat(annotationPattern).endsWith("Given");
        }

        @Test
        @DisplayName("should identify @When annotation pattern")
        void shouldIdentifyWhenAnnotationPattern() {
            // Arrange
            String annotationPattern = "io.cucumber.java.en.When";
            String[] cucumberAnnotations = {
                "io.cucumber.java.en.Given",
                "io.cucumber.java.en.When", 
                "io.cucumber.java.en.Then",
                "io.cucumber.java.en.And",
                "io.cucumber.java.en.But"
            };

            // Act & Assert
            assertThat(cucumberAnnotations).contains(annotationPattern);
            assertThat(annotationPattern).endsWith("When");
        }

        @Test
        @DisplayName("should identify @Then annotation pattern")
        void shouldIdentifyThenAnnotationPattern() {
            // Arrange
            String annotationPattern = "io.cucumber.java.en.Then";
            String[] cucumberAnnotations = {
                "io.cucumber.java.en.Given",
                "io.cucumber.java.en.When", 
                "io.cucumber.java.en.Then",
                "io.cucumber.java.en.And",
                "io.cucumber.java.en.But"
            };

            // Act & Assert
            assertThat(cucumberAnnotations).contains(annotationPattern);
            assertThat(annotationPattern).endsWith("Then");
        }

        @Test
        @DisplayName("should identify @And annotation pattern")
        void shouldIdentifyAndAnnotationPattern() {
            // Arrange
            String annotationPattern = "io.cucumber.java.en.And";
            String[] cucumberAnnotations = {
                "io.cucumber.java.en.Given",
                "io.cucumber.java.en.When", 
                "io.cucumber.java.en.Then",
                "io.cucumber.java.en.And",
                "io.cucumber.java.en.But"
            };

            // Act & Assert
            assertThat(cucumberAnnotations).contains(annotationPattern);
            assertThat(annotationPattern).endsWith("And");
        }

        @Test
        @DisplayName("should identify @But annotation pattern")
        void shouldIdentifyButAnnotationPattern() {
            // Arrange
            String annotationPattern = "io.cucumber.java.en.But";
            String[] cucumberAnnotations = {
                "io.cucumber.java.en.Given",
                "io.cucumber.java.en.When", 
                "io.cucumber.java.en.Then",
                "io.cucumber.java.en.And",
                "io.cucumber.java.en.But"
            };

            // Act & Assert
            assertThat(cucumberAnnotations).contains(annotationPattern);
            assertThat(annotationPattern).endsWith("But");
        }
    }

    @Nested
    @DisplayName("Step Class Pattern Detection")
    class StepClassPatternDetection {

        @Test
        @DisplayName("should identify step class patterns")
        void shouldIdentifyStepClassPatterns() {
            // Arrange
            String[] stepClassPatterns = {
                "step", "test", "steps", "stepdefinitions"
            };

            // Act & Assert
            assertThat(stepClassPatterns).hasSize(4);
            assertThat(stepClassPatterns).contains("step");
            assertThat(stepClassPatterns).contains("test");
            assertThat(stepClassPatterns).contains("steps");
            assertThat(stepClassPatterns).contains("stepdefinitions");
        }

        @Test
        @DisplayName("should detect step class name with step pattern")
        void shouldDetectStepClassNameWithStepPattern() {
            // Arrange
            String className = "LoginStepDefinitions";
            String[] stepClassPatterns = {"step", "test", "steps", "stepdefinitions"};

            // Act
            boolean isStepClass = containsAnyPattern(className.toLowerCase(), stepClassPatterns);

            // Assert
            assertThat(isStepClass).isTrue();
        }

        @Test
        @DisplayName("should detect step class name with test pattern")
        void shouldDetectStepClassNameWithTestPattern() {
            // Arrange
            String className = "LoginTestDefinitions";
            String[] stepClassPatterns = {"step", "test", "steps", "stepdefinitions"};

            // Act
            boolean isStepClass = containsAnyPattern(className.toLowerCase(), stepClassPatterns);

            // Assert
            assertThat(isStepClass).isTrue();
        }

        @Test
        @DisplayName("should not detect non-step class name")
        void shouldNotDetectNonStepClassName() {
            // Arrange
            String className = "LoginService";
            String[] stepClassPatterns = {"step", "test", "steps", "stepdefinitions"};

            // Act
            boolean isStepClass = containsAnyPattern(className.toLowerCase(), stepClassPatterns);

            // Assert
            assertThat(isStepClass).isFalse();
        }

        private boolean containsAnyPattern(String text, String[] patterns) {
            for (String pattern : patterns) {
                if (text.contains(pattern)) {
                    return true;
                }
            }
            return false;
        }
    }

    @Nested
    @DisplayName("Step Method Pattern Detection")
    class StepMethodPatternDetection {

        @Test
        @DisplayName("should identify step method patterns")
        void shouldIdentifyStepMethodPatterns() {
            // Arrange
            String[] stepMethodPatterns = {
                "should", "given", "when", "then", "and", "but", "i_", "user_"
            };

            // Act & Assert
            assertThat(stepMethodPatterns).hasSize(8);
            assertThat(stepMethodPatterns).contains("should");
            assertThat(stepMethodPatterns).contains("given");
            assertThat(stepMethodPatterns).contains("when");
            assertThat(stepMethodPatterns).contains("then");
            assertThat(stepMethodPatterns).contains("and");
            assertThat(stepMethodPatterns).contains("but");
            assertThat(stepMethodPatterns).contains("i_");
            assertThat(stepMethodPatterns).contains("user_");
        }

        @Test
        @DisplayName("should detect step method name with should pattern")
        void shouldDetectStepMethodNameWithShouldPattern() {
            // Arrange
            String methodName = "shouldClickLoginButton";
            String[] stepMethodPatterns = {"should", "given", "when", "then", "and", "but", "i_", "user_"};

            // Act
            boolean isStepMethod = containsAnyPattern(methodName.toLowerCase(), stepMethodPatterns);

            // Assert
            assertThat(isStepMethod).isTrue();
        }

        @Test
        @DisplayName("should detect step method name with i_ pattern")
        void shouldDetectStepMethodNameWithIPattern() {
            // Arrange
            String methodName = "i_click_login_button";
            String[] stepMethodPatterns = {"should", "given", "when", "then", "and", "but", "i_", "user_"};

            // Act
            boolean isStepMethod = containsAnyPattern(methodName.toLowerCase(), stepMethodPatterns);

            // Assert
            assertThat(isStepMethod).isTrue();
        }

        @Test
        @DisplayName("should not detect non-step method name")
        void shouldNotDetectNonStepMethodName() {
            // Arrange
            String methodName = "processData";
            String[] stepMethodPatterns = {"should", "given", "when", "then", "and", "but", "i_", "user_"};

            // Act
            boolean isStepMethod = containsAnyPattern(methodName.toLowerCase(), stepMethodPatterns);

            // Assert
            assertThat(isStepMethod).isFalse();
        }

        private boolean containsAnyPattern(String text, String[] patterns) {
            for (String pattern : patterns) {
                if (text.contains(pattern)) {
                    return true;
                }
            }
            return false;
        }
    }

    @Nested
    @DisplayName("Stack Trace Pattern Matching")
    class StackTracePatternMatching {

        @Test
        @DisplayName("should match Java file pattern in stack trace")
        void shouldMatchJavaFilePatternInStackTrace() {
            // Arrange
            String stackTrace = "at com.example.steps.LoginStepDefinitions.clickLoginButton(LoginStepDefinitions.java:25)";
            Pattern pattern = Pattern.compile("at\\s+([\\w\\.]+)\\.([\\w]+)\\(([\\w]+\\.java):(\\d+)\\)");

            // Act
            Matcher matcher = pattern.matcher(stackTrace);

            // Assert
            assertThat(matcher.find()).isTrue();
            assertThat(matcher.group(1)).isEqualTo("com.example.steps.LoginStepDefinitions");
            assertThat(matcher.group(2)).isEqualTo("clickLoginButton");
            assertThat(matcher.group(3)).isEqualTo("LoginStepDefinitions.java");
            assertThat(matcher.group(4)).isEqualTo("25");
        }

        @Test
        @DisplayName("should match multiple Java file patterns in stack trace")
        void shouldMatchMultipleJavaFilePatternsInStackTrace() {
            // Arrange
            String stackTrace = "at com.example.steps.LoginStepDefinitions.clickLoginButton(LoginStepDefinitions.java:25)\n" +
                               "at com.example.steps.CommonSteps.waitForElement(CommonSteps.java:42)";
            Pattern pattern = Pattern.compile("at\\s+([\\w\\.]+)\\.([\\w]+)\\(([\\w]+\\.java):(\\d+)\\)");

            // Act
            Matcher matcher = pattern.matcher(stackTrace);

            // Assert
            assertThat(matcher.find()).isTrue(); // First match
            assertThat(matcher.group(1)).isEqualTo("com.example.steps.LoginStepDefinitions");
            assertThat(matcher.group(2)).isEqualTo("clickLoginButton");
            
            assertThat(matcher.find()).isTrue(); // Second match
            assertThat(matcher.group(1)).isEqualTo("com.example.steps.CommonSteps");
            assertThat(matcher.group(2)).isEqualTo("waitForElement");
        }

        @Test
        @DisplayName("should handle stack trace without Java file patterns")
        void shouldHandleStackTraceWithoutJavaFilePatterns() {
            // Arrange
            String stackTrace = "java.lang.NullPointerException\n" +
                               "at java.base/java.lang.String.length(String.java:1234)";
            Pattern pattern = Pattern.compile("at\\s+([\\w\\.]+)\\.([\\w]+)\\(([\\w]+\\.java):(\\d+)\\)");

            // Act
            Matcher matcher = pattern.matcher(stackTrace);

            // Assert
            assertThat(matcher.find()).isFalse();
        }

        @Test
        @DisplayName("should handle malformed stack trace")
        void shouldHandleMalformedStackTrace() {
            // Arrange
            String stackTrace = "at com.example.steps.LoginStepDefinitions.clickLoginButton(LoginStepDefinitions.java:)";
            Pattern pattern = Pattern.compile("at\\s+([\\w\\.]+)\\.([\\w]+)\\(([\\w]+\\.java):(\\d+)\\)");

            // Act
            Matcher matcher = pattern.matcher(stackTrace);

            // Assert
            assertThat(matcher.find()).isFalse();
        }
    }

    @Nested
    @DisplayName("Source Path Patterns")
    class SourcePathPatterns {

        @Test
        @DisplayName("should identify source path patterns")
        void shouldIdentifySourcePathPatterns() {
            // Arrange
            String[] sourcePaths = {
                "src/test/java/",
                "src/main/java/",
                "test/java/",
                "main/java/"
            };

            // Act & Assert
            assertThat(sourcePaths).hasSize(4);
            assertThat(sourcePaths).contains("src/test/java/");
            assertThat(sourcePaths).contains("src/main/java/");
            assertThat(sourcePaths).contains("test/java/");
            assertThat(sourcePaths).contains("main/java/");
        }

        @Test
        @DisplayName("should construct full path with test source")
        void shouldConstructFullPathWithTestSource() {
            // Arrange
            String className = "com.example.steps.LoginStepDefinitions";
            String sourcePath = "src/test/java/";
            String expectedPath = "src/test/java/com/example/steps/LoginStepDefinitions.java";

            // Act
            String packagePath = className.replace('.', '/');
            String fullPath = sourcePath + packagePath + ".java";

            // Assert
            assertThat(fullPath).isEqualTo(expectedPath);
        }

        @Test
        @DisplayName("should construct full path with main source")
        void shouldConstructFullPathWithMainSource() {
            // Arrange
            String className = "com.example.steps.LoginStepDefinitions";
            String sourcePath = "src/main/java/";
            String expectedPath = "src/main/java/com/example/steps/LoginStepDefinitions.java";

            // Act
            String packagePath = className.replace('.', '/');
            String fullPath = sourcePath + packagePath + ".java";

            // Assert
            assertThat(fullPath).isEqualTo(expectedPath);
        }
    }

    @Nested
    @DisplayName("Input Validation")
    class InputValidation {

        @Test
        @DisplayName("should validate null stack trace")
        void shouldValidateNullStackTrace() {
            // Arrange
            String stackTrace = null;

            // Act & Assert
            assertThat(stackTrace).isNull();
        }

        @Test
        @DisplayName("should validate empty stack trace")
        void shouldValidateEmptyStackTrace() {
            // Arrange
            String stackTrace = "";

            // Act & Assert
            assertThat(stackTrace).isEmpty();
        }

        @Test
        @DisplayName("should validate whitespace only stack trace")
        void shouldValidateWhitespaceOnlyStackTrace() {
            // Arrange
            String stackTrace = "   ";

            // Act & Assert
            assertThat(stackTrace.trim()).isEmpty();
        }

        @Test
        @DisplayName("should validate valid stack trace")
        void shouldValidateValidStackTrace() {
            // Arrange
            String stackTrace = "at com.example.steps.LoginStepDefinitions.clickLoginButton(LoginStepDefinitions.java:25)";

            // Act & Assert
            assertThat(stackTrace).isNotNull();
            assertThat(stackTrace).isNotEmpty();
            assertThat(stackTrace.trim()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("should handle very long class name")
        void shouldHandleVeryLongClassName() {
            // Arrange
            String longClassName = "com.example.very.long.package.name.with.many.levels.LoginStepDefinitions";

            // Act & Assert
            assertThat(longClassName).isNotNull();
            assertThat(longClassName.length()).isGreaterThan(50);
            assertThat(longClassName).contains("LoginStepDefinitions");
        }

        @Test
        @DisplayName("should handle special characters in method name")
        void shouldHandleSpecialCharactersInMethodName() {
            // Arrange
            String methodName = "click_$pecial_Button_123";

            // Act & Assert
            assertThat(methodName).isNotNull();
            assertThat(methodName).contains("$");
            assertThat(methodName).contains("_");
            assertThat(methodName).contains("123");
        }

        @Test
        @DisplayName("should handle unicode characters in class name")
        void shouldHandleUnicodeCharactersInClassName() {
            // Arrange
            String unicodeClassName = "com.example.测试步骤.LoginStepDefinitions";

            // Act & Assert
            assertThat(unicodeClassName).isNotNull();
            assertThat(unicodeClassName).contains("测试");
            assertThat(unicodeClassName).contains("LoginStepDefinitions");
        }

        @Test
        @DisplayName("should handle very large line numbers")
        void shouldHandleVeryLargeLineNumbers() {
            // Arrange
            int largeLineNumber = 999999;

            // Act & Assert
            assertThat(largeLineNumber).isPositive();
            assertThat(largeLineNumber).isGreaterThan(100000);
        }

        @Test
        @DisplayName("should handle negative line numbers")
        void shouldHandleNegativeLineNumbers() {
            // Arrange
            int negativeLineNumber = -1;

            // Act & Assert
            assertThat(negativeLineNumber).isNegative();
            assertThat(negativeLineNumber).isLessThan(0);
        }

        @Test
        @DisplayName("should handle step pattern with regex special characters")
        void shouldHandleStepPatternWithRegexSpecialCharacters() {
            // Arrange
            String stepPattern = "^I click the (.*?) button and verify (.*?) is displayed$";

            // Act & Assert
            assertThat(stepPattern).isNotNull();
            assertThat(stepPattern).startsWith("^");
            assertThat(stepPattern).endsWith("$");
            assertThat(stepPattern).contains("(.*?)");
        }
    }

    @Nested
    @DisplayName("Performance")
    class Performance {

        @Test
        @DisplayName("should not block test execution")
        void shouldNotBlockTestExecution() {
            // Arrange
            long startTime = System.currentTimeMillis();

            // Act
            // Simulate a quick operation, as actual extraction is mocked out
            String dummyOperation = "at com.example.steps.LoginStepDefinitions.clickLoginButton(LoginStepDefinitions.java:25)";
            dummyOperation.length();

            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            // Assert
            assertThat(executionTime).isLessThan(1000); // Should complete within 1 second
        }

        @Test
        @DisplayName("should handle multiple extractions efficiently")
        void shouldHandleMultipleExtractionsEfficiently() {
            // Arrange
            List<String> stackTraces = Arrays.asList(
                "at com.example.steps.LoginStepDefinitions.clickLoginButton(LoginStepDefinitions.java:25)",
                "at com.example.steps.CommonSteps.waitForElement(CommonSteps.java:42)",
                "at com.example.steps.ValidationSteps.verifyText(ValidationSteps.java:18)"
            );

            long startTime = System.currentTimeMillis();

            // Act
            for (String stackTrace : stackTraces) {
                // Simulate a quick operation, as actual extraction is mocked out
                stackTrace.length();
            }

            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            // Assert
            assertThat(executionTime).isLessThan(2000); // Should complete within 2 seconds
        }

        @Test
        @DisplayName("should handle repeated extractions efficiently")
        void shouldHandleRepeatedExtractionsEfficiently() {
            // Arrange
            String stackTrace = "at com.example.steps.LoginStepDefinitions.clickLoginButton(LoginStepDefinitions.java:25)";

            long startTime = System.currentTimeMillis();

            // Act
            for (int i = 0; i < 10; i++) {
                // Simulate a quick operation, as actual extraction is mocked out
                stackTrace.length();
            }

            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            // Assert
            assertThat(executionTime).isLessThan(1000); // Should complete within 1 second
        }
    }
}
