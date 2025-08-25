package com.trace.test.models;

import java.util.List;

/**
 * Domain model representing comprehensive information about a Gherkin scenario.
 * 
 * <p>This class encapsulates all the information needed to understand, analyze, and
 * work with a Gherkin scenario. It provides rich structured data for both human
 * analysis and automated processing of Cucumber feature files.</p>
 * 
 * <p>The model supports multiple use cases:</p>
 * <ul>
 *   <li>Scenario Analysis: Complete scenario context and step details</li>
 *   <li>Feature Context: Full feature file information and background</li>
 *   <li>Data-Driven Testing: Support for scenario outlines and data tables</li>
 *   <li>Tag Management: Scenario and feature-level tagging information</li>
 *   <li>Code Navigation: Precise file location and line number information</li>
 *   <li>Content Analysis: Full feature file content for comprehensive review</li>
 * </ul>
 * 
 * <p>This model is designed to be immutable and thread-safe, making it suitable
 * for use in concurrent environments and caching scenarios.</p>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
public class GherkinScenarioInfo {
    
    // ============================================================================
    // Scenario Information
    // ============================================================================
    
    /** The name of the feature (e.g., "User Authentication") */
    private final String featureName;
    
    /** The name of the scenario (e.g., "Successful login with valid credentials") */
    private final String scenarioName;
    
    /** List of scenario steps in execution order */
    private final List<String> steps;
    
    /** List of tags associated with the scenario */
    private final List<String> tags;
    
    /** List of background steps that apply to this scenario */
    private final List<String> backgroundSteps;
    
    /** Data table rows if this is a scenario outline */
    private final List<String> dataTable;
    
    /** The complete scenario text as it appears in the feature file */
    private final String fullScenarioText;
    
    /** Whether this is a scenario outline (parameterized scenario) */
    private final boolean isScenarioOutline;
    
    // ============================================================================
    // File Information
    // ============================================================================
    
    /** Path to the feature file containing this scenario */
    private final String sourceFilePath;
    
    /** Line number where the scenario starts in the feature file */
    private final int lineNumber;
    
    /** The complete content of the feature file */
    private final String featureFileContent;
    
    // ============================================================================
    // Constructor
    // ============================================================================
    
    /**
     * Creates a new GherkinScenarioInfo instance with complete scenario details.
     * 
     * <p>This constructor accepts all possible scenario information. For most use cases,
     * consider using the {@link Builder} pattern for more flexible construction.</p>
     * 
     * @param featureName the name of the feature
     * @param scenarioName the name of the scenario
     * @param steps the list of scenario steps in execution order
     * @param tags the list of tags associated with the scenario
     * @param backgroundSteps the list of background steps that apply to this scenario
     * @param dataTable the data table rows if this is a scenario outline
     * @param fullScenarioText the complete scenario text as it appears in the feature file
     * @param isScenarioOutline whether this is a scenario outline (parameterized scenario)
     * @param sourceFilePath the path to the feature file containing this scenario
     * @param lineNumber the line number where the scenario starts in the feature file
     * @param featureFileContent the complete content of the feature file
     */
    public GherkinScenarioInfo(String featureName, String scenarioName, List<String> steps,
                              List<String> tags, List<String> backgroundSteps, List<String> dataTable,
                              String fullScenarioText, boolean isScenarioOutline,
                              String sourceFilePath, int lineNumber, String featureFileContent) {
        this.featureName = featureName;
        this.scenarioName = scenarioName;
        this.steps = steps;
        this.tags = tags;
        this.backgroundSteps = backgroundSteps;
        this.dataTable = dataTable;
        this.fullScenarioText = fullScenarioText;
        this.isScenarioOutline = isScenarioOutline;
        this.sourceFilePath = sourceFilePath;
        this.lineNumber = lineNumber;
        this.featureFileContent = featureFileContent;
    }
    
    // ============================================================================
    // Scenario Information Getters
    // ============================================================================
    
    /**
     * Gets the name of the feature.
     * 
     * @return the feature name, or null if not available
     */
    public String getFeatureName() {
        return featureName;
    }
    
    /**
     * Gets the name of the scenario.
     * 
     * @return the scenario name, or null if not available
     */
    public String getScenarioName() {
        return scenarioName;
    }
    
    /**
     * Gets the list of scenario steps in execution order.
     * 
     * <p>These steps represent the Given-When-Then sequence that defines
     * the scenario's behavior.</p>
     * 
     * @return the list of steps, or null if not available
     */
    public List<String> getSteps() {
        return steps;
    }
    
    /**
     * Gets the list of tags associated with the scenario.
     * 
     * <p>Tags can be used for filtering, grouping, and organizing scenarios
     * in test execution and reporting.</p>
     * 
     * @return the list of tags, or null if not available
     */
    public List<String> getTags() {
        return tags;
    }
    
    /**
     * Gets the list of background steps that apply to this scenario.
     * 
     * <p>Background steps are executed before each scenario in the feature
     * and provide common setup or context.</p>
     * 
     * @return the list of background steps, or null if not available
     */
    public List<String> getBackgroundSteps() {
        return backgroundSteps;
    }
    
    /**
     * Gets the data table rows if this is a scenario outline.
     * 
     * <p>For scenario outlines, this contains the parameter values that
     * will be substituted into the scenario steps during execution.</p>
     * 
     * @return the data table rows, or null if not available or not a scenario outline
     */
    public List<String> getDataTable() {
        return dataTable;
    }
    
    /**
     * Gets the complete scenario text as it appears in the feature file.
     * 
     * <p>This provides the raw scenario content for analysis, debugging,
     * and understanding the original feature file structure.</p>
     * 
     * @return the complete scenario text, or null if not available
     */
    public String getFullScenarioText() {
        return fullScenarioText;
    }
    
    /**
     * Checks whether this is a scenario outline (parameterized scenario).
     * 
     * <p>Scenario outlines use data tables to run the same scenario with
     * different parameter values.</p>
     * 
     * @return true if this is a scenario outline, false otherwise
     */
    public boolean isScenarioOutline() {
        return isScenarioOutline;
    }
    
    // ============================================================================
    // File Information Getters
    // ============================================================================
    
    /**
     * Gets the path to the feature file containing this scenario.
     * 
     * @return the source file path, or null if not available
     */
    public String getSourceFilePath() {
        return sourceFilePath;
    }
    
    /**
     * Gets the line number where the scenario starts in the feature file.
     * 
     * @return the line number, or -1 if not available
     */
    public int getLineNumber() {
        return lineNumber;
    }
    
    /**
     * Gets the complete content of the feature file.
     * 
     * <p>This provides the full feature file content for comprehensive analysis,
     * including all scenarios, background, and feature-level information.</p>
     * 
     * @return the complete feature file content, or null if not available
     */
    public String getFeatureFileContent() {
        return featureFileContent;
    }
    
    // ============================================================================
    // Builder Pattern
    // ============================================================================
    
    /**
     * Fluent builder for creating GherkinScenarioInfo instances.
     * 
     * <p>This builder provides a clean, readable API for constructing GherkinScenarioInfo
     * objects with optional fields. It's particularly useful when some information
     * may not be available during construction.</p>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * GherkinScenarioInfo scenario = new GherkinScenarioInfo.Builder()
     *     .withFeatureName("User Authentication")
     *     .withScenarioName("Successful login with valid credentials")
     *     .withSteps(Arrays.asList(
     *         "Given I am on the login page",
     *         "When I enter valid credentials",
     *         "And I click the login button",
     *         "Then I should be logged in successfully"
     *     ))
     *     .withTags(Arrays.asList("@smoke", "@login"))
     *     .withBackgroundSteps(Arrays.asList("Given the application is running"))
     *     .withSourceFilePath("/path/to/login.feature")
     *     .withLineNumber(15)
     *     .withFeatureFileContent("Feature: User Authentication\n...")
     *     .build();
     * }</pre>
     */
    public static class Builder {
        
        // Scenario information
        private String featureName;
        private String scenarioName;
        private List<String> steps;
        private List<String> tags;
        private List<String> backgroundSteps;
        private List<String> dataTable;
        private String fullScenarioText;
        private boolean isScenarioOutline = false;
        
        // File information
        private String sourceFilePath;
        private int lineNumber = -1;
        private String featureFileContent;
        
        // ========================================================================
        // Scenario Information Builders
        // ========================================================================
        
        /**
         * Sets the feature name.
         * 
         * @param featureName the name of the feature
         * @return this builder for method chaining
         */
        public Builder withFeatureName(String featureName) {
            this.featureName = featureName;
            return this;
        }
        
        /**
         * Sets the scenario name.
         * 
         * @param scenarioName the name of the scenario
         * @return this builder for method chaining
         */
        public Builder withScenarioName(String scenarioName) {
            this.scenarioName = scenarioName;
            return this;
        }
        
        /**
         * Sets the scenario steps.
         * 
         * @param steps the list of scenario steps in execution order
         * @return this builder for method chaining
         */
        public Builder withSteps(List<String> steps) {
            this.steps = steps;
            return this;
        }
        
        /**
         * Sets the scenario tags.
         * 
         * @param tags the list of tags associated with the scenario
         * @return this builder for method chaining
         */
        public Builder withTags(List<String> tags) {
            this.tags = tags;
            return this;
        }
        
        /**
         * Sets the background steps.
         * 
         * @param backgroundSteps the list of background steps that apply to this scenario
         * @return this builder for method chaining
         */
        public Builder withBackgroundSteps(List<String> backgroundSteps) {
            this.backgroundSteps = backgroundSteps;
            return this;
        }
        
        /**
         * Sets the data table.
         * 
         * @param dataTable the data table rows if this is a scenario outline
         * @return this builder for method chaining
         */
        public Builder withDataTable(List<String> dataTable) {
            this.dataTable = dataTable;
            return this;
        }
        
        /**
         * Sets the full scenario text.
         * 
         * @param fullScenarioText the complete scenario text as it appears in the feature file
         * @return this builder for method chaining
         */
        public Builder withFullScenarioText(String fullScenarioText) {
            this.fullScenarioText = fullScenarioText;
            return this;
        }
        
        /**
         * Sets whether this is a scenario outline.
         * 
         * @param isScenarioOutline whether this is a scenario outline (parameterized scenario)
         * @return this builder for method chaining
         */
        public Builder withIsScenarioOutline(boolean isScenarioOutline) {
            this.isScenarioOutline = isScenarioOutline;
            return this;
        }
        
        // ========================================================================
        // File Information Builders
        // ========================================================================
        
        /**
         * Sets the source file path.
         * 
         * @param sourceFilePath the path to the feature file containing this scenario
         * @return this builder for method chaining
         */
        public Builder withSourceFilePath(String sourceFilePath) {
            this.sourceFilePath = sourceFilePath;
            return this;
        }
        
        /**
         * Sets the line number.
         * 
         * @param lineNumber the line number where the scenario starts in the feature file
         * @return this builder for method chaining
         */
        public Builder withLineNumber(int lineNumber) {
            this.lineNumber = lineNumber;
            return this;
        }
        
        /**
         * Sets the feature file content.
         * 
         * @param featureFileContent the complete content of the feature file
         * @return this builder for method chaining
         */
        public Builder withFeatureFileContent(String featureFileContent) {
            this.featureFileContent = featureFileContent;
            return this;
        }
        
        // ========================================================================
        // Build Method
        // ========================================================================
        
        /**
         * Builds a new GherkinScenarioInfo instance with the configured values.
         * 
         * @return a new GherkinScenarioInfo instance with all specified values
         */
        public GherkinScenarioInfo build() {
            return new GherkinScenarioInfo(
                featureName, scenarioName, steps, tags, backgroundSteps, dataTable,
                fullScenarioText, isScenarioOutline, sourceFilePath, lineNumber, featureFileContent
            );
        }
    }
} 