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
@DisplayName("Gherkin Scenario Info Unit Tests")
class GherkinScenarioInfoUnitTest {

    @Nested
    @DisplayName("Constructor and Builder")
    class ConstructorAndBuilder {

        @Test
        @DisplayName("should create GherkinScenarioInfo with all required fields")
        void shouldCreateGherkinScenarioInfoWithAllRequiredFields() {
            // Arrange
            String featureName = "User Authentication";
            String scenarioName = "Successful login with valid credentials";
            List<String> steps = Arrays.asList("Given I am on the login page", "When I click the login button", "Then I should be logged in");
            List<String> tags = Arrays.asList("@smoke", "@login");
            List<String> backgroundSteps = Arrays.asList("Given the application is running");
            List<String> dataTable = Arrays.asList("| username | password |", "| user1    | pass1    |");
            String fullScenarioText = "Scenario: Successful login with valid credentials\nGiven I am on the login page\nWhen I click the login button\nThen I should be logged in";
            boolean isScenarioOutline = false;
            String sourceFilePath = "login.feature";
            int lineNumber = 15;
            String featureFileContent = "Feature: User Authentication\nBackground:\nGiven the application is running\n\nScenario: Successful login with valid credentials\nGiven I am on the login page\nWhen I click the login button\nThen I should be logged in";

            // Act
            GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo(featureName, scenarioName, steps, tags, backgroundSteps, dataTable, fullScenarioText, isScenarioOutline, sourceFilePath, lineNumber, featureFileContent);

            // Assert
            assertThat(scenarioInfo).isNotNull();
            assertThat(scenarioInfo.getFeatureName()).isEqualTo(featureName);
            assertThat(scenarioInfo.getScenarioName()).isEqualTo(scenarioName);
            assertThat(scenarioInfo.getSteps()).isEqualTo(steps);
            assertThat(scenarioInfo.getTags()).isEqualTo(tags);
            assertThat(scenarioInfo.getBackgroundSteps()).isEqualTo(backgroundSteps);
            assertThat(scenarioInfo.getDataTable()).isEqualTo(dataTable);
            assertThat(scenarioInfo.getFullScenarioText()).isEqualTo(fullScenarioText);
            assertThat(scenarioInfo.isScenarioOutline()).isEqualTo(isScenarioOutline);
            assertThat(scenarioInfo.getSourceFilePath()).isEqualTo(sourceFilePath);
            assertThat(scenarioInfo.getLineNumber()).isEqualTo(lineNumber);
            assertThat(scenarioInfo.getFeatureFileContent()).isEqualTo(featureFileContent);
        }

        @Test
        @DisplayName("should create GherkinScenarioInfo using builder pattern")
        void shouldCreateGherkinScenarioInfoUsingBuilderPattern() {
            // Arrange
            String featureName = "User Authentication";
            String scenarioName = "Successful login with valid credentials";
            List<String> steps = Arrays.asList("Given I am on the login page", "When I click the login button", "Then I should be logged in");

            // Act
            GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
                .withFeatureName(featureName)
                .withScenarioName(scenarioName)
                .withSteps(steps)
                .withTags(Arrays.asList("@smoke", "@login"))
                .withBackgroundSteps(Arrays.asList("Given the application is running"))
                .withDataTable(Arrays.asList("| username | password |", "| user1    | pass1    |"))
                .withFullScenarioText("Scenario: Successful login with valid credentials\nGiven I am on the login page\nWhen I click the login button\nThen I should be logged in")
                .withIsScenarioOutline(false)
                .withSourceFilePath("login.feature")
                .withLineNumber(15)
                .withFeatureFileContent("Feature: User Authentication\nBackground:\nGiven the application is running\n\nScenario: Successful login with valid credentials\nGiven I am on the login page\nWhen I click the login button\nThen I should be logged in")
                .build();

            // Assert
            assertThat(scenarioInfo).isNotNull();
            assertThat(scenarioInfo.getFeatureName()).isEqualTo(featureName);
            assertThat(scenarioInfo.getScenarioName()).isEqualTo(scenarioName);
            assertThat(scenarioInfo.getSteps()).isEqualTo(steps);
        }

        @Test
        @DisplayName("should handle null optional fields gracefully")
        void shouldHandleNullOptionalFieldsGracefully() {
            // Act
            GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
                .withFeatureName("User Authentication")
                .withScenarioName("Successful login with valid credentials")
                .withSteps(null)
                .withTags(null)
                .withBackgroundSteps(null)
                .withDataTable(null)
                .withFullScenarioText(null)
                .withIsScenarioOutline(false)
                .withSourceFilePath(null)
                .withLineNumber(-1)
                .withFeatureFileContent(null)
                .build();

            // Assert
            assertThat(scenarioInfo).isNotNull();
            assertThat(scenarioInfo.getFeatureName()).isEqualTo("User Authentication");
            assertThat(scenarioInfo.getScenarioName()).isEqualTo("Successful login with valid credentials");
            assertThat(scenarioInfo.getSteps()).isNull();
            assertThat(scenarioInfo.getTags()).isNull();
            assertThat(scenarioInfo.getBackgroundSteps()).isNull();
            assertThat(scenarioInfo.getDataTable()).isNull();
            assertThat(scenarioInfo.getFullScenarioText()).isNull();
            assertThat(scenarioInfo.isScenarioOutline()).isFalse();
            assertThat(scenarioInfo.getSourceFilePath()).isNull();
            assertThat(scenarioInfo.getLineNumber()).isEqualTo(-1);
            assertThat(scenarioInfo.getFeatureFileContent()).isNull();
        }

        @Test
        @DisplayName("should handle empty lists gracefully")
        void shouldHandleEmptyListsGracefully() {
            // Act
            GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
                .withFeatureName("User Authentication")
                .withScenarioName("Successful login with valid credentials")
                .withSteps(Arrays.asList())
                .withTags(Arrays.asList())
                .withBackgroundSteps(Arrays.asList())
                .withDataTable(Arrays.asList())
                .build();

            // Assert
            assertThat(scenarioInfo).isNotNull();
            assertThat(scenarioInfo.getSteps()).isEmpty();
            assertThat(scenarioInfo.getTags()).isEmpty();
            assertThat(scenarioInfo.getBackgroundSteps()).isEmpty();
            assertThat(scenarioInfo.getDataTable()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Scenario Information")
    class ScenarioInformation {

        @Test
        @DisplayName("should return correct feature name")
        void shouldReturnCorrectFeatureName() {
            // Arrange
            String featureName = "User Authentication";

            // Act
            GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
                .withFeatureName(featureName)
                .build();

            // Assert
            assertThat(scenarioInfo.getFeatureName()).isEqualTo(featureName);
        }

        @Test
        @DisplayName("should return correct scenario name")
        void shouldReturnCorrectScenarioName() {
            // Arrange
            String scenarioName = "Successful login with valid credentials";

            // Act
            GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
                .withScenarioName(scenarioName)
                .build();

            // Assert
            assertThat(scenarioInfo.getScenarioName()).isEqualTo(scenarioName);
        }

        @Test
        @DisplayName("should return correct steps list")
        void shouldReturnCorrectStepsList() {
            // Arrange
            List<String> steps = Arrays.asList("Given I am on the login page", "When I click the login button", "Then I should be logged in");

            // Act
            GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
                .withSteps(steps)
                .build();

            // Assert
            assertThat(scenarioInfo.getSteps()).isEqualTo(steps);
            assertThat(scenarioInfo.getSteps()).hasSize(3);
            assertThat(scenarioInfo.getSteps()).contains("Given I am on the login page");
            assertThat(scenarioInfo.getSteps()).contains("When I click the login button");
            assertThat(scenarioInfo.getSteps()).contains("Then I should be logged in");
        }

        @Test
        @DisplayName("should return correct tags list")
        void shouldReturnCorrectTagsList() {
            // Arrange
            List<String> tags = Arrays.asList("@smoke", "@login", "@regression");

            // Act
            GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
                .withTags(tags)
                .build();

            // Assert
            assertThat(scenarioInfo.getTags()).isEqualTo(tags);
            assertThat(scenarioInfo.getTags()).hasSize(3);
            assertThat(scenarioInfo.getTags()).contains("@smoke");
            assertThat(scenarioInfo.getTags()).contains("@login");
            assertThat(scenarioInfo.getTags()).contains("@regression");
        }

        @Test
        @DisplayName("should return correct background steps list")
        void shouldReturnCorrectBackgroundStepsList() {
            // Arrange
            List<String> backgroundSteps = Arrays.asList("Given the application is running", "And the database is connected");

            // Act
            GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
                .withBackgroundSteps(backgroundSteps)
                .build();

            // Assert
            assertThat(scenarioInfo.getBackgroundSteps()).isEqualTo(backgroundSteps);
            assertThat(scenarioInfo.getBackgroundSteps()).hasSize(2);
            assertThat(scenarioInfo.getBackgroundSteps()).contains("Given the application is running");
            assertThat(scenarioInfo.getBackgroundSteps()).contains("And the database is connected");
        }

        @Test
        @DisplayName("should return correct data table")
        void shouldReturnCorrectDataTable() {
            // Arrange
            List<String> dataTable = Arrays.asList("| username | password |", "| user1    | pass1    |", "| user2    | pass2    |");

            // Act
            GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
                .withDataTable(dataTable)
                .build();

            // Assert
            assertThat(scenarioInfo.getDataTable()).isEqualTo(dataTable);
            assertThat(scenarioInfo.getDataTable()).hasSize(3);
            assertThat(scenarioInfo.getDataTable()).contains("| username | password |");
            assertThat(scenarioInfo.getDataTable()).contains("| user1    | pass1    |");
            assertThat(scenarioInfo.getDataTable()).contains("| user2    | pass2    |");
        }

        @Test
        @DisplayName("should return correct full scenario text")
        void shouldReturnCorrectFullScenarioText() {
            // Arrange
            String fullScenarioText = "Scenario: Successful login with valid credentials\nGiven I am on the login page\nWhen I click the login button\nThen I should be logged in";

            // Act
            GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
                .withFullScenarioText(fullScenarioText)
                .build();

            // Assert
            assertThat(scenarioInfo.getFullScenarioText()).isEqualTo(fullScenarioText);
            assertThat(scenarioInfo.getFullScenarioText()).contains("Scenario: Successful login with valid credentials");
            assertThat(scenarioInfo.getFullScenarioText()).contains("Given I am on the login page");
        }

        @Test
        @DisplayName("should return correct scenario outline flag")
        void shouldReturnCorrectScenarioOutlineFlag() {
            // Act
            GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
                .withIsScenarioOutline(true)
                .build();

            // Assert
            assertThat(scenarioInfo.isScenarioOutline()).isTrue();
        }
    }

    @Nested
    @DisplayName("File Information")
    class FileInformation {

        @Test
        @DisplayName("should return correct source file path")
        void shouldReturnCorrectSourceFilePath() {
            // Arrange
            String sourceFilePath = "src/test/resources/features/login.feature";

            // Act
            GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
                .withSourceFilePath(sourceFilePath)
                .build();

            // Assert
            assertThat(scenarioInfo.getSourceFilePath()).isEqualTo(sourceFilePath);
        }

        @Test
        @DisplayName("should return correct line number")
        void shouldReturnCorrectLineNumber() {
            // Arrange
            int lineNumber = 25;

            // Act
            GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
                .withLineNumber(lineNumber)
                .build();

            // Assert
            assertThat(scenarioInfo.getLineNumber()).isEqualTo(lineNumber);
        }

        @Test
        @DisplayName("should return correct feature file content")
        void shouldReturnCorrectFeatureFileContent() {
            // Arrange
            String featureFileContent = "Feature: User Authentication\n\nBackground:\nGiven the application is running\n\nScenario: Successful login with valid credentials\nGiven I am on the login page\nWhen I click the login button\nThen I should be logged in";

            // Act
            GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
                .withFeatureFileContent(featureFileContent)
                .build();

            // Assert
            assertThat(scenarioInfo.getFeatureFileContent()).isEqualTo(featureFileContent);
            assertThat(scenarioInfo.getFeatureFileContent()).contains("Feature: User Authentication");
            assertThat(scenarioInfo.getFeatureFileContent()).contains("Background:");
            assertThat(scenarioInfo.getFeatureFileContent()).contains("Scenario: Successful login with valid credentials");
        }
    }

    @Nested
    @DisplayName("Step Handling")
    class StepHandling {

        @Test
        @DisplayName("should handle Given steps correctly")
        void shouldHandleGivenStepsCorrectly() {
            // Arrange
            List<String> steps = Arrays.asList("Given I am on the login page", "Given the user is authenticated");

            // Act
            GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
                .withSteps(steps)
                .build();

            // Assert
            assertThat(scenarioInfo.getSteps()).hasSize(2);
            assertThat(scenarioInfo.getSteps()).allMatch(step -> step.startsWith("Given"));
        }

        @Test
        @DisplayName("should handle When steps correctly")
        void shouldHandleWhenStepsCorrectly() {
            // Arrange
            List<String> steps = Arrays.asList("When I click the login button", "When I enter valid credentials");

            // Act
            GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
                .withSteps(steps)
                .build();

            // Assert
            assertThat(scenarioInfo.getSteps()).hasSize(2);
            assertThat(scenarioInfo.getSteps()).allMatch(step -> step.startsWith("When"));
        }

        @Test
        @DisplayName("should handle Then steps correctly")
        void shouldHandleThenStepsCorrectly() {
            // Arrange
            List<String> steps = Arrays.asList("Then I should be logged in", "Then I should see the dashboard");

            // Act
            GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
                .withSteps(steps)
                .build();

            // Assert
            assertThat(scenarioInfo.getSteps()).hasSize(2);
            assertThat(scenarioInfo.getSteps()).allMatch(step -> step.startsWith("Then"));
        }

        @Test
        @DisplayName("should handle And steps correctly")
        void shouldHandleAndStepsCorrectly() {
            // Arrange
            List<String> steps = Arrays.asList("Given I am on the login page", "And I enter my username", "And I enter my password");

            // Act
            GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
                .withSteps(steps)
                .build();

            // Assert
            assertThat(scenarioInfo.getSteps()).hasSize(3);
            assertThat(scenarioInfo.getSteps()).anyMatch(step -> step.startsWith("And"));
        }

        @Test
        @DisplayName("should handle But steps correctly")
        void shouldHandleButStepsCorrectly() {
            // Arrange
            List<String> steps = Arrays.asList("Given I am on the login page", "But I am not logged in");

            // Act
            GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
                .withSteps(steps)
                .build();

            // Assert
            assertThat(scenarioInfo.getSteps()).hasSize(2);
            assertThat(scenarioInfo.getSteps()).anyMatch(step -> step.startsWith("But"));
        }

        @Test
        @DisplayName("should handle empty steps list")
        void shouldHandleEmptyStepsList() {
            // Act
            GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
                .withSteps(Arrays.asList())
                .build();

            // Assert
            assertThat(scenarioInfo.getSteps()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Tag Handling")
    class TagHandling {

        @Test
        @DisplayName("should handle single tag correctly")
        void shouldHandleSingleTagCorrectly() {
            // Arrange
            List<String> tags = Arrays.asList("@smoke");

            // Act
            GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
                .withTags(tags)
                .build();

            // Assert
            assertThat(scenarioInfo.getTags()).hasSize(1);
            assertThat(scenarioInfo.getTags()).contains("@smoke");
        }

        @Test
        @DisplayName("should handle multiple tags correctly")
        void shouldHandleMultipleTagsCorrectly() {
            // Arrange
            List<String> tags = Arrays.asList("@smoke", "@login", "@regression", "@ui");

            // Act
            GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
                .withTags(tags)
                .build();

            // Assert
            assertThat(scenarioInfo.getTags()).hasSize(4);
            assertThat(scenarioInfo.getTags()).contains("@smoke", "@login", "@regression", "@ui");
        }

        @Test
        @DisplayName("should handle empty tags list")
        void shouldHandleEmptyTagsList() {
            // Act
            GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
                .withTags(Arrays.asList())
                .build();

            // Assert
            assertThat(scenarioInfo.getTags()).isEmpty();
        }

        @Test
        @DisplayName("should handle null tags list")
        void shouldHandleNullTagsList() {
            // Act
            GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
                .withTags(null)
                .build();

            // Assert
            assertThat(scenarioInfo.getTags()).isNull();
        }
    }

    @Nested
    @DisplayName("Background Steps")
    class BackgroundSteps {

        @Test
        @DisplayName("should handle background steps correctly")
        void shouldHandleBackgroundStepsCorrectly() {
            // Arrange
            List<String> backgroundSteps = Arrays.asList("Given the application is running", "And the database is connected", "And the test data is loaded");

            // Act
            GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
                .withBackgroundSteps(backgroundSteps)
                .build();

            // Assert
            assertThat(scenarioInfo.getBackgroundSteps()).hasSize(3);
            assertThat(scenarioInfo.getBackgroundSteps()).contains("Given the application is running");
            assertThat(scenarioInfo.getBackgroundSteps()).contains("And the database is connected");
            assertThat(scenarioInfo.getBackgroundSteps()).contains("And the test data is loaded");
        }

        @Test
        @DisplayName("should handle empty background steps list")
        void shouldHandleEmptyBackgroundStepsList() {
            // Act
            GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
                .withBackgroundSteps(Arrays.asList())
                .build();

            // Assert
            assertThat(scenarioInfo.getBackgroundSteps()).isEmpty();
        }

        @Test
        @DisplayName("should handle null background steps list")
        void shouldHandleNullBackgroundStepsList() {
            // Act
            GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
                .withBackgroundSteps(null)
                .build();

            // Assert
            assertThat(scenarioInfo.getBackgroundSteps()).isNull();
        }
    }

    @Nested
    @DisplayName("Data Table Handling")
    class DataTableHandling {

        @Test
        @DisplayName("should handle data table correctly")
        void shouldHandleDataTableCorrectly() {
            // Arrange
            List<String> dataTable = Arrays.asList(
                "| username | password | email           |",
                "| user1    | pass1    | user1@test.com  |",
                "| user2    | pass2    | user2@test.com  |",
                "| user3    | pass3    | user3@test.com  |"
            );

            // Act
            GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
                .withDataTable(dataTable)
                .build();

            // Assert
            assertThat(scenarioInfo.getDataTable()).hasSize(4);
            assertThat(scenarioInfo.getDataTable()).contains("| username | password | email           |");
            assertThat(scenarioInfo.getDataTable()).contains("| user1    | pass1    | user1@test.com  |");
        }

        @Test
        @DisplayName("should handle empty data table")
        void shouldHandleEmptyDataTable() {
            // Act
            GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
                .withDataTable(Arrays.asList())
                .build();

            // Assert
            assertThat(scenarioInfo.getDataTable()).isEmpty();
        }

        @Test
        @DisplayName("should handle null data table")
        void shouldHandleNullDataTable() {
            // Act
            GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
                .withDataTable(null)
                .build();

            // Assert
            assertThat(scenarioInfo.getDataTable()).isNull();
        }

        @Test
        @DisplayName("should handle scenario outline examples")
        void shouldHandleScenarioOutlineExamples() {
            // Arrange
            List<String> dataTable = Arrays.asList(
                "Examples:",
                "| username | password | expected_result |",
                "| valid    | valid    | success         |",
                "| invalid  | valid    | failure         |",
                "| valid    | invalid  | failure         |"
            );

            // Act
            GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
                .withDataTable(dataTable)
                .withIsScenarioOutline(true)
                .build();

            // Assert
            assertThat(scenarioInfo.getDataTable()).hasSize(5);
            assertThat(scenarioInfo.getDataTable()).contains("Examples:");
            assertThat(scenarioInfo.isScenarioOutline()).isTrue();
        }
    }

    @Nested
    @DisplayName("Scenario Outline")
    class ScenarioOutline {

        @Test
        @DisplayName("should identify scenario outline correctly")
        void shouldIdentifyScenarioOutlineCorrectly() {
            // Act
            GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
                .withIsScenarioOutline(true)
                .withDataTable(Arrays.asList("| username | password |", "| user1    | pass1    |"))
                .build();

            // Assert
            assertThat(scenarioInfo.isScenarioOutline()).isTrue();
            assertThat(scenarioInfo.getDataTable()).isNotNull();
            assertThat(scenarioInfo.getDataTable()).hasSize(2);
        }

        @Test
        @DisplayName("should handle regular scenario correctly")
        void shouldHandleRegularScenarioCorrectly() {
            // Act
            GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
                .withIsScenarioOutline(false)
                .withSteps(Arrays.asList("Given I am on the login page", "When I click the login button", "Then I should be logged in"))
                .build();

            // Assert
            assertThat(scenarioInfo.isScenarioOutline()).isFalse();
            assertThat(scenarioInfo.getSteps()).hasSize(3);
        }

        @Test
        @DisplayName("should handle scenario outline with examples")
        void shouldHandleScenarioOutlineWithExamples() {
            // Arrange
            List<String> steps = Arrays.asList("Given I am on the login page", "When I login with <username> and <password>", "Then I should see <expected_result>");
            List<String> examples = Arrays.asList(
                "Examples:",
                "| username | password | expected_result |",
                "| valid    | valid    | success         |",
                "| invalid  | valid    | failure         |"
            );

            // Act
            GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
                .withSteps(steps)
                .withDataTable(examples)
                .withIsScenarioOutline(true)
                .build();

            // Assert
            assertThat(scenarioInfo.isScenarioOutline()).isTrue();
            assertThat(scenarioInfo.getSteps()).hasSize(3);
            assertThat(scenarioInfo.getSteps()).anyMatch(step -> step.contains("<username>"));
            assertThat(scenarioInfo.getSteps()).anyMatch(step -> step.contains("<password>"));
            assertThat(scenarioInfo.getSteps()).anyMatch(step -> step.contains("<expected_result>"));
            assertThat(scenarioInfo.getDataTable()).hasSize(4);
            assertThat(scenarioInfo.getDataTable()).contains("Examples:");
        }
    }

    @Nested
    @DisplayName("Builder Pattern")
    class BuilderPattern {

        @Test
        @DisplayName("should support method chaining")
        void shouldSupportMethodChaining() {
            // Act
            GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
                .withFeatureName("User Authentication")
                .withScenarioName("Successful login with valid credentials")
                .withSteps(Arrays.asList("Given I am on the login page", "When I click the login button", "Then I should be logged in"))
                .withTags(Arrays.asList("@smoke", "@login"))
                .withBackgroundSteps(Arrays.asList("Given the application is running"))
                .withDataTable(Arrays.asList("| username | password |", "| user1    | pass1    |"))
                .withFullScenarioText("Scenario: Successful login with valid credentials")
                .withIsScenarioOutline(false)
                .withSourceFilePath("login.feature")
                .withLineNumber(15)
                .withFeatureFileContent("Feature: User Authentication")
                .build();

            // Assert
            assertThat(scenarioInfo).isNotNull();
            assertThat(scenarioInfo.getFeatureName()).isEqualTo("User Authentication");
            assertThat(scenarioInfo.getScenarioName()).isEqualTo("Successful login with valid credentials");
            assertThat(scenarioInfo.getSteps()).hasSize(3);
            assertThat(scenarioInfo.getTags()).hasSize(2);
            assertThat(scenarioInfo.getBackgroundSteps()).hasSize(1);
            assertThat(scenarioInfo.getDataTable()).hasSize(2);
            assertThat(scenarioInfo.getFullScenarioText()).isEqualTo("Scenario: Successful login with valid credentials");
            assertThat(scenarioInfo.isScenarioOutline()).isFalse();
            assertThat(scenarioInfo.getSourceFilePath()).isEqualTo("login.feature");
            assertThat(scenarioInfo.getLineNumber()).isEqualTo(15);
            assertThat(scenarioInfo.getFeatureFileContent()).isEqualTo("Feature: User Authentication");
        }

        @Test
        @DisplayName("should create multiple instances independently")
        void shouldCreateMultipleInstancesIndependently() {
            // Arrange
            GherkinScenarioInfo.Builder builder = new GherkinScenarioInfo.Builder();

            // Act
            GherkinScenarioInfo scenario1 = builder.withFeatureName("Feature 1").withScenarioName("Scenario 1").build();
            GherkinScenarioInfo scenario2 = builder.withFeatureName("Feature 2").withScenarioName("Scenario 2").build();

            // Assert
            assertThat(scenario1.getFeatureName()).isEqualTo("Feature 1");
            assertThat(scenario1.getScenarioName()).isEqualTo("Scenario 1");
            assertThat(scenario2.getFeatureName()).isEqualTo("Feature 2");
            assertThat(scenario2.getScenarioName()).isEqualTo("Scenario 2");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("should handle empty strings gracefully")
        void shouldHandleEmptyStringsGracefully() {
            // Act
            GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
                .withFeatureName("")
                .withScenarioName("")
                .withFullScenarioText("")
                .withSourceFilePath("")
                .withFeatureFileContent("")
                .build();

            // Assert
            assertThat(scenarioInfo.getFeatureName()).isEmpty();
            assertThat(scenarioInfo.getScenarioName()).isEmpty();
            assertThat(scenarioInfo.getFullScenarioText()).isEmpty();
            assertThat(scenarioInfo.getSourceFilePath()).isEmpty();
            assertThat(scenarioInfo.getFeatureFileContent()).isEmpty();
        }

        @Test
        @DisplayName("should handle very long scenario names")
        void shouldHandleVeryLongScenarioNames() {
            // Arrange
            String longScenarioName = "This is a very long scenario name that contains many words and should be handled gracefully by the GherkinScenarioInfo model without causing any issues or exceptions";

            // Act
            GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
                .withScenarioName(longScenarioName)
                .build();

            // Assert
            assertThat(scenarioInfo.getScenarioName()).isEqualTo(longScenarioName);
            assertThat(scenarioInfo.getScenarioName().length()).isGreaterThan(100);
        }

        @Test
        @DisplayName("should handle special characters in scenario text")
        void shouldHandleSpecialCharactersInScenarioText() {
            // Arrange
            String specialCharsText = "Scenario with special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?\\`~";

            // Act
            GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
                .withFullScenarioText(specialCharsText)
                .build();

            // Assert
            assertThat(scenarioInfo.getFullScenarioText()).isEqualTo(specialCharsText);
            assertThat(scenarioInfo.getFullScenarioText()).contains("!");
            assertThat(scenarioInfo.getFullScenarioText()).contains("@");
            assertThat(scenarioInfo.getFullScenarioText()).contains("#");
        }

        @Test
        @DisplayName("should handle negative line numbers")
        void shouldHandleNegativeLineNumbers() {
            // Arrange
            int negativeLineNumber = -1;

            // Act
            GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
                .withLineNumber(negativeLineNumber)
                .build();

            // Assert
            assertThat(scenarioInfo.getLineNumber()).isEqualTo(-1);
        }

        @Test
        @DisplayName("should handle very large feature files")
        void shouldHandleVeryLargeFeatureFiles() {
            // Arrange
            StringBuilder largeFeatureFile = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                largeFeatureFile.append("Feature: Large Feature File\n");
                largeFeatureFile.append("Scenario: Test Scenario ").append(i).append("\n");
                largeFeatureFile.append("Given I am on the test page\n");
                largeFeatureFile.append("When I perform test action\n");
                largeFeatureFile.append("Then I should see expected result\n\n");
            }
            String largeFeatureFileContent = largeFeatureFile.toString();

            // Act
            GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
                .withFeatureFileContent(largeFeatureFileContent)
                .build();

            // Assert
            assertThat(scenarioInfo.getFeatureFileContent()).isEqualTo(largeFeatureFileContent);
            assertThat(scenarioInfo.getFeatureFileContent().length()).isGreaterThan(10000);
            assertThat(scenarioInfo.getFeatureFileContent()).contains("Feature: Large Feature File");
        }

        @Test
        @DisplayName("should handle unicode characters")
        void shouldHandleUnicodeCharacters() {
            // Arrange
            String unicodeText = "Scenario with unicode: café résumé naïve 测试 テスト";

            // Act
            GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
                .withScenarioName(unicodeText)
                .build();

            // Assert
            assertThat(scenarioInfo.getScenarioName()).isEqualTo(unicodeText);
            assertThat(scenarioInfo.getScenarioName()).contains("é");
            assertThat(scenarioInfo.getScenarioName()).contains("ï");
            assertThat(scenarioInfo.getScenarioName()).contains("测试");
        }
    }

    @Nested
    @DisplayName("Comprehensive Scenario")
    class ComprehensiveScenario {

        @Test
        @DisplayName("should create comprehensive scenario with all fields")
        void shouldCreateComprehensiveScenarioWithAllFields() {
            // Arrange
            List<String> steps = Arrays.asList(
                "Given I am on the login page",
                "And I enter my username \"testuser\"",
                "And I enter my password \"testpass\"",
                "When I click the login button",
                "Then I should be logged in successfully",
                "And I should see the dashboard"
            );

            List<String> tags = Arrays.asList("@smoke", "@login", "@regression", "@ui");
            List<String> backgroundSteps = Arrays.asList("Given the application is running", "And the database is connected", "And the test data is loaded");
            List<String> dataTable = Arrays.asList(
                "Examples:",
                "| username | password | expected_result |",
                "| valid    | valid    | success         |",
                "| invalid  | valid    | failure         |",
                "| valid    | invalid  | failure         |"
            );

            String fullScenarioText = "Scenario Outline: Login with different credentials\nGiven I am on the login page\nWhen I login with <username> and <password>\nThen I should see <expected_result>\n\nExamples:\n| username | password | expected_result |\n| valid    | valid    | success         |\n| invalid  | valid    | failure         |";

            String featureFileContent = "Feature: User Authentication\n\nBackground:\nGiven the application is running\nAnd the database is connected\nAnd the test data is loaded\n\nScenario Outline: Login with different credentials\nGiven I am on the login page\nWhen I login with <username> and <password>\nThen I should see <expected_result>\n\nExamples:\n| username | password | expected_result |\n| valid    | valid    | success         |\n| invalid  | valid    | failure         |";

            // Act
            GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
                .withFeatureName("User Authentication")
                .withScenarioName("Login with different credentials")
                .withSteps(steps)
                .withTags(tags)
                .withBackgroundSteps(backgroundSteps)
                .withDataTable(dataTable)
                .withFullScenarioText(fullScenarioText)
                .withIsScenarioOutline(true)
                .withSourceFilePath("src/test/resources/features/login.feature")
                .withLineNumber(25)
                .withFeatureFileContent(featureFileContent)
                .build();

            // Assert
            assertThat(scenarioInfo).isNotNull();
            assertThat(scenarioInfo.getFeatureName()).isEqualTo("User Authentication");
            assertThat(scenarioInfo.getScenarioName()).isEqualTo("Login with different credentials");
            assertThat(scenarioInfo.getSteps()).hasSize(6);
            assertThat(scenarioInfo.getSteps()).contains("Given I am on the login page");
            assertThat(scenarioInfo.getSteps()).contains("When I click the login button");
            assertThat(scenarioInfo.getSteps()).contains("Then I should be logged in successfully");
            assertThat(scenarioInfo.getTags()).hasSize(4);
            assertThat(scenarioInfo.getTags()).contains("@smoke", "@login", "@regression", "@ui");
            assertThat(scenarioInfo.getBackgroundSteps()).hasSize(3);
            assertThat(scenarioInfo.getBackgroundSteps()).contains("Given the application is running");
            assertThat(scenarioInfo.getDataTable()).hasSize(5);
            assertThat(scenarioInfo.getDataTable()).contains("Examples:");
            assertThat(scenarioInfo.getFullScenarioText()).contains("Scenario Outline: Login with different credentials");
            assertThat(scenarioInfo.isScenarioOutline()).isTrue();
            assertThat(scenarioInfo.getSourceFilePath()).isEqualTo("src/test/resources/features/login.feature");
            assertThat(scenarioInfo.getLineNumber()).isEqualTo(25);
            assertThat(scenarioInfo.getFeatureFileContent()).contains("Feature: User Authentication");
            assertThat(scenarioInfo.getFeatureFileContent()).contains("Background:");
        }
    }
}
