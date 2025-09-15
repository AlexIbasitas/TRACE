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
 * OpenAI-specific AI service provider implementation.
 * 
 * <p>This provider handles all OpenAI API communication, including request building,
 * HTTP communication, response parsing, and error handling specific to OpenAI's API.</p>
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>OpenAI Chat Completions API integration</li>
 *   <li>Support for multiple OpenAI models (GPT-4, GPT-3.5-turbo, etc.)</li>
 *   <li>OpenAI-specific request and response formatting</li>
 *   <li>Comprehensive error handling for OpenAI API errors</li>
 *   <li>Connection validation with OpenAI service</li>
 * </ul>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
public class OpenAIProvider implements AIServiceProvider {
    
    private static final Logger LOG = Logger.getInstance(OpenAIProvider.class);
    
    // OpenAI API Configuration
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final int DEFAULT_MAX_TOKENS = 2000;
    private static final double DEFAULT_TEMPERATURE = 0.3;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    
    // JSON Processing
    private final Gson gson = new Gson();
    
    // HTTP client for making requests
    private final HttpClient httpClient;
    
    /**
     * Constructor for OpenAIProvider.
     * 
     * @param httpClient the HTTP client to use for requests
     */
    public OpenAIProvider(@NotNull HttpClient httpClient) {
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
                analyze(testPrompt, "gpt-4o", apiKey).get();
                LOG.info("OpenAI connection validation successful");
                return true;
            } catch (Exception e) {
                LOG.warn("OpenAI connection validation failed: " + e.getMessage());
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
                LOG.info("Starting OpenAI analysis with model: " + modelId);
                
                // Build OpenAI-specific request
                JsonObject request = buildOpenAIRequest(prompt, modelId);
                
                // Make HTTP request
                String response = executeOpenAIRequest(request, apiKey);
                
                // Parse OpenAI-specific response
                AIAnalysisResult result = parseOpenAIResponse(response, modelId, startTime);
                
                LOG.info("OpenAI analysis completed in " + (System.currentTimeMillis() - startTime) + "ms");
                
                return result;
                
            } catch (Exception e) {
                LOG.error("OpenAI analysis failed", e);
                throw new RuntimeException("OpenAI analysis failed: " + e.getMessage(), e);
            }
        });
    }
    
    @Override
    public CompletableFuture<String[]> discoverAvailableModels(@NotNull String apiKey) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Query OpenAI's models endpoint to get available models
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/models"))
                    .header("Authorization", "Bearer " + apiKey)
                    .GET()
                    .timeout(REQUEST_TIMEOUT)
                    .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
                    JsonArray data = jsonResponse.getAsJsonArray("data");
                    
                    List<String> availableModels = new ArrayList<>();
                    for (int i = 0; i < data.size(); i++) {
                        JsonObject model = data.get(i).getAsJsonObject();
                        String modelId = model.get("id").getAsString();
                        // Only include GPT models
                        if (modelId.startsWith("gpt-")) {
                            availableModels.add(modelId);
                        }
                    }
                    
                    LOG.info("Discovered " + availableModels.size() + " OpenAI models");
                    return availableModels.toArray(new String[0]);
                } else {
                    LOG.warn("Failed to discover OpenAI models: " + response.statusCode());
                    // Return default models as fallback
                    return new String[]{"gpt-3.5-turbo", "gpt-4o", "gpt-4o-mini"};
                }
            } catch (Exception e) {
                LOG.warn("Error discovering OpenAI models: " + e.getMessage());
                // Return default models as fallback
                return new String[]{"gpt-3.5-turbo", "gpt-4o", "gpt-4o-mini"};
            }
        });
    }
    
    @Override
    public AIServiceType getServiceType() {
        return AIServiceType.OPENAI;
    }
    
    @Override
    public String getDisplayName() {
        return "OpenAI";
    }
    
    /**
     * Builds an OpenAI-specific request JSON.
     * 
     * @param prompt the prompt to analyze
     * @param modelId the model ID to use
     * @return the request JSON object
     */
    private JsonObject buildOpenAIRequest(@NotNull String prompt, @NotNull String modelId) {
        JsonObject request = new JsonObject();
        request.addProperty("model", modelId);
        request.addProperty("max_tokens", DEFAULT_MAX_TOKENS);
        request.addProperty("temperature", DEFAULT_TEMPERATURE);
        
        // Add messages array
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);
        
        JsonObject messagesArray = new JsonObject();
        messagesArray.add("messages", gson.toJsonTree(new JsonObject[]{message}));
        
        request.add("messages", messagesArray.get("messages"));
        
        return request;
    }
    
    /**
     * Executes an HTTP request to the OpenAI API.
     * 
     * @param request the request JSON object
     * @param apiKey the API key for authentication
     * @return the response body as a string
     * @throws Exception if the request fails
     */
    private String executeOpenAIRequest(@NotNull JsonObject request, @NotNull String apiKey) throws Exception {
        String requestBody = gson.toJson(request);
        
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(REQUEST_TIMEOUT)
                .build();
        
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("OpenAI API request failed with status " + 
                    response.statusCode() + ": " + response.body());
        }
        
        return response.body();
    }
    
    /**
     * Parses an OpenAI-specific response.
     * 
     * @param response the response body from OpenAI
     * @param modelId the model ID used
     * @param startTime the start time for calculating processing time
     * @return the parsed analysis result
     */
    private AIAnalysisResult parseOpenAIResponse(@NotNull String response, 
                                               @NotNull String modelId, 
                                               long startTime) {
        try {
            JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
            
            // Extract the analysis text
            String analysis = extractOpenAIResponseText(jsonResponse);
            
            // Calculate processing time
            long processingTime = System.currentTimeMillis() - startTime;
            
            return new AIAnalysisResult(
                analysis,
                AIServiceType.OPENAI,
                modelId,
                System.currentTimeMillis(),
                processingTime
            );
            
        } catch (Exception e) {
            LOG.error("Failed to parse OpenAI response", e);
            throw new RuntimeException("Failed to parse OpenAI response: " + e.getMessage(), e);
        }
    }
    
    /**
     * Extracts the analysis text from OpenAI response JSON.
     * 
     * @param jsonResponse the parsed JSON response
     * @return the analysis text
     */
    private String extractOpenAIResponseText(@NotNull JsonObject jsonResponse) {
        try {
            // Navigate through the OpenAI response structure
            var choices = jsonResponse.getAsJsonArray("choices");
            if (choices == null || choices.size() == 0) {
                throw new RuntimeException("No choices in OpenAI response");
            }
            
            var firstChoice = choices.get(0).getAsJsonObject();
            var message = firstChoice.getAsJsonObject("message");
            if (message == null) {
                throw new RuntimeException("No message in OpenAI response choice");
            }
            
            var content = message.get("content");
            if (content == null) {
                throw new RuntimeException("No content in OpenAI response message");
            }
            
            return content.getAsString();
            
        } catch (Exception e) {
            LOG.error("Failed to extract text from OpenAI response", e);
            throw new RuntimeException("Failed to extract analysis text: " + e.getMessage(), e);
        }
    }
} 