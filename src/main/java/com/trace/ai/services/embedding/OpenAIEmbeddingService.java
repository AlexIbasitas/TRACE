package com.trace.ai.services.embedding;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * OpenAI embedding service for generating vector embeddings.
 * 
 * <p>This service is completely independent of IntelliJ Platform dependencies,
 * making it suitable for use in both Gradle tasks and plugin contexts.</p>
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>OpenAI text-embedding-ada-002 model integration</li>
 *   <li>Platform-independent HTTP communication</li>
 *   <li>Comprehensive error handling with retry logic</li>
 *   <li>Efficient JSON processing with Gson</li>
 *   <li>Thread-safe operations</li>
 * </ul>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
public class OpenAIEmbeddingService {
    
    private static final Logger LOG = Logger.getInstance(OpenAIEmbeddingService.class);
    
    // OpenAI Embedding API Configuration
    private static final String EMBEDDING_URL = "https://api.openai.com/v1/embeddings";
    private static final String DEFAULT_MODEL = "text-embedding-ada-002";
    private static final int EMBEDDING_DIMENSIONS = 1536;
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
     * Creates a new OpenAI embedding service.
     * 
     * @param apiKey the OpenAI API key for authentication
     * @throws IllegalArgumentException if apiKey is null or empty
     */
    public OpenAIEmbeddingService(@NotNull String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("API key cannot be null or empty");
        }
        
        this.apiKey = apiKey.trim();
        this.httpClient = HttpClient.newHttpClient();
        
        LOG.info("OpenAI embedding service initialized");
    }
    
    /**
     * Generates an embedding for the given text using OpenAI's text-embedding-ada-002 model.
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
        
        LOG.info("Generating OpenAI embedding for text length: " + text.length());
        
        return CompletableFuture.supplyAsync(() -> {
            for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
                try {
                    return generateEmbeddingWithRetry(text, attempt);
                } catch (Exception e) {
                    LOG.warn("OpenAI embedding generation attempt " + (attempt + 1) + " failed", e);
                    
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
            
            LOG.error("Failed to generate OpenAI embedding after " + MAX_RETRIES + " attempts");
            throw new RuntimeException("Failed to generate OpenAI embedding after " + MAX_RETRIES + " attempts");
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
        LOG.info("OpenAI embedding generated in " + processingTime + "ms");
        
        return embedding;
    }
    
    /**
     * Builds the OpenAI embedding request JSON.
     * 
     * @param text the text to embed
     * @return the request JSON object
     */
    private JsonObject buildEmbeddingRequest(@NotNull String text) {
        JsonObject request = new JsonObject();
        request.addProperty("model", DEFAULT_MODEL);
        request.addProperty("input", text);
        return request;
    }
    
    /**
     * Executes the HTTP request to the OpenAI embedding API.
     * 
     * @param requestBody the JSON request body
     * @return the response body as a string
     * @throws Exception if the request fails
     */
    private String executeEmbeddingRequest(@NotNull String requestBody) throws Exception {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(EMBEDDING_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(REQUEST_TIMEOUT)
                .build();
        
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            String errorMessage = "OpenAI embedding API request failed with status " + 
                    response.statusCode() + ": " + response.body();
            LOG.error(errorMessage);
            throw new RuntimeException(errorMessage);
        }
        
        return response.body();
    }
    
    /**
     * Parses the OpenAI embedding response to extract the embedding vector.
     * 
     * @param response the response body from OpenAI
     * @return the embedding as a float array
     * @throws Exception if parsing fails
     */
    private float[] parseEmbeddingResponse(@NotNull String response) throws Exception {
        try {
            JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
            
            // Navigate through the OpenAI embedding response structure
            var data = jsonResponse.getAsJsonArray("data");
            if (data == null || data.size() == 0) {
                throw new RuntimeException("No data in OpenAI embedding response");
            }
            
            var firstData = data.get(0).getAsJsonObject();
            var embedding = firstData.getAsJsonArray("embedding");
            if (embedding == null) {
                throw new RuntimeException("No embedding in OpenAI response data");
            }
            
            // Convert JSON array to float array
            float[] embeddingArray = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                embeddingArray[i] = embedding.get(i).getAsFloat();
            }
            
            // Validate embedding dimensions
            if (embeddingArray.length != EMBEDDING_DIMENSIONS) {
                LOG.warn("Embedding dimensions mismatch: expected " + EMBEDDING_DIMENSIONS + ", got " + embeddingArray.length);
            }
            
            return embeddingArray;
            
        } catch (Exception e) {
            LOG.error("Failed to parse OpenAI embedding response", e);
            throw new RuntimeException("Failed to parse OpenAI embedding response: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validates the connection to OpenAI's embedding service.
     * 
     * @return a CompletableFuture containing true if connection is valid, false otherwise
     */
    public CompletableFuture<Boolean> validateConnection() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Test with a simple text
                String testText = "Hello, world!";
                generateEmbedding(testText).get();
                LOG.info("OpenAI embedding service connection validation successful");
                return true;
            } catch (Exception e) {
                LOG.warn("OpenAI embedding service connection validation failed: " + e.getMessage());
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