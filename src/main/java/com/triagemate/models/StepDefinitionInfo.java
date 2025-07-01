package com.triagemate.models;

import java.util.List;

/**
 * Domain model representing information about a Cucumber step definition.
 * 
 * <p>This class contains all the information needed to understand and analyze
 * a step definition method, including its location, parameters, and implementation.</p>
 */
public class StepDefinitionInfo {
    
    // Method information
    private final String methodName;
    private final String className;
    private final String packageName;
    private final String sourceFilePath;
    private final int lineNumber;
    
    // Step definition information
    private final String stepPattern;
    private final List<String> parameters;
    private final String methodText;
    
    /**
     * Constructor for StepDefinitionInfo.
     * 
     * @param methodName the name of the step definition method
     * @param className the name of the class containing the method
     * @param packageName the package name of the class
     * @param sourceFilePath the source file path
     * @param lineNumber the line number of the method
     * @param stepPattern the Cucumber step pattern
     * @param parameters the method parameters
     * @param methodText the full method text
     */
    public StepDefinitionInfo(String methodName, String className, String packageName,
                             String sourceFilePath, int lineNumber, String stepPattern,
                             List<String> parameters, String methodText) {
        this.methodName = methodName;
        this.className = className;
        this.packageName = packageName;
        this.sourceFilePath = sourceFilePath;
        this.lineNumber = lineNumber;
        this.stepPattern = stepPattern;
        this.parameters = parameters;
        this.methodText = methodText;
    }
    
    // Getters for method information
    
    public String getMethodName() {
        return methodName;
    }
    
    public String getClassName() {
        return className;
    }
    
    public String getPackageName() {
        return packageName;
    }
    
    public String getSourceFilePath() {
        return sourceFilePath;
    }
    
    public int getLineNumber() {
        return lineNumber;
    }
    
    // Getters for step definition information
    
    public String getStepPattern() {
        return stepPattern;
    }
    
    public List<String> getParameters() {
        return parameters;
    }
    
    public String getMethodText() {
        return methodText;
    }
    
    /**
     * Builder pattern for creating StepDefinitionInfo instances.
     * This provides a fluent API for constructing StepDefinitionInfo objects.
     */
    public static class Builder {
        private String methodName;
        private String className;
        private String packageName;
        private String sourceFilePath;
        private int lineNumber = -1;
        private String stepPattern;
        private List<String> parameters;
        private String methodText;
        
        public Builder withMethodName(String methodName) {
            this.methodName = methodName;
            return this;
        }
        
        public Builder withClassName(String className) {
            this.className = className;
            return this;
        }
        
        public Builder withPackageName(String packageName) {
            this.packageName = packageName;
            return this;
        }
        
        public Builder withSourceFilePath(String sourceFilePath) {
            this.sourceFilePath = sourceFilePath;
            return this;
        }
        
        public Builder withLineNumber(int lineNumber) {
            this.lineNumber = lineNumber;
            return this;
        }
        
        public Builder withStepPattern(String stepPattern) {
            this.stepPattern = stepPattern;
            return this;
        }
        
        public Builder withParameters(List<String> parameters) {
            this.parameters = parameters;
            return this;
        }
        
        public Builder withMethodText(String methodText) {
            this.methodText = methodText;
            return this;
        }
        
        public StepDefinitionInfo build() {
            return new StepDefinitionInfo(
                methodName, className, packageName, sourceFilePath, lineNumber,
                stepPattern, parameters, methodText
            );
        }
    }
} 