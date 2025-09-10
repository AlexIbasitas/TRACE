package com.trace.ai.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import javax.swing.JButton;
import com.intellij.util.ui.UIUtil;
import com.trace.ai.services.AIModelService;
import com.trace.ai.models.AIModel;
import com.trace.ai.configuration.AIServiceType;
import com.trace.ai.services.providers.GeminiProvider;
import com.trace.ai.services.providers.OpenAIProvider;
import com.trace.security.SecureAPIKeyManager;
import com.intellij.openapi.application.ApplicationManager;
import java.net.http.HttpClient;
import java.util.List;
import java.util.ArrayList;
import javax.swing.DefaultListModel;

/**
 * Helper class for managing AI model operations in the AI Service Configuration panel.
 * 
 * <p>This class provides methods for model discovery, management, and configuration.
 * It handles model list operations, default model selection, and model discovery
 * from various AI service providers.</p>
 * 
 * <p>The helper encapsulates all model management logic to keep the main
 * configuration panel focused on UI concerns while delegating model operations
 * to this specialized helper.</p>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
public class ModelManagerHelper {
    
    private static final Logger LOG = Logger.getInstance(ModelManagerHelper.class);
    
    /**
     * Sets the selected model as default.
     */
    public void setSelectedAsDefault(JBList<AIModel> modelList, AIModelService modelService, java.awt.Component parentComponent) {
        setSelectedAsDefault(modelList, modelService, parentComponent, null);
    }
    
    /**
     * Sets the selected model as default with optional callback for UI updates.
     */
    public void setSelectedAsDefault(JBList<AIModel> modelList, AIModelService modelService, java.awt.Component parentComponent, Runnable uiUpdateCallback) {
        AIModel selectedModel = modelList.getSelectedValue();
        if (selectedModel == null) {
            showError("Please select a model to set as default", parentComponent);
            return;
        }
        
        // Check if this model is already the default
        AIModel currentDefault = modelService.getDefaultModel();
        if (currentDefault != null && currentDefault.getId().equals(selectedModel.getId())) {
            showSuccess("Model is already set as default: " + selectedModel.getDisplayName(), parentComponent);
            return;
        }
        
        if (modelService.setDefaultModel(selectedModel.getId())) {
            showSuccess("Default model set to: " + selectedModel.getDisplayName(), parentComponent);
            
            // Update UI if callback provided
            if (uiUpdateCallback != null) {
                uiUpdateCallback.run();
            }
        } else {
            showError("Failed to set default model", parentComponent);
        }
    }
    
    /**
     * Refreshes the model list by fetching new models from API providers.
     * This method will discover available models from configured services and update the UI.
     */
    public void refreshModelList(JBList<AIModel> modelList, DefaultListModel<AIModel> listModel, 
                                JButton refreshModelsButton, AIModelService modelService, java.awt.Component parentComponent) {
        LOG.info("Refreshing model list - replacing with current available models from APIs");
        
        // Disable refresh button and show loading state
        refreshModelsButton.setEnabled(false);
        refreshModelsButton.setText("Refreshing...");
        
        // Get stored API keys in background
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            String storedOpenAIKey = SecureAPIKeyManager.getAPIKey(AIServiceType.OPENAI);
            String storedGeminiKey = SecureAPIKeyManager.getAPIKey(AIServiceType.GEMINI);
            
            // Replace entire model list with current available models
            replaceModelListWithCurrentModels(modelList, listModel, refreshModelsButton, modelService, 
                                            storedOpenAIKey, storedGeminiKey, parentComponent);
        });
    }
    
    /**
     * Refreshes the model list with provided API keys.
     * Only shows models for services with valid stored API keys.
     */
    public void refreshModelList(JBList<AIModel> modelList, DefaultListModel<AIModel> listModel, 
                                AIModelService modelService, String storedOpenAIKey, String storedGeminiKey) {
        LOG.debug("Refreshing model list - OpenAI: " + (storedOpenAIKey != null && !storedOpenAIKey.trim().isEmpty()) + 
                 ", Gemini: " + (storedGeminiKey != null && !storedGeminiKey.trim().isEmpty()));
        
        listModel.clear();
        
        // Get all models from storage
        List<AIModel> allModels = modelService.getAllModels();
        
        // Only show models for services with valid stored API keys
        List<AIModel> availableModels = new ArrayList<>();
        for (AIModel model : allModels) {
            boolean hasValidKey = false;
            if (model.getServiceType() == AIServiceType.OPENAI) {
                hasValidKey = (storedOpenAIKey != null && !storedOpenAIKey.trim().isEmpty());
            } else if (model.getServiceType() == AIServiceType.GEMINI) {
                hasValidKey = (storedGeminiKey != null && !storedGeminiKey.trim().isEmpty());
            }
            
            if (hasValidKey) {
                availableModels.add(model);
            }
        }
        
        // Add available models to UI
        for (AIModel model : availableModels) {
            listModel.addElement(model);
        }
        
        LOG.debug("Model list refreshed with " + availableModels.size() + " models");
    }
    
    /**
     * Discovers and adds new models for configured services with provided API keys.
     * Only called after successful API key validation.
     */
    public void discoverNewModels(AIModelService modelService, String storedOpenAIKey, String storedGeminiKey) {
        LOG.info("Discovering new models for configured services");
        
        // Create default models for services with valid stored keys
        if (storedOpenAIKey != null && !storedOpenAIKey.trim().isEmpty()) {
            createDefaultModelsIfNeeded(modelService, AIServiceType.OPENAI);
        }
        
        if (storedGeminiKey != null && !storedGeminiKey.trim().isEmpty()) {
            discoverGeminiModels(modelService, storedGeminiKey);
        }
    }
    
    /**
     * Creates default models for a service if they don't already exist.
     */
    public void createDefaultModelsIfNeeded(AIModelService modelService, AIServiceType serviceType) {
        String[] defaultModels;
        switch (serviceType) {
            case OPENAI:
                // Use the same stable models as the filter
                defaultModels = new String[]{"gpt-4o", "gpt-4o-mini", "gpt-3.5-turbo"};
                break;
            case GEMINI:
                defaultModels = new String[]{"gemini-1.5-flash", "gemini-1.5-pro"};
                break;
            default:
                return;
        }
        
        for (String modelId : defaultModels) {
            String displayName = getDefaultDisplayName(serviceType, modelId);
            if (!modelService.hasModelWithName(displayName)) {
                modelService.addDiscoveredModel(displayName, serviceType, modelId);
                LOG.info("Added default model: " + displayName);
            }
        }
    }
    
    /**
     * Discovers Gemini models dynamically using the API and filters to stable models only.
     */
    public void discoverGeminiModels(AIModelService modelService, String apiKey) {
        LOG.info("Discovering Gemini models dynamically");
        
        GeminiProvider provider = new GeminiProvider(HttpClient.newHttpClient());
        provider.discoverAvailableModels(apiKey)
            .thenAccept(allModels -> {
                ApplicationManager.getApplication().invokeLater(() -> {
                    // Filter to only stable, production-ready models
                    String[] stableModels = filterToStableGeminiModels(allModels);
                    
                    for (String modelId : stableModels) {
                        String displayName = getDefaultDisplayName(AIServiceType.GEMINI, modelId);
                        if (!modelService.hasModelWithName(displayName)) {
                            modelService.addDiscoveredModel(displayName, AIServiceType.GEMINI, modelId);
                            LOG.info("Added stable Gemini model: " + displayName);
                        }
                    }
                });
            })
            .exceptionally(throwable -> {
                LOG.warn("Failed to discover Gemini models, using defaults: " + throwable.getMessage());
                // Fallback to hardcoded models
                createDefaultModelsIfNeeded(modelService, AIServiceType.GEMINI);
                return null;
            });
    }
    
    /**
     * Filters discovered Gemini models to only include stable, production-ready models.
     * This ensures users see only the most reliable and useful models.
     */
    public String[] filterToStableGeminiModels(String[] allModels) {
        // Define stable, production-ready models in order of preference
        String[] stableModelIds = {
            "gemini-1.5-flash",      // Fast, cost-effective, general purpose
            "gemini-1.5-pro",        // High quality, general purpose
            "gemini-2.0-flash",      // Latest fast model (if available)
            "gemini-2.0-pro"         // Latest high-quality model (if available)
        };
        
        List<String> filteredModels = new ArrayList<>();
        
        // Add stable models that are available
        for (String stableId : stableModelIds) {
            for (String discoveredId : allModels) {
                if (discoveredId.equals(stableId)) {
                    filteredModels.add(discoveredId);
                    break;
                }
            }
        }
        
        // If no stable models found, fall back to basic models
        if (filteredModels.isEmpty()) {
            LOG.warn("No stable Gemini models found, using basic fallback");
            return new String[]{"gemini-1.5-flash", "gemini-1.5-pro"};
        }
        
        LOG.info("Filtered to " + filteredModels.size() + " stable Gemini models: " + 
                String.join(", ", filteredModels));
        
        return filteredModels.toArray(new String[0]);
    }
    
    /**
     * Filters discovered OpenAI models to only include stable, production-ready models suitable for text analysis.
     * This ensures users see only the most reliable and useful models for our use case.
     */
    public String[] filterToStableOpenAIModels(String[] allModels) {
        // Define stable, production-ready models in order of preference
        // Focus on models suitable for text analysis and code review
        String[] stableModelIds = {
            "gpt-4o",              // Latest GPT-4 model, best performance
            "gpt-4o-mini",         // Fast, cost-effective GPT-4
            "gpt-4-turbo",         // Previous GPT-4 Turbo
            "gpt-4",               // Standard GPT-4
            "gpt-3.5-turbo",       // Reliable, cost-effective option
            "gpt-3.5-turbo-16k"    // Higher context window version
        };
        
        List<String> filteredModels = new ArrayList<>();
        
        // Add stable models that are available
        for (String stableId : stableModelIds) {
            for (String discoveredId : allModels) {
                if (discoveredId.equals(stableId)) {
                    filteredModels.add(discoveredId);
                    break;
                }
            }
        }
        
        // If no stable models found, fall back to basic models
        if (filteredModels.isEmpty()) {
            LOG.warn("No stable OpenAI models found, using basic fallback");
            return new String[]{"gpt-3.5-turbo", "gpt-4o", "gpt-4o-mini"};
        }
        
        LOG.info("Filtered to " + filteredModels.size() + " stable OpenAI models: " + 
                String.join(", ", filteredModels));
        
        return filteredModels.toArray(new String[0]);
    }
    
    /**
     * Discovers OpenAI models using the OpenAI provider.
     * 
     * @param apiKey the OpenAI API key
     * @return true if models were discovered successfully, false otherwise
     */
    public boolean discoverOpenAIModels(AIModelService modelService, String apiKey) {
        try {
            LOG.info("Discovering OpenAI models");
            
            OpenAIProvider provider = new OpenAIProvider(HttpClient.newHttpClient());
            String[] discoveredModels = provider.discoverAvailableModels(apiKey).get();
            
            if (discoveredModels != null && discoveredModels.length > 0) {
                // Filter to only stable, production-ready models suitable for text analysis
                String[] stableModels = filterToStableOpenAIModels(discoveredModels);
                
                // Add discovered models
                for (String modelId : stableModels) {
                    String displayName = getDefaultDisplayName(AIServiceType.OPENAI, modelId);
                    modelService.addDiscoveredModel(displayName, AIServiceType.OPENAI, modelId);
                }
                
                LOG.info("Successfully discovered " + stableModels.length + " stable OpenAI models");
                return true;
            } else {
                LOG.warn("No OpenAI models discovered");
                return false;
            }
        } catch (Exception e) {
            LOG.error("Error discovering OpenAI models: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Discovers Gemini models using the Gemini provider.
     * 
     * @param apiKey the Gemini API key
     * @return true if models were discovered successfully, false otherwise
     */
    public boolean discoverGeminiModelsFromProvider(AIModelService modelService, String apiKey) {
        try {
            LOG.info("Discovering Gemini models from provider");
            
            GeminiProvider provider = new GeminiProvider(HttpClient.newHttpClient());
            String[] discoveredModels = provider.discoverAvailableModels(apiKey).get();
            
            if (discoveredModels != null && discoveredModels.length > 0) {
                // Filter to stable models only
                String[] stableModels = filterToStableGeminiModels(discoveredModels);
                
                // Add discovered models
                for (String modelId : stableModels) {
                    String displayName = getDefaultDisplayName(AIServiceType.GEMINI, modelId);
                    modelService.addDiscoveredModel(displayName, AIServiceType.GEMINI, modelId);
                }
                
                LOG.info("Successfully discovered " + stableModels.length + " stable Gemini models");
                return true;
            } else {
                LOG.warn("No Gemini models discovered");
                return false;
            }
        } catch (Exception e) {
            LOG.error("Error discovering Gemini models: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Discovers OpenAI models if API key is configured.
     * 
     * @param storedOpenAIKey the stored OpenAI API key
     * @return true if models were discovered successfully, false otherwise
     */
    public boolean discoverOpenAIModelsIfConfigured(AIModelService modelService, String storedOpenAIKey) {
        if (storedOpenAIKey == null || storedOpenAIKey.trim().isEmpty()) {
            return false;
        }
        
        try {
            return discoverOpenAIModels(modelService, storedOpenAIKey);
        } catch (Exception e) {
            LOG.warn("Failed to discover OpenAI models: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Discovers Gemini models if API key is configured.
     * 
     * @param storedGeminiKey the stored Gemini API key
     * @return true if models were discovered successfully, false otherwise
     */
    public boolean discoverGeminiModelsIfConfigured(AIModelService modelService, String storedGeminiKey) {
        if (storedGeminiKey == null || storedGeminiKey.trim().isEmpty()) {
            return false;
        }
        
        try {
            return discoverGeminiModelsFromProvider(modelService, storedGeminiKey);
        } catch (Exception e) {
            LOG.warn("Failed to discover Gemini models: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Replaces the entire model list with currently available models from APIs.
     * This ensures the list reflects the current state of the user's API access.
     * 
     * @param storedOpenAIKey the stored OpenAI API key
     * @param storedGeminiKey the stored Gemini API key
     */
    public void replaceModelListWithCurrentModels(JBList<AIModel> modelList, DefaultListModel<AIModel> listModel,
                                                JButton refreshModelsButton, AIModelService modelService,
                                                String storedOpenAIKey, String storedGeminiKey, java.awt.Component parentComponent) {
        LOG.info("Replacing model list with current available models from APIs");
        
        // Clear existing models first
        modelService.deleteAllModels();
        
        // Discover current available models
        final boolean openaiDiscovered = discoverOpenAIModelsIfConfigured(modelService, storedOpenAIKey);
        final boolean geminiDiscovered = discoverGeminiModelsIfConfigured(modelService, storedGeminiKey);
        
        // Update UI on EDT after discovery completes
        ApplicationManager.getApplication().invokeLater(() -> {
            // Re-enable refresh button
            refreshModelsButton.setEnabled(true);
            refreshModelsButton.setText("Refresh Models");
            
            // Show results to user
            if (openaiDiscovered || geminiDiscovered) {
                showSuccess("Model list refreshed successfully", parentComponent);
            } else if (storedOpenAIKey == null && storedGeminiKey == null) {
                showError("No API keys configured. Please configure API keys first.", parentComponent);
            } else {
                showError("Failed to refresh models. Please check your API keys and try again.", parentComponent);
            }
        });
    }
    
    /**
     * Gets the default display name for a model.
     */
    public String getDefaultDisplayName(AIServiceType serviceType, String modelId) {
        switch (serviceType) {
            case OPENAI:
                switch (modelId) {
                    case "gpt-3.5-turbo": return "GPT-3.5 Turbo";
                    case "gpt-3.5-turbo-16k": return "GPT-3.5 Turbo (16K)";
                    case "gpt-4": return "GPT-4";
                    case "gpt-4-turbo": return "GPT-4 Turbo";
                    case "gpt-4o": return "GPT-4o";
                    case "gpt-4o-mini": return "GPT-4o Mini";
                    default: return modelId;
                }
            case GEMINI:
                switch (modelId) {
                    case "gemini-1.5-flash": return "Gemini 1.5 Flash";
                    case "gemini-1.5-pro": return "Gemini 1.5 Pro";
                    case "gemini-2.0-flash": return "Gemini 2.0 Flash";
                    case "gemini-2.0-pro": return "Gemini 2.0 Pro";
                    default: return modelId; // Fallback for unknown models
                }
            default:
                return modelId;
        }
    }
    
    /**
     * Updates button states based on current selection.
     */
    public void updateButtonStates(JBList<AIModel> modelList, JButton setDefaultButton) {
        AIModel selectedModel = modelList.getSelectedValue();
        boolean hasSelection = selectedModel != null;
        
        setDefaultButton.setEnabled(hasSelection);
    }
    
    /**
     * Updates the default model display label.
     */
    public void updateDefaultModelDisplay(JBLabel defaultModelLabel, AIModelService modelService) {
        AIModel defaultModel = modelService.getDefaultModel();
        if (defaultModel != null) {
            defaultModelLabel.setText(defaultModel.getDisplayName() + " (" + defaultModel.getServiceType().getDisplayName() + ")");
        } else {
            defaultModelLabel.setText("No default model selected");
        }
    }
    
    /**
     * Shows an error message.
     */
    private void showError(String message, java.awt.Component parentComponent) {
        Messages.showErrorDialog(parentComponent, message, "Configuration Error");
    }
    
    /**
     * Shows a success message.
     */
    private void showSuccess(String message, java.awt.Component parentComponent) {
        Messages.showInfoMessage(parentComponent, message, "Success");
    }
}
