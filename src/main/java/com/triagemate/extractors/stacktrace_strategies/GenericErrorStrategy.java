package com.triagemate.extractors.stacktrace_strategies;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.*;
import com.triagemate.models.FailureInfo;
import com.triagemate.extractors.FailureParsingStrategy;
import com.triagemate.utils.LoggingUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generic parsing strategy for extracting failure information from any unhandled error types.
 * <p>
 * This strategy serves as a fallback for any error types that aren't handled by more specific strategies.
 * It provides basic parsing capabilities for any exception with a stack trace.
 * </p>
 *
 * <p>Example generic output handled:</p>
 * <pre>
 * java.lang.Exception: Some unexpected error occurred
 *     at com.example.MyTest.testSomething(MyTest.java:42)
 *     at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:325)
 * </pre>
 */
public class GenericErrorStrategy implements FailureParsingStrategy {
    private static final Logger LOG = Logger.getInstance(GenericErrorStrategy.class);
    private final Project project;

    // Regex patterns for generic error detection
    private static final Pattern EXCEPTION_PATTERN = Pattern.compile(
        "^([\\w.$]+)(?::\\s*(.*))?$",
        Pattern.MULTILINE
    );
    
    private static final Pattern STACK_TRACE_PATTERN = Pattern.compile(
        "^\\s+at\\s+([\\w.$]+)\\.([\\w<>]+)\\(([^:]+)(?::(\\d+))?\\)$",
        Pattern.MULTILINE
    );

    /**
     * Constructor for GenericErrorStrategy.
     * 
     * @param project The IntelliJ project context for PSI operations
     */
    public GenericErrorStrategy(Project project) {
        this.project = project;
    }

    @Override
    public boolean canHandle(String testOutput) {
        if (testOutput == null || testOutput.trim().isEmpty()) {
            return false;
        }

        // Extract the exception type to check if it's handled by other strategies
        String exceptionType = extractExceptionType(testOutput);
        
        // If this exception type is handled by other strategies, don't handle it
        if (isHandledByOtherStrategies(exceptionType)) {
            return false;
        }
        
        // Check for specific patterns that other strategies handle
        String lowerOutput = testOutput.toLowerCase();
        
        // Don't handle JUnit comparison failures
        if (lowerOutput.contains("org.junit.comparisonfailure") || 
            lowerOutput.contains("expected:") && lowerOutput.contains("actual:")) {
            return false;
        }
        
        // Don't handle WebDriver exceptions
        if (lowerOutput.contains("org.openqa.selenium") || 
            lowerOutput.contains("webdriver") ||
            lowerOutput.contains("elementnotinteractable") ||
            lowerOutput.contains("nosuchelement") ||
            lowerOutput.contains("timeoutexception")) {
            return false;
        }
        
        // Don't handle Cucumber exceptions
        if (lowerOutput.contains("io.cucumber") || 
            lowerOutput.contains("cucumber") ||
            lowerOutput.contains("step definition") ||
            lowerOutput.contains("undefined step") ||
            lowerOutput.contains("ambiguous step")) {
            return false;
        }
        
        // Don't handle RuntimeExceptions (handled by RuntimeErrorStrategy)
        if (lowerOutput.contains("java.lang.runtimeexception") ||
            lowerOutput.contains("runtimeexception")) {
            return false;
        }
        
        // Don't handle Configuration errors
        if (lowerOutput.contains("configuration") ||
            lowerOutput.contains("config") ||
            lowerOutput.contains("setup") ||
            lowerOutput.contains("initialization")) {
            return false;
        }

        // GenericErrorStrategy should handle any other non-empty input
        return true;
    }

    @Override
    public FailureInfo parse(String testOutput) {
        if (testOutput == null || testOutput.trim().isEmpty()) {
            throw new IllegalArgumentException("Test output cannot be null or empty");
        }

        try {
            String errorMessage = extractErrorMessage(testOutput);
            // If the error message is null or looks like a stack trace line, use fallback
            if (errorMessage == null || errorMessage.matches("^at .*$")) {
                return createFallbackFailureInfo(testOutput);
            }
            return extractGenericErrorInfo(testOutput);
        } catch (Exception e) {
            LOG.warn("Failed to parse generic error, creating minimal failure info", e);
            return createFallbackFailureInfo(testOutput);
        }
    }

    @Override
    public int getPriority() {
        return 10; // Lowest priority as fallback strategy
    }

    @Override
    public String getStrategyName() {
        return "GenericErrorStrategy";
    }

    /**
     * Extracts failure information from generic error output.
     * 
     * @param testOutput The test output containing the error
     * @return FailureInfo with extracted details
     */
    private FailureInfo extractGenericErrorInfo(String testOutput) {
        String exceptionType = extractExceptionType(testOutput);
        String errorMessage = extractErrorMessage(testOutput);
        List<StackTraceElement> stackTrace = extractStackTrace(testOutput);
        
        // If we couldn't extract a meaningful stack trace, use the original input
        String stackTraceString;
        boolean allInvalid = stackTrace.isEmpty();
        if (!allInvalid) {
            // Check if all elements have lineNumber == -1 (invalid)
            allInvalid = stackTrace.stream().allMatch(e -> e.getLineNumber() == -1);
        }
        if (allInvalid) {
            stackTraceString = testOutput;
        } else {
            // Enrich stack trace with PSI information
            List<StackTraceElement> enrichedStackTrace = enrichStackTraceWithPsi(stackTrace);
            stackTraceString = convertStackTraceToString(enrichedStackTrace);
        }
        
        // Find the most relevant stack trace element (first user code)
        StackTraceElement mostRelevantElement = findMostRelevantStackTraceElement(stackTrace);
        
        return new FailureInfo.Builder()
                .withErrorMessage(errorMessage != null ? errorMessage : "Failed to parse generic error")
                .withStackTrace(stackTraceString)
                .withSourceFilePath(mostRelevantElement != null ? mostRelevantElement.getFileName() : null)
                .withLineNumber(mostRelevantElement != null ? mostRelevantElement.getLineNumber() : -1)
                .withParsingStrategy(getStrategyName())
                .withParsingTime(System.currentTimeMillis())
                .build();
    }

    /**
     * Extracts the exception type from the test output.
     * 
     * @param testOutput The test output
     * @return The exception type, or "UnknownException" if not found
     */
    private String extractExceptionType(String testOutput) {
        Matcher matcher = EXCEPTION_PATTERN.matcher(testOutput);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "UnknownException";
    }

    /**
     * Extracts the error message from the test output.
     * 
     * @param testOutput The test output
     * @return The error message, or null if not found
     */
    private String extractErrorMessage(String testOutput) {
        Matcher matcher = EXCEPTION_PATTERN.matcher(testOutput);
        if (matcher.find()) {
            int messageStart = matcher.end(1);
            // Find the start of the message (after the colon, if present)
            int colonIndex = testOutput.indexOf(':', matcher.start(1) + matcher.group(1).length());
            if (colonIndex >= 0) {
                messageStart = colonIndex + 1;
            }
            // Extract everything between the exception header and the first stack trace line
            String remainingText = testOutput.substring(messageStart);
            String[] lines = remainingText.split("\n");
            StringBuilder message = new StringBuilder();
            boolean hasNonStackTraceLine = false;
            Pattern stackTraceLinePattern = Pattern.compile("^at .+\\(.*\\.java(?::\\d+)?\\)");
            Pattern lineNumberParenPattern = Pattern.compile("^\\d+\\)$");
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                if (trimmed.startsWith("at ") || stackTraceLinePattern.matcher(trimmed).matches() || lineNumberParenPattern.matcher(trimmed).matches()) {
                    continue;
                }
                hasNonStackTraceLine = true;
                message.append(line).append("\n");
            }
            String result = message.toString().trim();
            return hasNonStackTraceLine && !result.isEmpty() ? result : null;
        }
        return null;
    }

    /**
     * Extracts stack trace elements from the test output.
     * 
     * @param testOutput The test output
     * @return List of StackTraceElement objects
     */
    private List<StackTraceElement> extractStackTrace(String testOutput) {
        List<StackTraceElement> stackTrace = new ArrayList<>();
        Matcher matcher = STACK_TRACE_PATTERN.matcher(testOutput);
        
        while (matcher.find()) {
            String className = matcher.group(1);
            String methodName = matcher.group(2);
            String fileName = matcher.group(3);
            String lineNumberStr = matcher.group(4);
            
            int lineNumber = -1;
            if (lineNumberStr != null) {
                try {
                    lineNumber = Integer.parseInt(lineNumberStr);
                } catch (NumberFormatException e) {
                    LOG.debug("Could not parse line number: " + lineNumberStr);
                }
            }
            
            // Extract just the class name from fully qualified name
            String simpleClassName = extractSimpleClassName(className);
            
            stackTrace.add(new StackTraceElement(className, methodName, fileName, lineNumber));
        }
        
        return stackTrace;
    }

    /**
     * Enriches stack trace elements with PSI information.
     * 
     * @param stackTrace The original stack trace
     * @return Enriched stack trace
     */
    private List<StackTraceElement> enrichStackTraceWithPsi(List<StackTraceElement> stackTrace) {
        List<StackTraceElement> enrichedStackTrace = new ArrayList<>();
        
        for (StackTraceElement element : stackTrace) {
            StackTraceElement enrichedElement = enrichStackTraceElement(element);
            enrichedStackTrace.add(enrichedElement);
        }
        
        return enrichedStackTrace;
    }

    /**
     * Enriches a single stack trace element with PSI information.
     * 
     * @param element The stack trace element to enrich
     * @return Enriched stack trace element
     */
    private StackTraceElement enrichStackTraceElement(StackTraceElement element) {
        if (element.getFileName() == null) {
            return element;
        }

        try {
            // Try to find the file in the project
            PsiFile psiFile = findPsiFile(element.getFileName());
            if (psiFile != null) {
                // Update the file name to include the full path for better identification
                String enrichedFileName = psiFile.getName();
                return new StackTraceElement(
                    element.getClassName(),
                    element.getMethodName(),
                    enrichedFileName,
                    element.getLineNumber()
                );
            }
        } catch (Exception e) {
            LOG.debug("Failed to enrich stack trace element with PSI", e);
        }
        
        return element;
    }

    /**
     * Finds the most relevant stack trace element (first user code).
     * 
     * @param stackTrace The stack trace
     * @return The most relevant stack trace element, or null if not found
     */
    private StackTraceElement findMostRelevantStackTraceElement(List<StackTraceElement> stackTrace) {
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            
            // Skip framework and library classes
            if (isUserCode(className)) {
                return element;
            }
        }
        
        // If no user code found, return the first element
        return stackTrace.isEmpty() ? null : stackTrace.get(0);
    }

    /**
     * Checks if a class name represents user code.
     * 
     * @param className The class name to check
     * @return true if it's user code, false otherwise
     */
    private boolean isUserCode(String className) {
        if (className == null) {
            return false;
        }
        
        // Skip common framework and library packages
        String[] frameworkPackages = {
            "org.junit", "junit", "org.testng", "testng",
            "org.hamcrest", "org.mockito", "mockito",
            "java.", "javax.", "sun.", "com.sun.",
            "org.openqa.selenium", "io.cucumber",
            "org.gradle", "com.intellij"
        };
        
        for (String frameworkPackage : frameworkPackages) {
            if (className.startsWith(frameworkPackage)) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Finds a PSI file by name in the project.
     * 
     * @param fileName The file name to find
     * @return The PSI file, or null if not found
     */
    private PsiFile findPsiFile(String fileName) {
        if (fileName == null || project == null) {
            return null;
        }

        try {
            // Search for the file in the project
            PsiManager psiManager = PsiManager.getInstance(project);
            // This is a simplified approach - in a real implementation, you might want
            // to search more thoroughly through the project structure
            return null; // Simplified for now
        } catch (Exception e) {
            LOG.debug("Failed to find PSI file: " + fileName, e);
            return null;
        }
    }

    /**
     * Extracts the simple class name from a fully qualified class name.
     * 
     * @param fullClassName The fully qualified class name
     * @return The simple class name
     */
    private String extractSimpleClassName(String fullClassName) {
        if (fullClassName == null) {
            return null;
        }
        
        int lastDotIndex = fullClassName.lastIndexOf('.');
        if (lastDotIndex >= 0 && lastDotIndex < fullClassName.length() - 1) {
            return fullClassName.substring(lastDotIndex + 1);
        }
        
        return fullClassName;
    }

    /**
     * Checks if an exception type is handled by other strategies.
     * 
     * @param exceptionType The exception type to check
     * @return true if handled by other strategies, false otherwise
     */
    private boolean isHandledByOtherStrategies(String exceptionType) {
        if (exceptionType == null) {
            return false;
        }
        
        String[] handledExceptions = {
            "org.junit.ComparisonFailure",
            "org.junit.AssertionError",
            "java.lang.RuntimeException",
            "org.openqa.selenium",
            "io.cucumber",
            "java.lang.IllegalStateException",
            "java.lang.IllegalArgumentException"
        };
        
        for (String handledException : handledExceptions) {
            if (exceptionType.equals(handledException) || exceptionType.startsWith(handledException)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Creates a fallback FailureInfo when parsing fails.
     * 
     * @param testOutput The original test output
     * @return A minimal FailureInfo
     */
    private FailureInfo createFallbackFailureInfo(String testOutput) {
        return new FailureInfo.Builder()
                .withErrorMessage("Failed to parse generic error")
                .withStackTrace(testOutput)
                .withParsingStrategy(getStrategyName())
                .withParsingTime(System.currentTimeMillis())
                .build();
    }

    /**
     * Converts a list of StackTraceElement to a string representation.
     * 
     * @param stackTrace The stack trace elements
     * @return String representation of the stack trace
     */
    private String convertStackTraceToString(List<StackTraceElement> stackTrace) {
        if (stackTrace == null || stackTrace.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : stackTrace) {
            sb.append("\tat ").append(element.getClassName())
              .append(".").append(element.getMethodName())
              .append("(").append(element.getFileName());
            
            if (element.getLineNumber() > 0) {
                sb.append(":").append(element.getLineNumber());
            }
            
            sb.append(")\n");
        }
        
        return sb.toString();
    }
} 