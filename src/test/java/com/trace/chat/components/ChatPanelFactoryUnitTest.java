package com.trace.chat.components;

import com.trace.common.constants.TriagePanelConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ChatPanelFactory class.
 * 
 * <p>These tests verify that ChatPanelFactory correctly creates chat panel
 * components with proper styling, layout, and functionality.</p>
 */
@DisplayName("ChatPanelFactory Unit Tests")
class ChatPanelFactoryUnitTest {

    @Nested
    @DisplayName("Chat Panel Creation")
    class ChatPanelCreation {

        @Test
        @DisplayName("should create chat panel successfully")
        void shouldCreateChatPanelSuccessfully() {
            // Act
            JPanel chatPanel = ChatPanelFactory.createChatPanel();

            // Assert
            assertNotNull(chatPanel);
            assertTrue(chatPanel.isVisible());
            assertNotNull(chatPanel.getLayout());
            assertTrue(chatPanel.getLayout() instanceof BorderLayout);
        }

        @Test
        @DisplayName("should have correct layout manager")
        void shouldHaveCorrectLayoutManager() {
            // Act
            JPanel chatPanel = ChatPanelFactory.createChatPanel();

            // Assert
            assertTrue(chatPanel.getLayout() instanceof BorderLayout);
        }

        @Test
        @DisplayName("should have correct background color")
        void shouldHaveCorrectBackgroundColor() {
            // Act
            JPanel chatPanel = ChatPanelFactory.createChatPanel();

            // Assert
            assertEquals(TriagePanelConstants.getPanelBackground(), chatPanel.getBackground());
        }

        @Test
        @DisplayName("should have correct border")
        void shouldHaveCorrectBorder() {
            // Act
            JPanel chatPanel = ChatPanelFactory.createChatPanel();

            // Assert
            assertEquals(TriagePanelConstants.MESSAGE_CONTAINER_BORDER, chatPanel.getBorder());
        }

        @Test
        @DisplayName("should have correct alignment")
        void shouldHaveCorrectAlignment() {
            // Act
            JPanel chatPanel = ChatPanelFactory.createChatPanel();

            // Assert
            assertEquals(Component.LEFT_ALIGNMENT, chatPanel.getAlignmentX());
            assertEquals(Component.TOP_ALIGNMENT, chatPanel.getAlignmentY());
        }
    }

    @Nested
    @DisplayName("Message Container Creation")
    class MessageContainerCreation {

        @Test
        @DisplayName("should create message container successfully")
        void shouldCreateMessageContainerSuccessfully() {
            // Act
            JPanel messageContainer = ChatPanelFactory.createMessageContainer();

            // Assert
            assertNotNull(messageContainer);
            assertTrue(messageContainer.isVisible());
            assertNotNull(messageContainer.getLayout());
            assertTrue(messageContainer.getLayout() instanceof BoxLayout);
        }

        @Test
        @DisplayName("should have correct layout manager")
        void shouldHaveCorrectLayoutManager() {
            // Act
            JPanel messageContainer = ChatPanelFactory.createMessageContainer();

            // Assert
            BoxLayout layout = (BoxLayout) messageContainer.getLayout();
            assertEquals(BoxLayout.Y_AXIS, layout.getAxis());
        }

        @Test
        @DisplayName("should have correct background color")
        void shouldHaveCorrectBackgroundColor() {
            // Act
            JPanel messageContainer = ChatPanelFactory.createMessageContainer();

            // Assert
            assertEquals(TriagePanelConstants.getPanelBackground(), messageContainer.getBackground());
        }

        @Test
        @DisplayName("should not be opaque")
        void shouldNotBeOpaque() {
            // Act
            JPanel messageContainer = ChatPanelFactory.createMessageContainer();

            // Assert
            assertFalse(messageContainer.isOpaque());
        }

        @Test
        @DisplayName("should have correct alignment")
        void shouldHaveCorrectAlignment() {
            // Act
            JPanel messageContainer = ChatPanelFactory.createMessageContainer();

            // Assert
            assertEquals(Component.LEFT_ALIGNMENT, messageContainer.getAlignmentX());
            assertEquals(Component.TOP_ALIGNMENT, messageContainer.getAlignmentY());
        }

        @Test
        @DisplayName("should have vertical glue component")
        void shouldHaveVerticalGlueComponent() {
            // Act
            JPanel messageContainer = ChatPanelFactory.createMessageContainer();

            // Assert
            assertTrue(messageContainer.getComponentCount() > 0);
            // Should have vertical glue as the first component
            Component firstComponent = messageContainer.getComponent(0);
            assertTrue(firstComponent instanceof Box.Filler);
        }
    }

    @Nested
    @DisplayName("Scroll Pane Creation")
    class ScrollPaneCreation {

        @Test
        @DisplayName("should create scroll pane successfully")
        void shouldCreateScrollPaneSuccessfully() {
            // Arrange
            JPanel messageContainer = ChatPanelFactory.createMessageContainer();

            // Act
            JScrollPane scrollPane = ChatPanelFactory.createScrollPane(messageContainer);

            // Assert
            assertNotNull(scrollPane);
            assertTrue(scrollPane.isVisible());
            assertEquals(messageContainer, scrollPane.getViewport().getView());
        }

        @Test
        @DisplayName("should handle null message container gracefully")
        void shouldHandleNullMessageContainerGracefully() {
            // Act
            JScrollPane scrollPane = ChatPanelFactory.createScrollPane(null);

            // Assert
            assertNotNull(scrollPane);
            assertTrue(scrollPane.isVisible());
            // Should create scroll pane with empty panel as fallback
            assertNotNull(scrollPane.getViewport().getView());
        }

        @Test
        @DisplayName("should have correct background color")
        void shouldHaveCorrectBackgroundColor() {
            // Arrange
            JPanel messageContainer = ChatPanelFactory.createMessageContainer();

            // Act
            JScrollPane scrollPane = ChatPanelFactory.createScrollPane(messageContainer);

            // Assert
            assertFalse(scrollPane.isOpaque());
            assertFalse(scrollPane.getViewport().isOpaque());
            assertEquals(TriagePanelConstants.getPanelBackground(), scrollPane.getViewport().getBackground());
        }

        @Test
        @DisplayName("should have correct border")
        void shouldHaveCorrectBorder() {
            // Arrange
            JPanel messageContainer = ChatPanelFactory.createMessageContainer();

            // Act
            JScrollPane scrollPane = ChatPanelFactory.createScrollPane(messageContainer);

            // Assert
            assertEquals(TriagePanelConstants.EMPTY_BORDER, scrollPane.getBorder());
        }

        @Test
        @DisplayName("should have proper scroll bar configuration")
        void shouldHaveProperScrollBarConfiguration() {
            // Arrange
            JPanel messageContainer = ChatPanelFactory.createMessageContainer();

            // Act
            JScrollPane scrollPane = ChatPanelFactory.createScrollPane(messageContainer);

            // Assert
            assertEquals(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, scrollPane.getVerticalScrollBarPolicy());
            assertEquals(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED, scrollPane.getHorizontalScrollBarPolicy());
            assertTrue(scrollPane.getVerticalScrollBar().getUnitIncrement() > 0);
        }
    }

    @Nested
    @DisplayName("Vertical Spacing Creation")
    class VerticalSpacingCreation {

        @Test
        @DisplayName("should create vertical spacing successfully")
        void shouldCreateVerticalSpacingSuccessfully() {
            // Act
            Component spacing = ChatPanelFactory.createVerticalSpacing(10);

            // Assert
            assertNotNull(spacing);
            assertTrue(spacing instanceof Box.Filler);
        }

        @Test
        @DisplayName("should have correct spacing size")
        void shouldHaveCorrectSpacingSize() {
            // Act
            Component spacing = ChatPanelFactory.createVerticalSpacing(15);

            // Assert
            Dimension preferredSize = spacing.getPreferredSize();
            assertEquals(15, preferredSize.height);
        }

        @Test
        @DisplayName("should create default message spacing successfully")
        void shouldCreateDefaultMessageSpacingSuccessfully() {
            // Act
            Component spacing = ChatPanelFactory.createDefaultMessageSpacing();

            // Assert
            assertNotNull(spacing);
            assertTrue(spacing instanceof Box.Filler);
        }

        @Test
        @DisplayName("should create default component spacing successfully")
        void shouldCreateDefaultComponentSpacingSuccessfully() {
            // Act
            Component spacing = ChatPanelFactory.createDefaultComponentSpacing();

            // Assert
            assertNotNull(spacing);
            assertTrue(spacing instanceof Box.Filler);
        }
    }

    @Nested
    @DisplayName("Vertical Glue Creation")
    class VerticalGlueCreation {

        @Test
        @DisplayName("should create vertical glue successfully")
        void shouldCreateVerticalGlueSuccessfully() {
            // Act
            Component glue = ChatPanelFactory.createVerticalGlue();

            // Assert
            assertNotNull(glue);
            assertTrue(glue instanceof Box.Filler);
        }
    }

    @Nested
    @DisplayName("Component Alignment")
    class ComponentAlignment {

        @Test
        @DisplayName("should configure component alignment successfully")
        void shouldConfigureComponentAlignmentSuccessfully() {
            // Arrange
            JLabel testLabel = new JLabel("Test");

            // Act
            ChatPanelFactory.configureComponentAlignment(testLabel);

            // Assert
            assertEquals(Component.LEFT_ALIGNMENT, testLabel.getAlignmentX());
            assertEquals(Component.TOP_ALIGNMENT, testLabel.getAlignmentY());
        }

        @Test
        @DisplayName("should handle null component gracefully")
        void shouldHandleNullComponentGracefully() {
            // Act
            ChatPanelFactory.configureComponentAlignment(null);

            // Assert
            // Should not throw exception, should log warning and return early
            // The method handles null gracefully with defensive programming
        }
    }

    @Nested
    @DisplayName("Complete Chat Panel Setup")
    class CompleteChatPanelSetup {

        @Test
        @DisplayName("should create complete chat panel setup successfully")
        void shouldCreateCompleteChatPanelSetupSuccessfully() {
            // Act
            Object[] setup = ChatPanelFactory.createCompleteChatPanelSetup();

            // Assert
            assertNotNull(setup);
            assertEquals(3, setup.length);
            assertTrue(setup[0] instanceof JPanel); // mainPanel
            assertTrue(setup[1] instanceof JPanel); // messageContainer
            assertTrue(setup[2] instanceof JScrollPane); // scrollPane
        }

        @Test
        @DisplayName("should have proper component hierarchy")
        void shouldHaveProperComponentHierarchy() {
            // Act
            Object[] setup = ChatPanelFactory.createCompleteChatPanelSetup();

            // Assert
            JPanel mainPanel = (JPanel) setup[0];
            JPanel messageContainer = (JPanel) setup[1];
            JScrollPane scrollPane = (JScrollPane) setup[2];
            
            assertTrue(mainPanel.getComponentCount() > 0);
            assertEquals(scrollPane, mainPanel.getComponent(0));
            assertEquals(messageContainer, scrollPane.getViewport().getView());
        }
    }

    @Nested
    @DisplayName("Message Container Operations")
    class MessageContainerOperations {

        @Test
        @DisplayName("should add message to container successfully")
        void shouldAddMessageToContainerSuccessfully() {
            // Arrange
            JPanel messageContainer = ChatPanelFactory.createMessageContainer();
            JLabel testMessage = new JLabel("Test Message");

            // Act
            ChatPanelFactory.addMessageToContainer(messageContainer, testMessage, false);

            // Assert
            assertTrue(messageContainer.getComponentCount() > 1); // Should have glue + message
        }

        @Test
        @DisplayName("should handle null container gracefully")
        void shouldHandleNullContainerGracefully() {
            // Arrange
            JLabel testMessage = new JLabel("Test Message");

            // Act
            ChatPanelFactory.addMessageToContainer(null, testMessage, false);

            // Assert
            // Should not throw exception, should log warning and return early
            // The method handles null gracefully with defensive programming
        }

        @Test
        @DisplayName("should handle null message gracefully")
        void shouldHandleNullMessageGracefully() {
            // Arrange
            JPanel messageContainer = ChatPanelFactory.createMessageContainer();

            // Act
            ChatPanelFactory.addMessageToContainer(messageContainer, null, false);

            // Assert
            // Should not throw exception, should log warning and return early
            // The method handles null gracefully with defensive programming
        }

        @Test
        @DisplayName("should clear message container successfully")
        void shouldClearMessageContainerSuccessfully() {
            // Arrange
            JPanel messageContainer = ChatPanelFactory.createMessageContainer();
            JLabel testMessage = new JLabel("Test Message");
            messageContainer.add(testMessage);

            // Act
            ChatPanelFactory.clearMessageContainer(messageContainer);

            // Assert
            assertEquals(1, messageContainer.getComponentCount()); // Should only have glue
        }

        @Test
        @DisplayName("should handle null container in clear operation")
        void shouldHandleNullContainerInClearOperation() {
            // Act
            ChatPanelFactory.clearMessageContainer(null);

            // Assert
            // Should not throw exception, should log warning and return early
            // The method handles null gracefully with defensive programming
        }
    }

    @Nested
    @DisplayName("Scroll to Bottom")
    class ScrollToBottom {

        @Test
        @DisplayName("should scroll to bottom successfully")
        void shouldScrollToBottomSuccessfully() {
            // Arrange
            Object[] setup = ChatPanelFactory.createCompleteChatPanelSetup();
            JScrollPane scrollPane = (JScrollPane) setup[2];

            // Act & Assert
            assertDoesNotThrow(() -> ChatPanelFactory.scrollToBottom(scrollPane));
        }

        @Test
        @DisplayName("should handle null scroll pane gracefully")
        void shouldHandleNullScrollPaneGracefully() {
            // Act & Assert
            assertDoesNotThrow(() -> ChatPanelFactory.scrollToBottom(null));
        }
    }
} 