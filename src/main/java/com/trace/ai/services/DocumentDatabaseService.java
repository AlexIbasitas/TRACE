package com.trace.ai.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.intellij.openapi.util.io.FileUtil;
import com.trace.ai.models.DocumentEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.ByteBuffer;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Service for managing document storage in SQLite database for the frozen vector store.
 * 
 * <p>This service provides comprehensive database operations for storing and retrieving
 * test failure documents with their embeddings. It follows IntelliJ Platform best practices
 * for database integration and provides thread-safe operations.</p>
 * 
 * <p>The service handles:</p>
 * <ul>
 *   <li>Database initialization and schema creation</li>
 *   <li>Document insertion and retrieval</li>
 *   <li>Embedding storage and retrieval</li>
 *   <li>Search statistics tracking</li>
 *   <li>Database maintenance and cleanup</li>
 * </ul>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
public class DocumentDatabaseService {
    
    private static final Logger LOG = LoggerFactory.getLogger(DocumentDatabaseService.class);
    
    private static final String DATABASE_NAME = "trace-documents.db";
    private static final String DATABASE_VERSION = "1.0";
    
    private Connection connection;
    private final ReadWriteLock dbLock = new ReentrantReadWriteLock();
    
    /**
     * Constructor for DocumentDatabaseService.
     * 
     * <p>Uses IntelliJ's global config directory for persistent storage,
     * so no project dependency is required.</p>
     */
    public DocumentDatabaseService() {
        // No project dependency needed - uses IntelliJ's global config directory
    }
    
    /**
     * Initializes the database connection and creates the schema if it doesn't exist.
     * 
     * @throws SQLException if database initialization fails
     */
    public void initializeDatabase() throws SQLException {
        LOG.info("Initializing document database");
        
        String dbPath = getDatabasePath();
        
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        connection.setAutoCommit(false);
        
        createTables();
        createIndexes();
        
        LOG.info("Document database initialized successfully");
    }
    
    /**
     * Creates the database tables with proper schema.
     * 
     * @throws SQLException if table creation fails
     */
    private void createTables() throws SQLException {
        // Single documents table with multiple embedding columns
        connection.createStatement().execute("""
            CREATE TABLE IF NOT EXISTS documents (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                category TEXT NOT NULL,
                title TEXT NOT NULL,
                content TEXT NOT NULL,
                summary TEXT,
                root_causes TEXT,
                resolution_steps TEXT,
                tags TEXT,
                -- OpenAI embeddings
                openai_embedding_data BLOB,
                openai_embedding_dimension INTEGER,
                -- Gemini embeddings
                gemini_embedding_data BLOB,
                gemini_embedding_dimension INTEGER,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """);
        
        connection.commit();
    }
    
    /**
     * Creates database indexes for optimal query performance.
     * 
     * @throws SQLException if index creation fails
     */
    private void createIndexes() throws SQLException {
        // Index for category-based queries
        connection.createStatement().execute(
            "CREATE INDEX IF NOT EXISTS idx_documents_category ON documents(category)"
        );
        
        // Index for title searches
        connection.createStatement().execute(
            "CREATE INDEX IF NOT EXISTS idx_documents_title ON documents(title)"
        );
        
        // Index for tag-based searches
        connection.createStatement().execute(
            "CREATE INDEX IF NOT EXISTS idx_documents_tags ON documents(tags)"
        );
        
        connection.commit();
    }
    
    /**
     * Inserts a document entry into the database.
     * 
     * @param entry the document entry to insert
     * @param openaiEmbedding the OpenAI embedding data as float array (optional)
     * @param geminiEmbedding the Gemini embedding data as float array (optional)
     * @return the ID of the inserted document
     * @throws SQLException if insertion fails
     */
    public long insertDocument(DocumentEntry entry, 
                             float[] openaiEmbedding, 
                             float[] geminiEmbedding) throws SQLException {
        dbLock.writeLock().lock();
        try {
            String sql = """
                INSERT INTO documents (
                    category, title, content, summary, root_causes, 
                    resolution_steps, tags, openai_embedding_data, openai_embedding_dimension,
                    gemini_embedding_data, gemini_embedding_dimension
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
            
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, entry.getCategory());
            stmt.setString(2, entry.getTitle());
            stmt.setString(3, entry.getContent());
            stmt.setString(4, entry.getSummary());
            stmt.setString(5, entry.getRootCauses());
            stmt.setString(6, entry.getResolutionSteps());
            stmt.setString(7, entry.getTags());
            
            // Set OpenAI embedding
            if (openaiEmbedding != null) {
                stmt.setBytes(8, serializeEmbedding(openaiEmbedding));
                stmt.setInt(9, openaiEmbedding.length);
            } else {
                stmt.setNull(8, Types.BLOB);
                stmt.setNull(9, Types.INTEGER);
            }
            
            // Set Gemini embedding
            if (geminiEmbedding != null) {
                stmt.setBytes(10, serializeEmbedding(geminiEmbedding));
                stmt.setInt(11, geminiEmbedding.length);
            } else {
                stmt.setNull(10, Types.BLOB);
                stmt.setNull(11, Types.INTEGER);
            }
            
            stmt.executeUpdate();
            
            // Get the generated ID using SQLite's last_insert_rowid()
            ResultSet rs = connection.createStatement().executeQuery("SELECT last_insert_rowid()");
            long documentId = -1;
            if (rs.next()) {
                documentId = rs.getLong(1);
            }
            
            connection.commit();
            LOG.debug("Inserted document: " + entry.getTitle() + " with ID: " + documentId);
            
            return documentId;
            
        } finally {
            dbLock.writeLock().unlock();
        }
    }
    

    
    /**
     * Updates the OpenAI embedding data for an existing document.
     * 
     * @param documentId the document ID to update
     * @param embedding the new OpenAI embedding data
     * @throws SQLException if update fails
     */
    public void updateOpenAIEmbedding(long documentId, @NotNull float[] embedding) throws SQLException {
        dbLock.writeLock().lock();
        try {
            String sql = """
                UPDATE documents 
                SET openai_embedding_data = ?, openai_embedding_dimension = ?, updated_at = CURRENT_TIMESTAMP 
                WHERE id = ?
            """;
            
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setBytes(1, serializeEmbedding(embedding));
            stmt.setInt(2, embedding.length);
            stmt.setLong(3, documentId);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                connection.commit();
                LOG.debug("Updated OpenAI embedding for document ID: " + documentId);
            } else {
                LOG.warn("No document found with ID: " + documentId);
            }
            
        } finally {
            dbLock.writeLock().unlock();
        }
    }
    
    /**
     * Updates the Gemini embedding data for an existing document.
     * 
     * @param documentId the document ID to update
     * @param embedding the new Gemini embedding data
     * @throws SQLException if update fails
     */
    public void updateGeminiEmbedding(long documentId, @NotNull float[] embedding) throws SQLException {
        dbLock.writeLock().lock();
        try {
            String sql = """
                UPDATE documents 
                SET gemini_embedding_data = ?, gemini_embedding_dimension = ?, updated_at = CURRENT_TIMESTAMP 
                WHERE id = ?
            """;
            
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setBytes(1, serializeEmbedding(embedding));
            stmt.setInt(2, embedding.length);
            stmt.setLong(3, documentId);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                connection.commit();
                LOG.debug("Updated Gemini embedding for document ID: " + documentId);
            } else {
                LOG.warn("No document found with ID: " + documentId);
            }
            
        } finally {
            dbLock.writeLock().unlock();
        }
    }
    

    
    /**
     * Retrieves all documents with their embeddings.
     * 
     * @return list of documents with embeddings
     * @throws SQLException if retrieval fails
     */
    public List<DocumentWithEmbedding> getAllDocumentsWithEmbeddings() throws SQLException {
        dbLock.readLock().lock();
        try {
            String sql = """
                SELECT id, category, title, content, summary, root_causes, 
                       resolution_steps, tags, openai_embedding_data, openai_embedding_dimension,
                       gemini_embedding_data, gemini_embedding_dimension
                FROM documents 
                WHERE openai_embedding_data IS NOT NULL OR gemini_embedding_data IS NOT NULL
                ORDER BY id
            """;
            
            PreparedStatement stmt = connection.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            
            List<DocumentWithEmbedding> documents = new ArrayList<>();
            while (rs.next()) {
                DocumentWithEmbedding doc = new DocumentWithEmbedding();
                doc.setId(rs.getLong("id"));
                doc.setCategory(rs.getString("category"));
                doc.setTitle(rs.getString("title"));
                doc.setContent(rs.getString("content"));
                doc.setSummary(rs.getString("summary"));
                doc.setRootCauses(rs.getString("root_causes"));
                doc.setResolutionSteps(rs.getString("resolution_steps"));
                doc.setTags(rs.getString("tags"));
                
                // Try OpenAI embedding first, then Gemini
                byte[] openaiEmbeddingBytes = rs.getBytes("openai_embedding_data");
                if (openaiEmbeddingBytes != null) {
                    doc.setEmbedding(deserializeEmbedding(openaiEmbeddingBytes));
                } else {
                    byte[] geminiEmbeddingBytes = rs.getBytes("gemini_embedding_data");
                    if (geminiEmbeddingBytes != null) {
                        doc.setEmbedding(deserializeEmbedding(geminiEmbeddingBytes));
                    }
                }
                
                documents.add(doc);
            }
            
            LOG.debug("Retrieved " + documents.size() + " documents with embeddings");
            return documents;
            
        } finally {
            dbLock.readLock().unlock();
        }
    }
    
    /**
     * Retrieves documents without embeddings (for embedding generation).
     * 
     * @return list of documents without embeddings
     * @throws SQLException if retrieval fails
     */
    public List<DocumentEntry> getAllDocumentsWithoutEmbeddings() throws SQLException {
        dbLock.readLock().lock();
        try {
            String sql = """
                SELECT id, category, title, content, summary, root_causes, 
                       resolution_steps, tags
                FROM documents 
                WHERE openai_embedding_data IS NULL AND gemini_embedding_data IS NULL
                ORDER BY id
            """;
            
            PreparedStatement stmt = connection.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            
            List<DocumentEntry> documents = new ArrayList<>();
            while (rs.next()) {
                DocumentEntry doc = new DocumentEntry();
                doc.setId(rs.getLong("id"));
                doc.setCategory(rs.getString("category"));
                doc.setTitle(rs.getString("title"));
                doc.setContent(rs.getString("content"));
                doc.setSummary(rs.getString("summary"));
                doc.setRootCauses(rs.getString("root_causes"));
                doc.setResolutionSteps(rs.getString("resolution_steps"));
                doc.setTags(rs.getString("tags"));
                
                documents.add(doc);
            }
            
            LOG.debug("Retrieved " + documents.size() + " documents without embeddings");
            return documents;
            
        } finally {
            dbLock.readLock().unlock();
        }
    }
    

    
    /**
     * Clears all documents from the database.
     * 
     * @throws SQLException if clearing fails
     */
    public void clearAllDocuments() throws SQLException {
        dbLock.writeLock().lock();
        try {
            String sql = "DELETE FROM documents";
            PreparedStatement stmt = connection.prepareStatement(sql);
            int deletedCount = stmt.executeUpdate();
            connection.commit();
            LOG.info("Cleared " + deletedCount + " documents from database");
        } finally {
            dbLock.writeLock().unlock();
        }
    }
    
    /**
     * Closes the database connection.
     */
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                LOG.info("Document database connection closed");
            } catch (SQLException e) {
                LOG.error("Error closing database connection", e);
            }
        }
    }
    
    /**
     * Gets the database file path in a consistent, predictable location.
     * 
     * <p>Uses a single, consistent path that works in both plugin and standalone modes.
     * This eliminates confusion and ensures the database is always in the same location.</p>
     * 
     * @return the database file path
     */
    public String getDatabasePath() {
        // Use a consistent, predictable location that works in all modes
        String baseDir = System.getProperty("user.home") + "/.trace/documents";
        
        // Ensure the directory exists
        java.io.File dir = new java.io.File(baseDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        return baseDir + "/" + DATABASE_NAME;
    }
    

    
    /**
     * Serializes a float array to byte array for BLOB storage.
     * 
     * @param embedding the embedding array
     * @return serialized byte array
     */
    private byte[] serializeEmbedding(float[] embedding) {
        ByteBuffer buffer = ByteBuffer.allocate(embedding.length * 4);
        for (float value : embedding) {
            buffer.putFloat(value);
        }
        return buffer.array();
    }
    
    /**
     * Deserializes a byte array to float array from BLOB storage.
     * 
     * @param bytes the serialized byte array
     * @return deserialized float array
     */
    private float[] deserializeEmbedding(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        float[] embedding = new float[bytes.length / 4];
        for (int i = 0; i < embedding.length; i++) {
            embedding[i] = buffer.getFloat();
        }
        return embedding;
    }
    
    /**
     * Data class for documents with embeddings.
     */
    public static class DocumentWithEmbedding extends DocumentEntry {
        private float[] embedding;
        
        public float[] getEmbedding() {
            return embedding;
        }
        
        public void setEmbedding(float[] embedding) {
            this.embedding = embedding;
        }
    }
    

} 