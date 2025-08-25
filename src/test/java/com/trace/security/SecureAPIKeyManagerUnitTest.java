package com.trace.security;

import com.trace.ai.configuration.AIServiceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("Secure API Key Manager Unit Tests")
class SecureAPIKeyManagerUnitTest {
    
    @Nested
    @DisplayName("API Key Storage")
    class ApiKeyStorage {
        
        @Test
        @DisplayName("should store OpenAI API key successfully")
        void shouldStoreOpenAiApiKeySuccessfully() {
            // Arrange
            String apiKey = "sk-1234567890abcdef1234567890abcdef1234567890abcdef";
            AIServiceType serviceType = AIServiceType.OPENAI;
            
            // Act
            boolean result = SecureAPIKeyManager.storeAPIKey(serviceType, apiKey);
            
            // Assert
            assertThat(result).isTrue();
            
            // Verify retrieval
            String retrievedKey = SecureAPIKeyManager.getAPIKey(serviceType);
            assertThat(retrievedKey).isEqualTo(apiKey);
        }
        
        @Test
        @DisplayName("should store Gemini API key successfully")
        void shouldStoreGeminiApiKeySuccessfully() {
            // Arrange
            String apiKey = "AIzaSyC1234567890abcdef1234567890abcdef1234567890abcdef";
            AIServiceType serviceType = AIServiceType.GEMINI;
            
            // Act
            boolean result = SecureAPIKeyManager.storeAPIKey(serviceType, apiKey);
            
            // Assert
            assertThat(result).isTrue();
            
            // Verify retrieval
            String retrievedKey = SecureAPIKeyManager.getAPIKey(serviceType);
            assertThat(retrievedKey).isEqualTo(apiKey);
        }
        
        @Test
        @DisplayName("should reject empty API key")
        void shouldRejectEmptyApiKey() {
            // Arrange
            String emptyKey = "";
            AIServiceType serviceType = AIServiceType.OPENAI;
            
            // Act
            boolean result = SecureAPIKeyManager.storeAPIKey(serviceType, emptyKey);
            
            // Assert
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("should reject null API key")
        void shouldRejectNullApiKey() {
            // Arrange
            AIServiceType serviceType = AIServiceType.OPENAI;
            
            // Act & Assert
            assertThatThrownBy(() -> SecureAPIKeyManager.storeAPIKey(serviceType, null))
                .isInstanceOf(IllegalArgumentException.class);
        }
        
        @Test
        @DisplayName("should reject null service type")
        void shouldRejectNullServiceType() {
            // Arrange
            String apiKey = "sk-1234567890abcdef1234567890abcdef1234567890abcdef";
            
            // Act & Assert
            assertThatThrownBy(() -> SecureAPIKeyManager.storeAPIKey(null, apiKey))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }
    
    @Nested
    @DisplayName("API Key Retrieval")
    class ApiKeyRetrieval {
        
        @Test
        @DisplayName("should retrieve stored OpenAI API key")
        void shouldRetrieveStoredOpenAiApiKey() {
            // Arrange
            String apiKey = "sk-1234567890abcdef1234567890abcdef1234567890abcdef";
            AIServiceType serviceType = AIServiceType.OPENAI;
            SecureAPIKeyManager.storeAPIKey(serviceType, apiKey);
            
            // Act
            String retrievedKey = SecureAPIKeyManager.getAPIKey(serviceType);
            
            // Assert
            assertThat(retrievedKey).isEqualTo(apiKey);
        }
        
        @Test
        @DisplayName("should retrieve stored Gemini API key")
        void shouldRetrieveStoredGeminiApiKey() {
            // Arrange
            String apiKey = "AIzaSyC1234567890abcdef1234567890abcdef1234567890abcdef";
            AIServiceType serviceType = AIServiceType.GEMINI;
            SecureAPIKeyManager.storeAPIKey(serviceType, apiKey);
            
            // Act
            String retrievedKey = SecureAPIKeyManager.getAPIKey(serviceType);
            
            // Assert
            assertThat(retrievedKey).isEqualTo(apiKey);
        }
        
        @Test
        @DisplayName("should return null for non-existent key")
        void shouldReturnNullForNonExistentKey() {
            // Arrange
            AIServiceType serviceType = AIServiceType.OPENAI;
            
            // Act
            String retrievedKey = SecureAPIKeyManager.getAPIKey(serviceType);
            
            // Assert
            assertThat(retrievedKey).isNull();
        }
        
        @Test
        @DisplayName("should throw exception for null service type")
        void shouldThrowExceptionForNullServiceType() {
            // Act & Assert
            assertThatThrownBy(() -> SecureAPIKeyManager.getAPIKey(null))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }
    
    @Nested
    @DisplayName("API Key Management")
    class ApiKeyManagement {
        
        @Test
        @DisplayName("should check if API key exists")
        void shouldCheckIfApiKeyExists() {
            // Arrange
            String apiKey = "sk-1234567890abcdef1234567890abcdef1234567890abcdef";
            AIServiceType serviceType = AIServiceType.OPENAI;
            
            // Act & Assert - Initially no key
            assertThat(SecureAPIKeyManager.hasAPIKey(serviceType)).isFalse();
            
            // Store key
            SecureAPIKeyManager.storeAPIKey(serviceType, apiKey);
            
            // Act & Assert - Now key exists
            assertThat(SecureAPIKeyManager.hasAPIKey(serviceType)).isTrue();
        }
        
        @Test
        @DisplayName("should clear API key successfully")
        void shouldClearApiKeySuccessfully() {
            // Arrange
            String apiKey = "sk-1234567890abcdef1234567890abcdef1234567890abcdef";
            AIServiceType serviceType = AIServiceType.OPENAI;
            SecureAPIKeyManager.storeAPIKey(serviceType, apiKey);
            
            // Verify key exists
            assertThat(SecureAPIKeyManager.hasAPIKey(serviceType)).isTrue();
            
            // Act
            boolean result = SecureAPIKeyManager.clearAPIKey(serviceType);
            
            // Assert
            assertThat(result).isTrue();
            assertThat(SecureAPIKeyManager.hasAPIKey(serviceType)).isFalse();
            assertThat(SecureAPIKeyManager.getAPIKey(serviceType)).isNull();
        }
        
        @Test
        @DisplayName("should handle clearing non-existent key")
        void shouldHandleClearingNonExistentKey() {
            // Arrange
            AIServiceType serviceType = AIServiceType.OPENAI;
            
            // Act
            boolean result = SecureAPIKeyManager.clearAPIKey(serviceType);
            
            // Assert
            assertThat(result).isTrue(); // Should succeed even if no key exists
        }
        
        @Test
        @DisplayName("should handle multiple service types independently")
        void shouldHandleMultipleServiceTypesIndependently() {
            // Arrange
            String openaiKey = "sk-1234567890abcdef1234567890abcdef1234567890abcdef";
            String geminiKey = "AIzaSyC1234567890abcdef1234567890abcdef1234567890abcdef";
            
            // Act - Store both keys
            SecureAPIKeyManager.storeAPIKey(AIServiceType.OPENAI, openaiKey);
            SecureAPIKeyManager.storeAPIKey(AIServiceType.GEMINI, geminiKey);
            
            // Assert - Both keys exist
            assertThat(SecureAPIKeyManager.hasAPIKey(AIServiceType.OPENAI)).isTrue();
            assertThat(SecureAPIKeyManager.hasAPIKey(AIServiceType.GEMINI)).isTrue();
            assertThat(SecureAPIKeyManager.getAPIKey(AIServiceType.OPENAI)).isEqualTo(openaiKey);
            assertThat(SecureAPIKeyManager.getAPIKey(AIServiceType.GEMINI)).isEqualTo(geminiKey);
            
            // Act - Clear one key
            SecureAPIKeyManager.clearAPIKey(AIServiceType.OPENAI);
            
            // Assert - Only one key remains
            assertThat(SecureAPIKeyManager.hasAPIKey(AIServiceType.OPENAI)).isFalse();
            assertThat(SecureAPIKeyManager.hasAPIKey(AIServiceType.GEMINI)).isTrue();
            assertThat(SecureAPIKeyManager.getAPIKey(AIServiceType.OPENAI)).isNull();
            assertThat(SecureAPIKeyManager.getAPIKey(AIServiceType.GEMINI)).isEqualTo(geminiKey);
        }
    }
}
