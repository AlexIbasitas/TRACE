package com.trace.ai.ui;

import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBList;
import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.DefaultListModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ListSelectionModel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.trace.ai.models.AIModel;
import com.trace.ai.services.AIModelService;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;

/**
 * Helper class for UI component creation in the AI Service Configuration panel.
 * 
 * <p>This class provides methods for creating and configuring UI components
 * used in the AI service configuration interface. It handles component creation,
 * styling, responsive sizing, and theme change handling.</p>
 * 
 * <p>The helper encapsulates all UI component creation logic to keep the main
 * configuration panel focused on business logic while delegating UI creation
 * to this specialized helper.</p>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
public class AIServiceConfigUIHelper {
    
    /**
     * Creates the API key configuration panel.
     */
    @NotNull
    public JPanel createAPIKeyPanel(JBPasswordField openaiApiKeyField, JBPasswordField geminiApiKeyField,
                                   JButton testOpenAIButton, JButton testGeminiButton,
                                   JBLabel openaiStatusLabel, JBLabel geminiStatusLabel,
                                   Runnable clearOpenAIAction, Runnable clearGeminiAction) {
        JPanel panel = new JBPanel<>(new BorderLayout());
        panel.setBorder(JBUI.Borders.compound(
            JBUI.Borders.customLine(UIUtil.getPanelBackground().darker(), 1),
            JBUI.Borders.empty(10)
        ));
        
        // Header with zoom responsiveness
        JBLabel headerLabel = createZoomResponsiveHeader("API Key Configuration");
        headerLabel.setBorder(JBUI.Borders.emptyBottom(5)); // Smaller border
        
        // Create a structured layout for consistent sizing
        JPanel keysPanel = new JBPanel<>(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        
        // Let input fields expand to fit content naturally (height tied to font metrics)
        int fieldBaseFontSize = UIUtil.getLabelFont().getSize();
        int fieldHeight = Math.max((int) Math.round(fieldBaseFontSize * 2.2), 28);
        Insets fieldInsets = new Insets(6, 10, 6, 10);
        
        configurePasswordField(openaiApiKeyField, fieldHeight, fieldInsets);
        configurePasswordField(geminiApiKeyField, fieldHeight, fieldInsets);
        
        // Configure Apply buttons responsively (avoid truncation at high zoom)
        configureButtonForText(testOpenAIButton);
        configureButtonForText(testGeminiButton);
        
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
        keysPanel.add(createTextFieldWithX(openaiApiKeyField, clearOpenAIAction), gbc);
        
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
        keysPanel.add(createTextFieldWithX(geminiApiKeyField, clearGeminiAction), gbc);
        
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
     * Creates the model management panel.
     */
    @NotNull
    public JPanel createModelPanel(JBList<AIModel> modelList, JBLabel defaultModelLabel,
                                  JButton setDefaultButton, JButton refreshModelsButton) {
        JPanel panel = new JBPanel<>(new BorderLayout());
        panel.setBorder(JBUI.Borders.compound(
            JBUI.Borders.customLine(UIUtil.getPanelBackground().darker(), 1),
            JBUI.Borders.empty(10)
        ));
        
        // Header with zoom responsiveness
        JBLabel headerLabel = createZoomResponsiveHeader("Available Models");
        headerLabel.setBorder(JBUI.Borders.emptyBottom(5)); // Smaller border
        
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
        
        // Model list (main control) - non-scrollable with zoom-responsive cells
        modelList.setCellRenderer(new AIModelListCellRenderer());
        modelList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Configure list to be non-scrollable and scale with zoom
        configureResponsiveModelList(modelList);
        
        // Action buttons panel - use proper layout for professional appearance
        JPanel actionButtonPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 8, 4)); // Proper spacing
        actionButtonPanel.setBorder(JBUI.Borders.empty(8, 0, 0, 0)); // Proper top margin
        
        // Configure buttons responsively to prevent truncation at any zoom
        configureButtonForText(setDefaultButton);
        configureButtonForText(refreshModelsButton);
        
        actionButtonPanel.add(setDefaultButton);
        actionButtonPanel.add(refreshModelsButton);
        
        // Simple layout following JetBrains best practices
        // Default model info at top, main control in center, action buttons at bottom
        contentPanel.add(defaultSection, BorderLayout.NORTH);
        contentPanel.add(modelList, BorderLayout.CENTER);
        contentPanel.add(actionButtonPanel, BorderLayout.SOUTH);
        
        // Add sections to main panel
        panel.add(headerLabel, BorderLayout.NORTH);
        panel.add(contentPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Creates a text field with an X icon for clearing.
     */
    public JPanel createTextFieldWithX(JBPasswordField textField, Runnable clearAction) {
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
        xIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                clearAction.run();
            }
        });
        
        // Add document listener to show/hide X based on content
        textField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                xIcon.setVisible(textField.getPassword().length > 0);
            }
            
            @Override
            public void removeUpdate(DocumentEvent e) {
                xIcon.setVisible(textField.getPassword().length > 0);
            }
            
            @Override
            public void changedUpdate(DocumentEvent e) {
                xIcon.setVisible(textField.getPassword().length > 0);
            }
        });
        
        panel.add(xIcon, BorderLayout.EAST);
        
        return panel;
    }
    
    /**
     * Creates a zoom-responsive header label that updates with font changes.
     * 
     * @param text the header text to display
     * @return a JBLabel that responds to zoom changes
     */
    public JBLabel createZoomResponsiveHeader(String text) {
        return new JBLabel(text) {
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
     * Creates a password field that scales its height with the current UI font.
     */
    public JBPasswordField createResponsivePasswordField() {
        JBPasswordField field = new JBPasswordField() {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                int fontSize = UIUtil.getLabelFont().getSize();
                int height = Math.max((int) Math.round(fontSize * 2.2), 28);
                return new Dimension(d.width, height);
            }
        };
        field.setFont(UIUtil.getLabelFont());
        field.setMargin(new Insets(6, 10, 6, 10));
        return field;
    }
    
    /**
     * Creates a JButton that sizes itself based on its text (avoids truncation on zoom).
     */
    public JButton createResponsiveButton(String text) {
        JButton button = new JButton(text) {
            @Override
            public Dimension getPreferredSize() {
                Font font = UIUtil.getLabelFont();
                FontMetrics fm = getFontMetrics(font);
                int textWidth = fm.stringWidth(getText());
                int fontSize = font.getSize();
                
                // More generous sizing to prevent text cutoff at smaller zoom levels
                int width = textWidth + Math.max(32, fontSize * 3); // Increased padding
                int height = Math.max((int) Math.round(fontSize * 2.5), 32); // Increased height
                
                return new Dimension(width, height);
            }
        };
        button.setFont(UIUtil.getLabelFont());
        button.setMargin(new Insets(8, 16, 8, 16)); // Increased margins
        return button;
    }
    
    /**
     * Applies consistent sizing rules to a password field instance.
     */
    public void configurePasswordField(JBPasswordField field, int height, Insets insets) {
        field.setPreferredSize(new Dimension(0, height));
        field.setMinimumSize(new Dimension(0, height));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        field.setFont(UIUtil.getLabelFont());
        field.setMargin(insets);
    }
    
    /**
     * Ensures a button is large enough for its text at current zoom.
     */
    public void configureButtonForText(JButton button) {
        Font font = UIUtil.getLabelFont();
        FontMetrics fm = button.getFontMetrics(font);
        int textWidth = fm.stringWidth(button.getText());
        int fontSize = font.getSize();
        
        // More generous sizing to prevent text cutoff at smaller zoom levels
        int width = textWidth + Math.max(32, fontSize * 3); // Increased padding
        int height = Math.max((int) Math.round(fontSize * 2.5), 32); // Increased height
        
        button.setFont(font);
        button.setMinimumSize(new Dimension(width, height));
        button.setPreferredSize(new Dimension(width, height));
        button.setMargin(new Insets(8, 16, 8, 16)); // Increased margins
    }
    
    /**
     * Configures the model list to be non-scrollable with zoom-responsive cell sizing.
     */
    public void configureResponsiveModelList(JBList<AIModel> modelList) {
        // Set font to current UI font for zoom responsiveness
        modelList.setFont(UIUtil.getLabelFont());
        
        // Calculate cell height based on current font size
        int fontSize = UIUtil.getLabelFont().getSize();
        int cellHeight = Math.max((int) Math.round(fontSize * 2.5), 32);
        
        // Set fixed cell height that scales with zoom
        modelList.setFixedCellHeight(cellHeight);
        modelList.setFixedCellWidth(-1); // Let width be determined by container
        
        // Remove scroll pane behavior - let the list size naturally
        modelList.setPreferredSize(null); // Let Swing calculate based on content
        modelList.setMinimumSize(new Dimension(0, 0)); // Allow shrinking
        modelList.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        
        // Add a listener to recalculate cell height when font changes
        modelList.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                updateModelListCellHeight(modelList);
            }
        });
    }
    
    /**
     * Updates the model list cell height based on current font size.
     */
    public void updateModelListCellHeight(JBList<AIModel> modelList) {
        int fontSize = UIUtil.getLabelFont().getSize();
        int cellHeight = Math.max((int) Math.round(fontSize * 2.5), 32);
        modelList.setFixedCellHeight(cellHeight);
        modelList.revalidate();
        modelList.repaint();
    }
    
    /**
     * Updates button sizes only if font size has changed significantly.
     */
    public void updateButtonSizesIfNeeded(JButton testOpenAIButton, JButton testGeminiButton,
                                         JButton setDefaultButton, JButton refreshModelsButton,
                                         java.util.concurrent.atomic.AtomicInteger lastKnownFontSize) {
        // Check if font size has changed significantly since last update
        int currentFontSize = UIUtil.getLabelFont().getSize();
        if (Math.abs(currentFontSize - lastKnownFontSize.get()) >= 2) { // Only update if font changed by 2+ points
            lastKnownFontSize.set(currentFontSize);
            configureButtonForText(testOpenAIButton);
            configureButtonForText(testGeminiButton);
            configureButtonForText(setDefaultButton);
            configureButtonForText(refreshModelsButton);
        }
    }
    
    /**
     * Sets up theme change listener to handle zoom and font changes.
     */
    public void setupThemeChangeListener(java.awt.Component component, JBList<AIModel> modelList,
                                        JButton testOpenAIButton, JButton testGeminiButton,
                                        JButton setDefaultButton, JButton refreshModelsButton,
                                        java.util.concurrent.atomic.AtomicInteger lastKnownFontSize) {
        // Only listen for component visibility changes (when panel is shown)
        // This prevents constant revalidation that causes auto-scroll issues
        component.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent event) {
                SwingUtilities.invokeLater(() -> {
                    updateModelListCellHeight(modelList);
                    // Only update buttons if font size has changed significantly
                    updateButtonSizesIfNeeded(testOpenAIButton, testGeminiButton, setDefaultButton, 
                                            refreshModelsButton, lastKnownFontSize);
                });
            }
        });
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
}
