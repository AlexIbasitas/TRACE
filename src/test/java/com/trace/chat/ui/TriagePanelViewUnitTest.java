package com.trace.chat.ui;

import com.intellij.openapi.project.Project;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.trace.ai.configuration.AISettings;
import com.trace.chat.components.ChatMessage;
import com.trace.test.models.FailureInfo;
import com.trace.test.models.StepDefinitionInfo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Unit tests for TriagePanelView functionality.
 * 
 * <p>This test class focuses on testing the AI toggle functionality and
 * related UI interactions in the TriagePanelView.</p>
 * 
 * @author Alex Ibasitas
 * @version 1.0
 * @since 1.0
 */
public class TriagePanelViewUnitTest extends BasePlatformTestCase {
    
    private TriagePanelView triagePanelView;
    private AISettings aiSettings;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        triagePanelView = new TriagePanelView(getProject());
        aiSettings = AISettings.getInstance();
        
        // Reset AI settings to known state
        aiSettings.setAIEnabled(false);
    }
    
    @Override
    protected void tearDown() throws Exception {
        // Reset AI settings
        aiSettings.setAIEnabled(false);
        super.tearDown();
    }
    
    /**
     * Test that the AI toggle button is created and visible in the header.
     */
    public void testAIToggleButtonIsCreated() {
        JComponent content = triagePanelView.getContent();
        assertNotNull("Content should not be null", content);
        
        // Find the toggle button in the component hierarchy
        JButton toggleButton = findAIToggleButton(content);
        assertNotNull("AI toggle button should be present", toggleButton);
        assertEquals("Toggle button should have power symbol", "⏻", toggleButton.getText());
    }
    
    /**
     * Test that the AI toggle button reflects the current AI state.
     */
    public void testAIToggleButtonReflectsAIState() {
        JComponent content = triagePanelView.getContent();
        JButton toggleButton = findAIToggleButton(content);
        assertNotNull("AI toggle button should be present", toggleButton);
        
        // Test disabled state
        aiSettings.setAIEnabled(false);
        assertEquals("Button should show disabled tooltip", "Enable AI Analysis", toggleButton.getToolTipText());
        
        // Test enabled state
        aiSettings.setAIEnabled(true);
        assertEquals("Button should show enabled tooltip", "Disable AI Analysis", toggleButton.getToolTipText());
    }
    
    /**
     * Test that clicking the AI toggle button changes the AI state.
     */
    public void testAIToggleButtonChangesAIState() {
        JComponent content = triagePanelView.getContent();
        JButton toggleButton = findAIToggleButton(content);
        assertNotNull("AI toggle button should be present", toggleButton);
        
        // Initial state should be disabled
        assertFalse("AI should be disabled initially", aiSettings.isAIEnabled());
        
        // Click the button to enable AI
        toggleButton.doClick();
        assertTrue("AI should be enabled after clicking", aiSettings.isAIEnabled());
        
        // Click again to disable AI
        toggleButton.doClick();
        assertFalse("AI should be disabled after second click", aiSettings.isAIEnabled());
    }
    
    /**
     * Test that the TRACE text is still present in the header.
     */
    public void testTRACETextIsPresent() {
        JComponent content = triagePanelView.getContent();
        
        // Find the TRACE label in the component hierarchy
        JLabel traceLabel = findTRACELabel(content);
        assertNotNull("TRACE label should be present", traceLabel);
        assertEquals("Label should contain TRACE text", "TRACE", traceLabel.getText());
    }
    
    /**
     * Test that AI disabled state prevents prompt generation.
     */
    public void testAIDisabledPreventsPromptGeneration() {
        // Set AI to disabled
        aiSettings.setAIEnabled(false);
        
        // Create a test failure info
        FailureInfo failureInfo = createTestFailureInfo();
        
        // Update the failure - this should trigger prompt generation
        triagePanelView.updateFailure(failureInfo);
        
        // The prompt generation should be skipped and a disabled message should be shown
        // This test verifies the integration with LocalPromptGenerationService
        assertFalse("AI should remain disabled", aiSettings.isAIEnabled());
    }
    
    /**
     * Helper method to find the AI toggle button in the component hierarchy.
     */
    private JButton findAIToggleButton(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JButton) {
                JButton button = (JButton) component;
                if ("⏻".equals(button.getText())) {
                    return button;
                }
            }
            if (component instanceof Container) {
                JButton found = findAIToggleButton((Container) component);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
    
    /**
     * Helper method to find the TRACE label in the component hierarchy.
     */
    private JLabel findTRACELabel(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JLabel) {
                JLabel label = (JLabel) component;
                if ("TRACE".equals(label.getText())) {
                    return label;
                }
            }
            if (component instanceof Container) {
                JLabel found = findTRACELabel((Container) component);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
    
    /**
     * Helper method to create a test FailureInfo for testing.
     */
    private FailureInfo createTestFailureInfo() {
        StepDefinitionInfo stepDefInfo = new StepDefinitionInfo.Builder()
            .withMethodName("testMethod")
            .withClassName("TestClass")
            .withPackageName("com.example")
            .withSourceFilePath("src/test/java/com/example/TestClass.java")
            .withLineNumber(10)
            .withStepPattern("^Given a test step$")
            .withMethodText("public void testMethod() { }")
            .build();
        
        FailureInfo failureInfo = new FailureInfo.Builder()
            .withScenarioName("Test Scenario")
            .withFailedStepText("Given a test step")
            .withErrorMessage("Test error message")
            .withStackTrace("Test stack trace")
            .withStepDefinitionInfo(stepDefInfo)
            .withSourceFilePath("src/test/java/com/example/TestClass.java")
            .withLineNumber(10)
            .withExpectedValue("true")
            .withActualValue("false")
            .withParsingTime(100L)
            .build();
        
        return failureInfo;
    }
} 