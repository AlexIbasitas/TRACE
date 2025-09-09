package com.trace.ai.services;

import com.intellij.openapi.diagnostic.Logger;
import com.trace.ai.configuration.AIServiceType;
import com.trace.ai.models.AIModel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Helper class for AI model selection operations.
 * Provides methods for model selection, auto-selection, and default model management.
 * 
 * <p>This class encapsulates model selection operations to reduce the complexity
 * of the main AIModelService class and improve code organization.</p>
 * 
 * @author Alex Ibasitas
 * @version 1.0
 * @since 1.0
 */
public class AIModelSelectionHelper {
    
    private static final Logger LOG = Logger.getInstance(AIModelSelectionHelper.class);
    
    /**
     * Gets the best available model for a specific service.
     * 
     * @param serviceType the service type
     * @param modelCache the model cache to search in
     * @return the best available model for the service, or null if none available
     */
    @Nullable
    public static AIModel getBestAvailableModelForService(@NotNull AIServiceType serviceType, 
                                                          @NotNull java.util.Map<String, AIModel> modelCache) {
        List<AIModel> enabledModels = modelCache.values().stream()
                .filter(model -> model.getServiceType() == serviceType && model.isEnabled())
                .collect(Collectors.toList());
        
        if (enabledModels.isEmpty()) {
            return null;
        }
        
        // Return the first enabled model (could be enhanced with priority logic)
        return enabledModels.get(0);
    }
    
    /**
     * Auto-selects the best model based on current configuration.
     * 
     * @param modelCache the model cache to search in
     * @return the best model, or null if none available
     */
    @Nullable
    public static AIModel autoSelectBestModel(@NotNull java.util.Map<String, AIModel> modelCache) {
        List<AIModel> enabledModels = modelCache.values().stream()
                .filter(AIModel::isEnabled)
                .collect(Collectors.toList());
        
        if (enabledModels.isEmpty()) {
            return null;
        }
        
        // Simple selection: prefer GPT-4 models, then Gemini Pro models
        Optional<AIModel> gpt4Model = enabledModels.stream()
                .filter(model -> model.getServiceType() == AIServiceType.OPENAI && 
                               model.getModelId().contains("gpt-4"))
                .findFirst();
        
        if (gpt4Model.isPresent()) {
            return gpt4Model.get();
        }
        
        Optional<AIModel> geminiProModel = enabledModels.stream()
                .filter(model -> model.getServiceType() == AIServiceType.GEMINI && 
                               model.getModelId().contains("pro"))
                .findFirst();
        
        if (geminiProModel.isPresent()) {
            return geminiProModel.get();
        }
        
        // Return the first enabled model
        return enabledModels.get(0);
    }
    
    /**
     * Gets the next available model ID to use as default.
     * 
     * @param modelCache the model cache to search in
     * @return the next default model ID, or null if no models available
     */
    @Nullable
    public static String getNextDefaultModelId(@NotNull java.util.Map<String, AIModel> modelCache) {
        List<AIModel> enabledModels = modelCache.values().stream()
                .filter(AIModel::isEnabled)
                .collect(Collectors.toList());
        
        if (enabledModels.isEmpty()) {
            return null;
        }
        
        // Prefer the best available model (GPT-4 models first, then Gemini Pro)
        Optional<AIModel> bestModel = enabledModels.stream()
                .filter(model -> model.getServiceType() == AIServiceType.OPENAI && 
                               model.getModelId().contains("gpt-4"))
                .findFirst();
        
        if (bestModel.isPresent()) {
            return bestModel.get().getId();
        }
        
        // Fall back to first available model
        return enabledModels.get(0).getId();
    }
    
    /**
     * Ensures a valid default model is set, auto-selecting the best available if needed.
     * This is called when the current default model becomes unavailable.
     * 
     * @param currentDefault the current default model
     * @param modelCache the model cache to search in
     * @param state the state object to update
     * @param notifyStateChanged callback to notify state change
     * @return true if a new default was set, false otherwise
     */
    public static boolean ensureValidDefaultModel(@Nullable AIModel currentDefault,
                                                 @NotNull java.util.Map<String, AIModel> modelCache,
                                                 AIModelService.State state,
                                                 Runnable notifyStateChanged) {
        if (currentDefault != null && currentDefault.isEnabled()) {
            return false; // Current default is valid
        }
        
        // Current default is invalid or missing, find a new one
        String newDefaultId = getNextDefaultModelId(modelCache);
        if (newDefaultId != null) {
            state.defaultModelId = newDefaultId;
            AIModel newDefault = modelCache.get(newDefaultId);
            LOG.info("Auto-selected new default model: " + newDefault.getFullDisplayName());
            notifyStateChanged.run();
            return true;
        }
        
        return false;
    }
}