package com.trace.ai.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.icons.AllIcons;
import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.DefaultListModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ListSelectionModel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.trace.security.SecureAPIKeyManager;
import com.trace.ai.services.AIModelService;
import com.trace.ai.models.AIModel;
import com.trace.ai.configuration.AIServiceType;
import com.trace.ai.configuration.AISettings;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;
import com.trace.common.constants.TriagePanelConstants;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.application.ApplicationManager;
import com.trace.chat.ui.APIKeyManagerHelper;
import com.trace.ai.ui.ModelManagerHelper;
import com.trace.ai.ui.AIServiceConfigUIHelper;

/**
 * Enhanced AI Service Configuration panel for TRACE settings.
 * 
 * <p>This panel provides a comprehensive interface for configuring AI services,
 * including service selection, API key management, connection testing, and
 * automatic model discovery and management.</p>
 * 
 * <p>The panel includes:</p>
 * <ul>
 *   <li>API key configuration for OpenAI and Google Gemini</li>
 *   <li>Automatic model discovery when API keys are configured</li>
 *   <li>Model list display with status indicators</li>
 *   <li>Default model selection</li>
 *   <li>Model enable/disable functionality</li>
 *   <li>Real-time validation and user feedback</li>
 * </ul>
 * 
 * <p>This component automatically discovers available models when API keys
 * are configured and provides a simple interface for model management.</p>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
public class AIServiceConfigPanel extends JBPanel<AIServiceConfigPanel> {
    
    private static final Logger LOG = Logger.getInstance(AIServiceConfigPanel.class);
    
    // Services
    private final AISettings aiSettings;
    private final AIModelService modelService;
    private final APIKeyManagerHelper apiKeyHelper;
    private final ModelManagerHelper modelManagerHelper;
    private final AIServiceConfigUIHelper uiHelper;
    
    // API Key Configuration
    private final JBPasswordField openaiApiKeyField;
    private final JBPasswordField geminiApiKeyField;
    private final JButton testOpenAIButton;
    private final JButton testGeminiButton;
    private final JBLabel openaiStatusLabel;
    private final JBLabel geminiStatusLabel;
    
    // Model Management
    private final JBList<AIModel> modelList;
    private final DefaultListModel<AIModel> listModel;
    private final JBLabel defaultModelLabel;
    private final JButton setDefaultButton;
    private final JButton refreshModelsButton;
    
    // State tracking
    private String originalOpenAIKey;
    private String originalGeminiKey;
    private final java.util.concurrent.atomic.AtomicBoolean isTestingConnection = new java.util.concurrent.atomic.AtomicBoolean(false);
    
    // Independent status tracking for each service
    private String openaiStatus = "Not configured";
    private String geminiStatus = "Not configured";
    
    // Font size tracking for responsive updates
    private final java.util.concurrent.atomic.AtomicInteger lastKnownFontSize = new java.util.concurrent.atomic.AtomicInteger(-1);
    
    /**
     * Creates a new AI service configuration panel.
     * 
     * @param aiSettings the AI settings instance
     */
    public AIServiceConfigPanel(@NotNull AISettings aiSettings) {
        this.aiSettings = aiSettings;
        this.modelService = AIModelService.getInstance();
        this.apiKeyHelper = new APIKeyManagerHelper();
        this.modelManagerHelper = new ModelManagerHelper();
        this.uiHelper = new AIServiceConfigUIHelper();
        
        // Initialize UI components
        this.openaiApiKeyField = uiHelper.createResponsivePasswordField();
        this.geminiApiKeyField = uiHelper.createResponsivePasswordField();
        this.testOpenAIButton = uiHelper.createResponsiveButton("Apply");
        this.testGeminiButton = uiHelper.createResponsiveButton("Apply");
        this.openaiStatusLabel = new JBLabel("Not configured");
        this.geminiStatusLabel = new JBLabel("Not configured");
        
        // Initialize model list components
        this.listModel = new DefaultListModel<>();
        this.modelList = new JBList<>(listModel);
        this.defaultModelLabel = new JBLabel("No default model selected");
        this.setDefaultButton = uiHelper.createResponsiveButton("Set as Default");
        this.refreshModelsButton = uiHelper.createResponsiveButton("Refresh Models");
        
        // Initialize font size tracking
        this.lastKnownFontSize.set(UIUtil.getLabelFont().getSize());
        
        // Initialize the panel
        initializePanel();
        setupEventHandlers();
        uiHelper.setupThemeChangeListener(this, modelList, testOpenAIButton, testGeminiButton, 
                                         setDefaultButton, refreshModelsButton, lastKnownFontSize);
        
        // Load settings in background to avoid EDT violations
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            // Update original state in background
            updateOriginalState();
            
            // Load current settings in background
            loadCurrentSettings();
            
            LOG.info("AI service configuration panel created and initialized");
        });
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
        
        // Allow sections to expand to fit their content naturally
        int panelBaseFontSize = UIUtil.getLabelFont().getSize();
        
        // Let Swing calculate natural size instead of forcing fixed dimensions
        setMinimumSize(new Dimension(0, 0)); // Allow shrinking
        setPreferredSize(null); // Let Swing calculate natural size
        setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        
        // Create header with zoom responsiveness
        JBLabel headerLabel = uiHelper.createZoomResponsiveHeader("AI Model Configuration");
        headerLabel.setBorder(JBUI.Borders.emptyBottom(3)); // Smaller border
        
        // Create main content panel
        JPanel contentPanel = new JBPanel<>(new BorderLayout());
        contentPanel.setBorder(JBUI.Borders.empty(5));
        
        // API Key Configuration Panel
        JPanel apiKeyPanel = uiHelper.createAPIKeyPanel(openaiApiKeyField, geminiApiKeyField, 
                                                       testOpenAIButton, testGeminiButton,
                                                       openaiStatusLabel, geminiStatusLabel,
                                                       () -> apiKeyHelper.clearOpenAIKey(openaiApiKeyField, openaiStatusLabel, () -> refreshModelList()),
                                                       () -> apiKeyHelper.clearGeminiKey(geminiApiKeyField, geminiStatusLabel, () -> refreshModelList()));
        
        // Model Management Panel
        JPanel modelPanel = uiHelper.createModelPanel(modelList, defaultModelLabel, setDefaultButton, refreshModelsButton);
        
        // Add panels to content
        contentPanel.add(apiKeyPanel, BorderLayout.NORTH);
        contentPanel.add(modelPanel, BorderLayout.CENTER);
        
        // Add to main panel
        add(headerLabel, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);
    }
    
    
    
    
    

    
    /**
     * Sets up event handlers for all components.
     */
    private void setupEventHandlers() {
        // API Key testing
        testOpenAIButton.addActionListener(e -> apiKeyHelper.testOpenAIKey(openaiApiKeyField, openaiStatusLabel, testOpenAIButton, isTestingConnection, this::discoverNewModels, this));
        testGeminiButton.addActionListener(e -> apiKeyHelper.testGeminiKey(geminiApiKeyField, geminiStatusLabel, testGeminiButton, isTestingConnection, this::discoverNewModels, this));
        
        // Model management
        setDefaultButton.addActionListener(e -> modelManagerHelper.setSelectedAsDefault(modelList, modelService, this, this::updateDefaultModelDisplay));
        refreshModelsButton.addActionListener(e -> modelManagerHelper.refreshModelList(modelList, listModel, refreshModelsButton, modelService, this));
        modelList.addListSelectionListener(e -> updateButtonStates());
        
        // API key change listeners
        openaiApiKeyField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) { apiKeyHelper.handleOpenAIKeyChange(openaiApiKeyField, openaiStatusLabel); }
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) { apiKeyHelper.handleOpenAIKeyChange(openaiApiKeyField, openaiStatusLabel); }
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) { apiKeyHelper.handleOpenAIKeyChange(openaiApiKeyField, openaiStatusLabel); }
        });
        
        geminiApiKeyField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) { apiKeyHelper.handleGeminiKeyChange(geminiApiKeyField, geminiStatusLabel); }
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) { apiKeyHelper.handleGeminiKeyChange(geminiApiKeyField, geminiStatusLabel); }
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) { apiKeyHelper.handleGeminiKeyChange(geminiApiKeyField, geminiStatusLabel); }
        });
    }
    
    /**
     * Loads current settings into the UI.
     */
    public void loadCurrentSettings() {
        LOG.info("Loading current AI service settings into UI");
        
        // Load API keys from secure storage on background thread
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            String storedOpenAIKey = SecureAPIKeyManager.getAPIKey(AIServiceType.OPENAI);
            String storedGeminiKey = SecureAPIKeyManager.getAPIKey(AIServiceType.GEMINI);
            
            // Update UI on EDT after loading keys
            ApplicationManager.getApplication().invokeLater(() -> {
                // Always load stored keys into UI (this is what user expects when reopening settings)
                if (storedOpenAIKey != null && !storedOpenAIKey.trim().isEmpty()) {
                    openaiApiKeyField.setText(storedOpenAIKey);
                    openaiStatusLabel.setText("Connected");
                    openaiStatusLabel.setForeground(UIUtil.getLabelSuccessForeground());
                    openaiStatusLabel.setIcon(AllIcons.General.InspectionsOK);
                    LOG.debug("Loaded OpenAI API key into UI");
                } else {
                    openaiApiKeyField.setText("");
                    openaiStatusLabel.setText("Not Connected");
                    openaiStatusLabel.setForeground(new TriagePanelConstants().errorForeground);
                    openaiStatusLabel.setIcon(AllIcons.General.Error);
                    LOG.debug("No OpenAI API key found in storage");
                }
                
                if (storedGeminiKey != null && !storedGeminiKey.trim().isEmpty()) {
                    geminiApiKeyField.setText(storedGeminiKey);
                    geminiStatusLabel.setText("Connected");
                    geminiStatusLabel.setForeground(UIUtil.getLabelSuccessForeground());
                    geminiStatusLabel.setIcon(AllIcons.General.InspectionsOK);
                    LOG.debug("Loaded Gemini API key into UI");
                } else {
                    geminiApiKeyField.setText("");
                    geminiStatusLabel.setText("Not Connected");
                    geminiStatusLabel.setForeground(new TriagePanelConstants().errorForeground);
                    geminiStatusLabel.setIcon(AllIcons.General.Error);
                    LOG.debug("No Gemini API key found in storage");
                }
                
                // Show existing models from storage (no discovery)
                refreshModelList(storedOpenAIKey, storedGeminiKey);
                
                LOG.info("Finished loading AI service settings into UI");
            });
        });
    }
    
    /**
     * Updates the original state for modification detection.
     */
    private void updateOriginalState() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            String openaiKey = SecureAPIKeyManager.getAPIKey(AIServiceType.OPENAI);
            String geminiKey = SecureAPIKeyManager.getAPIKey(AIServiceType.GEMINI);
            
            ApplicationManager.getApplication().invokeLater(() -> {
                this.originalOpenAIKey = openaiKey;
                this.originalGeminiKey = geminiKey;
            });
        });
    }
    
    /**
     * Refreshes the model list with provided API keys.
     * Only shows models for services with valid stored API keys.
     */
    private void refreshModelList(String storedOpenAIKey, String storedGeminiKey) {
        modelManagerHelper.refreshModelList(modelList, listModel, modelService, storedOpenAIKey, storedGeminiKey);
        updateDefaultModelDisplay();
        updateButtonStates();
    }
    
    /**
     * Refreshes the model list using stored API keys.
     */
    private void refreshModelList() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            String storedOpenAIKey = SecureAPIKeyManager.getAPIKey(AIServiceType.OPENAI);
            String storedGeminiKey = SecureAPIKeyManager.getAPIKey(AIServiceType.GEMINI);
            
            ApplicationManager.getApplication().invokeLater(() -> {
                refreshModelList(storedOpenAIKey, storedGeminiKey);
            });
        });
    }
    
    /**
     * Discovers and adds new models for configured services.
     */
    private void discoverNewModels() {
        LOG.debug("Discovering new models after successful API key validation");
        
        // Get stored keys in background
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            String storedOpenAIKey = SecureAPIKeyManager.getAPIKey(AIServiceType.OPENAI);
            String storedGeminiKey = SecureAPIKeyManager.getAPIKey(AIServiceType.GEMINI);
            
            modelManagerHelper.discoverNewModels(modelService, storedOpenAIKey, storedGeminiKey);
            
            // Refresh the model list to show the newly created models
            ApplicationManager.getApplication().invokeLater(() -> {
                refreshModelList(storedOpenAIKey, storedGeminiKey);
            });
        });
    }
    
    
    
    
    
    
    /**
     * Updates button states based on current selection.
     */
    private void updateButtonStates() {
        modelManagerHelper.updateButtonStates(modelList, setDefaultButton);
    }
    
    /**
     * Updates the default model display label.
     */
    private void updateDefaultModelDisplay() {
        modelManagerHelper.updateDefaultModelDisplay(defaultModelLabel, modelService);
    }
    

    
    
    /**
     * Checks if the panel has been modified.
     * 
     * @return true if any settings have changed, false otherwise
     */
    public boolean isModified() {
        String currentOpenAIKey = new String(openaiApiKeyField.getPassword());
        String currentGeminiKey = new String(geminiApiKeyField.getPassword());
        
        // Compare current UI state with original stored state
        boolean openaiChanged = !Objects.equals(currentOpenAIKey, originalOpenAIKey);
        boolean geminiChanged = !Objects.equals(currentGeminiKey, originalGeminiKey);
        
        if (openaiChanged || geminiChanged) {
            LOG.debug("AI service config modified - OpenAI: " + openaiChanged + ", Gemini: " + geminiChanged);
        }
        
        return openaiChanged || geminiChanged;
    }
    
    /**
     * Applies the current settings.
     */
    public void apply() {
        LOG.info("Applying AI service configuration settings");
        
        // Only handle clearing empty keys - Apply buttons handle saving valid keys
        String openaiKey = new String(openaiApiKeyField.getPassword());
        String geminiKey = new String(geminiApiKeyField.getPassword());
        
        // Check if we need to clear any keys
        boolean needToClearOpenAI = openaiKey.trim().isEmpty();
        boolean needToClearGemini = geminiKey.trim().isEmpty();
        
        if (needToClearOpenAI || needToClearGemini) {
            // Perform clearing operations on background thread
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                if (needToClearOpenAI) {
                    SecureAPIKeyManager.clearAPIKey(AIServiceType.OPENAI);
                    LOG.info("OpenAI API key cleared");
                }
                
                if (needToClearGemini) {
                    SecureAPIKeyManager.clearAPIKey(AIServiceType.GEMINI);
                    LOG.info("Gemini API key cleared");
                }
                
                // Update UI on EDT after clearing operations
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (needToClearOpenAI) {
                        openaiStatusLabel.setText("Not configured");
                        openaiStatusLabel.setForeground(UIUtil.getLabelForeground());
                    }
                    
                    if (needToClearGemini) {
                        geminiStatusLabel.setText("Not configured");
                        geminiStatusLabel.setForeground(UIUtil.getLabelForeground());
                    }
                    
                    // Update original state for modification detection
                    updateOriginalState();
                    
                    LOG.info("AI service configuration settings applied");
                });
            });
        } else {
            // No clearing needed, just update original state
            updateOriginalState();
            LOG.info("AI service configuration settings applied");
        }
    }
    
    /**
     * Resets the UI to the original settings.
     */
    public void reset() {
        LOG.info("Resetting AI service configuration to current stored state");
        
        // Load current settings from storage (this is what user expects when reopening settings)
        loadCurrentSettings();
        
        LOG.info("AI service configuration reset completed");
    }
    
    /**
     * Disposes UI resources.
     */
    public void disposeUIResources() {
        // Clear sensitive data
        openaiApiKeyField.setText("");
        geminiApiKeyField.setText("");
    }
    
    /**
     * Shows an error message.
     */
    private void showError(String message) {
        Messages.showErrorDialog(this, message, "Configuration Error");
    }
    
    /**
     * Shows a success message.
     */
    private void showSuccess(String message) {
        Messages.showInfoMessage(this, message, "Success");
    }
    
    /**
     * Custom list cell renderer for AI models.
     */
    private static class AIModelListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, 
                                                    int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof AIModel) {
                AIModel model = (AIModel) value;
                String status = model.isEnabled() ? "✓" : "✗";
                String service = model.getServiceType().getDisplayName();
                String displayText = String.format("%s %s (%s)", status, model.getDisplayName(), service);
                
                // Add indicator if this is the default model
                if (AIModelService.getInstance().getDefaultModel() != null && 
                    model.getId().equals(AIModelService.getInstance().getDefaultModel().getId())) {
                    displayText += " [Default]";
                }
                
                setText(displayText);
                
                if (model.isEnabled()) {
                    setForeground(UIUtil.getLabelForeground());
                } else {
                    setForeground(UIUtil.getLabelDisabledForeground());
                }
            }
            
            return this;
        }
    }




    
    
    
    
    /**
     * Gets the OpenAI API key field for testing purposes.
     * 
     * @return the OpenAI API key password field
     */
    @VisibleForTesting
    public JBPasswordField getOpenaiApiKeyField() {
        return openaiApiKeyField;
    }
    
    /**
     * Gets the Gemini API key field for testing purposes.
     * 
     * @return the Gemini API key password field
     */
    @VisibleForTesting
    public JBPasswordField getGeminiApiKeyField() {
        return geminiApiKeyField;
    }
    
    /**
     * Gets the OpenAI status label for testing purposes.
     * 
     * @return the OpenAI status label
     */
    @VisibleForTesting
    public JBLabel getOpenaiStatusLabel() {
        return openaiStatusLabel;
    }
    
    /**
     * Gets the Gemini status label for testing purposes.
     * 
     * @return the Gemini status label
     */
    @VisibleForTesting
    public JBLabel getGeminiStatusLabel() {
        return geminiStatusLabel;
    }
    
    /**
     * Gets the OpenAI test/apply button for testing purposes.
     * 
     * @return the OpenAI test/apply button
     */
    @VisibleForTesting
    public JButton getTestOpenAIButton() {
        return testOpenAIButton;
    }
    
    /**
     * Gets the Gemini test/apply button for testing purposes.
     * 
     * @return the Gemini test/apply button
     */
    @VisibleForTesting
    public JButton getTestGeminiButton() {
        return testGeminiButton;
    }
} 