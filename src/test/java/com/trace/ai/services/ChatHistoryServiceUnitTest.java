package com.trace.ai.services;

import com.trace.ai.models.UserQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("Chat History Service Unit Tests")
class ChatHistoryServiceUnitTest {
    
    private ChatHistoryService chatHistoryService;
    
    @BeforeEach
    void setUp() {
        chatHistoryService = new ChatHistoryService();
    }
    
    @Nested
    @DisplayName("Failure Context Management")
    class FailureContextManagement {
        
        @Test
        @DisplayName("should set failure context successfully when valid context provided")
        void shouldSetFailureContextSuccessfully_whenValidContextProvided() {
            // Arrange
            String failureContext = "Test failure: AssertionError expected true but was false";
            
            // Act
            chatHistoryService.setFailureContext(failureContext);
            
            // Assert
            assertThat(chatHistoryService.hasFailureContext()).isTrue();
        }
        
        @Test
        @DisplayName("should throw exception when null failure context provided")
        void shouldThrowException_whenNullFailureContextProvided() {
            // Act & Assert
            assertThatThrownBy(() -> chatHistoryService.setFailureContext(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Argument for @NotNull parameter 'failureContext'");
        }
        
        @Test
        @DisplayName("should throw exception when empty failure context provided")
        void shouldThrowException_whenEmptyFailureContextProvided() {
            // Act & Assert
            assertThatThrownBy(() -> chatHistoryService.setFailureContext(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Failure context cannot be null or empty");
        }
        
        @Test
        @DisplayName("should throw exception when whitespace-only failure context provided")
        void shouldThrowException_whenWhitespaceOnlyFailureContextProvided() {
            // Act & Assert
            assertThatThrownBy(() -> chatHistoryService.setFailureContext("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Failure context cannot be null or empty");
        }
        
        @Test
        @DisplayName("should trim failure context when provided with whitespace")
        void shouldTrimFailureContext_whenProvidedWithWhitespace() {
            // Arrange
            String failureContext = "  Test failure  ";
            
            // Act
            chatHistoryService.setFailureContext(failureContext);
            
            // Assert
            assertThat(chatHistoryService.hasFailureContext()).isTrue();
        }
    }
    
    @Nested
    @DisplayName("User Query Management")
    class UserQueryManagement {
        
        @Test
        @DisplayName("should add user query successfully when valid query provided")
        void shouldAddUserQuerySuccessfully_whenValidQueryProvided() {
            // Arrange
            String query = "What went wrong with this test?";
            
            // Act
            chatHistoryService.addUserQuery(query);
            
            // Assert
            assertThat(chatHistoryService.getUserQueryCount()).isEqualTo(1);
        }
        
        @Test
        @DisplayName("should throw exception when null user query provided")
        void shouldThrowException_whenNullUserQueryProvided() {
            // Act & Assert
            assertThatThrownBy(() -> chatHistoryService.addUserQuery(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Argument for @NotNull parameter 'query'");
        }
        
        @Test
        @DisplayName("should throw exception when empty user query provided")
        void shouldThrowException_whenEmptyUserQueryProvided() {
            // Act & Assert
            assertThatThrownBy(() -> chatHistoryService.addUserQuery(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Query cannot be null or empty");
        }
        
        @Test
        @DisplayName("should throw exception when whitespace-only user query provided")
        void shouldThrowException_whenWhitespaceOnlyUserQueryProvided() {
            // Act & Assert
            assertThatThrownBy(() -> chatHistoryService.addUserQuery("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Query cannot be null or empty");
        }
        
        @Test
        @DisplayName("should trim user query when provided with whitespace")
        void shouldTrimUserQuery_whenProvidedWithWhitespace() {
            // Arrange
            String query = "  What went wrong?  ";
            
            // Act
            chatHistoryService.addUserQuery(query);
            
            // Assert
            assertThat(chatHistoryService.getUserQueryCount()).isEqualTo(1);
        }
        
        @Test
        @DisplayName("should add multiple user queries successfully")
        void shouldAddMultipleUserQueriesSuccessfully() {
            // Arrange
            String query1 = "What went wrong?";
            String query2 = "How can I fix this?";
            String query3 = "What are the best practices?";
            
            // Act
            chatHistoryService.addUserQuery(query1);
            chatHistoryService.addUserQuery(query2);
            chatHistoryService.addUserQuery(query3);
            
            // Assert
            assertThat(chatHistoryService.getUserQueryCount()).isEqualTo(3);
        }
    }
    
    @Nested
    @DisplayName("Sliding Window Management")
    class SlidingWindowManagement {
        
        @Test
        @DisplayName("should maintain default window size of three queries")
        void shouldMaintainDefaultWindowSizeOfThreeQueries() {
            // Arrange
            String query1 = "First query";
            String query2 = "Second query";
            String query3 = "Third query";
            String query4 = "Fourth query";
            String query5 = "Fifth query";
            
            // Act
            chatHistoryService.addUserQuery(query1);
            chatHistoryService.addUserQuery(query2);
            chatHistoryService.addUserQuery(query3);
            chatHistoryService.addUserQuery(query4);
            chatHistoryService.addUserQuery(query5);
            
            // Assert
            assertThat(chatHistoryService.getUserQueryCount()).isEqualTo(3);
        }
        
        @Test
        @DisplayName("should apply sliding window when exceeding default size")
        void shouldApplySlidingWindow_whenExceedingDefaultSize() {
            // Arrange
            String query1 = "Oldest query";
            String query2 = "Second query";
            String query3 = "Third query";
            String query4 = "Newest query";
            
            // Act
            chatHistoryService.addUserQuery(query1);
            chatHistoryService.addUserQuery(query2);
            chatHistoryService.addUserQuery(query3);
            chatHistoryService.addUserQuery(query4);
            
            // Assert
            assertThat(chatHistoryService.getUserQueryCount()).isEqualTo(3);
        }
        
        @Test
        @DisplayName("should set custom window size successfully")
        void shouldSetCustomWindowSizeSuccessfully() {
            // Arrange
            int customWindowSize = 5;
            
            // Act
            chatHistoryService.setUserMessageWindowSize(customWindowSize);
            
            // Assert
            assertThat(chatHistoryService.getUserMessageWindowSize()).isEqualTo(customWindowSize);
        }
        
        @Test
        @DisplayName("should throw exception when setting zero window size")
        void shouldThrowException_whenSettingZeroWindowSize() {
            // Act & Assert
            assertThatThrownBy(() -> chatHistoryService.setUserMessageWindowSize(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Window size must be positive");
        }
        
        @Test
        @DisplayName("should throw exception when setting negative window size")
        void shouldThrowException_whenSettingNegativeWindowSize() {
            // Act & Assert
            assertThatThrownBy(() -> chatHistoryService.setUserMessageWindowSize(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Window size must be positive");
        }
        
        @Test
        @DisplayName("should apply custom window size immediately")
        void shouldApplyCustomWindowSizeImmediately() {
            // Arrange
            chatHistoryService.setUserMessageWindowSize(2);
            String query1 = "First query";
            String query2 = "Second query";
            String query3 = "Third query";
            
            // Act
            chatHistoryService.addUserQuery(query1);
            chatHistoryService.addUserQuery(query2);
            chatHistoryService.addUserQuery(query3);
            
            // Assert
            assertThat(chatHistoryService.getUserQueryCount()).isEqualTo(2);
        }
    }
    
    @Nested
    @DisplayName("Context String Building")
    class ContextStringBuilding {
        
        @Test
        @DisplayName("should build context string with failure context only")
        void shouldBuildContextStringWithFailureContextOnly() {
            // Arrange
            String failureContext = "Test failure: AssertionError";
            String currentQuery = "What went wrong?";
            chatHistoryService.setFailureContext(failureContext);
            
            // Act
            String contextString = chatHistoryService.buildContextString(currentQuery);
            
            // Assert
            assertThat(contextString).contains("### Test Failure Context ###");
            assertThat(contextString).contains(failureContext);
            assertThat(contextString).doesNotContain("### Recent Conversation Context ###");
        }
        
        @Test
        @DisplayName("should build context string with user queries only")
        void shouldBuildContextStringWithUserQueriesOnly() {
            // Arrange
            String query1 = "First question";
            String query2 = "Second question";
            String currentQuery = "Current question";
            chatHistoryService.addUserQuery(query1);
            chatHistoryService.addUserQuery(query2);
            
            // Act
            String contextString = chatHistoryService.buildContextString(currentQuery);
            
            // Assert
            assertThat(contextString).contains("### Recent Conversation Context ###");
            assertThat(contextString).contains("User: " + query1);
            assertThat(contextString).contains("User: " + query2);
            assertThat(contextString).doesNotContain("### Test Failure Context ###");
        }
        
        @Test
        @DisplayName("should build context string with both failure context and user queries")
        void shouldBuildContextStringWithBothFailureContextAndUserQueries() {
            // Arrange
            String failureContext = "Test failure: AssertionError";
            String query1 = "First question";
            String query2 = "Second question";
            String currentQuery = "Current question";
            
            chatHistoryService.setFailureContext(failureContext);
            chatHistoryService.addUserQuery(query1);
            chatHistoryService.addUserQuery(query2);
            
            // Act
            String contextString = chatHistoryService.buildContextString(currentQuery);
            
            // Assert
            assertThat(contextString).contains("### Test Failure Context ###");
            assertThat(contextString).contains(failureContext);
            assertThat(contextString).contains("### Recent Conversation Context ###");
            assertThat(contextString).contains("User: " + query1);
            assertThat(contextString).contains("User: " + query2);
        }
        
        @Test
        @DisplayName("should throw exception when null current query provided")
        void shouldThrowException_whenNullCurrentQueryProvided() {
            // Act & Assert
            assertThatThrownBy(() -> chatHistoryService.buildContextString(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Argument for @NotNull parameter 'currentQuery'");
        }
        
        @Test
        @DisplayName("should throw exception when empty current query provided")
        void shouldThrowException_whenEmptyCurrentQueryProvided() {
            // Act & Assert
            assertThatThrownBy(() -> chatHistoryService.buildContextString(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Current query cannot be null or empty");
        }
        
        @Test
        @DisplayName("should build empty context string when no history")
        void shouldBuildEmptyContextString_whenNoHistory() {
            // Arrange
            String currentQuery = "What went wrong?";
            
            // Act
            String contextString = chatHistoryService.buildContextString(currentQuery);
            
            // Assert
            assertThat(contextString).isEmpty();
        }
        
        @Test
        @DisplayName("should maintain chronological order in context string")
        void shouldMaintainChronologicalOrderInContextString() {
            // Arrange
            String query1 = "First question";
            String query2 = "Second question";
            String query3 = "Third question";
            String currentQuery = "Current question";
            
            chatHistoryService.addUserQuery(query1);
            chatHistoryService.addUserQuery(query2);
            chatHistoryService.addUserQuery(query3);
            
            // Act
            String contextString = chatHistoryService.buildContextString(currentQuery);
            
            // Assert
            int index1 = contextString.indexOf("User: " + query1);
            int index2 = contextString.indexOf("User: " + query2);
            int index3 = contextString.indexOf("User: " + query3);
            
            assertThat(index1).isLessThan(index2);
            assertThat(index2).isLessThan(index3);
        }
    }
    
    @Nested
    @DisplayName("History Clearing")
    class HistoryClearing {
        
        @Test
        @DisplayName("should clear all history successfully")
        void shouldClearAllHistorySuccessfully() {
            // Arrange
            chatHistoryService.setFailureContext("Test failure");
            chatHistoryService.addUserQuery("Test query 1");
            chatHistoryService.addUserQuery("Test query 2");
            
            // Act
            chatHistoryService.clearHistory();
            
            // Assert
            assertThat(chatHistoryService.getUserQueryCount()).isEqualTo(0);
            assertThat(chatHistoryService.hasFailureContext()).isFalse();
        }
        
        @Test
        @DisplayName("should clear empty history without errors")
        void shouldClearEmptyHistoryWithoutErrors() {
            // Act
            chatHistoryService.clearHistory();
            
            // Assert
            assertThat(chatHistoryService.getUserQueryCount()).isEqualTo(0);
            assertThat(chatHistoryService.hasFailureContext()).isFalse();
        }
    }
    
    @Nested
    @DisplayName("State Management")
    class StateManagement {
        
        @Test
        @DisplayName("should get state with current data")
        void shouldGetStateWithCurrentData() {
            // Arrange
            chatHistoryService.setFailureContext("Test failure");
            chatHistoryService.addUserQuery("Test query");
            chatHistoryService.setUserMessageWindowSize(5);
            
            // Act
            ChatHistoryService.State state = chatHistoryService.getState();
            
            // Assert
            assertThat(state.failureContext).isEqualTo("Test failure");
            assertThat(state.userQueries).hasSize(1);
            assertThat(state.userMessageWindowSize).isEqualTo(5);
            assertThat(state.failureContextTimestamp).isPositive();
        }
        
        @Test
        @DisplayName("should load state successfully")
        void shouldLoadStateSuccessfully() {
            // Arrange
            ChatHistoryService.State state = new ChatHistoryService.State();
            state.failureContext = "Loaded failure context";
            state.userMessageWindowSize = 7;
            state.failureContextTimestamp = System.currentTimeMillis();
            
            ChatHistoryService.UserQueryData queryData = new ChatHistoryService.UserQueryData();
            queryData.query = "Loaded query";
            queryData.timestamp = System.currentTimeMillis();
            state.userQueries.add(queryData);
            
            // Act
            chatHistoryService.loadState(state);
            
            // Assert
            assertThat(chatHistoryService.hasFailureContext()).isTrue();
            assertThat(chatHistoryService.getUserQueryCount()).isEqualTo(1);
            assertThat(chatHistoryService.getUserMessageWindowSize()).isEqualTo(7);
        }
        
        @Test
        @DisplayName("should handle empty state loading")
        void shouldHandleEmptyStateLoading() {
            // Arrange
            ChatHistoryService.State state = new ChatHistoryService.State();
            
            // Act
            chatHistoryService.loadState(state);
            
            // Assert
            assertThat(chatHistoryService.getUserQueryCount()).isEqualTo(0);
            assertThat(chatHistoryService.hasFailureContext()).isFalse();
            assertThat(chatHistoryService.getUserMessageWindowSize()).isEqualTo(ChatHistoryService.DEFAULT_USER_MESSAGE_WINDOW_SIZE);
        }
    }
    
    @Nested
    @DisplayName("UserQueryData Serialization")
    class UserQueryDataSerialization {
        
        @Test
        @DisplayName("should create UserQueryData from UserQuery")
        void shouldCreateUserQueryDataFromUserQuery() {
            // Arrange
            UserQuery userQuery = new UserQuery("Test query", 1234567890L);
            
            // Act
            ChatHistoryService.UserQueryData queryData = new ChatHistoryService.UserQueryData(userQuery);
            
            // Assert
            assertThat(queryData.query).isEqualTo("Test query");
            assertThat(queryData.timestamp).isEqualTo(1234567890L);
        }
        
        @Test
        @DisplayName("should convert UserQueryData back to UserQuery")
        void shouldConvertUserQueryDataBackToUserQuery() {
            // Arrange
            ChatHistoryService.UserQueryData queryData = new ChatHistoryService.UserQueryData();
            queryData.query = "Test query";
            queryData.timestamp = 1234567890L;
            
            // Act
            UserQuery userQuery = queryData.toUserQuery();
            
            // Assert
            assertThat(userQuery.getQuery()).isEqualTo("Test query");
            assertThat(userQuery.getTimestamp()).isEqualTo(1234567890L);
        }
        
        @Test
        @DisplayName("should create empty UserQueryData with default constructor")
        void shouldCreateEmptyUserQueryDataWithDefaultConstructor() {
            // Act
            ChatHistoryService.UserQueryData queryData = new ChatHistoryService.UserQueryData();
            
            // Assert
            assertThat(queryData.query).isNull();
            assertThat(queryData.timestamp).isEqualTo(0L);
        }
    }
    
    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesAndErrorHandling {
        
        @Test
        @DisplayName("should handle very long failure context")
        void shouldHandleVeryLongFailureContext() {
            // Arrange
            StringBuilder longContext = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                longContext.append("Very long failure context line ").append(i).append(" ");
            }
            
            // Act
            chatHistoryService.setFailureContext(longContext.toString());
            
            // Assert
            assertThat(chatHistoryService.hasFailureContext()).isTrue();
        }
        
        @Test
        @DisplayName("should handle very long user query")
        void shouldHandleVeryLongUserQuery() {
            // Arrange
            StringBuilder longQuery = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                longQuery.append("Very long user query line ").append(i).append(" ");
            }
            
            // Act
            chatHistoryService.addUserQuery(longQuery.toString());
            
            // Assert
            assertThat(chatHistoryService.getUserQueryCount()).isEqualTo(1);
        }
        
        @Test
        @DisplayName("should handle special characters in failure context")
        void shouldHandleSpecialCharactersInFailureContext() {
            // Arrange
            String specialContext = "Test failure with special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?";
            
            // Act
            chatHistoryService.setFailureContext(specialContext);
            
            // Assert
            assertThat(chatHistoryService.hasFailureContext()).isTrue();
        }
        
        @Test
        @DisplayName("should handle special characters in user query")
        void shouldHandleSpecialCharactersInUserQuery() {
            // Arrange
            String specialQuery = "Query with special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?";
            
            // Act
            chatHistoryService.addUserQuery(specialQuery);
            
            // Assert
            assertThat(chatHistoryService.getUserQueryCount()).isEqualTo(1);
        }
        
        @Test
        @DisplayName("should handle unicode characters in failure context")
        void shouldHandleUnicodeCharactersInFailureContext() {
            // Arrange
            String unicodeContext = "Test failure with unicode: 你好世界 🌍 🚀";
            
            // Act
            chatHistoryService.setFailureContext(unicodeContext);
            
            // Assert
            assertThat(chatHistoryService.hasFailureContext()).isTrue();
        }
        
        @Test
        @DisplayName("should handle unicode characters in user query")
        void shouldHandleUnicodeCharactersInUserQuery() {
            // Arrange
            String unicodeQuery = "Query with unicode: 你好世界 🌍 🚀";
            
            // Act
            chatHistoryService.addUserQuery(unicodeQuery);
            
            // Assert
            assertThat(chatHistoryService.getUserQueryCount()).isEqualTo(1);
        }
    }
}
