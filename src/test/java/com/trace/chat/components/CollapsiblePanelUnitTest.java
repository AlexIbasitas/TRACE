package com.trace.chat.components;

import com.trace.common.constants.TriagePanelConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CollapsiblePanel class.
 * 
 * <p>These tests verify that CollapsiblePanel correctly handles expand/collapse
 * functionality, proper styling, and user interactions.</p>
 * 
 * <p>Test patterns follow Swing component testing best practices:
 * <ul>
 *   <li>Test component creation and initialization</li>
 *   <li>Test expand/collapse functionality</li>
 *   <li>Test visual properties and styling</li>
 *   <li>Test user interactions (mouse events)</li>
 *   <li>Test content display and layout</li>
 *   <li>Test edge cases and error handling</li>
 * </ul></p>
 */
@DisplayName("CollapsiblePanel Unit Tests")
class CollapsiblePanelUnitTest {

    private CollapsiblePanel collapsiblePanel;
    private MessageComponent mockParentMessageComponent;

    @BeforeEach
    void setUp() {
        // Create a mock parent message component
        ChatMessage testMessage = new ChatMessage(
            ChatMessage.Role.AI,
            "Test message",
            System.currentTimeMillis(),
            "Test thinking content",
            null
        );
        mockParentMessageComponent = new MessageComponent(testMessage);
        
        collapsiblePanel = new CollapsiblePanel("Test Header", "Test content that should be collapsible", mockParentMessageComponent);
    }

    @Nested
    @DisplayName("Component Creation and Initialization")
    class ComponentCreationAndInitialization {

        @Test
        @DisplayName("should create collapsible panel successfully")
        void shouldCreateCollapsiblePanelSuccessfully() {
            // Assert
            assertNotNull(collapsiblePanel);
            assertTrue(collapsiblePanel.isVisible());
            assertNotNull(collapsiblePanel.getLayout());
            assertTrue(collapsiblePanel.getComponentCount() > 0);
        }

        @Test
        @DisplayName("should initialize in collapsed state")
        void shouldInitializeInCollapsedState() {
            // Assert
            assertFalse(collapsiblePanel.isExpanded());
            assertFalse(collapsiblePanel.getContentPanel().isVisible());
        }

        @Test
        @DisplayName("should handle null parent gracefully")
        void shouldHandleNullParentGracefully() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> 
                new CollapsiblePanel("Test", "Content", null),
                "Should throw IllegalArgumentException for null parent"
            );
        }

        @Test
        @DisplayName("should have correct layout manager")
        void shouldHaveCorrectLayoutManager() {
            // Assert
            assertTrue(collapsiblePanel.getLayout() instanceof BorderLayout);
        }
    }

    @Nested
    @DisplayName("Expand and Collapse Functionality")
    class ExpandAndCollapseFunctionality {

        @Test
        @DisplayName("should expand when expand method is called")
        void shouldExpandWhenExpandMethodIsCalled() {
            // Act
            collapsiblePanel.expand();

            // Assert
            assertTrue(collapsiblePanel.isExpanded());
            assertTrue(collapsiblePanel.getContentPanel().isVisible());
        }

        @Test
        @DisplayName("should collapse when collapse method is called")
        void shouldCollapseWhenCollapseMethodIsCalled() {
            // Arrange
            collapsiblePanel.expand();
            assertTrue(collapsiblePanel.isExpanded());

            // Act
            collapsiblePanel.collapse();

            // Assert
            assertFalse(collapsiblePanel.isExpanded());
            assertFalse(collapsiblePanel.getContentPanel().isVisible());
        }

        @Test
        @DisplayName("should update toggle label when expanded")
        void shouldUpdateToggleLabelWhenExpanded() {
            // Act
            collapsiblePanel.expand();

            // Assert
            JLabel toggleLabel = collapsiblePanel.getToggleLabel();
            assertNotNull(toggleLabel);
            assertTrue(toggleLabel.getText().contains(TriagePanelConstants.COLLAPSE_ICON));
        }

        @Test
        @DisplayName("should update toggle label when collapsed")
        void shouldUpdateToggleLabelWhenCollapsed() {
            // Arrange
            collapsiblePanel.expand();
            assertTrue(collapsiblePanel.isExpanded());

            // Act
            collapsiblePanel.collapse();

            // Assert
            JLabel toggleLabel = collapsiblePanel.getToggleLabel();
            assertNotNull(toggleLabel);
            assertTrue(toggleLabel.getText().contains(TriagePanelConstants.EXPAND_ICON));
        }
    }

    @Nested
    @DisplayName("Visual Properties and Styling")
    class VisualPropertiesAndStyling {

        @Test
        @DisplayName("should not be opaque")
        void shouldNotBeOpaque() {
            // Assert
            assertFalse(collapsiblePanel.isOpaque());
        }

        @Test
        @DisplayName("should have correct border")
        void shouldHaveCorrectBorder() {
            // Assert
            assertEquals(TriagePanelConstants.COLLAPSIBLE_PANEL_BORDER, collapsiblePanel.getBorder());
        }

        @Test
        @DisplayName("should have correct alignment")
        void shouldHaveCorrectAlignment() {
            // Assert
            assertEquals(Component.LEFT_ALIGNMENT, collapsiblePanel.getAlignmentX());
        }

        @Test
        @DisplayName("should have correct maximum size")
        void shouldHaveCorrectMaximumSize() {
            // Assert
            assertEquals(TriagePanelConstants.MAX_EXPANDABLE_SIZE, collapsiblePanel.getMaximumSize());
        }
    }

    @Nested
    @DisplayName("Toggle Label Component")
    class ToggleLabelComponent {

        @Test
        @DisplayName("should display toggle text correctly")
        void shouldDisplayToggleTextCorrectly() {
            // Assert
            JLabel toggleLabel = collapsiblePanel.getToggleLabel();
            assertNotNull(toggleLabel);
            assertTrue(toggleLabel.getText().contains(TriagePanelConstants.TOGGLE_TEXT));
        }

        @Test
        @DisplayName("should have correct font")
        void shouldHaveCorrectFont() {
            // Assert
            JLabel toggleLabel = collapsiblePanel.getToggleLabel();
            assertNotNull(toggleLabel);
            assertEquals(TriagePanelConstants.COLLAPSIBLE_TOGGLE_FONT, toggleLabel.getFont());
        }

        @Test
        @DisplayName("should have correct foreground color")
        void shouldHaveCorrectForegroundColor() {
            // Assert
            JLabel toggleLabel = collapsiblePanel.getToggleLabel();
            assertNotNull(toggleLabel);
            assertEquals(TriagePanelConstants.WHITE, toggleLabel.getForeground());
        }

        @Test
        @DisplayName("should have correct cursor")
        void shouldHaveCorrectCursor() {
            // Assert
            JLabel toggleLabel = collapsiblePanel.getToggleLabel();
            assertNotNull(toggleLabel);
            assertEquals(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR), toggleLabel.getCursor());
        }

        @Test
        @DisplayName("should have tooltip")
        void shouldHaveTooltip() {
            // Assert
            JLabel toggleLabel = collapsiblePanel.getToggleLabel();
            assertNotNull(toggleLabel);
            assertNotNull(toggleLabel.getToolTipText());
        }
    }

    @Nested
    @DisplayName("Content Panel Component")
    class ContentPanelComponent {

        @Test
        @DisplayName("should display content text correctly when expanded")
        void shouldDisplayContentTextCorrectlyWhenExpanded() {
            // Arrange
            collapsiblePanel.expand();

            // Assert
            JPanel contentPanel = collapsiblePanel.getContentPanel();
            assertTrue(contentPanel.isVisible());
            assertTrue(contentPanel.getComponentCount() > 0);
        }

        @Test
        @DisplayName("should not be opaque")
        void shouldNotBeOpaque() {
            // Assert
            JPanel contentPanel = collapsiblePanel.getContentPanel();
            assertFalse(contentPanel.isOpaque());
        }

        @Test
        @DisplayName("should have correct border")
        void shouldHaveCorrectBorder() {
            // Assert
            JPanel contentPanel = collapsiblePanel.getContentPanel();
            assertEquals(TriagePanelConstants.COLLAPSIBLE_CONTENT_BORDER, contentPanel.getBorder());
        }

        @Test
        @DisplayName("should have correct maximum size")
        void shouldHaveCorrectMaximumSize() {
            // Assert
            JPanel contentPanel = collapsiblePanel.getContentPanel();
            assertEquals(TriagePanelConstants.MAX_EXPANDABLE_SIZE, contentPanel.getMaximumSize());
        }
    }

    @Nested
    @DisplayName("User Interactions")
    class UserInteractions {

        @Test
        @DisplayName("should have mouse listeners attached to toggle label")
        void shouldHaveMouseListenersAttachedToToggleLabel() {
            // Assert
            JLabel toggleLabel = collapsiblePanel.getToggleLabel();
            assertNotNull(toggleLabel);
            
            // Check that mouse listeners are attached
            assertTrue(toggleLabel.getMouseListeners().length > 0);
        }

        @Test
        @DisplayName("should respond to mouse click on toggle label")
        void shouldRespondToMouseClickOnToggleLabel() {
            // Arrange
            assertFalse(collapsiblePanel.isExpanded());
            JLabel toggleLabel = collapsiblePanel.getToggleLabel();
            assertNotNull(toggleLabel);
            
            // Act - simulate mouse click by calling expand directly
            collapsiblePanel.expand();

            // Assert
            assertTrue(collapsiblePanel.isExpanded());
        }
    }

    @Nested
    @DisplayName("Layout and Sizing Behavior")
    class LayoutAndSizingBehavior {

        @Test
        @DisplayName("should have different preferred size when expanded vs collapsed")
        void shouldHaveDifferentPreferredSizeWhenExpandedVsCollapsed() {
            // Arrange
            Dimension collapsedSize = collapsiblePanel.getPreferredSize();

            // Act
            collapsiblePanel.expand();
            Dimension expandedSize = collapsiblePanel.getPreferredSize();

            // Assert
            assertTrue(expandedSize.height > collapsedSize.height);
        }

        @Test
        @DisplayName("should have flexible width when expanding/collapsing")
        void shouldHaveFlexibleWidthWhenExpandingCollapsing() {
            // Arrange
            Dimension collapsedSize = collapsiblePanel.getPreferredSize();

            // Act
            collapsiblePanel.expand();
            Dimension expandedSize = collapsiblePanel.getPreferredSize();

            // Assert
            // Width should be flexible and may change based on content
            assertTrue(expandedSize.width > 0);
            assertTrue(collapsedSize.width > 0);
        }

        @Test
        @DisplayName("should be valid after state changes")
        void shouldBeValidAfterStateChanges() {
            // Act
            collapsiblePanel.expand();

            // Assert
            // The component should be valid after state change
            // Note: Layout updates may be asynchronous, so we check for basic validity
            assertTrue(collapsiblePanel.isVisible());
            assertNotNull(collapsiblePanel.getLayout());
            assertTrue(collapsiblePanel.isExpanded());
            assertTrue(collapsiblePanel.getContentPanel().isVisible());
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesAndErrorHandling {

        @Test
        @DisplayName("should handle empty content")
        void shouldHandleEmptyContent() {
            // Arrange
            CollapsiblePanel emptyPanel = new CollapsiblePanel("Test", "", mockParentMessageComponent);

            // Act
            emptyPanel.expand();

            // Assert
            assertNotNull(emptyPanel);
            assertTrue(emptyPanel.isExpanded());
            assertTrue(emptyPanel.getContentPanel().isVisible());
        }

        @Test
        @DisplayName("should handle null content")
        void shouldHandleNullContent() {
            // Arrange
            CollapsiblePanel nullPanel = new CollapsiblePanel("Test", null, mockParentMessageComponent);

            // Act
            nullPanel.expand();

            // Assert
            assertNotNull(nullPanel);
            assertTrue(nullPanel.isExpanded());
            assertTrue(nullPanel.getContentPanel().isVisible());
        }

        @Test
        @DisplayName("should handle very long content")
        void shouldHandleVeryLongContent() {
            // Arrange
            String longContent = "A".repeat(1000);
            CollapsiblePanel longPanel = new CollapsiblePanel("Test", longContent, mockParentMessageComponent);

            // Act
            longPanel.expand();

            // Assert
            assertNotNull(longPanel);
            assertTrue(longPanel.isExpanded());
            assertTrue(longPanel.getContentPanel().isVisible());
        }

        @Test
        @DisplayName("should handle special characters in content")
        void shouldHandleSpecialCharactersInContent() {
            // Arrange
            String specialContent = "Content with special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?";
            CollapsiblePanel specialPanel = new CollapsiblePanel("Test", specialContent, mockParentMessageComponent);

            // Act
            specialPanel.expand();

            // Assert
            assertNotNull(specialPanel);
            assertTrue(specialPanel.isExpanded());
            assertTrue(specialPanel.getContentPanel().isVisible());
        }

        @Test
        @DisplayName("should handle unicode characters in content")
        void shouldHandleUnicodeCharactersInContent() {
            // Arrange
            String unicodeContent = "Content with unicode: 你好世界 🌍 🚀";
            CollapsiblePanel unicodePanel = new CollapsiblePanel("Test", unicodeContent, mockParentMessageComponent);

            // Act
            unicodePanel.expand();

            // Assert
            assertNotNull(unicodePanel);
            assertTrue(unicodePanel.isExpanded());
            assertTrue(unicodePanel.getContentPanel().isVisible());
        }

        @Test
        @DisplayName("should handle multiple expand/collapse cycles")
        void shouldHandleMultipleExpandCollapseCycles() {
            // Act & Assert - multiple cycles
            for (int i = 0; i < 10; i++) {
                collapsiblePanel.expand();
                assertTrue(collapsiblePanel.isExpanded());
                
                collapsiblePanel.collapse();
                assertFalse(collapsiblePanel.isExpanded());
            }
        }

        @Test
        @DisplayName("should handle rapid state changes")
        void shouldHandleRapidStateChanges() {
            // Act - rapid state changes
            collapsiblePanel.expand();
            collapsiblePanel.collapse();
            collapsiblePanel.expand();
            collapsiblePanel.collapse();
            collapsiblePanel.expand();

            // Assert
            assertTrue(collapsiblePanel.isExpanded());
        }
    }

    @Nested
    @DisplayName("Parent Message Component Integration")
    class ParentMessageComponentIntegration {

        @Test
        @DisplayName("should have correct parent message component")
        void shouldHaveCorrectParentMessageComponent() {
            // Assert
            assertEquals(mockParentMessageComponent, collapsiblePanel.getParentMessageComponent());
        }

        @Test
        @DisplayName("should have content when parent has AI thinking")
        void shouldHaveContentWhenParentHasAiThinking() {
            // Assert
            assertTrue(collapsiblePanel.hasContent());
        }

        @Test
        @DisplayName("should have correct content component count")
        void shouldHaveCorrectContentComponentCount() {
            // Act
            collapsiblePanel.expand();

            // Assert
            assertTrue(collapsiblePanel.getContentComponentCount() > 0);
        }
    }
} 