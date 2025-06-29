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
 * Parsing strategy for extracting failure information from JUnit ComparisonFailure errors.
 * <p>
 * This strategy specifically handles org.junit.ComparisonFailure and similar JUnit assertion errors
 * that provide expected vs actual value comparisons. It uses PSI-based analysis for accurate 
 * stack trace parsing and code navigation.
 * </p>
 *
 * <p>Example JUnit output handled:</p>
 * <pre>
 * org.junit.ComparisonFailure: expected:<foo> but was:<bar>
 *     at com.example.MyTest.testSomething(MyTest.java:42)
 * </pre>
 */
public class JUnitComparisonFailureStrategy implements FailureParsingStrategy {
    private static final Logger LOG = Logger.getInstance(JUnitComparisonFailureStrategy.class);
    private final Project project;
    
    /**
     * Regex pattern for JUnit ComparisonFailure (expected/actual values).
     * Example: org.junit.ComparisonFailure: expected:<foo> but was:<bar>
     */
    private static final Pattern JUNIT_COMPARISON_PATTERN = Pattern.compile(
            "org\\.junit\\.ComparisonFailure: expected:<(.+?)> but was:<(.+?)>",
            Pattern.DOTALL);

    /**
     * Constructor for JUnitComparisonFailureStrategy.
     * 
     * @param project The IntelliJ project context for PSI operations
     */
    public JUnitComparisonFailureStrategy(Project project) {
        this.project = project;
    }

    @Override
    public boolean canHandle(String testOutput) {
        if (testOutput == null || testOutput.isEmpty()) {
            return false;
        }
        // Use only regex pattern - no PSI operations
        return JUNIT_COMPARISON_PATTERN.matcher(testOutput).find();
    }

    @Override
    public FailureInfo parse(String testOutput) {
        if (testOutput == null) {
            throw new IllegalArgumentException("testOutput cannot be null");
        }
        
        Matcher matcher = JUNIT_COMPARISON_PATTERN.matcher(testOutput);
        if (!matcher.find()) {
            throw new RuntimeException("No JUnit ComparisonFailure found in test output");
        }
        
        String expected = matcher.group(1);
        String actual = matcher.group(2);

        // Use PSI-based stack trace analysis
        StackTraceInfo stackTraceInfo = extractStackTraceInfo(testOutput);

        return new FailureInfo.Builder()
                .withExpectedValue(expected)
                .withActualValue(actual)
                .withAssertionType("JUNIT_COMPARISON")
                .withErrorMessage("JUnit ComparisonFailure: expected <" + expected + "> but was <" + actual + ">")
                .withStackTrace(stackTraceInfo.getStackTrace())
                .withStepDefinitionMethod(stackTraceInfo.getMethodName())
                .withSourceFilePath(stackTraceInfo.getSourceFilePath())
                .withLineNumber(stackTraceInfo.getLineNumber())
                .withParsingStrategy(getStrategyName())
                .build();
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
                if (line.startsWith("at ") && !line.contains("junit.") && !line.contains("org.junit")) {
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
                    // Keep the parsed information as fallback
                }
                return null;
            });
        } catch (Exception e) {
            LOG.debug("Failed to run read action for PSI enrichment of " + element.getClassName(), e);
            // Keep the parsed information as fallback
        }
    }

    @Override
    public int getPriority() {
        return 100; // High priority for JUnit-specific parsing
    }

    @Override
    public String getStrategyName() {
        return "JUnitComparisonFailureStrategy";
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