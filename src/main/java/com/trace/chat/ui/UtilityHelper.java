package com.trace.chat.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.trace.ai.configuration.AISettings;
import com.trace.ai.services.ChatHistoryService;
import com.trace.common.utils.ThemeUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for utility methods in the TriagePanelView.
 * Provides methods for message handling, typing indicators, and general utility operations.
 * 
 * <p>This class encapsulates utility operations to reduce the complexity
 * of the main TriagePanelView class and improve code organization.</p>
 * 
 * @author Alex Ibasitas
 * @version 1.0
 * @since 1.0
 */
public class UtilityHelper {
    
    private static final Logger LOG = Logger.getInstance(UtilityHelper.class);
    
    /**
     * Gets the last 3 user queries from chat history for logging purposes.
     * This helps verify that the sliding window context is working correctly.
     *
     * @param project The current project instance
     * @return List of the last 3 user queries, or empty list if none available
     */
    public static List<String> getLastThreeUserQueries(Project project) {
        List<String> recentQueries = new ArrayList<>();
        try {
            ChatHistoryService chatHistoryService = project.getService(ChatHistoryService.class);
            if (chatHistoryService != null) {
                // Get the last 3 user queries from chat history
                int queryCount = chatHistoryService.getUserQueryCount();
                LOG.info("Total user queries in history: " + queryCount);
                
                // For now, we'll log the query count since the ChatHistoryService doesn't expose individual queries
                // In a future enhancement, we could add a method to get the actual query content
                if (queryCount > 0) {
                    recentQueries.add("Last " + Math.min(3, queryCount) + " user queries available in chat history");
                }
            }
        } catch (Exception e) {
            LOG.error("Error accessing chat history for recent queries: " + e.getMessage());
        }
        return recentQueries;
    }
    
    /**
     * Manually triggers a theme refresh for all components.
     * This can be called programmatically to test theme change behavior.
     *
     * @param themeHelper The theme helper instance to refresh
     */
    public static void manualThemeRefresh(ThemeHelper themeHelper) {
        themeHelper.manualThemeRefresh();
    }
    
    /**
     * Refreshes the main panel when switching tabs.
     * Rebuilds the panel layout based on the current tab state.
     *
     * @param mainPanel The main panel to refresh
     * @param chatOverlayPanel The chat overlay panel
     * @param chatScrollPane The chat scroll pane
     * @param inputPanel The input panel
     * @param showSettingsTab Whether to show the settings tab
     * @param clearChatCallback Callback for clear chat action
     * @param toggleSettingsCallback Callback for toggle settings action
     * @param backToChatCallback Callback for back to chat action
     */
    public static void refreshMainPanel(JPanel mainPanel, JPanel chatOverlayPanel, 
                                      JScrollPane chatScrollPane, JPanel inputPanel,
                                      boolean showSettingsTab,
                                      Runnable clearChatCallback,
                                      Runnable toggleSettingsCallback,
                                      Runnable backToChatCallback) {
        mainPanel.removeAll();
        mainPanel.setLayout(new BorderLayout());
        
        Color panelBg = ThemeUtils.panelBackground();
        mainPanel.setBackground(panelBg);
        mainPanel.setOpaque(true);
        
        JButton aiToggleButton2 = UIComponentHelper.createAIToggleButton();
        JButton clearChatButton2 = UIComponentHelper.createClearChatButton();
        JButton settingsButton2 = UIComponentHelper.createSettingsButton();
        
        // Add action listeners
        clearChatButton2.addActionListener(e -> {
            LOG.debug("Clear chat button clicked");
            clearChatCallback.run();
        });
        
        settingsButton2.addActionListener(e -> {
            LOG.debug("Settings button clicked");
            toggleSettingsCallback.run();
        });
        
        mainPanel.add(UIComponentHelper.createCustomHeaderPanel(aiToggleButton2, clearChatButton2, settingsButton2), BorderLayout.NORTH);
        if (showSettingsTab) {
            AISettings aiSettings = AISettings.getInstance();
            ActionListener backToChatListener = e -> backToChatCallback.run();
            mainPanel.add(UIComponentHelper.createSettingsPanel(aiSettings, backToChatListener), BorderLayout.CENTER);
        } else {
            mainPanel.add(chatOverlayPanel != null ? chatOverlayPanel : chatScrollPane, BorderLayout.CENTER);
            mainPanel.add(inputPanel, BorderLayout.SOUTH);
        }
        
        mainPanel.revalidate();
        mainPanel.repaint();
    }
}
