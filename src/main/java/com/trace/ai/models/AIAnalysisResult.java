package com.trace.ai.models;

import com.trace.ai.configuration.AIServiceType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Result of AI analysis with metadata.
 * 
 * <p>This class encapsulates the result of an AI analysis operation,
 * including the analysis text, the prompt that was sent to the AI,
 * and service metadata. The confidence level is expected to be 
 * included in the analysis text itself.</p>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
public final class AIAnalysisResult {
    
    private final String analysis;
    private final String prompt;
    private final AIServiceType serviceType;
    private final String modelId;
    private final long timestamp;
    private final long processingTimeMs;
    
    /**
     * Constructor for AIAnalysisResult.
     * 
     * @param analysis the analysis text from the AI service
     * @param serviceType the AI service type used
     * @param modelId the specific model ID used
     * @param timestamp the timestamp when analysis was performed
     * @param processingTimeMs the processing time in milliseconds
     */
    public AIAnalysisResult(@NotNull String analysis,
                           @NotNull AIServiceType serviceType,
                           @NotNull String modelId,
                           long timestamp,
                           long processingTimeMs) {
        this(analysis, null, serviceType, modelId, timestamp, processingTimeMs);
    }
    
    /**
     * Constructor for AIAnalysisResult with prompt.
     * 
     * @param analysis the analysis text from the AI service
     * @param prompt the prompt that was sent to the AI (can be null)
     * @param serviceType the AI service type used
     * @param modelId the specific model ID used
     * @param timestamp the timestamp when analysis was performed
     * @param processingTimeMs the processing time in milliseconds
     */
    public AIAnalysisResult(@NotNull String analysis,
                           @Nullable String prompt,
                           @NotNull AIServiceType serviceType,
                           @NotNull String modelId,
                           long timestamp,
                           long processingTimeMs) {
        this.analysis = analysis;
        this.prompt = prompt;
        this.serviceType = serviceType;
        this.modelId = modelId;
        this.timestamp = timestamp;
        this.processingTimeMs = processingTimeMs;
    }
    
    /**
     * Gets the analysis text from the AI service.
     * 
     * @return the analysis text
     */
    public String getAnalysis() {
        return analysis;
    }
    
    /**
     * Gets the prompt that was sent to the AI service.
     * 
     * @return the prompt, or null if not available
     */
    public String getPrompt() {
        return prompt;
    }
    
    /**
     * Checks if this result has a prompt available for display.
     * 
     * @return true if a prompt is available, false otherwise
     */
    public boolean hasPrompt() {
        return prompt != null && !prompt.trim().isEmpty();
    }
    
    /**
     * Gets the AI service type used for analysis.
     * 
     * @return the service type
     */
    public AIServiceType getServiceType() {
        return serviceType;
    }
    
    /**
     * Gets the specific model ID used for analysis.
     * 
     * @return the model ID
     */
    public String getModelId() {
        return modelId;
    }
    
    /**
     * Gets the timestamp when analysis was performed.
     * 
     * @return the timestamp in milliseconds since epoch
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * Gets the processing time in milliseconds.
     * 
     * @return the processing time
     */
    public long getProcessingTimeMs() {
        return processingTimeMs;
    }
    
    /**
     * Gets a user-friendly description of the result.
     * 
     * @return a formatted description
     */
    public String getDescription() {
        return String.format("Analysis by %s (%s) - %dms",
                serviceType.getDisplayName(),
                modelId,
                processingTimeMs);
    }
    
    @Override
    public String toString() {
        return "AIAnalysisResult{" +
                "analysis='" + (analysis != null ? analysis.substring(0, Math.min(analysis.length(), 50)) + "..." : "null") + '\'' +
                ", hasPrompt=" + hasPrompt() +
                ", serviceType=" + serviceType +
                ", modelId='" + modelId + '\'' +
                ", timestamp=" + timestamp +
                ", processingTimeMs=" + processingTimeMs +
                '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        AIAnalysisResult that = (AIAnalysisResult) obj;
        
        if (timestamp != that.timestamp) return false;
        if (processingTimeMs != that.processingTimeMs) return false;
        if (!analysis.equals(that.analysis)) return false;
        if (prompt != null ? !prompt.equals(that.prompt) : that.prompt != null) return false;
        if (serviceType != that.serviceType) return false;
        return modelId.equals(that.modelId);
    }
    
    @Override
    public int hashCode() {
        int result = analysis.hashCode();
        result = 31 * result + (prompt != null ? prompt.hashCode() : 0);
        result = 31 * result + serviceType.hashCode();
        result = 31 * result + modelId.hashCode();
        result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
        result = 31 * result + (int) (processingTimeMs ^ (processingTimeMs >>> 32));
        return result;
    }
} 