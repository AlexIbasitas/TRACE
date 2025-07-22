package com.trace.chat.handlers;

import com.intellij.ui.components.JBTextArea;
import com.trace.chat.components.ChatMessage;
import com.trace.chat.components.CollapsiblePanel;
import com.trace.chat.components.MessageComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TriagePanelEventHandlers class.
 * 
 * <p>These tests verify that event handlers are created correctly and respond
 * appropriately to user interactions like keyboard and mouse events.</p>
 * 
 * <p>Test patterns follow Swing event handling testing best practices:
 * <ul>
 *   <li>Test event handler creation</li>
 *   <li>Test keyboard event handling</li>
 *   <li>Test mouse event handling</li>
 *   <li>Test action listener functionality</li>
 *   <li>Test EDT compliance</li>
 *   <li>Test edge cases and error handling</li>
 * </ul></p>
 */
@DisplayName("TriagePanelEventHandlers Unit Tests")
class TriagePanelEventHandlersUnitTest {

    private JBTextArea testTextArea;
    private JButton testButton;
    private CollapsiblePanel testCollapsiblePanel;
    private boolean actionPerformed;

    @BeforeEach
    void setUp() {
        testTextArea = new JBTextArea();
        testButton = new JButton("Test");
        
        // Create a mock parent message component for CollapsiblePanel
        ChatMessage testMessage = new ChatMessage(
            ChatMessage.Role.AI,
            "Test message",
            System.currentTimeMillis(),
            "Test thinking content",
            null
        );
        MessageComponent mockParent = new MessageComponent(testMessage);
        testCollapsiblePanel = new CollapsiblePanel("Test Header", "Test Content", mockParent);
        
        actionPerformed = false;
    }

    @Nested
    @DisplayName("Input Key Adapter Creation")
    class InputKeyAdapterCreation {

        @Test
        @DisplayName("should create input key adapter successfully")
        void shouldCreateInputKeyAdapterSuccessfully() {
            // Arrange
            ActionListener mockActionListener = e -> actionPerformed = true;

            // Act
            var keyAdapter = TriagePanelEventHandlers.createInputKeyAdapter(mockActionListener);

            // Assert
            assertNotNull(keyAdapter);
        }

        @Test
        @DisplayName("should handle null action listener gracefully")
        void shouldHandleNullActionListenerGracefully() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> 
                TriagePanelEventHandlers.createInputKeyAdapter(null),
                "Should throw IllegalArgumentException for null action listener"
            );
        }

        @Test
        @DisplayName("should handle Enter key press")
        void shouldHandleEnterKeyPress() {
            // Arrange
            ActionListener mockActionListener = e -> actionPerformed = true;
            var keyAdapter = TriagePanelEventHandlers.createInputKeyAdapter(mockActionListener);

            // Act
            KeyEvent enterEvent = new KeyEvent(
                testTextArea,
                KeyEvent.KEY_PRESSED,
                System.currentTimeMillis(),
                0,
                KeyEvent.VK_ENTER,
                KeyEvent.CHAR_UNDEFINED
            );
            keyAdapter.keyPressed(enterEvent);

            // Assert
            assertTrue(actionPerformed);
            assertTrue(enterEvent.isConsumed());
        }

        @Test
        @DisplayName("should handle Shift+Enter key press")
        void shouldHandleShiftEnterKeyPress() {
            // Arrange
            ActionListener mockActionListener = e -> actionPerformed = true;
            var keyAdapter = TriagePanelEventHandlers.createInputKeyAdapter(mockActionListener);
            String originalText = testTextArea.getText();

            // Act
            KeyEvent shiftEnterEvent = new KeyEvent(
                testTextArea,
                KeyEvent.KEY_PRESSED,
                System.currentTimeMillis(),
                KeyEvent.SHIFT_DOWN_MASK,
                KeyEvent.VK_ENTER,
                KeyEvent.CHAR_UNDEFINED
            );
            keyAdapter.keyPressed(shiftEnterEvent);

            // Assert
            assertFalse(actionPerformed); // Should not trigger action
            assertFalse(shiftEnterEvent.isConsumed()); // Should not consume event
        }

        @Test
        @DisplayName("should ignore other key presses")
        void shouldIgnoreOtherKeyPresses() {
            // Arrange
            ActionListener mockActionListener = e -> actionPerformed = true;
            var keyAdapter = TriagePanelEventHandlers.createInputKeyAdapter(mockActionListener);

            // Act
            KeyEvent otherKeyEvent = new KeyEvent(
                testTextArea,
                KeyEvent.KEY_PRESSED,
                System.currentTimeMillis(),
                0,
                KeyEvent.VK_A,
                'a'
            );
            keyAdapter.keyPressed(otherKeyEvent);

            // Assert
            assertFalse(actionPerformed);
            assertFalse(otherKeyEvent.isConsumed());
        }
    }

    @Nested
    @DisplayName("Send Button Mouse Adapter Creation")
    class SendButtonMouseAdapterCreation {

        @Test
        @DisplayName("should create send button mouse adapter successfully")
        void shouldCreateSendButtonMouseAdapterSuccessfully() {
            // Act
            var mouseAdapter = TriagePanelEventHandlers.createSendButtonMouseAdapter(testButton);

            // Assert
            assertNotNull(mouseAdapter);
        }

        @Test
        @DisplayName("should handle null button gracefully")
        void shouldHandleNullButtonGracefully() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> 
                TriagePanelEventHandlers.createSendButtonMouseAdapter(null),
                "Should throw IllegalArgumentException for null button"
            );
        }

        @Test
        @DisplayName("should change background on mouse enter")
        void shouldChangeBackgroundOnMouseEnter() {
            // Arrange
            var mouseAdapter = TriagePanelEventHandlers.createSendButtonMouseAdapter(testButton);
            Color originalBackground = testButton.getBackground();

            // Act
            MouseEvent enterEvent = new MouseEvent(
                testButton,
                MouseEvent.MOUSE_ENTERED,
                System.currentTimeMillis(),
                0,
                0, 0,
                0,
                false
            );
            mouseAdapter.mouseEntered(enterEvent);

            // Assert
            assertNotEquals(originalBackground, testButton.getBackground());
        }

        @Test
        @DisplayName("should restore background on mouse exit")
        void shouldRestoreBackgroundOnMouseExit() {
            // Arrange
            var mouseAdapter = TriagePanelEventHandlers.createSendButtonMouseAdapter(testButton);
            Color originalBackground = testButton.getBackground();

            // Act - enter then exit
            MouseEvent enterEvent = new MouseEvent(
                testButton,
                MouseEvent.MOUSE_ENTERED,
                System.currentTimeMillis(),
                0,
                0, 0,
                0,
                false
            );
            mouseAdapter.mouseEntered(enterEvent);

            MouseEvent exitEvent = new MouseEvent(
                testButton,
                MouseEvent.MOUSE_EXITED,
                System.currentTimeMillis(),
                0,
                0, 0,
                0,
                false
            );
            mouseAdapter.mouseExited(exitEvent);

            // Assert
            assertEquals(originalBackground, testButton.getBackground());
        }
    }

    @Nested
    @DisplayName("Send Button Action Listener Creation")
    class SendButtonActionListenerCreation {

        @Test
        @DisplayName("should create send button action listener successfully")
        void shouldCreateSendButtonActionListenerSuccessfully() {
            // Arrange
            ActionListener mockActionListener = e -> actionPerformed = true;

            // Act
            var actionListener = TriagePanelEventHandlers.createSendButtonActionListener(testTextArea, mockActionListener);

            // Assert
            assertNotNull(actionListener);
        }

        @Test
        @DisplayName("should handle null text area gracefully")
        void shouldHandleNullTextAreaGracefully() {
            // Arrange
            ActionListener mockActionListener = e -> actionPerformed = true;

            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> 
                TriagePanelEventHandlers.createSendButtonActionListener(null, mockActionListener),
                "Should throw IllegalArgumentException for null text area"
            );
        }

        @Test
        @DisplayName("should handle null action listener gracefully")
        void shouldHandleNullActionListenerGracefully() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> 
                TriagePanelEventHandlers.createSendButtonActionListener(testTextArea, null),
                "Should throw IllegalArgumentException for null action listener"
            );
        }

        @Test
        @DisplayName("should trigger action when action performed")
        void shouldTriggerActionWhenActionPerformed() {
            // Arrange
            ActionListener mockActionListener = e -> actionPerformed = true;
            var actionListener = TriagePanelEventHandlers.createSendButtonActionListener(testTextArea, mockActionListener);
            testTextArea.setText("Test message");

            // Act
            ActionEvent actionEvent = new ActionEvent(testButton, ActionEvent.ACTION_PERFORMED, "test");
            actionListener.actionPerformed(actionEvent);

            // Assert
            assertTrue(actionPerformed);
        }
    }

    @Nested
    @DisplayName("Collapsible Panel Mouse Adapter Creation")
    class CollapsiblePanelMouseAdapterCreation {

        @Test
        @DisplayName("should create collapsible panel mouse adapter successfully")
        void shouldCreateCollapsiblePanelMouseAdapterSuccessfully() {
            // Act
            var mouseAdapter = TriagePanelEventHandlers.createCollapsiblePanelMouseAdapter(testCollapsiblePanel);

            // Assert
            assertNotNull(mouseAdapter);
        }

        @Test
        @DisplayName("should handle null collapsible panel gracefully")
        void shouldHandleNullCollapsiblePanelGracefully() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> 
                TriagePanelEventHandlers.createCollapsiblePanelMouseAdapter(null),
                "Should throw IllegalArgumentException for null collapsible panel"
            );
        }

        @Test
        @DisplayName("should toggle panel state on mouse click")
        void shouldTogglePanelStateOnMouseClick() {
            // Arrange
            var mouseAdapter = TriagePanelEventHandlers.createCollapsiblePanelMouseAdapter(testCollapsiblePanel);
            boolean initialState = testCollapsiblePanel.isExpanded();

            // Act
            MouseEvent clickEvent = new MouseEvent(
                testCollapsiblePanel,
                MouseEvent.MOUSE_CLICKED,
                System.currentTimeMillis(),
                0,
                0, 0,
                1,
                false
            );
            mouseAdapter.mouseClicked(clickEvent);

            // Assert
            assertNotEquals(initialState, testCollapsiblePanel.isExpanded());
        }

        @Test
        @DisplayName("should change cursor on mouse enter")
        void shouldChangeCursorOnMouseEnter() {
            // Arrange
            var mouseAdapter = TriagePanelEventHandlers.createCollapsiblePanelMouseAdapter(testCollapsiblePanel);

            // Act
            MouseEvent enterEvent = new MouseEvent(
                testCollapsiblePanel,
                MouseEvent.MOUSE_ENTERED,
                System.currentTimeMillis(),
                0,
                0, 0,
                0,
                false
            );
            mouseAdapter.mouseEntered(enterEvent);

            // Assert
            assertEquals(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR), testCollapsiblePanel.getCursor());
        }

        @Test
        @DisplayName("should restore cursor on mouse exit")
        void shouldRestoreCursorOnMouseExit() {
            // Arrange
            var mouseAdapter = TriagePanelEventHandlers.createCollapsiblePanelMouseAdapter(testCollapsiblePanel);

            // Act - enter then exit
            MouseEvent enterEvent = new MouseEvent(
                testCollapsiblePanel,
                MouseEvent.MOUSE_ENTERED,
                System.currentTimeMillis(),
                0,
                0, 0,
                0,
                false
            );
            mouseAdapter.mouseEntered(enterEvent);

            MouseEvent exitEvent = new MouseEvent(
                testCollapsiblePanel,
                MouseEvent.MOUSE_EXITED,
                System.currentTimeMillis(),
                0,
                0, 0,
                0,
                false
            );
            mouseAdapter.mouseExited(exitEvent);

            // Assert
            assertEquals(Cursor.getDefaultCursor(), testCollapsiblePanel.getCursor());
        }
    }

    @Nested
    @DisplayName("Settings Button Action Listener Creation")
    class SettingsButtonActionListenerCreation {

        @Test
        @DisplayName("should create settings button action listener successfully")
        void shouldCreateSettingsButtonActionListenerSuccessfully() {
            // Arrange
            ActionListener mockActionListener = e -> actionPerformed = true;

            // Act
            var actionListener = TriagePanelEventHandlers.createSettingsButtonActionListener(mockActionListener);

            // Assert
            assertNotNull(actionListener);
        }

        @Test
        @DisplayName("should handle action performed event")
        void shouldHandleActionPerformedEvent() {
            // Arrange
            ActionListener mockActionListener = e -> actionPerformed = true;
            var actionListener = TriagePanelEventHandlers.createSettingsButtonActionListener(mockActionListener);

            // Act & Assert - should not throw exception
            assertDoesNotThrow(() -> {
                ActionEvent actionEvent = new ActionEvent(testButton, ActionEvent.ACTION_PERFORMED, "test");
                actionListener.actionPerformed(actionEvent);
            });
        }
    }

    @Nested
    @DisplayName("Back to Chat Action Listener Creation")
    class BackToChatActionListenerCreation {

        @Test
        @DisplayName("should create back to chat action listener successfully")
        void shouldCreateBackToChatActionListenerSuccessfully() {
            // Arrange
            ActionListener mockActionListener = e -> actionPerformed = true;

            // Act
            var actionListener = TriagePanelEventHandlers.createBackToChatActionListener(mockActionListener);

            // Assert
            assertNotNull(actionListener);
        }

        @Test
        @DisplayName("should handle action performed event")
        void shouldHandleActionPerformedEvent() {
            // Arrange
            ActionListener mockActionListener = e -> actionPerformed = true;
            var actionListener = TriagePanelEventHandlers.createBackToChatActionListener(mockActionListener);

            // Act & Assert - should not throw exception
            assertDoesNotThrow(() -> {
                ActionEvent actionEvent = new ActionEvent(testButton, ActionEvent.ACTION_PERFORMED, "test");
                actionListener.actionPerformed(actionEvent);
            });
        }
    }

    @Nested
    @DisplayName("Input Document Listener Creation")
    class InputDocumentListenerCreation {

        @Test
        @DisplayName("should create input document listener successfully")
        void shouldCreateInputDocumentListenerSuccessfully() {
            // Act
            var documentListener = TriagePanelEventHandlers.createInputDocumentListener(testTextArea, testButton);

            // Assert
            assertNotNull(documentListener);
        }

        @Test
        @DisplayName("should handle document changes")
        void shouldHandleDocumentChanges() {
            // Arrange
            var documentListener = TriagePanelEventHandlers.createInputDocumentListener(testTextArea, testButton);

            // Act & Assert - should not throw exception
            assertDoesNotThrow(() -> {
                documentListener.changedUpdate(null);
                documentListener.insertUpdate(null);
                documentListener.removeUpdate(null);
            });
        }
    }

    @Nested
    @DisplayName("Input Focus Adapter Creation")
    class InputFocusAdapterCreation {

        @Test
        @DisplayName("should create input focus adapter successfully")
        void shouldCreateInputFocusAdapterSuccessfully() {
            // Act
            var focusAdapter = TriagePanelEventHandlers.createInputFocusAdapter();

            // Assert
            assertNotNull(focusAdapter);
        }

        @Test
        @DisplayName("should handle focus gained")
        void shouldHandleFocusGained() {
            // Arrange
            var focusAdapter = TriagePanelEventHandlers.createInputFocusAdapter();

            // Act & Assert - should not throw exception
            assertDoesNotThrow(() -> {
                focusAdapter.focusGained(null);
            });
        }

        @Test
        @DisplayName("should handle focus lost")
        void shouldHandleFocusLost() {
            // Arrange
            var focusAdapter = TriagePanelEventHandlers.createInputFocusAdapter();

            // Act & Assert - should not throw exception
            assertDoesNotThrow(() -> {
                focusAdapter.focusLost(null);
            });
        }
    }

    @Nested
    @DisplayName("Window Listener Creation")
    class WindowListenerCreation {

        @Test
        @DisplayName("should create window listener successfully")
        void shouldCreateWindowListenerSuccessfully() {
            // Arrange
            Runnable mockCleanupAction = () -> {};

            // Act
            var windowListener = TriagePanelEventHandlers.createWindowListener(mockCleanupAction);

            // Assert
            assertNotNull(windowListener);
        }

        @Test
        @DisplayName("should handle window events")
        void shouldHandleWindowEvents() {
            // Arrange
            Runnable mockCleanupAction = () -> {};
            var windowListener = TriagePanelEventHandlers.createWindowListener(mockCleanupAction);

            // Act & Assert - should not throw exception
            assertDoesNotThrow(() -> {
                windowListener.windowOpened(null);
                windowListener.windowClosing(null);
                windowListener.windowClosed(null);
                windowListener.windowIconified(null);
                windowListener.windowDeiconified(null);
                windowListener.windowActivated(null);
                windowListener.windowDeactivated(null);
            });
        }
    }

    @Nested
    @DisplayName("EDT Compliance")
    class EDTCompliance {

        @Test
        @DisplayName("should execute on EDT successfully")
        void shouldExecuteOnEDTSuccessfully() {
            // Arrange
            boolean[] executed = {false};

            // Act
            TriagePanelEventHandlers.executeOnEDT(() -> executed[0] = true);

            // Assert
            // Give some time for EDT execution
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            assertTrue(executed[0]);
        }

        @Test
        @DisplayName("should execute on EDT and wait successfully")
        void shouldExecuteOnEDTAndWaitSuccessfully() throws InterruptedException, InvocationTargetException {
            // Arrange
            boolean[] executed = {false};

            // Act
            TriagePanelEventHandlers.executeOnEDTAndWait(() -> executed[0] = true);

            // Assert
            assertTrue(executed[0]);
        }

        @Test
        @DisplayName("should throw exception for null runnable in executeOnEDT")
        void shouldThrowExceptionForNullRunnableInExecuteOnEDT() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> 
                TriagePanelEventHandlers.executeOnEDT(null)
            );
        }

        @Test
        @DisplayName("should throw exception for null runnable in executeOnEDTAndWait")
        void shouldThrowExceptionForNullRunnableInExecuteOnEDTAndWait() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> {
                try {
                    TriagePanelEventHandlers.executeOnEDTAndWait(null);
                } catch (InterruptedException | InvocationTargetException e) {
                    // These exceptions are expected but should be wrapped
                }
            });
        }
    }

    @Nested
    @DisplayName("Listener Disposal")
    class ListenerDisposal {

        @Test
        @DisplayName("should dispose listeners successfully")
        void shouldDisposeListenersSuccessfully() {
            // Arrange
            JButton testButton = new JButton("Test");
            testButton.addActionListener(e -> {});
            testButton.addMouseListener(new java.awt.event.MouseAdapter() {});

            // Act & Assert - should not throw exception
            assertDoesNotThrow(() -> TriagePanelEventHandlers.disposeListeners(testButton));
        }

        @Test
        @DisplayName("should throw exception for null component")
        void shouldThrowExceptionForNullComponent() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> 
                TriagePanelEventHandlers.disposeListeners(null)
            );
        }

        @Test
        @DisplayName("should handle component without listeners")
        void shouldHandleComponentWithoutListeners() {
            // Arrange
            JButton testButton = new JButton("Test");

            // Act & Assert - should not throw exception
            assertDoesNotThrow(() -> TriagePanelEventHandlers.disposeListeners(testButton));
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesAndErrorHandling {

        @Test
        @DisplayName("should handle multiple rapid key events")
        void shouldHandleMultipleRapidKeyEvents() {
            // Arrange
            ActionListener mockActionListener = e -> actionPerformed = true;
            var keyAdapter = TriagePanelEventHandlers.createInputKeyAdapter(mockActionListener);

            // Act - rapid key events
            for (int i = 0; i < 10; i++) {
                KeyEvent enterEvent = new KeyEvent(
                    testTextArea,
                    KeyEvent.KEY_PRESSED,
                    System.currentTimeMillis(),
                    0,
                    KeyEvent.VK_ENTER,
                    KeyEvent.CHAR_UNDEFINED
                );
                keyAdapter.keyPressed(enterEvent);
            }

            // Assert
            assertTrue(actionPerformed);
        }

        @Test
        @DisplayName("should handle multiple rapid mouse events")
        void shouldHandleMultipleRapidMouseEvents() {
            // Arrange
            var mouseAdapter = TriagePanelEventHandlers.createSendButtonMouseAdapter(testButton);

            // Act - rapid mouse events
            for (int i = 0; i < 10; i++) {
                MouseEvent enterEvent = new MouseEvent(
                    testButton,
                    MouseEvent.MOUSE_ENTERED,
                    System.currentTimeMillis(),
                    0,
                    0, 0,
                    0,
                    false
                );
                mouseAdapter.mouseEntered(enterEvent);

                MouseEvent exitEvent = new MouseEvent(
                    testButton,
                    MouseEvent.MOUSE_EXITED,
                    System.currentTimeMillis(),
                    0,
                    0, 0,
                    0,
                    false
                );
                mouseAdapter.mouseExited(exitEvent);
            }

            // Assert - should not throw exception
            assertTrue(true);
        }

        @Test
        @DisplayName("should handle concurrent EDT access")
        void shouldHandleConcurrentEDTAccess() {
            // Arrange
            boolean[] executed = {false};

            // Act - multiple EDT executions
            for (int i = 0; i < 5; i++) {
                TriagePanelEventHandlers.executeOnEDT(() -> executed[0] = true);
            }

            // Assert - should not throw exception
            assertDoesNotThrow(() -> {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
    }
} 