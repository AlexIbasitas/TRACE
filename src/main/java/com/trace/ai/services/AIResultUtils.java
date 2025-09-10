package com.trace.ai.services;

import com.trace.ai.configuration.AIServiceType;
import com.trace.ai.models.AIAnalysisResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class for creating standardized AIAnalysisResult objects.
 * 
 * <p>This class provides stateless factory methods for creating AIAnalysisResult
 * objects with consistent formatting and error handling patterns used throughout
 * the AI analysis system.</p>
 * 
 * <p>Key responsibilities:</p>
 * <ul>
 *   <li>Creating success results with analysis and prompt data</li>
 *   <li>Creating error results with standardized error messages</li>
 *   <li>Creating disabled results when AI features are turned off</li>
 *   <li>Providing consistent result formatting across the system</li>
 * </ul>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
public final class AIResultUtils {
    
    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private AIResultUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * Creates a successful AI analysis result with analysis and prompt data.
     *
     * @param analysis the AI analysis text
     * @param prompt the prompt that was sent to the AI
     * @param serviceType the AI service type used
     * @param modelId the model identifier
     * @param processingTimeMs the processing time in milliseconds
     * @return a successful AIAnalysisResult
     * @throws IllegalArgumentException if analysis is null
     */
    @NotNull
    public static AIAnalysisResult createSuccessResult(@NotNull String analysis, 
                                                      @Nullable String prompt,
                                                      @NotNull AIServiceType serviceType,
                                                      @NotNull String modelId,
                                                      long processingTimeMs) {
        if (analysis == null) {
            throw new IllegalArgumentException("Analysis cannot be null");
        }
        if (serviceType == null) {
            throw new IllegalArgumentException("ServiceType cannot be null");
        }
        if (modelId == null) {
            throw new IllegalArgumentException("ModelId cannot be null");
        }
        
        return new AIAnalysisResult(
            analysis,
            prompt,
            serviceType,
            modelId,
            System.currentTimeMillis(),
            processingTimeMs
        );
    }
    
    /**
     * Creates an error result for AI analysis failures.
     *
     * @param errorMessage the error message describing what went wrong
     * @param serviceType the AI service type that was attempted
     * @return an error AIAnalysisResult
     * @throws IllegalArgumentException if errorMessage is null or empty
     */
    @NotNull
    public static AIAnalysisResult createErrorResult(@NotNull String errorMessage, 
                                                    @NotNull AIServiceType serviceType) {
        if (errorMessage == null || errorMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("ErrorMessage cannot be null or empty");
        }
        if (serviceType == null) {
            throw new IllegalArgumentException("ServiceType cannot be null");
        }
        
        return new AIAnalysisResult(
            "Analysis failed: " + errorMessage,
            serviceType,
            "error",
            System.currentTimeMillis(),
            0L
        );
    }
    
    /**
     * Creates an error result for AI analysis failures with a throwable.
     *
     * @param throwable the exception that caused the failure
     * @param serviceType the AI service type that was attempted
     * @return an error AIAnalysisResult
     * @throws IllegalArgumentException if throwable is null
     */
    @NotNull
    public static AIAnalysisResult createErrorResult(@NotNull Throwable throwable, 
                                                    @NotNull AIServiceType serviceType) {
        if (throwable == null) {
            throw new IllegalArgumentException("Throwable cannot be null");
        }
        if (serviceType == null) {
            throw new IllegalArgumentException("ServiceType cannot be null");
        }
        
        return createErrorResult(throwable.getMessage(), serviceType);
    }
    
    /**
     * Creates a disabled result when AI features are turned off.
     *
     * @param message the message explaining why the feature is disabled
     * @param serviceType the AI service type that would have been used
     * @return a disabled AIAnalysisResult
     * @throws IllegalArgumentException if message is null or empty
     */
    @NotNull
    public static AIAnalysisResult createDisabledResult(@NotNull String message, 
                                                       @NotNull AIServiceType serviceType) {
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Message cannot be null or empty");
        }
        if (serviceType == null) {
            throw new IllegalArgumentException("ServiceType cannot be null");
        }
        
        return new AIAnalysisResult(
            message,
            serviceType,
            "disabled",
            System.currentTimeMillis(),
            0L
        );
    }
    
    /**
     * Creates a prompt-only result when AI analysis is disabled but prompt preview is enabled.
     *
     * @param prompt the prompt that would have been sent to the AI
     * @param serviceType the AI service type that would have been used
     * @return a prompt-only AIAnalysisResult
     * @throws IllegalArgumentException if prompt is null
     */
    @NotNull
    public static AIAnalysisResult createPromptOnlyResult(@NotNull String prompt, 
                                                         @NotNull AIServiceType serviceType) {
        if (prompt == null) {
            throw new IllegalArgumentException("Prompt cannot be null");
        }
        if (serviceType == null) {
            throw new IllegalArgumentException("ServiceType cannot be null");
        }
        
        return new AIAnalysisResult(
            "Prompt preview only. AI Analysis is disabled.",
            prompt,
            serviceType,
            "disabled",
            System.currentTimeMillis(),
            0L
        );
    }
    
    /**
     * Creates a TRACE disabled result when the TRACE feature is turned off.
     *
     * @param serviceType the AI service type that would have been used
     * @return a TRACE disabled AIAnalysisResult
     * @throws IllegalArgumentException if serviceType is null
     */
    @NotNull
    public static AIAnalysisResult createTraceDisabledResult(@NotNull AIServiceType serviceType) {
        if (serviceType == null) {
            throw new IllegalArgumentException("ServiceType cannot be null");
        }
        
        return createDisabledResult(
            "TRACE is OFF. Turn TRACE ON to enable context extraction and AI features.",
            serviceType
        );
    }
    
    /**
     * Creates a fallback result when document retrieval fails.
     *
     * @param originalError the original error that occurred
     * @param serviceType the AI service type that was attempted
     * @return a fallback AIAnalysisResult
     * @throws IllegalArgumentException if originalError is null
     */
    @NotNull
    public static AIAnalysisResult createFallbackResult(@NotNull String originalError, 
                                                       @NotNull AIServiceType serviceType) {
        if (originalError == null || originalError.trim().isEmpty()) {
            throw new IllegalArgumentException("OriginalError cannot be null or empty");
        }
        if (serviceType == null) {
            throw new IllegalArgumentException("ServiceType cannot be null");
        }
        
        return new AIAnalysisResult(
            "Document retrieval failed, falling back to basic analysis: " + originalError,
            serviceType,
            "fallback",
            System.currentTimeMillis(),
            0L
        );
    }
}
