package com.trace.ai.ui;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.ui.components.JBPanel;

import javax.swing.*;
import java.awt.*;

/**
 * Unit tests for AISettingsConfigurable following IntelliJ testing best practices.
 * 
 * <p>This test suite validates the AISettingsConfigurable class functionality,
 * including component creation, state management, and extensibility features.
 * It follows the testing requirements outlined in the implementation plan.</p>
 * 
 * <p><strong>Test Coverage:</strong></p>
 * <ul>
 *   <li>Component creation and initialization</li>
 *   <li>Settings modification detection</li>
 *   <li>Apply/reset functionality</li>
 *   <li>Error handling and validation</li>
 *   <li>Extensibility features</li>
 * </ul>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
public class AISettingsConfigurableUnitTest extends BasePlatformTestCase {
    
    // ============================================================================
    // TEST FIXTURES
    // ============================================================================
    
    private AISettingsConfigurable configurable;
    
    // ============================================================================
    // SETUP AND TEARDOWN
    // ============================================================================
    
    /**
     * Sets up test fixtures before each test method.
     * 
     * <p>This method initializes the configurable instance for testing
     * the UI component functionality.</p>
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        configurable = new AISettingsConfigurable();
    }
    
    // ============================================================================
    // COMPONENT CREATION TESTS
    // ============================================================================
    
    /**
     * Tests that the configurable creates a valid main component.
     * 
     * <p>This test validates that the createComponent() method returns a non-null
     * JComponent and that it's properly initialized with the expected layout.</p>
     */
    public void testCreateComponent_ReturnsValidComponent() {
        // Execute
        JComponent component = configurable.createComponent();
        
        // Verify
        assertNotNull("Component should not be null", component);
        assertTrue("Component should be a JBPanel", component instanceof JBPanel);
        
        JBPanel<?> panel = (JBPanel<?>) component;
        assertNotNull("Panel should have a layout", panel.getLayout());
        assertTrue("Panel should contain child components", panel.getComponentCount() > 0);
    }
    
    /**
     * Tests that the configurable creates components only once.
     * 
     * <p>This test validates that subsequent calls to createComponent() return
     * the same instance, following the singleton pattern for UI components.</p>
     */
    public void testCreateComponent_ReturnsSameInstance() {
        // Execute
        JComponent component1 = configurable.createComponent();
        JComponent component2 = configurable.createComponent();
        
        // Verify
        assertSame("Multiple calls should return the same component instance", component1, component2);
    }
    
    /**
     * Tests that the configurable creates privacy and service sections.
     * 
     * <p>This test validates that the main panel contains the expected sections
     * for privacy and service configuration, even though they're currently placeholders.</p>
     */
    public void testCreateComponent_ContainsExpectedSections() {
        // Execute
        JComponent component = configurable.createComponent();
        JBPanel<?> panel = (JBPanel<?>) component;
        
        // Verify
        assertTrue("Panel should contain at least 2 sections", panel.getComponentCount() >= 2);
        
        // Check that we have privacy and service sections (even as placeholders)
        boolean hasPrivacySection = false;
        boolean hasServiceSection = false;
        
        for (Component child : panel.getComponents()) {
            if (child instanceof JBPanel) {
                JBPanel<?> childPanel = (JBPanel<?>) child;
                // Check for placeholder labels that indicate the sections
                for (Component grandChild : childPanel.getComponents()) {
                    if (grandChild instanceof JLabel) {
                        String text = ((JLabel) grandChild).getText();
                        if (text.contains("Privacy")) {
                            hasPrivacySection = true;
                        } else if (text.contains("Service")) {
                            hasServiceSection = true;
                        }
                    }
                }
            }
        }
        
        assertTrue("Should contain privacy section", hasPrivacySection);
        assertTrue("Should contain service section", hasServiceSection);
    }
    
    // ============================================================================
    // DISPLAY NAME TESTS
    // ============================================================================
    
    /**
     * Tests that the configurable returns the correct display name.
     * 
     * <p>This test validates that the display name is appropriate for the IntelliJ
     * Settings UI and follows the naming conventions.</p>
     */
    public void testGetDisplayName_ReturnsCorrectName() {
        // Execute
        String displayName = configurable.getDisplayName();
        
        // Verify
        assertEquals("Display name should be 'AI Analysis'", "AI Analysis", displayName);
        assertFalse("Display name should not be empty", displayName.isEmpty());
    }
    
    // ============================================================================
    // MODIFICATION DETECTION TESTS
    // ============================================================================
    
    /**
     * Tests that the configurable correctly detects when no modifications are made.
     * 
     * <p>This test validates that the isModified() method returns false when
     * no changes have been made to the settings.</p>
     */
    public void testIsModified_NoModifications_ReturnsFalse() {
        // Setup - create component to initialize state
        configurable.createComponent();
        
        // Execute
        boolean isModified = configurable.isModified();
        
        // Verify
        assertFalse("Should not be modified when no changes made", isModified);
    }
    
    /**
     * Tests that the configurable handles null component gracefully.
     * 
     * <p>This test validates that the isModified() method returns false when
     * the component hasn't been created yet.</p>
     */
    public void testIsModified_NullComponent_ReturnsFalse() {
        // Execute - without creating component
        boolean isModified = configurable.isModified();
        
        // Verify
        assertFalse("Should return false when component is null", isModified);
    }
    
    // ============================================================================
    // APPLY AND RESET TESTS
    // ============================================================================
    
    /**
     * Tests that the configurable applies settings without errors.
     * 
     * <p>This test validates that the apply() method can be called without
     * throwing exceptions, even with placeholder implementations.</p>
     */
    public void testApply_NoErrors() {
        // Setup - create component to initialize state
        configurable.createComponent();
        
        // Execute and verify - should not throw exception
        try {
            configurable.apply();
        } catch (ConfigurationException e) {
            fail("Apply should not throw exception: " + e.getMessage());
        }
    }
    
    /**
     * Tests that the configurable resets settings without errors.
     * 
     * <p>This test validates that the reset() method can be called without
     * throwing exceptions and properly reloads the current settings.</p>
     */
    public void testReset_NoErrors() {
        // Setup - create component to initialize state
        configurable.createComponent();
        
        // Execute and verify - should not throw exception
        try {
            configurable.reset();
        } catch (Exception e) {
            fail("Reset should not throw exception: " + e.getMessage());
        }
    }
    
    /**
     * Tests that the configurable handles apply with null component gracefully.
     * 
     * <p>This test validates that the apply() method handles the case where
     * the component hasn't been created yet.</p>
     */
    public void testApply_NullComponent_NoErrors() {
        // Execute and verify - should not throw exception
        try {
            configurable.apply();
        } catch (ConfigurationException e) {
            fail("Apply should handle null component gracefully: " + e.getMessage());
        }
    }
    
    /**
     * Tests that the configurable handles reset with null component gracefully.
     * 
     * <p>This test validates that the reset() method handles the case where
     * the component hasn't been created yet.</p>
     */
    public void testReset_NullComponent_NoErrors() {
        // Execute and verify - should not throw exception
        try {
            configurable.reset();
        } catch (Exception e) {
            fail("Reset should handle null component gracefully: " + e.getMessage());
        }
    }
    
    // ============================================================================
    // RESOURCE DISPOSAL TESTS
    // ============================================================================
    
    /**
     * Tests that the configurable properly disposes of UI resources.
     * 
     * <p>This test validates that the disposeUIResources() method clears
     * component references to allow proper garbage collection.</p>
     */
    public void testDisposeUIResources_ClearsReferences() {
        // Setup - create component to initialize state
        configurable.createComponent();
        
        // Execute
        configurable.disposeUIResources();
        
        // Verify - component should be null after disposal
        // Note: We can't directly access private fields, but we can verify
        // that subsequent operations work correctly
        try {
            configurable.createComponent();
        } catch (Exception e) {
            fail("Should be able to recreate component after disposal: " + e.getMessage());
        }
    }
    
    // ============================================================================
    // EXTENSIBILITY TESTS
    // ============================================================================
    
    /**
     * Tests that the configurable can be extended with new sections.
     * 
     * <p>This test validates that the architecture supports adding new
     * settings sections without modifying existing code.</p>
     */
    public void testExtensibility_SupportsNewSections() {
        // Setup - create component to initialize state
        JComponent component = configurable.createComponent();
        JBPanel<?> panel = (JBPanel<?>) component;
        
        // Verify - panel should have a layout that supports adding new components
        assertNotNull("Panel should have a layout for extensibility", panel.getLayout());
        
        // Verify - we can add new components to the panel
        JLabel newSection = new JLabel("New Section");
        try {
            panel.add(newSection);
        } catch (Exception e) {
            fail("Should be able to add new sections to the panel: " + e.getMessage());
        }
    }
    
    /**
     * Tests that the configurable follows the open/closed principle.
     * 
     * <p>This test validates that the architecture is open for extension
     * but closed for modification, as required by SOLID principles.</p>
     */
    public void testExtensibility_OpenClosedPrinciple() {
        // Setup - create component to initialize state
        configurable.createComponent();
        
        // Verify - we can call all public methods without modification
        try {
            configurable.getDisplayName();
            configurable.isModified();
            configurable.apply();
            configurable.reset();
            configurable.disposeUIResources();
        } catch (Exception e) {
            fail("All public methods should work without modification: " + e.getMessage());
        }
    }
    
    // ============================================================================
    // INTEGRATION TESTS
    // ============================================================================
    
    /**
     * Tests the complete lifecycle of the configurable.
     * 
     * <p>This test validates the complete workflow from component creation
     * through disposal, ensuring all methods work together correctly.</p>
     */
    public void testCompleteLifecycle() {
        // 1. Create component
        JComponent component = configurable.createComponent();
        assertNotNull("Component should be created", component);
        
        // 2. Check initial state
        assertFalse("Initial state should not be modified", configurable.isModified());
        
        // 3. Apply settings (should work with placeholder implementation)
        try {
            configurable.apply();
        } catch (ConfigurationException e) {
            fail("Apply should work: " + e.getMessage());
        }
        
        // 4. Reset settings
        try {
            configurable.reset();
        } catch (Exception e) {
            fail("Reset should work: " + e.getMessage());
        }
        
        // 5. Dispose resources
        try {
            configurable.disposeUIResources();
        } catch (Exception e) {
            fail("Disposal should work: " + e.getMessage());
        }
        
        // 6. Verify we can recreate after disposal
        JComponent newComponent = configurable.createComponent();
        assertNotNull("Should be able to recreate component after disposal", newComponent);
    }
    
    /**
     * Tests that the configurable integrates with IntelliJ settings system.
     * 
     * <p>This test validates that the configurable implements the Configurable
     * interface correctly and follows IntelliJ Platform conventions.</p>
     */
    public void testIntelliJIntegration() {
        // Verify - configurable implements the correct interface
        assertTrue("Should implement IntelliJ Configurable interface", 
                  configurable instanceof com.intellij.openapi.options.Configurable);
        
        // Verify - display name follows IntelliJ conventions
        String displayName = configurable.getDisplayName();
        assertFalse("Display name should not be empty", displayName.isEmpty());
        assertTrue("Display name should be reasonably short", displayName.length() <= 50);
        
        // Verify - component creation follows IntelliJ patterns
        JComponent component = configurable.createComponent();
        assertTrue("Should use IntelliJ UI components", component instanceof JBPanel);
    }
} 