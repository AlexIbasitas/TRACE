package com.trace.ai.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.trace.ai.configuration.AISettings;
import com.trace.common.constants.TriagePanelConstants;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * Custom Rule configuration panel for TRACE settings.
 * 
 * <p>This panel provides a simple interface for users to input custom rules
 * that will be appended to AI prompts for personalized analysis.</p>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
public class CustomRulePanel extends JBPanel<CustomRulePanel> {
    
    private static final Logger LOG = Logger.getInstance(CustomRulePanel.class);
    
    // Services
    private final AISettings aiSettings;
    
    // Custom Rule Configuration
    private final JTextArea customRuleTextArea;
    private final JBLabel characterCounterLabel;
    private final JButton saveCustomRuleButton;
    private final JButton clearCustomRuleButton;
    
    /**
     * Creates a new custom rule configuration panel.
     * 
     * @param aiSettings the AI settings instance
     */
    public CustomRulePanel(@NotNull AISettings aiSettings) {
        this.aiSettings = aiSettings;
        
        // Initialize custom rule components
        this.customRuleTextArea = new JTextArea();
        this.characterCounterLabel = new JBLabel("0/500");
        this.saveCustomRuleButton = new JButton("Save");
        this.clearCustomRuleButton = new JButton("Clear");
        
        // Initialize the panel
        initializePanel();
        setupEventHandlers();
        setupThemeChangeListener();
        
        // Load current settings
        loadCurrentSettings();
        
        LOG.info("Custom rule panel created and initialized");
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
        
        // Header with zoom responsiveness
        JBLabel headerLabel = createZoomResponsiveHeader("Custom Rule");
        headerLabel.setBorder(JBUI.Borders.emptyBottom(5)); // Smaller border
        
        // Subheading with zoom responsiveness
        JBLabel subheadingLabel = createZoomResponsiveSubheading("Add a custom rule for your preferences");
        subheadingLabel.setBorder(JBUI.Borders.emptyBottom(5)); // Smaller border
        
        // Content panel
        JPanel contentPanel = new JBPanel<>(new BorderLayout());
        
        // Configure text area with zoom-responsive sizing
        configureResponsiveTextArea();
        
        // Create scroll pane with responsive sizing
        JBScrollPane scrollPane = createResponsiveScrollPane();
        
        // Character counter and buttons panel
        JPanel bottomPanel = new JBPanel<>(new BorderLayout());
        bottomPanel.setBorder(JBUI.Borders.emptyTop(10));
        
        // Character counter
        characterCounterLabel.setFont(characterCounterLabel.getFont().deriveFont(Font.PLAIN, 11f));
        characterCounterLabel.setForeground(UIUtil.getLabelDisabledForeground());
        
        // Buttons panel
        JPanel buttonPanel = new JBPanel<>(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(clearCustomRuleButton);
        buttonPanel.add(saveCustomRuleButton);
        
        bottomPanel.add(characterCounterLabel, BorderLayout.WEST);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);
        
        // Add components to content panel
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        contentPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        // Create a header panel to contain both header and subheading
        JPanel headerPanel = new JBPanel<>(new BorderLayout());
        headerPanel.add(headerLabel, BorderLayout.NORTH);
        headerPanel.add(subheadingLabel, BorderLayout.CENTER);
        
        // Add components to main panel
        add(headerPanel, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);
    }
    
    /**
     * Creates a zoom-responsive header label that updates with font changes.
     * 
     * @param text the header text to display
     * @return a JBLabel that responds to zoom changes
     */
    private JBLabel createZoomResponsiveHeader(String text) {
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
     * Creates a zoom-responsive subheading label that updates with font changes.
     * 
     * @param text the subheading text to display
     * @return a JBLabel that responds to zoom changes
     */
    private JBLabel createZoomResponsiveSubheading(String text) {
        return new JBLabel(text) {
            @Override
            public Font getFont() {
                // Always return the current UI font to respond to zoom changes
                Font baseFont = UIUtil.getLabelFont();
                return baseFont.deriveFont(Font.PLAIN, baseFont.getSize() - 2);
            }
            
            @Override
            public void setFont(Font font) {
                // Override to always use UI font for zoom responsiveness
                Font baseFont = UIUtil.getLabelFont();
                super.setFont(baseFont.deriveFont(Font.PLAIN, baseFont.getSize() - 2));
            }
            
            @Override
            public void paint(Graphics g) {
                // Ensure font is always current before painting
                Font baseFont = UIUtil.getLabelFont();
                setFont(baseFont.deriveFont(Font.PLAIN, baseFont.getSize() - 2));
                super.paint(g);
            }
            
            @Override
            public Color getForeground() {
                // Always return the disabled foreground color
                return UIUtil.getLabelDisabledForeground();
            }
            
            @Override
            public void setForeground(Color color) {
                // Override to always use disabled foreground color
                super.setForeground(UIUtil.getLabelDisabledForeground());
            }
        };
    }
    
    /**
     * Configures the text area with dynamic sizing that grows with content.
     */
    private void configureResponsiveTextArea() {
        customRuleTextArea.setLineWrap(true);
        customRuleTextArea.setWrapStyleWord(true);
        customRuleTextArea.setFont(UIUtil.getLabelFont());
        customRuleTextArea.setBorder(JBUI.Borders.compound(
            JBUI.Borders.customLine(UIUtil.getTextFieldBackground().darker(), 1),
            JBUI.Borders.empty(5)
        ));
        
        // Set responsive minimum and maximum sizes
        int fontSize = UIUtil.getLabelFont().getSize();
        int minHeight = Math.max(fontSize * 4, 60);
        
        customRuleTextArea.setMinimumSize(new Dimension(100, minHeight));
        customRuleTextArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
    }
    
    /**
     * Creates a scroll pane that dynamically adapts to text area content.
     */
    private JBScrollPane createResponsiveScrollPane() {
        JBScrollPane scrollPane = new JBScrollPane(customRuleTextArea);
        
        // Calculate responsive minimum sizing
        int fontSize = UIUtil.getLabelFont().getSize();
        int minHeight = Math.max(fontSize * 4, 60);
        int maxHeight = Math.max(fontSize * 12, 200); // Maximum height to prevent excessive growth
        
        // Set responsive dimensions that allow horizontal shrinking but maintain vertical visibility
        scrollPane.setMinimumSize(new Dimension(100, minHeight));
        scrollPane.setPreferredSize(new Dimension(250, Math.max(fontSize * 6, 80)));
        scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, maxHeight));
        
        // Configure scroll policies
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        return scrollPane;
    }
    
    /**
     * Updates the scroll pane size when text content changes.
     */
    private void updateScrollPaneSize() {
        Container parent = customRuleTextArea.getParent();
        if (parent instanceof JViewport) {
            JScrollPane scrollPane = (JScrollPane) parent.getParent();
            
            // Calculate dynamic height based on content
            int fontSize = UIUtil.getLabelFont().getSize();
            int minHeight = Math.max(fontSize * 4, 60);
            int maxHeight = Math.max(fontSize * 12, 200);
            
            // Calculate content height
            int contentHeight = calculateTextAreaContentHeight();
            int height = Math.max(minHeight, Math.min(contentHeight, maxHeight));
            
            // Update scroll pane size
            Dimension currentSize = scrollPane.getPreferredSize();
            scrollPane.setPreferredSize(new Dimension(currentSize.width, height));
            
            scrollPane.revalidate();
            scrollPane.repaint();
        }
    }
    
    /**
     * Calculates the height needed to display all text content.
     */
    private int calculateTextAreaContentHeight() {
        String text = customRuleTextArea.getText();
        if (text == null || text.isEmpty()) {
            return UIUtil.getLabelFont().getSize() * 4;
        }
        
        FontMetrics fm = customRuleTextArea.getFontMetrics(customRuleTextArea.getFont());
        int lineHeight = fm.getHeight();
        
        // Calculate wrapped lines based on current width
        int width = customRuleTextArea.getWidth();
        if (width <= 0) width = 200; // Default width if not yet laid out
        
        int totalLines = 0;
        String[] paragraphs = text.split("\n");
        
        for (String paragraph : paragraphs) {
            if (paragraph.isEmpty()) {
                totalLines++;
            } else {
                int lineWidth = fm.stringWidth(paragraph);
                int effectiveWidth = Math.max(width - 40, 100); // Account for padding and borders
                int linesForParagraph = Math.max(1, (lineWidth / effectiveWidth) + 1);
                totalLines += linesForParagraph;
            }
        }
        
        // Add some padding and ensure minimum height
        return (totalLines * lineHeight) + 30; // 30px padding for borders and spacing
    }
    
    /**
     * Updates the text area and scroll pane sizing based on current font size.
     */
    private void updateResponsiveSizing() {
        // Update font to current UI font for zoom responsiveness
        customRuleTextArea.setFont(UIUtil.getLabelFont());
        
        // Trigger recalculation of dynamic sizes
        customRuleTextArea.revalidate();
        updateScrollPaneSize();
        
        // Force the entire panel to recalculate its layout
        revalidate();
        repaint();
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
                    updateResponsiveSizing();
                });
            }
        });
    }
    
    /**
     * Sets up event handlers for all components.
     */
    private void setupEventHandlers() {
        // Custom rule management
        saveCustomRuleButton.addActionListener(e -> saveCustomRule());
        clearCustomRuleButton.addActionListener(e -> clearCustomRule());
        customRuleTextArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) { 
                updateCharacterCounter();
                SwingUtilities.invokeLater(() -> {
                    customRuleTextArea.revalidate();
                    updateScrollPaneSize();
                });
            }
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) { 
                updateCharacterCounter();
                SwingUtilities.invokeLater(() -> {
                    customRuleTextArea.revalidate();
                    updateScrollPaneSize();
                });
            }
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) { 
                updateCharacterCounter();
                SwingUtilities.invokeLater(() -> {
                    customRuleTextArea.revalidate();
                    updateScrollPaneSize();
                });
            }
        });
    }
    
    /**
     * Updates the character counter for the custom rule text area.
     */
    private void updateCharacterCounter() {
        String text = customRuleTextArea.getText();
        int length = text.length();
        int maxLength = 500;
        
        characterCounterLabel.setText(length + "/" + maxLength);
        
        // Update color based on length
        if (length > maxLength) {
            characterCounterLabel.setForeground(TriagePanelConstants.ERROR_FOREGROUND);
            saveCustomRuleButton.setEnabled(false);
        } else if (length > maxLength * 0.8) {
            characterCounterLabel.setForeground(TriagePanelConstants.WARNING_FOREGROUND);
            saveCustomRuleButton.setEnabled(true);
        } else {
            characterCounterLabel.setForeground(UIUtil.getLabelDisabledForeground());
            saveCustomRuleButton.setEnabled(true);
        }
    }
    
    /**
     * Saves the custom rule to settings.
     */
    private void saveCustomRule() {
        String customRule = customRuleTextArea.getText().trim();
        if (customRule.length() > 500) {
            showError("Custom rule cannot exceed 500 characters");
            return;
        }
        
        aiSettings.setCustomRule(customRule);
        showSuccess("Custom rule saved successfully");
        LOG.info("Custom rule saved: " + (customRule.isEmpty() ? "empty" : "length " + customRule.length()));
    }
    
    /**
     * Clears the custom rule text area.
     */
    private void clearCustomRule() {
        customRuleTextArea.setText("");
        updateCharacterCounter();
        LOG.info("Custom rule cleared");
    }
    
    /**
     * Loads current settings into the UI.
     */
    public void loadCurrentSettings() {
        LOG.info("Loading current custom rule settings into UI");
        
        // Load custom rule
        String customRule = aiSettings.getCustomRule();
        if (customRule != null) {
            customRuleTextArea.setText(customRule);
            updateCharacterCounter();
            LOG.debug("Loaded custom rule into UI");
        } else {
            customRuleTextArea.setText("");
            updateCharacterCounter();
            LOG.debug("No custom rule found in settings");
        }
        
        LOG.info("Finished loading custom rule settings into UI");
    }
    
    /**
     * Checks if the panel has been modified.
     * 
     * @return true if any settings have changed, false otherwise
     */
    public boolean isModified() {
        String currentCustomRule = customRuleTextArea.getText();
        String originalCustomRule = aiSettings.getCustomRule();
        
        boolean customRuleChanged = !java.util.Objects.equals(currentCustomRule, originalCustomRule);
        
        if (customRuleChanged) {
            LOG.debug("Custom rule panel modified");
        }
        
        return customRuleChanged;
    }
    
    /**
     * Applies the current settings.
     */
    public void apply() {
        LOG.info("Applying custom rule settings");
        
        String customRule = customRuleTextArea.getText().trim();
        String originalCustomRule = aiSettings.getCustomRule();
        
        // Save custom rule if changed
        if (!java.util.Objects.equals(customRule, originalCustomRule)) {
            aiSettings.setCustomRule(customRule);
            LOG.info("Custom rule applied: " + (customRule.isEmpty() ? "empty" : "length " + customRule.length()));
        }
        
        LOG.info("Custom rule settings applied");
    }
    
    /**
     * Resets the UI to the original settings.
     */
    public void reset() {
        LOG.info("Resetting custom rule configuration to current stored state");
        
        // Load current settings from storage
        loadCurrentSettings();
        
        LOG.info("Custom rule configuration reset completed");
    }
    
    /**
     * Disposes UI resources.
     */
    public void disposeUIResources() {
        // Clear sensitive data
        customRuleTextArea.setText("");
    }
    
    /**
     * Shows an error message to the user.
     * 
     * @param message the error message to display
     */
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * Shows a success message to the user.
     * 
     * @param message the success message to display
     */
    private void showSuccess(String message) {
        JOptionPane.showMessageDialog(this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }
} 