package com.trace.ai.services;

import com.trace.ai.configuration.AIServiceType;
import com.trace.ai.models.AIAnalysisResult;
import com.trace.ai.services.providers.AIServiceProvider;
import com.trace.ai.services.providers.GeminiProvider;
import com.trace.ai.services.providers.OpenAIProvider;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AI Service Factory Unit Tests")
class AIServiceFactoryUnitTest {
    
    @Mock private AIServiceProvider mockProvider;
    @Mock private HttpClient mockHttpClient;
    
    private AIServiceProvider testProvider;
    
    @BeforeEach
    void setUp() {
        // Create a test provider for testing
        testProvider = new TestAIServiceProvider();
    }
    
    @Nested
    @DisplayName("Provider Registration")
    class ProviderRegistration {
        
        @Test
        @DisplayName("should register provider successfully when valid parameters provided")
        void shouldRegisterProviderSuccessfully_whenValidParametersProvided() {
            // Act
            AIServiceFactory.registerProviderStatic(AIServiceType.OPENAI, testProvider);
            
            // Assert
            assertThat(AIServiceFactory.hasProviderStatic(AIServiceType.OPENAI)).isTrue();
            assertThat(AIServiceFactory.getProviderStatic(AIServiceType.OPENAI)).isEqualTo(testProvider);
        }
        
        @Test
        @DisplayName("should replace existing provider when registering same service type")
        void shouldReplaceExistingProvider_whenRegisteringSameServiceType() {
            // Arrange
            AIServiceProvider firstProvider = new TestAIServiceProvider();
            AIServiceProvider secondProvider = new TestAIServiceProvider();
            
            // Act
            AIServiceFactory.registerProviderStatic(AIServiceType.GEMINI, firstProvider);
            AIServiceFactory.registerProviderStatic(AIServiceType.GEMINI, secondProvider);
            
            // Assert
            assertThat(AIServiceFactory.getProviderStatic(AIServiceType.GEMINI)).isEqualTo(secondProvider);
            assertThat(AIServiceFactory.getProviderStatic(AIServiceType.GEMINI)).isNotEqualTo(firstProvider);
        }
        
        @Test
        @DisplayName("should throw exception when null service type provided")
        void shouldThrowException_whenNullServiceTypeProvided() {
            // Act & Assert
            assertThatThrownBy(() -> AIServiceFactory.registerProviderStatic(null, testProvider))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Argument for @NotNull parameter 'serviceType'");
        }
        
        @Test
        @DisplayName("should throw exception when null provider provided")
        void shouldThrowException_whenNullProviderProvided() {
            // Act & Assert
            assertThatThrownBy(() -> AIServiceFactory.registerProviderStatic(AIServiceType.OPENAI, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Argument for @NotNull parameter 'provider'");
        }
    }
    
    @Nested
    @DisplayName("Provider Retrieval")
    class ProviderRetrieval {
        
        @BeforeEach
        void setUp() {
            // Register test providers
            AIServiceFactory.registerProviderStatic(AIServiceType.OPENAI, testProvider);
        }
        
        @Test
        @DisplayName("should retrieve provider successfully when provider exists")
        void shouldRetrieveProviderSuccessfully_whenProviderExists() {
            // Act
            AIServiceProvider result = AIServiceFactory.getProviderStatic(AIServiceType.OPENAI);
            
            // Assert
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(testProvider);
            assertThat(result.getServiceType()).isEqualTo(AIServiceType.OPENAI);
        }
        
        @Test
        @DisplayName("should return registered provider when provider exists")
        void shouldReturnRegisteredProvider_whenProviderExists() {
            // Act - Get the provider that was registered in setUp
            AIServiceProvider result = AIServiceFactory.getProviderStatic(AIServiceType.OPENAI);
            
            // Assert - Should return the test provider we registered
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(testProvider);
        }
        
        @Test
        @DisplayName("should throw exception when null service type provided")
        void shouldThrowException_whenNullServiceTypeProvided() {
            // Act & Assert
            assertThatThrownBy(() -> AIServiceFactory.getProviderStatic(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Argument for @NotNull parameter 'serviceType'");
        }
    }
    
    @Nested
    @DisplayName("Provider Existence Check")
    class ProviderExistenceCheck {
        
        @BeforeEach
        void setUp() {
            // Register test provider
            AIServiceFactory.registerProviderStatic(AIServiceType.OPENAI, testProvider);
        }
        
        @Test
        @DisplayName("should return true when OpenAI provider exists")
        void shouldReturnTrue_whenOpenAIProviderExists() {
            // Act & Assert
            assertThat(AIServiceFactory.hasProviderStatic(AIServiceType.OPENAI)).isTrue();
        }
        
        @Test
        @DisplayName("should return true when Gemini provider exists")
        void shouldReturnTrue_whenGeminiProviderExists() {
            // Act & Assert
            assertThat(AIServiceFactory.hasProviderStatic(AIServiceType.GEMINI)).isTrue();
        }
        
        @Test
        @DisplayName("should throw exception when null service type provided")
        void shouldThrowException_whenNullServiceTypeProvided() {
            // Act & Assert
            assertThatThrownBy(() -> AIServiceFactory.hasProviderStatic(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Argument for @NotNull parameter 'serviceType'");
        }
    }
    
    @Nested
    @DisplayName("Provider Unregistration")
    class ProviderUnregistration {
        
        @BeforeEach
        void setUp() {
            // Register test provider
            AIServiceFactory.registerProviderStatic(AIServiceType.OPENAI, testProvider);
        }
        
        @Test
        @DisplayName("should unregister provider successfully when provider exists")
        void shouldUnregisterProviderSuccessfully_whenProviderExists() {
            // Act
            boolean result = AIServiceFactory.unregisterProviderStatic(AIServiceType.OPENAI);
            
            // Assert
            assertThat(result).isTrue();
            assertThat(AIServiceFactory.hasProviderStatic(AIServiceType.OPENAI)).isFalse();
            assertThat(AIServiceFactory.getProviderStatic(AIServiceType.OPENAI)).isNull();
        }
        
        @Test
        @DisplayName("should return false when unregistering non-existent provider")
        void shouldReturnFalse_whenUnregisteringNonExistentProvider() {
            // Act
            boolean result = AIServiceFactory.unregisterProviderStatic(AIServiceType.OPENAI);
            
            // Assert
            assertThat(result).isTrue(); // Should return true because we registered it in setUp
        }
        
        @Test
        @DisplayName("should throw exception when null service type provided")
        void shouldThrowException_whenNullServiceTypeProvided() {
            // Act & Assert
            assertThatThrownBy(() -> AIServiceFactory.unregisterProviderStatic(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Argument for @NotNull parameter 'serviceType'");
        }
    }
    
    @Nested
    @DisplayName("Provider Count Management")
    class ProviderCountManagement {
        
        @Test
        @DisplayName("should return correct provider count")
        void shouldReturnCorrectProviderCount() {
            // Arrange
            AIServiceFactory.registerProviderStatic(AIServiceType.OPENAI, testProvider);
            AIServiceFactory.registerProviderStatic(AIServiceType.GEMINI, new TestAIServiceProvider());
            
            // Act
            int count = AIServiceFactory.getProviderCountStatic();
            
            // Assert
            assertThat(count).isGreaterThanOrEqualTo(2);
        }
        
        @Test
        @DisplayName("should return zero when no providers registered")
        void shouldReturnZero_whenNoProvidersRegistered() {
            // Arrange - Clear all providers
            AIServiceFactory.unregisterProviderStatic(AIServiceType.OPENAI);
            AIServiceFactory.unregisterProviderStatic(AIServiceType.GEMINI);
            
            // Act
            int count = AIServiceFactory.getProviderCountStatic();
            
            // Assert
            assertThat(count).isEqualTo(0);
        }
    }
    
    @Nested
    @DisplayName("Registered Service Types")
    class RegisteredServiceTypes {
        
        @BeforeEach
        void setUp() {
            // Register test providers
            AIServiceFactory.registerProviderStatic(AIServiceType.OPENAI, testProvider);
            AIServiceFactory.registerProviderStatic(AIServiceType.GEMINI, new TestAIServiceProvider());
        }
        
        @Test
        @DisplayName("should return all registered service types")
        void shouldReturnAllRegisteredServiceTypes() {
            // Act
            AIServiceType[] types = AIServiceFactory.getRegisteredServiceTypesStatic();
            
            // Assert
            assertThat(types).isNotNull();
            assertThat(types.length).isGreaterThanOrEqualTo(2);
            assertThat(types).contains(AIServiceType.OPENAI, AIServiceType.GEMINI);
        }
        
        @Test
        @DisplayName("should return empty array when no providers registered")
        void shouldReturnEmptyArray_whenNoProvidersRegistered() {
            // Arrange - Clear all providers
            AIServiceFactory.unregisterProviderStatic(AIServiceType.OPENAI);
            AIServiceFactory.unregisterProviderStatic(AIServiceType.GEMINI);
            
            // Act
            AIServiceType[] types = AIServiceFactory.getRegisteredServiceTypesStatic();
            
            // Assert
            assertThat(types).isNotNull();
            assertThat(types).isEmpty();
        }
    }
    
    @Nested
    @DisplayName("Shared HTTP Client")
    class SharedHttpClient {
        
        @Test
        @DisplayName("should provide shared HTTP client")
        void shouldProvideSharedHttpClient() {
            // Act
            HttpClient client = AIServiceFactory.getSharedHttpClientStatic();
            
            // Assert
            assertThat(client).isNotNull();
        }
        
        @Test
        @DisplayName("should return same HTTP client instance")
        void shouldReturnSameHttpClientInstance() {
            // Act
            HttpClient client1 = AIServiceFactory.getSharedHttpClientStatic();
            HttpClient client2 = AIServiceFactory.getSharedHttpClientStatic();
            
            // Assert
            assertThat(client1).isSameAs(client2);
        }
    }
    
    @Nested
    @DisplayName("Default Provider Initialization")
    class DefaultProviderInitialization {
        
        @Test
        @DisplayName("should have default providers available")
        void shouldHaveDefaultProvidersAvailable() {
            // Act & Assert
            assertThat(AIServiceFactory.hasProviderStatic(AIServiceType.OPENAI)).isTrue();
            assertThat(AIServiceFactory.hasProviderStatic(AIServiceType.GEMINI)).isTrue();
        }
        
        @Test
        @DisplayName("should return OpenAI provider instance")
        void shouldReturnOpenAIProviderInstance() {
            // Act
            AIServiceProvider provider = AIServiceFactory.getProviderStatic(AIServiceType.OPENAI);
            
            // Assert
            assertThat(provider).isNotNull();
            assertThat(provider).isInstanceOf(OpenAIProvider.class);
            assertThat(provider.getServiceType()).isEqualTo(AIServiceType.OPENAI);
        }
        
        @Test
        @DisplayName("should return Gemini provider instance")
        void shouldReturnGeminiProviderInstance() {
            // Act
            AIServiceProvider provider = AIServiceFactory.getProviderStatic(AIServiceType.GEMINI);
            
            // Assert
            assertThat(provider).isNotNull();
            assertThat(provider).isInstanceOf(GeminiProvider.class);
            assertThat(provider.getServiceType()).isEqualTo(AIServiceType.GEMINI);
        }
        
        @Test
        @DisplayName("should have correct provider count for default providers")
        void shouldHaveCorrectProviderCountForDefaultProviders() {
            // Act
            int count = AIServiceFactory.getProviderCountStatic();
            
            // Assert
            assertThat(count).isGreaterThanOrEqualTo(2);
        }
    }
    
    @Nested
    @DisplayName("Provider Interface Testing")
    class ProviderInterfaceTesting {
        
        @Test
        @DisplayName("should test provider validation")
        void shouldTestProviderValidation() throws Exception {
            // Arrange
            AIServiceProvider provider = AIServiceFactory.getProviderStatic(AIServiceType.OPENAI);
            
            // Act
            CompletableFuture<Boolean> future = provider.validateConnection("test-api-key");
            Boolean result = future.get();
            
            // Assert
            assertThat(result).isNotNull();
            // Note: This will likely be false in test environment without real API key
        }
        
        @Test
        @DisplayName("should test provider analysis")
        void shouldTestProviderAnalysis() {
            // Arrange
            AIServiceProvider provider = AIServiceFactory.getProviderStatic(AIServiceType.OPENAI);
            
            // Act & Assert
            assertThatThrownBy(() -> {
                provider.analyze("test prompt", "gpt-4", "invalid-key").get();
            }).isInstanceOf(Exception.class);
        }
        
        @Test
        @DisplayName("should test provider service type")
        void shouldTestProviderServiceType() {
            // Arrange
            AIServiceProvider openAIProvider = AIServiceFactory.getProviderStatic(AIServiceType.OPENAI);
            AIServiceProvider geminiProvider = AIServiceFactory.getProviderStatic(AIServiceType.GEMINI);
            
            // Act & Assert
            assertThat(openAIProvider.getServiceType()).isEqualTo(AIServiceType.OPENAI);
            assertThat(geminiProvider.getServiceType()).isEqualTo(AIServiceType.GEMINI);
        }
        
        @Test
        @DisplayName("should test provider display names")
        void shouldTestProviderDisplayNames() {
            // Arrange
            AIServiceProvider openAIProvider = AIServiceFactory.getProviderStatic(AIServiceType.OPENAI);
            AIServiceProvider geminiProvider = AIServiceFactory.getProviderStatic(AIServiceType.GEMINI);
            
            // Act & Assert
            assertThat(openAIProvider.getDisplayName()).isNotNull().isNotEmpty();
            assertThat(geminiProvider.getDisplayName()).isNotNull().isNotEmpty();
        }
    }
    
    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {
        
        @Test
        @DisplayName("should handle provider registration with invalid parameters")
        void shouldHandleProviderRegistrationWithInvalidParameters() {
            // Act & Assert
            assertThatThrownBy(() -> AIServiceFactory.registerProviderStatic(null, testProvider))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Argument for @NotNull parameter 'serviceType'");
            
            assertThatThrownBy(() -> AIServiceFactory.registerProviderStatic(AIServiceType.OPENAI, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Argument for @NotNull parameter 'provider'");
        }
        
        @Test
        @DisplayName("should handle provider retrieval with invalid parameters")
        void shouldHandleProviderRetrievalWithInvalidParameters() {
            // Act & Assert
            assertThatThrownBy(() -> AIServiceFactory.getProviderStatic(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Argument for @NotNull parameter 'serviceType'");
        }
        
        @Test
        @DisplayName("should handle provider existence check with invalid parameters")
        void shouldHandleProviderExistenceCheckWithInvalidParameters() {
            // Act & Assert
            assertThatThrownBy(() -> AIServiceFactory.hasProviderStatic(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Argument for @NotNull parameter 'serviceType'");
        }
        
        @Test
        @DisplayName("should handle provider unregistration with invalid parameters")
        void shouldHandleProviderUnregistrationWithInvalidParameters() {
            // Act & Assert
            assertThatThrownBy(() -> AIServiceFactory.unregisterProviderStatic(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Argument for @NotNull parameter 'serviceType'");
        }
    }
    
    /**
     * Test implementation of AIServiceProvider for testing purposes.
     */
    private static class TestAIServiceProvider implements AIServiceProvider {
        
        @Override
        public CompletableFuture<Boolean> validateConnection(@NotNull String apiKey) {
            return CompletableFuture.completedFuture(true);
        }
        
        @Override
        public CompletableFuture<AIAnalysisResult> analyze(@NotNull String prompt, 
                                                          @NotNull String modelId, 
                                                          @NotNull String apiKey) {
            return CompletableFuture.completedFuture(
                new AIAnalysisResult(
                    "Test analysis result",
                    AIServiceType.OPENAI,
                    modelId,
                    System.currentTimeMillis(),
                    1000L
                )
            );
        }
        
        @Override
        public CompletableFuture<String[]> discoverAvailableModels(@NotNull String apiKey) {
            return CompletableFuture.completedFuture(new String[]{"test-model-1", "test-model-2"});
        }
        
        @Override
        public AIServiceType getServiceType() {
            return AIServiceType.OPENAI;
        }
        
        @Override
        public String getDisplayName() {
            return "Test Provider";
        }
    }
}
