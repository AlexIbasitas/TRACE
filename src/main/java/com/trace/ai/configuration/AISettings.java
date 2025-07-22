package com.trace.ai.configuration;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;

/**
 * Persistent settings service for TRACE AI features.
 * 
 * <p>This service manages user preferences, consent, and configuration for AI analysis
 * features. It follows IntelliJ Platform best practices for persistent state management
 * using a separate State class for data storage.</p>
 * 
 * <p>The service provides:</p>
 * <ul>
 *   <li>User consent management for AI features</li>
 *   <li>AI service selection and configuration</li>
 *   <li>Chat history persistence settings</li>
 *   <li>Auto-analysis preferences</li>
 * </ul>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
@State(
    name = "com.trace.ai.configuration.ai-settings",
    storages = @Storage("trace-ai-settings.xml")
)
public final class AISettings implements PersistentStateComponent<AISettings.State> {
    
    private static final Logger LOG = Logger.getInstance(AISettings.class);
    
    /**
     * State class containing all persistent settings data.
     * 
     * <p>This class holds all the configuration data that needs to be persisted
     * between IDE sessions. All fields are public to allow automatic serialization
     * by the IntelliJ Platform.</p>
     */
    public static class State {
        // User consent and preferences
        public boolean aiEnabled = false;
        public boolean userConsentGiven = false;
        public String consentDate; // Store as string to avoid LocalDateTime serialization issues
        
        // AI service configuration
        public String preferredAIService = AIServiceType.OPENAI.getId(); // Store as string for persistence
        public boolean autoAnalyzeEnabled = true;
        public boolean showConfidenceScores = true;
        
        // Chat settings
        public boolean persistChatHistory = true;
        public int maxChatHistorySize = 50;
        
        // Advanced features (optional)
        public boolean customRulesEnabled = false;
        
        /**
         * Default constructor for state initialization.
         * All fields have default values to ensure proper initialization.
         */
        public State() {
            // Default values are set in field declarations
        }
    }
    
    private State myState = new State();
    
    /**
     * Private constructor to enforce singleton pattern.
     */
    private AISettings() {
        LOG.debug("AISettings service initialized");
    }
    
    /**
     * Gets the singleton instance of AISettings.
     * 
     * @return the AISettings instance
     */
    public static AISettings getInstance() {
        return ApplicationManager.getApplication().getService(AISettings.class);
    }
    
    @Override
    public @Nullable State getState() {
        LOG.debug("Getting AISettings state");
        return myState;
    }
    
    @Override
    public void loadState(@NotNull State state) {
        LOG.debug("Loading AISettings state");
        myState = state;
    }
    
    // ============================================================================
    // USER CONSENT MANAGEMENT
    // ============================================================================
    
    /**
     * Checks if AI features are enabled.
     * 
     * @return true if AI features are enabled, false otherwise
     */
    public boolean isAIEnabled() {
        return myState.aiEnabled;
    }
    
    /**
     * Sets whether AI features are enabled.
     * 
     * @param enabled true to enable AI features, false to disable
     */
    public void setAIEnabled(boolean enabled) {
        LOG.info("Setting AI enabled: " + enabled);
        myState.aiEnabled = enabled;
    }
    
    /**
     * Checks if the user has given consent for AI features.
     * 
     * @return true if user has given consent, false otherwise
     */
    public boolean hasUserConsent() {
        return myState.userConsentGiven;
    }
    
    /**
     * Sets the user consent status.
     * 
     * @param consentGiven true if user has given consent, false otherwise
     */
    public void setUserConsentGiven(boolean consentGiven) {
        LOG.info("Setting user consent: " + consentGiven);
        myState.userConsentGiven = consentGiven;
        
        // Record consent timestamp for audit trail, clear when consent revoked
        if (consentGiven) {
            myState.consentDate = LocalDateTime.now().toString();
        } else {
            myState.consentDate = null; // Clear date when consent revoked
        }
    }
    
    /**
     * Gets the date when user consent was given.
     * 
     * @return the consent date, or null if no consent given
     */
    public @Nullable LocalDateTime getConsentDate() {
        if (myState.consentDate == null || myState.consentDate.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(myState.consentDate);
        } catch (Exception e) {
            LOG.warn("Failed to parse consent date: " + myState.consentDate, e);
            return null;
        }
    }
    
    // ============================================================================
    // AI SERVICE CONFIGURATION
    // ============================================================================
    
    /**
     * Gets the preferred AI service type.
     * 
     * @return the preferred AI service type, or default if invalid
     */
    public AIServiceType getPreferredAIService() {
        AIServiceType serviceType = AIServiceType.fromId(myState.preferredAIService);
        return serviceType != null ? serviceType : AIServiceType.getDefault();
    }
    
    /**
     * Sets the preferred AI service type.
     * 
     * @param serviceType the AI service type to use
     */
    public void setPreferredAIService(AIServiceType serviceType) {
        String serviceId = serviceType != null ? serviceType.getId() : AIServiceType.getDefault().getId();
        LOG.info("Setting preferred AI service: " + serviceId);
        myState.preferredAIService = serviceId;
    }
    
    /**
     * Sets the preferred AI service by string ID (for backward compatibility).
     * 
     * @param serviceId the AI service ID string
     */
    public void setPreferredAIService(String serviceId) {
        LOG.info("Setting preferred AI service by ID: " + serviceId);
        myState.preferredAIService = serviceId;
    }
    
    /**
     * Checks if auto-analysis is enabled.
     * 
     * @return true if auto-analysis is enabled, false otherwise
     */
    public boolean isAutoAnalyzeEnabled() {
        return myState.autoAnalyzeEnabled;
    }
    
    /**
     * Sets whether auto-analysis is enabled.
     * 
     * @param enabled true to enable auto-analysis, false to disable
     */
    public void setAutoAnalyzeEnabled(boolean enabled) {
        LOG.info("Setting auto-analyze enabled: " + enabled);
        myState.autoAnalyzeEnabled = enabled;
    }
    
    /**
     * Checks if confidence scores should be shown.
     * 
     * @return true if confidence scores should be shown, false otherwise
     */
    public boolean isShowConfidenceScores() {
        return myState.showConfidenceScores;
    }
    
    /**
     * Sets whether confidence scores should be shown.
     * 
     * @param show true to show confidence scores, false to hide
     */
    public void setShowConfidenceScores(boolean show) {
        LOG.info("Setting show confidence scores: " + show);
        myState.showConfidenceScores = show;
    }
    
    // ============================================================================
    // CHAT SETTINGS
    // ============================================================================
    
    /**
     * Checks if chat history should be persisted.
     * 
     * @return true if chat history should be persisted, false otherwise
     */
    public boolean isPersistChatHistory() {
        return myState.persistChatHistory;
    }
    
    /**
     * Sets whether chat history should be persisted.
     * 
     * @param persist true to persist chat history, false to not persist
     */
    public void setPersistChatHistory(boolean persist) {
        LOG.info("Setting persist chat history: " + persist);
        myState.persistChatHistory = persist;
    }
    
    /**
     * Gets the maximum chat history size.
     * 
     * @return the maximum number of messages to keep in history
     */
    public int getMaxChatHistorySize() {
        return myState.maxChatHistorySize;
    }
    
    /**
     * Sets the maximum chat history size.
     * 
     * @param maxSize the maximum number of messages to keep in history
     */
    public void setMaxChatHistorySize(int maxSize) {
        LOG.info("Setting max chat history size: " + maxSize);
        // Clamp between 10-500: 10 ensures minimal context, 500 prevents memory bloat
        myState.maxChatHistorySize = Math.max(10, Math.min(500, maxSize));
    }
    
    // ============================================================================
    // ADVANCED FEATURES
    // ============================================================================
    
    /**
     * Checks if custom rules are enabled.
     * 
     * @return true if custom rules are enabled, false otherwise
     */
    public boolean isCustomRulesEnabled() {
        return myState.customRulesEnabled;
    }
    
    /**
     * Sets whether custom rules are enabled.
     * 
     * @param enabled true to enable custom rules, false to disable
     */
    public void setCustomRulesEnabled(boolean enabled) {
        LOG.info("Setting custom rules enabled: " + enabled);
        myState.customRulesEnabled = enabled;
    }
    
    // ============================================================================
    // VALIDATION AND UTILITY METHODS
    // ============================================================================
    
    /**
     * Checks if AI analysis is properly configured and ready to use.
     * 
     * <p>This method checks that:</p>
     * <ul>
     *   <li>AI features are enabled</li>
     *   <li>User has given consent</li>
     *   <li>A preferred AI service is set</li>
     * </ul>
     * 
     * @return true if AI analysis is configured and ready, false otherwise
     */
    public boolean isConfigured() {
        // Configuration requires: AI enabled + user consent + service selected
        // This ensures compliance and prevents incomplete setups
        return myState.aiEnabled && 
               myState.userConsentGiven && 
               myState.preferredAIService != null && 
               !myState.preferredAIService.trim().isEmpty();
    }
    
    /**
     * Resets all settings to their default values.
     * 
     * <p>This method is useful for testing or when users want to start fresh.</p>
     */
    public void resetToDefaults() {
        LOG.info("Resetting AISettings to defaults");
        myState = new State();
    }
    
    /**
     * Gets a summary of the current configuration status.
     * 
     * @return a string describing the current configuration status
     */
    public String getConfigurationStatus() {
        if (!myState.aiEnabled) {
            return "AI features are disabled";
        }
        
        if (!myState.userConsentGiven) {
            return "User consent required";
        }
        
        if (myState.preferredAIService == null || myState.preferredAIService.trim().isEmpty()) {
            return "No AI service configured";
        }
        
        return "Configured for " + myState.preferredAIService + 
               (myState.autoAnalyzeEnabled ? " with auto-analysis" : " with manual analysis");
    }
} 