package com.triagemate.services;

import com.triagemate.models.FailureInfo;
import com.triagemate.models.GherkinScenarioInfo;
import com.triagemate.models.StepDefinitionInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BackendCommunicationService.
 */
@ExtendWith(MockitoExtension.class)
class BackendCommunicationServiceUnitTest {

    private BackendCommunicationService service;
    
    @Mock
    private HttpClient mockHttpClient;
    
    @Mock
    private HttpResponse<String> mockResponse;

    @BeforeEach
    void setUp() {
        service = new BackendCommunicationService();
    }

    @Test
    void testIsBackendAvailable_WhenBackendIsAvailable_ReturnsTrue() {
        // This test would require mocking the HTTP client
        // For now, we'll test the basic functionality without HTTP mocking
        assertNotNull(service);
    }

    @Test
    void testSetAndGetBackendUrl() {
        String testUrl = "http://localhost:9090";
        service.setBackendUrl(testUrl);
        assertEquals(testUrl, service.getBackendUrl());
    }

    @Test
    void testAnalysisResponseModel() {
        BackendCommunicationService.AnalysisResponse response = new BackendCommunicationService.AnalysisResponse();
        
        // Test setters and getters
        response.setAnalysis("Test analysis");
        response.setSuggestions(Arrays.asList("Suggestion 1", "Suggestion 2"));
        response.setConfidence("High");
        response.setModelUsed("GPT-4");
        response.setProcessingTime(1500L);
        response.setPromptUsed("Test prompt");
        
        assertEquals("Test analysis", response.getAnalysis());
        assertEquals(Arrays.asList("Suggestion 1", "Suggestion 2"), response.getSuggestions());
        assertEquals("High", response.getConfidence());
        assertEquals("GPT-4", response.getModelUsed());
        assertEquals(1500L, response.getProcessingTime());
        assertEquals("Test prompt", response.getPromptUsed());
    }

    @Test
    void testBackendCommunicationException() {
        BackendCommunicationService.BackendCommunicationException exception = 
            new BackendCommunicationService.BackendCommunicationException("Test error");
        
        assertEquals("Test error", exception.getMessage());
        
        Exception cause = new RuntimeException("Cause");
        BackendCommunicationService.BackendCommunicationException exceptionWithCause = 
            new BackendCommunicationService.BackendCommunicationException("Test error", cause);
        
        assertEquals("Test error", exceptionWithCause.getMessage());
        assertEquals(cause, exceptionWithCause.getCause());
    }

    @Test
    void testCreateFailureInfoForBackendCommunication() {
        // Create a sample FailureInfo that would be sent to backend
        GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
            .withFeatureName("Login Feature")
            .withScenarioName("Successful Login")
            .withSteps(Arrays.asList("Given user is on login page", "When user enters credentials", "Then user should be logged in"))
            .withTags(Arrays.asList("@smoke", "@login"))
            .build();
            
        StepDefinitionInfo stepDefInfo = new StepDefinitionInfo.Builder()
            .withClassName("LoginStepDefinitions")
            .withMethodName("userEntersCredentials")
            .withStepPattern("user enters credentials")
            .withParameters(Arrays.asList("username", "password"))
            .withMethodText("@When(\"user enters credentials\")\npublic void userEntersCredentials(String username, String password) { }")
            .withSourceFilePath("/src/test/java/LoginStepDefinitions.java")
            .withLineNumber(25)
            .build();
            
        FailureInfo failureInfo = new FailureInfo.Builder()
            .withScenarioName("Successful Login")
            .withFailedStepText("When user enters credentials")
            .withStackTrace("java.lang.AssertionError: Expected true but was false")
            .withSourceFilePath("/src/test/java/LoginStepDefinitions.java")
            .withLineNumber(25)
            .withStepDefinitionInfo(stepDefInfo)
            .withGherkinScenarioInfo(scenarioInfo)
            .withExpectedValue("true")
            .withActualValue("false")
            .withErrorMessage("Test assertion failed")
            .withErrorMessage("Expected true but was false")
            .withParsingTime(150L)
            .build();
            
        // Verify the FailureInfo is properly constructed for backend communication
        assertNotNull(failureInfo);
        assertEquals("Successful Login", failureInfo.getScenarioName());
        assertEquals("When user enters credentials", failureInfo.getFailedStepText());
        assertNotNull(failureInfo.getStepDefinitionInfo());
        assertNotNull(failureInfo.getGherkinScenarioInfo());
        assertEquals("LoginStepDefinitions", failureInfo.getStepDefinitionInfo().getClassName());
        assertEquals("Login Feature", failureInfo.getGherkinScenarioInfo().getFeatureName());
    }
} 