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
        handleUserMessageWithAI(messageText);
    }
    
    /**
     * Handles user messages with AI analysis if configured, otherwise shows configuration guidance.
     * Enhanced with comprehensive logging for prompt verification and robust error handling.
     *
     * @param messageText The user's message text
     */
    private void handleUserMessageWithAI(String messageText) {
        LOG.info("Handling user message with AI: " + messageText.substring(0, Math.min(messageText.length(), 50)) + "...");
        
        AISettings aiSettings = AISettings.getInstance();
        
        // TRACE gating: If TRACE is OFF, notify user and stop
        if (!aiSettings.isTraceEnabled()) {
            LOG.info("TRACE is OFF - notifying user and skipping all processing");
            addMessage(new ChatMessage(ChatMessage.Role.AI,
                "TRACE is OFF. Turn TRACE ON to enable context extraction and AI features.",
                System.currentTimeMillis(), null, null));
            return;
        }

        // With TRACE ON and Enable AI Analysis OFF: build prompt preview only (no docs, no AI call)
        if (!aiSettings.isAIAnalysisEnabled()) {
            LOG.info("Enable AI Analysis is OFF - generating prompt preview without RAG or AI call");
            if (currentFailureInfo == null) {
                addMessage(new ChatMessage(ChatMessage.Role.AI,
                    "No test failure context available. Please run a test first to establish context for prompt preview.",
                    System.currentTimeMillis(), null, null));
                return;
            }
            ChatHistoryService chatHistoryService = project.getService(ChatHistoryService.class);
            if (chatHistoryService != null) {
                chatHistoryService.addUserQuery(messageText);
            }
            // Build prompt preview only; do not attach it to Show AI thinking for user messages
            aiAnalysisOrchestrator.getUserQueryOrchestrator()
                .generatePrompt(currentFailureInfo, messageText, chatHistoryService);
            addMessage(new ChatMessage(
                ChatMessage.Role.AI,
                "Enable AI Analysis is OFF. Turn it ON to run AI analysis.",
                System.currentTimeMillis(), null, null));
            return;
        }

        // TRACE ON and Enable AI Analysis ON: proceed
        if (!aiSettings.isConfigured()) {
            // AI is not configured, show configuration guidance
            String configurationStatus = aiSettings.getConfigurationStatus();
            String guidanceMessage = "AI features are not configured. " + configurationStatus + 
                                   " Please configure AI settings to use AI-powered features.";
            LOG.info("AI not configured - showing configuration guidance");
            addMessage(new ChatMessage(ChatMessage.Role.AI, guidanceMessage, 
                                    System.currentTimeMillis(), null, null));
            return;
        }
        
        // AI is configured and enabled, process the message with orchestrator
        LOG.info("AI is configured and enabled - processing user query with orchestrator");
        
        // Check if we have failure context for user queries
        if (currentFailureInfo == null) {
            LOG.info("No failure context available for user query");
            addMessage(new ChatMessage(ChatMessage.Role.AI, 
                "No test failure context available. Please run a test first to establish context for AI analysis.", 
                System.currentTimeMillis(), null, null));
            return;
        }
        
        if (LOG.isDebugEnabled()) {
            LOG.debug("Failure Context Available - Scenario Name: " + currentFailureInfo.getScenarioName() + 
                     ", Failed Step: " + (currentFailureInfo.getFailedStepText() != null ? 
                         currentFailureInfo.getFailedStepText().substring(0, Math.min(currentFailureInfo.getFailedStepText().length(), 50)) + "..." : "null") + 
                     ", Error Message: " + (currentFailureInfo.getErrorMessage() != null ? 
                         currentFailureInfo.getErrorMessage().substring(0, Math.min(currentFailureInfo.getErrorMessage().length(), 50)) + "..." : "null"));
        }
        
        // Check chat history service availability and log recent queries
        try {
            ChatHistoryService chatHistoryService = project.getService(ChatHistoryService.class);
            if (chatHistoryService == null) {
                LOG.info("ChatHistoryService is null - proceeding without chat history");
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Chat History Service State - User Query Count: " + chatHistoryService.getUserQueryCount() + 
                             ", Has Failure Context: " + chatHistoryService.hasFailureContext() + 
                             ", Window Size: " + chatHistoryService.getUserMessageWindowSize());
                }
                
                // Log recent user queries for context verification
                List<String> recentQueries = getLastThreeUserQueries();
                if (!recentQueries.isEmpty() && LOG.isDebugEnabled()) {
                    LOG.debug("Recent User Queries Context: " + String.join(", ", recentQueries));
                }
            }
        } catch (Exception e) {
            LOG.error("Error accessing ChatHistoryService: " + e.getMessage());
        }
        
        // Process the user query with enhanced analysis and RAG
        LOG.info("Calling aiAnalysisOrchestrator.analyzeUserQueryWithDocuments()");
        CompletableFuture<AIAnalysisResult> analysisFuture = 
            aiAnalysisOrchestrator.analyzeUserQueryWithDocuments(currentFailureInfo, messageText);
        
        // Handle the analysis result
        analysisFuture.thenAccept(result -> {
            LOG.info("User query analysis result received");
            if (result != null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("AI Analysis Result - Service Type: " + result.getServiceType() + 
                             ", Timestamp: " + result.getTimestamp() + 
                             ", Analysis Length: " + (result.getAnalysis() != null ? result.getAnalysis().length() : 0) + " characters" + 
                             ", Has Prompt: " + result.hasPrompt());
                }
                
                if (result.getAnalysis() != null && !result.getAnalysis().trim().isEmpty()) {
                    LOG.info("Adding AI response to chat");
                    // Do not attach Show AI thinking for user query responses
                    addMessage(new ChatMessage(ChatMessage.Role.AI, result.getAnalysis(), 
                                            System.currentTimeMillis(), null, null));
                } else {
                    LOG.info("AI analysis returned empty content");
                    addMessage(new ChatMessage(ChatMessage.Role.AI, 
                        "AI analysis completed but returned no content.", 
                        System.currentTimeMillis(), null, null));
                }
            } else {
                LOG.info("AI analysis returned null result");
                addMessage(new ChatMessage(ChatMessage.Role.AI, 
                    "AI analysis returned no result.", 
                    System.currentTimeMillis(), null, null));
            }
        }).exceptionally(throwable -> {
            LOG.error("User query analysis failed: " + throwable.getMessage(), throwable);
            String errorMessage = "AI analysis failed: " + throwable.getMessage();
            addMessage(new ChatMessage(ChatMessage.Role.AI, errorMessage, 
                                    System.currentTimeMillis(), null, null));
            return null;
        });
        
        LOG.info("User message handling completed");
    }

    /**
     * Adds a message to the chat history and updates the UI with proper EDT compliance.
     * Ensures thread safety by dispatching UI updates to the Event Dispatch Thread.
     *
     * @param message The chat message to add
     */
    private void addMessage(ChatMessage message) {
        if (!SwingUtilities.isEventDispatchThread()) {
            ApplicationManager.getApplication().invokeLater(() -> addMessage(message));
            return;
        }
        
        // If assistant content arrives, remove typing indicator before appending
        if (message != null && message.isFromAI()) {
            // Capture whether we are currently near the latest user target BEFORE appending
            try {
                if (chatScrollPane != null && latestUserMessageComponent != null) {
                    int preTarget = scrollHelper.computeAlignTopTarget(chatScrollPane, latestUserMessageComponent);
                    boolean preNear = scrollHelper.isNearTarget(chatScrollPane, preTarget, scrollHelper.getNearTargetThresholdPx());
                    scrollHelper.setMaintainAlignAfterAppend(preNear);
                    // Capture anchor to preserve the user's row fixed position
                    scrollHelper.setAnchorActiveForAppend(true);
                    scrollHelper.setAnchorUserTopYBeforeAppend(latestUserMessageComponent.getY());
                    scrollHelper.setAnchorScrollValueBeforeAppend(chatScrollPane.getVerticalScrollBar().getValue());
                    LOG.debug("preAppend AI: preNear=" + preNear + " preTarget=" + preTarget +
                        " value=" + chatScrollPane.getVerticalScrollBar().getValue());
                } else {
                    scrollHelper.setMaintainAlignAfterAppend(false);
                    scrollHelper.setAnchorActiveForAppend(false);
                }
            } catch (Exception ignore) {
                scrollHelper.setMaintainAlignAfterAppend(false);
                scrollHelper.setAnchorActiveForAppend(false);
            }
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

        if (LOG.isDebugEnabled()) {
            LOG.debug("Adding message to chat history: " + message.getRole() + " - " + 
                     message.getText().substring(0, Math.min(message.getText().length(), 30)) + "...");
        }
        chatHistory.add(message);
        addMessageToUI(message);
    }

    /**
     * Replaces the typing indicator row with the provided AI message component in-place
     * to avoid visual jumping. Keeps spacer as last child, and avoids any animated scroll.
     */
    private void replaceTypingIndicatorWithMessageComponent(MessageComponent aiComponent) {
        if (messageContainer == null || aiComponent == null) return;

        int count = messageContainer.getComponentCount();
        int idx = -1;
        for (int i = 0; i < count; i++) {
            if (messageContainer.getComponent(i) == typingIndicatorRow) {
                idx = i;
                break;
            }
        }
        if (idx >= 0) {
            // Stop animation and clear indicator state but do not trigger rebuild
            try { if (typingIndicatorRow != null) typingIndicatorRow.stopAnimation(); } catch (Exception ignore) {}
            typingIndicatorVisible = false;

            // Replace indicator with AI component at the same index
            messageContainer.remove(idx);
            aiComponent.setAlignmentY(Component.TOP_ALIGNMENT);
            messageContainer.add(aiComponent, idx);

            // Ensure bottom spacer is the last child
            if (bottomSpacer != null) {
                // Remove and re-add to guarantee it is last
                messageContainer.remove(bottomSpacer);
                messageContainer.add(bottomSpacer);
            }

            // Refresh layout and recompute spacer to keep maxScroll tight
            messageContainer.revalidate();
            messageContainer.repaint();
            ApplicationManager.getApplication().invokeLater(() -> {
                scrollHelper.recomputeBottomSpacer();
                scrollHelper.restoreAnchorAfterAppend();
            });

            // Reset indicator reference
            typingIndicatorRow = null;
        }
    }



    /**
     * Adds a message component to the UI with proper layout.
     * Recreates the message container with all messages and proper spacing.
     *
     * @param message The message to add to the UI
     */
    private void addMessageToUI(ChatMessage message) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Starting message UI addition - message role: " + message.getRole() + 
                     ", text length: " + (message.getText() != null ? message.getText().length() : 0) + 
                     ", chatHistory size: " + chatHistory.size() + 
                     ", messageContainer component count: " + messageContainer.getComponentCount() + 
                     ", messageContainer size: " + messageContainer.getSize() + 
                     ", messageContainer preferred size: " + messageContainer.getPreferredSize());
        }
        
        // Remove the vertical glue temporarily
        messageContainer.removeAll();
        
        if (LOG.isDebugEnabled()) {
            LOG.debug("Container cleared - component count after clear: " + messageContainer.getComponentCount());
        }
        
        // Add all existing messages with proper spacing
        for (int i = 0; i < chatHistory.size(); i++) {
            ChatMessage existingMessage = chatHistory.get(i);
            
            MessageComponent existingComponent = new MessageComponent(existingMessage);
            existingComponent.setAlignmentY(Component.TOP_ALIGNMENT);
            
            messageContainer.add(existingComponent);
            
            // Add spacing between messages, but not after the last one
            if (i < chatHistory.size() - 1) {
                messageContainer.add(Box.createVerticalStrut(16));
            }
        }

        // Track newest user message component
        latestUserMessageComponent = null;
        for (int i = messageContainer.getComponentCount() - 1; i >= 0; i--) {
            Component c = messageContainer.getComponent(i);
            if (c instanceof MessageComponent) {
                ChatMessage cm = ((MessageComponent) c).getMessage();
                if (cm != null && cm.isFromUser()) {
                    latestUserMessageComponent = (JComponent) c;
                    scrollHelper.setLatestUserMessageComponent(latestUserMessageComponent);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Latest user message set - index: " + i + ", y: " + latestUserMessageComponent.getY() +
                            ", prefH: " + latestUserMessageComponent.getPreferredSize().height +
                            ", count: " + messageContainer.getComponentCount());
                    }
                    break;
                }
            }
        }

        // Optionally append typing indicator row just after the last message
        if (typingIndicatorVisible) {
            if (typingIndicatorRow == null) {
                typingIndicatorRow = new TypingIndicatorRow();
            }
            // Add spacing before the indicator when there are messages
            if (!chatHistory.isEmpty()) {
                messageContainer.add(Box.createVerticalStrut(16));
            }
            messageContainer.add(typingIndicatorRow);
        }

        // Ensure bottom spacer is the last child
        messageContainer.add(bottomSpacer);
        scrollHelper.recomputeBottomSpacer();
        
        if (LOG.isDebugEnabled()) {
            LOG.debug("Final container state - component count: " + messageContainer.getComponentCount() + 
                     ", size: " + messageContainer.getSize() + 
                     ", preferred size: " + messageContainer.getPreferredSize() + 
                     ", viewport extent size: " + chatScrollPane.getViewport().getExtentSize() + 
                     ", scrollbar value: " + chatScrollPane.getVerticalScrollBar().getValue());
        }
        
        // Revalidate and repaint
        messageContainer.revalidate();
        messageContainer.repaint();
        
        LOG.debug("Layout validation completed");

        // Track latest user message component and align newest to top if near
        if (!chatHistory.isEmpty()) {
            ChatMessage last = chatHistory.get(chatHistory.size() - 1);
            if (last != null && last.isFromUser()) {
                // The last added component corresponds to the last message
                int componentIndex = messageContainer.getComponentCount() - 1; // includes glue; adjust search
                // Find the last MessageComponent in reverse
                for (int i = messageContainer.getComponentCount() - 1; i >= 0; i--) {
                    Component c = messageContainer.getComponent(i);
                    if (c instanceof MessageComponent) {
                        latestUserMessageComponent = (JComponent) c;
                        scrollHelper.setLatestUserMessageComponent(latestUserMessageComponent);
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Latest user message set - index: " + i + ", y: " +
                                latestUserMessageComponent.getY() + ", prefH: " +
                                latestUserMessageComponent.getPreferredSize().height + ", count: " +
                                messageContainer.getComponentCount());
                        }
                        break;
                    }
                }
            }
        }

        // For user sends: align newest to top only if the viewport is already near the target
	        if (message != null && message.isFromUser() && latestUserMessageComponent != null) {
            ApplicationManager.getApplication().invokeLater(() -> {
                try {
                    javax.swing.Timer settleTimer = new javax.swing.Timer(scrollHelper.getLayoutSettleDelayMs(), evt -> {
                        try {
                            // Ensure the spacer reflects the latest layout so target is reachable
                            scrollHelper.recomputeBottomSpacer();
                            int target = scrollHelper.computeAlignTopTarget(chatScrollPane, latestUserMessageComponent);
                            boolean nearTarget = scrollHelper.isNearTarget(chatScrollPane, target, scrollHelper.getNearTargetThresholdPx());
                            boolean allowAlign = nearTarget || scrollHelper.isWasNearBottomBeforeUserSend();
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("User send align - allow: " + allowAlign + ", start: " + 
                                         chatScrollPane.getVerticalScrollBar().getValue() + ", target: " + target + 
                                         ", nearTarget: " + nearTarget + ", wasNearBottom: " + scrollHelper.isWasNearBottomBeforeUserSend());
                            }
                            if (allowAlign) {
                                // Smoothly scroll to align the latest user row at the top edge
                                scrollHelper.scrollToComponentTopSmooth(chatScrollPane, latestUserMessageComponent, scrollHelper.getSmoothScrollDurationMs());
                            } else {
                                // Do not yank; show the chip instead
                                scrollHelper.showNewMessagesChip();
                            }
                        } catch (Exception ignore) {
                        } finally {
                            scrollHelper.setWasNearBottomBeforeUserSend(false);
                        }
                    });
                    settleTimer.setRepeats(false);
                    settleTimer.start();
                } catch (Exception ignore) {
                }
            });
        } else {
            // For non-user (AI) messages, maintain alignment if it was near before append,
            // otherwise conditionally align-if-near after a brief settle.
            ApplicationManager.getApplication().invokeLater(() -> {
                try {
                    javax.swing.Timer settleTimer = new javax.swing.Timer(scrollHelper.getLayoutSettleDelayMs(), evt -> {
                        try {
                            scrollHelper.recomputeBottomSpacer();
                            if (latestUserMessageComponent != null) {
                                int target = scrollHelper.computeAlignTopTarget(chatScrollPane, latestUserMessageComponent);
                                boolean stillNear = scrollHelper.isNearTarget(chatScrollPane, target, scrollHelper.getNearTargetThresholdPx());
                                if (LOG.isDebugEnabled()) {
                                    LOG.debug("AI append - maintain: " + scrollHelper.isMaintainAlignAfterAppend() + ", value: " + 
                                             chatScrollPane.getVerticalScrollBar().getValue() + ", target: " + target + 
                                             ", stillNear: " + stillNear);
                                }
                                if (scrollHelper.isMaintainAlignAfterAppend() || stillNear) {
                                    // Snap immediately to keep user's message top-aligned; no animation
                                    scrollHelper.alignTopImmediate(chatScrollPane, latestUserMessageComponent);
                                } else {
                                    scrollHelper.showNewMessagesChip();
                                }
                            }
                        } catch (Exception ignore) {
                        }
                        finally {
                            scrollHelper.setMaintainAlignAfterAppend(false);
                        }
                    });
                    settleTimer.setRepeats(false);
                    settleTimer.start();
                } catch (Exception ignore) {
                }
            });
        }
    }

    // ===== Typing indicator helpers =====
    private void showTypingIndicator() {
        if (!SwingUtilities.isEventDispatchThread()) {
            ApplicationManager.getApplication().invokeLater(this::showTypingIndicator);
            return;
        }
        try {
            if (!typingIndicatorVisible) {
                typingIndicatorVisible = true;
                if (typingIndicatorRow == null) {
                    typingIndicatorRow = new TypingIndicatorRow();
                }
                // Insert before bottom spacer if currently built
                int count = messageContainer.getComponentCount();
                if (count > 0 && bottomSpacer != null) {
                    int idx = -1;
                    for (int i = 0; i < count; i++) {
                        if (messageContainer.getComponent(i) == bottomSpacer) {
                            idx = i;
                            break;
                        }
                    }
                    if (idx >= 0) {
                        // Add spacing before indicator when preceding component is a message
                        if (idx > 0) {
                            Component prev = messageContainer.getComponent(idx - 1);
                            if (!(prev instanceof Box.Filler)) {
                                messageContainer.add(Box.createVerticalStrut(16), idx++);
                            }
                        }
                        messageContainer.add(typingIndicatorRow, idx);
                    } else {
                        // Fallback: rebuild to include indicator
                        addMessageToUI(chatHistory.isEmpty() ? null : chatHistory.get(chatHistory.size() - 1));
                    }
                } else {
                    // Fallback: rebuild to include indicator
                    addMessageToUI(chatHistory.isEmpty() ? null : chatHistory.get(chatHistory.size() - 1));
                }
                messageContainer.revalidate();
                messageContainer.repaint();
                ApplicationManager.getApplication().invokeLater(() -> scrollHelper.requestAlignNewestIfNear(chatScrollPane));
            }
        } catch (Exception ignore) {
        }
    }

    private void hideTypingIndicator() {
        if (!SwingUtilities.isEventDispatchThread()) {
            ApplicationManager.getApplication().invokeLater(this::hideTypingIndicator);
            return;
        }
        try {
            if (typingIndicatorVisible) {
                typingIndicatorVisible = false;
                if (typingIndicatorRow != null) {
                    try {
                        typingIndicatorRow.stopAnimation();
                    } catch (Exception ignore) {}
                    Container parent = typingIndicatorRow.getParent();
                    if (parent != null) {
                        parent.remove(typingIndicatorRow);
                    }
                }
                typingIndicatorRow = null;
                messageContainer.revalidate();
                messageContainer.repaint();
            }
        } catch (Exception ignore) {
        }
    }

    /**
     * Updates the panel with new failure information.
     * Clears the chat and generates an initial AI prompt for the failure.
     *
     * @param failureInfo The failure information to analyze
     * @throws IllegalArgumentException if failureInfo is null
     */
    public boolean updateFailure(FailureInfo failureInfo) {
        if (failureInfo == null) {
            throw new IllegalArgumentException("FailureInfo cannot be null");
        }
        
        LOG.info("Updating failure info: " + failureInfo.getScenarioName() + " - " + 
                failureInfo.getFailedStepText().substring(0, Math.min(failureInfo.getFailedStepText().length(), 50)) + "...");
        
        // Ensure we're on the EDT
        if (!SwingUtilities.isEventDispatchThread()) {
            final boolean[] result = {false};
            try {
                ApplicationManager.getApplication().invokeAndWait(() -> result[0] = updateFailure(failureInfo));
            } catch (Exception e) {
                LOG.error("Error during EDT execution", e);
            }
            return result[0];
        }
        
        // Check if we already have a failure in this test run
        if (currentTestRunId != null) {
            // Same test run - ignore silently
            LOG.info("Ignoring subsequent failure in test run: " + currentTestRunId);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failure details - Scenario: " + failureInfo.getScenarioName() + 
                         ", Failed Step: " + failureInfo.getFailedStepText());
            }
            return false;
        }
        
        // First failure of this test run - analyze
        currentTestRunId = "test_run_" + System.currentTimeMillis();
        LOG.info("Processing first failure in new test run: " + currentTestRunId);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Failure details - Scenario: " + failureInfo.getScenarioName() + 
                     ", Failed Step: " + failureInfo.getFailedStepText());
        }
        
        // Store failure context for future user queries
        this.currentFailureInfo = failureInfo;
        aiAnalysisOrchestrator.storeFailureContext(failureInfo);
        
        clearChat();
        generateAndDisplayPrompt(failureInfo);
        return true;
    }

    /**
     * Clears the chat history and UI.
     * Removes all messages and resets the message container.
     */
    private void clearChat() {
        LOG.debug("Clearing chat history and UI");
        chatHistory.clear();
        messageContainer.removeAll();
        if (bottomSpacer == null) {
            bottomSpacer = new JPanel();
            bottomSpacer.setOpaque(false);
        }
        bottomSpacer.setPreferredSize(new Dimension(1, 0));
        messageContainer.add(bottomSpacer);
        latestUserMessageComponent = null;
        messageContainer.revalidate();
        messageContainer.repaint();
    }

    /**
     * Generates and displays the initial prompt for the failure.
     * Creates an AI message with the generated prompt and failure information.
     *
     * @param failureInfo The failure information to generate a prompt for
     */
    private void generateAndDisplayPrompt(FailureInfo failureInfo) {
        // Check if TRACE is enabled (power button) - if not, do nothing at all
        AISettings aiSettings = AISettings.getInstance();
        if (!aiSettings.isTraceEnabled()) {
            LOG.info("TRACE is disabled - skipping prompt generation");
            return; // Complete silence - no messages from TRACE
        }
        
        try {
            LOG.debug("Generating " + currentAnalysisMode.toLowerCase() + " analysis for failure");
            
            // Check if AI analysis is enabled
            if (aiSettings.isTraceEnabled()) {
                LOG.info("Using enhanced analysis with RAG (AI enabled)");
                
                // Show the test failure context immediately (scenario name and failed step)
                // Also attach a placeholder so the "Show AI Thinking" section is visible right away
                addMessage(new ChatMessage(
                    ChatMessage.Role.AI,
                    "",
                    System.currentTimeMillis(),
                    "Preparing analysis prompt...",
                    failureInfo
                ));

                // Populate the AI thinking section with the base prompt before document retrieval
                try {
                    final String basePrompt = ANALYSIS_MODE_OVERVIEW.equals(currentAnalysisMode)
                        ? aiAnalysisOrchestrator.getInitialOrchestrator().generateSummaryPrompt(failureInfo)
                        : aiAnalysisOrchestrator.getInitialOrchestrator().generateDetailedPrompt(failureInfo);

                    if (!chatHistory.isEmpty()) {
                        ChatMessage firstMessage = chatHistory.get(0);
                        ChatMessage updatedWithBasePrompt = new ChatMessage(
                            firstMessage.getRole(),
                            firstMessage.getText(),
                            firstMessage.getTimestamp(),
                            basePrompt,
                            firstMessage.getFailureInfo()
                        );
                        chatHistory.set(0, updatedWithBasePrompt);

                        // Refresh UI to reflect the base prompt in "Show AI Thinking"
                        messageContainer.removeAll();
                        for (ChatMessage message : chatHistory) {
                            addMessageToUI(message);
                        }
                        ApplicationManager.getApplication().invokeLater(() -> scrollHelper.requestAlignNewestIfNear(chatScrollPane));
                    }
                } catch (Exception promptBuildError) {
                    LOG.error("Failed to build base prompt prior to document retrieval: " + promptBuildError.getMessage());
                }
                
                // Show typing indicator while waiting for AI analysis
                showTypingIndicator();

                // Use enhanced analysis with RAG for both overview and full analysis modes
                CompletableFuture<AIAnalysisResult> analysisFuture = 
                    aiAnalysisOrchestrator.analyzeInitialFailureWithDocuments(
                        failureInfo,
                        ANALYSIS_MODE_OVERVIEW.equals(currentAnalysisMode) ? AnalysisMode.OVERVIEW : AnalysisMode.FULL
                    );
                
                // Handle the analysis result
                analysisFuture.thenAccept(result -> {
                    // Remove typing indicator on first content arrival
                    hideTypingIndicator();
                    if (result != null && result.getAnalysis() != null && !result.getAnalysis().trim().isEmpty()) {
                        // Update the first message with the enhanced prompt in "Show AI thinking"
                        String aiThinking = result.hasPrompt() ? result.getPrompt() : null;
                        if (aiThinking != null && !chatHistory.isEmpty()) {
                            // Update the first message (failure context) with the AI thinking content
                            ChatMessage firstMessage = chatHistory.get(0);
                            ChatMessage updatedMessage = new ChatMessage(
                                firstMessage.getRole(),
                                firstMessage.getText(),
                                firstMessage.getTimestamp(),
                                aiThinking,
                                firstMessage.getFailureInfo()
                            );
                            chatHistory.set(0, updatedMessage);
                            
                            // Update the UI
                            messageContainer.removeAll();
                            for (ChatMessage message : chatHistory) {
                                addMessageToUI(message);
                            }
                            ApplicationManager.getApplication().invokeLater(() -> scrollHelper.requestAlignNewestIfNear(chatScrollPane));
                        }
                        
                        // Create a separate AI message with just the analysis result
                        addMessage(new ChatMessage(ChatMessage.Role.AI, result.getAnalysis(),
                                                System.currentTimeMillis(), null, null));
                    } else {
                        addMessage(new ChatMessage(ChatMessage.Role.AI, 
                            "AI analysis completed but returned no content.", 
                            System.currentTimeMillis(), null, null));
                    }
                }).exceptionally(throwable -> {
                    hideTypingIndicator();
                    LOG.error("Error during enhanced failure analysis: " + throwable.getMessage(), throwable);
                    String errorMessage = ERROR_GENERATING_PROMPT_PREFIX + throwable.getMessage();
                    addMessage(new ChatMessage(ChatMessage.Role.AI, errorMessage, 
                                            System.currentTimeMillis(), null, null));
                    return null;
                });
                
            } else {
                LOG.info("Using basic prompt generation (AI disabled)");
                
                // Show the test failure context immediately (scenario name and failed step)
                addMessage(new ChatMessage(ChatMessage.Role.AI, "", System.currentTimeMillis(), null, failureInfo));
                
                // Generate basic prompt for display only (no AI analysis)
                String prompt;
                if (ANALYSIS_MODE_OVERVIEW.equals(currentAnalysisMode)) {
                    prompt = aiAnalysisOrchestrator.getInitialOrchestrator().generateSummaryPrompt(failureInfo);
                } else {
                    prompt = aiAnalysisOrchestrator.getInitialOrchestrator().generateDetailedPrompt(failureInfo);
                }
                
                // Update the first message with the prompt in "Show AI thinking" section
                if (!chatHistory.isEmpty()) {
                    ChatMessage firstMessage = chatHistory.get(0);
                    ChatMessage updatedMessage = new ChatMessage(
                        firstMessage.getRole(),
                        firstMessage.getText(),
                        firstMessage.getTimestamp(),
                        prompt,
                        firstMessage.getFailureInfo()
                    );
                    chatHistory.set(0, updatedMessage);
                    
                    // Update the UI
                    messageContainer.removeAll();
                    for (ChatMessage message : chatHistory) {
                        addMessageToUI(message);
                    }
                    ApplicationManager.getApplication().invokeLater(() -> scrollHelper.requestAlignNewestIfNear(chatScrollPane));
                }
                
                // No AI analysis - just show the prompt
                addMessage(new ChatMessage(ChatMessage.Role.AI, 
                    "AI analysis is disabled. Review the prompt above for manual analysis.", 
                    System.currentTimeMillis(), null, null));
            }
            
        } catch (Exception e) {
            LOG.error("Error generating prompt: " + e.getMessage(), e);
            String errorMessage = ERROR_GENERATING_PROMPT_PREFIX + e.getMessage();
            addMessage(new ChatMessage(ChatMessage.Role.AI, errorMessage, System.currentTimeMillis(), null, failureInfo));
        }
    }
    
    /**
     * Displays an AI analysis result in the chat interface.
     * This method is called when AI analysis completes successfully.
     * 
     * NOTE: This method is now deprecated in favor of direct orchestrator integration.
     * The TriagePanelView now handles analysis results directly through the orchestrator.
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
        
        LOG.info("Displaying AI analysis result via legacy method");
        
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
     * Gets the last 3 user queries from chat history for logging purposes.
     * This helps verify that the sliding window context is working correctly.
     *
     * @return List of the last 3 user queries, or empty list if none available
     */
    private List<String> getLastThreeUserQueries() {
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
        themeHelper.manualThemeRefresh();
    }




    
    
    
    




    /**
     * Refreshes the main panel when switching tabs.
     * Rebuilds the panel layout based on the current tab state.
     */
    private void refreshMainPanel() {
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
            clearChat();
        });
        
        settingsButton2.addActionListener(e -> {
            LOG.debug("Settings button clicked");
            showSettingsTab = !showSettingsTab;
            refreshMainPanel();
        });
        
        mainPanel.add(UIComponentHelper.createCustomHeaderPanel(aiToggleButton2, clearChatButton2, settingsButton2), BorderLayout.NORTH);
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
        
        mainPanel.revalidate();
        mainPanel.repaint();
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

 