package com.trace.chat.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.trace.ai.configuration.AISettings;
import com.trace.ai.models.AIAnalysisResult;
import com.trace.ai.services.AIAnalysisOrchestrator;
import com.trace.ai.services.ChatHistoryService;
import com.trace.ai.services.AnalysisMode;
import com.trace.chat.components.ChatMessage;
import com.trace.chat.components.MessageComponent;
import com.trace.chat.components.TypingIndicatorRow;
import com.trace.test.models.FailureInfo;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Helper class for failure analysis-related functionality in the TriagePanelView.
 * Provides methods for handling failure analysis, user message processing, and prompt generation.
 * 
 * <p>This class encapsulates all failure analysis operations to reduce the complexity
 * of the main TriagePanelView class and improve code organization.</p>
 * 
 * @author Alex Ibasitas
 * @version 1.0
 * @since 1.0
 */
public class FailureAnalysisHelper {
    
    private static final Logger LOG = Logger.getInstance(FailureAnalysisHelper.class);
    
    // Constants for error messages
    private static final String ERROR_GENERATING_PROMPT_PREFIX = "Error generating prompt: ";
    
    // Analysis mode constants
    private static final String ANALYSIS_MODE_OVERVIEW = "Quick Overview";
    private static final String ANALYSIS_MODE_FULL = "Full Analysis";
    
    // Component references needed for failure analysis operations
    private final Project project;
    private final AIAnalysisOrchestrator aiAnalysisOrchestrator;
    private final List<ChatMessage> chatHistory;
    private final JPanel messageContainer;
    private final JScrollPane chatScrollPane;
    private final JPanel bottomSpacer;
    private final ScrollHelper scrollHelper;
    private final TypingIndicatorRow typingIndicatorRow;
    private final boolean typingIndicatorVisible;
    private final JComponent latestUserMessageComponent;
    private String currentAnalysisMode;
    private String currentTestRunId;
    private FailureInfo currentFailureInfo;
    
    /**
     * Constructor for FailureAnalysisHelper.
     * 
     * @param project The current project instance
     * @param aiAnalysisOrchestrator The AI analysis orchestrator
     * @param chatHistory The chat history list
     * @param messageContainer The message container component
     * @param chatScrollPane The chat scroll pane component
     * @param bottomSpacer The bottom spacer component
     * @param scrollHelper The scroll helper instance
     * @param typingIndicatorRow The typing indicator row component
     * @param typingIndicatorVisible Whether the typing indicator is visible
     * @param latestUserMessageComponent The latest user message component
     * @param currentAnalysisMode The current analysis mode
     * @param currentTestRunId The current test run ID
     * @param currentFailureInfo The current failure info
     */
    public FailureAnalysisHelper(Project project, AIAnalysisOrchestrator aiAnalysisOrchestrator,
                               List<ChatMessage> chatHistory, JPanel messageContainer,
                               JScrollPane chatScrollPane, JPanel bottomSpacer,
                               ScrollHelper scrollHelper, TypingIndicatorRow typingIndicatorRow,
                               boolean typingIndicatorVisible, JComponent latestUserMessageComponent,
                               String currentAnalysisMode, String currentTestRunId,
                               FailureInfo currentFailureInfo) {
        this.project = project;
        this.aiAnalysisOrchestrator = aiAnalysisOrchestrator;
        this.chatHistory = chatHistory;
        this.messageContainer = messageContainer;
        this.chatScrollPane = chatScrollPane;
        this.bottomSpacer = bottomSpacer;
        this.scrollHelper = scrollHelper;
        this.typingIndicatorRow = typingIndicatorRow;
        this.typingIndicatorVisible = typingIndicatorVisible;
        this.latestUserMessageComponent = latestUserMessageComponent;
        this.currentAnalysisMode = currentAnalysisMode;
        this.currentTestRunId = currentTestRunId;
        this.currentFailureInfo = currentFailureInfo;
    }
    
    /**
     * Updates the panel with new failure information.
     * Clears the chat and generates an initial AI prompt for the failure.
     *
     * @param failureInfo The failure information to analyze
     * @throws IllegalArgumentException if failureInfo is null
     */
    public boolean updateFailure(FailureInfo failureInfo) {
        if (failureInfo == null) {
            throw new IllegalArgumentException("FailureInfo cannot be null");
        }
        
        LOG.info("Updating failure info: " + failureInfo.getScenarioName() + " - " + 
                failureInfo.getFailedStepText().substring(0, Math.min(failureInfo.getFailedStepText().length(), 50)) + "...");
        
        // Ensure we're on the EDT
        if (!SwingUtilities.isEventDispatchThread()) {
            final boolean[] result = {false};
            try {
                ApplicationManager.getApplication().invokeAndWait(() -> result[0] = updateFailure(failureInfo));
            } catch (Exception e) {
                LOG.error("Error during EDT execution", e);
            }
            return result[0];
        }
        
        // Check if we already have a failure in this test run
        if (currentTestRunId != null) {
            // Same test run - ignore silently
            LOG.info("Ignoring subsequent failure in test run: " + currentTestRunId);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failure details - Scenario: " + failureInfo.getScenarioName() + 
                         ", Failed Step: " + failureInfo.getFailedStepText());
            }
            return false;
        }
        
        // First failure of this test run - analyze
        currentTestRunId = "test_run_" + System.currentTimeMillis();
        LOG.info("Processing first failure in new test run: " + currentTestRunId);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Failure details - Scenario: " + failureInfo.getScenarioName() + 
                     ", Failed Step: " + failureInfo.getFailedStepText());
        }
        
        // Store failure context for future user queries
        this.currentFailureInfo = failureInfo;
        aiAnalysisOrchestrator.storeFailureContext(failureInfo);
        
        clearChat();
        generateAndDisplayPrompt(failureInfo);
        return true;
    }
    
    /**
     * Handles user messages with AI analysis if configured, otherwise shows configuration guidance.
     * Enhanced with comprehensive logging for prompt verification and robust error handling.
     *
     * @param messageText The user's message text
     */
    public void handleUserMessageWithAI(String messageText) {
        LOG.info("Handling user message with AI: " + messageText.substring(0, Math.min(messageText.length(), 50)) + "...");
        
        AISettings aiSettings = AISettings.getInstance();
        
        // TRACE gating: If TRACE is OFF, notify user and stop
        if (!aiSettings.isTraceEnabled()) {
            LOG.info("TRACE is OFF - notifying user and skipping all processing");
            addMessage(new ChatMessage(ChatMessage.Role.AI,
                "TRACE is OFF. Turn TRACE ON to enable context extraction and AI features.",
                System.currentTimeMillis(), null, null));
            return;
        }

        // With TRACE ON and Enable AI Analysis OFF: build prompt preview only (no docs, no AI call)
        if (!aiSettings.isAIAnalysisEnabled()) {
            LOG.info("Enable AI Analysis is OFF - generating prompt preview without RAG or AI call");
            if (currentFailureInfo == null) {
                addMessage(new ChatMessage(ChatMessage.Role.AI,
                    "No test failure context available. Please run a test first to establish context for prompt preview.",
                    System.currentTimeMillis(), null, null));
                return;
            }
            ChatHistoryService chatHistoryService = project.getService(ChatHistoryService.class);
            if (chatHistoryService != null) {
                chatHistoryService.addUserQuery(messageText);
            }
            // Build prompt preview only; do not attach it to Show AI thinking for user messages
            aiAnalysisOrchestrator.getUserQueryOrchestrator()
                .generatePrompt(currentFailureInfo, messageText, chatHistoryService);
            addMessage(new ChatMessage(
                ChatMessage.Role.AI,
                "Enable AI Analysis is OFF. Turn it ON to run AI analysis.",
                System.currentTimeMillis(), null, null));
            return;
        }

        // TRACE ON and Enable AI Analysis ON: proceed
        if (!aiSettings.isConfigured()) {
            // AI is not configured, show configuration guidance
            String configurationStatus = aiSettings.getConfigurationStatus();
            String guidanceMessage = "AI features are not configured. " + configurationStatus + 
                                   " Please configure AI settings to use AI-powered features.";
            LOG.info("AI not configured - showing configuration guidance");
            addMessage(new ChatMessage(ChatMessage.Role.AI, guidanceMessage, 
                                    System.currentTimeMillis(), null, null));
            return;
        }
        
        // AI is configured and enabled, process the message with orchestrator
        LOG.info("AI is configured and enabled - processing user query with orchestrator");
        
        // Check if we have failure context for user queries
        if (currentFailureInfo == null) {
            LOG.info("No failure context available for user query");
            addMessage(new ChatMessage(ChatMessage.Role.AI, 
                "No test failure context available. Please run a test first to establish context for AI analysis.", 
                System.currentTimeMillis(), null, null));
            return;
        }
        
        if (LOG.isDebugEnabled()) {
            LOG.debug("Failure Context Available - Scenario Name: " + currentFailureInfo.getScenarioName() + 
                     ", Failed Step: " + (currentFailureInfo.getFailedStepText() != null ? 
                         currentFailureInfo.getFailedStepText().substring(0, Math.min(currentFailureInfo.getFailedStepText().length(), 50)) + "..." : "null") + 
                     ", Error Message: " + (currentFailureInfo.getErrorMessage() != null ? 
                         currentFailureInfo.getErrorMessage().substring(0, Math.min(currentFailureInfo.getErrorMessage().length(), 50)) + "..." : "null"));
        }
        
        // Check chat history service availability and log recent queries
        try {
            ChatHistoryService chatHistoryService = project.getService(ChatHistoryService.class);
            if (chatHistoryService == null) {
                LOG.info("ChatHistoryService is null - proceeding without chat history");
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Chat History Service State - User Query Count: " + chatHistoryService.getUserQueryCount() + 
                             ", Has Failure Context: " + chatHistoryService.hasFailureContext() + 
                             ", Window Size: " + chatHistoryService.getUserMessageWindowSize());
                }
                
                // Log recent user queries for context verification
                List<String> recentQueries = UtilityHelper.getLastThreeUserQueries(project);
                if (!recentQueries.isEmpty() && LOG.isDebugEnabled()) {
                    LOG.debug("Recent User Queries Context: " + String.join(", ", recentQueries));
                }
            }
        } catch (Exception e) {
            LOG.error("Error accessing ChatHistoryService: " + e.getMessage());
        }
        
        // Process the user query with enhanced analysis and RAG
        LOG.info("Calling aiAnalysisOrchestrator.analyzeUserQueryWithDocuments()");
        CompletableFuture<AIAnalysisResult> analysisFuture = 
            aiAnalysisOrchestrator.analyzeUserQueryWithDocuments(currentFailureInfo, messageText);
        
        // Handle the analysis result
        analysisFuture.thenAccept(result -> {
            LOG.info("User query analysis result received");
            if (result != null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("AI Analysis Result - Service Type: " + result.getServiceType() + 
                             ", Timestamp: " + result.getTimestamp() + 
                             ", Analysis Length: " + (result.getAnalysis() != null ? result.getAnalysis().length() : 0) + " characters" + 
                             ", Has Prompt: " + result.hasPrompt());
                }
                
                if (result.getAnalysis() != null && !result.getAnalysis().trim().isEmpty()) {
                    LOG.info("Adding AI response to chat");
                    // Do not attach Show AI thinking for user query responses
                    addMessage(new ChatMessage(ChatMessage.Role.AI, result.getAnalysis(), 
                                            System.currentTimeMillis(), null, null));
                } else {
                    LOG.info("AI analysis returned empty content");
                    addMessage(new ChatMessage(ChatMessage.Role.AI, 
                        "AI analysis completed but returned no content.", 
                        System.currentTimeMillis(), null, null));
                }
            } else {
                LOG.info("AI analysis returned null result");
                addMessage(new ChatMessage(ChatMessage.Role.AI, 
                    "AI analysis returned no result.", 
                    System.currentTimeMillis(), null, null));
            }
        }).exceptionally(throwable -> {
            LOG.error("User query analysis failed: " + throwable.getMessage(), throwable);
            String errorMessage = "AI analysis failed: " + throwable.getMessage();
            addMessage(new ChatMessage(ChatMessage.Role.AI, errorMessage, 
                                    System.currentTimeMillis(), null, null));
            return null;
        });
        
        LOG.info("User message handling completed");
    }
    
    /**
     * Generates and displays the initial prompt for the failure.
     * Creates an AI message with the generated prompt and failure information.
     *
     * @param failureInfo The failure information to generate a prompt for
     */
    public void generateAndDisplayPrompt(FailureInfo failureInfo) {
        // Check if TRACE is enabled (power button) - if not, do nothing at all
        AISettings aiSettings = AISettings.getInstance();
        if (!aiSettings.isTraceEnabled()) {
            LOG.info("TRACE is disabled - skipping prompt generation");
            return; // Complete silence - no messages from TRACE
        }
        
        try {
            LOG.debug("Generating " + currentAnalysisMode.toLowerCase() + " analysis for failure");
            
            // Check if AI analysis is enabled
            if (aiSettings.isTraceEnabled()) {
                LOG.info("Using enhanced analysis with RAG (AI enabled)");
                
                // Show the test failure context immediately (scenario name and failed step)
                // Also attach a placeholder so the "Show AI Thinking" section is visible right away
                addMessage(new ChatMessage(
                    ChatMessage.Role.AI,
                    "",
                    System.currentTimeMillis(),
                    "Preparing analysis prompt...",
                    failureInfo
                ));

                // Populate the AI thinking section with the base prompt before document retrieval
                try {
                    final String basePrompt = ANALYSIS_MODE_OVERVIEW.equals(currentAnalysisMode)
                        ? aiAnalysisOrchestrator.getInitialOrchestrator().generateSummaryPrompt(failureInfo)
                        : aiAnalysisOrchestrator.getInitialOrchestrator().generateDetailedPrompt(failureInfo);

                    if (!chatHistory.isEmpty()) {
                        ChatMessage firstMessage = chatHistory.get(0);
                        ChatMessage updatedWithBasePrompt = new ChatMessage(
                            firstMessage.getRole(),
                            firstMessage.getText(),
                            firstMessage.getTimestamp(),
                            basePrompt,
                            firstMessage.getFailureInfo()
                        );
                        chatHistory.set(0, updatedWithBasePrompt);

                        // Refresh UI to reflect the base prompt in "Show AI Thinking"
                        messageContainer.removeAll();
                        for (ChatMessage message : chatHistory) {
                            MessageManagerHelper.addMessageToUI(message, chatHistory, messageContainer, 
                                typingIndicatorRow, typingIndicatorVisible, latestUserMessageComponent, 
                                scrollHelper, bottomSpacer);
                        }
                        ApplicationManager.getApplication().invokeLater(() -> scrollHelper.requestAlignNewestIfNear(chatScrollPane));
                    }
                } catch (Exception promptBuildError) {
                    LOG.error("Failed to build base prompt prior to document retrieval: " + promptBuildError.getMessage());
                }
                
                // Show typing indicator while waiting for AI analysis
                showTypingIndicator();

                // Use enhanced analysis with RAG for both overview and full analysis modes
                CompletableFuture<AIAnalysisResult> analysisFuture = 
                    aiAnalysisOrchestrator.analyzeInitialFailureWithDocuments(
                        failureInfo,
                        ANALYSIS_MODE_OVERVIEW.equals(currentAnalysisMode) ? AnalysisMode.OVERVIEW : AnalysisMode.FULL
                    );
                
                // Handle the analysis result
                analysisFuture.thenAccept(result -> {
                    // Remove typing indicator on first content arrival
                    hideTypingIndicator();
                    if (result != null && result.getAnalysis() != null && !result.getAnalysis().trim().isEmpty()) {
                        // Update the first message with the enhanced prompt in "Show AI thinking"
                        String aiThinking = result.hasPrompt() ? result.getPrompt() : null;
                        if (aiThinking != null && !chatHistory.isEmpty()) {
                            // Update the first message (failure context) with the AI thinking content
                            ChatMessage firstMessage = chatHistory.get(0);
                            ChatMessage updatedMessage = new ChatMessage(
                                firstMessage.getRole(),
                                firstMessage.getText(),
                                firstMessage.getTimestamp(),
                                aiThinking,
                                firstMessage.getFailureInfo()
                            );
                            chatHistory.set(0, updatedMessage);
                            
                            // Update the UI
                            messageContainer.removeAll();
                            for (ChatMessage message : chatHistory) {
                                MessageManagerHelper.addMessageToUI(message, chatHistory, messageContainer, 
                                    typingIndicatorRow, typingIndicatorVisible, latestUserMessageComponent, 
                                    scrollHelper, bottomSpacer);
                            }
                            ApplicationManager.getApplication().invokeLater(() -> scrollHelper.requestAlignNewestIfNear(chatScrollPane));
                        }
                        
                        // Create a separate AI message with just the analysis result
                        addMessage(new ChatMessage(ChatMessage.Role.AI, result.getAnalysis(),
                                                System.currentTimeMillis(), null, null));
                    } else {
                        addMessage(new ChatMessage(ChatMessage.Role.AI, 
                            "AI analysis completed but returned no content.", 
                            System.currentTimeMillis(), null, null));
                    }
                }).exceptionally(throwable -> {
                    hideTypingIndicator();
                    LOG.error("Error during enhanced failure analysis: " + throwable.getMessage(), throwable);
                    String errorMessage = ERROR_GENERATING_PROMPT_PREFIX + throwable.getMessage();
                    addMessage(new ChatMessage(ChatMessage.Role.AI, errorMessage, 
                                            System.currentTimeMillis(), null, null));
                    return null;
                });
                
            } else {
                LOG.info("Using basic prompt generation (AI disabled)");
                
                // Show the test failure context immediately (scenario name and failed step)
                addMessage(new ChatMessage(ChatMessage.Role.AI, "", System.currentTimeMillis(), null, failureInfo));
                
                // Generate basic prompt for display only (no AI analysis)
                String prompt;
                if (ANALYSIS_MODE_OVERVIEW.equals(currentAnalysisMode)) {
                    prompt = aiAnalysisOrchestrator.getInitialOrchestrator().generateSummaryPrompt(failureInfo);
                } else {
                    prompt = aiAnalysisOrchestrator.getInitialOrchestrator().generateDetailedPrompt(failureInfo);
                }
                
                // Update the first message with the prompt in "Show AI thinking" section
                if (!chatHistory.isEmpty()) {
                    ChatMessage firstMessage = chatHistory.get(0);
                    ChatMessage updatedMessage = new ChatMessage(
                        firstMessage.getRole(),
                        firstMessage.getText(),
                        firstMessage.getTimestamp(),
                        prompt,
                        firstMessage.getFailureInfo()
                    );
                    chatHistory.set(0, updatedMessage);
                    
                    // Update the UI
                    messageContainer.removeAll();
                    for (ChatMessage message : chatHistory) {
                        MessageManagerHelper.addMessageToUI(message, chatHistory, messageContainer, 
                            typingIndicatorRow, typingIndicatorVisible, latestUserMessageComponent, 
                            scrollHelper, bottomSpacer);
                    }
                    ApplicationManager.getApplication().invokeLater(() -> scrollHelper.requestAlignNewestIfNear(chatScrollPane));
                }
                
                // No AI analysis - just show the prompt
                addMessage(new ChatMessage(ChatMessage.Role.AI, 
                    "AI analysis is disabled. Review the prompt above for manual analysis.", 
                    System.currentTimeMillis(), null, null));
            }
            
        } catch (Exception e) {
            LOG.error("Error generating prompt: " + e.getMessage(), e);
            String errorMessage = ERROR_GENERATING_PROMPT_PREFIX + e.getMessage();
            addMessage(new ChatMessage(ChatMessage.Role.AI, errorMessage, System.currentTimeMillis(), null, failureInfo));
        }
    }
    
    /**
     * Adds a message to the chat history and updates the UI with proper EDT compliance.
     * Ensures thread safety by dispatching UI updates to the Event Dispatch Thread.
     *
     * @param message The chat message to add
     */
    private void addMessage(ChatMessage message) {
        // If assistant content arrives, remove typing indicator before appending
        if (message != null && message.isFromAI()) {
            // If we have a visible typing indicator, replace it in-place with the AI message component
            if (typingIndicatorRow != null && typingIndicatorVisible) {
                try {
                    chatHistory.add(message);
                    MessageComponent aiComponent = new MessageComponent(message);
                    scrollHelper.replaceTypingIndicatorWithMessageComponent(aiComponent, typingIndicatorRow);
                    return; // UI updated in-place; skip full rebuild
                } catch (Exception ex) {
                    // Fallback to default path
                    LOG.debug("Failed to replace typing indicator in-place, falling back to rebuild: " + ex.getMessage());
                }
            }
            // No indicator to replace; proceed with normal flow
            hideTypingIndicator();
        }

        MessageManagerHelper.addMessage(message, chatHistory, messageContainer, typingIndicatorRow, 
            typingIndicatorVisible, latestUserMessageComponent, scrollHelper, chatScrollPane, bottomSpacer);
    }
    
    /**
     * Clears the chat history and UI.
     * Removes all messages and resets the message container.
     */
    private void clearChat() {
        MessageManagerHelper.clearChat(chatHistory, messageContainer, bottomSpacer, latestUserMessageComponent);
    }
    
    /**
     * Shows the typing indicator in the chat interface.
     */
    private void showTypingIndicator() {
        MessageManagerHelper.showTypingIndicator(messageContainer, typingIndicatorRow, typingIndicatorVisible, 
            bottomSpacer, chatHistory, latestUserMessageComponent, scrollHelper, chatScrollPane);
    }

    /**
     * Hides the typing indicator in the chat interface.
     */
    private void hideTypingIndicator() {
        MessageManagerHelper.hideTypingIndicator(messageContainer, typingIndicatorRow, typingIndicatorVisible);
    }
    
    // Getters and setters for state variables that TriagePanelView needs to access
    public String getCurrentAnalysisMode() {
        return currentAnalysisMode;
    }
    
    public void setCurrentAnalysisMode(String currentAnalysisMode) {
        this.currentAnalysisMode = currentAnalysisMode;
    }
    
    public String getCurrentTestRunId() {
        return currentTestRunId;
    }
    
    public void setCurrentTestRunId(String currentTestRunId) {
        this.currentTestRunId = currentTestRunId;
    }
    
    public FailureInfo getCurrentFailureInfo() {
        return currentFailureInfo;
    }
    
    public void setCurrentFailureInfo(FailureInfo currentFailureInfo) {
        this.currentFailureInfo = currentFailureInfo;
    }
}
