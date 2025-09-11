package com.trace.chat.ui;

import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsListener;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.trace.test.listeners.CucumberTestExecutionListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for creating and managing TriagePanel tool window instances.
 * 
 * <p>This factory is responsible for:</p>
 * <ul>
 *   <li>Creating TriagePanel tool window content for each project</li>
 *   <li>Managing panel instances across multiple projects</li>
 *   <li>Registering test execution listeners for each project</li>
 *   <li>Providing access to panel instances for external components</li>
 * </ul>
 * 
 * <p>The factory maintains a thread-safe registry of panel instances using
 * {@link ConcurrentHashMap} to ensure proper isolation between projects.</p>
 * 
 * @author Alex Ibasitas
 * @version 1.0
 * @since 1.0
 * @see TriagePanelView
 * @see CucumberTestExecutionListener
 */
public class TriagePanelToolWindowFactory implements ToolWindowFactory, Disposable {
    
    private static final Logger LOG = Logger.getInstance(TriagePanelToolWindowFactory.class);
    
    /**
     * Thread-safe registry of panel instances by project.
     * Ensures proper isolation between different project instances.
     */
    private static final ConcurrentHashMap<Project, TriagePanelView> panelInstances = new ConcurrentHashMap<>();
    
    /**
     * Thread-safe registry of MessageBusConnection instances by project.
     * Ensures proper cleanup of message bus connections.
     */
    private static final ConcurrentHashMap<Project, com.intellij.util.messages.MessageBusConnection> messageBusConnections = new ConcurrentHashMap<>();

    /**
     * Creates the tool window content for the specified project.
     * 
     * <p>This method:</p>
     * <ul>
     *   <li>Creates a new TriagePanelView instance for the project</li>
     *   <li>Registers the panel instance in the global registry</li>
     *   <li>Sets up test execution listener for the project</li>
     *   <li>Creates and adds the tool window content</li>
     * </ul>
     * 
     * @param project    The current project context
     * @param toolWindow The tool window to create content for
     * @throws IllegalArgumentException if project or toolWindow is null
     */
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        if (project == null) {
            throw new IllegalArgumentException("Project cannot be null");
        }
        if (toolWindow == null) {
            throw new IllegalArgumentException("ToolWindow cannot be null");
        }
        
        LOG.info("Creating tool window content for project: " + project.getName());
        
        try {
            TriagePanelView triagePanelView = new TriagePanelView(project);
            
            // Store the panel instance for this project
            panelInstances.put(project, triagePanelView);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Stored TriagePanelView for project: " + project.getName() + 
                         " (Total instances: " + panelInstances.size() + ")");
            }
            
            // Register the test execution listener for this project
            registerTestExecutionListener(project);
            
            // Create and add the tool window content
            Content content = ContentFactory.getInstance().createContent(triagePanelView.getContent(), "", false);
            toolWindow.getContentManager().addContent(content);
            
            LOG.info("Tool window content created successfully for project: " + project.getName());
            
        } catch (Exception e) {
            LOG.error("Failed to create tool window content for project: " + project.getName(), e);
            throw new RuntimeException("Failed to create TriagePanel tool window content", e);
        }
    }
    
    /**
     * Registers the test execution listener for the given project.
     * 
     * <p>This method creates and registers a {@link CucumberTestExecutionListener}
     * instance that will receive test execution events for the specified project.
     * The listener is connected to the project's message bus to receive
     * {@link SMTRunnerEventsListener.TEST_STATUS} events.</p>
     * 
     * @param project The project to register the listener for
     * @throws IllegalArgumentException if project is null
     * @throws RuntimeException if listener registration fails
     */
    private void registerTestExecutionListener(@NotNull Project project) {
        if (project == null) {
            throw new IllegalArgumentException("Project cannot be null");
        }
        
        LOG.debug("Registering test execution listener for project: " + project.getName());
        
        try {
            // Create a listener instance for this project
            CucumberTestExecutionListener listener = new CucumberTestExecutionListener(project);
            
            // Create and store the message bus connection for proper cleanup
            com.intellij.util.messages.MessageBusConnection connection = project.getMessageBus().connect();
            messageBusConnections.put(project, connection);
            
            // Register with the test framework via project message bus
            connection.subscribe(
                SMTRunnerEventsListener.TEST_STATUS, 
                listener
            );
            
            LOG.info("Test execution listener registered successfully for project: " + project.getName());
            
        } catch (Exception e) {
            LOG.error("Failed to register test execution listener for project: " + project.getName(), e);
            throw new RuntimeException("Failed to register test execution listener", e);
        }
    }

    /**
     * Gets the TriagePanel instance for the given project.
     * 
     * <p>This method provides access to the panel instance that was created
     * for the specified project. Returns null if no panel instance exists
     * for the project.</p>
     * 
     * @param project The project to get the panel for
     * @return The TriagePanel instance or null if not found
     * @throws IllegalArgumentException if project is null
     */
    @Nullable
    public static TriagePanelView getPanelForProject(@NotNull Project project) {
        if (project == null) {
            throw new IllegalArgumentException("Project cannot be null");
        }
        
        TriagePanelView panel = panelInstances.get(project);
        if (panel == null) {
            LOG.debug("No panel instance found for project: " + project.getName());
        } else {
            LOG.debug("Retrieved panel instance for project: " + project.getName());
        }
        return panel;
    }
    
    /**
     * Removes the panel instance for the given project.
     * 
     * <p>This method is primarily used for cleanup purposes, such as during
     * testing or when a project is being closed. It removes the panel instance
     * from the global registry and allows for proper resource cleanup.</p>
     * 
     * @param project The project to remove the panel for
     * @throws IllegalArgumentException if project is null
     */
    public static void removePanelForProject(@NotNull Project project) {
        if (project == null) {
            throw new IllegalArgumentException("Project cannot be null");
        }
        
        LOG.debug("Cleaning up panel for project: " + project.getName());
        
        // Dispose MessageBusConnection first
        com.intellij.util.messages.MessageBusConnection connection = messageBusConnections.remove(project);
        if (connection != null) {
            try {
                connection.disconnect();
                LOG.debug("Disconnected MessageBusConnection for project: " + project.getName());
            } catch (Exception e) {
                LOG.warn("Error disconnecting MessageBusConnection for project: " + project.getName(), e);
            }
        }
        
        // Remove and dispose panel instance
        TriagePanelView removedPanel = panelInstances.remove(project);
        if (removedPanel != null) {
            try {
                // Dispose the panel if it implements Disposable
                if (removedPanel instanceof Disposable) {
                    Disposer.dispose((Disposable) removedPanel);
                    LOG.debug("Disposed TriagePanelView for project: " + project.getName());
                }
            } catch (Exception e) {
                LOG.warn("Error disposing TriagePanelView for project: " + project.getName(), e);
            }
            
            if (LOG.isDebugEnabled()) {
                LOG.info("Removed panel instance for project: " + project.getName() + 
                        " (Remaining instances: " + panelInstances.size() + ")");
            } else {
                LOG.info("Removed panel instance for project: " + project.getName());
            }
        } else {
            LOG.debug("No panel instance found to remove for project: " + project.getName());
        }
    }
    
    /**
     * Gets the total number of active panel instances.
     * 
     * <p>This method is useful for monitoring and debugging purposes
     * to track how many panel instances are currently active.</p>
     * 
     * @return The number of active panel instances
     */
    public static int getActivePanelCount() {
        return panelInstances.size();
    }
    
    /**
     * Checks if a panel instance exists for the given project.
     * 
     * @param project The project to check
     * @return true if a panel instance exists for the project, false otherwise
     * @throws IllegalArgumentException if project is null
     */
    public static boolean hasPanelForProject(@NotNull Project project) {
        if (project == null) {
            throw new IllegalArgumentException("Project cannot be null");
        }
        return panelInstances.containsKey(project);
    }
    
    /**
     * Cleans up static resources to prevent memory leaks and ensure consistent startup behavior.
     * 
     * <p>This method should be called during plugin shutdown or when resources need to be reset.
     * It clears the static panel instances map to prevent memory leaks.</p>
     */
    public static void cleanup() {
        LOG.info("Starting cleanup of TriagePanelToolWindowFactory static resources");
        
        int resourcesCleaned = panelInstances.size();
        
        try {
            // Dispose all panel instances and message bus connections
            for (Project project : panelInstances.keySet()) {
                removePanelForProject(project);
            }
            
            LOG.info("TriagePanelToolWindowFactory cleanup completed - disposed " + resourcesCleaned + " panel instances");
        } catch (Exception e) {
            LOG.error("Error during TriagePanelToolWindowFactory cleanup: " + e.getMessage(), e);
        }
    }
    
    /**
     * Disposes of all resources managed by this factory.
     * 
     * <p>This method is called by IntelliJ's Disposer system when the plugin is being
     * unloaded or when the factory needs to be disposed. It ensures proper cleanup
     * of all MessageBusConnections, panel instances, and other resources.</p>
     * 
     * <p>This method implements the Disposable interface and is automatically called
     * by the IntelliJ platform when the plugin is disposed.</p>
     */
    @Override
    public void dispose() {
        LOG.info("Disposing TriagePanelToolWindowFactory");
        
        try {
            // Clean up all static resources
            cleanup();
            
            LOG.info("TriagePanelToolWindowFactory disposed successfully");
        } catch (Exception e) {
            LOG.error("Error during TriagePanelToolWindowFactory disposal: " + e.getMessage(), e);
        }
    }
} 