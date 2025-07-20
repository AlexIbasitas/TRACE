package com.triagemate.settings;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.triagemate.ui.TriagePanelConstants;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Privacy and Consent panel for TriageMate AI settings.
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
    private final JTextArea explanationTextArea;
    private final JButton learnMoreButton;
    private final JButton revokeAccessButton;
    
    // Settings service
    private final AISettings aiSettings;
    
    // State tracking for modification detection
    private boolean originalAIEnabled;
    private boolean originalUserConsent;
    
    /**
     * Constructor for PrivacyConsentPanel.
     * 
     * @param aiSettings the AISettings service for data persistence
     */
    public PrivacyConsentPanel(AISettings aiSettings) {
        this.aiSettings = aiSettings;
        
        // Initialize state tracking
        this.originalAIEnabled = aiSettings.isAIEnabled();
        this.originalUserConsent = aiSettings.hasUserConsent();
        
        // Create UI components
        this.aiEnabledCheckBox = new JBCheckBox("Enable AI analysis features");
        this.explanationTextArea = new JTextArea();
        this.learnMoreButton = new JButton("Learn More");
        this.revokeAccessButton = new JButton("Revoke Access");
        
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
            JBUI.Borders.empty(15)
        ));
        
        // Make the panel itself responsive using proper Swing sizing
        setMinimumSize(new Dimension(300, 0));
        setPreferredSize(new Dimension(400, 120));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        
        // Create header
        JBLabel headerLabel = new JBLabel("🔒 Privacy & Consent");
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 14f));
        headerLabel.setBorder(JBUI.Borders.emptyBottom(10));
        
        // Create main content panel with responsive layout
        JPanel contentPanel = new JBPanel<>(new BorderLayout());
        contentPanel.setBorder(JBUI.Borders.empty(5));
        // Let the layout manager handle sizing naturally
        
        // Add checkbox and explanation with proper wrapping
        JPanel checkboxPanel = new JBPanel<>(new BorderLayout());
        checkboxPanel.add(aiEnabledCheckBox, BorderLayout.NORTH);
        
        // Configure explanation text area for proper text wrapping using JetBrains approach
        explanationTextArea.setBorder(JBUI.Borders.emptyTop(8));
        explanationTextArea.setLineWrap(true);
        explanationTextArea.setWrapStyleWord(true);
        explanationTextArea.setEditable(false);
        explanationTextArea.setOpaque(false);
        explanationTextArea.setFont(TriagePanelConstants.MESSAGE_FONT);
        explanationTextArea.setForeground(TriagePanelConstants.WHITE);
        
        // Set minimum width following JetBrains guidelines for paragraph text
        explanationTextArea.setMinimumSize(new Dimension(TriagePanelConstants.MIN_SETTINGS_WIDTH_BEFORE_SCROLL, 40));
        
        // Force the text area to wrap by setting a maximum width
        explanationTextArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        
        // Configure text area for proper responsive behavior
        explanationTextArea.setLineWrap(true);
        explanationTextArea.setWrapStyleWord(true);
        explanationTextArea.setEditable(false);
        explanationTextArea.setOpaque(false);
        explanationTextArea.setBackground(UIUtil.getPanelBackground());
        explanationTextArea.setFont(UIUtil.getLabelFont());
        
        // Set proper sizing according to JetBrains guidelines
        // Text area: min 270px width, min 55px height (3 lines)
        explanationTextArea.setMinimumSize(new Dimension(270, 55));
        explanationTextArea.setPreferredSize(new Dimension(300, 60));
        explanationTextArea.setMaximumSize(new Dimension(600, Integer.MAX_VALUE));
        
        checkboxPanel.add(explanationTextArea, BorderLayout.CENTER);
        checkboxPanel.setBorder(JBUI.Borders.emptyTop(5));
        
        // Create responsive button panel
        JPanel buttonPanel = new JBPanel<>(new BorderLayout());
        buttonPanel.setBorder(JBUI.Borders.emptyTop(10));
        
        // Use FlowLayout for buttons with proper wrapping
        JPanel buttonContainer = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 10, 5));
        buttonContainer.add(learnMoreButton);
        buttonContainer.add(revokeAccessButton);
        
        buttonPanel.add(buttonContainer, BorderLayout.WEST);
        
        // Assemble the panel
        add(headerLabel, BorderLayout.NORTH);
        contentPanel.add(checkboxPanel, BorderLayout.CENTER);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(contentPanel, BorderLayout.CENTER);
        
        // Set explanation text
        updateExplanationText();
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
        
        // Learn More button handler
        learnMoreButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showPrivacyDetails();
            }
        });
        
        // Revoke Access button handler
        revokeAccessButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleRevokeAccess();
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
            // User is disabling AI features - show confirmation dialog
            showDisableConfirmationDialog();
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
            aiSettings.setAIEnabled(true);
            updateExplanationText();
            updateButtonStates();
        } else {
            // User declined consent - revert checkbox
            aiEnabledCheckBox.setSelected(false);
        }
    }
    
    /**
     * Shows the confirmation dialog when user disables AI features.
     */
    private void showDisableConfirmationDialog() {
        int result = Messages.showYesNoDialog(
            "Disable AI Analysis",
            "Are you sure you want to disable AI analysis features?\n\n" +
            "This will:\n" +
            "• Stop AI-powered debugging suggestions\n" +
            "• Clear your API key configuration\n" +
            "• Remove consent for data processing\n\n" +
            "You can re-enable these features at any time in settings.",
            "Disable Features",
            "Cancel",
            Messages.getQuestionIcon()
        );
        
        if (result == Messages.YES) {
            // User confirmed - disable features
            aiSettings.setAIEnabled(false);
            aiSettings.setUserConsentGiven(false);
            updateExplanationText();
            updateButtonStates();
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
            "TriageMate AI Analysis helps you debug test failures by sending " +
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
            "• Data is not stored permanently by TriageMate\n" +
            "• AI services may store data according to their policies\n" +
            "• You can revoke access at any time\n\n" +
            "For more information, visit our privacy policy.",
            "Privacy Information"
        );
    }
    
    /**
     * Handles the revoke access button click.
     */
    private void handleRevokeAccess() {
        RevokeAccessDialog dialog = new RevokeAccessDialog();
        if (dialog.showAndGet()) {
            // Revoke all access
            aiSettings.setAIEnabled(false);
            aiSettings.setUserConsentGiven(false);
            aiEnabledCheckBox.setSelected(false);
            updateExplanationText();
            updateButtonStates();
        }
    }
    
    /**
     * Updates the explanation text based on current settings.
     */
    private void updateExplanationText() {
        if (aiSettings.isAIEnabled() && aiSettings.hasUserConsent()) {
            explanationTextArea.setText(
                "AI analysis is enabled. Test failure data and project structure information " +
                "are sent to AI services for debugging suggestions. Source code files are " +
                "parsed to locate step definitions. Data is not stored permanently."
            );
        } else {
            explanationTextArea.setText(
                "Enable AI analysis to get debugging suggestions when tests fail. " +
                "The plugin will parse your source code files to locate step definitions " +
                "and use project structure information. Requires consent and an API key."
            );
        }
    }
    
    /**
     * Updates button states based on current settings.
     */
    private void updateButtonStates() {
        boolean hasConsent = aiSettings.hasUserConsent();
        learnMoreButton.setEnabled(true); // Always enabled
        revokeAccessButton.setEnabled(hasConsent);
    }
    
    /**
     * Loads current settings from the AISettings service.
     */
    public void loadCurrentSettings() {
        aiEnabledCheckBox.setSelected(aiSettings.isAIEnabled());
        updateExplanationText();
        updateButtonStates();
        
        // Update original state
        originalAIEnabled = aiSettings.isAIEnabled();
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
            JTextArea contentArea = new JTextArea();
            contentArea.setEditable(false);
            contentArea.setLineWrap(true);
            contentArea.setWrapStyleWord(true);
            contentArea.setBackground(panel.getBackground());
            contentArea.setFont(panel.getFont());
            contentArea.setText(
                "🔒 Privacy Notice\n\n" +
                "TriageMate AI Analysis sends test failure information to AI services " +
                "to provide debugging suggestions.\n\n" +
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
                "• Data is not stored permanently by TriageMate\n" +
                "• AI services may store data according to their policies\n" +
                "• You can revoke access at any time\n\n" +
                "By accepting, you agree to allow TriageMate to send test failure " +
                "data to AI services for analysis and debugging assistance."
            );
            
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
     * Custom dialog for revoking AI access with proper bulleted list formatting.
     */
    private static class RevokeAccessDialog extends DialogWrapper {
        
        public RevokeAccessDialog() {
            super(true);
            setTitle("Revoke AI Access");
            setResizable(false);
            init();
        }
        
        @Override
        protected @Nullable JComponent createCenterPanel() {
            JPanel panel = new JBPanel<>(new BorderLayout());
            panel.setPreferredSize(new Dimension(450, 200));
            
            // Create warning icon and text panel
            JPanel contentPanel = new JBPanel<>(new BorderLayout(15, 0));
            contentPanel.setBorder(JBUI.Borders.empty(20));
            
            // Add warning icon
            JLabel warningIcon = new JBLabel(Messages.getWarningIcon());
            contentPanel.add(warningIcon, BorderLayout.WEST);
            
            // Create text content with proper HTML formatting
            JTextPane textPane = new JTextPane();
            textPane.setContentType("text/html");
            textPane.setEditable(false);
            textPane.setOpaque(false);
            textPane.setBackground(UIUtil.getPanelBackground());
            textPane.setFont(UIUtil.getLabelFont());
            
            // HTML content with proper bulleted list
            String htmlContent = 
                "<html><body style='margin: 0; padding: 0;'>" +
                "<p style='margin: 0 0 10px 0;'>This will immediately:</p>" +
                "<ul style='margin: 0 0 15px 0; padding-left: 20px;'>" +
                "<li>Disable all AI analysis features</li>" +
                "<li>Clear your API key configuration</li>" +
                "<li>Remove consent for data processing</li>" +
                "<li>Clear any stored chat history</li>" +
                "</ul>" +
                "<p style='margin: 0;'>Are you sure you want to revoke access?</p>" +
                "</body></html>";
            
            textPane.setText(htmlContent);
            
            // Wrap in scroll pane for safety
            JBScrollPane scrollPane = new JBScrollPane(textPane);
            scrollPane.setBorder(null);
            scrollPane.setOpaque(false);
            scrollPane.getViewport().setOpaque(false);
            
            contentPanel.add(scrollPane, BorderLayout.CENTER);
            panel.add(contentPanel, BorderLayout.CENTER);
            
            return panel;
        }
        
        @Override
        protected Action[] createActions() {
            return new Action[]{
                getCancelAction(),
                getOKAction()
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