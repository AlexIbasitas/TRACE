package com.triagemate.services;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.triagemate.models.FailureInfo;
import com.triagemate.models.GherkinScenarioInfo;
import com.triagemate.models.StepDefinitionInfo;

import java.util.List;

/**
 * Local implementation of prompt generation service.
 * 
 * <p>This service generates structured AI prompts from test failure information
 * without requiring any external dependencies. It creates prompts that are
 * immediately useful for users who want to copy them to their preferred AI service.</p>
 * 
 * <p>The service provides two levels of detail:</p>
 * <ul>
 *   <li><strong>Summary:</strong> Concise prompts for quick triage and analysis</li>
 *   <li><strong>Detailed:</strong> Comprehensive prompts with full context for thorough investigation</li>
 * </ul>
 */
@Service
public final class LocalPromptGenerationService implements PromptGenerationService {
    
    private static final Logger LOG = Logger.getInstance(LocalPromptGenerationService.class);
    
    @Override
    public String generateSummaryPrompt(FailureInfo failureInfo) {
        if (failureInfo == null) {
            throw new IllegalArgumentException("failureInfo cannot be null");
        }
        
        StringBuilder prompt = new StringBuilder();
        
        // Add concise header
        prompt.append("Test Failure Summary:\n\n");
        
        // Add key failure details
        prompt.append("**Scenario:** ").append(failureInfo.getScenarioName()).append("\n");
        prompt.append("**Failed Step:** ").append(failureInfo.getFailedStepText()).append("\n");
        prompt.append("**Error:** ").append(failureInfo.getErrorMessage()).append("\n");
        
        // Add assertion details if available
        if (failureInfo.getExpectedValue() != null && failureInfo.getActualValue() != null) {
            prompt.append("**Expected:** ").append(failureInfo.getExpectedValue()).append("\n");
            prompt.append("**Actual:** ").append(failureInfo.getActualValue()).append("\n");
        }
        
        // Add brief analysis request
        prompt.append("\n**Please provide:**\n");
        prompt.append("- Likely cause of the failure\n");
        prompt.append("- Suggested fix\n");
        
        return prompt.toString();
    }
    
    @Override
    public String generateDetailedPrompt(FailureInfo failureInfo) {
        if (failureInfo == null) {
            throw new IllegalArgumentException("failureInfo cannot be null");
        }
        
        LOG.debug("Generating detailed prompt for scenario: " + failureInfo.getScenarioName());
        
        StringBuilder prompt = new StringBuilder();
        
        // Add comprehensive header
        prompt.append("# Comprehensive Test Failure Analysis\n\n");
        
        // Add full test context
        appendDetailedTestContext(prompt, failureInfo);
        
        // Add complete error details
        appendDetailedErrorDetails(prompt, failureInfo);
        
        // Add full code context
        appendDetailedCodeContext(prompt, failureInfo);
        
        // Add scenario details
        appendScenarioDetails(prompt, failureInfo);
        
        // Add comprehensive analysis request
        appendDetailedAnalysisRequest(prompt, failureInfo);
        
        return prompt.toString();
    }
    

    
    private void appendDetailedTestContext(StringBuilder prompt, FailureInfo failureInfo) {
        prompt.append("## Detailed Test Context\n\n");
        
        if (failureInfo.getScenarioName() != null) {
            prompt.append("**Scenario:** ").append(failureInfo.getScenarioName()).append("\n");
        }
        
        if (failureInfo.getFailedStepText() != null) {
            prompt.append("**Failed Step:** ").append(failureInfo.getFailedStepText()).append("\n");
        }
        
        if (failureInfo.getParsingTime() > 0) {
            prompt.append("**Parsing Time:** ").append(failureInfo.getParsingTime()).append("ms\n");
        }
        
        prompt.append("\n");
    }
    
    private void appendDetailedErrorDetails(StringBuilder prompt, FailureInfo failureInfo) {
        prompt.append("## Detailed Error Information\n\n");
        
        if (failureInfo.getErrorMessage() != null) {
            prompt.append("**Error Message:** ").append(failureInfo.getErrorMessage()).append("\n");
        }
        
        if (failureInfo.getAssertionType() != null) {
            prompt.append("**Error Type:** ").append(failureInfo.getAssertionType()).append("\n");
        }
        
        if (failureInfo.getExpectedValue() != null && failureInfo.getActualValue() != null) {
            prompt.append("**Expected Value:** ").append(failureInfo.getExpectedValue()).append("\n");
            prompt.append("**Actual Value:** ").append(failureInfo.getActualValue()).append("\n");
        }
        
        // Add full stack trace if available
        if (failureInfo.getStackTrace() != null && !failureInfo.getStackTrace().trim().isEmpty()) {
            prompt.append("**Stack Trace:**\n```\n").append(failureInfo.getStackTrace()).append("\n```\n");
        }
        
        prompt.append("\n");
    }
    
    private void appendDetailedCodeContext(StringBuilder prompt, FailureInfo failureInfo) {
        prompt.append("## Detailed Code Context\n\n");
        
        if (failureInfo.getSourceFilePath() != null) {
            prompt.append("**Source File:** ").append(failureInfo.getSourceFilePath()).append("\n");
        }
        
        if (failureInfo.getLineNumber() > 0) {
            prompt.append("**Line Number:** ").append(failureInfo.getLineNumber()).append("\n");
        }
        
        // Add detailed step definition info
        StepDefinitionInfo stepDefInfo = failureInfo.getStepDefinitionInfo();
        if (stepDefInfo != null) {
            prompt.append("**Step Definition Class:** ").append(stepDefInfo.getClassName()).append("\n");
            prompt.append("**Step Definition Method:** ").append(stepDefInfo.getMethodName()).append("\n");
            prompt.append("**Step Pattern:** ").append(stepDefInfo.getStepPattern()).append("\n");
            
            List<String> parameters = stepDefInfo.getParameters();
            if (parameters != null && !parameters.isEmpty()) {
                prompt.append("**Parameters:** ").append(String.join(", ", parameters)).append("\n");
            }
            
            if (stepDefInfo.getMethodText() != null) {
                prompt.append("**Method Implementation:**\n```java\n").append(stepDefInfo.getMethodText()).append("\n```\n");
            }
        }
        
        prompt.append("\n");
    }
    
    private void appendScenarioDetails(StringBuilder prompt, FailureInfo failureInfo) {
        GherkinScenarioInfo scenarioInfo = failureInfo.getGherkinScenarioInfo();
        if (scenarioInfo != null) {
            prompt.append("## Scenario Details\n\n");
            
            if (scenarioInfo.getFeatureName() != null) {
                prompt.append("**Feature:** ").append(scenarioInfo.getFeatureName()).append("\n");
            }
            
            if (scenarioInfo.getScenarioName() != null) {
                prompt.append("**Scenario:** ").append(scenarioInfo.getScenarioName()).append("\n");
            }
            
            List<String> tags = scenarioInfo.getTags();
            if (tags != null && !tags.isEmpty()) {
                prompt.append("**Tags:** ").append(String.join(" ", tags)).append("\n");
            }
            
            List<String> steps = scenarioInfo.getSteps();
            if (steps != null && !steps.isEmpty()) {
                prompt.append("**Steps:**\n");
                for (String step : steps) {
                    prompt.append("- ").append(step).append("\n");
                }
            }
            
            prompt.append("\n");
        }
    }
    
    private void appendDetailedAnalysisRequest(StringBuilder prompt, FailureInfo failureInfo) {
        prompt.append("## Comprehensive Analysis Request\n\n");
        
        prompt.append("Please provide a thorough analysis of this test failure including:\n\n");
        prompt.append("1. **Root Cause Analysis:** What is the most likely cause of this failure?\n");
        prompt.append("2. **Technical Details:** Explain the technical aspects of the failure\n");
        prompt.append("3. **Suggested Fix:** Provide specific, actionable steps to resolve the issue\n");
        prompt.append("4. **Code Review:** Are there any code quality issues that contributed to this failure?\n");
        prompt.append("5. **Test Design:** Could the test be improved to be more robust?\n");
        prompt.append("6. **Prevention Strategies:** How can similar failures be prevented in the future?\n");
        prompt.append("7. **Additional Investigation:** What other areas should be investigated?\n\n");
        
        prompt.append("Provide practical, implementable advice that a test developer can use immediately.\n");
    }
} 