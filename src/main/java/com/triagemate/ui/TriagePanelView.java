package com.triagemate.ui;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.util.IconLoader;
import com.intellij.icons.AllIcons;
import com.triagemate.models.FailureInfo;
import com.triagemate.services.LocalPromptGenerationService;
import com.triagemate.services.BackendCommunicationService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;

/**
 * Chat-style UI component for displaying test failure analysis and user interactions.
 * Implements a simplified chat interface following Phase 4 specifications.
 */
public class TriagePanelView {
    private final Project project;
    private final JPanel mainPanel;
    private final JPanel headerPanel;
    private final JPanel chatPanel = new JPanel() {
        @Override
        public Dimension getPreferredSize() {
            if (getParent() instanceof JViewport) {
                int width = ((JViewport) getParent()).getWidth();
                int height = super.getPreferredSize().height;
                return new Dimension(width, height);
            }
            return super.getPreferredSize();
        }
    };
    private final JPanel inputPanel;
    private final JBTextArea inputArea;
    private final JButton sendButton;
    private final JBLabel headerLabel;
    private final JBLabel statusLabel;
    
    // Chat state management
    private final List<ChatMessage> chatHistory;
    private FailureInfo currentFailure;
    private final LocalPromptGenerationService promptService;
    private final BackendCommunicationService backendService;
    private JScrollPane chatScrollPane;
    private boolean showSettingsTab = false;

    /**
     * Constructor for TriagePanelView
     *
     * @param project The current project
     */
    public TriagePanelView(Project project) {
        this.project = project;
        this.chatHistory = new ArrayList<>();
        this.promptService = project.getService(LocalPromptGenerationService.class);
        this.backendService = project.getService(BackendCommunicationService.class);
        
        // Initialize UI components
        this.mainPanel = new JPanel(new BorderLayout());
        this.headerPanel = new JPanel(new BorderLayout());
        this.inputPanel = new JPanel(new BorderLayout());
        this.inputArea = new JBTextArea();
        this.sendButton = new JButton("Send");
        this.headerLabel = new JBLabel("No test failure detected");
        this.statusLabel = new JBLabel("");
        
        initializeUI();
        setupEventHandlers();
    }

    /**
     * Initializes the UI components with simplified chat interface
     */
    private void initializeUI() {
        setupHeaderPanel();
        setupChatPanel();
        setupInputPanel();
        Color panelBg = javax.swing.UIManager.getColor("Panel.background");
        if (panelBg == null) panelBg = new java.awt.Color(43,43,43);
        mainPanel.removeAll();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(panelBg);
        mainPanel.setOpaque(true);
        chatPanel.setBackground(panelBg);
        chatPanel.setOpaque(true);
        chatScrollPane = new javax.swing.JScrollPane(chatPanel,
            javax.swing.JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        chatScrollPane.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        chatScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        chatScrollPane.setBackground(panelBg);
        chatScrollPane.getViewport().setBackground(panelBg);
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
     * Sets up the header panel with failure metadata
     */
    private void setupHeaderPanel() {
        headerPanel.removeAll();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        Color panelBg = javax.swing.UIManager.getColor("Panel.background");
        if (panelBg == null) panelBg = new java.awt.Color(43,43,43); // fallback for dark theme
        headerPanel.setBackground(panelBg);
        headerPanel.setOpaque(true);
        headerPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 24, 16, 24));

        // Remove window title setting code (cannot change tool window title at runtime)

        if (currentFailure != null) {
            // Top row: warning icon + Failure Detected
            javax.swing.JPanel topRow = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
            topRow.setOpaque(false);
            javax.swing.JLabel warningIcon = new javax.swing.JLabel("\u26A0"); // ⚠️
            warningIcon.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 16));
            warningIcon.setForeground(new java.awt.Color(255, 193, 7)); // Amber
            javax.swing.JLabel failureLabel = new javax.swing.JLabel("Failure Detected");
            failureLabel.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 13));
            failureLabel.setForeground(new java.awt.Color(180, 180, 180));
            topRow.add(warningIcon);
            topRow.add(failureLabel);

            // Second row: Scenario (orange text) + scenario name (bold white)
            javax.swing.JPanel scenarioRow = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
            scenarioRow.setOpaque(false);
            javax.swing.JLabel scenarioLabel = new javax.swing.JLabel("Scenario");
            scenarioLabel.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 13));
            scenarioLabel.setForeground(new java.awt.Color(255, 152, 0)); // Cucumber orange, no background
            javax.swing.JLabel scenarioName = new javax.swing.JLabel(currentFailure.getScenarioName() != null ? currentFailure.getScenarioName() : "Unknown Scenario");
            scenarioName.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 17));
            scenarioName.setForeground(java.awt.Color.WHITE);
            scenarioName.setOpaque(false);
            scenarioRow.add(scenarioLabel);
            scenarioRow.add(scenarioName);

            headerPanel.add(topRow);
            headerPanel.add(javax.swing.Box.createVerticalStrut(8));
            headerPanel.add(scenarioRow);
            headerPanel.add(javax.swing.Box.createVerticalStrut(8));
            // Bottom border for separation
            javax.swing.JSeparator sep = new javax.swing.JSeparator();
            sep.setForeground(new java.awt.Color(60, 60, 60));
            headerPanel.add(sep);
        }
        // If no failure, do not show any header (no label, no empty header)
    }

    /**
     * Sets up the chat panel for message display using proper layout
     */
    private void setupChatPanel() {
        // Use a vertical BoxLayout for proper message stacking
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        
        // Get theme-aware background color
        Color panelBg = UIManager.getColor("Panel.background");
        if (panelBg == null) {
            panelBg = new Color(43, 43, 43);
        }
        
        chatPanel.setBackground(panelBg);
        chatPanel.setOpaque(true);
        chatPanel.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        
        // Add initial vertical glue to push messages to top
        chatPanel.add(Box.createVerticalGlue());
    }

    /**
     * Sets up the input panel with modern styling
     */
    private void setupInputPanel() {
        inputPanel.setBorder(BorderFactory.createEmptyBorder(8, 16, 16, 16));
        
        // Create input container with rounded border
        JPanel inputContainer = new JPanel(new BorderLayout());
        inputContainer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 60, 60), 1, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 8)
        ));
        inputContainer.setBackground(new Color(50, 50, 50));
        inputContainer.setOpaque(true);
        
        // Configure text area
        inputArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputArea.setRows(1);
        inputArea.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        inputArea.setBackground(new Color(50, 50, 50));
        inputArea.setForeground(Color.WHITE);
        inputArea.setCaretColor(Color.WHITE);
        inputArea.setOpaque(false);
        inputArea.putClientProperty("JTextField.placeholderText", "Ask anything about the test failure...");
        
        // Create send button
        JButton sendIconButton = new JButton("→");
        sendIconButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        sendIconButton.setBackground(new Color(70, 70, 70));
        sendIconButton.setForeground(Color.WHITE);
        sendIconButton.setFocusPainted(false);
        sendIconButton.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        sendIconButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        sendIconButton.setToolTipText("Send message");
        sendIconButton.addActionListener(e -> sendMessage());
        
        // Add components to input container
        inputContainer.add(inputArea, BorderLayout.CENTER);
        inputContainer.add(sendIconButton, BorderLayout.EAST);
        
        // Add to input panel
        inputPanel.add(inputContainer, BorderLayout.CENTER);
        
        // Hide the old send button
        sendButton.setVisible(false);
    }

    /**
     * Sets up event handlers for user interactions
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
     * Sends the current message from the input area
     */
    private void sendMessage() {
        String messageText = inputArea.getText().trim();
        if (messageText.isEmpty()) {
            return;
        }
        
        // Add user message to chat
        addMessage(new ChatMessage(ChatMessage.Role.USER, messageText, System.currentTimeMillis()));
        
        // Clear input area
        inputArea.setText("");
        
        // TODO: Handle AI response when backend is available
        // For now, show a professional backend configuration message
        String backendMessage = "AI backend service is not currently configured. To enable AI-powered test failure analysis, please configure your backend connection in the settings.";
        
        addMessage(new ChatMessage(ChatMessage.Role.AI, 
            backendMessage, 
            System.currentTimeMillis(), null));
    }

    /**
     * Adds a message to the chat history and updates the UI
     */
    private void addMessage(ChatMessage message) {
        chatHistory.add(message);
        addMessageToUI(message);
    }

    /**
     * Adds a message component to the UI with proper layout
     */
    private void addMessageToUI(ChatMessage message) {
        JPanel messagePanel = createMessagePanel(message);
        
        // Remove the vertical glue, add message, then add glue back
        chatPanel.removeAll();
        
        // Add all existing messages
        for (ChatMessage existingMessage : chatHistory) {
            JPanel existingPanel = createMessagePanel(existingMessage);
            chatPanel.add(existingPanel);
            chatPanel.add(Box.createVerticalStrut(16)); // Add spacing between messages
        }
        
        // Add vertical glue to push messages to top
        chatPanel.add(Box.createVerticalGlue());
        
        // Revalidate and repaint
        chatPanel.revalidate();
        chatPanel.repaint();
        
        // Scroll to bottom
        scrollToBottom();
    }

    /**
     * Creates a properly styled message panel with Codeium-like layout
     */
    private JPanel createMessagePanel(ChatMessage message) {
        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        messagePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        messagePanel.setOpaque(false);
        messagePanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        
        // Create header with logo, sender and timestamp
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        
        // Left side: logo + sender name
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setOpaque(false);
        
        // Add logo for all messages (both user and AI)
        try {
            String iconPath = message.getRole() == ChatMessage.Role.USER ? "/icons/user_profile_24.png" : "/icons/logo_24.png";
            Icon logoIcon = IconLoader.getIcon(iconPath, getClass());
            JLabel logoLabel = new JLabel(logoIcon);
            logoLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8)); // Add right padding
            leftPanel.add(logoLabel, BorderLayout.WEST);
        } catch (Exception e) {
            // Fallback: no icon
        }
        
        JLabel senderLabel = new JLabel(message.getRole() == ChatMessage.Role.USER ? "You" : "TriageMate");
        senderLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        senderLabel.setForeground(Color.WHITE);
        leftPanel.add(senderLabel, BorderLayout.CENTER);
        
        headerPanel.add(leftPanel, BorderLayout.WEST);
        
        // Right side: full timestamp
        JLabel timeLabel = new JLabel(formatFullTimestamp(message.getTimestamp()));
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        timeLabel.setForeground(new Color(150, 150, 150));
        headerPanel.add(timeLabel, BorderLayout.EAST);
        
        // Create content panel
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setOpaque(false);
        
        // Add scenario information for AI messages if available
        if ((message.getRole() == ChatMessage.Role.AI || message.getRole() == ChatMessage.Role.SYSTEM) && currentFailure != null) {
            // Add scenario information
            JPanel scenarioPanel = new JPanel(new BorderLayout());
            scenarioPanel.setOpaque(false);
            scenarioPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
            
            // Create scenario label with orange "Scenario:" and bold white test name
            JPanel scenarioLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            scenarioLabelPanel.setOpaque(false);
            
            JLabel scenarioPrefix = new JLabel("Scenario: ");
            scenarioPrefix.setFont(new Font("Segoe UI", Font.BOLD, 13));
            scenarioPrefix.setForeground(new Color(255, 152, 0)); // Orange color for "Scenario:"
            
            JLabel scenarioName = new JLabel(currentFailure.getScenarioName() != null ? currentFailure.getScenarioName() : "Unknown Scenario");
            scenarioName.setFont(new Font("Segoe UI", Font.BOLD, 13)); // Bold for test name
            scenarioName.setForeground(Color.WHITE); // White color for test name
            
            scenarioLabelPanel.add(scenarioPrefix);
            scenarioLabelPanel.add(scenarioName);
            scenarioPanel.add(scenarioLabelPanel, BorderLayout.WEST);
            
            contentPanel.add(scenarioPanel, BorderLayout.NORTH);
            
            // Add failed step information
            if (currentFailure.getFailedStepText() != null && !currentFailure.getFailedStepText().trim().isEmpty()) {
                JPanel failedStepPanel = new JPanel(new BorderLayout());
                failedStepPanel.setOpaque(false);
                failedStepPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
                
                // Create failed step label with failure symbol, orange "Failed Step:" and red bold step text
                JPanel failedStepLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
                failedStepLabelPanel.setOpaque(false);
                
                // Add red X failure symbol
                JLabel failureSymbol = new JLabel("✗ ");
                failureSymbol.setFont(new Font("Segoe UI", Font.BOLD, 13));
                failureSymbol.setForeground(new Color(255, 100, 100)); // Red color for failure symbol
                
                JLabel failedStepPrefix = new JLabel("Failed Step: ");
                failedStepPrefix.setFont(new Font("Segoe UI", Font.BOLD, 13));
                failedStepPrefix.setForeground(new Color(255, 100, 100)); // Red color for "Failed Step:"
                
                JLabel failedStepText = new JLabel(currentFailure.getFailedStepText());
                failedStepText.setFont(new Font("Segoe UI", Font.BOLD, 13)); // Bold font for step text
                failedStepText.setForeground(Color.WHITE); // White color for step text
                
                failedStepLabelPanel.add(failureSymbol);
                failedStepLabelPanel.add(failedStepPrefix);
                failedStepLabelPanel.add(failedStepText);
                failedStepPanel.add(failedStepLabelPanel, BorderLayout.WEST);
                
                contentPanel.add(failedStepPanel, BorderLayout.CENTER);
            } else {
                // Debug: Show when failed step text is not available
                JPanel debugPanel = new JPanel(new BorderLayout());
                debugPanel.setOpaque(false);
                debugPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
                
                JLabel debugLabel = new JLabel("Debug: Failed step text is null or empty");
                debugLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                debugLabel.setForeground(new Color(255, 100, 100)); // Red for debug
                
                debugPanel.add(debugLabel, BorderLayout.WEST);
                contentPanel.add(debugPanel, BorderLayout.CENTER);
            }
        }
        
        // Add AI thinking section for AI messages
        if ((message.getRole() == ChatMessage.Role.AI || message.getRole() == ChatMessage.Role.SYSTEM) && 
            message.getAiThinking() != null && !message.getAiThinking().trim().isEmpty()) {
            
            CollapsiblePanel aiThinkingPanel = new CollapsiblePanel("AI Thinking", message.getAiThinking());
            contentPanel.add(aiThinkingPanel, BorderLayout.CENTER);
        }
        
        // Create message text area (no background, just text)
        JTextArea messageText = new JTextArea(message.getText());
        messageText.setLineWrap(true);
        messageText.setWrapStyleWord(true);
        messageText.setEditable(false);
        messageText.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        messageText.setBackground(chatPanel.getBackground()); // Use chat panel background
        messageText.setForeground(Color.WHITE);
        messageText.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        messageText.setOpaque(false);
        
        contentPanel.add(messageText, BorderLayout.SOUTH);
        
        // Add components to message panel
        messagePanel.add(headerPanel, BorderLayout.NORTH);
        messagePanel.add(contentPanel, BorderLayout.CENTER);
        
        return messagePanel;
    }

    /**
     * Scrolls the chat to the bottom
     */
    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            if (chatScrollPane != null) {
                JScrollBar verticalBar = chatScrollPane.getVerticalScrollBar();
                verticalBar.setValue(verticalBar.getMaximum());
            }
        });
    }

    /**
     * Updates the panel with new failure information
     */
    public void updateFailure(FailureInfo failureInfo) {
        this.currentFailure = failureInfo;
        
        // Debug logging
        System.out.println("TriagePanelView.updateFailure called");
        System.out.println("Scenario name: " + (failureInfo.getScenarioName() != null ? failureInfo.getScenarioName() : "null"));
        System.out.println("Failed step text: " + (failureInfo.getFailedStepText() != null ? failureInfo.getFailedStepText() : "null"));
        System.out.println("Error message: " + (failureInfo.getErrorMessage() != null ? failureInfo.getErrorMessage() : "null"));
        
        // Clear chat history for new failure
        clearChat();
        
        // Generate and display initial prompt
        generateAndDisplayPrompt(failureInfo);
    }

    /**
     * Clears the chat history and UI
     */
    private void clearChat() {
        chatHistory.clear();
        chatPanel.removeAll();
        chatPanel.add(Box.createVerticalGlue());
        chatPanel.revalidate();
        chatPanel.repaint();
    }

    /**
     * Generates and displays the initial prompt for the failure
     */
    private void generateAndDisplayPrompt(FailureInfo failureInfo) {
        try {
            String prompt = promptService.generateDetailedPrompt(failureInfo);
            
            // Check backend availability
            if (!backendService.isBackendAvailable()) {
                prompt = "Backend not configured. Here's your test failure analysis to copy to your preferred AI service:\n\n" + prompt;
            }
            
            // Create AI thinking content based on the failure analysis
            String aiThinking = generateAiThinkingContent(failureInfo);
            
            // Create a special AI message with the prompt in the collapsible section
            addMessage(new ChatMessage(ChatMessage.Role.AI, "", System.currentTimeMillis(), prompt));
            
        } catch (Exception e) {
            String errorMessage = "Error generating prompt: " + e.getMessage();
            addMessage(new ChatMessage(ChatMessage.Role.AI, errorMessage, System.currentTimeMillis()));
        }
    }
    
    /**
     * Generates AI thinking content based on the failure information
     */
    private String generateAiThinkingContent(FailureInfo failureInfo) {
        StringBuilder thinking = new StringBuilder();
        thinking.append("Analyzing test failure...\n\n");
        
        if (failureInfo.getScenarioName() != null) {
            thinking.append("• Scenario: ").append(failureInfo.getScenarioName()).append("\n");
        }
        
        if (failureInfo.getFailedStepText() != null) {
            thinking.append("• Failed Step: ").append(failureInfo.getFailedStepText()).append("\n");
        }
        
        if (failureInfo.getErrorMessage() != null) {
            thinking.append("• Error: ").append(failureInfo.getErrorMessage()).append("\n");
        }
        
        if (failureInfo.getExpectedValue() != null && failureInfo.getActualValue() != null) {
            thinking.append("• Expected: ").append(failureInfo.getExpectedValue()).append("\n");
            thinking.append("• Actual: ").append(failureInfo.getActualValue()).append("\n");
        }
        
        thinking.append("\nGenerating analysis and recommendations...");
        
        return thinking.toString();
    }

    /**
     * Gets the main content panel
     *
     * @return The main panel
     */
    public JComponent getContent() {
        return mainPanel;
    }

    /**
     * Inner class representing a chat message
     */
    private static class ChatMessage {
        public enum Role { USER, AI, SYSTEM }
        
        private final Role role;
        private final String text;
        private final long timestamp;
        private final String aiThinking; // New field for AI thinking content
        
        public ChatMessage(Role role, String text, long timestamp) {
            this(role, text, timestamp, null);
        }
        
        public ChatMessage(Role role, String text, long timestamp, String aiThinking) {
            this.role = role;
            this.text = text;
            this.timestamp = timestamp;
            this.aiThinking = aiThinking;
        }
        
        public Role getRole() { return role; }
        public String getText() { return text; }
        public long getTimestamp() { return timestamp; }
        public String getAiThinking() { return aiThinking; }
    }

    /**
     * Custom collapsible panel for AI thinking content
     */
    private static class CollapsiblePanel extends JPanel {
        private final JPanel contentPanel;
        private final JLabel toggleLabel;
        private boolean isExpanded = false;
        
        public CollapsiblePanel(String title, String content) {
            setLayout(new BorderLayout());
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
            
            // Create toggle button with expand/collapse indicator
            toggleLabel = new JLabel("▶ Show AI Thinking");
            toggleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
            toggleLabel.setForeground(Color.WHITE); // White color
            toggleLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            toggleLabel.setToolTipText("Click to " + (isExpanded ? "hide" : "show") + " AI thinking");
            
            // Create content panel
            contentPanel = new JPanel(new BorderLayout());
            contentPanel.setOpaque(false);
            contentPanel.setBorder(BorderFactory.createEmptyBorder(8, 16, 0, 0)); // Indent content
            
            if (content != null && !content.trim().isEmpty()) {
                JTextArea contentArea = new JTextArea(content);
                contentArea.setLineWrap(true);
                contentArea.setWrapStyleWord(true);
                contentArea.setEditable(false);
                contentArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                contentArea.setForeground(new Color(200, 200, 200)); // Light gray for thinking content
                contentArea.setBackground(new Color(50, 50, 50)); // Dark background for thinking section
                contentArea.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(80, 80, 80), 1),
                    BorderFactory.createEmptyBorder(8, 8, 8, 8)
                ));
                contentArea.setOpaque(true);
                contentPanel.add(contentArea, BorderLayout.CENTER);
            }
            
            // Add click listener
            toggleLabel.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    toggleExpanded();
                }
            });
            
            // Initially collapsed
            contentPanel.setVisible(false);
            add(toggleLabel, BorderLayout.NORTH);
            add(contentPanel, BorderLayout.CENTER);
        }
        
        private void toggleExpanded() {
            isExpanded = !isExpanded;
            contentPanel.setVisible(isExpanded);
            toggleLabel.setText((isExpanded ? "▼ " : "▶ ") + "Show AI Thinking");
            toggleLabel.setToolTipText("Click to " + (isExpanded ? "hide" : "show") + " AI thinking");
            
            // Trigger revalidation to adjust scroll pane
            revalidate();
            repaint();
            
            // Don't auto-scroll - let the user control the view
            // The collapsible section should stay in view when toggled
        }
    }

    /**
     * Formats full timestamp for display (like Codeium)
     */
    private String formatFullTimestamp(long timestamp) {
        return new java.text.SimpleDateFormat("h:mm a, MMM d, yyyy").format(new java.util.Date(timestamp));
    }

    /**
     * Creates the custom header panel with logo and scenario information
     */
    private JPanel createCustomHeaderPanel() {
        Color darkBg = UIManager.getColor("Panel.background");
        if (darkBg == null) {
            darkBg = new Color(43, 43, 43);
        }
        
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(darkBg);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(68, 68, 68)),
            BorderFactory.createEmptyBorder(8, 16, 8, 16) // Reduced vertical padding for more minimalist look
        ));
        
        // Create left side with smaller, more minimalist title
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftPanel.setOpaque(false);
        
        JLabel title = new JLabel("TriageMate Chat");
        title.setFont(new Font("Segoe UI", Font.PLAIN, 14)); // Smaller, non-bold font for minimalist look
        title.setForeground(new Color(180, 180, 180)); // Lighter color for subtle appearance
        leftPanel.add(title);
        
        header.add(leftPanel, BorderLayout.WEST);
        
        // Settings button with better right alignment
        JButton settingsButton = new JButton("⚙");
        settingsButton.setFont(new Font("Segoe UI", Font.PLAIN, 14)); // Smaller font
        settingsButton.setForeground(new Color(180, 180, 180));
        settingsButton.setBackground(darkBg);
        settingsButton.setBorderPainted(false);
        settingsButton.setFocusPainted(false);
        settingsButton.setContentAreaFilled(false);
        settingsButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        settingsButton.setToolTipText("Settings");
        settingsButton.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0)); // Add left padding for better spacing
        settingsButton.addActionListener(e -> {
            showSettingsTab = !showSettingsTab;
            refreshMainPanel();
        });
        
        // Create a panel to hold the settings button for better right alignment
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightPanel.setOpaque(false);
        rightPanel.add(settingsButton);
        header.add(rightPanel, BorderLayout.EAST);
        
        return header;
    }

    /**
     * Creates the settings panel
     */
    private JPanel createSettingsPanel() {
        Color darkBg = UIManager.getColor("Panel.background");
        if (darkBg == null) {
            darkBg = new Color(43, 43, 43);
        }
        
        JPanel settingsPanel = new JPanel(new BorderLayout());
        settingsPanel.setBackground(darkBg);
        
        JLabel placeholder = new JLabel("Settings page (placeholder)");
        placeholder.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        placeholder.setForeground(Color.WHITE);
        placeholder.setHorizontalAlignment(SwingConstants.CENTER);
        settingsPanel.add(placeholder, BorderLayout.CENTER);
        
        // Back to chat button
        JButton backToChat = new JButton("Back to Chat");
        backToChat.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        backToChat.setFocusPainted(false);
        backToChat.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backToChat.addActionListener(e -> {
            showSettingsTab = false;
            refreshMainPanel();
        });
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(darkBg);
        buttonPanel.add(backToChat);
        settingsPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        return settingsPanel;
    }

    /**
     * Refreshes the main panel when switching tabs
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