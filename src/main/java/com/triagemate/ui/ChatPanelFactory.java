package com.triagemate.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.*;
import java.awt.*;

/**
 * Factory class for creating chat panel components with proper styling and layout.
 * 
 * <p>This factory provides methods for creating all the components needed for
 * a functional chat interface, including panels, scroll panes, and spacing
 * components. All components are configured with consistent styling and
 * proper layout management.</p>
 * 
 * <p>This factory implements defensive programming patterns to handle edge cases
 * gracefully and provide robust component creation even with invalid inputs.</p>
 * 
 * @author Alex Ibasitas
 * @version 1.0
 * @since 1.0
 */
public final class ChatPanelFactory {
    
    private static final Logger LOG = Logger.getInstance(ChatPanelFactory.class);
    
    // Private constructor to prevent instantiation
    private ChatPanelFactory() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    
    /**
     * Creates and configures the main chat panel.
     * 
     * <p>The chat panel serves as the main container for the chat interface,
     * providing the foundation for message display and scrolling.</p>
     *
     * @return The configured chat panel
     */
    public static JPanel createChatPanel() {
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setBackground(TriagePanelConstants.getPanelBackground());
        chatPanel.setBorder(TriagePanelConstants.MESSAGE_CONTAINER_BORDER);
        chatPanel.setOpaque(true);
        chatPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        chatPanel.setAlignmentY(Component.TOP_ALIGNMENT);
        
        return chatPanel;
    }
    
    /**
     * Creates and configures the message container panel.
     * 
     * <p>The message container holds all chat messages and provides proper
     * layout management for message positioning and spacing.</p>
     *
     * @return The configured message container
     */
    public static JPanel createMessageContainer() {
        JPanel messageContainer = new JPanel();
        messageContainer.setLayout(new BoxLayout(messageContainer, BoxLayout.Y_AXIS));
        messageContainer.setBackground(TriagePanelConstants.getPanelBackground());
        messageContainer.setOpaque(false);
        messageContainer.setBorder(TriagePanelConstants.MESSAGE_CONTAINER_BORDER);
        messageContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        messageContainer.setAlignmentY(Component.TOP_ALIGNMENT);
        
        // Add initial vertical glue to push messages to top
        messageContainer.add(Box.createVerticalGlue());
        
        return messageContainer;
    }
    
    /**
     * Creates and configures a scroll pane for the chat panel.
     * 
     * <p>The scroll pane is configured with proper scrolling behavior, including
     * unit increment settings for smooth scrolling. It uses the provided viewport
     * view and applies appropriate styling.</p>
     * 
     * <p>This method implements defensive programming - if viewportView is null,
     * it creates a scroll pane with an empty panel as the viewport.</p>
     *
     * @param viewportView The component to display in the scroll pane (can be null)
     * @return The configured scroll pane
     */
    public static JScrollPane createScrollPane(Component viewportView) {
        // Defensive programming: handle null viewport gracefully
        if (viewportView == null) {
            LOG.warn("ChatPanelFactory: createScrollPane called with null viewportView - using empty panel");
            viewportView = new JPanel(); // Create empty panel as fallback
        }
        
        JScrollPane scrollPane = new JScrollPane(viewportView);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(TriagePanelConstants.EMPTY_BORDER);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getViewport().setBackground(TriagePanelConstants.getPanelBackground());
        
        // Configure scroll bar for smooth scrolling
        JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
        verticalBar.setUnitIncrement(TriagePanelConstants.SCROLL_BAR_UNIT_INCREMENT);
        verticalBar.setBlockIncrement(TriagePanelConstants.SCROLL_BAR_UNIT_INCREMENT * 3);
        
        // Set minimum width to enable soft wrapping before horizontal scrollbar appears
        scrollPane.setMinimumSize(new Dimension(TriagePanelConstants.MIN_CHAT_WIDTH_BEFORE_SCROLL, 200));
        
        return scrollPane;
    }
    
    /**
     * Creates a vertical spacing component between messages.
     * 
     * <p>This method creates a rigid area with the specified height to provide
     * consistent spacing between chat messages.</p>
     *
     * @param height The height of the spacing in pixels
     * @return A rigid area component for spacing
     */
    public static Component createVerticalSpacing(int height) {
        return Box.createVerticalStrut(height);
    }
    
    /**
     * Creates a vertical spacing component using the default message spacing.
     * 
     * @return A rigid area component with default message spacing
     */
    public static Component createDefaultMessageSpacing() {
        return createVerticalSpacing(TriagePanelConstants.MESSAGE_SPACING);
    }
    
    /**
     * Creates a vertical spacing component using the default component spacing.
     * 
     * @return A rigid area component with default component spacing
     */
    public static Component createDefaultComponentSpacing() {
        return createVerticalSpacing(TriagePanelConstants.COMPONENT_SPACING);
    }
    
    /**
     * Creates a vertical glue component to push content to the top.
     * 
     * <p>Vertical glue is used to push messages to the top of the container
     * and maintain proper layout behavior.</p>
     *
     * @return A vertical glue component
     */
    public static Component createVerticalGlue() {
        return Box.createVerticalGlue();
    }
    
    /**
     * Configures a component for proper alignment in the chat layout.
     * 
     * <p>This method sets the alignment properties for components to ensure
     * they are properly positioned within the BoxLayout container.</p>
     * 
     * <p>This method implements defensive programming - if component is null,
     * it logs a warning and returns early without throwing an exception.</p>
     *
     * @param component The component to configure (can be null)
     */
    public static void configureComponentAlignment(Component component) {
        // Defensive programming: handle null component gracefully
        if (component == null) {
            LOG.warn("ChatPanelFactory: configureComponentAlignment called with null component - skipping alignment");
            return;
        }
        
        if (component instanceof JComponent) {
            JComponent jComponent = (JComponent) component;
            jComponent.setAlignmentX(Component.LEFT_ALIGNMENT);
            jComponent.setAlignmentY(Component.TOP_ALIGNMENT);
        }
    }
    
    /**
     * Creates a complete chat panel setup with all necessary components.
     * 
     * <p>This method creates and configures all the components needed for
     * a functional chat panel, including the main panel, message container,
     * and scroll pane. It returns an array containing the main panel and
     * message container for external use.</p>
     *
     * @return An array containing [mainPanel, messageContainer, scrollPane]
     */
    public static Object[] createCompleteChatPanelSetup() {
        JPanel mainPanel = createChatPanel();
        JPanel messageContainer = createMessageContainer();
        JScrollPane scrollPane = createScrollPane(messageContainer);
        
        // Add scroll pane to main panel
        mainPanel.add(scrollPane);
        
        return new Object[]{mainPanel, messageContainer, scrollPane};
    }
    
    /**
     * Adds a message component to the message container with proper spacing.
     * 
     * <p>This method adds a message component to the container and ensures
     * proper spacing between messages. It handles the layout updates
     * automatically.</p>
     * 
     * <p>This method implements defensive programming - if any parameter is null,
     * it logs a warning and returns early without throwing an exception.</p>
     *
     * @param messageContainer The message container to add to (can be null)
     * @param messageComponent The message component to add (can be null)
     * @param isLastMessage Whether this is the last message (no spacing after)
     */
    public static void addMessageToContainer(JPanel messageContainer, Component messageComponent, boolean isLastMessage) {
        // Defensive programming: handle null parameters gracefully
        if (messageContainer == null) {
            LOG.warn("ChatPanelFactory: addMessageToContainer called with null messageContainer - skipping add operation");
            return;
        }
        if (messageComponent == null) {
            LOG.warn("ChatPanelFactory: addMessageToContainer called with null messageComponent - skipping add operation");
            return;
        }
        
        // Configure component alignment
        configureComponentAlignment(messageComponent);
        
        // Add the message component
        messageContainer.add(messageComponent, messageContainer.getComponentCount() - 1); // Add before vertical glue
        
        // Add spacing if not the last message
        if (!isLastMessage) {
            Component spacing = createDefaultMessageSpacing();
            configureComponentAlignment(spacing);
            messageContainer.add(spacing, messageContainer.getComponentCount() - 1);
        }
        
        // Trigger layout update
        messageContainer.revalidate();
        messageContainer.repaint();
    }
    
    /**
     * Clears all messages from the message container.
     * 
     * <p>This method removes all message components from the container while
     * preserving the vertical glue at the bottom.</p>
     * 
     * <p>This method implements defensive programming - if messageContainer is null,
     * it logs a warning and returns early without throwing an exception.</p>
     *
     * @param messageContainer The message container to clear (can be null)
     */
    public static void clearMessageContainer(JPanel messageContainer) {
        // Defensive programming: handle null container gracefully
        if (messageContainer == null) {
            LOG.warn("ChatPanelFactory: clearMessageContainer called with null messageContainer - skipping clear operation");
            return;
        }
        
        messageContainer.removeAll();
        messageContainer.add(createVerticalGlue());
        messageContainer.revalidate();
        messageContainer.repaint();
    }
    
    /**
     * Scrolls the scroll pane to the bottom to show the latest messages.
     * 
     * <p>This method scrolls the vertical scroll bar to its maximum value
     * to ensure the latest messages are visible.</p>
     * 
     * <p>This method implements defensive programming - if scrollPane is null,
     * it logs a warning and returns early without throwing an exception.</p>
     *
     * @param scrollPane The scroll pane to scroll (can be null)
     */
    public static void scrollToBottom(JScrollPane scrollPane) {
        // Defensive programming: handle null scroll pane gracefully
        if (scrollPane == null) {
            LOG.warn("ChatPanelFactory: scrollToBottom called with null scrollPane - skipping scroll operation");
            return;
        }
        
        SwingUtilities.invokeLater(() -> {
            try {
                JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
                if (verticalBar != null) {
                    verticalBar.setValue(verticalBar.getMaximum());
                }
            } catch (Exception e) {
                LOG.warn("ChatPanelFactory: Error during scrollToBottom operation", e);
            }
        });
    }
} 