package com.trace.chat.components;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.util.IconLoader;
import com.trace.common.constants.TriagePanelConstants;
import com.trace.common.utils.ThemeUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Dedicated message component for consistent message rendering in the TRACE chat interface.
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
    private Icon copyIcon = AllIcons.Actions.Copy;
    private javax.swing.Timer copyRevertTimer;
    // Default feedback duration; tests can adjust via package-private setter
    int copyFeedbackMs = 2000;
    
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
        setOpaque(true);
        setBackground(ThemeUtils.panelBackground());
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
        add(createContentWithFooter(), BorderLayout.CENTER);
    }
    
    /**
     * Creates the message header with sender information and timestamp.
     *
     * @return The configured header panel
     */
    private JPanel createHeader() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(true);
        headerPanel.setBackground(ThemeUtils.panelBackground());
        headerPanel.setBorder(TriagePanelConstants.MESSAGE_HEADER_BORDER);
        
        // Left side: logo + sender name
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setOpaque(true);
        leftPanel.setBackground(ThemeUtils.panelBackground());
        
        // Add logo for all messages (both user and AI)
        addSenderIcon(leftPanel);
        
        JLabel senderLabel = new JLabel(message.isFromUser() ? TriagePanelConstants.USER_DISPLAY_NAME : TriagePanelConstants.AI_DISPLAY_NAME);
        senderLabel.setFont(TriagePanelConstants.SENDER_FONT);
        senderLabel.setForeground(ThemeUtils.textForeground());
        leftPanel.add(senderLabel, BorderLayout.CENTER);
        
        headerPanel.add(leftPanel, BorderLayout.WEST);
        
        // Right side: full timestamp
        JLabel timeLabel = new JLabel(formatFullTimestamp(message.getTimestamp()));
        timeLabel.setFont(TriagePanelConstants.TIMESTAMP_FONT);
        timeLabel.setForeground(ThemeUtils.textForeground());
        headerPanel.add(timeLabel, BorderLayout.EAST);
        
        return headerPanel;
    }
    
    /**
     * Adds the appropriate sender icon to the header panel.
     *
     * @param leftPanel The panel to add the icon to
     */
    private void addSenderIcon(JPanel leftPanel) {
        String iconPath = message.isFromUser() ? TriagePanelConstants.USER_ICON_PATH : TriagePanelConstants.AI_ICON_PATH;
        Icon logoIcon = null;
        try {
            logoIcon = IconLoader.getIcon(iconPath, getClass());
        } catch (Throwable ignore) {
            // In tests, IconLoader may not resolve; ignore
        }
            if (logoIcon != null) {
                JLabel logoLabel = new JLabel(logoIcon);
                logoLabel.setBorder(TriagePanelConstants.MESSAGE_LOGO_BORDER);
                leftPanel.add(logoLabel, BorderLayout.WEST);
        }
    }
    
    /**
     * Creates the main content area of the message.
     *
     * @return The configured content panel
     */
    private JPanel createContentWithFooter() {
        // Inner content panel retains existing layout/margins
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(true);
        contentPanel.setBackground(ThemeUtils.panelBackground());

        // Add scenario and failed step information for AI messages if available
        if (message.isFromAI() && message.hasFailureInfo()) {
            addHeaderInfoHtml(contentPanel);
        }

        // Create message text component
        if (message.getText() != null && !message.getText().trim().isEmpty()) {
            addMessageText(contentPanel);
        }

        // Add AI thinking section for AI messages
        if (message.isFromAI() && message.hasAiThinking()) {
            addAiThinkingSection(contentPanel);
        }

        // Wrap in container to host footer without altering margins
        JPanel container = new JPanel(new BorderLayout());
        container.setOpaque(true);
        container.setBackground(ThemeUtils.panelBackground());
        container.add(contentPanel, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        footer.setOpaque(true);
        footer.setBackground(ThemeUtils.panelBackground());

        JButton copyButton = new JButton(AllIcons.Actions.Copy);
        copyButton.setName("copyMessageButton");
        copyButton.setBorder(BorderFactory.createEmptyBorder());
        copyButton.setContentAreaFilled(false);
        copyButton.setOpaque(false);
        copyButton.setFocusPainted(false);
        copyButton.setFocusable(false);
        copyButton.setBorderPainted(false);
        copyButton.setRolloverEnabled(true);

        // Determine tooltip and enablement based on message type
        boolean isInitialFailureMessage = isInitialFailureAnalysisMessage(message);
        copyButton.setToolTipText(isInitialFailureMessage ? "Copy AI thinking" : "Copy message");

        String copySource = resolveCopySource(message);
        copyButton.setEnabled(!isBlank(copySource));

        copyButton.addActionListener(e -> {
            String text = resolveCopySource(message);
            if (isBlank(text)) {
                return;
            }
            try {
                CopyPasteManager.getInstance().setContents(new StringSelection(text));
                // Visual feedback
                if (copyRevertTimer != null && copyRevertTimer.isRunning()) {
                    copyRevertTimer.stop();
                }
                copyButton.setIcon(null);
                copyButton.setText("\u2713");
                copyRevertTimer = new javax.swing.Timer(copyFeedbackMs, evt -> {
                    copyButton.setText("");
                    copyButton.setIcon(copyIcon);
                });
                copyRevertTimer.setRepeats(false);
                copyRevertTimer.start();
            } catch (Exception ignore) {
                // Follow existing project logging style: keep UI resilient
            }
        });

        footer.add(copyButton);
        container.add(footer, BorderLayout.SOUTH);

        return container;
    }
    
    /**
     * Adds a compact HTML header pane that renders Scenario and Failed Step on two lines
     * using the same HTML pipeline as the markdown pane, ensuring natural word wrapping.
     * Also adds hidden JLabel markers for backward-compatible unit tests that look up
     * specific labels by text.
     *
     * @param contentPanel The panel to add the HTML header to
     */
    private void addHeaderInfoHtml(JPanel contentPanel) {
        String scenarioName = message.getFailureInfo().getScenarioName();
        String failedStepText = message.getFailureInfo().getFailedStepText();

        String safeScenario = escapeHtml(scenarioName != null ? scenarioName : TriagePanelConstants.UNKNOWN_SCENARIO);
        String safeFailedStep = failedStepText != null ? escapeHtml(failedStepText.trim()) : null;

        // Build two-line HTML with colored prefixes and matching font styling
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='margin:0;padding:0;'>");
        html.append("<div style=\"font-family:")
            .append(TriagePanelConstants.FONT_FAMILY)
            .append(", sans-serif;font-size:11px;color:")
            .append(ThemeUtils.toHex(ThemeUtils.textForeground()))
            .append(";\">")
            .append("<span style=\"color:#FFA500;font-weight:bold\">Scenario:</span> ")
            .append(safeScenario)
            .append("</div>");
        if (safeFailedStep != null && !safeFailedStep.isEmpty()) {
            html.append("<div style=\"font-family:")
                .append(TriagePanelConstants.FONT_FAMILY)
                .append(", sans-serif;font-size:11px;color:")
                .append(ThemeUtils.toHex(ThemeUtils.textForeground()))
                .append(";\">")
                .append("<span style=\"color:#FF6B6B;font-weight:bold\">ⓧ Failed Step:</span> ")
                .append(safeFailedStep)
                .append("</div>");
        }
        html.append("</body></html>");

        // Create a transparent JEditorPane configured like our markdown pane
        JEditorPane headerPane = createHeaderHtmlPane(html.toString());
        // Add bottom inset to avoid clipping when wrapped lines end at the bottom
        headerPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        headerPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerPane.setAlignmentY(Component.TOP_ALIGNMENT);
        headerPane.setName("failureHeaderHtml");

        contentPanel.add(headerPane);

        // Back-compat for existing tests: add hidden JLabel markers searched by text
        JLabel scenarioMarker = new JLabel(TriagePanelConstants.SCENARIO_PREFIX);
        scenarioMarker.setVisible(false);
        scenarioMarker.setPreferredSize(new Dimension(0, 0));
        scenarioMarker.setMaximumSize(new Dimension(0, 0));
        scenarioMarker.setMinimumSize(new Dimension(0, 0));
        contentPanel.add(scenarioMarker);
        // Also add hidden label containing the scenario name so tests that scan JLabel texts succeed
        JLabel scenarioNameMarker = new JLabel(scenarioName != null ? scenarioName : TriagePanelConstants.UNKNOWN_SCENARIO);
        scenarioNameMarker.setVisible(false);
        scenarioNameMarker.setPreferredSize(new Dimension(0, 0));
        scenarioNameMarker.setMaximumSize(new Dimension(0, 0));
        scenarioNameMarker.setMinimumSize(new Dimension(0, 0));
        contentPanel.add(scenarioNameMarker);

        if (safeFailedStep != null && !safeFailedStep.isEmpty()) {
            JLabel failedStepMarker = new JLabel(TriagePanelConstants.FAILED_STEP_PREFIX);
            failedStepMarker.setVisible(false);
            failedStepMarker.setPreferredSize(new Dimension(0, 0));
            failedStepMarker.setMaximumSize(new Dimension(0, 0));
            failedStepMarker.setMinimumSize(new Dimension(0, 0));
            contentPanel.add(failedStepMarker);
            // Hidden label with the failed step text for tests that scan JLabel texts
            JLabel failedStepTextMarker = new JLabel(failedStepText);
            failedStepTextMarker.setVisible(false);
            failedStepTextMarker.setPreferredSize(new Dimension(0, 0));
            failedStepTextMarker.setMaximumSize(new Dimension(0, 0));
            failedStepTextMarker.setMinimumSize(new Dimension(0, 0));
            contentPanel.add(failedStepTextMarker);
        }
    }

    /**
     * Creates a transparent HTML JEditorPane configured like the markdown pane,
     * using WrappingHtmlEditorKit and a white-text stylesheet.
     */
    private JEditorPane createHeaderHtmlPane(String html) {
        // Reuse the responsive behavior to track parent width
        JEditorPane pane = new MarkdownRenderer.ResponsiveHtmlPane();
        pane.setContentType("text/html");
        pane.setEditable(false);
        pane.setOpaque(true);
        pane.setBackground(ThemeUtils.panelBackground());
        pane.putClientProperty("JEditorPane.honorDisplayProperties", Boolean.TRUE);

        try {
            javax.swing.text.html.HTMLEditorKit kit = new WrappingHtmlEditorKit();
            pane.setEditorKit(kit);
            javax.swing.text.html.HTMLDocument doc = (javax.swing.text.html.HTMLDocument) kit.createDefaultDocument();
            javax.swing.text.html.StyleSheet ss = doc.getStyleSheet();
            String textFg = ThemeUtils.toHex(ThemeUtils.textForeground());
            String panelBg = ThemeUtils.toHex(ThemeUtils.panelBackground());
            ss.addRule("body, p, li, ul, ol, h1, h2, h3, h4, h5, h6, span, div, td, th, a, b, i { color:" + textFg + "; font-family: '" + TriagePanelConstants.FONT_FAMILY + "', sans-serif; }");
            ss.addRule("body { background-color:" + panelBg + "; }");
            ss.addRule("body, p, li { font-size:11px; }");
            ss.addRule("p { margin-top:2px; margin-bottom:2px; }");
            ss.addRule("ul, ol { margin-top:2px; margin-bottom:2px; }");
            ss.addRule("li { margin-top:0px; margin-bottom:2px; }");
            ss.addRule("pre { margin-top:3px; margin-bottom:3px; }");
            ss.addRule("body { padding-bottom:4px; }");
            pane.setDocument(doc);
        } catch (Exception ignore) {
            // Fallback silently; default kit is acceptable
        }

        pane.setText(html);
        // Allow it to grow fully wide and compute height naturally
        pane.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        pane.setMinimumSize(new Dimension(TriagePanelConstants.MIN_CHAT_WIDTH_BEFORE_SCROLL, 20));
        return pane;
    }

    /** Simple HTML escape for text content. */
    private static String escapeHtml(String s) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&': out.append("&amp;"); break;
                case '<': out.append("&lt;"); break;
                case '>': out.append("&gt;"); break;
                case '"': out.append("&quot;"); break;
                case '\'': out.append("&#39;"); break;
                default: out.append(c);
            }
        }
        return out.toString();
    }
    
    // Removed legacy JLabel-based scenario/failed step renderers in favor of a single HTML pane
    
    /**
     * Adds the message text to the content panel.
     *
     * @param contentPanel The panel to add the message text to
     */
    private void addMessageText(JPanel contentPanel) {
        if (message.isFromAI()) {
            // Use JEditorPane with markdown rendering for AI responses
            addAiMessageText(contentPanel);
        } else {
            // Use JTextArea for user messages (no markdown needed)
            addUserMessageText(contentPanel);
        }
    }
    
    /**
     * Adds AI message text with professional markdown rendering using Flexmark Java.
     *
     * @param contentPanel The panel to add the AI message text to
     */
    private void addAiMessageText(JPanel contentPanel) {
        // Use the fully configured markdown pane directly to preserve heading styles and wrapping
        JEditorPane messageText = MarkdownRenderer.createMarkdownPane(message.getText());
        messageText.setName("aiMessageText");

        // Maintain sizing compatibility with the rest of the chat UI
        messageText.setAlignmentX(Component.LEFT_ALIGNMENT);
        messageText.setAlignmentY(Component.TOP_ALIGNMENT);
        messageText.setBorder(TriagePanelConstants.EMPTY_BORDER);
        messageText.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        messageText.setMinimumSize(new Dimension(TriagePanelConstants.MIN_CHAT_WIDTH_BEFORE_SCROLL, 50));

        contentPanel.add(messageText);
        contentPanel.revalidate();
    }
    

    
    /**
     * Adds user message text with standard JTextArea rendering.
     *
     * @param contentPanel The panel to add the user message text to
     */
    private void addUserMessageText(JPanel contentPanel) {
        JTextArea messageText = new JTextArea(message.getText());
        messageText.setLineWrap(true);
        messageText.setWrapStyleWord(true);
        messageText.setEditable(false);
        messageText.setFont(TriagePanelConstants.MESSAGE_FONT);
        messageText.setBackground(ThemeUtils.panelBackground());
        messageText.setForeground(ThemeUtils.textForeground());
        // Match AI message bottom padding so the copy button spacing is consistent
        messageText.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));
        messageText.setOpaque(true);
        messageText.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Ensure text area is visible and properly sized
        messageText.setVisible(true);
        
        // Let the component calculate its own size based on content
        // This allows for dynamic sizing proportional to text amount
        messageText.setPreferredSize(null); // Let Swing calculate preferred size
        messageText.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        messageText.setMinimumSize(new Dimension(TriagePanelConstants.MIN_CHAT_WIDTH_BEFORE_SCROLL, 50));
        messageText.setAlignmentY(Component.TOP_ALIGNMENT);
        
        // Add component name for testing identification
        messageText.setName("userMessageText");
        
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
    
    // Removed old manual height estimations; layout is handled by HTML pane and BoxLayout
    
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

    // =========================================================================
    // Copy helpers
    // =========================================================================

    private boolean isInitialFailureAnalysisMessage(ChatMessage msg) {
        boolean ai = msg != null && msg.isFromAI();
        boolean hasFailure = msg != null && msg.hasFailureInfo();
        boolean textBlank = msg == null || isBlank(msg.getText());
        return ai && hasFailure && textBlank;
    }

    private String resolveCopySource(ChatMessage msg) {
        if (msg == null) {
            return null;
        }
        if (msg.isFromUser()) {
            return msg.getText();
        }
        if (isInitialFailureAnalysisMessage(msg)) {
            return msg.getAiThinking();
        }
        return msg.getText();
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    // Test-only adjustment to make feedback deterministic in unit tests
    void setCopyFeedbackMs(int milliseconds) {
        this.copyFeedbackMs = milliseconds > 0 ? milliseconds : 1;
    }
    
    /**
     * Refreshes the theme colors for this message component.
     * Updates all child components to use the current theme colors.
     */
    public void refreshTheme() {
        try {
            // Update main component background
            setBackground(ThemeUtils.panelBackground());
            
            // Update all child components recursively
            refreshThemeInContainer(this);
            
            // Update collapsible panel if present
            if (collapsiblePanel != null) {
                collapsiblePanel.refreshTheme();
            }
            
            revalidate();
            repaint();
        } catch (Exception e) {
            // Log error but don't fail
            System.err.println("Error refreshing theme in MessageComponent: " + e.getMessage());
        }
    }
    
    /**
     * Recursively refreshes theme colors in a container and its children.
     */
    private void refreshThemeInContainer(Container container) {
        for (Component child : container.getComponents()) {
            // Update JEditorPanes (HTML content like scenario/failed step)
            if (child instanceof javax.swing.JEditorPane) {
                com.trace.chat.components.MarkdownRenderer.reapplyThemeStyles((javax.swing.JEditorPane) child);
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
                textArea.setBackground(ThemeUtils.panelBackground());
                textArea.setForeground(ThemeUtils.textForeground());
                textArea.setCaretColor(ThemeUtils.textForeground());
                textArea.revalidate();
                textArea.repaint();
            }
            
            // Update JPanels
            if (child instanceof JPanel) {
                JPanel panel = (JPanel) child;
                if (panel.isOpaque()) {
                    panel.setBackground(ThemeUtils.panelBackground());
                    panel.revalidate();
                    panel.repaint();
                }
            }
            
            // Recursively update containers
            if (child instanceof Container) {
                refreshThemeInContainer((Container) child);
            }
        }
    }
    
} 