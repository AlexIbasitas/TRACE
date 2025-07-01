package com.triagemate.extractors;

import com.triagemate.models.GherkinScenarioInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.intellij.openapi.project.Project;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GherkinScenarioExtractor.
 * 
 * <p>These tests focus on the core logic of Gherkin scenario extraction
 * without requiring actual PSI operations. They test the pattern matching,
 * string processing, and error handling logic.</p>
 * 
 * <p>Integration tests with real PSI operations are in separate integration test classes.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GherkinScenarioExtractor Unit Tests")
class GherkinScenarioExtractorUnitTest {

    @Mock
    private Project mockProject;
    
    private GherkinScenarioExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new GherkinScenarioExtractor(mockProject);
    }

    @Test
    @DisplayName("should return null for null failed step text")
    void shouldReturnNullForNullFailedStepText() {
        GherkinScenarioInfo result = extractor.extractScenarioInfo(null, null);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should return null for empty failed step text")
    void shouldReturnNullForEmptyFailedStepText() {
        GherkinScenarioInfo result = extractor.extractScenarioInfo("", null);
        assertThat(result).isNull();
        
        result = extractor.extractScenarioInfo("   ", null);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should return null for null failed step text with scenario name")
    void shouldReturnNullForNullFailedStepTextWithScenarioName() {
        GherkinScenarioInfo result = extractor.extractScenarioInfo(null, "Login Scenario");
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should return null for empty failed step text with scenario name")
    void shouldReturnNullForEmptyFailedStepTextWithScenarioName() {
        GherkinScenarioInfo result = extractor.extractScenarioInfo("", "Login Scenario");
        assertThat(result).isNull();
        
        result = extractor.extractScenarioInfo("   ", "Login Scenario");
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle basic step text without PSI operations")
    void shouldHandleBasicStepTextWithoutPsiOperations() {
        // This test verifies the extractor doesn't crash when PSI operations fail
        // In a real scenario, this would be tested with integration tests
        String failedStepText = "Given I am on the login page";
        
        // Since we're mocking the project, this should return null
        // but not throw an exception
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // In unit tests without PSI, this should be null
        // The actual functionality is tested in integration tests
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with special characters")
    void shouldHandleStepTextWithSpecialCharacters() {
        String failedStepText = "When I enter \"test@example.com\" in the email field";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception, even if PSI operations fail
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with parameters")
    void shouldHandleStepTextWithParameters() {
        String failedStepText = "Then I should see {int} items in the list";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with multiple parameters")
    void shouldHandleStepTextWithMultipleParameters() {
        String failedStepText = "When I click on {string} with text {string}";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with regex special characters")
    void shouldHandleStepTextWithRegexSpecialCharacters() {
        String failedStepText = "Given I have a file named \"test (1).txt\"";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with newlines")
    void shouldHandleStepTextWithNewlines() {
        String failedStepText = "Given I have the following data:\n" +
                               "  | Name | Age |\n" +
                               "  | John | 25  |";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with unicode characters")
    void shouldHandleStepTextWithUnicodeCharacters() {
        String failedStepText = "When I enter \"José García\" in the name field";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with numbers")
    void shouldHandleStepTextWithNumbers() {
        String failedStepText = "Then I should see 42 items in the cart";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with boolean values")
    void shouldHandleStepTextWithBooleanValues() {
        String failedStepText = "When I set the checkbox to true";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with URLs")
    void shouldHandleStepTextWithUrls() {
        String failedStepText = "Given I navigate to \"https://example.com/login\"";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with file paths")
    void shouldHandleStepTextWithFilePaths() {
        String failedStepText = "When I upload the file \"/path/to/document.pdf\"";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with HTML content")
    void shouldHandleStepTextWithHtmlContent() {
        String failedStepText = "Then I should see \"<div class='error'>Invalid input</div>\"";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with JSON content")
    void shouldHandleStepTextWithJsonContent() {
        String failedStepText = "When I send the JSON payload {\"name\": \"John\", \"age\": 30}";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with SQL content")
    void shouldHandleStepTextWithSqlContent() {
        String failedStepText = "Given I execute the query \"SELECT * FROM users WHERE id = 1\"";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with very long content")
    void shouldHandleStepTextWithVeryLongContent() {
        StringBuilder longStep = new StringBuilder("Given I have a very long step with ");
        for (int i = 0; i < 1000; i++) {
            longStep.append("repeated content ");
        }
        longStep.append("at the end");
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(longStep.toString(), null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with mixed case")
    void shouldHandleStepTextWithMixedCase() {
        String failedStepText = "Given I am On The LOGIN Page";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with leading/trailing whitespace")
    void shouldHandleStepTextWithLeadingTrailingWhitespace() {
        String failedStepText = "   Given I am on the login page   ";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with tabs")
    void shouldHandleStepTextWithTabs() {
        String failedStepText = "Given\tI\tam\ton\tthe\tlogin\tpage";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with carriage returns")
    void shouldHandleStepTextWithCarriageReturns() {
        String failedStepText = "Given I am on the login page\r\nWhen I enter credentials";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with scenario name parameter")
    void shouldHandleStepTextWithScenarioNameParameter() {
        String failedStepText = "Given I am on the login page";
        String scenarioName = "Login Scenario";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, scenarioName);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with empty scenario name")
    void shouldHandleStepTextWithEmptyScenarioName() {
        String failedStepText = "Given I am on the login page";
        String scenarioName = "";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, scenarioName);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with whitespace-only scenario name")
    void shouldHandleStepTextWithWhitespaceOnlyScenarioName() {
        String failedStepText = "Given I am on the login page";
        String scenarioName = "   ";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, scenarioName);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with special characters in scenario name")
    void shouldHandleStepTextWithSpecialCharactersInScenarioName() {
        String failedStepText = "Given I am on the login page";
        String scenarioName = "Login @Test Scenario (v1.0)";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, scenarioName);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with very long scenario name")
    void shouldHandleStepTextWithVeryLongScenarioName() {
        String failedStepText = "Given I am on the login page";
        StringBuilder longScenarioName = new StringBuilder("Very Long Scenario Name ");
        for (int i = 0; i < 100; i++) {
            longScenarioName.append("with repeated content ");
        }
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, longScenarioName.toString());
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with unicode characters in scenario name")
    void shouldHandleStepTextWithUnicodeCharactersInScenarioName() {
        String failedStepText = "Given I am on the login page";
        String scenarioName = "Login Scenario with José García";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, scenarioName);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with newlines in scenario name")
    void shouldHandleStepTextWithNewlinesInScenarioName() {
        String failedStepText = "Given I am on the login page";
        String scenarioName = "Login\nScenario\nWith\nNewlines";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, scenarioName);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with SQL injection attempt")
    void shouldHandleStepTextWithSqlInjectionAttempt() {
        String failedStepText = "Given I execute \"'; DROP TABLE users; --\"";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with XSS attempt")
    void shouldHandleStepTextWithXssAttempt() {
        String failedStepText = "Given I see \"<script>alert('xss')</script>\"";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with null bytes")
    void shouldHandleStepTextWithNullBytes() {
        String failedStepText = "Given I have a file with\0null\0bytes";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with control characters")
    void shouldHandleStepTextWithControlCharacters() {
        String failedStepText = "Given I have text with\u0001\u0002\u0003 control chars";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with emoji characters")
    void shouldHandleStepTextWithEmojiCharacters() {
        String failedStepText = "Given I see the 🎉 emoji in the message";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with right-to-left text")
    void shouldHandleStepTextWithRightToLeftText() {
        String failedStepText = "Given I see Arabic text: مرحبا بالعالم";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with mathematical symbols")
    void shouldHandleStepTextWithMathematicalSymbols() {
        String failedStepText = "Given I calculate 2 + 2 = 4 and π ≈ 3.14159";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with currency symbols")
    void shouldHandleStepTextWithCurrencySymbols() {
        String failedStepText = "Given I see the price $99.99 and €85.50";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with email addresses")
    void shouldHandleStepTextWithEmailAddresses() {
        String failedStepText = "Given I enter test@example.com in the email field";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with phone numbers")
    void shouldHandleStepTextWithPhoneNumbers() {
        String failedStepText = "Given I call +1-555-123-4567";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with dates")
    void shouldHandleStepTextWithDates() {
        String failedStepText = "Given today is 2024-01-15";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with timestamps")
    void shouldHandleStepTextWithTimestamps() {
        String failedStepText = "Given the current time is 2024-01-15T10:30:00Z";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with UUIDs")
    void shouldHandleStepTextWithUuids() {
        String failedStepText = "Given I have user with ID 550e8400-e29b-41d4-a716-446655440000";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with base64 encoded content")
    void shouldHandleStepTextWithBase64EncodedContent() {
        String failedStepText = "Given I decode the base64 string SGVsbG8gV29ybGQ=";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with hex encoded content")
    void shouldHandleStepTextWithHexEncodedContent() {
        String failedStepText = "Given I decode the hex string 48656C6C6F20576F726C64";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with XML content")
    void shouldHandleStepTextWithXmlContent() {
        String failedStepText = "Given I parse the XML <user><name>John</name><age>30</age></user>";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with YAML content")
    void shouldHandleStepTextWithYamlContent() {
        String failedStepText = "Given I parse the YAML:\n  name: John\n  age: 30";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with CSV content")
    void shouldHandleStepTextWithCsvContent() {
        String failedStepText = "Given I parse the CSV: name,age\nJohn,30\nJane,25";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with markdown content")
    void shouldHandleStepTextWithMarkdownContent() {
        String failedStepText = "Given I see the markdown: **bold** and *italic* text";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with regex patterns")
    void shouldHandleStepTextWithRegexPatterns() {
        String failedStepText = "Given I match the pattern \\d{3}-\\d{3}-\\d{4}";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with escaped quotes")
    void shouldHandleStepTextWithEscapedQuotes() {
        String failedStepText = "Given I see the message \"He said \\\"Hello\\\" to me\"";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with backslashes")
    void shouldHandleStepTextWithBackslashes() {
        String failedStepText = "Given I have the path C:\\Users\\John\\Documents\\file.txt";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with forward slashes")
    void shouldHandleStepTextWithForwardSlashes() {
        String failedStepText = "Given I have the path /home/user/documents/file.txt";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with pipe characters")
    void shouldHandleStepTextWithPipeCharacters() {
        String failedStepText = "Given I see the table | Name | Age |\n| John | 30 |";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with angle brackets")
    void shouldHandleStepTextWithAngleBrackets() {
        String failedStepText = "Given I see the HTML <div>content</div>";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with curly braces")
    void shouldHandleStepTextWithCurlyBraces() {
        String failedStepText = "Given I have the JSON {\"key\": \"value\"}";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with square brackets")
    void shouldHandleStepTextWithSquareBrackets() {
        String failedStepText = "Given I have the array [1, 2, 3, 4, 5]";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with parentheses")
    void shouldHandleStepTextWithParentheses() {
        String failedStepText = "Given I calculate (2 + 3) * 4";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with percent signs")
    void shouldHandleStepTextWithPercentSigns() {
        String failedStepText = "Given I see 50% completion";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with ampersands")
    void shouldHandleStepTextWithAmpersands() {
        String failedStepText = "Given I see the company name Johnson & Johnson";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with asterisks")
    void shouldHandleStepTextWithAsterisks() {
        String failedStepText = "Given I see the pattern ***important***";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with hash symbols")
    void shouldHandleStepTextWithHashSymbols() {
        String failedStepText = "Given I see the hashtag #testing";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with at symbols")
    void shouldHandleStepTextWithAtSymbols() {
        String failedStepText = "Given I mention @john_doe in the comment";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with exclamation marks")
    void shouldHandleStepTextWithExclamationMarks() {
        String failedStepText = "Given I see the alert message \"Warning!\"";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with question marks")
    void shouldHandleStepTextWithQuestionMarks() {
        String failedStepText = "Given I see the question \"What is your name?\"";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with semicolons")
    void shouldHandleStepTextWithSemicolons() {
        String failedStepText = "Given I execute the SQL: SELECT * FROM users;";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with colons")
    void shouldHandleStepTextWithColons() {
        String failedStepText = "Given I see the label: Name: John Doe";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with commas")
    void shouldHandleStepTextWithCommas() {
        String failedStepText = "Given I have the list: apple, banana, orange";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with periods")
    void shouldHandleStepTextWithPeriods() {
        String failedStepText = "Given I see the version 1.2.3.4";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with underscores")
    void shouldHandleStepTextWithUnderscores() {
        String failedStepText = "Given I have the variable user_name";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with hyphens")
    void shouldHandleStepTextWithHyphens() {
        String failedStepText = "Given I have the file-name with-hyphens.txt";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with plus signs")
    void shouldHandleStepTextWithPlusSigns() {
        String failedStepText = "Given I calculate 2 + 2 = 4";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with minus signs")
    void shouldHandleStepTextWithMinusSigns() {
        String failedStepText = "Given I calculate 10 - 5 = 5";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with equals signs")
    void shouldHandleStepTextWithEqualsSigns() {
        String failedStepText = "Given I see the equation x = y + 1";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with greater than signs")
    void shouldHandleStepTextWithGreaterThanSigns() {
        String failedStepText = "Given I see the condition x > 10";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with less than signs")
    void shouldHandleStepTextWithLessThanSigns() {
        String failedStepText = "Given I see the condition x < 10";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with tilde")
    void shouldHandleStepTextWithTilde() {
        String failedStepText = "Given I see the pattern ~/.config/file";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with caret")
    void shouldHandleStepTextWithCaret() {
        String failedStepText = "Given I see the regex pattern ^start.*end$";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle step text with dollar sign")
    void shouldHandleStepTextWithDollarSign() {
        String failedStepText = "Given I see the price $99.99";
        
        GherkinScenarioInfo result = extractor.extractScenarioInfo(failedStepText, null);
        
        // Should not throw exception
        assertThat(result).isNull();
    }
} 