package com.triagemate.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import javax.swing.JComboBox;
import com.intellij.util.ui.JBUI;
import com.triagemate.models.AIModel;
import com.triagemate.services.AIModelService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.util.List;

/**
 * Simple AI model selector component for the chat interface.
 * 
 * <p>This component provides a dropdown that allows users to quickly switch
 * between available AI models without leaving the chat interface.</p>
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>Shows all enabled models in a dropdown</li>
 *   <li>Highlights the currently selected model</li>
 *   <li>Updates automatically when models are added/removed</li>
 *   <li>Compact design that fits in chat interface</li>
 * </ul>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
public class AIModelSelector extends JBPanel<AIModelSelector> {
    
    private static final Logger LOG = Logger.getInstance(AIModelSelector.class);
    
    // Services
    private final AIModelService modelService;
    
    // UI Components
    private final JBLabel label;
    private final JComboBox<AIModel> modelComboBox;
    private final DefaultComboBoxModel<AIModel> comboBoxModel;
    
    // Current state
    private AIModel currentModel;
    private boolean isUpdating = false;
    
    /**
     * Constructor for AIModelSelector.
     * 
     * @param modelService the AI model service
     */
    public AIModelSelector(@NotNull AIModelService modelService) {
        this.modelService = modelService;
        
        // Initialize UI components
        this.label = new JBLabel("Model:");
        this.comboBoxModel = new DefaultComboBoxModel<>();
        this.modelComboBox = new JComboBox<>(comboBoxModel);
        
        // Setup UI
        setupUI();
        setupEventHandlers();
        refreshModelList();
    }
    
    // ============================================================================
    // UI SETUP
    // ============================================================================
    
    /**
     * Sets up the UI layout and components.
     */
    private void setupUI() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 5, 2));
        setBorder(JBUI.Borders.empty(2));
        
        // Add components
        add(label);
        add(modelComboBox);
        
        // Set preferred size for compact display
        modelComboBox.setPreferredSize(new Dimension(150, 24));
    }
    
    /**
     * Sets up event handlers for the combo box.
     */
    private void setupEventHandlers() {
        modelComboBox.addActionListener(e -> {
            if (!isUpdating) {
                AIModel selectedModel = (AIModel) modelComboBox.getSelectedItem();
                if (selectedModel != null && selectedModel != currentModel) {
                    currentModel = selectedModel;
                    modelService.setDefaultModel(selectedModel.getId());
                    LOG.info("Model changed to: " + selectedModel.getDisplayName());
                }
            }
        });
        
        // Refresh model list when dropdown opens
        modelComboBox.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                refreshModelList();
            }
            
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                // No action needed
            }
            
            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                // No action needed
            }
        });
    }
    
    // ============================================================================
    // MODEL MANAGEMENT
    // ============================================================================
    
    /**
     * Refreshes the model list in the combo box.
     */
    public void refreshModelList() {
        isUpdating = true;
        
        try {
            // Store current selection
            AIModel currentSelection = (AIModel) modelComboBox.getSelectedItem();
            
            // Clear and repopulate
            comboBoxModel.removeAllElements();
            
            // Add enabled models
            List<AIModel> enabledModels = modelService.getEnabledModels();
            for (AIModel model : enabledModels) {
                comboBoxModel.addElement(model);
            }
            
            // Restore selection or select default
            if (currentSelection != null && enabledModels.contains(currentSelection)) {
                modelComboBox.setSelectedItem(currentSelection);
                currentModel = currentSelection;
            } else {
                AIModel defaultModel = modelService.getDefaultModel();
                if (defaultModel != null) {
                    modelComboBox.setSelectedItem(defaultModel);
                    currentModel = defaultModel;
                }
            }
            
        } finally {
            isUpdating = false;
        }
    }
    
    /**
     * Gets the currently selected model.
     * 
     * @return the currently selected model, or null if none selected
     */
    @Nullable
    public AIModel getSelectedModel() {
        return (AIModel) modelComboBox.getSelectedItem();
    }
    
    /**
     * Sets the selected model.
     * 
     * @param model the model to select
     */
    public void setSelectedModel(@Nullable AIModel model) {
        if (model != null) {
            modelComboBox.setSelectedItem(model);
            currentModel = model;
        }
    }
    
    /**
     * Sets the selected model by ID.
     * 
     * @param modelId the ID of the model to select
     */
    public void setSelectedModelById(@Nullable String modelId) {
        if (modelId != null) {
            AIModel model = modelService.getModel(modelId);
            if (model != null) {
                setSelectedModel(model);
            }
        }
    }
    
    /**
     * Checks if the selector has any models available.
     * 
     * @return true if models are available, false otherwise
     */
    public boolean hasModels() {
        return comboBoxModel.getSize() > 0;
    }
    
    /**
     * Gets the number of available models.
     * 
     * @return the number of available models
     */
    public int getModelCount() {
        return comboBoxModel.getSize();
    }
    
    // ============================================================================
    // UI STATE MANAGEMENT
    // ============================================================================
    
    /**
     * Sets whether the selector is enabled.
     * 
     * @param enabled true to enable, false to disable
     */
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        label.setEnabled(enabled);
        modelComboBox.setEnabled(enabled);
    }
    
    /**
     * Sets the label text.
     * 
     * @param text the label text
     */
    public void setLabelText(@NotNull String text) {
        label.setText(text);
    }
    
    /**
     * Gets the label text.
     * 
     * @return the label text
     */
    @NotNull
    public String getLabelText() {
        return label.getText();
    }
    
    /**
     * Sets the preferred width of the combo box.
     * 
     * @param width the preferred width
     */
    public void setComboBoxWidth(int width) {
        Dimension size = modelComboBox.getPreferredSize();
        size.width = width;
        modelComboBox.setPreferredSize(size);
    }
    
    /**
     * Gets the combo box component for custom styling.
     * 
     * @return the combo box component
     */
    @NotNull
    public JComboBox<AIModel> getComboBox() {
        return modelComboBox;
    }
    
    /**
     * Gets the label component for custom styling.
     * 
     * @return the label component
     */
    @NotNull
    public JBLabel getLabel() {
        return label;
    }
} 