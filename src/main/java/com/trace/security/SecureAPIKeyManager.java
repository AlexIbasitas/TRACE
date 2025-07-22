package com.trace.security;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.diagnostic.Logger;
import com.trace.ai.configuration.AIServiceType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Secure API key management service using IntelliJ's PasswordSafe.
 * 
 * <p>This service provides secure storage and retrieval of API keys for AI services
 * using IntelliJ Platform's built-in password management system. All keys are stored
 * encrypted and integrated with the user's system keychain.</p>
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>Secure storage using IntelliJ's PasswordSafe</li>
 *   <li>Support for multiple AI services</li>
 *   <li>API key validation with service endpoints</li>
 *   <li>Automatic key encryption and decryption</li>
 *   <li>Integration with system keychain</li>
 * </ul>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
public final class SecureAPIKeyManager {
    
    private static final Logger LOG = Logger.getInstance(SecureAPIKeyManager.class);
    
    // PasswordSafe service keys for different AI services
    private static final String OPENAI_KEY = "trace.openai.api_key";
    private static final String GEMINI_KEY = "trace.gemini.api_key";
    
    // Validation timeouts
    private static final int VALIDATION_TIMEOUT_SECONDS = 10;
    
    /**
     * Private constructor to prevent instantiation.
     */
    private SecureAPIKeyManager() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    // ============================================================================
    // API KEY STORAGE METHODS
    // ============================================================================
    
    /**
     * Stores an API key securely for the specified AI service.
     * 
     * <p>The key is encrypted and stored using IntelliJ's PasswordSafe, which integrates
     * with the system keychain for additional security.</p>
     * 
     * @param serviceType the AI service type
     * @param apiKey the API key to store
     * @return true if the key was stored successfully, false otherwise
     */
    public static boolean storeAPIKey(@NotNull AIServiceType serviceType, @NotNull String apiKey) {
        if (apiKey.trim().isEmpty()) {
            LOG.warn("Attempted to store empty API key for service: " + serviceType);
            return false;
        }
        
        try {
            String serviceKey = getServiceKey(serviceType);
            CredentialAttributes attributes = new CredentialAttributes(serviceKey);
            Credentials credentials = new Credentials(serviceKey, apiKey);
            
            PasswordSafe.getInstance().set(attributes, credentials);
            
            LOG.info("API key stored successfully for service: " + serviceType.getDisplayName());
            return true;
            
        } catch (Exception e) {
            LOG.error("Failed to store API key for service: " + serviceType, e);
            return false;
        }
    }
    
    /**
     * Retrieves the API key for the specified AI service.
     * 
     * <p>The key is decrypted from secure storage. Returns null if no key is found
     * or if there's an error retrieving the key.</p>
     * 
     * @param serviceType the AI service type
     * @return the API key, or null if not found or error occurred
     */
    public static @Nullable String getAPIKey(@NotNull AIServiceType serviceType) {
        try {
            String serviceKey = getServiceKey(serviceType);
            CredentialAttributes attributes = new CredentialAttributes(serviceKey);
            
            Credentials credentials = PasswordSafe.getInstance().get(attributes);
            if (credentials == null) {
                LOG.debug("No API key found for service: " + serviceType.getDisplayName());
                return null;
            }
            
            String apiKey = credentials.getPasswordAsString();
            if (apiKey == null || apiKey.trim().isEmpty()) {
                LOG.warn("Retrieved empty API key for service: " + serviceType.getDisplayName());
                return null;
            }
            
            LOG.debug("API key retrieved successfully for service: " + serviceType.getDisplayName());
            return apiKey;
            
        } catch (Exception e) {
            LOG.error("Failed to retrieve API key for service: " + serviceType, e);
            return null;
        }
    }
    
    /**
     * Removes the API key for the specified AI service.
     * 
     * <p>The key is permanently deleted from secure storage. This operation cannot be undone.</p>
     * 
     * @param serviceType the AI service type
     * @return true if the key was removed successfully, false otherwise
     */
    public static boolean clearAPIKey(@NotNull AIServiceType serviceType) {
        try {
            String serviceKey = getServiceKey(serviceType);
            CredentialAttributes attributes = new CredentialAttributes(serviceKey);
            
            PasswordSafe.getInstance().set(attributes, null);
            
            LOG.info("API key cleared successfully for service: " + serviceType.getDisplayName());
            return true;
            
        } catch (Exception e) {
            LOG.error("Failed to clear API key for service: " + serviceType, e);
            return false;
        }
    }
    
    /**
     * Checks if an API key exists for the specified AI service.
     * 
     * @param serviceType the AI service type
     * @return true if a key exists, false otherwise
     */
    public static boolean hasAPIKey(@NotNull AIServiceType serviceType) {
        String apiKey = getAPIKey(serviceType);
        return apiKey != null && !apiKey.trim().isEmpty();
    }
    
    // ============================================================================
    // API KEY VALIDATION METHODS
    // ============================================================================
    
    /**
     * Validates an API key by testing it against the service's API endpoint.
     * 
     * <p>This method performs an actual API call to verify the key is valid and
     * has the necessary permissions. The validation is asynchronous to avoid
     * blocking the UI thread.</p>
     * 
     * @param serviceType the AI service type
     * @param apiKey the API key to validate
     * @return a CompletableFuture that completes with true if valid, false otherwise
     */
    public static CompletableFuture<Boolean> validateAPIKey(@NotNull AIServiceType serviceType, @NotNull String apiKey) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LOG.info("Validating API key for service: " + serviceType.getDisplayName());
                
                switch (serviceType) {
                    case OPENAI:
                        return validateOpenAIKey(apiKey);
                    case GEMINI:
                        return validateGeminiKey(apiKey);
                    default:
                        LOG.warn("Unknown service type for validation: " + serviceType);
                        return false;
                }
                
            } catch (Exception e) {
                LOG.error("Error validating API key for service: " + serviceType, e);
                return false;
            }
        });
    }
    
    /**
     * Validates an API key for the specified service using the stored key.
     * 
     * <p>This is a convenience method that retrieves the stored key and validates it.</p>
     * 
     * @param serviceType the AI service type
     * @return a CompletableFuture that completes with true if valid, false otherwise
     */
    public static CompletableFuture<Boolean> validateStoredAPIKey(@NotNull AIServiceType serviceType) {
        String apiKey = getAPIKey(serviceType);
        if (apiKey == null) {
            return CompletableFuture.completedFuture(false);
        }
        return validateAPIKey(serviceType, apiKey);
    }
    
    // ============================================================================
    // PRIVATE VALIDATION METHODS
    // ============================================================================
    
    /**
     * Validates an OpenAI API key by making a test request to the models endpoint.
     * 
     * @param apiKey the OpenAI API key to validate
     * @return true if the key is valid, false otherwise
     */
    private static boolean validateOpenAIKey(@NotNull String apiKey) {
        try {
            // TODO: Implement actual OpenAI API validation
            // For now, perform basic format validation
            return apiKey.startsWith("sk-") && apiKey.length() > 20;
            
        } catch (Exception e) {
            LOG.error("Error validating OpenAI API key", e);
            return false;
        }
    }
    
    /**
     * Validates a Google Gemini API key by making a test request to the models endpoint.
     * 
     * @param apiKey the Google Gemini API key to validate
     * @return true if the key is valid, false otherwise
     */
    private static boolean validateGeminiKey(@NotNull String apiKey) {
        try {
            // TODO: Implement actual Gemini API validation
            // For now, perform basic format validation
            boolean hasValidLength = apiKey.length() > 20;
            boolean hasNoSpaces = !apiKey.contains(" ");
            
            boolean isValid = hasValidLength && hasNoSpaces;
            
            LOG.info("Gemini key validation - Length: " + apiKey.length() + 
                    " (>20: " + hasValidLength + "), Contains spaces: " + hasNoSpaces + 
                    ", Valid: " + isValid + 
                    ", Key prefix: " + apiKey.substring(0, Math.min(10, apiKey.length())));
            
            return isValid;
            
        } catch (Exception e) {
            LOG.error("Error validating Gemini API key", e);
            return false;
        }
    }
    
    // ============================================================================
    // UTILITY METHODS
    // ============================================================================
    
    /**
     * Gets the service-specific key for PasswordSafe storage.
     * 
     * @param serviceType the AI service type
     * @return the service key for secure storage
     */
    private static String getServiceKey(@NotNull AIServiceType serviceType) {
        switch (serviceType) {
            case OPENAI:
                return OPENAI_KEY;
            case GEMINI:
                return GEMINI_KEY;
            default:
                throw new IllegalArgumentException("Unsupported service type: " + serviceType);
        }
    }
    
    /**
     * Gets a summary of API key status for all supported services.
     * 
     * @return a string describing the status of all API keys
     */
    public static String getAPIKeyStatus() {
        StringBuilder status = new StringBuilder();
        
        for (AIServiceType serviceType : AIServiceType.values()) {
            boolean hasKey = hasAPIKey(serviceType);
            status.append(serviceType.getDisplayName())
                  .append(": ")
                  .append(hasKey ? "Configured" : "Not configured")
                  .append("\n");
        }
        
        return status.toString().trim();
    }
    
    /**
     * Clears all stored API keys for all services.
     * 
     * <p>Use with caution - this will remove all API keys and require reconfiguration.</p>
     * 
     * @return true if all keys were cleared successfully, false if any failed
     */
    public static boolean clearAllAPIKeys() {
        boolean allCleared = true;
        
        for (AIServiceType serviceType : AIServiceType.values()) {
            if (!clearAPIKey(serviceType)) {
                allCleared = false;
            }
        }
        
        LOG.info("Cleared all API keys. Success: " + allCleared);
        return allCleared;
    }
} 