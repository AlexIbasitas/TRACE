package com.trace.ai.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.intellij.openapi.project.Project;
import com.trace.ai.models.AIAnalysisResult;
import com.trace.ai.prompts.InitialPromptFailureAnalysisService;
import com.trace.ai.prompts.UserQueryPromptService;
import com.trace.test.models.FailureInfo;
import com.trace.ai.configuration.AISettings;
import com.trace.ai.configuration.AIServiceType;
import com.trace.ai.services.embedding.OpenAIEmbeddingService;
import com.trace.ai.services.embedding.GeminiEmbeddingService;
import com.trace.security.SecureAPIKeyManager;

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
 *   <li>Integrates document retrieval for enhanced AI analysis</li>
 * </ul>
 * 
 * <p>This service coordinates:</p>
 * <ul>
 *   <li>InitialPromptFailureAnalysisService for initial failure analysis</li>
 *   <li>UserQueryPromptService for user query analysis</li>
 *   <li>AIRequestHandler for network communication</li>
 *   <li>DocumentRetrievalService for relevant document context</li>
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
    private final DocumentRetrievalService documentRetrievalService;
    private final AISettings aiSettings;
    
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
        this.aiSettings = AISettings.getInstance();
        
        // Initialize embedding services
        OpenAIEmbeddingService openAIEmbeddingService = new OpenAIEmbeddingService(
            SecureAPIKeyManager.getAPIKey(AIServiceType.OPENAI) != null ? 
            SecureAPIKeyManager.getAPIKey(AIServiceType.OPENAI) : ""
        );
        GeminiEmbeddingService geminiEmbeddingService = new GeminiEmbeddingService(
            SecureAPIKeyManager.getAPIKey(AIServiceType.GEMINI) != null ? 
            SecureAPIKeyManager.getAPIKey(AIServiceType.GEMINI) : ""
        );
        
        // Initialize document database service
        DocumentDatabaseService databaseService = new DocumentDatabaseService();
        try {
            databaseService.initializeDatabase();
        } catch (Exception e) {
            LOG.warn("Failed to initialize document database", e);
        }
        
        // Initialize document retrieval service
        this.documentRetrievalService = new DocumentRetrievalService(
            databaseService, openAIEmbeddingService, geminiEmbeddingService, aiSettings
        );
        
        this.requestHandler = new AIRequestHandler(project);
        this.initialOrchestrator = new InitialPromptFailureAnalysisService();
        this.userQueryOrchestrator = new UserQueryPromptService();
        
        LOG.info("AIAnalysisOrchestrator initialized with document retrieval service");
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
    public CompletableFuture<AIAnalysisResult> analyzeInitialFailure(@NotNull FailureInfo failureInfo, @NotNull AnalysisMode mode) {
        LOG.info("=== DEBUG: analyzeInitialFailure called ===");
        LOG.info("=== DEBUG: This is a test log message ===");
        
        if (failureInfo == null) {
            throw new IllegalArgumentException("FailureInfo cannot be null");
        }
        if (mode == null) {
            throw new IllegalArgumentException("AnalysisMode cannot be null");
        }
        if (!aiSettings.isTraceEnabled()) {
            LOG.info("TRACE is OFF - skipping analysis");
            return CompletableFuture.completedFuture(new AIAnalysisResult(
                "TRACE is OFF. Turn TRACE ON to enable context extraction and AI features.",
                AIServiceType.OPENAI,
                "disabled",
                System.currentTimeMillis(),
                0L
            ));
        }
        
        LOG.info("Dispatching initial failure analysis for: " + failureInfo.getScenarioName());
        
        try {
            // Select prompt based on analysis mode
            final String prompt = (mode == AnalysisMode.OVERVIEW)
                ? initialOrchestrator.generateSummaryPrompt(failureInfo)
                : initialOrchestrator.generateDetailedPrompt(failureInfo);
            
            LOG.info("=== INITIAL FAILURE ANALYSIS PROMPT ===");
            LOG.info("Prompt length: " + prompt.length() + " characters");
            LOG.info("Complete prompt:");
            LOG.info(prompt);
            LOG.info("=== END INITIAL FAILURE ANALYSIS PROMPT ===");
            
            // Gate: if AI analysis is disabled, return a result with prompt only (no network)
            if (!aiSettings.isAutoAnalyzeEnabled()) {
                LOG.info("Enable AI Analysis is OFF - returning prompt-only result (no AI call)");
                return CompletableFuture.completedFuture(new AIAnalysisResult(
                    "Prompt preview only. AI Analysis is disabled.",
                    prompt,
                    AIServiceType.OPENAI,
                    "disabled",
                    System.currentTimeMillis(),
                    0L
                ));
            }

            // Send the prompt to the AI request handler
            final String requestLabel = (mode == AnalysisMode.OVERVIEW) ? "Quick Overview" : "Full Analysis";
            return requestHandler.sendRequest(prompt, requestLabel)
                .thenApply(result -> {
                    LOG.info("Initial failure analysis completed successfully");
                    
                    // Create a new result that includes the prompt for display in "Show AI thinking"
                    return new AIAnalysisResult(
                        result.getAnalysis(),
                        prompt,  // Include the prompt for display
                        result.getServiceType(),
                        result.getModelId(),
                        result.getTimestamp(),
                        result.getProcessingTimeMs()
                    );
                })
                .exceptionally(throwable -> {
                    LOG.error("Initial failure analysis failed", throwable);
                    return new AIAnalysisResult(
                        "Analysis failed due to an error: " + throwable.getMessage(),
                        AIServiceType.OPENAI,
                        "error",
                        System.currentTimeMillis(),
                        0L
                    );
                });
        } catch (Exception e) {
            LOG.error("Failed to create initial failure analysis", e);
            return CompletableFuture.completedFuture(new AIAnalysisResult(
                "Failed to create analysis: " + e.getMessage(),
                AIServiceType.OPENAI,
                "error",
                System.currentTimeMillis(),
                0L
            ));
        }
    }
    
    /**
     * Analyzes an initial test failure with document retrieval integration.
     * 
     * <p>This method demonstrates how to integrate document retrieval with AI analysis:</p>
     * <ol>
     *   <li>Retrieves relevant documents for the failure context</li>
     *   <li>Includes document context in the AI prompt</li>
     *   <li>Performs enhanced analysis with document support</li>
     * </ol>
     * 
     * @param failureInfo the failure information to analyze
     * @return a CompletableFuture containing the analysis result
     * @throws IllegalArgumentException if failureInfo is null
     */
    public CompletableFuture<AIAnalysisResult> analyzeInitialFailureWithDocuments(@NotNull FailureInfo failureInfo, @NotNull AnalysisMode mode) {
        LOG.info("=== DEBUG: analyzeInitialFailureWithDocuments called ===");
        LOG.info("=== DEBUG: Document retrieval service available: " + (documentRetrievalService != null) + " ===");
        
        if (failureInfo == null) {
            throw new IllegalArgumentException("FailureInfo cannot be null");
        }
        if (mode == null) {
            throw new IllegalArgumentException("AnalysisMode cannot be null");
        }
        if (!aiSettings.isTraceEnabled()) {
            LOG.info("TRACE is OFF - skipping analysis");
            return CompletableFuture.completedFuture(new AIAnalysisResult(
                "TRACE is OFF. Turn TRACE ON to enable context extraction and AI features.",
                AIServiceType.OPENAI,
                "disabled",
                System.currentTimeMillis(),
                0L
            ));
        }
        
        LOG.info("Dispatching initial failure analysis with document retrieval for: " + failureInfo.getScenarioName());
        
        try {
            // If AI analysis is disabled, skip document retrieval and AI call
            if (!aiSettings.isAutoAnalyzeEnabled()) {
                LOG.info("Enable AI Analysis is OFF - generating base prompt without RAG or AI call");
                final String basePrompt = (mode == AnalysisMode.OVERVIEW)
                    ? initialOrchestrator.generateSummaryPrompt(failureInfo)
                    : initialOrchestrator.generateDetailedPrompt(failureInfo);
                return CompletableFuture.completedFuture(new AIAnalysisResult(
                    "Prompt preview only. AI Analysis is disabled.",
                    basePrompt,
                    AIServiceType.OPENAI,
                    "disabled",
                    System.currentTimeMillis(),
                    0L
                ));
            }

            // First, retrieve relevant documents
            return documentRetrievalService.retrieveRelevantDocuments(
                failureInfo.getScenarioName(),
                "failure_analysis",
                failureInfo.getErrorMessage(),
                "detailed"
            ).thenCompose(documentContext -> {
                // Create enhanced prompt with document context positioned as context, not response format
                final String basePrompt = (mode == AnalysisMode.OVERVIEW)
                    ? initialOrchestrator.generateSummaryPrompt(failureInfo)
                    : initialOrchestrator.generateDetailedPrompt(failureInfo);
                
        // Insert document context before the "Analysis Request" section and avoid duplicate headers
        String enhancedPrompt = insertDocumentContext(basePrompt, documentContext);
                
                LOG.info("=== DOCUMENT RETRIEVAL RESULTS ===");
                LOG.info("Query: " + failureInfo.getScenarioName());
                LOG.info("Document Context Retrieved:");
                LOG.info(documentContext);
                LOG.info("=== END DOCUMENT RETRIEVAL RESULTS ===");
                
                LOG.info("=== ENHANCED INITIAL FAILURE ANALYSIS PROMPT WITH DOCUMENTS ===");
                LOG.info("Base prompt length: " + basePrompt.length() + " characters");
                LOG.info("Document context length: " + documentContext.length() + " characters");
                LOG.info("Enhanced prompt length: " + enhancedPrompt.length() + " characters");
                LOG.info("Complete enhanced prompt:");
                LOG.info(enhancedPrompt);
                LOG.info("=== END ENHANCED INITIAL FAILURE ANALYSIS PROMPT ===");
                
                // Send the enhanced prompt to the AI request handler
                final String requestLabel = (mode == AnalysisMode.OVERVIEW)
                    ? "Quick Overview"
                    : "Enhanced Analysis with Documents";
                return requestHandler.sendRequest(enhancedPrompt, requestLabel)
                    .thenApply(result -> {
                        LOG.info("Enhanced failure analysis completed successfully");
                        
                        // Create a new result that includes the prompt for display in "Show AI thinking"
                        return new AIAnalysisResult(
                            result.getAnalysis(),
                            enhancedPrompt,  // Include the prompt for display
                            result.getServiceType(),
                            result.getModelId(),
                            result.getTimestamp(),
                            result.getProcessingTimeMs()
                        );
                    })
                    .exceptionally(throwable -> {
                        LOG.error("Enhanced failure analysis failed", throwable);
                        return new AIAnalysisResult(
                            "Enhanced analysis failed due to an error: " + throwable.getMessage(),
                            AIServiceType.OPENAI,
                            "error",
                            System.currentTimeMillis(),
                            0L
                        );
                    });
            }).exceptionally(throwable -> {
                LOG.error("Document retrieval failed, falling back to basic analysis", throwable);
                // Fallback to basic analysis if document retrieval fails
                return analyzeInitialFailure(failureInfo, mode).join();
            });
            
        } catch (Exception e) {
            LOG.error("Failed to create enhanced failure analysis", e);
            return CompletableFuture.completedFuture(new AIAnalysisResult(
                "Failed to create enhanced analysis: " + e.getMessage(),
                AIServiceType.OPENAI,
                "error",
                System.currentTimeMillis(),
                0L
            ));
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
            if (!aiSettings.isTraceEnabled()) {
                LOG.info("TRACE is OFF - skipping analysis");
                return CompletableFuture.completedFuture(new AIAnalysisResult(
                    "TRACE is OFF. Turn TRACE ON to enable context extraction and AI features.",
                    AIServiceType.OPENAI,
                    "disabled",
                    System.currentTimeMillis(),
                    0L
                ));
            }
            // Gate: if AI analysis is disabled, build prompt only and return (no RAG, no AI call)
            if (!aiSettings.isAutoAnalyzeEnabled()) {
                ChatHistoryService chatHistoryService = project.getService(ChatHistoryService.class);
                chatHistoryService.addUserQuery(userQuery);
                String prompt = userQueryOrchestrator.generatePrompt(failureInfo, userQuery, chatHistoryService);
                return CompletableFuture.completedFuture(new AIAnalysisResult(
                    "Prompt preview only. AI Analysis is disabled.",
                    prompt,
                    AIServiceType.OPENAI,
                    "disabled",
                    System.currentTimeMillis(),
                    0L
                ));
            }

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
                    
                    // Create a new result that includes the prompt for display in "Show AI thinking"
                    return new AIAnalysisResult(
                        result.getAnalysis(),
                        prompt,  // Include the prompt for display
                        result.getServiceType(),
                        result.getModelId(),
                        result.getTimestamp(),
                        result.getProcessingTimeMs()
                    );
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
     * Analyzes a user query with document retrieval integration.
     * 
     * <p>This method enhances user query analysis by:</p>
     * <ol>
     *   <li>Retrieving relevant documents for the user query</li>
     *   <li>Including document context in the AI prompt</li>
     *   <li>Performing enhanced analysis with document support</li>
     * </ol>
     * 
     * @param failureInfo the original failure information
     * @param userQuery the user's specific question or request
     * @return a CompletableFuture containing the analysis result
     * @throws IllegalArgumentException if any parameter is null
     */
    public CompletableFuture<AIAnalysisResult> analyzeUserQueryWithDocuments(@NotNull FailureInfo failureInfo, @NotNull String userQuery) {
        if (failureInfo == null) {
            throw new IllegalArgumentException("FailureInfo cannot be null");
        }
        if (userQuery == null || userQuery.trim().isEmpty()) {
            throw new IllegalArgumentException("UserQuery cannot be null or empty");
        }
        
        LOG.info("=== USER QUERY ANALYSIS WITH DOCUMENTS STARTED ===");
        LOG.info("User Query: " + userQuery);
        LOG.info("Failure Context: " + failureInfo.getScenarioName());
        
        try {
            if (!aiSettings.isTraceEnabled()) {
                LOG.info("TRACE is OFF - skipping analysis");
                return CompletableFuture.completedFuture(new AIAnalysisResult(
                    "TRACE is OFF. Turn TRACE ON to enable context extraction and AI features.",
                    AIServiceType.OPENAI,
                    "disabled",
                    System.currentTimeMillis(),
                    0L
                ));
            }
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
            
            // First, retrieve relevant documents for the user query
            return documentRetrievalService.retrieveRelevantDocuments(
                userQuery,
                "user_query",
                failureInfo.getErrorMessage(),
                "detailed"
            ).thenCompose(documentContext -> {
                // Use the user query orchestrator to create a base prompt with context
                String basePrompt = userQueryOrchestrator.generatePrompt(failureInfo, userQuery, chatHistoryService);
                
                // Insert document context before the "Analysis Request" section
                String enhancedPrompt = insertDocumentContext(basePrompt, documentContext);
                
                LOG.info("=== DOCUMENT RETRIEVAL RESULTS FOR USER QUERY ===");
                LOG.info("Query: " + userQuery);
                LOG.info("Document Context Retrieved:");
                LOG.info(documentContext);
                LOG.info("=== END DOCUMENT RETRIEVAL RESULTS ===");
                
                LOG.info("=== ENHANCED USER QUERY PROMPT WITH DOCUMENTS ===");
                LOG.info("Base prompt length: " + basePrompt.length() + " characters");
                LOG.info("Document context length: " + documentContext.length() + " characters");
                LOG.info("Enhanced prompt length: " + enhancedPrompt.length() + " characters");
                LOG.info("Complete enhanced prompt:");
                LOG.info(enhancedPrompt);
                LOG.info("=== END ENHANCED USER QUERY PROMPT ===");
                
                // Send the enhanced prompt to the AI request handler
                return requestHandler.sendRequest(enhancedPrompt, "Enhanced User Query with Documents")
                    .thenApply(result -> {
                        LOG.info("=== ENHANCED USER QUERY ANALYSIS COMPLETED ===");
                        LOG.info("AI Response Length: " + (result.getAnalysis() != null ? result.getAnalysis().length() : 0) + " characters");
                        LOG.info("AI Response Service Type: " + result.getServiceType());
                        LOG.info("AI Response Timestamp: " + result.getTimestamp());
                        
                        // Create a new result that includes the prompt for display in "Show AI thinking"
                        return new AIAnalysisResult(
                            result.getAnalysis(),
                            enhancedPrompt,  // Include the prompt for display
                            result.getServiceType(),
                            result.getModelId(),
                            result.getTimestamp(),
                            result.getProcessingTimeMs()
                        );
                    })
                    .exceptionally(throwable -> {
                        LOG.error("=== ENHANCED USER QUERY ANALYSIS FAILED ===");
                        LOG.error("Error: " + throwable.getMessage(), throwable);
                        return new AIAnalysisResult(
                            "Enhanced user query analysis failed: " + throwable.getMessage(),
                            AIServiceType.OPENAI,
                            "error",
                            System.currentTimeMillis(),
                            0L
                        );
                    });
            }).exceptionally(throwable -> {
                LOG.error("Document retrieval failed for user query, falling back to basic analysis", throwable);
                // Fallback to basic user query analysis if document retrieval fails
                return analyzeUserQuery(failureInfo, userQuery).join();
            });
            
        } catch (Exception e) {
            LOG.error("=== UNEXPECTED ERROR IN ENHANCED USER QUERY ANALYSIS ===");
            LOG.error("Error: " + e.getMessage(), e);
            return CompletableFuture.completedFuture(
                new AIAnalysisResult(
                    "Unexpected error during enhanced user query analysis: " + e.getMessage(),
                    AIServiceType.OPENAI,
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
     * Gets the user query prompt orchestrator for direct prompt generation.
     * This method is used by the UI to generate prompts for display in collapsible sections.
     *
     * @return the user query prompt service
     */
    public UserQueryPromptService getUserQueryOrchestrator() {
        return userQueryOrchestrator;
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

    /**
     * Inserts document context into the prompt before the "Analysis Request" section.
     * This positions the document context as background information rather than
     * part of the response format.
     *
     * @param prompt the base prompt
     * @param documentContext the retrieved document context
     * @return the enhanced prompt with document context inserted
     */
    private String insertDocumentContext(String prompt, String documentContext) {
        // Insert document context before the "Analysis Request" section
        final String analysisRequestMarker = "### Analysis Request ###";
        final String docsHeader = "### Relevant Documentation ###";
        int insertIndex = prompt.indexOf(analysisRequestMarker);

        // Normalize incoming document context and avoid duplicate headers
        String docSection = documentContext == null ? "" : documentContext.trim();
        if (!docSection.startsWith(docsHeader)) {
            docSection = docsHeader + "\n" + docSection;
        }

        if (insertIndex != -1) {
            // Insert document context before the Analysis Request section
            String beforeAnalysisRequest = prompt.substring(0, insertIndex)
                .replaceAll("\n+$", ""); // remove trailing newlines
            String afterAnalysisRequest = prompt.substring(insertIndex)
                .replaceAll("^\n+", ""); // remove leading newlines
            // Ensure exactly one blank line above docs and two above Analysis Request
            return beforeAnalysisRequest + "\n" + docSection + "\n\n" + afterAnalysisRequest;
        } else {
            // Fallback: append document context at the end
            LOG.warn("Analysis Request section not found in prompt, appending document context at the end");
            return prompt + "\n\n" + docSection;
        }
    }
} 