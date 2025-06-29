package com.triagemate.extractors;

import com.intellij.execution.testframework.sm.runner.SMTestProxy;

/**
 * Extracts stack trace and failed step text from test failure information.
 */
public class StackTraceExtractor {

    /**
     * Extracts the stack trace from a failed test
     *
     * @param test The failed test proxy
     * @return The stack trace as a string
     */
    public String extractStackTrace(SMTestProxy test) {
        // Placeholder implementation
        // In the actual implementation, we'll extract the stack trace from the test failure
        if (test.getErrorMessage() != null) {
            return test.getErrorMessage();
        }
        return "Stack trace not available";
    }

    /**
     * Extracts the text of the failed step from the test information
     *
     * @param test The failed test proxy
     * @return The text of the failed step
     */
    public String extractFailedStepText(SMTestProxy test) {
        // Placeholder implementation
        // In the actual implementation, we'll parse the error message or test output
        // to identify the specific Cucumber step that failed
        if (test.getName().contains("Step:")) {
            return test.getName().split("Step:")[1].trim();
        }
        return "Failed step text not available";
    }
} 