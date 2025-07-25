package com.trace.chat.components;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.components.JBTextArea;
import com.trace.common.constants.TriagePanelConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Factory class for creating input panel components with proper styling and behavior.
 * 
 * <p>This factory provides methods for creating all the components needed for
 * a functional input interface, including text areas, buttons, and containers.
 * All components are configured with consistent styling and proper event handling.</p>
 * 
 * <p>This factory implements defensive programming patterns to handle edge cases
 * gracefully and provide robust component creation even with invalid inputs.</p>
 * 
 * @author Alex Ibasitas
 * @version 1.0
 * @since 1.0
 */
public final class InputPanelFactory {
    
    private static final Logger LOG = Logger.getInstance(InputPanelFactory.class);
    
    // Private constructor to prevent instantiation
    private InputPanelFactory() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    
    /**
     * Creates and configures the main input panel.
     * 
     * <p>The input panel serves as the main container for the input interface,
     * providing the foundation for text input and send functionality.</p>
     *
     * @return The configured input panel with proper naming for identification
     */
    public static JPanel createInputPanel() {
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBackground(TriagePanelConstants.getPanelBackground());
        inputPanel.setBorder(TriagePanelConstants.INPUT_PANEL_BORDER);
        inputPanel.setOpaque(true);
        inputPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        inputPanel.setAlignmentY(Component.BOTTOM_ALIGNMENT);
        inputPanel.setName("inputPanel");
        
        return inputPanel;
    }
    
    /**
     * Creates and configures the input container panel.
     * 
     * <p>The input container provides the visual styling and layout for the
     * text input area and send button.</p>
     *
     * @return The configured input container with proper naming for identification
     */
    public static JPanel createInputContainer() {
        JPanel inputContainer = new JPanel(new BorderLayout());
        inputContainer.setBorder(TriagePanelConstants.INPUT_CONTAINER_BORDER_COMPOUND);
        inputContainer.setBackground(TriagePanelConstants.INPUT_CONTAINER_BACKGROUND);
        inputContainer.setOpaque(true);
        inputContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        inputContainer.setAlignmentY(Component.CENTER_ALIGNMENT);
        inputContainer.setName("inputContainer");
        
        return inputContainer;
    }
    
    /**
     * Creates and configures a text area for user input with proper styling and behavior.
     * 
     * <p>The input area is configured with multi-line support, proper keyboard
     * event handling, and consistent styling that matches the chat interface.</p>
     * 
     * <p>This method implements defensive programming - if sendActionListener is null,
     * it creates a text area without keyboard event handling.</p>
     *
     * @param sendActionListener The action listener for send functionality (can be null)
     * @return The configured input text area with proper naming for identification
     */
    public static JBTextArea createInputArea(ActionListener sendActionListener) {
        JBTextArea inputArea = new JBTextArea();
        inputArea.setRows(TriagePanelConstants.INPUT_AREA_ROWS);
        inputArea.setFont(TriagePanelConstants.INPUT_FONT);
        inputArea.setBackground(TriagePanelConstants.INPUT_CONTAINER_BACKGROUND);
        inputArea.setForeground(TriagePanelConstants.WHITE);
        inputArea.setCaretColor(TriagePanelConstants.WHITE);
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setBorder(TriagePanelConstants.EMPTY_BORDER);
        inputArea.setOpaque(false);
        inputArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        inputArea.setAlignmentY(Component.CENTER_ALIGNMENT);
        inputArea.setName("inputArea");
        
        // Set placeholder text (JBTextArea doesn't have setPlaceholderText method)
        // The placeholder text will be handled by the UI theme or can be set via other means
        
        // Add keyboard event handling only if action listener is provided
        if (sendActionListener != null) {
            inputArea.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        if (e.isShiftDown()) {
                            // Shift+Enter for new line
                            return;
                        } else {
                            // Enter to send message
                            e.consume();
                            sendActionListener.actionPerformed(null);
                        }
                    }
                }
            });
        } else {
            LOG.warn("InputPanelFactory: createInputArea called with null sendActionListener - keyboard events disabled");
        }
        
        return inputArea;
    }
    
    /**
     * Creates and configures the button container for the send button.
     * 
     * <p>The button container provides proper sizing and positioning for
     * the send button within the input panel layout.</p>
     *
     * @return The configured button container with proper naming for identification
     */
    public static JPanel createButtonContainer() {
        JPanel buttonContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonContainer.setOpaque(false);
        buttonContainer.setPreferredSize(TriagePanelConstants.BUTTON_CONTAINER_SIZE);
        buttonContainer.setMaximumSize(TriagePanelConstants.BUTTON_CONTAINER_SIZE);
        buttonContainer.setMinimumSize(TriagePanelConstants.BUTTON_CONTAINER_SIZE);
        buttonContainer.setAlignmentX(Component.RIGHT_ALIGNMENT);
        buttonContainer.setAlignmentY(Component.CENTER_ALIGNMENT);
        buttonContainer.setName("buttonContainer");
        
        return buttonContainer;
    }
    
    /**
     * Creates and configures a modern send button with proper styling and behavior.
     * 
     * <p>The send button is configured with an icon, proper sizing, and mouse
     * event handling for hover and press effects. It includes proper tooltips
     * and accessibility features.</p>
     * 
     * <p>This method implements defensive programming - if sendActionListener is null,
     * it creates a button without action listener but with visual styling.</p>
     *
     * @param sendActionListener The action listener for button clicks (can be null)
     * @return The configured send button with proper naming for identification
     */
    public static JButton createModernSendButton(ActionListener sendActionListener) {
        JButton sendButton = new JButton();
        sendButton.setPreferredSize(TriagePanelConstants.SEND_BUTTON_SIZE);
        sendButton.setMaximumSize(TriagePanelConstants.SEND_BUTTON_SIZE);
        sendButton.setMinimumSize(TriagePanelConstants.SEND_BUTTON_SIZE);
        sendButton.setFont(TriagePanelConstants.SEND_BUTTON_FONT);
        sendButton.setForeground(TriagePanelConstants.WHITE);
        sendButton.setBackground(TriagePanelConstants.TRANSPARENT);
        sendButton.setBorder(TriagePanelConstants.SEND_BUTTON_BORDER);
        sendButton.setBorderPainted(false);
        sendButton.setFocusPainted(false);
        sendButton.setContentAreaFilled(false);
        sendButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        sendButton.setToolTipText(TriagePanelConstants.SEND_BUTTON_TOOLTIP);
        sendButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        sendButton.setAlignmentY(Component.CENTER_ALIGNMENT);
        sendButton.setName("sendButton");
        
        // Try to load the send icon
        try {
            Icon sendIcon = IconLoader.getIcon(TriagePanelConstants.SEND_ICON_PATH, InputPanelFactory.class);
            if (sendIcon != null) {
                sendButton.setIcon(sendIcon);
            } else {
                // Fallback to text if icon not found
                sendButton.setText(TriagePanelConstants.SEND_BUTTON_FALLBACK_TEXT);
            }
        } catch (Exception e) {
            LOG.warn("InputPanelFactory: Failed to load send icon, using fallback text", e);
            // Fallback to text if icon loading fails
            sendButton.setText(TriagePanelConstants.SEND_BUTTON_FALLBACK_TEXT);
        }
        
        // Add action listener only if provided
        if (sendActionListener != null) {
            sendButton.addActionListener(sendActionListener);
        } else {
            LOG.warn("InputPanelFactory: createModernSendButton called with null sendActionListener - button will be non-functional");
            sendButton.setEnabled(false); // Disable button if no action listener
        }
        
        // Add mouse event handling for hover effects
        sendButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                sendButton.setBackground(TriagePanelConstants.HOVER_OVERLAY);
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                sendButton.setBackground(TriagePanelConstants.TRANSPARENT);
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                sendButton.setBackground(TriagePanelConstants.PRESS_OVERLAY);
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                sendButton.setBackground(TriagePanelConstants.HOVER_OVERLAY);
            }
        });
        
        return sendButton;
    }
    
    /**
     * Creates a complete input panel setup with all necessary components.
     * 
     * <p><strong>Component Hierarchy:</strong></p>
     * <pre>
     * inputPanel (JPanel - BorderLayout)
     * └── inputContainer (JPanel - BorderLayout)
     *     ├── inputArea (JBTextArea - CENTER)
     *     └── buttonContainer (JPanel - FlowLayout - EAST)
     *         └── sendButton (JButton)
     * </pre>
     * 
     * <p><strong>Return Value Structure:</strong></p>
     * <ul>
     *   <li><strong>Index 0:</strong> inputPanel (JPanel) - Main container panel</li>
     *   <li><strong>Index 1:</strong> inputContainer (JPanel) - Input area container with border</li>
     *   <li><strong>Index 2:</strong> inputArea (JBTextArea) - Text input component</li>
     *   <li><strong>Index 3:</strong> buttonContainer (JPanel) - Button positioning container</li>
     *   <li><strong>Index 4:</strong> sendButton (JButton) - Send action button</li>
     * </ul>
     * 
     * <p><strong>Component Relationships:</strong></p>
     * <ul>
     *   <li>inputPanel contains inputContainer as its only child</li>
     *   <li>inputContainer contains inputArea (CENTER) and buttonContainer (EAST)</li>
     *   <li>buttonContainer contains sendButton for proper positioning</li>
     *   <li>All components are properly named for identification and testing</li>
     * </ul>
     * 
     * <p>This method implements defensive programming - if sendActionListener is null,
     * it creates components with limited functionality but still returns valid objects.</p>
     *
     * @param sendActionListener The action listener for send functionality (can be null)
     * @return An array containing [inputPanel, inputContainer, inputArea, buttonContainer, sendButton]
     *         Total: 5 components in specific hierarchy
     */
    public static Object[] createCompleteInputPanelSetup(ActionListener sendActionListener) {
        // Defensive programming: handle null action listener gracefully
        if (sendActionListener == null) {
            LOG.warn("InputPanelFactory: createCompleteInputPanelSetup called with null sendActionListener - creating limited functionality components");
        }
        
        // Create main components with proper naming
        JPanel inputPanel = createInputPanel();
        inputPanel.setName("inputPanel");
        
        JPanel inputContainer = createInputContainer();
        inputContainer.setName("inputContainer");
        
        JBTextArea inputArea = createInputArea(sendActionListener);
        inputArea.setName("inputArea");
        
        JPanel buttonContainer = createButtonContainer();
        buttonContainer.setName("buttonContainer");
        
        JButton sendButton = createModernSendButton(sendActionListener);
        sendButton.setName("sendButton");
        
        // Assemble the components according to hierarchy
        buttonContainer.add(sendButton);
        inputContainer.add(inputArea, BorderLayout.CENTER);
        inputContainer.add(buttonContainer, BorderLayout.EAST);
        inputPanel.add(inputContainer, BorderLayout.CENTER);
        
        return new Object[]{inputPanel, inputContainer, inputArea, buttonContainer, sendButton};
    }
    
    /**
     * Creates a simple input panel with basic components.
     * 
     * <p>This method creates a minimal input panel setup with just the essential
     * components for basic functionality.</p>
     * 
     * <p>This method implements defensive programming - if sendActionListener is null,
     * it creates a panel with limited functionality but still returns a valid object.</p>
     *
     * @param sendActionListener The action listener for send functionality (can be null)
     * @return The configured input panel
     */
    public static JPanel createSimpleInputPanel(ActionListener sendActionListener) {
        // Defensive programming: handle null action listener gracefully
        if (sendActionListener == null) {
            LOG.warn("InputPanelFactory: createSimpleInputPanel called with null sendActionListener - creating limited functionality panel");
        }
        
        Object[] components = createCompleteInputPanelSetup(sendActionListener);
        return (JPanel) components[0];
    }
    
    /**
     * Clears the input text area.
     * 
     * <p>This method clears the text content of the input area and resets
     * the caret position to the beginning.</p>
     * 
     * <p>This method implements defensive programming - if inputArea is null,
     * it logs a warning and returns early without throwing an exception.</p>
     *
     * @param inputArea The input text area to clear (can be null)
     */
    public static void clearInputArea(JBTextArea inputArea) {
        // Defensive programming: handle null input area gracefully
        if (inputArea == null) {
            LOG.warn("InputPanelFactory: clearInputArea called with null inputArea - skipping clear operation");
            return;
        }
        
        inputArea.setText("");
        inputArea.setCaretPosition(0);
    }
    
    /**
     * Gets the text content from the input area.
     * 
     * <p>This method retrieves the current text content and trims any
     * leading or trailing whitespace.</p>
     * 
     * <p>This method implements defensive programming - if inputArea is null,
     * it returns an empty string instead of throwing an exception.</p>
     *
     * @param inputArea The input text area to get text from (can be null)
     * @return The trimmed text content, or empty string if inputArea is null
     */
    public static String getInputText(JBTextArea inputArea) {
        // Defensive programming: handle null input area gracefully
        if (inputArea == null) {
            LOG.warn("InputPanelFactory: getInputText called with null inputArea - returning empty string");
            return "";
        }
        
        return inputArea.getText().trim();
    }
    
    /**
     * Checks if the input area has content.
     * 
     * <p>This method checks if the input area contains any non-empty text
     * after trimming whitespace.</p>
     * 
     * <p>This method implements defensive programming - if inputArea is null,
     * it returns false instead of throwing an exception.</p>
     *
     * @param inputArea The input text area to check (can be null)
     * @return true if the input area has content, false otherwise or if inputArea is null
     */
    public static boolean hasInputContent(JBTextArea inputArea) {
        // Defensive programming: handle null input area gracefully
        if (inputArea == null) {
            LOG.warn("InputPanelFactory: hasInputContent called with null inputArea - returning false");
            return false;
        }
        
        return !getInputText(inputArea).isEmpty();
    }
    
    /**
     * Sets the input area to be enabled or disabled.
     * 
     * <p>This method controls whether the input area can accept user input.</p>
     * 
     * <p>This method implements defensive programming - if inputArea is null,
     * it logs a warning and returns early without throwing an exception.</p>
     *
     * @param inputArea The input text area to configure (can be null)
     * @param enabled Whether the input area should be enabled
     */
    public static void setInputEnabled(JBTextArea inputArea, boolean enabled) {
        // Defensive programming: handle null input area gracefully
        if (inputArea == null) {
            LOG.warn("InputPanelFactory: setInputEnabled called with null inputArea - skipping enable operation");
            return;
        }
        
        inputArea.setEnabled(enabled);
    }
    
    /**
     * Sets the send button to be enabled or disabled.
     * 
     * <p>This method controls whether the send button can be clicked.</p>
     * 
     * <p>This method implements defensive programming - if sendButton is null,
     * it logs a warning and returns early without throwing an exception.</p>
     *
     * @param sendButton The send button to configure (can be null)
     * @param enabled Whether the send button should be enabled
     */
    public static void setSendButtonEnabled(JButton sendButton, boolean enabled) {
        // Defensive programming: handle null send button gracefully
        if (sendButton == null) {
            LOG.warn("InputPanelFactory: setSendButtonEnabled called with null sendButton - skipping enable operation");
            return;
        }
        
        sendButton.setEnabled(enabled);
    }
    
    // ============================================================================
    // HELPER METHODS FOR COMPONENT ACCESS AND IDENTIFICATION
    // ============================================================================
    
    /**
     * Gets the main input panel from a component array returned by createCompleteInputPanelSetup.
     * 
     * @param components The component array from createCompleteInputPanelSetup
     * @return The main input panel, or null if components is null or invalid
     */
    public static JPanel getInputPanel(Object[] components) {
        if (components == null || components.length < 1) {
            LOG.warn("InputPanelFactory: getInputPanel called with invalid components array");
            return null;
        }
        return (JPanel) components[0];
    }
    
    /**
     * Gets the input container from a component array returned by createCompleteInputPanelSetup.
     * 
     * @param components The component array from createCompleteInputPanelSetup
     * @return The input container, or null if components is null or invalid
     */
    public static JPanel getInputContainer(Object[] components) {
        if (components == null || components.length < 2) {
            LOG.warn("InputPanelFactory: getInputContainer called with invalid components array");
            return null;
        }
        return (JPanel) components[1];
    }
    
    /**
     * Gets the input text area from a component array returned by createCompleteInputPanelSetup.
     * 
     * @param components The component array from createCompleteInputPanelSetup
     * @return The input text area, or null if components is null or invalid
     */
    public static JBTextArea getInputArea(Object[] components) {
        if (components == null || components.length < 3) {
            LOG.warn("InputPanelFactory: getInputArea called with invalid components array");
            return null;
        }
        return (JBTextArea) components[2];
    }
    
    /**
     * Gets the button container from a component array returned by createCompleteInputPanelSetup.
     * 
     * @param components The component array from createCompleteInputPanelSetup
     * @return The button container, or null if components is null or invalid
     */
    public static JPanel getButtonContainer(Object[] components) {
        if (components == null || components.length < 4) {
            LOG.warn("InputPanelFactory: getButtonContainer called with invalid components array");
            return null;
        }
        return (JPanel) components[3];
    }
    
    /**
     * Gets the send button from a component array returned by createCompleteInputPanelSetup.
     * 
     * @param components The component array from createCompleteInputPanelSetup
     * @return The send button, or null if components is null or invalid
     */
    public static JButton getSendButton(Object[] components) {
        if (components == null || components.length < 5) {
            LOG.warn("InputPanelFactory: getSendButton called with invalid components array");
            return null;
        }
        return (JButton) components[4];
    }
    
    /**
     * Validates that a component array has the expected structure and component count.
     * 
     * @param components The component array to validate
     * @return true if the array is valid, false otherwise
     */
    public static boolean isValidComponentArray(Object[] components) {
        if (components == null) {
            LOG.warn("InputPanelFactory: isValidComponentArray called with null components");
            return false;
        }
        
        if (components.length != 5) {
            LOG.warn("InputPanelFactory: isValidComponentArray called with wrong component count: " + components.length + " (expected 5)");
            return false;
        }
        
        // Validate component types
        if (!(components[0] instanceof JPanel) || !(components[1] instanceof JPanel) || 
            !(components[2] instanceof JBTextArea) || !(components[3] instanceof JPanel) || 
            !(components[4] instanceof JButton)) {
            LOG.warn("InputPanelFactory: isValidComponentArray called with wrong component types");
            return false;
        }
        
        return true;
    }
    
    /**
     * Gets the total number of components returned by createCompleteInputPanelSetup.
     * 
     * @return The number of components (always 5)
     */
    public static int getComponentCount() {
        return 5;
    }
} 