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
import com.trace.ai.services.providers.GeminiProvider;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import com.trace.common.constants.TriagePanelConstants;
import com.google.common.annotations.VisibleForTesting;
import java.util.concurrent.CompletableFuture;
import com.intellij.openapi.application.ApplicationManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import com.trace.ai.services.providers.OpenAIProvider;

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
    private final JBLabel defaultModelLabel;
    private final JButton setDefaultButton;
    private final JButton refreshModelsButton;
    
    // State tracking
    private String originalOpenAIKey;
    private String originalGeminiKey;
    private boolean isTestingConnection = false;
    
    // Independent status tracking for each service
    private String openaiStatus = "Not configured";
    private String geminiStatus = "Not configured";
    
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
        this.defaultModelLabel = new JBLabel("No default model selected");
        this.setDefaultButton = new JButton("Set as Default");
        this.refreshModelsButton = new JButton("Refresh Models");
        
        // Initialize the panel
        initializePanel();
        setupEventHandlers();
        
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
        
        // Main content panel using BorderLayout for proper alignment
        JPanel contentPanel = new JBPanel<>(new BorderLayout());
        contentPanel.setBorder(JBUI.Borders.empty(0, 0, 0, 0));
        
        // Default model section - moved to NORTH position for predictable alignment
        JPanel defaultSection = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 0, 0));
        defaultSection.setBorder(JBUI.Borders.empty(0, 0, 8, 0)); // bottom margin only
        
        // Label and value properly aligned using FlowLayout
        JBLabel defaultLabel = new JBLabel("Default Model:");
        defaultLabel.setBorder(JBUI.Borders.empty(0, 0, 0, 8)); // Right margin for spacing
        
        // Style the default model label
        defaultModelLabel.setPreferredSize(new Dimension(300, 20));
        
        defaultSection.add(defaultLabel);
        defaultSection.add(defaultModelLabel);
        
        // Model list (main control)
        modelList.setCellRenderer(new AIModelListCellRenderer());
        modelList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        modelList.addListSelectionListener(e -> updateButtonStates());
        JBScrollPane scrollPane = new JBScrollPane(modelList);
        scrollPane.setPreferredSize(new Dimension(500, 250));
        
        // Action buttons panel - aligned with left edge of list content
        JPanel actionButtonPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 0, 0));
        actionButtonPanel.setBorder(JBUI.Borders.empty(8, 0, 0, 0)); // top margin only
        
        setDefaultButton.setMargin(new Insets(0, 0, 0, 0));
        refreshModelsButton.setMargin(new Insets(0, 0, 0, 0));
        
        actionButtonPanel.add(setDefaultButton);
        actionButtonPanel.add(refreshModelsButton);
        
        // Simple layout following JetBrains best practices
        // Default model info at top, main control in center, action buttons at bottom
        contentPanel.add(defaultSection, BorderLayout.NORTH);
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        contentPanel.add(actionButtonPanel, BorderLayout.SOUTH);
        
        // Add sections to main panel
        panel.add(headerLabel, BorderLayout.NORTH);
        panel.add(contentPanel, BorderLayout.CENTER);
        
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
        refreshModelsButton.addActionListener(e -> refreshModelList());
        
        // API key change listeners
        openaiApiKeyField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) { handleOpenAIKeyChange(); }
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) { handleOpenAIKeyChange(); }
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) { handleOpenAIKeyChange(); }
        });
        
        geminiApiKeyField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) { handleGeminiKeyChange(); }
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) { handleGeminiKeyChange(); }
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) { handleGeminiKeyChange(); }
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
        
        // Validate API key format
        if (!isValidOpenAIKeyFormat(apiKey)) {
            showError("Invalid OpenAI API key format. Should be a valid OpenAI API key.");
            return;
        }
        
        isTestingConnection = true;
        testOpenAIButton.setEnabled(false);
        openaiStatusLabel.setText("Testing connection...");
        openaiStatusLabel.setForeground(UIUtil.getLabelInfoForeground());
        
        // Test basic connectivity without consuming quota
        CompletableFuture.supplyAsync(() -> {
            try {
                return testOpenAIConnectivity(apiKey);
            } catch (Exception e) {
                LOG.warn("OpenAI connectivity test failed: " + e.getMessage());
                return false;
            }
        }).thenAcceptAsync(success -> {
            ApplicationManager.getApplication().invokeLater(() -> {
                isTestingConnection = false;
                testOpenAIButton.setEnabled(true);
                
                if (success) {
                    // Test successful - save the API key in background thread
                    ApplicationManager.getApplication().executeOnPooledThread(() -> {
                        boolean saved = SecureAPIKeyManager.storeAPIKey(AIServiceType.OPENAI, apiKey);
                        ApplicationManager.getApplication().invokeLater(() -> {
                            if (saved) {
                                openaiStatusLabel.setText("✅ Connected");
                                openaiStatusLabel.setForeground(UIUtil.getLabelSuccessForeground());
                                showSuccess("OpenAI API key applied successfully");
                                
                                // Discover and show new models after successful validation
                                discoverNewModels();
                                
                                // Update original state for modification detection
                                updateOriginalState();
                                
                                LOG.info("OpenAI API key tested and saved successfully");
                            } else {
                                openaiStatusLabel.setText("Failed to save API key");
                                openaiStatusLabel.setForeground(TriagePanelConstants.ERROR_FOREGROUND);
                                showError("Failed to save OpenAI API key");
                                LOG.error("Failed to save OpenAI API key");
                            }
                        });
                    });
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
        
        // Validate API key format
        if (!isValidGeminiKeyFormat(apiKey)) {
            showError("Invalid Gemini API key format. Should be a valid Google API key.");
            return;
        }
        
        isTestingConnection = true;
        testGeminiButton.setEnabled(false);
        geminiStatusLabel.setText("Testing connection...");
        geminiStatusLabel.setForeground(UIUtil.getLabelInfoForeground());
        
        // Test basic connectivity without consuming quota
        CompletableFuture.supplyAsync(() -> {
            try {
                return testGeminiConnectivity(apiKey);
            } catch (Exception e) {
                LOG.warn("Gemini connectivity test failed: " + e.getMessage());
                return false;
            }
        }).thenAcceptAsync(success -> {
            ApplicationManager.getApplication().invokeLater(() -> {
                isTestingConnection = false;
                testGeminiButton.setEnabled(true);
                
                if (success) {
                    // Test successful - save the API key in background thread
                    ApplicationManager.getApplication().executeOnPooledThread(() -> {
                        boolean saved = SecureAPIKeyManager.storeAPIKey(AIServiceType.GEMINI, apiKey);
                        ApplicationManager.getApplication().invokeLater(() -> {
                            if (saved) {
                                geminiStatusLabel.setText("✅ Connected");
                                geminiStatusLabel.setForeground(UIUtil.getLabelSuccessForeground());
                                showSuccess("Gemini API key applied successfully");
                                
                                // Discover and show new models after successful validation
                                discoverNewModels();
                                
                                // Update original state for modification detection
                                updateOriginalState();
                                
                                LOG.info("Gemini API key tested and saved successfully");
                            } else {
                                geminiStatusLabel.setText("Failed to save API key");
                                geminiStatusLabel.setForeground(TriagePanelConstants.ERROR_FOREGROUND);
                                showError("Failed to save Gemini API key");
                                LOG.error("Failed to save Gemini API key");
                            }
                        });
                    });
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
     * Validates OpenAI API key format.
     */
    private boolean isValidOpenAIKeyFormat(String apiKey) {
        return apiKey != null && apiKey.startsWith("sk-") && apiKey.length() >= 50;
    }
    
    /**
     * Validates Gemini API key format.
     */
    private boolean isValidGeminiKeyFormat(String apiKey) {
        return apiKey != null && apiKey.length() >= 20 && apiKey.matches("^[A-Za-z0-9_-]+$");
    }
    
    /**
     * Tests OpenAI connectivity without consuming quota.
     */
    private boolean testOpenAIConnectivity(String apiKey) {
        try {
            LOG.info("Testing OpenAI connectivity with API key: " + apiKey.substring(0, 8) + "...");
            
            // Use HEAD request to test connectivity without consuming quota
            var client = HttpClient.newHttpClient();
            var request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/models"))
                .header("Authorization", "Bearer " + apiKey)
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .timeout(Duration.ofSeconds(10))
                .build();
            
            var response = client.send(request, HttpResponse.BodyHandlers.discarding());
            
            LOG.info("OpenAI connectivity test response: " + response.statusCode());
            
            // Only accept 200 (OK) as valid response
            if (response.statusCode() == 200) {
                LOG.info("OpenAI connectivity test successful - API key is valid");
                return true;
            } else {
                LOG.warn("OpenAI connectivity test failed - Status code: " + response.statusCode());
                return false;
            }
        } catch (Exception e) {
            LOG.warn("OpenAI connectivity test failed with exception: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Tests Gemini connectivity without consuming quota.
     */
    private boolean testGeminiConnectivity(String apiKey) {
        try {
            LOG.info("Testing Gemini connectivity with API key: " + apiKey.substring(0, 8) + "...");
            
            // Use GET request to test connectivity without consuming quota
            var client = HttpClient.newHttpClient();
            var request = HttpRequest.newBuilder()
                .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models?key=" + apiKey))
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();
            
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            LOG.info("Gemini connectivity test response: " + response.statusCode());
            
            // Only accept 200 (OK) as valid response
            if (response.statusCode() == 200) {
                LOG.info("Gemini connectivity test successful - API key is valid");
                return true;
            } else {
                LOG.warn("Gemini connectivity test failed - Status code: " + response.statusCode() + ", Response: " + response.body());
                return false;
            }
        } catch (Exception e) {
            LOG.warn("Gemini connectivity test failed with exception: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Clears the OpenAI API key.
     */
    private void clearOpenAIKey() {
        // Clear UI immediately for responsive feedback
        openaiApiKeyField.setText("");
        openaiStatusLabel.setText("Clearing...");
        openaiStatusLabel.setForeground(UIUtil.getLabelInfoForeground());
        
        // Perform slow operation on background thread
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            boolean cleared = SecureAPIKeyManager.clearAPIKey(AIServiceType.OPENAI);
            
            // Update UI on EDT after operation completes
            ApplicationManager.getApplication().invokeLater(() -> {
                if (cleared) {
                    openaiStatusLabel.setText("Not configured");
                    openaiStatusLabel.setForeground(UIUtil.getLabelForeground());
                    LOG.info("OpenAI API key cleared successfully");
                } else {
                    openaiStatusLabel.setText("Failed to clear");
                    openaiStatusLabel.setForeground(TriagePanelConstants.ERROR_FOREGROUND);
                    LOG.error("Failed to clear OpenAI API key");
                }
                
                // Remove OpenAI models from the list when key is cleared
                refreshModelList();
            });
        });
    }
    
    /**
     * Clears the Gemini API key.
     */
    private void clearGeminiKey() {
        // Clear UI immediately for responsive feedback
        geminiApiKeyField.setText("");
        geminiStatusLabel.setText("Clearing...");
        geminiStatusLabel.setForeground(UIUtil.getLabelInfoForeground());
        
        // Perform slow operation on background thread
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            boolean cleared = SecureAPIKeyManager.clearAPIKey(AIServiceType.GEMINI);
            
            // Update UI on EDT after operation completes
            ApplicationManager.getApplication().invokeLater(() -> {
                if (cleared) {
                    geminiStatusLabel.setText("Not configured");
                    geminiStatusLabel.setForeground(UIUtil.getLabelForeground());
                    LOG.info("Gemini API key cleared successfully");
                } else {
                    geminiStatusLabel.setText("Failed to clear");
                    geminiStatusLabel.setForeground(TriagePanelConstants.ERROR_FOREGROUND);
                    LOG.error("Failed to clear Gemini API key");
                }
                
                // Remove Gemini models from the list when key is cleared
                refreshModelList();
            });
        });
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
        
        // Check if this model is already the default
        AIModel currentDefault = modelService.getDefaultModel();
        if (currentDefault != null && currentDefault.getId().equals(selectedModel.getId())) {
            showSuccess("Model is already set as default: " + selectedModel.getDisplayName());
            return;
        }
        
        if (modelService.setDefaultModel(selectedModel.getId())) {
            updateDefaultModelDisplay();
            showSuccess("Default model set to: " + selectedModel.getDisplayName());
            updateButtonStates();
        } else {
            showError("Failed to set default model");
        }
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
                    openaiStatusLabel.setText("✅ Connected");
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
                    geminiStatusLabel.setText("✅ Connected");
                    geminiStatusLabel.setForeground(UIUtil.getLabelSuccessForeground());
                    LOG.debug("Loaded Gemini API key into UI");
                } else {
                    geminiApiKeyField.setText("");
                    geminiStatusLabel.setText("Not configured");
                    geminiStatusLabel.setForeground(UIUtil.getLabelForeground());
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
        LOG.debug("Refreshing model list - OpenAI: " + (storedOpenAIKey != null && !storedOpenAIKey.trim().isEmpty()) + 
                 ", Gemini: " + (storedGeminiKey != null && !storedGeminiKey.trim().isEmpty()));
        
        listModel.clear();
        
        // Get all models from storage
        List<AIModel> allModels = modelService.getAllModels();
        
        // Only show models for services with valid stored API keys
        List<AIModel> availableModels = new ArrayList<>();
        for (AIModel model : allModels) {
            boolean hasValidKey = false;
            if (model.getServiceType() == AIServiceType.OPENAI) {
                hasValidKey = (storedOpenAIKey != null && !storedOpenAIKey.trim().isEmpty());
            } else if (model.getServiceType() == AIServiceType.GEMINI) {
                hasValidKey = (storedGeminiKey != null && !storedGeminiKey.trim().isEmpty());
            }
            
            if (hasValidKey) {
                availableModels.add(model);
            }
        }
        
        // Add available models to UI
        for (AIModel model : availableModels) {
            listModel.addElement(model);
        }
        
        updateDefaultModelDisplay();
        updateButtonStates();
        
        LOG.debug("Model list refreshed with " + availableModels.size() + " models");
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
            
            discoverNewModels(storedOpenAIKey, storedGeminiKey);
        });
    }
    
    /**
     * Discovers and adds new models for configured services with provided API keys.
     * Only called after successful API key validation.
     */
    private void discoverNewModels(String storedOpenAIKey, String storedGeminiKey) {
        LOG.info("Discovering new models for configured services");
        
        // Create default models for services with valid stored keys
        if (storedOpenAIKey != null && !storedOpenAIKey.trim().isEmpty()) {
            createDefaultModelsIfNeeded(AIServiceType.OPENAI);
        }
        
        if (storedGeminiKey != null && !storedGeminiKey.trim().isEmpty()) {
            discoverGeminiModels(storedGeminiKey);
        }
        
        // Refresh the model list to show the newly created models
        ApplicationManager.getApplication().invokeLater(() -> {
            refreshModelList(storedOpenAIKey, storedGeminiKey);
        });
    }
    
    /**
     * Creates default models for a service if they don't already exist.
     */
    private void createDefaultModelsIfNeeded(AIServiceType serviceType) {
        String[] defaultModels;
        switch (serviceType) {
            case OPENAI:
                // Use the same stable models as the filter
                defaultModels = new String[]{"gpt-4o", "gpt-4o-mini", "gpt-3.5-turbo"};
                break;
            case GEMINI:
                defaultModels = new String[]{"gemini-1.5-flash", "gemini-1.5-pro"};
                break;
            default:
                return;
        }
        
        for (String modelId : defaultModels) {
            String displayName = getDefaultDisplayName(serviceType, modelId);
            if (!modelService.hasModelWithName(displayName)) {
                modelService.addDiscoveredModel(displayName, serviceType, modelId);
                LOG.info("Added default model: " + displayName);
            }
        }
    }
    
    /**
     * Discovers Gemini models dynamically using the API and filters to stable models only.
     */
    private void discoverGeminiModels(String apiKey) {
        LOG.info("Discovering Gemini models dynamically");
        
        GeminiProvider provider = new GeminiProvider(HttpClient.newHttpClient());
        provider.discoverAvailableModels(apiKey)
            .thenAccept(allModels -> {
                ApplicationManager.getApplication().invokeLater(() -> {
                    // Filter to only stable, production-ready models
                    String[] stableModels = filterToStableGeminiModels(allModels);
                    
                    for (String modelId : stableModels) {
                        String displayName = getDefaultDisplayName(AIServiceType.GEMINI, modelId);
                        if (!modelService.hasModelWithName(displayName)) {
                            modelService.addDiscoveredModel(displayName, AIServiceType.GEMINI, modelId);
                            LOG.info("Added stable Gemini model: " + displayName);
                        }
                    }
                    refreshModelList();
                });
            })
            .exceptionally(throwable -> {
                LOG.warn("Failed to discover Gemini models, using defaults: " + throwable.getMessage());
                // Fallback to hardcoded models
                createDefaultModelsIfNeeded(AIServiceType.GEMINI);
                return null;
            });
    }
    
    /**
     * Filters discovered Gemini models to only include stable, production-ready models.
     * This ensures users see only the most reliable and useful models.
     */
    private String[] filterToStableGeminiModels(String[] allModels) {
        // Define stable, production-ready models in order of preference
        String[] stableModelIds = {
            "gemini-1.5-flash",      // Fast, cost-effective, general purpose
            "gemini-1.5-pro",        // High quality, general purpose
            "gemini-2.0-flash",      // Latest fast model (if available)
            "gemini-2.0-pro"         // Latest high-quality model (if available)
        };
        
        List<String> filteredModels = new ArrayList<>();
        
        // Add stable models that are available
        for (String stableId : stableModelIds) {
            for (String discoveredId : allModels) {
                if (discoveredId.equals(stableId)) {
                    filteredModels.add(discoveredId);
                    break;
                }
            }
        }
        
        // If no stable models found, fall back to basic models
        if (filteredModels.isEmpty()) {
            LOG.warn("No stable Gemini models found, using basic fallback");
            return new String[]{"gemini-1.5-flash", "gemini-1.5-pro"};
        }
        
        LOG.info("Filtered to " + filteredModels.size() + " stable Gemini models: " + 
                String.join(", ", filteredModels));
        
        return filteredModels.toArray(new String[0]);
    }
    
    /**
     * Gets the default display name for a model.
     */
    private String getDefaultDisplayName(AIServiceType serviceType, String modelId) {
        switch (serviceType) {
            case OPENAI:
                switch (modelId) {
                    case "gpt-3.5-turbo": return "GPT-3.5 Turbo";
                    case "gpt-3.5-turbo-16k": return "GPT-3.5 Turbo (16K)";
                    case "gpt-4": return "GPT-4";
                    case "gpt-4-turbo": return "GPT-4 Turbo";
                    case "gpt-4o": return "GPT-4o";
                    case "gpt-4o-mini": return "GPT-4o Mini";
                    default: return modelId;
                }
            case GEMINI:
                switch (modelId) {
                    case "gemini-1.5-flash": return "Gemini 1.5 Flash";
                    case "gemini-1.5-pro": return "Gemini 1.5 Pro";
                    case "gemini-2.0-flash": return "Gemini 2.0 Flash";
                    case "gemini-2.0-pro": return "Gemini 2.0 Pro";
                    default: return modelId; // Fallback for unknown models
                }
            default:
                return modelId;
        }
    }
    
    /**
     * Updates button states based on current selection.
     */
    private void updateButtonStates() {
        AIModel selectedModel = modelList.getSelectedValue();
        boolean hasSelection = selectedModel != null;
        
        setDefaultButton.setEnabled(hasSelection);
    }
    
    /**
     * Updates the default model display label.
     */
    private void updateDefaultModelDisplay() {
        AIModel defaultModel = modelService.getDefaultModel();
        if (defaultModel != null) {
            defaultModelLabel.setText(defaultModel.getDisplayName() + " (" + defaultModel.getServiceType().getDisplayName() + ")");
        } else {
            defaultModelLabel.setText("No default model selected");
        }
    }
    

    
    /**
     * Handles OpenAI API key changes.
     */
    private void handleOpenAIKeyChange() {
        String openaiKey = new String(openaiApiKeyField.getPassword());
        if (!openaiKey.trim().isEmpty()) {
            openaiStatusLabel.setText("API key entered (click Apply to test & save)");
            openaiStatusLabel.setForeground(UIUtil.getLabelInfoForeground());
        } else {
            openaiStatusLabel.setText("Not configured");
            openaiStatusLabel.setForeground(UIUtil.getLabelForeground());
        }
    }
    
    /**
     * Handles Gemini API key changes.
     */
    private void handleGeminiKeyChange() {
        String geminiKey = new String(geminiApiKeyField.getPassword());
        if (!geminiKey.trim().isEmpty()) {
            geminiStatusLabel.setText("API key entered (click Apply to test & save)");
            geminiStatusLabel.setForeground(UIUtil.getLabelInfoForeground());
        } else {
            geminiStatusLabel.setText("Not configured");
            geminiStatusLabel.setForeground(UIUtil.getLabelForeground());
        }
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

    /**
     * Refreshes the model list by fetching new models from API providers.
     * This method will discover available models from configured services and update the UI.
     */
    private void refreshModelList() {
        LOG.info("Refreshing model list - replacing with current available models from APIs");
        
        // Disable refresh button and show loading state
        refreshModelsButton.setEnabled(false);
        refreshModelsButton.setText("Refreshing...");
        
        // Get stored API keys in background
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            String storedOpenAIKey = SecureAPIKeyManager.getAPIKey(AIServiceType.OPENAI);
            String storedGeminiKey = SecureAPIKeyManager.getAPIKey(AIServiceType.GEMINI);
            
            // Replace entire model list with current available models
            replaceModelListWithCurrentModels(storedOpenAIKey, storedGeminiKey);
        });
    }
    
    /**
     * Replaces the entire model list with currently available models from APIs.
     * This ensures the list reflects the current state of the user's API access.
     * 
     * @param storedOpenAIKey the stored OpenAI API key
     * @param storedGeminiKey the stored Gemini API key
     */
    private void replaceModelListWithCurrentModels(String storedOpenAIKey, String storedGeminiKey) {
        LOG.info("Replacing model list with current available models from APIs");
        
        // Clear existing models first
        modelService.deleteAllModels();
        
        // Discover current available models
        final boolean openaiDiscovered = discoverOpenAIModelsIfConfigured(storedOpenAIKey);
        final boolean geminiDiscovered = discoverGeminiModelsIfConfigured(storedGeminiKey);
        
        // Update UI on EDT after discovery completes
        ApplicationManager.getApplication().invokeLater(() -> {
            // Re-enable refresh button
            refreshModelsButton.setEnabled(true);
            refreshModelsButton.setText("Refresh Models");
            
            // Show results to user
            if (openaiDiscovered || geminiDiscovered) {
                showSuccess("Model list refreshed successfully");
            } else if (storedOpenAIKey == null && storedGeminiKey == null) {
                showError("No API keys configured. Please configure API keys first.");
            } else {
                showError("Failed to refresh models. Please check your API keys and try again.");
            }
            
            // Update the UI to show the new model list
            refreshModelList(storedOpenAIKey, storedGeminiKey);
        });
    }
    
    /**
     * Discovers OpenAI models if API key is configured.
     * 
     * @param storedOpenAIKey the stored OpenAI API key
     * @return true if models were discovered successfully, false otherwise
     */
    private boolean discoverOpenAIModelsIfConfigured(String storedOpenAIKey) {
        if (storedOpenAIKey == null || storedOpenAIKey.trim().isEmpty()) {
            return false;
        }
        
        try {
            return discoverOpenAIModels(storedOpenAIKey);
        } catch (Exception e) {
            LOG.warn("Failed to discover OpenAI models: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Discovers Gemini models if API key is configured.
     * 
     * @param storedGeminiKey the stored Gemini API key
     * @return true if models were discovered successfully, false otherwise
     */
    private boolean discoverGeminiModelsIfConfigured(String storedGeminiKey) {
        if (storedGeminiKey == null || storedGeminiKey.trim().isEmpty()) {
            return false;
        }
        
        try {
            return discoverGeminiModelsFromProvider(storedGeminiKey);
        } catch (Exception e) {
            LOG.warn("Failed to discover Gemini models: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Discovers OpenAI models using the OpenAI provider.
     * 
     * @param apiKey the OpenAI API key
     * @return true if models were discovered successfully, false otherwise
     */
    private boolean discoverOpenAIModels(String apiKey) {
        try {
            LOG.info("Discovering OpenAI models");
            
            OpenAIProvider provider = new OpenAIProvider(HttpClient.newHttpClient());
            String[] discoveredModels = provider.discoverAvailableModels(apiKey).get();
            
            if (discoveredModels != null && discoveredModels.length > 0) {
                // Filter to only stable, production-ready models suitable for text analysis
                String[] stableModels = filterToStableOpenAIModels(discoveredModels);
                
                // Add discovered models
                for (String modelId : stableModels) {
                    String displayName = getDefaultDisplayName(AIServiceType.OPENAI, modelId);
                    modelService.addDiscoveredModel(displayName, AIServiceType.OPENAI, modelId);
                }
                
                LOG.info("Successfully discovered " + stableModels.length + " stable OpenAI models");
                return true;
            } else {
                LOG.warn("No OpenAI models discovered");
                return false;
            }
        } catch (Exception e) {
            LOG.error("Error discovering OpenAI models: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Filters discovered OpenAI models to only include stable, production-ready models suitable for text analysis.
     * This ensures users see only the most reliable and useful models for our use case.
     */
    private String[] filterToStableOpenAIModels(String[] allModels) {
        // Define stable, production-ready models in order of preference
        // Focus on models suitable for text analysis and code review
        String[] stableModelIds = {
            "gpt-4o",              // Latest GPT-4 model, best performance
            "gpt-4o-mini",         // Fast, cost-effective GPT-4
            "gpt-4-turbo",         // Previous GPT-4 Turbo
            "gpt-4",               // Standard GPT-4
            "gpt-3.5-turbo",       // Reliable, cost-effective option
            "gpt-3.5-turbo-16k"    // Higher context window version
        };
        
        List<String> filteredModels = new ArrayList<>();
        
        // Add stable models that are available
        for (String stableId : stableModelIds) {
            for (String discoveredId : allModels) {
                if (discoveredId.equals(stableId)) {
                    filteredModels.add(discoveredId);
                    break;
                }
            }
        }
        
        // If no stable models found, fall back to basic models
        if (filteredModels.isEmpty()) {
            LOG.warn("No stable OpenAI models found, using basic fallback");
            return new String[]{"gpt-3.5-turbo", "gpt-4o", "gpt-4o-mini"};
        }
        
        LOG.info("Filtered to " + filteredModels.size() + " stable OpenAI models: " + 
                String.join(", ", filteredModels));
        
        return filteredModels.toArray(new String[0]);
    }
    
    /**
     * Discovers Gemini models using the Gemini provider.
     * 
     * @param apiKey the Gemini API key
     * @return true if models were discovered successfully, false otherwise
     */
    private boolean discoverGeminiModelsFromProvider(String apiKey) {
        try {
            LOG.info("Discovering Gemini models from provider");
            
            GeminiProvider provider = new GeminiProvider(HttpClient.newHttpClient());
            String[] discoveredModels = provider.discoverAvailableModels(apiKey).get();
            
            if (discoveredModels != null && discoveredModels.length > 0) {
                // Filter to stable models only
                String[] stableModels = filterToStableGeminiModels(discoveredModels);
                
                // Add discovered models
                for (String modelId : stableModels) {
                    String displayName = getDefaultDisplayName(AIServiceType.GEMINI, modelId);
                    modelService.addDiscoveredModel(displayName, AIServiceType.GEMINI, modelId);
                }
                
                LOG.info("Successfully discovered " + stableModels.length + " stable Gemini models");
                return true;
            } else {
                LOG.warn("No Gemini models discovered");
                return false;
            }
        } catch (Exception e) {
            LOG.error("Error discovering Gemini models: " + e.getMessage(), e);
            return false;
        }
    }
} 