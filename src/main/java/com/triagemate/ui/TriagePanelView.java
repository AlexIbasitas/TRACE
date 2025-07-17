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
 * Implements a robust chat interface following Swing best practices.
 */
public class TriagePanelView {
    private final Project project;
    private final JPanel mainPanel;
    private final JPanel inputPanel;
    private final JBTextArea inputArea;
    private final JButton sendButton;
    private final JBLabel headerLabel;
    private final JBLabel statusLabel;
    
    // Chat state management
    private final List<ChatMessage> chatHistory;
    private final LocalPromptGenerationService promptService;
    private final BackendCommunicationService backendService;
    private JScrollPane chatScrollPane;
    private JPanel messageContainer;
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
        this.inputPanel = new JPanel(new BorderLayout());
        this.inputArea = new JBTextArea();
        this.sendButton = new JButton("Send");
        this.headerLabel = new JBLabel("No test failure detected");
        this.statusLabel = new JBLabel("");
        
        initializeUI();
        setupEventHandlers();
    }

    /**
     * Initializes the UI components with proper chat interface
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
     * Sets up the chat panel with proper layout management
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
        addMessage(new ChatMessage(ChatMessage.Role.USER, messageText, System.currentTimeMillis(), null, null));
        
        // Clear input area
        inputArea.setText("");
        
        // Show backend configuration message
        String backendMessage = "AI backend service is not currently configured. To enable AI-powered test failure analysis, please configure your backend connection in the settings.";
        
        addMessage(new ChatMessage(ChatMessage.Role.AI, 
            backendMessage, 
            System.currentTimeMillis(), null, null));
    }

    /**
     * Adds a message to the chat history and updates the UI with proper EDT compliance
     */
    private void addMessage(ChatMessage message) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> addMessage(message));
            return;
        }
        
        chatHistory.add(message);
        addMessageToUI(message);
    }

    /**
     * Adds a message component to the UI with proper layout
     */
    private void addMessageToUI(ChatMessage message) {
        System.out.println("=== addMessageToUI Debug ===");
        System.out.println("Adding message to UI - role: " + message.getRole());
        System.out.println("Message text: '" + message.getText() + "'");
        System.out.println("Chat history size: " + chatHistory.size());
        
        // Create the new message component
        MessageComponent messageComponent = new MessageComponent(message);
        
        // Remove the vertical glue temporarily
        messageContainer.removeAll();
        
        // Add all existing messages with proper spacing
        for (int i = 0; i < chatHistory.size(); i++) {
            ChatMessage existingMessage = chatHistory.get(i);
            MessageComponent existingComponent = new MessageComponent(existingMessage);
            messageContainer.add(existingComponent);
            
            System.out.println("Added message " + i + " to container - component size: " + existingComponent.getPreferredSize());
            
            // Add spacing between messages, but not after the last one
            if (i < chatHistory.size() - 1) {
                messageContainer.add(Box.createVerticalStrut(16));
                System.out.println("Added vertical strut between messages");
            }
        }
        
        // Add vertical glue to push messages to top
        messageContainer.add(Box.createVerticalGlue());
        
        // Revalidate and repaint
        messageContainer.revalidate();
        messageContainer.repaint();
        
        System.out.println("Final message container size: " + messageContainer.getPreferredSize());
        System.out.println("Message container component count: " + messageContainer.getComponentCount());
        System.out.println("================================");
        
        // Scroll to bottom
        scrollToBottom();
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
        // Ensure we're on the EDT
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> updateFailure(failureInfo));
            return;
        }
        
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
        messageContainer.removeAll();
        messageContainer.add(Box.createVerticalGlue());
        messageContainer.revalidate();
        messageContainer.repaint();
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
            
            // Create a special AI message with the prompt in the collapsible section AND failure info
            addMessage(new ChatMessage(ChatMessage.Role.AI, "", System.currentTimeMillis(), prompt, failureInfo));
            
        } catch (Exception e) {
            String errorMessage = "Error generating prompt: " + e.getMessage();
            addMessage(new ChatMessage(ChatMessage.Role.AI, errorMessage, System.currentTimeMillis(), null, failureInfo));
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
     * Inner class representing a chat message with embedded failure info
     */
    private static class ChatMessage {
        public enum Role { USER, AI, SYSTEM }
        
        private final Role role;
        private final String text;
        private final long timestamp;
        private final String aiThinking;
        private final FailureInfo failureInfo; // Direct reference to failure info
        
        public ChatMessage(Role role, String text, long timestamp) {
            this(role, text, timestamp, null, null);
        }
        
        public ChatMessage(Role role, String text, long timestamp, String aiThinking) {
            this(role, text, timestamp, aiThinking, null);
        }
        
        public ChatMessage(Role role, String text, long timestamp, String aiThinking, FailureInfo failureInfo) {
            this.role = role;
            this.text = text;
            this.timestamp = timestamp;
            this.aiThinking = aiThinking;
            this.failureInfo = failureInfo;
        }
        
        public Role getRole() { return role; }
        public String getText() { return text; }
        public long getTimestamp() { return timestamp; }
        public String getAiThinking() { return aiThinking; }
        public FailureInfo getFailureInfo() { return failureInfo; }
    }

    /**
     * Dedicated message component for consistent message rendering
     */
    private static class MessageComponent extends JPanel {
        private final ChatMessage message;
        
        public MessageComponent(ChatMessage message) {
            this.message = message;
            setLayout(new BorderLayout());
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            
            System.out.println("=== MessageComponent Debug ===");
            System.out.println("Creating MessageComponent for role: " + message.getRole());
            System.out.println("Message text: '" + message.getText() + "'");
            System.out.println("Message text length: " + (message.getText() != null ? message.getText().length() : 0));
            System.out.println("Has AI thinking: " + (message.getAiThinking() != null && !message.getAiThinking().trim().isEmpty()));
            System.out.println("Has failure info: " + (message.getFailureInfo() != null));
            System.out.println("Initial preferred size: " + getPreferredSize());
            System.out.println("Initial maximum size: " + getMaximumSize());
            
            initializeComponents();
            
            System.out.println("After initialization - preferred size: " + getPreferredSize());
            System.out.println("After initialization - maximum size: " + getMaximumSize());
            System.out.println("===============================");
        }
        
        private void initializeComponents() {
            add(createHeader(), BorderLayout.NORTH);
            add(createContent(), BorderLayout.CENTER);
        }
        
        private JPanel createHeader() {
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
                logoLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
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
            
            return headerPanel;
        }
        
        private JPanel createContent() {
            JPanel contentPanel = new JPanel(new BorderLayout());
            contentPanel.setOpaque(false);
            
            System.out.println("=== createContent Debug ===");
            System.out.println("Creating content panel for role: " + message.getRole());
            System.out.println("Content panel initial size: " + contentPanel.getPreferredSize());
            
            // Add scenario information for AI messages if available
            if ((message.getRole() == ChatMessage.Role.AI || message.getRole() == ChatMessage.Role.SYSTEM) && 
                message.getFailureInfo() != null) {
                
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
                
                JLabel scenarioName = new JLabel(message.getFailureInfo().getScenarioName() != null ? 
                    message.getFailureInfo().getScenarioName() : "Unknown Scenario");
                scenarioName.setFont(new Font("Segoe UI", Font.BOLD, 13));
                scenarioName.setForeground(Color.WHITE);
                
                scenarioLabelPanel.add(scenarioPrefix);
                scenarioLabelPanel.add(scenarioName);
                scenarioPanel.add(scenarioLabelPanel, BorderLayout.WEST);
                
                contentPanel.add(scenarioPanel, BorderLayout.NORTH);
                
                // Add failed step information
                if (message.getFailureInfo().getFailedStepText() != null && 
                    !message.getFailureInfo().getFailedStepText().trim().isEmpty()) {
                    
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
                    
                    JLabel failedStepText = new JLabel(message.getFailureInfo().getFailedStepText());
                    failedStepText.setFont(new Font("Segoe UI", Font.BOLD, 13));
                    failedStepText.setForeground(Color.WHITE);
                    
                    failedStepLabelPanel.add(failureSymbol);
                    failedStepLabelPanel.add(failedStepPrefix);
                    failedStepLabelPanel.add(failedStepText);
                    failedStepPanel.add(failedStepLabelPanel, BorderLayout.WEST);
                    
                    contentPanel.add(failedStepPanel, BorderLayout.CENTER);
                }
            }
            
            // Add AI thinking section for AI messages
            if ((message.getRole() == ChatMessage.Role.AI || message.getRole() == ChatMessage.Role.SYSTEM) && 
                message.getAiThinking() != null && !message.getAiThinking().trim().isEmpty()) {
                
                CollapsiblePanel aiThinkingPanel = new CollapsiblePanel("AI Thinking", message.getAiThinking());
                contentPanel.add(aiThinkingPanel, BorderLayout.SOUTH);
            }
            
            // Create message text component - use JLabel for simple text, JTextArea for multi-line content
            if (message.getText() != null && !message.getText().trim().isEmpty()) {
                if (message.getText().contains("\n") || message.getText().length() > 100) {
                    // Use JTextArea for multi-line or long content
                    JTextArea messageText = new JTextArea(message.getText());
                    messageText.setLineWrap(true);
                    messageText.setWrapStyleWord(true);
                    messageText.setEditable(false);
                    messageText.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                    messageText.setBackground(getBackground());
                    messageText.setForeground(Color.WHITE);
                    messageText.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
                    messageText.setOpaque(false);
                    
                    // Let JTextArea calculate its natural size with minimal padding
                    Dimension preferredSize = messageText.getPreferredSize();
                    System.out.println("=== JTextArea Debug ===");
                    System.out.println("Message text: '" + message.getText() + "'");
                    System.out.println("Text length: " + message.getText().length());
                    System.out.println("Contains newlines: " + message.getText().contains("\n"));
                    System.out.println("Line count: " + messageText.getLineCount());
                    System.out.println("Preferred size: " + preferredSize);
                    System.out.println("Font: " + messageText.getFont());
                    System.out.println("Font metrics height: " + messageText.getFontMetrics(messageText.getFont()).getHeight());
                    System.out.println("Font metrics ascent: " + messageText.getFontMetrics(messageText.getFont()).getAscent());
                    System.out.println("Font metrics descent: " + messageText.getFontMetrics(messageText.getFont()).getDescent());
                    System.out.println("Font metrics leading: " + messageText.getFontMetrics(messageText.getFont()).getLeading());
                    
                    messageText.setPreferredSize(preferredSize);
                    messageText.setMaximumSize(new Dimension(Integer.MAX_VALUE, preferredSize.height + 4));
                    
                    System.out.println("Final preferred size: " + messageText.getPreferredSize());
                    System.out.println("Final maximum size: " + messageText.getMaximumSize());
                    System.out.println("=====================");
                    
                    contentPanel.add(messageText, BorderLayout.CENTER);
                } else {
                    // Use JLabel for simple text
                    JLabel messageLabel = new JLabel(message.getText());
                    messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                    messageLabel.setForeground(Color.WHITE);
                    messageLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
                    
                    contentPanel.add(messageLabel, BorderLayout.CENTER);
                }
            }
            
            System.out.println("Content panel final size: " + contentPanel.getPreferredSize());
            System.out.println("Content panel maximum size: " + contentPanel.getMaximumSize());
            System.out.println("===========================");
            
            return contentPanel;
        }
        
        private String formatFullTimestamp(long timestamp) {
            return new java.text.SimpleDateFormat("h:mm a, MMM d, yyyy").format(new java.util.Date(timestamp));
        }
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
            setMaximumSize(new Dimension(Integer.MAX_VALUE, Short.MAX_VALUE));
            
            // Create toggle button with expand/collapse indicator
            toggleLabel = new JLabel("▶ Show AI Thinking");
            toggleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
            toggleLabel.setForeground(Color.WHITE);
            toggleLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            toggleLabel.setToolTipText("Click to " + (isExpanded ? "hide" : "show") + " AI thinking");
            
            // Create content panel
            contentPanel = new JPanel(new BorderLayout());
            contentPanel.setOpaque(false);
            contentPanel.setBorder(BorderFactory.createEmptyBorder(8, 16, 0, 0));
            contentPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Short.MAX_VALUE));
            
            if (content != null && !content.trim().isEmpty()) {
                JTextArea contentArea = new JTextArea(content);
                contentArea.setLineWrap(true);
                contentArea.setWrapStyleWord(true);
                contentArea.setEditable(false);
                contentArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                contentArea.setForeground(new Color(200, 200, 200));
                contentArea.setBackground(new Color(50, 50, 50));
                contentArea.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(80, 80, 80), 1),
                    BorderFactory.createEmptyBorder(8, 8, 8, 8)
                ));
                contentArea.setOpaque(true);
                
                // Let JTextArea calculate its natural size with minimal padding
                Dimension preferredSize = contentArea.getPreferredSize();
                System.out.println("=== CollapsiblePanel JTextArea Debug ===");
                System.out.println("Content text: '" + content + "'");
                System.out.println("Content length: " + content.length());
                System.out.println("Contains newlines: " + content.contains("\n"));
                System.out.println("Line count: " + contentArea.getLineCount());
                System.out.println("Preferred size: " + preferredSize);
                System.out.println("Font: " + contentArea.getFont());
                System.out.println("Font metrics height: " + contentArea.getFontMetrics(contentArea.getFont()).getHeight());
                
                contentArea.setPreferredSize(preferredSize);
                contentArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, preferredSize.height + 4));
                
                System.out.println("Final preferred size: " + contentArea.getPreferredSize());
                System.out.println("Final maximum size: " + contentArea.getMaximumSize());
                System.out.println("================================");
                
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
        }
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
            BorderFactory.createEmptyBorder(8, 16, 8, 16)
        ));
        
        // Create left side with smaller, more minimalist title
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftPanel.setOpaque(false);
        
        JLabel title = new JLabel("TriageMate Chat");
        title.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        title.setForeground(new Color(180, 180, 180));
        leftPanel.add(title);
        
        header.add(leftPanel, BorderLayout.WEST);
        
        // Settings button with better right alignment
        JButton settingsButton = new JButton("⚙");
        settingsButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        settingsButton.setForeground(new Color(180, 180, 180));
        settingsButton.setBackground(darkBg);
        settingsButton.setBorderPainted(false);
        settingsButton.setFocusPainted(false);
        settingsButton.setContentAreaFilled(false);
        settingsButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        settingsButton.setToolTipText("Settings");
        settingsButton.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
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