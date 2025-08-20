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
 * hardcoded values. Uses dynamic color resolution for automatic theme updates.</p>
 */
public final class ThemeUtils {

    private ThemeUtils() {}

    /**
     * Returns a UI color by key with a fallback if the key is missing.
     * Uses dynamic resolution for automatic theme updates.
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
     * Returns a dynamically resolved color that updates with theme changes.
     * This is the preferred method for components that need theme-aware colors.
     */
    public static JBColor dynamicColor(String key, Color lightFallback, Color darkFallback) {
        return JBColor.namedColor(key, new JBColor(lightFallback, darkFallback));
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
     * Returns the current panel background from LaF using dynamic resolution.
     * Uses JBColor.lazy() for true dynamic color resolution that updates automatically.
     */
    public static Color panelBackground() {
        return JBColor.lazy(() -> {
            Color uiColor = UIManager.getColor("Panel.background");
            return uiColor != null ? uiColor : new JBColor(new Color(245, 245, 245), new Color(43, 43, 43));
        });
    }

    /**
     * Returns the current label/text foreground from LaF using dynamic resolution.
     * Uses JBColor.lazy() for true dynamic color resolution that updates automatically.
     */
    public static Color textForeground() {
        return JBColor.lazy(() -> {
            Color uiColor = UIManager.getColor("Label.foreground");
            return uiColor != null ? uiColor : new JBColor(new Color(0x1F1F1F), new Color(255, 255, 255));
        });
    }

    /**
     * Returns a theme-aware background for text fields/areas using dynamic resolution.
     * Uses JBColor.lazy() for true dynamic color resolution that updates automatically.
     */
    public static Color textFieldBackground() {
        return JBColor.lazy(() -> {
            Color uiColor = UIManager.getColor("TextField.background");
            return uiColor != null ? uiColor : new JBColor(new Color(255, 255, 255), new Color(50, 50, 50));
        });
    }
    
    /**
     * Returns theme-aware border color using dynamic resolution.
     */
    public static Color borderColor() {
        return JBColor.namedColor("Component.borderColor", 
            new JBColor(new Color(192, 192, 192), new Color(80, 80, 80)));
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



