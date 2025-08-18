package com.trace.ai.ui;

import com.trace.ai.configuration.AISettings;
import com.intellij.openapi.diagnostic.Logger;
import com.trace.common.constants.TriagePanelConstants;
import com.trace.common.utils.ThemeUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Dedicated settings panel for the TRACE tool window.
 * 
 * This class encapsulates all settings UI logic and provides a clean,
 * extensible interface for managing different settings sections.
 * 
 * @author Alex Ibasitas
 */
public class SettingsPanel extends JPanel {
    
    private static final Logger LOG = Logger.getInstance(SettingsPanel.class);
    
    private final AISettings aiSettings;
    private final ActionListener backToChatListener;
    
    // Settings sections
    private PrivacyConsentPanel privacyPanel;
    private AIServiceConfigPanel servicePanel;
    private CustomRulePanel customRulePanel;
    
    // UI components
    private JScrollPane scrollPane;
    private JButton backToChatButton;
    
    /**
     * Creates a new settings panel with the specified AI settings and navigation listener.
     * 
     * @param aiSettings The AI settings service for data persistence
     * @param backToChatListener Action listener for the "Back to Chat" button
     */
    public SettingsPanel(AISettings aiSettings, ActionListener backToChatListener) {
        this.aiSettings = aiSettings;
        this.backToChatListener = backToChatListener;
        
        initializePanel();
        setupLayout();
        createSettingsSections();
        createNavigationButton();
        assemblePanel();
    }
    
    /**
     * Initializes the main panel with proper styling and configuration.
     */
    private void initializePanel() {
        setLayout(new BorderLayout());
        setBackground(ThemeUtils.panelBackground());
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
    }
    
    /**
     * Sets up the main layout structure for the settings panel.
     */
    private void setupLayout() {
        // Create scrollable settings content
        JPanel settingsContent = new JPanel(new BorderLayout());
        settingsContent.setBackground(ThemeUtils.panelBackground());
        settingsContent.setOpaque(true);
        
        // Create container for settings sections
        JPanel sectionsContainer = createSectionsContainer();
        
        // Create scroll pane for better UX
        scrollPane = createScrollPane(sectionsContainer);
        settingsContent.add(scrollPane, BorderLayout.CENTER);
        
        add(settingsContent, BorderLayout.CENTER);
    }
    
    /**
     * Creates the container for all settings sections with proper spacing.
     * 
     * @return The configured sections container
     */
    private JPanel createSectionsContainer() {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBackground(ThemeUtils.panelBackground());
        container.setOpaque(true);
        
        // Set minimum width to enable soft wrapping before horizontal scrollbar appears
        container.setMinimumSize(new Dimension(TriagePanelConstants.MIN_SETTINGS_WIDTH_BEFORE_SCROLL, 0));
        
        return container;
    }
    
    /**
     * Creates a scroll pane for the settings sections.
     * 
     * @param view The component to display in the scroll pane
     * @return The configured scroll pane
     */
    private JScrollPane createScrollPane(JComponent view) {
        JScrollPane scrollPane = new JScrollPane(view);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        Color bg = ThemeUtils.panelBackground();
        scrollPane.setBackground(bg);
        scrollPane.getViewport().setBackground(bg);
        
        // Configure for better horizontal responsiveness with soft wrapping
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        
        // Set minimum width to enable soft wrapping before horizontal scrollbar appears
        scrollPane.setMinimumSize(new Dimension(TriagePanelConstants.MIN_SETTINGS_WIDTH_BEFORE_SCROLL, 200));
        
        return scrollPane;
    }
    
    /**
     * Creates and configures all settings sections.
     */
    private void createSettingsSections() {
        JPanel sectionsContainer = (JPanel) scrollPane.getViewport().getView();
        
        // Create settings sections
        privacyPanel = new PrivacyConsentPanel(aiSettings);
        servicePanel = new AIServiceConfigPanel(aiSettings);
        customRulePanel = new CustomRulePanel(aiSettings);
        
        // Add sections with proper spacing
        sectionsContainer.add(privacyPanel);
        sectionsContainer.add(Box.createVerticalStrut(20));
        sectionsContainer.add(servicePanel);
        sectionsContainer.add(Box.createVerticalStrut(20));
        sectionsContainer.add(customRulePanel);
        
        // Add extensibility space for future sections
        sectionsContainer.add(Box.createVerticalStrut(10));
    }
    
    /**
     * Creates the navigation button for returning to chat.
     */
    private void createNavigationButton() {
        backToChatButton = new JButton("← Back to Chat");
        backToChatButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        backToChatButton.setForeground(ThemeUtils.textForeground());
        backToChatButton.setBackground(ThemeUtils.panelBackground());
        backToChatButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeUtils.uiColor("Component.borderColor", new Color(80, 80, 80)), 1, true),
            BorderFactory.createEmptyBorder(8, 16, 8, 16)
        ));
        backToChatButton.setFocusPainted(false);
        backToChatButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backToChatButton.addActionListener(backToChatListener);
    }
    
    /**
     * Assembles the final panel with navigation button.
     */
    private void assemblePanel() {
        // Button container
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 10));
        buttonPanel.setBackground(ThemeUtils.panelBackground());
        buttonPanel.setOpaque(true);
        buttonPanel.add(backToChatButton);
        
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    /**
     * Gets the dark background color for consistent theming.
     * 
     * @return The dark background color
     */
    private Color getDarkBackgroundColor() { return ThemeUtils.panelBackground(); }
    
    /**
     * Adds a new settings section to the panel.
     * 
     * @param section The settings section component to add
     * @param spacing The vertical spacing to add before this section
     */
    public void addSettingsSection(JComponent section, int spacing) {
        JPanel sectionsContainer = (JPanel) scrollPane.getViewport().getView();
        
        if (spacing > 0) {
            sectionsContainer.add(Box.createVerticalStrut(spacing));
        }
        sectionsContainer.add(section);
        
        // Revalidate to update the layout
        sectionsContainer.revalidate();
        sectionsContainer.repaint();
    }
    
    /**
     * Refreshes all settings sections to reflect current state.
     */
    public void refreshSettings() {
        if (privacyPanel != null) {
            privacyPanel.loadCurrentSettings();
        }
        if (servicePanel != null) {
            // Settings are now loaded asynchronously in the constructor
            // No need to call loadCurrentSettings() explicitly
        }
        
        revalidate();
        repaint();
    }
    
    /**
     * Gets the privacy consent panel for external access.
     * 
     * @return The privacy consent panel
     */
    public PrivacyConsentPanel getPrivacyPanel() {
        return privacyPanel;
    }
    
    /**
     * Gets the AI service configuration panel for external access.
     * 
     * @return The AI service configuration panel
     */
    public AIServiceConfigPanel getServicePanel() {
        return servicePanel;
    }
} 