package com.trace.ai.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.trace.ai.configuration.AISettings;
import com.trace.common.constants.TriagePanelConstants;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * Custom Rule configuration panel for TRACE settings.
 * 
 * <p>This panel provides a simple interface for users to input custom rules
 * that will be appended to AI prompts for personalized analysis.</p>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
public class CustomRulePanel extends JBPanel<CustomRulePanel> {
    
    private static final Logger LOG = Logger.getInstance(CustomRulePanel.class);
    
    // Services
    private final AISettings aiSettings;
    
    // Custom Rule Configuration
    private final JTextArea customRuleTextArea;
    private final JBLabel characterCounterLabel;
    private final JButton saveCustomRuleButton;
    private final JButton clearCustomRuleButton;
    
    /**
     * Creates a new custom rule configuration panel.
     * 
     * @param aiSettings the AI settings instance
     */
    public CustomRulePanel(@NotNull AISettings aiSettings) {
        this.aiSettings = aiSettings;
        
        // Initialize custom rule components
        this.customRuleTextArea = new JTextArea();
        this.characterCounterLabel = new JBLabel("0/500");
        this.saveCustomRuleButton = new JButton("Save");
        this.clearCustomRuleButton = new JButton("Clear");
        
        // Initialize the panel
        initializePanel();
        setupEventHandlers();
        
        // Load current settings
        loadCurrentSettings();
        
        LOG.info("Custom rule panel created and initialized");
    }
    
    /**
     * Initializes the panel layout and styling.
     */
    private void initializePanel() {
        setLayout(new BorderLayout());
        setBorder(JBUI.Borders.compound(
            JBUI.Borders.customLine(UIUtil.getPanelBackground().darker(), 1),
            JBUI.Borders.empty(10)
        ));
        
        // Allow sections to expand to fit their content naturally
        int panelBaseFontSize = UIUtil.getLabelFont().getSize();
        
        // Let Swing calculate natural size instead of forcing fixed dimensions
        setMinimumSize(new Dimension(0, 0)); // Allow shrinking
        setPreferredSize(null); // Let Swing calculate natural size
        setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        
        // Header with smaller font for aggressive shrinking
        JBLabel headerLabel = new JBLabel("Custom Rule");
        int baseFontSize = UIUtil.getLabelFont().getSize();
        headerLabel.setFont(UIUtil.getLabelFont().deriveFont(Font.BOLD, baseFontSize + 1)); // Smaller font
        headerLabel.setBorder(JBUI.Borders.emptyBottom(5)); // Smaller border
        
        // Subheading with smaller font for aggressive shrinking
        JBLabel subheadingLabel = new JBLabel("Add a custom rule for your preferences");
        subheadingLabel.setFont(UIUtil.getLabelFont().deriveFont(Font.PLAIN, baseFontSize - 2)); // Smaller font
        subheadingLabel.setForeground(UIUtil.getLabelDisabledForeground());
        subheadingLabel.setBorder(JBUI.Borders.emptyBottom(5)); // Smaller border
        
        // Content panel
        JPanel contentPanel = new JBPanel<>(new BorderLayout());
        
        // Text area with scroll pane and responsive sizing
        customRuleTextArea.setLineWrap(true);
        customRuleTextArea.setWrapStyleWord(true);
        customRuleTextArea.setFont(UIUtil.getLabelFont());
        customRuleTextArea.setBorder(JBUI.Borders.compound(
            JBUI.Borders.customLine(UIUtil.getTextFieldBackground().darker(), 1),
            JBUI.Borders.empty(5)
        ));
        
        // Let Swing calculate natural size instead of forcing fixed dimensions
        customRuleTextArea.setPreferredSize(new Dimension(0, 0)); // Let Swing calculate
        customRuleTextArea.setMinimumSize(new Dimension(0, baseFontSize * 3)); // Minimum height only
        customRuleTextArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        
        JBScrollPane scrollPane = new JBScrollPane(customRuleTextArea);
        scrollPane.setPreferredSize(new Dimension(0, 0)); // Let Swing calculate
        scrollPane.setMinimumSize(new Dimension(0, 0));
        scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        
        // Character counter and buttons panel
        JPanel bottomPanel = new JBPanel<>(new BorderLayout());
        bottomPanel.setBorder(JBUI.Borders.emptyTop(10));
        
        // Character counter
        characterCounterLabel.setFont(characterCounterLabel.getFont().deriveFont(Font.PLAIN, 11f));
        characterCounterLabel.setForeground(UIUtil.getLabelDisabledForeground());
        
        // Buttons panel
        JPanel buttonPanel = new JBPanel<>(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(clearCustomRuleButton);
        buttonPanel.add(saveCustomRuleButton);
        
        bottomPanel.add(characterCounterLabel, BorderLayout.WEST);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);
        
        // Add components to content panel
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        contentPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        // Create a header panel to contain both header and subheading
        JPanel headerPanel = new JBPanel<>(new BorderLayout());
        headerPanel.add(headerLabel, BorderLayout.NORTH);
        headerPanel.add(subheadingLabel, BorderLayout.CENTER);
        
        // Add components to main panel
        add(headerPanel, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);
    }
    
    /**
     * Sets up event handlers for all components.
     */
    private void setupEventHandlers() {
        // Custom rule management
        saveCustomRuleButton.addActionListener(e -> saveCustomRule());
        clearCustomRuleButton.addActionListener(e -> clearCustomRule());
        customRuleTextArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateCharacterCounter(); }
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateCharacterCounter(); }
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateCharacterCounter(); }
        });
    }
    
    /**
     * Updates the character counter for the custom rule text area.
     */
    private void updateCharacterCounter() {
        String text = customRuleTextArea.getText();
        int length = text.length();
        int maxLength = 500;
        
        characterCounterLabel.setText(length + "/" + maxLength);
        
        // Update color based on length
        if (length > maxLength) {
            characterCounterLabel.setForeground(TriagePanelConstants.ERROR_FOREGROUND);
            saveCustomRuleButton.setEnabled(false);
        } else if (length > maxLength * 0.8) {
            characterCounterLabel.setForeground(TriagePanelConstants.WARNING_FOREGROUND);
            saveCustomRuleButton.setEnabled(true);
        } else {
            characterCounterLabel.setForeground(UIUtil.getLabelDisabledForeground());
            saveCustomRuleButton.setEnabled(true);
        }
    }
    
    /**
     * Saves the custom rule to settings.
     */
    private void saveCustomRule() {
        String customRule = customRuleTextArea.getText().trim();
        if (customRule.length() > 500) {
            showError("Custom rule cannot exceed 500 characters");
            return;
        }
        
        aiSettings.setCustomRule(customRule);
        showSuccess("Custom rule saved successfully");
        LOG.info("Custom rule saved: " + (customRule.isEmpty() ? "empty" : "length " + customRule.length()));
    }
    
    /**
     * Clears the custom rule text area.
     */
    private void clearCustomRule() {
        customRuleTextArea.setText("");
        updateCharacterCounter();
        LOG.info("Custom rule cleared");
    }
    
    /**
     * Loads current settings into the UI.
     */
    public void loadCurrentSettings() {
        LOG.info("Loading current custom rule settings into UI");
        
        // Load custom rule
        String customRule = aiSettings.getCustomRule();
        if (customRule != null) {
            customRuleTextArea.setText(customRule);
            updateCharacterCounter();
            LOG.debug("Loaded custom rule into UI");
        } else {
            customRuleTextArea.setText("");
            updateCharacterCounter();
            LOG.debug("No custom rule found in settings");
        }
        
        LOG.info("Finished loading custom rule settings into UI");
    }
    
    /**
     * Checks if the panel has been modified.
     * 
     * @return true if any settings have changed, false otherwise
     */
    public boolean isModified() {
        String currentCustomRule = customRuleTextArea.getText();
        String originalCustomRule = aiSettings.getCustomRule();
        
        boolean customRuleChanged = !java.util.Objects.equals(currentCustomRule, originalCustomRule);
        
        if (customRuleChanged) {
            LOG.debug("Custom rule panel modified");
        }
        
        return customRuleChanged;
    }
    
    /**
     * Applies the current settings.
     */
    public void apply() {
        LOG.info("Applying custom rule settings");
        
        String customRule = customRuleTextArea.getText().trim();
        String originalCustomRule = aiSettings.getCustomRule();
        
        // Save custom rule if changed
        if (!java.util.Objects.equals(customRule, originalCustomRule)) {
            aiSettings.setCustomRule(customRule);
            LOG.info("Custom rule applied: " + (customRule.isEmpty() ? "empty" : "length " + customRule.length()));
        }
        
        LOG.info("Custom rule settings applied");
    }
    
    /**
     * Resets the UI to the original settings.
     */
    public void reset() {
        LOG.info("Resetting custom rule configuration to current stored state");
        
        // Load current settings from storage
        loadCurrentSettings();
        
        LOG.info("Custom rule configuration reset completed");
    }
    
    /**
     * Disposes UI resources.
     */
    public void disposeUIResources() {
        // Clear sensitive data
        customRuleTextArea.setText("");
    }
    
    /**
     * Shows an error message to the user.
     * 
     * @param message the error message to display
     */
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * Shows a success message to the user.
     * 
     * @param message the success message to display
     */
    private void showSuccess(String message) {
        JOptionPane.showMessageDialog(this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }
} 