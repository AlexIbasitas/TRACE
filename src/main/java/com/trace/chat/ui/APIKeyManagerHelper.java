package com.trace.chat.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.util.ui.UIUtil;
import com.trace.security.SecureAPIKeyManager;
import com.trace.ai.configuration.AIServiceType;
import com.trace.common.constants.TriagePanelConstants;
import com.intellij.openapi.application.ApplicationManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Helper class for managing API key operations in the AI Service Configuration panel.
 * 
 * <p>This class provides methods for testing, validating, and managing API keys
 * for OpenAI and Google Gemini services. It handles connectivity testing,
 * format validation, and secure storage operations.</p>
 * 
 * <p>The helper encapsulates all API key management logic to keep the main
 * configuration panel focused on UI concerns while delegating API operations
 * to this specialized helper.</p>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
public class APIKeyManagerHelper {
    
    private static final Logger LOG = Logger.getInstance(APIKeyManagerHelper.class);
    
    /**
     * Tests and applies the OpenAI API key.
     */
    public void testOpenAIKey(JBPasswordField openaiApiKeyField, JBLabel openaiStatusLabel, 
                             javax.swing.JButton testOpenAIButton, java.util.concurrent.atomic.AtomicBoolean isTestingConnection,
                             Runnable onSuccessCallback, java.awt.Component parentComponent) {
        if (isTestingConnection.get()) {
            return;
        }
        
        String apiKey = new String(openaiApiKeyField.getPassword());
        if (apiKey.trim().isEmpty()) {
            showError("Please enter an OpenAI API key", parentComponent);
            return;
        }
        
        // Validate API key format
        if (!isValidOpenAIKeyFormat(apiKey)) {
            showError("Invalid OpenAI API key format. Should be a valid OpenAI API key.", parentComponent);
            return;
        }
        
        isTestingConnection.set(true);
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
                isTestingConnection.set(false);
                testOpenAIButton.setEnabled(true);
                
                if (success) {
                    // Test successful - save the API key in background thread
                    ApplicationManager.getApplication().executeOnPooledThread(() -> {
                        boolean saved = SecureAPIKeyManager.storeAPIKey(AIServiceType.OPENAI, apiKey);
                        ApplicationManager.getApplication().invokeLater(() -> {
                            if (saved) {
                                openaiStatusLabel.setText("✅ Connected");
                                openaiStatusLabel.setForeground(UIUtil.getLabelSuccessForeground());
                                showSuccess("OpenAI API key applied successfully", parentComponent);
                                
                                // Call success callback for model discovery
                                if (onSuccessCallback != null) {
                                    onSuccessCallback.run();
                                }
                                
                                LOG.info("OpenAI API key tested and saved successfully");
                            } else {
                                openaiStatusLabel.setText("Failed to save API key");
                                openaiStatusLabel.setForeground(TriagePanelConstants.ERROR_FOREGROUND);
                                showError("Failed to save OpenAI API key", parentComponent);
                                LOG.error("Failed to save OpenAI API key");
                            }
                        });
                    });
                } else {
                    openaiStatusLabel.setText("Connection failed");
                    openaiStatusLabel.setForeground(TriagePanelConstants.ERROR_FOREGROUND);
                    showError("OpenAI API key is invalid or connection failed", parentComponent);
                    LOG.warn("OpenAI API key test failed");
                }
            });
        });
    }
    
    /**
     * Tests and applies the Gemini API key.
     */
    public void testGeminiKey(JBPasswordField geminiApiKeyField, JBLabel geminiStatusLabel, 
                             javax.swing.JButton testGeminiButton, java.util.concurrent.atomic.AtomicBoolean isTestingConnection,
                             Runnable onSuccessCallback, java.awt.Component parentComponent) {
        if (isTestingConnection.get()) {
            return;
        }
        
        String apiKey = new String(geminiApiKeyField.getPassword());
        if (apiKey.trim().isEmpty()) {
            showError("Please enter a Gemini API key", parentComponent);
            return;
        }
        
        // Validate API key format
        if (!isValidGeminiKeyFormat(apiKey)) {
            showError("Invalid Gemini API key format. Should be a valid Google API key.", parentComponent);
            return;
        }
        
        isTestingConnection.set(true);
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
                isTestingConnection.set(false);
                testGeminiButton.setEnabled(true);
                
                if (success) {
                    // Test successful - save the API key in background thread
                    ApplicationManager.getApplication().executeOnPooledThread(() -> {
                        boolean saved = SecureAPIKeyManager.storeAPIKey(AIServiceType.GEMINI, apiKey);
                        ApplicationManager.getApplication().invokeLater(() -> {
                            if (saved) {
                                geminiStatusLabel.setText("✅ Connected");
                                geminiStatusLabel.setForeground(UIUtil.getLabelSuccessForeground());
                                showSuccess("Gemini API key applied successfully", parentComponent);
                                
                                // Call success callback for model discovery
                                if (onSuccessCallback != null) {
                                    onSuccessCallback.run();
                                }
                                
                                LOG.info("Gemini API key tested and saved successfully");
                            } else {
                                geminiStatusLabel.setText("Failed to save API key");
                                geminiStatusLabel.setForeground(TriagePanelConstants.ERROR_FOREGROUND);
                                showError("Failed to save Gemini API key", parentComponent);
                                LOG.error("Failed to save Gemini API key");
                            }
                        });
                    });
                } else {
                    geminiStatusLabel.setText("Connection failed");
                    geminiStatusLabel.setForeground(TriagePanelConstants.ERROR_FOREGROUND);
                    showError("Gemini API key is invalid or connection failed", parentComponent);
                    LOG.warn("Gemini API key test failed");
                }
            });
        });
    }
    
    /**
     * Validates OpenAI API key format.
     */
    public boolean isValidOpenAIKeyFormat(String apiKey) {
        return apiKey != null && apiKey.startsWith("sk-") && apiKey.length() >= 50;
    }
    
    /**
     * Validates Gemini API key format.
     */
    public boolean isValidGeminiKeyFormat(String apiKey) {
        return apiKey != null && apiKey.length() >= 20 && apiKey.matches("^[A-Za-z0-9_-]+$");
    }
    
    /**
     * Tests OpenAI connectivity without consuming quota.
     */
    public boolean testOpenAIConnectivity(String apiKey) {
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
    public boolean testGeminiConnectivity(String apiKey) {
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
    public void clearOpenAIKey(JBPasswordField openaiApiKeyField, JBLabel openaiStatusLabel, Runnable onClearCallback) {
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
                
                // Call clear callback for model list refresh
                if (onClearCallback != null) {
                    onClearCallback.run();
                }
            });
        });
    }
    
    /**
     * Clears the Gemini API key.
     */
    public void clearGeminiKey(JBPasswordField geminiApiKeyField, JBLabel geminiStatusLabel, Runnable onClearCallback) {
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
                
                // Call clear callback for model list refresh
                if (onClearCallback != null) {
                    onClearCallback.run();
                }
            });
        });
    }
    
    /**
     * Handles OpenAI API key changes.
     */
    public void handleOpenAIKeyChange(JBPasswordField openaiApiKeyField, JBLabel openaiStatusLabel) {
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
    public void handleGeminiKeyChange(JBPasswordField geminiApiKeyField, JBLabel geminiStatusLabel) {
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
     * Shows an error message.
     */
    private void showError(String message, java.awt.Component parentComponent) {
        Messages.showErrorDialog(parentComponent, message, "Configuration Error");
    }
    
    /**
     * Shows a success message.
     */
    private void showSuccess(String message, java.awt.Component parentComponent) {
        Messages.showInfoMessage(parentComponent, message, "Success");
    }
}
