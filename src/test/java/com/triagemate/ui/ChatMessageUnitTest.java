package com.triagemate.ui;

import com.triagemate.models.FailureInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ChatMessage class.
 * 
 * <p>These tests verify the immutable data class functionality, including
 * constructor validation, utility methods, and object equality behavior.</p>
 * 
 * <p>Test patterns follow standard Java unit testing best practices:
 * <ul>
 *   <li>Test constructor validation and edge cases</li>
 *   <li>Test utility methods for role checking</li>
 *   <li>Test equals, hashCode, and toString methods</li>
 *   <li>Test immutable behavior</li>
 *   <li>Test null handling and validation</li>
 * </ul></p>
 */
@DisplayName("ChatMessage Unit Tests")
class ChatMessageUnitTest {

    private ChatMessage userMessage;
    private ChatMessage aiMessage;
    private ChatMessage aiMessageWithThinking;
    private ChatMessage aiMessageWithFailureInfo;

    @BeforeEach
    void setUp() {
        userMessage = new ChatMessage(
            ChatMessage.Role.USER,
            "Hello, can you help me with this test failure?",
            System.currentTimeMillis(),
            null,
            null
        );

        aiMessage = new ChatMessage(
            ChatMessage.Role.AI,
            "I can help you analyze this test failure.",
            System.currentTimeMillis(),
            null,
            null
        );

        aiMessageWithThinking = new ChatMessage(
            ChatMessage.Role.AI,
            "Here's my analysis:",
            System.currentTimeMillis(),
            "This appears to be a Selenium element not found error. The test is trying to locate an element that doesn't exist on the page.",
            null
        );

        FailureInfo failureInfo = new FailureInfo.Builder()
            .withScenarioName("Login Test")
            .withFailedStepText("When user enters credentials")
            .withErrorMessage("Element not found")
            .build();

        aiMessageWithFailureInfo = new ChatMessage(
            ChatMessage.Role.AI,
            "Analysis of your test failure:",
            System.currentTimeMillis(),
            "The test failed because the login form element could not be found.",
            failureInfo
        );
    }

    @Nested
    @DisplayName("Constructor and Validation")
    class ConstructorAndValidation {

        @Test
        @DisplayName("should create user message successfully")
        void shouldCreateUserMessageSuccessfully() {
            // Act
            ChatMessage message = new ChatMessage(
                ChatMessage.Role.USER,
                "Test message",
                System.currentTimeMillis(),
                null,
                null
            );

            // Assert
            assertNotNull(message);
            assertEquals(ChatMessage.Role.USER, message.getRole());
            assertEquals("Test message", message.getText());
            assertNull(message.getAiThinking());
            assertNull(message.getFailureInfo());
        }

        @Test
        @DisplayName("should create AI message with thinking successfully")
        void shouldCreateAiMessageWithThinkingSuccessfully() {
            // Act
            ChatMessage message = new ChatMessage(
                ChatMessage.Role.AI,
                "Summary",
                System.currentTimeMillis(),
                "Detailed analysis here",
                null
            );

            // Assert
            assertNotNull(message);
            assertEquals(ChatMessage.Role.AI, message.getRole());
            assertEquals("Summary", message.getText());
            assertEquals("Detailed analysis here", message.getAiThinking());
            assertNull(message.getFailureInfo());
        }

        @Test
        @DisplayName("should handle null text gracefully")
        void shouldHandleNullTextGracefully() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> 
                new ChatMessage(ChatMessage.Role.USER, null, System.currentTimeMillis(), null, null),
                "Should throw IllegalArgumentException for null text"
            );
        }

        @Test
        @DisplayName("should handle null role gracefully")
        void shouldHandleNullRoleGracefully() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> 
                new ChatMessage(null, "Test message", System.currentTimeMillis(), null, null),
                "Should throw IllegalArgumentException for null role"
            );
        }
    }

    @Nested
    @DisplayName("Utility Methods")
    class UtilityMethods {

        @Test
        @DisplayName("should correctly identify user messages")
        void shouldCorrectlyIdentifyUserMessages() {
            // Assert
            assertTrue(userMessage.isFromUser());
            assertFalse(userMessage.isFromAI());
        }

        @Test
        @DisplayName("should correctly identify AI messages")
        void shouldCorrectlyIdentifyAiMessages() {
            // Assert
            assertTrue(aiMessage.isFromAI());
            assertFalse(aiMessage.isFromUser());
        }

        @Test
        @DisplayName("should correctly identify AI thinking presence")
        void shouldCorrectlyIdentifyAiThinkingPresence() {
            // Assert
            assertFalse(userMessage.hasAiThinking());
            assertFalse(aiMessage.hasAiThinking());
            assertTrue(aiMessageWithThinking.hasAiThinking());
        }

        @Test
        @DisplayName("should correctly identify failure info presence")
        void shouldCorrectlyIdentifyFailureInfoPresence() {
            // Assert
            assertFalse(userMessage.hasFailureInfo());
            assertFalse(aiMessage.hasFailureInfo());
            assertFalse(aiMessageWithThinking.hasFailureInfo());
            assertTrue(aiMessageWithFailureInfo.hasFailureInfo());
        }
    }

    @Nested
    @DisplayName("Object Equality")
    class ObjectEquality {

        @Test
        @DisplayName("should be equal to itself")
        void shouldBeEqualToItself() {
            // Assert
            assertEquals(userMessage, userMessage);
            assertEquals(aiMessage, aiMessage);
        }

        @Test
        @DisplayName("should be equal to identical message")
        void shouldBeEqualToIdenticalMessage() {
            // Arrange
            ChatMessage identicalUserMessage = new ChatMessage(
                ChatMessage.Role.USER,
                "Hello, can you help me with this test failure?",
                userMessage.getTimestamp(),
                null,
                null
            );

            // Assert
            assertEquals(userMessage, identicalUserMessage);
            assertEquals(identicalUserMessage, userMessage);
        }

        @Test
        @DisplayName("should not be equal to different message")
        void shouldNotBeEqualToDifferentMessage() {
            // Arrange
            ChatMessage differentMessage = new ChatMessage(
                ChatMessage.Role.AI,
                "Different text",
                System.currentTimeMillis(),
                null,
                null
            );

            // Assert
            assertNotEquals(userMessage, differentMessage);
            assertNotEquals(differentMessage, userMessage);
        }

        @Test
        @DisplayName("should have consistent hashCode")
        void shouldHaveConsistentHashCode() {
            // Arrange
            ChatMessage message1 = new ChatMessage(
                ChatMessage.Role.USER,
                "Test message",
                1234567890L,
                null,
                null
            );
            ChatMessage message2 = new ChatMessage(
                ChatMessage.Role.USER,
                "Test message",
                1234567890L,
                null,
                null
            );

            // Assert
            assertEquals(message1.hashCode(), message2.hashCode());
        }
    }
}
