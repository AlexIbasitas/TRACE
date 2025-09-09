package com.trace.ai.services;

import com.intellij.openapi.diagnostic.Logger;
import com.trace.ai.configuration.AIServiceType;
import com.trace.ai.models.AIModel;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Helper class for AI model maintenance and cleanup operations.
 * Provides methods for model cleanup, initialization, and cache management.
 * 
 * <p>This class encapsulates maintenance operations to reduce the complexity
 * of the main AIModelService class and improve code organization.</p>
 * 
 * @author Alex Ibasitas
 * @version 1.0
 * @since 1.0
 */
public class AIModelMaintenanceHelper {
    
    private static final Logger LOG = Logger.getInstance(AIModelMaintenanceHelper.class);
    
    /**
     * Cleans up deprecated models from the cache and state.
     * 
     * @param modelCache the model cache to clean up
     * @param deleteModelCallback callback to delete a model by ID
     */
    public static void cleanupDeprecatedModels(@NotNull Map<String, AIModel> modelCache, 
                                              @NotNull java.util.function.Function<String, Boolean> deleteModelCallback) {
        LOG.debug("AIModelMaintenanceHelper.cleanupDeprecatedModels() called");
        List<String> deprecatedModelIds = new ArrayList<>();
        
        for (AIModel model : modelCache.values()) {
            if (AIModelValidationHelper.isDeprecatedModel(model.getServiceType(), model.getModelId())) {
                LOG.info("Found deprecated model: " + model.getFullDisplayName() + " - will be removed");
                deprecatedModelIds.add(model.getId());
            }
        }
        
        // Remove deprecated models
        for (String modelId : deprecatedModelIds) {
            deleteModelCallback.apply(modelId);
        }
        
        if (!deprecatedModelIds.isEmpty()) {
            LOG.info("Cleaned up " + deprecatedModelIds.size() + " deprecated models");
        }
    }
    
    /**
     * Initializes default models if no models exist.
     * 
     * @param addDiscoveredModelCallback callback to add a discovered model
     */
    public static void initializeDefaultModels(@NotNull Function3<String, AIServiceType, String, AIModel> addDiscoveredModelCallback) {
        LOG.debug("Initializing default models");
        
        // Create default OpenAI model
        AIModel openaiModel = addDiscoveredModelCallback.apply("GPT-3.5 Turbo (Default)", AIServiceType.OPENAI, "gpt-3.5-turbo");
        if (openaiModel != null) {
            LOG.debug("Created OpenAI model: " + openaiModel.getFullDisplayName());
            LOG.debug("OpenAI model service type: " + openaiModel.getServiceType());
            LOG.debug("OpenAI model ID: " + openaiModel.getModelId());
        }
        
        // Create default Gemini model
        AIModel geminiModel = addDiscoveredModelCallback.apply("Gemini 1.5 Flash (Default)", AIServiceType.GEMINI, "gemini-1.5-flash");
        if (geminiModel != null) {
            LOG.debug("Created Gemini model: " + geminiModel.getFullDisplayName());
            LOG.debug("Gemini model service type: " + geminiModel.getServiceType());
            LOG.debug("Gemini model ID: " + geminiModel.getModelId());
        }
        
        // Also create the specific model that's causing the issue
        AIModel geminiProModel = addDiscoveredModelCallback.apply("Gemini 1.5 Pro (Default)", AIServiceType.GEMINI, "gemini-1.5-pro");
        if (geminiProModel != null) {
            LOG.debug("Created Gemini Pro model: " + geminiProModel.getFullDisplayName());
            LOG.debug("Gemini Pro model service type: " + geminiProModel.getServiceType());
            LOG.debug("Gemini Pro model ID: " + geminiProModel.getModelId());
        }
    }
    
    /**
     * Invalidates the retrieval cache to ensure fresh data on next request.
     * 
     * @param retrievalCache the retrieval cache to invalidate
     */
    public static void invalidateRetrievalCache(@NotNull Map<String, ?> retrievalCache) {
        retrievalCache.clear();
        LOG.debug("Retrieval cache invalidated");
    }
    
    /**
     * Functional interface for three-parameter function.
     * 
     * @param <T> first parameter type
     * @param <U> second parameter type
     * @param <V> third parameter type
     * @param <R> return type
     */
    @FunctionalInterface
    public interface Function3<T, U, V, R> {
        R apply(T t, U u, V v);
    }
}
