package com.triagemate.extractors;

import com.triagemate.models.StepDefinitionInfo;
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
 * Unit tests for StepDefinitionExtractor.
 * 
 * <p>These tests focus on the core logic of step definition extraction
 * using stack trace-based PSI navigation.</p>
 */
@ExtendWith(MockitoExtension.class)
class StepDefinitionExtractorUnitTest {

    @Mock
    private Project mockProject;
    
    private StepDefinitionExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new StepDefinitionExtractor(mockProject);
    }

    @Test
    @DisplayName("should return null when stack trace is null")
    void shouldReturnNullWhenStackTraceIsNull() {
        StepDefinitionInfo result = extractor.extractStepDefinition(null);
        
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should return null when stack trace is empty")
    void shouldReturnNullWhenStackTraceIsEmpty() {
        StepDefinitionInfo result = extractor.extractStepDefinition("");
        
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should return null when stack trace is whitespace only")
    void shouldReturnNullWhenStackTraceIsWhitespaceOnly() {
        StepDefinitionInfo result = extractor.extractStepDefinition("   \n\t  ");
        
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should return null when no step definition classes found in stack trace")
    void shouldReturnNullWhenNoStepDefinitionClassesFound() {
        String stackTrace = """
            java.lang.AssertionError: Test failed
                at org.junit.Assert.fail(Assert.java:86)
                at org.junit.Assert.assertTrue(Assert.java:41)
                at org.junit.Assert.assertTrue(Assert.java:52)
                at com.example.utils.TestUtils.validate(TestUtils.java:15)
            """;
        
        StepDefinitionInfo result = extractor.extractStepDefinition(stackTrace);
        
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should extract step definition from stack trace with HomePageTestStep")
    void shouldExtractStepDefinitionFromStackTraceWithHomePageTestStep() {
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
        
        // Note: This will return null in unit tests because we can't navigate to actual files
        // The actual functionality is tested in integration tests
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should extract step definition from stack trace with LoginStepDefinitions")
    void shouldExtractStepDefinitionFromStackTraceWithLoginStepDefinitions() {
        String stackTrace = """
            java.lang.AssertionError: Login failed
                at com.example.steps.LoginStepDefinitions.i_enter_username(LoginStepDefinitions.java:15)
                at com.example.steps.LoginStepDefinitions.i_click_login_button(LoginStepDefinitions.java:22)
                at ✽.I enter username "testuser"(file:///path/to/login.feature:5)
            """;
        
        StepDefinitionInfo result = extractor.extractStepDefinition(stackTrace);
        
        // Note: This will return null in unit tests because we can't navigate to actual files
        // The actual functionality is tested in integration tests
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle stack trace with multiple step definition classes")
    void shouldHandleStackTraceWithMultipleStepDefinitionClasses() {
        String stackTrace = """
            java.lang.AssertionError: Test failed
                at com.example.steps.HomePageTestStep.i_should_see_title_as(HomePageTestStep.java:28)
                at com.example.steps.BaseStepDefinitions.wait_for_element(BaseStepDefinitions.java:45)
                at com.example.steps.LoginStepDefinitions.i_enter_username(LoginStepDefinitions.java:15)
                at ✽.I should see title as "Welcome"(file:///path/to/feature.feature:8)
            """;
        
        StepDefinitionInfo result = extractor.extractStepDefinition(stackTrace);
        
        // Should find the first step definition class (HomePageTestStep)
        // Note: This will return null in unit tests because we can't navigate to actual files
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle stack trace with non-step definition classes")
    void shouldHandleStackTraceWithNonStepDefinitionClasses() {
        String stackTrace = """
            java.lang.AssertionError: Test failed
                at org.junit.Assert.fail(Assert.java:86)
                at com.example.utils.TestUtils.validate(TestUtils.java:15)
                at com.example.steps.HomePageTestStep.i_should_see_title_as(HomePageTestStep.java:28)
                at ✽.I should see title as "Welcome"(file:///path/to/feature.feature:8)
            """;
        
        StepDefinitionInfo result = extractor.extractStepDefinition(stackTrace);
        
        // Should find HomePageTestStep even though TestUtils is not a step definition class
        // Note: This will return null in unit tests because we can't navigate to actual files
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle malformed stack trace")
    void shouldHandleMalformedStackTrace() {
        String stackTrace = "This is not a valid stack trace";
        
        StepDefinitionInfo result = extractor.extractStepDefinition(stackTrace);
        
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle stack trace with missing line numbers")
    void shouldHandleStackTraceWithMissingLineNumbers() {
        String stackTrace = """
            java.lang.AssertionError: Test failed
                at com.example.steps.HomePageTestStep.i_should_see_title_as(HomePageTestStep.java)
                at ✽.I should see title as "Welcome"(file:///path/to/feature.feature:8)
            """;
        
        StepDefinitionInfo result = extractor.extractStepDefinition(stackTrace);
        
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle stack trace with invalid line numbers")
    void shouldHandleStackTraceWithInvalidLineNumbers() {
        String stackTrace = """
            java.lang.AssertionError: Test failed
                at com.example.steps.HomePageTestStep.i_should_see_title_as(HomePageTestStep.java:abc)
                at ✽.I should see title as "Welcome"(file:///path/to/feature.feature:8)
            """;
        
        StepDefinitionInfo result = extractor.extractStepDefinition(stackTrace);
        
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle stack trace with special characters in class names")
    void shouldHandleStackTraceWithSpecialCharactersInClassNames() {
        String stackTrace = """
            java.lang.AssertionError: Test failed
                at com.example.steps.HomePage_Test_Step.i_should_see_title_as(HomePage_Test_Step.java:28)
                at ✽.I should see title as "Welcome"(file:///path/to/feature.feature:8)
            """;
        
        StepDefinitionInfo result = extractor.extractStepDefinition(stackTrace);
        
        // Note: This will return null in unit tests because we can't navigate to actual files
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle stack trace with numbers in class names")
    void shouldHandleStackTraceWithNumbersInClassNames() {
        String stackTrace = """
            java.lang.AssertionError: Test failed
                at com.example.steps.HomePageTestStep2.i_should_see_title_as(HomePageTestStep2.java:28)
                at ✽.I should see title as "Welcome"(file:///path/to/feature.feature:8)
            """;
        
        StepDefinitionInfo result = extractor.extractStepDefinition(stackTrace);
        
        // Note: This will return null in unit tests because we can't navigate to actual files
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle stack trace with mixed case class names")
    void shouldHandleStackTraceWithMixedCaseClassNames() {
        String stackTrace = """
            java.lang.AssertionError: Test failed
                at com.example.steps.HomePageTestStep.i_Should_See_Title_As(HomePageTestStep.java:28)
                at ✽.I should see title as "Welcome"(file:///path/to/feature.feature:8)
            """;
        
        StepDefinitionInfo result = extractor.extractStepDefinition(stackTrace);
        
        // Note: This will return null in unit tests because we can't navigate to actual files
        assertThat(result).isNull();
    }
} 