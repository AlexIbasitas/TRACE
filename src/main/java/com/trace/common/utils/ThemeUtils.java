package com.trace.common.utils;

import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.ui.JBColor;

import javax.swing.*;
import java.awt.*;

/**
 * Theme-aware utilities for retrieving UI colors and formatting helpers.
 *
 * <p>This class centralizes access to IntelliJ Look and Feel colors and editor scheme
 * defaults so UI components can adapt to light and dark themes without hardcoded values.
 * It uses dynamic color resolution for automatic theme updates and provides fallback
 * mechanisms for robust color handling.</p>
 * 
 * <p>All color methods in this class are designed to work seamlessly with IntelliJ's
 * theme system and provide appropriate fallbacks for different IDE versions and themes.</p>
 * 
 * @author Alex Ibasitas
 * @version 1.0
 * @since 1.0
 */
public final class ThemeUtils {

    /**
     * Private constructor to prevent instantiation.
     * This is a utility class and should not be instantiated.
     */
    private ThemeUtils() {}

    /**
     * Returns a UI color by key with a fallback if the key is missing.
     * 
     * <p>This method safely retrieves colors from UIManager with proper error handling.
     * It uses dynamic resolution for automatic theme updates and provides a fallback
     * color if the requested key is not available.</p>
     * 
     * @param key the UIManager color key to retrieve
     * @param fallback the fallback color to use if the key is missing
     * @return the UI color or fallback if not available
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
     * 
     * <p>This is the preferred method for components that need theme-aware colors.
     * It creates a JBColor that automatically adapts to theme changes and provides
     * appropriate light and dark theme fallbacks.</p>
     * 
     * @param key the UIManager color key to use
     * @param lightFallback the fallback color for light themes
     * @param darkFallback the fallback color for dark themes
     * @return a JBColor that adapts to theme changes
     */
    public static JBColor dynamicColor(String key, Color lightFallback, Color darkFallback) {
        return JBColor.namedColor(key, new JBColor(lightFallback, darkFallback));
    }

    /**
     * Returns a theme-aware background color suitable for code blocks.
     * 
     * <p>This method provides a JBColor that automatically adapts to light and dark
     * themes with appropriate contrast for code display.</p>
     * 
     * @return a theme-aware code background color
     */
    public static JBColor codeBackground() {
        return new JBColor(new Color(245, 245, 245), new Color(30, 30, 30));
    }

    /**
     * Returns the editor scheme default foreground for code, falling back to label foreground.
     * 
     * <p>This method attempts to use the editor's default foreground color for optimal
     * code display, falling back to the standard label foreground if the editor scheme
     * is not available.</p>
     * 
     * @return the code foreground color
     */
    public static Color codeForeground() {
        try {
            Color editorFg = EditorColorsManager.getInstance().getGlobalScheme().getDefaultForeground();
            if (editorFg != null) {
                return editorFg;
            }
        } catch (Throwable ignore) {
            // Fall back to text foreground if editor scheme is not available
        }
        return textForeground();
    }

    /**
     * Returns the current panel background from LaF using dynamic resolution.
     * 
     * <p>This method uses JBColor.lazy() for true dynamic color resolution that updates
     * automatically when the theme changes. It first attempts to use UIManager's
     * Panel.background color, falling back to appropriate light/dark theme defaults.</p>
     * 
     * @return the current panel background color
     */
    public static Color panelBackground() {
        return JBColor.lazy(() -> {
            Color uiColor = UIManager.getColor("Panel.background");
            return uiColor != null ? uiColor : new JBColor(new Color(245, 245, 245), new Color(43, 43, 43));
        });
    }

    /**
     * Returns the current label/text foreground from LaF using dynamic resolution.
     * 
     * <p>This method uses JBColor.lazy() for true dynamic color resolution that updates
     * automatically when the theme changes. It first attempts to use UIManager's
     * Label.foreground color, falling back to appropriate light/dark theme defaults.</p>
     * 
     * @return the current text foreground color
     */
    public static Color textForeground() {
        return JBColor.lazy(() -> {
            Color uiColor = UIManager.getColor("Label.foreground");
            return uiColor != null ? uiColor : new JBColor(new Color(0x1F1F1F), new Color(255, 255, 255));
        });
    }

    /**
     * Returns a theme-aware background for text fields/areas using dynamic resolution.
     * 
     * <p>This method uses JBColor.lazy() for true dynamic color resolution that updates
     * automatically when the theme changes. It first attempts to use UIManager's
     * TextField.background color, falling back to appropriate light/dark theme defaults.</p>
     * 
     * @return the current text field background color
     */
    public static Color textFieldBackground() {
        return JBColor.lazy(() -> {
            Color uiColor = UIManager.getColor("TextField.background");
            return uiColor != null ? uiColor : new JBColor(new Color(255, 255, 255), new Color(50, 50, 50));
        });
    }
    
    /**
     * Returns theme-aware border color using dynamic resolution.
     * 
     * <p>This method creates a JBColor that automatically adapts to theme changes
     * using the Component.borderColor with appropriate light and dark theme fallbacks.</p>
     * 
     * @return a theme-aware border color
     */
    public static Color borderColor() {
        return JBColor.namedColor("Component.borderColor", 
            new JBColor(new Color(192, 192, 192), new Color(80, 80, 80)));
    }
    
    /**
     * Detects if we're in a high contrast theme based on panel background color.
     * 
     * <p>This method analyzes the panel background color to determine if the current
     * theme is a high contrast theme. High contrast themes typically have very dark
     * backgrounds with RGB values all below a certain threshold.</p>
     * 
     * @param panelBg the panel background color to analyze
     * @return true if the theme appears to be high contrast, false otherwise
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
     * 
     * <p>This method provides a JBColor that automatically adapts to light and dark
     * themes with appropriate contrast for inline code display.</p>
     * 
     * @return a theme-aware inline code background color
     */
    public static JBColor inlineCodeBackground() {
        return new JBColor(new Color(235, 235, 235), new Color(30, 30, 30));
    }

    /**
     * Converts a color to a hex string like #RRGGBB.
     * 
     * <p>This utility method converts a Color object to its hexadecimal representation
     * in the standard #RRGGBB format. It handles null colors by returning a default
     * black color.</p>
     * 
     * @param c the color to convert
     * @return the hexadecimal color string in #RRGGBB format
     */
    public static String toHex(Color c) {
        if (c == null) return "#000000";
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }
}



