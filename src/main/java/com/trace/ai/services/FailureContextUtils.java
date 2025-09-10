package com.trace.ai.services;

import com.trace.test.models.FailureInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class for building and formatting failure context information.
 * 
 * <p>This class provides stateless utility methods for creating comprehensive
 * failure context strings from FailureInfo objects, which are used to provide
 * context for AI analysis and chat history.</p>
 * 
 * <p>Key responsibilities:</p>
 * <ul>
 *   <li>Building failure context strings from FailureInfo objects</li>
 *   <li>Formatting failure information for display and analysis</li>
 *   <li>Handling null values and optional failure data</li>
 * </ul>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
public final class FailureContextUtils {
    
    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private FailureContextUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * Builds a comprehensive failure context string from a FailureInfo object.
     * This context string provides structured information about the test failure
     * that can be used for AI analysis and chat history.
     *
     * @param failureInfo the failure information to build context from
     * @return a formatted failure context string
     * @throws IllegalArgumentException if failureInfo is null
     */
    @NotNull
    public static String buildFailureContext(@NotNull FailureInfo failureInfo) {
        if (failureInfo == null) {
            throw new IllegalArgumentException("FailureInfo cannot be null");
        }
        
        StringBuilder contextBuilder = new StringBuilder();
        
        // Always include test name
        contextBuilder.append("Test Name: ").append(failureInfo.getScenarioName()).append("\n");
        
        // Add failed step if available
        if (failureInfo.getFailedStepText() != null && !failureInfo.getFailedStepText().trim().isEmpty()) {
            contextBuilder.append("Failed Step: ").append(failureInfo.getFailedStepText()).append("\n");
        }
        
        // Add error message if available
        if (failureInfo.getErrorMessage() != null && !failureInfo.getErrorMessage().trim().isEmpty()) {
            contextBuilder.append("Error: ").append(failureInfo.getErrorMessage()).append("\n");
        }
        
        // Add expected/actual values if both are available
        if (hasExpectedActualValues(failureInfo)) {
            contextBuilder.append("Expected: ").append(failureInfo.getExpectedValue()).append("\n");
            contextBuilder.append("Actual: ").append(failureInfo.getActualValue()).append("\n");
        }
        
        return contextBuilder.toString().trim();
    }
    
    /**
     * Builds a concise failure summary from a FailureInfo object.
     * This provides a shorter version suitable for quick reference.
     *
     * @param failureInfo the failure information to summarize
     * @return a concise failure summary string
     * @throws IllegalArgumentException if failureInfo is null
     */
    @NotNull
    public static String buildFailureSummary(@NotNull FailureInfo failureInfo) {
        if (failureInfo == null) {
            throw new IllegalArgumentException("FailureInfo cannot be null");
        }
        
        StringBuilder summary = new StringBuilder();
        summary.append(failureInfo.getScenarioName());
        
        if (failureInfo.getErrorMessage() != null && !failureInfo.getErrorMessage().trim().isEmpty()) {
            // Truncate long error messages for summary
            String error = failureInfo.getErrorMessage().trim();
            if (error.length() > 100) {
                error = error.substring(0, 97) + "...";
            }
            summary.append(" - ").append(error);
        }
        
        return summary.toString();
    }
    
    /**
     * Extracts the primary error message from a FailureInfo object.
     * This method handles null values and provides a fallback message.
     *
     * @param failureInfo the failure information
     * @return the primary error message or a default message
     * @throws IllegalArgumentException if failureInfo is null
     */
    @NotNull
    public static String getPrimaryErrorMessage(@NotNull FailureInfo failureInfo) {
        if (failureInfo == null) {
            throw new IllegalArgumentException("FailureInfo cannot be null");
        }
        
        if (failureInfo.getErrorMessage() != null && !failureInfo.getErrorMessage().trim().isEmpty()) {
            return failureInfo.getErrorMessage().trim();
        }
        
        if (failureInfo.getFailedStepText() != null && !failureInfo.getFailedStepText().trim().isEmpty()) {
            return "Failed at step: " + failureInfo.getFailedStepText().trim();
        }
        
        return "Test failure occurred";
    }
    
    /**
     * Checks if a FailureInfo object has both expected and actual values.
     *
     * @param failureInfo the failure information to check
     * @return true if both expected and actual values are present and non-empty
     * @throws IllegalArgumentException if failureInfo is null
     */
    public static boolean hasExpectedActualValues(@NotNull FailureInfo failureInfo) {
        if (failureInfo == null) {
            throw new IllegalArgumentException("FailureInfo cannot be null");
        }
        
        return failureInfo.getExpectedValue() != null && 
               !failureInfo.getExpectedValue().trim().isEmpty() &&
               failureInfo.getActualValue() != null && 
               !failureInfo.getActualValue().trim().isEmpty();
    }
    
    /**
     * Formats expected and actual values for display in failure context.
     *
     * @param expectedValue the expected value
     * @param actualValue the actual value
     * @return formatted string showing both values
     */
    @NotNull
    public static String formatExpectedActual(@Nullable String expectedValue, @Nullable String actualValue) {
        if (expectedValue == null || actualValue == null) {
            return "";
        }
        
        return String.format("Expected: %s\nActual: %s", expectedValue, actualValue);
    }
}
