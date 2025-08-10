package com.trace.ai.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.trace.ai.services.embedding.GeminiEmbeddingService;
import com.trace.ai.services.embedding.OpenAIEmbeddingService;
import com.trace.ai.configuration.AISettings;
import com.trace.ai.configuration.AIServiceType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.trace.ai.models.AIModel;
import com.trace.ai.services.AIModelService;
import com.trace.security.SecureAPIKeyManager;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Service for retrieving relevant documents using vector similarity search.
 * 
 * <p>This service implements the document retrieval workflow as specified in the AI prompt template.
 * It handles query embedding generation, vector similarity search, and document context integration
 * to provide relevant documentation for AI analysis.</p>
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Query embedding generation using OpenAI or Gemini services</li>
 *   <li>Vector similarity search with cosine similarity calculation</li>
 *   <li>Configurable similarity threshold (default: 0.7)</li>
 *   <li>Top 3 most relevant document retrieval</li>
 *   <li>Formatted document context for AI prompt inclusion</li>
 *   <li>Comprehensive error handling and logging</li>
 * </ul>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
public class DocumentRetrievalService {
    
    private static final Logger LOG = LoggerFactory.getLogger(DocumentRetrievalService.class);
    
    // Configuration constants
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.7;
    private static final int DEFAULT_MAX_RESULTS = 3;
    private static final int EMBEDDING_TIMEOUT_SECONDS = 30;
    
    // Services
    private final DocumentDatabaseService databaseService;
    private final OpenAIEmbeddingService openAIEmbeddingService;
    private final GeminiEmbeddingService geminiEmbeddingService;
    private final AISettings aiSettings;
    
    /**
     * Creates a new DocumentRetrievalService.
     * 
     * @param databaseService the document database service
     * @param openAIEmbeddingService the OpenAI embedding service
     * @param geminiEmbeddingService the Gemini embedding service
     * @param aiSettings the AI settings configuration
     * @throws NullPointerException if any required service is null
     */
    public DocumentRetrievalService(@NotNull DocumentDatabaseService databaseService,
                                   @NotNull OpenAIEmbeddingService openAIEmbeddingService,
                                   @NotNull GeminiEmbeddingService geminiEmbeddingService,
                                   @NotNull AISettings aiSettings) {
        if (databaseService == null) {
            throw new NullPointerException("Database service cannot be null");
        }
        if (openAIEmbeddingService == null) {
            throw new NullPointerException("OpenAI embedding service cannot be null");
        }
        if (geminiEmbeddingService == null) {
            throw new NullPointerException("Gemini embedding service cannot be null");
        }
        if (aiSettings == null) {
            throw new NullPointerException("AI settings cannot be null");
        }
        
        this.databaseService = databaseService;
        this.openAIEmbeddingService = openAIEmbeddingService;
        this.geminiEmbeddingService = geminiEmbeddingService;
        this.aiSettings = aiSettings;
        
        LOG.info("Document retrieval service initialized");
    }
    
    /**
     * Retrieves relevant documents for a given query.
     * 
     * <p>This method implements the complete document retrieval workflow:</p>
     * <ol>
     *   <li>Generate embedding for the query text</li>
     *   <li>Perform vector similarity search</li>
     *   <li>Format relevant documents for AI context</li>
     * </ol>
     * 
     * @param queryText the query text to search for
     * @param queryType the type of query (user_query or failure_analysis)
     * @param failureContext optional failure context for additional context
     * @param analysisMode the analysis mode (overview or detailed)
     * @return a CompletableFuture containing the formatted document context
     * @throws IllegalArgumentException if queryText is null or empty
     */
    public CompletableFuture<String> retrieveRelevantDocuments(@NotNull String queryText,
                                                             @NotNull String queryType,
                                                             @Nullable String failureContext,
                                                             @NotNull String analysisMode) {
        if (queryText == null || queryText.trim().isEmpty()) {
            throw new IllegalArgumentException("Query text cannot be null or empty");
        }
        
        LOG.info("Retrieving relevant documents for query type: " + queryType + ", mode: " + analysisMode);
        
        return generateQueryEmbedding(queryText)
            .thenCompose(queryEmbedding -> {
                if (queryEmbedding == null) {
                    LOG.warn("Failed to generate query embedding, proceeding without documents");
                    return CompletableFuture.completedFuture(formatNoDocumentsFound());
                }
                
                return findRelevantDocuments(queryEmbedding)
                    .thenApply(this::formatDocumentContext);
            })
            .exceptionally(throwable -> {
                LOG.error("Document retrieval failed", throwable);
                return formatNoDocumentsFound();
            });
    }
    
    /**
     * Generates an embedding for the query text.
     * 
     * @param queryText the text to generate embedding for
     * @return a CompletableFuture containing the generated embedding
     */
    private CompletableFuture<float[]> generateQueryEmbedding(@NotNull String queryText) {
        // Choose embedding provider based on default model's service; fallback to first available key
        AIModel defaultModel = AIModelService.getInstance().getDefaultModel();
        AIServiceType serviceType = defaultModel != null ? defaultModel.getServiceType() : null;
        if (serviceType == null) {
            // Fallback: pick first available provider
            if (SecureAPIKeyManager.getAPIKey(AIServiceType.OPENAI) != null && !SecureAPIKeyManager.getAPIKey(AIServiceType.OPENAI).trim().isEmpty()) {
                serviceType = AIServiceType.OPENAI;
            } else if (SecureAPIKeyManager.getAPIKey(AIServiceType.GEMINI) != null && !SecureAPIKeyManager.getAPIKey(AIServiceType.GEMINI).trim().isEmpty()) {
                serviceType = AIServiceType.GEMINI;
            }
        }
        
        LOG.info("Generating query embedding using service: " + serviceType);
        LOG.info("Query text: " + queryText.substring(0, Math.min(100, queryText.length())));
        if (queryText.length() > 100) {
            LOG.info("Query text (truncated): " + queryText.substring(0, 100) + "...");
        }
        
        CompletableFuture<float[]> embeddingFuture;
        if (serviceType == null) {
            LOG.warn("No embedding provider available (no default model and no API key). Skipping embeddings.");
            return CompletableFuture.completedFuture(null);
        }

        switch (serviceType) {
            case OPENAI:
                LOG.info("Using OpenAI embedding service (1536 dimensions)");
                embeddingFuture = openAIEmbeddingService.generateEmbedding(queryText);
                break;
            case GEMINI:
                LOG.info("Using Gemini embedding service (3072 dimensions)");
                embeddingFuture = geminiEmbeddingService.generateEmbedding(queryText);
                break;
            default:
                LOG.warn("Unknown service type: " + serviceType + ". Skipping embeddings.");
                embeddingFuture = CompletableFuture.completedFuture(null);
                break;
        }
        
        return embeddingFuture
            .orTimeout(EMBEDDING_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .thenApply(embedding -> {
                LOG.info("Successfully generated embedding with " + embedding.length + " dimensions");
                return embedding;
            })
            .exceptionally(throwable -> {
                LOG.error("Failed to generate query embedding", throwable);
                return null;
            });
    }
    
    /**
     * Finds relevant documents using vector similarity search.
     * 
     * @param queryEmbedding the query embedding to search against
     * @return a CompletableFuture containing the list of relevant documents
     */
    private CompletableFuture<List<DocumentDatabaseService.DocumentWithSimilarity>> findRelevantDocuments(float[] queryEmbedding) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Resolve the same service used for query embedding
                AIModel defaultModel = AIModelService.getInstance().getDefaultModel();
                AIServiceType serviceType = defaultModel != null ? defaultModel.getServiceType() : null;
                if (serviceType == null) {
                    LOG.warn("No embedding provider available (no default model). Skipping retrieval.");
                    return java.util.Collections.emptyList();
                }
                DocumentDatabaseService.EmbeddingType embeddingType = getEmbeddingType(serviceType);
                int expectedDims = (serviceType == AIServiceType.OPENAI) ? 1536 : 3072;
                if (queryEmbedding == null || queryEmbedding.length != expectedDims) {
                    LOG.warn("Embedding dimension mismatch (got " + (queryEmbedding == null ? -1 : queryEmbedding.length) + ", expected " + expectedDims + "). Skipping retrieval.");
                    return java.util.Collections.emptyList();
                }
                
                LOG.info("Searching for relevant documents with similarity threshold: " + DEFAULT_SIMILARITY_THRESHOLD);
                LOG.info("Using embedding type: " + embeddingType + " for service: " + serviceType);
                
                List<DocumentDatabaseService.DocumentWithSimilarity> documents = 
                    databaseService.findRelevantDocuments(queryEmbedding, embeddingType, DEFAULT_MAX_RESULTS, DEFAULT_SIMILARITY_THRESHOLD);
                
                LOG.info("Found " + documents.size() + " relevant documents above threshold " + DEFAULT_SIMILARITY_THRESHOLD);
                
                if (!documents.isEmpty()) {
                    LOG.info("Top document similarity scores:");
                    for (int i = 0; i < Math.min(3, documents.size()); i++) {
                        DocumentDatabaseService.DocumentWithSimilarity doc = documents.get(i);
                        LOG.info("  " + (i + 1) + ". " + doc.getTitle() + " - " + 
                                String.format("%.3f", doc.getSimilarityScore()));
                    }
                } else {
                    LOG.info("No documents found above similarity threshold");
                }
                
                return documents;
                
            } catch (SQLException e) {
                LOG.error("Database error during document search", e);
                throw new RuntimeException("Failed to search documents", e);
            }
        });
    }
    
    /**
     * Converts AIServiceType to EmbeddingType.
     * 
     * @param serviceType the AI service type
     * @return the corresponding embedding type
     */
    private DocumentDatabaseService.EmbeddingType getEmbeddingType(AIServiceType serviceType) {
        switch (serviceType) {
            case OPENAI:
                return DocumentDatabaseService.EmbeddingType.OPENAI;
            case GEMINI:
                return DocumentDatabaseService.EmbeddingType.GEMINI;
            default:
                LOG.warn("Unknown service type: " + serviceType + ", using OpenAI as fallback");
                return DocumentDatabaseService.EmbeddingType.OPENAI;
        }
    }
    
    /**
     * Formats the document context for inclusion in AI prompts.
     * 
     * @param documents the list of relevant documents
     * @return formatted document context string
     */
    private String formatDocumentContext(List<DocumentDatabaseService.DocumentWithSimilarity> documents) {
        if (documents == null || documents.isEmpty()) {
            LOG.info("No relevant documents found - returning fallback message");
            return formatNoDocumentsFound();
        }
        
        LOG.info("Formatting " + documents.size() + " relevant documents for AI prompt");
        
        StringBuilder context = new StringBuilder();
        context.append("### Relevant Documentation ###\n");
        
        for (int i = 0; i < documents.size(); i++) {
            DocumentDatabaseService.DocumentWithSimilarity doc = documents.get(i);
            
            LOG.info("Document " + (i + 1) + ": " + doc.getTitle() + " (similarity: " + 
                    String.format("%.3f", doc.getSimilarityScore()) + ")");
            
            context.append("**Document ").append(i + 1).append(":** ")
                   .append(doc.getTitle())
                   .append(" - Similarity: ")
                   .append(String.format("%.3f", doc.getSimilarityScore()))
                   .append("\n");
            
            // Add key content sections
            if (doc.getSummary() != null && !doc.getSummary().trim().isEmpty()) {
                LOG.info("  - Summary: " + doc.getSummary().substring(0, Math.min(100, doc.getSummary().length())));
                context.append(doc.getSummary()).append("\n\n");
            }
            
            if (doc.getRootCauses() != null && !doc.getRootCauses().trim().isEmpty()) {
                LOG.info("  - Root Causes: " + doc.getRootCauses().substring(0, Math.min(100, doc.getRootCauses().length())));
                context.append("**Root Causes:** ").append(doc.getRootCauses()).append("\n\n");
            }
            
            if (doc.getResolutionSteps() != null && !doc.getResolutionSteps().trim().isEmpty()) {
                LOG.info("  - Resolution Steps: " + doc.getResolutionSteps().substring(0, Math.min(100, doc.getResolutionSteps().length())));
                context.append("**Resolution Steps:** ").append(doc.getResolutionSteps()).append("\n\n");
            }
            
            if (i < documents.size() - 1) {
                context.append("---\n\n");
            }
        }
        
        String finalContext = context.toString();
        LOG.info("Formatted document context length: " + finalContext.length() + " characters");
        LOG.debug("Formatted document context with " + documents.size() + " documents");
        return finalContext;
    }
    
    /**
     * Formats the fallback message when no documents are found.
     * 
     * @return formatted fallback message
     */
    private String formatNoDocumentsFound() {
        return ""; // Return empty to avoid inserting a docs section when nothing is found
    }
    
    /**
     * Gets the count of documents with embeddings for the current service type.
     * 
     * @return the number of documents with embeddings
     */
    public int getDocumentCount() {
        try {
            AIServiceType serviceType = aiSettings.getPreferredAIService();
            DocumentDatabaseService.EmbeddingType embeddingType = getEmbeddingType(serviceType);
            return databaseService.getDocumentCountWithEmbeddings(embeddingType);
        } catch (SQLException e) {
            LOG.error("Failed to get document count", e);
            return 0;
        }
    }
    
    /**
     * Validates that the document database is properly configured.
     * 
     * @return true if the database is ready for document retrieval
     */
    public boolean isDatabaseReady() {
        try {
            int documentCount = getDocumentCount();
            LOG.info("Document database contains " + documentCount + " documents with embeddings");
            return documentCount > 0;
        } catch (Exception e) {
            LOG.error("Database validation failed", e);
            return false;
        }
    }
} 