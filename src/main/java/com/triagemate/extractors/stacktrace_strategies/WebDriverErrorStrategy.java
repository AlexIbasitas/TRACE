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
 * Parsing strategy for extracting failure information from WebDriver exceptions.
 * <p>
 * This strategy handles common Selenium WebDriver exceptions such as:
 * - NoSuchElementException
 * - TimeoutException
 * - ElementNotInteractableException
 * - StaleElementReferenceException
 * - ElementClickInterceptedException
 * - NoSuchWindowException
 * - NoSuchFrameException
 * - WebDriverException
 * </p>
 *
 * <p>Example WebDriver output handled:</p>
 * <pre>
 * org.openqa.selenium.NoSuchElementException: no such element: Unable to locate element: {"method":"css selector","selector":"#nonexistent"}
 *     at com.example.MyTest.testSomething(MyTest.java:42)
 * </pre>
 */
public class WebDriverErrorStrategy implements FailureParsingStrategy {
    private static final Logger LOG = Logger.getInstance(WebDriverErrorStrategy.class);
    private final Project project;
    
    /**
     * Regex patterns for common WebDriver exceptions.
     * These are minimal and only used for initial detection and basic extraction.
     */
    private static final Pattern NO_SUCH_ELEMENT_PATTERN = Pattern.compile(
            "org\\.openqa\\.selenium\\.NoSuchElementException:\\s*(.+?)(?:\\s*at\\s|$)",
            Pattern.DOTALL);
    
    private static final Pattern TIMEOUT_PATTERN = Pattern.compile(
            "org\\.openqa\\.selenium\\.TimeoutException:\\s*(.+?)(?:\\s*at\\s|$)",
            Pattern.DOTALL);
    
    private static final Pattern ELEMENT_NOT_INTERACTABLE_PATTERN = Pattern.compile(
            "org\\.openqa\\.selenium\\.ElementNotInteractableException:\\s*(.+?)(?:\\s*at\\s|$)",
            Pattern.DOTALL);
    
    private static final Pattern STALE_ELEMENT_PATTERN = Pattern.compile(
            "org\\.openqa\\.selenium\\.StaleElementReferenceException:\\s*(.+?)(?:\\s*at\\s|$)",
            Pattern.DOTALL);
    
    private static final Pattern ELEMENT_CLICK_INTERCEPTED_PATTERN = Pattern.compile(
            "org\\.openqa\\.selenium\\.ElementClickInterceptedException:\\s*([\\s\\S]+?)(?=\\n\\s*at |$)",
            Pattern.DOTALL);
    
    private static final Pattern NO_SUCH_WINDOW_PATTERN = Pattern.compile(
            "org\\.openqa\\.selenium\\.NoSuchWindowException:\\s*(.+?)(?:\\s*at\\s|$)",
            Pattern.DOTALL);
    
    private static final Pattern NO_SUCH_FRAME_PATTERN = Pattern.compile(
            "org\\.openqa\\.selenium\\.NoSuchFrameException:\\s*(.+?)(?:\\s*at\\s|$)",
            Pattern.DOTALL);
    
    private static final Pattern WEB_DRIVER_PATTERN = Pattern.compile(
            "org\\.openqa\\.selenium\\.WebDriverException:\\s*(.+?)(?:\\s*at\\s|$)",
            Pattern.DOTALL);

    /**
     * Constructor for WebDriverErrorStrategy.
     * 
     * @param project The IntelliJ project context for PSI operations
     */
    public WebDriverErrorStrategy(Project project) {
        this.project = project;
    }

    @Override
    public boolean canHandle(String testOutput) {
        if (testOutput == null || testOutput.isEmpty()) {
            return false;
        }
        // Use regex patterns for detection (require colon and message)
        boolean matches = NO_SUCH_ELEMENT_PATTERN.matcher(testOutput).find() ||
                TIMEOUT_PATTERN.matcher(testOutput).find() ||
                ELEMENT_NOT_INTERACTABLE_PATTERN.matcher(testOutput).find() ||
                STALE_ELEMENT_PATTERN.matcher(testOutput).find() ||
                ELEMENT_CLICK_INTERCEPTED_PATTERN.matcher(testOutput).find() ||
                NO_SUCH_WINDOW_PATTERN.matcher(testOutput).find() ||
                NO_SUCH_FRAME_PATTERN.matcher(testOutput).find() ||
                WEB_DRIVER_PATTERN.matcher(testOutput).find();
        // Fallback: substring check for exception class names (for consistency with other strategies)
        boolean contains = testOutput.contains("org.openqa.selenium.NoSuchElementException") ||
                testOutput.contains("org.openqa.selenium.TimeoutException") ||
                testOutput.contains("org.openqa.selenium.ElementNotInteractableException") ||
                testOutput.contains("org.openqa.selenium.StaleElementReferenceException") ||
                testOutput.contains("org.openqa.selenium.ElementClickInterceptedException") ||
                testOutput.contains("org.openqa.selenium.NoSuchWindowException") ||
                testOutput.contains("org.openqa.selenium.NoSuchFrameException") ||
                testOutput.contains("org.openqa.selenium.WebDriverException");
        return matches || contains;
    }

    @Override
    public FailureInfo parse(String testOutput) {
        if (testOutput == null) {
            throw new IllegalArgumentException("testOutput cannot be null");
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Extract error type and message using minimal regex
            WebDriverErrorInfo errorInfo = extractWebDriverErrorInfo(testOutput);
            
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
            
            LOG.debug("Successfully parsed WebDriver error", 
                    "errorType", errorInfo.getErrorType(),
                    "message", errorInfo.getMessage(),
                    "duration", System.currentTimeMillis() - startTime);
            
            return failureInfo;
            
        } catch (Exception e) {
            LOG.warn("Failed to parse WebDriver error, creating minimal failure info", e);
            
            // Create minimal failure info as fallback
            return new FailureInfo.Builder()
                    .withErrorMessage("WebDriver error: " + extractBasicErrorMessage(testOutput))
                    .withAssertionType("WEBDRIVER_ERROR")
                    .withStackTrace(testOutput)
                    .withParsingStrategy(getStrategyName())
                    .withParsingTime(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    /**
     * Extracts WebDriver error information using minimal regex.
     * 
     * @param testOutput the test output containing the error
     * @return WebDriverErrorInfo containing error type and message
     */
    private WebDriverErrorInfo extractWebDriverErrorInfo(String testOutput) {
        // Try NoSuchElementException first
        Matcher matcher = NO_SUCH_ELEMENT_PATTERN.matcher(testOutput);
        if (matcher.find()) {
            return new WebDriverErrorInfo("NO_SUCH_ELEMENT_EXCEPTION", matcher.group(1).trim());
        }
        
        // Try TimeoutException
        matcher = TIMEOUT_PATTERN.matcher(testOutput);
        if (matcher.find()) {
            return new WebDriverErrorInfo("TIMEOUT_EXCEPTION", matcher.group(1).trim());
        }
        
        // Try ElementNotInteractableException
        matcher = ELEMENT_NOT_INTERACTABLE_PATTERN.matcher(testOutput);
        if (matcher.find()) {
            return new WebDriverErrorInfo("ELEMENT_NOT_INTERACTABLE_EXCEPTION", matcher.group(1).trim());
        }
        
        // Try StaleElementReferenceException
        matcher = STALE_ELEMENT_PATTERN.matcher(testOutput);
        if (matcher.find()) {
            return new WebDriverErrorInfo("STALE_ELEMENT_REFERENCE_EXCEPTION", matcher.group(1).trim());
        }
        
        // Try ElementClickInterceptedException
        matcher = ELEMENT_CLICK_INTERCEPTED_PATTERN.matcher(testOutput);
        if (matcher.find()) {
            return new WebDriverErrorInfo("ELEMENT_CLICK_INTERCEPTED_EXCEPTION", matcher.group(1).trim());
        }
        
        // Try NoSuchWindowException
        matcher = NO_SUCH_WINDOW_PATTERN.matcher(testOutput);
        if (matcher.find()) {
            return new WebDriverErrorInfo("NO_SUCH_WINDOW_EXCEPTION", matcher.group(1).trim());
        }
        
        // Try NoSuchFrameException
        matcher = NO_SUCH_FRAME_PATTERN.matcher(testOutput);
        if (matcher.find()) {
            return new WebDriverErrorInfo("NO_SUCH_FRAME_EXCEPTION", matcher.group(1).trim());
        }
        
        // Try generic WebDriverException
        matcher = WEB_DRIVER_PATTERN.matcher(testOutput);
        if (matcher.find()) {
            return new WebDriverErrorInfo("WEBDRIVER_EXCEPTION", matcher.group(1).trim());
        }
        
        // Fallback for any WebDriver-related error
        if (testOutput.contains("org.openqa.selenium.NoSuchElementException") ||
            testOutput.contains("org.openqa.selenium.TimeoutException") ||
            testOutput.contains("org.openqa.selenium.ElementNotInteractableException") ||
            testOutput.contains("org.openqa.selenium.StaleElementReferenceException") ||
            testOutput.contains("org.openqa.selenium.ElementClickInterceptedException") ||
            testOutput.contains("org.openqa.selenium.NoSuchWindowException") ||
            testOutput.contains("org.openqa.selenium.NoSuchFrameException") ||
            testOutput.contains("org.openqa.selenium.WebDriverException")) {
            String firstLine = testOutput.split("\n")[0];
            return new WebDriverErrorInfo("WEBDRIVER_ERROR", firstLine);
        }
        
        throw new RuntimeException("No recognizable WebDriver error pattern found in test output");
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
                    !line.contains("com.intellij") &&
                    !line.contains("org.openqa.selenium")) {
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
        return "Unknown WebDriver error";
    }

    @Override
    public int getPriority() {
        return 70; // Medium-high priority for WebDriver error parsing
    }

    @Override
    public String getStrategyName() {
        return "WebDriverErrorStrategy";
    }

    /**
     * Internal class to hold WebDriver error information.
     */
    private static class WebDriverErrorInfo {
        private final String errorType;
        private final String message;
        
        public WebDriverErrorInfo(String errorType, String message) {
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