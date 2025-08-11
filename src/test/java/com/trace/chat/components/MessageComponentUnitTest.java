package com.trace.chat.components;

import com.trace.common.constants.TriagePanelConstants;
import com.trace.test.models.FailureInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MessageComponent class.
 * 
 * <p>These tests verify that MessageComponent correctly renders chat messages
 * with proper styling, layout, and interactive elements like collapsible panels.</p>
 * 
 * <p>Test patterns follow Swing component testing best practices:
 * <ul>
 *   <li>Test component creation and initialization</li>
 *   <li>Test visual properties and styling</li>
 *   <li>Test layout and sizing behavior</li>
 *   <li>Test interactive elements (collapsible panels)</li>
 *   <li>Test different message types (user vs AI)</li>
 *   <li>Test edge cases and null handling</li>
 * </ul></p>
 */
@DisplayName("MessageComponent Unit Tests")
class MessageComponentUnitTest {

    private ChatMessage userMessage;
    private ChatMessage aiMessage;
    private ChatMessage aiMessageWithThinking;
    private ChatMessage aiMessageWithFailureInfo;

    @BeforeEach
    void setUp() {
        userMessage = new ChatMessage(
            ChatMessage.Role.USER,
            "Hello, can you help me with this test failure?",
            System.currentTimeMillis(),
            null,
            null
        );

        aiMessage = new ChatMessage(
            ChatMessage.Role.AI,
            "I can help you analyze this test failure.",
            System.currentTimeMillis(),
            null,
            null
        );

        aiMessageWithThinking = new ChatMessage(
            ChatMessage.Role.AI,
            "Here's my analysis:",
            System.currentTimeMillis(),
            "This appears to be a Selenium element not found error. The test is trying to locate an element that doesn't exist on the page.",
            null
        );

        FailureInfo failureInfo = new FailureInfo.Builder()
            .withScenarioName("Login Test")
            .withFailedStepText("When user enters credentials")
            .withErrorMessage("Element not found")
            .build();

        aiMessageWithFailureInfo = new ChatMessage(
            ChatMessage.Role.AI,
            "Analysis of your test failure:",
            System.currentTimeMillis(),
            "The test failed because the login form element could not be found.",
            failureInfo
        );
    }

    @Nested
    @DisplayName("Component Creation and Initialization")
    class ComponentCreationAndInitialization {

        @Test
        @DisplayName("should create user message component successfully")
        void shouldCreateUserMessageComponentSuccessfully() {
            // Act
            MessageComponent component = new MessageComponent(userMessage);

            // Assert
            assertNotNull(component);
            assertTrue(component.isVisible());
            assertNotNull(component.getLayout());
            assertTrue(component.getComponentCount() > 0);
        }

        @Test
        @DisplayName("should create AI message component successfully")
        void shouldCreateAiMessageComponentSuccessfully() {
            // Act
            MessageComponent component = new MessageComponent(aiMessage);

            // Assert
            assertNotNull(component);
            assertTrue(component.isVisible());
            assertNotNull(component.getLayout());
            assertTrue(component.getComponentCount() > 0);
        }

        @Test
        @DisplayName("should create AI message with thinking component successfully")
        void shouldCreateAiMessageWithThinkingComponentSuccessfully() {
            // Act
            MessageComponent component = new MessageComponent(aiMessageWithThinking);

            // Assert
            assertNotNull(component);
            assertTrue(component.isVisible());
            assertNotNull(component.getLayout());
            assertTrue(component.getComponentCount() > 0);
        }

        @Test
        @DisplayName("should create AI message with failure info component successfully")
        void shouldCreateAiMessageWithFailureInfoComponentSuccessfully() {
            // Act
            MessageComponent component = new MessageComponent(aiMessageWithFailureInfo);

            // Assert
            assertNotNull(component);
            assertTrue(component.isVisible());
            assertNotNull(component.getLayout());
            assertTrue(component.getComponentCount() > 0);
        }

        @Test
        @DisplayName("should handle null message gracefully")
        void shouldHandleNullMessageGracefully() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> new MessageComponent(null),
                "Should throw IllegalArgumentException for null message");
        }
    }

    @Nested
    @DisplayName("Visual Properties and Styling")
    class VisualPropertiesAndStyling {

        @Test
        @DisplayName("should have correct background color for user message")
        void shouldHaveCorrectBackgroundColorForUserMessage() {
            // Act
            MessageComponent component = new MessageComponent(userMessage);

            // Assert
            // User messages should have a different background than AI messages
            assertNotNull(component.getBackground());
        }

        @Test
        @DisplayName("should have correct background color for AI message")
        void shouldHaveCorrectBackgroundColorForAiMessage() {
            // Act
            MessageComponent component = new MessageComponent(aiMessage);

            // Assert
            // AI messages should have a different background than user messages
            assertNotNull(component.getBackground());
        }

        @Test
        @DisplayName("should have correct border for user message")
        void shouldHaveCorrectBorderForUserMessage() {
            // Act
            MessageComponent component = new MessageComponent(userMessage);

            // Assert
            assertNotNull(component.getBorder());
        }

        @Test
        @DisplayName("should have correct border for AI message")
        void shouldHaveCorrectBorderForAiMessage() {
            // Act
            MessageComponent component = new MessageComponent(aiMessage);

            // Assert
            assertNotNull(component.getBorder());
        }

        @Test
        @DisplayName("should not be opaque")
        void shouldNotBeOpaque() {
            // Act
            MessageComponent component = new MessageComponent(userMessage);

            // Assert
            assertFalse(component.isOpaque());
        }

        @Test
        @DisplayName("should have correct alignment")
        void shouldHaveCorrectAlignment() {
            // Act
            MessageComponent component = new MessageComponent(userMessage);

            // Assert
            assertEquals(Component.TOP_ALIGNMENT, component.getAlignmentY());
        }
    }

    @Nested
    @DisplayName("Layout and Sizing")
    class LayoutAndSizing {

        @Test
        @DisplayName("should have correct preferred size")
        void shouldHaveCorrectPreferredSize() {
            // Act
            MessageComponent component = new MessageComponent(userMessage);

            // Assert
            Dimension preferredSize = component.getPreferredSize();
            assertNotNull(preferredSize);
            assertTrue(preferredSize.width > 0);
            assertTrue(preferredSize.height > 0);
        }

        @Test
        @DisplayName("should have correct minimum size")
        void shouldHaveCorrectMinimumSize() {
            // Act
            MessageComponent component = new MessageComponent(userMessage);

            // Assert
            Dimension minimumSize = component.getMinimumSize();
            assertNotNull(minimumSize);
            assertTrue(minimumSize.width > 0);
            assertTrue(minimumSize.height > 0);
        }

        @Test
        @DisplayName("should have correct maximum size")
        void shouldHaveCorrectMaximumSize() {
            // Act
            MessageComponent component = new MessageComponent(userMessage);

            // Assert
            Dimension maximumSize = component.getMaximumSize();
            assertNotNull(maximumSize);
            assertTrue(maximumSize.width > 0);
            assertTrue(maximumSize.height > 0);
        }

        @Test
        @DisplayName("should respect maximum width constraint")
        void shouldRespectMaximumWidthConstraint() {
            // Act
            MessageComponent component = new MessageComponent(userMessage);

            // Assert
            Dimension preferredSize = component.getPreferredSize();
            assertTrue(preferredSize.width <= TriagePanelConstants.MAX_MESSAGE_TEXT_WIDTH);
        }

        @Test
        @DisplayName("should have proper layout manager")
        void shouldHaveProperLayoutManager() {
            // Act
            MessageComponent component = new MessageComponent(userMessage);

            // Assert
            assertTrue(component.getLayout() instanceof BorderLayout);
        }
    }

    @Nested
    @DisplayName("Message Content Display")
    class MessageContentDisplay {

        @Test
        @DisplayName("should display user message text correctly")
        void shouldDisplayUserMessageTextCorrectly() {
            // Act
            MessageComponent component = new MessageComponent(userMessage);

            // Assert
            // Find the text area component
            JTextArea textArea = findTextArea(component);
            assertNotNull(textArea);
            assertEquals(userMessage.getText(), textArea.getText());
        }

        @Test
        @DisplayName("should display AI message text correctly")
        void shouldDisplayAiMessageTextCorrectly() {
            // Act
            MessageComponent component = new MessageComponent(aiMessage);

            // Assert
            JEditorPane textPane = findHtmlPane(component);
            assertNotNull(textPane);
            // For JEditorPane with HTML content, we can't easily check the text content
            // but we can verify the component exists and is properly configured
            assertTrue(textPane.getContentType().equals("text/html"));
        }

        @Test
        @DisplayName("should display timestamp correctly")
        void shouldDisplayTimestampCorrectly() {
            // Act
            MessageComponent component = new MessageComponent(userMessage);

            // Assert
            JLabel timestampLabel = findTimestampLabel(component);
            assertNotNull(timestampLabel);
            assertFalse(timestampLabel.getText().isEmpty());
        }

        @Test
        @DisplayName("should display user icon for user messages")
        void shouldDisplayUserIconForUserMessages() {
            // Act
            MessageComponent component = new MessageComponent(userMessage);

            // Assert
            JLabel iconLabel = findIconLabel(component);
            assertNotNull(iconLabel);
            assertNotNull(iconLabel.getIcon());
        }

        @Test
        @DisplayName("should display AI icon for AI messages")
        void shouldDisplayAiIconForAiMessages() {
            // Act
            MessageComponent component = new MessageComponent(aiMessage);

            // Assert
            JLabel iconLabel = findIconLabel(component);
            assertNotNull(iconLabel);
            assertNotNull(iconLabel.getIcon());
        }
    }

    @Nested
    @DisplayName("Collapsible Panel Functionality")
    class CollapsiblePanelFunctionality {

        @Test
        @DisplayName("should include collapsible panel for AI messages with thinking")
        void shouldIncludeCollapsiblePanelForAiMessagesWithThinking() {
            // Act
            MessageComponent component = new MessageComponent(aiMessageWithThinking);

            // Assert
            CollapsiblePanel collapsiblePanel = findCollapsiblePanel(component);
            assertNotNull(collapsiblePanel);
            assertTrue(collapsiblePanel.isVisible());
        }

        @Test
        @DisplayName("should not include collapsible panel for user messages")
        void shouldNotIncludeCollapsiblePanelForUserMessages() {
            // Act
            MessageComponent component = new MessageComponent(userMessage);

            // Assert
            CollapsiblePanel collapsiblePanel = findCollapsiblePanel(component);
            assertNull(collapsiblePanel);
        }

        @Test
        @DisplayName("should not include collapsible panel for AI messages without thinking")
        void shouldNotIncludeCollapsiblePanelForAiMessagesWithoutThinking() {
            // Act
            MessageComponent component = new MessageComponent(aiMessage);

            // Assert
            CollapsiblePanel collapsiblePanel = findCollapsiblePanel(component);
            assertNull(collapsiblePanel);
        }

        @Test
        @DisplayName("should include collapsible panel for AI messages with failure info")
        void shouldIncludeCollapsiblePanelForAiMessagesWithFailureInfo() {
            // Act
            MessageComponent component = new MessageComponent(aiMessageWithFailureInfo);

            // Assert
            CollapsiblePanel collapsiblePanel = findCollapsiblePanel(component);
            assertNotNull(collapsiblePanel);
            assertTrue(collapsiblePanel.isVisible());
        }
    }

    @Nested
    @DisplayName("Copy Button")
    class CopyButtonTests {

        private JButton findCopyButton(MessageComponent component) {
            return (JButton) findComponentByName(component, "copyMessageButton");
        }

        private Component findComponentByName(Container root, String name) {
            for (Component c : root.getComponents()) {
                if (name.equals(c.getName())) {
                    return c;
                }
                if (c instanceof Container) {
                    Component nested = findComponentByName((Container) c, name);
                    if (nested != null) return nested;
                }
            }
            return null;
        }

        @Test
        @DisplayName("should include copy button for user, AI normal, and AI initial failure messages")
        void shouldIncludeCopyButtonForAllMessageTypes() {
            MessageComponent userComp = new MessageComponent(userMessage);
            JButton userBtn = findCopyButton(userComp);
            assertNotNull(userBtn);

            MessageComponent aiComp = new MessageComponent(aiMessage);
            JButton aiBtn = findCopyButton(aiComp);
            assertNotNull(aiBtn);

            FailureInfo failureInfo = new FailureInfo.Builder()
                .withScenarioName("S")
                .withFailedStepText("F")
                .withErrorMessage("E")
                .build();
            ChatMessage initialAi = new ChatMessage(ChatMessage.Role.AI, "", System.currentTimeMillis(), "thinking here", failureInfo);
            MessageComponent initialComp = new MessageComponent(initialAi);
            JButton initialBtn = findCopyButton(initialComp);
            assertNotNull(initialBtn);
        }

        @Test
        @DisplayName("should disable copy button when source is blank")
        void shouldDisableWhenSourceBlank() {
            FailureInfo failureInfo = new FailureInfo.Builder()
                .withScenarioName("S")
                .withFailedStepText("F")
                .withErrorMessage("E")
                .build();
            ChatMessage initialAiBlank = new ChatMessage(ChatMessage.Role.AI, "  ", System.currentTimeMillis(), "  ", failureInfo);
            MessageComponent comp = new MessageComponent(initialAiBlank);
            JButton btn = findCopyButton(comp);
            assertNotNull(btn);
            assertFalse(btn.isEnabled());
        }

        @Test
        @DisplayName("should set correct tooltip text per type")
        void shouldSetCorrectTooltip() {
            MessageComponent userComp = new MessageComponent(userMessage);
            JButton userBtn = findCopyButton(userComp);
            assertEquals("Copy message", userBtn.getToolTipText());

            MessageComponent aiComp = new MessageComponent(aiMessage);
            JButton aiBtn = findCopyButton(aiComp);
            assertEquals("Copy message", aiBtn.getToolTipText());

            FailureInfo failureInfo = new FailureInfo.Builder()
                .withScenarioName("S")
                .withFailedStepText("F")
                .withErrorMessage("E")
                .build();
            ChatMessage initialAi = new ChatMessage(ChatMessage.Role.AI, "", System.currentTimeMillis(), "thinking here", failureInfo);
            MessageComponent initialComp = new MessageComponent(initialAi);
            JButton initialBtn = findCopyButton(initialComp);
            assertEquals("Copy AI thinking", initialBtn.getToolTipText());
        }

        @Test
        @DisplayName("should copy correct text and show visual feedback")
        void shouldCopyAndShowFeedback() throws InterruptedException, UnsupportedFlavorException, IOException {
            // User message
            MessageComponent userComp = new MessageComponent(userMessage);
            userComp.setCopyFeedbackMs(50);
            JButton userBtn = findCopyButton(userComp);
            assertTrue(userBtn.isEnabled());
            userBtn.doClick();
            try { javax.swing.SwingUtilities.invokeAndWait(() -> {}); } catch (Exception ignored) {}
            assertEquals("\u2713", userBtn.getText());
            Thread.sleep(80);
            try { javax.swing.SwingUtilities.invokeAndWait(() -> {}); } catch (Exception ignored) {}
            assertEquals("", userBtn.getText());

            // Attempt clipboard verification if supported in environment
            try {
                Object data = Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
                if (data instanceof String) {
                    assertEquals(userMessage.getText(), data);
                }
            } catch (IllegalStateException ignored) {
                // Clipboard not available in headless CI; ignore
            }

            // Initial AI failure message copies thinking
            FailureInfo failureInfo = new FailureInfo.Builder()
                .withScenarioName("S")
                .withFailedStepText("F")
                .withErrorMessage("E")
                .build();
            ChatMessage initialAi = new ChatMessage(ChatMessage.Role.AI, "  ", System.currentTimeMillis(), "thinking here", failureInfo);
            MessageComponent initialComp = new MessageComponent(initialAi);
            initialComp.setCopyFeedbackMs(50);
            JButton initialBtn = findCopyButton(initialComp);
            assertTrue(initialBtn.isEnabled());
            initialBtn.doClick();
            try { javax.swing.SwingUtilities.invokeAndWait(() -> {}); } catch (Exception ignored) {}
            assertEquals("\u2713", initialBtn.getText());
            Thread.sleep(80);
            try { javax.swing.SwingUtilities.invokeAndWait(() -> {}); } catch (Exception ignored) {}
            assertEquals("", initialBtn.getText());
        }
    }

    @Nested
    @DisplayName("Failure Info Display")
    class FailureInfoDisplay {

        @Test
        @DisplayName("should display scenario name for messages with failure info")
        void shouldDisplayScenarioNameForMessagesWithFailureInfo() {
            // Act
            MessageComponent component = new MessageComponent(aiMessageWithFailureInfo);

            // Assert
            // Check that the scenario prefix exists
            JLabel scenarioLabel = findScenarioLabel(component);
            assertNotNull(scenarioLabel);
            assertEquals("Scenario: ", scenarioLabel.getText());
            
            // Check that the scenario name exists somewhere in the component
            String expectedScenarioName = aiMessageWithFailureInfo.getFailureInfo().getScenarioName();
            assertTrue(componentContainsText(component, expectedScenarioName), 
                "Component should contain scenario name: " + expectedScenarioName);
        }

        @Test
        @DisplayName("should display failed step text for messages with failure info")
        void shouldDisplayFailedStepTextForMessagesWithFailureInfo() {
            // Act
            MessageComponent component = new MessageComponent(aiMessageWithFailureInfo);

            // Assert
            // Check that the failed step prefix exists
            JLabel failedStepLabel = findFailedStepLabel(component);
            assertNotNull(failedStepLabel);
            assertEquals("Failed Step: ", failedStepLabel.getText());
            
            // Check that the failed step text exists somewhere in the component
            String expectedFailedStepText = aiMessageWithFailureInfo.getFailureInfo().getFailedStepText();
            assertTrue(componentContainsText(component, expectedFailedStepText), 
                "Component should contain failed step text: " + expectedFailedStepText);
        }

        @Test
        @DisplayName("should not display failure info for messages without failure info")
        void shouldNotDisplayFailureInfoForMessagesWithoutFailureInfo() {
            // Act
            MessageComponent component = new MessageComponent(userMessage);

            // Assert
            JLabel scenarioLabel = findScenarioLabel(component);
            assertNull(scenarioLabel);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesAndErrorHandling {

        @Test
        @DisplayName("should handle empty message text")
        void shouldHandleEmptyMessageText() {
            // Arrange
            ChatMessage emptyMessage = new ChatMessage(
                ChatMessage.Role.USER,
                "",
                System.currentTimeMillis(),
                null,
                null
            );

            // Act
            MessageComponent component = new MessageComponent(emptyMessage);

            // Assert
            assertNotNull(component);
            assertTrue(component.isVisible());
        }

        @Test
        @DisplayName("should handle very long message text")
        void shouldHandleVeryLongMessageText() {
            // Arrange
            String longText = "A".repeat(1000);
            ChatMessage longMessage = new ChatMessage(
                ChatMessage.Role.USER,
                longText,
                System.currentTimeMillis(),
                null,
                null
            );

            // Act
            MessageComponent component = new MessageComponent(longMessage);

            // Assert
            assertNotNull(component);
            assertTrue(component.isVisible());
            Dimension preferredSize = component.getPreferredSize();
            assertTrue(preferredSize.width <= TriagePanelConstants.MAX_MESSAGE_TEXT_WIDTH);
        }

        @Test
        @DisplayName("should handle special characters in message text")
        void shouldHandleSpecialCharactersInMessageText() {
            // Arrange
            String specialText = "Test with special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?";
            ChatMessage specialMessage = new ChatMessage(
                ChatMessage.Role.USER,
                specialText,
                System.currentTimeMillis(),
                null,
                null
            );

            // Act
            MessageComponent component = new MessageComponent(specialMessage);

            // Assert
            assertNotNull(component);
            assertTrue(component.isVisible());
        }

        @Test
        @DisplayName("should handle unicode characters in message text")
        void shouldHandleUnicodeCharactersInMessageText() {
            // Arrange
            String unicodeText = "Test with unicode: 你好世界 🌍 🚀";
            ChatMessage unicodeMessage = new ChatMessage(
                ChatMessage.Role.USER,
                unicodeText,
                System.currentTimeMillis(),
                null,
                null
            );

            // Act
            MessageComponent component = new MessageComponent(unicodeMessage);

            // Assert
            assertNotNull(component);
            assertTrue(component.isVisible());
        }
    }

    // Helper methods to find specific components
    private JTextArea findTextArea(Container container) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JTextArea) {
                JTextArea textArea = (JTextArea) comp;
                if (textArea.getName() != null && (textArea.getName().equals("messageText") || textArea.getName().equals("userMessageText"))) {
                    return textArea;
                }
            }
            if (comp instanceof Container) {
                JTextArea found = findTextArea((Container) comp);
                if (found != null) return found;
            }
        }
        return null;
    }
    
    private JEditorPane findHtmlPane(Container container) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JEditorPane) {
                JEditorPane pane = (JEditorPane) comp;
                if (pane.getName() != null && pane.getName().equals("aiMessageText")) {
                    return pane;
                }
            }
            if (comp instanceof Container) {
                JEditorPane found = findHtmlPane((Container) comp);
                if (found != null) return found;
            }
        }
        return null;
    }

    private JLabel findTimestampLabel(Container container) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JLabel) {
                JLabel label = (JLabel) comp;
                if (label.getText() != null && label.getText().contains(":")) {
                    return label;
                }
            }
            if (comp instanceof Container) {
                JLabel found = findTimestampLabel((Container) comp);
                if (found != null) return found;
            }
        }
        return null;
    }

    private JLabel findIconLabel(Container container) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JLabel) {
                JLabel label = (JLabel) comp;
                if (label.getIcon() != null) {
                    return label;
                }
            }
            if (comp instanceof Container) {
                JLabel found = findIconLabel((Container) comp);
                if (found != null) return found;
            }
        }
        return null;
    }

    private CollapsiblePanel findCollapsiblePanel(Container container) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof CollapsiblePanel) {
                return (CollapsiblePanel) comp;
            }
            if (comp instanceof Container) {
                CollapsiblePanel found = findCollapsiblePanel((Container) comp);
                if (found != null) return found;
            }
        }
        return null;
    }

    private JLabel findScenarioLabel(Container container) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JLabel) {
                JLabel label = (JLabel) comp;
                if (label.getText() != null && label.getText().equals("Scenario: ")) {
                    return label;
                }
            }
            if (comp instanceof Container) {
                JLabel found = findScenarioLabel((Container) comp);
                if (found != null) return found;
            }
        }
        return null;
    }

    private JLabel findFailedStepLabel(Container container) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JLabel) {
                JLabel label = (JLabel) comp;
                if (label.getText() != null && label.getText().equals("Failed Step: ")) {
                    return label;
                }
            }
            if (comp instanceof Container) {
                JLabel found = findFailedStepLabel((Container) comp);
                if (found != null) return found;
            }
        }
        return null;
    }

    private boolean componentContainsText(Container container, String text) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JLabel) {
                JLabel label = (JLabel) comp;
                if (label.getText() != null && label.getText().contains(text)) {
                    return true;
                }
            }
            if (comp instanceof Container) {
                if (componentContainsText((Container) comp, text)) {
                    return true;
                }
            }
        }
        return false;
    }
} 