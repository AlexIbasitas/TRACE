package com.triagemate.services;

import com.intellij.openapi.components.Service;
import com.triagemate.models.FailureInfo;

/**
 * Service for formatting failure information into a structured prompt for AI analysis.
 */
@Service
public final class PromptFormatterService {

    /**
     * Formats the failure information into a structured prompt for AI analysis
     *
     * @param failureInfo The failure information to format
     * @return A formatted prompt string
     */
    public String formatPrompt(FailureInfo failureInfo) {
        // Placeholder implementation
        // In the actual implementation, we'll create a well-structured prompt
        // that includes all relevant information for AI analysis
        
        StringBuilder promptBuilder = new StringBuilder();
        
        promptBuilder.append("# Test Failure Analysis Request\n\n");
        
        // Include scenario information
        promptBuilder.append("## Failed Scenario\n");
        promptBuilder.append("```gherkin\n");
        promptBuilder.append(failureInfo.getGherkinScenario());
        promptBuilder.append("\n```\n\n");
        
        // Include failed step
        promptBuilder.append("## Failed Step\n");
        promptBuilder.append("`").append(failureInfo.getFailedStepText()).append("`\n\n");
        
        // Include step definition
        promptBuilder.append("## Step Definition Method\n");
        promptBuilder.append("File: ").append(failureInfo.getSourceFilePath()).append("\n");
        promptBuilder.append("Line: ").append(failureInfo.getLineNumber()).append("\n");
        promptBuilder.append("```java\n");
        promptBuilder.append(failureInfo.getStepDefinitionMethod());
        promptBuilder.append("\n```\n\n");
        
        // Include stack trace
        promptBuilder.append("## Stack Trace\n");
        promptBuilder.append("```\n");
        promptBuilder.append(failureInfo.getStackTrace());
        promptBuilder.append("\n```\n\n");
        
        // Include analysis request
        promptBuilder.append("## Analysis Request\n");
        promptBuilder.append("Please analyze this test failure and suggest possible causes and solutions.");
        
        return promptBuilder.toString();
    }
} 