package com.triagemate.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.border.Border;
import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TriagePanelConstants class.
 * 
 * <p>These tests verify that all UI constants are properly defined and accessible,
 * and that utility methods work correctly for color fallbacks and border creation.</p>
 * 
 * <p>Test patterns follow standard Java unit testing best practices:
 * <ul>
 *   <li>Test all constant values are not null</li>
 *   <li>Test color constants are valid colors</li>
 *   <li>Test font constants are valid fonts</li>
 *   <li>Test dimension constants have positive values</li>
 *   <li>Test utility methods for color fallbacks</li>
 *   <li>Test border creation methods</li>
 * </ul></p>
 */
@DisplayName("TriagePanelConstants Unit Tests")
class TriagePanelConstantsUnitTest {

    @Test
    @DisplayName("should have valid color constants")
    void shouldHaveValidColorConstants() {
        // Assert all color constants are not null and valid
        assertNotNull(TriagePanelConstants.PANEL_BACKGROUND);
        assertNotNull(TriagePanelConstants.INPUT_CONTAINER_BACKGROUND);
        assertNotNull(TriagePanelConstants.INPUT_CONTAINER_BORDER);
        assertNotNull(TriagePanelConstants.HEADER_BORDER);
        assertNotNull(TriagePanelConstants.HEADER_TEXT);
        assertNotNull(TriagePanelConstants.WHITE);
        assertNotNull(TriagePanelConstants.TIMESTAMP_COLOR);
        assertNotNull(TriagePanelConstants.SCENARIO_COLOR);
        assertNotNull(TriagePanelConstants.FAILURE_COLOR);
        assertNotNull(TriagePanelConstants.COLLAPSIBLE_TEXT_COLOR);
        assertNotNull(TriagePanelConstants.COLLAPSIBLE_BACKGROUND);
        assertNotNull(TriagePanelConstants.COLLAPSIBLE_BORDER);
        assertNotNull(TriagePanelConstants.TRANSPARENT);
        assertNotNull(TriagePanelConstants.HOVER_OVERLAY);
        assertNotNull(TriagePanelConstants.PRESS_OVERLAY);
    }

    @Test
    @DisplayName("should have valid font constants")
    void shouldHaveValidFontConstants() {
        // Assert all font constants are not null and valid
        assertNotNull(TriagePanelConstants.FONT_FAMILY);
        assertNotNull(TriagePanelConstants.INPUT_FONT);
        assertNotNull(TriagePanelConstants.HEADER_TITLE_FONT);
        assertNotNull(TriagePanelConstants.HEADER_BUTTON_FONT);
        assertNotNull(TriagePanelConstants.SETTINGS_PLACEHOLDER_FONT);
        assertNotNull(TriagePanelConstants.SETTINGS_BUTTON_FONT);
        assertNotNull(TriagePanelConstants.SEND_BUTTON_FONT);
        assertNotNull(TriagePanelConstants.SENDER_FONT);
        assertNotNull(TriagePanelConstants.TIMESTAMP_FONT);
        assertNotNull(TriagePanelConstants.SCENARIO_FONT);
        assertNotNull(TriagePanelConstants.MESSAGE_FONT);
        assertNotNull(TriagePanelConstants.COLLAPSIBLE_TOGGLE_FONT);
        assertNotNull(TriagePanelConstants.COLLAPSIBLE_CONTENT_FONT);
        
        // Test that fonts are valid
        assertTrue(TriagePanelConstants.INPUT_FONT.getSize() > 0);
        assertTrue(TriagePanelConstants.HEADER_TITLE_FONT.getSize() > 0);
        assertTrue(TriagePanelConstants.MESSAGE_FONT.getSize() > 0);
    }

    @Test
    @DisplayName("should have valid dimension constants")
    void shouldHaveValidDimensionConstants() {
        // Assert all dimension constants are not null and have positive values
        assertNotNull(TriagePanelConstants.BUTTON_CONTAINER_SIZE);
        assertNotNull(TriagePanelConstants.SEND_BUTTON_SIZE);
        assertNotNull(TriagePanelConstants.MAX_MESSAGE_TEXT_WIDTH);
        assertNotNull(TriagePanelConstants.MAX_COLLAPSIBLE_CONTENT_WIDTH);
        assertNotNull(TriagePanelConstants.MAX_EXPANDABLE_SIZE);
        
        // Test that dimensions have positive values
        assertTrue(TriagePanelConstants.BUTTON_CONTAINER_SIZE.width > 0);
        assertTrue(TriagePanelConstants.BUTTON_CONTAINER_SIZE.height > 0);
        assertTrue(TriagePanelConstants.SEND_BUTTON_SIZE.width > 0);
        assertTrue(TriagePanelConstants.SEND_BUTTON_SIZE.height > 0);
        assertTrue(TriagePanelConstants.MAX_MESSAGE_TEXT_WIDTH > 0);
        assertTrue(TriagePanelConstants.MAX_COLLAPSIBLE_CONTENT_WIDTH > 0);
    }

    @Test
    @DisplayName("should have valid icon path constants")
    void shouldHaveValidIconPathConstants() {
        // Assert all icon path constants are not null and not empty
        assertNotNull(TriagePanelConstants.USER_ICON_PATH);
        assertNotNull(TriagePanelConstants.AI_ICON_PATH);
        assertNotNull(TriagePanelConstants.SEND_ICON_PATH);
        
        // Test that paths are not empty
        assertFalse(TriagePanelConstants.USER_ICON_PATH.isEmpty());
        assertFalse(TriagePanelConstants.AI_ICON_PATH.isEmpty());
        assertFalse(TriagePanelConstants.SEND_ICON_PATH.isEmpty());
        assertTrue(TriagePanelConstants.USER_ICON_PATH.startsWith("/"));
        assertTrue(TriagePanelConstants.AI_ICON_PATH.startsWith("/"));
        assertTrue(TriagePanelConstants.SEND_ICON_PATH.startsWith("/"));
    }

    @Test
    @DisplayName("should have valid border constants")
    void shouldHaveValidBorderConstants() {
        // Assert all border constants are not null
        assertNotNull(TriagePanelConstants.EMPTY_BORDER);
        assertNotNull(TriagePanelConstants.INPUT_PANEL_BORDER);
        assertNotNull(TriagePanelConstants.INPUT_CONTAINER_BORDER_COMPOUND);
        assertNotNull(TriagePanelConstants.MESSAGE_CONTAINER_BORDER);
        assertNotNull(TriagePanelConstants.HEADER_BORDER_COMPOUND);
        assertNotNull(TriagePanelConstants.SETTINGS_BUTTON_BORDER);
        assertNotNull(TriagePanelConstants.SEND_BUTTON_BORDER);
        assertNotNull(TriagePanelConstants.MESSAGE_COMPONENT_BORDER);
        assertNotNull(TriagePanelConstants.MESSAGE_HEADER_BORDER);
        assertNotNull(TriagePanelConstants.MESSAGE_LOGO_BORDER);
        assertNotNull(TriagePanelConstants.SCENARIO_PANEL_BORDER);
        assertNotNull(TriagePanelConstants.FAILED_STEP_PANEL_BORDER);
        assertNotNull(TriagePanelConstants.COLLAPSIBLE_PANEL_BORDER);
    }

    @Test
    @DisplayName("should have valid spacing constants")
    void shouldHaveValidSpacingConstants() {
        // Assert all spacing constants are positive
        assertTrue(TriagePanelConstants.SPACING_SMALL > 0);
        assertTrue(TriagePanelConstants.SPACING_MEDIUM > 0);
        assertTrue(TriagePanelConstants.SPACING_LARGE > 0);
        assertTrue(TriagePanelConstants.SPACING_XLARGE > 0);
        assertTrue(TriagePanelConstants.COMPONENT_SPACING > 0);
        assertTrue(TriagePanelConstants.MESSAGE_SPACING > 0);
        assertTrue(TriagePanelConstants.CONTENT_PADDING > 0);
        assertTrue(TriagePanelConstants.CONTENT_INDENT > 0);
    }

    @Test
    @DisplayName("should have valid text constants")
    void shouldHaveValidTextConstants() {
        // Assert all text constants are not null and not empty
        assertNotNull(TriagePanelConstants.UNKNOWN_SCENARIO);
        assertNotNull(TriagePanelConstants.USER_DISPLAY_NAME);
        assertNotNull(TriagePanelConstants.AI_DISPLAY_NAME);
        assertNotNull(TriagePanelConstants.SCENARIO_PREFIX);
        assertNotNull(TriagePanelConstants.FAILED_STEP_PREFIX);
        assertNotNull(TriagePanelConstants.FAILURE_SYMBOL);
        assertNotNull(TriagePanelConstants.EXPAND_ICON);
        assertNotNull(TriagePanelConstants.COLLAPSE_ICON);
        assertNotNull(TriagePanelConstants.TOGGLE_TEXT);
        assertNotNull(TriagePanelConstants.TOOLTIP_EXPAND);
        assertNotNull(TriagePanelConstants.TOOLTIP_COLLAPSE);
        assertNotNull(TriagePanelConstants.SETTINGS_BUTTON_TEXT);
        assertNotNull(TriagePanelConstants.SETTINGS_BUTTON_TOOLTIP);
        assertNotNull(TriagePanelConstants.HEADER_TITLE_TEXT);
        assertNotNull(TriagePanelConstants.SETTINGS_PLACEHOLDER_TEXT);
        assertNotNull(TriagePanelConstants.BACK_TO_CHAT_TEXT);
        assertNotNull(TriagePanelConstants.SEND_BUTTON_FALLBACK_TEXT);
        assertNotNull(TriagePanelConstants.SEND_BUTTON_TOOLTIP);
        assertNotNull(TriagePanelConstants.INPUT_PLACEHOLDER_TEXT);
        
        // Test that text constants are not empty
        assertFalse(TriagePanelConstants.UNKNOWN_SCENARIO.isEmpty());
        assertFalse(TriagePanelConstants.USER_DISPLAY_NAME.isEmpty());
        assertFalse(TriagePanelConstants.AI_DISPLAY_NAME.isEmpty());
        assertFalse(TriagePanelConstants.HEADER_TITLE_TEXT.isEmpty());
    }

    @Test
    @DisplayName("should have valid configuration constants")
    void shouldHaveValidConfigurationConstants() {
        // Assert all configuration constants are valid
        assertTrue(TriagePanelConstants.ESTIMATED_LINE_HEIGHT > 0);
        assertTrue(TriagePanelConstants.CHARS_PER_LINE > 0);
        assertTrue(TriagePanelConstants.SCROLL_BAR_UNIT_INCREMENT > 0);
        assertTrue(TriagePanelConstants.INPUT_AREA_ROWS > 0);
    }

    @Test
    @DisplayName("should have working utility methods")
    void shouldHaveWorkingUtilityMethods() {
        // Test getPanelBackground utility method
        Color panelBackground = TriagePanelConstants.getPanelBackground();
        assertNotNull(panelBackground);
        // Theme-aware colors may vary by system, so just check it's not null

        // Test border creation utility methods
        Border compoundBorder = TriagePanelConstants.createCompoundBorder(
            TriagePanelConstants.EMPTY_BORDER, 
            TriagePanelConstants.EMPTY_BORDER
        );
        assertNotNull(compoundBorder);

        Border emptyBorder = TriagePanelConstants.createEmptyBorder(1, 2, 3, 4);
        assertNotNull(emptyBorder);

        Border lineBorder = TriagePanelConstants.createLineBorder(Color.BLACK, 1, false);
        assertNotNull(lineBorder);

        Border matteBorder = TriagePanelConstants.createMatteBorder(1, 2, 3, 4, Color.BLACK);
        assertNotNull(matteBorder);
    }

    @Test
    @DisplayName("should have consistent color scheme")
    void shouldHaveConsistentColorScheme() {
        // Test that colors are properly defined
        assertNotNull(TriagePanelConstants.PANEL_BACKGROUND);
        assertNotNull(TriagePanelConstants.INPUT_CONTAINER_BACKGROUND);
        assertNotNull(TriagePanelConstants.WHITE);
        assertNotNull(TriagePanelConstants.TRANSPARENT);
        
        // Test that transparent color is actually transparent
        assertEquals(0, TriagePanelConstants.TRANSPARENT.getAlpha());
        
        // Test that overlay colors have proper alpha values
        assertTrue(TriagePanelConstants.HOVER_OVERLAY.getAlpha() > 0);
        assertTrue(TriagePanelConstants.PRESS_OVERLAY.getAlpha() > 0);
    }

    @Test
    @DisplayName("should have proper font hierarchy")
    void shouldHaveProperFontHierarchy() {
        // Test that fonts have reasonable sizes
        assertTrue(TriagePanelConstants.INPUT_FONT.getSize() > 0);
        assertTrue(TriagePanelConstants.HEADER_TITLE_FONT.getSize() > 0);
        assertTrue(TriagePanelConstants.MESSAGE_FONT.getSize() > 0);
        assertTrue(TriagePanelConstants.TIMESTAMP_FONT.getSize() > 0);
        
        // Test that fonts are valid (may use system fallbacks)
        assertNotNull(TriagePanelConstants.INPUT_FONT.getFamily());
        assertNotNull(TriagePanelConstants.HEADER_TITLE_FONT.getFamily());
        assertNotNull(TriagePanelConstants.MESSAGE_FONT.getFamily());
    }

    @Test
    @DisplayName("should have proper dimension relationships")
    void shouldHaveProperDimensionRelationships() {
        // Test that button container is larger than send button
        assertTrue(TriagePanelConstants.BUTTON_CONTAINER_SIZE.width >= TriagePanelConstants.SEND_BUTTON_SIZE.width);
        assertTrue(TriagePanelConstants.BUTTON_CONTAINER_SIZE.height >= TriagePanelConstants.SEND_BUTTON_SIZE.height);
        
        // Test that max widths are reasonable
        assertTrue(TriagePanelConstants.MAX_MESSAGE_TEXT_WIDTH > 0);
        assertTrue(TriagePanelConstants.MAX_COLLAPSIBLE_CONTENT_WIDTH > 0);
        assertTrue(TriagePanelConstants.MAX_MESSAGE_TEXT_WIDTH >= TriagePanelConstants.MAX_COLLAPSIBLE_CONTENT_WIDTH);
    }

    @Test
    @DisplayName("should have proper spacing relationships")
    void shouldHaveProperSpacingRelationships() {
        // Test that spacing values are in ascending order
        assertTrue(TriagePanelConstants.SPACING_SMALL <= TriagePanelConstants.SPACING_MEDIUM);
        assertTrue(TriagePanelConstants.SPACING_MEDIUM <= TriagePanelConstants.SPACING_LARGE);
        assertTrue(TriagePanelConstants.SPACING_LARGE <= TriagePanelConstants.SPACING_XLARGE);
        
        // Test that component spacing is reasonable
        assertTrue(TriagePanelConstants.COMPONENT_SPACING >= TriagePanelConstants.SPACING_MEDIUM);
        assertTrue(TriagePanelConstants.MESSAGE_SPACING >= TriagePanelConstants.SPACING_SMALL);
    }
} 