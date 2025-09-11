package com.trace.chat.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ide.ui.LafManagerListener;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.UIUtil;
import com.trace.common.constants.TriagePanelConstants;
import com.trace.common.utils.ThemeUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for theme-related functionality in the TriagePanelView.
 * Provides methods for theme refresh, component recreation, and theme change detection.
 * 
 * <p>This class encapsulates all theme-related operations to reduce the complexity
 * of the main TriagePanelView class and improve code organization.</p>
 * 
 * @author Alex Ibasitas
 * @version 1.0
 * @since 1.0
 */
public class ThemeHelper {
    
    private static final Logger LOG = Logger.getInstance(ThemeHelper.class);
    
    // Theme-related fields
    private MessageBusConnection messageBusConnection;
    
    // Component references needed for theme operations
    private JPanel mainPanel;
    private JScrollPane chatScrollPane;
    private JPanel messageContainer;
    private JTextArea inputArea;
    private JPanel inputPanel;
    private JPanel bottomSpacer;
    private JBLabel headerLabel;
    private JBLabel statusLabel;
    
    /**
     * Constructor for ThemeHelper.
     * 
     * @param mainPanel The main panel component
     * @param chatScrollPane The chat scroll pane component
     * @param messageContainer The message container component
     * @param inputArea The input text area component
     * @param inputPanel The input panel component
     * @param bottomSpacer The bottom spacer component
     * @param headerLabel The header label component
     * @param statusLabel The status label component
     */
    public ThemeHelper(JPanel mainPanel, JScrollPane chatScrollPane, JPanel messageContainer,
                      JTextArea inputArea, JPanel inputPanel, JPanel bottomSpacer,
                      JBLabel headerLabel, JBLabel statusLabel) {
        this.mainPanel = mainPanel;
        this.chatScrollPane = chatScrollPane;
        this.messageContainer = messageContainer;
        this.inputArea = inputArea;
        this.inputPanel = inputPanel;
        this.bottomSpacer = bottomSpacer;
        this.headerLabel = headerLabel;
        this.statusLabel = statusLabel;
    }
    
    /**
     * Refreshes all theme colors and styles for the chat interface.
     * Updates all components to match the current IDE theme.
     */
    public void refreshTheme() {
        try {
            LOG.info("Theme refresh started");
            if (LOG.isDebugEnabled()) {
                LOG.debug("Current theme: " + UIManager.getLookAndFeel().getName());
                LOG.debug("Theme colors - Panel background: " + ThemeUtils.panelBackground());
                LOG.debug("Theme colors - Text foreground: " + ThemeUtils.textForeground());
                LOG.debug("Theme colors - Text field background: " + ThemeUtils.textFieldBackground());
                LOG.debug("UIManager Panel.background: " + UIManager.getColor("Panel.background"));
                LOG.debug("UIManager Label.foreground: " + UIManager.getColor("Label.foreground"));
                LOG.debug("UIManager TextField.background: " + UIManager.getColor("TextField.background"));
            }
            
            // Update main panel background
            if (mainPanel != null) {
                Color bg = ThemeUtils.panelBackground();
                mainPanel.setBackground(bg);
                mainPanel.revalidate();
                mainPanel.repaint();
                LOG.debug("Updated main panel background to: " + bg);
            }
            
            // Update chat scroll pane and viewport
            if (chatScrollPane != null && chatScrollPane.getViewport() != null) {
                Color bg = ThemeUtils.panelBackground();
                chatScrollPane.setBackground(bg);
                chatScrollPane.getViewport().setBackground(bg);
                chatScrollPane.revalidate();
                chatScrollPane.repaint();
            }
            
            // Update message container and all message components
            if (messageContainer != null) {
                Color bg = ThemeUtils.panelBackground();
                messageContainer.setBackground(bg);
                
                // NUCLEAR OPTION: Recreate all message components to ensure proper theme switching
                // This is the most reliable way to handle JEditorPane HTML content that doesn't refresh properly
                recreateAllMessageComponents();
                
                messageContainer.revalidate();
                messageContainer.repaint();
            }
            
            // Update input area and input panel
            if (inputArea != null) {
                inputArea.setBackground(ThemeUtils.textFieldBackground());
                inputArea.setForeground(ThemeUtils.textForeground());
                inputArea.setCaretColor(ThemeUtils.textForeground());
                inputArea.revalidate();
                inputArea.repaint();
            }
            
            if (inputPanel != null) {
                // Keep input panel grey and render white background via the inner inputBoxContainer
                inputPanel.setBackground(ThemeUtils.panelBackground());
                inputPanel.setOpaque(true);

                for (Component child : inputPanel.getComponents()) {
                    if (child instanceof JPanel && "inputBoxContainer".equals(child.getName())) {
                        JPanel box = (JPanel) child;
                        box.setOpaque(true);
                        box.setBackground(TriagePanelConstants.getInputContainerBackground());
                        box.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(TriagePanelConstants.getInputContainerBorder(), 1, true),
                            BorderFactory.createEmptyBorder(8, 12, 8, 0)
                        ));

                        for (Component inner : box.getComponents()) {
                            if (inner instanceof JTextArea) {
                                JTextArea textArea = (JTextArea) inner;
                                // Do not paint its own background; rely on the box
                                textArea.setOpaque(false);
                                textArea.setForeground(ThemeUtils.textForeground());
                                textArea.setCaretColor(ThemeUtils.textForeground());
                                textArea.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
                            } else if (inner instanceof JPanel && "buttonPanel".equals(inner.getName())) {
                                JPanel buttonPanel = (JPanel) inner;
                                // Let the white box show through
                                buttonPanel.setOpaque(false);
                                buttonPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
                            }
                        }
                    }
                }
                inputPanel.revalidate();
                inputPanel.repaint();
            }
            
            // Update bottom spacer
            if (bottomSpacer != null) {
                bottomSpacer.setBackground(ThemeUtils.panelBackground());
                bottomSpacer.revalidate();
                bottomSpacer.repaint();
            }
            
            // Update header and status labels
            if (headerLabel != null) {
                headerLabel.setForeground(ThemeUtils.textForeground());
                headerLabel.revalidate();
                headerLabel.repaint();
            }
            
            if (statusLabel != null) {
                statusLabel.setForeground(ThemeUtils.textForeground());
                statusLabel.revalidate();
                statusLabel.repaint();
            }
            
            LOG.info("Theme refresh completed");
        } catch (Exception e) {
            LOG.error("Error during theme refresh: " + e.getMessage(), e);
        }
    }
    
    /**
     * Recreates all message components to ensure proper theme switching.
     * This is the most reliable way to handle JEditorPane HTML content that doesn't refresh properly.
     */
    public void recreateAllMessageComponents() {
        try {
            LOG.debug("Recreating all message components for theme refresh");
            
            // Store current components and their order
            List<Component> components = new ArrayList<>();
            for (Component child : messageContainer.getComponents()) {
                if (child instanceof com.trace.chat.components.MessageComponent) {
                    components.add(child);
                }
            }
            
            // Remove all message components
            for (Component child : components) {
                messageContainer.remove(child);
            }
            
            // Recreate all message components with current theme
            for (Component oldComponent : components) {
                if (oldComponent instanceof com.trace.chat.components.MessageComponent) {
                    com.trace.chat.components.MessageComponent oldMsg = (com.trace.chat.components.MessageComponent) oldComponent;
                    com.trace.chat.components.ChatMessage message = oldMsg.getMessage();
                    
                    // Create new message component with same message
                    com.trace.chat.components.MessageComponent newMsg = new com.trace.chat.components.MessageComponent(message);
                    newMsg.setAlignmentX(Component.LEFT_ALIGNMENT);
                    
                    // Add to container
                    messageContainer.add(newMsg);
                }
            }
            
            if (LOG.isDebugEnabled()) {
                LOG.debug("Recreated " + components.size() + " message components");
            }
        } catch (Exception e) {
            LOG.error("Error recreating message components: " + e.getMessage(), e);
        }
    }

    /**
     * Recursively refreshes theme colors for all components in a container.
     * 
     * @param container The container to refresh theme colors for
     */
    public void refreshThemeInContainer(Container container) {
        for (Component child : container.getComponents()) {
            // Update markdown content
            if (child instanceof javax.swing.JEditorPane) {
                com.trace.chat.components.MarkdownRenderer.reapplyThemeStyles((javax.swing.JEditorPane) child);
            }
            
            // Update JPanels and other containers
            if (child instanceof JPanel) {
                JPanel panel = (JPanel) child;
                // Only update panels that are opaque (have explicit backgrounds)
                if (panel.isOpaque()) {
                    panel.setBackground(ThemeUtils.panelBackground());
                    panel.revalidate();
                    panel.repaint();
                }
            }
            
            // Update JLabels
            if (child instanceof JLabel) {
                JLabel label = (JLabel) child;
                label.setForeground(ThemeUtils.textForeground());
                label.revalidate();
                label.repaint();
            }
            
            // Update JTextAreas
            if (child instanceof JTextArea) {
                JTextArea textArea = (JTextArea) child;
                // Use textFieldBackground for all text areas to ensure proper theme switching
                textArea.setBackground(ThemeUtils.textFieldBackground());
                textArea.setForeground(ThemeUtils.textForeground());
                textArea.setCaretColor(ThemeUtils.textForeground());
                textArea.revalidate();
                textArea.repaint();
            }
            
            // Update JButtons
            if (child instanceof JButton) {
                JButton button = (JButton) child;
                button.setForeground(ThemeUtils.textForeground());
                button.revalidate();
                button.repaint();
            }
            
            // Recursively update containers
            if (child instanceof Container) {
                refreshThemeInContainer((Container) child);
            }
        }
    }

    /**
     * Sets up theme change listener to automatically refresh all components when the IDE theme changes.
     * This ensures that existing chat messages and UI elements update their colors to match the new theme.
     * Uses a modern approach compatible with newer IntelliJ versions.
     */
    public void setupThemeChangeListener() {
        try {
            // Use modern MessageBus approach for theme change detection (IntelliJ 2025.2+)
            messageBusConnection = ApplicationManager.getApplication().getMessageBus().connect();
            messageBusConnection.subscribe(LafManagerListener.TOPIC, source -> {
                ApplicationManager.getApplication().invokeLater(() -> {
                        try {
                            LOG.debug("Theme changed to: " + UIManager.getLookAndFeel().getName());
                            LOG.info("Label foreground: " + UIManager.getColor("Label.foreground"));
                            LOG.info("Text field background: " + UIManager.getColor("TextField.background"));
                            
                            // Store current scroll position
                            int currentScrollValue = 0;
                            if (chatScrollPane != null && chatScrollPane.getVerticalScrollBar() != null) {
                                currentScrollValue = chatScrollPane.getVerticalScrollBar().getValue();
                            }
                            
                            // Refresh all theme colors
                            refreshTheme();
                            
                            // Restore scroll position to maintain user's view
                            if (chatScrollPane != null && chatScrollPane.getVerticalScrollBar() != null) {
                                chatScrollPane.getVerticalScrollBar().setValue(currentScrollValue);
                                LOG.info("Restored scroll position to: " + currentScrollValue);
                            }
                            
                            LOG.debug("Theme refresh completed");
                        } catch (Exception ex) {
                            LOG.error("Error during theme change refresh: " + ex.getMessage(), ex);
                        }
                    });
            });
            
            // Backup: Also add a property change listener for theme-related properties
            mainPanel.addPropertyChangeListener("UI", evt -> {
                ApplicationManager.getApplication().invokeLater(() -> {
                    try {
                        LOG.debug("Theme property changed: " + evt.getPropertyName());
                        refreshTheme();
                        LOG.debug("Theme refresh completed");
                    } catch (Exception ex) {
                        LOG.error("Error during theme change refresh: " + ex.getMessage(), ex);
                    }
                });
            });
            
            // Add font change listener to respond to IDE font size changes
            UIManager.addPropertyChangeListener(evt -> {
                if ("defaultFont".equals(evt.getPropertyName())) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        try {
                            LOG.debug("Font changed: " + evt.getPropertyName());
                            refreshTheme();
                            LOG.debug("Font refresh completed");
                        } catch (Exception ex) {
                            LOG.error("Error during font change refresh: " + ex.getMessage(), ex);
                        }
                    });
                }
            });
            
            LOG.info("Theme change listeners registered successfully");
        } catch (Exception e) {
            LOG.error("Failed to register theme change listeners: " + e.getMessage(), e);
        }
    }

    /**
     * Manually triggers a theme refresh for all components.
     * This can be called programmatically to test theme change behavior.
     */
    public void manualThemeRefresh() {
        LOG.info("Manual theme refresh triggered");
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                // Store current scroll position
                int currentScrollValue = 0;
                if (chatScrollPane != null && chatScrollPane.getVerticalScrollBar() != null) {
                    currentScrollValue = chatScrollPane.getVerticalScrollBar().getValue();
                }
                
                // Refresh all theme colors
                refreshTheme();
                
                // Restore scroll position to maintain user's view
                if (chatScrollPane != null && chatScrollPane.getVerticalScrollBar() != null) {
                    chatScrollPane.getVerticalScrollBar().setValue(currentScrollValue);
                    LOG.debug("Restored scroll position to: " + currentScrollValue);
                }
                
                LOG.info("Manual theme refresh completed");
            } catch (Exception e) {
                LOG.error("Error during manual theme refresh: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Disposes of resources and cleans up listeners to prevent memory leaks.
     * This method should be called when the component is no longer needed.
     */
    public void dispose() {
        try {
            LOG.info("Disposing ThemeHelper resources");
            
            // Disconnect MessageBus connection
            if (messageBusConnection != null) {
                try {
                    messageBusConnection.disconnect();
                    messageBusConnection = null;
                    LOG.info("Disconnected MessageBus connection");
                } catch (Exception e) {
                    LOG.error("Error disconnecting MessageBus connection: " + e.getMessage());
                }
            }
            
            LOG.info("ThemeHelper disposal completed");
        } catch (Exception e) {
            LOG.error("Error during ThemeHelper disposal: " + e.getMessage(), e);
        }
    }
}
