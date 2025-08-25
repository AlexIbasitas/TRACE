package com.trace.test.models;

/**
 * Domain model representing a failed Cucumber test with full context.
 * 
 * <p>This class serves as the central data structure for test failure analysis in the
 * TRACE plugin. It encapsulates all information needed to understand, analyze,
 * and provide intelligent suggestions for test failures.</p>
 * 
 * <p>The model supports multiple use cases:</p>
 * <ul>
 *   <li>Failure Analysis: Complete context for AI-powered failure diagnosis</li>
 *   <li>UI Display: Rich information for the TriagePanel interface</li>
 *   <li>Code Navigation: Precise source file and line number information</li>
 *   <li>Debugging Support: Parsing metadata and performance metrics</li>
 * </ul>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
public class FailureInfo {
    
    // ============================================================================
    // Core Failure Information
    // ============================================================================
    
    /** The name of the failed scenario (e.g., "User login with valid credentials") */
    private final String scenarioName;
    
    /** The text of the specific step that failed (e.g., "I click the login button") */
    private final String failedStepText;
    
    /** The complete stack trace from the test failure */
    private final String stackTrace;
    
    /** Path to the source file containing the step definition */
    private final String sourceFilePath;
    
    /** Line number of the step definition in the source file */
    private final int lineNumber;
    
    // ============================================================================
    // Rich Structured Data (Composed Objects)
    // ============================================================================
    
    /** Detailed information about the step definition method */
    private final StepDefinitionInfo stepDefinitionInfo;
    
    /** Complete Gherkin scenario context and metadata */
    private final GherkinScenarioInfo gherkinScenarioInfo;
    
    // ============================================================================
    // Assertion Details (Extracted from Test Output)
    // ============================================================================
    
    /** Expected value from the assertion (e.g., "true", "User logged in") */
    private final String expectedValue;
    
    /** Actual value from the assertion (e.g., "false", "Login failed") */
    private final String actualValue;
    
    /** Error message from the assertion failure */
    private final String errorMessage;
    
    // ============================================================================
    // Parsing Metadata (Debugging and Performance)
    // ============================================================================
    
    /** Time taken to parse the test output in milliseconds */
    private final long parsingTime;
    
    // ============================================================================
    // Constructor
    // ============================================================================
    
    /**
     * Creates a new FailureInfo instance with all failure context.
     * 
     * <p>This constructor accepts all possible failure information. For most use cases,
     * consider using the {@link Builder} pattern for more flexible construction.</p>
     * 
     * @param scenarioName the name of the failed scenario
     * @param failedStepText the text of the step that failed
     * @param stackTrace the complete stack trace of the failure
     * @param sourceFilePath path to the source file containing the step definition
     * @param lineNumber line number of the step definition in the source file
     * @param stepDefinitionInfo detailed step definition information (can be null)
     * @param gherkinScenarioInfo complete Gherkin scenario context (can be null)
     * @param expectedValue the expected value from the assertion
     * @param actualValue the actual value from the assertion
     * @param errorMessage the error message from the assertion
     * @param parsingTime the time taken to parse the test output in milliseconds
     */
    public FailureInfo(String scenarioName, String failedStepText, String stackTrace,
                      String sourceFilePath, int lineNumber,
                      StepDefinitionInfo stepDefinitionInfo, GherkinScenarioInfo gherkinScenarioInfo,
                      String expectedValue, String actualValue, String errorMessage, long parsingTime) {
        this.scenarioName = scenarioName;
        this.failedStepText = failedStepText;
        this.stackTrace = stackTrace;
        this.sourceFilePath = sourceFilePath;
        this.lineNumber = lineNumber;
        this.stepDefinitionInfo = stepDefinitionInfo;
        this.gherkinScenarioInfo = gherkinScenarioInfo;
        this.expectedValue = expectedValue;
        this.actualValue = actualValue;
        this.errorMessage = errorMessage;
        this.parsingTime = parsingTime;
    }
    
    // ============================================================================
    // Core Failure Information Getters
    // ============================================================================
    
    /**
     * Gets the name of the failed scenario.
     * 
     * @return the scenario name, or null if not available
     */
    public String getScenarioName() {
        return scenarioName;
    }
    
    /**
     * Gets the text of the step that failed.
     * 
     * @return the failed step text, or null if not available
     */
    public String getFailedStepText() {
        return failedStepText;
    }
    
    /**
     * Gets the complete stack trace from the test failure.
     * 
     * @return the stack trace, or null if not available
     */
    public String getStackTrace() {
        return stackTrace;
    }
    
    /**
     * Gets the path to the source file containing the step definition.
     * 
     * @return the source file path, or null if not available
     */
    public String getSourceFilePath() {
        return sourceFilePath;
    }
    
    /**
     * Gets the line number of the step definition in the source file.
     * 
     * @return the line number, or -1 if not available
     */
    public int getLineNumber() {
        return lineNumber;
    }
    
    // ============================================================================
    // Rich Structured Data Getters
    // ============================================================================
    
    /**
     * Gets detailed information about the step definition method.
     * 
     * <p>This provides rich structured data including method signature, parameters,
     * and implementation details for comprehensive analysis.</p>
     * 
     * @return StepDefinitionInfo object with complete method details, or null if not available
     */
    public StepDefinitionInfo getStepDefinitionInfo() {
        return stepDefinitionInfo;
    }
    
    /**
     * Gets complete Gherkin scenario context and metadata.
     * 
     * <p>This provides rich structured data including scenario steps, tags, background,
     * and full feature context for comprehensive analysis.</p>
     * 
     * @return GherkinScenarioInfo object with complete scenario details, or null if not available
     */
    public GherkinScenarioInfo getGherkinScenarioInfo() {
        return gherkinScenarioInfo;
    }
    
    // ============================================================================
    // Assertion Details Getters
    // ============================================================================
    
    /**
     * Gets the expected value from the assertion.
     * 
     * @return the expected value, or null if not available
     */
    public String getExpectedValue() {
        return expectedValue;
    }
    
    /**
     * Gets the actual value from the assertion.
     * 
     * @return the actual value, or null if not available
     */
    public String getActualValue() {
        return actualValue;
    }
    
    /**
     * Gets the error message from the assertion failure.
     * 
     * @return the error message, or null if not available
     */
    public String getErrorMessage() {
        return errorMessage;
    }
    
    // ============================================================================
    // Parsing Metadata Getters
    // ============================================================================
    
    /**
     * Gets the time taken to parse the test output.
     * 
     * @return the parsing time in milliseconds
     */
    public long getParsingTime() {
        return parsingTime;
    }
    
    // ============================================================================
    // Builder Pattern
    // ============================================================================
    
    /**
     * Fluent builder for creating FailureInfo instances.
     * 
     * <p>This builder provides a clean, readable API for constructing FailureInfo
     * objects with optional fields.</p>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * FailureInfo failure = new FailureInfo.Builder()
     *     .withScenarioName("User login test")
     *     .withFailedStepText("I click the login button")
     *     .withStackTrace("java.lang.AssertionError: Button not found")
     *     .withStepDefinitionInfo(stepDefInfo)
     *     .withGherkinScenarioInfo(scenarioInfo)
     *     .build();
     * }</pre>
     */
    public static class Builder {
        
        // Core failure information
        private String scenarioName;
        private String failedStepText;
        private String stackTrace;
        private String sourceFilePath;
        private int lineNumber = -1;
        
        // Rich structured data
        private StepDefinitionInfo stepDefinitionInfo;
        private GherkinScenarioInfo gherkinScenarioInfo;
        
        // Assertion details
        private String expectedValue;
        private String actualValue;
        private String errorMessage;
        
        // Parsing metadata
        private long parsingTime;
        
        // ========================================================================
        // Core Failure Information Builders
        // ========================================================================
        
        /**
         * Sets the scenario name.
         * 
         * @param scenarioName the name of the failed scenario
         * @return this builder for method chaining
         */
        public Builder withScenarioName(String scenarioName) {
            this.scenarioName = scenarioName;
            return this;
        }
        
        /**
         * Sets the failed step text.
         * 
         * @param failedStepText the text of the step that failed
         * @return this builder for method chaining
         */
        public Builder withFailedStepText(String failedStepText) {
            this.failedStepText = failedStepText;
            return this;
        }
        
        /**
         * Sets the stack trace.
         * 
         * @param stackTrace the complete stack trace from the failure
         * @return this builder for method chaining
         */
        public Builder withStackTrace(String stackTrace) {
            this.stackTrace = stackTrace;
            return this;
        }
        
        /**
         * Sets the source file path.
         * 
         * @param sourceFilePath path to the source file containing the step definition
         * @return this builder for method chaining
         */
        public Builder withSourceFilePath(String sourceFilePath) {
            this.sourceFilePath = sourceFilePath;
            return this;
        }
        
        /**
         * Sets the line number.
         * 
         * @param lineNumber line number of the step definition in the source file
         * @return this builder for method chaining
         */
        public Builder withLineNumber(int lineNumber) {
            this.lineNumber = lineNumber;
            return this;
        }
        
        // ========================================================================
        // Rich Structured Data Builders
        // ========================================================================
        
        /**
         * Sets the step definition information.
         * 
         * @param stepDefinitionInfo detailed step definition information
         * @return this builder for method chaining
         */
        public Builder withStepDefinitionInfo(StepDefinitionInfo stepDefinitionInfo) {
            this.stepDefinitionInfo = stepDefinitionInfo;
            return this;
        }
        
        /**
         * Sets the Gherkin scenario information.
         * 
         * @param gherkinScenarioInfo complete Gherkin scenario context
         * @return this builder for method chaining
         */
        public Builder withGherkinScenarioInfo(GherkinScenarioInfo gherkinScenarioInfo) {
            this.gherkinScenarioInfo = gherkinScenarioInfo;
            return this;
        }
        
        // ========================================================================
        // Assertion Details Builders
        // ========================================================================
        
        /**
         * Sets the expected value from the assertion.
         * 
         * @param expectedValue the expected value
         * @return this builder for method chaining
         */
        public Builder withExpectedValue(String expectedValue) {
            this.expectedValue = expectedValue;
            return this;
        }
        
        /**
         * Sets the actual value from the assertion.
         * 
         * @param actualValue the actual value
         * @return this builder for method chaining
         */
        public Builder withActualValue(String actualValue) {
            this.actualValue = actualValue;
            return this;
        }
        
        /**
         * Sets the error message from the assertion.
         * 
         * @param errorMessage the error message
         * @return this builder for method chaining
         */
        public Builder withErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }
        
        // ========================================================================
        // Parsing Metadata Builders
        // ========================================================================
        
        /**
         * Sets the parsing time.
         * 
         * @param parsingTime the time taken to parse the test output in milliseconds
         * @return this builder for method chaining
         */
        public Builder withParsingTime(long parsingTime) {
            this.parsingTime = parsingTime;
            return this;
        }
        
        // ========================================================================
        // Build Method
        // ========================================================================
        
        /**
         * Builds a new FailureInfo instance with the configured values.
         * 
         * @return a new FailureInfo instance with all specified values
         */
        public FailureInfo build() {
            return new FailureInfo(scenarioName, failedStepText, stackTrace,
                                 sourceFilePath, lineNumber,
                                 stepDefinitionInfo, gherkinScenarioInfo,
                                 expectedValue, actualValue, errorMessage, parsingTime);
        }
    }
} 