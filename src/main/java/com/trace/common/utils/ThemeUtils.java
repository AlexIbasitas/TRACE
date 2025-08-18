package com.trace.common.utils;

import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.ui.JBColor;

import javax.swing.*;
import java.awt.*;

/**
 * Theme-aware utilities for retrieving UI colors and formatting helpers.
 *
 * <p>Centralizes access to IntelliJ Look and Feel colors and editor scheme
 * defaults so UI components can adapt to light and dark themes without
 * hardcoded values.</p>
 */
public final class ThemeUtils {

    private ThemeUtils() {}

    /**
     * Returns a UI color by key with a fallback if the key is missing.
     */
    public static Color uiColor(String key, Color fallback) {
        try {
            Color c = UIManager.getColor(key);
            return c != null ? c : fallback;
        } catch (Exception ignore) {
            return fallback;
        }
    }

    /**
     * Returns a theme-aware background color suitable for code blocks.
     */
    public static JBColor codeBackground() {
        return new JBColor(new Color(245, 245, 245), new Color(30, 30, 30));
    }

    /**
     * Returns the editor scheme default foreground for code, falling back to label foreground.
     */
    public static Color codeForeground() {
        try {
            Color editorFg = EditorColorsManager.getInstance().getGlobalScheme().getDefaultForeground();
            if (editorFg != null) {
                return editorFg;
            }
        } catch (Throwable ignore) {
        }
        return textForeground();
    }

    /**
     * Returns the current panel background from LaF.
     */
    public static Color panelBackground() {
        // Try to get the current theme-aware panel background
        Color panelBg = uiColor("Panel.background", null);
        if (panelBg != null) {
            return panelBg;
        }
        
        // Fallback to JBColor which automatically adapts to light/dark theme
        return new JBColor(new Color(245, 245, 245), new Color(43, 43, 43));
    }

    /**
     * Returns the current label/text foreground from LaF.
     */
    public static Color textForeground() {
        return uiColor("Label.foreground", new JBColor(new Color(0x1F1F1F), Color.WHITE));
    }

    /**
     * Returns a theme-aware background for text fields/areas.
     */
    public static Color textFieldBackground() {
        // Special handling for high contrast themes - ensure input box is lighter than surroundings
        Color defaultColor = uiColor("TextField.background", new JBColor(Color.WHITE, new Color(50, 50, 50)));
        
        // Check if we're in a high contrast theme by looking at the panel background
        Color panelBg = panelBackground();
        if (panelBg != null) {
            // If panel background is very dark (high contrast), make input box lighter
            if (isHighContrastTheme(panelBg)) {
                return new JBColor(Color.WHITE, new Color(80, 80, 80)); // Lighter grey for dark high contrast
            }
        }
        
        return defaultColor;
    }
    
    /**
     * Detects if we're in a high contrast theme based on panel background color.
     */
    private static boolean isHighContrastTheme(Color panelBg) {
        if (panelBg == null) return false;
        
        // High contrast themes typically have very dark backgrounds
        // Check if the panel background is very dark (RGB values all low)
        int brightness = (panelBg.getRed() + panelBg.getGreen() + panelBg.getBlue()) / 3;
        return brightness < 30; // Very dark background indicates high contrast theme
    }

    /**
     * Returns a theme-aware inline code background.
     */
    public static JBColor inlineCodeBackground() {
        return new JBColor(new Color(235, 235, 235), new Color(30, 30, 30));
    }

    /**
     * Converts a color to a hex string like #RRGGBB.
     */
    public static String toHex(Color c) {
        if (c == null) return "#000000";
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }
}



