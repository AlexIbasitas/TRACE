package com.trace.ai.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.trace.ai.configuration.AISettings;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Configurable component for TRACE AI settings.
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
    
    private static final Logger LOG = Logger.getInstance(AISettingsConfigurable.class);
    
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
    
    /**
     * Checks if any settings have been modified.
     * 
     * @return true if any settings have changed, false otherwise
     */
    private boolean hasSettingsChanged() {
        if (privacyConsentPanel == null || aiServiceConfigPanel == null) {
            return false;
        }
        
        // Check privacy and consent changes
        boolean aiEnabledChanged = privacyConsentPanel.isAIEnabled() != originalAIEnabled;
        boolean userConsentChanged = privacyConsentPanel.hasUserConsent() != originalUserConsent;
        boolean privacyPanelModified = privacyConsentPanel.isModified();
        
        // Check AI service configuration changes
        boolean aiServiceConfigModified = aiServiceConfigPanel.isModified();
        
        boolean hasChanges = aiEnabledChanged || userConsentChanged || privacyPanelModified || aiServiceConfigModified;
        
        if (hasChanges) {
            LOG.debug("Settings modified - AI Enabled: " + aiEnabledChanged + 
                     ", User Consent: " + userConsentChanged + 
                     ", Privacy Panel: " + privacyPanelModified + 
                     ", AI Service Config: " + aiServiceConfigModified);
        }
        
        return hasChanges;
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
            
            // Create AI service configuration panel first
            aiServiceConfigPanel = new AIServiceConfigPanel(aiSettings);
            
            // Load current settings immediately when panel is created
            // Settings are now loaded asynchronously in the constructor
            // No need to call loadCurrentSettings() explicitly
            
            // Create privacy consent panel
            privacyConsentPanel = new PrivacyConsentPanel(aiSettings, () -> {
                // No longer need to update AI service config panel enabled state
                // The new panel handles this internally
            });
            
            // Create a content panel to hold all components
            JPanel contentPanel = new JBPanel<>(new BorderLayout());
            contentPanel.setBorder(JBUI.Borders.empty(5));
            
            // Add panels to content with proper spacing
            contentPanel.add(privacyConsentPanel, BorderLayout.NORTH);
            contentPanel.add(Box.createVerticalStrut(15), BorderLayout.CENTER);
            contentPanel.add(aiServiceConfigPanel, BorderLayout.SOUTH);
            
            // Create scrollable pane
            JBScrollPane scrollPane = new JBScrollPane(contentPanel);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setBorder(null);
            
            // Set preferred size to ensure scrollability
            contentPanel.setPreferredSize(new Dimension(600, 800));
            
            mainPanel.add(scrollPane, BorderLayout.CENTER);
            
            // Initialize state for modification detection
            initializeState();
            
            LOG.info("AI settings configurable component created and initialized");
        }
        
        return mainPanel;
    }
    
    @Override
    public boolean isModified() {
        return hasSettingsChanged();
    }
    
    @Override
    public void apply() throws ConfigurationException {
        if (privacyConsentPanel == null || aiServiceConfigPanel == null) {
            LOG.warn("Cannot apply settings - panels not initialized");
            return;
        }
        
        LOG.info("Applying AI settings configuration");
        
        try {
            // Apply privacy and consent settings
            privacyConsentPanel.apply();
            LOG.debug("Privacy and consent settings applied successfully");
            
            // Apply AI service configuration
            aiServiceConfigPanel.apply();
            LOG.debug("AI service configuration applied successfully");
            
            // Update original state for modification detection
            initializeState();
            LOG.info("All AI settings applied successfully");
            
        } catch (Exception e) {
            LOG.error("Failed to apply AI settings", e);
            throw new ConfigurationException("Failed to save settings: " + e.getMessage(), "Settings Error");
        }
    }
    
    @Override
    public void reset() {
        if (privacyConsentPanel == null || aiServiceConfigPanel == null) {
            LOG.warn("Cannot reset settings - panels not initialized");
            return;
        }
        
        LOG.info("Resetting AI settings configuration");
        
        try {
            // Reset privacy and consent panel
            privacyConsentPanel.reset();
            LOG.debug("Privacy and consent panel reset successfully");
            
            // Reset AI service configuration panel
            aiServiceConfigPanel.reset();
            LOG.debug("AI service configuration panel reset successfully");
            
            // Update original state
            initializeState();
            LOG.info("All AI settings reset successfully");
            
        } catch (Exception e) {
            LOG.error("Failed to reset AI settings", e);
        }
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