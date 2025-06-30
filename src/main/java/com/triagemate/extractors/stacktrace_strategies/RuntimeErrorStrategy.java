package com.triagemate.extractors.stacktrace_strategies;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.triagemate.models.FailureInfo;
import com.triagemate.extractors.FailureParsingStrategy;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parsing strategy for extracting failure information from runtime exceptions.
 * <p>
 * This strategy handles general runtime exceptions such as:
 * - NullPointerException
 * - IllegalArgumentException
 * - IllegalStateException
 * - ArrayIndexOutOfBoundsException
 * - ClassCastException
 * - NumberFormatException
 * - IndexOutOfBoundsException
 * - ConcurrentModificationException
 * - SecurityException
 * </p>
 *
 * <p>Example runtime output handled:</p>
 * <pre>
 * java.lang.NullPointerException: Cannot invoke "String.length()" because "str" is null
 *     at com.example.MyTest.testSomething(MyTest.java:42)
 * </pre>
 */
public class RuntimeErrorStrategy implements FailureParsingStrategy {
    private static final Logger LOG = Logger.getInstance(RuntimeErrorStrategy.class);
    private final Project project;
    
    /**
     * Regex patterns for common runtime exceptions.
     * These are minimal and only used for initial detection and basic extraction.
     */
    private static final Pattern NULL_POINTER_PATTERN = Pattern.compile(
            "java\\.lang\\.NullPointerException:\\s*(.+?)(?:\\s*at\\s|$)",
            Pattern.DOTALL);
    
    private static final Pattern ILLEGAL_ARGUMENT_PATTERN = Pattern.compile(
            "java\\.lang\\.IllegalArgumentException:\\s*(.+?)(?:\\s*at\\s|$)",
            Pattern.DOTALL);
    
    private static final Pattern ILLEGAL_STATE_PATTERN = Pattern.compile(
            "java\\.lang\\.IllegalStateException:\\s*(.+?)(?:\\s*at\\s|$)",
            Pattern.DOTALL);
    
    private static final Pattern ARRAY_INDEX_OUT_OF_BOUNDS_PATTERN = Pattern.compile(
            "java\\.lang\\.ArrayIndexOutOfBoundsException:\\s*(.+?)(?:\\s*at\\s|$)",
            Pattern.DOTALL);
    
    private static final Pattern CLASS_CAST_PATTERN = Pattern.compile(
            "java\\.lang\\.ClassCastException:\\s*(.+?)(?:\\s*at\\s|$)",
            Pattern.DOTALL);
    
    private static final Pattern NUMBER_FORMAT_PATTERN = Pattern.compile(
            "java\\.lang\\.NumberFormatException:\\s*(.+?)(?:\\s*at\\s|$)",
            Pattern.DOTALL);
    
    private static final Pattern INDEX_OUT_OF_BOUNDS_PATTERN = Pattern.compile(
            "java\\.lang\\.IndexOutOfBoundsException:\\s*(.+?)(?:\\s*at\\s|$)",
            Pattern.DOTALL);
    
    private static final Pattern CONCURRENT_MODIFICATION_PATTERN = Pattern.compile(
            "java\\.util\\.ConcurrentModificationException:\\s*(.+?)(?:\\s*at\\s|$)",
            Pattern.DOTALL);
    
    private static final Pattern SECURITY_PATTERN = Pattern.compile(
            "java\\.lang\\.SecurityException:\\s*(.+?)(?:\\s*at\\s|$)",
            Pattern.DOTALL);
    
    private static final Pattern GENERIC_RUNTIME_PATTERN = Pattern.compile(
            "java\\.lang\\.RuntimeException:\\s*(.+?)(?:\\s*at\\s|$)",
            Pattern.DOTALL);

    /**
     * Constructor for RuntimeErrorStrategy.
     * 
     * @param project The IntelliJ project context for PSI operations
     */
    public RuntimeErrorStrategy(Project project) {
        this.project = project;
    }

    @Override
    public boolean canHandle(String testOutput) {
        if (testOutput == null || testOutput.isEmpty()) {
            return false;
        }
        
        // Use only regex patterns for detection - no PSI operations
        return NULL_POINTER_PATTERN.matcher(testOutput).find() ||
               ILLEGAL_ARGUMENT_PATTERN.matcher(testOutput).find() ||
               ILLEGAL_STATE_PATTERN.matcher(testOutput).find() ||
               ARRAY_INDEX_OUT_OF_BOUNDS_PATTERN.matcher(testOutput).find() ||
               CLASS_CAST_PATTERN.matcher(testOutput).find() ||
               NUMBER_FORMAT_PATTERN.matcher(testOutput).find() ||
               INDEX_OUT_OF_BOUNDS_PATTERN.matcher(testOutput).find() ||
               CONCURRENT_MODIFICATION_PATTERN.matcher(testOutput).find() ||
               SECURITY_PATTERN.matcher(testOutput).find() ||
               GENERIC_RUNTIME_PATTERN.matcher(testOutput).find() ||
               testOutput.contains("java.lang.RuntimeException") ||
               testOutput.contains("java.lang.NullPointerException") ||
               testOutput.contains("java.lang.IllegalArgumentException") ||
               testOutput.contains("java.lang.IllegalStateException") ||
               testOutput.contains("java.lang.ArrayIndexOutOfBoundsException") ||
               testOutput.contains("java.lang.ClassCastException") ||
               testOutput.contains("java.lang.NumberFormatException") ||
               testOutput.contains("java.lang.IndexOutOfBoundsException") ||
               testOutput.contains("java.util.ConcurrentModificationException") ||
               testOutput.contains("java.lang.SecurityException");
    }

    @Override
    public FailureInfo parse(String testOutput) {
        if (testOutput == null) {
            throw new IllegalArgumentException("testOutput cannot be null");
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Extract error type and message using minimal regex
            RuntimeErrorInfo errorInfo = extractRuntimeErrorInfo(testOutput);
            
            // Use PSI-based stack trace analysis for accurate code navigation
            StackTraceInfo stackTraceInfo = extractStackTraceInfo(testOutput);
            
            // Build the failure info
            FailureInfo failureInfo = new FailureInfo.Builder()
                    .withErrorMessage(errorInfo.getMessage())
                    .withAssertionType(errorInfo.getErrorType())
                    .withStackTrace(stackTraceInfo.getStackTrace())
                    .withStepDefinitionMethod(stackTraceInfo.getMethodName())
                    .withSourceFilePath(stackTraceInfo.getSourceFilePath())
                    .withLineNumber(stackTraceInfo.getLineNumber())
                    .withParsingStrategy(getStrategyName())
                    .withParsingTime(System.currentTimeMillis() - startTime)
                    .build();
            
            LOG.debug("Successfully parsed runtime error", 
                    "errorType", errorInfo.getErrorType(),
                    "message", errorInfo.getMessage(),
                    "duration", System.currentTimeMillis() - startTime);
            
            return failureInfo;
            
        } catch (Exception e) {
            LOG.warn("Failed to parse runtime error, creating minimal failure info", e);
            
            // Create minimal failure info as fallback
            return new FailureInfo.Builder()
                    .withErrorMessage("Runtime error: " + extractBasicErrorMessage(testOutput))
                    .withAssertionType("RUNTIME_ERROR")
                    .withStackTrace(testOutput)
                    .withParsingStrategy(getStrategyName())
                    .withParsingTime(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    /**
     * Extracts runtime error information using minimal regex.
     * 
     * @param testOutput the test output containing the error
     * @return RuntimeErrorInfo containing error type and message
     */
    private RuntimeErrorInfo extractRuntimeErrorInfo(String testOutput) {
        // Try NullPointerException first
        Matcher matcher = NULL_POINTER_PATTERN.matcher(testOutput);
        if (matcher.find()) {
            return new RuntimeErrorInfo("NULL_POINTER_EXCEPTION", matcher.group(1).trim());
        }
        
        // Try IllegalArgumentException
        matcher = ILLEGAL_ARGUMENT_PATTERN.matcher(testOutput);
        if (matcher.find()) {
            return new RuntimeErrorInfo("ILLEGAL_ARGUMENT_EXCEPTION", matcher.group(1).trim());
        }
        
        // Try IllegalStateException
        matcher = ILLEGAL_STATE_PATTERN.matcher(testOutput);
        if (matcher.find()) {
            return new RuntimeErrorInfo("ILLEGAL_STATE_EXCEPTION", matcher.group(1).trim());
        }
        
        // Try ArrayIndexOutOfBoundsException
        matcher = ARRAY_INDEX_OUT_OF_BOUNDS_PATTERN.matcher(testOutput);
        if (matcher.find()) {
            return new RuntimeErrorInfo("ARRAY_INDEX_OUT_OF_BOUNDS_EXCEPTION", matcher.group(1).trim());
        }
        
        // Try ClassCastException
        matcher = CLASS_CAST_PATTERN.matcher(testOutput);
        if (matcher.find()) {
            return new RuntimeErrorInfo("CLASS_CAST_EXCEPTION", matcher.group(1).trim());
        }
        
        // Try NumberFormatException
        matcher = NUMBER_FORMAT_PATTERN.matcher(testOutput);
        if (matcher.find()) {
            return new RuntimeErrorInfo("NUMBER_FORMAT_EXCEPTION", matcher.group(1).trim());
        }
        
        // Try IndexOutOfBoundsException
        matcher = INDEX_OUT_OF_BOUNDS_PATTERN.matcher(testOutput);
        if (matcher.find()) {
            return new RuntimeErrorInfo("INDEX_OUT_OF_BOUNDS_EXCEPTION", matcher.group(1).trim());
        }
        
        // Try ConcurrentModificationException
        matcher = CONCURRENT_MODIFICATION_PATTERN.matcher(testOutput);
        if (matcher.find()) {
            return new RuntimeErrorInfo("CONCURRENT_MODIFICATION_EXCEPTION", matcher.group(1).trim());
        }
        
        // Try SecurityException
        matcher = SECURITY_PATTERN.matcher(testOutput);
        if (matcher.find()) {
            return new RuntimeErrorInfo("SECURITY_EXCEPTION", matcher.group(1).trim());
        }
        
        // Try generic RuntimeException
        matcher = GENERIC_RUNTIME_PATTERN.matcher(testOutput);
        if (matcher.find()) {
            return new RuntimeErrorInfo("RUNTIME_EXCEPTION", matcher.group(1).trim());
        }
        
        // Fallback for any runtime-related error
        if (testOutput.contains("java.lang.RuntimeException") ||
            testOutput.contains("java.lang.NullPointerException") ||
            testOutput.contains("java.lang.IllegalArgumentException") ||
            testOutput.contains("java.lang.IllegalStateException") ||
            testOutput.contains("java.lang.ArrayIndexOutOfBoundsException") ||
            testOutput.contains("java.lang.ClassCastException") ||
            testOutput.contains("java.lang.NumberFormatException") ||
            testOutput.contains("java.lang.IndexOutOfBoundsException") ||
            testOutput.contains("java.util.ConcurrentModificationException") ||
            testOutput.contains("java.lang.SecurityException")) {
            String firstLine = testOutput.split("\n")[0];
            return new RuntimeErrorInfo("RUNTIME_ERROR", firstLine);
        }
        
        throw new RuntimeException("No recognizable runtime error pattern found in test output");
    }

    /**
     * Extracts stack trace information using PSI-based analysis.
     * 
     * @param testOutput the test output containing the stack trace
     * @return StackTraceInfo containing parsed stack trace details
     */
    private StackTraceInfo extractStackTraceInfo(String testOutput) {
        StackTraceInfo info = new StackTraceInfo();
        
        // Always set the full stack trace for debugging
        info.setStackTrace(testOutput);
        
        try {
            // Extract the first stack trace line that points to user code
            String[] lines = testOutput.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("at ") && 
                    !line.contains("junit.") && 
                    !line.contains("org.junit") &&
                    !line.contains("java.lang") &&
                    !line.contains("java.util") &&
                    !line.contains("sun.") &&
                    !line.contains("com.sun") &&
                    !line.contains("jdk.") &&
                    !line.contains("com.intellij")) {
                    // Found a user code stack trace line
                    StackTraceElement element = parseStackTraceElement(line);
                    if (element != null) {
                        info.setMethodName(element.getClassName() + "." + element.getMethodName());
                        info.setSourceFilePath(element.getFileName());
                        info.setLineNumber(element.getLineNumber());
                        
                        // Use PSI to get more accurate information
                        enrichWithPsiInfo(element, info);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn("Failed to parse stack trace using PSI, falling back to basic parsing", e);
            // Stack trace is already set above, so we're good
        }
        
        return info;
    }

    /**
     * Parses a stack trace line into a StackTraceElement.
     * 
     * @param line the stack trace line (e.g., "at com.example.MyTest.testSomething(MyTest.java:42)")
     * @return StackTraceElement or null if parsing fails
     */
    StackTraceElement parseStackTraceElement(String line) {
        try {
            // Remove "at " prefix
            String content = line.substring(3);
            
            // Find the last parenthesis which contains file and line info
            int lastParen = content.lastIndexOf('(');
            int lastParenClose = content.lastIndexOf(')');
            
            if (lastParen > 0 && lastParenClose > lastParen) {
                String methodPart = content.substring(0, lastParen);
                String filePart = content.substring(lastParen + 1, lastParenClose);
                
                // Parse file and line
                String[] fileInfo = filePart.split(":");
                String fileName = (fileInfo.length > 0 && !fileInfo[0].isEmpty()) ? fileInfo[0] : null;
                int lineNumber = fileInfo.length > 1 ? Integer.parseInt(fileInfo[1]) : -1;
                
                // Parse class and method
                int lastDot = methodPart.lastIndexOf('.');
                if (lastDot > 0) {
                    String className = methodPart.substring(0, lastDot);
                    String methodName = methodPart.substring(lastDot + 1);
                    
                    return new StackTraceElement(className, methodName, fileName, lineNumber);
                }
            }
        } catch (Exception e) {
            LOG.debug("Failed to parse stack trace line: " + line, e);
        }
        
        return null;
    }

    /**
     * Enriches stack trace information using PSI analysis.
     * 
     * @param element the parsed stack trace element
     * @param info the StackTraceInfo to enrich
     */
    private void enrichWithPsiInfo(StackTraceElement element, StackTraceInfo info) {
        try {
            // Use read action for PSI operations
            ApplicationManager.getApplication().<Void>runReadAction(() -> {
                try {
                    // Find the class using PSI
                    PsiClass psiClass = JavaPsiFacade.getInstance(project)
                            .findClass(element.getClassName(), GlobalSearchScope.allScope(project));
                    
                    if (psiClass != null) {
                        // Get the actual source file
                        PsiFile psiFile = psiClass.getContainingFile();
                        if (psiFile != null) {
                            info.setSourceFilePath(psiFile.getName());
                            
                            // Find the method and get its exact line number
                            PsiMethod[] methods = psiClass.findMethodsByName(element.getMethodName(), false);
                            if (methods.length > 0) {
                                PsiMethod method = methods[0];
                                try {
                                    int lineNumber = psiFile.getText().substring(0, method.getTextOffset())
                                            .split("\n").length;
                                    info.setLineNumber(lineNumber);
                                } catch (Exception e) {
                                    LOG.debug("Failed to calculate line number for method " + element.getMethodName(), e);
                                    // Keep the parsed line number as fallback
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    LOG.debug("Failed to enrich with PSI info for " + element.getClassName(), e);
                }
            });
        } catch (Exception e) {
            LOG.debug("Failed to run PSI enrichment for " + element.getClassName(), e);
        }
    }

    /**
     * Extracts a basic error message from test output as fallback.
     * 
     * @param testOutput the test output
     * @return basic error message
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
        return "Unknown runtime error";
    }

    @Override
    public int getPriority() {
        return 80; // Medium priority for runtime error parsing
    }

    @Override
    public String getStrategyName() {
        return "RuntimeErrorStrategy";
    }

    /**
     * Internal class to hold runtime error information.
     */
    private static class RuntimeErrorInfo {
        private final String errorType;
        private final String message;
        
        public RuntimeErrorInfo(String errorType, String message) {
            this.errorType = errorType;
            this.message = message;
        }
        
        public String getErrorType() { return errorType; }
        public String getMessage() { return message; }
    }

    /**
     * Internal class to hold stack trace information.
     */
    private static class StackTraceInfo {
        private String stackTrace;
        private String methodName;
        private String sourceFilePath;
        private int lineNumber = -1;
        
        public String getStackTrace() { return stackTrace; }
        public void setStackTrace(String stackTrace) { this.stackTrace = stackTrace; }
        
        public String getMethodName() { return methodName; }
        public void setMethodName(String methodName) { this.methodName = methodName; }
        
        public String getSourceFilePath() { return sourceFilePath; }
        public void setSourceFilePath(String sourceFilePath) { this.sourceFilePath = sourceFilePath; }
        
        public int getLineNumber() { return lineNumber; }
        public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }
    }
} 