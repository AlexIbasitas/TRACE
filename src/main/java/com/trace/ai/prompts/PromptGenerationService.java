package com.trace.ai.prompts;

import com.trace.test.models.FailureInfo;

/**
 * Service for generating AI prompts from test failure information.
 * 
 * <p>This service creates structured prompts that can be used for AI analysis
 * of test failures. It focuses on generating clear, actionable prompts that
 * provide immediate value to users without requiring external services.</p>
 * 
 * <p>The service provides two levels of detail:</p>
 * <ul>
 *   <li><strong>Summary:</strong> Concise prompts for quick triage and analysis</li>
 *   <li><strong>Detailed:</strong> Comprehensive prompts with full context for thorough investigation</li>
 * </ul>
 */
public interface PromptGenerationService {
    
    /**
     * Generates a concise summary prompt for quick analysis.
     * 
     * <p>This method creates a shorter, focused prompt that highlights the key
     * failure details without extensive context. Ideal for:</p>
     * <ul>
     *   <li>Quick triage and initial assessment</li>
     *   <li>Chat-based AI interactions where space is limited</li>
     *   <li>When you need a fast overview of the failure</li>
     * </ul>
     * 
     * @param failureInfo The failure information to format
     * @return A concise prompt for quick analysis
     * @throws IllegalArgumentException if failureInfo is null
     */
    String generateSummaryPrompt(FailureInfo failureInfo);
    
    /**
     * Generates a detailed analysis prompt with full context.
     * 
     * <p>This method creates a comprehensive prompt that includes all available
     * context, including step definitions, scenario details, and full stack traces.
     * Ideal for:</p>
     * <ul>
     *   <li>Thorough investigation of complex failures</li>
     *   <li>When you need complete context for accurate analysis</li>
     *   <li>Deep debugging sessions</li>
     *   <li>Documentation and knowledge sharing</li>
     * </ul>
     * 
     * @param failureInfo The failure information to format
     * @return A detailed prompt with full context
     * @throws IllegalArgumentException if failureInfo is null
     */
    String generateDetailedPrompt(FailureInfo failureInfo);
} 