package com.trace.ai.services;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.trace.ai.models.AIAnalysisResult;
import com.trace.ai.models.AIModel;
import com.trace.ai.services.providers.AIServiceProvider;
import com.trace.ai.services.AIServiceFactory;
import com.trace.ai.services.AIModelService;
import com.trace.test.models.FailureInfo;
import com.trace.security.SecureAPIKeyManager;
import com.trace.ai.configuration.AIServiceType;
import com.trace.ai.configuration.AISettings;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Pure network layer that handles HTTP requests/responses for AI analysis.
 * 
 * <p>This service focuses solely on network communication with AI providers.
 * It has no knowledge of prompt composition or orchestration - it simply
 * sends prompts to AI services and returns responses.</p>
 * 
 * <p>Key responsibilities:</p>
 * <ul>
 *   <li>HTTP request/response handling</li>
 *   <li>API authentication & key management</li>
 *   <li>Connection validation</li>
 *   <li>Error handling for network issues</li>
 *   <li>Response parsing</li>
 * </ul>
 * 
 * <p>This service does NOT:</p>
 * <ul>
 *   <li>Compose or orchestrate prompts</li>
 *   <li>Handle business logic routing</li>
 *   <li>Manage prompt services</li>
 *   <li>Coordinate between different prompt types</li>
 * </ul>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
public final class AIRequestHandler {
    
    private static final Logger LOG = Logger.getInstance(AIRequestHandler.class);
    
    // Services
    private final Project project;
    
    /**
     * Constructor for AIRequestHandler.
     * 
     * @param project The IntelliJ project context
     * @throws NullPointerException if project is null
     */
    public AIRequestHandler(@NotNull Project project) {
        if (project == null) {
            throw new NullPointerException("Project cannot be null");
        }
        
        this.project = project;
    }
    
    /**
     * Sends a prompt to the AI service for analysis.
     * 
     * <p>This method handles the pure network communication with AI providers.
     * It takes a pre-composed prompt and sends it to the appropriate AI service.</p>
     * 
     * @param prompt the pre-composed prompt to send
     * @param analysisMode the analysis mode ("Quick Overview" or "Full Analysis")
     * @return a CompletableFuture containing the analysis result
     * @throws IllegalArgumentException if prompt is null or empty
     * @throws IllegalStateException if no AI service is properly configured
     */
    public CompletableFuture<AIAnalysisResult> sendRequest(@NotNull String prompt, @NotNull String analysisMode) {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new IllegalArgumentException("Prompt cannot be null or empty");
        }
        
        // Gate on AI Analysis (auto analyze) - do not check TRACE power here
        AISettings aiSettings = AISettings.getInstance();
        if (!aiSettings.isAutoAnalyzeEnabled()) {
            LOG.info("AI analysis is disabled (auto-analyze OFF) - skipping network call");
            return CompletableFuture.completedFuture(
                new AIAnalysisResult(
                    "AI analysis is disabled. Enable AI Analysis to run model calls.",
                    AIServiceType.OPENAI,
                    "disabled",
                    System.currentTimeMillis(),
                    0L
                )
            );
        }

        LOG.info("Sending AI analysis request with mode: " + analysisMode);
        
        try {
            // Route by selected default model's service type
            AIModelService modelService = AIModelService.getInstance();
            AIModel defaultModel = modelService.getDefaultModel();
            if (defaultModel == null) {
                LOG.warn("No default model available");
                return CompletableFuture.completedFuture(
                    new AIAnalysisResult(
                        "No default model is configured. Set a default model in Settings.",
                        AIServiceType.OPENAI,
                        "disabled",
                        System.currentTimeMillis(),
                        0L
                    )
                );
            }

            final AIServiceType serviceType = defaultModel.getServiceType();
            // Validate API key for routed service
            String apiKey = SecureAPIKeyManager.getAPIKey(serviceType);
            if (apiKey == null || apiKey.trim().isEmpty()) {
                String errorMessage = "API key not configured for " + serviceType.name() + ". Please configure your API key in the settings.";
                LOG.warn(errorMessage);
                return CompletableFuture.completedFuture(
                    new AIAnalysisResult(
                        errorMessage,
                        serviceType,
                        "error",
                        System.currentTimeMillis(),
                        0L
                    )
                );
            }
            
            // Get the provider for this service
            AIServiceProvider provider = AIServiceFactory.getProvider(serviceType);
            if (provider == null) {
                LOG.error("No provider available for service: " + serviceType);
                return CompletableFuture.completedFuture(
                    new AIAnalysisResult(
                        "No AI provider available for " + serviceType.name(),
                        serviceType,
                        "error",
                        System.currentTimeMillis(),
                        0L
                    )
                );
            }
            
            // Send the request to the AI service provider
            return provider.analyze(prompt, defaultModel.getModelId(), apiKey)
                .thenApply(result -> {
                    LOG.info("AI analysis completed successfully");
                    return result;
                })
                .exceptionally(throwable -> {
                    LOG.error("AI analysis failed", throwable);
                    return new AIAnalysisResult(
                        "AI analysis failed: " + throwable.getMessage(),
                        serviceType,
                        "error",
                        System.currentTimeMillis(),
                        0L
                    );
                });
                
        } catch (Exception e) {
            LOG.error("Unexpected error during AI analysis", e);
            return CompletableFuture.completedFuture(
                new AIAnalysisResult(
                    "Unexpected error during AI analysis: " + e.getMessage(),
                    AIServiceType.OPENAI,
                    "error",
                    System.currentTimeMillis(),
                    0L
                )
            );
        }
    }
    
    /**
     * Validates the connection to a specific AI service.
     * 
     * @param serviceType the AI service type to validate
     * @return a CompletableFuture containing the validation result
     */
    public CompletableFuture<Boolean> validateConnection(@NotNull AIServiceType serviceType) {
        if (serviceType == null) {
            return CompletableFuture.completedFuture(false);
        }
        
        LOG.info("Validating connection to " + serviceType.name());
        
        try {
            // Check if API key is configured
            String apiKey = SecureAPIKeyManager.getAPIKey(serviceType);
            if (apiKey == null || apiKey.trim().isEmpty()) {
                LOG.warn("No API key configured for " + serviceType.name());
                return CompletableFuture.completedFuture(false);
            }
            
            // Get the provider for this service
            AIServiceProvider provider = AIServiceFactory.getProvider(serviceType);
            if (provider == null) {
                LOG.warn("No provider available for service: " + serviceType);
                return CompletableFuture.completedFuture(false);
            }
            
            // Validate with the service provider
            return provider.validateConnection(apiKey)
                .thenApply(result -> {
                    LOG.info("Connection validation result for " + serviceType.name() + ": " + result);
                    return result;
                })
                .exceptionally(throwable -> {
                    LOG.error("Connection validation failed for " + serviceType.name(), throwable);
                    return false;
                });
                
        } catch (Exception e) {
            LOG.error("Unexpected error during connection validation", e);
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * Checks if a specific AI service provider is available.
     * 
     * @param serviceType the AI service type to check
     * @return true if the provider is available, false otherwise
     */
    public boolean hasProvider(@NotNull AIServiceType serviceType) {
        if (serviceType == null) {
            return false;
        }
        
        return AIServiceFactory.hasProvider(serviceType);
    }
    
    /**
     * Gets all registered AI service types.
     * 
     * @return array of registered service types
     */
    public AIServiceType[] getRegisteredServiceTypes() {
        return AIServiceFactory.getRegisteredServiceTypes();
    }
    
    /**
     * Shuts down the AI request handler and cleans up resources.
     */
    public void shutdown() {
        LOG.info("Shutting down AIRequestHandler");
        try {
            // No specific cleanup needed for the new architecture
            // HTTP client is shared and managed by the factory
            LOG.info("AIRequestHandler shutdown completed");
        } catch (Exception e) {
            LOG.error("Error during AIRequestHandler shutdown", e);
        }
    }
} 