package com.triagemate.services;

import com.intellij.openapi.diagnostic.Logger;
import com.triagemate.settings.AIServiceType;
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
    
    // Provider registry
    private static final Map<AIServiceType, AIServiceProvider> providers = new ConcurrentHashMap<>();
    
    // Shared HTTP client for efficient resource usage
    private static final HttpClient sharedHttpClient = createSharedHttpClient();
    
    // Static initialization block to register default providers
    static {
        initializeDefaultProviders();
    }
    
    /**
     * Private constructor to prevent instantiation.
     */
    private AIServiceFactory() {
        throw new UnsupportedOperationException("Factory class cannot be instantiated");
    }
    
    /**
     * Gets a provider for the specified service type.
     * 
     * @param serviceType the service type
     * @return the provider, or null if not found
     */
    @Nullable
    public static AIServiceProvider getProvider(@NotNull AIServiceType serviceType) {
        AIServiceProvider provider = providers.get(serviceType);
        if (provider == null) {
            LOG.warn("No provider found for service type: " + serviceType);
        }
        return provider;
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
    public static void registerProvider(@NotNull AIServiceType serviceType, 
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
     * Unregisters a provider for a service type.
     * 
     * @param serviceType the service type to unregister
     * @return true if provider was removed, false if not found
     */
    public static boolean unregisterProvider(@NotNull AIServiceType serviceType) {
        AIServiceProvider removed = providers.remove(serviceType);
        if (removed != null) {
            LOG.info("Unregistered provider for service type: " + serviceType);
            return true;
        }
        return false;
    }
    
    /**
     * Gets all registered service types.
     * 
     * @return array of registered service types
     */
    public static AIServiceType[] getRegisteredServiceTypes() {
        return providers.keySet().toArray(new AIServiceType[0]);
    }
    
    /**
     * Checks if a provider is registered for the given service type.
     * 
     * @param serviceType the service type to check
     * @return true if a provider is registered, false otherwise
     */
    public static boolean hasProvider(@NotNull AIServiceType serviceType) {
        return providers.containsKey(serviceType);
    }
    
    /**
     * Gets the number of registered providers.
     * 
     * @return the number of registered providers
     */
    public static int getProviderCount() {
        return providers.size();
    }
    
    /**
     * Gets the shared HTTP client used by all providers.
     * 
     * <p>This method provides access to the shared HTTP client for providers
     * that need custom HTTP configuration.</p>
     * 
     * @return the shared HTTP client
     */
    public static HttpClient getSharedHttpClient() {
        return sharedHttpClient;
    }
    
    /**
     * Initializes the default providers.
     * 
     * <p>This method registers the built-in providers for OpenAI and Google Gemini.
     * It's called during static initialization.</p>
     */
    private static void initializeDefaultProviders() {
        try {
            // Register OpenAI provider
            registerProvider(AIServiceType.OPENAI, new OpenAIProvider(sharedHttpClient));
            
            // Register Google Gemini provider
            registerProvider(AIServiceType.GEMINI, new GeminiProvider(sharedHttpClient));
            
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
    private static HttpClient createSharedHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }
} 