package com.trace.ai.services;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
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
    
    // Singleton instance
    private static AIModelService instance;
    
    // Current state
    private State myState = new State();
    
    // In-memory cache for fast access
    private final Map<String, AIModel> modelCache = new ConcurrentHashMap<>();
    
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
            AIServiceType serviceType = AIServiceType.valueOf(this.serviceType);
            return new AIModel(id, name, serviceType, modelId, enabled, notes,
                             createdAt, lastModified);
        }
    }
    
    /**
     * Private constructor for singleton pattern.
     */
    private AIModelService() {
        LOG.debug("AIModelService initialized");
    }
    
    /**
     * Gets the singleton instance of AIModelService.
     * 
     * @return the singleton instance
     */
    @NotNull
    public static AIModelService getInstance() {
        if (instance == null) {
            instance = new AIModelService();
        }
        return instance;
    }
    
    /**
     * Gets the singleton instance of AIModelService for a specific project.
     * 
     * @param project the project context
     * @return the singleton instance
     */
    @NotNull
    public static AIModelService getInstance(@NotNull Project project) {
        if (instance == null) {
            instance = new AIModelService();
        }
        return instance;
    }
    
    // ============================================================================
    // CRUD OPERATIONS
    // ============================================================================
    
    /**
     * Creates a new AI model with the specified parameters.
     * 
     * @param name user-friendly name for the model
     * @param serviceType the AI service type
     * @param modelId the specific model identifier
     * @return the created model, or null if creation failed
     */
    @Nullable
    public AIModel createModel(@NotNull String name, @NotNull AIServiceType serviceType, @NotNull String modelId) {
        if (name == null || name.trim().isEmpty()) {
            LOG.warn("Cannot create model with empty name");
            return null;
        }
        
        if (serviceType == null) {
            LOG.warn("Cannot create model with null service type");
            return null;
        }
        
        if (modelId == null || modelId.trim().isEmpty()) {
            LOG.warn("Cannot create model with empty model ID");
            return null;
        }
        
        // Check if model ID is valid for the service
        if (!isValidModelId(serviceType, modelId)) {
            LOG.warn("Invalid model ID '" + modelId + "' for service " + serviceType);
            return null;
        }
        
        // Check if name already exists
        if (hasModelWithName(name)) {
            LOG.warn("Model with name '" + name + "' already exists");
            return null;
        }
        
        try {
            AIModel model = new AIModel(name, serviceType, modelId);
            
            // Add to cache and state
            modelCache.put(model.getId(), model);
            myState.models.add(new AIModelData(model));
            
            // Set as default if no default exists
            if (myState.defaultModelId == null) {
                myState.defaultModelId = model.getId();
                LOG.info("Set new model as default: " + model.getFullDisplayName());
            }
            
            LOG.info("Created new AI model: " + model.getFullDisplayName());
            return model;
            
        } catch (Exception e) {
            LOG.error("Failed to create AI model", e);
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
        return new ArrayList<>(modelCache.values());
    }
    
    /**
     * Gets all enabled models.
     * 
     * @return list of enabled models
     */
    @NotNull
    public List<AIModel> getEnabledModels() {
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
                myState.defaultModelId = getNextDefaultModelId();
                LOG.info("Updated default model after deletion: " + myState.defaultModelId);
            }
            
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
     * Gets the default model.
     * 
     * @return the default model, or null if no default is set
     */
    @Nullable
    public AIModel getDefaultModel() {
        if (myState.defaultModelId == null) {
            return null;
        }
        
        AIModel model = modelCache.get(myState.defaultModelId);
        if (model == null || !model.isEnabled()) {
            // Default model not found or disabled, try to find a new default
            myState.defaultModelId = getNextDefaultModelId();
            if (myState.defaultModelId != null) {
                model = modelCache.get(myState.defaultModelId);
            }
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
        return autoSelectBestModel();
    }
    
    /**
     * Gets the best available model for a specific service.
     * 
     * @param serviceType the service type
     * @return the best available model for the service, or null if none available
     */
    @Nullable
    public AIModel getBestAvailableModelForService(@NotNull AIServiceType serviceType) {
        List<AIModel> enabledModels = getEnabledModelsForService(serviceType);
        if (enabledModels.isEmpty()) {
            return null;
        }
        
        // Return the first enabled model (could be enhanced with priority logic)
        return enabledModels.get(0);
    }
    
    /**
     * Auto-selects the best model based on current configuration.
     * 
     * @return the best model, or null if none available
     */
    @Nullable
    public AIModel autoSelectBestModel() {
        List<AIModel> enabledModels = getEnabledModels();
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
    
    // ============================================================================
    // VALIDATION AND UTILITY
    // ============================================================================
    
    /**
     * Checks if a model ID is valid for a given service.
     * 
     * @param serviceType the service type
     * @param modelId the model ID to validate
     * @return true if valid, false otherwise
     */
    public boolean isValidModelId(@NotNull AIServiceType serviceType, @NotNull String modelId) {
        String[] availableModels = AIModel.getAvailableModelIds(serviceType);
        return Arrays.asList(availableModels).contains(modelId);
    }
    
    /**
     * Checks if a model with the given name already exists.
     * 
     * @param name the name to check
     * @return true if a model with this name exists, false otherwise
     */
    public boolean hasModelWithName(@NotNull String name) {
        return modelCache.values().stream()
                .anyMatch(model -> model.getName().equals(name));
    }
    
    /**
     * Gets the total number of models.
     * 
     * @return the model count
     */
    public int getModelCount() {
        return modelCache.size();
    }
    
    /**
     * Gets the number of enabled models.
     * 
     * @return the enabled model count
     */
    public int getEnabledModelCount() {
        return (int) modelCache.values().stream()
                .filter(AIModel::isEnabled)
                .count();
    }
    
    /**
     * Checks if auto-selection of best model is enabled.
     * 
     * @return true if auto-selection is enabled, false otherwise
     */
    public boolean isAutoSelectBestModel() {
        return myState.autoSelectBestModel;
    }
    
    /**
     * Sets whether auto-selection of best model is enabled.
     * 
     * @param autoSelect true to enable auto-selection, false to disable
     */
    public void setAutoSelectBestModel(boolean autoSelect) {
        myState.autoSelectBestModel = autoSelect;
    }
    
    // ============================================================================
    // PERSISTENCE
    // ============================================================================
    
    @Override
    @Nullable
    public State getState() {
        return myState;
    }
    
    @Override
    public void loadState(@NotNull State state) {
        myState = state;
        
        // Rebuild cache from state
        modelCache.clear();
        for (AIModelData modelData : myState.models) {
            try {
                AIModel model = modelData.toAIModel();
                modelCache.put(model.getId(), model);
            } catch (Exception e) {
                LOG.error("Failed to load model from state: " + modelData.id, e);
            }
        }
        
        // Initialize default models if none exist
        if (modelCache.isEmpty()) {
            initializeDefaultModels();
        }
        
        LOG.info("Loaded " + modelCache.size() + " AI models from state");
    }
    
    /**
     * Initializes default models if no models exist.
     */
    private void initializeDefaultModels() {
        LOG.info("Initializing default AI models");
        
        // Create default OpenAI model
        createModel("GPT-4o (Default)", AIServiceType.OPENAI, "gpt-4o");
        
        // Create default Gemini model
        createModel("Gemini Pro (Default)", AIServiceType.GEMINI, "gemini-pro");
    }
    
    /**
     * Gets the next available model ID to use as default.
     * 
     * @return the next default model ID, or null if no models available
     */
    @Nullable
    private String getNextDefaultModelId() {
        List<AIModel> enabledModels = getEnabledModels();
        if (enabledModels.isEmpty()) {
            return null;
        }
        return enabledModels.get(0).getId();
    }
} 