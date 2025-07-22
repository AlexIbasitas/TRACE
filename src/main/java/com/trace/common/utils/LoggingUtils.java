package com.trace.common.utils;

import com.intellij.openapi.diagnostic.Logger;

/**
 * Utility class for consistent logging across the TRACE plugin.
 * Uses IntelliJ's built-in logging framework.
 */
public final class LoggingUtils {
    
    private LoggingUtils() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Creates a logger for the specified class.
     * 
     * @param clazz the class to create a logger for
     * @return a Logger instance
     */
    public static Logger getLogger(Class<?> clazz) {
        return Logger.getInstance(clazz);
    }
    
    /**
     * Creates a logger with the specified name.
     * 
     * @param name the logger name
     * @return a Logger instance
     */
    public static Logger getLogger(String name) {
        return Logger.getInstance(name);
    }
} 