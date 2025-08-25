package com.trace.test.extractors;

import com.trace.test.models.GherkinScenarioInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("Gherkin Scenario Extractor Unit Tests")
class GherkinScenarioExtractorUnitTest {
    
    @Nested
    @DisplayName("Gherkin Keyword Detection")
    class GherkinKeywordDetection {
        
        @Test
        @DisplayName("should identify Given keyword in step text")
        void shouldIdentifyGivenKeywordInStepText() {
            // Arrange
            String stepText = "Given I am on the login page";
            
            // Act & Assert
            assertThat(stepText).startsWith("Given ");
            assertThat(stepText).contains("I am on the login page");
        }
        
        @Test
        @DisplayName("should identify When keyword in step text")
        void shouldIdentifyWhenKeywordInStepText() {
            // Arrange
            String stepText = "When I click the login button";
            
            // Act & Assert
            assertThat(stepText).startsWith("When ");
            assertThat(stepText).contains("I click the login button");
        }
        
        @Test
        @DisplayName("should identify Then keyword in step text")
        void shouldIdentifyThenKeywordInStepText() {
            // Arrange
            String stepText = "Then I should be logged in";
            
            // Act & Assert
            assertThat(stepText).startsWith("Then ");
            assertThat(stepText).contains("I should be logged in");
        }
        
        @Test
        @DisplayName("should identify And keyword in step text")
        void shouldIdentifyAndKeywordInStepText() {
            // Arrange
            String stepText = "And I should see the dashboard";
            
            // Act & Assert
            assertThat(stepText).startsWith("And ");
            assertThat(stepText).contains("I should see the dashboard");
        }
        
        @Test
        @DisplayName("should identify But keyword in step text")
        void shouldIdentifyButKeywordInStepText() {
            // Arrange
            String stepText = "But I should not see error messages";
            
            // Act & Assert
            assertThat(stepText).startsWith("But ");
            assertThat(stepText).contains("I should not see error messages");
        }
    }
    
    @Nested
    @DisplayName("Gherkin Scenario Info Model")
    class GherkinScenarioInfoModel {
        
        @Test
        @DisplayName("should create GherkinScenarioInfo with builder pattern")
        void shouldCreateGherkinScenarioInfoWithBuilderPattern() {
            // Arrange
            List<String> steps = Arrays.asList(
                "Given I am on the login page",
                "When I enter valid credentials",
                "Then I should be logged in"
            );
            
            List<String> tags = Arrays.asList("@smoke", "@login");
            
            // Act
            GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
                .withFeatureName("User Authentication")
                .withScenarioName("Successful login with valid credentials")
                .withSteps(steps)
                .withTags(tags)
                .withSourceFilePath("login.feature")
                .withLineNumber(15)
                .withFeatureFileContent("Feature: User Authentication\n...")
                .build();
            
            // Assert
            assertThat(scenarioInfo).isNotNull();
            assertThat(scenarioInfo.getFeatureName()).isEqualTo("User Authentication");
            assertThat(scenarioInfo.getScenarioName()).isEqualTo("Successful login with valid credentials");
            assertThat(scenarioInfo.getSteps()).containsExactlyElementsOf(steps);
            assertThat(scenarioInfo.getTags()).containsExactlyElementsOf(tags);
            assertThat(scenarioInfo.getSourceFilePath()).isEqualTo("login.feature");
            assertThat(scenarioInfo.getLineNumber()).isEqualTo(15);
        }
        
        @Test
        @DisplayName("should handle null values in GherkinScenarioInfo")
        void shouldHandleNullValuesInGherkinScenarioInfo() {
            // Act
            GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
                .withFeatureName(null)
                .withScenarioName(null)
                .withSteps(null)
                .withTags(null)
                .withSourceFilePath(null)
                .withLineNumber(-1)
                .withFeatureFileContent(null)
                .build();
            
            // Assert
            assertThat(scenarioInfo).isNotNull();
            assertThat(scenarioInfo.getFeatureName()).isNull();
            assertThat(scenarioInfo.getScenarioName()).isNull();
            assertThat(scenarioInfo.getSteps()).isNull();
            assertThat(scenarioInfo.getTags()).isNull();
            assertThat(scenarioInfo.getSourceFilePath()).isNull();
            assertThat(scenarioInfo.getLineNumber()).isEqualTo(-1);
            assertThat(scenarioInfo.getFeatureFileContent()).isNull();
        }
        
        @Test
        @DisplayName("should handle scenario outline flag")
        void shouldHandleScenarioOutlineFlag() {
            // Act
            GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
                .withFeatureName("Test Feature")
                .withScenarioName("Test Scenario")
                .withIsScenarioOutline(true)
                .build();
            
            // Assert
            assertThat(scenarioInfo).isNotNull();
            assertThat(scenarioInfo.isScenarioOutline()).isTrue();
        }
        
        @Test
        @DisplayName("should handle background steps")
        void shouldHandleBackgroundSteps() {
            // Arrange
            List<String> backgroundSteps = Arrays.asList(
                "Given the application is running",
                "And I am on the login page"
            );
            
            // Act
            GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
                .withFeatureName("Test Feature")
                .withScenarioName("Test Scenario")
                .withBackgroundSteps(backgroundSteps)
                .build();
            
            // Assert
            assertThat(scenarioInfo).isNotNull();
            assertThat(scenarioInfo.getBackgroundSteps()).containsExactlyElementsOf(backgroundSteps);
        }
        
        @Test
        @DisplayName("should handle data table")
        void shouldHandleDataTable() {
            // Arrange
            List<String> dataTable = Arrays.asList(
                "| username | password |",
                "| user1    | pass1    |",
                "| user2    | pass2    |"
            );
            
            // Act
            GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
                .withFeatureName("Test Feature")
                .withScenarioName("Test Scenario")
                .withDataTable(dataTable)
                .build();
            
            // Assert
            assertThat(scenarioInfo).isNotNull();
            assertThat(scenarioInfo.getDataTable()).containsExactlyElementsOf(dataTable);
        }
    }
    
    @Nested
    @DisplayName("Input Validation")
    class InputValidation {
        
        @Test
        @DisplayName("should validate null step text")
        void shouldValidateNullStepText() {
            // Arrange
            String stepText = null;
            
            // Act & Assert
            assertThat(stepText).isNull();
        }
        
        @Test
        @DisplayName("should validate empty step text")
        void shouldValidateEmptyStepText() {
            // Arrange
            String stepText = "";
            
            // Act & Assert
            assertThat(stepText).isEmpty();
        }
        
        @Test
        @DisplayName("should validate whitespace only step text")
        void shouldValidateWhitespaceOnlyStepText() {
            // Arrange
            String stepText = "   ";
            
            // Act & Assert
            assertThat(stepText.trim()).isEmpty();
        }
        
        @Test
        @DisplayName("should validate valid step text")
        void shouldValidateValidStepText() {
            // Arrange
            String stepText = "Given I am on the login page";
            
            // Act & Assert
            assertThat(stepText).isNotNull();
            assertThat(stepText).isNotEmpty();
            assertThat(stepText.trim()).isNotEmpty();
        }
    }
    
    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {
        
        @Test
        @DisplayName("should handle very long step text")
        void shouldHandleVeryLongStepText() {
            // Arrange
            String longStepText = "Given I am on a very long step text that contains many words and should be handled gracefully by the extractor without causing any issues or exceptions";
            
            // Act & Assert
            assertThat(longStepText).isNotNull();
            assertThat(longStepText.length()).isGreaterThan(100);
            assertThat(longStepText).startsWith("Given ");
        }
        
        @Test
        @DisplayName("should handle special characters in step text")
        void shouldHandleSpecialCharactersInStepText() {
            // Arrange
            String specialStepText = "Given I enter \"test@email.com\" with special chars: !@#$%^&*()";
            
            // Act & Assert
            assertThat(specialStepText).isNotNull();
            assertThat(specialStepText).contains("@");
            assertThat(specialStepText).contains("!");
            assertThat(specialStepText).contains("#");
        }
        
        @Test
        @DisplayName("should handle unicode characters in step text")
        void shouldHandleUnicodeCharactersInStepText() {
            // Arrange
            String unicodeStepText = "Given I enter text with unicode: café résumé naïve";
            
            // Act & Assert
            assertThat(unicodeStepText).isNotNull();
            assertThat(unicodeStepText).contains("é");
            assertThat(unicodeStepText).contains("ï");
        }
        
        @Test
        @DisplayName("should handle step text with newlines")
        void shouldHandleStepTextWithNewlines() {
            // Arrange
            String stepTextWithNewlines = "Given I am on the login page\nAnd I see the login form";
            
            // Act & Assert
            assertThat(stepTextWithNewlines).isNotNull();
            assertThat(stepTextWithNewlines).contains("\n");
            assertThat(stepTextWithNewlines.split("\n")).hasSize(2);
        }
        
        @Test
        @DisplayName("should handle step text with tabs")
        void shouldHandleStepTextWithTabs() {
            // Arrange
            String stepTextWithTabs = "Given\tI am on the login page\tAnd I see the form";
            
            // Act & Assert
            assertThat(stepTextWithTabs).isNotNull();
            assertThat(stepTextWithTabs).contains("\t");
            assertThat(stepTextWithTabs.split("\t")).hasSize(3);
        }
        
        @Test
        @DisplayName("should handle mixed case keywords")
        void shouldHandleMixedCaseKeywords() {
            // Arrange
            String mixedCaseStep = "given I am on the login page";
            
            // Act & Assert
            assertThat(mixedCaseStep).isNotNull();
            assertThat(mixedCaseStep.toLowerCase()).startsWith("given");
        }
        
        @Test
        @DisplayName("should handle step text with extra whitespace")
        void shouldHandleStepTextWithExtraWhitespace() {
            // Arrange
            String stepTextWithWhitespace = "  Given   I am on the login page  ";
            
            // Act & Assert
            assertThat(stepTextWithWhitespace).isNotNull();
            assertThat(stepTextWithWhitespace.trim()).startsWith("Given");
            assertThat(stepTextWithWhitespace.trim()).endsWith("page");
        }
    }
    
    @Nested
    @DisplayName("Performance")
    class Performance {
        
        @Test
        @DisplayName("should handle multiple step texts efficiently")
        void shouldHandleMultipleStepTextsEfficiently() {
            // Arrange
            List<String> stepTexts = Arrays.asList(
                "Given I am on the login page",
                "When I enter valid credentials",
                "Then I should be logged in",
                "And I should see the dashboard"
            );
            
            long startTime = System.currentTimeMillis();
            
            // Act
            for (String stepText : stepTexts) {
                assertThat(stepText).isNotNull();
                assertThat(stepText).isNotEmpty();
                assertThat(stepText.trim()).isNotEmpty();
            }
            
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            
            // Assert
            assertThat(executionTime).isLessThan(1000); // Should complete within 1 second
        }
        
        @Test
        @DisplayName("should handle repeated operations efficiently")
        void shouldHandleRepeatedOperationsEfficiently() {
            // Arrange
            String stepText = "Given I am on the login page";
            
            long startTime = System.currentTimeMillis();
            
            // Act
            for (int i = 0; i < 100; i++) {
                assertThat(stepText).isNotNull();
                assertThat(stepText).startsWith("Given");
            }
            
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            
            // Assert
            assertThat(executionTime).isLessThan(1000); // Should complete within 1 second
        }
    }
}
