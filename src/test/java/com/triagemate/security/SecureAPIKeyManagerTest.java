package com.triagemate.security;

import com.triagemate.settings.AIServiceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SecureAPIKeyManager validation logic.
 * 
 * <p>These tests focus on API key validation and error handling without requiring
 * the full IntelliJ Platform environment. Storage operations are not tested here
 * as they require PasswordSafe integration.</p>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
public class SecureAPIKeyManagerTest {
    
    private static final String VALID_OPENAI_KEY = "sk-test123456789012345678901234567890";
    private static final String VALID_GEMINI_KEY = "AIzaSyC123456789012345678901234567890";
    private static final String INVALID_KEY = "invalid-key";
    private static final String EMPTY_KEY = "";
    
    // ============================================================================
    // API KEY VALIDATION TESTS
    // ============================================================================
    
    @Test
    public void shouldValidateOpenAIKeyFormat() throws ExecutionException, InterruptedException, TimeoutException {
        CompletableFuture<Boolean> future = SecureAPIKeyManager.validateAPIKey(AIServiceType.OPENAI, VALID_OPENAI_KEY);
        Boolean result = future.get(5, TimeUnit.SECONDS);
        
        assertTrue(result, "Should validate OpenAI key with correct format");
    }
    
    @Test
    public void shouldRejectInvalidOpenAIKeyFormat() throws ExecutionException, InterruptedException, TimeoutException {
        CompletableFuture<Boolean> future = SecureAPIKeyManager.validateAPIKey(AIServiceType.OPENAI, INVALID_KEY);
        Boolean result = future.get(5, TimeUnit.SECONDS);
        
        assertFalse(result, "Should reject OpenAI key with incorrect format");
    }
    
    @Test
    public void shouldValidateGeminiKeyFormat() throws ExecutionException, InterruptedException, TimeoutException {
        CompletableFuture<Boolean> future = SecureAPIKeyManager.validateAPIKey(AIServiceType.GEMINI, VALID_GEMINI_KEY);
        Boolean result = future.get(5, TimeUnit.SECONDS);
        
        assertTrue(result, "Should validate Gemini key with correct format");
    }
    
    @Test
    public void shouldRejectInvalidGeminiKeyFormat() throws ExecutionException, InterruptedException, TimeoutException {
        CompletableFuture<Boolean> future = SecureAPIKeyManager.validateAPIKey(AIServiceType.GEMINI, INVALID_KEY);
        Boolean result = future.get(5, TimeUnit.SECONDS);
        
        assertFalse(result, "Should reject Gemini key with incorrect format");
    }
    
    @Test
    public void shouldRejectEmptyAPIKeyForValidation() throws ExecutionException, InterruptedException, TimeoutException {
        assertFalse(SecureAPIKeyManager.validateAPIKey(AIServiceType.OPENAI, EMPTY_KEY)
                .get(5, TimeUnit.SECONDS), "Should reject empty API key for validation");
        
        assertFalse(SecureAPIKeyManager.validateAPIKey(AIServiceType.OPENAI, "   ")
                .get(5, TimeUnit.SECONDS), "Should reject whitespace-only API key for validation");
    }
    
    @Test
    public void shouldRejectNullAPIKeyForValidation() {
        assertThrows(IllegalArgumentException.class, () -> {
            SecureAPIKeyManager.validateAPIKey(AIServiceType.OPENAI, null);
        }, "Should throw exception for null API key");
    }
    
    @Test
    public void shouldRejectNullServiceTypeForValidation() {
        assertThrows(IllegalArgumentException.class, () -> {
            SecureAPIKeyManager.validateAPIKey(null, VALID_OPENAI_KEY);
        }, "Should throw exception for null service type");
    }
    
    // ============================================================================
    // UTILITY METHOD TESTS
    // ============================================================================
    
    @Test
    public void shouldHandleNullServiceTypeForStoredValidation() {
        assertThrows(IllegalArgumentException.class, () -> {
            SecureAPIKeyManager.validateStoredAPIKey(null);
        }, "Should throw exception for null service type");
    }
    
    // ============================================================================
    // ERROR HANDLING TESTS
    // ============================================================================
    
    @Test
    public void shouldHandleValidationTimeout() {
        // This test verifies that validation doesn't hang indefinitely
        CompletableFuture<Boolean> future = SecureAPIKeyManager.validateAPIKey(AIServiceType.OPENAI, VALID_OPENAI_KEY);
        
        // Should complete within reasonable time
        assertDoesNotThrow(() -> {
            future.get(10, TimeUnit.SECONDS);
        }, "Validation should complete within timeout");
    }
    
    @Test
    public void shouldHandleConcurrentValidation() throws ExecutionException, InterruptedException, TimeoutException {
        // Test multiple concurrent validations
        CompletableFuture<Boolean> future1 = SecureAPIKeyManager.validateAPIKey(AIServiceType.OPENAI, VALID_OPENAI_KEY);
        CompletableFuture<Boolean> future2 = SecureAPIKeyManager.validateAPIKey(AIServiceType.GEMINI, VALID_GEMINI_KEY);
        
        Boolean result1 = future1.get(5, TimeUnit.SECONDS);
        Boolean result2 = future2.get(5, TimeUnit.SECONDS);
        
        assertTrue(result1, "First validation should succeed");
        assertTrue(result2, "Second validation should succeed");
    }
} 