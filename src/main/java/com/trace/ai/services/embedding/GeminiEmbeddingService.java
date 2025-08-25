package com.trace.ai.services.embedding;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Google Gemini embedding service for generating vector embeddings.
 * 
 * <p>This service is completely independent of IntelliJ Platform dependencies,
 * making it suitable for use in both Gradle tasks and plugin contexts.</p>
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>Google Gemini text-embedding-001 model integration</li>
 *   <li>Platform-independent HTTP communication</li>
 *   <li>Comprehensive error handling with retry logic</li>
 *   <li>Efficient JSON processing with Gson</li>
 *   <li>Thread-safe operations</li>
 * </ul>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
public class GeminiEmbeddingService {
    
    private static final Logger LOG = Logger.getInstance(GeminiEmbeddingService.class);
    
    // Google Gemini Embedding API Configuration
    private static final String EMBEDDING_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent";
    private static final String DEFAULT_MODEL = "gemini-embedding-001";
    private static final int EMBEDDING_DIMENSIONS = 3072; // gemini-embedding-001 returns 3072 dimensions
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    
    // Retry configuration
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 1000;
    private static final long MAX_BACKOFF_MS = 10000;
    
    // JSON Processing
    private static final Gson GSON = new Gson();
    
    // HTTP client for making requests
    private final HttpClient httpClient;
    private final String apiKey;
    
    /**
     * Creates a new Gemini embedding service.
     * 
     * @param apiKey the Google API key for authentication
     * @throws IllegalArgumentException if apiKey is null or empty
     */
    public GeminiEmbeddingService(@NotNull String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("API key cannot be null or empty");
        }
        
        this.apiKey = apiKey.trim();
        this.httpClient = HttpClient.newHttpClient();
        
        LOG.info("Gemini embedding service initialized");
    }
    
    /**
     * Generates an embedding for the given text using Gemini's embedding-001 model.
     * 
     * <p>This method handles the complete embedding generation workflow including
     * request building, HTTP communication, response parsing, and error handling.</p>
     * 
     * @param text the text to generate embedding for
     * @return a CompletableFuture containing the generated embedding as a float array
     * @throws IllegalArgumentException if text is null or empty
     */
    public CompletableFuture<float[]> generateEmbedding(@NotNull String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text cannot be null or empty");
        }
        
        LOG.info("Generating Gemini embedding for text length: " + text.length());
        
        return CompletableFuture.supplyAsync(() -> {
            for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
                try {
                    return generateEmbeddingWithRetry(text, attempt);
                } catch (Exception e) {
                    LOG.warn("Gemini embedding generation attempt " + (attempt + 1) + " failed", e);
                    
                    if (attempt < MAX_RETRIES - 1) {
                        // Exponential backoff
                        long backoffMs = Math.min(INITIAL_BACKOFF_MS * (1L << attempt), MAX_BACKOFF_MS);
                        try {
                            Thread.sleep(backoffMs);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Embedding generation interrupted", ie);
                        }
                    }
                }
            }
            
            LOG.error("Failed to generate Gemini embedding after " + MAX_RETRIES + " attempts");
            throw new RuntimeException("Failed to generate Gemini embedding after " + MAX_RETRIES + " attempts");
        });
    }
    
    /**
     * Generates an embedding with retry logic for a specific attempt.
     * 
     * @param text the text to generate embedding for
     * @param attempt the current attempt number (0-based)
     * @return the generated embedding
     * @throws Exception if the embedding generation fails
     */
    private float[] generateEmbeddingWithRetry(@NotNull String text, int attempt) throws Exception {
        long startTime = System.currentTimeMillis();
        
        // Build the request
        JsonObject request = buildEmbeddingRequest(text);
        String requestBody = GSON.toJson(request);
        
        // Request body logging removed for security
        
        // Execute the request
        String response = executeEmbeddingRequest(requestBody);
        
        // Parse the response
        float[] embedding = parseEmbeddingResponse(response);
        
        long processingTime = System.currentTimeMillis() - startTime;
        LOG.info("Gemini embedding generated in " + processingTime + "ms");
        
        return embedding;
    }
    
    /**
     * Builds the Gemini embedding request JSON.
     * 
     * @param text the text to embed
     * @return the request JSON object
     */
    private JsonObject buildEmbeddingRequest(@NotNull String text) {
        JsonObject request = new JsonObject();
        
        // Add the model name
        request.addProperty("model", "models/gemini-embedding-001");
        
        // Add the content with parts structure
        JsonObject content = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", text);
        parts.add(part);
        content.add("parts", parts);
        request.add("content", content);
        
        return request;
    }
    
    /**
     * Executes the HTTP request to the Gemini embedding API.
     * 
     * @param requestBody the JSON request body
     * @return the response body as a string
     * @throws Exception if the request fails
     */
    private String executeEmbeddingRequest(@NotNull String requestBody) throws Exception {
        String url = EMBEDDING_URL + "?key=" + apiKey;
        
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(REQUEST_TIMEOUT)
                .build();
        
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            String errorMessage = "Gemini embedding API request failed with status " + 
                    response.statusCode() + ": " + response.body();
            LOG.error(errorMessage);
            throw new RuntimeException(errorMessage);
        }
        
        return response.body();
    }
    
    /**
     * Parses the Gemini embedding response to extract the embedding vector.
     * 
     * @param response the response body from Gemini
     * @return the embedding as a float array
     * @throws Exception if parsing fails
     */
    private float[] parseEmbeddingResponse(@NotNull String response) throws Exception {
        try {
            JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
            
            // Response structure logging removed for security
            
            // Try different possible response structures
            var embeddings = jsonResponse.getAsJsonArray("embeddings");
            if (embeddings == null) {
                // Try alternative structure - maybe it's directly in the response
                var embedding = jsonResponse.getAsJsonObject("embedding");
                if (embedding != null) {
                    var values = embedding.getAsJsonArray("values");
                    if (values != null) {
                        return parseValuesArray(values);
                    }
                }
                
                // Try another possible structure
                var values = jsonResponse.getAsJsonArray("values");
                if (values != null) {
                    return parseValuesArray(values);
                }
                
                throw new RuntimeException("No embeddings in Gemini response. Response keys: " + jsonResponse.keySet());
            }
            
            if (embeddings.size() == 0) {
                throw new RuntimeException("Empty embeddings array in Gemini response");
            }
            
            // Get the first embedding (we only send one text)
            var embedding = embeddings.get(0).getAsJsonObject();
            var values = embedding.getAsJsonArray("values");
            if (values == null) {
                throw new RuntimeException("No values in Gemini embedding response");
            }
            
            return parseValuesArray(values);
            
        } catch (Exception e) {
            LOG.error("Failed to parse Gemini embedding response", e);
            throw new RuntimeException("Failed to parse Gemini embedding response: " + e.getMessage(), e);
        }
    }
    
    /**
     * Parses a JSON array of values into a float array.
     * 
     * @param values the JSON array of values
     * @return the float array
     */
    private float[] parseValuesArray(JsonArray values) {
        // Convert JSON array to float array
        float[] embeddingArray = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            embeddingArray[i] = values.get(i).getAsFloat();
        }
        
        // Validate embedding dimensions
        if (embeddingArray.length != EMBEDDING_DIMENSIONS) {
            LOG.warn("Embedding dimensions mismatch: expected " + EMBEDDING_DIMENSIONS + ", got " + embeddingArray.length);
        }
        
        return embeddingArray;
    }
    
    /**
     * Validates the connection to Gemini's embedding service.
     * 
     * @return a CompletableFuture containing true if connection is valid, false otherwise
     */
    public CompletableFuture<Boolean> validateConnection() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Test with a simple text
                String testText = "Hello, world!";
                generateEmbedding(testText).get();
                LOG.info("Gemini embedding service connection validation successful");
                return true;
            } catch (Exception e) {
                LOG.warn("Gemini embedding service connection validation failed: " + e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Gets the embedding dimensions for this service.
     * 
     * @return the number of dimensions in the embedding vector
     */
    public int getEmbeddingDimensions() {
        return EMBEDDING_DIMENSIONS;
    }
    
    /**
     * Gets the default model used by this service.
     * 
     * @return the default model name
     */
    public String getDefaultModel() {
        return DEFAULT_MODEL;
    }
} 