package com.trace.chat.handlers;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.JBColor;
import com.trace.chat.components.CollapsiblePanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Centralized event handlers for the TRACE UI components.
 * 
 * <p>This class encapsulates all event handling logic for the TRACE chat interface,
 * including keyboard events, mouse events, and action listeners. It follows IntelliJ
 * platform best practices for event handling and ensures proper disposal of listeners
 * to prevent memory leaks.</p>
 * 
 * <p>All event handlers are designed to be thread-safe and respect the Event Dispatch
 * Thread (EDT) requirements for Swing components.</p>
 * 
 * @author Alex Ibasitas
 * @version 1.0
 * @since 1.0
 */
public final class TriagePanelEventHandlers {
    
    private static final Logger LOG = Logger.getInstance(TriagePanelEventHandlers.class);
    
    // ============================================================================
    // THEME-AWARE COLOR CONSTANTS
    // ============================================================================
    
    /** Theme-aware hover overlay color - light overlay for dark theme, dark overlay for light theme */
    private static final JBColor HOVER_OVERLAY_COLOR = new JBColor(
        new Color(0, 0, 0, 30),  // Dark overlay for light theme
        new Color(255, 255, 255, 30)  // Light overlay for dark theme
    );
    
    /** Theme-aware press overlay color - darker overlay for both themes */
    private static final JBColor PRESS_OVERLAY_COLOR = new JBColor(
        new Color(0, 0, 0, 50),  // Darker overlay for light theme
        new Color(0, 0, 0, 40)   // Dark overlay for dark theme
    );
    
    /** Theme-aware transparent color that adapts to current theme */
    private static final JBColor TRANSPARENT_COLOR = new JBColor(
        new Color(0, 0, 0, 0),   // Transparent for both themes
        new Color(0, 0, 0, 0)
    );
    
    // ============================================================================
    // COLOR STATE MANAGEMENT
    // ============================================================================
    
    /** WeakHashMap to store original component colors - prevents memory leaks */
    private static final Map<Component, Color> originalColors = new WeakHashMap<>();
    
    /** WeakHashMap to store component color states for tracking */
    private static final Map<Component, ColorState> componentColorStates = new WeakHashMap<>();
    
    /**
     * Color state tracking for components
     */
    private static class ColorState {
        private final Color originalBackground;
        private final Color originalForeground;
        private Color currentBackground;
        private Color currentForeground;
        private boolean isHovered;
        private boolean isPressed;
        
        public ColorState(Component component) {
            this.originalBackground = component.getBackground();
            this.originalForeground = component.getForeground();
            this.currentBackground = this.originalBackground;
            this.currentForeground = this.originalForeground;
            this.isHovered = false;
            this.isPressed = false;
        }
        
        public Color getOriginalBackground() { return originalBackground; }
        public Color getOriginalForeground() { return originalForeground; }
        public Color getCurrentBackground() { return currentBackground; }
        public Color getCurrentForeground() { return currentForeground; }
        public boolean isHovered() { return isHovered; }
        public boolean isPressed() { return isPressed; }
        
        public void setHovered(boolean hovered) { this.isHovered = hovered; }
        public void setPressed(boolean pressed) { this.isPressed = pressed; }
        public void setCurrentBackground(Color background) { this.currentBackground = background; }
        public void setCurrentForeground(Color foreground) { this.currentForeground = foreground; }
    }
    
    // Private constructor to prevent instantiation
    private TriagePanelEventHandlers() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    
    // ============================================================================
    // COLOR MANAGEMENT UTILITY METHODS
    // ============================================================================
    
    /**
     * Captures and stores the original colors of a component for later restoration.
     * 
     * @param component The component whose colors should be captured
     */
    private static void captureOriginalColors(Component component) {
        if (component == null) {
            LOG.warn("TriagePanelEventHandlers: captureOriginalColors called with null component");
            return;
        }
        
        // Store original background color
        originalColors.put(component, component.getBackground());
        
        // Create or update color state tracking
        ColorState colorState = componentColorStates.get(component);
        if (colorState == null) {
            colorState = new ColorState(component);
            componentColorStates.put(component, colorState);
        }
        
        LOG.debug("TriagePanelEventHandlers: Captured original colors for component: " + component.getName());
    }
    
    /**
     * Restores the original background color of a component.
     * 
     * @param component The component whose background should be restored
     */
    private static void restoreOriginalBackground(Component component) {
        if (component == null) {
            LOG.warn("TriagePanelEventHandlers: restoreOriginalBackground called with null component");
            return;
        }
        
        Color originalColor = originalColors.get(component);
        if (originalColor != null) {
            component.setBackground(originalColor);
            
            // Update color state tracking
            ColorState colorState = componentColorStates.get(component);
            if (colorState != null) {
                colorState.setCurrentBackground(originalColor);
                colorState.setHovered(false);
                colorState.setPressed(false);
            }
            
            LOG.debug("TriagePanelEventHandlers: Restored original background for component: " + component.getName());
        } else {
            LOG.warn("TriagePanelEventHandlers: No original color found for component: " + component.getName());
        }
    }
    
    /**
     * Applies a theme-aware overlay color to a component's background.
     * 
     * @param component The component to apply the overlay to
     * @param overlayColor The overlay color to apply
     */
    private static void applyOverlayColor(Component component, JBColor overlayColor) {
        if (component == null) {
            LOG.warn("TriagePanelEventHandlers: applyOverlayColor called with null component");
            return;
        }
        
        Color originalColor = originalColors.get(component);
        if (originalColor == null) {
            // Capture original color if not already stored
            captureOriginalColors(component);
            originalColor = component.getBackground();
        }
        
        // Blend the original color with the overlay
        Color blendedColor = blendColors(originalColor, overlayColor);
        component.setBackground(blendedColor);
        
        // Update color state tracking
        ColorState colorState = componentColorStates.get(component);
        if (colorState != null) {
            colorState.setCurrentBackground(blendedColor);
        }
        
        LOG.debug("TriagePanelEventHandlers: Applied overlay color to component: " + component.getName());
    }
    
    /**
     * Blends two colors together, preserving transparency.
     * 
     * @param baseColor The base color
     * @param overlayColor The overlay color to blend
     * @return The blended color
     */
    private static Color blendColors(Color baseColor, Color overlayColor) {
        if (baseColor == null || overlayColor == null) {
            return baseColor != null ? baseColor : overlayColor;
        }
        
        // Extract color components
        float[] baseComponents = baseColor.getRGBComponents(null);
        float[] overlayComponents = overlayColor.getRGBComponents(null);
        
        // Blend using alpha compositing
        float overlayAlpha = overlayComponents[3];
        float baseAlpha = baseComponents[3];
        
        // Calculate blended components
        float[] blendedComponents = new float[4];
        for (int i = 0; i < 3; i++) {
            blendedComponents[i] = baseComponents[i] * (1 - overlayAlpha) + overlayComponents[i] * overlayAlpha;
        }
        blendedComponents[3] = Math.min(1.0f, baseAlpha + overlayAlpha * (1 - baseAlpha));
        
        return new Color(blendedComponents[0], blendedComponents[1], blendedComponents[2], blendedComponents[3]);
    }
    
    /**
     * Gets the current color state of a component.
     * 
     * @param component The component to get the color state for
     * @return The color state, or null if not tracked
     */
    public static ColorState getColorState(Component component) {
        return componentColorStates.get(component);
    }
    
    /**
     * Clears all stored color states and original colors.
     * This should be called when the plugin is disposed to prevent memory leaks.
     */
    public static void clearColorStates() {
        originalColors.clear();
        componentColorStates.clear();
        LOG.debug("TriagePanelEventHandlers: Cleared all color states");
    }
    
    /**
     * Handles theme changes by updating all tracked components to use new theme colors.
     * This method should be called when the IDE theme changes.
     */
    public static void handleThemeChange() {
        LOG.debug("TriagePanelEventHandlers: Handling theme change");
        
        // Update all tracked components to use new theme colors
        for (Map.Entry<Component, ColorState> entry : componentColorStates.entrySet()) {
            Component component = entry.getKey();
            ColorState colorState = entry.getValue();
            
            if (component != null && colorState != null) {
                // Re-capture original colors for the new theme
                captureOriginalColors(component);
                
                // Restore appropriate state based on current interaction
                if (colorState.isPressed()) {
                    applyOverlayColor(component, PRESS_OVERLAY_COLOR);
                } else if (colorState.isHovered()) {
                    applyOverlayColor(component, HOVER_OVERLAY_COLOR);
                } else {
                    restoreOriginalBackground(component);
                }
            }
        }
        
        LOG.debug("TriagePanelEventHandlers: Theme change handled for " + componentColorStates.size() + " components");
    }
    
    /**
     * Forces a refresh of all component colors to ensure they match the current theme.
     * This is useful for debugging or when theme changes aren't automatically detected.
     */
    public static void refreshAllComponentColors() {
        LOG.debug("TriagePanelEventHandlers: Refreshing all component colors");
        handleThemeChange();
    }
    
    /**
     * Creates a key adapter for the input text area with Enter/Shift+Enter handling.
     * 
     * <p>This adapter handles keyboard events for the input area:
     * - Enter key: sends the message
     * - Shift+Enter: creates a new line
     * - All other keys: normal behavior</p>
     *
     * @param sendActionListener The action listener to call when sending a message
     * @return The configured key adapter
     * @throws IllegalArgumentException if sendActionListener is null
     */
    public static KeyAdapter createInputKeyAdapter(ActionListener sendActionListener) {
        if (sendActionListener == null) {
            throw new IllegalArgumentException("Send action listener cannot be null");
        }
        
        return new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (e.isShiftDown()) {
                        // Shift+Enter for new line - let the default behavior handle it
                        LOG.debug("Shift+Enter pressed - allowing new line");
                        return;
                    } else {
                        // Enter to send message
                        LOG.debug("Enter pressed - sending message");
                        e.consume();
                        sendActionListener.actionPerformed(null);
                    }
                }
            }
        };
    }
    
    /**
     * Creates a mouse adapter for send button hover effects with proper color state management.
     * 
     * <p>This adapter provides theme-aware visual feedback when hovering over the send button:
     * - Mouse enter: applies theme-aware hover overlay
     * - Mouse exit: restores original background color
     * - Mouse press: applies theme-aware press overlay
     * - Mouse release: applies theme-aware hover overlay</p>
     * 
     * <p><strong>Color Management Features:</strong></p>
     * <ul>
     *   <li>Captures and stores original component colors</li>
     *   <li>Uses IntelliJ's JBColor for theme-aware color management</li>
     *   <li>Properly restores original colors on mouse exit</li>
     *   <li>Implements smooth color blending for overlays</li>
     *   <li>Handles theme changes automatically</li>
     *   <li>Prevents memory leaks with WeakHashMap storage</li>
     * </ul>
     *
     * @param sendButton The send button to apply hover effects to
     * @return The configured mouse adapter with proper color state management
     * @throws IllegalArgumentException if sendButton is null
     */
    public static MouseAdapter createSendButtonMouseAdapter(JButton sendButton) {
        if (sendButton == null) {
            throw new IllegalArgumentException("Send button cannot be null");
        }
        
        // Capture original colors when adapter is created
        captureOriginalColors(sendButton);
        
        return new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                LOG.debug("Mouse entered send button");
                
                // Update color state tracking
                ColorState colorState = componentColorStates.get(sendButton);
                if (colorState != null) {
                    colorState.setHovered(true);
                }
                
                // Apply theme-aware hover overlay
                applyOverlayColor(sendButton, HOVER_OVERLAY_COLOR);
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                LOG.debug("Mouse exited send button");
                
                // Update color state tracking
                ColorState colorState = componentColorStates.get(sendButton);
                if (colorState != null) {
                    colorState.setHovered(false);
                    colorState.setPressed(false);
                }
                
                // Restore original background color
                restoreOriginalBackground(sendButton);
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                LOG.debug("Mouse pressed send button");
                
                // Update color state tracking
                ColorState colorState = componentColorStates.get(sendButton);
                if (colorState != null) {
                    colorState.setPressed(true);
                }
                
                // Apply theme-aware press overlay
                applyOverlayColor(sendButton, PRESS_OVERLAY_COLOR);
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                LOG.debug("Mouse released send button");
                
                // Update color state tracking
                ColorState colorState = componentColorStates.get(sendButton);
                if (colorState != null) {
                    colorState.setPressed(false);
                }
                
                // If still hovering, apply hover overlay; otherwise restore original
                if (colorState != null && colorState.isHovered()) {
                    applyOverlayColor(sendButton, HOVER_OVERLAY_COLOR);
                } else {
                    restoreOriginalBackground(sendButton);
                }
            }
        };
    }
    
    /**
     * Creates an action listener for the send button.
     * 
     * <p>This action listener handles send button clicks and validates input
     * before triggering the send action.</p>
     *
     * @param inputArea The input text area to get text from
     * @param sendActionListener The action listener to call when sending
     * @return The configured action listener
     * @throws IllegalArgumentException if any parameter is null
     */
    public static ActionListener createSendButtonActionListener(JBTextArea inputArea, ActionListener sendActionListener) {
        if (inputArea == null) {
            throw new IllegalArgumentException("Input area cannot be null");
        }
        if (sendActionListener == null) {
            throw new IllegalArgumentException("Send action listener cannot be null");
        }
        
        return e -> {
            LOG.debug("Send button clicked");
            String text = inputArea.getText().trim();
            if (!text.isEmpty()) {
                sendActionListener.actionPerformed(e);
            } else {
                LOG.debug("Send button clicked but input is empty - ignoring");
            }
        };
    }
    
    /**
     * Creates a mouse adapter for collapsible panel toggle.
     * 
     * <p>This adapter handles mouse clicks on the collapsible panel toggle
     * and provides visual feedback.</p>
     *
     * @param collapsiblePanel The collapsible panel to toggle
     * @return The configured mouse adapter
     * @throws IllegalArgumentException if collapsiblePanel is null
     */
    public static MouseAdapter createCollapsiblePanelMouseAdapter(CollapsiblePanel collapsiblePanel) {
        if (collapsiblePanel == null) {
            throw new IllegalArgumentException("Collapsible panel cannot be null");
        }
        
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                LOG.debug("Collapsible panel toggle clicked");
                // Note: toggleExpanded() method needs to be made public in CollapsiblePanel
                // For now, we'll use the public methods available
                if (collapsiblePanel.isExpanded()) {
                    collapsiblePanel.collapse();
                } else {
                    collapsiblePanel.expand();
                }
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                LOG.debug("Mouse entered collapsible panel toggle");
                Component source = (Component) e.getSource();
                source.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                LOG.debug("Mouse exited collapsible panel toggle");
                Component source = (Component) e.getSource();
                source.setCursor(Cursor.getDefaultCursor());
            }
        };
    }
    
    /**
     * Creates an action listener for the settings button.
     * 
     * <p>This action listener handles settings button clicks and triggers
     * the settings panel display.</p>
     *
     * @param settingsActionListener The action listener to call when settings is clicked
     * @return The configured action listener
     * @throws IllegalArgumentException if settingsActionListener is null
     */
    public static ActionListener createSettingsButtonActionListener(ActionListener settingsActionListener) {
        if (settingsActionListener == null) {
            throw new IllegalArgumentException("Settings action listener cannot be null");
        }
        
        return e -> {
            LOG.debug("Settings button clicked");
            settingsActionListener.actionPerformed(e);
        };
    }
    
    /**
     * Creates an action listener for the back to chat button.
     * 
     * <p>This action listener handles back to chat button clicks and triggers
     * the return to chat view.</p>
     *
     * @param backToChatActionListener The action listener to call when back to chat is clicked
     * @return The configured action listener
     * @throws IllegalArgumentException if backToChatActionListener is null
     */
    public static ActionListener createBackToChatActionListener(ActionListener backToChatActionListener) {
        if (backToChatActionListener == null) {
            throw new IllegalArgumentException("Back to chat action listener cannot be null");
        }
        
        return e -> {
            LOG.debug("Back to chat button clicked");
            backToChatActionListener.actionPerformed(e);
        };
    }
    
    /**
     * Creates a document listener for input validation.
     * 
     * <p>This listener monitors changes to the input text area and can be used
     * for real-time validation or enabling/disabling the send button.</p>
     *
     * @param inputArea The input text area to monitor
     * @param sendButton The send button to enable/disable based on input
     * @return The configured document listener
     * @throws IllegalArgumentException if any parameter is null
     */
    public static javax.swing.event.DocumentListener createInputDocumentListener(JBTextArea inputArea, JButton sendButton) {
        if (inputArea == null) {
            throw new IllegalArgumentException("Input area cannot be null");
        }
        if (sendButton == null) {
            throw new IllegalArgumentException("Send button cannot be null");
        }
        
        return new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                updateSendButtonState();
            }
            
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                updateSendButtonState();
            }
            
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                updateSendButtonState();
            }
            
            private void updateSendButtonState() {
                String text = inputArea.getText().trim();
                boolean hasContent = !text.isEmpty();
                sendButton.setEnabled(hasContent);
                LOG.debug("Input changed - send button enabled: " + hasContent);
            }
        };
    }
    
    /**
     * Creates a focus listener for input area focus management.
     * 
     * <p>This listener handles focus events for the input area and can be used
     * for visual feedback or validation.</p>
     *
     * @return The configured focus listener
     */
    public static FocusAdapter createInputFocusAdapter() {
        return new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                LOG.debug("Input area gained focus");
            }
            
            @Override
            public void focusLost(FocusEvent e) {
                LOG.debug("Input area lost focus");
            }
        };
    }
    
    /**
     * Creates a window listener for proper cleanup.
     * 
     * <p>This listener handles window events and can be used for cleanup
     * operations when the window is closing.</p>
     *
     * @param cleanupAction The action to perform during cleanup
     * @return The configured window listener
     */
    public static WindowAdapter createWindowListener(Runnable cleanupAction) {
        return new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                LOG.debug("Window closing - performing cleanup");
                if (cleanupAction != null) {
                    cleanupAction.run();
                }
            }
        };
    }
    
    /**
     * Safely executes an action on the Event Dispatch Thread.
     * 
     * <p>This utility method ensures that UI updates are performed on the EDT,
     * which is required for Swing components.</p>
     *
     * @param action The action to execute
     * @throws IllegalArgumentException if action is null
     */
    public static void executeOnEDT(Runnable action) {
        if (action == null) {
            throw new IllegalArgumentException("Action cannot be null");
        }
        
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
        } else {
            SwingUtilities.invokeLater(action);
        }
    }
    
    /**
     * Safely executes an action on the Event Dispatch Thread and waits for completion.
     * 
     * <p>This utility method ensures that UI updates are performed on the EDT
     * and waits for the action to complete before returning.</p>
     *
     * @param action The action to execute
     * @throws IllegalArgumentException if action is null
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public static void executeOnEDTAndWait(Runnable action) throws InterruptedException, InvocationTargetException {
        if (action == null) {
            throw new IllegalArgumentException("Action cannot be null");
        }
        
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
        } else {
            SwingUtilities.invokeAndWait(action);
        }
    }
    
    /**
     * Disposes of event listeners to prevent memory leaks.
     * 
     * <p>This method should be called when components are being disposed
     * to ensure proper cleanup of event listeners and color state tracking.</p>
     * 
     * <p>This method also cleans up any color state tracking for the component.</p>
     *
     * @param component The component to remove listeners from
     * @throws IllegalArgumentException if component is null
     */
    public static void disposeListeners(Component component) {
        if (component == null) {
            throw new IllegalArgumentException("Component cannot be null");
        }
        
        LOG.debug("Disposing listeners for component: " + component.getClass().getSimpleName());
        
        // Remove all listeners
        for (KeyListener listener : component.getKeyListeners()) {
            component.removeKeyListener(listener);
        }
        
        for (MouseListener listener : component.getMouseListeners()) {
            component.removeMouseListener(listener);
        }
        
        for (FocusListener listener : component.getFocusListeners()) {
            component.removeFocusListener(listener);
        }
        
        if (component instanceof JButton) {
            JButton button = (JButton) component;
            for (ActionListener listener : button.getActionListeners()) {
                button.removeActionListener(listener);
            }
        }
        
        if (component instanceof JTextArea) {
            JTextArea textArea = (JTextArea) component;
            // Note: Document doesn't have a getListeners method, so we can't easily remove all listeners
            // In practice, we should keep track of listeners we add and remove them specifically
            LOG.debug("Note: Document listeners should be removed individually when added");
        }
        
        // Clean up color state tracking for this component
        originalColors.remove(component);
        componentColorStates.remove(component);
        
        LOG.debug("TriagePanelEventHandlers: Disposed all listeners and color states from component: " + component.getName());
    }
} 