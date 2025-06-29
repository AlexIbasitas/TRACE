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
 * Parsing strategy for extracting failure information from configuration and setup errors.
 * <p>
 * This strategy handles configuration-related exceptions such as:
 * - FileNotFoundException
 * - IOException
 * - ConfigurationException
 * - Property loading errors
 * - Resource not found errors
 * - Database connection errors
 * - ClassNotFoundException
 * - NoClassDefFoundError
 * </p>
 *
 * <p>Example configuration output handled:</p>
 * <pre>
 * java.io.FileNotFoundException: config.properties (No such file or directory)
 *     at com.example.MyTest.testSomething(MyTest.java:42)
 * </pre>
 */
public class ConfigurationErrorStrategy implements FailureParsingStrategy {
    private static final Logger LOG = Logger.getInstance(ConfigurationErrorStrategy.class);
    private final Project project;
    
    /**
     * Pattern for common configuration error exceptions.
     * Uses minimal regex only for exception class names that are well-defined.
     */
    private static final Pattern CONFIGURATION_ERROR_PATTERN = Pattern.compile(
            "(java\\.io\\.FileNotFoundException|" +
            "java\\.io\\.IOException|" +
            "java\\.lang\\.ClassNotFoundException|" +
            "java\\.lang\\.NoClassDefFoundError|" +
            "java\\.sql\\.SQLException|" +
            "javax\\.naming\\.NamingException|" +
            "org\\.springframework\\.beans\\.factory\\.BeanCreationException|" +
            "org\\.springframework\\.context\\.ApplicationContextException|" +
            "com\\.fasterxml\\.jackson\\.core\\.JsonParseException|" +
            "org\\.yaml\\.snakeyaml\\.error\\.YAMLException)",
            Pattern.CASE_INSENSITIVE);

    /**
     * Constructor for ConfigurationErrorStrategy.
     * 
     * @param project The IntelliJ project context for PSI operations
     */
    public ConfigurationErrorStrategy(Project project) {
        this.project = project;
    }

    @Override
    public boolean canHandle(String testOutput) {
        if (testOutput == null || testOutput.isEmpty()) {
            return false;
        }
        
        // Use only regex patterns for detection - no PSI operations
        return CONFIGURATION_ERROR_PATTERN.matcher(testOutput).find() ||
               testOutput.toLowerCase().contains("configuration") ||
               testOutput.toLowerCase().contains("resource") ||
               testOutput.toLowerCase().contains("file") ||
               testOutput.toLowerCase().contains("property");
    }

    @Override
    public FailureInfo parse(String testOutput) {
        if (testOutput == null) {
            throw new IllegalArgumentException("testOutput cannot be null");
        }
        
        if (!canHandle(testOutput)) {
            throw new RuntimeException("No configuration error found in test output");
        }
        
        // Extract configuration error details using PSI-based analysis
        ConfigurationErrorInfo errorInfo = extractConfigurationErrorInfo(testOutput);
        
        return new FailureInfo.Builder()
                .withAssertionType("CONFIGURATION_ERROR")
                .withErrorMessage(errorInfo.getErrorMessage())
                .withStackTrace(errorInfo.getStackTrace())
                .withStepDefinitionMethod(errorInfo.getMethodName())
                .withSourceFilePath(errorInfo.getSourceFilePath())
                .withLineNumber(errorInfo.getLineNumber())
                .withFailedStepText(errorInfo.getFailedStepText())
                .withParsingStrategy(getStrategyName())
                .build();
    }

    /**
     * Extracts configuration error information using PSI-based analysis.
     * 
     * @param testOutput the test output containing the configuration error
     * @return ConfigurationErrorInfo containing parsed error details
     */
    private ConfigurationErrorInfo extractConfigurationErrorInfo(String testOutput) {
        ConfigurationErrorInfo info = new ConfigurationErrorInfo();
        
        // Always set the full stack trace for debugging
        info.setStackTrace(testOutput);
        
        try {
            // Extract the first line (exception message)
            String[] lines = testOutput.split("\n");
            if (lines.length > 0) {
                String firstLine = lines[0].trim();
                info.setErrorMessage(firstLine);
                
                // Extract failed step text from the error message
                info.setFailedStepText(extractFailedStepText(firstLine));
            }
            
            // Extract stack trace information using PSI
            extractStackTraceInfo(testOutput, info);
            
        } catch (Exception e) {
            LOG.warn("Failed to parse configuration error using PSI, falling back to basic parsing", e);
            // Basic information is already set above
        }
        
        return info;
    }

    /**
     * Extracts stack trace information using PSI-based analysis.
     * 
     * @param testOutput the test output containing the stack trace
     * @param info the ConfigurationErrorInfo to populate
     */
    private void extractStackTraceInfo(String testOutput, ConfigurationErrorInfo info) {
        try {
            // Use read action for PSI operations
            ApplicationManager.getApplication().<Void>runReadAction(() -> {
                try {
                    // Extract the first stack trace line that points to user code
                    String[] lines = testOutput.split("\n");
                    for (String line : lines) {
                        line = line.trim();
                        if (line.startsWith("at ") && !line.contains("java.") && !line.contains("sun.") && !line.contains("com.sun.")) {
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
                    LOG.debug("Failed to extract stack trace info for configuration error", e);
                }
                return null;
            });
        } catch (Exception e) {
            LOG.debug("Failed to extract stack trace info for configuration error", e);
        }
    }

    // Change from private to package-private for testability
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
     * Enriches configuration error information using PSI analysis.
     * 
     * @param element the parsed stack trace element
     * @param info the ConfigurationErrorInfo to enrich
     */
    private void enrichWithPsiInfo(StackTraceElement element, ConfigurationErrorInfo info) {
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
     * Extracts the failed step text from the configuration error message.
     * 
     * @param errorMessage the error message from the configuration exception
     * @return the failed step text
     */
    private String extractFailedStepText(String errorMessage) {
        try {
            // For JSON parsing errors, extract the specific error (skip quoted string extraction)
            if (errorMessage.contains("JsonParseException:")) {
                int colonIndex = errorMessage.indexOf(":");
                if (colonIndex > 0) {
                    String afterColon = errorMessage.substring(colonIndex + 1).trim();
                    return afterColon;
                }
            }
            // Try to extract quoted resource names or file paths from the error message (for other error types)
            Pattern quotedPattern = Pattern.compile("['\"]([^'\"]+)['\"]");
            Matcher quotedMatcher = quotedPattern.matcher(errorMessage);
            if (quotedMatcher.find()) {
                return quotedMatcher.group(1);
            }
            
            // Try to extract file paths or resource names after common patterns
            Pattern filePattern = Pattern.compile("(?:file|resource|config|property)\\s*[:=]\\s*([^\\s]+)");
            Matcher fileMatcher = filePattern.matcher(errorMessage);
            if (fileMatcher.find()) {
                return fileMatcher.group(1);
            }
            
            // For FileNotFoundException, extract the filename before the parenthesis
            if (errorMessage.contains("FileNotFoundException:")) {
                int colonIndex = errorMessage.indexOf(":");
                if (colonIndex > 0) {
                    String afterColon = errorMessage.substring(colonIndex + 1).trim();
                    int parenIndex = afterColon.indexOf("(");
                    if (parenIndex > 0) {
                        return afterColon.substring(0, parenIndex).trim();
                    }
                    return afterColon;
                }
            }
            
            // For ClassNotFoundException, extract the class name
            if (errorMessage.contains("ClassNotFoundException:")) {
                int colonIndex = errorMessage.indexOf(":");
                if (colonIndex > 0) {
                    String afterColon = errorMessage.substring(colonIndex + 1).trim();
                    // Remove any additional details after the class name
                    String[] parts = afterColon.split("\\s+");
                    return parts[0];
                }
            }
            
            // For Spring BeanCreationException, extract the bean name if quoted
            if (errorMessage.contains("BeanCreationException:")) {
                Pattern beanPattern = Pattern.compile("bean\\s+['\"]([^'\"]+)['\"]");
                Matcher beanMatcher = beanPattern.matcher(errorMessage);
                if (beanMatcher.find()) {
                    return beanMatcher.group(1);
                }
            }
            
            // Fallback: extract the first meaningful part of the error message
            String[] parts = errorMessage.split(":");
            if (parts.length > 1) {
                String afterColon = parts[1].trim();
                // Remove any additional details in parentheses
                int parenIndex = afterColon.indexOf("(");
                if (parenIndex > 0) {
                    return afterColon.substring(0, parenIndex).trim();
                }
                return afterColon;
            }
            
        } catch (Exception e) {
            LOG.debug("Failed to extract failed step text from configuration error", e);
        }
        
        return "Configuration error occurred";
    }

    @Override
    public int getPriority() {
        return 75; // Medium priority for configuration error parsing
    }

    @Override
    public String getStrategyName() {
        return "ConfigurationErrorStrategy";
    }

    /**
     * Helper class to hold configuration error information.
     */
    private static class ConfigurationErrorInfo {
        private String errorMessage;
        private String stackTrace;
        private String methodName;
        private String sourceFilePath;
        private int lineNumber = -1;
        private String failedStepText;

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public String getStackTrace() { return stackTrace; }
        public void setStackTrace(String stackTrace) { this.stackTrace = stackTrace; }
        
        public String getMethodName() { return methodName; }
        public void setMethodName(String methodName) { this.methodName = methodName; }
        
        public String getSourceFilePath() { return sourceFilePath; }
        public void setSourceFilePath(String sourceFilePath) { this.sourceFilePath = sourceFilePath; }
        
        public int getLineNumber() { return lineNumber; }
        public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }
        
        public String getFailedStepText() { return failedStepText; }
        public void setFailedStepText(String failedStepText) { this.failedStepText = failedStepText; }
    }
} 