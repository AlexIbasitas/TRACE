package com.triagemate.ui;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.components.JBTextArea;

import javax.swing.*;
import java.awt.*;

/**
 * UI component for displaying test failure information and analysis.
 */
public class TriagePanelView {
    private final Project project;
    private final JPanel mainPanel;
    private final JBTextArea promptArea;
    private final JBTabbedPane tabbedPane;

    /**
     * Constructor for TriagePanelView
     *
     * @param project The current project
     */
    public TriagePanelView(Project project) {
        this.project = project;
        this.mainPanel = new JPanel(new BorderLayout());
        this.promptArea = new JBTextArea();
        this.tabbedPane = new JBTabbedPane();
        
        initializeUI();
    }

    /**
     * Initializes the UI components
     */
    private void initializeUI() {
        // Set up the prompt area
        promptArea.setEditable(false);
        promptArea.setLineWrap(true);
        promptArea.setWrapStyleWord(true);
        JBScrollPane promptScrollPane = new JBScrollPane(promptArea);
        
        // Create tabs
        JPanel analysisTab = createAnalysisTab();
        JPanel configTab = createConfigTab();
        
        // Add tabs to tabbed pane
        tabbedPane.addTab("Analysis", analysisTab);
        tabbedPane.addTab("Configuration", configTab);
        
        // Add components to main panel
        mainPanel.add(promptScrollPane, BorderLayout.CENTER);
        mainPanel.add(tabbedPane, BorderLayout.SOUTH);
    }

    /**
     * Creates the analysis tab
     *
     * @return The analysis tab panel
     */
    private JPanel createAnalysisTab() {
        JPanel panel = new JPanel(new BorderLayout());
        // Placeholder - will contain analysis results
        return panel;
    }

    /**
     * Creates the configuration tab
     *
     * @return The configuration tab panel
     */
    private JPanel createConfigTab() {
        JPanel panel = new JPanel(new BorderLayout());
        // Placeholder - will contain configuration options
        return panel;
    }

    /**
     * Updates the prompt area with new text
     *
     * @param promptText The text to display in the prompt area
     */
    public void updatePrompt(String promptText) {
        promptArea.setText(promptText);
    }

    /**
     * Gets the main content panel
     *
     * @return The main panel
     */
    public JComponent getContent() {
        return mainPanel;
    }
} 