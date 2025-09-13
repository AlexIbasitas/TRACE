package com.trace.security;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.diagnostic.Logger;
import com.trace.ai.configuration.AIServiceType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Secure API key management service using IntelliJ's PasswordSafe.
 * 
 * <p>This service provides secure storage and retrieval of API keys for AI services
 * using IntelliJ Platform's built-in password management system. All keys are stored
 * encrypted and integrated with the user's system keychain.</p>
 * 
 * <p>The service supports multiple AI providers and provides validation capabilities
 * to ensure stored keys are valid before use. All operations are logged for security
 * auditing purposes.</p>
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
     * This is a utility class and should not be instantiated.
     */
    private SecureAPIKeyManager() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    // --- API key storage methods ---
    
    /**
     * Stores an API key securely for the specified AI service.
     * 
     * <p>The key is encrypted and stored using IntelliJ's PasswordSafe, which integrates
     * with the system keychain for additional security. Empty or null keys are rejected
     * and logged as warnings.</p>
     * 
     * @param serviceType the AI service type
     * @param apiKey the API key to store
     * @return true if the key was stored successfully, false otherwise
     */
    public static boolean storeAPIKey(@NotNull AIServiceType serviceType, @NotNull String apiKey) {
        if (apiKey.trim().isEmpty()) {
            LOG.warn("Attempted to store empty API key for service: " + serviceType.getDisplayName());
            return false;
        }
        
        try {
            PasswordSafe passwordSafe = PasswordSafe.getInstance();
            String serviceKey = getServiceKey(serviceType);
            CredentialAttributes attributes = createCredentialAttributes(serviceKey);
            Credentials credentials = new Credentials(null, apiKey);
            
            passwordSafe.set(attributes, credentials);
            
            LOG.info("API key stored for service: " + serviceType.getDisplayName());
            return true;
            
        } catch (Exception e) {
            LOG.error("Failed to store API key for service: " + serviceType.getDisplayName() + 
                     " (IDE: " + ApplicationInfo.getInstance().getVersionName() + ")", e);
            return false;
        }
    }
    
    /**
     * Retrieves the API key for the specified AI service.
     * 
     * <p>The key is decrypted from secure storage. Returns null if no key is found
     * or if there's an error retrieving the key. Empty keys are treated as missing
     * and logged as warnings.</p>
     * 
     * @param serviceType the AI service type
     * @return the API key, or null if not found or error occurred
     */
    public static @Nullable String getAPIKey(@NotNull AIServiceType serviceType) {
        try {
            String serviceKey = getServiceKey(serviceType);
            CredentialAttributes attributes = createCredentialAttributes(serviceKey);
            String apiKey = PasswordSafe.getInstance().getPassword(attributes);
            
            if (apiKey == null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("No API key found for service: " + serviceType.getDisplayName());
                }
                return null;
            }
            
            if (apiKey.trim().isEmpty()) {
                LOG.warn("Retrieved empty API key for service: " + serviceType.getDisplayName());
                return null;
            }
            
            if (LOG.isDebugEnabled()) {
                LOG.debug("API key retrieved for service: " + serviceType.getDisplayName());
            }
            return apiKey;
            
        } catch (Exception e) {
            LOG.warn("Failed to retrieve API key for service: " + serviceType.getDisplayName(), e);
            return null;
        }
    }
    
    /**
     * Removes the API key for the specified AI service.
     * 
     * <p>The key is permanently deleted from secure storage. This operation cannot be undone.
     * The method logs successful removals and any errors that occur during the process.</p>
     * 
     * @param serviceType the AI service type
     * @return true if the key was removed successfully, false otherwise
     */
    public static boolean clearAPIKey(@NotNull AIServiceType serviceType) {
        try {
            String serviceKey = getServiceKey(serviceType);
            CredentialAttributes attributes = createCredentialAttributes(serviceKey);
            
            PasswordSafe.getInstance().set(attributes, null);
            
            LOG.info("API key cleared for service: " + serviceType.getDisplayName());
            return true;
            
        } catch (Exception e) {
            LOG.warn("Failed to clear API key for service: " + serviceType.getDisplayName(), e);
            return false;
        }
    }
    
    /**
     * Checks if an API key exists for the specified AI service.
     * 
     * <p>This method verifies that a valid, non-empty API key exists for the service.
     * It uses the getAPIKey method internally to perform the check.</p>
     * 
     * @param serviceType the AI service type
     * @return true if a valid key exists, false otherwise
     */
    public static boolean hasAPIKey(@NotNull AIServiceType serviceType) {
        String apiKey = getAPIKey(serviceType);
        return apiKey != null && !apiKey.trim().isEmpty();
    }
    
    // --- Utility methods ---
    
    /**
     * Creates CredentialAttributes using the official IntelliJ pattern.
     * 
     * <p>This method uses CredentialAttributesKt.generateServiceName to ensure
     * consistent service key formatting across different environments.</p>
     * 
     * @param key the service-specific key
     * @return properly formatted CredentialAttributes
     */
    private static CredentialAttributes createCredentialAttributes(@NotNull String key) {
        return new CredentialAttributes(
            CredentialAttributesKt.generateServiceName("TRACE", key)
        );
    }
    
    /**
     * Gets the service-specific key for PasswordSafe storage.
     * 
     * <p>This method maps service types to their corresponding storage keys used
     * by the PasswordSafe system. It throws an exception for unknown service types.</p>
     * 
     * @param serviceType the AI service type
     * @return the service key for secure storage
     * @throws IllegalArgumentException if the service type is not supported
     */
    private static String getServiceKey(@NotNull AIServiceType serviceType) {
        switch (serviceType) {
            case OPENAI:
                return OPENAI_KEY;
            case GEMINI:
                return GEMINI_KEY;
            default:
                throw new IllegalArgumentException("Unknown service type: " + serviceType);
        }
    }
    
    /**
     * Gets a summary of API key status for all supported services.
     * 
     * <p>This method checks the status of all configured AI services and returns
     * a human-readable summary of which services have API keys configured.</p>
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
     * <p>This method removes all API keys from secure storage. Use with caution
     * as this will require reconfiguration of all AI services. The method logs
     * the overall success or failure of the operation.</p>
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
        
        LOG.info("Cleared all API keys - Success: " + allCleared);
        return allCleared;
    }
} 