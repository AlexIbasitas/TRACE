package com.trace.ai.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.intellij.openapi.project.Project;
import com.trace.ai.models.AIAnalysisResult;
import com.trace.ai.prompts.InitialPromptFailureAnalysisService;
import com.trace.ai.prompts.UserQueryPromptService;
import com.trace.test.models.FailureInfo;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Coordinates between prompt orchestrators and request handler.
 * 
 * <p>This service handles business logic routing and coordinates between
 * different prompt orchestrators and the AI request handler. It has no
 * knowledge of network communication - that's handled by AIRequestHandler.</p>
 * 
 * <p>Key responsibilities:</p>
 * <ul>
 *   <li>Routes to correct prompt orchestrator</li>
 *   <li>Manages service dependencies</li>
 *   <li>Handles business logic routing</li>
 *   <li>No network knowledge</li>
 * </ul>
 * 
 * <p>This service coordinates:</p>
 * <ul>
 *   <li>InitialPromptFailureAnalysisService for initial failure analysis</li>
 *   <li>UserQueryPromptService for user query analysis</li>
 *   <li>AIRequestHandler for network communication</li>
 * </ul>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
public final class AIAnalysisOrchestrator {
    
    private static final Logger LOG = LoggerFactory.getLogger(AIAnalysisOrchestrator.class);
    
    // Services
    private final Project project;
    private final AIRequestHandler requestHandler;
    private final InitialPromptFailureAnalysisService initialOrchestrator;
    private final UserQueryPromptService userQueryOrchestrator;
    
    /**
     * Constructor for AIAnalysisOrchestrator.
     * 
     * @param project The IntelliJ project context
     * @throws NullPointerException if project is null
     */
    public AIAnalysisOrchestrator(@NotNull Project project) {
        if (project == null) {
            throw new NullPointerException("Project cannot be null");
        }
        
        this.project = project;
        this.requestHandler = new AIRequestHandler(project);
        this.initialOrchestrator = new InitialPromptFailureAnalysisService();
        this.userQueryOrchestrator = new UserQueryPromptService();
    }
    
    /**
     * Analyzes an initial test failure using comprehensive prompt orchestration.
     * 
     * <p>This method coordinates the initial failure analysis by:</p>
     * <ol>
     *   <li>Using InitialPromptFailureAnalysisService to orchestrate a detailed prompt</li>
     *   <li>Sending the prompt to AIRequestHandler for network communication</li>
     *   <li>Returning the analysis result</li>
     * </ol>
     * 
     * @param failureInfo the failure information to analyze
     * @return a CompletableFuture containing the analysis result
     * @throws IllegalArgumentException if failureInfo is null
     */
    public CompletableFuture<AIAnalysisResult> analyzeInitialFailure(@NotNull FailureInfo failureInfo) {
        if (failureInfo == null) {
            throw new IllegalArgumentException("FailureInfo cannot be null");
        }
        
        LOG.info("Dispatching initial failure analysis for: " + failureInfo.getScenarioName());
        
        try {
            // Use the initial failure orchestrator to create a detailed prompt
            String prompt = initialOrchestrator.generateDetailedPrompt(failureInfo);
            
            // Send the prompt to the AI request handler
            return requestHandler.sendRequest(prompt, "Full Analysis")
                .thenApply(result -> {
                    LOG.info("Initial failure analysis completed successfully");
                    return result;
                })
                .exceptionally(throwable -> {
                    LOG.error("Initial failure analysis failed", throwable);
                    return new AIAnalysisResult(
                        "Initial failure analysis failed: " + throwable.getMessage(),
                        null,
                        "error",
                        System.currentTimeMillis(),
                        0L
                    );
                });
                
        } catch (Exception e) {
            LOG.error("Unexpected error during initial failure analysis", e);
            return CompletableFuture.completedFuture(
                new AIAnalysisResult(
                    "Unexpected error during initial failure analysis: " + e.getMessage(),
                    null,
                    "error",
                    System.currentTimeMillis(),
                    0L
                )
            );
        }
    }
    
    /**
     * Analyzes a user query with context composition.
     * 
     * <p>This method coordinates user query analysis by:</p>
     * <ol>
     *   <li>Using UserQueryPromptService to orchestrate a prompt with context</li>
     *   <li>Sending the prompt to AIRequestHandler for network communication</li>
     *   <li>Returning the analysis result</li>
     * </ol>
     * 
     * @param failureInfo the original failure information
     * @param userQuery the user's specific question or request
     * @return a CompletableFuture containing the analysis result
     * @throws IllegalArgumentException if any parameter is null
     */
    public CompletableFuture<AIAnalysisResult> analyzeUserQuery(@NotNull FailureInfo failureInfo, @NotNull String userQuery) {
        if (failureInfo == null) {
            throw new IllegalArgumentException("FailureInfo cannot be null");
        }
        if (userQuery == null || userQuery.trim().isEmpty()) {
            throw new IllegalArgumentException("UserQuery cannot be null or empty");
        }
        
        LOG.info("=== USER QUERY ANALYSIS STARTED ===");
        LOG.info("User Query: " + userQuery);
        LOG.info("Failure Context: " + failureInfo.getScenarioName());
        
        try {
            // Get the chat history service for context
            ChatHistoryService chatHistoryService = project.getService(ChatHistoryService.class);
            
            // Log current chat history state
            LOG.info("Chat History State:");
            LOG.info("  - User Query Count: " + chatHistoryService.getUserQueryCount());
            LOG.info("  - Has Failure Context: " + chatHistoryService.hasFailureContext());
            LOG.info("  - Window Size: " + chatHistoryService.getUserMessageWindowSize());
            
            // Add user query to chat history (atomic operation)
            chatHistoryService.addUserQuery(userQuery);
            LOG.info("Added user query to chat history: " + userQuery);
            
            // Use the user query orchestrator to create a prompt with context
            String prompt = userQueryOrchestrator.generatePrompt(failureInfo, userQuery, chatHistoryService);
            
            // Log the complete prompt for manual verification
            LOG.info("=== COMPLETE AI PROMPT FOR USER QUERY ===");
            LOG.info("Prompt Length: " + prompt.length() + " characters");
            LOG.info("Estimated Tokens: ~" + (prompt.length() / 4) + " tokens");
            LOG.info("Prompt Content:");
            LOG.info("--- START PROMPT ---");
            LOG.info(prompt);
            LOG.info("--- END PROMPT ---");
            LOG.info("=== END PROMPT LOGGING ===");
            
            // Send the prompt to the AI request handler
            return requestHandler.sendRequest(prompt, "User Query")
                .thenApply(result -> {
                    LOG.info("=== USER QUERY ANALYSIS COMPLETED ===");
                    LOG.info("AI Response Length: " + (result.getAnalysis() != null ? result.getAnalysis().length() : 0) + " characters");
                    LOG.info("AI Response Service Type: " + result.getServiceType());
                    LOG.info("AI Response Timestamp: " + result.getTimestamp());
                    return result;
                })
                .exceptionally(throwable -> {
                    LOG.error("=== USER QUERY ANALYSIS FAILED ===");
                    LOG.error("Error: " + throwable.getMessage(), throwable);
                    return new AIAnalysisResult(
                        "User query analysis failed: " + throwable.getMessage(),
                        null,
                        "error",
                        System.currentTimeMillis(),
                        0L
                    );
                });
                
        } catch (Exception e) {
            LOG.error("=== UNEXPECTED ERROR IN USER QUERY ANALYSIS ===");
            LOG.error("Error: " + e.getMessage(), e);
            return CompletableFuture.completedFuture(
                new AIAnalysisResult(
                    "Unexpected error during user query analysis: " + e.getMessage(),
                    null,
                    "error",
                    System.currentTimeMillis(),
                    0L
                )
            );
        }
    }
    
    /**
     * Stores failure context in chat history for future user queries.
     * 
     * <p>This method sets the failure context that will be preserved for all
     * subsequent user queries. The failure context provides the initial test
     * failure information that gives context to user follow-up questions.</p>
     * 
     * @param failureInfo the failure information to store as context
     * @throws IllegalArgumentException if failureInfo is null
     */
    public void storeFailureContext(@NotNull FailureInfo failureInfo) {
        if (failureInfo == null) {
            throw new IllegalArgumentException("FailureInfo cannot be null");
        }
        
        LOG.info("Storing failure context for: " + failureInfo.getScenarioName());
        
        try {
            ChatHistoryService chatHistoryService = project.getService(ChatHistoryService.class);
            
            // Build comprehensive failure context string
            StringBuilder contextBuilder = new StringBuilder();
            contextBuilder.append("Test Name: ").append(failureInfo.getScenarioName()).append("\n");
            
            if (failureInfo.getFailedStepText() != null) {
                contextBuilder.append("Failed Step: ").append(failureInfo.getFailedStepText()).append("\n");
            }
            
            if (failureInfo.getErrorMessage() != null) {
                contextBuilder.append("Error: ").append(failureInfo.getErrorMessage()).append("\n");
            }
            
            if (failureInfo.getExpectedValue() != null && failureInfo.getActualValue() != null) {
                contextBuilder.append("Expected: ").append(failureInfo.getExpectedValue()).append("\n");
                contextBuilder.append("Actual: ").append(failureInfo.getActualValue()).append("\n");
            }
            
            // Store the failure context
            chatHistoryService.setFailureContext(contextBuilder.toString());
            LOG.debug("Stored failure context successfully");
            
        } catch (Exception e) {
            LOG.error("Failed to store failure context", e);
            throw new RuntimeException("Failed to store failure context: " + e.getMessage(), e);
        }
    }
    
    /**
     * Analyzes an initial test failure using summary prompt orchestration.
     * 
     * <p>This method coordinates the initial failure analysis using a summary prompt
     * for quick analysis and overview.</p>
     * 
     * @param failureInfo the failure information to analyze
     * @return a CompletableFuture containing the analysis result
     * @throws IllegalArgumentException if failureInfo is null
     */
    public CompletableFuture<AIAnalysisResult> analyzeInitialFailureSummary(@NotNull FailureInfo failureInfo) {
        if (failureInfo == null) {
            throw new IllegalArgumentException("FailureInfo cannot be null");
        }
        
        LOG.info("Dispatching initial failure summary analysis for: " + failureInfo.getScenarioName());
        
        try {
            // Use the initial failure orchestrator to create a summary prompt
            String prompt = initialOrchestrator.generateSummaryPrompt(failureInfo);
            
            // Send the prompt to the AI request handler
            return requestHandler.sendRequest(prompt, "Quick Overview")
                .thenApply(result -> {
                    LOG.info("Initial failure summary analysis completed successfully");
                    return result;
                })
                .exceptionally(throwable -> {
                    LOG.error("Initial failure summary analysis failed", throwable);
                    return new AIAnalysisResult(
                        "Initial failure summary analysis failed: " + throwable.getMessage(),
                        null,
                        "error",
                        System.currentTimeMillis(),
                        0L
                    );
                });
                
        } catch (Exception e) {
            LOG.error("Unexpected error during initial failure summary analysis", e);
            return CompletableFuture.completedFuture(
                new AIAnalysisResult(
                    "Unexpected error during initial failure summary analysis: " + e.getMessage(),
                    null,
                    "error",
                    System.currentTimeMillis(),
                    0L
                )
            );
        }
    }
    
    /**
     * Gets the initial prompt orchestrator for direct prompt generation.
     * This method is used by the UI to generate prompts for display in collapsible sections.
     *
     * @return the initial prompt failure analysis service
     */
    public InitialPromptFailureAnalysisService getInitialOrchestrator() {
        return initialOrchestrator;
    }
    
    /**
     * Gets the request handler for direct AI communication.
     * This method is used by the UI to send prompts directly to avoid duplication.
     *
     * @return the AI request handler
     */
    public AIRequestHandler getRequestHandler() {
        return requestHandler;
    }
    
    /**
     * Shuts down the orchestrator and cleans up resources.
     */
    public void shutdown() {
        LOG.info("Shutting down AIAnalysisOrchestrator");
        try {
            requestHandler.shutdown();
            LOG.info("AIAnalysisOrchestrator shutdown completed");
        } catch (Exception e) {
            LOG.error("Error during AIAnalysisOrchestrator shutdown", e);
        }
    }
} 