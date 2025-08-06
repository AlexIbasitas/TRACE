package com.trace.ai.models;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a user query in the chat history for context building.
 * 
 * <p>This immutable data class stores user queries with timestamps.
 * It's designed to be lightweight and focused on maintaining conversation
 * context for AI analysis.</p>
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Immutable design for thread safety</li>
 *   <li>Timestamp tracking for potential future enhancements</li>
 *   <li>Simple data storage with single responsibility</li>
 *   <li>Clean serialization for IntelliJ persistence</li>
 * </ul>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
public final class UserQuery {
    
    private final String query;
    private final long timestamp;
    
    /**
     * Creates a user query.
     *
     * @param query the user's query text
     * @param timestamp the query timestamp in milliseconds since epoch
     * @throws IllegalArgumentException if query is null or empty
     */
    public UserQuery(@NotNull String query, long timestamp) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Query cannot be null or empty");
        }
        
        this.query = query.trim();
        this.timestamp = timestamp;
    }
    
    /**
     * Gets the user's query text.
     *
     * @return the query text
     */
    public String getQuery() {
        return query;
    }
    
    /**
     * Gets the query timestamp.
     *
     * @return the timestamp in milliseconds since epoch
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String toString() {
        return "UserQuery{" +
                "query='" + query.substring(0, Math.min(query.length(), 50)) + "...'" +
                ", timestamp=" + timestamp +
                '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        UserQuery that = (UserQuery) obj;
        
        if (timestamp != that.timestamp) return false;
        return query.equals(that.query);
    }
    
    @Override
    public int hashCode() {
        int result = query.hashCode();
        result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
        return result;
    }
} 