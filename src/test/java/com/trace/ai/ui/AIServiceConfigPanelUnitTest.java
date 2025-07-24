package com.trace.ai.ui;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.trace.ai.configuration.AISettings;
import com.trace.ai.configuration.AIServiceType;
import com.trace.security.SecureAPIKeyManager;
import org.junit.Test;
import org.mockito.Mock;

import javax.swing.*;
import java.awt.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AIServiceConfigPanel API key persistence functionality.
 * 
 * <p>These tests verify that API keys are properly saved, loaded, and managed
 * through the SecureAPIKeyManager integration.</p>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
class AIServiceConfigPanelUnitTest extends BasePlatformTestCase {
    
    @Mock
    private AISettings aiSettings;
    
    private AIServiceConfigPanel configPanel;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        configPanel = new AIServiceConfigPanel(aiSettings);
    }
    
    @Test
    public void testApplySavesAPIKeys() {
        // Given: API keys entered in the UI
        String testOpenAIKey = "sk-test-openai-key-12345";
        String testGeminiKey = "test-gemini-key-12345";
        
        // Set up the password fields
        setPasswordFieldText(configPanel, "openaiApiKeyField", testOpenAIKey);
        setPasswordFieldText(configPanel, "geminiApiKeyField", testGeminiKey);
        
        // When: apply() is called
        configPanel.apply();
        
        // Then: keys should be stored in SecureAPIKeyManager
        String storedOpenAIKey = SecureAPIKeyManager.getAPIKey(AIServiceType.OPENAI);
        String storedGeminiKey = SecureAPIKeyManager.getAPIKey(AIServiceType.GEMINI);
        
        assertEquals(testOpenAIKey, storedOpenAIKey, "OpenAI key should be saved");
        assertEquals(testGeminiKey, storedGeminiKey, "Gemini key should be saved");
    }
    
    @Test
    public void testApplyClearsEmptyKeys() {
        // Given: empty API key fields
        setPasswordFieldText(configPanel, "openaiApiKeyField", "");
        setPasswordFieldText(configPanel, "geminiApiKeyField", "");
        
        // When: apply() is called
        configPanel.apply();
        
        // Then: keys should be cleared from storage
        String storedOpenAIKey = SecureAPIKeyManager.getAPIKey(AIServiceType.OPENAI);
        String storedGeminiKey = SecureAPIKeyManager.getAPIKey(AIServiceType.GEMINI);
        
        assertNull(storedOpenAIKey, "OpenAI key should be cleared");
        assertNull(storedGeminiKey, "Gemini key should be cleared");
    }
    
    @Test
    public void testConstructorLoadsStoredKeys() throws InterruptedException {
        // Given: API keys stored in SecureAPIKeyManager
        String testOpenAIKey = "sk-test-openai-key-67890";
        String testGeminiKey = "test-gemini-key-67890";
        
        SecureAPIKeyManager.storeAPIKey(AIServiceType.OPENAI, testOpenAIKey);
        SecureAPIKeyManager.storeAPIKey(AIServiceType.GEMINI, testGeminiKey);
        
        // When: new panel is created (loads settings asynchronously)
        AIServiceConfigPanel newPanel = new AIServiceConfigPanel(aiSettings);
        
        // Wait for async operations to complete
        Thread.sleep(200);
        
        // Then: UI fields should be populated with stored keys
        String uiOpenAIKey = getPasswordFieldText(newPanel, "openaiApiKeyField");
        String uiGeminiKey = getPasswordFieldText(newPanel, "geminiApiKeyField");
        
        assertEquals(testOpenAIKey, uiOpenAIKey, "OpenAI key should be loaded into UI");
        assertEquals(testGeminiKey, uiGeminiKey, "Gemini key should be loaded into UI");
    }
    
    @Test
    public void testIsModifiedDetectsChanges() throws InterruptedException {
        // Given: original state with no keys
        SecureAPIKeyManager.clearAPIKey(AIServiceType.OPENAI);
        SecureAPIKeyManager.clearAPIKey(AIServiceType.GEMINI);
        
        // Create new panel to load settings
        AIServiceConfigPanel newPanel = new AIServiceConfigPanel(aiSettings);
        Thread.sleep(200); // Wait for async loading
        
        // When: user enters a key
        setPasswordFieldText(newPanel, "openaiApiKeyField", "sk-new-key");
        
        // Then: modification should be detected
        assertTrue("Should detect modification when key is entered", newPanel.isModified());
    }
    

    
    @Test
    public void testIsModifiedNoChanges() throws InterruptedException {
        // Given: original state with a key
        String testKey = "sk-test-key";
        SecureAPIKeyManager.storeAPIKey(AIServiceType.OPENAI, testKey);
        
        // Create new panel to load settings
        AIServiceConfigPanel newPanel = new AIServiceConfigPanel(aiSettings);
        Thread.sleep(200); // Wait for async loading
        
        // When: UI matches original state
        setPasswordFieldText(newPanel, "openaiApiKeyField", testKey);
        
        // Then: no modification should be detected
        assertFalse("Should not detect modification when state matches", newPanel.isModified());
    }
    
    @Test
    public void testResetRestoresOriginalState() throws InterruptedException {
        // Given: original state with stored keys
        String originalOpenAIKey = "sk-original-openai";
        String originalGeminiKey = "original-gemini";
        
        SecureAPIKeyManager.storeAPIKey(AIServiceType.OPENAI, originalOpenAIKey);
        SecureAPIKeyManager.storeAPIKey(AIServiceType.GEMINI, originalGeminiKey);
        
        // Create new panel to load settings
        AIServiceConfigPanel newPanel = new AIServiceConfigPanel(aiSettings);
        Thread.sleep(200); // Wait for async loading
        
        // User changes the keys
        setPasswordFieldText(newPanel, "openaiApiKeyField", "sk-changed-openai");
        setPasswordFieldText(newPanel, "geminiApiKeyField", "changed-gemini");
        
        // When: reset() is called
        newPanel.reset();
        Thread.sleep(200); // Wait for async reset
        
        // Then: original keys should be restored
        String uiOpenAIKey = getPasswordFieldText(newPanel, "openaiApiKeyField");
        String uiGeminiKey = getPasswordFieldText(newPanel, "geminiApiKeyField");
        
        assertEquals(originalOpenAIKey, uiOpenAIKey, "OpenAI key should be reset to original");
        assertEquals(originalGeminiKey, uiGeminiKey, "Gemini key should be reset to original");
    }
    
    @Test
    public void testConstructorDisplaysStoredKeys() throws InterruptedException {
        // Given: API keys are stored in secure storage
        String testOpenAIKey = "sk-test-openai-key";
        String testGeminiKey = "test-gemini-key";
        SecureAPIKeyManager.storeAPIKey(AIServiceType.OPENAI, testOpenAIKey);
        SecureAPIKeyManager.storeAPIKey(AIServiceType.GEMINI, testGeminiKey);
        
        // When: new panel is created (loads settings asynchronously)
        AIServiceConfigPanel newPanel = new AIServiceConfigPanel(aiSettings);
        Thread.sleep(200); // Wait for async loading
        
        // Then: UI should display the stored keys and show connected status
        String displayedOpenAIKey = new String(newPanel.getOpenaiApiKeyField().getPassword());
        String displayedGeminiKey = new String(newPanel.getGeminiApiKeyField().getPassword());
        
        assertEquals(testOpenAIKey, displayedOpenAIKey, "OpenAI key should be displayed in UI");
        assertEquals(testGeminiKey, displayedGeminiKey, "Gemini key should be displayed in UI");
        
        // Verify status labels show connected state
        assertTrue("OpenAI status should show connected", 
                  newPanel.getOpenaiStatusLabel().getText().contains("Connected"));
        assertTrue("Gemini status should show connected", 
                  newPanel.getGeminiStatusLabel().getText().contains("Connected"));
    }
    
    @Test
    public void testResetLoadsStoredKeys() throws InterruptedException {
        // Given: API keys are stored in secure storage
        String testOpenAIKey = "sk-test-openai-key-reset";
        String testGeminiKey = "test-gemini-key-reset";
        SecureAPIKeyManager.storeAPIKey(AIServiceType.OPENAI, testOpenAIKey);
        SecureAPIKeyManager.storeAPIKey(AIServiceType.GEMINI, testGeminiKey);
        
        // Create new panel to load settings
        AIServiceConfigPanel newPanel = new AIServiceConfigPanel(aiSettings);
        Thread.sleep(200); // Wait for async loading
        
        // When: reset is called (simulating reopening settings panel)
        newPanel.reset();
        Thread.sleep(200); // Wait for async reset
        
        // Then: UI should display the stored keys
        String displayedOpenAIKey = new String(newPanel.getOpenaiApiKeyField().getPassword());
        String displayedGeminiKey = new String(newPanel.getGeminiApiKeyField().getPassword());
        
        assertEquals(testOpenAIKey, displayedOpenAIKey, "OpenAI key should be displayed after reset");
        assertEquals(testGeminiKey, displayedGeminiKey, "Gemini key should be displayed after reset");
    }
    
    @Test
    public void testApplyButtonSavesAPIKeys() {
        // Given: API key is entered in the field
        setPasswordFieldText(configPanel, "openaiApiKeyField", "sk-test-openai-key");
        
        // When: Apply button is clicked (simulates testOpenAIKey method)
        configPanel.getTestOpenAIButton().doClick();
        
        // Wait for async operation to complete
        try {
            Thread.sleep(1500); // Wait for the simulated test delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Then: API key should be saved
        String storedKey = SecureAPIKeyManager.getAPIKey(AIServiceType.OPENAI);
        assertEquals("sk-test-openai-key", storedKey, "API key should be saved after Apply button click");
        
        // And: status should show connected
        assertTrue("Status should show connected after successful Apply", 
                  configPanel.getOpenaiStatusLabel().getText().contains("Connected"));
    }
    
    // Helper methods to access private fields for testing
    private void setPasswordFieldText(AIServiceConfigPanel panel, String fieldName, String text) {
        try {
            java.lang.reflect.Field field = AIServiceConfigPanel.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            JPasswordField passwordField = (JPasswordField) field.get(panel);
            passwordField.setText(text);
        } catch (Exception e) {
            fail("Failed to set password field text: " + e.getMessage());
        }
    }
    
    private String getPasswordFieldText(AIServiceConfigPanel panel, String fieldName) {
        try {
            java.lang.reflect.Field field = AIServiceConfigPanel.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            JPasswordField passwordField = (JPasswordField) field.get(panel);
            return new String(passwordField.getPassword());
        } catch (Exception e) {
            fail("Failed to get password field text: " + e.getMessage());
            return null;
        }
    }
    
    @Override
    protected void tearDown() throws Exception {
        try {
            // Clean up stored keys after each test
            SecureAPIKeyManager.clearAPIKey(AIServiceType.OPENAI);
            SecureAPIKeyManager.clearAPIKey(AIServiceType.GEMINI);
            
            // Clean up any UI resources if needed
            if (configPanel != null) {
                configPanel.disposeUIResources();
            }
            
        } catch (Exception e) {
            addSuppressedException(e);
        } finally {
            super.tearDown();
        }
    }
} 