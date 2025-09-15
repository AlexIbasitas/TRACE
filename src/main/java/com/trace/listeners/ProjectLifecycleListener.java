package com.trace.listeners;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import com.trace.chat.ui.TriagePanelToolWindowFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Listens for project lifecycle events to ensure proper cleanup of TRACE resources.
 */
public class ProjectLifecycleListener implements ProjectManagerListener {
    
    private static final Logger LOG = Logger.getInstance(ProjectLifecycleListener.class);
    
    @Override
    public void projectClosing(@NotNull Project project) {
        LOG.info("Project closing: " + project.getName() + " - cleaning up TRACE resources");
        TriagePanelToolWindowFactory.removePanelForProjectStatic(project);
    }
}
