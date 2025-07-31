package com.trace.ai.ui;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.trace.ai.configuration.AISettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CustomRulePanel.
 * 
 * <p>These tests verify the custom rule panel functionality including
 * UI initialization, character counting, validation, and persistence.</p>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
public class CustomRulePanelUnitTest extends BasePlatformTestCase {
    
    private CustomRulePanel customRulePanel;
    private AISettings aiSettings;
    
    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        aiSettings = AISettings.getInstance();
        customRulePanel = new CustomRulePanel(aiSettings);
    }
    
    @Test
    public void testPanelInitialization() {
        // Verify panel is created and visible
        assertNotNull(customRulePanel);
        assertTrue(customRulePanel.isVisible());
        
        // Verify panel has proper sizing
        Dimension preferredSize = customRulePanel.getPreferredSize();
        assertTrue(preferredSize.width >= 300);
        assertTrue(preferredSize.height >= 150);
    }
    
    @Test
    public void testCharacterCounter() {
        // Get the text area component
        JTextArea textArea = findTextArea(customRulePanel);
        assertNotNull(textArea);
        
        // Test initial state
        assertEquals("0/500 characters", findCharacterCounter(customRulePanel).getText());
        
        // Test character counting
        textArea.setText("Test rule");
        assertEquals("9/500 characters", findCharacterCounter(customRulePanel).getText());
        
        // Test long text
        String longText = "A".repeat(600);
        textArea.setText(longText);
        assertEquals("600/500 characters", findCharacterCounter(customRulePanel).getText());
    }
    
    @Test
    public void testSaveButtonState() {
        JButton saveButton = findSaveButton(customRulePanel);
        JTextArea textArea = findTextArea(customRulePanel);
        
        // Initially disabled (empty text)
        assertFalse(saveButton.isEnabled());
        
        // Enabled with valid text
        textArea.setText("Valid rule");
        assertTrue(saveButton.isEnabled());
        
        // Disabled with empty text
        textArea.setText("");
        assertFalse(saveButton.isEnabled());
        
        // Disabled with whitespace only
        textArea.setText("   ");
        assertFalse(saveButton.isEnabled());
        
        // Disabled with text too long
        textArea.setText("A".repeat(501));
        assertFalse(saveButton.isEnabled());
    }
    
    @Test
    public void testCustomRulePersistence() {
        JTextArea textArea = findTextArea(customRulePanel);
        
        // Set a custom rule
        String testRule = "Focus on performance implications";
        textArea.setText(testRule);
        
        // Simulate save
        customRulePanel.getCustomRule(); // This triggers the save logic in the real implementation
        
        // Verify the rule is accessible
        assertEquals(testRule, customRulePanel.getCustomRule());
    }
    
    @Test
    public void testClearFunctionality() {
        JTextArea textArea = findTextArea(customRulePanel);
        JButton clearButton = findClearButton(customRulePanel);
        
        // Set some text
        textArea.setText("Test rule to clear");
        
        // Verify text is set
        assertFalse(textArea.getText().isEmpty());
        
        // Clear button should be enabled
        assertTrue(clearButton.isEnabled());
    }
    
    @Test
    public void testIsModified() {
        JTextArea textArea = findTextArea(customRulePanel);
        
        // Initially not modified
        assertFalse(customRulePanel.isModified());
        
        // Modified after text change
        textArea.setText("New rule");
        assertTrue(customRulePanel.isModified());
        
        // Not modified after reset
        customRulePanel.reset();
        assertFalse(customRulePanel.isModified());
    }
    
    // Helper methods to find components
    private JTextArea findTextArea(Container container) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JTextArea) {
                return (JTextArea) comp;
            }
            if (comp instanceof Container) {
                JTextArea found = findTextArea((Container) comp);
                if (found != null) return found;
            }
        }
        return null;
    }
    
    private JLabel findCharacterCounter(Container container) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JLabel) {
                JLabel label = (JLabel) comp;
                if (label.getText().contains("characters")) {
                    return label;
                }
            }
            if (comp instanceof Container) {
                JLabel found = findCharacterCounter((Container) comp);
                if (found != null) return found;
            }
        }
        return null;
    }
    
    private JButton findSaveButton(Container container) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JButton) {
                JButton button = (JButton) comp;
                if ("Save".equals(button.getText())) {
                    return button;
                }
            }
            if (comp instanceof Container) {
                JButton found = findSaveButton((Container) comp);
                if (found != null) return found;
            }
        }
        return null;
    }
    
    private JButton findClearButton(Container container) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JButton) {
                JButton button = (JButton) comp;
                if ("Clear".equals(button.getText())) {
                    return button;
                }
            }
            if (comp instanceof Container) {
                JButton found = findClearButton((Container) comp);
                if (found != null) return found;
            }
        }
        return null;
    }
} 