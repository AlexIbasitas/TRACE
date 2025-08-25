package com.trace.common.utils;

import com.intellij.openapi.diagnostic.Logger;

/**
 * Utility class for consistent logging across the TRACE plugin.
 * 
 * <p>This class provides centralized logging utilities that ensure consistent
 * logger creation and configuration throughout the TRACE plugin. It uses
 * IntelliJ's built-in logging framework for optimal IDE integration.</p>
 * 
 * <p>All logging in the TRACE plugin should use this utility class to ensure
 * consistent logger naming and configuration across all components.</p>
 * 
 * @author Alex Ibasitas
 * @version 1.0
 * @since 1.0
 */
public final class LoggingUtils {
    
    /**
     * Private constructor to prevent instantiation.
     * This is a utility class and should not be instantiated.
     */
    private LoggingUtils() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Creates a logger for the specified class.
     * 
     * <p>This method creates a logger instance using the class name as the logger name.
     * This is the preferred method for creating loggers within classes as it provides
     * automatic logger naming based on the class.</p>
     * 
     * @param clazz the class to create a logger for
     * @return a Logger instance configured for the specified class
     * @throws IllegalArgumentException if clazz is null
     */
    public static Logger getLogger(Class<?> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("Class cannot be null");
        }
        return Logger.getInstance(clazz);
    }
    
    /**
     * Creates a logger with the specified name.
     * 
     * <p>This method creates a logger instance using the provided name. This is useful
     * for creating loggers with custom names or for components that don't have a
     * specific class context.</p>
     * 
     * @param name the logger name
     * @return a Logger instance with the specified name
     * @throws IllegalArgumentException if name is null or empty
     */
    public static Logger getLogger(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Logger name cannot be null or empty");
        }
        return Logger.getInstance(name);
    }
} 