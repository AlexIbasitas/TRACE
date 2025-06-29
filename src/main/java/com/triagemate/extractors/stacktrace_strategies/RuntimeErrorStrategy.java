package com.triagemate.extractors.stacktrace_strategies;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.diagnostic.Logger;
import com.triagemate.models.FailureInfo;
import com.triagemate.extractors.FailureParsingStrategy;

/**
 * Parsing strategy for extracting failure information from runtime exceptions.
 * <p>
 * This strategy handles general runtime exceptions such as:
 * - NullPointerException
 * - IllegalArgumentException
 * - IllegalStateException
 * - ArrayIndexOutOfBoundsException
 * - ClassCastException
 * - NumberFormatException
 * </p>
 *
 * <p>Example runtime output handled:</p>
 * <pre>
 * java.lang.NullPointerException: Cannot invoke "String.length()" because "str" is null
 *     at com.example.MyTest.testSomething(MyTest.java:42)
 * </pre>
 */
public class RuntimeErrorStrategy implements FailureParsingStrategy {
    private static final Logger LOG = Logger.getInstance(RuntimeErrorStrategy.class);
    private final Project project;

    /**
     * Constructor for RuntimeErrorStrategy.
     * 
     * @param project The IntelliJ project context for PSI operations
     */
    public RuntimeErrorStrategy(Project project) {
        this.project = project;
    }

    @Override
    public boolean canHandle(String testOutput) {
        // TODO: Implement runtime error detection
        return false;
    }

    @Override
    public FailureInfo parse(String testOutput) {
        // TODO: Implement runtime error parsing
        return null;
    }

    @Override
    public int getPriority() {
        return 80; // Medium priority for runtime error parsing
    }

    @Override
    public String getStrategyName() {
        return "RuntimeErrorStrategy";
    }
} 