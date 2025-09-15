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
 * @author Alex Ibasitas
 * @since 1.0.0
 */
public class TestOutputCaptureListener {
    
    private static final Logger LOG = Logger.getInstance(TestOutputCaptureListener.class);
    
    // Thread-safe storage for test output by test proxy (instance-based to allow proper cleanup)
    private final ConcurrentMap<SMTestProxy, StringBuilder> testOutputMap = new ConcurrentHashMap<>();
    
    // Singleton instance for backward compatibility
    private static volatile TestOutputCaptureListener instance;
    
    /**
     * Private constructor for singleton pattern.
     */
    private TestOutputCaptureListener() {
        instance = this;
    }
    
    /**
     * Gets the singleton instance.
     * 
     * @return the singleton instance
     */
    public static TestOutputCaptureListener getInstance() {
        if (instance == null) {
            synchronized (TestOutputCaptureListener.class) {
                if (instance == null) {
                    instance = new TestOutputCaptureListener();
                }
            }
        }
        return instance;
    }
    
    /**
     * Captures a single line of test output for a specific test proxy.
     * 
     * <p>This method handles null inputs gracefully and ensures proper line endings.
     * It's designed to be called frequently during test execution without performance impact.</p>
     * 
     * @param testProxy The test proxy to capture output for
     * @param outputLine The output line to capture (null or empty lines are ignored)
     */
    public void captureTestOutput(SMTestProxy testProxy, String outputLine) {
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
    public void captureTestErrorOutput(SMTestProxy testProxy) {
        try {
            if (testProxy == null) {
                return;
            }
            
            StringBuilder output = testOutputMap.computeIfAbsent(testProxy, k -> new StringBuilder());
            
            // Capture error message (includes both error message and stack trace)
            String errorMessage = testProxy.getErrorMessage();
            if (errorMessage != null && !errorMessage.trim().isEmpty()) {
                output.append("ERROR OUTPUT:\n");
                output.append(errorMessage);
                if (!errorMessage.endsWith("\n")) {
                    output.append("\n");
                }
                output.append("\n");
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Captured error output for test: " + testProxy.getName());
                }
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
    public void captureComprehensiveTestOutput(SMTestProxy testProxy) {
        if (testProxy == null) {
            return;
        }
        
        try {
            StringBuilder comprehensiveOutput = new StringBuilder();
            
            // Capture the error message from the test proxy
            String errorMessage = testProxy.getErrorMessage();
            if (errorMessage != null && !errorMessage.trim().isEmpty()) {
                comprehensiveOutput.append("ERROR:\n");
                comprehensiveOutput.append(errorMessage).append("\n");
            }
            
            // Capture any existing output we've already collected
            String existingOutput = getCapturedOutput(testProxy);
            if (existingOutput != null && !existingOutput.trim().isEmpty()) {
                comprehensiveOutput.append("OUTPUT:\n");
                comprehensiveOutput.append(existingOutput).append("\n");
            }
            
            // Try to capture additional error details from children
            if (testProxy.getChildren() != null && !testProxy.getChildren().isEmpty()) {
                comprehensiveOutput.append("CHILD TESTS:\n");
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
            comprehensiveOutput.append("TEST INFO:\n");
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
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Captured comprehensive output for test: " + testProxy.getName());
                }
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
    public String getCapturedOutput(SMTestProxy testProxy) {
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
    public String getCapturedOutputWithChildren(SMTestProxy testProxy) {
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
    public void clearCapturedOutput(SMTestProxy testProxy) {
        if (testProxy != null) {
            testOutputMap.remove(testProxy);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Cleared captured output for test: " + testProxy.getName());
            }
        }
    }
    
    /**
     * Clears all captured test output.
     * 
     * <p>This method should be called when test execution completes to free
     * all memory used for output storage. It's essential for preventing
     * memory leaks in long-running test sessions.</p>
     */
    public void clearAllCapturedOutput() {
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
    public int getCapturedOutputCount() {
        return testOutputMap.size();
    }
    
    /**
     * Cleans up static resources to prevent memory leaks and ensure consistent startup behavior.
     * 
     * <p>This method should be called during plugin shutdown or when resources need to be reset.
     * It clears the static test output map to prevent memory leaks.</p>
     */
    public void cleanup() {
        LOG.info("Starting cleanup of TestOutputCaptureListener instance resources");
        
        int resourcesCleaned = testOutputMap.size();
        
        try {
            testOutputMap.clear();
            LOG.info("TestOutputCaptureListener cleanup completed - cleared " + resourcesCleaned + " test output entries");
        } catch (Exception e) {
            LOG.error("Error during TestOutputCaptureListener cleanup: " + e.getMessage(), e);
        }
    }
    
    // Static wrapper methods for backward compatibility
    public static void captureTestOutputStatic(SMTestProxy testProxy, String outputLine) {
        getInstance().captureTestOutput(testProxy, outputLine);
    }
    
    public static void captureTestErrorOutputStatic(SMTestProxy testProxy) {
        getInstance().captureTestErrorOutput(testProxy);
    }
    
    public static void captureComprehensiveTestOutputStatic(SMTestProxy testProxy) {
        getInstance().captureComprehensiveTestOutput(testProxy);
    }
    
    public static String getCapturedOutputStatic(SMTestProxy testProxy) {
        return getInstance().getCapturedOutput(testProxy);
    }
    
    public static String getCapturedOutputWithChildrenStatic(SMTestProxy testProxy) {
        return getInstance().getCapturedOutputWithChildren(testProxy);
    }
    
    public static void clearCapturedOutputStatic(SMTestProxy testProxy) {
        getInstance().clearCapturedOutput(testProxy);
    }
    
    public static void clearAllCapturedOutputStatic() {
        getInstance().clearAllCapturedOutput();
    }
    
    public static int getCapturedOutputCountStatic() {
        return getInstance().getCapturedOutputCount();
    }
    
    public static void cleanupStatic() {
        if (instance != null) {
            instance.cleanup();
            instance = null;
        }
    }
} 