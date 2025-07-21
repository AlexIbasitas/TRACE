package com.triagemate.services;

import com.triagemate.models.AIAnalysisResult;
import com.triagemate.settings.AIServiceType;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for AI service providers.
 * 
 * <p>This interface standardizes the interaction with different AI services
 * while allowing service-specific implementation details. It provides a clean
 * abstraction layer that separates model management from service-specific
 * connection logic.</p>
 * 
 * <p>Key responsibilities:</p>
 * <ul>
 *   <li>Validate connections to AI services</li>
 *   <li>Analyze prompts using specific models</li>
 *   <li>Provide available models for the service</li>
 *   <li>Handle service-specific error scenarios</li>
 * </ul>
 * 
 * <p>This interface enables:</p>
 * <ul>
 *   <li>Clean separation of concerns between model management and service connection</li>
 *   <li>Easy addition of new AI services (Claude, local models, etc.)</li>
 *   <li>Service-specific logic isolation for better testability</li>
 *   <li>Standardized interface across all AI providers</li>
 * </ul>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
public interface AIServiceProvider {
    
    /**
     * Validates the connection to the AI service.
     * 
     * <p>This method tests the API key and service availability by making
     * a simple test request. It should handle service-specific validation
     * logic and error scenarios.</p>
     * 
     * @param apiKey the API key to validate
     * @return true if connection is valid, false otherwise
     * @throws IllegalArgumentException if apiKey is null or empty
     */
    CompletableFuture<Boolean> validateConnection(@NotNull String apiKey);
    
    /**
     * Analyzes a prompt using the specified model.
     * 
     * <p>This method handles the complete analysis workflow for a specific
     * AI service, including request building, HTTP communication, response
     * parsing, and error handling.</p>
     * 
     * @param prompt the prompt to analyze
     * @param modelId the model ID to use for analysis
     * @param apiKey the API key for authentication
     * @return the analysis result with metadata
     * @throws IllegalArgumentException if any parameter is null or empty
     * @throws RuntimeException if the service is unavailable or returns an error
     */
    CompletableFuture<AIAnalysisResult> analyze(@NotNull String prompt, 
                                               @NotNull String modelId, 
                                               @NotNull String apiKey);
    
    /**
     * Gets the available models for this service.
     * 
     * <p>Returns an array of model IDs that are supported by this service.
     * These models can be used in the analyze method.</p>
     * 
     * @return array of available model IDs
     */
    String[] getAvailableModels();
    
    /**
     * Gets the service type this provider handles.
     * 
     * <p>This method identifies which AI service this provider implements,
     * allowing the factory to properly route requests.</p>
     * 
     * @return the service type
     */
    AIServiceType getServiceType();
    
    /**
     * Gets the display name for this service provider.
     * 
     * <p>This method provides a user-friendly name for the service that
     * can be displayed in UI components.</p>
     * 
     * @return the display name
     */
    String getDisplayName();
} 