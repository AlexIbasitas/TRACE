package com.triagemate.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Configurable component for TriageMate AI settings.
 * 
 * <p>This class provides the settings UI for configuring AI analysis features,
 * following IntelliJ Platform best practices for settings configuration.
 * It integrates with the AISettings service for data persistence and provides
 * a clean, extensible interface for user configuration.</p>
 * 
 * <p>The settings are organized into logical sections:</p>
 * <ul>
 *   <li><strong>Privacy & Consent:</strong> User consent management and data usage explanation</li>
 *   <li><strong>AI Service Configuration:</strong> Service selection and API key management</li>
 * </ul>
 * 
 * <p>This component is designed for extensibility - new settings sections can be
 * easily added without modifying existing code.</p>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
public class AISettingsConfigurable implements Configurable {
    
    // UI Components
    private JPanel mainPanel;
    private PrivacyConsentPanel privacyConsentPanel;
    private AIServiceConfigPanel aiServiceConfigPanel;
    
    // Settings service
    private final AISettings aiSettings;
    
    // State tracking for modification detection
    private boolean originalAIEnabled;
    private boolean originalUserConsent;
    private String originalPreferredService;
    
    /**
     * Constructor for AISettingsConfigurable.
     * Initializes the settings service and prepares the UI components.
     */
    public AISettingsConfigurable() {
        this.aiSettings = AISettings.getInstance();
        initializeState();
    }
    
    /**
     * Initializes the original state for modification detection.
     */
    private void initializeState() {
        originalAIEnabled = aiSettings.isAIEnabled();
        originalUserConsent = aiSettings.hasUserConsent();
        originalPreferredService = aiSettings.getPreferredAIService().getId();
    }
    
    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "AI Analysis";
    }
    
    @Nullable
    @Override
    public JComponent createComponent() {
        if (mainPanel == null) {
            mainPanel = new JBPanel<>(new BorderLayout());
            mainPanel.setBorder(JBUI.Borders.empty(10));
            
            // Create and add privacy consent panel
            privacyConsentPanel = new PrivacyConsentPanel(aiSettings);
            mainPanel.add(privacyConsentPanel, BorderLayout.NORTH);
            
            // Create and add AI service configuration panel
            aiServiceConfigPanel = new AIServiceConfigPanel(aiSettings);
            mainPanel.add(aiServiceConfigPanel, BorderLayout.CENTER);
            
            // Add spacing between panels
            mainPanel.add(Box.createVerticalStrut(20), BorderLayout.CENTER);
        }
        
        return mainPanel;
    }
    
    @Override
    public boolean isModified() {
        if (privacyConsentPanel == null || aiServiceConfigPanel == null) {
            return false;
        }
        
        // Check if any settings have changed
        boolean aiEnabledChanged = privacyConsentPanel.isAIEnabled() != originalAIEnabled;
        boolean userConsentChanged = privacyConsentPanel.hasUserConsent() != originalUserConsent;
        boolean serviceChanged = !aiServiceConfigPanel.getSelectedService().equals(originalPreferredService);
        
        return aiEnabledChanged || userConsentChanged || serviceChanged || 
               privacyConsentPanel.isModified() || aiServiceConfigPanel.isModified();
    }
    
    @Override
    public void apply() throws ConfigurationException {
        if (privacyConsentPanel == null || aiServiceConfigPanel == null) {
            return;
        }
        
        try {
            // Apply privacy and consent settings
            privacyConsentPanel.apply();
            
            // Apply AI service configuration
            aiServiceConfigPanel.apply();
            
            // Update original state for modification detection
            initializeState();
            
        } catch (Exception e) {
            throw new ConfigurationException("Failed to save settings: " + e.getMessage(), "Settings Error");
        }
    }
    
    @Override
    public void reset() {
        if (privacyConsentPanel == null || aiServiceConfigPanel == null) {
            return;
        }
        
        // Reset privacy and consent panel
        privacyConsentPanel.reset();
        
        // Reset AI service configuration panel
        aiServiceConfigPanel.reset();
        
        // Update original state
        initializeState();
    }
    
    @Override
    public void disposeUIResources() {
        if (privacyConsentPanel != null) {
            privacyConsentPanel.disposeUIResources();
        }
        if (aiServiceConfigPanel != null) {
            aiServiceConfigPanel.disposeUIResources();
        }
        mainPanel = null;
    }
} 