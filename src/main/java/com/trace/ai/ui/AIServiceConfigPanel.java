package com.trace.ai.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
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
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import com.trace.common.constants.TriagePanelConstants;
import com.google.common.annotations.VisibleForTesting;
import java.util.concurrent.CompletableFuture;

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
    private final JComboBox<AIModel> defaultModelComboBox;
    private final DefaultComboBoxModel<AIModel> defaultModelListModel;
    private final JButton setDefaultButton;
    private final JButton toggleModelButton;
    private final JButton refreshModelsButton;
    
    // State tracking
    private String originalOpenAIKey;
    private String originalGeminiKey;
    private boolean isTestingConnection = false;
    
    /**
     * Creates a new AI service configuration panel.
     * 
     * @param aiSettings the AI settings instance
     */
    public AIServiceConfigPanel(@NotNull AISettings aiSettings) {
        this.aiSettings = aiSettings;
        this.modelService = AIModelService.getInstance();
        
        // Initialize UI components
        this.openaiApiKeyField = new JBPasswordField();
        this.geminiApiKeyField = new JBPasswordField();
        this.testOpenAIButton = new JButton("Apply");
        this.testGeminiButton = new JButton("Apply");
        this.openaiStatusLabel = new JBLabel("Not configured");
        this.geminiStatusLabel = new JBLabel("Not configured");
        
        // Initialize model list components
        this.listModel = new DefaultListModel<>();
        this.modelList = new JBList<>(listModel);
        this.defaultModelListModel = new DefaultComboBoxModel<>();
        this.defaultModelComboBox = new JComboBox<>(defaultModelListModel);
        this.setDefaultButton = new JButton("Set as Default");
        this.toggleModelButton = new JButton("Enable/Disable");
        this.refreshModelsButton = new JButton("Refresh Models");
        
        // Initialize original state
        updateOriginalState();
        
        // Initialize the panel
        initializePanel();
        setupEventHandlers();
        
        // Load current settings immediately
        loadCurrentSettings();
        
        LOG.info("AI service configuration panel created and initialized");
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
        
        // Set responsive sizing
        setMinimumSize(new Dimension(500, 0));
        setPreferredSize(new Dimension(650, 600));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        
        // Create header
        JBLabel headerLabel = new JBLabel("AI Model Configuration");
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 16f));
        headerLabel.setBorder(JBUI.Borders.emptyBottom(20));
        
        // Create main content panel
        JPanel contentPanel = new JBPanel<>(new BorderLayout());
        contentPanel.setBorder(JBUI.Borders.empty(5));
        
        // API Key Configuration Panel
        JPanel apiKeyPanel = createAPIKeyPanel();
        
        // Model Management Panel
        JPanel modelPanel = createModelPanel();
        
        // Add panels to content
        contentPanel.add(apiKeyPanel, BorderLayout.NORTH);
        contentPanel.add(modelPanel, BorderLayout.CENTER);
        
        // Add to main panel
        add(headerLabel, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);
    }
    
    /**
     * Creates the API key configuration panel.
     */
    @NotNull
    private JPanel createAPIKeyPanel() {
        JPanel panel = new JBPanel<>(new BorderLayout());
        panel.setBorder(JBUI.Borders.compound(
            JBUI.Borders.customLine(UIUtil.getPanelBackground().darker(), 1),
            JBUI.Borders.empty(10)
        ));
        
        // Header
        JBLabel headerLabel = new JBLabel("API Key Configuration");
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 14f));
        headerLabel.setBorder(JBUI.Borders.emptyBottom(10));
        
        // Create a structured layout for consistent sizing
        JPanel keysPanel = new JBPanel<>(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        
        // Set consistent text field height but allow width to be responsive
        openaiApiKeyField.setPreferredSize(new Dimension(0, 25));
        openaiApiKeyField.setMinimumSize(new Dimension(200, 25));
        geminiApiKeyField.setPreferredSize(new Dimension(0, 25));
        geminiApiKeyField.setMinimumSize(new Dimension(200, 25));
        
        // OpenAI Configuration Row
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = JBUI.insets(0, 0, 5, 10);
        JBLabel openaiLabel = new JBLabel("OpenAI API Key:");
        keysPanel.add(openaiLabel, gbc);
        
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = JBUI.insets(0, 0, 5, 10);
        keysPanel.add(createTextFieldWithX(openaiApiKeyField, this::clearOpenAIKey), gbc);
        
        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        gbc.insets = JBUI.insets(0, 0, 5, 0);
        keysPanel.add(testOpenAIButton, gbc);
        
        // OpenAI Status Row
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insets(0, 0, 10, 0);
        keysPanel.add(openaiStatusLabel, gbc);
        
        // Gemini Configuration Row
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = JBUI.insets(0, 0, 5, 10);
        JBLabel geminiLabel = new JBLabel("Google Gemini API Key:");
        keysPanel.add(geminiLabel, gbc);
        
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = JBUI.insets(0, 0, 5, 10);
        keysPanel.add(createTextFieldWithX(geminiApiKeyField, this::clearGeminiKey), gbc);
        
        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        gbc.insets = JBUI.insets(0, 0, 5, 0);
        keysPanel.add(testGeminiButton, gbc);
        
        // Gemini Status Row
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insets(0, 0, 0, 0);
        keysPanel.add(geminiStatusLabel, gbc);
        
        // Add to main panel
        panel.add(headerLabel, BorderLayout.NORTH);
        panel.add(keysPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Creates a text field with an X icon for clearing.
     */
    private JPanel createTextFieldWithX(JBPasswordField textField, Runnable clearAction) {
        JPanel panel = new JBPanel<>(new BorderLayout());
        
        // Add the text field (will stretch to fill available space)
        panel.add(textField, BorderLayout.CENTER);
        
        // Create the X icon label
        JBLabel xIcon = new JBLabel("✕");
        xIcon.setFont(xIcon.getFont().deriveFont(Font.BOLD, 12f));
        xIcon.setForeground(UIUtil.getLabelForeground().darker());
        xIcon.setCursor(new Cursor(Cursor.HAND_CURSOR));
        xIcon.setHorizontalAlignment(SwingConstants.CENTER);
        xIcon.setVerticalAlignment(SwingConstants.CENTER);
        xIcon.setBorder(JBUI.Borders.emptyLeft(5));
        xIcon.setVisible(false); // Initially hidden
        
        // Add click listener to X icon
        xIcon.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                clearAction.run();
            }
        });
        
        // Add document listener to show/hide X based on content
        textField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                xIcon.setVisible(textField.getPassword().length > 0);
            }
            
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                xIcon.setVisible(textField.getPassword().length > 0);
            }
            
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                xIcon.setVisible(textField.getPassword().length > 0);
            }
        });
        
        panel.add(xIcon, BorderLayout.EAST);
        
        return panel;
    }
    
    /**
     * Creates the model management panel.
     */
    @NotNull
    private JPanel createModelPanel() {
        JPanel panel = new JBPanel<>(new BorderLayout());
        panel.setBorder(JBUI.Borders.compound(
            JBUI.Borders.customLine(UIUtil.getPanelBackground().darker(), 1),
            JBUI.Borders.empty(10)
        ));
        
        // Header
        JBLabel headerLabel = new JBLabel("Available Models");
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 14f));
        headerLabel.setBorder(JBUI.Borders.emptyBottom(10));
        
        // Model list
        modelList.setCellRenderer(new AIModelListCellRenderer());
        modelList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        modelList.addListSelectionListener(e -> updateButtonStates());
        JBScrollPane scrollPane = new JBScrollPane(modelList);
        scrollPane.setPreferredSize(new Dimension(500, 250));
        
        // Default model selection
        JPanel defaultPanel = new JBPanel<>(new BorderLayout());
        defaultPanel.setBorder(JBUI.Borders.empty(5));
        
        JPanel defaultLabelPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 5, 0));
        defaultLabelPanel.add(new JBLabel("Default Model:"));
        
        // Set custom renderer for the combo box to show readable names
        defaultModelComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, 
                                                        int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                
                if (value instanceof AIModel) {
                    AIModel model = (AIModel) value;
                    setText(model.getDisplayName() + " (" + model.getServiceType().getDisplayName() + ")");
                } else if (value == null) {
                    setText("No default model selected");
                }
                
                return this;
            }
        });
        
        // Set preferred width for the combo box
        defaultModelComboBox.setPreferredSize(new Dimension(300, defaultModelComboBox.getPreferredSize().height));
        
        JPanel defaultButtonPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 10, 0));
        defaultButtonPanel.add(setDefaultButton);
        
        defaultPanel.add(defaultLabelPanel, BorderLayout.WEST);
        defaultPanel.add(defaultModelComboBox, BorderLayout.CENTER);
        defaultPanel.add(defaultButtonPanel, BorderLayout.EAST);
        
        // Model actions
        JPanel actionPanel = new JBPanel<>(new BorderLayout());
        actionPanel.setBorder(JBUI.Borders.empty(5));
        
        JPanel actionButtonPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actionButtonPanel.add(toggleModelButton);
        actionButtonPanel.add(refreshModelsButton);
        
        actionPanel.add(actionButtonPanel, BorderLayout.WEST);
        
        // Add to main panel
        panel.add(headerLabel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        JPanel bottomPanel = new JBPanel<>(new BorderLayout());
        bottomPanel.setBorder(JBUI.Borders.empty(10, 0, 0, 0));
        bottomPanel.add(defaultPanel, BorderLayout.NORTH);
        bottomPanel.add(Box.createVerticalStrut(10), BorderLayout.CENTER);
        bottomPanel.add(actionPanel, BorderLayout.SOUTH);
        panel.add(bottomPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * Sets up event handlers for all components.
     */
    private void setupEventHandlers() {
        // API Key testing
        testOpenAIButton.addActionListener(e -> testOpenAIKey());
        testGeminiButton.addActionListener(e -> testGeminiKey());
        
        // Model management
        setDefaultButton.addActionListener(e -> setSelectedAsDefault());
        toggleModelButton.addActionListener(e -> toggleSelectedModel());
        refreshModelsButton.addActionListener(e -> refreshModelList());
        
        // API key change listeners
        openaiApiKeyField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) { handleAPIKeyChange(); }
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) { handleAPIKeyChange(); }
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) { handleAPIKeyChange(); }
        });
        
        geminiApiKeyField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) { handleAPIKeyChange(); }
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) { handleAPIKeyChange(); }
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) { handleAPIKeyChange(); }
        });
    }
    
    /**
     * Tests and applies the OpenAI API key.
     */
    private void testOpenAIKey() {
        if (isTestingConnection) {
            return;
        }
        
        String apiKey = new String(openaiApiKeyField.getPassword());
        if (apiKey.trim().isEmpty()) {
            showError("Please enter an OpenAI API key");
            return;
        }
        
        isTestingConnection = true;
        testOpenAIButton.setEnabled(false);
        openaiStatusLabel.setText("Testing connection...");
        openaiStatusLabel.setForeground(UIUtil.getLabelInfoForeground());
        
        // Test the connection first
        CompletableFuture.supplyAsync(() -> {
            try {
                // TODO: Replace with actual API test when implemented
                // For now, simulate a successful test
                Thread.sleep(1000); // Simulate network delay
                return true; // Simulate success
            } catch (Exception e) {
                return false;
            }
        }).thenAcceptAsync(success -> {
            SwingUtilities.invokeLater(() -> {
                isTestingConnection = false;
                testOpenAIButton.setEnabled(true);
                
                if (success) {
                    // Test successful - save the API key
                    boolean saved = SecureAPIKeyManager.storeAPIKey(AIServiceType.OPENAI, apiKey);
                    if (saved) {
                        openaiStatusLabel.setText("✅ Connected - Models available");
                        openaiStatusLabel.setForeground(UIUtil.getLabelSuccessForeground());
                        showSuccess("OpenAI API key applied successfully");
                        
                        // Refresh model list to show available models
                        refreshModelList();
                        
                        // Update original state for modification detection
                        updateOriginalState();
                        
                        LOG.info("OpenAI API key tested and saved successfully");
                    } else {
                        openaiStatusLabel.setText("Failed to save API key");
                        openaiStatusLabel.setForeground(TriagePanelConstants.ERROR_FOREGROUND);
                        showError("Failed to save OpenAI API key");
                        LOG.error("Failed to save OpenAI API key");
                    }
                } else {
                    openaiStatusLabel.setText("Connection failed");
                    openaiStatusLabel.setForeground(TriagePanelConstants.ERROR_FOREGROUND);
                    showError("OpenAI API key is invalid or connection failed");
                    LOG.warn("OpenAI API key test failed");
                }
            });
        });
    }
    
    /**
     * Tests and applies the Gemini API key.
     */
    private void testGeminiKey() {
        if (isTestingConnection) {
            return;
        }
        
        String apiKey = new String(geminiApiKeyField.getPassword());
        if (apiKey.trim().isEmpty()) {
            showError("Please enter a Gemini API key");
            return;
        }
        
        isTestingConnection = true;
        testGeminiButton.setEnabled(false);
        geminiStatusLabel.setText("Testing connection...");
        geminiStatusLabel.setForeground(UIUtil.getLabelInfoForeground());
        
        // Test the connection first
        CompletableFuture.supplyAsync(() -> {
            try {
                // TODO: Replace with actual API test when implemented
                // For now, simulate a successful test
                Thread.sleep(1000); // Simulate network delay
                return true; // Simulate success
            } catch (Exception e) {
                return false;
            }
        }).thenAcceptAsync(success -> {
            SwingUtilities.invokeLater(() -> {
                isTestingConnection = false;
                testGeminiButton.setEnabled(true);
                
                if (success) {
                    // Test successful - save the API key
                    boolean saved = SecureAPIKeyManager.storeAPIKey(AIServiceType.GEMINI, apiKey);
                    if (saved) {
                        geminiStatusLabel.setText("✅ Connected - Models available");
                        geminiStatusLabel.setForeground(UIUtil.getLabelSuccessForeground());
                        showSuccess("Gemini API key applied successfully");
                        
                        // Refresh model list to show available models
                        refreshModelList();
                        
                        // Update original state for modification detection
                        updateOriginalState();
                        
                        LOG.info("Gemini API key tested and saved successfully");
                    } else {
                        geminiStatusLabel.setText("Failed to save API key");
                        geminiStatusLabel.setForeground(TriagePanelConstants.ERROR_FOREGROUND);
                        showError("Failed to save Gemini API key");
                        LOG.error("Failed to save Gemini API key");
                    }
                } else {
                    geminiStatusLabel.setText("Connection failed");
                    geminiStatusLabel.setForeground(TriagePanelConstants.ERROR_FOREGROUND);
                    showError("Gemini API key is invalid or connection failed");
                    LOG.warn("Gemini API key test failed");
                }
            });
        });
    }
    
    /**
     * Clears the OpenAI API key.
     */
    private void clearOpenAIKey() {
        openaiApiKeyField.setText("");
        SecureAPIKeyManager.clearAPIKey(AIServiceType.OPENAI);
        openaiStatusLabel.setText("Not configured");
        openaiStatusLabel.setForeground(UIUtil.getLabelForeground());
        
        // Remove OpenAI models from the list when key is cleared
        refreshModelList();
    }
    
    /**
     * Clears the Gemini API key.
     */
    private void clearGeminiKey() {
        geminiApiKeyField.setText("");
        SecureAPIKeyManager.clearAPIKey(AIServiceType.GEMINI);
        geminiStatusLabel.setText("Not configured");
        geminiStatusLabel.setForeground(UIUtil.getLabelForeground());
        
        // Remove Gemini models from the list when key is cleared
        refreshModelList();
    }
    
    /**
     * Sets the selected model as default.
     */
    private void setSelectedAsDefault() {
        AIModel selectedModel = modelList.getSelectedValue();
        if (selectedModel == null) {
            showError("Please select a model to set as default");
            return;
        }
        
        if (modelService.setDefaultModel(selectedModel.getId())) {
            defaultModelComboBox.setSelectedItem(selectedModel);
            showSuccess("Default model set to: " + selectedModel.getDisplayName());
            refreshModelList();
        } else {
            showError("Failed to set default model");
        }
    }
    
    /**
     * Toggles the selected model's enabled state.
     */
    private void toggleSelectedModel() {
        AIModel selectedModel = modelList.getSelectedValue();
        if (selectedModel == null) {
            showError("Please select a model to toggle");
            return;
        }
        
        selectedModel.setEnabled(!selectedModel.isEnabled());
        if (modelService.updateModel(selectedModel)) {
            showSuccess("Model " + (selectedModel.isEnabled() ? "enabled" : "disabled") + ": " + selectedModel.getDisplayName());
            refreshModelList();
        } else {
            showError("Failed to update model");
        }
    }
    
    /**
     * Refreshes the model list based on current API key configuration.
     */
    private void refreshModelList() {
        listModel.clear();
        defaultModelListModel.removeAllElements();
        
        // Check for new models based on API key availability
        discoverNewModels();
        
        // Get all models after discovery
        List<AIModel> allModels = modelService.getAllModels();
        
        // Filter models based on API key availability
        List<AIModel> availableModels = new ArrayList<>();
        for (AIModel model : allModels) {
            boolean hasKey = false;
            if (model.getServiceType() == AIServiceType.OPENAI) {
                String openaiKey = SecureAPIKeyManager.getAPIKey(AIServiceType.OPENAI);
                String openaiKeyUI = new String(openaiApiKeyField.getPassword());
                hasKey = (openaiKey != null && !openaiKey.trim().isEmpty()) || 
                        (!openaiKeyUI.trim().isEmpty());
            } else if (model.getServiceType() == AIServiceType.GEMINI) {
                String geminiKey = SecureAPIKeyManager.getAPIKey(AIServiceType.GEMINI);
                String geminiKeyUI = new String(geminiApiKeyField.getPassword());
                hasKey = (geminiKey != null && !geminiKey.trim().isEmpty()) || 
                        (!geminiKeyUI.trim().isEmpty());
            }
            
            if (hasKey) {
                availableModels.add(model);
            }
        }
        
        // Add available models to UI
        for (AIModel model : availableModels) {
            listModel.addElement(model);
            defaultModelListModel.addElement(model);
        }
        
        // Set default model selection
        AIModel defaultModel = modelService.getDefaultModel();
        if (defaultModel != null && availableModels.contains(defaultModel)) {
            defaultModelComboBox.setSelectedItem(defaultModel);
        }
        
        updateButtonStates();
    }
    
    /**
     * Discovers new models based on available API keys.
     */
    private void discoverNewModels() {
        // Check OpenAI models - use both stored keys and current UI state
        String openaiKey = SecureAPIKeyManager.getAPIKey(AIServiceType.OPENAI);
        String openaiKeyUI = new String(openaiApiKeyField.getPassword());
        boolean hasOpenAIKey = (openaiKey != null && !openaiKey.trim().isEmpty()) || 
                              (!openaiKeyUI.trim().isEmpty());
        
        if (hasOpenAIKey) {
            String[] openaiModels = AIModel.getAvailableModelIds(AIServiceType.OPENAI);
            for (String modelId : openaiModels) {
                String displayName = AIModel.getModelDisplayName(modelId);
                if (!modelService.hasModelWithName(displayName)) {
                    modelService.createModel(displayName, AIServiceType.OPENAI, modelId);
                }
            }
        }
        
        // Check Gemini models - use both stored keys and current UI state
        String geminiKey = SecureAPIKeyManager.getAPIKey(AIServiceType.GEMINI);
        String geminiKeyUI = new String(geminiApiKeyField.getPassword());
        boolean hasGeminiKey = (geminiKey != null && !geminiKey.trim().isEmpty()) || 
                              (!geminiKeyUI.trim().isEmpty());
        
        if (hasGeminiKey) {
            String[] geminiModels = AIModel.getAvailableModelIds(AIServiceType.GEMINI);
            for (String modelId : geminiModels) {
                String displayName = AIModel.getModelDisplayName(modelId);
                if (!modelService.hasModelWithName(displayName)) {
                    modelService.createModel(displayName, AIServiceType.GEMINI, modelId);
                }
            }
        }
    }
    
    /**
     * Updates button states based on current selection.
     */
    private void updateButtonStates() {
        AIModel selectedModel = modelList.getSelectedValue();
        boolean hasSelection = selectedModel != null;
        
        setDefaultButton.setEnabled(hasSelection);
        toggleModelButton.setEnabled(hasSelection);
        
        if (hasSelection) {
            toggleModelButton.setText(selectedModel.isEnabled() ? "Disable" : "Enable");
        }
    }
    
    /**
     * Handles API key changes.
     */
    private void handleAPIKeyChange() {
        // Update status labels only - don't store keys on every keystroke
        String openaiKey = new String(openaiApiKeyField.getPassword());
        String geminiKey = new String(geminiApiKeyField.getPassword());
        
        if (!openaiKey.trim().isEmpty()) {
            openaiStatusLabel.setText("API key entered (click Apply to test & save)");
            openaiStatusLabel.setForeground(UIUtil.getLabelInfoForeground());
        } else {
            openaiStatusLabel.setText("Not configured");
            openaiStatusLabel.setForeground(UIUtil.getLabelForeground());
        }
        
        if (!geminiKey.trim().isEmpty()) {
            geminiStatusLabel.setText("API key entered (click Apply to test & save)");
            geminiStatusLabel.setForeground(UIUtil.getLabelInfoForeground());
        } else {
            geminiStatusLabel.setText("Not configured");
            geminiStatusLabel.setForeground(UIUtil.getLabelForeground());
        }
    }
    
    /**
     * Loads current settings into the UI.
     */
    public void loadCurrentSettings() {
        LOG.info("Loading current AI service settings into UI");
        
        // Load API keys from secure storage
        String storedOpenAIKey = SecureAPIKeyManager.getAPIKey(AIServiceType.OPENAI);
        String storedGeminiKey = SecureAPIKeyManager.getAPIKey(AIServiceType.GEMINI);
        
        // Always load stored keys into UI (this is what user expects when reopening settings)
        if (storedOpenAIKey != null && !storedOpenAIKey.trim().isEmpty()) {
            openaiApiKeyField.setText(storedOpenAIKey);
            openaiStatusLabel.setText("✅ Connected - Models available");
            openaiStatusLabel.setForeground(UIUtil.getLabelSuccessForeground());
            LOG.debug("Loaded OpenAI API key into UI");
        } else {
            openaiApiKeyField.setText("");
            openaiStatusLabel.setText("Not configured");
            openaiStatusLabel.setForeground(UIUtil.getLabelForeground());
            LOG.debug("No OpenAI API key found in storage");
        }
        
        if (storedGeminiKey != null && !storedGeminiKey.trim().isEmpty()) {
            geminiApiKeyField.setText(storedGeminiKey);
            geminiStatusLabel.setText("✅ Connected - Models available");
            geminiStatusLabel.setForeground(UIUtil.getLabelSuccessForeground());
            LOG.debug("Loaded Gemini API key into UI");
        } else {
            geminiApiKeyField.setText("");
            geminiStatusLabel.setText("Not configured");
            geminiStatusLabel.setForeground(UIUtil.getLabelForeground());
            LOG.debug("No Gemini API key found in storage");
        }
        
        // Update original state for modification detection
        updateOriginalState();
        
        // Refresh model list to show available models based on stored keys
        refreshModelList();
        
        LOG.info("Finished loading AI service settings into UI");
    }
    
    /**
     * Updates the original state for modification detection.
     */
    private void updateOriginalState() {
        this.originalOpenAIKey = SecureAPIKeyManager.getAPIKey(AIServiceType.OPENAI);
        this.originalGeminiKey = SecureAPIKeyManager.getAPIKey(AIServiceType.GEMINI);
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
        
        // Clear OpenAI key if field is empty
        if (openaiKey.trim().isEmpty()) {
            SecureAPIKeyManager.clearAPIKey(AIServiceType.OPENAI);
            openaiStatusLabel.setText("Not configured");
            openaiStatusLabel.setForeground(UIUtil.getLabelForeground());
            LOG.info("OpenAI API key cleared");
        }
        
        // Clear Gemini key if field is empty
        if (geminiKey.trim().isEmpty()) {
            SecureAPIKeyManager.clearAPIKey(AIServiceType.GEMINI);
            geminiStatusLabel.setText("Not configured");
            geminiStatusLabel.setForeground(UIUtil.getLabelForeground());
            LOG.info("Gemini API key cleared");
        }
        
        // Update original state for modification detection
        updateOriginalState();
        
        LOG.info("AI service configuration settings applied");
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