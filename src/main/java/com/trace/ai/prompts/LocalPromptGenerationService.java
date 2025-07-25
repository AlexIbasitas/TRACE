package com.trace.ai.prompts;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.trace.test.models.FailureInfo;
import com.trace.test.models.GherkinScenarioInfo;
import com.trace.test.models.StepDefinitionInfo;

import java.util.List;

/**
 * Local implementation of prompt generation service.
 * 
 * <p>This service generates structured AI prompts from test failure information
 * following OpenAI and Prompting Guide best practices. It creates prompts that are
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
        
        // Clear instruction at the beginning with specific role and task
        prompt.append("### Instruction ###\n");
        prompt.append("You are an expert test automation engineer. Analyze this test failure and provide a concise summary.\n\n");
        
        // Context section
        prompt.append("### Context ###\n");
        appendSummaryContext(prompt, failureInfo);
        
        // Clear output format request with specific structure
        prompt.append("### Output Format ###\n");
        prompt.append("Provide your analysis in this exact format:\n");
        prompt.append("- **Issue:** Brief description of what went wrong\n");
        prompt.append("- **Likely Cause:** Most probable reason for the failure\n");
        prompt.append("- **Suggested Fix:** Specific action to resolve the issue\n");
        
        return prompt.toString();
    }
    
    @Override
    public String generateDetailedPrompt(FailureInfo failureInfo) {
        if (failureInfo == null) {
            throw new IllegalArgumentException("failureInfo cannot be null");
        }
        
        LOG.debug("Generating detailed prompt for scenario: " + failureInfo.getScenarioName());
        StepDefinitionInfo stepDefInfo = failureInfo.getStepDefinitionInfo();
        if (stepDefInfo == null) {
            LOG.warn("LocalPromptGenerationService: StepDefinitionInfo is null");
        } else {
            LOG.info("LocalPromptGenerationService: StepDefinitionInfo present. Method: " + stepDefInfo.getMethodName() + ", Class: " + stepDefInfo.getClassName());
            if (stepDefInfo.getMethodText() == null) {
                LOG.warn("LocalPromptGenerationService: StepDefinitionInfo.methodText is null");
            } else {
                LOG.info("LocalPromptGenerationService: StepDefinitionInfo.methodText length: " + stepDefInfo.getMethodText().length());
            }
        }
        StringBuilder prompt = new StringBuilder();
        
        // Enhanced instruction with specific role, expertise, and step-by-step guidance
        prompt.append("### Instruction ###\n");
        prompt.append("You are an expert test automation engineer with deep expertise in Cucumber, Selenium, and test failure analysis. ");
        prompt.append("Your role is to analyze this test failure systematically and provide actionable guidance.\n\n");
        
        prompt.append("**Your Task:**\n");
        prompt.append("1. Analyze the failure evidence step-by-step\n");
        prompt.append("2. Classify the failure type and root cause\n");
        prompt.append("3. Provide specific, actionable recommendations\n");
        prompt.append("4. Focus on whether this represents a product defect, automation issue, data problem, or environment issue\n\n");
        
        prompt.append("**Analysis Approach:**\n");
        prompt.append("- Take your time to examine all provided evidence\n");
        prompt.append("- Consider the relationship between the Gherkin scenario, step definition, and error details\n");
        prompt.append("- Base your conclusions on the technical evidence provided\n");
        prompt.append("- Provide confidence levels for your assessments\n\n");
        
        // Structured context with clear sections and improved organization
        prompt.append("### Test Failure Context ###\n");
        appendFailureContext(prompt, failureInfo);
        
        prompt.append("### Error Details ###\n");
        appendErrorDetails(prompt, failureInfo);
        
        // Add Gherkin scenario context only if available
        GherkinScenarioInfo scenarioInfo = failureInfo.getGherkinScenarioInfo();
        if (scenarioInfo != null && scenarioInfo.getSteps() != null && !scenarioInfo.getSteps().isEmpty()) {
            prompt.append("### Gherkin Scenario ###\n");
            appendGherkinScenario(prompt, failureInfo);
        }
        
        // Add step definition context only if available
        if (stepDefInfo != null && stepDefInfo.getMethodText() != null) {
            LOG.info("LocalPromptGenerationService: Including step definition in prompt");
            prompt.append("### Step Definition ###\n");
            appendStepDefinition(prompt, failureInfo);
        } else {
            LOG.warn("LocalPromptGenerationService: Step definition not included in prompt - missing method text");
        }
        
        prompt.append("### Code Context ###\n");
        appendCodeContext(prompt, failureInfo);
        
        // Enhanced analysis request with specific guidance and examples
        prompt.append("### Analysis Request ###\n");
        prompt.append("Provide your analysis in this exact format. Be specific and actionable:\n\n");
        
        prompt.append("## Failure Analysis\n");
        prompt.append("- **Failure Type:** [Assertion/Exception/Configuration/Environment/Other] - Choose the most specific category\n");
        prompt.append("- **Likely Cause:** [Product Defect/Automation Issue/Data Issue/Environment Issue/Test Design Issue] - Select the primary cause\n");
        prompt.append("- **Confidence:** [High/Medium/Low] - Based on the evidence quality and your analysis\n\n");
        
        prompt.append("## Technical Details\n");
        prompt.append("- **What Failed:** [Specific description of what the test was trying to do and what actually happened]\n");
        prompt.append("- **Why It Failed:** [Technical explanation based on the evidence - reference specific parts of the stack trace, step definition, or scenario]\n\n");
        
        prompt.append("## Recommended Actions\n");
        prompt.append("- **Immediate Steps:** [Specific, actionable steps to resolve this issue - be concrete]\n");
        prompt.append("- **Investigation Areas:** [What to check next to confirm the root cause]\n");
        prompt.append("- **Test Improvements:** [How to make this test more robust and prevent similar failures]\n\n");
        
        prompt.append("**Important:** Base your analysis on the evidence provided. If you need more information, specify what additional context would help.\n");
        
        LOG.info("LocalPromptGenerationService: Final generated prompt:\n" + prompt.toString());
        return prompt.toString();
    }
    
    private void appendSummaryContext(StringBuilder prompt, FailureInfo failureInfo) {
        if (failureInfo.getScenarioName() != null) {
            prompt.append("**Scenario:** ").append(failureInfo.getScenarioName()).append("\n");
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
        
        prompt.append("\n");
    }
    
    private void appendFailureContext(StringBuilder prompt, FailureInfo failureInfo) {
        // Test identification with clear labeling
        if (failureInfo.getScenarioName() != null) {
            prompt.append("**Test Name:** ").append(failureInfo.getScenarioName()).append("\n");
        }
        
        if (failureInfo.getFailedStepText() != null) {
            prompt.append("**Failed Step:** ").append(failureInfo.getFailedStepText()).append("\n");
        }
        
        prompt.append("\n");
    }
    
    private void appendErrorDetails(StringBuilder prompt, FailureInfo failureInfo) {
        // Assertion details (most important for triage) with clear labeling
        if (failureInfo.getExpectedValue() != null && failureInfo.getActualValue() != null) {
            prompt.append("**Expected Value:** ").append(failureInfo.getExpectedValue()).append("\n");
            prompt.append("**Actual Value:** ").append(failureInfo.getActualValue()).append("\n");
        }
        
        // Enhanced stack trace presentation with better formatting
        if (failureInfo.getStackTrace() != null && !failureInfo.getStackTrace().trim().isEmpty()) {
            String cleanStackTrace = cleanStackTrace(failureInfo.getStackTrace());
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
    
    private void appendGherkinScenario(StringBuilder prompt, FailureInfo failureInfo) {
        GherkinScenarioInfo scenarioInfo = failureInfo.getGherkinScenarioInfo();
        if (scenarioInfo != null) {
            if (scenarioInfo.getFeatureName() != null) {
                prompt.append("**Feature:** ").append(scenarioInfo.getFeatureName()).append("\n");
            }
            
            if (scenarioInfo.getScenarioName() != null) {
                prompt.append("**Scenario:** ").append(scenarioInfo.getScenarioName()).append("\n");
            }
            
            List<String> tags = scenarioInfo.getTags();
            if (tags != null && !tags.isEmpty()) {
                prompt.append("**Tags:** ").append(String.join(", ", tags)).append("\n");
            }
            
            // Use the full scenario text which includes examples table for scenario outlines
            String fullScenarioText = scenarioInfo.getFullScenarioText();
            if (fullScenarioText != null && !fullScenarioText.trim().isEmpty()) {
                prompt.append("**Full Scenario:**\n```gherkin\n");
                prompt.append(fullScenarioText);
                prompt.append("```\n");
            } else {
                // Fallback to just steps if full scenario text is not available
                List<String> steps = scenarioInfo.getSteps();
                if (steps != null && !steps.isEmpty()) {
                    prompt.append("**Full Scenario:**\n```gherkin\n");
                    for (String step : steps) {
                        prompt.append(step).append("\n");
                    }
                    prompt.append("```\n");
                }
            }
        }
        
        prompt.append("\n");
    }
    
    private void appendStepDefinition(StringBuilder prompt, FailureInfo failureInfo) {
        StepDefinitionInfo stepDefInfo = failureInfo.getStepDefinitionInfo();
        if (stepDefInfo != null) {
            if (stepDefInfo.getClassName() != null) {
                prompt.append("**Class:** ").append(stepDefInfo.getClassName()).append("\n");
            }
            
            if (stepDefInfo.getMethodName() != null) {
                prompt.append("**Method:** ").append(stepDefInfo.getMethodName()).append("\n");
            }
            
            if (stepDefInfo.getStepPattern() != null) {
                prompt.append("**Pattern:** ").append(stepDefInfo.getStepPattern()).append("\n");
            }
            
            List<String> parameters = stepDefInfo.getParameters();
            if (parameters != null && !parameters.isEmpty()) {
                prompt.append("**Parameters:** ").append(String.join(", ", parameters)).append("\n");
            }
            
            // Method implementation (crucial for understanding what the step does)
            if (stepDefInfo.getMethodText() != null) {
                prompt.append("**Implementation:**\n```java\n").append(stepDefInfo.getMethodText()).append("\n```\n");
            }
        }
        
        prompt.append("\n");
    }
    
    private void appendCodeContext(StringBuilder prompt, FailureInfo failureInfo) {
        // Source location with clear labeling
        if (failureInfo.getSourceFilePath() != null) {
            // Extract just the filename, not the full path
            String fileName = extractFileName(failureInfo.getSourceFilePath());
            prompt.append("**Source File:** ").append(fileName).append("\n");
        }
        
        if (failureInfo.getLineNumber() > 0) {
            prompt.append("**Line Number:** ").append(failureInfo.getLineNumber()).append("\n");
        }
        
        prompt.append("\n");
    }
    
    private String extractFileName(String filePath) {
        if (filePath == null) return null;
        int lastSlash = filePath.lastIndexOf('/');
        int lastBackslash = filePath.lastIndexOf('\\');
        int lastSeparator = Math.max(lastSlash, lastBackslash);
        return lastSeparator >= 0 ? filePath.substring(lastSeparator + 1) : filePath;
    }
} 