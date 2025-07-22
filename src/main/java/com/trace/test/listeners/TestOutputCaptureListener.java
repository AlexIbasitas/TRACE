package com.trace.test.listeners;

import com.intellij.execution.testframework.sm.runner.SMTestProxy;
import com.intellij.openapi.diagnostic.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Captures and manages test output during test execution for failure analysis.
 * 
 * <p>This utility class provides thread-safe storage and retrieval of test output
 * including error messages, stack traces, and console output. It works in conjunction
 * with the CucumberTestExecutionListener to provide comprehensive failure context
 * for the TriagePanel analysis.</p>
 * 
 * <p>The class maintains a concurrent map of test output by test proxy, allowing
 * efficient storage and retrieval of output data across multiple test executions.</p>
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Thread-safe output capture using ConcurrentHashMap</li>
 *   <li>Comprehensive error output collection</li>
 *   <li>Hierarchical output retrieval (including child tests)</li>
 *   <li>Memory management through output clearing</li>
 * </ul>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
public class TestOutputCaptureListener {
    
    private static final Logger LOG = Logger.getInstance(TestOutputCaptureListener.class);
    
    // Thread-safe storage for test output by test proxy
    private static final ConcurrentMap<SMTestProxy, StringBuilder> testOutputMap = new ConcurrentHashMap<>();
    
    /**
     * Captures a single line of test output for a specific test proxy.
     * 
     * <p>This method handles null inputs gracefully and ensures proper line endings.
     * It's designed to be called frequently during test execution without performance impact.</p>
     * 
     * @param testProxy The test proxy to capture output for
     * @param outputLine The output line to capture (null or empty lines are ignored)
     */
    public static void captureTestOutput(SMTestProxy testProxy, String outputLine) {
        try {
            if (testProxy == null) {
                LOG.debug("Received test output line with null test proxy");
                return;
            }
            
            if (outputLine == null || outputLine.trim().isEmpty()) {
                return;
            }
            
            // Get or create StringBuilder for this test
            StringBuilder output = testOutputMap.computeIfAbsent(testProxy, k -> new StringBuilder());
            
            // Append the output line with proper line ending
            output.append(outputLine);
            if (!outputLine.endsWith("\n")) {
                output.append("\n");
            }
            
        } catch (Exception e) {
            LOG.warn("Error capturing test output line", e);
        }
    }
    
    /**
     * Captures error output from the test's error message and stack trace.
     * 
     * <p>This method extracts the complete error information from the test proxy
     * and formats it for consistent analysis. It's typically called when a test fails.</p>
     * 
     * @param testProxy The test proxy to capture error output for
     */
    public static void captureTestErrorOutput(SMTestProxy testProxy) {
        try {
            if (testProxy == null) {
                return;
            }
            
            StringBuilder output = testOutputMap.computeIfAbsent(testProxy, k -> new StringBuilder());
            
            // Capture error message (includes both error message and stack trace)
            String errorMessage = testProxy.getErrorMessage();
            if (errorMessage != null && !errorMessage.trim().isEmpty()) {
                output.append("=== FULL ERROR OUTPUT ===\n");
                output.append(errorMessage);
                if (!errorMessage.endsWith("\n")) {
                    output.append("\n");
                }
                output.append("=== END ERROR OUTPUT ===\n\n");
                LOG.debug("Captured error output for test: " + testProxy.getName());
            }
            
        } catch (Exception e) {
            LOG.warn("Error capturing test error output", e);
        }
    }
    
    /**
     * Captures comprehensive test output including error streams, system output, and metadata.
     * 
     * <p>This method attempts to capture all available output from a test including
     * error messages, existing captured output, child test output, and test metadata.
     * It provides the most complete picture of test execution for failure analysis.</p>
     *
     * @param testProxy The test proxy to capture comprehensive output for
     */
    public static void captureComprehensiveTestOutput(SMTestProxy testProxy) {
        if (testProxy == null) {
            return;
        }
        
        try {
            StringBuilder comprehensiveOutput = new StringBuilder();
            
            // Capture the error message from the test proxy
            String errorMessage = testProxy.getErrorMessage();
            if (errorMessage != null && !errorMessage.trim().isEmpty()) {
                comprehensiveOutput.append("=== ERROR MESSAGE ===\n");
                comprehensiveOutput.append(errorMessage).append("\n");
            }
            
            // Capture any existing output we've already collected
            String existingOutput = getCapturedOutput(testProxy);
            if (existingOutput != null && !existingOutput.trim().isEmpty()) {
                comprehensiveOutput.append("=== CAPTURED OUTPUT ===\n");
                comprehensiveOutput.append(existingOutput).append("\n");
            }
            
            // Try to capture additional error details from children
            if (testProxy.getChildren() != null && !testProxy.getChildren().isEmpty()) {
                comprehensiveOutput.append("=== CHILD TEST OUTPUT ===\n");
                for (SMTestProxy child : testProxy.getChildren()) {
                    String childError = child.getErrorMessage();
                    if (childError != null && !childError.trim().isEmpty()) {
                        comprehensiveOutput.append("Child: ").append(child.getName()).append("\n");
                        comprehensiveOutput.append(childError).append("\n");
                    }
                    
                    // Also capture any output from children
                    String childOutput = getCapturedOutput(child);
                    if (childOutput != null && !childOutput.trim().isEmpty()) {
                        comprehensiveOutput.append("Child Output: ").append(child.getName()).append("\n");
                        comprehensiveOutput.append(childOutput).append("\n");
                    }
                }
            }
            
            // Capture test metadata
            comprehensiveOutput.append("=== TEST METADATA ===\n");
            comprehensiveOutput.append("Test Name: ").append(testProxy.getName()).append("\n");
            
            String locationUrl = testProxy.getLocationUrl();
            if (locationUrl != null) {
                comprehensiveOutput.append("Location: ").append(locationUrl).append("\n");
            }
            
            comprehensiveOutput.append("Status: ").append(testProxy.getMagnitudeInfo()).append("\n");
            
            SMTestProxy parent = testProxy.getParent();
            if (parent != null) {
                comprehensiveOutput.append("Parent: ").append(parent.getName()).append("\n");
            }
            
            // Store the comprehensive output
            if (comprehensiveOutput.length() > 0) {
                testOutputMap.put(testProxy, new StringBuilder(comprehensiveOutput.toString()));
                LOG.debug("Captured comprehensive output for test: " + testProxy.getName());
            }
            
        } catch (Exception e) {
            LOG.error("Error capturing comprehensive test output", e);
        }
    }
    
    /**
     * Retrieves the captured output for a specific test proxy.
     * 
     * @param testProxy The test proxy to get output for
     * @return The captured output as a string, or null if not found
     */
    public static String getCapturedOutput(SMTestProxy testProxy) {
        if (testProxy == null) {
            return null;
        }
        
        StringBuilder output = testOutputMap.get(testProxy);
        if (output != null) {
            return output.toString().trim();
        }
        
        return null;
    }
    
    /**
     * Retrieves captured output for a test proxy including output from all its children.
     * 
     * <p>This method is useful for test suites or parameterized tests where the main
     * test contains multiple child tests. It recursively collects output from the
     * entire test hierarchy.</p>
     * 
     * @param testProxy The test proxy to get output for
     * @return The captured output as a string including children, or null if not found
     */
    public static String getCapturedOutputWithChildren(SMTestProxy testProxy) {
        if (testProxy == null) {
            return null;
        }
        
        StringBuilder combinedOutput = new StringBuilder();
        
        // Get output for this test
        String mainOutput = getCapturedOutput(testProxy);
        if (mainOutput != null && !mainOutput.isEmpty()) {
            combinedOutput.append(mainOutput).append("\n");
        }
        
        // Get output from all children
        for (SMTestProxy child : testProxy.getChildren()) {
            String childOutput = getCapturedOutputWithChildren(child);
            if (childOutput != null && !childOutput.isEmpty()) {
                combinedOutput.append("--- Child Test Output ---\n");
                combinedOutput.append(childOutput).append("\n");
            }
        }
        
        String result = combinedOutput.toString().trim();
        return result.isEmpty() ? null : result;
    }
    
    /**
     * Clears captured output for a specific test proxy.
     * 
     * <p>This method should be called when a test completes to free memory.
     * It's particularly important for long-running test suites to prevent
     * memory accumulation.</p>
     * 
     * @param testProxy The test proxy to clear output for
     */
    public static void clearCapturedOutput(SMTestProxy testProxy) {
        if (testProxy != null) {
            testOutputMap.remove(testProxy);
            LOG.debug("Cleared captured output for test: " + testProxy.getName());
        }
    }
    
    /**
     * Clears all captured test output.
     * 
     * <p>This method should be called when test execution completes to free
     * all memory used for output storage. It's essential for preventing
     * memory leaks in long-running test sessions.</p>
     */
    public static void clearAllCapturedOutput() {
        int size = testOutputMap.size();
        testOutputMap.clear();
        LOG.info("Cleared all captured test output (" + size + " entries)");
    }
    
    /**
     * Gets the number of tests with captured output.
     * 
     * <p>This method is useful for debugging and monitoring output capture
     * performance. It can help identify if output is being properly cleared
     * or if there are memory leaks.</p>
     * 
     * @return The number of tests with captured output
     */
    public static int getCapturedOutputCount() {
        return testOutputMap.size();
    }
} 