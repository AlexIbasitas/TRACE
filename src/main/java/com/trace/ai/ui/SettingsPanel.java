package com.trace.ai.ui;

import com.trace.ai.configuration.AISettings;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;
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
        
        // Use flexible sizing that adapts to parent container
        setMinimumSize(new Dimension(0, 0)); // Allow shrinking
        setPreferredSize(null); // Let Swing calculate natural size
        setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        
        // Use proper padding that doesn't compress content
        int baseFontSize = UIUtil.getLabelFont().getSize();
        int dynamicPadding = Math.max(12, baseFontSize);
        setBorder(BorderFactory.createEmptyBorder(dynamicPadding, dynamicPadding, dynamicPadding, dynamicPadding));
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
        
        // Use flexible sizing that allows horizontal shrinking
        container.setMinimumSize(new Dimension(0, 0)); // Allow shrinking
        container.setPreferredSize(null); // Let Swing calculate natural size
        container.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        
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
        
        // Ensure proper scroll increment for smooth scrolling
        int baseFontSize = UIUtil.getLabelFont().getSize();
        int scrollIncrement = Math.max(16, baseFontSize);
        scrollPane.getVerticalScrollBar().setUnitIncrement(scrollIncrement);
        
        Color bg = ThemeUtils.panelBackground();
        scrollPane.setBackground(bg);
        scrollPane.getViewport().setBackground(bg);
        
        // Configure for better responsiveness - no horizontal scrollbar, let content wrap
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        
        // Use flexible sizing that allows horizontal shrinking
        scrollPane.setMinimumSize(new Dimension(0, 0)); // Allow shrinking
        scrollPane.setPreferredSize(null); // Let Swing calculate natural size
        scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        
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
        
        // CRITICAL: Ensure sections can shrink for proper scrolling
        privacyPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        servicePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        customRulePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        
        // Use proper spacing between sections that doesn't compress content
        int baseFontSize = UIUtil.getLabelFont().getSize();
        int dynamicSpacing = Math.max(16, baseFontSize);
        
        // Add sections with proper spacing
        sectionsContainer.add(privacyPanel);
        sectionsContainer.add(Box.createVerticalStrut(dynamicSpacing));
        sectionsContainer.add(servicePanel);
        sectionsContainer.add(Box.createVerticalStrut(dynamicSpacing));
        sectionsContainer.add(customRulePanel);
        
        // Add extensibility space for future sections
        sectionsContainer.add(Box.createVerticalStrut(dynamicSpacing / 2));
        
        // CRITICAL: Force layout update to ensure proper scrolling
        sectionsContainer.revalidate();
        sectionsContainer.repaint();
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
            BorderFactory.createLineBorder(ThemeUtils.uiColor("Component.borderColor", new JBColor(new Color(80, 80, 80), new Color(80, 80, 80))), 1, true),
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