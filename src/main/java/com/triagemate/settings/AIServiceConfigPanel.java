package com.triagemate.settings;

import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.triagemate.ui.TriagePanelConstants;

import javax.swing.*;
import java.awt.*;


/**
 * AI Service Configuration panel for TriageMate AI settings.
 * 
 * <p>This panel provides configuration options for AI service selection
 * and API key management. It will be fully implemented in Part 1.4.3.</p>
 * 
 * <p>This component is designed for extensibility - additional AI services
 * can be easily added in the future.</p>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
public class AIServiceConfigPanel extends JBPanel<AIServiceConfigPanel> {
    
    // Settings service
    private final AISettings aiSettings;
    
    // State tracking for modification detection
    private String originalPreferredService;
    
    /**
     * Constructor for AIServiceConfigPanel.
     * 
     * @param aiSettings the AISettings service for data persistence
     */
    public AIServiceConfigPanel(AISettings aiSettings) {
        this.aiSettings = aiSettings;
        this.originalPreferredService = aiSettings.getPreferredAIService().getId();
        
        initializePanel();
        loadCurrentSettings();
    }
    
    /**
     * Initializes the panel layout and styling.
     */
    private void initializePanel() {
        setLayout(new BorderLayout());
        setBorder(JBUI.Borders.compound(
            JBUI.Borders.customLine(UIUtil.getPanelBackground().darker(), 1),
            JBUI.Borders.empty(15)
        ));
        
        // Make the panel itself responsive using proper Swing sizing
        setMinimumSize(new Dimension(300, 0));
        setPreferredSize(new Dimension(400, 80));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        
        // Create header
        JBLabel headerLabel = new JBLabel("🤖 AI Service Configuration");
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 14f));
        headerLabel.setBorder(JBUI.Borders.emptyBottom(10));
        
        // Create responsive placeholder content using JTextArea for proper wrapping
        JTextArea placeholderTextArea = new JTextArea("AI Service Configuration - Coming in Part 1.4.3");
        placeholderTextArea.setForeground(UIUtil.getLabelDisabledForeground());
        placeholderTextArea.setBorder(JBUI.Borders.empty(20));
        placeholderTextArea.setLineWrap(true);
        placeholderTextArea.setWrapStyleWord(true);
        placeholderTextArea.setEditable(false);
        placeholderTextArea.setOpaque(false);
        placeholderTextArea.setFont(TriagePanelConstants.SETTINGS_PLACEHOLDER_FONT);
        
        // Set minimum width to enable soft wrapping before horizontal scrollbar appears
        placeholderTextArea.setMinimumSize(new Dimension(TriagePanelConstants.MIN_SETTINGS_WIDTH_BEFORE_SCROLL, 30));
        placeholderTextArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        
        // Configure text area for proper responsive behavior
        placeholderTextArea.setLineWrap(true);
        placeholderTextArea.setWrapStyleWord(true);
        placeholderTextArea.setEditable(false);
        placeholderTextArea.setOpaque(false);
        placeholderTextArea.setBackground(UIUtil.getPanelBackground());
        placeholderTextArea.setFont(UIUtil.getLabelFont());
        
        // Set proper sizing according to JetBrains guidelines
        // Text area: min 270px width, min 40px height (2 lines)
        placeholderTextArea.setMinimumSize(new Dimension(270, 40));
        placeholderTextArea.setPreferredSize(new Dimension(300, 45));
        placeholderTextArea.setMaximumSize(new Dimension(600, Integer.MAX_VALUE));
        
        // Assemble the panel
        add(headerLabel, BorderLayout.NORTH);
        add(placeholderTextArea, BorderLayout.CENTER);
    }
    
    /**
     * Loads current settings from the AISettings service.
     */
    public void loadCurrentSettings() {
        originalPreferredService = aiSettings.getPreferredAIService().getId();
    }
    
    /**
     * Applies current UI settings to the AISettings service.
     */
    public void apply() {
        // Will be implemented in Part 1.4.3
    }
    
    /**
     * Resets the panel to the current saved settings.
     */
    public void reset() {
        loadCurrentSettings();
    }
    
    /**
     * Disposes of UI resources.
     */
    public void disposeUIResources() {
        // No specific cleanup needed for this panel
    }
    
    /**
     * Gets the selected AI service.
     * 
     * @return the selected AI service type
     */
    public String getSelectedService() {
        return aiSettings.getPreferredAIService().getId();
    }
    
    /**
     * Checks if any settings have been modified.
     * 
     * @return true if settings have been modified, false otherwise
     */
    public boolean isModified() {
        return !aiSettings.getPreferredAIService().getId().equals(originalPreferredService);
    }
} 