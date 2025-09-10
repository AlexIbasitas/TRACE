package com.trace.ai.services;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class for prompt manipulation and enhancement operations.
 * 
 * <p>This class provides stateless utility methods for working with AI prompts,
 * including document context insertion and prompt formatting operations.</p>
 * 
 * <p>Key responsibilities:</p>
 * <ul>
 *   <li>Document context insertion into prompts</li>
 *   <li>Prompt formatting and normalization</li>
 *   <li>String manipulation for prompt enhancement</li>
 * </ul>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
public final class PromptUtils {
    
    private static final Logger LOG = Logger.getInstance(PromptUtils.class);
    
    // Constants for prompt section markers
    private static final String ANALYSIS_REQUEST_MARKER = "### Analysis Request ###";
    private static final String DOCS_HEADER = "### Relevant Documentation ###";
    
    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private PromptUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * Inserts document context into the prompt before the "Analysis Request" section.
     * This positions the document context as background information rather than
     * part of the response format.
     *
     * @param prompt the base prompt
     * @param documentContext the retrieved document context
     * @return the enhanced prompt with document context inserted
     * @throws IllegalArgumentException if prompt is null
     */
    @NotNull
    public static String insertDocumentContext(@NotNull String prompt, @Nullable String documentContext) {
        if (prompt == null) {
            throw new IllegalArgumentException("Prompt cannot be null");
        }
        
        // Normalize incoming document context and avoid duplicate headers
        String docSection = documentContext == null ? "" : documentContext.trim();
        if (!docSection.isEmpty() && !docSection.startsWith(DOCS_HEADER)) {
            docSection = DOCS_HEADER + "\n" + docSection;
        }
        
        // If no document context, return original prompt
        if (docSection.isEmpty()) {
            return prompt;
        }
        
        int insertIndex = prompt.indexOf(ANALYSIS_REQUEST_MARKER);
        
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
            LOG.debug("Analysis Request section not found in prompt, appending document context at the end");
            return prompt + "\n\n" + docSection;
        }
    }
    
    /**
     * Estimates the approximate token count for a prompt based on character count.
     * This is a rough estimation using the common 4 characters per token ratio.
     *
     * @param prompt the prompt text
     * @return estimated token count
     * @throws IllegalArgumentException if prompt is null
     */
    public static int estimateTokenCount(@NotNull String prompt) {
        if (prompt == null) {
            throw new IllegalArgumentException("Prompt cannot be null");
        }
        return prompt.length() / 4;
    }
    
    /**
     * Normalizes a prompt by removing excessive whitespace and ensuring consistent formatting.
     *
     * @param prompt the prompt to normalize
     * @return the normalized prompt
     * @throws IllegalArgumentException if prompt is null
     */
    @NotNull
    public static String normalizePrompt(@NotNull String prompt) {
        if (prompt == null) {
            throw new IllegalArgumentException("Prompt cannot be null");
        }
        
        return prompt
            .replaceAll("\n{3,}", "\n\n")  // Replace 3+ newlines with 2
            .replaceAll(" +\n", "\n")      // Remove trailing spaces before newlines
            .replaceAll("\n +", "\n")      // Remove leading spaces after newlines
            .trim();                       // Remove leading/trailing whitespace
    }
}
