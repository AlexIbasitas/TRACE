package com.trace.ai.prompts;

import com.intellij.openapi.components.Service;
import com.trace.test.models.FailureInfo;
import com.trace.ai.services.ChatHistoryService;
import com.trace.ai.configuration.AISettings;


import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.diagnostic.Logger;

/**
 * Service for generating AI prompts for user queries in test failure analysis.
 * 
 * <p>This service composes multiple data sources to create comprehensive prompts
 * for user follow-up questions about test failures. It follows Gemini best practices
 * by placing the user query at the end of the prompt.</p>
 * 
 * <p>The service integrates:</p>
 * <ul>
 *   <li><strong>Failure Metadata:</strong> Original test failure context</li>
 *   <li><strong>User Query:</strong> The specific question being asked</li>
 *   <li><strong>Chat History:</strong> Previous conversation context</li>
 *   <li><strong>RAG Documents:</strong> Retrieved relevant documentation (when available)</li>
 * </ul>
 * 
 * <p>This service is specifically designed for user query processing and should
 * not be used for initial failure analysis. For initial analysis, see
 * {@link InitialPromptFailureAnalysisService}.</p>
 */
@Service
public final class UserQueryPromptService {
    
    private static final Logger LOG = Logger.getInstance(UserQueryPromptService.class);
    
    /**
     * Generates a comprehensive prompt for user query analysis.
     * 
     * <p>This method creates a structured prompt that combines failure metadata,
     * chat history, retrieved documents (when available), and the user's specific query. 
     * The prompt follows Gemini best practices by placing the user query at the end.</p>
     * 
     * @param failureInfo The original test failure information
     * @param userQuery The user's specific question or request
     * @param chatHistoryService Service for accessing conversation history
     * @return A comprehensive prompt for AI analysis of the user query
     * @throws IllegalArgumentException if any required parameter is null
     */
    public String generatePrompt(@NotNull FailureInfo failureInfo, 
                                @NotNull String userQuery, 
                                @NotNull ChatHistoryService chatHistoryService) {
        if (failureInfo == null) {
            throw new IllegalArgumentException("failureInfo cannot be null");
        }
        if (userQuery == null || userQuery.trim().isEmpty()) {
            throw new IllegalArgumentException("userQuery cannot be null or empty");
        }
        if (chatHistoryService == null) {
            throw new IllegalArgumentException("chatHistoryService cannot be null");
        }
        
        // Check if AI is enabled
        AISettings aiSettings = AISettings.getInstance();
        if (!aiSettings.isTraceEnabled()) {
            LOG.info("User query prompt generation skipped - AI disabled");
            return "AI Analysis is currently disabled. Enable AI in the header to use AI-powered features.";
        }
        
        StringBuilder prompt = new StringBuilder();
        
        // Clear role and task definition
        prompt.append("### Role ###\n");
        prompt.append("You are an expert test automation engineer analyzing user questions about test failures.\n\n");
        
        prompt.append("### Task ###\n");
        prompt.append("Answer the user's specific question about the test failure using the provided context.\n\n");
        
        // Original failure context (always preserved)
        prompt.append("### Test Failure Context ###\n");
        appendFailureContext(prompt, failureInfo);
        
        // Chat history context (if available)
        String chatHistoryContext = chatHistoryService.buildContextString(userQuery);
        if (chatHistoryContext != null && !chatHistoryContext.trim().isEmpty()) {
            prompt.append("### Recent Conversation ###\n");
            prompt.append(chatHistoryContext).append("\n");
        }
        

        
        // User's specific query (placed at the end per Gemini best practices)
        prompt.append("### Current Query ###\n");
        prompt.append(userQuery.trim()).append("\n\n");
        
        // Response format guidance
        prompt.append("### Response Guidelines ###\n");
        prompt.append("Provide a clear, specific answer to the user's question. Be actionable and relevant.\n\n");
        
        // Add custom rule if present
        String customRule = AISettings.getInstance().getCustomRule();
        if (customRule != null && !customRule.trim().isEmpty()) {
            prompt.append("### Custom Instructions ###\n");
            prompt.append(customRule.trim()).append("\n");
        }
        
        LOG.info("User query prompt generated for: " + userQuery);
        return prompt.toString();
    }
    

    
    private void appendFailureContext(StringBuilder prompt, FailureInfo failureInfo) {
        if (failureInfo.getScenarioName() != null) {
            prompt.append("**Test Name:** ").append(failureInfo.getScenarioName()).append("\n");
        }
        
        if (failureInfo.getFailedStepText() != null) {
            prompt.append("**Failed Step:** ").append(failureInfo.getFailedStepText()).append("\n");
        }
        
        if (failureInfo.getErrorMessage() != null) {
            prompt.append("**Error:** ").append(failureInfo.getErrorMessage()).append("\n");
        }
        
        if (failureInfo.getExpectedValue() != null && failureInfo.getActualValue() != null) {
            prompt.append("**Expected:** ").append(failureInfo.getExpectedValue()).append("\n");
            prompt.append("**Actual:** ").append(failureInfo.getActualValue()).append("\n");
        }
        
        // Add stack trace if available (abbreviated for user queries)
        if (failureInfo.getStackTrace() != null && !failureInfo.getStackTrace().trim().isEmpty()) {
            String cleanStackTrace = cleanStackTrace(failureInfo.getStackTrace());
            // Limit stack trace length for user queries
            if (cleanStackTrace.length() > 500) {
                cleanStackTrace = cleanStackTrace.substring(0, 500) + "...";
            }
            prompt.append("**Stack Trace:**\n```\n").append(cleanStackTrace).append("\n```\n");
        }
        
        prompt.append("\n");
    }
    
    private String cleanStackTrace(String stackTrace) {
        // Remove unnecessary metadata and formatting while preserving essential information
        return stackTrace
            .replaceAll("=== SOURCE INFORMATION ===.*?\n", "")
            .replaceAll("=== ERROR MESSAGE ===.*?\n", "")
            .replaceAll("=== PRIMARY OUTPUT ===.*?\n", "")
            .replaceAll("Primary source: stack trace.*?\n", "")
            .replaceAll("Test name:.*?\n", "")
            .replaceAll("Test location:.*?\n", "")
            .replaceAll("Step failed.*?\n", "")
            .trim();
    }
} 