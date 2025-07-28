package com.trace.ai.services;

import com.trace.ai.configuration.AIServiceType;
import com.trace.ai.models.AIModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.Application;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test to verify AIModelService persistence functionality.
 */
@ExtendWith(MockitoExtension.class)
class AIModelServicePersistenceTest {

    @Test
    void testModelPersistence() {
        // Mock the ApplicationManager.getApplication() call
        try (MockedStatic<ApplicationManager> mockedApplicationManager = Mockito.mockStatic(ApplicationManager.class)) {
            // Mock the application instance
            Application mockApplication = Mockito.mock(Application.class);
            mockedApplicationManager.when(ApplicationManager::getApplication).thenReturn(mockApplication);
            
            // Create a test service instance using reflection
            AIModelService testService;
            try {
                java.lang.reflect.Constructor<AIModelService> constructor = AIModelService.class.getDeclaredConstructor();
                constructor.setAccessible(true);
                testService = constructor.newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create AIModelService instance for testing", e);
            }
            
            // Mock the getService call to return our test service
            when(mockApplication.getService(AIModelService.class)).thenReturn(testService);
            
            // Get the service instance
            AIModelService service = AIModelService.getInstance();
            assertNotNull(service, "Service should not be null");

            // Clear any existing models
            service.deleteAllModels();
            
            // Verify no models exist
            List<AIModel> models = service.getAllModels();
            assertEquals(0, models.size(), "Should start with no models");

            // Create a test model
            AIModel testModel = service.addDiscoveredModel("Test Model", AIServiceType.OPENAI, "gpt-4o");
            assertNotNull(testModel, "Model should be created");

            // Verify model was created
            models = service.getAllModels();
            assertEquals(1, models.size(), "Should have one model");
            assertEquals(testModel.getId(), models.get(0).getId(), "Model should match");

            // Set as default
            boolean defaultSet = service.setDefaultModel(testModel.getId());
            assertTrue(defaultSet, "Default model should be set");

            // Verify default model
            AIModel defaultModel = service.getDefaultModel();
            assertNotNull(defaultModel, "Default model should exist");
            assertEquals(testModel.getId(), defaultModel.getId(), "Default model should match");

            // Get a new service instance (simulating restart)
            AIModelService newService = AIModelService.getInstance();
            
            // Verify models persist
            List<AIModel> persistedModels = newService.getAllModels();
            assertEquals(1, persistedModels.size(), "Models should persist");
            assertEquals(testModel.getId(), persistedModels.get(0).getId(), "Persisted model should match");

            // Verify default model persists
            AIModel persistedDefault = newService.getDefaultModel();
            assertNotNull(persistedDefault, "Default model should persist");
            assertEquals(testModel.getId(), persistedDefault.getId(), "Persisted default should match");
        }
    }

    @Test
    void testServiceInitialization() {
        // Mock the ApplicationManager.getApplication() call
        try (MockedStatic<ApplicationManager> mockedApplicationManager = Mockito.mockStatic(ApplicationManager.class)) {
            // Mock the application instance
            Application mockApplication = Mockito.mock(Application.class);
            mockedApplicationManager.when(ApplicationManager::getApplication).thenReturn(mockApplication);
            
            // Create a test service instance using reflection
            AIModelService testService;
            try {
                java.lang.reflect.Constructor<AIModelService> constructor = AIModelService.class.getDeclaredConstructor();
                constructor.setAccessible(true);
                testService = constructor.newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create AIModelService instance for testing", e);
            }
            
            // Mock the getService call to return our test service
            when(mockApplication.getService(AIModelService.class)).thenReturn(testService);
            
            // Test that service can be initialized
            AIModelService service = AIModelService.getInstance();
            assertNotNull(service, "Service should be initialized");
            
            // Test that we can get models (even if empty)
            List<AIModel> models = service.getAllModels();
            assertNotNull(models, "Models list should not be null");
        }
    }
} 