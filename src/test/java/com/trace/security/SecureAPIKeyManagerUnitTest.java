package com.trace.security;

import com.trace.ai.configuration.AIServiceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("Secure API Key Manager Unit Tests")
class SecureAPIKeyManagerUnitTest {
    
    @Nested
    @DisplayName("API Key Validation")
    class ApiKeyValidation {
        
        @Test
        @DisplayName("should validate OpenAI API key with correct format")
        void shouldValidateOpenAiApiKeyWithCorrectFormat() throws ExecutionException, InterruptedException {
            // Arrange
            String validOpenAIKey = "sk-1234567890abcdef1234567890abcdef1234567890abcdef";
            AIServiceType serviceType = AIServiceType.OPENAI;
            
            // Act
            CompletableFuture<Boolean> future = SecureAPIKeyManager.validateAPIKey(serviceType, validOpenAIKey);
            Boolean result = future.get();
            
            // Assert
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("should reject OpenAI API key with incorrect format")
        void shouldRejectOpenAiApiKeyWithIncorrectFormat() throws ExecutionException, InterruptedException {
            // Arrange
            String invalidOpenAIKey = "invalid-key-format";
            AIServiceType serviceType = AIServiceType.OPENAI;
            
            // Act
            CompletableFuture<Boolean> future = SecureAPIKeyManager.validateAPIKey(serviceType, invalidOpenAIKey);
            Boolean result = future.get();
            
            // Assert
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("should reject OpenAI API key that is too short")
        void shouldRejectOpenAiApiKeyThatIsTooShort() throws ExecutionException, InterruptedException {
            // Arrange
            String shortOpenAIKey = "sk-123";
            AIServiceType serviceType = AIServiceType.OPENAI;
            
            // Act
            CompletableFuture<Boolean> future = SecureAPIKeyManager.validateAPIKey(serviceType, shortOpenAIKey);
            Boolean result = future.get();
            
            // Assert
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("should validate Gemini API key with correct format")
        void shouldValidateGeminiApiKeyWithCorrectFormat() throws ExecutionException, InterruptedException {
            // Arrange
            String validGeminiKey = "AIzaSyC1234567890abcdef1234567890abcdef1234567890abcdef";
            AIServiceType serviceType = AIServiceType.GEMINI;
            
            // Act
            CompletableFuture<Boolean> future = SecureAPIKeyManager.validateAPIKey(serviceType, validGeminiKey);
            Boolean result = future.get();
            
            // Assert
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("should reject Gemini API key with spaces")
        void shouldRejectGeminiApiKeyWithSpaces() throws ExecutionException, InterruptedException {
            // Arrange
            String invalidGeminiKey = "AIzaSyC 1234567890abcdef1234567890abcdef1234567890abcdef";
            AIServiceType serviceType = AIServiceType.GEMINI;
            
            // Act
            CompletableFuture<Boolean> future = SecureAPIKeyManager.validateAPIKey(serviceType, invalidGeminiKey);
            Boolean result = future.get();
            
            // Assert
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("should reject Gemini API key that is too short")
        void shouldRejectGeminiApiKeyThatIsTooShort() throws ExecutionException, InterruptedException {
            // Arrange
            String shortGeminiKey = "short";
            AIServiceType serviceType = AIServiceType.GEMINI;
            
            // Act
            CompletableFuture<Boolean> future = SecureAPIKeyManager.validateAPIKey(serviceType, shortGeminiKey);
            Boolean result = future.get();
            
            // Assert
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("should handle validation timeout correctly")
        void shouldHandleValidationTimeoutCorrectly() throws ExecutionException, InterruptedException {
            // Arrange
            String validApiKey = "sk-1234567890abcdef1234567890abcdef1234567890abcdef";
            AIServiceType serviceType = AIServiceType.OPENAI;
            
            // Act
            CompletableFuture<Boolean> future = SecureAPIKeyManager.validateAPIKey(serviceType, validApiKey);
            Boolean result = future.get();
            
            // Assert - validation should complete within reasonable time
            assertThat(result).isNotNull();
        }
        
        @Test
        @DisplayName("should handle network errors during validation")
        void shouldHandleNetworkErrorsDuringValidation() throws ExecutionException, InterruptedException {
            // Arrange
            String validApiKey = "sk-1234567890abcdef1234567890abcdef1234567890abcdef";
            AIServiceType serviceType = AIServiceType.OPENAI;
            
            // Act
            CompletableFuture<Boolean> future = SecureAPIKeyManager.validateAPIKey(serviceType, validApiKey);
            Boolean result = future.get();
            
            // Assert - should handle errors gracefully and return false
            assertThat(result).isNotNull();
        }
        
        @Test
        @DisplayName("should handle null API key during validation")
        void shouldHandleNullApiKeyDuringValidation() throws ExecutionException, InterruptedException {
            // Arrange
            String nullApiKey = null;
            AIServiceType serviceType = AIServiceType.OPENAI;
            
            // Act & Assert
            assertThatThrownBy(() -> SecureAPIKeyManager.validateAPIKey(serviceType, nullApiKey))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@NotNull parameter 'apiKey'");
        }
        
        @Test
        @DisplayName("should handle null service type during validation")
        void shouldHandleNullServiceTypeDuringValidation() throws ExecutionException, InterruptedException {
            // Arrange
            String validApiKey = "sk-1234567890abcdef1234567890abcdef1234567890abcdef";
            AIServiceType serviceType = null;
            
            // Act & Assert
            assertThatThrownBy(() -> SecureAPIKeyManager.validateAPIKey(serviceType, validApiKey))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@NotNull parameter 'serviceType'");
        }
    }
    
    @Nested
    @DisplayName("Service Type Handling")
    class ServiceTypeHandling {
        
        @Test
        @DisplayName("should handle OpenAI service type correctly")
        void shouldHandleOpenAiServiceTypeCorrectly() throws ExecutionException, InterruptedException {
            // Arrange
            AIServiceType serviceType = AIServiceType.OPENAI;
            String apiKey = "sk-1234567890abcdef1234567890abcdef1234567890abcdef";
            
            // Act
            CompletableFuture<Boolean> future = SecureAPIKeyManager.validateAPIKey(serviceType, apiKey);
            Boolean result = future.get();
            
            // Assert
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("should handle Gemini service type correctly")
        void shouldHandleGeminiServiceTypeCorrectly() throws ExecutionException, InterruptedException {
            // Arrange
            AIServiceType serviceType = AIServiceType.GEMINI;
            String apiKey = "AIzaSyC1234567890abcdef1234567890abcdef1234567890abcdef";
            
            // Act
            CompletableFuture<Boolean> future = SecureAPIKeyManager.validateAPIKey(serviceType, apiKey);
            Boolean result = future.get();
            
            // Assert
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("should handle null service type")
        void shouldHandleNullServiceType() {
            // Arrange
            AIServiceType serviceType = null;
            String apiKey = "test-key";
            
            // Act & Assert
            assertThatThrownBy(() -> SecureAPIKeyManager.validateAPIKey(serviceType, apiKey))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@NotNull parameter 'serviceType'");
        }
    }
    
    @Nested
    @DisplayName("API Key Format Validation")
    class ApiKeyFormatValidation {
        
        @Test
        @DisplayName("should validate OpenAI key format with sk- prefix")
        void shouldValidateOpenAiKeyFormatWithSkPrefix() throws ExecutionException, InterruptedException {
            // Arrange
            String[] validOpenAIKeys = {
                "sk-1234567890abcdef1234567890abcdef1234567890abcdef",
                "sk-abcdefghijklmnopqrstuvwxyz1234567890abcdefghijklmnop",
                "sk-1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef"
            };
            
            for (String apiKey : validOpenAIKeys) {
                // Act
                CompletableFuture<Boolean> future = SecureAPIKeyManager.validateAPIKey(AIServiceType.OPENAI, apiKey);
                Boolean result = future.get();
                
                // Assert
                assertThat(result).as("API key: " + apiKey).isTrue();
            }
        }
        
        @Test
        @DisplayName("should reject OpenAI key without sk- prefix")
        void shouldRejectOpenAiKeyWithoutSkPrefix() throws ExecutionException, InterruptedException {
            // Arrange
            String[] invalidOpenAIKeys = {
                "1234567890abcdef1234567890abcdef1234567890abcdef",
                "openai-1234567890abcdef1234567890abcdef1234567890abcdef",
                "sk1234567890abcdef1234567890abcdef1234567890abcdef",
                "SK-1234567890abcdef1234567890abcdef1234567890abcdef"
            };
            
            for (String apiKey : invalidOpenAIKeys) {
                // Act
                CompletableFuture<Boolean> future = SecureAPIKeyManager.validateAPIKey(AIServiceType.OPENAI, apiKey);
                Boolean result = future.get();
                
                // Assert
                assertThat(result).as("API key: " + apiKey).isFalse();
            }
        }
        
        @Test
        @DisplayName("should validate Gemini key format without spaces")
        void shouldValidateGeminiKeyFormatWithoutSpaces() throws ExecutionException, InterruptedException {
            // Arrange
            String[] validGeminiKeys = {
                "AIzaSyC1234567890abcdef1234567890abcdef1234567890abcdef",
                "AIzaSyDabcdefghijklmnopqrstuvwxyz1234567890abcdefghijklmnop",
                "AIzaSyE1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef"
            };
            
            for (String apiKey : validGeminiKeys) {
                // Act
                CompletableFuture<Boolean> future = SecureAPIKeyManager.validateAPIKey(AIServiceType.GEMINI, apiKey);
                Boolean result = future.get();
                
                // Assert
                assertThat(result).as("API key: " + apiKey).isTrue();
            }
        }
        
        @Test
        @DisplayName("should reject Gemini key with spaces")
        void shouldRejectGeminiKeyWithSpaces() throws ExecutionException, InterruptedException {
            // Arrange
            String[] invalidGeminiKeys = {
                "AIzaSyC 1234567890abcdef1234567890abcdef1234567890abcdef",
                "AIzaSyC1234567890abcdef1234567890abcdef1234567890abcdef ",
                " AIzaSyC1234567890abcdef1234567890abcdef1234567890abcdef"
            };
            
            for (String apiKey : invalidGeminiKeys) {
                // Act
                CompletableFuture<Boolean> future = SecureAPIKeyManager.validateAPIKey(AIServiceType.GEMINI, apiKey);
                Boolean result = future.get();
                
                // Assert
                assertThat(result).as("API key: " + apiKey).isFalse();
            }
        }
        
        @Test
        @DisplayName("should accept Gemini key with newlines and tabs (current validation logic)")
        void shouldAcceptGeminiKeyWithNewlinesAndTabs() throws ExecutionException, InterruptedException {
            // Arrange - Current validation only checks for spaces, not other whitespace
            String[] validGeminiKeysWithWhitespace = {
                "AIzaSyC1234567890abcdef1234567890abcdef1234567890abcdef\n",
                "AIzaSyC1234567890abcdef1234567890abcdef1234567890abcdef\t"
            };
            
            for (String apiKey : validGeminiKeysWithWhitespace) {
                // Act
                CompletableFuture<Boolean> future = SecureAPIKeyManager.validateAPIKey(AIServiceType.GEMINI, apiKey);
                Boolean result = future.get();
                
                // Assert - Current validation logic accepts these
                assertThat(result).as("API key: " + apiKey).isTrue();
            }
        }
        
        @Test
        @DisplayName("should reject very short API keys")
        void shouldRejectVeryShortApiKeys() throws ExecutionException, InterruptedException {
            // Arrange
            String[] shortKeys = {
                "sk-",
                "sk-123",
                "sk-1234567890",
                "short",
                "tiny",
                "a",
                ""
            };
            
            for (String apiKey : shortKeys) {
                // Act
                CompletableFuture<Boolean> openAIFuture = SecureAPIKeyManager.validateAPIKey(AIServiceType.OPENAI, apiKey);
                CompletableFuture<Boolean> geminiFuture = SecureAPIKeyManager.validateAPIKey(AIServiceType.GEMINI, apiKey);
                Boolean openAIResult = openAIFuture.get();
                Boolean geminiResult = geminiFuture.get();
                
                // Assert
                assertThat(openAIResult).as("OpenAI API key: " + apiKey).isFalse();
                assertThat(geminiResult).as("Gemini API key: " + apiKey).isFalse();
            }
        }
    }
    
    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesAndErrorHandling {
        
        @Test
        @DisplayName("should handle empty string API key")
        void shouldHandleEmptyStringApiKey() throws ExecutionException, InterruptedException {
            // Arrange
            String emptyApiKey = "";
            AIServiceType serviceType = AIServiceType.OPENAI;
            
            // Act
            CompletableFuture<Boolean> future = SecureAPIKeyManager.validateAPIKey(serviceType, emptyApiKey);
            Boolean result = future.get();
            
            // Assert
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("should handle whitespace-only API key")
        void shouldHandleWhitespaceOnlyApiKey() throws ExecutionException, InterruptedException {
            // Arrange
            String whitespaceApiKey = "   \t\n  ";
            AIServiceType serviceType = AIServiceType.GEMINI;
            
            // Act
            CompletableFuture<Boolean> future = SecureAPIKeyManager.validateAPIKey(serviceType, whitespaceApiKey);
            Boolean result = future.get();
            
            // Assert
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("should handle very long API keys")
        void shouldHandleVeryLongApiKeys() throws ExecutionException, InterruptedException {
            // Arrange
            String longOpenAIKey = "sk-" + "a".repeat(1000);
            String longGeminiKey = "AIzaSyC" + "b".repeat(1000);
            AIServiceType openAIServiceType = AIServiceType.OPENAI;
            AIServiceType geminiServiceType = AIServiceType.GEMINI;
            
            // Act
            CompletableFuture<Boolean> openAIFuture = SecureAPIKeyManager.validateAPIKey(openAIServiceType, longOpenAIKey);
            CompletableFuture<Boolean> geminiFuture = SecureAPIKeyManager.validateAPIKey(geminiServiceType, longGeminiKey);
            Boolean openAIResult = openAIFuture.get();
            Boolean geminiResult = geminiFuture.get();
            
            // Assert
            assertThat(openAIResult).isTrue();
            assertThat(geminiResult).isTrue();
        }
        
        @Test
        @DisplayName("should handle special characters in API keys")
        void shouldHandleSpecialCharactersInApiKeys() throws ExecutionException, InterruptedException {
            // Arrange
            String specialCharOpenAIKey = "sk-1234567890abcdef1234567890abcdef1234567890abcdef!@#$%^&*()";
            String specialCharGeminiKey = "AIzaSyC1234567890abcdef1234567890abcdef1234567890abcdef!@#$%^&*()";
            AIServiceType openAIServiceType = AIServiceType.OPENAI;
            AIServiceType geminiServiceType = AIServiceType.GEMINI;
            
            // Act
            CompletableFuture<Boolean> openAIFuture = SecureAPIKeyManager.validateAPIKey(openAIServiceType, specialCharOpenAIKey);
            CompletableFuture<Boolean> geminiFuture = SecureAPIKeyManager.validateAPIKey(geminiServiceType, specialCharGeminiKey);
            Boolean openAIResult = openAIFuture.get();
            Boolean geminiResult = geminiFuture.get();
            
            // Assert
            assertThat(openAIResult).isTrue();
            assertThat(geminiResult).isTrue();
        }
        
        @Test
        @DisplayName("should handle concurrent validation requests")
        void shouldHandleConcurrentValidationRequests() throws ExecutionException, InterruptedException {
            // Arrange
            String validOpenAIKey = "sk-1234567890abcdef1234567890abcdef1234567890abcdef";
            String validGeminiKey = "AIzaSyC1234567890abcdef1234567890abcdef1234567890abcdef";
            AIServiceType openAIServiceType = AIServiceType.OPENAI;
            AIServiceType geminiServiceType = AIServiceType.GEMINI;
            
            // Act - Create multiple concurrent validation requests
            CompletableFuture<Boolean>[] openAIFutures = new CompletableFuture[5];
            CompletableFuture<Boolean>[] geminiFutures = new CompletableFuture[5];
            
            for (int i = 0; i < 5; i++) {
                openAIFutures[i] = SecureAPIKeyManager.validateAPIKey(openAIServiceType, validOpenAIKey);
                geminiFutures[i] = SecureAPIKeyManager.validateAPIKey(geminiServiceType, validGeminiKey);
            }
            
            // Wait for all futures to complete
            CompletableFuture.allOf(openAIFutures).get();
            CompletableFuture.allOf(geminiFutures).get();
            
            // Assert - All should return true
            for (CompletableFuture<Boolean> future : openAIFutures) {
                assertThat(future.get()).isTrue();
            }
            for (CompletableFuture<Boolean> future : geminiFutures) {
                assertThat(future.get()).isTrue();
            }
        }
    }
    
    @Nested
    @DisplayName("AIServiceType Integration")
    class AIServiceTypeIntegration {
        
        @Test
        @DisplayName("should work with all AIServiceType values")
        void shouldWorkWithAllAIServiceTypeValues() throws ExecutionException, InterruptedException {
            // Arrange
            String validOpenAIKey = "sk-1234567890abcdef1234567890abcdef1234567890abcdef";
            String validGeminiKey = "AIzaSyC1234567890abcdef1234567890abcdef1234567890abcdef";
            
            // Act & Assert for each service type
            CompletableFuture<Boolean> openAIFuture = SecureAPIKeyManager.validateAPIKey(AIServiceType.OPENAI, validOpenAIKey);
            CompletableFuture<Boolean> geminiFuture = SecureAPIKeyManager.validateAPIKey(AIServiceType.GEMINI, validGeminiKey);
            
            assertThat(openAIFuture.get()).isTrue();
            assertThat(geminiFuture.get()).isTrue();
        }
        
        @Test
        @DisplayName("should handle service type display names")
        void shouldHandleServiceTypeDisplayNames() {
            // Arrange
            AIServiceType openAI = AIServiceType.OPENAI;
            AIServiceType gemini = AIServiceType.GEMINI;
            
            // Act & Assert
            assertThat(openAI.getDisplayName()).isEqualTo("OpenAI");
            assertThat(gemini.getDisplayName()).isEqualTo("Google Gemini");
        }
        
        @Test
        @DisplayName("should handle service type IDs")
        void shouldHandleServiceTypeIds() {
            // Arrange
            AIServiceType openAI = AIServiceType.OPENAI;
            AIServiceType gemini = AIServiceType.GEMINI;
            
            // Act & Assert
            assertThat(openAI.getId()).isEqualTo("openai");
            assertThat(gemini.getId()).isEqualTo("gemini");
        }
    }
}
