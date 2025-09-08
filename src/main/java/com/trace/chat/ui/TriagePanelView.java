package com.trace.chat.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.JBColor;
import com.intellij.ide.ui.LafManager;
import com.intellij.ide.ui.LafManagerListener;
import com.intellij.ui.components.JBLabel;

import com.intellij.util.ui.JBUI;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.openapi.Disposable;
import com.trace.ai.configuration.AISettings;
import com.trace.ai.models.AIAnalysisResult;
import com.trace.ai.services.AIAnalysisOrchestrator;
import com.trace.ai.services.ChatHistoryService;
import com.trace.ai.services.AnalysisMode;
import com.trace.ai.prompts.InitialPromptFailureAnalysisService;
import com.trace.ai.ui.SettingsPanel;
import com.trace.chat.components.ChatMessage;
import com.trace.chat.components.MessageComponent;
import com.trace.chat.components.TypingIndicatorRow;
import com.trace.common.constants.TriagePanelConstants;
import com.trace.common.utils.ThemeUtils;
import com.trace.test.models.FailureInfo;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import com.intellij.util.ui.UIUtil;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Chat-style UI component for displaying test failure analysis and user interactions.
 * Implements a robust chat interface following Swing best practices.
 * 
 * <p>This component provides a modern chat interface for analyzing test failures
 * with AI-powered assistance. It displays failure information, allows user interaction,
 * and provides collapsible AI thinking sections for detailed analysis.</p>
 * 
 * @author Alex Ibasitas
 * @version 1.0
 * @since 1.0
 */
public class TriagePanelView {
    
    private static final Logger LOG = Logger.getInstance(TriagePanelView.class);
    
    // Constants for error messages
    private static final String BACKEND_NOT_CONFIGURED_MESSAGE = 
        "AI backend service is not currently configured. To enable AI-powered test failure analysis, please configure your backend connection in the settings.";
    private static final String ERROR_GENERATING_PROMPT_PREFIX = "Error generating prompt: ";
    
    // UI component references
    private final Project project;
    private final JPanel mainPanel;
    private final JPanel inputPanel;
    private final JTextArea inputArea;
    private final JButton sendButton;
    private final JBLabel headerLabel;
    private final JBLabel statusLabel;
    
    // Chat state management
    private final List<ChatMessage> chatHistory;
    private final AIAnalysisOrchestrator aiAnalysisOrchestrator;
    private FailureInfo currentFailureInfo;
    private JScrollPane chatScrollPane;
    private JPanel messageContainer;
    private JPanel chatOverlayPanel;
    private JButton newMessagesChip;
    private JComponent latestUserMessageComponent;
    private JPanel bottomSpacer;
    private boolean showSettingsTab = false;
    // Typing indicator state
    private TypingIndicatorRow typingIndicatorRow;
    private boolean typingIndicatorVisible = false;
    
    // Scroll helper for managing scroll-related functionality
    private final ScrollHelper scrollHelper;
 
    // Theme helper for managing theme-related functionality
    private final ThemeHelper themeHelper;
    
    // Failure analysis helper for managing failure analysis functionality
    private final FailureAnalysisHelper failureAnalysisHelper;
 
    // Analysis mode state management
    private String currentAnalysisMode = "Quick Overview";
    private static final String ANALYSIS_MODE_OVERVIEW = "Quick Overview";
    private static final String ANALYSIS_MODE_FULL = "Full Analysis";
    
    // Test run tracking for "First Failure Wins" feature
    private String currentTestRunId = null;

    /**
     * Constructor for TriagePanelView.
     * Initializes the chat interface with all necessary components and services.
     *
     * @param project The current project instance
     * @throws IllegalArgumentException if project is null
     */
    public TriagePanelView(Project project) {
        if (project == null) {
            throw new IllegalArgumentException("Project cannot be null");
        }
        
        LOG.info("Creating TriagePanelView for project: " + project.getName());
        this.project = project;
        this.chatHistory = new ArrayList<>();
        this.aiAnalysisOrchestrator = new AIAnalysisOrchestrator(project);
        this.currentFailureInfo = null;
        this.scrollHelper = new ScrollHelper();
        
        // Initialize UI components
        this.mainPanel = new JPanel(new BorderLayout());
        this.inputPanel = new JPanel(new BorderLayout());
        this.inputArea = new JTextArea();
        this.sendButton = new JButton("Send");
        this.headerLabel = new JBLabel("No test failure detected");
        this.statusLabel = new JBLabel("");
        
        initializeUI();
        
        // Initialize theme helper after UI components are created
        this.themeHelper = new ThemeHelper(mainPanel, chatScrollPane, messageContainer, 
                                          inputArea, inputPanel, bottomSpacer, 
                                          headerLabel, statusLabel);
        
        // Initialize failure analysis helper after UI components are created
        this.failureAnalysisHelper = new FailureAnalysisHelper(project, aiAnalysisOrchestrator,
                                                              chatHistory, messageContainer,
                                                              chatScrollPane, bottomSpacer,
                                                              scrollHelper, typingIndicatorRow,
                                                              typingIndicatorVisible, latestUserMessageComponent,
                                                              currentAnalysisMode, currentTestRunId,
                                                              currentFailureInfo);
        
        setupEventHandlers();
        setupThemeChangeListener();
        LOG.info("TriagePanelView created successfully");
    }

    /**
     * Gets the current analysis mode.
     *
     * @return The current analysis mode ("Quick Overview" or "Full Analysis")
     */
    public String getCurrentAnalysisMode() {
        return currentAnalysisMode;
    }
    
    /**
     * Sets the current analysis mode.
     *
     * @param mode The analysis mode to set ("Quick Overview" or "Full Analysis")
     * @throws IllegalArgumentException if mode is null or invalid
     */
    public void setCurrentAnalysisMode(String mode) {
        if (mode == null) {
            throw new IllegalArgumentException("Analysis mode cannot be null");
        }
        if (!mode.equals(ANALYSIS_MODE_OVERVIEW) && !mode.equals(ANALYSIS_MODE_FULL)) {
            throw new IllegalArgumentException("Invalid analysis mode: " + mode);
        }
        
        LOG.info("Setting analysis mode from '" + currentAnalysisMode + "' to '" + mode + "'");
        this.currentAnalysisMode = mode;
        
        // Update the failure analysis helper
        if (failureAnalysisHelper != null) {
            failureAnalysisHelper.setCurrentAnalysisMode(mode);
        }
        
        // Update the analysis mode button text if it exists
        // We need to find the button in the component hierarchy
        UIComponentHelper.updateAnalysisModeButtonTextInUI(mainPanel, currentAnalysisMode);
    }
    
    /**
     * Called when a new test run starts.
     * Clears the current test run tracking and chat history to allow analysis of the first failure.
     * This method is called by the CucumberTestExecutionListener when onTestingStarted() fires.
     */
    public void onTestRunStarted() {
        LOG.info("=== TRIAGE PANEL: TEST RUN STARTED ===");
        LOG.info("Previous Test Run ID: " + currentTestRunId);
        LOG.info("Clearing test run tracking and chat history");
        
        // Clear test run tracking
        currentTestRunId = null;
        
        // Update the failure analysis helper
        if (failureAnalysisHelper != null) {
            failureAnalysisHelper.setCurrentTestRunId(null);
        }
        
        // Clear chat history to prevent context from previous test runs
        try {
            ChatHistoryService chatHistoryService = project.getService(ChatHistoryService.class);
            if (chatHistoryService != null) {
                chatHistoryService.clearHistory();
                LOG.info("Chat history cleared for new test run");
            }
        } catch (Exception e) {
            LOG.error("Error clearing chat history: " + e.getMessage());
        }
        
        LOG.info("=== END TRIAGE PANEL: TEST RUN STARTED ===");
    }
    
    /**
     * Gets the chat history for testing purposes.
     * 
     * @return The list of chat messages
     */
    public List<ChatMessage> getChatHistory() {
        return new ArrayList<>(chatHistory);
    }
    

    /**
     * Initializes the UI components with proper chat interface layout.
     * Sets up the main panel, chat area, and input components.
     */
    private void initializeUI() {
        setupChatPanel();
        setupInputPanel();
        
        Color panelBg = ThemeUtils.panelBackground();
        
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(panelBg);
        mainPanel.setOpaque(true);
        
        // Use custom header and tab logic
        JButton aiToggleButton = UIComponentHelper.createAIToggleButton();
        JButton clearChatButton = UIComponentHelper.createClearChatButton();
        JButton settingsButton = UIComponentHelper.createSettingsButton();
        
        // Add action listeners
        clearChatButton.addActionListener(e -> {
            LOG.debug("Clear chat button clicked");
            clearChat();
        });
        
        settingsButton.addActionListener(e -> {
            LOG.debug("Settings button clicked");
            showSettingsTab = !showSettingsTab;
            refreshMainPanel();
        });
        
        mainPanel.add(UIComponentHelper.createCustomHeaderPanel(aiToggleButton, clearChatButton, settingsButton), BorderLayout.NORTH);
        if (showSettingsTab) {
            AISettings aiSettings = AISettings.getInstance();
            ActionListener backToChatListener = e -> {
                showSettingsTab = false;
                refreshMainPanel();
            };
            mainPanel.add(UIComponentHelper.createSettingsPanel(aiSettings, backToChatListener), BorderLayout.CENTER);
        } else {
            mainPanel.add(chatOverlayPanel != null ? chatOverlayPanel : chatScrollPane, BorderLayout.CENTER);
            mainPanel.add(inputPanel, BorderLayout.SOUTH);
        }
    }

    private void refreshTheme() {
        themeHelper.refreshTheme();
    }
    
    /**
     * Recreates all message components to ensure proper theme switching.
     * This is the most reliable way to handle JEditorPane HTML content that doesn't refresh properly.
     */
    private void recreateAllMessageComponents() {
        themeHelper.recreateAllMessageComponents();
    }

    private void refreshThemeInContainer(Container container) {
        themeHelper.refreshThemeInContainer(container);
    }

    /**
     * Sets up the chat panel with proper layout management.
     * Creates the message container and scroll pane for displaying chat messages.
     */
    private void setupChatPanel() {
        LOG.debug("Setting up chat panel");
        
        // Create message container with proper layout and viewport-width tracking
        messageContainer = new com.trace.chat.components.ViewportWidthTrackingPanel();
        messageContainer.setLayout(new BoxLayout(messageContainer, BoxLayout.Y_AXIS));
        messageContainer.setOpaque(true);
        Color panelBg = ThemeUtils.panelBackground();
        messageContainer.setBackground(panelBg);
        messageContainer.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        
        if (LOG.isDebugEnabled()) {
            LOG.debug("Message container created - size: " + messageContainer.getSize() + 
                     ", preferred size: " + messageContainer.getPreferredSize() + 
                     ", layout: " + messageContainer.getLayout().getClass().getSimpleName());
        }
        
        // Create scroll pane with proper configuration
        chatScrollPane = new JScrollPane(messageContainer);
        chatScrollPane.setBorder(BorderFactory.createEmptyBorder());
        chatScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        chatScrollPane.setBackground(panelBg);
        chatScrollPane.getViewport().setBackground(panelBg);
         // Improve large-content scroll performance
         chatScrollPane.getViewport().setScrollMode(JViewport.BLIT_SCROLL_MODE);
        // Allow horizontal scrolling when the content's minimum width exceeds the viewport
        chatScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        
        // Set up ScrollHelper references
        scrollHelper.setChatScrollPane(chatScrollPane);
        scrollHelper.setMessageContainer(messageContainer);
        
        if (LOG.isDebugEnabled()) {
            LOG.debug("Scroll pane created - size: " + chatScrollPane.getSize() + 
                     ", preferred size: " + chatScrollPane.getPreferredSize() + 
                     ", viewport size: " + chatScrollPane.getViewport().getSize());
        }

        // Recompute spacer and possibly re-align on viewport size changes (e.g., window resize)
        chatScrollPane.getViewport().addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                if (chatScrollPane != null) {
                    scrollHelper.recomputeBottomSpacer();
                    if (latestUserMessageComponent != null) {
                        int target = scrollHelper.computeAlignTopTarget(chatScrollPane, latestUserMessageComponent);
                        if (scrollHelper.isNearTarget(chatScrollPane, target, scrollHelper.getNearTargetThresholdPx())) {
                            scrollHelper.scheduleDebouncedReScroll();
                        } else {
                            scrollHelper.showNewMessagesChip();
                        }
                    }
                }
            }
        });

        // Ensure the message container uses the same cutoff as our constants
        if (messageContainer instanceof com.trace.chat.components.ViewportWidthTrackingPanel) {
            ((com.trace.chat.components.ViewportWidthTrackingPanel) messageContainer)
                .setMinWidthBeforeHorizontalScroll(com.trace.common.constants.TriagePanelConstants.MIN_CHAT_WIDTH_BEFORE_SCROLL);
        }
        
        // Create a dynamic bottom spacer to guarantee enough scroll range
        bottomSpacer = new JPanel();
        bottomSpacer.setOpaque(true);
        bottomSpacer.setBackground(panelBg);
        bottomSpacer.setPreferredSize(new Dimension(1, 0));
        messageContainer.add(bottomSpacer);
        
        // Set up ScrollHelper bottom spacer reference
        scrollHelper.setBottomSpacer(bottomSpacer);

        // Add a component listener to handle post-layout re-scroll when content grows
        messageContainer.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                // If user is near the latest target during content growth, gently keep alignment
                if (chatScrollPane != null && latestUserMessageComponent != null) {
                    scrollHelper.recomputeBottomSpacer();
                    int target = scrollHelper.computeAlignTopTarget(chatScrollPane, latestUserMessageComponent);
                    LOG.debug(String.format(
                        "componentResized: prefH=%d viewportH=%d target=%d value=%d near=%s",
                        messageContainer.getPreferredSize().height,
                        chatScrollPane.getViewport().getExtentSize().height,
                        target,
                        chatScrollPane.getVerticalScrollBar().getValue(),
                        Boolean.toString(scrollHelper.isNearTarget(chatScrollPane, target, scrollHelper.getNearTargetThresholdPx()))));
                    if (scrollHelper.isNearTarget(chatScrollPane, target, scrollHelper.getNearTargetThresholdPx())) {
                        scrollHelper.scheduleDebouncedReScroll();
                    } else {
                        // Content grew but user is away; do not yank, show chip
                        scrollHelper.showNewMessagesChip();
                    }
                }
            }
        });

        // Create overlay container to host a floating "New messages" chip over the scroll area
        JPanel overlay = new JPanel();
        overlay.setOpaque(true);
        overlay.setBackground(panelBg);
        overlay.setLayout(new OverlayLayout(overlay));

        // Create chip host panel aligned bottom-right using GridBagLayout
        JPanel chipHost = new JPanel(new GridBagLayout());
        chipHost.setOpaque(true);
        chipHost.setBackground(panelBg);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.SOUTHEAST;
        gbc.insets = new Insets(0, 0, 12, 12); // margin from bottom-right

        newMessagesChip = new JButton("Jump to latest");
        newMessagesChip.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        newMessagesChip.setFocusable(false);
        newMessagesChip.setVisible(false);
        newMessagesChip.addActionListener(e -> {
            scrollHelper.hideNewMessagesChip();
            if (latestUserMessageComponent != null) {
                LOG.debug("chip: click → align latest");
                scrollHelper.scrollToComponentTopSmooth(chatScrollPane, latestUserMessageComponent, 200);
            }
        });
        chipHost.add(newMessagesChip, gbc);
        
        // Set up ScrollHelper newMessagesChip reference
        scrollHelper.setNewMessagesChip(newMessagesChip);

        // Add scroll pane first, then chip host so the chip paints on top
        overlay.add(chatScrollPane);
        overlay.add(chipHost);
        this.chatOverlayPanel = overlay;
        
        // Set up ScrollHelper chatOverlayPanel reference
        scrollHelper.setChatOverlayPanel(chatOverlayPanel);

        // Add scrollbar listener to cancel smooth scroll on user interaction and hide chip at bottom
        chatScrollPane.getVerticalScrollBar().addAdjustmentListener(e -> {
            // Cancel animation if the user scrolls
            if (scrollHelper.isSmoothScrolling() && !scrollHelper.isProgrammaticScroll()) {
                LOG.debug("userScroll: canceling smooth scroll (value=" + e.getValue() + ")");
                scrollHelper.cancelSmoothScroll();
            }
            // Auto-hide chip when near the latest target
            if (latestUserMessageComponent != null) {
                int target = scrollHelper.computeAlignTopTarget(chatScrollPane, latestUserMessageComponent);
                if (scrollHelper.isNearTarget(chatScrollPane, target, 8)) {
                    LOG.debug("atTarget: hiding chip (value=" + e.getValue() + ", target=" + target + ")");
                    scrollHelper.hideNewMessagesChip();
                }
            }
        });
    }

    /**
     * Sets up the input panel with modern styling.
     * Configures the text input area and send button with proper layout and styling.
     */
    private void setupInputPanel() {
        LOG.debug("Setting up input panel");
        
        inputPanel.setBorder(BorderFactory.createEmptyBorder(8, 16, 16, 16));
        
        // Create analysis mode button - SIMPLE LEFT ALIGNMENT
        JButton analysisModeButton = UIComponentHelper.createAnalysisModeButton(currentAnalysisMode);
        
        // Add action listener to toggle between modes
        analysisModeButton.addActionListener(e -> {
            if (ANALYSIS_MODE_OVERVIEW.equals(currentAnalysisMode)) {
                setCurrentAnalysisMode(ANALYSIS_MODE_FULL);
            } else {
                setCurrentAnalysisMode(ANALYSIS_MODE_OVERVIEW);
            }
            UIComponentHelper.updateAnalysisModeButtonText(analysisModeButton, currentAnalysisMode);
        });
        
        // Create input container with enhanced border for better visibility
        JPanel inputBoxContainer = new JPanel(new BorderLayout());
        inputBoxContainer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(TriagePanelConstants.getInputContainerBorder(), 1, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 0)
        ));
        inputBoxContainer.setBackground(TriagePanelConstants.getInputContainerBackground());
        inputBoxContainer.setOpaque(true);
        inputBoxContainer.setName("inputBoxContainer");
        
        // SIMPLE FIX: Ensure container has minimum height for any button size
        inputBoxContainer.setMinimumSize(new Dimension(300, 64)); // Minimum container height
        
        // Configure text area for multi-line input with dynamic sizing
        inputArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputArea.setRows(3);
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        inputArea.setBackground(ThemeUtils.textFieldBackground());
        inputArea.setForeground(ThemeUtils.textForeground());
        inputArea.setCaretColor(ThemeUtils.textForeground());
        inputArea.setOpaque(true);
        inputArea.putClientProperty("JTextField.placeholderText", TriagePanelConstants.INPUT_PLACEHOLDER_TEXT);
        
        // SIMPLE FIX: Make input area grow with button size to prevent cut-off
        inputArea.setMinimumSize(new Dimension(200, 48)); // Minimum height to accommodate any button size
        
        // Create modern send button with custom icon
        JButton sendIconButton = UIComponentHelper.createModernSendButton();
        
        // Add action listener
        sendIconButton.addActionListener(e -> sendMessage());
        
        // NATURAL LAYOUT: Simple BorderLayout placement, let IntelliJ handle sizing
        inputBoxContainer.add(inputArea, BorderLayout.CENTER);
        inputBoxContainer.add(sendIconButton, BorderLayout.EAST);
        
        // Use BorderLayout with minimal gap for tight spacing
        inputPanel.setLayout(new BorderLayout(0, 2)); // 2px gap for tight spacing
        inputPanel.add(inputBoxContainer, BorderLayout.CENTER);
        
        // Create a compact container for the toggle below the input box
        JPanel toggleContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        toggleContainer.setOpaque(false);
        toggleContainer.setPreferredSize(new Dimension(inputBoxContainer.getPreferredSize().width, 20)); // Reduced height for tighter spacing
        toggleContainer.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0)); // Minimal padding for tight appearance
        toggleContainer.add(analysisModeButton);
        
        // FORCE LEFT ALIGNMENT - Set button alignment properties
        analysisModeButton.setHorizontalAlignment(SwingConstants.LEFT);
        analysisModeButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Position button at top of container to be right below input box
        toggleContainer.setLayout(new BorderLayout());
        toggleContainer.add(analysisModeButton, BorderLayout.NORTH);
        
        inputPanel.add(toggleContainer, BorderLayout.SOUTH);
        
        // Hide the old send button
        sendButton.setVisible(false);
        
        // Add detailed logging to understand the layout hierarchy
        if (LOG.isDebugEnabled()) {
            LOG.debug("Layout investigation - Input panel border: " + inputPanel.getBorder() + 
                     ", insets: " + inputPanel.getInsets() + 
                     ", toggle container preferred size: " + toggleContainer.getPreferredSize() + 
                     ", input box container preferred size: " + inputBoxContainer.getPreferredSize());
        }
        
        LOG.debug("Input panel setup completed");
    }

    /**
     * Sets up event handlers for user interactions.
     * Configures keyboard shortcuts and button click handlers.
     */
    private void setupEventHandlers() {
        // Send button click handler
        sendButton.addActionListener(e -> sendMessage());
        
        // Enter key handler for input area
        inputArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (e.isShiftDown()) {
                        // Shift+Enter: insert newline
                        inputArea.append("\n");
                    } else {
                        // Enter: send message
                        e.consume();
                        sendMessage();
                    }
                }
            }
        });
    }
    
    /**
     * Sets up theme change listener to automatically refresh all components when the IDE theme changes.
     * This ensures that existing chat messages and UI elements update their colors to match the new theme.
     * Uses a modern approach compatible with newer IntelliJ versions.
     */
    
    private void setupThemeChangeListener() {
        themeHelper.setupThemeChangeListener();
    }

    /**
     * Sends the current message from the input area.
     * Processes user input and adds appropriate responses to the chat.
     */
    private void sendMessage() {
        String messageText = inputArea.getText().trim();
        if (messageText.isEmpty()) {
            LOG.debug("Send message called but input is empty - ignoring");
            return;
        }
        
        LOG.info("Sending user message: " + messageText.substring(0, Math.min(messageText.length(), 50)) + "...");
	        // Capture whether the viewport was at bottom before content growth
        try {
            scrollHelper.setWasNearBottomBeforeUserSend(scrollHelper.isNearBottom(chatScrollPane, scrollHelper.getNearTargetThresholdPx()));
        } catch (Exception ignore) {}
        
        // Add user message to chat
        addMessage(new ChatMessage(ChatMessage.Role.USER, messageText, System.currentTimeMillis(), null, null));
        
        // Clear input area
        inputArea.setText("");

        // Show typing indicator immediately after user message
        showTypingIndicator();
        
        // Check AI configuration and provide appropriate response
        failureAnalysisHelper.handleUserMessageWithAI(messageText);
    }
    

    /**
     * Adds a message to the chat history and updates the UI with proper EDT compliance.
     * Ensures thread safety by dispatching UI updates to the Event Dispatch Thread.
     *
     * @param message The chat message to add
     */
    private void addMessage(ChatMessage message) {
        // If assistant content arrives, remove typing indicator before appending
        if (message != null && message.isFromAI()) {
            // If we have a visible typing indicator, replace it in-place with the AI message component
            if (typingIndicatorRow != null && typingIndicatorVisible) {
                try {
                    chatHistory.add(message);
                    MessageComponent aiComponent = new MessageComponent(message);
                    scrollHelper.replaceTypingIndicatorWithMessageComponent(aiComponent, typingIndicatorRow);
                    return; // UI updated in-place; skip full rebuild
                } catch (Exception ex) {
                    // Fallback to default path
                    LOG.debug("Failed to replace typing indicator in-place, falling back to rebuild: " + ex.getMessage());
                }
            }
            // No indicator to replace; proceed with normal flow
            hideTypingIndicator();
        }

        MessageManagerHelper.addMessage(message, chatHistory, messageContainer, typingIndicatorRow, 
            typingIndicatorVisible, latestUserMessageComponent, scrollHelper, chatScrollPane, bottomSpacer);
    }

    /**
     * Replaces the typing indicator row with the provided AI message component in-place
     * to avoid visual jumping. Keeps spacer as last child, and avoids any animated scroll.
     */



    /**
     * Adds a message component to the UI with proper layout.
     * Recreates the message container with all messages and proper spacing.
     *
     * @param message The message to add to the UI
     */

    // ===== Typing indicator helpers =====
    private void showTypingIndicator() {
        MessageManagerHelper.showTypingIndicator(messageContainer, typingIndicatorRow, typingIndicatorVisible, 
            bottomSpacer, chatHistory, latestUserMessageComponent, scrollHelper, chatScrollPane);
        typingIndicatorVisible = true;
    }

    private void hideTypingIndicator() {
        MessageManagerHelper.hideTypingIndicator(messageContainer, typingIndicatorRow, typingIndicatorVisible);
        typingIndicatorVisible = false;
        typingIndicatorRow = null;
    }

    /**
     * Updates the panel with new failure information.
     * Clears the chat and generates an initial AI prompt for the failure.
     *
     * @param failureInfo The failure information to analyze
     * @throws IllegalArgumentException if failureInfo is null
     */
    public boolean updateFailure(FailureInfo failureInfo) {
        boolean result = failureAnalysisHelper.updateFailure(failureInfo);
        
        // Update local state to keep in sync with helper
        if (result) {
            this.currentFailureInfo = failureInfo;
            this.currentTestRunId = failureAnalysisHelper.getCurrentTestRunId();
        }
        
        return result;
    }

    /**
     * Clears the chat history and UI.
     * Removes all messages and resets the message container.
     */
    private void clearChat() {
        MessageManagerHelper.clearChat(chatHistory, messageContainer, bottomSpacer, latestUserMessageComponent);
        latestUserMessageComponent = null;
    }

    
    /**
     * Displays an AI analysis result in the chat interface.
     * This method is called when AI analysis completes successfully.
     * 
     *
     * @param result The AI analysis result to display
     */
    public void displayAIAnalysisResult(AIAnalysisResult result) {
        // Check if TRACE is enabled (power button) - if not, do nothing at all
        AISettings aiSettings = AISettings.getInstance();
        if (!aiSettings.isAIEnabled()) {
            LOG.info("TRACE is disabled - skipping AI analysis result display");
            return; // Complete silence - no messages from TRACE
        }
        
        if (result == null) {
            LOG.info("AI analysis result is null, cannot display");
            return;
        }
                
        // Ensure we're on the EDT
        if (!SwingUtilities.isEventDispatchThread()) {
            ApplicationManager.getApplication().invokeLater(() -> displayAIAnalysisResult(result));
            return;
        }
        
        try {
            // Create an AI message with the analysis result
            String analysisText = result.getAnalysis();
            if (analysisText != null && !analysisText.trim().isEmpty()) {
                addMessage(new ChatMessage(ChatMessage.Role.AI, analysisText, System.currentTimeMillis(), null, null));
            } else {
                LOG.info("AI analysis result is empty");
                addMessage(new ChatMessage(ChatMessage.Role.AI, "AI analysis completed but returned no content.", System.currentTimeMillis(), null, null));
            }
        } catch (Exception e) {
            LOG.error("Error displaying AI analysis result: " + e.getMessage(), e);
            addMessage(new ChatMessage(ChatMessage.Role.AI, "Error displaying AI analysis result: " + e.getMessage(), System.currentTimeMillis(), null, null));
        }
    }
    
    /**
     * Displays an AI analysis error in the chat interface.
     * This method is called when AI analysis fails.
     *
     * @param errorMessage The error message to display
     */
    public void displayAIAnalysisError(String errorMessage) {
        // Check if TRACE is enabled (power button) - if not, do nothing at all
        AISettings aiSettings = AISettings.getInstance();
        if (!aiSettings.isAIEnabled()) {
            LOG.info("TRACE is disabled - skipping AI analysis error display");
            return; // Complete silence - no messages from TRACE
        }
        
        if (errorMessage == null || errorMessage.trim().isEmpty()) {
            LOG.info("AI analysis error message is null or empty");
            return;
        }
        
        LOG.info("Displaying AI analysis error: " + errorMessage);
        
        // Ensure we're on the EDT
        if (!SwingUtilities.isEventDispatchThread()) {
            ApplicationManager.getApplication().invokeLater(() -> displayAIAnalysisError(errorMessage));
            return;
        }
        
        try {
            // Create an AI message with the error
            addMessage(new ChatMessage(ChatMessage.Role.AI, "AI Analysis Error: " + errorMessage, System.currentTimeMillis(), null, null));
        } catch (Exception e) {
            LOG.error("Error displaying AI analysis error: " + e.getMessage(), e);
        }
    }

    /**
     * Gets the main content panel for integration with IntelliJ's tool window system.
     *
     * @return The main panel component
     */
    public JComponent getContent() {
        return mainPanel;
    }
    
    
    /**
     * Shuts down the orchestrator and cleans up resources.
     * This method should be called when the panel is being disposed.
     */
    public void shutdown() {
        LOG.info("Shutting down TriagePanelView");
        if (aiAnalysisOrchestrator != null) {
            aiAnalysisOrchestrator.shutdown();
        }
        LOG.info("TriagePanelView shutdown completed");
    }
    
    /**
     * Manually triggers a theme refresh for all components.
     * This can be called programmatically to test theme change behavior.
     */
    public void manualThemeRefresh() {
        UtilityHelper.manualThemeRefresh(themeHelper);
    }




    
    
    
    




    /**
     * Refreshes the main panel when switching tabs.
     * Rebuilds the panel layout based on the current tab state.
     */
    private void refreshMainPanel() {
        UtilityHelper.refreshMainPanel(
            mainPanel, 
            chatOverlayPanel, 
            chatScrollPane, 
            inputPanel,
            showSettingsTab,
            this::clearChat,
            () -> {
                showSettingsTab = !showSettingsTab;
                refreshMainPanel();
            },
            () -> {
                showSettingsTab = false;
                refreshMainPanel();
            }
        );
    }
    
    /**
     * Disposes of resources and cleans up listeners to prevent memory leaks.
     * This method should be called when the component is no longer needed.
     */
    public void dispose() {
        try {
            LOG.info("Disposing TriagePanelView resources");
            
            // Dispose theme helper
            if (themeHelper != null) {
                themeHelper.dispose();
            }
            
            // Stop any running timers in ScrollHelper
            if (scrollHelper != null) {
                scrollHelper.cancelSmoothScroll();
            }
            
            // Clear chat history
            if (chatHistory != null) {
                chatHistory.clear();
            }
            
            // Clear component references
            currentFailureInfo = null;
            latestUserMessageComponent = null;
            typingIndicatorRow = null;
            
            LOG.info("TriagePanelView disposal completed");
        } catch (Exception e) {
            LOG.error("Error during TriagePanelView disposal: " + e.getMessage(), e);
        }
    }
}