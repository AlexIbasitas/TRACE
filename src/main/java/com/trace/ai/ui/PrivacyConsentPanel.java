package com.trace.ai.ui;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ide.BrowserUtil;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.trace.ai.configuration.AISettings;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
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
public class PrivacyConsentPanel extends JBPanel<PrivacyConsentPanel> {
    
    // UI Components
    private final JBCheckBox aiEnabledCheckBox;
    private final JEditorPane explanationPane;
    
    // Settings service
    private final AISettings aiSettings;
    
    // Callback for AI service config panel
    private Runnable onAIStateChanged;
    
    // State tracking for modification detection
    private boolean originalAIEnabled;
    private boolean originalUserConsent;
    
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
        this.explanationPane = new JEditorPane();
        
        // Initialize the panel
        initializePanel();
        setupEventHandlers();
        loadCurrentSettings();
    }
    
    /**
     * Initializes the panel layout and styling.
     */
    private void initializePanel() {
        setLayout(new BorderLayout());
        setBorder(JBUI.Borders.compound(
            JBUI.Borders.customLine(UIUtil.getPanelBackground().darker(), 1),
            JBUI.Borders.empty(10)
        ));
        
        // Make the panel itself responsive using proper Swing sizing
        setMinimumSize(new Dimension(300, 0));
        setPreferredSize(new Dimension(400, 160));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        
        // Create header
        JBLabel headerLabel = new JBLabel("Privacy & Consent");
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 14f));
        headerLabel.setBorder(JBUI.Borders.emptyBottom(5));
        
        // Create main content panel with responsive layout
        JPanel contentPanel = new JBPanel<>(new BorderLayout());
        contentPanel.setBorder(JBUI.Borders.empty(2));
        
        // Add checkbox and explanation with proper wrapping
        JPanel checkboxPanel = new JBPanel<>(new BorderLayout());
        checkboxPanel.setBorder(JBUI.Borders.empty(2));
        checkboxPanel.add(aiEnabledCheckBox, BorderLayout.NORTH);
        
        // Configure explanation pane for proper text wrapping and clickable links
        explanationPane.setBorder(JBUI.Borders.emptyTop(5));
        explanationPane.setEditable(false);
        explanationPane.setOpaque(false);
        explanationPane.setBackground(UIUtil.getPanelBackground());
        explanationPane.setFont(UIUtil.getLabelFont());
        explanationPane.setForeground(UIUtil.getLabelForeground());
        explanationPane.setContentType("text/html");
        
        // Set proper sizing according to JetBrains guidelines
        // Pane: min 270px width, min 45px height (2-3 lines)
        explanationPane.setMinimumSize(new Dimension(270, 45));
        explanationPane.setPreferredSize(new Dimension(300, 60));
        explanationPane.setMaximumSize(new Dimension(600, 80));
        
        checkboxPanel.add(explanationPane, BorderLayout.CENTER);
        checkboxPanel.setBorder(JBUI.Borders.emptyTop(5));
        
        // Assemble the panel
        add(headerLabel, BorderLayout.NORTH);
        contentPanel.add(checkboxPanel, BorderLayout.CENTER);
        add(contentPanel, BorderLayout.CENTER);
        
        // Set explanation text
        updateExplanationText();
        
        // Ensure components are visible
        aiEnabledCheckBox.setVisible(true);
        explanationPane.setVisible(true);
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
        
        // Privacy Policy link handler
        explanationPane.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    BrowserUtil.browse("https://alexibasitas.github.io/TRACE/PRIVACY.html");
                }
            }
        });
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
            
            // Notify callback if provided
            if (onAIStateChanged != null) {
                onAIStateChanged.run();
            }
        } else {
            // User declined consent - revert checkbox
            aiEnabledCheckBox.setSelected(false);
        }
    }
    

    
    /**
     * Shows warning dialog when user tries to disable AI analysis.
     */
    private void showDisableWarningDialog() {
        int result = Messages.showYesNoDialog(
            "You will lose access to AI-powered debugging suggestions for test failures. Are you sure you want to disable AI analysis?",
            "Disable AI Analysis?",
            "Disable",
            "Cancel",
            Messages.getQuestionIcon(),
            null
        );
        
        if (result == Messages.YES) {
            // User confirmed - disable AI analysis features
            aiSettings.setAIAnalysisEnabled(false);
            aiSettings.setUserConsentGiven(false);
            updateExplanationText();
            
            // Notify callback if provided
            if (onAIStateChanged != null) {
                onAIStateChanged.run();
            }
        } else {
            // User cancelled - revert checkbox
            aiEnabledCheckBox.setSelected(true);
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
        String baseText = "The plugin examines your project to understand test context and provide better debugging suggestions. See our <a href=\"#\">Privacy Policy</a> for details.";
        
        if (aiSettings.isAIAnalysisEnabled() && aiSettings.hasUserConsent()) {
            explanationPane.setText(
                "AI analysis is enabled and provides debugging suggestions for test failures. " + baseText
            );
        } else {
            explanationPane.setText(
                "Enable AI analysis to get debugging suggestions for test failures. " + baseText
            );
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
        // No specific cleanup needed for this panel
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
    

} 