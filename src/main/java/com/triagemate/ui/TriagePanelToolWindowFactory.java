package com.triagemate.ui;

import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.triagemate.listeners.CucumberTestExecutionListener;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for creating the TriagePanel tool window.
 */
public class TriagePanelToolWindowFactory implements ToolWindowFactory {
    
    // Static map to store panel instances by project
    private static final ConcurrentHashMap<Project, TriagePanelView> panelInstances = new ConcurrentHashMap<>();

    /**
     * Creates the tool window content
     *
     * @param project    The current project
     * @param toolWindow The tool window to create content for
     */
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        System.out.println("TriageMate: Creating tool window content for project: " + project.getName());
        
        TriagePanelView triagePanelView = new TriagePanelView(project);
        
        // Store the panel instance for this project
        panelInstances.put(project, triagePanelView);
        System.out.println("TriageMate: Stored TriagePanelView for project: " + project.getName());
        System.out.println("TriageMate: Total panel instances: " + panelInstances.size());
        
        // Register the test execution listener for this project
        registerTestExecutionListener(project);
        
        Content content = ContentFactory.getInstance().createContent(triagePanelView.getContent(), "", false);
        toolWindow.getContentManager().addContent(content);
        
        System.out.println("TriageMate: Tool window content created successfully");
    }
    
    /**
     * Registers the test execution listener for the given project
     */
    private void registerTestExecutionListener(Project project) {
        try {
            System.out.println("TriageMate: Registering test execution listener for project: " + project.getName());
            
            // Create a listener instance for this project
            CucumberTestExecutionListener listener = new CucumberTestExecutionListener(project);
            
            // Register with the test framework
            project.getMessageBus().connect().subscribe(
                SMTRunnerEventsListener.TEST_STATUS, 
                listener
            );
            
            System.out.println("TriageMate: Test execution listener registered successfully");
        } catch (Exception e) {
            System.err.println("TriageMate: Failed to register test execution listener: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Gets the TriagePanel instance for the given project.
     * 
     * @param project The project to get the panel for
     * @return The TriagePanel instance or null if not found
     */
    public static TriagePanelView getPanelForProject(Project project) {
        return panelInstances.get(project);
    }
    
    /**
     * Removes the panel instance for the given project.
     * Used for cleanup in tests.
     * 
     * @param project The project to remove the panel for
     */
    public static void removePanelForProject(Project project) {
        panelInstances.remove(project);
    }
} 