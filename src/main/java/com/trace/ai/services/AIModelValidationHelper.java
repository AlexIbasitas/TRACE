package com.trace.ai.services;

import com.intellij.openapi.diagnostic.Logger;
import com.trace.ai.configuration.AIServiceType;
import com.trace.ai.models.AIModel;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Helper class for AI model validation and utility operations.
 * Provides methods for model validation, counting, and configuration management.
 * 
 * <p>This class encapsulates validation and utility operations to reduce the complexity
 * of the main AIModelService class and improve code organization.</p>
 * 
 * @author Alex Ibasitas
 * @version 1.0
 * @since 1.0
 */
public class AIModelValidationHelper {
    
    private static final Logger LOG = Logger.getInstance(AIModelValidationHelper.class);
    
    // Stupid simple - hardcoded list of known deprecated models
    private static final Set<String> DEPRECATED_MODELS = Set.of(
        "gemini-1.0-pro-vision-latest",
        "gemini-pro", 
        "gemini-pro-vision"
    );
    
    /**
     * Checks if a model with the given name already exists.
     * 
     * @param name the name to check
     * @param modelCache the model cache to search in
     * @return true if a model with this name exists, false otherwise
     */
    public static boolean hasModelWithName(@NotNull String name, @NotNull java.util.Map<String, AIModel> modelCache) {
        return modelCache.values().stream()
                .anyMatch(model -> model.getName().equals(name));
    }
    
    /**
     * Gets the total number of models.
     * 
     * @param modelCache the model cache to count
     * @return the model count
     */
    public static int getModelCount(@NotNull java.util.Map<String, AIModel> modelCache) {
        return modelCache.size();
    }
    
    /**
     * Gets the number of enabled models.
     * 
     * @param modelCache the model cache to count
     * @return the enabled model count
     */
    public static int getEnabledModelCount(@NotNull java.util.Map<String, AIModel> modelCache) {
        return (int) modelCache.values().stream()
                .filter(AIModel::isEnabled)
                .count();
    }
    
    /**
     * Checks if a model ID is deprecated.
     * 
     * @param serviceType the service type
     * @param modelId the model ID to check
     * @return true if deprecated, false otherwise
     */
    public static boolean isDeprecatedModel(@NotNull AIServiceType serviceType, @NotNull String modelId) {
        switch (serviceType) {
            case GEMINI:
                return DEPRECATED_MODELS.contains(modelId);
            case OPENAI:
                // Currently no deprecated OpenAI models
                return false;
            default:
                return false;
        }
    }
    
    /**
     * Checks if auto-selection of best model is enabled.
     * 
     * @param autoSelectBestModel the auto-select setting
     * @return true if auto-selection is enabled, false otherwise
     */
    public static boolean isAutoSelectBestModel(boolean autoSelectBestModel) {
        return autoSelectBestModel;
    }
    
    /**
     * Sets whether auto-selection of best model is enabled.
     * 
     * @param autoSelect true to enable auto-selection, false to disable
     * @param state the state object to update
     * @param notifyStateChanged callback to notify state change
     */
    public static void setAutoSelectBestModel(boolean autoSelect, AIModelService.State state, Runnable notifyStateChanged) {
        state.autoSelectBestModel = autoSelect;
        notifyStateChanged.run();
    }
}