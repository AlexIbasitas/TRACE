package com.trace.chat.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import com.trace.ai.configuration.AISettings;
import com.trace.ai.models.AIAnalysisResult;
import com.trace.ai.services.AIAnalysisOrchestrator;
import com.trace.ai.services.ChatHistoryService;
import com.trace.ai.services.AnalysisMode;
import com.trace.ai.prompts.InitialPromptFailureAnalysisService;
import com.trace.ai.ui.SettingsPanel;
import com.trace.chat.components.ChatMessage;
import com.trace.chat.components.MessageComponent;
import com.trace.common.constants.TriagePanelConstants;
import com.trace.test.models.FailureInfo;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.awt.event.ActionListener;
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
    private final JBTextArea inputArea;
    private final JButton sendButton;
    private final JBLabel headerLabel;
    private final JBLabel statusLabel;
    
    // Chat state management
    private final List<ChatMessage> chatHistory;
    private final AIAnalysisOrchestrator aiAnalysisOrchestrator;
    private FailureInfo currentFailureInfo;
    private JScrollPane chatScrollPane;
    private JPanel messageContainer;
    private boolean showSettingsTab = false;
    
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
        
        // Initialize UI components
        this.mainPanel = new JPanel(new BorderLayout());
        this.inputPanel = new JPanel(new BorderLayout());
        this.inputArea = new JBTextArea();
        this.sendButton = new JButton("Send");
        this.headerLabel = new JBLabel("No test failure detected");
        this.statusLabel = new JBLabel("");
        
        initializeUI();
        setupEventHandlers();
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
        updateAnalysisModeButtonTextInUI();
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
            LOG.warn("Error clearing chat history: " + e.getMessage());
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
     * Updates the analysis mode button text in the UI.
     * This method searches for the analysis mode button and updates its text.
     */
    private void updateAnalysisModeButtonTextInUI() {
        // Find the analysis mode button in the component hierarchy
        if (mainPanel != null) {
            findAndUpdateAnalysisModeButton(mainPanel);
        }
    }
    
    /**
     * Recursively searches for the analysis mode button and updates its text.
     *
     * @param component The component to search in
     */
    private void findAndUpdateAnalysisModeButton(Container component) {
        for (Component child : component.getComponents()) {
            if (child instanceof JButton) {
                JButton button = (JButton) child;
                // Check if this is the analysis mode button by looking at its tooltip
                if ("Click to switch between Quick Overview and Full Analysis modes".equals(button.getToolTipText())) {
                    updateAnalysisModeButtonText(button);
                    return;
                }
            }
            if (child instanceof Container) {
                findAndUpdateAnalysisModeButton((Container) child);
            }
        }
    }

    /**
     * Initializes the UI components with proper chat interface layout.
     * Sets up the main panel, chat area, and input components.
     */
    private void initializeUI() {
        setupChatPanel();
        setupInputPanel();
        
        Color panelBg = UIManager.getColor("Panel.background");
        if (panelBg == null) panelBg = new Color(43, 43, 43);
        
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(panelBg);
        mainPanel.setOpaque(true);
        
        // Use custom header and tab logic
        mainPanel.add(createCustomHeaderPanel(), BorderLayout.NORTH);
        if (showSettingsTab) {
            mainPanel.add(createSettingsPanel(), BorderLayout.CENTER);
        } else {
            mainPanel.add(chatScrollPane, BorderLayout.CENTER);
        mainPanel.add(inputPanel, BorderLayout.SOUTH);
        }
    }

    /**
     * Sets up the chat panel with proper layout management.
     * Creates the message container and scroll pane for displaying chat messages.
     */
    private void setupChatPanel() {
        // Create message container with proper layout
        messageContainer = new JPanel();
        messageContainer.setLayout(new BoxLayout(messageContainer, BoxLayout.Y_AXIS));
        messageContainer.setOpaque(false);
        
        // Get theme-aware background color
        Color panelBg = UIManager.getColor("Panel.background");
        if (panelBg == null) {
            panelBg = new Color(43, 43, 43);
        }
        
        messageContainer.setBackground(panelBg);
        messageContainer.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        
        // Create scroll pane with proper configuration
        chatScrollPane = new JScrollPane(messageContainer);
        chatScrollPane.setBorder(BorderFactory.createEmptyBorder());
        chatScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        chatScrollPane.setBackground(panelBg);
        chatScrollPane.getViewport().setBackground(panelBg);
        
        // Add initial vertical glue to push messages to top
        messageContainer.add(Box.createVerticalGlue());
    }

    /**
     * Sets up the input panel with modern styling.
     * Configures the text input area and send button with proper layout and styling.
     */
    private void setupInputPanel() {
        LOG.info("=== SETUP INPUT PANEL - SIMPLE APPROACH ===");
        
        inputPanel.setBorder(BorderFactory.createEmptyBorder(8, 16, 16, 16));
        
        // Create analysis mode button - SIMPLE LEFT ALIGNMENT
        JButton analysisModeButton = createAnalysisModeButton();
        
        // Create input container with rounded border
        JPanel inputBoxContainer = new JPanel(new BorderLayout());
        inputBoxContainer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 60, 60), 1, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 0)
        ));
        inputBoxContainer.setBackground(new Color(50, 50, 50));
        inputBoxContainer.setOpaque(true);
        
        // Configure text area for multi-line input
        inputArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputArea.setRows(3);
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        inputArea.setBackground(new Color(50, 50, 50));
        inputArea.setForeground(Color.WHITE);
        inputArea.setCaretColor(Color.WHITE);
        inputArea.setOpaque(false);
        inputArea.putClientProperty("JTextField.placeholderText", TriagePanelConstants.INPUT_PLACEHOLDER_TEXT);
        
        // Create modern send button with custom icon
        JButton sendIconButton = createModernSendButton();
        
        // Create a fixed-size container for the send button with vertical centering
        JPanel buttonContainer = new JPanel();
        buttonContainer.setLayout(new BoxLayout(buttonContainer, BoxLayout.Y_AXIS));
        buttonContainer.setOpaque(false);
        buttonContainer.setPreferredSize(new Dimension(40, 40));
        buttonContainer.setMaximumSize(new Dimension(40, 40));
        buttonContainer.setMinimumSize(new Dimension(40, 40));
        buttonContainer.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        
        // Add vertical glue to center the button
        buttonContainer.add(Box.createVerticalGlue());
        buttonContainer.add(sendIconButton);
        buttonContainer.add(Box.createVerticalGlue());
        
        // Add components to input box container
        inputBoxContainer.add(inputArea, BorderLayout.CENTER);
        inputBoxContainer.add(buttonContainer, BorderLayout.EAST);
        
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
        LOG.info("=== LAYOUT INVESTIGATION ===");
        LOG.info("Input panel border: " + inputPanel.getBorder());
        LOG.info("Input panel insets: " + inputPanel.getInsets());
        LOG.info("Toggle container preferred size: " + toggleContainer.getPreferredSize());
        LOG.info("Input box container preferred size: " + inputBoxContainer.getPreferredSize());
        LOG.info("Analysis mode button border: " + analysisModeButton.getBorder());
        LOG.info("Input box container border: " + inputBoxContainer.getBorder());
        
        LOG.info("=== SIMPLE SETUP COMPLETE ===");
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
        
        // Add user message to chat
        addMessage(new ChatMessage(ChatMessage.Role.USER, messageText, System.currentTimeMillis(), null, null));
        
        // Clear input area
        inputArea.setText("");
        
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
        LOG.info("=== HANDLING USER MESSAGE WITH AI ===");
        LOG.info("User Message: " + messageText);
        
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
            LOG.warn("No failure context available for user query");
            addMessage(new ChatMessage(ChatMessage.Role.AI, 
                "No test failure context available. Please run a test first to establish context for AI analysis.", 
                System.currentTimeMillis(), null, null));
            return;
        }
        
        LOG.info("Failure Context Available:");
        LOG.info("  - Scenario Name: " + currentFailureInfo.getScenarioName());
        LOG.info("  - Failed Step: " + (currentFailureInfo.getFailedStepText() != null ? 
            currentFailureInfo.getFailedStepText().substring(0, Math.min(currentFailureInfo.getFailedStepText().length(), 50)) + "..." : "null"));
        LOG.info("  - Error Message: " + (currentFailureInfo.getErrorMessage() != null ? 
            currentFailureInfo.getErrorMessage().substring(0, Math.min(currentFailureInfo.getErrorMessage().length(), 50)) + "..." : "null"));
        
        // Check chat history service availability and log recent queries
        try {
            ChatHistoryService chatHistoryService = project.getService(ChatHistoryService.class);
            if (chatHistoryService == null) {
                LOG.warn("ChatHistoryService is null - proceeding without chat history");
            } else {
                LOG.info("Chat History Service State:");
                LOG.info("  - User Query Count: " + chatHistoryService.getUserQueryCount());
                LOG.info("  - Has Failure Context: " + chatHistoryService.hasFailureContext());
                LOG.info("  - Window Size: " + chatHistoryService.getUserMessageWindowSize());
                
                // Log recent user queries for context verification
                List<String> recentQueries = getLastThreeUserQueries();
                if (!recentQueries.isEmpty()) {
                    LOG.info("Recent User Queries Context:");
                    for (String query : recentQueries) {
                        LOG.info("  - " + query);
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn("Error accessing ChatHistoryService: " + e.getMessage());
        }
        
        // Process the user query with enhanced analysis and RAG
        LOG.info("Calling aiAnalysisOrchestrator.analyzeUserQueryWithDocuments()");
        CompletableFuture<AIAnalysisResult> analysisFuture = 
            aiAnalysisOrchestrator.analyzeUserQueryWithDocuments(currentFailureInfo, messageText);
        
        // Handle the analysis result
        analysisFuture.thenAccept(result -> {
            LOG.info("=== USER QUERY ANALYSIS RESULT RECEIVED ===");
            if (result != null) {
                LOG.info("AI Analysis Result:");
                LOG.info("  - Service Type: " + result.getServiceType());
                LOG.info("  - Timestamp: " + result.getTimestamp());
                LOG.info("  - Analysis Length: " + (result.getAnalysis() != null ? result.getAnalysis().length() : 0) + " characters");
                LOG.info("  - Has Prompt: " + result.hasPrompt());
                
                if (result.getAnalysis() != null && !result.getAnalysis().trim().isEmpty()) {
                    LOG.info("Adding AI response to chat");
                    // Do not attach Show AI thinking for user query responses
                    addMessage(new ChatMessage(ChatMessage.Role.AI, result.getAnalysis(), 
                                            System.currentTimeMillis(), null, null));
                } else {
                    LOG.warn("AI analysis returned empty content");
                    addMessage(new ChatMessage(ChatMessage.Role.AI, 
                        "AI analysis completed but returned no content.", 
                        System.currentTimeMillis(), null, null));
                }
            } else {
                LOG.warn("AI analysis returned null result");
                addMessage(new ChatMessage(ChatMessage.Role.AI, 
                    "AI analysis returned no result.", 
                    System.currentTimeMillis(), null, null));
            }
        }).exceptionally(throwable -> {
            LOG.error("=== USER QUERY ANALYSIS FAILED ===");
            LOG.error("Error during AI analysis: " + throwable.getMessage(), throwable);
            String errorMessage = "AI analysis failed: " + throwable.getMessage();
            addMessage(new ChatMessage(ChatMessage.Role.AI, errorMessage, 
                                    System.currentTimeMillis(), null, null));
            return null;
        });
        
        LOG.info("=== USER MESSAGE HANDLING COMPLETED ===");
    }

    /**
     * Adds a message to the chat history and updates the UI with proper EDT compliance.
     * Ensures thread safety by dispatching UI updates to the Event Dispatch Thread.
     *
     * @param message The chat message to add
     */
    private void addMessage(ChatMessage message) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> addMessage(message));
            return;
        }
        
        LOG.debug("Adding message to chat history: " + message.getRole() + " - " + 
                 message.getText().substring(0, Math.min(message.getText().length(), 30)) + "...");
        chatHistory.add(message);
        addMessageToUI(message);
    }

    /**
     * Adds a message component to the UI with proper layout.
     * Recreates the message container with all messages and proper spacing.
     *
     * @param message The message to add to the UI
     */
    private void addMessageToUI(ChatMessage message) {
        // Remove the vertical glue temporarily
        messageContainer.removeAll();
        
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
        
        // Add vertical glue to push messages to top
        messageContainer.add(Box.createVerticalGlue());
        
        // Revalidate and repaint
        messageContainer.revalidate();
        messageContainer.repaint();
        
        // Scroll to bottom with a slight delay to ensure layout is complete
        SwingUtilities.invokeLater(this::scrollToBottom);
    }

    /**
     * Scrolls the chat to the bottom to show the latest messages.
     * Uses EDT to ensure thread safety.
     * Optimized to avoid unnecessary scroll operations.
     */
    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            if (chatScrollPane != null) {
                JScrollBar verticalBar = chatScrollPane.getVerticalScrollBar();
                int currentValue = verticalBar.getValue();
                int maxValue = verticalBar.getMaximum();
                
                // Only scroll if we're not already at the bottom
                if (currentValue < maxValue) {
                    verticalBar.setValue(maxValue);
                    LOG.debug("Scrolled chat to bottom");
                } else {
                    LOG.debug("Already at bottom - no scroll needed");
                }
            } else {
                LOG.warn("Chat scroll pane is null - cannot scroll to bottom");
            }
        });
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
                SwingUtilities.invokeAndWait(() -> result[0] = updateFailure(failureInfo));
            } catch (InterruptedException e) {
                LOG.error("Interrupted while waiting for EDT execution", e);
                Thread.currentThread().interrupt();
            } catch (java.lang.reflect.InvocationTargetException e) {
                LOG.error("Error during EDT execution", e);
            }
            return result[0];
        }
        
        // Check if we already have a failure in this test run
        if (currentTestRunId != null) {
            // Same test run - ignore silently
            LOG.info("=== TRIAGE PANEL: IGNORING SUBSEQUENT FAILURE ===");
            LOG.info("Current Test Run ID: " + currentTestRunId);
            LOG.info("Failure: " + failureInfo.getScenarioName());
            LOG.info("Failed Step: " + failureInfo.getFailedStepText());
            LOG.info("=== END TRIAGE PANEL: IGNORING SUBSEQUENT FAILURE ===");
            return false;
        }
        
        // First failure of this test run - analyze
        currentTestRunId = "test_run_" + System.currentTimeMillis();
        LOG.info("=== TRIAGE PANEL: PROCESSING FIRST FAILURE ===");
        LOG.info("New Test Run ID: " + currentTestRunId);
        LOG.info("Failure: " + failureInfo.getScenarioName());
        LOG.info("Failed Step: " + failureInfo.getFailedStepText());
        LOG.info("=== END TRIAGE PANEL: PROCESSING FIRST FAILURE ===");
        
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
        messageContainer.add(Box.createVerticalGlue());
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
            LOG.info("TRACE is disabled (power off) - skipping prompt generation");
            return; // Complete silence - no messages from TRACE
        }
        
        try {
            LOG.debug("Generating " + currentAnalysisMode.toLowerCase() + " analysis for failure");
            
            // Check if AI analysis is enabled
            if (aiSettings.isTraceEnabled()) {
                LOG.info("=== DEBUG: Using enhanced analysis with RAG (AI enabled) ===");
                
                // Show the test failure context immediately (scenario name and failed step)
                addMessage(new ChatMessage(ChatMessage.Role.AI, "", System.currentTimeMillis(), null, failureInfo));
                
                // Use enhanced analysis with RAG for both overview and full analysis modes
                CompletableFuture<AIAnalysisResult> analysisFuture = 
                    aiAnalysisOrchestrator.analyzeInitialFailureWithDocuments(
                        failureInfo,
                        ANALYSIS_MODE_OVERVIEW.equals(currentAnalysisMode) ? AnalysisMode.OVERVIEW : AnalysisMode.FULL
                    );
                
                // Handle the analysis result
                analysisFuture.thenAccept(result -> {
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
                            messageContainer.add(Box.createVerticalGlue());
                            scrollToBottom();
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
                    LOG.error("Error during enhanced failure analysis: " + throwable.getMessage(), throwable);
                    String errorMessage = ERROR_GENERATING_PROMPT_PREFIX + throwable.getMessage();
                    addMessage(new ChatMessage(ChatMessage.Role.AI, errorMessage, 
                                            System.currentTimeMillis(), null, null));
                    return null;
                });
                
            } else {
                LOG.info("=== DEBUG: Using basic prompt generation (AI disabled) ===");
                
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
                    messageContainer.add(Box.createVerticalGlue());
                    scrollToBottom();
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
            LOG.info("TRACE is disabled (power off) - skipping AI analysis result display");
            return; // Complete silence - no messages from TRACE
        }
        
        if (result == null) {
            LOG.warn("AI analysis result is null, cannot display");
            return;
        }
        
        LOG.info("Displaying AI analysis result via legacy method");
        
        // Ensure we're on the EDT
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> displayAIAnalysisResult(result));
            return;
        }
        
        try {
            // Create an AI message with the analysis result
            String analysisText = result.getAnalysis();
            if (analysisText != null && !analysisText.trim().isEmpty()) {
                addMessage(new ChatMessage(ChatMessage.Role.AI, analysisText, System.currentTimeMillis(), null, null));
            } else {
                LOG.warn("AI analysis result is empty");
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
            LOG.info("TRACE is disabled (power off) - skipping AI analysis error display");
            return; // Complete silence - no messages from TRACE
        }
        
        if (errorMessage == null || errorMessage.trim().isEmpty()) {
            LOG.warn("AI analysis error message is null or empty");
            return;
        }
        
        LOG.info("Displaying AI analysis error: " + errorMessage);
        
        // Ensure we're on the EDT
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> displayAIAnalysisError(errorMessage));
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
            LOG.warn("Error accessing chat history for recent queries: " + e.getMessage());
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
     * Creates the custom header panel with logo and scenario information.
     *
     * @return The configured header panel
     */
    private JPanel createCustomHeaderPanel() {
        Color darkBg = UIManager.getColor("Panel.background");
        if (darkBg == null) {
            darkBg = new Color(43, 43, 43);
        }
        
        LOG.info("Creating custom header panel with ultra-compact layout");
        
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(darkBg);
        
        // Minimal header padding
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(68, 68, 68)),
            BorderFactory.createEmptyBorder(6, 8, 6, 8) // Increased from (2, 4, 2, 4) for better visual balance
        ));
        
        // Create left side with BoxLayout for precise control
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.X_AXIS));
        leftPanel.setOpaque(false);
        
        LOG.info("Creating AI toggle button for header");
        // Create AI toggle button
        JButton aiToggleButton = createAIToggleButton();
        aiToggleButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(aiToggleButton);
        
        // Add minimal spacing between button and text
        leftPanel.add(Box.createHorizontalStrut(2));
        
        LOG.info("Creating TRACE title label");
        JLabel title = new JLabel("TRACE");
        title.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        title.setForeground(new Color(180, 180, 180));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(title);
        
        LOG.info("Adding left panel to header with " + leftPanel.getComponentCount() + " components");
        header.add(leftPanel, BorderLayout.WEST);
        
        // Log component details for debugging
        LOG.info("AI toggle button preferred size: " + aiToggleButton.getPreferredSize());
        LOG.info("TRACE label preferred size: " + title.getPreferredSize());
        LOG.info("Left panel preferred size: " + leftPanel.getPreferredSize());
        
        // Create right panel with BoxLayout for tight packing
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.X_AXIS));
        rightPanel.setOpaque(false);
        
        // Trash button for clear chat
        JButton clearChatButton = createClearChatButton();
        clearChatButton.setAlignmentX(Component.RIGHT_ALIGNMENT);
        rightPanel.add(clearChatButton);
        
        // Minimal spacing between buttons
        rightPanel.add(Box.createHorizontalStrut(2));
        
        // Settings button
        JButton settingsButton = createSettingsButton();
        settingsButton.setAlignmentX(Component.RIGHT_ALIGNMENT);
        rightPanel.add(settingsButton);
        
        header.add(rightPanel, BorderLayout.EAST);
        
        LOG.info("Header panel created with dimensions: " + header.getPreferredSize());
        return header;
    }

    /**
     * Creates the AI toggle button with proper styling and functionality.
     *
     * @return The configured AI toggle button
     */
    private JButton createAIToggleButton() {
        JButton aiToggleButton = new JButton("⏻");
        aiToggleButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        aiToggleButton.setBorderPainted(false);
        aiToggleButton.setFocusPainted(false);
        aiToggleButton.setContentAreaFilled(false);
        aiToggleButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        // Zero padding on the toggle button
        aiToggleButton.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        
        // Set minimum and preferred sizes to be compact
        Dimension buttonSize = new Dimension(20, 20);
        aiToggleButton.setMinimumSize(buttonSize);
        aiToggleButton.setPreferredSize(buttonSize);
        aiToggleButton.setMaximumSize(buttonSize);
        
        // Update initial appearance
        updateAIToggleButtonAppearance(aiToggleButton);
        
        // Add action listener to toggle AI state
        aiToggleButton.addActionListener(e -> {
            AISettings aiSettings = AISettings.getInstance();
        boolean currentState = aiSettings.isTraceEnabled();
        aiSettings.setTraceEnabled(!currentState);
            updateAIToggleButtonAppearance(aiToggleButton);
            LOG.info("AI toggle clicked - new state: " + (!currentState));
        });
        
        return aiToggleButton;
    }
    
    /**
     * Creates the clear chat button with proper styling and functionality.
     *
     * @return The configured clear chat button
     */
    private JButton createClearChatButton() {
        JButton clearChatButton = new JButton();
        
        // Load custom red trash icon
        try {
            ImageIcon trashIcon = new ImageIcon(getClass().getResource("/icons/trash_20.png"));
            // Scale the icon down to 18x18
            Image img = trashIcon.getImage();
            Image scaledImg = img.getScaledInstance(18, 18, Image.SCALE_SMOOTH);
            clearChatButton.setIcon(new ImageIcon(scaledImg));
            clearChatButton.setText(""); // Remove text, use icon only
        } catch (Exception e) {
            LOG.warn("Could not load trash icon, falling back to text: " + e.getMessage());
            clearChatButton.setText("🗑");
        }
        
        clearChatButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        clearChatButton.setBorderPainted(false);
        clearChatButton.setFocusPainted(false);
        clearChatButton.setContentAreaFilled(false);
        clearChatButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clearChatButton.setToolTipText("Clear Chat");
        
        // Zero padding on the button
        clearChatButton.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        
        // Set minimum and preferred sizes to be compact - smaller size
        Dimension buttonSize = new Dimension(18, 18);
        clearChatButton.setMinimumSize(buttonSize);
        clearChatButton.setPreferredSize(buttonSize);
        clearChatButton.setMaximumSize(buttonSize);
        
        // Add action listener to clear chat
        clearChatButton.addActionListener(e -> {
            LOG.info("Clear chat button clicked");
            clearChat();
        });
        
        return clearChatButton;
    }
    
    /**
     * Creates the settings button with proper styling and functionality.
     *
     * @return The configured settings button
     */
    private JButton createSettingsButton() {
        JButton settingsButton = new JButton("⚙");
        settingsButton.setFont(new Font("Segoe UI", Font.PLAIN, 16)); // Increased from 14 to 16
        settingsButton.setForeground(new Color(180, 180, 180));
        settingsButton.setBorderPainted(false);
        settingsButton.setFocusPainted(false);
        settingsButton.setContentAreaFilled(false);
        settingsButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        settingsButton.setToolTipText("Settings");
        
        // Zero padding on the button
        settingsButton.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        
        // Set minimum and preferred sizes to be compact - matching size
        Dimension buttonSize = new Dimension(20, 20);
        settingsButton.setMinimumSize(buttonSize);
        settingsButton.setPreferredSize(buttonSize);
        settingsButton.setMaximumSize(buttonSize);
        
        // Add action listener to toggle settings
        settingsButton.addActionListener(e -> {
            LOG.info("Settings button clicked");
            showSettingsTab = !showSettingsTab;
            refreshMainPanel();
        });
        
        return settingsButton;
    }
    
    /**
     * Creates the analysis mode button with proper styling and functionality.
     * Shows "Quick Overview | Full Analysis" with the selected option in bold.
     *
     * @return The configured analysis mode button
     */
    private JButton createAnalysisModeButton() {
        LOG.info("=== CREATING ANALYSIS MODE BUTTON ===");
        
        JButton analysisModeButton = new JButton();
        
        // Minimal styling - reverted to original approach
        analysisModeButton.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        analysisModeButton.setForeground(new Color(200, 200, 200)); // Lighter gray for better contrast
        analysisModeButton.setBorderPainted(false);
        analysisModeButton.setFocusPainted(false);
        analysisModeButton.setContentAreaFilled(false);
        analysisModeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        analysisModeButton.setToolTipText("Click to switch between Quick Overview and Full Analysis modes");
        
        // Minimal padding for compact appearance
        analysisModeButton.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0)); // NO PADDING - container handles alignment
        
        // Set compact size for position above input - reduced height for tighter appearance
        Dimension buttonSize = new Dimension(250, 16); // Reduced height for tighter appearance
        analysisModeButton.setMinimumSize(buttonSize);
        analysisModeButton.setPreferredSize(buttonSize);
        analysisModeButton.setMaximumSize(buttonSize);
        LOG.info("Button size set to: " + buttonSize);
        
        // Update button text based on current mode
        updateAnalysisModeButtonText(analysisModeButton);
        LOG.info("Button text set to: " + analysisModeButton.getText());
        
        // Add action listener to toggle between modes
        analysisModeButton.addActionListener(e -> {
            if (ANALYSIS_MODE_OVERVIEW.equals(currentAnalysisMode)) {
                setCurrentAnalysisMode(ANALYSIS_MODE_FULL);
            } else {
                setCurrentAnalysisMode(ANALYSIS_MODE_OVERVIEW);
            }
            updateAnalysisModeButtonText(analysisModeButton);
        });
        
        LOG.info("Analysis mode button creation complete");
        return analysisModeButton;
    }
    
    /**
     * Updates the analysis mode button text to show the current selection in bold.
     *
     * @param button The analysis mode button to update
     */
    private void updateAnalysisModeButtonText(JButton button) {
        // Create HTML text with blue color for selected option - matching send button blue
        String htmlText;
        if (ANALYSIS_MODE_OVERVIEW.equals(currentAnalysisMode)) {
            htmlText = "<html><nobr><b style='font-size: 11px; color: #1976D2;'>Quick Overview</b> <span style='font-size: 10px; color: #888;'>|</span> <span style='font-size: 10px; color: #666;'>Full Analysis</span></nobr></html>";
        } else {
            htmlText = "<html><nobr><span style='font-size: 10px; color: #666;'>Quick Overview</span> <span style='font-size: 10px; color: #888;'>|</span> <b style='font-size: 11px; color: #1976D2;'>Full Analysis</b></nobr></html>";
        }
        
        button.setText(htmlText);
    }

    /**
     * Updates the AI toggle button appearance based on the current AI state.
     *
     * @param button The toggle button to update
     */
    private void updateAIToggleButtonAppearance(JButton button) {
        AISettings aiSettings = AISettings.getInstance();
        boolean aiEnabled = aiSettings.isTraceEnabled();
        
        LOG.info("Updating TRACE toggle button appearance - TRACE enabled: " + aiEnabled);
        
        if (aiEnabled) {
            // TRACE is enabled - green color
            button.setForeground(new Color(76, 175, 80)); // Material Design Green
            button.setToolTipText("Disable TRACE");
            LOG.info("TRACE toggle button set to enabled state (green)");
        } else {
            // TRACE is disabled - gray color
            button.setForeground(new Color(158, 158, 158)); // Material Design Gray
            button.setToolTipText("Enable TRACE");
            LOG.info("TRACE toggle button set to disabled state (gray)");
        }
    }

    /**
     * Creates the settings panel using the dedicated SettingsPanel class.
     *
     * @return The configured settings panel
     */
    private JPanel createSettingsPanel() {
        AISettings aiSettings = AISettings.getInstance();
        
        // Create action listener for back to chat navigation
        ActionListener backToChatListener = e -> {
            showSettingsTab = false;
            refreshMainPanel();
        };
        
        // Create and return the dedicated settings panel
        return new SettingsPanel(aiSettings, backToChatListener);
    }

    /**
     * Creates a modern send button with custom icon and styling.
     *
     * @return The configured send button
     */
    private JButton createModernSendButton() {
        JButton sendButton = new JButton();
        
        // Load the send icon with proper exception handling
        try {
            Icon sendIcon = IconLoader.getIcon("/icons/send_32.png", getClass());
            if (sendIcon != null) {
            sendButton.setIcon(sendIcon);
            } else {
                // Fallback to text if icon not found
                sendButton.setText("→");
                sendButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
            }
        } catch (Exception e) {
            // Fallback to text if icon loading fails
            sendButton.setText("→");
            sendButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        }
        
        // Modern styling - transparent background with just icon
        sendButton.setPreferredSize(new Dimension(32, 32));
        sendButton.setMaximumSize(new Dimension(32, 32));
        sendButton.setMinimumSize(new Dimension(32, 32));
        
        // Transparent background
        sendButton.setBackground(new Color(0, 0, 0, 0));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
        sendButton.setBorderPainted(false);
        sendButton.setContentAreaFilled(false);
        sendButton.setOpaque(false);
        
        // Minimal border for icon spacing
        sendButton.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        
        // Cursor and tooltip
        sendButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        sendButton.setToolTipText("Send message");
        
        // Hover effects
        sendButton.addMouseListener(new java.awt.event.MouseAdapter() {
        @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                sendButton.setBackground(new Color(255, 255, 255, 30));
                sendButton.repaint();
        }
        
        @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                sendButton.setBackground(new Color(0, 0, 0, 0));
                sendButton.repaint();
            }
            
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                sendButton.setBackground(new Color(0, 0, 0, 20));
                sendButton.repaint();
        }
        
        @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                sendButton.setBackground(new Color(255, 255, 255, 30));
                sendButton.repaint();
            }
        });
        
        // Action listener
        sendButton.addActionListener(e -> sendMessage());
        
        return sendButton;
    }

    /**
     * Refreshes the main panel when switching tabs.
     * Rebuilds the panel layout based on the current tab state.
     */
    private void refreshMainPanel() {
        mainPanel.removeAll();
        mainPanel.setLayout(new BorderLayout());
        
        Color panelBg = UIManager.getColor("Panel.background");
        if (panelBg == null) {
            panelBg = new Color(43, 43, 43);
        }
        mainPanel.setBackground(panelBg);
        mainPanel.setOpaque(true);
        
        mainPanel.add(createCustomHeaderPanel(), BorderLayout.NORTH);
        if (showSettingsTab) {
            mainPanel.add(createSettingsPanel(), BorderLayout.CENTER);
        } else {
            mainPanel.add(chatScrollPane, BorderLayout.CENTER);
            mainPanel.add(inputPanel, BorderLayout.SOUTH);
        }
        
        mainPanel.revalidate();
        mainPanel.repaint();
    }
} 