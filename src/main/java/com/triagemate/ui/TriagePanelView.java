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
     * Sets up the chat panel for message display
     */
    private void setupChatPanel() {
        chatPanel.removeAll();
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        Color panelBg = javax.swing.UIManager.getColor("Panel.background");
        if (panelBg == null) panelBg = new java.awt.Color(43,43,43);
        chatPanel.setBackground(panelBg);
        chatPanel.setOpaque(true);
        chatPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Add 10px padding around chat
    }

    /**
     * Sets up the input panel with text area and send button
     */
    private void setupInputPanel() {
        inputPanel.removeAll();
        inputPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(12, 16, 12, 16); // More vertical and horizontal margin

        // Custom panel to hold text area and send button together
        JPanel inputBoxPanel = new JPanel(new BorderLayout());
        inputBoxPanel.setBackground(new Color(38, 38, 38));
        inputBoxPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(0, 0, 0, 0),
            BorderFactory.createLineBorder(new Color(60, 60, 60), 1, true)
        ));
        inputBoxPanel.setOpaque(true);
        // Remove drop shadow

        // Text area with placeholder
        inputArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputArea.setRows(1);
        inputArea.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 0));
        inputArea.setBackground(new Color(38, 38, 38));
        inputArea.setForeground(Color.WHITE);
        inputArea.setCaretColor(Color.WHITE);
        inputArea.setOpaque(false);
        inputArea.setText("");
        inputArea.putClientProperty("JTextField.placeholderText", "Ask Anything");
        inputArea.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) {
                if (inputArea.getText().isEmpty()) inputArea.repaint();
            }
            public void focusLost(java.awt.event.FocusEvent e) {
                if (inputArea.getText().isEmpty()) inputArea.repaint();
            }
        });
        inputBoxPanel.add(inputArea, BorderLayout.CENTER);

        // Send button as small icon inside the text box, encapsulated in a lighter grey box with rounded corners
        JPanel sendButtonPanel = new JPanel(new BorderLayout());
        sendButtonPanel.setBackground(new Color(60, 60, 60)); // lighter grey
        sendButtonPanel.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        sendButtonPanel.setOpaque(true);
        sendButtonPanel.setMaximumSize(new Dimension(32, 32));
        sendButtonPanel.setPreferredSize(new Dimension(32, 32));
        sendButtonPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 60, 60), 1, true),
            BorderFactory.createEmptyBorder(2, 8, 2, 8)
        ));
        JButton sendIconButton = new JButton("→");
        sendIconButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        sendIconButton.setBackground(new Color(60, 60, 60));
        sendIconButton.setForeground(new Color(180, 180, 180));
        sendIconButton.setFocusPainted(false);
        sendIconButton.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        sendIconButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        sendIconButton.setToolTipText("Send");
        sendIconButton.setOpaque(false);
        sendIconButton.setContentAreaFilled(false);
        sendIconButton.setBorderPainted(false);
        sendIconButton.addActionListener(e -> sendButton.doClick());
        sendButtonPanel.add(sendIconButton, BorderLayout.CENTER);
        inputBoxPanel.add(sendButtonPanel, BorderLayout.EAST);

        // Hide the old sendButton
        sendButton.setVisible(false);

        inputPanel.add(inputBoxPanel, gbc);
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
    }

    /**
     * Adds a message component to the UI
     */
    private void addMessageToUI(ChatMessage message) {
        JPanel messagePanel = createMessagePanel(message);
        chatPanel.add(messagePanel);
        chatPanel.revalidate();
        chatPanel.repaint();
        scrollToBottom();
    }

    /**
     * Creates a message panel for display
     */
    private JPanel createMessagePanel(ChatMessage message) {
        // Message card panel (the chat bubble)
        JPanel cardPanel = new JPanel();
        cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));
        cardPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        Color darkBg = javax.swing.UIManager.getColor("Panel.background");
        if (darkBg == null) darkBg = new java.awt.Color(43,43,43);
        Color debugCardColor = new java.awt.Color(60, 60, 80); // TEMP: distinguish cardPanel
        cardPanel.setBackground(debugCardColor); // TEMP: visually distinguish card
        cardPanel.setOpaque(true);
        cardPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(12, 16, 12, 16),
            BorderFactory.createLineBorder(new Color(60, 60, 60), 1, true)
        ));
        cardPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, cardPanel.getPreferredSize().height)); // Only width

        boolean isInitialPrompt = false;
        if ((message.getRole() == ChatMessage.Role.SYSTEM || message.getRole() == ChatMessage.Role.AI)
                && chatHistory.size() > 0 && chatHistory.get(0) == message) {
            isInitialPrompt = true;
        }

        // --- Add header panel first ---
        JPanel headerPanel = createMessageHeaderPanel(message);
        headerPanel.setBackground(new java.awt.Color(80, 80, 100)); // TEMP: visually distinguish header
        headerPanel.setOpaque(true);
        headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, headerPanel.getPreferredSize().height)); // Only width
        cardPanel.add(headerPanel);

        if (isInitialPrompt) {
            // Scenario row
            JPanel scenarioRow = new JPanel();
            scenarioRow.setLayout(new BoxLayout(scenarioRow, BoxLayout.X_AXIS));
            scenarioRow.setAlignmentX(Component.LEFT_ALIGNMENT);
            scenarioRow.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
            scenarioRow.setBackground(new java.awt.Color(100, 80, 80)); // TEMP: visually distinguish scenarioRow
            scenarioRow.setOpaque(true);
            JLabel warningIcon = new JLabel("\u26A0"); // ⚠️
            warningIcon.setFont(new Font("Segoe UI", Font.PLAIN, 15));
            warningIcon.setForeground(new Color(255, 193, 7)); // Amber
            scenarioRow.add(warningIcon);
            scenarioRow.add(Box.createHorizontalStrut(6));
            JLabel scenarioLabel = new JLabel("Scenario: ");
            scenarioLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
            scenarioLabel.setForeground(new Color(255, 152, 0)); // Cucumber orange
            scenarioRow.add(scenarioLabel);
            String scenarioName = (currentFailure != null && currentFailure.getScenarioName() != null) ? currentFailure.getScenarioName() : "Unknown Scenario";
            JLabel scenarioNameLabel = new JLabel(scenarioName);
            scenarioNameLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
            scenarioNameLabel.setForeground(Color.WHITE);
            scenarioRow.add(scenarioNameLabel);
            scenarioRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, scenarioRow.getPreferredSize().height)); // Only width
            cardPanel.add(scenarioRow);

            // Expandable Show AI Thinking section
            JPanel aiThinkingContainer = new JPanel();
            aiThinkingContainer.setLayout(new BoxLayout(aiThinkingContainer, BoxLayout.Y_AXIS));
            aiThinkingContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
            aiThinkingContainer.setOpaque(true);
            aiThinkingContainer.setBackground(new java.awt.Color(80, 100, 80)); // TEMP: visually distinguish aiThinkingContainer
            aiThinkingContainer.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
            // Do NOT set maximum size for aiThinkingContainer (let it grow vertically)

            JPanel toggleRow = new JPanel();
            toggleRow.setLayout(new BoxLayout(toggleRow, BoxLayout.X_AXIS));
            toggleRow.setOpaque(false);
            toggleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
            toggleRow.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
            toggleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, toggleRow.getPreferredSize().height));
            JLabel chevronLabel = new JLabel("▶");
            chevronLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
            chevronLabel.setForeground(new Color(180, 180, 180));
            chevronLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            JButton toggleButton = new JButton("Show AI Thinking");
            toggleButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
            toggleButton.setFocusPainted(false);
            toggleButton.setContentAreaFilled(false);
            toggleButton.setBorderPainted(false);
            toggleButton.setHorizontalAlignment(SwingConstants.LEFT);
            toggleButton.setForeground(new Color(180, 180, 180));
            toggleRow.add(chevronLabel);
            toggleRow.add(Box.createHorizontalStrut(6));
            toggleRow.add(toggleButton);
            aiThinkingContainer.add(toggleRow);

            // Use JLabel with HTML for promptArea
            String htmlText = "<html>" + message.getText().replace("\n", "<br>") + "</html>";
            JLabel promptLabel = new JLabel(htmlText);
            promptLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            promptLabel.setForeground(Color.WHITE);
            promptLabel.setBackground(darkBg);
            promptLabel.setOpaque(false);
            promptLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
            promptLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            promptLabel.setVisible(false);
            aiThinkingContainer.add(promptLabel);

            java.awt.event.ActionListener toggleAction = e -> {
                boolean expanded = promptLabel.isVisible();
                promptLabel.setVisible(!expanded);
                chevronLabel.setText(expanded ? "▶" : "▼");
                toggleButton.setText(expanded ? "Show AI Thinking" : "Hide AI Thinking");
                aiThinkingContainer.revalidate();
                aiThinkingContainer.repaint();
                cardPanel.revalidate();
                cardPanel.repaint();
                chatPanel.revalidate();
                chatPanel.repaint();
            };
            toggleButton.addActionListener(toggleAction);
            chevronLabel.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    toggleAction.actionPerformed(null);
                }
            });
            cardPanel.add(aiThinkingContainer);
        } else {
            JTextArea messageArea = new JTextArea(message.getText());
            messageArea.setLineWrap(true);
            messageArea.setWrapStyleWord(true);
            messageArea.setEditable(false);
            messageArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            messageArea.setBackground(darkBg);
            messageArea.setForeground(Color.WHITE);
            messageArea.setBorder(BorderFactory.createEmptyBorder(16, 0, 16, 0));
            messageArea.setOpaque(true);
            messageArea.setAlignmentX(Component.LEFT_ALIGNMENT);
            messageArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, messageArea.getPreferredSize().height));
            cardPanel.add(messageArea);

            if (message.getRole() != ChatMessage.Role.USER) {
                JButton copyButton = new JButton("Copy");
                copyButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                copyButton.setFocusPainted(false);
                copyButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                copyButton.addActionListener(e -> copyToClipboard(message.getText()));
                copyButton.setAlignmentX(Component.RIGHT_ALIGNMENT);
                JPanel copyPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
                copyPanel.setOpaque(false);
                copyPanel.add(copyButton);
                copyPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, copyPanel.getPreferredSize().height));
                cardPanel.add(copyPanel);
            }
        }

        // Print the component hierarchy for this cardPanel
        System.out.println("[DEBUG] cardPanel hierarchy for message: " + message.getText());
        printComponentHierarchy(cardPanel, "  ");

        return cardPanel;
    }

    // Helper to print component hierarchy
    private void printComponentHierarchy(Component comp, String indent) {
        System.out.println(indent + comp.getClass().getSimpleName() + (comp instanceof JPanel ? " (JPanel)" : ""));
        if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents()) {
                printComponentHierarchy(child, indent + "  ");
            }
        }
    }

    /**
     * Creates the header panel for a chat message (logo, sender, timestamp)
     */
    private JPanel createMessageHeaderPanel(ChatMessage message) {
        // Use horizontal BoxLayout for tight header row
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.X_AXIS));
        headerPanel.setOpaque(true);
        headerPanel.setBackground(new java.awt.Color(80, 80, 100)); // TEMP: visually distinguish header
        headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        // Left: logo + sender
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.X_AXIS));
        leftPanel.setOpaque(false);
        leftPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (message.getRole() == ChatMessage.Role.AI || message.getRole() == ChatMessage.Role.SYSTEM) {
            try {
                Icon logoIcon = IconLoader.getIcon("/icons/logo_24.png", getClass());
                JLabel logoLabel = new JLabel(logoIcon);
                logoLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 6));
                leftPanel.add(logoLabel);
            } catch (Exception e) {
                // fallback: no icon
            }
            JLabel senderLabel = new JLabel("TriageMate");
            senderLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
            senderLabel.setForeground(Color.WHITE);
            leftPanel.add(senderLabel);
        } else if (message.getRole() == ChatMessage.Role.USER) {
            JLabel senderLabel = new JLabel(getCurrentUsername());
            senderLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
            senderLabel.setForeground(new Color(180, 180, 180));
            leftPanel.add(senderLabel);
        }
        headerPanel.add(leftPanel);
        // Add horizontal glue to push timestamp to the right
        headerPanel.add(Box.createHorizontalGlue());
        JLabel timeLabel = new JLabel(formatTimestamp(message.getTimestamp()) + " " + new java.text.SimpleDateFormat("MMM d, yyyy").format(new java.util.Date(message.getTimestamp())));
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        timeLabel.setForeground(new Color(180, 180, 180));
        headerPanel.add(timeLabel);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        return headerPanel;
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
        headerLabel.setText("\uD83D\uDD27 " + scenarioName); // Wrench emoji + scenario name
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 16f));
        headerLabel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        statusLabel.setText("");
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
        public enum Role { USER, AI, SYSTEM }
        
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

    // Get the current system username for user messages
    private String getCurrentUsername() {
        return System.getProperty("user.name", "User");
    }

    // --- Custom header with TriageMate Chat and settings icon ---
    private JPanel createCustomHeaderPanel() {
        Color darkBg = javax.swing.UIManager.getColor("Panel.background");
        if (darkBg == null) darkBg = new java.awt.Color(43,43,43);
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(darkBg);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(68, 68, 68)),
            BorderFactory.createEmptyBorder(8, 16, 8, 16)));
        JLabel title = new JLabel("TriageMate Chat");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);
        // Settings icon (Unicode gear or use an icon if available)
        JButton settingsButton = new JButton("\u2699"); // ⚙️
        settingsButton.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 18));
        settingsButton.setForeground(new Color(180, 180, 180));
        settingsButton.setBackground(darkBg);
        settingsButton.setBorderPainted(false);
        settingsButton.setFocusPainted(false);
        settingsButton.setContentAreaFilled(false);
        settingsButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        settingsButton.setToolTipText("Settings");
        settingsButton.addActionListener(e -> {
            showSettingsTab = !showSettingsTab;
            refreshMainPanel();
        });
        header.add(settingsButton, BorderLayout.EAST);
        return header;
    }

    // --- Settings panel placeholder ---
    private JPanel createSettingsPanel() {
        Color darkBg = javax.swing.UIManager.getColor("Panel.background");
        if (darkBg == null) darkBg = new java.awt.Color(43,43,43);
        JPanel settingsPanel = new JPanel();
        settingsPanel.setBackground(darkBg);
        settingsPanel.setLayout(new BorderLayout());
        JLabel placeholder = new JLabel("Settings page (placeholder)");
        placeholder.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        placeholder.setForeground(Color.WHITE);
        placeholder.setHorizontalAlignment(SwingConstants.CENTER);
        settingsPanel.add(placeholder, BorderLayout.CENTER);
        // Add a button to return to chat
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

    // --- Helper to refresh the main panel when switching tabs ---
    private void refreshMainPanel() {
        mainPanel.removeAll();
        mainPanel.setLayout(new BorderLayout());
        Color panelBg = javax.swing.UIManager.getColor("Panel.background");
        if (panelBg == null) panelBg = new java.awt.Color(43,43,43);
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