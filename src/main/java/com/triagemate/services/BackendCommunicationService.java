package com.triagemate.services;

import com.intellij.openapi.components.Service;
import com.triagemate.models.FailureInfo;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.application.ApplicationManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Service for communicating with the TriageMate backend API.
 * 
 * <p>This service handles sending FailureInfo data to the local Spring Boot backend
 * for AI analysis and custom rule processing. It provides both synchronous and
 * asynchronous communication methods with proper error handling and retry logic.</p>
 */
@Service
public final class BackendCommunicationService {
    private static final Logger LOG = Logger.getInstance(BackendCommunicationService.class);
    
    // Backend configuration
    private static final String DEFAULT_BACKEND_URL = "http://localhost:8080";
    private static final String API_BASE_PATH = "/api/v1";
    private static final String ANALYZE_ENDPOINT = "/analyze";
    
    // HTTP client configuration
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final int MAX_RETRIES = 3;
    private static final Duration RETRY_DELAY = Duration.ofSeconds(1);
    
    private final HttpClient httpClient;
    private final Gson gson;
    private String backendUrl;
    
    public BackendCommunicationService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .build();
        
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
        
        this.backendUrl = DEFAULT_BACKEND_URL;
    }
    
    /**
     * Sends failure information to the backend for AI analysis.
     * 
     * @param failureInfo The failure information to analyze
     * @return AnalysisResponse containing AI analysis results
     * @throws BackendCommunicationException if communication fails
     */
    public AnalysisResponse analyzeFailure(FailureInfo failureInfo) throws BackendCommunicationException {
        try {
            String requestBody = gson.toJson(failureInfo);
            String url = backendUrl + API_BASE_PATH + ANALYZE_ENDPOINT;
            
            LOG.debug("Sending failure analysis request to backend - URL: " + url + 
                    ", Scenario: " + failureInfo.getScenarioName());
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(REQUEST_TIMEOUT)
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                AnalysisResponse analysisResponse = gson.fromJson(response.body(), AnalysisResponse.class);
                            LOG.info("Successfully received analysis from backend - Model: " + 
                    analysisResponse.getModelUsed() + ", Processing time: " + 
                    analysisResponse.getProcessingTime() + "ms");
                return analysisResponse;
            } else {
                throw new BackendCommunicationException(
                    "Backend returned error status: " + response.statusCode() + 
                    ", body: " + response.body());
            }
            
        } catch (BackendCommunicationException e) {
            // Re-throw BackendCommunicationException without wrapping
            throw e;
        } catch (Exception e) {
            LOG.warn("Failed to communicate with backend", e);
            throw new BackendCommunicationException("Failed to communicate with backend", e);
        }
    }
    
    /**
     * Sends failure information to the backend for AI analysis asynchronously.
     * 
     * @param failureInfo The failure information to analyze
     * @return CompletableFuture containing the analysis response
     */
    public CompletableFuture<AnalysisResponse> analyzeFailureAsync(FailureInfo failureInfo) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return analyzeFailure(failureInfo);
            } catch (BackendCommunicationException e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * Checks if the backend is available and responding.
     * 
     * @return true if backend is available, false otherwise
     */
    public boolean isBackendAvailable() {
        try {
            String healthUrl = backendUrl + API_BASE_PATH + "/health";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(healthUrl))
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
            
        } catch (Exception e) {
            LOG.debug("Backend health check failed", e);
            return false;
        }
    }
    
    /**
     * Sets the backend URL for communication.
     * 
     * @param backendUrl The backend URL (e.g., "http://localhost:8080")
     */
    public void setBackendUrl(String backendUrl) {
        this.backendUrl = backendUrl;
        LOG.info("Backend URL updated to: " + backendUrl);
    }
    
    /**
     * Gets the current backend URL.
     * 
     * @return The current backend URL
     */
    public String getBackendUrl() {
        return backendUrl;
    }
    
    /**
     * Response model for backend analysis results.
     */
    public static class AnalysisResponse {
        private String analysis;
        private java.util.List<String> suggestions;
        private String confidence;
        private String modelUsed;
        private long processingTime;
        private String promptUsed;
        
        // Getters
        public String getAnalysis() { return analysis; }
        public java.util.List<String> getSuggestions() { return suggestions; }
        public String getConfidence() { return confidence; }
        public String getModelUsed() { return modelUsed; }
        public long getProcessingTime() { return processingTime; }
        public String getPromptUsed() { return promptUsed; }
        
        // Setters for JSON deserialization
        public void setAnalysis(String analysis) { this.analysis = analysis; }
        public void setSuggestions(java.util.List<String> suggestions) { this.suggestions = suggestions; }
        public void setConfidence(String confidence) { this.confidence = confidence; }
        public void setModelUsed(String modelUsed) { this.modelUsed = modelUsed; }
        public void setProcessingTime(long processingTime) { this.processingTime = processingTime; }
        public void setPromptUsed(String promptUsed) { this.promptUsed = promptUsed; }
    }
    
    /**
     * Exception thrown when backend communication fails.
     */
    public static class BackendCommunicationException extends Exception {
        public BackendCommunicationException(String message) {
            super(message);
        }
        
        public BackendCommunicationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
} 