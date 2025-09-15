package com.trace.test.listeners;

import com.intellij.execution.testframework.sm.runner.SMTestProxy;
import com.intellij.openapi.diagnostic.Logger;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Utility helper class for test-related operations.
 * Contains stateless utility methods extracted from CucumberTestExecutionListener.
 * 
 * <p>This class provides common test utilities that can be used across different
 * test listeners and components without creating dependencies on specific listener
 * implementations.</p>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
public class TestUtilityHelper {
    
    private static final Logger LOG = Logger.getInstance(TestUtilityHelper.class);
    
    // Stream capture for test output analysis (instance-based to allow proper cleanup)
    private final ConcurrentMap<SMTestProxy, ByteArrayOutputStream> testOutputStreams = new ConcurrentHashMap<>();
    private final ConcurrentMap<SMTestProxy, ByteArrayOutputStream> testErrorStreams = new ConcurrentHashMap<>();
    private static PrintStream originalOut;
    private static PrintStream originalErr;
    
    // Singleton instance for backward compatibility
    private static volatile TestUtilityHelper instance;
    
    /**
     * Private constructor for singleton pattern.
     */
    private TestUtilityHelper() {
        instance = this;
    }
    
    /**
     * Gets the singleton instance.
     * 
     * @return the singleton instance
     */
    public static TestUtilityHelper getInstance() {
        if (instance == null) {
            synchronized (TestUtilityHelper.class) {
                if (instance == null) {
                    instance = new TestUtilityHelper();
                }
            }
        }
        return instance;
    }

    /**
     * Determines if a test is a Cucumber test using multiple detection methods.
     *
     * @param test The test proxy to check
     * @return true if the test is a Cucumber test, false otherwise
     */
    public static boolean isCucumberTest(SMTestProxy test) {
        if (test == null) {
            return false;
        }
        
        // Method 1: Check test location (most reliable)
        String testLocation = test.getLocationUrl();
        if (testLocation != null) {
            // Check if test is in a feature file
            if (testLocation.contains(".feature") || testLocation.contains("features/")) {
                LOG.debug("Detected Cucumber test via feature file location");
                return true;
            }
            
            // Check if test is in a Cucumber runner class
            if (testLocation.contains("Cucumber") || testLocation.contains("cucumber")) {
                LOG.debug("Detected Cucumber test via runner class location");
                return true;
            }
        }
        
        // Method 2: Check test hierarchy for Cucumber indicators
        SMTestProxy parent = test.getParent();
        while (parent != null) {
            String parentName = parent.getName();
            if (parentName != null && (
                parentName.contains("Cucumber") ||
                parentName.contains("Feature:") ||
                parentName.contains("Scenario:") ||
                parentName.contains("cucumber.runtime"))) {
                LOG.debug("Detected Cucumber test via parent hierarchy: " + parentName);
                return true;
            }
            parent = parent.getParent();
        }
        
        // Method 3: Check error message for Cucumber-specific exceptions (fallback)
        String errorMessage = test.getErrorMessage();
        if (errorMessage != null && (
            errorMessage.contains("io.cucumber") ||
            errorMessage.contains("cucumber.runtime") ||
            errorMessage.contains("UndefinedStepException") ||
            errorMessage.contains("AmbiguousStepDefinitionsException") ||
            errorMessage.contains("PendingException"))) {
            LOG.debug("Detected Cucumber test via error message");
            return true;
        }
        
        return false;
    }

    /**
     * Formats the scenario name to include both scenario outline title and example identifier.
     * For scenario outlines, it formats as "Scenario Title (Example #1.1)".
     * For regular scenarios, it returns the scenario name as is.
     * 
     * @param gherkinScenarioName The scenario name from GherkinScenarioInfo
     * @param basicScenarioName The scenario name from StackTraceExtractor (might contain example identifier)
     * @param isScenarioOutline Whether this is a scenario outline
     * @return The formatted scenario name
     */
    public static String formatScenarioName(String gherkinScenarioName, String basicScenarioName, boolean isScenarioOutline) {
        if (gherkinScenarioName == null || gherkinScenarioName.isEmpty()) {
            return basicScenarioName != null ? basicScenarioName : "Unknown Scenario";
        }
        
        // Check if this is a scenario outline (basic scenario name contains "Example #")
        if (isScenarioOutline && basicScenarioName != null && basicScenarioName.matches("Example #\\d+\\.\\d+")) {
            return gherkinScenarioName + " (" + basicScenarioName + ")";
        }
        
        // Regular scenario, return Gherkin scenario name as is
        return gherkinScenarioName;
    }

    /**
     * Sets up output stream capture for a specific test.
     * Redirects System.out and System.err to capture test output.
     * 
     * @param test The test to capture output for
     */
    public void setupTestOutputCapture(SMTestProxy test) {
        if (test == null) return;
        
        try {
            // Create capture streams for this test
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
            
            testOutputStreams.put(test, outputStream);
            testErrorStreams.put(test, errorStream);
            
            // Store original streams if not already stored
            if (originalOut == null) {
                originalOut = System.out;
                originalErr = System.err;
            }
            
            // Create custom print streams that capture output
            PrintStream capturedOut = new PrintStream(outputStream, true) {
                @Override
                public void write(byte[] buf, int off, int len) {
                    super.write(buf, off, len);
                    originalOut.write(buf, off, len); // Also write to original
                }
            };
            
            PrintStream capturedErr = new PrintStream(errorStream, true) {
                @Override
                public void write(byte[] buf, int off, int len) {
                    super.write(buf, off, len);
                    originalErr.write(buf, off, len); // Also write to original
                }
            };
            
            // Redirect system streams
            System.setOut(capturedOut);
            System.setErr(capturedErr);
            
            if (LOG.isDebugEnabled()) {
                LOG.debug("Output capture started for test: " + test.getName());
            }
            
        } catch (Exception e) {
            LOG.error("Error setting up output capture: " + e.getMessage(), e);
        }
    }

    /**
     * Captures and stores the output streams for a test.
     * Restores original streams and cleans up capture resources.
     * 
     * @param test The test to capture streams for
     */
    public void captureTestStreams(SMTestProxy test) {
        if (test == null) return;
        
        try {
            ByteArrayOutputStream outputStream = testOutputStreams.get(test);
            ByteArrayOutputStream errorStream = testErrorStreams.get(test);
            
            if (outputStream != null) {
                String capturedOutput = outputStream.toString();
                if (!capturedOutput.trim().isEmpty()) {
                    TestOutputCaptureListener.captureTestOutputStatic(test, "STDOUT:\n" + capturedOutput);
                }
            }
            
            if (errorStream != null) {
                String capturedError = errorStream.toString();
                if (!capturedError.trim().isEmpty()) {
                    TestOutputCaptureListener.captureTestOutputStatic(test, "STDERR:\n" + capturedError);
                }
            }
            
            // Restore original streams
            if (originalOut != null) {
                System.setOut(originalOut);
            }
            if (originalErr != null) {
                System.setErr(originalErr);
            }
            
            // Clean up
            testOutputStreams.remove(test);
            testErrorStreams.remove(test);
            
        } catch (Exception e) {
            LOG.error("Error capturing test streams: " + e.getMessage(), e);
        }
    }
    
    /**
     * Cleans up instance resources to prevent memory leaks and ensure consistent startup behavior.
     * 
     * <p>This method should be called during plugin shutdown or when resources need to be reset.
     * It clears the instance test output and error stream maps to prevent memory leaks.</p>
     */
    public void cleanup() {
        LOG.info("Starting cleanup of TestUtilityHelper instance resources");
        
        int outputStreamsCleaned = 0;
        int errorStreamsCleaned = 0;
        
        try {
            // Close and clear test output streams
            for (ByteArrayOutputStream stream : testOutputStreams.values()) {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (Exception e) {
                        LOG.warn("Error closing test output stream: " + e.getMessage());
                    }
                }
                outputStreamsCleaned++;
            }
            testOutputStreams.clear();
            
            // Close and clear test error streams
            for (ByteArrayOutputStream stream : testErrorStreams.values()) {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (Exception e) {
                        LOG.warn("Error closing test error stream: " + e.getMessage());
                    }
                }
                errorStreamsCleaned++;
            }
            testErrorStreams.clear();
            
            // Clear static PrintStream references to prevent memory leaks
            originalOut = null;
            originalErr = null;
            
            LOG.info("TestUtilityHelper cleanup completed - cleared " + outputStreamsCleaned + 
                    " output streams and " + errorStreamsCleaned + " error streams");
                    
        } catch (Exception e) {
            LOG.error("Error during TestUtilityHelper cleanup: " + e.getMessage(), e);
        }
    }
    
    // Static wrapper methods for backward compatibility
    public static void setupTestOutputCaptureStatic(SMTestProxy test) {
        getInstance().setupTestOutputCapture(test);
    }
    
    public static void captureTestStreamsStatic(SMTestProxy test) {
        getInstance().captureTestStreams(test);
    }
    
    /**
     * Static wrapper for cleanup.
     */
    public static void cleanupStatic() {
        if (instance != null) {
            instance.cleanup();
            instance = null;
        }
    }
}
