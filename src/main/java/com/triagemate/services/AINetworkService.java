package com.triagemate.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.triagemate.settings.AIServiceType;
import com.triagemate.models.FailureInfo;
import com.triagemate.security.SecureAPIKeyManager;
import com.triagemate.settings.AISettings;
import com.triagemate.services.PromptGenerationService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Network service for AI analysis using cloud AI providers.
 * 
 * <p>This service provides asynchronous communication with AI services (OpenAI, Google Gemini)
 * for analyzing test failures and providing intelligent debugging suggestions.</p>
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>Asynchronous HTTP communication with configurable timeouts</li>
 *   <li>Support for multiple AI providers (OpenAI, Google Gemini)</li>
 *   <li>Secure API key management integration</li>
 *   <li>Integration with LocalPromptGenerationService for consistent prompt generation</li>
 *   <li>Comprehensive error handling and logging</li>
 *   <li>JSON request/response parsing</li>
 *   <li>Thread-safe operations</li>
 * </ul>
 * 
 * <p>This service follows IntelliJ Platform best practices for:</p>
 * <ul>
 *   <li>Asynchronous operations to avoid blocking the UI thread</li>
 *   <li>Proper error handling and user feedback</li>
 *   <li>Secure credential management</li>
 *   <li>Comprehensive logging for debugging</li>
 * </ul>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
public final class AINetworkService {
    
    private static final Logger LOG = Logger.getInstance(AINetworkService.class);
    
    // HTTP Configuration
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final int MAX_RETRIES = 3;
    private static final Duration RETRY_DELAY = Duration.ofSeconds(2);
    
    // OpenAI API Configuration
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String OPENAI_MODEL = "gpt-3.5-turbo";
    private static final int OPENAI_MAX_TOKENS = 2000;
    private static final double OPENAI_TEMPERATURE = 0.3;
    
    // Google Gemini API Configuration
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent";
    private static final int GEMINI_MAX_TOKENS = 2000;
    private static final double GEMINI_TEMPERATURE = 0.3;
    
    // JSON Processing
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    
    // Thread Management
    private final ExecutorService executorService;
    private final HttpClient httpClient;
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
        this.executorService = Executors.newCachedThreadPool();
        this.httpClient = createHttpClient();
        this.promptService = new LocalPromptGenerationService();
        
        LOG.debug("AINetworkService initialized for project: " + project.getName());
    }
    
    /**
     * Creates and configures the HTTP client with appropriate timeouts and settings.
     * 
     * @return Configured HttpClient instance
     */
    private HttpClient createHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
    }
    
    /**
     * Analyzes a test failure using AI services.
     * 
     * <p>This method:</p>
     * <ul>
     *   <li>Generates a detailed prompt using LocalPromptGenerationService</li>
     *   <li>Determines the configured AI service</li>
     *   <li>Makes an asynchronous API call</li>
     *   <li>Parses and returns the AI response</li>
     * </ul>
     * 
     * @param failureInfo The test failure information to analyze
     * @return CompletableFuture containing the AI analysis result, or null if analysis fails
     * @throws IllegalArgumentException if failureInfo is null
     */
    public CompletableFuture<AIAnalysisResult> analyze(@NotNull FailureInfo failureInfo) {
        if (failureInfo == null) {
            throw new IllegalArgumentException("FailureInfo cannot be null");
        }
        
        LOG.info("Starting AI analysis for failure: " + failureInfo.getErrorMessage());
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get the configured AI service
                AIServiceType serviceType = getConfiguredAIService();
                if (serviceType == null) {
                    LOG.warn("No AI service configured for analysis");
                    return null;
                }
                
                // Build the prompt using the existing prompt generation service
                String prompt = promptService.generateDetailedPrompt(failureInfo);
                if (prompt == null || prompt.trim().isEmpty()) {
                    LOG.warn("Failed to build prompt for analysis");
                    return null;
                }
                
                // Make the API call based on service type
                String response;
                switch (serviceType) {
                    case OPENAI:
                        response = callOpenAI(prompt);
                        break;
                    case GEMINI:
                        response = callGemini(prompt);
                        break;
                    default:
                        LOG.warn("Unsupported AI service type: " + serviceType);
                        return null;
                }
                
                if (response == null || response.trim().isEmpty()) {
                    LOG.warn("Empty response from AI service");
                    return null;
                }
                
                // Parse the response
                return parseAIResponse(response, serviceType);
                
            } catch (Exception e) {
                LOG.error("Error during AI analysis", e);
                return null;
            }
        }, executorService);
    }
    
    /**
     * Makes an API call to OpenAI's chat completion endpoint.
     * 
     * @param prompt The prompt to send to OpenAI
     * @return The AI response, or null if the call fails
     */
    private String callOpenAI(@NotNull String prompt) {
        try {
            // Get API key
            String apiKey = SecureAPIKeyManager.getAPIKey(AIServiceType.OPENAI);
            if (apiKey == null || apiKey.trim().isEmpty()) {
                LOG.warn("OpenAI API key not configured");
                return null;
            }
            
            // Build request body
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", OPENAI_MODEL);
            requestBody.addProperty("max_tokens", OPENAI_MAX_TOKENS);
            requestBody.addProperty("temperature", OPENAI_TEMPERATURE);
            
            JsonObject message = new JsonObject();
            message.addProperty("role", "user");
            message.addProperty("content", prompt);
            
            JsonObject[] messages = {message};
            requestBody.add("messages", GSON.toJsonTree(messages));
            
            // Create HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OPENAI_API_URL))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(requestBody)))
                    .build();
            
            LOG.debug("Making OpenAI API request");
            
            // Execute request with retry logic
            return executeRequestWithRetry(request, "OpenAI");
            
        } catch (Exception e) {
            LOG.error("Error calling OpenAI API", e);
            return null;
        }
    }
    
    /**
     * Makes an API call to Google's Gemini endpoint.
     * 
     * @param prompt The prompt to send to Gemini
     * @return The AI response, or null if the call fails
     */
    private String callGemini(@NotNull String prompt) {
        try {
            // Get API key
            String apiKey = SecureAPIKeyManager.getAPIKey(AIServiceType.GEMINI);
            if (apiKey == null || apiKey.trim().isEmpty()) {
                LOG.warn("Gemini API key not configured");
                return null;
            }
            
            // Build request body
            JsonObject requestBody = new JsonObject();
            
            JsonObject content = new JsonObject();
            JsonObject[] parts = {new JsonObject()};
            parts[0].addProperty("text", prompt);
            content.add("parts", GSON.toJsonTree(parts));
            
            JsonObject[] contents = {content};
            requestBody.add("contents", GSON.toJsonTree(contents));
            
            // Add generation config
            JsonObject generationConfig = new JsonObject();
            generationConfig.addProperty("maxOutputTokens", GEMINI_MAX_TOKENS);
            generationConfig.addProperty("temperature", GEMINI_TEMPERATURE);
            requestBody.add("generationConfig", generationConfig);
            
            // Create HTTP request with API key in URL
            String urlWithKey = GEMINI_API_URL + "?key=" + apiKey;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlWithKey))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(requestBody)))
                    .build();
            
            LOG.debug("Making Gemini API request");
            
            // Execute request with retry logic
            return executeRequestWithRetry(request, "Gemini");
            
        } catch (Exception e) {
            LOG.error("Error calling Gemini API", e);
            return null;
        }
    }
    
    /**
     * Executes an HTTP request with retry logic.
     * 
     * @param request The HTTP request to execute
     * @param serviceName The name of the service for logging
     * @return The response body, or null if all retries fail
     */
    private String executeRequestWithRetry(@NotNull HttpRequest request, @NotNull String serviceName) {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                LOG.debug("Making " + serviceName + " API request (attempt " + attempt + "/" + MAX_RETRIES + ")");
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    LOG.debug(serviceName + " API request successful");
                    return response.body();
                } else {
                    LOG.warn(serviceName + " API request failed with status: " + response.statusCode() + 
                            ", body: " + response.body());
                    
                    // Don't retry on client errors (4xx)
                    if (response.statusCode() >= 400 && response.statusCode() < 500) {
                        break;
                    }
                }
                
            } catch (IOException | InterruptedException e) {
                lastException = e;
                LOG.warn(serviceName + " API request failed (attempt " + attempt + "/" + MAX_RETRIES + ")", e);
                
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            // Wait before retry (except on last attempt)
            if (attempt < MAX_RETRIES) {
                try {
                    Thread.sleep(RETRY_DELAY.toMillis());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        LOG.error("All " + serviceName + " API request attempts failed", lastException);
        return null;
    }
    

    
    /**
     * Parses the AI service response into a structured result.
     * 
     * @param response The raw API response
     * @param serviceType The AI service type used
     * @return Parsed AIAnalysisResult, or null if parsing fails
     */
    private AIAnalysisResult parseAIResponse(@NotNull String response, @NotNull AIServiceType serviceType) {
        try {
            String analysis = extractAnalysisFromResponse(response, serviceType);
            if (analysis == null || analysis.trim().isEmpty()) {
                LOG.warn("Could not extract analysis from " + serviceType + " response");
                return null;
            }
            
            return new AIAnalysisResult(
                    analysis,
                    serviceType,
                    System.currentTimeMillis(),
                    extractConfidenceLevel(analysis)
            );
            
        } catch (Exception e) {
            LOG.error("Error parsing " + serviceType + " response", e);
            return null;
        }
    }
    
    /**
     * Extracts the analysis text from the AI service response.
     * 
     * @param response The raw API response
     * @param serviceType The AI service type
     * @return The extracted analysis text
     */
    private String extractAnalysisFromResponse(@NotNull String response, @NotNull AIServiceType serviceType) {
        try {
            JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
            
            switch (serviceType) {
                case OPENAI:
                    return extractOpenAIResponse(jsonResponse);
                case GEMINI:
                    return extractGeminiResponse(jsonResponse);
                default:
                    LOG.warn("Unknown service type for response parsing: " + serviceType);
                    return null;
            }
            
        } catch (Exception e) {
            LOG.error("Error extracting analysis from " + serviceType + " response", e);
            return null;
        }
    }
    
    /**
     * Extracts analysis from OpenAI response format.
     */
    private String extractOpenAIResponse(@NotNull JsonObject jsonResponse) {
        try {
            if (jsonResponse.has("choices") && jsonResponse.get("choices").isJsonArray()) {
                var choices = jsonResponse.getAsJsonArray("choices");
                if (choices.size() > 0) {
                    var firstChoice = choices.get(0).getAsJsonObject();
                    if (firstChoice.has("message") && firstChoice.get("message").isJsonObject()) {
                        var message = firstChoice.getAsJsonObject("message");
                        if (message.has("content")) {
                            return message.get("content").getAsString();
                        }
                    }
                }
            }
            return null;
        } catch (Exception e) {
            LOG.error("Error extracting OpenAI response", e);
            return null;
        }
    }
    
    /**
     * Extracts analysis from Gemini response format.
     */
    private String extractGeminiResponse(@NotNull JsonObject jsonResponse) {
        try {
            if (jsonResponse.has("candidates") && jsonResponse.get("candidates").isJsonArray()) {
                var candidates = jsonResponse.getAsJsonArray("candidates");
                if (candidates.size() > 0) {
                    var firstCandidate = candidates.get(0).getAsJsonObject();
                    if (firstCandidate.has("content") && firstCandidate.get("content").isJsonObject()) {
                        var content = firstCandidate.getAsJsonObject("content");
                        if (content.has("parts") && content.get("parts").isJsonArray()) {
                            var parts = content.getAsJsonArray("parts");
                            if (parts.size() > 0) {
                                var firstPart = parts.get(0).getAsJsonObject();
                                if (firstPart.has("text")) {
                                    return firstPart.get("text").getAsString();
                                }
                            }
                        }
                    }
                }
            }
            return null;
        } catch (Exception e) {
            LOG.error("Error extracting Gemini response", e);
            return null;
        }
    }
    
    /**
     * Extracts confidence level from analysis text.
     */
    private ConfidenceLevel extractConfidenceLevel(@NotNull String analysis) {
        String lowerAnalysis = analysis.toLowerCase();
        
        if (lowerAnalysis.contains("high confidence") || lowerAnalysis.contains("very confident")) {
            return ConfidenceLevel.HIGH;
        } else if (lowerAnalysis.contains("medium confidence") || lowerAnalysis.contains("somewhat confident")) {
            return ConfidenceLevel.MEDIUM;
        } else if (lowerAnalysis.contains("low confidence") || lowerAnalysis.contains("uncertain")) {
            return ConfidenceLevel.LOW;
        } else {
            // Default to medium if no confidence level is explicitly stated
            return ConfidenceLevel.MEDIUM;
        }
    }
    
    /**
     * Gets the configured AI service type.
     * 
     * @return The configured AI service type, or null if not configured
     */
    private AIServiceType getConfiguredAIService() {
        try {
            // Get the preferred service from AISettings
            AISettings aiSettings = AISettings.getInstance();
            AIServiceType preferredService = aiSettings.getPreferredAIService();
            
            // Check if the preferred service has an API key configured
            if (SecureAPIKeyManager.hasAPIKey(preferredService)) {
                return preferredService;
            }
        } catch (Exception e) {
            // AISettings not available (e.g., in test environment)
            LOG.debug("AISettings not available, using fallback service selection");
        }
        
        // Fallback to any available service with a configured API key
        if (SecureAPIKeyManager.hasAPIKey(AIServiceType.OPENAI)) {
            return AIServiceType.OPENAI;
        } else if (SecureAPIKeyManager.hasAPIKey(AIServiceType.GEMINI)) {
            return AIServiceType.GEMINI;
        }
        
        return null;
    }
    
    /**
     * Shuts down the service and releases resources.
     * This method should be called when the service is no longer needed.
     */
    public void shutdown() {
        try {
            executorService.shutdown();
            LOG.debug("AINetworkService shutdown completed");
        } catch (Exception e) {
            LOG.warn("Error during AINetworkService shutdown", e);
        }
    }
    
    /**
     * Result of AI analysis containing the analysis text and metadata.
     */
    public static final class AIAnalysisResult {
        private final String analysis;
        private final AIServiceType serviceType;
        private final long timestamp;
        private final ConfidenceLevel confidenceLevel;
        
        public AIAnalysisResult(@NotNull String analysis, 
                              @NotNull AIServiceType serviceType, 
                              long timestamp, 
                              @NotNull ConfidenceLevel confidenceLevel) {
            this.analysis = analysis;
            this.serviceType = serviceType;
            this.timestamp = timestamp;
            this.confidenceLevel = confidenceLevel;
        }
        
        public String getAnalysis() { return analysis; }
        public AIServiceType getServiceType() { return serviceType; }
        public long getTimestamp() { return timestamp; }
        public ConfidenceLevel getConfidenceLevel() { return confidenceLevel; }
        
        @Override
        public String toString() {
            return "AIAnalysisResult{" +
                    "serviceType=" + serviceType +
                    ", confidenceLevel=" + confidenceLevel +
                    ", timestamp=" + timestamp +
                    ", analysisLength=" + (analysis != null ? analysis.length() : 0) +
                    '}';
        }
    }
    
    /**
     * Confidence levels for AI analysis results.
     */
    public enum ConfidenceLevel {
        HIGH, MEDIUM, LOW
    }
} 