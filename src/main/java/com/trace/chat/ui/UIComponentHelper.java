package com.trace.chat.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import com.trace.ai.configuration.AISettings;
import com.trace.ai.ui.SettingsPanel;
import com.trace.common.constants.TriagePanelConstants;
import com.trace.common.utils.ThemeUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Helper class for UI component creation in the TriagePanelView.
 * Provides methods for creating and configuring various UI components including buttons, panels, and input elements.
 * 
 * <p>This class encapsulates all UI component creation logic to reduce the complexity
 * of the main TriagePanelView class and improve code organization.</p>
 * 
 * @author Alex Ibasitas
 * @version 1.0
 * @since 1.0
 */
public class UIComponentHelper {
    
    private static final Logger LOG = Logger.getInstance(UIComponentHelper.class);
    
    // Analysis mode constants
    private static final String ANALYSIS_MODE_OVERVIEW = "Quick Overview";
    private static final String ANALYSIS_MODE_FULL = "Full Analysis";
    
    /**
     * Creates the AI toggle button with proper styling and functionality.
     *
     * @return The configured AI toggle button
     */
    public static JButton createAIToggleButton() {
        JButton aiToggleButton = new JButton("⏻");
        aiToggleButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        aiToggleButton.setBorderPainted(false);
        aiToggleButton.setFocusPainted(false);
        aiToggleButton.setContentAreaFilled(false);
        aiToggleButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        // Zero padding on the toggle button
        aiToggleButton.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        
        // Set minimum and preferred sizes to be compact
        Dimension buttonSize = new Dimension(20, 20);
        aiToggleButton.setMinimumSize(buttonSize);
        aiToggleButton.setPreferredSize(buttonSize);
        aiToggleButton.setMaximumSize(buttonSize);
        
        // Update initial appearance
        updateAIToggleButtonAppearance(aiToggleButton);
        
        // Add action listener to toggle AI state
        aiToggleButton.addActionListener(e -> {
            AISettings aiSettings = AISettings.getInstance();
            boolean currentState = aiSettings.isTraceEnabled();
            aiSettings.setTraceEnabled(!currentState);
            updateAIToggleButtonAppearance(aiToggleButton);
            LOG.info("AI toggle clicked - new state: " + (!currentState));
        });
        
        return aiToggleButton;
    }
    
    /**
     * Creates the clear chat button with proper styling and functionality.
     *
     * @return The configured clear chat button
     */
    public static JButton createClearChatButton() {
        JButton clearChatButton = new JButton();
        
        // Load custom red trash icon
        try {
            ImageIcon trashIcon = new ImageIcon(UIComponentHelper.class.getResource("/icons/trash_20.png"));
            // Scale the icon down to 18x18
            Image img = trashIcon.getImage();
            Image scaledImg = img.getScaledInstance(18, 18, Image.SCALE_SMOOTH);
            clearChatButton.setIcon(new ImageIcon(scaledImg));
            clearChatButton.setText(""); // Remove text, use icon only
        } catch (Exception e) {
            LOG.warn("Could not load trash icon, falling back to text: " + e.getMessage());
            clearChatButton.setText("🗑");
        }
        
        clearChatButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        clearChatButton.setBorderPainted(false);
        clearChatButton.setFocusPainted(false);
        clearChatButton.setContentAreaFilled(false);
        clearChatButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clearChatButton.setToolTipText("Clear Chat");
        
        // Zero padding on the button
        clearChatButton.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        
        // Set minimum and preferred sizes to be compact - smaller size
        Dimension buttonSize = new Dimension(18, 18);
        clearChatButton.setMinimumSize(buttonSize);
        clearChatButton.setPreferredSize(buttonSize);
        clearChatButton.setMaximumSize(buttonSize);
        
        return clearChatButton;
    }
    
    /**
     * Creates the settings button with proper styling and functionality.
     *
     * @return The configured settings button
     */
    public static JButton createSettingsButton() {
        JButton settingsButton = new JButton("⚙");
        settingsButton.setFont(new Font("Segoe UI", Font.PLAIN, 16)); // Increased from 14 to 16
        settingsButton.setForeground(ThemeUtils.textForeground());
        settingsButton.setBorderPainted(false);
        settingsButton.setFocusPainted(false);
        settingsButton.setContentAreaFilled(false);
        settingsButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        settingsButton.setToolTipText("Settings");
        
        // Zero padding on the button
        settingsButton.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        
        // Set minimum and preferred sizes to be compact - matching size
        Dimension buttonSize = new Dimension(20, 20);
        settingsButton.setMinimumSize(buttonSize);
        settingsButton.setPreferredSize(buttonSize);
        settingsButton.setMaximumSize(buttonSize);
        
        return settingsButton;
    }
    
    /**
     * Creates the analysis mode button with proper styling and functionality.
     * Shows "Quick Overview | Full Analysis" with the selected option in bold.
     *
     * @param currentAnalysisMode The current analysis mode
     * @return The configured analysis mode button
     */
    public static JButton createAnalysisModeButton(String currentAnalysisMode) {
        LOG.debug("Creating analysis mode button");
        
        JButton analysisModeButton = new JButton();
        
        // Minimal styling - reverted to original approach
        analysisModeButton.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        analysisModeButton.setForeground(ThemeUtils.textForeground());
        analysisModeButton.setBorderPainted(false);
        analysisModeButton.setFocusPainted(false);
        analysisModeButton.setContentAreaFilled(false);
        analysisModeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        analysisModeButton.setToolTipText("Click to switch between Quick Overview and Full Analysis modes");
        
        // Minimal padding for compact appearance
        analysisModeButton.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0)); // NO PADDING - container handles alignment
        
        // Set compact size for position above input - reduced height for tighter appearance
        Dimension buttonSize = new Dimension(250, 16); // Reduced height for tighter appearance
        analysisModeButton.setMinimumSize(buttonSize);
        analysisModeButton.setPreferredSize(buttonSize);
        analysisModeButton.setMaximumSize(buttonSize);
        
        // Update button text based on current mode
        updateAnalysisModeButtonText(analysisModeButton, currentAnalysisMode);
        
        LOG.debug("Analysis mode button creation complete");
        return analysisModeButton;
    }
    
    /**
     * Creates a modern send button with custom icon and styling.
     *
     * @return The configured send button
     */
    public static JButton createModernSendButton() {
        JButton sendButton = new JButton();
        
        // NATURAL SCALING: Let IntelliJ handle icon scaling properly
        try {
            Icon sendIcon = IconLoader.getIcon("/icons/send_32.png", UIComponentHelper.class);
            sendButton.setIcon(sendIcon);
        } catch (Exception e) {
            sendButton.setText("→");
        }
        
        // NATURAL SIZE: Let button size itself based on icon and IntelliJ scaling
        sendButton.setIconTextGap(0);
        
        // Transparent background
        sendButton.setBackground(new JBColor(new Color(0, 0, 0, 0), new Color(0, 0, 0, 0)));
        sendButton.setForeground(ThemeUtils.textForeground());
        sendButton.setFocusPainted(false);
        sendButton.setBorderPainted(false);
        sendButton.setContentAreaFilled(false);
        sendButton.setOpaque(false);
        
        // Simple approach: just make it transparent and let the icon be round
        sendButton.setOpaque(false);
        sendButton.setContentAreaFilled(false);
        sendButton.setBorderPainted(false);
        sendButton.setFocusPainted(false);
        sendButton.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        
        // Cursor and tooltip
        sendButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        sendButton.setToolTipText("Send message");
        
        // Simple hover effect - just a subtle background change
        sendButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                sendButton.setBackground(new Color(255, 255, 255, 20));
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                sendButton.setBackground(new Color(0, 0, 0, 0));
            }
        });
        
        return sendButton;
    }
    
    /**
     * Updates the AI toggle button appearance based on the current AI state.
     *
     * @param button The toggle button to update
     */
    public static void updateAIToggleButtonAppearance(JButton button) {
        AISettings aiSettings = AISettings.getInstance();
        boolean aiEnabled = aiSettings.isTraceEnabled();
        
        if (LOG.isDebugEnabled()) {
            LOG.debug("Updating TRACE toggle button appearance - TRACE enabled: " + aiEnabled);
        }
        
        if (aiEnabled) {
            // TRACE is enabled - green color
            button.setForeground(new JBColor(new Color(76, 175, 80), new Color(76, 175, 80))); // Material Design Green
            button.setToolTipText("Disable TRACE");
            LOG.debug("TRACE toggle button set to enabled state (green)");
        } else {
            // TRACE is disabled - gray color
            button.setForeground(new JBColor(new Color(158, 158, 158), new Color(158, 158, 158))); // Material Design Gray
            button.setToolTipText("Enable TRACE - Parse test failures locally for AI analysis");
            LOG.debug("TRACE toggle button set to disabled state (gray)");
        }
    }
    
    /**
     * Updates the analysis mode button text to show the current selection in bold.
     *
     * @param button The analysis mode button to update
     * @param currentAnalysisMode The current analysis mode
     */
    public static void updateAnalysisModeButtonText(JButton button, String currentAnalysisMode) {
        // Create HTML text with blue color for selected option - matching send button blue
        String htmlText;
        if (ANALYSIS_MODE_OVERVIEW.equals(currentAnalysisMode)) {
            htmlText = "<html><nobr><b style='font-size: 11px; color: #1976D2;'>Quick Overview</b> <span style='font-size: 10px; color: #888;'>|</span> <span style='font-size: 10px; color: #666;'>Full Analysis</span></nobr></html>";
        } else {
            htmlText = "<html><nobr><span style='font-size: 10px; color: #666;'>Quick Overview</span> <span style='font-size: 10px; color: #888;'>|</span> <b style='font-size: 11px; color: #1976D2;'>Full Analysis</b></nobr></html>";
        }
        
        button.setText(htmlText);
    }
    
    /**
     * Updates the analysis mode button text in the UI.
     * This method searches for the analysis mode button and updates its text.
     *
     * @param mainPanel The main panel to search in
     * @param currentAnalysisMode The current analysis mode
     */
    public static void updateAnalysisModeButtonTextInUI(JPanel mainPanel, String currentAnalysisMode) {
        // Find the analysis mode button in the component hierarchy
        if (mainPanel != null) {
            findAndUpdateAnalysisModeButton(mainPanel, currentAnalysisMode);
        }
    }
    
    /**
     * Recursively searches for the analysis mode button and updates its text.
     *
     * @param component The component to search in
     * @param currentAnalysisMode The current analysis mode
     */
    public static void findAndUpdateAnalysisModeButton(Container component, String currentAnalysisMode) {
        for (Component child : component.getComponents()) {
            if (child instanceof JButton) {
                JButton button = (JButton) child;
                // Check if this is the analysis mode button by looking at its tooltip
                if ("Click to switch between Quick Overview and Full Analysis modes".equals(button.getToolTipText())) {
                    updateAnalysisModeButtonText(button, currentAnalysisMode);
                    return;
                }
            }
            if (child instanceof Container) {
                findAndUpdateAnalysisModeButton((Container) child, currentAnalysisMode);
            }
        }
    }
    
    /**
     * Creates the settings panel using the dedicated SettingsPanel class.
     *
     * @param aiSettings The AI settings instance
     * @param backToChatListener The action listener for back to chat navigation
     * @return The configured settings panel
     */
    public static JPanel createSettingsPanel(AISettings aiSettings, ActionListener backToChatListener) {
        // Create and return the dedicated settings panel
        return new SettingsPanel(aiSettings, backToChatListener);
    }
    
    /**
     * Creates the custom header panel with logo and scenario information.
     *
     * @param aiToggleButton The AI toggle button to include
     * @param clearChatButton The clear chat button to include
     * @param settingsButton The settings button to include
     * @return The configured header panel
     */
    public static JPanel createCustomHeaderPanel(JButton aiToggleButton, JButton clearChatButton, JButton settingsButton) {
        Color darkBg = ThemeUtils.panelBackground();
        
        LOG.debug("Creating custom header panel with ultra-compact layout");
        
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.X_AXIS));
        header.setBackground(darkBg);
        
        // Minimal header padding
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeUtils.uiColor("Component.borderColor", new JBColor(new Color(68, 68, 68), new Color(68, 68, 68)))),
            BorderFactory.createEmptyBorder(6, 8, 6, 8) // Increased from (2, 4, 2, 4) for better visual balance
        ));
        
        // Create left side with BoxLayout for precise control
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.X_AXIS));
        leftPanel.setOpaque(false);
        
        LOG.debug("Adding AI toggle button to header");
        // Add AI toggle button
        aiToggleButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(aiToggleButton);
        
        // Add minimal spacing between button and text
        leftPanel.add(Box.createHorizontalStrut(2));
        
        LOG.debug("Creating TRACE title label");
        JLabel title = new JLabel("TRACE") {
            @Override
            public Dimension getMaximumSize() {
                // Permit horizontal shrink (important for BoxLayout)
                Dimension pref = getPreferredSize();
                return new Dimension(Integer.MAX_VALUE, pref.height);
            }
        };
        title.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        title.setForeground(ThemeUtils.textForeground());
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(title);

        // Ultra-narrow polish: hide the title if there isn't enough room for right controls and toggle
        // Listener is added after rightPanel is created to avoid forward reference issues.
        
        // Allow the left group to shrink as needed so the right controls remain visible
        int leftHeight = leftPanel.getPreferredSize().height;
        leftPanel.setMinimumSize(new Dimension(0, leftHeight));
        leftPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, leftHeight));

        if (LOG.isDebugEnabled()) {
            LOG.debug("Adding left panel to header with " + leftPanel.getComponentCount() + " components");
            LOG.debug("AI toggle button preferred size: " + aiToggleButton.getPreferredSize());
            LOG.debug("TRACE label preferred size: " + title.getPreferredSize());
            LOG.debug("Left panel preferred size: " + leftPanel.getPreferredSize());
        }
        header.add(leftPanel);
        header.add(Box.createHorizontalGlue());
        
        // Create right panel with BoxLayout for tight packing
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.X_AXIS));
        rightPanel.setOpaque(false);
        
        // Add clear chat button
        clearChatButton.setAlignmentX(Component.RIGHT_ALIGNMENT);
        rightPanel.add(clearChatButton);
        
        // Minimal spacing between buttons
        rightPanel.add(Box.createHorizontalStrut(2));
        
        // Add settings button
        settingsButton.setAlignmentX(Component.RIGHT_ALIGNMENT);
        rightPanel.add(settingsButton);
        
        // Right group keeps natural size at the far right
        int rightHeight = rightPanel.getPreferredSize().height;
        rightPanel.setMinimumSize(new Dimension(rightPanel.getPreferredSize().width, rightHeight));
        rightPanel.setMaximumSize(new Dimension(rightPanel.getPreferredSize().width, rightHeight));
        header.add(rightPanel);

        // Ultra-narrow polish: hide the title if there isn't enough room for right controls and toggle
        header.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                try {
                    int available = header.getWidth();
                    int rightWidth = rightPanel.getPreferredSize().width;
                    int toggleWidth = aiToggleButton.getPreferredSize().width + 2; // + spacing
                    int titleWidth = title.getPreferredSize().width;
                    // Keep a 6px safety margin
                    boolean show = available >= rightWidth + toggleWidth + titleWidth + 6;
                    if (title.isVisible() != show) {
                        title.setVisible(show);
                        header.revalidate();
                        header.repaint();
                    }
                } catch (Exception ignore) {
                }
            }
        });

        if (LOG.isDebugEnabled()) {
            LOG.debug("Header panel created with dimensions: " + header.getPreferredSize());
        }
        return header;
    }
}


