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
    private final JPanel chatPanel;
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
        this.chatPanel = new JPanel();
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
        // Configure main panel
        mainPanel.setBorder(JBUI.Borders.empty(8));
        
        // Setup header panel
        setupHeaderPanel();
        
        // Setup chat panel
        setupChatPanel();
        
        // Setup input panel
        setupInputPanel();
        
        // Add components to main panel
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(chatPanel, BorderLayout.CENTER);
        mainPanel.add(inputPanel, BorderLayout.SOUTH);
    }

    /**
     * Sets up the header panel with failure metadata
     */
    private void setupHeaderPanel() {
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        
        // Configure header label
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 14f));
        headerLabel.setForeground(Color.BLACK);
        
        // Configure status label
        statusLabel.setForeground(Color.GRAY);
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 11f));
        
        headerPanel.add(headerLabel, BorderLayout.CENTER);
        headerPanel.add(statusLabel, BorderLayout.EAST);
    }

    /**
     * Sets up the chat panel for message display
     */
    private void setupChatPanel() {
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        chatPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        chatPanel.setBackground(Color.WHITE);
        
        // Add scroll pane
        JBScrollPane scrollPane = new JBScrollPane(chatPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        
        // Replace chat panel with scroll pane in main panel
        mainPanel.remove(chatPanel);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Sets up the input panel with text area and send button
     */
    private void setupInputPanel() {
        inputPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        
        // Configure input area
        inputArea.setRows(3);
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY),
            BorderFactory.createEmptyBorder(4, 4, 4, 4)
        ));
        
        // Configure send button
        sendButton.setText("Send");
        sendButton.setPreferredSize(new Dimension(80, 30));
        
        // Add components to input panel
        inputPanel.add(new JBScrollPane(inputArea), BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
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
        // For now, just show a placeholder response
        addMessage(new ChatMessage(ChatMessage.Role.AI, 
            "This is a placeholder response. AI integration will be added in Phase 5.", 
            System.currentTimeMillis()));
    }

    /**
     * Adds a message to the chat history and updates the UI
     */
    private void addMessage(ChatMessage message) {
        chatHistory.add(message);
        addMessageToUI(message);
        scrollToBottom();
    }

    /**
     * Adds a message component to the UI
     */
    private void addMessageToUI(ChatMessage message) {
        JPanel messagePanel = createMessagePanel(message);
        chatPanel.add(messagePanel);
        chatPanel.revalidate();
        chatPanel.repaint();
    }

    /**
     * Creates a message panel for display
     */
    private JPanel createMessagePanel(ChatMessage message) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        
        // Message content panel
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        
        // Set background color based on role
        if (message.getRole() == ChatMessage.Role.USER) {
            contentPanel.setBackground(new Color(227, 242, 253)); // Light blue
            panel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        } else {
            contentPanel.setBackground(new Color(245, 245, 245)); // Light gray
            panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        
        // Message text
        JBLabel messageLabel = new JBLabel("<html><body style='width: 300px'>" + 
            message.getText().replace("\n", "<br>") + "</body></html>");
        messageLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        
        // Copy button
        JButton copyButton = new JButton("Copy");
        copyButton.setPreferredSize(new Dimension(60, 24));
        copyButton.addActionListener(e -> copyToClipboard(message.getText()));
        
        // Timestamp
        JBLabel timestampLabel = new JBLabel(formatTimestamp(message.getTimestamp()));
        timestampLabel.setForeground(Color.GRAY);
        timestampLabel.setFont(timestampLabel.getFont().deriveFont(Font.PLAIN, 10f));
        
        // Add components
        contentPanel.add(messageLabel, BorderLayout.CENTER);
        contentPanel.add(copyButton, BorderLayout.EAST);
        
        panel.add(contentPanel, BorderLayout.CENTER);
        panel.add(timestampLabel, BorderLayout.SOUTH);
        
        return panel;
    }

    /**
     * Copies text to clipboard
     */
    private void copyToClipboard(String text) {
        CopyPasteManager.getInstance().setContents(new StringSelection(text));
        // TODO: Add visual feedback
    }

    /**
     * Formats timestamp for display
     */
    private String formatTimestamp(long timestamp) {
        return new java.text.SimpleDateFormat("HH:mm").format(new java.util.Date(timestamp));
    }

    /**
     * Scrolls the chat to the bottom
     */
    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            Container parent = chatPanel.getParent();
            if (parent instanceof JViewport) {
                JViewport viewport = (JViewport) parent;
                viewport.setViewPosition(new Point(0, chatPanel.getHeight()));
            }
        });
    }

    /**
     * Updates the panel with new failure information
     */
    public void updateFailure(FailureInfo failureInfo) {
        System.out.println("TriageMate: TriagePanelView.updateFailure called for scenario: " + 
            (failureInfo.getScenarioName() != null ? failureInfo.getScenarioName() : "null"));
        
        this.currentFailure = failureInfo;
        
        // Update header
        updateHeader(failureInfo);
        
        // Clear chat history for new failure
        clearChat();
        
        // Generate and display initial prompt
        generateAndDisplayPrompt(failureInfo);
        
        System.out.println("TriageMate: TriagePanelView.updateFailure completed successfully");
    }

    /**
     * Updates the header with failure metadata
     */
    private void updateHeader(FailureInfo failureInfo) {
        String scenarioName = failureInfo.getScenarioName() != null ? 
            failureInfo.getScenarioName() : "Unknown Scenario";
        String errorType = failureInfo.getAssertionType() != null ? 
            failureInfo.getAssertionType() : "Unknown Error";
        
        headerLabel.setText(scenarioName + " - " + errorType);
        statusLabel.setText("New failure detected");
    }

    /**
     * Clears the chat history and UI
     */
    private void clearChat() {
        chatHistory.clear();
        chatPanel.removeAll();
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
            
            addMessage(new ChatMessage(ChatMessage.Role.AI, prompt, System.currentTimeMillis()));
            
        } catch (Exception e) {
            String errorMessage = "Error generating prompt: " + e.getMessage();
            addMessage(new ChatMessage(ChatMessage.Role.AI, errorMessage, System.currentTimeMillis()));
        }
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
        public enum Role { USER, AI }
        
        private final Role role;
        private final String text;
        private final long timestamp;
        
        public ChatMessage(Role role, String text, long timestamp) {
            this.role = role;
            this.text = text;
            this.timestamp = timestamp;
        }
        
        public Role getRole() { return role; }
        public String getText() { return text; }
        public long getTimestamp() { return timestamp; }
    }
} 