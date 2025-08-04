package com.trace.ai.models;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a test failure document entry in the frozen vector store.
 * 
 * <p>This class encapsulates all the metadata and content for a test failure
 * document, including the structured sections like summary, root causes, and
 * resolution steps. It's designed to work with the SQLite database schema
 * and supports embedding generation.</p>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
public class DocumentEntry {
    
    private long id;
    private String category;
    private String title;
    private String content;
    private String summary;
    private String rootCauses;
    private String resolutionSteps;
    private String tags;
    
    /**
     * Default constructor.
     */
    public DocumentEntry() {
        // Default constructor for serialization
    }
    
    /**
     * Constructor with all fields.
     * 
     * @param category the document category (e.g., "selenium", "cucumber")
     * @param title the document title
     * @param content the full document content
     * @param summary the summary section
     * @param rootCauses the root causes section
     * @param resolutionSteps the resolution steps section
     * @param tags the document tags
     */
    public DocumentEntry(@NotNull String category, @NotNull String title, @NotNull String content,
                        @Nullable String summary, @Nullable String rootCauses, 
                        @Nullable String resolutionSteps, @Nullable String tags) {
        this.category = category;
        this.title = title;
        this.content = content;
        this.summary = summary;
        this.rootCauses = rootCauses;
        this.resolutionSteps = resolutionSteps;
        this.tags = tags;
    }
    
    /**
     * Gets the document ID.
     * 
     * @return the document ID
     */
    public long getId() {
        return id;
    }
    
    /**
     * Sets the document ID.
     * 
     * @param id the document ID
     */
    public void setId(long id) {
        this.id = id;
    }
    
    /**
     * Gets the document category.
     * 
     * @return the document category
     */
    @NotNull
    public String getCategory() {
        return category;
    }
    
    /**
     * Sets the document category.
     * 
     * @param category the document category
     */
    public void setCategory(@NotNull String category) {
        this.category = category;
    }
    
    /**
     * Gets the document title.
     * 
     * @return the document title
     */
    @NotNull
    public String getTitle() {
        return title;
    }
    
    /**
     * Sets the document title.
     * 
     * @param title the document title
     */
    public void setTitle(@NotNull String title) {
        this.title = title;
    }
    
    /**
     * Gets the full document content.
     * 
     * @return the document content
     */
    @NotNull
    public String getContent() {
        return content;
    }
    
    /**
     * Sets the full document content.
     * 
     * @param content the document content
     */
    public void setContent(@NotNull String content) {
        this.content = content;
    }
    
    /**
     * Gets the document summary.
     * 
     * @return the document summary
     */
    @Nullable
    public String getSummary() {
        return summary;
    }
    
    /**
     * Sets the document summary.
     * 
     * @param summary the document summary
     */
    public void setSummary(@Nullable String summary) {
        this.summary = summary;
    }
    
    /**
     * Gets the root causes section.
     * 
     * @return the root causes
     */
    @Nullable
    public String getRootCauses() {
        return rootCauses;
    }
    
    /**
     * Sets the root causes section.
     * 
     * @param rootCauses the root causes
     */
    public void setRootCauses(@Nullable String rootCauses) {
        this.rootCauses = rootCauses;
    }
    
    /**
     * Gets the resolution steps section.
     * 
     * @return the resolution steps
     */
    @Nullable
    public String getResolutionSteps() {
        return resolutionSteps;
    }
    
    /**
     * Sets the resolution steps section.
     * 
     * @param resolutionSteps the resolution steps
     */
    public void setResolutionSteps(@Nullable String resolutionSteps) {
        this.resolutionSteps = resolutionSteps;
    }
    
    /**
     * Gets the document tags.
     * 
     * @return the document tags
     */
    @Nullable
    public String getTags() {
        return tags;
    }
    
    /**
     * Sets the document tags.
     * 
     * @param tags the document tags
     */
    public void setTags(@Nullable String tags) {
        this.tags = tags;
    }
    
    /**
     * Builds the full content for embedding generation.
     * 
     * <p>This method combines all the document sections into a single
     * string that can be used for generating embeddings. The format
     * is optimized for AI model consumption.</p>
     * 
     * @return the combined content for embedding
     */
    @NotNull
    public String buildEmbeddingContent() {
        StringBuilder content = new StringBuilder();
        
        content.append("Title: ").append(title).append("\n");
        
        if (summary != null && !summary.trim().isEmpty()) {
            content.append("Summary: ").append(summary).append("\n");
        }
        
        if (rootCauses != null && !rootCauses.trim().isEmpty()) {
            content.append("Root Causes: ").append(rootCauses).append("\n");
        }
        
        if (resolutionSteps != null && !resolutionSteps.trim().isEmpty()) {
            content.append("Resolution Steps: ").append(resolutionSteps).append("\n");
        }
        
        return content.toString();
    }
    
    /**
     * Creates a search-friendly representation of the document.
     * 
     * <p>This method creates a text representation that's optimized
     * for similarity search and retrieval.</p>
     * 
     * @return the search-friendly content
     */
    @NotNull
    public String buildSearchContent() {
        StringBuilder content = new StringBuilder();
        
        content.append(title).append(" ");
        
        if (summary != null) {
            content.append(summary).append(" ");
        }
        
        if (rootCauses != null) {
            content.append(rootCauses).append(" ");
        }
        
        if (resolutionSteps != null) {
            content.append(resolutionSteps).append(" ");
        }
        
        if (tags != null) {
            content.append(tags).append(" ");
        }
        
        return content.toString().trim();
    }
    
    @Override
    public String toString() {
        return "DocumentEntry{" +
                "id=" + id +
                ", category='" + category + '\'' +
                ", title='" + title + '\'' +
                ", summary='" + summary + '\'' +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        DocumentEntry that = (DocumentEntry) o;
        
        if (id != that.id) return false;
        if (!category.equals(that.category)) return false;
        return title.equals(that.title);
    }
    
    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + category.hashCode();
        result = 31 * result + title.hashCode();
        return result;
    }
} 