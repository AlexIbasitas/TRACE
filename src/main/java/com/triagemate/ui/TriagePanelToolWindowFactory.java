package com.triagemate.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
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
        
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(triagePanelView.getContent(), "", false);
        toolWindow.getContentManager().addContent(content);
        
        System.out.println("TriageMate: Tool window content created successfully");
    }
    
    /**
     * Gets the TriagePanelView instance for the given project.
     * 
     * @param project The project to get the panel for
     * @return The TriagePanelView instance or null if not found
     */
    public static TriagePanelView getPanelForProject(Project project) {
        return panelInstances.get(project);
    }
    
    /**
     * Removes the panel instance when the project is closed.
     * 
     * @param project The project being closed
     */
    public static void removePanelForProject(Project project) {
        panelInstances.remove(project);
    }
} 