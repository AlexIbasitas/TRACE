package com.trace.chat.components;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.trace.common.constants.TriagePanelConstants;
import com.trace.common.utils.ThemeUtils;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom collapsible panel for AI thinking content in the TRACE interface.
 * 
 * <p>This Swing component provides expandable/collapsible functionality for displaying
 * detailed AI analysis and thinking content. It includes a toggle button with visual
 * indicators and smooth expand/collapse behavior with proper layout management.</p>
 * 
 * <p>The component implements comprehensive layout management with proper EDT safety,
 * parent container notification, and layout listener support for seamless integration
 * with IntelliJ's UI framework.</p>
 * 
 * @author Alex Ibasitas
 * @version 1.0
 * @since 1.0
 */
public class CollapsiblePanel extends JPanel {
    
    private static final Logger LOG = Logger.getInstance(CollapsiblePanel.class);
    
    private JPanel contentPanel;
    private JLabel toggleLabel;
    private boolean isExpanded = false;
    private final MessageComponent parentMessageComponent;
    private final String contentText;
    private JTextArea contentTextArea;
    
    // Layout management support
    private final List<LayoutChangeListener> layoutListeners = new ArrayList<>();
    private boolean isDisposed = false;
    
    /**
     * Interface for components that need to respond to layout changes in the collapsible panel.
     */
    public interface LayoutChangeListener {
        /**
         * Called when the collapsible panel's layout state changes.
         * 
         * @param panel The collapsible panel that changed
         * @param isExpanded Whether the panel is now expanded
         */
        void onLayoutChanged(CollapsiblePanel panel, boolean isExpanded);
    }
    
    /**
     * Creates a new collapsible panel for displaying AI thinking content.
     *
     * @param title The title for the collapsible section (currently not used, kept for future extensibility)
     * @param content The content to display when expanded (can be null or empty)
     * @param parent The parent message component for layout coordination
     * @throws IllegalArgumentException if parent is null
     */
    public CollapsiblePanel(String title, String content, MessageComponent parent) {
        if (parent == null) {
            throw new IllegalArgumentException("Parent message component cannot be null");
        }
        
        this.parentMessageComponent = parent;
        this.contentText = content;
        
        initializeComponent();
        createToggleLabel();
        createContentPanel(content);
        setupEventHandlers();
        setupInitialState();
        setupLayoutManagement();
    }
    
    /**
     * Initializes the component with proper layout and styling.
     */
    private void initializeComponent() {
        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(ThemeUtils.panelBackground());
        setBorder(TriagePanelConstants.COLLAPSIBLE_PANEL_BORDER);
        setMaximumSize(TriagePanelConstants.MAX_EXPANDABLE_SIZE);
        setAlignmentX(Component.LEFT_ALIGNMENT);
    }
    
    /**
     * Creates the toggle label with proper styling and initial state.
     */
    private void createToggleLabel() {
        toggleLabel = new JLabel(TriagePanelConstants.EXPAND_ICON + TriagePanelConstants.TOGGLE_TEXT);
        toggleLabel.setFont(TriagePanelConstants.COLLAPSIBLE_TOGGLE_FONT);
        toggleLabel.setForeground(ThemeUtils.textForeground());
        toggleLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toggleLabel.setToolTipText(TriagePanelConstants.TOOLTIP_EXPAND);
    }
    
    /**
     * Creates the content panel with the provided content.
     *
     * @param content The content to display in the panel
     */
    private void createContentPanel(String content) {
        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setOpaque(true);
        contentPanel.setBackground(ThemeUtils.panelBackground());
        contentPanel.setBorder(TriagePanelConstants.COLLAPSIBLE_CONTENT_BORDER);
        contentPanel.setMaximumSize(TriagePanelConstants.MAX_EXPANDABLE_SIZE);
        
        if (content != null && !content.trim().isEmpty()) {
            addContentTextArea(content);
        }
    }
    
    /**
     * Adds a text area with the provided content to the content panel.
     *
     * @param content The text content to display
     */
    private void addContentTextArea(String content) {
        contentTextArea = new JTextArea(content);
        contentTextArea.setLineWrap(true);
        contentTextArea.setWrapStyleWord(true);
        contentTextArea.setEditable(false);
        contentTextArea.setFont(TriagePanelConstants.COLLAPSIBLE_CONTENT_FONT);
        contentTextArea.setForeground(ThemeUtils.textForeground());
        contentTextArea.setBackground(ThemeUtils.panelBackground());
        contentTextArea.setBorder(createThemeAwareBorder());
        contentTextArea.setOpaque(true);
        
        // Let the text area calculate its own size based on content
        // This allows for dynamic sizing proportional to text amount
        contentTextArea.setPreferredSize(null); // Let Swing calculate preferred size
        contentTextArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        contentTextArea.setMinimumSize(new Dimension(TriagePanelConstants.MIN_CHAT_WIDTH_BEFORE_SCROLL, 50));
        
        contentPanel.add(contentTextArea, BorderLayout.CENTER);
    }
    
    /**
     * Creates a theme-aware border for the content text area.
     * 
     * @return A compound border with theme-aware colors
     */
    private Border createThemeAwareBorder() {
        Color borderColor = ThemeUtils.uiColor("Component.borderColor", new Color(80, 80, 80));
        return BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor, 1),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        );
    }
    
    /**
     * Sets up event handlers for user interactions.
     */
    private void setupEventHandlers() {
        toggleLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                toggleExpanded();
            }
        });
    }
    
    /**
     * Sets up the initial collapsed state of the panel.
     */
    private void setupInitialState() {
        // Initially collapsed
        contentPanel.setVisible(false);

        // Build header with toggle on the left
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(true);
        header.setBackground(ThemeUtils.panelBackground());
        header.add(toggleLabel, BorderLayout.WEST);

        add(header, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);
    }
    
    /**
     * Sets up layout management and component lifecycle monitoring.
     */
    private void setupLayoutManagement() {
        // Monitor component lifecycle for proper cleanup
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                // Component is now visible - ensure proper layout
                if (isExpanded) {
                    performLayoutUpdate();
                }
            }
            
            @Override
            public void componentHidden(ComponentEvent e) {
                // Component is hidden - no need for layout updates
            }
        });
    }
    
    /**
     * Toggles the expanded state of the collapsible panel with comprehensive layout management.
     * Shows or hides the content and updates the toggle label with proper EDT safety
     * and parent container notification.
     */
    private void toggleExpanded() {
        // Check if component is disposed
        if (isDisposed) {
            LOG.warn("CollapsiblePanel: toggleExpanded called on disposed component - ignoring");
            return;
        }
        
        isExpanded = !isExpanded;
        contentPanel.setVisible(isExpanded);
        
        // Update toggle label text and tooltip
        String icon = isExpanded ? TriagePanelConstants.COLLAPSE_ICON : TriagePanelConstants.EXPAND_ICON;
        String tooltip = isExpanded ? TriagePanelConstants.TOOLTIP_COLLAPSE : TriagePanelConstants.TOOLTIP_EXPAND;
        
        toggleLabel.setText(icon + TriagePanelConstants.TOGGLE_TEXT);
        toggleLabel.setToolTipText(tooltip);
        
        // Perform comprehensive layout update
        performLayoutUpdate();
        
        // Notify layout listeners
        notifyLayoutListeners();
        
        // Notify parent containers
        notifyParentContainers();
        
        LOG.debug("CollapsiblePanel: Toggled to " + 
                 (isExpanded ? "expanded" : "collapsed") + " state");
    }
    
    /**
     * Performs a comprehensive layout update with proper EDT safety and error handling.
     * This method ensures all parent containers are properly notified of layout changes.
     */
    private void performLayoutUpdate() {
        try {
            // Ensure we're on the EDT
            if (!SwingUtilities.isEventDispatchThread()) {
                ApplicationManager.getApplication().invokeLater(this::performLayoutUpdate);
                return;
            }
            
            // Update our own layout
            revalidate();
            repaint();
            
            LOG.debug("CollapsiblePanel: Layout update completed for " + 
                     (isExpanded ? "expanded" : "collapsed") + " state");
            
        } catch (Exception e) {
            LOG.warn("CollapsiblePanel: Error during layout update", e);
        }
    }
    
    /**
     * Notifies parent containers to update their layouts.
     * Implements proper error handling for disposed components and null checks.
     */
    private void notifyParentContainers() {
        // Notify immediate parent (MessageComponent)
        if (parentMessageComponent != null && !isComponentDisposed(parentMessageComponent)) {
            ApplicationManager.getApplication().invokeLater(() -> {
                try {
                    parentMessageComponent.revalidate();
                    parentMessageComponent.repaint();
                } catch (Exception e) {
                    LOG.warn("CollapsiblePanel: Error updating parent MessageComponent", e);
                }
            });
        }
        
        // Notify grandparent containers (MessageContainer, ScrollPane, etc.)
        Container parent = getParent();
        while (parent != null && !isComponentDisposed(parent)) {
            final Container currentParent = parent;
            ApplicationManager.getApplication().invokeLater(() -> {
                try {
                    currentParent.revalidate();
                    currentParent.repaint();
                } catch (Exception e) {
                    LOG.warn("CollapsiblePanel: Error updating parent container", e);
                }
            });
            parent = parent.getParent();
        }
    }
    
    /**
     * Checks if a component is disposed or invalid.
     * 
     * @param component The component to check
     * @return true if the component is disposed or invalid, false otherwise
     */
    private boolean isComponentDisposed(Component component) {
        try {
            return component == null || !component.isDisplayable() || !component.isVisible();
        } catch (Exception e) {
            return true;
        }
    }
    
    /**
     * Notifies all registered layout change listeners.
     */
    private void notifyLayoutListeners() {
        for (LayoutChangeListener listener : new ArrayList<>(layoutListeners)) {
            try {
                if (listener != null) {
                    listener.onLayoutChanged(this, isExpanded);
                }
            } catch (Exception e) {
                LOG.warn("CollapsiblePanel: Error notifying layout listener", e);
            }
        }
    }
    
    /**
     * Adds a layout change listener to this collapsible panel.
     * 
     * @param listener The listener to add
     */
    public void addLayoutChangeListener(LayoutChangeListener listener) {
        if (listener != null && !layoutListeners.contains(listener)) {
            layoutListeners.add(listener);
        }
    }
    
    /**
     * Removes a layout change listener from this collapsible panel.
     * 
     * @param listener The listener to remove
     */
    public void removeLayoutChangeListener(LayoutChangeListener listener) {
        layoutListeners.remove(listener);
    }
    
    /**
     * Disposes of the component and cleans up resources.
     * This method should be called when the component is no longer needed.
     */
    public void dispose() {
        if (isDisposed) {
            return;
        }
        
        isDisposed = true;
        
        // Clear listeners
        layoutListeners.clear();
        
        // Remove mouse listeners
        if (toggleLabel != null) {
            for (java.awt.event.MouseListener listener : toggleLabel.getMouseListeners()) {
                toggleLabel.removeMouseListener(listener);
            }
        }
        
        // Remove component listeners
        for (ComponentListener listener : getComponentListeners()) {
            removeComponentListener(listener);
        }
        
        LOG.debug("CollapsiblePanel: Component disposed");
    }
    
    /**
     * Gets the current expanded state of the panel.
     *
     * @return true if the panel is expanded, false otherwise
     */
    public boolean isExpanded() {
        return isExpanded;
    }
    
    /**
     * Gets the content panel for external access.
     *
     * @return The content panel
     */
    public JPanel getContentPanel() {
        return contentPanel;
    }
    
    /**
     * Gets the toggle label for external access.
     *
     * @return The toggle label
     */
    public JLabel getToggleLabel() {
        return toggleLabel;
    }
    
    /**
     * Gets the parent message component.
     *
     * @return The parent message component
     */
    public MessageComponent getParentMessageComponent() {
        return parentMessageComponent;
    }
    
    /**
     * Programmatically expands the panel with proper layout management.
     * This method can be called externally to expand the panel without user interaction.
     */
    public void expand() {
        if (!isExpanded && !isDisposed) {
            toggleExpanded();
        }
    }
    
    /**
     * Programmatically collapses the panel with proper layout management.
     * This method can be called externally to collapse the panel without user interaction.
     */
    public void collapse() {
        if (isExpanded && !isDisposed) {
            toggleExpanded();
        }
    }
    
    /**
     * Checks if the panel has content to display.
     *
     * @return true if the panel has content, false otherwise
     */
    public boolean hasContent() {
        return contentPanel != null && contentPanel.getComponentCount() > 0;
    }
    
    /**
     * Gets the number of components in the content panel.
     *
     * @return The number of components
     */
    public int getContentComponentCount() {
        return contentPanel != null ? contentPanel.getComponentCount() : 0;
    }
    
    /**
     * Gets the content text area for external access.
     *
     * @return The content text area, or null if not available
     */
    public JTextArea getContentTextArea() {
        return contentTextArea;
    }
    
    /**
     * Gets the content text for external access.
     *
     * @return The content text, or null if not available
     */
    public String getContentText() {
        return contentText;
    }
    
    /**
     * Checks if the component is disposed.
     *
     * @return true if the component is disposed, false otherwise
     */
    public boolean isDisposed() {
        return isDisposed;
    }
    
    /**
     * Refreshes the theme colors for this collapsible panel.
     * Updates all child components to use the current theme colors.
     */
    public void refreshTheme() {
        try {
            // Update main component background
            setBackground(ThemeUtils.panelBackground());
            
            // Update toggle label
            if (toggleLabel != null) {
                toggleLabel.setForeground(ThemeUtils.textForeground());
                toggleLabel.revalidate();
                toggleLabel.repaint();
            }
            
            // Update content panel
            if (contentPanel != null) {
                contentPanel.setBackground(ThemeUtils.panelBackground());
                contentPanel.revalidate();
                contentPanel.repaint();
            }
            
            // Update content text area
            if (contentTextArea != null) {
                contentTextArea.setBackground(ThemeUtils.panelBackground());
                contentTextArea.setForeground(ThemeUtils.textForeground());
                contentTextArea.setBorder(createThemeAwareBorder());
                contentTextArea.revalidate();
                contentTextArea.repaint();
            }
            
            revalidate();
            repaint();
        } catch (Exception e) {
            // Log error but don't fail
            System.err.println("Error refreshing theme in CollapsiblePanel: " + e.getMessage());
        }
    }
} 