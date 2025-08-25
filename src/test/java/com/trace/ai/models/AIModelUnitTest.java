package com.trace.ai.models;

import com.trace.ai.configuration.AIServiceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AI Model Unit Tests")
class AIModelUnitTest {
    
    private AIModel openAIModel;
    private AIModel geminiModel;
    private AIModel customModel;
    
    @BeforeEach
    void setUp() {
        openAIModel = new AIModel("GPT-4o", AIServiceType.OPENAI, "gpt-4o");
        geminiModel = new AIModel("Gemini Pro", AIServiceType.GEMINI, "gemini-1.5-pro");
        customModel = new AIModel("Custom Model", AIServiceType.OPENAI, "custom-model");
    }
    
    @Nested
    @DisplayName("Model Creation")
    class ModelCreation {
        
        @Test
        @DisplayName("should create model with valid parameters")
        void shouldCreateModel_whenValidParametersProvided() {
            // Act
            AIModel model = new AIModel("Test Model", AIServiceType.OPENAI, "gpt-4");
            
            // Assert
            assertThat(model.getName()).isEqualTo("Test Model");
            assertThat(model.getServiceType()).isEqualTo(AIServiceType.OPENAI);
            assertThat(model.getModelId()).isEqualTo("gpt-4");
            assertThat(model.isEnabled()).isTrue();
            assertThat(model.getNotes()).isEmpty();
            assertThat(model.getId()).isNotNull().isNotEmpty();
            assertThat(model.getCreatedAt()).isPositive();
            assertThat(model.getLastModified()).isEqualTo(model.getCreatedAt());
        }
        
        @Test
        @DisplayName("should create model from existing data")
        void shouldCreateModel_whenExistingDataProvided() {
            // Arrange
            String id = "test-id-123";
            String name = "Existing Model";
            AIServiceType serviceType = AIServiceType.GEMINI;
            String modelId = "gemini-1.5-flash";
            boolean enabled = false;
            String notes = "Test notes";
            long createdAt = 1234567890L;
            long lastModified = 1234567899L;
            
            // Act
            AIModel model = new AIModel(id, name, serviceType, modelId, enabled, notes, createdAt, lastModified);
            
            // Assert
            assertThat(model.getId()).isEqualTo(id);
            assertThat(model.getName()).isEqualTo(name);
            assertThat(model.getServiceType()).isEqualTo(serviceType);
            assertThat(model.getModelId()).isEqualTo(modelId);
            assertThat(model.isEnabled()).isFalse();
            assertThat(model.getNotes()).isEqualTo(notes);
            assertThat(model.getCreatedAt()).isEqualTo(createdAt);
            assertThat(model.getLastModified()).isEqualTo(lastModified);
        }
        
        @Test
        @DisplayName("should throw exception when name is null")
        void shouldThrowException_whenNameIsNull() {
            // Act & Assert - @NotNull annotations are enforced by IntelliJ Platform
            assertThatThrownBy(() -> new AIModel(null, AIServiceType.OPENAI, "gpt-4"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@NotNull parameter 'name'");
        }
        
        @Test
        @DisplayName("should throw exception when service type is null")
        void shouldThrowException_whenServiceTypeIsNull() {
            // Act & Assert - @NotNull annotations are enforced by IntelliJ Platform
            assertThatThrownBy(() -> new AIModel("Test Model", null, "gpt-4"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@NotNull parameter 'serviceType'");
        }
        
        @Test
        @DisplayName("should throw exception when model ID is null")
        void shouldThrowException_whenModelIdIsNull() {
            // Act & Assert - @NotNull annotations are enforced by IntelliJ Platform
            assertThatThrownBy(() -> new AIModel("Test Model", AIServiceType.OPENAI, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@NotNull parameter 'modelId'");
        }
    }
    
    @Nested
    @DisplayName("Model Properties")
    class ModelProperties {
        
        @Test
        @DisplayName("should set and get model properties correctly")
        void shouldSetAndGetModelPropertiesCorrectly() {
            // Arrange
            String newName = "Updated Model Name";
            String newNotes = "Updated notes";
            boolean newEnabled = false;
            
            // Act
            openAIModel.setName(newName);
            openAIModel.setNotes(newNotes);
            openAIModel.setEnabled(newEnabled);
            
            // Assert
            assertThat(openAIModel.getName()).isEqualTo(newName);
            assertThat(openAIModel.getNotes()).isEqualTo(newNotes);
            assertThat(openAIModel.isEnabled()).isEqualTo(newEnabled);
            assertThat(openAIModel.getLastModified()).isGreaterThanOrEqualTo(openAIModel.getCreatedAt());
        }
        
        @Test
        @DisplayName("should handle property validation")
        void shouldHandlePropertyValidation() {
            // Act & Assert
            assertThatThrownBy(() -> openAIModel.setName(null))
                .isInstanceOf(IllegalArgumentException.class);
            
            assertThatThrownBy(() -> openAIModel.setNotes(null))
                .isInstanceOf(IllegalArgumentException.class);
        }
        
        @Test
        @DisplayName("should update last modified timestamp when properties change")
        void shouldUpdateLastModifiedTimestamp_whenPropertiesChange() {
            // Arrange
            long originalLastModified = openAIModel.getLastModified();
            
            // Act
            openAIModel.setName("New Name");
            
            // Assert
            assertThat(openAIModel.getLastModified()).isGreaterThanOrEqualTo(originalLastModified);
        }
    }
    
    @Nested
    @DisplayName("Model Validation")
    class ModelValidation {
        
        @Test
        @DisplayName("should validate model ID correctly")
        void shouldValidateModelIdCorrectly() {
            // Assert
            assertThat(openAIModel.getId()).isNotNull().isNotEmpty();
            assertThat(openAIModel.getId()).matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}");
        }
        
        @Test
        @DisplayName("should validate model name correctly")
        void shouldValidateModelNameCorrectly() {
            // Assert
            assertThat(openAIModel.getName()).isNotNull().isNotEmpty();
            assertThat(openAIModel.getName()).isEqualTo("GPT-4o");
        }
        
        @Test
        @DisplayName("should validate model configuration")
        void shouldValidateModelConfiguration() {
            // Assert
            assertThat(openAIModel.isValid()).isTrue();
            assertThat(openAIModel.getServiceType()).isNotNull();
            assertThat(openAIModel.getModelId()).isNotNull().isNotEmpty();
        }
        
        @Test
        @DisplayName("should accept empty string parameters")
        void shouldAcceptEmptyStringParameters() {
            // Act & Assert - Empty strings are valid input
            AIModel model = new AIModel("", AIServiceType.OPENAI, "gpt-4");
            assertThat(model).isNotNull();
            assertThat(model.getName()).isEmpty();
        }
    }
    
    @Nested
    @DisplayName("Optimal Settings")
    class OptimalSettings {
        
        @Test
        @DisplayName("should get optimal max tokens for OpenAI GPT-4")
        void shouldGetOptimalMaxTokens_whenOpenAIGPT4() {
            // Act
            int maxTokens = openAIModel.getMaxTokens();
            
            // Assert
            assertThat(maxTokens).isEqualTo(4000);
        }
        
        @Test
        @DisplayName("should get optimal max tokens for OpenAI GPT-3.5")
        void shouldGetOptimalMaxTokens_whenOpenAIGPT35() {
            // Arrange
            AIModel gpt35Model = new AIModel("GPT-3.5", AIServiceType.OPENAI, "gpt-3.5-turbo");
            
            // Act
            int maxTokens = gpt35Model.getMaxTokens();
            
            // Assert
            assertThat(maxTokens).isEqualTo(2000);
        }
        
        @Test
        @DisplayName("should get optimal max tokens for Gemini Pro")
        void shouldGetOptimalMaxTokens_whenGeminiPro() {
            // Act
            int maxTokens = geminiModel.getMaxTokens();
            
            // Assert
            assertThat(maxTokens).isEqualTo(4000);
        }
        
        @Test
        @DisplayName("should get optimal max tokens for Gemini Flash")
        void shouldGetOptimalMaxTokens_whenGeminiFlash() {
            // Arrange
            AIModel flashModel = new AIModel("Gemini Flash", AIServiceType.GEMINI, "gemini-1.5-flash");
            
            // Act
            int maxTokens = flashModel.getMaxTokens();
            
            // Assert
            assertThat(maxTokens).isEqualTo(2000);
        }
        
        @Test
        @DisplayName("should get optimal temperature for all models")
        void shouldGetOptimalTemperature_forAllModels() {
            // Act & Assert
            assertThat(openAIModel.getTemperature()).isEqualTo(0.3);
            assertThat(geminiModel.getTemperature()).isEqualTo(0.3);
            assertThat(customModel.getTemperature()).isEqualTo(0.3);
        }
        
        @Test
        @DisplayName("should include confidence scores for all models")
        void shouldIncludeConfidenceScores_forAllModels() {
            // Act & Assert
            assertThat(openAIModel.isIncludeConfidenceScores()).isTrue();
            assertThat(geminiModel.isIncludeConfidenceScores()).isTrue();
            assertThat(customModel.isIncludeConfidenceScores()).isTrue();
        }
    }
    
    @Nested
    @DisplayName("Display Methods")
    class DisplayMethods {
        
        @Test
        @DisplayName("should get display name correctly")
        void shouldGetDisplayNameCorrectly() {
            // Act & Assert
            assertThat(openAIModel.getDisplayName()).isEqualTo("GPT-4o");
            assertThat(geminiModel.getDisplayName()).isEqualTo("Gemini Pro");
        }
        
        @Test
        @DisplayName("should get full display name correctly")
        void shouldGetFullDisplayNameCorrectly() {
            // Act & Assert
            assertThat(openAIModel.getFullDisplayName()).isEqualTo("GPT-4o (OpenAI - gpt-4o)");
            assertThat(geminiModel.getFullDisplayName()).isEqualTo("Gemini Pro (Google Gemini - gemini-1.5-pro)");
        }
    }
    
    @Nested
    @DisplayName("Utility Methods")
    class UtilityMethods {
        
        @Test
        @DisplayName("should copy model with new name")
        void shouldCopyModelWithNewName() {
            // Act
            AIModel copy = openAIModel.copy("Copied GPT-4o");
            
            // Assert
            assertThat(copy.getName()).isEqualTo("Copied GPT-4o");
            assertThat(copy.getServiceType()).isEqualTo(openAIModel.getServiceType());
            assertThat(copy.getModelId()).isEqualTo(openAIModel.getModelId());
            assertThat(copy.getId()).isNotEqualTo(openAIModel.getId());
            // Note: CreatedAt might be the same if created in the same millisecond
            assertThat(copy.getCreatedAt()).isGreaterThanOrEqualTo(openAIModel.getCreatedAt());
        }
        
        @Test
        @DisplayName("should validate model correctly")
        void shouldValidateModelCorrectly() {
            // Assert
            assertThat(openAIModel.isValid()).isTrue();
            assertThat(geminiModel.isValid()).isTrue();
            assertThat(customModel.isValid()).isTrue();
        }
    }
    
    @Nested
    @DisplayName("Static Utility Methods")
    class StaticUtilityMethods {
        
        @Test
        @DisplayName("should get model display name for known models")
        void shouldGetModelDisplayName_forKnownModels() {
            // Act & Assert
            assertThat(AIModel.getModelDisplayName("gpt-4o")).isEqualTo("GPT-4o (Latest)");
            assertThat(AIModel.getModelDisplayName("gpt-3.5-turbo")).isEqualTo("GPT-3.5 Turbo");
            assertThat(AIModel.getModelDisplayName("gemini-1.5-pro")).isEqualTo("Gemini 1.5 Pro");
            assertThat(AIModel.getModelDisplayName("gemini-1.5-flash")).isEqualTo("Gemini 1.5 Flash");
        }
        
        @Test
        @DisplayName("should return model ID for unknown models")
        void shouldReturnModelId_forUnknownModels() {
            // Act & Assert
            assertThat(AIModel.getModelDisplayName("unknown-model")).isEqualTo("unknown-model");
        }
        
        @Test
        @DisplayName("should get model use case for different services")
        void shouldGetModelUseCase_forDifferentServices() {
            // Act & Assert
            assertThat(AIModel.getModelUseCase(AIServiceType.OPENAI, "gpt-4"))
                .isEqualTo("Complex analysis, reasoning, and detailed responses");
            assertThat(AIModel.getModelUseCase(AIServiceType.OPENAI, "gpt-3.5-turbo"))
                .isEqualTo("Fast responses, general tasks, and quick analysis");
            assertThat(AIModel.getModelUseCase(AIServiceType.GEMINI, "gemini-1.5-pro"))
                .isEqualTo("Advanced reasoning, complex tasks, and detailed analysis");
            assertThat(AIModel.getModelUseCase(AIServiceType.GEMINI, "gemini-1.5-flash"))
                .isEqualTo("Quick responses, general tasks, and fast processing");
        }
    }
    
    @Nested
    @DisplayName("Object Methods")
    class ObjectMethods {
        
        @Test
        @DisplayName("should implement equals correctly")
        void shouldImplementEqualsCorrectly() {
            // Arrange
            AIModel sameModel = new AIModel(openAIModel.getId(), "Different Name", 
                AIServiceType.OPENAI, "different-model", false, "different notes", 
                openAIModel.getCreatedAt(), openAIModel.getLastModified());
            
            // Act & Assert
            assertThat(openAIModel).isEqualTo(sameModel);
            assertThat(openAIModel).isNotEqualTo(geminiModel);
            assertThat(openAIModel).isNotEqualTo(null);
            assertThat(openAIModel).isNotEqualTo("string");
        }
        
        @Test
        @DisplayName("should implement hashCode correctly")
        void shouldImplementHashCodeCorrectly() {
            // Arrange
            AIModel sameModel = new AIModel(openAIModel.getId(), "Different Name", 
                AIServiceType.OPENAI, "different-model", false, "different notes", 
                openAIModel.getCreatedAt(), openAIModel.getLastModified());
            
            // Act & Assert
            assertThat(openAIModel.hashCode()).isEqualTo(sameModel.hashCode());
        }
        
        @Test
        @DisplayName("should implement toString correctly")
        void shouldImplementToStringCorrectly() {
            // Act
            String toString = openAIModel.toString();
            
            // Assert
            assertThat(toString).contains("AIModel{");
            assertThat(toString).contains("id='" + openAIModel.getId() + "'");
            assertThat(toString).contains("name='GPT-4o'");
            assertThat(toString).contains("serviceType=" + openAIModel.getServiceType());
            assertThat(toString).contains("modelId='gpt-4o'");
            assertThat(toString).contains("enabled=true");
            assertThat(toString).contains("createdAt=" + openAIModel.getCreatedAt());
        }
    }
}
