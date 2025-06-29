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
 * Parsing strategy for extracting failure information from Cucumber-specific errors.
 * <p>
 * This strategy handles Cucumber framework exceptions such as:
 * - UndefinedStepException
 * - AmbiguousStepDefinitionsException
 * - PendingException
 * - CucumberException
 * - Step definition not found errors
 * </p>
 *
 * <p>Example Cucumber output handled:</p>
 * <pre>
 * io.cucumber.junit.UndefinedStepException: The step "I click on the button" is undefined.
 *     at com.example.MyTest.testSomething(MyTest.java:42)
 * </pre>
 */
public class CucumberErrorStrategy implements FailureParsingStrategy {
    private static final Logger LOG = Logger.getInstance(CucumberErrorStrategy.class);
    private final Project project;
    
    /**
     * Regex patterns for Cucumber-specific exceptions.
     * These are minimal and only used for initial detection and basic extraction.
     */
    private static final Pattern UNDEFINED_STEP_PATTERN = Pattern.compile(
            "io\\.cucumber\\.(?:junit\\.)?UndefinedStepException:\\s*(.+?)(?:\\s*at\\s|$)",
            Pattern.DOTALL);
    
    private static final Pattern AMBIGUOUS_STEP_PATTERN = Pattern.compile(
            "io\\.cucumber\\.(?:junit\\.)?AmbiguousStepDefinitionsException:\\s*(.+?)(?:\\s*at\\s|$)",
            Pattern.DOTALL);
    
    private static final Pattern PENDING_STEP_PATTERN = Pattern.compile(
            "io\\.cucumber\\.(?:junit\\.)?PendingException:\\s*(.+?)(?:\\s*at\\s|$)",
            Pattern.DOTALL);
    
    private static final Pattern CUCUMBER_EXCEPTION_PATTERN = Pattern.compile(
            "io\\.cucumber\\.(?:junit\\.)?CucumberException:\\s*(.+?)(?:\\s*at\\s|$)",
            Pattern.DOTALL);
    
    private static final Pattern STEP_DEFINITION_NOT_FOUND_PATTERN = Pattern.compile(
            "Step\\s+definition\\s+not\\s+found\\s+for\\s+step\\s+['\"](.+?)['\"]",
            Pattern.DOTALL);

    /**
     * Constructor for CucumberErrorStrategy.
     * 
     * @param project The IntelliJ project context for PSI operations
     */
    public CucumberErrorStrategy(Project project) {
        this.project = project;
    }

    @Override
    public boolean canHandle(String testOutput) {
        if (testOutput == null || testOutput.isEmpty()) {
            return false;
        }
        
        // Use only regex patterns for detection - no PSI operations
        return UNDEFINED_STEP_PATTERN.matcher(testOutput).find() ||
               AMBIGUOUS_STEP_PATTERN.matcher(testOutput).find() ||
               PENDING_STEP_PATTERN.matcher(testOutput).find() ||
               CUCUMBER_EXCEPTION_PATTERN.matcher(testOutput).find() ||
               STEP_DEFINITION_NOT_FOUND_PATTERN.matcher(testOutput).find() ||
               testOutput.contains("io.cucumber") ||
               testOutput.contains("cucumber.runtime");
    }

    @Override
    public FailureInfo parse(String testOutput) {
        if (testOutput == null) {
            throw new IllegalArgumentException("testOutput cannot be null");
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Extract error type and message using minimal regex
            CucumberErrorInfo errorInfo = extractCucumberErrorInfo(testOutput);
            
            // Use PSI-based stack trace analysis for accurate code navigation
            StackTraceInfo stackTraceInfo = extractStackTraceInfo(testOutput);
            
            // Extract step text from error message using PSI when possible
            String failedStepText = extractFailedStepText(errorInfo.getMessage(), errorInfo.getErrorType(), stackTraceInfo);
            
            // Build the failure info
            FailureInfo failureInfo = new FailureInfo.Builder()
                    .withFailedStepText(failedStepText)
                    .withErrorMessage(errorInfo.getMessage())
                    .withAssertionType(errorInfo.getErrorType())
                    .withStackTrace(stackTraceInfo.getStackTrace())
                    .withStepDefinitionMethod(stackTraceInfo.getMethodName())
                    .withSourceFilePath(stackTraceInfo.getSourceFilePath())
                    .withLineNumber(stackTraceInfo.getLineNumber())
                    .withParsingStrategy(getStrategyName())
                    .withParsingTime(System.currentTimeMillis() - startTime)
                    .build();
            
            LOG.debug("Successfully parsed Cucumber error", 
                    "errorType", errorInfo.getErrorType(),
                    "stepText", failedStepText,
                    "duration", System.currentTimeMillis() - startTime);
            
            return failureInfo;
            
        } catch (Exception e) {
            LOG.warn("Failed to parse Cucumber error, creating minimal failure info", e);
            
            // Create minimal failure info as fallback
            return new FailureInfo.Builder()
                    .withFailedStepText("Cucumber step failed")
                    .withErrorMessage("Cucumber error: " + extractBasicErrorMessage(testOutput))
                    .withAssertionType("CUCUMBER_ERROR")
                    .withStackTrace(testOutput)
                    .withParsingStrategy(getStrategyName())
                    .withParsingTime(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    /**
     * Extracts Cucumber-specific error information using minimal regex.
     * 
     * @param testOutput the test output containing the error
     * @return CucumberErrorInfo containing error type and message
     */
    private CucumberErrorInfo extractCucumberErrorInfo(String testOutput) {
        // Try UndefinedStepException first
        Matcher matcher = UNDEFINED_STEP_PATTERN.matcher(testOutput);
        if (matcher.find()) {
            return new CucumberErrorInfo("UNDEFINED_STEP", matcher.group(1).trim());
        }
        
        // Try AmbiguousStepDefinitionsException
        matcher = AMBIGUOUS_STEP_PATTERN.matcher(testOutput);
        if (matcher.find()) {
            return new CucumberErrorInfo("AMBIGUOUS_STEP", matcher.group(1).trim());
        }
        
        // Try PendingException
        matcher = PENDING_STEP_PATTERN.matcher(testOutput);
        if (matcher.find()) {
            return new CucumberErrorInfo("PENDING_STEP", matcher.group(1).trim());
        }
        
        // Try CucumberException
        matcher = CUCUMBER_EXCEPTION_PATTERN.matcher(testOutput);
        if (matcher.find()) {
            return new CucumberErrorInfo("CUCUMBER_EXCEPTION", matcher.group(1).trim());
        }
        
        // Try step definition not found
        matcher = STEP_DEFINITION_NOT_FOUND_PATTERN.matcher(testOutput);
        if (matcher.find()) {
            return new CucumberErrorInfo("STEP_DEFINITION_NOT_FOUND", 
                    "Step definition not found for step: " + matcher.group(1));
        }
        
        // Fallback for any Cucumber-related error
        if (testOutput.contains("io.cucumber") || testOutput.contains("cucumber.runtime")) {
            String firstLine = testOutput.split("\n")[0];
            return new CucumberErrorInfo("CUCUMBER_ERROR", firstLine);
        }
        
        throw new RuntimeException("No recognizable Cucumber error pattern found in test output");
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
                    !line.contains("cucumber.") && 
                    !line.contains("io.cucumber") &&
                    !line.contains("junit.") && 
                    !line.contains("org.junit")) {
                    
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
     * Extracts the failed step text from the error message using PSI when possible.
     * 
     * @param errorMessage the error message from the Cucumber exception
     * @param errorType the error type from the Cucumber exception
     * @param stackTraceInfo the stack trace information for context
     * @return the failed step text
     */
    private String extractFailedStepText(String errorMessage, String errorType, StackTraceInfo stackTraceInfo) {
        try {
            // Special handling for step definition not found errors
            // Return the full error message as it's already informative and user-friendly
            if ("STEP_DEFINITION_NOT_FOUND".equals(errorType)) {
                return errorMessage;
            }
            
            // Try to extract quoted step text (single or double quotes) from the error message
            // Handle both single and double quotes, and try to extract the outermost quoted string
            String extractedText = null;
            
            // Try double quotes first (more common in error messages)
            Pattern doubleQuotePattern = Pattern.compile("\"([^\"]*)\"");
            Matcher doubleMatcher = doubleQuotePattern.matcher(errorMessage);
            if (doubleMatcher.find()) {
                extractedText = doubleMatcher.group(1);
            }
            
            // If no double quotes found, try single quotes
            if (extractedText == null) {
                Pattern singleQuotePattern = Pattern.compile("'([^']*)'");
                Matcher singleMatcher = singleQuotePattern.matcher(errorMessage);
                if (singleMatcher.find()) {
                    extractedText = singleMatcher.group(1);
                }
            }
            
            if (extractedText != null && !extractedText.trim().isEmpty()) {
                return extractedText;
            }
            
            // If we have stack trace info, try to find step definitions in the source file
            if (stackTraceInfo.getSourceFilePath() != null && stackTraceInfo.getLineNumber() > 0) {
                String stepText = findStepTextInSourceFile(stackTraceInfo.getSourceFilePath(), 
                                                         stackTraceInfo.getLineNumber());
                if (stepText != null) {
                    return stepText;
                }
            }
            
        } catch (Exception e) {
            LOG.debug("Failed to extract step text using PSI, using fallback", e);
        }
        
        // Fallback: return a generic message
        return "Cucumber step failed";
    }

    /**
     * Finds step text in the source file using PSI analysis.
     * 
     * @param sourceFilePath the path to the source file
     * @param lineNumber the line number to search around
     * @return the step text if found, null otherwise
     */
    private String findStepTextInSourceFile(String sourceFilePath, int lineNumber) {
        try {
            // Use read action for PSI operations
            return ApplicationManager.getApplication().<String>runReadAction(() -> {
                try {
                    // Use PSI to find the file and analyze it
                    PsiFile psiFile = findPsiFile(sourceFilePath);
                    if (psiFile != null && psiFile instanceof PsiJavaFile) {
                        PsiJavaFile javaFile = (PsiJavaFile) psiFile;
                        
                        // Find step definition annotations near the line number
                        for (PsiClass psiClass : javaFile.getClasses()) {
                            for (PsiMethod method : psiClass.getMethods()) {
                                PsiAnnotation[] annotations = method.getModifierList().getAnnotations();
                                for (PsiAnnotation annotation : annotations) {
                                    String annotationText = annotation.getText();
                                    if (annotationText.contains("@Given") || 
                                        annotationText.contains("@When") || 
                                        annotationText.contains("@Then")) {
                                        
                                        // Extract step text from annotation
                                        String stepText = extractStepTextFromAnnotation(annotation);
                                        if (stepText != null) {
                                            return stepText;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    return null;
                } catch (Exception e) {
                    LOG.debug("Failed to find step text in source file", e);
                    return null;
                }
            });
        } catch (Exception e) {
            LOG.debug("Failed to run read action for step text extraction", e);
            return null;
        }
    }

    /**
     * Finds a PSI file by path.
     * 
     * @param sourceFilePath the path to the source file
     * @return the PSI file if found, null otherwise
     */
    private PsiFile findPsiFile(String sourceFilePath) {
        try {
            // Use read action for PSI operations
            return ApplicationManager.getApplication().<PsiFile>runReadAction(() -> {
                try {
                    // Try to find the file in the project
                    return PsiManager.getInstance(project).findFile(
                        project.getBaseDir().getFileSystem().findFileByPath(sourceFilePath));
                } catch (Exception e) {
                    LOG.debug("Failed to find PSI file: " + sourceFilePath, e);
                    return null;
                }
            });
        } catch (Exception e) {
            LOG.debug("Failed to run read action for PSI file lookup", e);
            return null;
        }
    }

    /**
     * Extracts step text from a Cucumber annotation using PSI.
     * 
     * @param annotation the PSI annotation element
     * @return the step text if found, null otherwise
     */
    private String extractStepTextFromAnnotation(PsiAnnotation annotation) {
        try {
            // Get the annotation value which contains the step text
            PsiAnnotationMemberValue value = annotation.findAttributeValue("value");
            if (value != null) {
                String text = value.getText();
                // Remove quotes and return the step text
                if (text.startsWith("\"") && text.endsWith("\"")) {
                    return text.substring(1, text.length() - 1);
                }
            }
        } catch (Exception e) {
            LOG.debug("Failed to extract step text from annotation", e);
        }
        
        return null;
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
                String fileName = fileInfo[0];
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
                    // Keep the parsed information as fallback
                }
                return null;
            });
        } catch (Exception e) {
            LOG.debug("Failed to run read action for PSI enrichment of " + element.getClassName(), e);
            // Keep the parsed information as fallback
        }
    }

    /**
     * Extracts a basic error message from test output as fallback.
     * 
     * @param testOutput the test output
     * @return the basic error message
     */
    private String extractBasicErrorMessage(String testOutput) {
        String[] lines = testOutput.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty() && !line.startsWith("at ")) {
                return line;
            }
        }
        return "Unknown Cucumber error";
    }

    @Override
    public int getPriority() {
        return 85; // High priority for Cucumber-specific parsing
    }

    @Override
    public String getStrategyName() {
        return "CucumberErrorStrategy";
    }

    /**
     * Helper class to hold Cucumber error information.
     */
    private static class CucumberErrorInfo {
        private final String errorType;
        private final String message;

        public CucumberErrorInfo(String errorType, String message) {
            this.errorType = errorType;
            this.message = message;
        }

        public String getErrorType() { return errorType; }
        public String getMessage() { return message; }
    }

    /**
     * Helper class to hold stack trace information.
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