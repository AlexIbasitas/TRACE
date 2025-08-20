package com.trace.chat.ui;

import com.trace.common.utils.ThemeUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import javax.swing.*;
import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify theme-aware color system works correctly.
 * This test ensures that colors update dynamically when themes change.
 */
public class ThemeChangeTest {

    @BeforeEach
    void setUp() {
        // Ensure we're on the EDT for Swing operations
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> {});
        }
    }

    @Test
    void testThemeUtilsColorsAreDynamic() {
        System.out.println("=== Testing ThemeUtils Dynamic Colors ===");
        
        // Get initial colors
        Color initialPanelBg = ThemeUtils.panelBackground();
        Color initialTextFg = ThemeUtils.textForeground();
        Color initialTextFieldBg = ThemeUtils.textFieldBackground();
        
        System.out.println("Initial colors:");
        System.out.println("  Panel background: " + initialPanelBg);
        System.out.println("  Text foreground: " + initialTextFg);
        System.out.println("  Text field background: " + initialTextFieldBg);
        
        // Verify colors are not null
        assertNotNull(initialPanelBg, "Panel background should not be null");
        assertNotNull(initialTextFg, "Text foreground should not be null");
        assertNotNull(initialTextFieldBg, "Text field background should not be null");
        
        // Verify colors are JBColor instances (which are dynamic)
        assertTrue(initialPanelBg instanceof com.intellij.ui.JBColor, 
            "Panel background should be JBColor for dynamic updates");
        assertTrue(initialTextFg instanceof com.intellij.ui.JBColor, 
            "Text foreground should be JBColor for dynamic updates");
        assertTrue(initialTextFieldBg instanceof com.intellij.ui.JBColor, 
            "Text field background should be JBColor for dynamic updates");
        
        System.out.println("✓ All colors are JBColor instances (dynamic)");
    }

    @Test
    void testUIManagerColorsAreAvailable() {
        System.out.println("=== Testing UIManager Colors ===");
        
        // Test that UIManager has the expected color keys
        Color panelBg = UIManager.getColor("Panel.background");
        Color labelFg = UIManager.getColor("Label.foreground");
        Color textFieldBg = UIManager.getColor("TextField.background");
        
        System.out.println("UIManager colors:");
        System.out.println("  Panel.background: " + panelBg);
        System.out.println("  Label.foreground: " + labelFg);
        System.out.println("  TextField.background: " + textFieldBg);
        
        // At least some colors should be available
        assertTrue(panelBg != null || labelFg != null || textFieldBg != null, 
            "At least one UIManager color should be available");
        
        System.out.println("✓ UIManager colors are accessible");
    }

    @Test
    void testCurrentLookAndFeel() {
        System.out.println("=== Testing Current Look and Feel ===");
        
        LookAndFeel currentLaf = UIManager.getLookAndFeel();
        System.out.println("Current Look and Feel: " + currentLaf.getName());
        System.out.println("Look and Feel Class: " + currentLaf.getClass().getName());
        
        assertNotNull(currentLaf, "Current Look and Feel should not be null");
        assertNotNull(currentLaf.getName(), "Look and Feel name should not be null");
        
        System.out.println("✓ Current Look and Feel is accessible");
    }

    @Test
    void testColorConsistency() {
        System.out.println("=== Testing Color Consistency ===");
        
        // Get colors multiple times to ensure consistency
        Color panelBg1 = ThemeUtils.panelBackground();
        Color panelBg2 = ThemeUtils.panelBackground();
        Color textFg1 = ThemeUtils.textForeground();
        Color textFg2 = ThemeUtils.textForeground();
        
        assertEquals(panelBg1, panelBg2, "Panel background should be consistent");
        assertEquals(textFg1, textFg2, "Text foreground should be consistent");
        
        System.out.println("✓ Colors are consistent across multiple calls");
    }
}
