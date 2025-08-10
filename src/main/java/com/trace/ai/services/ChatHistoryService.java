package com.trace.ai.services;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.intellij.openapi.project.Project;
import com.trace.ai.models.UserQuery;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service for managing chat history with sliding window approach.
 * 
 * <p>This service maintains a conversation context using a sliding window approach
 * that stores only user messages (not AI responses) to keep context focused and relevant.
 * It uses IntelliJ's persistence API for project-specific storage and follows
 * Gemini best practices for context building.</p>
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Sliding window with configurable size (default: 3 user messages)</li>
 *   <li>Always preserves initial test failure context</li>
 *   <li>Builds context strings with query at the end (Gemini best practice)</li>
 *   <li>Project-specific persistence using IntelliJ's state management</li>
 *   <li>Thread-safe operations with CopyOnWriteArrayList</li>
 * </ul>
 * 
 * <p>Usage:</p>
 * <ul>
 *   <li>Add user messages with {@link #addUserMessage(String)}</li>
 *   <li>Set failure context with {@link #setFailureContext(String)}</li>
 *   <li>Build enhanced context with {@link #buildContextString(String)}</li>
 *   <li>Clear history with {@link #clearHistory()}</li>
 * </ul>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
@State(
    name = "com.trace.ai.services.chat-history-service",
    storages = @Storage("trace-chat-history.xml")
)
public final class ChatHistoryService implements PersistentStateComponent<ChatHistoryService.State> {
    
    private static final Logger LOG = LoggerFactory.getLogger(ChatHistoryService.class);
    
    /**
     * Default window size for user messages (configurable).
     * This represents the maximum number of user queries to keep in context.
     */
    public static final int DEFAULT_USER_MESSAGE_WINDOW_SIZE = 3;
    
    /**
     * Current window size for user messages.
     * Can be adjusted for different performance/context trade-offs.
     */
    private int userMessageWindowSize = DEFAULT_USER_MESSAGE_WINDOW_SIZE;
    
    /**
     * Current state for persistence.
     */
    private State myState = new State();
    
    /**
     * List of user queries.
     * Uses simple ArrayList since access is single-threaded on EDT.
     */
    private final List<UserQuery> userQueries = new ArrayList<>();
    
    /**
     * Current failure context that will always be preserved.
     * This is managed separately from user queries.
     */
    private String failureContext;
    
    /**
     * State class for persistence.
     */
    public static class State {
        public List<UserQueryData> userQueries = new ArrayList<>();
        public int userMessageWindowSize = DEFAULT_USER_MESSAGE_WINDOW_SIZE;
        public String failureContext = null;
        public long failureContextTimestamp = 0;
    }
    
    /**
     * Data class for serialization.
     */
    public static class UserQueryData {
        public String query;
        public long timestamp;
        
        public UserQueryData() {}
        
        public UserQueryData(UserQuery userQuery) {
            this.query = userQuery.getQuery();
            this.timestamp = userQuery.getTimestamp();
        }
        
        public UserQuery toUserQuery() {
            return new UserQuery(query, timestamp);
        }
    }
    
    /**
     * Constructor for ChatHistoryService.
     * 
     * <p>This constructor is called by IntelliJ's service management system.
     * The service is automatically instantiated and managed by the platform.</p>
     */
    public ChatHistoryService() {
        // IntelliJ will call loadState() to restore persisted data
    }
    
    /**
     * Sets the failure context that will always be preserved.
     * 
     * <p>This method should be called when a test failure is first detected.
     * The failure context will be preserved even when the sliding window
     * removes older user queries.</p>
     * 
     * @param failureContext the failure context text
     * @throws IllegalArgumentException if failureContext is null or empty
     */
    public void setFailureContext(@NotNull String failureContext) {
        if (failureContext == null || failureContext.trim().isEmpty()) {
            throw new IllegalArgumentException("Failure context cannot be null or empty");
        }
        
        LOG.info("Setting failure context: " + failureContext.substring(0, Math.min(failureContext.length(), 100)) + "...");
        
        // Set the failure context directly
        this.failureContext = failureContext.trim();
        
        // Update state for persistence
        myState.failureContext = failureContext.trim();
        myState.failureContextTimestamp = System.currentTimeMillis();
    }
    
    /**
     * Adds a user query to the chat history.
     * 
     * <p>This method implements the sliding window approach by maintaining
     * only the most recent user queries up to the configured window size.
     * Failure context is always preserved separately.</p>
     * 
     * @param query the user's query text
     * @throws IllegalArgumentException if query is null or empty
     */
    public void addUserQuery(@NotNull String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Query cannot be null or empty");
        }
        
        LOG.info("Adding user query: " + query.substring(0, Math.min(query.length(), 50)) + "...");
        
        // Add new user query
        UserQuery userQuery = new UserQuery(query.trim(), System.currentTimeMillis());
        userQueries.add(userQuery);
        
        // Apply sliding window: keep only the most recent user queries
        applySlidingWindow();
    }
    
    /**
     * Builds a context string for AI analysis following Gemini best practices.
     * 
     * <p>This method constructs a context string that puts the current query
     * at the end, as recommended by Gemini documentation. The context includes
     * the failure context (if available) and recent user queries.</p>
     * 
     * @param currentQuery the current query to append at the end
     * @return a formatted context string suitable for AI analysis
     * @throws IllegalArgumentException if currentQuery is null or empty
     */
    public String buildContextString(@NotNull String currentQuery) {
        if (currentQuery == null || currentQuery.trim().isEmpty()) {
            throw new IllegalArgumentException("Current query cannot be null or empty");
        }
        
        StringBuilder contextBuilder = new StringBuilder();
        
        // Start with failure context if available
        if (failureContext != null && !failureContext.trim().isEmpty()) {
            contextBuilder.append("### Test Failure Context ###\n");
            contextBuilder.append(failureContext).append("\n\n");
        }
        
        // Add recent user queries (excluding the current query)
        List<UserQuery> recentQueries = getUserQueries();
        if (!recentQueries.isEmpty()) {
            contextBuilder.append("### Recent Conversation Context ###\n");
            for (UserQuery userQuery : recentQueries) {
                contextBuilder.append("User: ").append(userQuery.getQuery()).append("\n");
            }
            contextBuilder.append("\n");
        }
        
        // Note: Current query is added by the calling service (UserQueryPromptService)
        // This prevents duplication of the query in the final prompt
        
        String contextString = contextBuilder.toString();
        LOG.debug("Built context string with " + recentQueries.size() + " user queries and " + 
                 (failureContext != null ? "failure context" : "no failure context"));
        
        return contextString;
    }
    
    /**
     * Gets the current user message window size.
     * 
     * @return the current window size
     */
    public int getUserMessageWindowSize() {
        return userMessageWindowSize;
    }
    
    /**
     * Sets the user message window size.
     * 
     * <p>This method allows dynamic adjustment of the sliding window size.
     * The change will be applied immediately and persisted.</p>
     * 
     * @param windowSize the new window size (must be positive)
     * @throws IllegalArgumentException if windowSize is not positive
     */
    public void setUserMessageWindowSize(int windowSize) {
        if (windowSize <= 0) {
            throw new IllegalArgumentException("Window size must be positive");
        }
        
        LOG.info("Setting user message window size to: " + windowSize);
        this.userMessageWindowSize = windowSize;
        myState.userMessageWindowSize = windowSize;
        
        // Apply the new window size immediately
        applySlidingWindow();
    }
    
    /**
     * Clears all chat history.
     * 
     * <p>This method removes all user queries and failure context.
     * Use with caution as this will reset the conversation context.</p>
     */
    public void clearHistory() {
        LOG.info("Clearing all chat history");
        userQueries.clear();
        failureContext = null;
        myState.userQueries.clear();
        myState.failureContext = null;
        myState.failureContextTimestamp = 0;
    }
    
    /**
     * Gets the number of user queries currently in history.
     * 
     * @return the number of user queries
     */
    public int getUserQueryCount() {
        return userQueries.size();
    }
    
    /**
     * Checks if there is a failure context available.
     * 
     * @return true if failure context is available
     */
    public boolean hasFailureContext() {
        return failureContext != null && !failureContext.trim().isEmpty();
    }
    
    /**
     * Applies the sliding window to maintain the configured query limit.
     * 
     * <p>This method ensures that only the most recent user queries are kept,
     * while always preserving the failure context separately.</p>
     */
    private void applySlidingWindow() {
        // If we have more user queries than the window size, remove the oldest ones
        if (userQueries.size() > userMessageWindowSize) {
            int queriesToRemove = userQueries.size() - userMessageWindowSize;
            LOG.debug("Removing " + queriesToRemove + " old user queries to maintain window size");
            
            // Remove the oldest user queries
            for (int i = 0; i < queriesToRemove; i++) {
                userQueries.remove(0);
            }
        }
    }
    
    /**
     * Gets all user queries in chronological order.
     * 
     * @return a list of user queries in chronological order
     */
    private List<UserQuery> getUserQueries() {
        return new ArrayList<>(userQueries.stream()
            .sorted((q1, q2) -> Long.compare(q1.getTimestamp(), q2.getTimestamp()))
            .toList());
    }
    
    // ============================================================================
    // PERSISTENCE IMPLEMENTATION
    // ============================================================================
    
    @Override
    public State getState() {
        // Convert current user queries to data objects for persistence
        myState.userQueries.clear();
        for (UserQuery userQuery : userQueries) {
            myState.userQueries.add(new UserQueryData(userQuery));
        }
        return myState;
    }
    
    @Override
    public void loadState(@NotNull State state) {
        myState = state;
        
        // Restore user queries from persisted data
        userQueries.clear();
        for (UserQueryData userQueryData : state.userQueries) {
            userQueries.add(userQueryData.toUserQuery());
        }
        
        // Restore failure context
        failureContext = state.failureContext;
        
        // Restore window size
        userMessageWindowSize = state.userMessageWindowSize;
        
        LOG.info("Loaded " + userQueries.size() + " user queries from persistence");
    }
} 