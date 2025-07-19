package com.triagemate.ui;

import com.triagemate.models.FailureInfo;

/**
 * Represents a chat message in the TriageMate chat interface.
 * 
 * <p>This immutable data class stores all information related to a chat message,
 * including the sender role, message content, timestamp, AI thinking content,
 * and associated failure information. It supports different message types
 * through the Role enum and provides multiple constructors for different use cases.</p>
 * 
 * <p>The class is designed to be thread-safe and follows the builder pattern
 * for creating messages with different combinations of optional fields.</p>
 * 
 * @author TriageMate Team
 * @version 1.0
 * @since 1.0
 */
public class ChatMessage {
    
    /**
     * Enumeration of possible message sender roles.
     * Defines the different types of participants in the chat conversation.
     */
    public enum Role { 
        /** Messages sent by the user */
        USER, 
        /** Messages sent by the AI assistant */
        AI, 
        /** System-generated messages */
        SYSTEM 
    }
    
    private final Role role;
    private final String text;
    private final long timestamp;
    private final String aiThinking;
    private final FailureInfo failureInfo;
    
    /**
     * Creates a basic chat message with essential fields.
     *
     * @param role The role of the message sender (USER, AI, or SYSTEM)
     * @param text The message text content
     * @param timestamp The message timestamp in milliseconds since epoch
     * @throws IllegalArgumentException if role is null or text is null
     */
    public ChatMessage(Role role, String text, long timestamp) {
        this(role, text, timestamp, null, null);
    }
    
    /**
     * Creates a chat message with AI thinking content.
     *
     * @param role The role of the message sender (USER, AI, or SYSTEM)
     * @param text The message text content
     * @param timestamp The message timestamp in milliseconds since epoch
     * @param aiThinking The AI thinking content for collapsible display (can be null)
     * @throws IllegalArgumentException if role is null or text is null
     */
    public ChatMessage(Role role, String text, long timestamp, String aiThinking) {
        this(role, text, timestamp, aiThinking, null);
    }
    
    /**
     * Creates a complete chat message with all optional fields.
     *
     * @param role The role of the message sender (USER, AI, or SYSTEM)
     * @param text The message text content
     * @param timestamp The message timestamp in milliseconds since epoch
     * @param aiThinking The AI thinking content for collapsible display (can be null)
     * @param failureInfo The associated failure information (can be null)
     * @throws IllegalArgumentException if role is null or text is null
     */
    public ChatMessage(Role role, String text, long timestamp, String aiThinking, FailureInfo failureInfo) {
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null");
        }
        if (text == null) {
            throw new IllegalArgumentException("Text cannot be null");
        }
        
        this.role = role;
        this.text = text;
        this.timestamp = timestamp;
        this.aiThinking = aiThinking;
        this.failureInfo = failureInfo;
    }
    
    /**
     * Gets the role of the message sender.
     *
     * @return The message sender role (USER, AI, or SYSTEM)
     */
    public Role getRole() { 
        return role; 
    }
    
    /**
     * Gets the message text content.
     *
     * @return The message text content
     */
    public String getText() { 
        return text; 
    }
    
    /**
     * Gets the message timestamp.
     *
     * @return The message timestamp in milliseconds since epoch
     */
    public long getTimestamp() { 
        return timestamp; 
    }
    
    /**
     * Gets the AI thinking content for collapsible display.
     *
     * @return The AI thinking content, or null if not available
     */
    public String getAiThinking() { 
        return aiThinking; 
    }
    
    /**
     * Gets the associated failure information.
     *
     * @return The failure information, or null if not associated with a failure
     */
    public FailureInfo getFailureInfo() { 
        return failureInfo; 
    }
    
    /**
     * Checks if this message has AI thinking content.
     *
     * @return true if the message has non-empty AI thinking content, false otherwise
     */
    public boolean hasAiThinking() {
        return aiThinking != null && !aiThinking.trim().isEmpty();
    }
    
    /**
     * Checks if this message is associated with failure information.
     *
     * @return true if the message has associated failure information, false otherwise
     */
    public boolean hasFailureInfo() {
        return failureInfo != null;
    }
    
    /**
     * Checks if this message is from an AI or SYSTEM role.
     *
     * @return true if the message is from AI or SYSTEM role, false otherwise
     */
    public boolean isFromAI() {
        return role == Role.AI || role == Role.SYSTEM;
    }
    
    /**
     * Checks if this message is from a user.
     *
     * @return true if the message is from USER role, false otherwise
     */
    public boolean isFromUser() {
        return role == Role.USER;
    }
    
    @Override
    public String toString() {
        return "ChatMessage{" +
                "role=" + role +
                ", text='" + text + '\'' +
                ", timestamp=" + timestamp +
                ", hasAiThinking=" + hasAiThinking() +
                ", hasFailureInfo=" + hasFailureInfo() +
                '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        ChatMessage that = (ChatMessage) obj;
        
        if (timestamp != that.timestamp) return false;
        if (role != that.role) return false;
        if (!text.equals(that.text)) return false;
        if (aiThinking != null ? !aiThinking.equals(that.aiThinking) : that.aiThinking != null) return false;
        return failureInfo != null ? failureInfo.equals(that.failureInfo) : that.failureInfo == null;
    }
    
    @Override
    public int hashCode() {
        int result = role.hashCode();
        result = 31 * result + text.hashCode();
        result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
        result = 31 * result + (aiThinking != null ? aiThinking.hashCode() : 0);
        result = 31 * result + (failureInfo != null ? failureInfo.hashCode() : 0);
        return result;
    }
} 