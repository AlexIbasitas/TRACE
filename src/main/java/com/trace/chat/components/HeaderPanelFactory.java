package com.trace.chat.components;

import com.trace.common.constants.TriagePanelConstants;
import com.trace.common.utils.ThemeUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Factory class for creating header panel components in the TRACE UI.
 * 
 * <p>This factory encapsulates the creation logic for header-related UI components,
 * including the main header panel and settings panel. It uses the centralized
 * constants from TriagePanelConstants to ensure consistency across all header
 * components.</p>
 * 
 * <p>The factory follows the Factory pattern to provide a clean separation
 * between component creation logic and component usage, making the code
 * more maintainable and testable.</p>
 * 
 * @author Alex Ibasitas
 * @version 1.0
 * @since 1.0
 */
public final class HeaderPanelFactory {
    
    // Private constructor to prevent instantiation
    private HeaderPanelFactory() {
        throw new UnsupportedOperationException("This is a factory class and cannot be instantiated");
    }
    
    /**
     * Creates and configures the custom header panel with logo and scenario information.
     * 
     * <p>This method creates a header panel with a title on the left side and a
     * settings button on the right side. The panel uses proper styling and
     * includes action handling for the settings button.</p>
     *
     * @param settingsActionListener The action listener for settings button clicks
     * @return The configured header panel
     * @throws IllegalArgumentException if settingsActionListener is null
     */
    public static JPanel createCustomHeaderPanel(ActionListener settingsActionListener) {
        if (settingsActionListener == null) {
            throw new IllegalArgumentException("Settings action listener cannot be null");
        }
        
        Color darkBg = ThemeUtils.panelBackground();
        
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(darkBg);
        header.setBorder(TriagePanelConstants.HEADER_BORDER_COMPOUND);
        
        // Create left side with title
        JPanel leftPanel = createHeaderLeftPanel();
        header.add(leftPanel, BorderLayout.WEST);
        
        // Create right side with settings button
        JPanel rightPanel = createHeaderRightPanel(settingsActionListener);
        header.add(rightPanel, BorderLayout.EAST);
        
        return header;
    }
    
    /**
     * Creates the left panel of the header containing the title.
     * 
     * <p>This method creates a panel with the TRACE title using
     * proper font and color styling.</p>
     *
     * @return The configured left panel
     */
    private static JPanel createHeaderLeftPanel() {
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftPanel.setOpaque(true);
        leftPanel.setBackground(ThemeUtils.panelBackground());
        
        JLabel title = new JLabel(TriagePanelConstants.HEADER_TITLE_TEXT);
        title.setFont(TriagePanelConstants.HEADER_TITLE_FONT);
        title.setForeground(ThemeUtils.textForeground());
        leftPanel.add(title);
        
        return leftPanel;
    }
    
    /**
     * Creates the right panel of the header containing the settings button.
     * 
     * <p>This method creates a panel with a settings button that has proper
     * styling and action handling.</p>
     *
     * @param settingsActionListener The action listener for settings button clicks
     * @return The configured right panel
     */
    private static JPanel createHeaderRightPanel(ActionListener settingsActionListener) {
        JButton settingsButton = createSettingsButton(settingsActionListener);
        
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightPanel.setOpaque(true);
        rightPanel.setBackground(ThemeUtils.panelBackground());
        rightPanel.add(settingsButton);
        
        return rightPanel;
    }
    
    /**
     * Creates and configures the settings button with proper styling and behavior.
     * 
     * <p>The settings button is configured with a gear icon, proper styling,
     * and action handling for toggling the settings panel.</p>
     *
     * @param actionListener The action listener for button clicks
     * @return The configured settings button
     * @throws IllegalArgumentException if actionListener is null
     */
    public static JButton createSettingsButton(ActionListener actionListener) {
        if (actionListener == null) {
            throw new IllegalArgumentException("Action listener cannot be null");
        }
        
        JButton settingsButton = new JButton(TriagePanelConstants.SETTINGS_BUTTON_TEXT);
        settingsButton.setFont(TriagePanelConstants.HEADER_BUTTON_FONT);
        settingsButton.setForeground(ThemeUtils.textForeground());
        settingsButton.setBackground(ThemeUtils.panelBackground());
        settingsButton.setBorderPainted(false);
        settingsButton.setFocusPainted(false);
        settingsButton.setContentAreaFilled(false);
        settingsButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        settingsButton.setToolTipText(TriagePanelConstants.SETTINGS_BUTTON_TOOLTIP);
        settingsButton.setBorder(TriagePanelConstants.SETTINGS_BUTTON_BORDER);
        settingsButton.addActionListener(actionListener);
        
        return settingsButton;
    }
    
    /**
     * Creates and configures the settings panel.
     * 
     * <p>This method creates a settings panel with a placeholder message and
     * a back to chat button. The panel uses proper styling and includes
     * action handling for the back button.</p>
     *
     * @param backToChatActionListener The action listener for back to chat button clicks
     * @return The configured settings panel
     * @throws IllegalArgumentException if backToChatActionListener is null
     */
    public static JPanel createSettingsPanel(ActionListener backToChatActionListener) {
        if (backToChatActionListener == null) {
            throw new IllegalArgumentException("Back to chat action listener cannot be null");
        }
        
        Color darkBg = TriagePanelConstants.getPanelBackground();
        
        JPanel settingsPanel = new JPanel(new BorderLayout());
        settingsPanel.setBackground(darkBg);
        
        // Create placeholder content
        JLabel placeholder = createSettingsPlaceholder();
        settingsPanel.add(placeholder, BorderLayout.CENTER);
        
        // Create back to chat button
        JButton backToChat = createBackToChatButton(backToChatActionListener);
        JPanel buttonPanel = createSettingsButtonPanel(backToChat);
        settingsPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        return settingsPanel;
    }
    
    /**
     * Creates the settings placeholder label.
     * 
     * <p>This method creates a placeholder label for the settings panel
     * with proper styling and centering.</p>
     *
     * @return The configured placeholder label
     */
    private static JLabel createSettingsPlaceholder() {
        JLabel placeholder = new JLabel(TriagePanelConstants.SETTINGS_PLACEHOLDER_TEXT);
        placeholder.setFont(TriagePanelConstants.SETTINGS_PLACEHOLDER_FONT);
        placeholder.setForeground(TriagePanelConstants.WHITE);
        placeholder.setHorizontalAlignment(SwingConstants.CENTER);
        
        return placeholder;
    }
    
    /**
     * Creates the back to chat button.
     * 
     * <p>This method creates a button for returning to the chat view
     * with proper styling and action handling.</p>
     *
     * @param actionListener The action listener for button clicks
     * @return The configured back to chat button
     */
    private static JButton createBackToChatButton(ActionListener actionListener) {
        JButton backToChat = new JButton(TriagePanelConstants.BACK_TO_CHAT_TEXT);
        backToChat.setFont(TriagePanelConstants.SETTINGS_BUTTON_FONT);
        backToChat.setFocusPainted(false);
        backToChat.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backToChat.addActionListener(actionListener);
        
        return backToChat;
    }
    
    /**
     * Creates the button panel for the settings panel.
     * 
     * <p>This method creates a panel to hold the back to chat button
     * with proper styling and positioning.</p>
     *
     * @param backToChatButton The back to chat button to include
     * @return The configured button panel
     */
    private static JPanel createSettingsButtonPanel(JButton backToChatButton) {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(TriagePanelConstants.getPanelBackground());
        buttonPanel.add(backToChatButton);
        
        return buttonPanel;
    }
    
    /**
     * Creates a complete header setup with all necessary components.
     * 
     * <p>This method creates and configures all the components needed for
     * a functional header, including the main header panel and settings panel.
     * It returns an array containing both panels for external use.</p>
     *
     * @param settingsActionListener The action listener for settings functionality
     * @param backToChatActionListener The action listener for back to chat functionality
     * @return An array containing [headerPanel, settingsPanel]
     * @throws IllegalArgumentException if any action listener is null
     */
    public static Object[] createCompleteHeaderSetup(ActionListener settingsActionListener, 
                                                   ActionListener backToChatActionListener) {
        if (settingsActionListener == null) {
            throw new IllegalArgumentException("Settings action listener cannot be null");
        }
        if (backToChatActionListener == null) {
            throw new IllegalArgumentException("Back to chat action listener cannot be null");
        }
        
        JPanel headerPanel = createCustomHeaderPanel(settingsActionListener);
        JPanel settingsPanel = createSettingsPanel(backToChatActionListener);
        
        return new Object[]{headerPanel, settingsPanel};
    }
    
    /**
     * Creates a simple header panel with basic components.
     * 
     * <p>This method creates a minimal header panel setup with just the essential
     * components for basic functionality.</p>
     *
     * @param settingsActionListener The action listener for settings functionality
     * @return The configured header panel
     * @throws IllegalArgumentException if settingsActionListener is null
     */
    public static JPanel createSimpleHeaderPanel(ActionListener settingsActionListener) {
        if (settingsActionListener == null) {
            throw new IllegalArgumentException("Settings action listener cannot be null");
        }
        
        return createCustomHeaderPanel(settingsActionListener);
    }
    
    /**
     * Creates a simple settings panel with basic components.
     * 
     * <p>This method creates a minimal settings panel setup with just the essential
     * components for basic functionality.</p>
     *
     * @param backToChatActionListener The action listener for back to chat functionality
     * @return The configured settings panel
     * @throws IllegalArgumentException if backToChatActionListener is null
     */
    public static JPanel createSimpleSettingsPanel(ActionListener backToChatActionListener) {
        if (backToChatActionListener == null) {
            throw new IllegalArgumentException("Back to chat action listener cannot be null");
        }
        
        return createSettingsPanel(backToChatActionListener);
    }
    
    /**
     * Updates the header title text.
     * 
     * <p>This method updates the title text in the header panel. It searches
     * for the title label and updates its text.</p>
     *
     * @param headerPanel The header panel to update
     * @param newTitle The new title text
     * @throws IllegalArgumentException if headerPanel is null or newTitle is null
     */
    public static void updateHeaderTitle(JPanel headerPanel, String newTitle) {
        if (headerPanel == null) {
            throw new IllegalArgumentException("Header panel cannot be null");
        }
        if (newTitle == null) {
            throw new IllegalArgumentException("New title cannot be null");
        }
        
        // Find the title label in the header panel
        for (Component component : headerPanel.getComponents()) {
            if (component instanceof JPanel) {
                JPanel panel = (JPanel) component;
                for (Component subComponent : panel.getComponents()) {
                    if (subComponent instanceof JLabel) {
                        JLabel label = (JLabel) subComponent;
                        if (TriagePanelConstants.HEADER_TITLE_TEXT.equals(label.getText())) {
                            label.setText(newTitle);
                            return;
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Sets the settings button to be enabled or disabled.
     * 
     * <p>This method controls whether the settings button can be clicked.</p>
     *
     * @param headerPanel The header panel containing the settings button
     * @param enabled Whether the settings button should be enabled
     * @throws IllegalArgumentException if headerPanel is null
     */
    public static void setSettingsButtonEnabled(JPanel headerPanel, boolean enabled) {
        if (headerPanel == null) {
            throw new IllegalArgumentException("Header panel cannot be null");
        }
        
        // Find the settings button in the header panel
        for (Component component : headerPanel.getComponents()) {
            if (component instanceof JPanel) {
                JPanel panel = (JPanel) component;
                for (Component subComponent : panel.getComponents()) {
                    if (subComponent instanceof JButton) {
                        JButton button = (JButton) subComponent;
                        if (TriagePanelConstants.SETTINGS_BUTTON_TEXT.equals(button.getText())) {
                            button.setEnabled(enabled);
                            return;
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Sets the back to chat button to be enabled or disabled.
     * 
     * <p>This method controls whether the back to chat button can be clicked.</p>
     *
     * @param settingsPanel The settings panel containing the back to chat button
     * @param enabled Whether the back to chat button should be enabled
     * @throws IllegalArgumentException if settingsPanel is null
     */
    public static void setBackToChatButtonEnabled(JPanel settingsPanel, boolean enabled) {
        if (settingsPanel == null) {
            throw new IllegalArgumentException("Settings panel cannot be null");
        }
        
        // Find the back to chat button in the settings panel
        for (Component component : settingsPanel.getComponents()) {
            if (component instanceof JPanel) {
                JPanel panel = (JPanel) component;
                for (Component subComponent : panel.getComponents()) {
                    if (subComponent instanceof JButton) {
                        JButton button = (JButton) subComponent;
                        if (TriagePanelConstants.BACK_TO_CHAT_TEXT.equals(button.getText())) {
                            button.setEnabled(enabled);
                            return;
                        }
                    }
                }
            }
        }
    }
} 