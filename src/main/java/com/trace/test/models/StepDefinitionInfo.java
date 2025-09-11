package com.trace.test.models;

import java.util.List;

/**
 * Domain model representing comprehensive information about a Cucumber step definition.
 * 
 * <p>This class encapsulates all the information needed to understand, analyze, and
 * navigate to a step definition method. It provides rich structured data for both
 * human analysis and automated processing.</p>
 * 
 * <p>The model supports multiple use cases:</p>
 * <ul>
 *   <li>Code Navigation: Precise file location and line number information</li>
 *   <li>Method Analysis: Complete method signature and parameter details</li>
 *   <li>Pattern Matching: Cucumber step patterns for step-to-method mapping</li>
 *   <li>Implementation Review: Full method text for code analysis</li>
 *   <li>Debugging Support: Package and class context for troubleshooting</li>
 * </ul>
 * 
 * <p>This model is designed to be immutable and thread-safe, making it suitable
 * for use in concurrent environments and caching scenarios.</p>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
public class StepDefinitionInfo {
    
    // --- Method information ---
    
    /** The name of the step definition method (e.g., "clickLoginButton") */
    private final String methodName;
    
    /** The name of the class containing the method (e.g., "LoginStepDefinitions") */
    private final String className;
    
    /** The package name of the class (e.g., "com.example.steps") */
    private final String packageName;
    
    /** Path to the source file containing the method */
    private final String sourceFilePath;
    
    /** Line number of the method in the source file */
    private final int lineNumber;
    
    // --- Step definition information ---
    
    /** The Cucumber step pattern (e.g., "^I click the (.*?) button$") */
    private final String stepPattern;
    
    /** List of method parameters extracted from the pattern */
    private final List<String> parameters;
    
    /** The complete method text including signature and implementation */
    private final String methodText;
    
    // --- Constructor ---
    
    /**
     * Creates a new StepDefinitionInfo instance with complete method details.
     * 
     * <p>This constructor accepts all possible step definition information. For most
     * use cases, consider using the {@link Builder} pattern for more flexible construction.</p>
     * 
     * @param methodName the name of the step definition method
     * @param className the name of the class containing the method
     * @param packageName the package name of the class
     * @param sourceFilePath the source file path
     * @param lineNumber the line number of the method
     * @param stepPattern the Cucumber step pattern
     * @param parameters the method parameters extracted from the pattern
     * @param methodText the complete method text including signature and implementation
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
    
    // --- Method information getters ---
    
    /**
     * Gets the name of the step definition method.
     * 
     * @return the method name, or null if not available
     */
    public String getMethodName() {
        return methodName;
    }
    
    /**
     * Gets the name of the class containing the method.
     * 
     * @return the class name, or null if not available
     */
    public String getClassName() {
        return className;
    }
    
    /**
     * Gets the package name of the class.
     * 
     * @return the package name, or null if not available
     */
    public String getPackageName() {
        return packageName;
    }
    
    /**
     * Gets the path to the source file containing the method.
     * 
     * @return the source file path, or null if not available
     */
    public String getSourceFilePath() {
        return sourceFilePath;
    }
    
    /**
     * Gets the line number of the method in the source file.
     * 
     * @return the line number, or -1 if not available
     */
    public int getLineNumber() {
        return lineNumber;
    }
    
    // --- Step definition information getters ---
    
    /**
     * Gets the Cucumber step pattern.
     * 
     * <p>The step pattern defines how Cucumber matches Gherkin steps to this method.
     * It may contain capture groups that correspond to method parameters.</p>
     * 
     * @return the step pattern, or null if not available
     */
    public String getStepPattern() {
        return stepPattern;
    }
    
    /**
     * Gets the list of method parameters extracted from the step pattern.
     * 
     * <p>These parameters represent the capture groups in the step pattern and
     * correspond to the method's formal parameters.</p>
     * 
     * @return the list of parameters, or null if not available
     */
    public List<String> getParameters() {
        return parameters;
    }
    
    /**
     * Gets the complete method text including signature and implementation.
     * 
     * <p>This provides the full method code for analysis, debugging, and
     * understanding the step definition implementation.</p>
     * 
     * @return the complete method text, or null if not available
     */
    public String getMethodText() {
        return methodText;
    }
    
    // --- Builder pattern ---
    
    /**
     * Fluent builder for creating StepDefinitionInfo instances.
     * 
     * <p>This builder provides a fluent API for constructing StepDefinitionInfo
     * objects with optional fields.</p>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * StepDefinitionInfo stepDef = new StepDefinitionInfo.Builder()
     *     .withMethodName("clickLoginButton")
     *     .withClassName("LoginStepDefinitions")
     *     .withPackageName("com.example.steps")
     *     .withSourceFilePath("/path/to/LoginStepDefinitions.java")
     *     .withLineNumber(25)
     *     .withStepPattern("^I click the (.*?) button$")
     *     .withParameters(Arrays.asList("buttonName"))
     *     .withMethodText("@When(\"^I click the (.*?) button$\") public void clickLoginButton(String buttonName) { ... }")
     *     .build();
     * }</pre>
     */
    public static class Builder {
        
        // Method information
        private String methodName;
        private String className;
        private String packageName;
        private String sourceFilePath;
        private int lineNumber = -1;
        
        // Step definition information
        private String stepPattern;
        private List<String> parameters;
        private String methodText;
        
        // --- Method information builders ---
        
        /**
         * Sets the method name.
         * 
         * @param methodName the name of the step definition method
         * @return this builder for method chaining
         */
        public Builder withMethodName(String methodName) {
            this.methodName = methodName;
            return this;
        }
        
        /**
         * Sets the class name.
         * 
         * @param className the name of the class containing the method
         * @return this builder for method chaining
         */
        public Builder withClassName(String className) {
            this.className = className;
            return this;
        }
        
        /**
         * Sets the package name.
         * 
         * @param packageName the package name of the class
         * @return this builder for method chaining
         */
        public Builder withPackageName(String packageName) {
            this.packageName = packageName;
            return this;
        }
        
        /**
         * Sets the source file path.
         * 
         * @param sourceFilePath the path to the source file containing the method
         * @return this builder for method chaining
         */
        public Builder withSourceFilePath(String sourceFilePath) {
            this.sourceFilePath = sourceFilePath;
            return this;
        }
        
        /**
         * Sets the line number.
         * 
         * @param lineNumber the line number of the method in the source file
         * @return this builder for method chaining
         */
        public Builder withLineNumber(int lineNumber) {
            this.lineNumber = lineNumber;
            return this;
        }
        
        // --- Step definition information builders ---
        
        /**
         * Sets the step pattern.
         * 
         * @param stepPattern the Cucumber step pattern
         * @return this builder for method chaining
         */
        public Builder withStepPattern(String stepPattern) {
            this.stepPattern = stepPattern;
            return this;
        }
        
        /**
         * Sets the method parameters.
         * 
         * @param parameters the list of method parameters extracted from the pattern
         * @return this builder for method chaining
         */
        public Builder withParameters(List<String> parameters) {
            this.parameters = parameters;
            return this;
        }
        
        /**
         * Sets the method text.
         * 
         * @param methodText the complete method text including signature and implementation
         * @return this builder for method chaining
         */
        public Builder withMethodText(String methodText) {
            this.methodText = methodText;
            return this;
        }
        
        // --- Build method ---
        
        /**
         * Builds a new StepDefinitionInfo instance with the configured values.
         * 
         * @return a new StepDefinitionInfo instance with all specified values
         */
        public StepDefinitionInfo build() {
            return new StepDefinitionInfo(
                methodName, className, packageName, sourceFilePath, lineNumber,
                stepPattern, parameters, methodText
            );
        }
    }
} 