package com.trace.ai.configuration;

/**
 * Enumeration of supported cloud AI services for TRACE.
 * 
 * <p>This enum defines the different cloud AI services that can be used for test failure analysis.
 * Each service has specific API endpoints and configuration requirements.</p>
 * 
 * <p>When adding new services, ensure they implement the AIService interface and
 * provide proper error handling and rate limiting.</p>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
public enum AIServiceType {
    
    /**
     * OpenAI GPT models (GPT-3.5, GPT-4).
     * 
     * <p>Characteristics:</p>
     * <ul>
     *   <li>Fast response times</li>
     *   <li>Good code analysis capabilities</li>
     *   <li>Requires OpenAI API key</li>
     *   <li>Rate limited per minute</li>
     * </ul>
     * 
     * <p>API Endpoint: https://api.openai.com/v1/chat/completions</p>
     */
    OPENAI("OpenAI"),
    
    /**
     * Google Gemini models (Gemini Pro, Gemini Flash).
     * 
     * <p>Characteristics:</p>
     * <ul>
     *   <li>Excellent reasoning capabilities</li>
     *   <li>Strong code analysis and debugging</li>
     *   <li>Requires Google API key</li>
     *   <li>Competitive pricing</li>
     * </ul>
     * 
     * <p>API Endpoint: https://generativelanguage.googleapis.com/v1beta/models</p>
     */
    GEMINI("Google Gemini");
    
    private final String displayName;
    
    /**
     * Constructor for AI service type.
     * 
     * @param displayName the human-readable name for UI display
     */
    AIServiceType(String displayName) {
        this.displayName = displayName;
    }
    
    /**
     * Gets the unique identifier for this service (same as enum name).
     * 
     * @return the service ID
     */
    public String getId() {
        return this.name().toLowerCase();
    }
    
    /**
     * Gets the human-readable display name for UI.
     * 
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Gets the AI service type from its ID.
     * 
     * @param id the service ID
     * @return the corresponding AIServiceType, or null if not found
     */
    public static AIServiceType fromId(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        
        try {
            return valueOf(id.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    /**
     * Gets the default AI service type.
     * 
     * @return the default service type (OPENAI)
     */
    public static AIServiceType getDefault() {
        return OPENAI;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
} 