package com.trace.ai.services.providers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.diagnostic.Logger;
import com.trace.ai.models.AIAnalysisResult;
import com.trace.ai.configuration.AIServiceType;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import com.google.gson.JsonArray;

/**
 * Google Gemini-specific AI service provider implementation.
 * 
 * <p>This provider handles all Google Gemini API communication, including request building,
 * HTTP communication, response parsing, and error handling specific to Gemini's API.</p>
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>Google Gemini GenerateContent API integration</li>
 *   <li>Support for multiple Gemini models (gemini-pro, gemini-pro-vision, etc.)</li>
 *   <li>Gemini-specific request and response formatting</li>
 *   <li>Comprehensive error handling for Gemini API errors</li>
 *   <li>Connection validation with Gemini service</li>
 * </ul>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
public class GeminiProvider implements AIServiceProvider {
    
    private static final Logger LOG = Logger.getInstance(GeminiProvider.class);
    
    // Google Gemini API Configuration
    private static final String API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final String API_ENDPOINT = ":generateContent";
    private static final int DEFAULT_MAX_TOKENS = 2000;
    private static final double DEFAULT_TEMPERATURE = 0.3;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    
    // JSON Processing
    private static final Gson GSON = new Gson();
    
    // HTTP client for making requests
    private final HttpClient httpClient;
    
    /**
     * Constructor for GeminiProvider.
     * 
     * @param httpClient the HTTP client to use for requests
     */
    public GeminiProvider(@NotNull HttpClient httpClient) {
        this.httpClient = httpClient;
    }
    
    @Override
    public CompletableFuture<Boolean> validateConnection(@NotNull String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Test with a simple prompt
                String testPrompt = "Hello";
                analyze(testPrompt, "gemini-pro", apiKey).get();
                LOG.info("Gemini connection validation successful");
                return true;
            } catch (Exception e) {
                LOG.warn("Gemini connection validation failed: " + e.getMessage());
                return false;
            }
        });
    }
    
    @Override
    public CompletableFuture<AIAnalysisResult> analyze(@NotNull String prompt, 
                                                      @NotNull String modelId, 
                                                      @NotNull String apiKey) {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new IllegalArgumentException("Prompt cannot be null or empty");
        }
        if (modelId == null || modelId.trim().isEmpty()) {
            throw new IllegalArgumentException("Model ID cannot be null or empty");
        }
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("API key cannot be null or empty");
        }
        
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                LOG.info("Starting Gemini analysis with model: " + modelId);
                
                // Build Gemini-specific request
                JsonObject request = buildGeminiRequest(prompt, modelId);
                
                // Make HTTP request
                String response = executeGeminiRequest(request, modelId, apiKey);
                
                // Parse Gemini-specific response
                AIAnalysisResult result = parseGeminiResponse(response, modelId, startTime);
                
                LOG.info("Gemini analysis completed in " + (System.currentTimeMillis() - startTime) + "ms");
                
                return result;
                
            } catch (Exception e) {
                LOG.error("Gemini analysis failed", e);
                throw new RuntimeException("Gemini analysis failed: " + e.getMessage(), e);
            }
        });
    }
    
    @Override
    public CompletableFuture<String[]> discoverAvailableModels(@NotNull String apiKey) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Query Gemini's models endpoint to get available models
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models?key=" + apiKey))
                    .GET()
                    .timeout(REQUEST_TIMEOUT)
                    .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
                    JsonArray models = jsonResponse.getAsJsonArray("models");
                    
                    List<String> availableModels = new ArrayList<>();
                    for (int i = 0; i < models.size(); i++) {
                        JsonObject model = models.get(i).getAsJsonObject();
                        String modelId = model.get("name").getAsString().replace("models/", "");
                        
                        // Check if model supports generateContent
                        if (model.has("supportedGenerationMethods")) {
                            JsonArray methods = model.getAsJsonArray("supportedGenerationMethods");
                            for (int j = 0; j < methods.size(); j++) {
                                if ("generateContent".equals(methods.get(j).getAsString())) {
                                    availableModels.add(modelId);
                                    break;
                                }
                            }
                        }
                    }
                    
                    LOG.info("Discovered " + availableModels.size() + " Gemini models");
                    return availableModels.toArray(new String[0]);
                } else {
                    LOG.warn("Failed to discover Gemini models: " + response.statusCode());
                    // Return default models as fallback
                    return new String[]{"gemini-1.5-flash", "gemini-1.5-pro"};
                }
            } catch (Exception e) {
                LOG.warn("Error discovering Gemini models: " + e.getMessage());
                // Return default models as fallback
                return new String[]{"gemini-1.5-flash", "gemini-1.5-pro"};
            }
        });
    }
    
    @Override
    public AIServiceType getServiceType() {
        return AIServiceType.GEMINI;
    }
    
    @Override
    public String getDisplayName() {
        return "Google Gemini";
    }
    
    /**
     * Builds a Gemini-specific request JSON.
     * 
     * @param prompt the prompt to analyze
     * @param modelId the model ID to use
     * @return the request JSON object
     */
    private JsonObject buildGeminiRequest(@NotNull String prompt, @NotNull String modelId) {
        JsonObject request = new JsonObject();
        
        // Add generation config
        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("maxOutputTokens", DEFAULT_MAX_TOKENS);
        generationConfig.addProperty("temperature", DEFAULT_TEMPERATURE);
        request.add("generationConfig", generationConfig);
        
        // Add content array
        JsonObject content = new JsonObject();
        content.addProperty("role", "user");
        
        JsonObject part = new JsonObject();
        part.addProperty("text", prompt);
        
        JsonObject partsArray = new JsonObject();
        partsArray.add("parts", GSON.toJsonTree(new JsonObject[]{part}));
        
        content.add("parts", partsArray.get("parts"));
        
        JsonObject contentsArray = new JsonObject();
        contentsArray.add("contents", GSON.toJsonTree(new JsonObject[]{content}));
        
        request.add("contents", contentsArray.get("contents"));
        
        return request;
    }
    
    /**
     * Executes an HTTP request to the Gemini API.
     * 
     * @param request the request JSON object
     * @param modelId the model ID to use
     * @param apiKey the API key for authentication
     * @return the response body as a string
     * @throws Exception if the request fails
     */
    private String executeGeminiRequest(@NotNull JsonObject request, 
                                      @NotNull String modelId, 
                                      @NotNull String apiKey) throws Exception {
        String requestBody = GSON.toJson(request);
        String url = API_BASE_URL + modelId + API_ENDPOINT + "?key=" + apiKey;
        
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(REQUEST_TIMEOUT)
                .build();
        
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("Gemini API request failed with status " + 
                    response.statusCode() + ": " + response.body());
        }
        
        return response.body();
    }
    
    /**
     * Parses a Gemini-specific response.
     * 
     * @param response the response body from Gemini
     * @param modelId the model ID used
     * @param startTime the start time for calculating processing time
     * @return the parsed analysis result
     */
    private AIAnalysisResult parseGeminiResponse(@NotNull String response, 
                                               @NotNull String modelId, 
                                               long startTime) {
        try {
            JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
            
            // Extract the analysis text
            String analysis = extractGeminiResponseText(jsonResponse);
            
            // Calculate processing time
            long processingTime = System.currentTimeMillis() - startTime;
            
            return new AIAnalysisResult(
                analysis,
                AIServiceType.GEMINI,
                modelId,
                System.currentTimeMillis(),
                processingTime
            );
            
        } catch (Exception e) {
            LOG.error("Failed to parse Gemini response", e);
            throw new RuntimeException("Failed to parse Gemini response: " + e.getMessage(), e);
        }
    }
    
    /**
     * Extracts the analysis text from Gemini response JSON.
     * 
     * @param jsonResponse the parsed JSON response
     * @return the analysis text
     */
    private String extractGeminiResponseText(@NotNull JsonObject jsonResponse) {
        try {
            // Navigate through the Gemini response structure
            var candidates = jsonResponse.getAsJsonArray("candidates");
            if (candidates == null || candidates.size() == 0) {
                throw new RuntimeException("No candidates in Gemini response");
            }
            
            var firstCandidate = candidates.get(0).getAsJsonObject();
            var content = firstCandidate.getAsJsonObject("content");
            if (content == null) {
                throw new RuntimeException("No content in Gemini response candidate");
            }
            
            var parts = content.getAsJsonArray("parts");
            if (parts == null || parts.size() == 0) {
                throw new RuntimeException("No parts in Gemini response content");
            }
            
            var firstPart = parts.get(0).getAsJsonObject();
            var text = firstPart.get("text");
            if (text == null) {
                throw new RuntimeException("No text in Gemini response part");
            }
            
            return text.getAsString();
            
        } catch (Exception e) {
            LOG.error("Failed to extract text from Gemini response", e);
            throw new RuntimeException("Failed to extract analysis text: " + e.getMessage(), e);
        }
    }
} 