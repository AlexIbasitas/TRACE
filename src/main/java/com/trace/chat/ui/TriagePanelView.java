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
        
        Color panelBg = ThemeUtils.panelBackground();
        
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(panelBg);
        mainPanel.setOpaque(true);
        
        // Use custom header and tab logic
        mainPanel.add(createCustomHeaderPanel(), BorderLayout.NORTH);
        if (showSettingsTab) {
            mainPanel.add(createSettingsPanel(), BorderLayout.CENTER);
        } else {
            mainPanel.add(chatOverlayPanel != null ? chatOverlayPanel : chatScrollPane, BorderLayout.CENTER);
            mainPanel.add(inputPanel, BorderLayout.SOUTH);
        }
    }

    private void refreshTheme() {
        try {
            LOG.info("Theme refresh started");
            if (LOG.isDebugEnabled()) {
                LOG.debug("Current theme: " + UIManager.getLookAndFeel().getName());
                LOG.debug("Theme colors - Panel background: " + ThemeUtils.panelBackground());
                LOG.debug("Theme colors - Text foreground: " + ThemeUtils.textForeground());
                LOG.debug("Theme colors - Text field background: " + ThemeUtils.textFieldBackground());
                LOG.debug("UIManager Panel.background: " + UIManager.getColor("Panel.background"));
                LOG.debug("UIManager Label.foreground: " + UIManager.getColor("Label.foreground"));
                LOG.debug("UIManager TextField.background: " + UIManager.getColor("TextField.background"));
            }
            
            // Update main panel background
            if (mainPanel != null) {
                Color bg = ThemeUtils.panelBackground();
                mainPanel.setBackground(bg);
                mainPanel.revalidate();
                mainPanel.repaint();
                LOG.debug("Updated main panel background to: " + bg);
            }
            
            // Update chat scroll pane and viewport
            if (chatScrollPane != null && chatScrollPane.getViewport() != null) {
                Color bg = ThemeUtils.panelBackground();
                chatScrollPane.setBackground(bg);
                chatScrollPane.getViewport().setBackground(bg);
                chatScrollPane.revalidate();
                chatScrollPane.repaint();
            }
            
            // Update message container and all message components
            if (messageContainer != null) {
                Color bg = ThemeUtils.panelBackground();
                messageContainer.setBackground(bg);
                
                // NUCLEAR OPTION: Recreate all message components to ensure proper theme switching
                // This is the most reliable way to handle JEditorPane HTML content that doesn't refresh properly
                recreateAllMessageComponents();
                
                messageContainer.revalidate();
                messageContainer.repaint();
            }
            
            // Update input area and input panel
            if (inputArea != null) {
                inputArea.setBackground(ThemeUtils.textFieldBackground());
                inputArea.setForeground(ThemeUtils.textForeground());
                inputArea.setCaretColor(ThemeUtils.textForeground());
                inputArea.revalidate();
                inputArea.repaint();
            }
            
            if (inputPanel != null) {
                // Keep input panel grey and render white background via the inner inputBoxContainer
                inputPanel.setBackground(ThemeUtils.panelBackground());
                inputPanel.setOpaque(true);

                for (Component child : inputPanel.getComponents()) {
                    if (child instanceof JPanel && "inputBoxContainer".equals(child.getName())) {
                        JPanel box = (JPanel) child;
                        box.setOpaque(true);
                        box.setBackground(TriagePanelConstants.getInputContainerBackground());
                        box.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(TriagePanelConstants.getInputContainerBorder(), 1, true),
                            BorderFactory.createEmptyBorder(8, 12, 8, 0)
                        ));

                        for (Component inner : box.getComponents()) {
                            if (inner instanceof JTextArea) {
                                JTextArea textArea = (JTextArea) inner;
                                // Do not paint its own background; rely on the box
                                textArea.setOpaque(false);
                                textArea.setForeground(ThemeUtils.textForeground());
                                textArea.setCaretColor(ThemeUtils.textForeground());
                                textArea.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
                            } else if (inner instanceof JPanel && "buttonPanel".equals(inner.getName())) {
                                JPanel buttonPanel = (JPanel) inner;
                                // Let the white box show through
                                buttonPanel.setOpaque(false);
                                buttonPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
                            }
                        }
                    }
                }
                inputPanel.revalidate();
                inputPanel.repaint();
            }
            
            // Update bottom spacer
            if (bottomSpacer != null) {
                bottomSpacer.setBackground(ThemeUtils.panelBackground());
                bottomSpacer.revalidate();
                bottomSpacer.repaint();
            }
            
            // Update header and status labels
            if (headerLabel != null) {
                headerLabel.setForeground(ThemeUtils.textForeground());
                headerLabel.revalidate();
                headerLabel.repaint();
            }
            
            if (statusLabel != null) {
                statusLabel.setForeground(ThemeUtils.textForeground());
                statusLabel.revalidate();
                statusLabel.repaint();
            }
            
            LOG.info("Theme refresh completed");
        } catch (Exception e) {
            LOG.error("Error during theme refresh: " + e.getMessage(), e);
        }
    }
    
    /**
     * Recreates all message components to ensure proper theme switching.
     * This is the most reliable way to handle JEditorPane HTML content that doesn't refresh properly.
     */
    private void recreateAllMessageComponents() {
        try {
            LOG.debug("Recreating all message components for theme refresh");
            
            // Store current components and their order
            List<Component> components = new ArrayList<>();
            for (Component child : messageContainer.getComponents()) {
                if (child instanceof com.trace.chat.components.MessageComponent) {
                    components.add(child);
                }
            }
            
            // Remove all message components
            for (Component child : components) {
                messageContainer.remove(child);
            }
            
            // Recreate all message components with current theme
            for (Component oldComponent : components) {
                if (oldComponent instanceof com.trace.chat.components.MessageComponent) {
                    com.trace.chat.components.MessageComponent oldMsg = (com.trace.chat.components.MessageComponent) oldComponent;
                    com.trace.chat.components.ChatMessage message = oldMsg.getMessage();
                    
                    // Create new message component with same message
                    com.trace.chat.components.MessageComponent newMsg = new com.trace.chat.components.MessageComponent(message);
                    newMsg.setAlignmentX(Component.LEFT_ALIGNMENT);
                    
                    // Add to container
                    messageContainer.add(newMsg);
                }
            }
            
            if (LOG.isDebugEnabled()) {
                LOG.debug("Recreated " + components.size() + " message components");
            }
        } catch (Exception e) {
            LOG.error("Error recreating message components: " + e.getMessage(), e);
        }
    }

    private void refreshThemeInContainer(Container container) {
        for (Component child : container.getComponents()) {
            // Update markdown content
            if (child instanceof javax.swing.JEditorPane) {
                com.trace.chat.components.MarkdownRenderer.reapplyThemeStyles((javax.swing.JEditorPane) child);
            }
            
            // Update JPanels and other containers
            if (child instanceof JPanel) {
                JPanel panel = (JPanel) child;
                // Only update panels that are opaque (have explicit backgrounds)
                if (panel.isOpaque()) {
                    panel.setBackground(ThemeUtils.panelBackground());
                    panel.revalidate();
                    panel.repaint();
                }
            }
            
            // Update JLabels
            if (child instanceof JLabel) {
                JLabel label = (JLabel) child;
                label.setForeground(ThemeUtils.textForeground());
                label.revalidate();
                label.repaint();
            }
            
            // Update JTextAreas
            if (child instanceof JTextArea) {
                JTextArea textArea = (JTextArea) child;
                // Use textFieldBackground for all text areas to ensure proper theme switching
                textArea.setBackground(ThemeUtils.textFieldBackground());
                textArea.setForeground(ThemeUtils.textForeground());
                textArea.setCaretColor(ThemeUtils.textForeground());
                textArea.revalidate();
                textArea.repaint();
            }
            
            // Update JButtons
            if (child instanceof JButton) {
                JButton button = (JButton) child;
                button.setForeground(ThemeUtils.textForeground());
                button.revalidate();
                button.repaint();
            }
            
            // Recursively update containers
            if (child instanceof Container) {
                refreshThemeInContainer((Container) child);
            }
        }
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
        JButton analysisModeButton = createAnalysisModeButton();
        
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
        JButton sendIconButton = createModernSendButton();
        
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
    private MessageBusConnection messageBusConnection;
    
    private void setupThemeChangeListener() {
        try {
            // Use modern MessageBus approach for theme change detection (IntelliJ 2025.2+)
            messageBusConnection = ApplicationManager.getApplication().getMessageBus().connect();
            messageBusConnection.subscribe(LafManagerListener.TOPIC, source -> {
                ApplicationManager.getApplication().invokeLater(() -> {
                        try {
                            LOG.info("=== THEME CHANGE DETECTED (via MessageBus) ===");
                            LOG.info("Current theme: " + UIManager.getLookAndFeel().getName());
                            LOG.info("Panel background: " + UIManager.getColor("Panel.background"));
                            LOG.info("Label foreground: " + UIManager.getColor("Label.foreground"));
                            LOG.info("Text field background: " + UIManager.getColor("TextField.background"));
                            
                            // Store current scroll position
                            int currentScrollValue = 0;
                            if (chatScrollPane != null && chatScrollPane.getVerticalScrollBar() != null) {
                                currentScrollValue = chatScrollPane.getVerticalScrollBar().getValue();
                            }
                            
                            // Refresh all theme colors
                            refreshTheme();
                            
                            // Restore scroll position to maintain user's view
                            if (chatScrollPane != null && chatScrollPane.getVerticalScrollBar() != null) {
                                chatScrollPane.getVerticalScrollBar().setValue(currentScrollValue);
                                LOG.info("Restored scroll position to: " + currentScrollValue);
                            }
                            
                            LOG.info("=== THEME CHANGE COMPLETED ===");
                        } catch (Exception ex) {
                            LOG.error("Error during theme change refresh: " + ex.getMessage(), ex);
                        }
                    });
            });
            
            // Backup: Also add a property change listener for theme-related properties
            mainPanel.addPropertyChangeListener("UI", evt -> {
                ApplicationManager.getApplication().invokeLater(() -> {
                    try {
                        LOG.info("=== THEME CHANGE DETECTED (via property change backup) ===");
                        LOG.info("Property change: " + evt.getPropertyName() + " = " + evt.getNewValue());
                        refreshTheme();
                        LOG.info("=== THEME CHANGE COMPLETED ===");
                    } catch (Exception ex) {
                        LOG.error("Error during theme change refresh: " + ex.getMessage(), ex);
                    }
                });
            });
            
            // Add font change listener to respond to IDE font size changes
            UIManager.addPropertyChangeListener(evt -> {
                if ("defaultFont".equals(evt.getPropertyName())) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        try {
                            LOG.info("=== FONT CHANGE DETECTED ===");
                            LOG.info("Font change: " + evt.getPropertyName() + " = " + evt.getNewValue());
                            refreshTheme();
                            LOG.info("=== FONT CHANGE COMPLETED ===");
                        } catch (Exception ex) {
                            LOG.error("Error during font change refresh: " + ex.getMessage(), ex);
                        }
                    });
                }
            });
            
            LOG.info("Theme change listeners registered successfully");
        } catch (Exception e) {
            LOG.error("Failed to register theme change listeners: " + e.getMessage(), e);
        }
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
        LOG.info("Manual theme refresh triggered");
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                // Store current scroll position
                int currentScrollValue = 0;
                if (chatScrollPane != null && chatScrollPane.getVerticalScrollBar() != null) {
                    currentScrollValue = chatScrollPane.getVerticalScrollBar().getValue();
                }
                
                // Refresh all theme colors
                refreshTheme();
                
                // Restore scroll position to maintain user's view
                if (chatScrollPane != null && chatScrollPane.getVerticalScrollBar() != null) {
                    chatScrollPane.getVerticalScrollBar().setValue(currentScrollValue);
                    LOG.debug("Restored scroll position to: " + currentScrollValue);
                }
                
                LOG.info("Manual theme refresh completed");
            } catch (Exception e) {
                LOG.error("Error during manual theme refresh: " + e.getMessage(), e);
            }
        });
    }



    /**
     * Creates the custom header panel with logo and scenario information.
     *
     * @return The configured header panel
     */
    private JPanel createCustomHeaderPanel() {
        Color darkBg = ThemeUtils.panelBackground();
        
        LOG.debug("Creating custom header panel with ultra-compact layout");
        
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.X_AXIS));
        header.setBackground(darkBg);
        
        // Minimal header padding
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeUtils.uiColor("Component.borderColor", new JBColor(new Color(68, 68, 68), new Color(68, 68, 68)))),
            BorderFactory.createEmptyBorder(6, 8, 6, 8) // Increased from (2, 4, 2, 4) for better visual balance
        ));
        
        // Create left side with BoxLayout for precise control
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.X_AXIS));
        leftPanel.setOpaque(false);
        
        LOG.debug("Creating AI toggle button for header");
        // Create AI toggle button
        JButton aiToggleButton = createAIToggleButton();
        aiToggleButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(aiToggleButton);
        
        // Add minimal spacing between button and text
        leftPanel.add(Box.createHorizontalStrut(2));
        
        LOG.debug("Creating TRACE title label");
        JLabel title = new JLabel("TRACE") {
            @Override
            public Dimension getMaximumSize() {
                // Permit horizontal shrink (important for BoxLayout)
                Dimension pref = getPreferredSize();
                return new Dimension(Integer.MAX_VALUE, pref.height);
            }
        };
        title.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        title.setForeground(ThemeUtils.textForeground());
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(title);

        // Ultra-narrow polish: hide the title if there isn't enough room for right controls and toggle
        // Listener is added after rightPanel is created to avoid forward reference issues.
        
        // Allow the left group to shrink as needed so the right controls remain visible
        int leftHeight = leftPanel.getPreferredSize().height;
        leftPanel.setMinimumSize(new Dimension(0, leftHeight));
        leftPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, leftHeight));

        if (LOG.isDebugEnabled()) {
            LOG.debug("Adding left panel to header with " + leftPanel.getComponentCount() + " components");
            LOG.debug("AI toggle button preferred size: " + aiToggleButton.getPreferredSize());
            LOG.debug("TRACE label preferred size: " + title.getPreferredSize());
            LOG.debug("Left panel preferred size: " + leftPanel.getPreferredSize());
        }
        header.add(leftPanel);
        header.add(Box.createHorizontalGlue());
        
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
        
        // Right group keeps natural size at the far right
        int rightHeight = rightPanel.getPreferredSize().height;
        rightPanel.setMinimumSize(new Dimension(rightPanel.getPreferredSize().width, rightHeight));
        rightPanel.setMaximumSize(new Dimension(rightPanel.getPreferredSize().width, rightHeight));
        header.add(rightPanel);

        // Ultra-narrow polish: hide the title if there isn't enough room for right controls and toggle
        header.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                try {
                    int available = header.getWidth();
                    int rightWidth = rightPanel.getPreferredSize().width;
                    int toggleWidth = aiToggleButton.getPreferredSize().width + 2; // + spacing
                    int titleWidth = title.getPreferredSize().width;
                    // Keep a 6px safety margin
                    boolean show = available >= rightWidth + toggleWidth + titleWidth + 6;
                    if (title.isVisible() != show) {
                        title.setVisible(show);
                        header.revalidate();
                        header.repaint();
                    }
                } catch (Exception ignore) {
                }
            }
        });

        if (LOG.isDebugEnabled()) {
            LOG.debug("Header panel created with dimensions: " + header.getPreferredSize());
        }
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
            LOG.debug("Clear chat button clicked");
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
        settingsButton.setForeground(ThemeUtils.textForeground());
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
            LOG.debug("Settings button clicked");
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
        LOG.debug("Creating analysis mode button");
        
        JButton analysisModeButton = new JButton();
        
        // Minimal styling - reverted to original approach
        analysisModeButton.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        analysisModeButton.setForeground(ThemeUtils.textForeground());
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
        
        // Update button text based on current mode
        updateAnalysisModeButtonText(analysisModeButton);
        
        // Add action listener to toggle between modes
        analysisModeButton.addActionListener(e -> {
            if (ANALYSIS_MODE_OVERVIEW.equals(currentAnalysisMode)) {
                setCurrentAnalysisMode(ANALYSIS_MODE_FULL);
            } else {
                setCurrentAnalysisMode(ANALYSIS_MODE_OVERVIEW);
            }
            updateAnalysisModeButtonText(analysisModeButton);
        });
        
        LOG.debug("Analysis mode button creation complete");
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
        
        if (LOG.isDebugEnabled()) {
            LOG.debug("Updating TRACE toggle button appearance - TRACE enabled: " + aiEnabled);
        }
        
        if (aiEnabled) {
            // TRACE is enabled - green color
            button.setForeground(new JBColor(new Color(76, 175, 80), new Color(76, 175, 80))); // Material Design Green
            button.setToolTipText("Disable TRACE");
            LOG.debug("TRACE toggle button set to enabled state (green)");
        } else {
            // TRACE is disabled - gray color
            button.setForeground(new JBColor(new Color(158, 158, 158), new Color(158, 158, 158))); // Material Design Gray
            button.setToolTipText("Enable TRACE - Parse test failures locally for AI analysis");
            LOG.debug("TRACE toggle button set to disabled state (gray)");
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
        
        // NATURAL SCALING: Let IntelliJ handle icon scaling properly
        try {
            Icon sendIcon = IconLoader.getIcon("/icons/send_32.png", getClass());
            sendButton.setIcon(sendIcon);
        } catch (Exception e) {
            sendButton.setText("→");
        }
        
        // NATURAL SIZE: Let button size itself based on icon and IntelliJ scaling
        sendButton.setIconTextGap(0);
        
        // Transparent background
        sendButton.setBackground(new JBColor(new Color(0, 0, 0, 0), new Color(0, 0, 0, 0)));
        sendButton.setForeground(ThemeUtils.textForeground());
        sendButton.setFocusPainted(false);
        sendButton.setBorderPainted(false);
        sendButton.setContentAreaFilled(false);
        sendButton.setOpaque(false);
        
        // Simple approach: just make it transparent and let the icon be round
        sendButton.setOpaque(false);
        sendButton.setContentAreaFilled(false);
        sendButton.setBorderPainted(false);
        sendButton.setFocusPainted(false);
        sendButton.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        
        // Cursor and tooltip
        sendButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        sendButton.setToolTipText("Send message");
        
        // Simple hover effect - just a subtle background change
        sendButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                sendButton.setBackground(new Color(255, 255, 255, 20));
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                sendButton.setBackground(new Color(0, 0, 0, 0));
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
        
        Color panelBg = ThemeUtils.panelBackground();
        mainPanel.setBackground(panelBg);
        mainPanel.setOpaque(true);
        
        mainPanel.add(createCustomHeaderPanel(), BorderLayout.NORTH);
        if (showSettingsTab) {
            mainPanel.add(createSettingsPanel(), BorderLayout.CENTER);
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
            
            // Disconnect MessageBus connection
            if (messageBusConnection != null) {
                try {
                    messageBusConnection.disconnect();
                    messageBusConnection = null;
                    LOG.info("Disconnected MessageBus connection");
                } catch (Exception e) {
                    LOG.error("Error disconnecting MessageBus connection: " + e.getMessage());
                }
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

 