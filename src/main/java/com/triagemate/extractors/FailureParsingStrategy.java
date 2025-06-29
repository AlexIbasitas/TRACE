package com.triagemate.extractors;

import com.triagemate.models.FailureInfo;

/**
 * Interface for different parsing strategies to handle various test failure output formats.
 * 
 * <p>This interface defines the contract for parsing strategies that can extract failure information
 * from different types of test outputs (e.g., JUnit assertions, generic assertions, etc.).
 * Each strategy should be able to determine if it can handle a specific output format and
 * parse it into a standardized FailureInfo object.</p>
 * 
 * <p>The strategy pattern allows for easy extension and maintenance of parsing logic
 * without modifying existing code. New assertion libraries or output formats can be
 * supported by implementing this interface.</p>
 * 
 * @see com.triagemate.models.FailureInfo
 * @see com.triagemate.extractors.StackTraceExtractor
 */
public interface FailureParsingStrategy {
    
    /**
     * Determines if this strategy can handle the given test output.
     * 
     * <p>This method should perform a quick check to determine if the test output
     * matches the expected format for this strategy. The check should be fast and
     * not perform full parsing.</p>
     * 
     * @param testOutput the test output to check (may be null or empty)
     * @return true if this strategy can parse the output, false otherwise
     */
    boolean canHandle(String testOutput);
    
    /**
     * Parses the test output and extracts failure information.
     * 
     * <p>This method should perform the actual parsing of the test output and extract
     * all available failure information. The method should handle edge cases gracefully
     * and return a FailureInfo object even if some information cannot be extracted.</p>
     * 
     * <p>If the test output cannot be parsed at all, this method should throw a
     * RuntimeException with a descriptive message.</p>
     * 
     * @param testOutput the test output to parse (must not be null)
     * @return a FailureInfo object containing the extracted failure information
     * @throws RuntimeException if the test output cannot be parsed by this strategy
     * @throws IllegalArgumentException if testOutput is null
     */
    FailureInfo parse(String testOutput);
    
    /**
     * Gets the priority of this strategy for automatic selection.
     * 
     * <p>Higher priority strategies are tried first when multiple strategies
     * can handle the same test output. This allows for more specific strategies
     * to take precedence over generic ones.</p>
     * 
     * <p>Recommended priority values:
     * <ul>
     *   <li>100: Specific assertion library strategies (e.g., JUnit)</li>
     *   <li>50: Framework-specific strategies (e.g., TestNG)</li>
     *   <li>10: Generic fallback strategies</li>
     * </ul></p>
     * 
     * @return the priority value (higher = more specific)
     */
    int getPriority();
    
    /**
     * Gets the name of this strategy for logging and debugging purposes.
     * 
     * <p>This name should be descriptive and unique among all strategies.
     * It will be used in log messages and error reporting to help identify
     * which strategy was used or failed.</p>
     * 
     * @return the strategy name (should not be null or empty)
     */
    String getStrategyName();
} 