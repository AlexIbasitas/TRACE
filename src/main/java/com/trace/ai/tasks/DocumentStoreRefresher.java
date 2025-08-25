package com.trace.ai.tasks;

import com.intellij.openapi.diagnostic.Logger;
import com.trace.ai.models.DocumentEntry;
import com.trace.ai.services.DocumentDatabaseService;
import com.trace.ai.services.DocumentParserService;
import com.trace.ai.services.embedding.OpenAIEmbeddingService;
import com.trace.ai.services.embedding.GeminiEmbeddingService;
import java.util.Map;
import java.util.HashMap;

import java.io.File;
import java.sql.SQLException;
import java.util.List;

/**
 * Standalone document store refresher that can be run from command line.
 * 
 * <p>This class allows developers to refresh the document store by parsing
 * documents and generating embeddings. API keys are provided as command line
 * arguments for security.</p>
 * 
 * <p>Usage:</p>
 * <pre>
 * java -cp build/libs/trace.jar com.trace.ai.tasks.DocumentStoreRefresher \
 *   --openai-key=your_openai_key \
 *   --gemini-key=your_gemini_key
 * </pre>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
public class DocumentStoreRefresher {
    
    private static final Logger LOG = Logger.getInstance(DocumentStoreRefresher.class);
    
    public static void main(String[] args) {
        // Parse command line arguments
        String openaiApiKey = null;
        String geminiApiKey = null;
        
        for (String arg : args) {
            if (arg.startsWith("--openai-key=")) {
                openaiApiKey = arg.substring("--openai-key=".length());
            } else if (arg.startsWith("--gemini-key=")) {
                geminiApiKey = arg.substring("--gemini-key=".length());
            }
        }
        
        if (openaiApiKey == null && geminiApiKey == null) {
            LOG.error("No API keys provided! Use -Dopenai.api.key=your_key");
            System.exit(1);
        }
        
        try {
            // Step 1: Parse documents
            LOG.info("1. Parsing documents...");
            DocumentParserService parser = new DocumentParserService();
            File documentsDir = new File("src/main/resources/documents");
            
            if (!documentsDir.exists()) {
                LOG.error("Documents directory not found: " + documentsDir.getAbsolutePath());
                System.exit(1);
            }
            
            List<DocumentEntry> documents = parser.parseDocumentsFromDirectory(documentsDir);
            LOG.info("Parsed " + documents.size() + " documents");
            
            // Step 2: Refresh single document store with all available embeddings
            refreshDocumentStore(documents, openaiApiKey, geminiApiKey);
            
            LOG.info("Document store refresh completed - " + documents.size() + " documents processed");
            if (openaiApiKey != null) {
                LOG.info("OpenAI embeddings ready");
            }
            if (geminiApiKey != null) {
                LOG.info("Gemini embeddings ready");
            }
            
        } catch (Exception e) {
            LOG.error("Refresh failed: " + e.getMessage(), e);
            System.exit(1);
        }
    }
    
    private static void refreshDocumentStore(List<DocumentEntry> documents, String openaiApiKey, String geminiApiKey) {
                    LOG.info("Refreshing document store");
        
        try {
            // Initialize single database
            DocumentDatabaseService database = new DocumentDatabaseService();
            database.initializeDatabase();
            LOG.info("Database initialized: trace-documents.db");
            
            // Clear existing documents before fresh insertion
            database.clearAllDocuments();
            LOG.info("Cleared existing documents");
            LOG.info("Preparing for fresh document insertion");
            
            // Insert documents and track their database IDs
            int insertedCount = 0;
            Map<String, Long> documentIds = new HashMap<>(); // title -> database ID mapping
            
            for (DocumentEntry doc : documents) {
                try {
                    // Use the new dual-embedding schema - insert without embeddings initially
                    long documentId = database.insertDocument(doc, null, null);
                    documentIds.put(doc.getTitle(), documentId);
                    insertedCount++;
                } catch (SQLException e) {
                    LOG.error("Failed to insert: " + doc.getTitle() + " - " + e.getMessage());
                }
            }
            LOG.info("Inserted " + insertedCount + " documents");
            
            // Generate embeddings using the new platform-independent services
            if (openaiApiKey != null || geminiApiKey != null) {
                LOG.info("Generating embeddings...");
                
                // Create embedding services
                OpenAIEmbeddingService openAIEmbeddingService = null;
                GeminiEmbeddingService geminiEmbeddingService = null;
                
                if (openaiApiKey != null) {
                    openAIEmbeddingService = new OpenAIEmbeddingService(openaiApiKey);
                    LOG.info("OpenAI embedding service initialized");
                }
                
                if (geminiApiKey != null) {
                    geminiEmbeddingService = new GeminiEmbeddingService(geminiApiKey);
                    LOG.info("Gemini embedding service initialized");
                }
                
                // Use embedding services directly - no need for EmbeddingService wrapper
                
                // Generate embeddings for documents
                int embeddingCount = 0;
                for (DocumentEntry doc : documents) {
                    try {
                        // Get the database ID for this document
                        Long databaseId = documentIds.get(doc.getTitle());
                        if (databaseId == null) {
                            LOG.error("No database ID found for: " + doc.getTitle());
                            continue;
                        }
                        
                        // Generate OpenAI embeddings if available
                        if (openAIEmbeddingService != null) {
                            float[] openAIEmbedding = openAIEmbeddingService.generateEmbedding(doc.buildEmbeddingContent()).get();
                            database.updateOpenAIEmbedding(databaseId, openAIEmbedding);
                            embeddingCount++;
                            LOG.info("Generated OpenAI embedding for: " + doc.getTitle());
                        }
                        
                        // Generate Gemini embeddings if available
                        if (geminiEmbeddingService != null) {
                            float[] geminiEmbedding = geminiEmbeddingService.generateEmbedding(doc.buildEmbeddingContent()).get();
                            database.updateGeminiEmbedding(databaseId, geminiEmbedding);
                            embeddingCount++;
                            LOG.info("Generated Gemini embedding for: " + doc.getTitle());
                        }
                        
                        // Small delay to respect API rate limits
                        Thread.sleep(200);
                        
                    } catch (Exception e) {
                        LOG.error("Failed to generate embedding for: " + doc.getTitle() + " - " + e.getMessage());
                    }
                }
                
                LOG.info("Generated " + embeddingCount + " embeddings successfully");
            } else {
                LOG.warn("No API keys provided - skipping embedding generation");
            }
            
            // Verify final state
            List<DocumentDatabaseService.DocumentWithEmbedding> docsWithEmbeddings = database.getAllDocumentsWithEmbeddings();
            LOG.info("Database contains " + docsWithEmbeddings.size() + " documents with embeddings");
            
            database.close();
            
        } catch (Exception e) {
            LOG.error("Failed to refresh document store: " + e.getMessage(), e);
            throw new RuntimeException("Failed to refresh document store", e);
        }
    }
} 