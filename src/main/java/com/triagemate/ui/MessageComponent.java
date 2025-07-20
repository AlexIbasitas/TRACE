package com.triagemate.ui;

import com.intellij.openapi.util.IconLoader;
import com.triagemate.models.FailureInfo;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Dedicated message component for consistent message rendering in the TriageMate chat interface.
 * 
 * <p>This Swing component handles the visual representation of individual chat messages,
 * including user messages, AI responses, and system messages. It provides a consistent
 * layout with proper styling, icons, timestamps, and support for collapsible AI thinking sections.</p>
 * 
 * <p>The component automatically adapts its appearance based on the message role and content,
 * displaying appropriate icons, colors, and layout elements for different message types.</p>
 * 
 * @author Alex Ibasitas
 * @version 1.0
 * @since 1.0
 */
public class MessageComponent extends JPanel {
    

    
    private final ChatMessage message;
    private CollapsiblePanel collapsiblePanel;
    
    /**
     * Creates a new message component for the given chat message.
     *
     * @param message The chat message to display (must not be null)
     * @throws IllegalArgumentException if message is null
     */
    public MessageComponent(ChatMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        
        this.message = message;
        initializeComponent();
    }
    
    /**
     * Initializes the component with proper layout and styling.
     */
    private void initializeComponent() {
        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(TriagePanelConstants.MESSAGE_COMPONENT_BORDER);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, getPreferredSize().height + TriagePanelConstants.COMPONENT_SPACING));
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setAlignmentY(Component.TOP_ALIGNMENT);
        
        initializeComponents();
    }
    
    /**
     * Initializes the component's child elements.
     */
    private void initializeComponents() {
        add(createHeader(), BorderLayout.NORTH);
        add(createContent(), BorderLayout.CENTER);
    }
    
    /**
     * Creates the message header with sender information and timestamp.
     *
     * @return The configured header panel
     */
    private JPanel createHeader() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(TriagePanelConstants.MESSAGE_HEADER_BORDER);
        
        // Left side: logo + sender name
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setOpaque(false);
        
        // Add logo for all messages (both user and AI)
        addSenderIcon(leftPanel);
        
        JLabel senderLabel = new JLabel(message.isFromUser() ? TriagePanelConstants.USER_DISPLAY_NAME : TriagePanelConstants.AI_DISPLAY_NAME);
        senderLabel.setFont(TriagePanelConstants.SENDER_FONT);
        senderLabel.setForeground(TriagePanelConstants.WHITE);
        leftPanel.add(senderLabel, BorderLayout.CENTER);
        
        headerPanel.add(leftPanel, BorderLayout.WEST);
        
        // Right side: full timestamp
        JLabel timeLabel = new JLabel(formatFullTimestamp(message.getTimestamp()));
        timeLabel.setFont(TriagePanelConstants.TIMESTAMP_FONT);
        timeLabel.setForeground(TriagePanelConstants.TIMESTAMP_COLOR);
        headerPanel.add(timeLabel, BorderLayout.EAST);
        
        return headerPanel;
    }
    
    /**
     * Adds the appropriate sender icon to the header panel.
     *
     * @param leftPanel The panel to add the icon to
     */
    private void addSenderIcon(JPanel leftPanel) {
        try {
            String iconPath = message.isFromUser() ? TriagePanelConstants.USER_ICON_PATH : TriagePanelConstants.AI_ICON_PATH;
            Icon logoIcon = IconLoader.getIcon(iconPath, getClass());
            if (logoIcon != null) {
                JLabel logoLabel = new JLabel(logoIcon);
                logoLabel.setBorder(TriagePanelConstants.MESSAGE_LOGO_BORDER);
                leftPanel.add(logoLabel, BorderLayout.WEST);
            }
        } catch (Exception e) {
            // Fallback: no icon - continue without icon
        }
    }
    
    /**
     * Creates the main content area of the message.
     *
     * @return The configured content panel
     */
    private JPanel createContent() {
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        
        // Add scenario information for AI messages if available
        if (message.isFromAI() && message.hasFailureInfo()) {
            addScenarioInformation(contentPanel);
            addFailedStepInformation(contentPanel);
        }
        
        // Create message text component
        if (message.getText() != null && !message.getText().trim().isEmpty()) {
            addMessageText(contentPanel);
        }
        
        // Add AI thinking section for AI messages
        if (message.isFromAI() && message.hasAiThinking()) {
            addAiThinkingSection(contentPanel);
        }
        
        return contentPanel;
    }
    
    /**
     * Adds scenario information to the content panel.
     *
     * @param contentPanel The panel to add scenario information to
     */
    private void addScenarioInformation(JPanel contentPanel) {
        JPanel scenarioPanel = new JPanel(new BorderLayout());
        scenarioPanel.setOpaque(false);
        scenarioPanel.setBorder(TriagePanelConstants.SCENARIO_PANEL_BORDER);
        scenarioPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Create scenario label with orange "Scenario:" and bold white test name
        JPanel scenarioLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        scenarioLabelPanel.setOpaque(false);
        
        JLabel scenarioPrefix = new JLabel(TriagePanelConstants.SCENARIO_PREFIX);
        scenarioPrefix.setFont(TriagePanelConstants.SCENARIO_FONT);
        scenarioPrefix.setForeground(TriagePanelConstants.SCENARIO_COLOR);
        
        String scenarioName = message.getFailureInfo().getScenarioName();
        JLabel scenarioNameLabel = new JLabel(scenarioName != null ? scenarioName : TriagePanelConstants.UNKNOWN_SCENARIO);
        scenarioNameLabel.setFont(TriagePanelConstants.SCENARIO_FONT);
        scenarioNameLabel.setForeground(TriagePanelConstants.WHITE);
        
        scenarioLabelPanel.add(scenarioPrefix);
        scenarioLabelPanel.add(scenarioNameLabel);
        scenarioPanel.add(scenarioLabelPanel, BorderLayout.WEST);
        
        contentPanel.add(scenarioPanel);
    }
    
    /**
     * Adds failed step information to the content panel.
     *
     * @param contentPanel The panel to add failed step information to
     */
    private void addFailedStepInformation(JPanel contentPanel) {
        String failedStepText = message.getFailureInfo().getFailedStepText();
        if (failedStepText != null && !failedStepText.trim().isEmpty()) {
            JPanel failedStepPanel = new JPanel(new BorderLayout());
            failedStepPanel.setOpaque(false);
            failedStepPanel.setBorder(TriagePanelConstants.FAILED_STEP_PANEL_BORDER);
            failedStepPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            // Create failed step label with failure symbol, red "Failed Step:" and red bold step text
            JPanel failedStepLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            failedStepLabelPanel.setOpaque(false);
            
            // Add red X failure symbol
            JLabel failureSymbol = new JLabel(TriagePanelConstants.FAILURE_SYMBOL);
            failureSymbol.setFont(TriagePanelConstants.SCENARIO_FONT);
            failureSymbol.setForeground(TriagePanelConstants.FAILURE_COLOR);
            
            JLabel failedStepPrefix = new JLabel(TriagePanelConstants.FAILED_STEP_PREFIX);
            failedStepPrefix.setFont(TriagePanelConstants.SCENARIO_FONT);
            failedStepPrefix.setForeground(TriagePanelConstants.FAILURE_COLOR);
            
            JLabel failedStepTextLabel = new JLabel(failedStepText);
            failedStepTextLabel.setFont(TriagePanelConstants.SCENARIO_FONT);
            failedStepTextLabel.setForeground(TriagePanelConstants.WHITE);
            
            failedStepLabelPanel.add(failureSymbol);
            failedStepLabelPanel.add(failedStepPrefix);
            failedStepLabelPanel.add(failedStepTextLabel);
            failedStepPanel.add(failedStepLabelPanel, BorderLayout.WEST);
            
            contentPanel.add(failedStepPanel);
        }
    }
    
    /**
     * Adds the message text to the content panel.
     *
     * @param contentPanel The panel to add the message text to
     */
    private void addMessageText(JPanel contentPanel) {
        JTextArea messageText = new JTextArea(message.getText());
        messageText.setLineWrap(true);
        messageText.setWrapStyleWord(true);
        messageText.setEditable(false);
        messageText.setFont(TriagePanelConstants.MESSAGE_FONT);
        messageText.setBackground(getBackground());
        messageText.setForeground(TriagePanelConstants.WHITE);
        messageText.setBorder(TriagePanelConstants.EMPTY_BORDER);
        messageText.setOpaque(false);
        messageText.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Ensure text area is visible and properly sized
        messageText.setVisible(true);
        
        // Implement soft wrapping: allow text to wrap until minimum width is reached
        // Set preferred width to allow wrapping, but respect minimum width constraint
        int preferredWidth = Math.max(TriagePanelConstants.MIN_CHAT_WIDTH_BEFORE_SCROLL, 
                                    TriagePanelConstants.MAX_MESSAGE_TEXT_WIDTH);
        messageText.setSize(preferredWidth, Short.MAX_VALUE);
        Dimension preferredSize = messageText.getPreferredSize();
        
        // Set flexible sizing that allows wrapping but respects minimum width
        messageText.setPreferredSize(new Dimension(preferredWidth, preferredSize.height));
        messageText.setMaximumSize(new Dimension(Integer.MAX_VALUE, preferredSize.height + TriagePanelConstants.CONTENT_PADDING));
        messageText.setMinimumSize(new Dimension(TriagePanelConstants.MIN_CHAT_WIDTH_BEFORE_SCROLL, preferredSize.height));
        messageText.setAlignmentY(Component.TOP_ALIGNMENT);
        
        // Add component name for testing identification
        messageText.setName("messageText");
        
        contentPanel.add(messageText);
        
        // Force layout update to ensure proper display
        contentPanel.revalidate();
    }
    
    /**
     * Adds the AI thinking section to the content panel.
     *
     * @param contentPanel The panel to add the AI thinking section to
     */
    private void addAiThinkingSection(JPanel contentPanel) {
        collapsiblePanel = new CollapsiblePanel("AI Thinking", message.getAiThinking(), this);
        collapsiblePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(collapsiblePanel);
    }
    
    /**
     * Calculates the estimated content height based on components.
     *
     * @return The estimated height in pixels
     */
    private int getContentHeight() {
        int height = 0;
        
        // Add header height (timestamp + sender info)
        height += 24;
        
        // Add message text height if present
        if (message.getText() != null && !message.getText().trim().isEmpty()) {
            int textHeight = estimateTextHeight(message.getText());
            height += textHeight;
        }
        
        // Add scenario/failed step height if present
        if (message.hasFailureInfo()) {
            height += 40;
        }
        
        // Add AI thinking toggle height if present
        if (message.hasAiThinking()) {
            height += 24;
        }
        
        return height;
    }
    
    /**
     * Estimates the height needed for text display based on content length and line breaks.
     *
     * @param text The text to estimate height for
     * @return The estimated height in pixels
     */
    private int estimateTextHeight(String text) {
        int lines = 1;
        if (text.contains("\n")) {
            lines = text.split("\n").length;
        } else if (text.length() > TriagePanelConstants.CHARS_PER_LINE) {
            lines = (text.length() / TriagePanelConstants.CHARS_PER_LINE) + 1;
        }
        return lines * TriagePanelConstants.ESTIMATED_LINE_HEIGHT;
    }
    
    /**
     * Formats a timestamp into a human-readable string.
     *
     * @param timestamp The timestamp in milliseconds since epoch
     * @return A formatted timestamp string
     */
    private String formatFullTimestamp(long timestamp) {
        return new SimpleDateFormat("h:mm a, MMM d, yyyy").format(new Date(timestamp));
    }
    
    /**
     * Gets the chat message associated with this component.
     *
     * @return The chat message
     */
    public ChatMessage getMessage() {
        return message;
    }
    
    /**
     * Gets the collapsible panel for AI thinking content.
     *
     * @return The collapsible panel, or null if not available
     */
    public CollapsiblePanel getCollapsiblePanel() {
        return collapsiblePanel;
    }
    
    /**
     * Checks if this component has a collapsible panel.
     *
     * @return true if the component has a collapsible panel, false otherwise
     */
    public boolean hasCollapsiblePanel() {
        return collapsiblePanel != null;
    }
} 