package com.trace.ai.services;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.trace.ai.models.AIModel;
import com.trace.ai.configuration.AIServiceType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for managing AI model configurations with full CRUD operations.
 * 
 * <p>This service provides comprehensive management of AI models, including:</p>
 * <ul>
 *   <li>Create, read, update, delete operations</li>
 *   <li>Database persistence across IDE sessions</li>
 *   <li>Model prioritization and selection</li>
 *   <li>Validation and error handling</li>
 *   <li>Integration with AISettings service</li>
 * </ul>
 * 
 * <p>The service maintains a collection of AIModel instances and provides
 * methods for managing them efficiently. All changes are persisted to
 * IntelliJ's state management system.</p>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
@State(
    name = "com.trace.ai.services.ai-model-service",
    storages = @Storage("trace-ai-models.xml")
)
public class AIModelService implements PersistentStateComponent<AIModelService.State> {
    
    private static final Logger LOG = Logger.getInstance(AIModelService.class);
    
    // Cache configuration
    private static final long MODEL_CACHE_DURATION_MS = 5000; // 5 seconds
    private static final String DEFAULT_MODEL_CACHE_KEY = "default_model";
    
    
    // Service instance managed by IntelliJ
    
    // Current state
    private State myState = new State();
    
    // In-memory cache for fast access
    private final Map<String, AIModel> modelCache = new ConcurrentHashMap<>();
    
    // Time-based cache for model retrieval to prevent spam
    private final Map<String, CachedModelResult> retrievalCache = new ConcurrentHashMap<>();
    
    // Flag to ensure cleanup only runs once
    private boolean cleanupPerformed = false;
    
    /**
     * Cached model result with timestamp for deduplication.
     */
    private static class CachedModelResult {
        final AIModel model;
        final long timestamp;
        
        CachedModelResult(AIModel model) {
            this.model = model;
            this.timestamp = System.currentTimeMillis();
        }
        
        boolean isValid() {
            return System.currentTimeMillis() - timestamp < MODEL_CACHE_DURATION_MS;
        }
    }
    
    /**
     * State class for persistence.
     */
    public static class State {
        public List<AIModelData> models = new ArrayList<>();
        public String defaultModelId = null;
        public boolean autoSelectBestModel = true;
    }
    
    /**
     * Data class for serialization.
     */
    public static class AIModelData {
        public String id;
        public String name;
        public String serviceType;
        public String modelId;
        public boolean enabled;

        public String notes;
        public long createdAt;
        public long lastModified;
        
        public AIModelData() {}
        
        public AIModelData(AIModel model) {
            this.id = model.getId();
            this.name = model.getName();
            this.serviceType = model.getServiceType().name();
            this.modelId = model.getModelId();
            this.enabled = model.isEnabled();
            this.notes = model.getNotes();
            this.createdAt = model.getCreatedAt();
            this.lastModified = model.getLastModified();
        }
        
        public AIModel toAIModel() {
            LOG.debug("Converting AIModelData to AIModel: serviceType=" + this.serviceType + ", modelId=" + this.modelId);
            AIServiceType serviceType = AIServiceType.valueOf(this.serviceType);
            LOG.debug("Parsed service type: " + serviceType);
            
            AIModel model = new AIModel(id, name, serviceType, modelId, enabled, notes,
                             createdAt, lastModified);
            LOG.debug("Created AIModel: " + model.getFullDisplayName() + 
                    " (Service: " + model.getServiceType() + 
                    ", ID: " + model.getModelId() + ")");
            return model;
        }
    }
    
    /**
     * Private constructor for singleton pattern.
     */
    private AIModelService() {
        LOG.debug("AIModelService constructor called");
        
        // Ensure cleanup runs when IntelliJ creates the service
        ensureCleanup();
    }
    
    /**
     * Gets the service instance from IntelliJ's service container.
     * 
     * @return the service instance
     */
    @NotNull
    public static AIModelService getInstance() {
        return com.intellij.openapi.application.ApplicationManager.getApplication().getService(AIModelService.class);
    }
    
    /**
     * Ensures cleanup has been performed (runs once per instance).
     */
    private void ensureCleanup() {
        if (!cleanupPerformed) {
            LOG.debug("Performing initial cleanup of deprecated models");
            AIModelMaintenanceHelper.cleanupDeprecatedModels(modelCache, this::deleteModel);
            cleanupPerformed = true;
        }
    }
    
    /**
     * Adds a discovered AI model to the service.
     * This is used internally when discovering models from APIs.
     * 
     * @param name the model name
     * @param serviceType the service type
     * @param modelId the model ID from the API
     * @return the added model, or null if addition failed
     */
    @Nullable
    public AIModel addDiscoveredModel(@NotNull String name, @NotNull AIServiceType serviceType, @NotNull String modelId) {
        if (name == null || name.trim().isEmpty()) {
            LOG.warn("Cannot add model with empty name");
            return null;
        }
        
        if (serviceType == null) {
            LOG.warn("Cannot add model with null service type");
            return null;
        }
        
        if (modelId == null || modelId.trim().isEmpty()) {
            LOG.warn("Cannot add model with empty model ID");
            return null;
        }
        
        // Check if name already exists
        if (AIModelValidationHelper.hasModelWithName(name, modelCache)) {
            LOG.debug("Model with name '" + name + "' already exists, skipping");
            return null;
        }
        
        try {
            AIModel model = new AIModel(name, serviceType, modelId);
            
            // Debug logging for model creation
            LOG.info("Creating model: " + model.getFullDisplayName());
            LOG.info("Model service type: " + model.getServiceType());
            LOG.info("Model ID: " + model.getModelId());
            
            // Add to cache and state
            modelCache.put(model.getId(), model);
            myState.models.add(new AIModelData(model));
            
            // Set as default if no default exists (first-time setup only)
            if (myState.defaultModelId == null) {
                myState.defaultModelId = model.getId();
                LOG.info("Set new model as default (first-time setup): " + model.getFullDisplayName());
            }
            
            // Notify IntelliJ that state has changed
            notifyStateChanged();
            
            LOG.debug("Added discovered AI model: " + model.getFullDisplayName());
            return model;
            
        } catch (Exception e) {
            LOG.error("Failed to add discovered AI model", e);
            return null;
        }
    }
    
    /**
     * Gets a model by its ID.
     * 
     * @param modelId the model ID
     * @return the model, or null if not found
     */
    @Nullable
    public AIModel getModel(@NotNull String modelId) {
        return modelCache.get(modelId);
    }
    
    /**
     * Gets all models.
     * 
     * @return list of all models
     */
    @NotNull
    public List<AIModel> getAllModels() {
        // Clean up deprecated models before returning
        AIModelMaintenanceHelper.cleanupDeprecatedModels(modelCache, this::deleteModel);
        
        List<AIModel> models = new ArrayList<>(modelCache.values());
        LOG.debug("Getting all models, count: " + models.size());
        
        // Only log model details at DEBUG level to reduce spam
        if (LOG.isDebugEnabled()) {
            for (AIModel model : models) {
                LOG.debug("Model in cache: " + model.getFullDisplayName() + 
                        " (Service: " + model.getServiceType() + 
                        ", ID: " + model.getModelId() + 
                        ", Enabled: " + model.isEnabled() + ")");
            }
        }
        
        return models;
    }
    
    /**
     * Gets all enabled models.
     * 
     * @return list of enabled models
     */
    @NotNull
    public List<AIModel> getEnabledModels() {
        // Clean up deprecated models before returning
        AIModelMaintenanceHelper.cleanupDeprecatedModels(modelCache, this::deleteModel);
        return modelCache.values().stream()
                .filter(AIModel::isEnabled)
                .collect(Collectors.toList());
    }
    
    /**
     * Gets all models for a specific service.
     * 
     * @param serviceType the service type
     * @return list of models for the service
     */
    @NotNull
    public List<AIModel> getModelsForService(@NotNull AIServiceType serviceType) {
        return modelCache.values().stream()
                .filter(model -> model.getServiceType() == serviceType)
                .collect(Collectors.toList());
    }
    
    /**
     * Gets all enabled models for a specific service.
     * 
     * @param serviceType the service type
     * @return list of enabled models for the service
     */
    @NotNull
    public List<AIModel> getEnabledModelsForService(@NotNull AIServiceType serviceType) {
        return modelCache.values().stream()
                .filter(model -> model.getServiceType() == serviceType && model.isEnabled())
                .collect(Collectors.toList());
    }
    
    /**
     * Updates an existing model.
     * 
     * @param model the model to update
     * @return true if update was successful, false otherwise
     */
    public boolean updateModel(@NotNull AIModel model) {
        if (model == null) {
            LOG.warn("Cannot update null model");
            return false;
        }
        
        if (!model.isValid()) {
            LOG.warn("Cannot update invalid model: " + model);
            return false;
        }
        
        try {
            // Update cache
            modelCache.put(model.getId(), model);
            
            // Update state
            AIModelData modelData = new AIModelData(model);
            myState.models.removeIf(data -> data.id.equals(model.getId()));
            myState.models.add(modelData);
            
            // Notify IntelliJ that state has changed
            notifyStateChanged();
            
            // Invalidate cache since model data changed
            AIModelMaintenanceHelper.invalidateRetrievalCache(retrievalCache);
            
            LOG.info("Updated AI model: " + model.getFullDisplayName());
            return true;
            
        } catch (Exception e) {
            LOG.error("Failed to update AI model", e);
            return false;
        }
    }
    
    /**
     * Deletes a model by its ID.
     * 
     * @param modelId the model ID to delete
     * @return true if deletion was successful, false otherwise
     */
    public boolean deleteModel(@NotNull String modelId) {
        if (modelId == null || modelId.trim().isEmpty()) {
            LOG.warn("Cannot delete model with empty ID");
            return false;
        }
        
        try {
            AIModel model = modelCache.get(modelId);
            if (model == null) {
                LOG.warn("Model not found for deletion: " + modelId);
                return false;
            }
            
            // Remove from cache
            modelCache.remove(modelId);
            
            // Remove from state
            myState.models.removeIf(data -> data.id.equals(modelId));
            
            // Update default model if this was the default
            if (modelId.equals(myState.defaultModelId)) {
                myState.defaultModelId = AIModelSelectionHelper.getNextDefaultModelId(modelCache);
                LOG.info("Updated default model after deletion: " + myState.defaultModelId);
            }
            
            // Notify IntelliJ that state has changed
            notifyStateChanged();
            
            LOG.info("Deleted AI model: " + model.getFullDisplayName());
            return true;
            
        } catch (Exception e) {
            LOG.error("Failed to delete AI model", e);
            return false;
        }
    }
    
    /**
     * Deletes all models.
     * 
     * @return true if deletion was successful, false otherwise
     */
    public boolean deleteAllModels() {
        try {
            int count = modelCache.size();
            modelCache.clear();
            myState.models.clear();
            myState.defaultModelId = null;
            
            // Notify IntelliJ that state has changed
            notifyStateChanged();
            
            LOG.info("Deleted all " + count + " AI models");
            return true;
            
        } catch (Exception e) {
            LOG.error("Failed to delete all AI models", e);
            return false;
        }
    }
    
    // ============================================================================
    // DEFAULT MODEL MANAGEMENT
    // ============================================================================
    
    /**
     * Gets the default model with time-based caching to prevent logging spam.
     * 
     * @return the default model, or null if no default is set
     */
    @Nullable
    public AIModel getDefaultModel() {
        // Check cache first to prevent redundant operations
        CachedModelResult cached = retrievalCache.get(DEFAULT_MODEL_CACHE_KEY);
        if (cached != null && cached.isValid()) {
            LOG.debug("Returning cached default model: " + 
                     (cached.model != null ? cached.model.getFullDisplayName() : "null"));
            return cached.model;
        }
        
        // Perform actual model retrieval
        AIModel model = retrieveDefaultModelInternal();
        
        // Cache the result
        retrievalCache.put(DEFAULT_MODEL_CACHE_KEY, new CachedModelResult(model));
        
        return model;
    }
    
    /**
     * Internal method to retrieve the default model with detailed logging.
     * 
     * @return the default model, or null if no default is set
     */
    @Nullable
    private AIModel retrieveDefaultModelInternal() {
        LOG.debug("Retrieving default model, defaultModelId: " + myState.defaultModelId);
        
        if (myState.defaultModelId == null) {
            LOG.debug("No default model ID set");
            return null;
        }
        
        AIModel model = modelCache.get(myState.defaultModelId);
        LOG.debug("Retrieved model from cache: " + (model != null ? model.getFullDisplayName() : "null"));
        
        if (model == null || !model.isEnabled()) {
            LOG.info("Default model not found or disabled, finding new default");
            // Default model not found or disabled, try to find a new default
            myState.defaultModelId = AIModelSelectionHelper.getNextDefaultModelId(modelCache);
            LOG.debug("New default model ID: " + myState.defaultModelId);
            if (myState.defaultModelId != null) {
                model = modelCache.get(myState.defaultModelId);
                LOG.debug("New default model: " + (model != null ? model.getFullDisplayName() : "null"));
            }
        }
        
        if (model != null) {
            LOG.debug("Returning default model: " + model.getFullDisplayName());
            LOG.debug("Default model service type: " + model.getServiceType());
            LOG.debug("Default model ID: " + model.getModelId());
        }
        
        return model;
    }
    
    /**
     * Sets the default model by ID.
     * 
     * @param modelId the model ID to set as default
     * @return true if successful, false otherwise
     */
    public boolean setDefaultModel(@NotNull String modelId) {
        if (modelId == null || modelId.trim().isEmpty()) {
            LOG.warn("Cannot set empty model ID as default");
            return false;
        }
        
        if (!modelCache.containsKey(modelId)) {
            LOG.warn("Model not found for setting as default: " + modelId);
            return false;
        }
        
        myState.defaultModelId = modelId;
        
        // Notify IntelliJ that state has changed
        notifyStateChanged();
        
        // Invalidate cache since default model changed
        AIModelMaintenanceHelper.invalidateRetrievalCache(retrievalCache);
        
        LOG.info("Set default model: " + modelId);
        return true;
    }
    
    // ============================================================================
    // MODEL SELECTION
    // ============================================================================
    
    /**
     * Gets the best available model based on current configuration.
     * 
     * @return the best available model, or null if none available
     */
    @Nullable
    public AIModel getBestAvailableModel() {
        // First try the default model
        AIModel defaultModel = getDefaultModel();
        if (defaultModel != null && defaultModel.isEnabled()) {
            return defaultModel;
        }
        
        // Fall back to auto-selection
        return AIModelSelectionHelper.autoSelectBestModel(modelCache);
    }
    
    /**
     * Gets the best available model for a specific service.
     * 
     * @param serviceType the service type
     * @return the best available model for the service, or null if none available
     */
    @Nullable
    public AIModel getBestAvailableModelForService(@NotNull AIServiceType serviceType) {
        return AIModelSelectionHelper.getBestAvailableModelForService(serviceType, modelCache);
    }
    
    /**
     * Auto-selects the best model based on current configuration.
     * 
     * @return the best model, or null if none available
     */
    @Nullable
    public AIModel autoSelectBestModel() {
        return AIModelSelectionHelper.autoSelectBestModel(modelCache);
    }
    
    // ============================================================================
    // VALIDATION AND UTILITY
    // ============================================================================
    
    /**
     * Checks if a model with the given name already exists.
     * 
     * @param name the name to check
     * @return true if a model with this name exists, false otherwise
     */
    public boolean hasModelWithName(@NotNull String name) {
        return AIModelValidationHelper.hasModelWithName(name, modelCache);
    }
    
    /**
     * Gets the total number of models.
     * 
     * @return the model count
     */
    public int getModelCount() {
        return AIModelValidationHelper.getModelCount(modelCache);
    }
    
    /**
     * Gets the number of enabled models.
     * 
     * @return the enabled model count
     */
    public int getEnabledModelCount() {
        return AIModelValidationHelper.getEnabledModelCount(modelCache);
    }
    
    /**
     * Checks if auto-selection of best model is enabled.
     * 
     * @return true if auto-selection is enabled, false otherwise
     */
    public boolean isAutoSelectBestModel() {
        return AIModelValidationHelper.isAutoSelectBestModel(myState.autoSelectBestModel);
    }
    
    /**
     * Sets whether auto-selection of best model is enabled.
     * 
     * @param autoSelect true to enable auto-selection, false to disable
     */
    public void setAutoSelectBestModel(boolean autoSelect) {
        AIModelValidationHelper.setAutoSelectBestModel(autoSelect, myState, this::notifyStateChanged);
    }
    
    
    /**
     * Notifies IntelliJ that the service state has changed and needs to be persisted.
     */
    private void notifyStateChanged() {
        try {
            // Get the service and notify that our state has changed
            ApplicationManager.getApplication().getService(AIModelService.class);
        } catch (Exception e) {
            LOG.error("Failed to notify state change", e);
        }
    }
    
    // ============================================================================
    // PERSISTENCE
    // ============================================================================
    
    @Override
    @Nullable
    public State getState() {
        // Clear and rebuild the models list from cache
        myState.models.clear();
        for (AIModel model : modelCache.values()) {
            myState.models.add(new AIModelData(model));
        }
        
        return myState;
    }
    
    @Override
    public void loadState(@NotNull State state) {
        LOG.debug("Loading state with " + state.models.size() + " models");
        LOG.debug("Default model ID in state: " + state.defaultModelId);
        
        myState = state;
        
        // Rebuild cache from state
        modelCache.clear();
        for (AIModelData modelData : myState.models) {
            try {
                LOG.debug("Loading model data: id=" + modelData.id + 
                        ", name=" + modelData.name + 
                        ", serviceType=" + modelData.serviceType + 
                        ", modelId=" + modelData.modelId);
                
                AIModel model = modelData.toAIModel();
                modelCache.put(model.getId(), model);
                LOG.debug("Loaded model from state: " + model.getFullDisplayName());
                LOG.debug("Loaded model service type: " + model.getServiceType());
                LOG.debug("Loaded model ID: " + model.getModelId());
            } catch (Exception e) {
                LOG.error("Failed to load model from state: " + modelData.id, e);
            }
        }
        
        // Clean up deprecated models
        AIModelMaintenanceHelper.cleanupDeprecatedModels(modelCache, this::deleteModel);
        
        // Initialize default models if none exist
        if (modelCache.isEmpty()) {
            LOG.debug("No models found in state, initializing defaults");
            AIModelMaintenanceHelper.initializeDefaultModels(this::addDiscoveredModel);
        }
    }
    
    /**
     * Ensures a valid default model is set, auto-selecting the best available if needed.
     * This is called when the current default model becomes unavailable.
     * 
     * @return true if a new default was set, false otherwise
     */
    public boolean ensureValidDefaultModel() {
        AIModel currentDefault = getDefaultModel();
        return AIModelSelectionHelper.ensureValidDefaultModel(currentDefault, modelCache, myState, this::notifyStateChanged);
    }
} 