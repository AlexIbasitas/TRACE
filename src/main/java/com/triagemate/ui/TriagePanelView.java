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
import com.triagemate.services.AINetworkService;
import com.triagemate.ui.ChatMessage;
import com.triagemate.ui.MessageComponent;
import com.triagemate.ui.CollapsiblePanel;
import com.triagemate.ui.TriagePanelConstants;
import com.triagemate.ui.ChatPanelFactory;
import com.triagemate.ui.InputPanelFactory;
import com.triagemate.ui.HeaderPanelFactory;
import com.triagemate.settings.AISettings;
import com.intellij.openapi.diagnostic.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;

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
    private final LocalPromptGenerationService promptService;
    private final AINetworkService aiNetworkService;
    private JScrollPane chatScrollPane;
    private JPanel messageContainer;
    private boolean showSettingsTab = false;

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
        this.promptService = project.getService(LocalPromptGenerationService.class);
        this.aiNetworkService = project.getService(AINetworkService.class);
        
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
        inputPanel.setBorder(BorderFactory.createEmptyBorder(8, 16, 16, 16));
        
        // Create input container with rounded border
        JPanel inputContainer = new JPanel(new BorderLayout());
        inputContainer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 60, 60), 1, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 0)
        ));
        inputContainer.setBackground(new Color(50, 50, 50));
        inputContainer.setOpaque(true);
        
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
        
        // Add components to input container
        inputContainer.add(inputArea, BorderLayout.CENTER);
        inputContainer.add(buttonContainer, BorderLayout.EAST);
        
        // Add to input panel
        inputPanel.add(inputContainer, BorderLayout.CENTER);
        
        // Hide the old send button
        sendButton.setVisible(false);
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
        
        // Show backend configuration message
        addMessage(new ChatMessage(ChatMessage.Role.AI, 
            BACKEND_NOT_CONFIGURED_MESSAGE, 
            System.currentTimeMillis(), null, null));
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
    public void updateFailure(FailureInfo failureInfo) {
        if (failureInfo == null) {
            throw new IllegalArgumentException("FailureInfo cannot be null");
        }
        
        LOG.info("Updating failure info: " + failureInfo.getScenarioName() + " - " + 
                failureInfo.getFailedStepText().substring(0, Math.min(failureInfo.getFailedStepText().length(), 50)) + "...");
        
        // Ensure we're on the EDT
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> updateFailure(failureInfo));
            return;
        }
        
        // Clear chat history for new failure
        clearChat();
        
        // Generate and display initial prompt
        generateAndDisplayPrompt(failureInfo);
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
        try {
            LOG.debug("Generating detailed prompt for failure");
            String prompt = promptService.generateDetailedPrompt(failureInfo);
            
            // Create a special AI message with the prompt in the collapsible section AND failure info
            addMessage(new ChatMessage(ChatMessage.Role.AI, "", System.currentTimeMillis(), prompt, failureInfo));
            
        } catch (Exception e) {
            LOG.error("Error generating prompt: " + e.getMessage(), e);
            String errorMessage = ERROR_GENERATING_PROMPT_PREFIX + e.getMessage();
            addMessage(new ChatMessage(ChatMessage.Role.AI, errorMessage, System.currentTimeMillis(), null, failureInfo));
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
     * Creates the custom header panel with logo and scenario information.
     *
     * @return The configured header panel
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