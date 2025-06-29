package com.triagemate.models;

/**
 * Domain model representing comprehensive information about a failed Cucumber test.
 * 
 * <p>This class serves multiple purposes in the TriageMate plugin architecture:</p>
 * <ul>
 *   <li><strong>Parsing Layer:</strong> Stores extracted assertion details and parsing metadata</li>
 *   <li><strong>UI Display:</strong> Provides all information needed for the TriagePanel</li>
 *   <li><strong>AI Analysis:</strong> Contains context needed for prompt generation</li>
 *   <li><strong>Code Navigation:</strong> Includes source file location information</li>
 *   <li><strong>Debugging:</strong> Stores parsing strategy and performance metrics</li>
 * </ul>
 * 
 * <p>While this class serves multiple responsibilities, it represents a cohesive domain concept:
 * "A test failure with all its context and metadata." This design choice prioritizes simplicity
 * and performance over strict adherence to the Single Responsibility Principle, which is
 * appropriate for a focused IntelliJ plugin.</p>
 */
public class FailureInfo {
    
    // Core failure information
    private final String scenarioName;
    private final String failedStepText;
    private final String stackTrace;
    private final String stepDefinitionMethod;
    private final String gherkinScenario;
    private final String sourceFilePath;
    private final int lineNumber;
    
    // Assertion details (extracted from test output)
    private final String expectedValue;
    private final String actualValue;
    private final String assertionType;
    private final String errorMessage;
    
    // Parsing metadata (for debugging and performance monitoring)
    private final String parsingStrategy;
    private final long parsingTime;
    
    /**
     * Constructor for FailureInfo.
     * 
     * @param scenarioName the name of the failed scenario
     * @param failedStepText the text of the step that failed
     * @param stackTrace the stack trace of the failure
     * @param stepDefinitionMethod the Java method implementing the step definition
     * @param gherkinScenario the full Gherkin scenario text
     * @param sourceFilePath path to the source file containing the step definition
     * @param lineNumber line number of the step definition in the source file
     * @param expectedValue the expected value from the assertion
     * @param actualValue the actual value from the assertion
     * @param assertionType the type of assertion (e.g., "HAMCREST", "JUNIT")
     * @param errorMessage the error message from the assertion
     * @param parsingStrategy the name of the parsing strategy used
     * @param parsingTime the time taken to parse the test output in milliseconds
     */
    public FailureInfo(String scenarioName, String failedStepText, String stackTrace,
                      String stepDefinitionMethod, String gherkinScenario,
                      String sourceFilePath, int lineNumber,
                      String expectedValue, String actualValue, String assertionType,
                      String errorMessage, String parsingStrategy, long parsingTime) {
        this.scenarioName = scenarioName;
        this.failedStepText = failedStepText;
        this.stackTrace = stackTrace;
        this.stepDefinitionMethod = stepDefinitionMethod;
        this.gherkinScenario = gherkinScenario;
        this.sourceFilePath = sourceFilePath;
        this.lineNumber = lineNumber;
        this.expectedValue = expectedValue;
        this.actualValue = actualValue;
        this.assertionType = assertionType;
        this.errorMessage = errorMessage;
        this.parsingStrategy = parsingStrategy;
        this.parsingTime = parsingTime;
    }
    
    // Getters for core failure information
    
    public String getScenarioName() {
        return scenarioName;
    }
    
    public String getFailedStepText() {
        return failedStepText;
    }
    
    public String getStackTrace() {
        return stackTrace;
    }
    
    public String getStepDefinitionMethod() {
        return stepDefinitionMethod;
    }
    
    public String getGherkinScenario() {
        return gherkinScenario;
    }
    
    public String getSourceFilePath() {
        return sourceFilePath;
    }
    
    public int getLineNumber() {
        return lineNumber;
    }
    
    // Getters for assertion details
    
    public String getExpectedValue() {
        return expectedValue;
    }
    
    public String getActualValue() {
        return actualValue;
    }
    
    public String getAssertionType() {
        return assertionType;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    // Getters for parsing metadata
    
    public String getParsingStrategy() {
        return parsingStrategy;
    }
    
    public long getParsingTime() {
        return parsingTime;
    }
    
    /**
     * Builder pattern for creating FailureInfo instances.
     * This provides a fluent API for constructing FailureInfo objects with optional fields.
     */
    public static class Builder {
        private String scenarioName;
        private String failedStepText;
        private String stackTrace;
        private String stepDefinitionMethod;
        private String gherkinScenario;
        private String sourceFilePath;
        private int lineNumber = -1;
        private String expectedValue;
        private String actualValue;
        private String assertionType;
        private String errorMessage;
        private String parsingStrategy;
        private long parsingTime;
        
        public Builder withScenarioName(String scenarioName) {
            this.scenarioName = scenarioName;
            return this;
        }
        
        public Builder withFailedStepText(String failedStepText) {
            this.failedStepText = failedStepText;
            return this;
        }
        
        public Builder withStackTrace(String stackTrace) {
            this.stackTrace = stackTrace;
            return this;
        }
        
        public Builder withStepDefinitionMethod(String stepDefinitionMethod) {
            this.stepDefinitionMethod = stepDefinitionMethod;
            return this;
        }
        
        public Builder withGherkinScenario(String gherkinScenario) {
            this.gherkinScenario = gherkinScenario;
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
        
        public Builder withExpectedValue(String expectedValue) {
            this.expectedValue = expectedValue;
            return this;
        }
        
        public Builder withActualValue(String actualValue) {
            this.actualValue = actualValue;
            return this;
        }
        
        public Builder withAssertionType(String assertionType) {
            this.assertionType = assertionType;
            return this;
        }
        
        public Builder withErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }
        
        public Builder withParsingStrategy(String parsingStrategy) {
            this.parsingStrategy = parsingStrategy;
            return this;
        }
        
        public Builder withParsingTime(long parsingTime) {
            this.parsingTime = parsingTime;
            return this;
        }
        
        /**
         * Builds a FailureInfo instance with the configured values.
         * 
         * @return a new FailureInfo instance
         */
        public FailureInfo build() {
            return new FailureInfo(scenarioName, failedStepText, stackTrace,
                                 stepDefinitionMethod, gherkinScenario,
                                 sourceFilePath, lineNumber,
                                 expectedValue, actualValue, assertionType,
                                 errorMessage, parsingStrategy, parsingTime);
        }
    }
} 