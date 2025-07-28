package com.trace.ai.services;

import com.intellij.openapi.project.Project;
import com.trace.ai.models.AIModel;
import com.trace.ai.configuration.AIServiceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AIModelService.
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
public class AIModelServiceUnitTest {

    @Test
    public void testModelServiceState() {
        AIModelService modelService = AIModelService.getInstance();
        
        // Get all models
        List<AIModel> allModels = modelService.getAllModels();
        System.out.println("Total models: " + allModels.size());
        
        for (AIModel model : allModels) {
            System.out.println("Model: " + model.getFullDisplayName());
            System.out.println("  Service Type: " + model.getServiceType());
            System.out.println("  Model ID: " + model.getModelId());
            System.out.println("  Enabled: " + model.isEnabled());
        }
        
        // Get default model
        AIModel defaultModel = modelService.getDefaultModel();
        if (defaultModel != null) {
            System.out.println("Default model: " + defaultModel.getFullDisplayName());
            System.out.println("Default model service type: " + defaultModel.getServiceType());
            System.out.println("Default model ID: " + defaultModel.getModelId());
        } else {
            System.out.println("No default model set");
        }
        
        // This test will help us see what models are currently in the state
        assertTrue(allModels.size() > 0, "Should have at least one model");
    }
    
    @Test
    public void testCreateGeminiModel() {
        AIModelService modelService = AIModelService.getInstance();
        
        // Create a new Gemini model
        AIModel geminiModel = modelService.addDiscoveredModel("Test Gemini Model", AIServiceType.GEMINI, "gemini-1.5-pro");
        
        assertNotNull(geminiModel, "Gemini model should be created");
        assertEquals(AIServiceType.GEMINI, geminiModel.getServiceType(), "Service type should be GEMINI");
        assertEquals("gemini-1.5-pro", geminiModel.getModelId(), "Model ID should be gemini-1.5-pro");
        
        // Verify it's in the list
        List<AIModel> allModels = modelService.getAllModels();
        boolean found = allModels.stream()
                .anyMatch(model -> model.getModelId().equals("gemini-1.5-pro") && 
                                 model.getServiceType() == AIServiceType.GEMINI);
        
        assertTrue(found, "Gemini model should be in the list");
    }
} 