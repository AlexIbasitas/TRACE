package com.triagemate.security;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.triagemate.settings.AIServiceType;

/**
 * Integration tests for SecureAPIKeyManager using full IntelliJ Platform environment.
 * 
 * <p>These tests verify the actual PasswordSafe integration and API key storage
 * functionality in a real IntelliJ Platform environment.</p>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
public class SecureAPIKeyManagerIntegrationTest extends BasePlatformTestCase {
    
    private static final String TEST_OPENAI_KEY = "sk-test123456789012345678901234567890";
    private static final String TEST_GEMINI_KEY = "AIzaSyC123456789012345678901234567890";
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Clear any existing test keys before each test
        SecureAPIKeyManager.clearAllAPIKeys();
    }
    
    @Override
    protected void tearDown() throws Exception {
        // Clean up test keys after each test
        SecureAPIKeyManager.clearAllAPIKeys();
        super.tearDown();
    }
    
    public void testStoreAndRetrieveOpenAIKey() {
        // Store the key
        boolean storeResult = SecureAPIKeyManager.storeAPIKey(AIServiceType.OPENAI, TEST_OPENAI_KEY);
        assertTrue("Should store OpenAI API key successfully", storeResult);
        
        // Verify key exists
        assertTrue("Should have OpenAI key after storing", SecureAPIKeyManager.hasAPIKey(AIServiceType.OPENAI));
        
        // Retrieve the key
        String retrievedKey = SecureAPIKeyManager.getAPIKey(AIServiceType.OPENAI);
        assertEquals("Should retrieve the same OpenAI key that was stored", TEST_OPENAI_KEY, retrievedKey);
    }
    
    public void testStoreAndRetrieveGeminiKey() {
        // Store the key
        boolean storeResult = SecureAPIKeyManager.storeAPIKey(AIServiceType.GEMINI, TEST_GEMINI_KEY);
        assertTrue("Should store Gemini API key successfully", storeResult);
        
        // Verify key exists
        assertTrue("Should have Gemini key after storing", SecureAPIKeyManager.hasAPIKey(AIServiceType.GEMINI));
        
        // Retrieve the key
        String retrievedKey = SecureAPIKeyManager.getAPIKey(AIServiceType.GEMINI);
        assertEquals("Should retrieve the same Gemini key that was stored", TEST_GEMINI_KEY, retrievedKey);
    }
    
    public void testClearAPIKey() {
        // Store a key first
        SecureAPIKeyManager.storeAPIKey(AIServiceType.OPENAI, TEST_OPENAI_KEY);
        assertTrue("Key should exist before clearing", SecureAPIKeyManager.hasAPIKey(AIServiceType.OPENAI));
        
        // Clear the key
        boolean clearResult = SecureAPIKeyManager.clearAPIKey(AIServiceType.OPENAI);
        assertTrue("Should clear API key successfully", clearResult);
        
        // Verify key is gone
        assertFalse("Key should not exist after clearing", SecureAPIKeyManager.hasAPIKey(AIServiceType.OPENAI));
        assertNull("Should return null after clearing", SecureAPIKeyManager.getAPIKey(AIServiceType.OPENAI));
    }
    
    public void testMultipleServicesIndependently() {
        // Store keys for both services
        assertTrue("Should store OpenAI key", SecureAPIKeyManager.storeAPIKey(AIServiceType.OPENAI, TEST_OPENAI_KEY));
        assertTrue("Should store Gemini key", SecureAPIKeyManager.storeAPIKey(AIServiceType.GEMINI, TEST_GEMINI_KEY));
        
        // Verify both keys exist
        assertTrue("Should have OpenAI key", SecureAPIKeyManager.hasAPIKey(AIServiceType.OPENAI));
        assertTrue("Should have Gemini key", SecureAPIKeyManager.hasAPIKey(AIServiceType.GEMINI));
        
        // Clear only OpenAI key
        SecureAPIKeyManager.clearAPIKey(AIServiceType.OPENAI);
        
        // Verify OpenAI key is gone but Gemini key remains
        assertFalse("OpenAI key should be cleared", SecureAPIKeyManager.hasAPIKey(AIServiceType.OPENAI));
        assertTrue("Gemini key should still exist", SecureAPIKeyManager.hasAPIKey(AIServiceType.GEMINI));
    }
    
    public void testAPIKeyStatus() {
        // Initially no keys
        String status = SecureAPIKeyManager.getAPIKeyStatus();
        assertTrue("Should show OpenAI status", status.contains("OpenAI:"));
        assertTrue("Should show Gemini status", status.contains("Google Gemini:"));
        
        // Store one key
        SecureAPIKeyManager.storeAPIKey(AIServiceType.OPENAI, TEST_OPENAI_KEY);
        status = SecureAPIKeyManager.getAPIKeyStatus();
        assertTrue("Should show OpenAI as configured", status.contains("OpenAI: Configured"));
    }
    
    public void testEmptyAPIKey() {
        boolean result = SecureAPIKeyManager.storeAPIKey(AIServiceType.OPENAI, "");
        assertFalse("Should reject empty API key", result);
        
        result = SecureAPIKeyManager.storeAPIKey(AIServiceType.OPENAI, "   ");
        assertFalse("Should reject whitespace-only API key", result);
    }
} 