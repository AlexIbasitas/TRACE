package com.triagemate.extractors;

import com.intellij.execution.testframework.sm.runner.SMTestProxy;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.diagnostic.Logger;
import com.triagemate.models.FailureInfo;
import com.triagemate.listeners.TestOutputCaptureListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracts stack trace and failed step text from test failure information.
 * 
 * <p>This class uses a simple raw extraction approach to capture the full stack trace
 * and error information without complex parsing strategies. The raw output is preserved
 * for AI analysis, which can handle the parsing more intelligently.</p>
 * 
 * <p>The extractor focuses on capturing complete error information and basic metadata
 * extraction while maintaining simplicity and reliability.</p>
 */
public class StackTraceExtractor {
    private static final Logger LOG = Logger.getInstance(StackTraceExtractor.class);
    private final Project project;

    /**
     * Constructor for StackTraceExtractor.
     * 
     * @param project The IntelliJ project context for PSI operations
     */
    public StackTraceExtractor(Project project) {
        if (project == null) {
            throw new NullPointerException("Project cannot be null");
        }
        this.project = project;
    }

    /**
     * Extracts comprehensive failure information from test output using simple raw extraction.
     * 
     * <p>This method extracts the raw stack trace and basic information without complex
     * strategy-based parsing. The full stack trace is preserved for AI analysis.</p>
     * 
     * @param testOutput The test output to parse
     * @return FailureInfo containing extracted failure details
     * @throws IllegalArgumentException if testOutput is null
     */
    public FailureInfo extractFailureInfo(String testOutput) {
        if (testOutput == null) {
            throw new IllegalArgumentException("testOutput cannot be null");
        }
        
        LOG.debug("Starting raw failure info extraction", "outputLength", testOutput.length());
        
        // Use simple raw extraction instead of complex strategy pattern
        return extractRawFailureInfo(testOutput);
    }
    
    /**
     * Extracts comprehensive failure information from a test proxy, using captured output when available.
     * 
     * <p>This method first checks for captured test output from the TestOutputCaptureListener,
     * and falls back to the test's error message if no captured output is available.</p>
     * 
     * @param test The test proxy to extract failure information from
     * @return FailureInfo containing extracted failure details
     * @throws IllegalArgumentException if test is null
     */
    public FailureInfo extractFailureInfo(SMTestProxy test) {
        if (test == null) {
            throw new IllegalArgumentException("test cannot be null");
        }
        
        LOG.debug("Starting failure info extraction from test proxy", "testName", test.getName());
        
        // First, try to get captured output from the TestOutputCaptureListener
        String capturedOutput = TestOutputCaptureListener.getCapturedOutputWithChildren(test);
        
        if (capturedOutput != null && !capturedOutput.trim().isEmpty()) {
            LOG.debug("Using captured test output", "outputLength", capturedOutput.length());
            return extractRawFailureInfo(capturedOutput);
        }
        
        // Fallback to test's error message
        String errorMessage = test.getErrorMessage();
        if (errorMessage != null && !errorMessage.trim().isEmpty()) {
            LOG.debug("Using test error message as fallback", "messageLength", errorMessage.length());
            return extractRawFailureInfo(errorMessage);
        }
        
        // Last resort: create minimal failure info
        LOG.warn("No test output available for test: " + test.getName());
        return createMinimalFailureInfo("No error information available");
    }
    
    /**
     * Extracts failure information using simple raw extraction approach.
     * This preserves the full stack trace for AI analysis without complex parsing.
     * 
     * @param testOutput The test output to parse
     * @return FailureInfo with raw stack trace and basic extraction
     */
    private FailureInfo extractRawFailureInfo(String testOutput) {
        long startTime = System.currentTimeMillis();
        
        // Extract basic information from the first few lines
        String[] lines = testOutput.split("\n");
        String errorMessage = extractBasicErrorMessage(lines);
        String expectedValue = extractExpectedValue(lines);
        String actualValue = extractActualValue(lines);
        
        // Find the first user code line in stack trace
        String userCodeLine = extractFirstUserCodeLine(lines);
        String sourceFilePath = null;
        int lineNumber = -1;
        
        if (userCodeLine != null) {
            String[] fileInfo = parseFileAndLine(userCodeLine);
            if (fileInfo != null) {
                sourceFilePath = fileInfo[0];
                lineNumber = Integer.parseInt(fileInfo[1]);
            }
        }
        
        long parsingTime = System.currentTimeMillis() - startTime;
        
        return new FailureInfo.Builder()
                .withErrorMessage(errorMessage)
                .withExpectedValue(expectedValue)
                .withActualValue(actualValue)
                .withStackTrace(testOutput)
                .withSourceFilePath(sourceFilePath)
                .withLineNumber(lineNumber)
                .withParsingTime(parsingTime)
                .build();
    }
    
    /**
     * Extracts basic error message from the first few lines.
     */
    private String extractBasicErrorMessage(String[] lines) {
        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty() && !line.startsWith("at ") && !line.startsWith("Expected:") && !line.startsWith("but: was")) {
                return line;
            }
        }
        return "Error occurred during test execution";
    }
    
    /**
     * Extracts expected value if present in the output.
     */
    private String extractExpectedValue(String[] lines) {
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("Expected:")) {
                return line.substring("Expected:".length()).trim();
            }
        }
        return null;
    }
    
    /**
     * Extracts actual value if present in the output.
     */
    private String extractActualValue(String[] lines) {
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("but: was")) {
                return line.substring("but: was".length()).trim();
            }
        }
        return null;
    }
    
    /**
     * Extracts the first user code line from the stack trace.
     */
    private String extractFirstUserCodeLine(String[] lines) {
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("at ") && 
                !line.contains("junit.") && 
                !line.contains("org.junit") &&
                !line.contains("hamcrest.") &&
                !line.contains("org.hamcrest") &&
                !line.contains("cucumber.") &&
                !line.contains("io.cucumber")) {
                return line;
            }
        }
        return null;
    }
    
    /**
     * Parses file name and line number from a stack trace line.
     */
    private String[] parseFileAndLine(String stackTraceLine) {
        try {
            // Remove "at " prefix
            String content = stackTraceLine.substring(3);
            
            // Find the last parenthesis which contains file and line info
            int lastParen = content.lastIndexOf('(');
            int lastParenClose = content.lastIndexOf(')');
            
            if (lastParen > 0 && lastParenClose > lastParen) {
                String filePart = content.substring(lastParen + 1, lastParenClose);
                
                // Parse file and line
                String[] fileInfo = filePart.split(":");
                if (fileInfo.length >= 2) {
                    return new String[]{fileInfo[0], fileInfo[1]};
                }
            }
        } catch (Exception e) {
            LOG.debug("Failed to parse file and line from: " + stackTraceLine, e);
        }
        
        return null;
    }

    /**
     * Extracts the stack trace from a failed test using simple raw extraction.
     *
     * @param test The failed test proxy
     * @return The full raw stack trace as a string
     */
    public String extractStackTrace(SMTestProxy test) {
        if (test == null) {
            return "Test proxy is null";
        }
        
        String errorMessage = test.getErrorMessage();
        if (errorMessage == null || errorMessage.isEmpty()) {
        return "Stack trace not available";
    }
        
        // Return the full raw error message - let AI handle the analysis
            return errorMessage;
    }

    /**
     * Extracts the text of the failed step from the test information using simple pattern matching.
     *
     * @param test The failed test proxy
     * @return The text of the failed step
     */
    public String extractFailedStepText(SMTestProxy test) {
        if (test == null) {
            return "Test proxy is null";
        }
        
        String errorMessage = test.getErrorMessage();
        if (errorMessage == null || errorMessage.isEmpty()) {
            return "Failed step text not available";
        }
        
        // Try to extract step text from Cucumber exceptions
        String stepText = extractStepTextFromCucumberException(errorMessage);
        if (stepText != null) {
            return stepText;
        }
        
        // Fallback to basic extraction from test name
        if (test.getName() != null && test.getName().contains("Step:")) {
            return test.getName().split("Step:")[1].trim();
        }
        
        return "Failed step text not available";
    }
    
    /**
     * Extracts step text from Cucumber exception messages.
     * 
     * @param errorMessage The error message to parse
     * @return The extracted step text or null if not found
     */
    private String extractStepTextFromCucumberException(String errorMessage) {
        // Pattern for Cucumber UndefinedStepException
        if (errorMessage.contains("UndefinedStepException")) {
            // Look for pattern: "The step "step text" is undefined"
            int startQuote = errorMessage.indexOf("\"");
            int endQuote = errorMessage.indexOf("\"", startQuote + 1);
            if (startQuote > 0 && endQuote > startQuote) {
                return errorMessage.substring(startQuote + 1, endQuote);
            }
        }
        
        // Pattern for other Cucumber exceptions
        if (errorMessage.contains("cucumber") || errorMessage.contains("Cucumber")) {
            // Look for quoted text that might be step text
            String[] lines = errorMessage.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.contains("\"") && !line.startsWith("at ")) {
                    int startQuote = line.indexOf("\"");
                    int endQuote = line.indexOf("\"", startQuote + 1);
                    if (startQuote > 0 && endQuote > startQuote) {
                        String potentialStep = line.substring(startQuote + 1, endQuote);
                        if (potentialStep.length() > 5 && !potentialStep.contains(".")) {
                            return potentialStep;
                        }
                    }
                }
            }
        }
        
        return null;
    }

    /**
     * Creates a minimal failure info when no test output is available.
     * 
     * @param testOutput The original test output
     * @return A minimal FailureInfo with basic information
     */
    private FailureInfo createMinimalFailureInfo(String testOutput) {
        String errorMessage = extractBasicErrorMessage(testOutput);
        return new FailureInfo.Builder()
                .withErrorMessage(errorMessage)
                .withStackTrace(testOutput)
                .withParsingTime(System.currentTimeMillis())
                .build();
    }

    /**
     * Extracts a basic error message from test output as fallback.
     * 
     * @param testOutput The test output
     * @return Basic error message
     */
    private String extractBasicErrorMessage(String testOutput) {
        try {
            String[] lines = testOutput.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("at ")) {
                    return line;
                }
            }
        } catch (Exception e) {
            LOG.debug("Failed to extract basic error message", e);
        }
        return "Unknown error occurred";
    }

    /**
     * Gets the number of available parsing strategies.
     * 
     * @return The number of strategies (always 1 for raw extraction)
     */
    public int getStrategyCount() {
        return 1;
    }

    /**
     * Gets the list of available strategies for debugging purposes.
     * 
     * @return List of strategy names
     */
    public List<String> getStrategyNames() {
        List<String> names = new ArrayList<>();
        names.add("StackTraceExtraction (Default)");
        return names;
    }
} 