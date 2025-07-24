package com.trace.ai.services;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.trace.ai.models.AIAnalysisResult;
import com.trace.ai.models.AIModel;
import com.trace.ai.prompts.LocalPromptGenerationService;
import com.trace.ai.prompts.PromptGenerationService;
import com.trace.ai.services.providers.AIServiceProvider;
import com.trace.test.models.FailureInfo;
import com.trace.security.SecureAPIKeyManager;
import com.trace.ai.configuration.AIServiceType;
import com.trace.ai.configuration.AISettings;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Simplified network service that delegates to specific AI service providers.
 * 
 * <p>This service has been refactored to use the new clean architecture with
 * proper separation of concerns. It now delegates all AI service communication
 * to specific provider implementations while maintaining the same public API.</p>
 * 
 * <p>Key improvements:</p>
 * <ul>
 *   <li>Clean separation between model management and service connection</li>
 *   <li>Delegation to service-specific providers</li>
 *   <li>Simplified error handling and validation</li>
 *   <li>Better extensibility for new AI services</li>
 *   <li>Consistent interface across all providers</li>
 * </ul>
 * 
 * <p>This service now focuses on:</p>
 * <ul>
 *   <li>Coordinating between model configuration and service providers</li>
 *   <li>Handling high-level error scenarios</li>
 *   <li>Providing a unified interface for AI analysis</li>
 *   <li>Managing service selection and fallback logic</li>
 * </ul>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
public final class AINetworkService {
    
    private static final Logger LOG = Logger.getInstance(AINetworkService.class);
    
    // Services
    private final Project project;
    private final PromptGenerationService promptService;
    
    /**
     * Constructor for AINetworkService.
     * 
     * @param project The IntelliJ project context
     * @throws NullPointerException if project is null
     */
    public AINetworkService(@NotNull Project project) {
        if (project == null) {
            throw new NullPointerException("Project cannot be null");
        }
        
        this.project = project;
        this.promptService = new LocalPromptGenerationService();
    }
    
    /**
     * Analyzes a failure using the configured AI service and model.
     * 
     * <p>This method coordinates between the model configuration and the
     * appropriate AI service provider to perform the analysis.</p>
     * 
     * @param failureInfo the failure information to analyze
     * @return a CompletableFuture containing the analysis result
     * @throws IllegalArgumentException if failureInfo is null
     * @throws IllegalStateException if no AI service is properly configured
     */
    public CompletableFuture<AIAnalysisResult> analyze(@NotNull FailureInfo failureInfo) {
        if (failureInfo == null) {
            throw new IllegalArgumentException("FailureInfo cannot be null");
        }
        
        LOG.info("Starting AI analysis for failure: " + failureInfo.getScenarioName());
        
        try {
            // Get configured services
            AISettings aiSettings = AISettings.getInstance();
            AIModelService modelService = AIModelService.getInstance();
            
            // Get the preferred service type
            AIServiceType serviceType = aiSettings.getPreferredAIService();
            if (serviceType == null) {
                throw new IllegalStateException("No AI service type configured");
            }
            
            // Get the default model for this service
            AIModel defaultModel = modelService.getDefaultModel();
            if (defaultModel == null) {
                throw new IllegalStateException("No default AI model configured");
            }
            
            // Validate that the model matches the service type
            if (defaultModel.getServiceType() != serviceType) {
                LOG.warn("Default model service type (" + defaultModel.getServiceType() + 
                        ") doesn't match preferred service type (" + serviceType + ")");
            }
            
            // Get the provider for this service
            AIServiceProvider provider = AIServiceFactory.getProvider(serviceType);
            if (provider == null) {
                throw new UnsupportedOperationException("No provider available for service: " + serviceType);
            }
            
            // Get API key for the service
            String apiKey = SecureAPIKeyManager.getAPIKey(serviceType);
            if (apiKey == null || apiKey.trim().isEmpty()) {
                throw new IllegalStateException("No API key configured for service: " + serviceType);
            }
            
            // Generate prompt using the prompt service
            String prompt = promptService.generateDetailedPrompt(failureInfo);
            
            LOG.info("Delegating analysis to " + provider.getDisplayName() + 
                    " provider with model: " + defaultModel.getModelId());
            
            // Delegate to the provider
            return provider.analyze(prompt, defaultModel.getModelId(), apiKey)
                    .thenApply(result -> {
                        LOG.info("Analysis completed successfully by " + 
                                provider.getDisplayName() + " in " + 
                                result.getProcessingTimeMs() + "ms");
                        return result;
                    })
                    .exceptionally(throwable -> {
                        LOG.error("Analysis failed", throwable);
                        throw new RuntimeException("AI analysis failed: " + throwable.getMessage(), throwable);
                    });
            
        } catch (Exception e) {
            LOG.error("Failed to start AI analysis", e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * Validates connection to a specific AI service.
     * 
     * <p>This method tests the connection to the specified AI service using
     * the appropriate provider and API key.</p>
     * 
     * @param serviceType the service type to validate
     * @return a CompletableFuture containing true if connection is valid, false otherwise
     * @throws IllegalArgumentException if serviceType is null
     */
    public CompletableFuture<Boolean> validateConnection(@NotNull AIServiceType serviceType) {
        if (serviceType == null) {
            throw new IllegalArgumentException("Service type cannot be null");
        }
        
        LOG.info("Validating connection to " + serviceType.getDisplayName());
        
        try {
            // Get the provider for this service
            AIServiceProvider provider = AIServiceFactory.getProvider(serviceType);
            if (provider == null) {
                LOG.warn("No provider available for service: " + serviceType);
                return CompletableFuture.completedFuture(false);
            }
            
            // Get API key for the service
            String apiKey = SecureAPIKeyManager.getAPIKey(serviceType);
            if (apiKey == null || apiKey.trim().isEmpty()) {
                LOG.warn("No API key configured for service: " + serviceType);
                return CompletableFuture.completedFuture(false);
            }
            
            // Delegate validation to the provider
            return provider.validateConnection(apiKey)
                    .thenApply(isValid -> {
                        if (isValid) {
                            LOG.info("Connection validation successful for " + serviceType.getDisplayName());
                        } else {
                            LOG.warn("Connection validation failed for " + serviceType.getDisplayName());
                        }
                        return isValid;
                    })
                    .exceptionally(throwable -> {
                        LOG.error("Connection validation failed for " + serviceType.getDisplayName(), throwable);
                        return false;
                    });
            
        } catch (Exception e) {
            LOG.error("Failed to validate connection to " + serviceType.getDisplayName(), e);
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * Checks if a service type has a registered provider.
     * 
     * @param serviceType the service type to check
     * @return true if a provider is registered, false otherwise
     */
    public boolean hasProvider(@NotNull AIServiceType serviceType) {
        return AIServiceFactory.hasProvider(serviceType);
    }
    
    /**
     * Gets all registered service types.
     * 
     * @return array of registered service types
     */
    public AIServiceType[] getRegisteredServiceTypes() {
        return AIServiceFactory.getRegisteredServiceTypes();
    }
    
    /**
     * Shuts down the service and cleans up resources.
     * 
     * <p>This method is called when the service is no longer needed.
     * It performs any necessary cleanup operations.</p>
     */
    public void shutdown() {
        LOG.info("Shutting down AINetworkService");
        // No specific cleanup needed for the new architecture
        // HTTP client is shared and managed by the factory
    }
} 