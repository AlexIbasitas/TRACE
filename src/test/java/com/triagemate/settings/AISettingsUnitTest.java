package com.triagemate.settings;

import com.intellij.openapi.application.ApplicationManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AISettings service.
 * 
 * <p>These tests verify the core functionality of the AISettings service including
 * state management, user consent handling, and configuration validation.</p>
 * 
 * <p>This test uses mocking to avoid requiring the full IntelliJ Platform test framework.</p>
 * 
 * @author TriageMate Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
public class AISettingsUnitTest {
    
    private AISettings aiSettings;
    private ApplicationManager mockApplicationManager;
    
    @BeforeEach
    public void setUp() {
        // Create a mock ApplicationManager
        mockApplicationManager = Mockito.mock(ApplicationManager.class);
        
        // Create a new AISettings instance for testing using reflection
        try {
            java.lang.reflect.Constructor<AISettings> constructor = AISettings.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            aiSettings = constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create AISettings instance for testing", e);
        }
        
        // Reset to defaults before each test
        aiSettings.resetToDefaults();
    }
    
    @AfterEach
    public void tearDown() {
        // Reset to defaults to prevent test interference
        if (aiSettings != null) {
            aiSettings.resetToDefaults();
        }
        
        // Clear any mock references
        mockApplicationManager = null;
        aiSettings = null;
    }
    
    @Test
    public void shouldInitializeWithDefaultValues() {
        // Verify default values
        assertFalse(aiSettings.isAIEnabled(), "AI should be disabled by default");
        assertFalse(aiSettings.hasUserConsent(), "User consent should be false by default");
        assertNull(aiSettings.getConsentDate(), "Consent date should be null by default");
        assertEquals(AIServiceType.OPENAI, aiSettings.getPreferredAIService(), "Default AI service should be OpenAI");
        assertTrue(aiSettings.isAutoAnalyzeEnabled(), "Auto-analysis should be enabled by default");
        assertTrue(aiSettings.isShowConfidenceScores(), "Confidence scores should be shown by default");
        assertTrue(aiSettings.isPersistChatHistory(), "Chat history should be persisted by default");
        assertEquals(50, aiSettings.getMaxChatHistorySize(), "Default max history size should be 50");
        assertFalse(aiSettings.isCustomRulesEnabled(), "Custom rules should be disabled by default");
    }
    
    @Test
    public void shouldManageAIEnabledState() {
        // Test setting AI enabled
        aiSettings.setAIEnabled(true);
        assertTrue(aiSettings.isAIEnabled(), "AI should be enabled");
        
        // Test setting AI disabled
        aiSettings.setAIEnabled(false);
        assertFalse(aiSettings.isAIEnabled(), "AI should be disabled");
    }
    
    @Test
    public void shouldManageUserConsent() {
        // Test setting consent
        aiSettings.setUserConsentGiven(true);
        assertTrue(aiSettings.hasUserConsent(), "User consent should be true");
        assertNotNull(aiSettings.getConsentDate(), "Consent date should be set");
        
        // Test revoking consent
        aiSettings.setUserConsentGiven(false);
        assertFalse(aiSettings.hasUserConsent(), "User consent should be false");
        assertNull(aiSettings.getConsentDate(), "Consent date should be null");
    }
    
    @Test
    public void shouldManagePreferredAIService() {
        // Test setting OpenAI
        aiSettings.setPreferredAIService(AIServiceType.OPENAI);
        assertEquals(AIServiceType.OPENAI, aiSettings.getPreferredAIService(), "Preferred service should be OpenAI");
        
        // Test setting Gemini
        aiSettings.setPreferredAIService(AIServiceType.GEMINI);
        assertEquals(AIServiceType.GEMINI, aiSettings.getPreferredAIService(), "Preferred service should be Gemini");
        
        // Test setting null (should default to OpenAI)
        aiSettings.setPreferredAIService((AIServiceType) null);
        assertEquals(AIServiceType.OPENAI, aiSettings.getPreferredAIService(), "Preferred service should default to OpenAI");
        
        // Test backward compatibility with string IDs
        aiSettings.setPreferredAIService("gemini");
        assertEquals(AIServiceType.GEMINI, aiSettings.getPreferredAIService(), "Should handle string ID correctly");
        
        // Test invalid string ID (should default to OpenAI)
        aiSettings.setPreferredAIService("invalid-service");
        assertEquals(AIServiceType.OPENAI, aiSettings.getPreferredAIService(), "Should default to OpenAI for invalid service");
    }
    
    @Test
    public void shouldManageAutoAnalysisSetting() {
        // Test enabling auto-analysis
        aiSettings.setAutoAnalyzeEnabled(true);
        assertTrue(aiSettings.isAutoAnalyzeEnabled(), "Auto-analysis should be enabled");
        
        // Test disabling auto-analysis
        aiSettings.setAutoAnalyzeEnabled(false);
        assertFalse(aiSettings.isAutoAnalyzeEnabled(), "Auto-analysis should be disabled");
    }
    
    @Test
    public void shouldManageConfidenceScoresSetting() {
        // Test showing confidence scores
        aiSettings.setShowConfidenceScores(true);
        assertTrue(aiSettings.isShowConfidenceScores(), "Confidence scores should be shown");
        
        // Test hiding confidence scores
        aiSettings.setShowConfidenceScores(false);
        assertFalse(aiSettings.isShowConfidenceScores(), "Confidence scores should be hidden");
    }
    
    @Test
    public void shouldManageChatHistoryPersistence() {
        // Test enabling chat history persistence
        aiSettings.setPersistChatHistory(true);
        assertTrue(aiSettings.isPersistChatHistory(), "Chat history should be persisted");
        
        // Test disabling chat history persistence
        aiSettings.setPersistChatHistory(false);
        assertFalse(aiSettings.isPersistChatHistory(), "Chat history should not be persisted");
    }
    
    @Test
    public void shouldManageMaxChatHistorySizeWithBounds() {
        // Test setting valid size
        aiSettings.setMaxChatHistorySize(100);
        assertEquals(100, aiSettings.getMaxChatHistorySize(), "Max history size should be 100");
        
        // Test setting size below minimum (should clamp to 10)
        aiSettings.setMaxChatHistorySize(5);
        assertEquals(10, aiSettings.getMaxChatHistorySize(), "Max history size should be clamped to 10");
        
        // Test setting size above maximum (should clamp to 500)
        aiSettings.setMaxChatHistorySize(1000);
        assertEquals(500, aiSettings.getMaxChatHistorySize(), "Max history size should be clamped to 500");
    }
    
    @Test
    public void shouldManageCustomRulesSetting() {
        // Test enabling custom rules
        aiSettings.setCustomRulesEnabled(true);
        assertTrue(aiSettings.isCustomRulesEnabled(), "Custom rules should be enabled");
        
        // Test disabling custom rules
        aiSettings.setCustomRulesEnabled(false);
        assertFalse(aiSettings.isCustomRulesEnabled(), "Custom rules should be disabled");
    }
    
    @Test
    public void shouldValidateConfigurationStatusCorrectly() {
        // Test when AI is disabled
        aiSettings.setAIEnabled(false);
        assertFalse(aiSettings.isConfigured(), "Should not be configured when AI is disabled");
        assertEquals("AI features are disabled", aiSettings.getConfigurationStatus());
        
        // Test when AI is enabled but no consent
        aiSettings.setAIEnabled(true);
        aiSettings.setUserConsentGiven(false);
        assertFalse(aiSettings.isConfigured(), "Should not be configured without consent");
        assertEquals("User consent required", aiSettings.getConfigurationStatus());
        
        // Test when AI is enabled and consent given but no service
        aiSettings.setUserConsentGiven(true);
        aiSettings.setPreferredAIService("");
        assertFalse(aiSettings.isConfigured(), "Should not be configured without service");
        assertEquals("No AI service configured", aiSettings.getConfigurationStatus());
        
        // Test when fully configured
        aiSettings.setPreferredAIService(AIServiceType.OPENAI);
        assertTrue(aiSettings.isConfigured(), "Should be configured when all requirements met");
        assertTrue(aiSettings.getConfigurationStatus().contains("openai"), "Status should mention configured service");
    }
    
    @Test
    public void shouldResetToDefaultsCorrectly() {
        // Set some custom values
        aiSettings.setAIEnabled(true);
        aiSettings.setUserConsentGiven(true);
        aiSettings.setPreferredAIService(AIServiceType.GEMINI);
        aiSettings.setAutoAnalyzeEnabled(false);
        aiSettings.setMaxChatHistorySize(100);
        
        // Reset to defaults
        aiSettings.resetToDefaults();
        
        // Verify all values are back to defaults
        assertFalse(aiSettings.isAIEnabled(), "AI should be disabled after reset");
        assertFalse(aiSettings.hasUserConsent(), "User consent should be false after reset");
        assertNull(aiSettings.getConsentDate(), "Consent date should be null after reset");
        assertEquals(AIServiceType.OPENAI, aiSettings.getPreferredAIService(), "Service should be OpenAI after reset");
        assertTrue(aiSettings.isAutoAnalyzeEnabled(), "Auto-analysis should be enabled after reset");
        assertEquals(50, aiSettings.getMaxChatHistorySize(), "Max history size should be 50 after reset");
    }
    
    @Test
    public void shouldHandleStatePersistence() {
        // Set some values
        aiSettings.setAIEnabled(true);
        aiSettings.setUserConsentGiven(true);
        aiSettings.setPreferredAIService(AIServiceType.GEMINI);
        
        // Get the state
        AISettings.State state = aiSettings.getState();
        
        // Verify state contains correct values
        assertTrue(state.aiEnabled, "State should contain AI enabled");
        assertTrue(state.userConsentGiven, "State should contain user consent");
        assertEquals("gemini", state.preferredAIService, "State should contain preferred service ID");
        
        // Create new state with different values
        AISettings.State newState = new AISettings.State();
        newState.aiEnabled = false;
        newState.userConsentGiven = false;
        newState.preferredAIService = "openai";
        
        // Load the new state
        aiSettings.loadState(newState);
        
        // Verify values are updated
        assertFalse(aiSettings.isAIEnabled(), "AI should be disabled after loading new state");
        assertFalse(aiSettings.hasUserConsent(), "User consent should be false after loading new state");
        assertEquals(AIServiceType.OPENAI, aiSettings.getPreferredAIService(), "Service should be OpenAI after loading new state");
    }
    
    @Test
    public void shouldProvideSingletonInstance() {
        // Mock the ApplicationManager.getApplication() call
        try (MockedStatic<ApplicationManager> mockedApplicationManager = Mockito.mockStatic(ApplicationManager.class)) {
            // Mock the application instance
            com.intellij.openapi.application.Application mockApplication = Mockito.mock(com.intellij.openapi.application.Application.class);
            mockedApplicationManager.when(ApplicationManager::getApplication).thenReturn(mockApplication);
            
            // Mock the getService call
            when(mockApplication.getService(AISettings.class)).thenReturn(aiSettings);
            
            // Test singleton behavior
            AISettings instance1 = AISettings.getInstance();
            AISettings instance2 = AISettings.getInstance();
            
            assertNotNull(instance1, "First instance should not be null");
            assertNotNull(instance2, "Second instance should not be null");
            assertSame(instance1, instance2, "Both instances should be the same");
        }
    }
} 