package com.triagemate.services;

import com.intellij.openapi.project.Project;
import com.triagemate.models.FailureInfo;
import com.triagemate.models.StepDefinitionInfo;
import com.triagemate.settings.AIServiceType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AINetworkService.
 * 
 * <p>These tests verify the core functionality of the AI network service including
 * service initialization, prompt generation integration, and result handling.
 * Network calls are not tested to avoid complex IntelliJ environment dependencies.</p>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class AINetworkServiceUnitTest {
    
    @Mock
    private Project mockProject;
    
    private AINetworkService aiNetworkService;
    private FailureInfo testFailureInfo;
    
    @BeforeEach
    void setUp() {
        // Initialize the service
        aiNetworkService = new AINetworkService(mockProject);
        
        // Create test failure info
        testFailureInfo = new FailureInfo.Builder()
                .withScenarioName("User login with valid credentials")
                .withFailedStepText("I click the login button")
                .withErrorMessage("Element not found: login-button")
                .withStackTrace("org.openqa.selenium.NoSuchElementException: Element not found")
                .withSourceFilePath("src/test/java/com/example/LoginSteps.java")
                .withLineNumber(25)
                .withStepDefinitionInfo(new StepDefinitionInfo.Builder()
                        .withMethodName("clickLoginButton")
                        .withClassName("LoginSteps")
                        .withPackageName("com.example")
                        .withStepPattern("@When(\"I click the login button\")")
                        .withMethodText("public void clickLoginButton() { driver.findElement(By.id(\"login-button\")).click(); }")
                        .build())
                .withExpectedValue("true")
                .withActualValue("false")
                .withParsingTime(150L)
                .build();
    }
    
    @AfterEach
    void tearDown() {
        // Clean up resources
        if (aiNetworkService != null) {
            aiNetworkService.shutdown();
        }
    }
    
    @Test
    void testServiceInitialization() {
        // Verify service is properly initialized
        assertNotNull(aiNetworkService);
        
        // Verify project is set
        // Note: We can't directly access the project field, but we can verify the service works
        assertDoesNotThrow(() -> aiNetworkService.shutdown());
    }
    
    @Test
    void testAnalyzeWithNullFailureInfo() {
        // Test that null failure info throws exception
        assertThrows(IllegalArgumentException.class, () -> {
            aiNetworkService.analyze(null);
        });
    }
    
    @Test
    void testServiceShutdown() {
        // Test that shutdown works without errors
        assertDoesNotThrow(() -> aiNetworkService.shutdown());
        
        // Test that shutdown can be called multiple times safely
        assertDoesNotThrow(() -> aiNetworkService.shutdown());
    }
    
    @Test
    void testAIAnalysisResultCreation() {
        // Test AIAnalysisResult creation and getters
        AINetworkService.AIAnalysisResult result = new AINetworkService.AIAnalysisResult(
                "Test analysis",
                AIServiceType.OPENAI,
                System.currentTimeMillis(),
                AINetworkService.ConfidenceLevel.HIGH
        );
        
        assertEquals("Test analysis", result.getAnalysis());
        assertEquals(AIServiceType.OPENAI, result.getServiceType());
        assertTrue(result.getTimestamp() > 0);
        assertEquals(AINetworkService.ConfidenceLevel.HIGH, result.getConfidenceLevel());
        
        // Test toString method
        String toString = result.toString();
        assertTrue(toString.contains("AIAnalysisResult"));
        assertTrue(toString.contains("serviceType=OpenAI")); // Note: enum toString() uses the actual enum name
        assertTrue(toString.contains("confidenceLevel=HIGH"));
        assertTrue(toString.contains("analysisLength=13")); // "Test analysis" has 13 characters
    }
    
    @Test
    void testConfidenceLevelEnum() {
        // Test all confidence levels
        assertEquals(3, AINetworkService.ConfidenceLevel.values().length);
        assertNotNull(AINetworkService.ConfidenceLevel.valueOf("HIGH"));
        assertNotNull(AINetworkService.ConfidenceLevel.valueOf("MEDIUM"));
        assertNotNull(AINetworkService.ConfidenceLevel.valueOf("LOW"));
    }
    
    @Test
    void testAnalyzeWithValidFailureInfo() {
        // Test that analyze method accepts valid failure info
        // Note: This will fail due to IntelliJ environment dependencies, but we can verify the method signature
        assertDoesNotThrow(() -> {
            // The method should not throw an exception for valid input
            // The actual network call will fail in test environment, but that's expected
            aiNetworkService.analyze(testFailureInfo);
        });
    }
    
    @Test
    void testAnalyzeWithMinimalFailureInfo() {
        // Test with minimal failure info
        FailureInfo minimalFailureInfo = new FailureInfo.Builder()
                .withErrorMessage("Test error")
                .build();
        
        // Test that analyze method accepts minimal failure info
        assertDoesNotThrow(() -> {
            // The method should not throw an exception for valid input
            // The actual network call will fail in test environment, but that's expected
            aiNetworkService.analyze(minimalFailureInfo);
        });
    }
} 