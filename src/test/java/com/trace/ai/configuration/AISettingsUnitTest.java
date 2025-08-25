package com.trace.ai.configuration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AI Settings Unit Tests")
class AISettingsUnitTest {
    
    private AISettings.State testState;
    private AISettings settings;
    
    @BeforeEach
    void setUp() {
        testState = new AISettings.State();
        // Create a testable instance by using reflection to access the private constructor
        // or by creating a test subclass. For now, we'll test the State class directly
        // and the public methods through a different approach
        settings = createTestableAISettings();
        settings.loadState(testState);
    }
    
    /**
     * Creates a testable instance of AISettings for unit testing.
     * This bypasses the singleton pattern to allow isolated testing.
     */
    private AISettings createTestableAISettings() {
        try {
            // Use reflection to access the private constructor
            java.lang.reflect.Constructor<AISettings> constructor = 
                AISettings.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create testable AISettings instance", e);
        }
    }
    
    @Nested
    @DisplayName("User Consent Management")
    class UserConsentManagement {
        
        @Test
        @DisplayName("should enable AI features when set to true")
        void shouldEnableAIFeatures_whenSetToTrue() {
            // Act
            settings.setAIEnabled(true);
            
            // Assert
            assertThat(settings.isAIEnabled()).isTrue();
            assertThat(settings.isTraceEnabled()).isTrue();
        }
        
        @Test
        @DisplayName("should disable AI features when set to false")
        void shouldDisableAIFeatures_whenSetToFalse() {
            // Act
            settings.setAIEnabled(false);
            
            // Assert
            assertThat(settings.isAIEnabled()).isFalse();
            assertThat(settings.isTraceEnabled()).isFalse();
        }
        
        @Test
        @DisplayName("should set user consent and record timestamp when consent given")
        void shouldSetUserConsentAndRecordTimestamp_whenConsentGiven() {
            // Act
            settings.setUserConsentGiven(true);
            
            // Assert
            assertThat(settings.hasUserConsent()).isTrue();
            assertThat(settings.getConsentDate()).isNotNull();
            assertThat(settings.getConsentDate()).isInstanceOf(LocalDateTime.class);
        }
        
        @Test
        @DisplayName("should clear consent date when consent revoked")
        void shouldClearConsentDate_whenConsentRevoked() {
            // Arrange
            settings.setUserConsentGiven(true);
            assertThat(settings.getConsentDate()).isNotNull();
            
            // Act
            settings.setUserConsentGiven(false);
            
            // Assert
            assertThat(settings.hasUserConsent()).isFalse();
            assertThat(settings.getConsentDate()).isNull();
        }
        
        @Test
        @DisplayName("should handle invalid consent date gracefully")
        void shouldHandleInvalidConsentDateGracefully() {
            // Arrange
            testState.consentDate = "invalid-date-format";
            
            // Act
            LocalDateTime result = settings.getConsentDate();
            
            // Assert
            assertThat(result).isNull();
        }
    }
    
    @Nested
    @DisplayName("AI Service Configuration")
    class AIServiceConfiguration {
        
        @Test
        @DisplayName("should set OpenAI as preferred service")
        void shouldSetOpenAIAsPreferredService() {
            // Act
            settings.setPreferredAIService(AIServiceType.OPENAI);
            
            // Assert
            assertThat(settings.getPreferredAIService()).isEqualTo(AIServiceType.OPENAI);
        }
        
        @Test
        @DisplayName("should set Gemini as preferred service")
        void shouldSetGeminiAsPreferredService() {
            // Act
            settings.setPreferredAIService(AIServiceType.GEMINI);
            
            // Assert
            assertThat(settings.getPreferredAIService()).isEqualTo(AIServiceType.GEMINI);
        }
        
        @Test
        @DisplayName("should return default service when invalid service ID provided")
        void shouldReturnDefaultService_whenInvalidServiceIdProvided() {
            // Arrange
            settings.setPreferredAIService("invalid-service");
            
            // Act
            AIServiceType result = settings.getPreferredAIService();
            
            // Assert
            assertThat(result).isEqualTo(AIServiceType.getDefault());
        }
        
        @Test
        @DisplayName("should handle null service type gracefully")
        void shouldHandleNullServiceTypeGracefully() {
            // Act
            settings.setPreferredAIService((AIServiceType) null);
            
            // Assert
            assertThat(settings.getPreferredAIService()).isEqualTo(AIServiceType.getDefault());
        }
        
        @Test
        @DisplayName("should set service by string ID")
        void shouldSetServiceByStringId() {
            // Act
            settings.setPreferredAIService("openai");
            
            // Assert
            assertThat(settings.getPreferredAIService()).isEqualTo(AIServiceType.OPENAI);
        }
    }
    
    @Nested
    @DisplayName("Analysis Configuration")
    class AnalysisConfiguration {
        
        @Test
        @DisplayName("should enable auto-analysis when set to true")
        void shouldEnableAutoAnalysis_whenSetToTrue() {
            // Act
            settings.setAutoAnalyzeEnabled(true);
            
            // Assert
            assertThat(settings.isAutoAnalyzeEnabled()).isTrue();
            assertThat(settings.isAIAnalysisEnabled()).isTrue();
        }
        
        @Test
        @DisplayName("should disable auto-analysis when set to false")
        void shouldDisableAutoAnalysis_whenSetToFalse() {
            // Act
            settings.setAutoAnalyzeEnabled(false);
            
            // Assert
            assertThat(settings.isAutoAnalyzeEnabled()).isFalse();
            assertThat(settings.isAIAnalysisEnabled()).isFalse();
        }
        
        @Test
        @DisplayName("should show confidence scores when enabled")
        void shouldShowConfidenceScores_whenEnabled() {
            // Act
            settings.setShowConfidenceScores(true);
            
            // Assert
            assertThat(settings.isShowConfidenceScores()).isTrue();
        }
        
        @Test
        @DisplayName("should hide confidence scores when disabled")
        void shouldHideConfidenceScores_whenDisabled() {
            // Act
            settings.setShowConfidenceScores(false);
            
            // Assert
            assertThat(settings.isShowConfidenceScores()).isFalse();
        }
    }
    
    @Nested
    @DisplayName("Chat Settings")
    class ChatSettings {
        
        @Test
        @DisplayName("should enable chat history persistence when set to true")
        void shouldEnableChatHistoryPersistence_whenSetToTrue() {
            // Act
            settings.setPersistChatHistory(true);
            
            // Assert
            assertThat(settings.isPersistChatHistory()).isTrue();
        }
        
        @Test
        @DisplayName("should disable chat history persistence when set to false")
        void shouldDisableChatHistoryPersistence_whenSetToFalse() {
            // Act
            settings.setPersistChatHistory(false);
            
            // Assert
            assertThat(settings.isPersistChatHistory()).isFalse();
        }
        
        @Test
        @DisplayName("should set valid chat history size")
        void shouldSetValidChatHistorySize() {
            // Act
            settings.setMaxChatHistorySize(100);
            
            // Assert
            assertThat(settings.getMaxChatHistorySize()).isEqualTo(100);
        }
        
        @Test
        @DisplayName("should clamp chat history size to minimum when below 10")
        void shouldClampChatHistorySizeToMinimum_whenBelow10() {
            // Act
            settings.setMaxChatHistorySize(5);
            
            // Assert
            assertThat(settings.getMaxChatHistorySize()).isEqualTo(10);
        }
        
        @Test
        @DisplayName("should clamp chat history size to maximum when above 500")
        void shouldClampChatHistorySizeToMaximum_whenAbove500() {
            // Act
            settings.setMaxChatHistorySize(1000);
            
            // Assert
            assertThat(settings.getMaxChatHistorySize()).isEqualTo(500);
        }
    }
    
    @Nested
    @DisplayName("Custom Rules")
    class CustomRules {
        
        @Test
        @DisplayName("should enable custom rules when set to true")
        void shouldEnableCustomRules_whenSetToTrue() {
            // Act
            settings.setCustomRulesEnabled(true);
            
            // Assert
            assertThat(settings.isCustomRulesEnabled()).isTrue();
        }
        
        @Test
        @DisplayName("should disable custom rules when set to false")
        void shouldDisableCustomRules_whenSetToFalse() {
            // Act
            settings.setCustomRulesEnabled(false);
            
            // Assert
            assertThat(settings.isCustomRulesEnabled()).isFalse();
        }
        
        @Test
        @DisplayName("should set custom rule text")
        void shouldSetCustomRuleText() {
            // Arrange
            String ruleText = "Always suggest unit tests for new methods";
            
            // Act
            settings.setCustomRule(ruleText);
            
            // Assert
            assertThat(settings.getCustomRule()).isEqualTo(ruleText);
        }
        
        @Test
        @DisplayName("should trim whitespace from custom rule")
        void shouldTrimWhitespaceFromCustomRule() {
            // Arrange
            String ruleText = "  Trim whitespace  ";
            
            // Act
            settings.setCustomRule(ruleText);
            
            // Assert
            assertThat(settings.getCustomRule()).isEqualTo("Trim whitespace");
        }
        
        @Test
        @DisplayName("should return null for empty custom rule")
        void shouldReturnNullForEmptyCustomRule() {
            // Act
            settings.setCustomRule("");
            
            // Assert
            assertThat(settings.getCustomRule()).isNull();
        }
        
        @Test
        @DisplayName("should handle null custom rule")
        void shouldHandleNullCustomRule() {
            // Act
            settings.setCustomRule(null);
            
            // Assert
            assertThat(settings.getCustomRule()).isNull();
        }
    }
    
    @Nested
    @DisplayName("Configuration Validation")
    class ConfigurationValidation {
        
        @Test
        @DisplayName("should return false when AI is disabled")
        void shouldReturnFalse_whenAIIsDisabled() {
            // Arrange
            settings.setAIEnabled(false);
            settings.setUserConsentGiven(true);
            settings.setPreferredAIService(AIServiceType.OPENAI);
            
            // Act
            boolean result = settings.isConfigured();
            
            // Assert
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("should return false when user consent not given")
        void shouldReturnFalse_whenUserConsentNotGiven() {
            // Arrange
            settings.setAIEnabled(true);
            settings.setUserConsentGiven(false);
            settings.setPreferredAIService(AIServiceType.OPENAI);
            
            // Act
            boolean result = settings.isConfigured();
            
            // Assert
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("should return false when no service configured")
        void shouldReturnFalse_whenNoServiceConfigured() {
            // Arrange
            settings.setAIEnabled(true);
            settings.setUserConsentGiven(true);
            settings.setPreferredAIService("");
            
            // Act
            boolean result = settings.isConfigured();
            
            // Assert
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("should return true when fully configured")
        void shouldReturnTrue_whenFullyConfigured() {
            // Arrange
            settings.setAIEnabled(true);
            settings.setUserConsentGiven(true);
            settings.setPreferredAIService(AIServiceType.OPENAI);
            
            // Act
            boolean result = settings.isConfigured();
            
            // Assert
            assertThat(result).isTrue();
        }
    }
    
    @Nested
    @DisplayName("State Management")
    class StateManagement {
        
        @Test
        @DisplayName("should get current state")
        void shouldGetCurrentState() {
            // Act
            AISettings.State state = settings.getState();
            
            // Assert
            assertThat(state).isNotNull();
            assertThat(state).isEqualTo(testState);
        }
        
        @Test
        @DisplayName("should load new state")
        void shouldLoadNewState() {
            // Arrange
            AISettings.State newState = new AISettings.State();
            newState.aiEnabled = true;
            newState.userConsentGiven = true;
            newState.preferredAIService = "openai";
            
            // Act
            settings.loadState(newState);
            
            // Assert
            assertThat(settings.isAIEnabled()).isTrue();
            assertThat(settings.hasUserConsent()).isTrue();
            assertThat(settings.getPreferredAIService()).isEqualTo(AIServiceType.OPENAI);
        }
        
        @Test
        @DisplayName("should reset to defaults")
        void shouldResetToDefaults() {
            // Arrange
            settings.setAIEnabled(true);
            settings.setUserConsentGiven(true);
            settings.setPreferredAIService(AIServiceType.GEMINI);
            settings.setAutoAnalyzeEnabled(false);
            
            // Act
            settings.resetToDefaults();
            
            // Assert
            assertThat(settings.isAIEnabled()).isFalse();
            assertThat(settings.hasUserConsent()).isFalse();
            assertThat(settings.getPreferredAIService()).isEqualTo(AIServiceType.getDefault());
            assertThat(settings.isAutoAnalyzeEnabled()).isTrue();
        }
    }
    
    @Nested
    @DisplayName("Configuration Status")
    class ConfigurationStatus {
        
        @Test
        @DisplayName("should return disabled status when AI is disabled")
        void shouldReturnDisabledStatus_whenAIIsDisabled() {
            // Arrange
            settings.setAIEnabled(false);
            
            // Act
            String status = settings.getConfigurationStatus();
            
            // Assert
            assertThat(status).isEqualTo("AI features are disabled");
        }
        
        @Test
        @DisplayName("should return consent required status when consent not given")
        void shouldReturnConsentRequiredStatus_whenConsentNotGiven() {
            // Arrange
            settings.setAIEnabled(true);
            settings.setUserConsentGiven(false);
            
            // Act
            String status = settings.getConfigurationStatus();
            
            // Assert
            assertThat(status).isEqualTo("User consent required");
        }
        
        @Test
        @DisplayName("should return no service status when no service configured")
        void shouldReturnNoServiceStatus_whenNoServiceConfigured() {
            // Arrange
            settings.setAIEnabled(true);
            settings.setUserConsentGiven(true);
            settings.setPreferredAIService("");
            
            // Act
            String status = settings.getConfigurationStatus();
            
            // Assert
            assertThat(status).isEqualTo("No AI service configured");
        }
        
        @Test
        @DisplayName("should return configured status with auto-analysis")
        void shouldReturnConfiguredStatusWithAutoAnalysis() {
            // Arrange
            settings.setAIEnabled(true);
            settings.setUserConsentGiven(true);
            settings.setPreferredAIService(AIServiceType.OPENAI);
            settings.setAutoAnalyzeEnabled(true);
            
            // Act
            String status = settings.getConfigurationStatus();
            
            // Assert
            assertThat(status).isEqualTo("Configured for openai with auto-analysis");
        }
        
        @Test
        @DisplayName("should return configured status without auto-analysis")
        void shouldReturnConfiguredStatusWithoutAutoAnalysis() {
            // Arrange
            settings.setAIEnabled(true);
            settings.setUserConsentGiven(true);
            settings.setPreferredAIService(AIServiceType.GEMINI);
            settings.setAutoAnalyzeEnabled(false);
            
            // Act
            String status = settings.getConfigurationStatus();
            
            // Assert
            assertThat(status).isEqualTo("Configured for gemini with manual analysis");
        }
    }
}
