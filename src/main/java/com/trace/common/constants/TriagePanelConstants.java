package com.trace.common.constants;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/**
 * Constants for the TRACE UI components.
 * 
 * <p>This class centralizes all hardcoded values used throughout the TRACE
 * user interface, including colors, fonts, dimensions, icon paths, borders,
 * and spacing values. This ensures consistency across all UI components and
 * makes it easier to maintain and modify the visual appearance.</p>
 * 
 * <p>All colors are now theme-aware using JBColor and UIUtil to ensure
 * compatibility with IntelliJ 2025.2.+ and future versions.</p>
 * 
 * @author Alex Ibasitas
 * @version 2.0
 * @since 1.0
 */
public final class TriagePanelConstants {
    
    // Private constructor to prevent instantiation
    private TriagePanelConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    
    // ============================================================================
    // THEME-AWARE COLOR CONSTANTS
    // ============================================================================
    
    /**
     * Gets the theme-aware panel background color using dynamic resolution.
     * 
     * <p>This method returns a JBColor that automatically adapts to the current
     * IDE theme. It first attempts to use the UIManager's Panel.background color,
     * falling back to appropriate light/dark theme defaults if not available.</p>
     * 
     * @return The theme-aware panel background color
     */
    public static JBColor getPanelBackground() {
        return JBColor.lazy(() -> {
            Color uiColor = UIManager.getColor("Panel.background");
            return uiColor != null ? uiColor : new JBColor(new Color(245, 245, 245), new Color(43, 43, 43));
        });
    }
    
    /**
     * Gets the theme-aware input container background color using dynamic resolution.
     * 
     * <p>This method returns a JBColor that automatically adapts to the current
     * IDE theme. It first attempts to use the UIManager's TextField.background color,
     * falling back to appropriate light/dark theme defaults if not available.</p>
     * 
     * @return The theme-aware input container background color
     */
    public static JBColor getInputContainerBackground() {
        return JBColor.lazy(() -> {
            Color uiColor = UIManager.getColor("TextField.background");
            if (uiColor != null) {
                return uiColor;
            }
            // Enhanced fallback with better high contrast support
            return new JBColor(
                new Color(255, 255, 255),  // Light theme: white
                new Color(80, 80, 80)      // Dark theme: lighter grey for better visibility
            );
        });
    }
    
    /**
     * Gets the theme-aware input container border color using dynamic resolution.
     * 
     * <p>This method returns a JBColor that automatically adapts to the current
     * IDE theme. It first attempts to use the UIManager's Component.borderColor,
     * falling back to appropriate light/dark theme defaults if not available.</p>
     * 
     * @return The theme-aware input container border color
     */
    public static JBColor getInputContainerBorder() {
        return JBColor.lazy(() -> {
            Color uiColor = UIManager.getColor("Component.borderColor");
            if (uiColor != null) {
                return uiColor;
            }
            // Enhanced fallback with better visibility
            return new JBColor(
                new Color(180, 180, 180),  // Light theme: medium grey
                new Color(100, 100, 100)   // Dark theme: lighter grey for better contrast
            );
        });
    }
    
    /**
     * Gets the theme-aware header border color using dynamic resolution.
     * 
     * <p>This method returns a JBColor that automatically adapts to the current
     * IDE theme. It first attempts to use the UIManager's Component.borderColor,
     * falling back to appropriate light/dark theme defaults if not available.</p>
     * 
     * @return The theme-aware header border color
     */
    public static JBColor getHeaderBorder() {
        return JBColor.lazy(() -> {
            Color uiColor = UIManager.getColor("Component.borderColor");
            return uiColor != null ? uiColor : new JBColor(new Color(68, 68, 68), new Color(68, 68, 68));
        });
    }
    
    /**
     * Gets the theme-aware header text color using dynamic resolution.
     * 
     * <p>This method returns a JBColor that automatically adapts to the current
     * IDE theme using the Label.foreground color with appropriate fallbacks.</p>
     * 
     * @return The theme-aware header text color
     */
    public static JBColor getHeaderText() {
        return JBColor.namedColor("Label.foreground", 
            new JBColor(new Color(31, 31, 31), new Color(255, 255, 255)));
    }
    
    /**
     * Gets the theme-aware white color for text.
     * 
     * <p>Note: Use getTextForeground() instead for general text color needs.
     * This method is provided for specific cases requiring white text.</p>
     * 
     * @return The theme-aware white color
     */
    public static JBColor getWhite() {
        return JBColor.namedColor("Label.foreground", 
            new JBColor(new Color(255, 255, 255), new Color(255, 255, 255)));
    }
    
    /**
     * Gets the theme-aware timestamp text color using dynamic resolution.
     * 
     * <p>This method returns a JBColor that automatically adapts to the current
     * IDE theme using the Label.infoForeground color with appropriate fallbacks.</p>
     * 
     * @return The theme-aware timestamp text color
     */
    public static JBColor getTimestampColor() {
        return JBColor.namedColor("Label.infoForeground", 
            new JBColor(new Color(150, 150, 150), new Color(150, 150, 150)));
    }
    
    /** Theme-aware scenario label color (orange) */
    public static final JBColor SCENARIO_COLOR = new JBColor(
        new Color(255, 152, 0),  // Material Design Orange
        new Color(255, 152, 0)
    );
    
    /** Theme-aware failure indicator color (red) */
    public static final JBColor FAILURE_COLOR = new JBColor(
        JBColor.RED,
        JBColor.RED
    );
    
    /** Theme-aware error foreground color for error states */
    public static final JBColor ERROR_FOREGROUND = new JBColor(
        JBColor.RED,
        JBColor.RED
    );
    
    /** Theme-aware warning foreground color for warning states */
    public static final JBColor WARNING_FOREGROUND = new JBColor(
        new Color(255, 193, 7),  // Material Design Amber
        new Color(255, 193, 7)
    );
    
    /** Theme-aware collapsible panel content text color */
    public static final JBColor COLLAPSIBLE_TEXT_COLOR = new JBColor(
        new Color(31, 31, 31),  // Light theme label foreground
        new Color(255, 255, 255)  // Dark theme label foreground
    );
    
    /** Theme-aware collapsible panel content background color */
    public static final JBColor COLLAPSIBLE_BACKGROUND = new JBColor(
        new Color(255, 255, 255),  // Light theme text field background
        new Color(50, 50, 50)      // Dark theme text field background
    );
    
    /** Theme-aware collapsible panel content border color */
    public static final JBColor COLLAPSIBLE_BORDER = new JBColor(
        new Color(80, 80, 80),  // Light theme border
        new Color(80, 80, 80)   // Dark theme border
    );
    
    /** Theme-aware transparent color for buttons */
    public static final JBColor TRANSPARENT = new JBColor(
        new Color(0, 0, 0, 0),
        new Color(0, 0, 0, 0)
    );
    
    /** Theme-aware hover overlay color for buttons */
    public static final JBColor HOVER_OVERLAY = new JBColor(
        new Color(0, 0, 0, 30),  // Dark overlay for light theme
        new Color(255, 255, 255, 30)  // Light overlay for dark theme
    );
    
    /** Theme-aware press overlay color for buttons */
    public static final JBColor PRESS_OVERLAY = new JBColor(
        new Color(0, 0, 0, 50),  // Darker overlay for light theme
        new Color(0, 0, 0, 40)   // Dark overlay for dark theme
    );
    
    // ============================================================================
    // FONT CONSTANTS
    // ============================================================================
    
    /** Default font family */
    public static final String FONT_FAMILY = "Segoe UI";
    
    /**
     * Gets the input area font using IDE's default font size for consistency.
     * 
     * @return The input area font
     */
    public static Font getInputFont() {
        return UIUtil.getLabelFont();
    }
    
    /**
     * Gets the header title font using IDE's default font size for consistency.
     * 
     * @return The header title font
     */
    public static Font getHeaderTitleFont() {
        return UIUtil.getLabelFont();
    }
    
    /**
     * Gets the header button font using IDE's default font size for consistency.
     * 
     * @return The header button font
     */
    public static Font getHeaderButtonFont() {
        return UIUtil.getLabelFont();
    }
    
    /**
     * Gets the settings placeholder font using IDE's default font size for consistency.
     * 
     * @return The settings placeholder font
     */
    public static Font getSettingsPlaceholderFont() {
        return UIUtil.getLabelFont();
    }
    
    /**
     * Gets the settings button font using IDE's default font size for consistency.
     * 
     * @return The settings button font
     */
    public static Font getSettingsButtonFont() {
        return UIUtil.getLabelFont();
    }
    
    /**
     * Gets the send button font using IDE's default font size for consistency.
     * 
     * @return The send button font in bold
     */
    public static Font getSendButtonFont() {
        Font baseFont = UIUtil.getLabelFont();
        return baseFont.deriveFont(Font.BOLD);
    }
    
    /**
     * Gets the message sender font using IDE's default font size for consistency.
     * 
     * @return The message sender font in bold
     */
    public static Font getSenderFont() {
        Font baseFont = UIUtil.getLabelFont();
        return baseFont.deriveFont(Font.BOLD);
    }
    
    /**
     * Gets the message timestamp font using IDE's default font size for consistency.
     * 
     * @return The message timestamp font (slightly smaller than base)
     */
    public static Font getTimestampFont() {
        Font baseFont = UIUtil.getLabelFont();
        return baseFont.deriveFont(baseFont.getSize() - 1f); // Slightly smaller than base
    }
    
    /**
     * Gets the scenario label font using IDE's default font size for consistency.
     * 
     * @return The scenario label font in bold
     */
    public static Font getScenarioFont() {
        Font baseFont = UIUtil.getLabelFont();
        return baseFont.deriveFont(Font.BOLD);
    }
    
    /**
     * Gets the message text font using IDE's default font size for consistency.
     * 
     * @return The message text font
     */
    public static Font getMessageFont() {
        return UIUtil.getLabelFont();
    }
    
    /**
     * Gets the collapsible toggle font using IDE's default font size for consistency.
     * 
     * @return The collapsible toggle font in bold
     */
    public static Font getCollapsibleToggleFont() {
        Font baseFont = UIUtil.getLabelFont();
        return baseFont.deriveFont(Font.BOLD);
    }
    
    /**
     * Gets the collapsible content font using IDE's default font size for consistency.
     * 
     * @return The collapsible content font (slightly smaller than base)
     */
    public static Font getCollapsibleContentFont() {
        Font baseFont = UIUtil.getLabelFont();
        return baseFont.deriveFont(baseFont.getSize() - 1f); // Slightly smaller than base
    }
    
    // ============================================================================
    // DIMENSION CONSTANTS
    // ============================================================================
    
    /** Button container size - accommodates button (40x40) plus border padding (2px each side) */
    public static final Dimension BUTTON_CONTAINER_SIZE = new Dimension(44, 44);
    
    /** Send button size */
    public static final Dimension SEND_BUTTON_SIZE = new Dimension(32, 32);
    
    /** Maximum width for message text */
    public static final int MAX_MESSAGE_TEXT_WIDTH = 600;
    
    /** Minimum width before horizontal scrollbar appears (soft wrapping threshold) */
    public static final int MIN_CHAT_WIDTH_BEFORE_SCROLL = 300;
    
    /** Minimum width before settings horizontal scrollbar appears (following JetBrains guidelines) */
    public static final int MIN_SETTINGS_WIDTH_BEFORE_SCROLL = 300;
    
    /** Maximum width for collapsible content */
    public static final int MAX_COLLAPSIBLE_CONTENT_WIDTH = 550;
    
    /** Maximum dimension for expandable components */
    public static final Dimension MAX_EXPANDABLE_SIZE = new Dimension(Integer.MAX_VALUE, Short.MAX_VALUE);
    
    // ============================================================================
    // ICON PATH CONSTANTS
    // ============================================================================
    
    /** User profile icon path */
    public static final String USER_ICON_PATH = "/icons/user_profile_24.png";
    
    /** AI logo icon path */
    public static final String AI_ICON_PATH = "/icons/logo_24.png";
    
    /** Send button icon path */
    public static final String SEND_ICON_PATH = "/icons/send_24.png";
    
    // ============================================================================
    // BORDER CONSTANTS
    // ============================================================================
    
    /** Empty border for general spacing */
    public static final Border EMPTY_BORDER = BorderFactory.createEmptyBorder();
    
    /** Input panel border */
    public static final Border INPUT_PANEL_BORDER = BorderFactory.createEmptyBorder(8, 16, 16, 16);
    
    /** Input container border - DEPRECATED: Use theme-aware borders instead */
    public static final Border INPUT_CONTAINER_BORDER_COMPOUND = BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(getInputContainerBorder(), 1, true),
        BorderFactory.createEmptyBorder(8, 12, 8, 0)
    );
    
    /** Message container border */
    public static final Border MESSAGE_CONTAINER_BORDER = BorderFactory.createEmptyBorder(8, 16, 8, 16);
    
    /** Header border compound */
    public static final Border HEADER_BORDER_COMPOUND = BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder(0, 0, 1, 0, getHeaderBorder()),
        BorderFactory.createEmptyBorder(8, 16, 8, 16)
    );
    
    /** Settings button border */
    public static final Border SETTINGS_BUTTON_BORDER = BorderFactory.createEmptyBorder(0, 8, 0, 0);
    
    /** Send button border - no padding (container provides spacing) */
    public static final Border SEND_BUTTON_BORDER = BorderFactory.createEmptyBorder(0, 0, 0, 0);
    
    /** Message component border */
    public static final Border MESSAGE_COMPONENT_BORDER = BorderFactory.createEmptyBorder(8, 0, 8, 0);
    
    /** Message header border */
    public static final Border MESSAGE_HEADER_BORDER = BorderFactory.createEmptyBorder(0, 0, 8, 0);
    
    /** Message logo border */
    public static final Border MESSAGE_LOGO_BORDER = BorderFactory.createEmptyBorder(0, 0, 0, 8);
    
    /** Scenario panel border */
    public static final Border SCENARIO_PANEL_BORDER = BorderFactory.createEmptyBorder(0, 0, 8, 0);
    
    /** Failed step panel border */
    public static final Border FAILED_STEP_PANEL_BORDER = BorderFactory.createEmptyBorder(0, 0, 8, 0);
    
    /** Collapsible panel border */
    public static final Border COLLAPSIBLE_PANEL_BORDER = BorderFactory.createEmptyBorder(4, 0, 4, 0);
    
    /** Collapsible content border */
    public static final Border COLLAPSIBLE_CONTENT_BORDER = BorderFactory.createEmptyBorder(8, 16, 0, 0);
    
    /** Collapsible text area border compound */
    public static final Border COLLAPSIBLE_TEXT_BORDER_COMPOUND = BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(COLLAPSIBLE_BORDER, 1),
        BorderFactory.createEmptyBorder(8, 8, 8, 8)
    );
    
    // ============================================================================
    // SPACING CONSTANTS
    // ============================================================================
    
    /** Small spacing (4px) */
    public static final int SPACING_SMALL = 4;
    
    /** Medium spacing (8px) */
    public static final int SPACING_MEDIUM = 8;
    
    /** Large spacing (12px) */
    public static final int SPACING_LARGE = 12;
    
    /** Extra large spacing (16px) */
    public static final int SPACING_XLARGE = 16;
    
    /** Component spacing for message components */
    public static final int COMPONENT_SPACING = 16;
    
    /** Message spacing between messages */
    public static final int MESSAGE_SPACING = 8;
    
    /** Content padding for text areas */
    public static final int CONTENT_PADDING = 4;
    
    /** Content indent for collapsible panels */
    public static final int CONTENT_INDENT = 16;
    
    // ============================================================================
    // TEXT CONSTANTS
    // ============================================================================
    
    /** Unknown scenario text */
    public static final String UNKNOWN_SCENARIO = "Unknown Scenario";
    
    /** User display name */
    public static final String USER_DISPLAY_NAME = "You";
    
    /** AI display name */
    public static final String AI_DISPLAY_NAME = "TRACE";
    
    /** Scenario prefix text */
    public static final String SCENARIO_PREFIX = "Scenario: ";
    
    /** Failed step prefix text */
    public static final String FAILED_STEP_PREFIX = "Failed Step: ";
    
    /** Failure symbol */
    public static final String FAILURE_SYMBOL = "✗ ";
    
    /** Expand icon */
    public static final String EXPAND_ICON = "▶ ";
    
    /** Collapse icon */
    public static final String COLLAPSE_ICON = "▼ ";
    
    /** Toggle text */
    public static final String TOGGLE_TEXT = "Show AI Thinking";
    
    /** Expand tooltip */
    public static final String TOOLTIP_EXPAND = "Click to show AI thinking";
    
    /** Collapse tooltip */
    public static final String TOOLTIP_COLLAPSE = "Click to hide AI thinking";
    
    /** Settings button text */
    public static final String SETTINGS_BUTTON_TEXT = "⚙";
    
    /** Settings button tooltip */
    public static final String SETTINGS_BUTTON_TOOLTIP = "Settings";
    
    /** Header title text */
    public static final String HEADER_TITLE_TEXT = "TRACE";
    
    /** Settings placeholder text */
    public static final String SETTINGS_PLACEHOLDER_TEXT = "Settings page (placeholder)";
    
    /** Back to chat button text */
    public static final String BACK_TO_CHAT_TEXT = "Back to Chat";
    
    /** Send button fallback text */
    public static final String SEND_BUTTON_FALLBACK_TEXT = "→";
    
    /** Send button tooltip */
    public static final String SEND_BUTTON_TOOLTIP = "Send message";
    
    /** Input placeholder text */
    public static final String INPUT_PLACEHOLDER_TEXT = "Ask anything about the test failure...";
    
    // ============================================================================
    // LAYOUT CONSTANTS
    // ============================================================================
    
    /** Estimated line height for text */
    public static final int ESTIMATED_LINE_HEIGHT = 18;
    
    /** Characters per line for text wrapping */
    public static final int CHARS_PER_LINE = 80;
    
    /** Scroll bar unit increment */
    public static final int SCROLL_BAR_UNIT_INCREMENT = 16;
    
    /** Input area rows */
    public static final int INPUT_AREA_ROWS = 3;
    
    // ============================================================================
    // UTILITY METHODS
    // ============================================================================
    
    /**
     * Gets the default panel background color using dynamic theme resolution.
     * 
     * <p>This method provides a fallback mechanism for panel background color
     * resolution. It first attempts to get the color from UIManager, then
     * falls back to the theme-aware getPanelBackground() method.</p>
     * 
     * @return The panel background color
     */
    public static Color getPanelBackgroundFallback() {
        Color panelBg = UIManager.getColor("Panel.background");
        return panelBg != null ? panelBg : getPanelBackground();
    }
    
    /**
     * Creates a compound border with the specified outer and inner borders.
     * 
     * @param outerBorder The outer border
     * @param innerBorder The inner border
     * @return The compound border
     */
    public static Border createCompoundBorder(Border outerBorder, Border innerBorder) {
        return BorderFactory.createCompoundBorder(outerBorder, innerBorder);
    }
    
    /**
     * Creates an empty border with the specified insets.
     * 
     * @param top The top inset
     * @param left The left inset
     * @param bottom The bottom inset
     * @param right The right inset
     * @return The empty border
     */
    public static Border createEmptyBorder(int top, int left, int bottom, int right) {
        return BorderFactory.createEmptyBorder(top, left, bottom, right);
    }
    
    /**
     * Creates a line border with the specified color and thickness.
     * 
     * @param color The border color
     * @param thickness The border thickness
     * @param rounded Whether the border should be rounded
     * @return The line border
     */
    public static Border createLineBorder(Color color, int thickness, boolean rounded) {
        return BorderFactory.createLineBorder(color, thickness, rounded);
    }
    
    /**
     * Creates a matte border with the specified insets and color.
     * 
     * @param top The top inset
     * @param left The left inset
     * @param bottom The bottom inset
     * @param right The right inset
     * @param color The border color
     * @return The matte border
     */
    public static Border createMatteBorder(int top, int left, int bottom, int right, Color color) {
        return BorderFactory.createMatteBorder(top, left, bottom, right, color);
    }
} 