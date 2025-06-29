package com.triagemate.extractors.stacktrace_strategies;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.diagnostic.Logger;
import com.triagemate.models.FailureInfo;
import com.triagemate.extractors.FailureParsingStrategy;

/**
 * Generic parsing strategy for extracting failure information from any unhandled error types.
 * <p>
 * This strategy serves as a fallback for any error types that aren't handled by more specific strategies.
 * It provides basic parsing capabilities for any exception with a stack trace.
 * </p>
 *
 * <p>Example generic output handled:</p>
 * <pre>
 * java.lang.Exception: Some unexpected error occurred
 *     at com.example.MyTest.testSomething(MyTest.java:42)
 * </pre>
 */
public class GenericErrorStrategy implements FailureParsingStrategy {
    private static final Logger LOG = Logger.getInstance(GenericErrorStrategy.class);
    private final Project project;

    /**
     * Constructor for GenericErrorStrategy.
     * 
     * @param project The IntelliJ project context for PSI operations
     */
    public GenericErrorStrategy(Project project) {
        this.project = project;
    }

    @Override
    public boolean canHandle(String testOutput) {
        // TODO: Implement generic error detection (should always return true as fallback)
        return false;
    }

    @Override
    public FailureInfo parse(String testOutput) {
        // TODO: Implement generic error parsing
        return null;
    }

    @Override
    public int getPriority() {
        return 10; // Lowest priority as fallback strategy
    }

    @Override
    public String getStrategyName() {
        return "GenericErrorStrategy";
    }
} 