package com.triagemate.ui;

import com.intellij.testFramework.UsefulTestCase;
import com.intellij.ui.components.JBTextArea;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for InputPanelFactory class.
 * 
 * <p>These tests verify that InputPanelFactory correctly creates input panel
 * components with proper styling, layout, and functionality.</p>
 */
@DisplayName("InputPanelFactory Unit Tests")
class InputPanelFactoryUnitTest extends UsefulTestCase {

    @Nested
    @DisplayName("Input Panel Creation")
    class InputPanelCreation {

        @Test
        @DisplayName("should create input panel successfully")
        void shouldCreateInputPanelSuccessfully() {
            // Act
            JPanel inputPanel = InputPanelFactory.createInputPanel();

            // Assert
            assertNotNull(inputPanel);
            assertTrue(inputPanel.isVisible());
            assertNotNull(inputPanel.getLayout());
            assertTrue(inputPanel.getLayout() instanceof BorderLayout);
        }

        @Test
        @DisplayName("should have correct layout manager")
        void shouldHaveCorrectLayoutManager() {
            // Act
            JPanel inputPanel = InputPanelFactory.createInputPanel();

            // Assert
            BorderLayout layout = (BorderLayout) inputPanel.getLayout();
            assertNotNull(layout);
        }

        @Test
        @DisplayName("should have correct border")
        void shouldHaveCorrectBorder() {
            // Act
            JPanel inputPanel = InputPanelFactory.createInputPanel();

            // Assert
            assertEquals(TriagePanelConstants.INPUT_PANEL_BORDER, inputPanel.getBorder());
        }

        @Test
        @DisplayName("should have correct background color")
        void shouldHaveCorrectBackgroundColor() {
            // Act
            JPanel inputPanel = InputPanelFactory.createInputPanel();

            // Assert
            assertEquals(TriagePanelConstants.getPanelBackground(), inputPanel.getBackground());
        }
    }

    @Nested
    @DisplayName("Input Container Creation")
    class InputContainerCreation {

        @Test
        @DisplayName("should create input container successfully")
        void shouldCreateInputContainerSuccessfully() {
            // Act
            JPanel inputContainer = InputPanelFactory.createInputContainer();

            // Assert
            assertNotNull(inputContainer);
            assertTrue(inputContainer.isVisible());
            assertNotNull(inputContainer.getLayout());
            assertTrue(inputContainer.getLayout() instanceof BorderLayout);
        }

        @Test
        @DisplayName("should have correct background color")
        void shouldHaveCorrectBackgroundColor() {
            // Act
            JPanel inputContainer = InputPanelFactory.createInputContainer();

            // Assert
            assertEquals(TriagePanelConstants.INPUT_CONTAINER_BACKGROUND, inputContainer.getBackground());
        }

        @Test
        @DisplayName("should have correct border")
        void shouldHaveCorrectBorder() {
            // Act
            JPanel inputContainer = InputPanelFactory.createInputContainer();

            // Assert
            assertEquals(TriagePanelConstants.INPUT_CONTAINER_BORDER_COMPOUND, inputContainer.getBorder());
        }

        @Test
        @DisplayName("should be opaque")
        void shouldBeOpaque() {
            // Act
            JPanel inputContainer = InputPanelFactory.createInputContainer();

            // Assert
            assertTrue(inputContainer.isOpaque());
        }
    }

    @Nested
    @DisplayName("Input Area Creation")
    class InputAreaCreation {

        @Test
        @DisplayName("should create input area successfully")
        void shouldCreateInputAreaSuccessfully() {
            // Arrange
            ActionListener mockActionListener = e -> {};

            // Act
            JBTextArea inputArea = InputPanelFactory.createInputArea(mockActionListener);

            // Assert
            assertNotNull(inputArea);
            assertTrue(inputArea.isVisible());
            assertTrue(inputArea.isEditable());
        }

        @Test
        @DisplayName("should have correct font")
        void shouldHaveCorrectFont() {
            // Arrange
            ActionListener mockActionListener = e -> {};

            // Act
            JBTextArea inputArea = InputPanelFactory.createInputArea(mockActionListener);

            // Assert
            assertEquals(TriagePanelConstants.INPUT_FONT, inputArea.getFont());
        }

        @Test
        @DisplayName("should have correct background color")
        void shouldHaveCorrectBackgroundColor() {
            // Arrange
            ActionListener mockActionListener = e -> {};

            // Act
            JBTextArea inputArea = InputPanelFactory.createInputArea(mockActionListener);

            // Assert
            assertEquals(TriagePanelConstants.INPUT_CONTAINER_BACKGROUND, inputArea.getBackground());
        }

        @Test
        @DisplayName("should have correct foreground color")
        void shouldHaveCorrectForegroundColor() {
            // Arrange
            ActionListener mockActionListener = e -> {};

            // Act
            JBTextArea inputArea = InputPanelFactory.createInputArea(mockActionListener);

            // Assert
            assertEquals(TriagePanelConstants.WHITE, inputArea.getForeground());
        }

        @Test
        @DisplayName("should have correct caret color")
        void shouldHaveCorrectCaretColor() {
            // Arrange
            ActionListener mockActionListener = e -> {};

            // Act
            JBTextArea inputArea = InputPanelFactory.createInputArea(mockActionListener);

            // Assert
            assertEquals(TriagePanelConstants.WHITE, inputArea.getCaretColor());
        }

        @Test
        @DisplayName("should have line wrap enabled")
        void shouldHaveLineWrapEnabled() {
            // Arrange
            ActionListener mockActionListener = e -> {};

            // Act
            JBTextArea inputArea = InputPanelFactory.createInputArea(mockActionListener);

            // Assert
            assertTrue(inputArea.getLineWrap());
        }

        @Test
        @DisplayName("should have word wrap enabled")
        void shouldHaveWordWrapEnabled() {
            // Arrange
            ActionListener mockActionListener = e -> {};

            // Act
            JBTextArea inputArea = InputPanelFactory.createInputArea(mockActionListener);

            // Assert
            assertTrue(inputArea.getWrapStyleWord());
        }

        @Test
        @DisplayName("should have correct number of rows")
        void shouldHaveCorrectNumberOfRows() {
            // Arrange
            ActionListener mockActionListener = e -> {};

            // Act
            JBTextArea inputArea = InputPanelFactory.createInputArea(mockActionListener);

            // Assert
            assertEquals(TriagePanelConstants.INPUT_AREA_ROWS, inputArea.getRows());
        }

        @Test
        @DisplayName("should have correct border")
        void shouldHaveCorrectBorder() {
            // Arrange
            ActionListener mockActionListener = e -> {};

            // Act
            JBTextArea inputArea = InputPanelFactory.createInputArea(mockActionListener);

            // Assert
            assertEquals(TriagePanelConstants.EMPTY_BORDER, inputArea.getBorder());
        }

        @Test
        @DisplayName("should not be opaque")
        void shouldNotBeOpaque() {
            // Arrange
            ActionListener mockActionListener = e -> {};

            // Act
            JBTextArea inputArea = InputPanelFactory.createInputArea(mockActionListener);

            // Assert
            assertFalse(inputArea.isOpaque());
        }

        @Test
        @DisplayName("should handle null action listener gracefully")
        void shouldHandleNullActionListenerGracefully() {
            // Act
            JBTextArea inputArea = InputPanelFactory.createInputArea(null);

            // Assert
            assertNotNull(inputArea);
            // Should not throw exception, should log warning and create limited functionality
            // Keyboard events will be disabled but component should still be created
        }
    }

    @Nested
    @DisplayName("Button Container Creation")
    class ButtonContainerCreation {

        @Test
        @DisplayName("should create button container successfully")
        void shouldCreateButtonContainerSuccessfully() {
            // Act
            JPanel buttonContainer = InputPanelFactory.createButtonContainer();

            // Assert
            assertNotNull(buttonContainer);
            assertTrue(buttonContainer.isVisible());
            assertNotNull(buttonContainer.getLayout());
            assertTrue(buttonContainer.getLayout() instanceof FlowLayout);
        }

        @Test
        @DisplayName("should have correct layout manager")
        void shouldHaveCorrectLayoutManager() {
            // Act
            JPanel buttonContainer = InputPanelFactory.createButtonContainer();

            // Assert
            FlowLayout layout = (FlowLayout) buttonContainer.getLayout();
            assertNotNull(layout);
        }

        @Test
        @DisplayName("should have correct preferred size")
        void shouldHaveCorrectPreferredSize() {
            // Act
            JPanel buttonContainer = InputPanelFactory.createButtonContainer();

            // Assert
            assertEquals(TriagePanelConstants.BUTTON_CONTAINER_SIZE, buttonContainer.getPreferredSize());
        }

        @Test
        @DisplayName("should have correct maximum size")
        void shouldHaveCorrectMaximumSize() {
            // Act
            JPanel buttonContainer = InputPanelFactory.createButtonContainer();

            // Assert
            assertEquals(TriagePanelConstants.BUTTON_CONTAINER_SIZE, buttonContainer.getMaximumSize());
        }

        @Test
        @DisplayName("should have correct minimum size")
        void shouldHaveCorrectMinimumSize() {
            // Act
            JPanel buttonContainer = InputPanelFactory.createButtonContainer();

            // Assert
            assertEquals(TriagePanelConstants.BUTTON_CONTAINER_SIZE, buttonContainer.getMinimumSize());
        }

        @Test
        @DisplayName("should not be opaque")
        void shouldNotBeOpaque() {
            // Act
            JPanel buttonContainer = InputPanelFactory.createButtonContainer();

            // Assert
            assertFalse(buttonContainer.isOpaque());
        }
    }

    @Nested
    @DisplayName("Modern Send Button Creation")
    class ModernSendButtonCreation {

        @Test
        @DisplayName("should create modern send button successfully")
        void shouldCreateModernSendButtonSuccessfully() {
            // Arrange
            ActionListener mockActionListener = e -> {};

            // Act
            JButton sendButton = InputPanelFactory.createModernSendButton(mockActionListener);

            // Assert
            assertNotNull(sendButton);
            assertTrue(sendButton.isVisible());
        }

        @Test
        @DisplayName("should have correct preferred size")
        void shouldHaveCorrectPreferredSize() {
            // Arrange
            ActionListener mockActionListener = e -> {};

            // Act
            JButton sendButton = InputPanelFactory.createModernSendButton(mockActionListener);

            // Assert
            assertEquals(TriagePanelConstants.SEND_BUTTON_SIZE, sendButton.getPreferredSize());
        }

        @Test
        @DisplayName("should have correct maximum size")
        void shouldHaveCorrectMaximumSize() {
            // Arrange
            ActionListener mockActionListener = e -> {};

            // Act
            JButton sendButton = InputPanelFactory.createModernSendButton(mockActionListener);

            // Assert
            assertEquals(TriagePanelConstants.SEND_BUTTON_SIZE, sendButton.getMaximumSize());
        }

        @Test
        @DisplayName("should have correct minimum size")
        void shouldHaveCorrectMinimumSize() {
            // Arrange
            ActionListener mockActionListener = e -> {};

            // Act
            JButton sendButton = InputPanelFactory.createModernSendButton(mockActionListener);

            // Assert
            assertEquals(TriagePanelConstants.SEND_BUTTON_SIZE, sendButton.getMinimumSize());
        }

        @Test
        @DisplayName("should have correct background color")
        void shouldHaveCorrectBackgroundColor() {
            // Arrange
            ActionListener mockActionListener = e -> {};

            // Act
            JButton sendButton = InputPanelFactory.createModernSendButton(mockActionListener);

            // Assert
            assertEquals(TriagePanelConstants.TRANSPARENT, sendButton.getBackground());
        }

        @Test
        @DisplayName("should have correct foreground color")
        void shouldHaveCorrectForegroundColor() {
            // Arrange
            ActionListener mockActionListener = e -> {};

            // Act
            JButton sendButton = InputPanelFactory.createModernSendButton(mockActionListener);

            // Assert
            assertEquals(TriagePanelConstants.WHITE, sendButton.getForeground());
        }

        @Test
        @DisplayName("should not show focus border")
        void shouldNotShowFocusBorder() {
            // Arrange
            ActionListener mockActionListener = e -> {};

            // Act
            JButton sendButton = InputPanelFactory.createModernSendButton(mockActionListener);

            // Assert
            assertFalse(sendButton.isFocusPainted());
        }

        @Test
        @DisplayName("should not show border")
        void shouldNotShowBorder() {
            // Arrange
            ActionListener mockActionListener = e -> {};

            // Act
            JButton sendButton = InputPanelFactory.createModernSendButton(mockActionListener);

            // Assert
            assertFalse(sendButton.isBorderPainted());
        }

        @Test
        @DisplayName("should not fill content area")
        void shouldNotFillContentArea() {
            // Arrange
            ActionListener mockActionListener = e -> {};

            // Act
            JButton sendButton = InputPanelFactory.createModernSendButton(mockActionListener);

            // Assert
            assertFalse(sendButton.isContentAreaFilled());
        }

        @Test
        @DisplayName("should not be opaque")
        void shouldNotBeOpaque() {
            // Arrange
            ActionListener mockActionListener = e -> {};

            // Act
            JButton sendButton = InputPanelFactory.createModernSendButton(mockActionListener);

            // Assert
            assertFalse(sendButton.isOpaque());
        }

        @Test
        @DisplayName("should have correct border")
        void shouldHaveCorrectBorder() {
            // Arrange
            ActionListener mockActionListener = e -> {};

            // Act
            JButton sendButton = InputPanelFactory.createModernSendButton(mockActionListener);

            // Assert
            assertEquals(TriagePanelConstants.SEND_BUTTON_BORDER, sendButton.getBorder());
        }

        @Test
        @DisplayName("should have correct cursor")
        void shouldHaveCorrectCursor() {
            // Arrange
            ActionListener mockActionListener = e -> {};

            // Act
            JButton sendButton = InputPanelFactory.createModernSendButton(mockActionListener);

            // Assert
            assertEquals(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR), sendButton.getCursor());
        }

        @Test
        @DisplayName("should have tooltip")
        void shouldHaveTooltip() {
            // Arrange
            ActionListener mockActionListener = e -> {};

            // Act
            JButton sendButton = InputPanelFactory.createModernSendButton(mockActionListener);

            // Assert
            assertEquals(TriagePanelConstants.SEND_BUTTON_TOOLTIP, sendButton.getToolTipText());
        }

        @Test
        @DisplayName("should handle null action listener gracefully")
        void shouldHandleNullActionListenerGracefully() {
            // Act
            JButton sendButton = InputPanelFactory.createModernSendButton(null);

            // Assert
            assertNotNull(sendButton);
            assertFalse(sendButton.isEnabled()); // Should be disabled when no action listener
            // Should not throw exception, should log warning and create limited functionality
        }
    }

    @Nested
    @DisplayName("Complete Input Panel Setup")
    class CompleteInputPanelSetup {

        @Test
        @DisplayName("should create complete input panel setup successfully")
        void shouldCreateCompleteInputPanelSetupSuccessfully() {
            // Arrange
            ActionListener mockActionListener = e -> {};

            // Act
            Object[] result = InputPanelFactory.createCompleteInputPanelSetup(mockActionListener);

            // Assert
            assertNotNull(result);
            assertEquals(5, result.length);
            assertTrue(result[0] instanceof JPanel); // inputPanel
            assertTrue(result[1] instanceof JPanel); // inputContainer
            assertTrue(result[2] instanceof JBTextArea); // inputArea
            assertTrue(result[3] instanceof JPanel); // buttonContainer
            assertTrue(result[4] instanceof JButton); // sendButton
        }

        @Test
        @DisplayName("should have proper component hierarchy")
        void shouldHaveProperComponentHierarchy() {
            // Arrange
            ActionListener mockActionListener = e -> {};

            // Act
            Object[] result = InputPanelFactory.createCompleteInputPanelSetup(mockActionListener);
            JPanel inputPanel = (JPanel) result[0];

            // Assert
            assertTrue(inputPanel.getComponentCount() > 0);
        }
    }

    @Nested
    @DisplayName("Input Area Operations")
    class InputAreaOperations {

        @Test
        @DisplayName("should clear input area successfully")
        void shouldClearInputAreaSuccessfully() {
            // Arrange
            ActionListener mockActionListener = e -> {};
            JBTextArea inputArea = InputPanelFactory.createInputArea(mockActionListener);
            inputArea.setText("Test text");

            // Act
            InputPanelFactory.clearInputArea(inputArea);

            // Assert
            assertEquals("", inputArea.getText());
        }

        @Test
        @DisplayName("should handle null input area in clear operation")
        void shouldHandleNullInputAreaInClearOperation() {
            // Act
            InputPanelFactory.clearInputArea(null);

            // Assert
            // Should not throw exception, should log warning and return early
            // The method handles null gracefully with defensive programming
        }

        @Test
        @DisplayName("should get input text successfully")
        void shouldGetInputTextSuccessfully() {
            // Arrange
            ActionListener mockActionListener = e -> {};
            JBTextArea inputArea = InputPanelFactory.createInputArea(mockActionListener);
            String expectedText = "Test text";
            inputArea.setText(expectedText);

            // Act
            String result = InputPanelFactory.getInputText(inputArea);

            // Assert
            assertEquals(expectedText, result);
        }

        @Test
        @DisplayName("should handle null input area in get text operation")
        void shouldHandleNullInputAreaInGetTextOperation() {
            // Act
            String result = InputPanelFactory.getInputText(null);

            // Assert
            assertEquals("", result); // Should return empty string for null input
            // Should not throw exception, should log warning and return empty string
        }

        @Test
        @DisplayName("should check input content correctly")
        void shouldCheckInputContentCorrectly() {
            // Arrange
            ActionListener mockActionListener = e -> {};
            JBTextArea inputArea = InputPanelFactory.createInputArea(mockActionListener);

            // Act & Assert - empty content
            assertFalse(InputPanelFactory.hasInputContent(inputArea));

            // Act & Assert - with content
            inputArea.setText("Test text");
            assertTrue(InputPanelFactory.hasInputContent(inputArea));
        }

        @Test
        @DisplayName("should handle null input area in content check")
        void shouldHandleNullInputAreaInContentCheck() {
            // Act
            boolean result = InputPanelFactory.hasInputContent(null);

            // Assert
            assertFalse(result); // Should return false for null input
            // Should not throw exception, should log warning and return false
        }
    }

    @Nested
    @DisplayName("Component State Management")
    class ComponentStateManagement {

        @Test
        @DisplayName("should set input enabled state")
        void shouldSetInputEnabledState() {
            // Arrange
            ActionListener mockActionListener = e -> {};
            JBTextArea inputArea = InputPanelFactory.createInputArea(mockActionListener);

            // Act
            InputPanelFactory.setInputEnabled(inputArea, false);

            // Assert
            assertFalse(inputArea.isEnabled());

            // Act
            InputPanelFactory.setInputEnabled(inputArea, true);

            // Assert
            assertTrue(inputArea.isEnabled());
        }

        @Test
        @DisplayName("should handle null input area in set enabled")
        void shouldHandleNullInputAreaInSetEnabled() {
            // Act
            InputPanelFactory.setInputEnabled(null, true);

            // Assert
            // Should not throw exception, should log warning and return early
            // The method handles null gracefully with defensive programming
        }

        @Test
        @DisplayName("should set send button enabled state")
        void shouldSetSendButtonEnabledState() {
            // Arrange
            ActionListener mockActionListener = e -> {};
            JButton sendButton = InputPanelFactory.createModernSendButton(mockActionListener);

            // Act
            InputPanelFactory.setSendButtonEnabled(sendButton, false);

            // Assert
            assertFalse(sendButton.isEnabled());

            // Act
            InputPanelFactory.setSendButtonEnabled(sendButton, true);

            // Assert
            assertTrue(sendButton.isEnabled());
        }

        @Test
        @DisplayName("should handle null send button in set enabled")
        void shouldHandleNullSendButtonInSetEnabled() {
            // Act
            InputPanelFactory.setSendButtonEnabled(null, true);

            // Assert
            // Should not throw exception, should log warning and return early
            // The method handles null gracefully with defensive programming
        }
    }
} 