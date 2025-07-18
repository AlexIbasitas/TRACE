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
        
        // Configure text area for multi-line input
        inputArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputArea.setRows(3); // Allow multiple rows
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        inputArea.setBackground(new Color(50, 50, 50));
        inputArea.setForeground(Color.WHITE);
        inputArea.setCaretColor(Color.WHITE);
        inputArea.setOpaque(false);
        inputArea.putClientProperty("JTextField.placeholderText", "Ask anything about the test failure...");
        
        // Create modern send button with custom icon
        JButton sendIconButton = createModernSendButton();
        
        // Create a fixed-size container for the send button with vertical centering
        JPanel buttonContainer = new JPanel();
        buttonContainer.setLayout(new BoxLayout(buttonContainer, BoxLayout.Y_AXIS));
        buttonContainer.setOpaque(false);
        buttonContainer.setPreferredSize(new Dimension(50, 40));
        buttonContainer.setMaximumSize(new Dimension(50, 40));
        buttonContainer.setMinimumSize(new Dimension(50, 40));
        
        // Add vertical glue to center the button
        buttonContainer.add(Box.createVerticalGlue());
        buttonContainer.add(sendIconButton);
        buttonContainer.add(Box.createVerticalGlue());
        
        // Add components to input container
        inputContainer.add(inputArea, BorderLayout.CENTER);
        inputContainer.add(buttonContainer, BorderLayout.EAST);
        buttonContainer.add(sendIconButton);
        
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
            existingComponent.setAlignmentY(Component.TOP_ALIGNMENT);
            messageContainer.add(existingComponent);
            
            System.out.println("Added message " + i + " to container - component size: " + existingComponent.getPreferredSize());
            
            // In addMessageToUI - after adding each component
            System.out.println("=== Container Layout Debug ===");
            System.out.println("MessageContainer layout: " + messageContainer.getLayout().getClass().getSimpleName());
            System.out.println("MessageContainer preferred size: " + messageContainer.getPreferredSize());
            System.out.println("MessageContainer minimum size: " + messageContainer.getMinimumSize());
            System.out.println("MessageContainer maximum size: " + messageContainer.getMaximumSize());
            System.out.println("Component " + i + " preferred size: " + existingComponent.getPreferredSize());
            System.out.println("Component " + i + " actual size: " + existingComponent.getSize());
            
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
        
        // After revalidate/repaint
        System.out.println("=== Post-Layout Debug ===");
        System.out.println("ScrollPane viewport size: " + chatScrollPane.getViewport().getSize());
        System.out.println("ScrollPane viewport preferred size: " + chatScrollPane.getViewport().getPreferredSize());
        System.out.println("Final message container size: " + messageContainer.getSize());
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
        private CollapsiblePanel collapsiblePanel; // Reference to track collapsible panel
        
        public MessageComponent(ChatMessage message) {
            this.message = message;
            setLayout(new BorderLayout());
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, getPreferredSize().height + 16));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setAlignmentY(Component.TOP_ALIGNMENT);
            
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
            
            // In MessageComponent constructor - after initialization
            System.out.println("=== MessageComponent Sizing Debug ===");
            System.out.println("Role: " + message.getRole());
            System.out.println("Preferred size: " + getPreferredSize());
            System.out.println("Minimum size: " + getMinimumSize());
            System.out.println("Maximum size: " + getMaximumSize());
            System.out.println("Alignment X: " + getAlignmentX());
            System.out.println("Alignment Y: " + getAlignmentY());
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
            // NEW FIX: Use BoxLayout instead of BorderLayout to stack components vertically
            JPanel contentPanel = new JPanel();
            contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
            contentPanel.setOpaque(false);
            
            System.out.println("=== NEW FIX: BoxLayout createContent Debug ===");
            System.out.println("Creating content panel for role: " + message.getRole());
            System.out.println("Content panel layout: " + contentPanel.getLayout().getClass().getSimpleName());
            System.out.println("Content panel initial size: " + contentPanel.getPreferredSize());
            
            // Track component order for debugging
            int componentIndex = 0;
            
            // Add scenario information for AI messages if available
            if ((message.getRole() == ChatMessage.Role.AI || message.getRole() == ChatMessage.Role.SYSTEM) && 
                message.getFailureInfo() != null) {
                
                System.out.println("Adding scenario component at index: " + componentIndex++);
                
                // Add scenario information
                JPanel scenarioPanel = new JPanel(new BorderLayout());
                scenarioPanel.setOpaque(false);
                scenarioPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
                scenarioPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
                
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
                
                contentPanel.add(scenarioPanel);
                System.out.println("Scenario panel added - preferred size: " + scenarioPanel.getPreferredSize());
                
                // Add failed step information
                if (message.getFailureInfo().getFailedStepText() != null && 
                    !message.getFailureInfo().getFailedStepText().trim().isEmpty()) {
                    
                    System.out.println("Adding failed step component at index: " + componentIndex++);
                    
                    JPanel failedStepPanel = new JPanel(new BorderLayout());
                    failedStepPanel.setOpaque(false);
                    failedStepPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
                    failedStepPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
                    
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
                    
                    contentPanel.add(failedStepPanel);
                    System.out.println("Failed step panel added - preferred size: " + failedStepPanel.getPreferredSize());
                }
            }
            
            // Create message text component - always use JTextArea for consistent spacing
            if (message.getText() != null && !message.getText().trim().isEmpty()) {
                System.out.println("Adding message text component at index: " + componentIndex++);
                
                JTextArea messageText = new JTextArea(message.getText());
                messageText.setLineWrap(true);
                messageText.setWrapStyleWord(true);
                messageText.setEditable(false);
                messageText.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                messageText.setBackground(getBackground());
                messageText.setForeground(Color.WHITE);
                messageText.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
                messageText.setOpaque(false);
                messageText.setAlignmentX(Component.LEFT_ALIGNMENT);
                
                // FIX: Calculate proper size for wrapped text
                // First, set a reasonable width to calculate wrapped height
                int maxWidth = 600; // Reasonable max width for chat messages
                messageText.setSize(maxWidth, Short.MAX_VALUE);
                
                // Get the preferred size after setting the width
                Dimension preferredSize = messageText.getPreferredSize();
                
                System.out.println("=== FIXED BoxLayout JTextArea Debug ===");
                System.out.println("Message text: '" + message.getText() + "'");
                System.out.println("Text length: " + message.getText().length());
                System.out.println("Contains newlines: " + message.getText().contains("\n"));
                System.out.println("Line count: " + messageText.getLineCount());
                System.out.println("Set width: " + maxWidth);
                System.out.println("Calculated preferred size: " + preferredSize);
                System.out.println("Font: " + messageText.getFont());
                System.out.println("Font metrics height: " + messageText.getFontMetrics(messageText.getFont()).getHeight());
                
                // Set the calculated size
                messageText.setPreferredSize(preferredSize);
                messageText.setMaximumSize(new Dimension(Integer.MAX_VALUE, preferredSize.height + 4));
                messageText.setAlignmentY(Component.TOP_ALIGNMENT);
                
                System.out.println("Final preferred size: " + messageText.getPreferredSize());
                System.out.println("Final maximum size: " + messageText.getMaximumSize());
                System.out.println("Alignment X: " + messageText.getAlignmentX());
                System.out.println("=====================================");
                
                contentPanel.add(messageText);
                System.out.println("Message text added - preferred size: " + messageText.getPreferredSize());
            }
            
            // Add AI thinking section for AI messages
            if ((message.getRole() == ChatMessage.Role.AI || message.getRole() == ChatMessage.Role.SYSTEM) && 
                message.getAiThinking() != null && !message.getAiThinking().trim().isEmpty()) {
                
                System.out.println("Adding collapsible panel component at index: " + componentIndex++);
                
                collapsiblePanel = new CollapsiblePanel("AI Thinking", message.getAiThinking(), this);
                collapsiblePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
                contentPanel.add(collapsiblePanel);
                System.out.println("Collapsible panel added - preferred size: " + collapsiblePanel.getPreferredSize());
            }
            
            System.out.println("=== BoxLayout Final Debug ===");
            System.out.println("Content panel layout: " + contentPanel.getLayout().getClass().getSimpleName());
            System.out.println("Content panel component count: " + contentPanel.getComponentCount());
            System.out.println("Content panel final preferred size: " + contentPanel.getPreferredSize());
            System.out.println("Content panel final maximum size: " + contentPanel.getMaximumSize());
            
            // Debug each component in the BoxLayout
            for (int i = 0; i < contentPanel.getComponentCount(); i++) {
                Component comp = contentPanel.getComponent(i);
                System.out.println("Component " + i + ": " + comp.getClass().getSimpleName() + 
                                 " - preferred size: " + comp.getPreferredSize() + 
                                 " - alignment X: " + comp.getAlignmentX());
            }
            System.out.println("=====================================");
            
            return contentPanel;
        }
        
        // Removed the moderate fix - reverting to original preferred size logic
        
        private int getContentHeight() {
            // Calculate actual content height based on components
            int height = 0;
            
            // Add header height (timestamp + sender info)
            height += 24; // Approximate header height
            
            // Add message text height if present
            if (message.getText() != null && !message.getText().trim().isEmpty()) {
                // Estimate text height based on content
                int textHeight = estimateTextHeight(message.getText());
                height += textHeight;
            }
            
            // Add scenario/failed step height if present
            if (message.getFailureInfo() != null) {
                height += 40; // Approximate height for scenario + failed step
            }
            
            // Add AI thinking toggle height if present
            if (message.getAiThinking() != null && !message.getAiThinking().trim().isEmpty()) {
                height += 24; // Approximate height for toggle
            }
            
            return height;
        }
        
        private int estimateTextHeight(String text) {
            // Simple estimation based on text length and line breaks
            int lines = 1;
            if (text.contains("\n")) {
                lines = text.split("\n").length;
            } else if (text.length() > 80) {
                // Estimate line wrapping
                lines = (text.length() / 80) + 1;
            }
            return lines * 18; // Approximate line height
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
        private final MessageComponent parentMessageComponent;
        
        public CollapsiblePanel(String title, String content, MessageComponent parent) {
            this.parentMessageComponent = parent;
            System.out.println("=== NEW FIX: BoxLayout CollapsiblePanel Constructor Debug ===");
            System.out.println("Creating CollapsiblePanel with title: '" + title + "'");
            System.out.println("Content length: " + (content != null ? content.length() : 0));
            
            setLayout(new BorderLayout());
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, Short.MAX_VALUE));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            
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
                
                // FIX: Calculate proper size for wrapped text in collapsible panel
                int maxWidth = 550; // Slightly smaller for collapsible content
                contentArea.setSize(maxWidth, Short.MAX_VALUE);
                
                // Get the preferred size after setting the width
                Dimension preferredSize = contentArea.getPreferredSize();
                
                System.out.println("=== FIXED CollapsiblePanel JTextArea Debug ===");
                System.out.println("Content text: '" + content + "'");
                System.out.println("Content length: " + content.length());
                System.out.println("Contains newlines: " + content.contains("\n"));
                System.out.println("Line count: " + contentArea.getLineCount());
                System.out.println("Set width: " + maxWidth);
                System.out.println("Calculated preferred size: " + preferredSize);
                System.out.println("Font: " + contentArea.getFont());
                System.out.println("Font metrics height: " + contentArea.getFontMetrics(contentArea.getFont()).getHeight());
                
                contentArea.setPreferredSize(preferredSize);
                contentArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, preferredSize.height + 4));
                
                System.out.println("Final preferred size: " + contentArea.getPreferredSize());
                System.out.println("Final maximum size: " + contentArea.getMaximumSize());
                System.out.println("=========================================");
                
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
            
            System.out.println("=== NEW FIX: BoxLayout CollapsiblePanel Constructor Complete ===");
            System.out.println("Initial preferred size: " + getPreferredSize());
            System.out.println("Initial maximum size: " + getMaximumSize());
            System.out.println("Alignment X: " + getAlignmentX());
            System.out.println("Toggle label text: " + toggleLabel.getText());
            System.out.println("Content panel visible: " + contentPanel.isVisible());
            System.out.println("Content panel preferred size: " + contentPanel.getPreferredSize());
            System.out.println("===============================================================");
        }
        
        private void toggleExpanded() {
            System.out.println("=== NEW FIX: BoxLayout CollapsiblePanel Toggle Debug ===");
            System.out.println("Before toggle - isExpanded: " + isExpanded);
            System.out.println("Before toggle - contentPanel visible: " + contentPanel.isVisible());
            System.out.println("Before toggle - contentPanel size: " + contentPanel.getSize());
            System.out.println("Before toggle - contentPanel preferred size: " + contentPanel.getPreferredSize());
            System.out.println("Before toggle - this component size: " + getSize());
            System.out.println("Before toggle - this component preferred size: " + getPreferredSize());
            System.out.println("Before toggle - parent layout: " + getParent().getLayout().getClass().getSimpleName());
            System.out.println("Before toggle - parent size: " + getParent().getSize());
            System.out.println("Before toggle - parent component count: " + getParent().getComponentCount());
            
            // Debug parent's BoxLayout components before toggle
            if (getParent().getLayout() instanceof BoxLayout) {
                System.out.println("=== Parent BoxLayout Components Before Toggle ===");
                for (int i = 0; i < getParent().getComponentCount(); i++) {
                    Component comp = getParent().getComponent(i);
                    System.out.println("Parent component " + i + ": " + comp.getClass().getSimpleName() + 
                                     " - size: " + comp.getSize() + 
                                     " - preferred size: " + comp.getPreferredSize() +
                                     " - visible: " + comp.isVisible());
                }
                System.out.println("===============================================");
            }
            
            isExpanded = !isExpanded;
            contentPanel.setVisible(isExpanded);
            toggleLabel.setText((isExpanded ? "▼ " : "▶ ") + "Show AI Thinking");
            toggleLabel.setToolTipText("Click to " + (isExpanded ? "hide" : "show") + " AI thinking");
            
            System.out.println("After toggle - isExpanded: " + isExpanded);
            System.out.println("After toggle - contentPanel visible: " + contentPanel.isVisible());
            System.out.println("After toggle - contentPanel size: " + contentPanel.getSize());
            System.out.println("After toggle - contentPanel preferred size: " + contentPanel.getPreferredSize());
            System.out.println("After toggle - this component size: " + getSize());
            System.out.println("After toggle - this component preferred size: " + getPreferredSize());
            
            // Trigger revalidation for BoxLayout
            revalidate();
            repaint();
            
            // Revalidate parent to ensure BoxLayout recalculates
            if (parentMessageComponent != null) {
                System.out.println("Revalidating parent message component");
                parentMessageComponent.revalidate();
                parentMessageComponent.repaint();
            }
            
            // Add a timer to check sizes after layout
            Timer timer = new Timer(100, e -> {
                System.out.println("=== Post-Layout BoxLayout CollapsiblePanel Debug ===");
                System.out.println("Post-layout - contentPanel size: " + contentPanel.getSize());
                System.out.println("Post-layout - this component size: " + getSize());
                System.out.println("Post-layout - parent size: " + getParent().getSize());
                System.out.println("Post-layout - parent preferred size: " + getParent().getPreferredSize());
                System.out.println("Post-layout - parent component count: " + getParent().getComponentCount());
                
                // Debug parent's BoxLayout components after toggle
                if (getParent().getLayout() instanceof BoxLayout) {
                    System.out.println("=== Parent BoxLayout Components After Toggle ===");
                    for (int i = 0; i < getParent().getComponentCount(); i++) {
                        Component comp = getParent().getComponent(i);
                        System.out.println("Parent component " + i + ": " + comp.getClass().getSimpleName() + 
                                         " - size: " + comp.getSize() + 
                                         " - preferred size: " + comp.getPreferredSize() +
                                         " - visible: " + comp.isVisible());
                    }
                    System.out.println("==============================================");
                }
                System.out.println("==================================================");
                ((Timer)e.getSource()).stop();
            });
            timer.setRepeats(false);
            timer.start();
        }

        public boolean isExpanded() {
            return isExpanded;
        }
        public JPanel getContentPanel() {
            return contentPanel;
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
     * Creates a modern send button with custom icon and styling
     */
    private JButton createModernSendButton() {
        JButton sendButton = new JButton();
        
        // Load the send icon
        try {
            Icon sendIcon = IconLoader.getIcon("/icons/send_32.png", getClass());
            sendButton.setIcon(sendIcon);
        } catch (Exception e) {
            // Fallback to text if icon not found
            sendButton.setText("→");
            sendButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        }
        
        // Modern styling - transparent background with just icon
        sendButton.setPreferredSize(new Dimension(32, 32));
        sendButton.setMaximumSize(new Dimension(32, 32));
        sendButton.setMinimumSize(new Dimension(32, 32));
        
        // Transparent background - no background color
        sendButton.setBackground(new Color(0, 0, 0, 0)); // Fully transparent
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
        sendButton.setBorderPainted(false);
        sendButton.setContentAreaFilled(false); // No background fill
        sendButton.setOpaque(false); // Transparent
        
        // Minimal border for icon spacing
        sendButton.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        
        // Cursor and tooltip
        sendButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        sendButton.setToolTipText("Send message");
        
        // Hover effects - subtle opacity change for transparent button
        sendButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                // Slightly increase opacity on hover for subtle effect
                sendButton.setBackground(new Color(255, 255, 255, 30)); // Very light white overlay
                sendButton.repaint();
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                // Return to fully transparent
                sendButton.setBackground(new Color(0, 0, 0, 0));
                sendButton.repaint();
            }
            
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                // Slightly darker overlay when pressed
                sendButton.setBackground(new Color(0, 0, 0, 20)); // Very light black overlay
                sendButton.repaint();
            }
            
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                // Return to hover state
                sendButton.setBackground(new Color(255, 255, 255, 30));
                sendButton.repaint();
            }
        });
        
        // Action listener
        sendButton.addActionListener(e -> sendMessage());
        
        return sendButton;
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