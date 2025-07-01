package com.triagemate.models;

import java.util.List;

/**
 * Domain model representing information about a Gherkin scenario.
 * 
 * <p>This class contains all the information needed to understand and analyze
 * a Gherkin scenario, including its steps, tags, and context.</p>
 */
public class GherkinScenarioInfo {
    
    // Scenario information
    private final String featureName;
    private final String scenarioName;
    private final List<String> steps;
    private final List<String> tags;
    
    // File information
    private final String sourceFilePath;
    private final int lineNumber;
    private final String featureFileContent;
    
    /**
     * Constructor for GherkinScenarioInfo.
     * 
     * @param featureName the name of the feature
     * @param scenarioName the name of the scenario
     * @param steps the list of scenario steps
     * @param tags the list of scenario tags
     * @param sourceFilePath the source file path
     * @param lineNumber the line number of the scenario
     * @param featureFileContent the full content of the feature file
     */
    public GherkinScenarioInfo(String featureName, String scenarioName, List<String> steps,
                              List<String> tags, String sourceFilePath, int lineNumber,
                              String featureFileContent) {
        this.featureName = featureName;
        this.scenarioName = scenarioName;
        this.steps = steps;
        this.tags = tags;
        this.sourceFilePath = sourceFilePath;
        this.lineNumber = lineNumber;
        this.featureFileContent = featureFileContent;
    }
    
    // Getters for scenario information
    
    public String getFeatureName() {
        return featureName;
    }
    
    public String getScenarioName() {
        return scenarioName;
    }
    
    public List<String> getSteps() {
        return steps;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    // Getters for file information
    
    public String getSourceFilePath() {
        return sourceFilePath;
    }
    
    public int getLineNumber() {
        return lineNumber;
    }
    
    public String getFeatureFileContent() {
        return featureFileContent;
    }
    
    /**
     * Builder pattern for creating GherkinScenarioInfo instances.
     * This provides a fluent API for constructing GherkinScenarioInfo objects.
     */
    public static class Builder {
        private String featureName;
        private String scenarioName;
        private List<String> steps;
        private List<String> tags;
        private String sourceFilePath;
        private int lineNumber = -1;
        private String featureFileContent;
        
        public Builder withFeatureName(String featureName) {
            this.featureName = featureName;
            return this;
        }
        
        public Builder withScenarioName(String scenarioName) {
            this.scenarioName = scenarioName;
            return this;
        }
        
        public Builder withSteps(List<String> steps) {
            this.steps = steps;
            return this;
        }
        
        public Builder withTags(List<String> tags) {
            this.tags = tags;
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
        
        public Builder withFeatureFileContent(String featureFileContent) {
            this.featureFileContent = featureFileContent;
            return this;
        }
        
        public GherkinScenarioInfo build() {
            return new GherkinScenarioInfo(
                featureName, scenarioName, steps, tags, sourceFilePath, lineNumber, featureFileContent
            );
        }
    }
} 