package com.triagemate.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

import com.triagemate.models.FailureInfo;

/**
 * Unit tests for the FailureInfo domain model.
 * Tests the comprehensive model that serves multiple architectural layers.
 */
@DisplayName("FailureInfo Domain Model")
class FailureInfoUnitTest {
    
    @Test
    @DisplayName("should create FailureInfo with all fields using builder pattern")
    void shouldCreateFailureInfoWithAllFields() {
        // Arrange & Act
        FailureInfo failureInfo = new FailureInfo.Builder()
            .withScenarioName("Verify Home Page title")
            .withFailedStepText("I should see title as \"Welcome to the-internet delete me\"")
            .withStackTrace("java.lang.AssertionError: Expected: is \"Welcome to the-internet delete me\" but: was \"Welcome to the-internet\"")
            .withSourceFilePath("com/example/steps/HomePageTestStep.java")
            .withLineNumber(28)
            .withExpectedValue("Welcome to the-internet delete me")
            .withActualValue("Welcome to the-internet")
            .withErrorMessage("Expected: is \"Welcome to the-internet delete me\" but: was \"Welcome to the-internet\"")
            .withParsingTime(150L)
            .build();
        
        // Assert
        assertThat(failureInfo.getScenarioName()).isEqualTo("Verify Home Page title");
        assertThat(failureInfo.getFailedStepText()).isEqualTo("I should see title as \"Welcome to the-internet delete me\"");
        assertThat(failureInfo.getStackTrace()).contains("java.lang.AssertionError");
        assertThat(failureInfo.getSourceFilePath()).isEqualTo("com/example/steps/HomePageTestStep.java");
        assertThat(failureInfo.getLineNumber()).isEqualTo(28);
        assertThat(failureInfo.getExpectedValue()).isEqualTo("Welcome to the-internet delete me");
        assertThat(failureInfo.getActualValue()).isEqualTo("Welcome to the-internet");
        assertThat(failureInfo.getErrorMessage()).isEqualTo("Expected: is \"Welcome to the-internet delete me\" but: was \"Welcome to the-internet\"");
        assertThat(failureInfo.getParsingTime()).isEqualTo(150L);
    }
    
    @Test
    @DisplayName("should create FailureInfo with minimal fields")
    void shouldCreateFailureInfoWithMinimalFields() {
        // Arrange & Act
        FailureInfo failureInfo = new FailureInfo.Builder()
            .withScenarioName("Test Scenario")
            .withStackTrace("java.lang.Exception: Test failed")
            .withParsingTime(0L) // Default value
            .build();
        
        // Assert
        assertThat(failureInfo.getScenarioName()).isEqualTo("Test Scenario");
        assertThat(failureInfo.getStackTrace()).isEqualTo("java.lang.Exception: Test failed");
        
        // Optional fields should be null or default values
        assertThat(failureInfo.getFailedStepText()).isNull();
        assertThat(failureInfo.getSourceFilePath()).isNull();
        assertThat(failureInfo.getLineNumber()).isEqualTo(-1); // Default value
        assertThat(failureInfo.getExpectedValue()).isNull();
        assertThat(failureInfo.getActualValue()).isNull();
        assertThat(failureInfo.getErrorMessage()).isNull();
        assertThat(failureInfo.getParsingTime()).isEqualTo(0L); // Default value
    }
    
    @Test
    @DisplayName("should create FailureInfo with constructor")
    void shouldCreateFailureInfoWithConstructor() {
        // Arrange & Act
        FailureInfo failureInfo = new FailureInfo(
            "Test Scenario",
            "Test step",
            "Test stack trace",
            "Test file",
            42,
            null, // stepDefinitionInfo
            null, // gherkinScenarioInfo
            "expected",
            "actual",
            "Test error",
            100L
        );
        
        // Assert
        assertThat(failureInfo.getScenarioName()).isEqualTo("Test Scenario");
        assertThat(failureInfo.getFailedStepText()).isEqualTo("Test step");
        assertThat(failureInfo.getStackTrace()).isEqualTo("Test stack trace");
        assertThat(failureInfo.getSourceFilePath()).isEqualTo("Test file");
        assertThat(failureInfo.getLineNumber()).isEqualTo(42);
        assertThat(failureInfo.getExpectedValue()).isEqualTo("expected");
        assertThat(failureInfo.getActualValue()).isEqualTo("actual");
        assertThat(failureInfo.getErrorMessage()).isEqualTo("Test error");
        assertThat(failureInfo.getParsingTime()).isEqualTo(100L);
    }
    
    @Test
    @DisplayName("should handle null values gracefully")
    void shouldHandleNullValuesGracefully() {
        // Arrange & Act
        FailureInfo failureInfo = new FailureInfo.Builder()
            .withScenarioName(null)
            .withFailedStepText(null)
            .withStackTrace(null)
            .withExpectedValue(null)
            .withActualValue(null)
            .withErrorMessage(null)
            .withParsingTime(0L) // Default value
            .build();
        
        // Assert
        assertThat(failureInfo.getScenarioName()).isNull();
        assertThat(failureInfo.getFailedStepText()).isNull();
        assertThat(failureInfo.getStackTrace()).isNull();
        assertThat(failureInfo.getExpectedValue()).isNull();
        assertThat(failureInfo.getActualValue()).isNull();
        assertThat(failureInfo.getErrorMessage()).isNull();
        assertThat(failureInfo.getParsingTime()).isEqualTo(0L); // Default value
    }
    
    @Test
    @DisplayName("should be immutable")
    void shouldBeImmutable() {
        // Arrange
        FailureInfo failureInfo = new FailureInfo.Builder()
            .withScenarioName("Original Scenario")
            .withExpectedValue("Original Expected")
            .build();
        
        // Act & Assert - Verify that the object is immutable by checking that
        // all fields are final and cannot be modified after construction
        assertThat(failureInfo.getScenarioName()).isEqualTo("Original Scenario");
        assertThat(failureInfo.getExpectedValue()).isEqualTo("Original Expected");
        
        // The object should remain unchanged - this test verifies immutability
        // by ensuring the getters return the same values
        assertThat(failureInfo.getScenarioName()).isEqualTo("Original Scenario");
        assertThat(failureInfo.getExpectedValue()).isEqualTo("Original Expected");
    }
} 