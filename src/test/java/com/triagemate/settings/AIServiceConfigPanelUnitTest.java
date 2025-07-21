package com.triagemate.settings;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.triagemate.security.SecureAPIKeyManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AIServiceConfigPanel.
 * 
 * <p>These tests verify that the AI service configuration panel correctly handles
 * service selection, API key management, connection testing, and state management.
 * The tests follow IntelliJ Platform best practices and use proper mocking for
 * external dependencies.</p>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
class AIServiceConfigPanelUnitTest extends BasePlatformTestCase {
    
    private AIServiceConfigPanel panel;
    private AISettings mockAISettings;
    
    @BeforeEach
    void setUp() {
        // Create mock AISettings
        mockAISettings = mock(AISettings.class);
        when(mockAISettings.getPreferredAIService()).thenReturn(AIServiceType.OPENAI);
        
        // Create panel with mock settings
        panel = new AIServiceConfigPanel(mockAISettings);
    }
    
    @Test
    @DisplayName("should initialize with correct default values")
    void shouldInitializeWithCorrectDefaultValues() {
        // Verify service combo box is populated
        JComboBox<AIServiceType> serviceComboBox = getServiceComboBox();
        assertNotNull(serviceComboBox);
        assertEquals(2, serviceComboBox.getItemCount()); // OpenAI and Gemini
        assertEquals(AIServiceType.OPENAI, serviceComboBox.getSelectedItem());
        
        // Verify API key field is empty
        JPasswordField apiKeyField = getAPIKeyField();
        assertNotNull(apiKeyField);
        assertEquals("", new String(apiKeyField.getPassword()));
        
        // Verify buttons are present
        assertNotNull(getTestConnectionButton());
        assertNotNull(getClearKeyButton());
        assertNotNull(getShowHideButton());
    }
    
    @Test
    @DisplayName("should handle service selection change")
    void shouldHandleServiceSelectionChange() {
        try (MockedStatic<SecureAPIKeyManager> mockedManager = Mockito.mockStatic(SecureAPIKeyManager.class)) {
            // Mock API key retrieval
            mockedManager.when(() -> SecureAPIKeyManager.getAPIKey(AIServiceType.GEMINI))
                        .thenReturn("test-gemini-key");
            
            // Get service combo box and change selection
            JComboBox<AIServiceType> serviceComboBox = getServiceComboBox();
            serviceComboBox.setSelectedItem(AIServiceType.GEMINI);
            
            // Simulate action event
            ActionEvent event = new ActionEvent(serviceComboBox, ActionEvent.ACTION_PERFORMED, "comboBoxChanged");
            for (ActionListener listener : serviceComboBox.getActionListeners()) {
                listener.actionPerformed(event);
            }
            
            // Verify service preference was updated
            verify(mockAISettings).setPreferredAIService(AIServiceType.GEMINI);
        }
    }
    
    @Test
    @DisplayName("should toggle API key visibility")
    void shouldToggleAPIKeyVisibility() {
        JPasswordField apiKeyField = getAPIKeyField();
        JButton showHideButton = getShowHideButton();
        
        // Initially hidden
        assertEquals('•', apiKeyField.getEchoChar());
        assertEquals("Show", showHideButton.getText());
        
        // Click show button
        ActionEvent event = new ActionEvent(showHideButton, ActionEvent.ACTION_PERFORMED, "buttonClicked");
        for (ActionListener listener : showHideButton.getActionListeners()) {
            listener.actionPerformed(event);
        }
        
        // Should now be visible
        assertEquals('\u0000', apiKeyField.getEchoChar());
        assertEquals("Hide", showHideButton.getText());
        
        // Click hide button
        for (ActionListener listener : showHideButton.getActionListeners()) {
            listener.actionPerformed(event);
        }
        
        // Should be hidden again
        assertEquals('•', apiKeyField.getEchoChar());
        assertEquals("Show", showHideButton.getText());
    }
    
    @Test
    @DisplayName("should store API key when user types")
    void shouldStoreAPIKeyWhenUserTypes() {
        try (MockedStatic<SecureAPIKeyManager> mockedManager = Mockito.mockStatic(SecureAPIKeyManager.class)) {
            // Mock API key storage
            mockedManager.when(() -> SecureAPIKeyManager.storeAPIKey(any(AIServiceType.class), anyString()))
                        .thenReturn(true);
            
            // Set service to OpenAI
            JComboBox<AIServiceType> serviceComboBox = getServiceComboBox();
            serviceComboBox.setSelectedItem(AIServiceType.OPENAI);
            
            // Type in API key
            JPasswordField apiKeyField = getAPIKeyField();
            apiKeyField.setText("test-api-key");
            
            // Simulate document change
            apiKeyField.getDocument().fireDocumentListeners();
            
            // Verify API key was stored
            mockedManager.verify(() -> SecureAPIKeyManager.storeAPIKey(AIServiceType.OPENAI, "test-api-key"));
        }
    }
    
    @Test
    @DisplayName("should test connection successfully")
    void shouldTestConnectionSuccessfully() {
        try (MockedStatic<SecureAPIKeyManager> mockedManager = Mockito.mockStatic(SecureAPIKeyManager.class)) {
            // Mock successful validation
            mockedManager.when(() -> SecureAPIKeyManager.validateAPIKey(any(AIServiceType.class), anyString()))
                        .thenReturn(CompletableFuture.completedFuture(true));
            
            // Set up test data
            JComboBox<AIServiceType> serviceComboBox = getServiceComboBox();
            serviceComboBox.setSelectedItem(AIServiceType.OPENAI);
            
            JPasswordField apiKeyField = getAPIKeyField();
            apiKeyField.setText("test-api-key");
            
            // Click test connection button
            JButton testButton = getTestConnectionButton();
            ActionEvent event = new ActionEvent(testButton, ActionEvent.ACTION_PERFORMED, "buttonClicked");
            for (ActionListener listener : testButton.getActionListeners()) {
                listener.actionPerformed(event);
            }
            
            // Verify validation was called
            mockedManager.verify(() -> SecureAPIKeyManager.validateAPIKey(AIServiceType.OPENAI, "test-api-key"));
        }
    }
    
    @Test
    @DisplayName("should handle connection test failure")
    void shouldHandleConnectionTestFailure() {
        try (MockedStatic<SecureAPIKeyManager> mockedManager = Mockito.mockStatic(SecureAPIKeyManager.class)) {
            // Mock failed validation
            mockedManager.when(() -> SecureAPIKeyManager.validateAPIKey(any(AIServiceType.class), anyString()))
                        .thenReturn(CompletableFuture.completedFuture(false));
            
            // Set up test data
            JComboBox<AIServiceType> serviceComboBox = getServiceComboBox();
            serviceComboBox.setSelectedItem(AIServiceType.OPENAI);
            
            JPasswordField apiKeyField = getAPIKeyField();
            apiKeyField.setText("invalid-api-key");
            
            // Click test connection button
            JButton testButton = getTestConnectionButton();
            ActionEvent event = new ActionEvent(testButton, ActionEvent.ACTION_PERFORMED, "buttonClicked");
            for (ActionListener listener : testButton.getActionListeners()) {
                listener.actionPerformed(event);
            }
            
            // Verify validation was called
            mockedManager.verify(() -> SecureAPIKeyManager.validateAPIKey(AIServiceType.OPENAI, "invalid-api-key"));
        }
    }
    
    @Test
    @DisplayName("should clear API key when requested")
    void shouldClearAPIKeyWhenRequested() {
        try (MockedStatic<SecureAPIKeyManager> mockedManager = Mockito.mockStatic(SecureAPIKeyManager.class)) {
            // Mock API key clearing
            mockedManager.when(() -> SecureAPIKeyManager.clearAPIKey(any(AIServiceType.class)))
                        .thenReturn(true);
            
            // Set up test data
            JComboBox<AIServiceType> serviceComboBox = getServiceComboBox();
            serviceComboBox.setSelectedItem(AIServiceType.OPENAI);
            
            JPasswordField apiKeyField = getAPIKeyField();
            apiKeyField.setText("test-api-key");
            
            // Click clear key button
            JButton clearButton = getClearKeyButton();
            ActionEvent event = new ActionEvent(clearButton, ActionEvent.ACTION_PERFORMED, "buttonClicked");
            for (ActionListener listener : clearButton.getActionListeners()) {
                listener.actionPerformed(event);
            }
            
            // Verify API key was cleared
            mockedManager.verify(() -> SecureAPIKeyManager.clearAPIKey(AIServiceType.OPENAI));
        }
    }
    
    @Test
    @DisplayName("should enable/disable panel based on AI analysis state")
    void shouldEnableDisablePanelBasedOnAIAnalysisState() {
        // Test enabled state
        panel.setPanelEnabled(true);
        
        JComboBox<AIServiceType> serviceComboBox = getServiceComboBox();
        JPasswordField apiKeyField = getAPIKeyField();
        JButton testButton = getTestConnectionButton();
        JButton clearButton = getClearKeyButton();
        JButton showHideButton = getShowHideButton();
        
        assertTrue(serviceComboBox.isEnabled());
        assertTrue(apiKeyField.isEnabled());
        assertTrue(testButton.isEnabled());
        assertTrue(clearButton.isEnabled());
        assertTrue(showHideButton.isEnabled());
        
        // Test disabled state
        panel.setPanelEnabled(false);
        
        assertFalse(serviceComboBox.isEnabled());
        assertFalse(apiKeyField.isEnabled());
        assertFalse(testButton.isEnabled());
        assertFalse(clearButton.isEnabled());
        assertFalse(showHideButton.isEnabled());
    }
    
    @Test
    @DisplayName("should detect modifications correctly")
    void shouldDetectModificationsCorrectly() {
        // Initially not modified
        assertFalse(panel.isModified());
        
        // Change service selection
        JComboBox<AIServiceType> serviceComboBox = getServiceComboBox();
        serviceComboBox.setSelectedItem(AIServiceType.GEMINI);
        
        // Should be modified
        assertTrue(panel.isModified());
        
        // Reset to original state
        serviceComboBox.setSelectedItem(AIServiceType.OPENAI);
        assertFalse(panel.isModified());
        
        // Change API key
        JPasswordField apiKeyField = getAPIKeyField();
        apiKeyField.setText("new-api-key");
        
        // Should be modified
        assertTrue(panel.isModified());
    }
    
    @Test
    @DisplayName("should apply settings correctly")
    void shouldApplySettingsCorrectly() {
        try (MockedStatic<SecureAPIKeyManager> mockedManager = Mockito.mockStatic(SecureAPIKeyManager.class)) {
            // Mock API key storage
            mockedManager.when(() -> SecureAPIKeyManager.storeAPIKey(any(AIServiceType.class), anyString()))
                        .thenReturn(true);
            
            // Set up test data
            JComboBox<AIServiceType> serviceComboBox = getServiceComboBox();
            serviceComboBox.setSelectedItem(AIServiceType.GEMINI);
            
            JPasswordField apiKeyField = getAPIKeyField();
            apiKeyField.setText("test-api-key");
            
            // Apply settings
            panel.apply();
            
            // Verify settings were applied
            verify(mockAISettings).setPreferredAIService(AIServiceType.GEMINI);
            mockedManager.verify(() -> SecureAPIKeyManager.storeAPIKey(AIServiceType.GEMINI, "test-api-key"));
        }
    }
    
    @Test
    @DisplayName("should reset to current settings")
    void shouldResetToCurrentSettings() {
        try (MockedStatic<SecureAPIKeyManager> mockedManager = Mockito.mockStatic(SecureAPIKeyManager.class)) {
            // Mock API key retrieval
            mockedManager.when(() -> SecureAPIKeyManager.getAPIKey(AIServiceType.OPENAI))
                        .thenReturn("original-api-key");
            
            // Change some values
            JComboBox<AIServiceType> serviceComboBox = getServiceComboBox();
            serviceComboBox.setSelectedItem(AIServiceType.GEMINI);
            
            JPasswordField apiKeyField = getAPIKeyField();
            apiKeyField.setText("changed-api-key");
            
            // Verify modified
            assertTrue(panel.isModified());
            
            // Reset
            panel.reset();
            
            // Verify reset to original values
            assertEquals(AIServiceType.OPENAI, serviceComboBox.getSelectedItem());
            assertEquals("original-api-key", new String(apiKeyField.getPassword()));
            assertFalse(panel.isModified());
        }
    }
    
    // Helper methods to access private UI components
    @SuppressWarnings("unchecked")
    private JComboBox<AIServiceType> getServiceComboBox() {
        return (JComboBox<AIServiceType>) findComponentByName(panel, "serviceComboBox");
    }
    
    private JPasswordField getAPIKeyField() {
        return (JPasswordField) findComponentByName(panel, "apiKeyField");
    }
    
    private JButton getTestConnectionButton() {
        return (JButton) findComponentByName(panel, "testConnectionButton");
    }
    
    private JButton getClearKeyButton() {
        return (JButton) findComponentByName(panel, "clearKeyButton");
    }
    
    private JButton getShowHideButton() {
        return (JButton) findComponentByName(panel, "showHideButton");
    }
    
    private Component findComponentByName(Container container, String name) {
        for (Component component : container.getComponents()) {
            if (name.equals(component.getName())) {
                return component;
            }
            if (component instanceof Container) {
                Component found = findComponentByName((Container) component, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
} 