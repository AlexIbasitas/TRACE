package com.trace.ai.services;

import com.intellij.openapi.diagnostic.Logger;
import com.trace.ai.services.providers.AIServiceProvider;
import com.trace.ai.services.providers.GeminiProvider;
import com.trace.ai.services.providers.OpenAIProvider;
import com.trace.ai.configuration.AIServiceType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Factory for creating and managing AI service providers.
 * 
 * <p>This factory implements the Factory pattern to provide a centralized
 * way to create and manage AI service providers. It enables clean separation
 * of concerns and easy extensibility for new AI services.</p>
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>Provider registration and retrieval</li>
 *   <li>Shared HTTP client for efficient resource usage</li>
 *   <li>Thread-safe provider management</li>
 *   <li>Automatic provider initialization</li>
 * </ul>
 * 
 * <p>Usage:</p>
 * <pre>
 * // Get a provider for a specific service
 * AIServiceProvider provider = AIServiceFactory.getProvider(AIServiceType.OPENAI);
 * 
 * // Register a custom provider
 * AIServiceFactory.registerProvider(AIServiceType.CUSTOM, new CustomProvider());
 * </pre>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
public final class AIServiceFactory {
    
    private static final Logger LOG = Logger.getInstance(AIServiceFactory.class);
    
    // Provider registry (instance-based to allow proper cleanup)
    private final Map<AIServiceType, AIServiceProvider> providers = new ConcurrentHashMap<>();
    
    // Shared HTTP client for efficient resource usage (lazy initialized)
    private volatile HttpClient httpClient;
    
    // Singleton instance for backward compatibility
    private static volatile AIServiceFactory instance;
    
    /**
     * Private constructor for singleton pattern.
     */
    private AIServiceFactory() {
        initializeDefaultProviders();
        instance = this;
    }
    
    /**
     * Gets the singleton instance.
     * 
     * @return the singleton instance
     */
    public static AIServiceFactory getInstance() {
        if (instance == null) {
            synchronized (AIServiceFactory.class) {
                if (instance == null) {
                    instance = new AIServiceFactory();
                }
            }
        }
        return instance;
    }
    
    /**
     * Gets a provider for the specified service type.
     * 
     * @param serviceType the service type
     * @return the provider, or null if not found
     */
    @Nullable
    public AIServiceProvider getProvider(@NotNull AIServiceType serviceType) {
        AIServiceProvider provider = providers.get(serviceType);
        if (provider == null) {
            LOG.warn("No provider found for service type: " + serviceType);
        }
        return provider;
    }
    
    /**
     * Static wrapper for backward compatibility.
     * 
     * @param serviceType the service type
     * @return the provider, or null if not found
     */
    @Nullable
    public static AIServiceProvider getProviderStatic(@NotNull AIServiceType serviceType) {
        return getInstance().getProvider(serviceType);
    }
    
    /**
     * Registers a provider for a service type.
     * 
     * <p>This method allows custom providers to be registered at runtime.
     * If a provider already exists for the service type, it will be replaced.</p>
     * 
     * @param serviceType the service type
     * @param provider the provider implementation
     */
    public void registerProvider(@NotNull AIServiceType serviceType, 
                                      @NotNull AIServiceProvider provider) {
        if (serviceType == null) {
            throw new IllegalArgumentException("Service type cannot be null");
        }
        if (provider == null) {
            throw new IllegalArgumentException("Provider cannot be null");
        }
        
        providers.put(serviceType, provider);
        LOG.info("Registered provider for service type: " + serviceType);
    }
    
    /**
     * Static wrapper for backward compatibility.
     */
    public static void registerProviderStatic(@NotNull AIServiceType serviceType, 
                                      @NotNull AIServiceProvider provider) {
        getInstance().registerProvider(serviceType, provider);
    }
    
    /**
     * Unregisters a provider for a service type.
     * 
     * @param serviceType the service type to unregister
     * @return true if provider was removed, false if not found
     */
    public boolean unregisterProvider(@NotNull AIServiceType serviceType) {
        AIServiceProvider removed = providers.remove(serviceType);
        if (removed != null) {
            LOG.info("Unregistered provider for service type: " + serviceType);
            return true;
        }
        return false;
    }
    
    public static boolean unregisterProviderStatic(@NotNull AIServiceType serviceType) {
        return getInstance().unregisterProvider(serviceType);
    }
    
    /**
     * Gets all registered service types.
     * 
     * @return array of registered service types
     */
    public AIServiceType[] getRegisteredServiceTypes() {
        return providers.keySet().toArray(new AIServiceType[0]);
    }
    
    public static AIServiceType[] getRegisteredServiceTypesStatic() {
        return getInstance().getRegisteredServiceTypes();
    }
    
    /**
     * Checks if a provider is registered for the given service type.
     * 
     * @param serviceType the service type to check
     * @return true if a provider is registered, false otherwise
     */
    public boolean hasProvider(@NotNull AIServiceType serviceType) {
        return providers.containsKey(serviceType);
    }
    
    public static boolean hasProviderStatic(@NotNull AIServiceType serviceType) {
        return getInstance().hasProvider(serviceType);
    }
    
    /**
     * Gets the number of registered providers.
     * 
     * @return the number of registered providers
     */
    public int getProviderCount() {
        return providers.size();
    }
    
    public static int getProviderCountStatic() {
        return getInstance().getProviderCount();
    }
    
    /**
     * Gets the shared HTTP client used by all providers.
     * 
     * <p>This method provides access to the shared HTTP client for providers
     * that need custom HTTP configuration. Uses lazy initialization with
     * double-checked locking for thread safety.</p>
     * 
     * @return the shared HTTP client
     */
    public HttpClient getSharedHttpClient() {
        if (httpClient == null) {
            synchronized (this) {
                if (httpClient == null) {
                    httpClient = createSharedHttpClient();
                    LOG.debug("Created shared HTTP client with lazy initialization");
                }
            }
        }
        return httpClient;
    }
    
    public static HttpClient getSharedHttpClientStatic() {
        return getInstance().getSharedHttpClient();
    }
    
    /**
     * Initializes the default providers.
     * 
     * <p>This method registers the built-in providers for OpenAI and Google Gemini.
     * It's called during instance initialization.</p>
     */
    private void initializeDefaultProviders() {
        try {
            // Register OpenAI provider
            registerProvider(AIServiceType.OPENAI, new OpenAIProvider(getSharedHttpClient()));
            
            // Register Google Gemini provider
            registerProvider(AIServiceType.GEMINI, new GeminiProvider(getSharedHttpClient()));
            
            LOG.info("Initialized " + providers.size() + " default AI service providers");
            
        } catch (Exception e) {
            LOG.error("Failed to initialize default providers", e);
        }
    }
    
    /**
     * Creates the shared HTTP client with optimal configuration.
     * 
     * @return the configured HTTP client
     */
    private HttpClient createSharedHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }
    
    /**
     * Cleans up instance resources to prevent memory leaks and ensure consistent startup behavior.
     * 
     * <p>This method should be called during plugin shutdown or when resources need to be reset.
     * It clears the providers map and closes the shared HTTP client to prevent memory leaks.</p>
     */
    public void cleanup() {
        LOG.info("Starting cleanup of AIServiceFactory instance resources");
        
        int resourcesCleaned = providers.size();
        
        try {
            // Clear providers first
            providers.clear();
            
            // Clear the shared HTTP client reference
            synchronized (this) {
                if (httpClient != null) {
                    try {
                        httpClient = null;
                        LOG.info("Cleared shared HTTP client reference - JVM will handle cleanup");
                    } catch (Exception e) {
                        LOG.warn("Error during HTTP client cleanup: " + e.getMessage(), e);
                    }
                }
            }
            
            LOG.info("AIServiceFactory cleanup completed - cleared " + resourcesCleaned + " AI service providers");
        } catch (Exception e) {
            LOG.error("Error during AIServiceFactory cleanup: " + e.getMessage(), e);
        }
    }
    
    /**
     * Static wrapper for cleanup.
     */
    public static void cleanupStatic() {
        if (instance != null) {
            instance.cleanup();
            instance = null;
        }
    }
} 