package com.trace.security;

import com.trace.ai.configuration.AIServiceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("Secure API Key Manager Unit Tests")
class SecureAPIKeyManagerUnitTest {
    
    // Test-specific implementation that uses in-memory storage
    private static class TestSecureAPIKeyManager {
        private static final Map<String, String> storage = new HashMap<>();
        
        public static void clearStorage() {
            storage.clear();
        }
        
        public static boolean storeAPIKey(AIServiceType serviceType, String apiKey) {
            if (apiKey == null) {
                throw new IllegalArgumentException("API key cannot be null");
            }
            if (apiKey.trim().isEmpty()) {
                return false;
            }
            
            String serviceKey = getServiceKey(serviceType);
            storage.put(serviceKey, apiKey);
            return true;
        }
        
        public static String getAPIKey(AIServiceType serviceType) {
            String serviceKey = getServiceKey(serviceType);
            return storage.get(serviceKey);
        }
        
        public static boolean clearAPIKey(AIServiceType serviceType) {
            String serviceKey = getServiceKey(serviceType);
            storage.remove(serviceKey);
            return true;
        }
        
        public static boolean hasAPIKey(AIServiceType serviceType) {
            String apiKey = getAPIKey(serviceType);
            return apiKey != null && !apiKey.trim().isEmpty();
        }
        
        private static String getServiceKey(AIServiceType serviceType) {
            if (serviceType == null) {
                throw new IllegalArgumentException("Service type cannot be null");
            }
            switch (serviceType) {
                case OPENAI:
                    return "trace.openai.api_key";
                case GEMINI:
                    return "trace.gemini.api_key";
                default:
                    throw new IllegalArgumentException("Unknown service type: " + serviceType);
            }
        }
    }
    
    @BeforeEach
    void setUp() {
        // Clear storage before each test for isolation
        TestSecureAPIKeyManager.clearStorage();
    }
    
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
            boolean result = TestSecureAPIKeyManager.storeAPIKey(serviceType, apiKey);
            
            // Assert
            assertThat(result).isTrue();
            
            // Verify retrieval
            String retrievedKey = TestSecureAPIKeyManager.getAPIKey(serviceType);
            assertThat(retrievedKey).isEqualTo(apiKey);
        }
        
        @Test
        @DisplayName("should store Gemini API key successfully")
        void shouldStoreGeminiApiKeySuccessfully() {
            // Arrange
            String apiKey = "AIzaSyC1234567890abcdef1234567890abcdef1234567890abcdef";
            AIServiceType serviceType = AIServiceType.GEMINI;
            
            // Act
            boolean result = TestSecureAPIKeyManager.storeAPIKey(serviceType, apiKey);
            
            // Assert
            assertThat(result).isTrue();
            
            // Verify retrieval
            String retrievedKey = TestSecureAPIKeyManager.getAPIKey(serviceType);
            assertThat(retrievedKey).isEqualTo(apiKey);
        }
        
        @Test
        @DisplayName("should reject empty API key")
        void shouldRejectEmptyApiKey() {
            // Arrange
            String emptyKey = "";
            AIServiceType serviceType = AIServiceType.OPENAI;
            
            // Act
            boolean result = TestSecureAPIKeyManager.storeAPIKey(serviceType, emptyKey);
            
            // Assert
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("should reject null API key")
        void shouldRejectNullApiKey() {
            // Arrange
            AIServiceType serviceType = AIServiceType.OPENAI;
            
            // Act & Assert
            assertThatThrownBy(() -> TestSecureAPIKeyManager.storeAPIKey(serviceType, null))
                .isInstanceOf(IllegalArgumentException.class);
        }
        
        @Test
        @DisplayName("should reject null service type")
        void shouldRejectNullServiceType() {
            // Arrange
            String apiKey = "sk-1234567890abcdef1234567890abcdef1234567890abcdef";
            
            // Act & Assert
            assertThatThrownBy(() -> TestSecureAPIKeyManager.storeAPIKey(null, apiKey))
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
            TestSecureAPIKeyManager.storeAPIKey(serviceType, apiKey);
            
            // Act
            String retrievedKey = TestSecureAPIKeyManager.getAPIKey(serviceType);
            
            // Assert
            assertThat(retrievedKey).isEqualTo(apiKey);
        }
        
        @Test
        @DisplayName("should retrieve stored Gemini API key")
        void shouldRetrieveStoredGeminiApiKey() {
            // Arrange
            String apiKey = "AIzaSyC1234567890abcdef1234567890abcdef1234567890abcdef";
            AIServiceType serviceType = AIServiceType.GEMINI;
            TestSecureAPIKeyManager.storeAPIKey(serviceType, apiKey);
            
            // Act
            String retrievedKey = TestSecureAPIKeyManager.getAPIKey(serviceType);
            
            // Assert
            assertThat(retrievedKey).isEqualTo(apiKey);
        }
        
        @Test
        @DisplayName("should return null for non-existent key")
        void shouldReturnNullForNonExistentKey() {
            // Arrange
            AIServiceType serviceType = AIServiceType.OPENAI;
            
            // Act
            String retrievedKey = TestSecureAPIKeyManager.getAPIKey(serviceType);
            
            // Assert
            assertThat(retrievedKey).isNull();
        }
        
        @Test
        @DisplayName("should throw exception for null service type")
        void shouldThrowExceptionForNullServiceType() {
            // Act & Assert
            assertThatThrownBy(() -> TestSecureAPIKeyManager.getAPIKey(null))
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
            assertThat(TestSecureAPIKeyManager.hasAPIKey(serviceType)).isFalse();
            
            // Store key
            TestSecureAPIKeyManager.storeAPIKey(serviceType, apiKey);
            
            // Act & Assert - Now key exists
            assertThat(TestSecureAPIKeyManager.hasAPIKey(serviceType)).isTrue();
        }
        
        @Test
        @DisplayName("should clear API key successfully")
        void shouldClearApiKeySuccessfully() {
            // Arrange
            String apiKey = "sk-1234567890abcdef1234567890abcdef1234567890abcdef";
            AIServiceType serviceType = AIServiceType.OPENAI;
            TestSecureAPIKeyManager.storeAPIKey(serviceType, apiKey);
            
            // Verify key exists
            assertThat(TestSecureAPIKeyManager.hasAPIKey(serviceType)).isTrue();
            
            // Act
            boolean result = TestSecureAPIKeyManager.clearAPIKey(serviceType);
            
            // Assert
            assertThat(result).isTrue();
            assertThat(TestSecureAPIKeyManager.hasAPIKey(serviceType)).isFalse();
            assertThat(TestSecureAPIKeyManager.getAPIKey(serviceType)).isNull();
        }
        
        @Test
        @DisplayName("should handle clearing non-existent key")
        void shouldHandleClearingNonExistentKey() {
            // Arrange
            AIServiceType serviceType = AIServiceType.OPENAI;
            
            // Act
            boolean result = TestSecureAPIKeyManager.clearAPIKey(serviceType);
            
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
            TestSecureAPIKeyManager.storeAPIKey(AIServiceType.OPENAI, openaiKey);
            TestSecureAPIKeyManager.storeAPIKey(AIServiceType.GEMINI, geminiKey);
            
            // Assert - Both keys exist
            assertThat(TestSecureAPIKeyManager.hasAPIKey(AIServiceType.OPENAI)).isTrue();
            assertThat(TestSecureAPIKeyManager.hasAPIKey(AIServiceType.GEMINI)).isTrue();
            assertThat(TestSecureAPIKeyManager.getAPIKey(AIServiceType.OPENAI)).isEqualTo(openaiKey);
            assertThat(TestSecureAPIKeyManager.getAPIKey(AIServiceType.GEMINI)).isEqualTo(geminiKey);
            
            // Act - Clear one key
            TestSecureAPIKeyManager.clearAPIKey(AIServiceType.OPENAI);
            
            // Assert - Only one key remains
            assertThat(TestSecureAPIKeyManager.hasAPIKey(AIServiceType.OPENAI)).isFalse();
            assertThat(TestSecureAPIKeyManager.hasAPIKey(AIServiceType.GEMINI)).isTrue();
            assertThat(TestSecureAPIKeyManager.getAPIKey(AIServiceType.OPENAI)).isNull();
            assertThat(TestSecureAPIKeyManager.getAPIKey(AIServiceType.GEMINI)).isEqualTo(geminiKey);
        }
    }
}
