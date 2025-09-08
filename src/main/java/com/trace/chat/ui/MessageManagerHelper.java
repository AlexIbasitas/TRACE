package com.trace.chat.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.application.ApplicationManager;
import com.trace.chat.components.ChatMessage;
import com.trace.chat.components.MessageComponent;
import com.trace.chat.components.TypingIndicatorRow;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Helper class for message management in the TriagePanelView.
 * Provides methods for adding, displaying, and managing chat messages.
 * 
 * <p>This class encapsulates all message-related logic to reduce the complexity
 * of the main TriagePanelView class and improve code organization.</p>
 * 
 * @author Alex Ibasitas
 * @version 1.0
 * @since 1.0
 */
public class MessageManagerHelper {
    
    private static final Logger LOG = Logger.getInstance(MessageManagerHelper.class);
    
    /**
     * Adds a message to the chat history and UI.
     * Handles thread safety and typing indicator replacement logic.
     *
     * @param message The message to add
     * @param chatHistory The chat history list
     * @param messageContainer The message container component
     * @param typingIndicatorRow The typing indicator row component
     * @param typingIndicatorVisible Whether the typing indicator is visible
     * @param latestUserMessageComponent The latest user message component
     * @param scrollHelper The scroll helper instance
     * @param chatScrollPane The chat scroll pane
     * @param bottomSpacer The bottom spacer component
     */
    public static void addMessage(ChatMessage message, 
                                 List<ChatMessage> chatHistory,
                                 JPanel messageContainer,
                                 TypingIndicatorRow typingIndicatorRow,
                                 boolean typingIndicatorVisible,
                                 JComponent latestUserMessageComponent,
                                 ScrollHelper scrollHelper,
                                 JScrollPane chatScrollPane,
                                 JPanel bottomSpacer) {
        if (!SwingUtilities.isEventDispatchThread()) {
            ApplicationManager.getApplication().invokeLater(() -> addMessage(message, chatHistory, messageContainer, 
                typingIndicatorRow, typingIndicatorVisible, latestUserMessageComponent, scrollHelper, chatScrollPane, bottomSpacer));
            return;
        }
        
        // If assistant content arrives, remove typing indicator before appending
        if (message != null && message.isFromAI()) {
            // Capture whether we are currently near the latest user target BEFORE appending
            try {
                if (chatScrollPane != null && latestUserMessageComponent != null) {
                    int preTarget = scrollHelper.computeAlignTopTarget(chatScrollPane, latestUserMessageComponent);
                    boolean preNear = scrollHelper.isNearTarget(chatScrollPane, preTarget, scrollHelper.getNearTargetThresholdPx());
                    scrollHelper.setMaintainAlignAfterAppend(preNear);
                    // Capture anchor to preserve the user's row fixed position
                    scrollHelper.setAnchorActiveForAppend(true);
                    scrollHelper.setAnchorUserTopYBeforeAppend(latestUserMessageComponent.getY());
                    scrollHelper.setAnchorScrollValueBeforeAppend(chatScrollPane.getVerticalScrollBar().getValue());
                    LOG.debug("preAppend AI: preNear=" + preNear + " preTarget=" + preTarget +
                        " value=" + chatScrollPane.getVerticalScrollBar().getValue());
                } else {
                    scrollHelper.setMaintainAlignAfterAppend(false);
                    scrollHelper.setAnchorActiveForAppend(false);
                }
            } catch (Exception ignore) {
                scrollHelper.setMaintainAlignAfterAppend(false);
                scrollHelper.setAnchorActiveForAppend(false);
            }
            // If we have a visible typing indicator, replace it in-place with the AI message component
            if (typingIndicatorRow != null && typingIndicatorVisible) {
                try {
                    chatHistory.add(message);
                    MessageComponent aiComponent = new MessageComponent(message);
                    scrollHelper.replaceTypingIndicatorWithMessageComponent(aiComponent, typingIndicatorRow);
                    return; // UI updated in-place; skip full rebuild
                } catch (Exception ex) {
                    // Fallback to default path
                    LOG.debug("Failed to replace typing indicator in-place, falling back to rebuild: " + ex.getMessage());
                }
            }
            // No indicator to replace; proceed with normal flow
            // hideTypingIndicator(); // This would need to be called from TriagePanelView
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Adding message to chat history: " + message.getRole() + " - " + 
                     message.getText().substring(0, Math.min(message.getText().length(), 30)) + "...");
        }
        chatHistory.add(message);
        addMessageToUIFull(message, chatHistory, messageContainer, typingIndicatorRow, typingIndicatorVisible, 
            latestUserMessageComponent, scrollHelper, bottomSpacer);
    }
    
    /**
     * Adds a message to the UI by rebuilding the message container.
     * Handles proper spacing, component alignment, and typing indicator display.
     *
     * @param message The message to add to UI
     * @param chatHistory The chat history list
     * @param messageContainer The message container component
     * @param typingIndicatorRow The typing indicator row component
     * @param typingIndicatorVisible Whether the typing indicator is visible
     * @param latestUserMessageComponent The latest user message component
     * @param scrollHelper The scroll helper instance
     * @param bottomSpacer The bottom spacer component
     */
    public static void addMessageToUIFull(ChatMessage message, 
                                         List<ChatMessage> chatHistory,
                                         JPanel messageContainer,
                                         TypingIndicatorRow typingIndicatorRow,
                                         boolean typingIndicatorVisible,
                                         JComponent latestUserMessageComponent,
                                         ScrollHelper scrollHelper,
                                         JPanel bottomSpacer) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Starting message UI addition - message role: " + message.getRole() + 
                     ", text length: " + (message.getText() != null ? message.getText().length() : 0) + 
                     ", chatHistory size: " + chatHistory.size() + 
                     ", messageContainer component count: " + messageContainer.getComponentCount() + 
                     ", messageContainer size: " + messageContainer.getSize() + 
                     ", messageContainer preferred size: " + messageContainer.getPreferredSize());
        }
        
        // Remove the vertical glue temporarily
        messageContainer.removeAll();
        
        if (LOG.isDebugEnabled()) {
            LOG.debug("Container cleared - component count after clear: " + messageContainer.getComponentCount());
        }
        
        // Add all existing messages with proper spacing
        for (int i = 0; i < chatHistory.size(); i++) {
            ChatMessage existingMessage = chatHistory.get(i);
            
            MessageComponent existingComponent = new MessageComponent(existingMessage);
            existingComponent.setAlignmentY(Component.TOP_ALIGNMENT);
            
            messageContainer.add(existingComponent);
            
            // Add spacing between messages, but not after the last one
            if (i < chatHistory.size() - 1) {
                messageContainer.add(Box.createVerticalStrut(16));
            }
        }

        // Track newest user message component
        latestUserMessageComponent = null;
        for (int i = messageContainer.getComponentCount() - 1; i >= 0; i--) {
            Component c = messageContainer.getComponent(i);
            if (c instanceof MessageComponent) {
                ChatMessage cm = ((MessageComponent) c).getMessage();
                if (cm != null && cm.isFromUser()) {
                    latestUserMessageComponent = (JComponent) c;
                    scrollHelper.setLatestUserMessageComponent(latestUserMessageComponent);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Latest user message set - index: " + i + ", y: " + latestUserMessageComponent.getY() +
                            ", prefH: " + latestUserMessageComponent.getPreferredSize().height +
                            ", count: " + messageContainer.getComponentCount());
                    }
                    break;
                }
            }
        }

        // Optionally append typing indicator row just after the last message
        if (typingIndicatorVisible) {
            if (typingIndicatorRow == null) {
                typingIndicatorRow = new TypingIndicatorRow();
            }
            // Add spacing before the indicator when there are messages
            if (!chatHistory.isEmpty()) {
                messageContainer.add(Box.createVerticalStrut(16));
            }
            messageContainer.add(typingIndicatorRow);
        }

        // Add bottom spacer to ensure proper scrolling behavior
        if (bottomSpacer == null) {
            bottomSpacer = new JPanel();
            bottomSpacer.setOpaque(false);
        }
        bottomSpacer.setPreferredSize(new Dimension(1, 0));
        messageContainer.add(bottomSpacer);

        // Revalidate and repaint the container
        messageContainer.revalidate();
        messageContainer.repaint();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Message UI addition complete - final component count: " + messageContainer.getComponentCount());
        }
    }
    
    /**
     * Replaces the typing indicator row with the provided AI message component in-place
     * to avoid visual jumping. Keeps spacer as last child, and avoids any animated scroll.
     *
     * @param aiComponent The AI message component to replace the typing indicator with
     * @param messageContainer The message container component
     * @param typingIndicatorRow The typing indicator row component
     * @param bottomSpacer The bottom spacer component
     */
    public static void replaceTypingIndicatorWithMessageComponent(MessageComponent aiComponent,
                                                                 JPanel messageContainer,
                                                                 TypingIndicatorRow typingIndicatorRow,
                                                                 JPanel bottomSpacer) {
        if (messageContainer == null || aiComponent == null) return;

        int count = messageContainer.getComponentCount();
        int idx = -1;
        for (int i = 0; i < count; i++) {
            if (messageContainer.getComponent(i) == typingIndicatorRow) {
                idx = i;
                break;
            }
        }
        if (idx >= 0) {
            // Stop animation and clear indicator state but do not trigger rebuild
            try { if (typingIndicatorRow != null) typingIndicatorRow.stopAnimation(); } catch (Exception ignore) {}
            // typingIndicatorVisible = false; // This would need to be set in TriagePanelView

            // Replace indicator with AI component at the same index
            messageContainer.remove(idx);
            aiComponent.setAlignmentY(Component.TOP_ALIGNMENT);
            messageContainer.add(aiComponent, idx);

            // Ensure bottom spacer is the last child
            if (bottomSpacer != null) {
                // Remove and re-add to guarantee it is last
                messageContainer.remove(bottomSpacer);
                messageContainer.add(bottomSpacer);
            }

            // Refresh layout and recompute spacer to keep maxScroll tight
            messageContainer.revalidate();
            messageContainer.repaint();
        }
    }
    
    /**
     * Clears the chat history and UI components.
     * Removes all messages and resets the message container.
     *
     * @param chatHistory The chat history list to clear
     * @param messageContainer The message container component
     * @param bottomSpacer The bottom spacer component
     * @param latestUserMessageComponent The latest user message component reference
     */
    public static void clearChat(List<ChatMessage> chatHistory,
                                JPanel messageContainer,
                                JPanel bottomSpacer,
                                JComponent latestUserMessageComponent) {
        LOG.debug("Clearing chat history and UI");
        chatHistory.clear();
        messageContainer.removeAll();
        if (bottomSpacer == null) {
            bottomSpacer = new JPanel();
            bottomSpacer.setOpaque(false);
        }
        bottomSpacer.setPreferredSize(new Dimension(1, 0));
        messageContainer.add(bottomSpacer);
        // latestUserMessageComponent = null; // This would need to be set in TriagePanelView
        messageContainer.revalidate();
        messageContainer.repaint();
    }
    
    /**
     * Simple method to add a message to UI that matches the original signature.
     * This is a wrapper around the full addMessageToUI method for backward compatibility.
     *
     * @param message The message to add to UI (can be null)
     * @param chatHistory The chat history list
     * @param messageContainer The message container component
     * @param typingIndicatorRow The typing indicator row component
     * @param typingIndicatorVisible Whether the typing indicator is visible
     * @param latestUserMessageComponent The latest user message component
     * @param scrollHelper The scroll helper instance
     * @param bottomSpacer The bottom spacer component
     */
    public static void addMessageToUI(ChatMessage message,
                                     List<ChatMessage> chatHistory,
                                     JPanel messageContainer,
                                     TypingIndicatorRow typingIndicatorRow,
                                     boolean typingIndicatorVisible,
                                     JComponent latestUserMessageComponent,
                                     ScrollHelper scrollHelper,
                                     JPanel bottomSpacer) {
        if (message != null) {
            addMessageToUIFull(message, chatHistory, messageContainer, typingIndicatorRow, 
                typingIndicatorVisible, latestUserMessageComponent, scrollHelper, bottomSpacer);
        } else {
            // Rebuild UI with existing messages
            messageContainer.removeAll();
            for (ChatMessage existingMessage : chatHistory) {
                MessageComponent existingComponent = new MessageComponent(existingMessage);
                existingComponent.setAlignmentY(Component.TOP_ALIGNMENT);
                messageContainer.add(existingComponent);
                if (chatHistory.indexOf(existingMessage) < chatHistory.size() - 1) {
                    messageContainer.add(Box.createVerticalStrut(16));
                }
            }
            
            // Add typing indicator if visible
            if (typingIndicatorVisible && typingIndicatorRow != null) {
                if (!chatHistory.isEmpty()) {
                    messageContainer.add(Box.createVerticalStrut(16));
                }
                messageContainer.add(typingIndicatorRow);
            }
            
            // Add bottom spacer
            if (bottomSpacer != null) {
                messageContainer.add(bottomSpacer);
            }
            
            messageContainer.revalidate();
            messageContainer.repaint();
        }
    }
    
    /**
     * Shows the typing indicator in the message container.
     * Handles thread safety and proper positioning before the bottom spacer.
     *
     * @param messageContainer The message container component
     * @param typingIndicatorRow The typing indicator row component
     * @param typingIndicatorVisible Whether the typing indicator is visible
     * @param bottomSpacer The bottom spacer component
     * @param chatHistory The chat history list
     * @param latestUserMessageComponent The latest user message component
     * @param scrollHelper The scroll helper instance
     * @param chatScrollPane The chat scroll pane
     */
    public static void showTypingIndicator(JPanel messageContainer,
                                          TypingIndicatorRow typingIndicatorRow,
                                          boolean typingIndicatorVisible,
                                          JPanel bottomSpacer,
                                          List<ChatMessage> chatHistory,
                                          JComponent latestUserMessageComponent,
                                          ScrollHelper scrollHelper,
                                          JScrollPane chatScrollPane) {
        if (!SwingUtilities.isEventDispatchThread()) {
            ApplicationManager.getApplication().invokeLater(() -> showTypingIndicator(messageContainer, 
                typingIndicatorRow, typingIndicatorVisible, bottomSpacer, chatHistory, 
                latestUserMessageComponent, scrollHelper, chatScrollPane));
            return;
        }
        try {
            if (!typingIndicatorVisible) {
                // typingIndicatorVisible = true; // This would need to be set in TriagePanelView
                if (typingIndicatorRow == null) {
                    typingIndicatorRow = new TypingIndicatorRow();
                }
                // Insert before bottom spacer if currently built
                int count = messageContainer.getComponentCount();
                if (count > 0 && bottomSpacer != null) {
                    int idx = -1;
                    for (int i = 0; i < count; i++) {
                        if (messageContainer.getComponent(i) == bottomSpacer) {
                            idx = i;
                            break;
                        }
                    }
                    if (idx >= 0) {
                        // Add spacing before indicator when preceding component is a message
                        if (idx > 0) {
                            Component prev = messageContainer.getComponent(idx - 1);
                            if (!(prev instanceof Box.Filler)) {
                                messageContainer.add(Box.createVerticalStrut(16), idx++);
                            }
                        }
                        messageContainer.add(typingIndicatorRow, idx);
                    } else {
                        // Fallback: rebuild to include indicator
                        MessageManagerHelper.addMessageToUI(chatHistory.isEmpty() ? null : chatHistory.get(chatHistory.size() - 1),
                            chatHistory, messageContainer, typingIndicatorRow, true, 
                            latestUserMessageComponent, scrollHelper, bottomSpacer);
                    }
                } else {
                    // Fallback: rebuild to include indicator
                    MessageManagerHelper.addMessageToUI(chatHistory.isEmpty() ? null : chatHistory.get(chatHistory.size() - 1),
                        chatHistory, messageContainer, typingIndicatorRow, true, 
                        latestUserMessageComponent, scrollHelper, bottomSpacer);
                }
                messageContainer.revalidate();
                messageContainer.repaint();
                ApplicationManager.getApplication().invokeLater(() -> scrollHelper.requestAlignNewestIfNear(chatScrollPane));
            }
        } catch (Exception ignore) {
        }
    }
    
    /**
     * Hides the typing indicator from the message container.
     * Handles thread safety and proper cleanup of the indicator.
     *
     * @param messageContainer The message container component
     * @param typingIndicatorRow The typing indicator row component
     * @param typingIndicatorVisible Whether the typing indicator is visible
     */
    public static void hideTypingIndicator(JPanel messageContainer,
                                          TypingIndicatorRow typingIndicatorRow,
                                          boolean typingIndicatorVisible) {
        if (!SwingUtilities.isEventDispatchThread()) {
            ApplicationManager.getApplication().invokeLater(() -> hideTypingIndicator(messageContainer, 
                typingIndicatorRow, typingIndicatorVisible));
            return;
        }
        try {
            if (typingIndicatorVisible) {
                // typingIndicatorVisible = false; // This would need to be set in TriagePanelView
                if (typingIndicatorRow != null) {
                    try {
                        typingIndicatorRow.stopAnimation();
                    } catch (Exception ignore) {}
                    Container parent = typingIndicatorRow.getParent();
                    if (parent != null) {
                        parent.remove(typingIndicatorRow);
                    }
                }
                // typingIndicatorRow = null; // This would need to be set in TriagePanelView
                messageContainer.revalidate();
                messageContainer.repaint();
            }
        } catch (Exception ignore) {
        }
    }
}
