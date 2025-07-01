/*
 * NOTE: This test class uses LightJavaCodeInsightFixtureTestCase and JUnit 3 style.
 * These tests must be run from the IDE using the IntelliJ Platform test runner.
 * They will not work with Gradle's test task.
 */
package com.triagemate.extractors;

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.triagemate.models.GherkinScenarioInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Test-specific version of GherkinScenarioExtractor that works with a provided list of feature files.
 * This is the JetBrains-recommended approach for test isolation.
 */
class TestGherkinScenarioExtractor extends GherkinScenarioExtractor {
    private final List<PsiFile> featureFiles;

    public TestGherkinScenarioExtractor(List<PsiFile> featureFiles, com.intellij.openapi.project.Project project) {
        super(project);
        this.featureFiles = featureFiles;
    }

    @Override
    protected List<PsiFile> findFeatureFiles() {
        return featureFiles;
    }
}

public class GherkinScenarioExtractorIntegrationTest extends LightJavaCodeInsightFixtureTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // No extractor here, will be created per test
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            // Clean up any test files if needed
        } catch (Exception e) {
            addSuppressedException(e);
        } finally {
            super.tearDown();
        }
    }

    public void testExtractScenarioInfoForBasicGivenStep() {
        List<PsiFile> featureFiles = new ArrayList<>();
        PsiFile featureFile = myFixture.addFileToProject(
            "src/test/resources/features/login.feature",
            """
            Feature: User Login
              As a user
              I want to log into the system
              So that I can access my account
            
              @smoke @login
              Scenario: Successful login with valid credentials
                Given I am on the login page
                When I enter \"test@example.com\" in the email field
                And I enter \"password123\" in the password field
                And I click on the \"Login\" button
                Then I should see the dashboard
                And I should see \"Welcome, User\" message
            """
        );
        featureFiles.add(featureFile);
        TestGherkinScenarioExtractor extractor = new TestGherkinScenarioExtractor(featureFiles, getProject());
        String failedStepText = "Given I am on the login page";
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        assertNotNull("Scenario info should not be null", result);
        assertEquals("Feature name should match", "User Login", result.getFeatureName());
        assertEquals("Scenario name should match", "Successful login with valid credentials", result.getScenarioName());
        assertEquals("Source file path should match", "login.feature", result.getSourceFilePath());
        assertTrue("Line number should be positive", result.getLineNumber() > 0);
        assertEquals("Number of steps should match", 6, result.getSteps().size());
        assertEquals("First step should match", "Given I am on the login page", result.getSteps().get(0));
        assertEquals("Second step should match", "When I enter \"test@example.com\" in the email field", result.getSteps().get(1));
        assertEquals("Third step should match", "And I enter \"password123\" in the password field", result.getSteps().get(2));
        assertEquals("Fourth step should match", "And I click on the \"Login\" button", result.getSteps().get(3));
        assertEquals("Fifth step should match", "Then I should see the dashboard", result.getSteps().get(4));
        assertEquals("Sixth step should match", "And I should see \"Welcome, User\" message", result.getSteps().get(5));
        assertEquals("Number of tags should match", 2, result.getTags().size());
        assertTrue("Should contain @smoke tag", result.getTags().contains("@smoke"));
        assertTrue("Should contain @login tag", result.getTags().contains("@login"));
        assertNotNull("Feature file content should not be null", result.getFeatureFileContent());
        assertTrue("Feature file content should contain feature name", result.getFeatureFileContent().contains("User Login"));
    }

    public void testExtractScenarioInfoForWhenStepWithParameters() {
        List<PsiFile> featureFiles = new ArrayList<>();
        PsiFile featureFile = myFixture.addFileToProject(
            "src/test/resources/features/user_management.feature",
            """
            Feature: User Management
              As an administrator
              I want to manage user accounts
              So that I can control system access
            
              Scenario: Create new user account
                Given I am logged in as an administrator
                When I navigate to the user management page
                And I click on "Add New User" button
                And I enter "john.doe@example.com" in the email field
                And I enter "John Doe" in the name field
                And I select "User" from the role dropdown
                And I click on "Save" button
                Then I should see "User created successfully" message
                And the new user should appear in the user list
            """
        );
        featureFiles.add(featureFile);
        TestGherkinScenarioExtractor extractor = new TestGherkinScenarioExtractor(featureFiles, getProject());
        String failedStepText = "When I navigate to the user management page";
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        assertNotNull("Scenario info should not be null", result);
        assertEquals("Feature name should match", "User Management", result.getFeatureName());
        assertEquals("Scenario name should match", "Create new user account", result.getScenarioName());
        assertEquals("Source file path should match", "user_management.feature", result.getSourceFilePath());
        assertTrue("Line number should be positive", result.getLineNumber() > 0);
        assertEquals("Number of steps should match", 9, result.getSteps().size());
        assertEquals("Failed step should be found", "When I navigate to the user management page", result.getSteps().get(1));
        assertNotNull("Feature file content should not be null", result.getFeatureFileContent());
        assertTrue("Feature file content should contain scenario name", result.getFeatureFileContent().contains("Create new user account"));
    }

    public void testExtractScenarioInfoForThenStep() {
        List<PsiFile> featureFiles = new ArrayList<>();
        PsiFile featureFile = myFixture.addFileToProject(
            "src/test/resources/features/search.feature",
            """
            Feature: Search Functionality
              As a user
              I want to search for content
              So that I can find relevant information
            
              @search @functional
              Scenario: Search with valid query
                Given I am on the search page
                When I enter "test query" in the search box
                And I click on the search button
                Then I should see search results
                And the results should contain "test query"
                And I should see at least 5 results
            """
        );
        featureFiles.add(featureFile);
        TestGherkinScenarioExtractor extractor = new TestGherkinScenarioExtractor(featureFiles, getProject());
        String failedStepText = "Then I should see search results";
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        assertNotNull("Scenario info should not be null", result);
        assertEquals("Feature name should match", "Search Functionality", result.getFeatureName());
        assertEquals("Scenario name should match", "Search with valid query", result.getScenarioName());
        assertEquals("Source file path should match", "search.feature", result.getSourceFilePath());
        assertTrue("Line number should be positive", result.getLineNumber() > 0);
        assertEquals("Number of steps should match", 6, result.getSteps().size());
        assertEquals("Failed step should be found", "Then I should see search results", result.getSteps().get(3));
        assertEquals("Number of tags should match", 2, result.getTags().size());
        assertTrue("Should contain @search tag", result.getTags().contains("@search"));
        assertTrue("Should contain @functional tag", result.getTags().contains("@functional"));
    }

    public void testExtractScenarioInfoWithScenarioName() {
        List<PsiFile> featureFiles = new ArrayList<>();
        PsiFile featureFile = myFixture.addFileToProject(
            "src/test/resources/features/multiple_scenarios.feature",
            """
            Feature: Multiple Scenarios Test
              As a tester
              I want to test multiple scenarios
              So that I can verify different behaviors
            
              Scenario: First scenario
                Given I am on the first page
                When I perform first action
                Then I should see first result
            
              Scenario: Second scenario
                Given I am on the second page
                When I perform second action
                Then I should see second result
            
              Scenario: Third scenario
                Given I am on the third page
                When I perform third action
                Then I should see third result
            """
        );
        featureFiles.add(featureFile);
        TestGherkinScenarioExtractor extractor = new TestGherkinScenarioExtractor(featureFiles, getProject());
        String failedStepText = "Given I am on the second page";
        String scenarioName = "Second scenario";
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, scenarioName);
        
        assertNotNull("Scenario info should not be null", result);
        assertEquals("Feature name should match", "Multiple Scenarios Test", result.getFeatureName());
        assertEquals("Scenario name should match", "Second scenario", result.getScenarioName());
        assertEquals("Source file path should match", "multiple_scenarios.feature", result.getSourceFilePath());
        assertTrue("Line number should be positive", result.getLineNumber() > 0);
        assertEquals("Number of steps should match", 3, result.getSteps().size());
        assertEquals("First step should match", "Given I am on the second page", result.getSteps().get(0));
        assertEquals("Second step should match", "When I perform second action", result.getSteps().get(1));
        assertEquals("Third step should match", "Then I should see second result", result.getSteps().get(2));
    }

    public void testExtractScenarioInfoForScenarioOutline() {
        List<PsiFile> featureFiles = new ArrayList<>();
        PsiFile featureFile = myFixture.addFileToProject(
            "src/test/resources/features/scenario_outline.feature",
            """
            Feature: Scenario Outline Test
              As a user
              I want to test with different data
              So that I can verify various scenarios
            
              @outline @data-driven
              Scenario Outline: Login with different credentials
                Given I am on the login page
                When I enter "<email>" in the email field
                And I enter "<password>" in the password field
                And I click on the "Login" button
                Then I should see "<expected_message>"
            
                Examples:
                  | email              | password    | expected_message           |
                  | valid@test.com     | validpass   | Welcome to Dashboard       |
                  | invalid@test.com   | wrongpass   | Invalid credentials        |
                  | empty@test.com     |             | Email is required          |
            """
        );
        featureFiles.add(featureFile);
        TestGherkinScenarioExtractor extractor = new TestGherkinScenarioExtractor(featureFiles, getProject());
        String failedStepText = "When I enter \"valid@test.com\" in the email field";
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        assertNotNull("Scenario info should not be null", result);
        assertEquals("Feature name should match", "Scenario Outline Test", result.getFeatureName());
        assertEquals("Scenario name should match", "Login with different credentials", result.getScenarioName());
        assertEquals("Source file path should match", "scenario_outline.feature", result.getSourceFilePath());
        assertTrue("Line number should be positive", result.getLineNumber() > 0);
        assertEquals("Number of steps should match", 5, result.getSteps().size());
        assertEquals("Failed step should be found", "When I enter \"<email>\" in the email field", result.getSteps().get(1));
        assertEquals("Number of tags should match", 2, result.getTags().size());
        assertTrue("Should contain @outline tag", result.getTags().contains("@outline"));
        assertTrue("Should contain @data-driven tag", result.getTags().contains("@data-driven"));
    }

    public void testExtractScenarioInfoWithMultipleTags() {
        List<PsiFile> featureFiles = new ArrayList<>();
        PsiFile featureFile = myFixture.addFileToProject(
            "src/test/resources/features/tagged_scenarios.feature",
            """
            Feature: Tagged Scenarios Test
              As a developer
              I want to organize my tests with tags
              So that I can run specific test suites
            
              @smoke @regression @ui @login
              Scenario: Comprehensive login test
                Given I am on the login page
                When I enter valid credentials
                And I click on the login button
                Then I should be logged in successfully
                And I should see the dashboard
            """
        );
        featureFiles.add(featureFile);
        TestGherkinScenarioExtractor extractor = new TestGherkinScenarioExtractor(featureFiles, getProject());
        String failedStepText = "Given I am on the login page";
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        assertNotNull("Scenario info should not be null", result);
        assertEquals("Feature name should match", "Tagged Scenarios Test", result.getFeatureName());
        assertEquals("Scenario name should match", "Comprehensive login test", result.getScenarioName());
        assertEquals("Source file path should match", "tagged_scenarios.feature", result.getSourceFilePath());
        assertTrue("Line number should be positive", result.getLineNumber() > 0);
        assertEquals("Number of steps should match", 5, result.getSteps().size());
        assertEquals("Number of tags should match", 4, result.getTags().size());
        assertTrue("Should contain @smoke tag", result.getTags().contains("@smoke"));
        assertTrue("Should contain @regression tag", result.getTags().contains("@regression"));
        assertTrue("Should contain @ui tag", result.getTags().contains("@ui"));
        assertTrue("Should contain @login tag", result.getTags().contains("@login"));
    }

    public void testExtractScenarioInfoWithComplexStepText() {
        List<PsiFile> featureFiles = new ArrayList<>();
        PsiFile featureFile = myFixture.addFileToProject(
            "src/test/resources/features/complex_steps.feature",
            """
            Feature: Complex Steps Test
              As a user
              I want to perform complex operations
              So that I can test advanced functionality
            
              Scenario: Complex data entry
                Given I am on the data entry form
                When I enter the following data:
                  | Field Name | Value           |
                  | First Name | John            |
                  | Last Name  | Doe             |
                  | Email      | john@doe.com    |
                  | Phone      | +1-555-123-4567 |
                And I select "Advanced User" from the role dropdown
                And I check the "Send notifications" checkbox
                And I upload the file "document.pdf"
                Then I should see "Data saved successfully" message
                And the form should be cleared
            """
        );
        featureFiles.add(featureFile);
        TestGherkinScenarioExtractor extractor = new TestGherkinScenarioExtractor(featureFiles, getProject());
        String failedStepText = "When I enter the following data:";
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        assertNotNull("Scenario info should not be null", result);
        assertEquals("Feature name should match", "Complex Steps Test", result.getFeatureName());
        assertEquals("Scenario name should match", "Complex data entry", result.getScenarioName());
        assertEquals("Source file path should match", "complex_steps.feature", result.getSourceFilePath());
        assertTrue("Line number should be positive", result.getLineNumber() > 0);
        assertEquals("Number of steps should match", 7, result.getSteps().size());
        assertEquals("Failed step should be found", "When I enter the following data:", result.getSteps().get(1));
        assertNotNull("Feature file content should not be null", result.getFeatureFileContent());
        assertTrue("Feature file content should contain table data", result.getFeatureFileContent().contains("| Field Name | Value           |"));
    }

    public void testExtractScenarioInfoWithSpecialCharacters() {
        List<PsiFile> featureFiles = new ArrayList<>();
        PsiFile featureFile = myFixture.addFileToProject(
            "src/test/resources/features/special_chars.feature",
            """
            Feature: Special Characters Test
              As a user
              I want to test with special characters
              So that I can verify encoding handling
            
              @special @encoding
              Scenario: Special character handling
                Given I am on the form page
                When I enter "José García" in the name field
                And I enter "test@example.com" in the email field
                And I enter "password123!" in the password field
                And I select "User & Admin" from the role dropdown
                Then I should see "Welcome, José García!" message
                And the form should contain special characters: áéíóúñ
            """
        );
        featureFiles.add(featureFile);
        TestGherkinScenarioExtractor extractor = new TestGherkinScenarioExtractor(featureFiles, getProject());
        String failedStepText = "When I enter \"José García\" in the name field";
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        assertNotNull("Scenario info should not be null", result);
        assertEquals("Feature name should match", "Special Characters Test", result.getFeatureName());
        assertEquals("Scenario name should match", "Special character handling", result.getScenarioName());
        assertEquals("Source file path should match", "special_chars.feature", result.getSourceFilePath());
        assertTrue("Line number should be positive", result.getLineNumber() > 0);
        assertEquals("Number of steps should match", 7, result.getSteps().size());
        assertEquals("Failed step should be found", "When I enter \"José García\" in the name field", result.getSteps().get(1));
        assertEquals("Number of tags should match", 2, result.getTags().size());
        assertTrue("Should contain @special tag", result.getTags().contains("@special"));
        assertTrue("Should contain @encoding tag", result.getTags().contains("@encoding"));
    }

    public void testExtractScenarioInfoWithMultipleFeatures() {
        List<PsiFile> featureFiles = new ArrayList<>();
        PsiFile featureFile1 = myFixture.addFileToProject(
            "src/test/resources/features/login.feature",
            """
            Feature: Login Feature
              As a user
              I want to log in
              So that I can access the system
            
              Scenario: Basic login
                Given I am on the login page
                When I enter credentials
                Then I should be logged in
            """
        );
        featureFiles.add(featureFile1);
        PsiFile featureFile2 = myFixture.addFileToProject(
            "src/test/resources/features/dashboard.feature",
            """
            Feature: Dashboard Feature
              As a logged in user
              I want to see the dashboard
              So that I can navigate the system
            
              Scenario: View dashboard
                Given I am logged in
                When I navigate to the dashboard
                Then I should see the dashboard
            """
        );
        TestGherkinScenarioExtractor extractor = new TestGherkinScenarioExtractor(featureFiles, getProject());
        String failedStepText = "Given I am on the login page";
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        assertNotNull("Scenario info should not be null", result);
        assertEquals("Feature name should match", "Login Feature", result.getFeatureName());
        assertEquals("Scenario name should match", "Basic login", result.getScenarioName());
        assertEquals("Source file path should match", "login.feature", result.getSourceFilePath());
        assertTrue("Line number should be positive", result.getLineNumber() > 0);
        assertEquals("Number of steps should match", 3, result.getSteps().size());
        assertEquals("Failed step should be found", "Given I am on the login page", result.getSteps().get(0));
    }

    public void testReturnNullForNonExistentStep() {
        List<PsiFile> featureFiles = new ArrayList<>();
        PsiFile featureFile = myFixture.addFileToProject(
            "src/test/resources/features/simple.feature",
            """
            Feature: Simple Test
              As a user
              I want to test basic functionality
              So that I can verify the system works
            
              Scenario: Basic test
                Given I am on the test page
                When I perform a test action
                Then I should see the expected result
            """
        );
        featureFiles.add(featureFile);
        TestGherkinScenarioExtractor extractor = new TestGherkinScenarioExtractor(featureFiles, getProject());
        String failedStepText = "Given I am on a page that does not exist";
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        assertNull("Scenario info should be null for non-existent step", result);
    }

    public void testReturnNullForNonExistentScenarioName() {
        List<PsiFile> featureFiles = new ArrayList<>();
        PsiFile featureFile = myFixture.addFileToProject(
            "src/test/resources/features/simple.feature",
            """
            Feature: Simple Test
              As a user
              I want to test basic functionality
              So that I can verify the system works
            
              Scenario: Basic test
                Given I am on the test page
                When I perform a test action
                Then I should see the expected result
            """
        );
        featureFiles.add(featureFile);
        TestGherkinScenarioExtractor extractor = new TestGherkinScenarioExtractor(featureFiles, getProject());
        String failedStepText = "Given I am on the test page";
        String nonExistentScenarioName = "Non-existent scenario";
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, nonExistentScenarioName);
        
        assertNull("Scenario info should be null for non-existent scenario name", result);
    }

    public void testExtractScenarioInfoWithEmptyScenarioName() {
        List<PsiFile> featureFiles = new ArrayList<>();
        PsiFile featureFile = myFixture.addFileToProject(
            "src/test/resources/features/simple.feature",
            """
            Feature: Simple Test
              As a user
              I want to test basic functionality
              So that I can verify the system works
            
              Scenario: Basic test
                Given I am on the test page
                When I perform a test action
                Then I should see the expected result
            """
        );
        featureFiles.add(featureFile);
        TestGherkinScenarioExtractor extractor = new TestGherkinScenarioExtractor(featureFiles, getProject());
        String failedStepText = "Given I am on the test page";
        String emptyScenarioName = "";
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, emptyScenarioName);
        
        assertNotNull("Scenario info should not be null when scenario name is empty", result);
        assertEquals("Feature name should match", "Simple Test", result.getFeatureName());
        assertEquals("Scenario name should match", "Basic test", result.getScenarioName());
        assertEquals("Source file path should match", "simple.feature", result.getSourceFilePath());
        assertTrue("Line number should be positive", result.getLineNumber() > 0);
        assertEquals("Number of steps should match", 3, result.getSteps().size());
    }

    public void testExtractScenarioInfoWithWhitespaceOnlyScenarioName() {
        List<PsiFile> featureFiles = new ArrayList<>();
        PsiFile featureFile = myFixture.addFileToProject(
            "src/test/resources/features/simple.feature",
            """
            Feature: Simple Test
              As a user
              I want to test basic functionality
              So that I can verify the system works
            
              Scenario: Basic test
                Given I am on the test page
                When I perform a test action
                Then I should see the expected result
            """
        );
        featureFiles.add(featureFile);
        TestGherkinScenarioExtractor extractor = new TestGherkinScenarioExtractor(featureFiles, getProject());
        String failedStepText = "Given I am on the test page";
        String whitespaceOnlyScenarioName = "   ";
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, whitespaceOnlyScenarioName);
        
        assertNotNull("Scenario info should not be null when scenario name is whitespace only", result);
        assertEquals("Feature name should match", "Simple Test", result.getFeatureName());
        assertEquals("Scenario name should match", "Basic test", result.getScenarioName());
        assertEquals("Source file path should match", "simple.feature", result.getSourceFilePath());
        assertTrue("Line number should be positive", result.getLineNumber() > 0);
        assertEquals("Number of steps should match", 3, result.getSteps().size());
    }

    public void testExtractScenarioInfoWithStepContainingQuotes() {
        List<PsiFile> featureFiles = new ArrayList<>();
        PsiFile featureFile = myFixture.addFileToProject(
            "src/test/resources/features/quoted_steps.feature",
            """
            Feature: Quoted Steps Test
              As a user
              I want to test steps with quotes
              So that I can verify quote handling
            
              Scenario: Steps with quotes
                Given I am on the form page
                When I enter "John's data" in the name field
                And I see the message "Welcome to our system!"
                And I click on the "Submit & Continue" button
                Then I should see "Success: Data saved" message
            """
        );
        featureFiles.add(featureFile);
        TestGherkinScenarioExtractor extractor = new TestGherkinScenarioExtractor(featureFiles, getProject());
        String failedStepText = "When I enter \"John's data\" in the name field";
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        assertNotNull("Scenario info should not be null", result);
        assertEquals("Feature name should match", "Quoted Steps Test", result.getFeatureName());
        assertEquals("Scenario name should match", "Steps with quotes", result.getScenarioName());
        assertEquals("Source file path should match", "quoted_steps.feature", result.getSourceFilePath());
        assertTrue("Line number should be positive", result.getLineNumber() > 0);
        assertEquals("Number of steps should match", 5, result.getSteps().size());
        assertEquals("Failed step should be found", "When I enter \"John's data\" in the name field", result.getSteps().get(1));
    }

    public void testExtractScenarioInfoWithStepContainingParameters() {
        List<PsiFile> featureFiles = new ArrayList<>();
        PsiFile featureFile = myFixture.addFileToProject(
            "src/test/resources/features/parameterized_steps.feature",
            """
            Feature: Parameterized Steps Test
              As a user
              I want to test steps with parameters
              So that I can verify parameter handling
            
              Scenario: Steps with parameters
                Given I am on the search page
                When I search for "test query"
                And I filter by "category" with value "important"
                And I set the limit to 10 items
                Then I should see results containing "test query"
                And the results should be limited to 10 items
            """
        );
        featureFiles.add(featureFile);
        TestGherkinScenarioExtractor extractor = new TestGherkinScenarioExtractor(featureFiles, getProject());
        String failedStepText = "When I search for \"test query\"";
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        assertNotNull("Scenario info should not be null", result);
        assertEquals("Feature name should match", "Parameterized Steps Test", result.getFeatureName());
        assertEquals("Scenario name should match", "Steps with parameters", result.getScenarioName());
        assertEquals("Source file path should match", "parameterized_steps.feature", result.getSourceFilePath());
        assertTrue("Line number should be positive", result.getLineNumber() > 0);
        assertEquals("Number of steps should match", 6, result.getSteps().size());
        assertEquals("Failed step should be found", "When I search for \"test query\"", result.getSteps().get(1));
    }

    public void testExtractScenarioInfoWithStepContainingNumbers() {
        List<PsiFile> featureFiles = new ArrayList<>();
        PsiFile featureFile = myFixture.addFileToProject(
            "src/test/resources/features/numeric_steps.feature",
            """
            Feature: Numeric Steps Test
              As a user
              I want to test steps with numbers
              So that I can verify numeric handling
            
              Scenario: Steps with numbers
                Given I have 5 items in my cart
                When I add 3 more items
                And I remove 1 item
                And I set the quantity to 10
                Then I should have 7 items in my cart
                And the total price should be $99.99
            """
        );
        featureFiles.add(featureFile);
        TestGherkinScenarioExtractor extractor = new TestGherkinScenarioExtractor(featureFiles, getProject());
        String failedStepText = "Given I have 5 items in my cart";
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        assertNotNull("Scenario info should not be null", result);
        assertEquals("Feature name should match", "Numeric Steps Test", result.getFeatureName());
        assertEquals("Scenario name should match", "Steps with numbers", result.getScenarioName());
        assertEquals("Source file path should match", "numeric_steps.feature", result.getSourceFilePath());
        assertTrue("Line number should be positive", result.getLineNumber() > 0);
        assertEquals("Number of steps should match", 6, result.getSteps().size());
        assertEquals("Failed step should be found", "Given I have 5 items in my cart", result.getSteps().get(0));
    }
} 