package com.triagemate.extractors;

import com.intellij.execution.testframework.sm.runner.SMTestProxy;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.diagnostic.Logger;
import com.triagemate.models.FailureInfo;
import com.triagemate.extractors.stacktrace_strategies.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Extracts stack trace and failed step text from test failure information.
 * 
 * <p>This class implements the strategy pattern to handle different types of test failures
 * using specialized parsing strategies. It automatically selects the appropriate strategy
 * based on the test output content and provides fallback mechanisms for unknown formats.</p>
 * 
 * <p>The extractor coordinates multiple parsing strategies in priority order:
 * <ul>
 *   <li>JUnit Comparison Failure (Priority: 100)</li>
 *   <li>WebDriver Errors (Priority: 90)</li>
 *   <li>Cucumber Errors (Priority: 85)</li>
 *   <li>Runtime Errors (Priority: 80)</li>
 *   <li>Configuration Errors (Priority: 70)</li>
 *   <li>Generic Errors (Priority: 10)</li>
 * </ul></p>
 */
public class StackTraceExtractor {
    private static final Logger LOG = Logger.getInstance(StackTraceExtractor.class);
    private final List<FailureParsingStrategy> strategies;
    private final Project project;

    /**
     * Constructor for StackTraceExtractor.
     * 
     * @param project The IntelliJ project context for PSI operations
     */
    public StackTraceExtractor(Project project) {
        if (project == null) {
            throw new NullPointerException("Project cannot be null");
        }
        this.project = project;
        this.strategies = initializeStrategies();
    }

    /**
     * Initializes all available parsing strategies in priority order.
     * 
     * @return List of strategies sorted by priority (highest first)
     */
    private List<FailureParsingStrategy> initializeStrategies() {
        List<FailureParsingStrategy> strategyList = new ArrayList<>();
        
        // Add strategies in order of specificity (highest priority first)
        strategyList.add(new JUnitComparisonFailureStrategy(project));
        strategyList.add(new WebDriverErrorStrategy(project));
        strategyList.add(new CucumberErrorStrategy(project));
        strategyList.add(new RuntimeErrorStrategy(project));
        strategyList.add(new ConfigurationErrorStrategy(project));
        strategyList.add(new GenericErrorStrategy(project));
        
        // Sort by priority (highest first)
        strategyList.sort(Comparator.comparing(FailureParsingStrategy::getPriority).reversed());
        
        LOG.info("Initialized " + strategyList.size() + " parsing strategies");
        for (FailureParsingStrategy strategy : strategyList) {
            LOG.debug("Strategy: " + strategy.getStrategyName() + " (Priority: " + strategy.getPriority() + ")");
        }
        
        return strategyList;
    }

    /**
     * Extracts comprehensive failure information from test output using strategy pattern.
     * 
     * <p>This method automatically selects the appropriate parsing strategy based on the
     * test output content and returns detailed failure information including stack trace,
     * error messages, and code location details.</p>
     * 
     * @param testOutput The test output to parse
     * @return FailureInfo containing extracted failure details
     * @throws IllegalArgumentException if testOutput is null
     */
    public FailureInfo extractFailureInfo(String testOutput) {
        if (testOutput == null) {
            throw new IllegalArgumentException("testOutput cannot be null");
        }
        
        LOG.debug("Starting failure info extraction", "outputLength", testOutput.length());
        
        // Try each strategy in priority order
        for (FailureParsingStrategy strategy : strategies) {
            try {
                if (strategy.canHandle(testOutput)) {
                    LOG.info("Using strategy: " + strategy.getStrategyName());
                    
                    long startTime = System.currentTimeMillis();
                    FailureInfo result = strategy.parse(testOutput);
                    long duration = System.currentTimeMillis() - startTime;
                    
                    LOG.info("Successfully parsed with " + strategy.getStrategyName() + 
                            " (duration: " + duration + "ms, priority: " + strategy.getPriority() + ")");
                    
                    return result;
                }
            } catch (Exception e) {
                LOG.warn("Strategy " + strategy.getStrategyName() + " failed", e);
                // Continue to next strategy
            }
        }
        
        // If no strategy can handle the output, create a minimal failure info
        LOG.warn("No strategy could handle the test output, creating minimal failure info");
        return createMinimalFailureInfo(testOutput);
    }

    /**
     * Extracts the stack trace from a failed test using strategy pattern.
     *
     * @param test The failed test proxy
     * @return The stack trace as a string
     */
    public String extractStackTrace(SMTestProxy test) {
        if (test == null) {
            return "Test proxy is null";
        }
        
        String errorMessage = test.getErrorMessage();
        if (errorMessage == null || errorMessage.isEmpty()) {
        return "Stack trace not available";
    }
        
        try {
            FailureInfo failureInfo = extractFailureInfo(errorMessage);
            String stackTrace = failureInfo.getStackTrace();
            // If the strategy returned an empty stack trace, fallback to raw error message
            if (stackTrace == null || stackTrace.trim().isEmpty()) {
                return errorMessage;
            }
            return stackTrace;
        } catch (Exception e) {
            LOG.warn("Failed to extract stack trace using strategies, falling back to raw error message", e);
            return errorMessage;
        }
    }

    /**
     * Extracts the text of the failed step from the test information using strategy pattern.
     *
     * @param test The failed test proxy
     * @return The text of the failed step
     */
    public String extractFailedStepText(SMTestProxy test) {
        if (test == null) {
            return "Test proxy is null";
        }
        
        String errorMessage = test.getErrorMessage();
        if (errorMessage == null || errorMessage.isEmpty()) {
            return "Failed step text not available";
        }
        
        try {
            FailureInfo failureInfo = extractFailureInfo(errorMessage);
            if (failureInfo.getFailedStepText() != null && !failureInfo.getFailedStepText().isEmpty()) {
                return failureInfo.getFailedStepText();
            }
        } catch (Exception e) {
            LOG.warn("Failed to extract step text using strategies", e);
        }
        
        // Fallback to basic extraction
        if (test.getName().contains("Step:")) {
            return test.getName().split("Step:")[1].trim();
        }
        
        return "Failed step text not available";
    }

    /**
     * Creates a minimal failure info when no strategy can handle the test output.
     * 
     * @param testOutput The original test output
     * @return A minimal FailureInfo with basic information
     */
    private FailureInfo createMinimalFailureInfo(String testOutput) {
        // Use GenericErrorStrategy as the true fallback
        GenericErrorStrategy genericStrategy = new GenericErrorStrategy(project);
        try {
            return genericStrategy.parse(testOutput);
        } catch (Exception e) {
            LOG.warn("GenericErrorStrategy failed, creating basic fallback", e);
            String errorMessage = extractBasicErrorMessage(testOutput);
            
            return new FailureInfo.Builder()
                    .withErrorMessage(errorMessage)
                    .withStackTrace(testOutput)
                    .withParsingStrategy("GenericErrorStrategy")
                    .withParsingTime(System.currentTimeMillis())
                    .build();
        }
    }

    /**
     * Extracts a basic error message from test output as fallback.
     * 
     * @param testOutput The test output
     * @return Basic error message
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
        return "Unknown error occurred";
    }

    /**
     * Gets the number of available parsing strategies.
     * 
     * @return The number of strategies
     */
    public int getStrategyCount() {
        return strategies.size();
    }

    /**
     * Gets the list of available strategies for debugging purposes.
     * 
     * @return List of strategy names
     */
    public List<String> getStrategyNames() {
        List<String> names = new ArrayList<>();
        for (FailureParsingStrategy strategy : strategies) {
            names.add(strategy.getStrategyName() + " (Priority: " + strategy.getPriority() + ")");
        }
        return names;
    }
} 