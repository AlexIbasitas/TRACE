package com.trace.ai.services;

import com.trace.ai.configuration.AIServiceType;
import com.trace.ai.services.providers.AIServiceProvider;
import com.trace.ai.services.providers.GeminiProvider;
import com.trace.ai.services.providers.OpenAIProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AIServiceFactory.
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
public class AIServiceFactoryUnitTest {

    @Test
    public void testProviderSelection() {
        // Test OpenAI provider
        AIServiceProvider openaiProvider = AIServiceFactory.getProvider(AIServiceType.OPENAI);
        assertNotNull(openaiProvider, "OpenAI provider should be available");
        assertTrue(openaiProvider instanceof OpenAIProvider, "Provider should be OpenAIProvider");
        assertEquals("OpenAI", openaiProvider.getDisplayName(), "Provider display name should be OpenAI");
        
        // Test Gemini provider
        AIServiceProvider geminiProvider = AIServiceFactory.getProvider(AIServiceType.GEMINI);
        assertNotNull(geminiProvider, "Gemini provider should be available");
        assertTrue(geminiProvider instanceof GeminiProvider, "Provider should be GeminiProvider");
        assertEquals("Google Gemini", geminiProvider.getDisplayName(), "Provider display name should be Google Gemini");
    }
    
    @Test
    public void testProviderAvailability() {
        // Test that both providers are registered
        assertTrue(AIServiceFactory.hasProvider(AIServiceType.OPENAI), "OpenAI provider should be registered");
        assertTrue(AIServiceFactory.hasProvider(AIServiceType.GEMINI), "Gemini provider should be registered");
        
        // Test that we get the expected number of providers
        assertEquals(2, AIServiceFactory.getProviderCount(), "Should have 2 providers registered");
        
        // Test that we get the expected service types
        AIServiceType[] registeredTypes = AIServiceFactory.getRegisteredServiceTypes();
        assertEquals(2, registeredTypes.length, "Should have 2 registered service types");
        
        boolean hasOpenAI = false;
        boolean hasGemini = false;
        for (AIServiceType type : registeredTypes) {
            if (type == AIServiceType.OPENAI) hasOpenAI = true;
            if (type == AIServiceType.GEMINI) hasGemini = true;
        }
        
        assertTrue(hasOpenAI, "Should have OpenAI service type registered");
        assertTrue(hasGemini, "Should have Gemini service type registered");
    }
} 