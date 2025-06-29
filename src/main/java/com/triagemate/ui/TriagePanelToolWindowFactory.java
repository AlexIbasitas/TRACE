package com.triagemate.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Factory for creating the TriagePanel tool window.
 */
public class TriagePanelToolWindowFactory implements ToolWindowFactory {

    /**
     * Creates the tool window content
     *
     * @param project    The current project
     * @param toolWindow The tool window to create content for
     */
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        TriagePanelView triagePanelView = new TriagePanelView(project);
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(triagePanelView.getContent(), "", false);
        toolWindow.getContentManager().addContent(content);
    }
} 