package com.trace.ai.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ide.BrowserUtil;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBCheckBox;

import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.openapi.Disposable;
import com.trace.ai.configuration.AISettings;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Privacy and Consent panel for TRACE AI settings.
 * 
 * <p>This panel provides a user-friendly interface for managing AI analysis consent
 * and privacy settings. It follows JetBrains UI writing guidelines with clear,
 * non-technical language that explains what the feature does from the user's perspective.</p>
 * 
 * <p>The panel includes:</p>
 * <ul>
 *   <li>Enable/disable AI analysis checkbox</li>
 *   <li>Clear explanation of data usage</li>
 *   <li>Learn More button for detailed privacy information</li>
 *   <li>Revoke Access button for consent management</li>
 *   <li>Consent and confirmation dialogs</li>
 * </ul>
 * 
 * <p>This component is designed for extensibility - additional consent options
 * can be easily added in the future.</p>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
public class PrivacyConsentPanel extends JBPanel<PrivacyConsentPanel> implements Disposable {
    
    private static final Logger LOG = Logger.getInstance(PrivacyConsentPanel.class);
    
    // UI Components
    private final JBCheckBox aiEnabledCheckBox;
    private final JTextPane explanationTextPane;
    
    // Settings service
    private final AISettings aiSettings;
    
    // Callback for AI service config panel
    private Runnable onAIStateChanged;
    
    // State tracking for modification detection
    private boolean originalAIEnabled;
    private boolean originalUserConsent;
    
    // Font change tracking for zoom responsiveness
    private int lastFontSize = -1;
    private int lastKnownFontSize = -1;
    
    /**
     * Constructor for PrivacyConsentPanel.
     * 
     * @param aiSettings the AISettings service for data persistence
     */
    public PrivacyConsentPanel(AISettings aiSettings) {
        this(aiSettings, null);
    }
    
    /**
     * Constructor for PrivacyConsentPanel with callback.
     * 
     * @param aiSettings the AISettings service for data persistence
     * @param onAIStateChanged callback to execute when AI state changes
     */
    public PrivacyConsentPanel(AISettings aiSettings, Runnable onAIStateChanged) {
        this.aiSettings = aiSettings;
        this.onAIStateChanged = onAIStateChanged;
        
        // Initialize state tracking
        this.originalAIEnabled = aiSettings.isAIAnalysisEnabled();
        this.originalUserConsent = aiSettings.hasUserConsent();
        
        // Create UI components
        this.aiEnabledCheckBox = new JBCheckBox("Enable AI analysis features");
        this.aiEnabledCheckBox.setFont(UIUtil.getLabelFont());
        this.explanationTextPane = createExplanationTextPane();
        
        // Initialize the panel
        initializePanel();
        setupEventHandlers();
        loadCurrentSettings();
        
        // Listen for theme/zoom changes
        setupThemeChangeListener();
        
        // Initialize font size tracking
        this.lastKnownFontSize = UIUtil.getLabelFont().getSize();
    }
    
    /**
     * Initializes the panel layout and styling.
     */
    private void initializePanel() {
        // Use BorderLayout for proper resizing behavior
        setLayout(new BorderLayout());
        setBorder(JBUI.Borders.compound(
            JBUI.Borders.customLine(UIUtil.getPanelBackground().darker(), 1),
            JBUI.Borders.empty(10)
        ));
        
        // Create main content panel with vertical layout
        JPanel contentPanel = new JBPanel<>();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        
        // Create header with zoom responsiveness
        JBLabel headerLabel = createZoomResponsiveHeader();
        headerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerLabel.setBorder(JBUI.Borders.emptyBottom(8));
        
        // Create checkbox
        aiEnabledCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        aiEnabledCheckBox.setBorder(JBUI.Borders.emptyBottom(8));
        
        // Add fixed components to content panel
        contentPanel.add(headerLabel);
        contentPanel.add(aiEnabledCheckBox);
        
        // Add the text pane which will handle its own sizing and wrapping
        contentPanel.add(explanationTextPane);
        
        // Add content panel to center so it fills available space
        add(contentPanel, BorderLayout.CENTER);
        
        // Set initial text based on current settings
        updateExplanationText();
    }
    
    /**
     * Creates a zoom-responsive header label that updates with font changes.
     */
    private JBLabel createZoomResponsiveHeader() {
        return new JBLabel("Privacy & Consent") {
            @Override
            public Font getFont() {
                // Always return the current UI font to respond to zoom changes
                Font baseFont = UIUtil.getLabelFont();
                return baseFont.deriveFont(Font.BOLD, baseFont.getSize() + 1);
            }
            
            @Override
            public void setFont(Font font) {
                // Override to always use UI font for zoom responsiveness
                Font baseFont = UIUtil.getLabelFont();
                super.setFont(baseFont.deriveFont(Font.BOLD, baseFont.getSize() + 1));
            }
            
            @Override
            public void paint(Graphics g) {
                // Ensure font is always current before painting
                Font baseFont = UIUtil.getLabelFont();
                setFont(baseFont.deriveFont(Font.BOLD, baseFont.getSize() + 1));
                super.paint(g);
            }
        };
    }
    
    /**
     * Creates and configures the explanation text pane with proper wrapping and resizing.
     */
    private JTextPane createExplanationTextPane() {
        // Override getPreferredSize to ensure proper wrapping behavior
        return new JTextPane() {
            {
                setContentType("text/html");
                setEditable(false);
                setOpaque(false);
                setFont(UIUtil.getLabelFont());
                setForeground(UIUtil.getLabelForeground());
                setAlignmentX(Component.LEFT_ALIGNMENT);
                setBorder(JBUI.Borders.empty(2));
                
                // Enable text wrapping and font scaling
                putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
                
                addHyperlinkListener(e -> {
                    if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                        BrowserUtil.browse("https://alexibasitas.github.io/TRACE/PRIVACY.html");
                    }
                });
            }
            
            @Override
            public Font getFont() {
                // Always return the current UI font to respond to zoom changes
                return UIUtil.getLabelFont();
            }
            
            @Override
            public void setFont(Font font) {
                // Override to always use UI font for zoom responsiveness
                super.setFont(UIUtil.getLabelFont());
            }
            
            @Override
            public Dimension getPreferredSize() {
                // Get the parent's width to calculate proper text wrapping
                Container parent = getParent();
                if (parent != null) {
                    int width = parent.getWidth();
                    if (width > 0) {
                        // Account for borders and padding
                        width = width - 20; // Leave some margin
                        setSize(width, Short.MAX_VALUE);
                        Dimension d = super.getPreferredSize();
                        return new Dimension(width, d.height);
                    }
                }
                return super.getPreferredSize();
            }
            
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return true;
            }
            
            @Override
            public void paint(Graphics g) {
                // Ensure font is always current before painting
                setFont(UIUtil.getLabelFont());
                super.paint(g);
            }
            
            @Override
            public void setText(String t) {
                // Always update font before setting text to ensure proper sizing
                setFont(UIUtil.getLabelFont());
                super.setText(t);
            }
        };
    }
    
    /**
     * Sets up theme change listener to handle zoom and font changes.
     */
    private void setupThemeChangeListener() {
        // Only listen for component visibility changes (when panel is shown)
        // This prevents constant revalidation that causes auto-scroll issues
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent event) {
                SwingUtilities.invokeLater(() -> {
                    updateExplanationTextIfNeeded();
                });
            }
        });
    }
    
    /**
     * Updates explanation text only if font size has changed significantly.
     */
    private void updateExplanationTextIfNeeded() {
        // Check if font size has changed significantly since last update
        int currentFontSize = UIUtil.getLabelFont().getSize();
        if (Math.abs(currentFontSize - lastKnownFontSize) >= 2) { // Only update if font changed by 2+ points
            lastKnownFontSize = currentFontSize;
            updateExplanationText();
        }
    }
    
    /**
     * Sets up event handlers for all interactive components.
     */
    private void setupEventHandlers() {
        // AI enabled checkbox handler
        aiEnabledCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleAIEnabledChange();
            }
        });
        
        // Hyperlink handling is already set up in createExplanationTextPane()
    }
    
    /**
     * Handles changes to the AI enabled checkbox.
     * Shows appropriate consent or confirmation dialogs.
     */
    private void handleAIEnabledChange() {
        if (aiEnabledCheckBox.isSelected()) {
            // User is enabling AI features - show consent dialog
            showConsentDialog();
        } else {
            // User is disabling AI features - show warning dialog
            showDisableWarningDialog();
        }
    }
    
    /**
     * Shows the consent dialog when user enables AI features.
     */
    private void showConsentDialog() {
        AIConsentDialog dialog = new AIConsentDialog();
        if (dialog.showAndGet()) {
            // User accepted consent
            aiSettings.setUserConsentGiven(true);
            aiSettings.setAIAnalysisEnabled(true);
            updateExplanationText();
            
            LOG.info("AI analysis enabled with user consent");
            
            // Notify callback if provided
            if (onAIStateChanged != null) {
                onAIStateChanged.run();
            }
        } else {
            // User declined consent - revert checkbox
            aiEnabledCheckBox.setSelected(false);
            LOG.info("AI analysis consent declined by user");
        }
    }
    

    
    /**
     * Shows warning dialog when user tries to disable AI analysis.
     */
    private void showDisableWarningDialog() {
        DisableAIWarningDialog dialog = new DisableAIWarningDialog();
        if (dialog.showAndGet()) {
            // User confirmed - disable AI analysis features
            aiSettings.setAIAnalysisEnabled(false);
            aiSettings.setUserConsentGiven(false);
            updateExplanationText();
            
            LOG.info("AI analysis disabled by user");
            
            // Notify callback if provided
            if (onAIStateChanged != null) {
                onAIStateChanged.run();
            }
        } else {
            // User cancelled - revert checkbox
            aiEnabledCheckBox.setSelected(true);
            LOG.info("AI analysis disable cancelled by user");
        }
    }
    
    /**
     * Shows detailed privacy information.
     */
    private void showPrivacyDetails() {
        Messages.showInfoMessage(
            "Privacy Information\n\n" +
            "TRACE AI Analysis helps you debug test failures by sending " +
            "failure information to AI services for analysis.\n\n" +
            "What we collect:\n" +
            "• Test failure details (stack traces, error messages)\n" +
            "• Test context (class names, method names)\n" +
            "• Your questions and follow-up messages\n\n" +
            "What we DON'T collect:\n" +
            "• Source code files\n" +
            "• Personal information\n" +
            "• Project structure\n\n" +
            "Data handling:\n" +
            "• Data is sent securely to AI services\n" +
            "• Data is not stored permanently by TRACE\n" +
            "• AI services may store data according to their policies\n" +
            "• You can revoke access at any time\n\n" +
            "For more information, visit our privacy policy.",
            "Privacy Information"
        );
    }
    

    
    /**
     * Updates the explanation text based on current settings.
     */
    private void updateExplanationText() {
        String baseText = "The plugin examines your project to understand test context and provide better debugging suggestions. See our <a href='privacy'>Privacy Policy</a> for details.";
        
        // Get current font to ensure zoom responsiveness - use the same font as the checkbox
        Font currentFont = UIUtil.getLabelFont();
        int currentFontSize = currentFont.getSize();
        
        // Only update if font size changed or if this is the first time
        boolean shouldUpdate = lastFontSize != currentFontSize;
        lastFontSize = currentFontSize;
        
        // Do NOT force font size/family in HTML; let the component font apply so it matches the checkbox exactly
        String htmlContent;
        if (aiSettings.isAIAnalysisEnabled() && aiSettings.hasUserConsent()) {
            htmlContent = "<html><body style='line-height: 1.2; margin: 0; padding: 0;'>" +
                "AI analysis is enabled and provides debugging suggestions for test failures. " + baseText +
                "</body></html>";
        } else {
            htmlContent = "<html><body style='line-height: 1.2; margin: 0; padding: 0;'>" +
                "Enable AI analysis to get debugging suggestions for test failures. " + baseText +
                "</body></html>";
        }
        
        explanationTextPane.setText(htmlContent);
        
        if (shouldUpdate) {
            explanationTextPane.revalidate();
            explanationTextPane.repaint();
        }
    }
    
    /**
     * Loads current settings from the AISettings service.
     */
    public void loadCurrentSettings() {
        aiEnabledCheckBox.setSelected(aiSettings.isAIAnalysisEnabled());
        updateExplanationText();
        
        // Update original state
        originalAIEnabled = aiSettings.isAIAnalysisEnabled();
        originalUserConsent = aiSettings.hasUserConsent();
    }
    
    /**
     * Applies current UI settings to the AISettings service.
     */
    public void apply() {
        // Settings are applied immediately when user interacts with the UI
        // This method is called by the parent configurable for consistency
    }
    
    /**
     * Resets the panel to the current saved settings.
     */
    public void reset() {
        loadCurrentSettings();
    }
    
    /**
     * Disposes of UI resources.
     */
    public void disposeUIResources() {
        dispose();
    }
    
    @Override
    public void dispose() {
        // Clean up any resources if needed
        // The message bus connection will be automatically cleaned up
    }
    
    /**
     * Checks if the AI analysis is enabled.
     * 
     * @return true if AI analysis is enabled, false otherwise
     */
    public boolean isAIEnabled() {
        return aiEnabledCheckBox.isSelected();
    }
    
    /**
     * Checks if the user has given consent.
     * 
     * @return true if user has given consent, false otherwise
     */
    public boolean hasUserConsent() {
        return aiSettings.hasUserConsent();
    }
    
    /**
     * Checks if any settings have been modified.
     * 
     * @return true if settings have been modified, false otherwise
     */
    public boolean isModified() {
        return aiEnabledCheckBox.isSelected() != originalAIEnabled ||
               aiSettings.hasUserConsent() != originalUserConsent;
    }
    
    /**
     * Consent dialog for AI analysis features.
     * 
     * <p>This dialog explains data usage and collects user consent
     * following JetBrains UI guidelines with clear, non-technical language.</p>
     */
    private static class AIConsentDialog extends DialogWrapper {
        
        public AIConsentDialog() {
            super(true);
            setTitle("AI Analysis Consent");
            setResizable(false);
            init();
        }
        
        @Override
        protected @Nullable JComponent createCenterPanel() {
            JPanel panel = new JBPanel<>(new BorderLayout());
            panel.setPreferredSize(new Dimension(500, 400));
            panel.setBorder(JBUI.Borders.empty(20));
            
            // Create content
            JEditorPane contentArea = new JEditorPane();
            contentArea.setEditable(false);
            contentArea.setContentType("text/html");
            contentArea.setBackground(panel.getBackground());
            contentArea.setFont(panel.getFont());
            contentArea.setText(
                "<html><body style='font-family: " + panel.getFont().getFamily() + "; font-size: " + (panel.getFont().getSize() - 2) + "px; line-height: 1.4;'>" +
                "<h3 style='font-size: " + (panel.getFont().getSize() + 2) + "px; margin-top: 0; margin-bottom: 12px;'>Privacy Notice</h3>" +
                "<p style='margin: 8px 0;'>TRACE AI Analysis sends test failure information to AI services " +
                "to provide debugging suggestions.</p>" +
                "<p style='margin: 12px 0 6px 0; font-weight: bold;'>What we collect:</p>" +
                "<ul style='margin: 6px 0 12px 0; padding-left: 20px;'>" +
                "<li style='margin: 3px 0;'>Test failure details (stack traces, error messages)</li>" +
                "<li style='margin: 3px 0;'>Project structure information (to locate step definitions)</li>" +
                "<li style='margin: 3px 0;'>Your questions and follow-up messages</li>" +
                "</ul>" +
                "<p style='margin: 12px 0 6px 0; font-weight: bold;'>What we DON'T collect:</p>" +
                "<ul style='margin: 6px 0 12px 0; padding-left: 20px;'>" +
                "<li style='margin: 3px 0;'>Source code files (only parsed for step definitions)</li>" +
                "<li style='margin: 3px 0;'>Personal information</li>" +
                "<li style='margin: 3px 0;'>Permanent storage of your data</li>" +
                "</ul>" +
                "<p style='margin: 12px 0 6px 0; font-weight: bold;'>Data handling:</p>" +
                "<ul style='margin: 6px 0 12px 0; padding-left: 20px;'>" +
                "<li style='margin: 3px 0;'>Data is sent securely to AI services</li>" +
                "<li style='margin: 3px 0;'>Data is not stored permanently by TRACE</li>" +
                "<li style='margin: 3px 0;'>AI services may store data according to their policies</li>" +
                "<li style='margin: 3px 0;'>You can revoke access at any time</li>" +
                "</ul>" +
                "<p style='margin: 12px 0 6px 0; font-weight: bold;'>Billing and Usage:</p>" +
                "<ul style='margin: 6px 0 12px 0; padding-left: 20px;'>" +
                "<li style='margin: 3px 0;'>You are responsible for your own AI service usage and billing</li>" +
                "<li style='margin: 3px 0;'>TRACE does not charge for AI analysis - costs are from your AI service provider</li>" +
                "<li style='margin: 3px 0;'>Monitor your AI service usage and billing through your provider's dashboard</li>" +
                "</ul>" +
                "<p style='margin: 12px 0;'>By accepting, you agree to allow TRACE to send test failure " +
                "data to AI services for analysis and debugging assistance.</p>" +
                "<p style='margin: 8px 0;'>For complete details, see our <a href=\"https://alexibasitas.github.io/TRACE/PRIVACY.html\">Privacy Policy</a>.</p>" +
                "</body></html>"
            );
            
            // Add hyperlink listener
            contentArea.addHyperlinkListener(e -> {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    BrowserUtil.browse(e.getURL());
                }
            });
            
            // Add scroll pane
            JScrollPane scrollPane = new JScrollPane(contentArea);
            scrollPane.setBorder(JBUI.Borders.empty());
            
            panel.add(scrollPane, BorderLayout.CENTER);
            
            return panel;
        }
        
        @Override
        protected Action[] createActions() {
            return new Action[]{
                getOKAction(),
                getCancelAction()
            };
        }
        
        @Override
        protected void doOKAction() {
            super.doOKAction();
        }
        
        @Override
        public void doCancelAction() {
            super.doCancelAction();
        }
    }
    
    /**
     * Warning dialog when user tries to disable AI analysis.
     * 
     * <p>This dialog explains what features will be lost and confirms
     * the user's intent to disable AI analysis.</p>
     */
    private static class DisableAIWarningDialog extends DialogWrapper {
        
        public DisableAIWarningDialog() {
            super(true);
            setTitle("Disable AI Analysis?");
            setResizable(false);
            init();
        }
        
        @Override
        protected @Nullable JComponent createCenterPanel() {
            JPanel panel = new JBPanel<>(new BorderLayout());
            panel.setBorder(JBUI.Borders.empty(20));
            
            // Create content with proper text wrapping
            JTextArea contentArea = new JTextArea();
            contentArea.setEditable(false);
            contentArea.setLineWrap(true);
            contentArea.setWrapStyleWord(true);
            contentArea.setBackground(panel.getBackground());
            contentArea.setFont(panel.getFont());
            contentArea.setText(
                "You will lose access to AI-powered debugging suggestions for test failures. Are you sure you want to disable AI analysis?"
            );
            
            // Set a reasonable width and let height adjust naturally
            contentArea.setPreferredSize(new Dimension(350, 60));
            contentArea.setMinimumSize(new Dimension(350, 60));
            
            panel.add(contentArea, BorderLayout.CENTER);
            
            return panel;
        }
        
        @Override
        protected Action[] createActions() {
            return new Action[]{
                getOKAction(),
                getCancelAction()
            };
        }
        
        @Override
        protected void doOKAction() {
            super.doOKAction();
        }
        
        @Override
        public void doCancelAction() {
            super.doCancelAction();
        }
    }
    

} 