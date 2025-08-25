package com.trace.ai.services;

import com.trace.ai.configuration.AIServiceType;
import com.trace.ai.models.AIModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("AI Model Service Unit Tests")
class AIModelServiceUnitTest {
    
    private AIModel testOpenAIModel;
    private AIModel testGeminiModel;
    
    @BeforeEach
    void setUp() {
        // Create test models for validation
        testOpenAIModel = new AIModel("GPT-4 Test", AIServiceType.OPENAI, "gpt-4");
        testGeminiModel = new AIModel("Gemini Pro Test", AIServiceType.GEMINI, "gemini-1.5-pro");
    }
    
    @Nested
    @DisplayName("AIModel Creation and Validation")
    class AIModelCreationAndValidation {
        
        @Test
        @DisplayName("should create model successfully when valid data provided")
        void shouldCreateModelSuccessfully_whenValidDataProvided() {
            // Act
            AIModel result = new AIModel("Test Model", AIServiceType.OPENAI, "gpt-3.5-turbo");
            
            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Test Model");
            assertThat(result.getServiceType()).isEqualTo(AIServiceType.OPENAI);
            assertThat(result.getModelId()).isEqualTo("gpt-3.5-turbo");
            assertThat(result.isEnabled()).isTrue();
            assertThat(result.getId()).isNotNull().isNotEmpty();
            assertThat(result.getCreatedAt()).isPositive();
            assertThat(result.getLastModified()).isEqualTo(result.getCreatedAt());
        }
        
        @Test
        @DisplayName("should create model with full constructor")
        void shouldCreateModelWithFullConstructor() {
            // Arrange
            String id = "test-id-123";
            String name = "Test Model";
            AIServiceType serviceType = AIServiceType.GEMINI;
            String modelId = "gemini-1.5-pro";
            boolean enabled = false;
            String notes = "Test notes";
            long createdAt = System.currentTimeMillis();
            long lastModified = createdAt + 1000;
            
            // Act
            AIModel result = new AIModel(id, name, serviceType, modelId, enabled, notes, createdAt, lastModified);
            
            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(id);
            assertThat(result.getName()).isEqualTo(name);
            assertThat(result.getServiceType()).isEqualTo(serviceType);
            assertThat(result.getModelId()).isEqualTo(modelId);
            assertThat(result.isEnabled()).isEqualTo(enabled);
            assertThat(result.getNotes()).isEqualTo(notes);
            assertThat(result.getCreatedAt()).isEqualTo(createdAt);
            assertThat(result.getLastModified()).isEqualTo(lastModified);
        }
        
        @Test
        @DisplayName("should update model properties correctly")
        void shouldUpdateModelPropertiesCorrectly() {
            // Arrange
            AIModel model = new AIModel("Original Name", AIServiceType.OPENAI, "gpt-4");
            long originalLastModified = model.getLastModified();
            
            // Act
            model.setName("Updated Name");
            model.setEnabled(false);
            model.setNotes("Updated notes");
            
            // Assert
            assertThat(model.getName()).isEqualTo("Updated Name");
            assertThat(model.isEnabled()).isFalse();
            assertThat(model.getNotes()).isEqualTo("Updated notes");
            assertThat(model.getLastModified()).isGreaterThanOrEqualTo(originalLastModified);
        }
        
        @Test
        @DisplayName("should validate model correctly")
        void shouldValidateModelCorrectly() {
            // Arrange
            AIModel validModel = new AIModel("Valid Model", AIServiceType.OPENAI, "gpt-4");
            
            // Act & Assert
            assertThat(validModel.isValid()).isTrue();
            assertThat(validModel.getFullDisplayName()).contains("Valid Model");
            assertThat(validModel.getFullDisplayName()).contains("gpt-4");
        }
    }
    
    @Nested
    @DisplayName("AIModel Service Type Operations")
    class AIModelServiceTypeOperations {
        
        @Test
        @DisplayName("should handle different service types correctly")
        void shouldHandleDifferentServiceTypesCorrectly() {
            // Arrange
            AIModel openAIModel = new AIModel("OpenAI Model", AIServiceType.OPENAI, "gpt-4");
            AIModel geminiModel = new AIModel("Gemini Model", AIServiceType.GEMINI, "gemini-1.5-pro");
            
            // Act & Assert
            assertThat(openAIModel.getServiceType()).isEqualTo(AIServiceType.OPENAI);
            assertThat(geminiModel.getServiceType()).isEqualTo(AIServiceType.GEMINI);
            assertThat(openAIModel.getServiceType()).isNotEqualTo(geminiModel.getServiceType());
        }
        
        @Test
        @DisplayName("should display service type correctly")
        void shouldDisplayServiceTypeCorrectly() {
            // Arrange
            AIModel model = new AIModel("Test Model", AIServiceType.OPENAI, "gpt-4");
            
            // Act & Assert
            assertThat(model.getFullDisplayName()).contains("Test Model");
            assertThat(model.getFullDisplayName()).contains("gpt-4");
            assertThat(model.getDisplayName()).contains("Test Model");
        }
        
        @Test
        @DisplayName("should handle model ID variations")
        void shouldHandleModelIdVariations() {
            // Arrange
            AIModel gpt4Model = new AIModel("GPT-4", AIServiceType.OPENAI, "gpt-4");
            AIModel gpt35Model = new AIModel("GPT-3.5", AIServiceType.OPENAI, "gpt-3.5-turbo");
            AIModel geminiProModel = new AIModel("Gemini Pro", AIServiceType.GEMINI, "gemini-1.5-pro");
            AIModel geminiFlashModel = new AIModel("Gemini Flash", AIServiceType.GEMINI, "gemini-1.5-flash");
            
            // Act & Assert
            assertThat(gpt4Model.getModelId()).isEqualTo("gpt-4");
            assertThat(gpt35Model.getModelId()).isEqualTo("gpt-3.5-turbo");
            assertThat(geminiProModel.getModelId()).isEqualTo("gemini-1.5-pro");
            assertThat(geminiFlashModel.getModelId()).isEqualTo("gemini-1.5-flash");
        }
    }
    
    @Nested
    @DisplayName("AIModel Object Methods")
    class AIModelObjectMethods {
        
        @Test
        @DisplayName("should implement equals correctly")
        void shouldImplementEqualsCorrectly() {
            // Arrange
            AIModel model1 = new AIModel("Test Model", AIServiceType.OPENAI, "gpt-4");
            AIModel model2 = new AIModel("Test Model", AIServiceType.OPENAI, "gpt-4");
            AIModel model3 = new AIModel("Different Model", AIServiceType.GEMINI, "gemini-1.5-pro");
            
            // Act & Assert
            assertThat(model1).isEqualTo(model1); // Reflexive
            assertThat(model1).isNotEqualTo(model2); // Different IDs
            assertThat(model1).isNotEqualTo(model3); // Different properties
            assertThat(model1).isNotEqualTo(null); // Not equal to null
            assertThat(model1).isNotEqualTo("string"); // Not equal to different type
        }
        
        @Test
        @DisplayName("should implement hashCode correctly")
        void shouldImplementHashCodeCorrectly() {
            // Arrange
            AIModel model1 = new AIModel("Test Model", AIServiceType.OPENAI, "gpt-4");
            AIModel model2 = new AIModel("Test Model", AIServiceType.OPENAI, "gpt-4");
            
            // Act & Assert
            assertThat(model1.hashCode()).isEqualTo(model1.hashCode()); // Same object
            // Different objects should have different hash codes (due to different IDs)
            assertThat(model1.hashCode()).isNotEqualTo(model2.hashCode());
        }
        
        @Test
        @DisplayName("should implement toString correctly")
        void shouldImplementToStringCorrectly() {
            // Arrange
            AIModel model = new AIModel("Test Model", AIServiceType.OPENAI, "gpt-4");
            
            // Act
            String toString = model.toString();
            
            // Assert
            assertThat(toString).contains("Test Model");
            assertThat(toString).contains("gpt-4");
            assertThat(toString).contains("OpenAI"); // AIServiceType.toString() returns "OpenAI", not "OPENAI"
            assertThat(toString).contains(model.getId());
        }
    }
    
    @Nested
    @DisplayName("AIModel Edge Cases")
    class AIModelEdgeCases {
        
        @Test
        @DisplayName("should handle empty notes correctly")
        void shouldHandleEmptyNotesCorrectly() {
            // Arrange
            AIModel model = new AIModel("Test Model", AIServiceType.OPENAI, "gpt-4");
            
            // Act
            model.setNotes("");
            
            // Assert
            assertThat(model.getNotes()).isEmpty();
            assertThat(model.isValid()).isTrue();
        }
        
        @Test
        @DisplayName("should handle long notes correctly")
        void shouldHandleLongNotesCorrectly() {
            // Arrange
            AIModel model = new AIModel("Test Model", AIServiceType.OPENAI, "gpt-4");
            String longNotes = "This is a very long note that contains many characters and should be handled properly by the AIModel class without any issues or truncation";
            
            // Act
            model.setNotes(longNotes);
            
            // Assert
            assertThat(model.getNotes()).isEqualTo(longNotes);
            assertThat(model.getNotes().length()).isEqualTo(longNotes.length());
        }
        
        @Test
        @DisplayName("should handle special characters in name")
        void shouldHandleSpecialCharactersInName() {
            // Arrange
            String nameWithSpecialChars = "Test Model @#$%^&*()_+-=[]{}|;':\",./<>?";
            
            // Act
            AIModel model = new AIModel(nameWithSpecialChars, AIServiceType.OPENAI, "gpt-4");
            
            // Assert
            assertThat(model.getName()).isEqualTo(nameWithSpecialChars);
            assertThat(model.getDisplayName()).contains(nameWithSpecialChars);
        }
        
        @Test
        @DisplayName("should handle model state changes correctly")
        void shouldHandleModelStateChangesCorrectly() {
            // Arrange
            AIModel model = new AIModel("Test Model", AIServiceType.OPENAI, "gpt-4");
            long originalLastModified = model.getLastModified();
            
            // Act - Multiple changes
            model.setEnabled(false);
            model.setName("Updated Name");
            model.setNotes("Updated notes");
            
            // Assert
            assertThat(model.isEnabled()).isFalse();
            assertThat(model.getName()).isEqualTo("Updated Name");
            assertThat(model.getNotes()).isEqualTo("Updated notes");
            assertThat(model.getLastModified()).isGreaterThanOrEqualTo(originalLastModified);
        }
    }
}
