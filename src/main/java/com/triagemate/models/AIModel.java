package com.triagemate.models;

import com.intellij.openapi.diagnostic.Logger;
import com.triagemate.settings.AIServiceType;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents an AI model configuration with hard-coded optimal settings.
 * 
 * <p>This simplified class provides pre-configured models with optimal settings
 * for different use cases. Users only need to choose which models to enable.</p>
 * 
 * <p>Examples:</p>
 * <ul>
 *   <li>GPT-4o for complex analysis</li>
 *   <li>GPT-3.5-turbo for faster responses</li>
 *   <li>Gemini Pro for reasoning tasks</li>
 *   <li>Gemini Flash for quick responses</li>
 * </ul>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
public class AIModel {
    
    private static final Logger LOG = Logger.getInstance(AIModel.class);
    
    // Unique identifier for this model configuration
    private final String id;
    
    // User-friendly name for this model
    private String name;
    
    // The AI service this model belongs to
    private final AIServiceType serviceType;
    
    // The specific model identifier (e.g., "gpt-4", "gemini-pro")
    private final String modelId;
    
    // Whether this model is enabled for use
    private boolean enabled;
    
    // User notes about this model
    private String notes;
    
    // Timestamp when this model was created
    private final long createdAt;
    
    // Timestamp when this model was last modified
    private long lastModified;
    
    /**
     * Constructor for creating a new AI model configuration.
     * 
     * @param name user-friendly name for this model
     * @param serviceType the AI service this model belongs to
     * @param modelId the specific model identifier
     */
    public AIModel(@NotNull String name, @NotNull AIServiceType serviceType, @NotNull String modelId) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.serviceType = serviceType;
        this.modelId = modelId;
        this.enabled = true;
        this.notes = "";
        this.createdAt = System.currentTimeMillis();
        this.lastModified = this.createdAt;
    }
    
    /**
     * Constructor for creating an AI model from existing data (e.g., from database).
     * 
     * @param id the unique identifier
     * @param name user-friendly name
     * @param serviceType the AI service
     * @param modelId the model identifier
     * @param enabled whether enabled
     * @param notes user notes
     * @param createdAt creation timestamp
     * @param lastModified last modification timestamp
     */
    public AIModel(@NotNull String id, @NotNull String name, @NotNull AIServiceType serviceType, 
                   @NotNull String modelId, boolean enabled, @NotNull String notes,
                   long createdAt, long lastModified) {
        this.id = id;
        this.name = name;
        this.serviceType = serviceType;
        this.modelId = modelId;
        this.enabled = enabled;
        this.notes = notes;
        this.createdAt = createdAt;
        this.lastModified = lastModified;
    }
    
    // ============================================================================
    // GETTERS AND SETTERS
    // ============================================================================
    
    /**
     * Gets the unique identifier for this model.
     * 
     * @return the unique identifier
     */
    @NotNull
    public String getId() {
        return id;
    }
    
    /**
     * Gets the user-friendly name for this model.
     * 
     * @return the model name
     */
    @NotNull
    public String getName() {
        return name;
    }
    
    /**
     * Sets the user-friendly name for this model.
     * 
     * @param name the new name
     */
    public void setName(@NotNull String name) {
        this.name = name;
        this.lastModified = System.currentTimeMillis();
    }
    
    /**
     * Gets the AI service type this model belongs to.
     * 
     * @return the service type
     */
    @NotNull
    public AIServiceType getServiceType() {
        return serviceType;
    }
    
    /**
     * Gets the specific model identifier.
     * 
     * @return the model ID
     */
    @NotNull
    public String getModelId() {
        return modelId;
    }
    
    /**
     * Checks if this model is enabled for use.
     * 
     * @return true if enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Sets whether this model is enabled for use.
     * 
     * @param enabled true to enable, false to disable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        this.lastModified = System.currentTimeMillis();
    }
    
    /**
     * Gets user notes about this model.
     * 
     * @return the notes
     */
    @NotNull
    public String getNotes() {
        return notes;
    }
    
    /**
     * Sets user notes about this model.
     * 
     * @param notes the new notes
     */
    public void setNotes(@NotNull String notes) {
        this.notes = notes;
        this.lastModified = System.currentTimeMillis();
    }
    
    /**
     * Gets the timestamp when this model was created.
     * 
     * @return the creation timestamp
     */
    public long getCreatedAt() {
        return createdAt;
    }
    
    /**
     * Gets the timestamp when this model was last modified.
     * 
     * @return the last modification timestamp
     */
    public long getLastModified() {
        return lastModified;
    }
    
    // ============================================================================
    // OPTIMAL SETTINGS (HARD-CODED FOR SIMPLICITY)
    // ============================================================================
    
    /**
     * Gets the optimal max tokens for this model.
     * 
     * @return the optimal max tokens
     */
    public int getMaxTokens() {
        return getOptimalMaxTokens(serviceType, modelId);
    }
    
    /**
     * Gets the optimal temperature for this model.
     * 
     * @return the optimal temperature
     */
    public double getTemperature() {
        return getOptimalTemperature(serviceType, modelId);
    }
    
    /**
     * Gets whether confidence scores should be included for this model.
     * 
     * @return true if confidence scores should be included
     */
    public boolean isIncludeConfidenceScores() {
        return getOptimalConfidenceScores(serviceType, modelId);
    }
    
    // ============================================================================
    // DISPLAY METHODS
    // ============================================================================
    
    /**
     * Gets the display name for this model.
     * 
     * @return the display name
     */
    @NotNull
    public String getDisplayName() {
        return name;
    }
    
    /**
     * Gets the full display name including service and model info.
     * 
     * @return the full display name
     */
    @NotNull
    public String getFullDisplayName() {
        return String.format("%s (%s - %s)", name, serviceType.getDisplayName(), modelId);
    }
    
    // ============================================================================
    // UTILITY METHODS
    // ============================================================================
    
    /**
     * Creates a copy of this model with a new name.
     * 
     * @param newName the new name for the copy
     * @return a new AIModel instance
     */
    @NotNull
    public AIModel copy(@NotNull String newName) {
        return new AIModel(newName, serviceType, modelId);
    }
    
    /**
     * Checks if this model configuration is valid.
     * 
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        return id != null && !id.trim().isEmpty() &&
               name != null && !name.trim().isEmpty() &&
               serviceType != null &&
               modelId != null && !modelId.trim().isEmpty();
    }
    
    // ============================================================================
    // STATIC UTILITY METHODS
    // ============================================================================
    
    /**
     * Gets the optimal max tokens for a given service and model.
     * 
     * @param serviceType the service type
     * @param modelId the model ID
     * @return the optimal max tokens
     */
    public static int getOptimalMaxTokens(@NotNull AIServiceType serviceType, @NotNull String modelId) {
        switch (serviceType) {
            case OPENAI:
                if (modelId.contains("gpt-4")) {
                    return 4000; // GPT-4 models can handle more tokens
                } else {
                    return 2000; // GPT-3.5 models
                }
            case GEMINI:
                if (modelId.contains("pro")) {
                    return 4000; // Gemini Pro models
                } else {
                    return 2000; // Gemini Flash models
                }
            default:
                return 2000; // Default fallback
        }
    }
    
    /**
     * Gets the optimal temperature for a given service and model.
     * 
     * @param serviceType the service type
     * @param modelId the model ID
     * @return the optimal temperature
     */
    public static double getOptimalTemperature(@NotNull AIServiceType serviceType, @NotNull String modelId) {
        // Lower temperature for more focused, deterministic responses
        return 0.3;
    }
    
    /**
     * Gets whether confidence scores should be included for a given service and model.
     * 
     * @param serviceType the service type
     * @param modelId the model ID
     * @return true if confidence scores should be included
     */
    public static boolean getOptimalConfidenceScores(@NotNull AIServiceType serviceType, @NotNull String modelId) {
        // Always include confidence scores for better analysis
        return true;
    }
    
    /**
     * Gets the available model IDs for a given service.
     * 
     * @param serviceType the service type
     * @return array of available model IDs
     */
    @NotNull
    public static String[] getAvailableModelIds(@NotNull AIServiceType serviceType) {
        switch (serviceType) {
            case OPENAI:
                return new String[]{
                    "gpt-4o",
                    "gpt-4o-mini",
                    "gpt-4",
                    "gpt-4-turbo",
                    "gpt-3.5-turbo",
                    "gpt-3.5-turbo-16k"
                };
            case GEMINI:
                return new String[]{
                    "gemini-pro",
                    "gemini-pro-vision",
                    "gemini-1.5-pro",
                    "gemini-1.5-flash"
                };
            default:
                return new String[0];
        }
    }
    
    /**
     * Gets the display name for a given model ID.
     * 
     * @param modelId the model ID
     * @return the display name
     */
    @NotNull
    public static String getModelDisplayName(@NotNull String modelId) {
        switch (modelId) {
            // OpenAI Models
            case "gpt-4o":
                return "GPT-4o (Latest)";
            case "gpt-4o-mini":
                return "GPT-4o Mini";
            case "gpt-4":
                return "GPT-4";
            case "gpt-4-turbo":
                return "GPT-4 Turbo";
            case "gpt-3.5-turbo":
                return "GPT-3.5 Turbo";
            case "gpt-3.5-turbo-16k":
                return "GPT-3.5 Turbo (16K)";
            
            // Gemini Models
            case "gemini-pro":
                return "Gemini Pro";
            case "gemini-pro-vision":
                return "Gemini Pro Vision";
            case "gemini-1.5-pro":
                return "Gemini 1.5 Pro";
            case "gemini-1.5-flash":
                return "Gemini 1.5 Flash";
            
            default:
                return modelId; // Fallback to model ID if unknown
        }
    }
    
    /**
     * Gets the use case description for a given service and model.
     * 
     * @param serviceType the service type
     * @param modelId the model ID
     * @return the use case description
     */
    @NotNull
    public static String getModelUseCase(@NotNull AIServiceType serviceType, @NotNull String modelId) {
        switch (serviceType) {
            case OPENAI:
                if (modelId.contains("gpt-4")) {
                    return "Complex analysis, reasoning, and detailed responses";
                } else {
                    return "Fast responses, general tasks, and quick analysis";
                }
            case GEMINI:
                if (modelId.contains("pro")) {
                    return "Advanced reasoning, complex tasks, and detailed analysis";
                } else {
                    return "Quick responses, general tasks, and fast processing";
                }
            default:
                return "General AI tasks and analysis";
        }
    }
    
    // ============================================================================
    // OBJECT METHODS
    // ============================================================================
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AIModel aiModel = (AIModel) obj;
        return Objects.equals(id, aiModel.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "AIModel{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", serviceType=" + serviceType +
                ", modelId='" + modelId + '\'' +
                ", enabled=" + enabled +
                ", createdAt=" + createdAt +
                '}';
    }
} 