package com.trace.common.constants;

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
 * <p>All constants are organized into logical groups for better readability
 * and maintenance. Colors follow a dark theme suitable for IDE integration.</p>
 * 
 * @author Alex Ibasitas
 * @version 1.0
 * @since 1.0
 */
public final class TriagePanelConstants {
    
    // Private constructor to prevent instantiation
    private TriagePanelConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    
    // ============================================================================
    // COLOR CONSTANTS
    // ============================================================================
    
    /** Default panel background color */
    public static final Color PANEL_BACKGROUND = new Color(43, 43, 43);
    
    /** Input container background color */
    public static final Color INPUT_CONTAINER_BACKGROUND = new Color(50, 50, 50);
    
    /** Input container border color */
    public static final Color INPUT_CONTAINER_BORDER = new Color(60, 60, 60);
    
    /** Header border color */
    public static final Color HEADER_BORDER = new Color(68, 68, 68);
    
    /** Header text color */
    public static final Color HEADER_TEXT = new Color(180, 180, 180);
    
    /** White color for text */
    public static final Color WHITE = Color.WHITE;
    
    /** Timestamp text color */
    public static final Color TIMESTAMP_COLOR = new Color(150, 150, 150);
    
    /** Scenario label color (orange) */
    public static final Color SCENARIO_COLOR = new Color(255, 152, 0);
    
    /** Failure indicator color (red) */
    public static final Color FAILURE_COLOR = new Color(255, 100, 100);
    
    /** Error foreground color for error states */
    public static final Color ERROR_FOREGROUND = new Color(255, 100, 100);
    
    /** Collapsible panel content text color */
    public static final Color COLLAPSIBLE_TEXT_COLOR = new Color(200, 200, 200);
    
    /** Collapsible panel content background color */
    public static final Color COLLAPSIBLE_BACKGROUND = new Color(50, 50, 50);
    
    /** Collapsible panel content border color */
    public static final Color COLLAPSIBLE_BORDER = new Color(80, 80, 80);
    
    /** Transparent color for buttons */
    public static final Color TRANSPARENT = new Color(0, 0, 0, 0);
    
    /** Hover overlay color for buttons */
    public static final Color HOVER_OVERLAY = new Color(255, 255, 255, 30);
    
    /** Press overlay color for buttons */
    public static final Color PRESS_OVERLAY = new Color(0, 0, 0, 20);
    
    // ============================================================================
    // FONT CONSTANTS
    // ============================================================================
    
    /** Default font family */
    public static final String FONT_FAMILY = "Segoe UI";
    
    /** Input area font */
    public static final Font INPUT_FONT = new Font(FONT_FAMILY, Font.PLAIN, 14);
    
    /** Header title font */
    public static final Font HEADER_TITLE_FONT = new Font(FONT_FAMILY, Font.PLAIN, 14);
    
    /** Header button font */
    public static final Font HEADER_BUTTON_FONT = new Font(FONT_FAMILY, Font.PLAIN, 14);
    
    /** Settings placeholder font */
    public static final Font SETTINGS_PLACEHOLDER_FONT = new Font(FONT_FAMILY, Font.PLAIN, 16);
    
    /** Settings button font */
    public static final Font SETTINGS_BUTTON_FONT = new Font(FONT_FAMILY, Font.PLAIN, 13);
    
    /** Send button font */
    public static final Font SEND_BUTTON_FONT = new Font(FONT_FAMILY, Font.BOLD, 16);
    
    /** Message sender font */
    public static final Font SENDER_FONT = new Font(FONT_FAMILY, Font.BOLD, 14);
    
    /** Message timestamp font */
    public static final Font TIMESTAMP_FONT = new Font(FONT_FAMILY, Font.PLAIN, 12);
    
    /** Scenario label font */
    public static final Font SCENARIO_FONT = new Font(FONT_FAMILY, Font.BOLD, 13);
    
    /** Message text font */
    public static final Font MESSAGE_FONT = new Font(FONT_FAMILY, Font.PLAIN, 14);
    
    /** Collapsible toggle font */
    public static final Font COLLAPSIBLE_TOGGLE_FONT = new Font(FONT_FAMILY, Font.BOLD, 12);
    
    /** Collapsible content font */
    public static final Font COLLAPSIBLE_CONTENT_FONT = new Font(FONT_FAMILY, Font.PLAIN, 12);
    
    // ============================================================================
    // DIMENSION CONSTANTS
    // ============================================================================
    
    /** Button container size */
    public static final Dimension BUTTON_CONTAINER_SIZE = new Dimension(40, 40);
    
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
    public static final String SEND_ICON_PATH = "/icons/send_32.png";
    
    // ============================================================================
    // BORDER CONSTANTS
    // ============================================================================
    
    /** Empty border for general spacing */
    public static final Border EMPTY_BORDER = BorderFactory.createEmptyBorder();
    
    /** Input panel border */
    public static final Border INPUT_PANEL_BORDER = BorderFactory.createEmptyBorder(8, 16, 16, 16);
    
    /** Input container border */
    public static final Border INPUT_CONTAINER_BORDER_COMPOUND = BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(INPUT_CONTAINER_BORDER, 1, true),
        BorderFactory.createEmptyBorder(8, 12, 8, 0)
    );
    
    /** Message container border */
    public static final Border MESSAGE_CONTAINER_BORDER = BorderFactory.createEmptyBorder(8, 16, 8, 16);
    
    /** Header border compound */
    public static final Border HEADER_BORDER_COMPOUND = BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder(0, 0, 1, 0, HEADER_BORDER),
        BorderFactory.createEmptyBorder(8, 16, 8, 16)
    );
    
    /** Settings button border */
    public static final Border SETTINGS_BUTTON_BORDER = BorderFactory.createEmptyBorder(0, 8, 0, 0);
    
    /** Send button border */
    public static final Border SEND_BUTTON_BORDER = BorderFactory.createEmptyBorder(2, 2, 2, 2);
    
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
     * Gets the default panel background color, falling back to the constant if UIManager returns null.
     *
     * @return The panel background color
     */
    public static Color getPanelBackground() {
        Color panelBg = UIManager.getColor("Panel.background");
        return panelBg != null ? panelBg : PANEL_BACKGROUND;
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