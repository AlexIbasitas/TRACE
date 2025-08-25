package com.trace.ai.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("User Query Unit Tests")
class UserQueryUnitTest {
    
    private UserQuery simpleQuery;
    private UserQuery longQuery;
    private UserQuery specialCharQuery;
    private UserQuery timestampQuery;
    
    @BeforeEach
    void setUp() {
        simpleQuery = new UserQuery("What is the error in this test?", System.currentTimeMillis());
        longQuery = new UserQuery("This is a very long query that contains many words and should be properly handled by the UserQuery class without any issues", System.currentTimeMillis());
        specialCharQuery = new UserQuery("Test query with special chars: @#$%^&*()_+-=[]{}|;':\",./<>?", System.currentTimeMillis());
        timestampQuery = new UserQuery("Query with specific timestamp", 1234567890L);
    }
    
    @Nested
    @DisplayName("Query Creation")
    class QueryCreation {
        
        @Test
        @DisplayName("should create query with valid parameters")
        void shouldCreateQuery_whenValidParametersProvided() {
            // Arrange
            String queryText = "Test query";
            long timestamp = System.currentTimeMillis();
            
            // Act
            UserQuery query = new UserQuery(queryText, timestamp);
            
            // Assert
            assertThat(query.getQuery()).isEqualTo(queryText);
            assertThat(query.getTimestamp()).isEqualTo(timestamp);
        }
        
        @Test
        @DisplayName("should trim whitespace from query text")
        void shouldTrimWhitespace_fromQueryText() {
            // Arrange
            String queryText = "  Test query with whitespace  ";
            long timestamp = System.currentTimeMillis();
            
            // Act
            UserQuery query = new UserQuery(queryText, timestamp);
            
            // Assert
            assertThat(query.getQuery()).isEqualTo("Test query with whitespace");
        }
        
        @Test
        @DisplayName("should throw exception when query is null")
        void shouldThrowException_whenQueryIsNull() {
            // Act & Assert
            assertThatThrownBy(() -> new UserQuery(null, System.currentTimeMillis()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@NotNull parameter 'query'");
        }
        
        @Test
        @DisplayName("should throw exception when query is empty")
        void shouldThrowException_whenQueryIsEmpty() {
            // Act & Assert
            assertThatThrownBy(() -> new UserQuery("", System.currentTimeMillis()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Query cannot be null or empty");
        }
        
        @Test
        @DisplayName("should throw exception when query is only whitespace")
        void shouldThrowException_whenQueryIsOnlyWhitespace() {
            // Act & Assert
            assertThatThrownBy(() -> new UserQuery("   ", System.currentTimeMillis()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Query cannot be null or empty");
        }
        
        @Test
        @DisplayName("should accept query with special characters")
        void shouldAcceptQuery_withSpecialCharacters() {
            // Arrange
            String queryText = "Test query with @#$%^&*()_+-=[]{}|;':\",./<>?";
            long timestamp = System.currentTimeMillis();
            
            // Act
            UserQuery query = new UserQuery(queryText, timestamp);
            
            // Assert
            assertThat(query.getQuery()).isEqualTo(queryText);
        }
    }
    
    @Nested
    @DisplayName("Query Validation")
    class QueryValidation {
        
        @Test
        @DisplayName("should validate query text correctly")
        void shouldValidateQueryTextCorrectly() {
            // Assert
            assertThat(simpleQuery.getQuery()).isNotNull().isNotEmpty();
            assertThat(simpleQuery.getQuery()).isEqualTo("What is the error in this test?");
        }
        
        @Test
        @DisplayName("should reject empty queries")
        void shouldRejectEmptyQueries() {
            // Act & Assert
            assertThatThrownBy(() -> new UserQuery("", System.currentTimeMillis()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Query cannot be null or empty");
        }
        
        @Test
        @DisplayName("should reject null queries")
        void shouldRejectNullQueries() {
            // Act & Assert
            assertThatThrownBy(() -> new UserQuery(null, System.currentTimeMillis()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@NotNull parameter 'query'");
        }
        
        @Test
        @DisplayName("should validate query length limits")
        void shouldValidateQueryLengthLimits() {
            // Arrange
            String veryLongQuery = "A".repeat(10000); // 10k character query
            
            // Act
            UserQuery query = new UserQuery(veryLongQuery, System.currentTimeMillis());
            
            // Assert
            assertThat(query.getQuery()).isEqualTo(veryLongQuery);
            assertThat(query.getQuery().length()).isEqualTo(10000);
        }
    }
    
    @Nested
    @DisplayName("Query Processing")
    class QueryProcessing {
        
        @Test
        @DisplayName("should process query text correctly")
        void shouldProcessQueryTextCorrectly() {
            // Assert
            assertThat(simpleQuery.getQuery()).isEqualTo("What is the error in this test?");
            assertThat(longQuery.getQuery()).contains("very long query");
            assertThat(specialCharQuery.getQuery()).contains("@#$%^&*");
        }
        
        @Test
        @DisplayName("should extract query parameters")
        void shouldExtractQueryParameters() {
            // Arrange
            UserQuery parameterQuery = new UserQuery("Find error in file: TestClass.java line: 42", System.currentTimeMillis());
            
            // Act & Assert
            assertThat(parameterQuery.getQuery()).contains("TestClass.java");
            assertThat(parameterQuery.getQuery()).contains("line: 42");
        }
        
        @Test
        @DisplayName("should handle special characters in queries")
        void shouldHandleSpecialCharactersInQueries() {
            // Assert
            assertThat(specialCharQuery.getQuery()).contains("@#$%^&*()_+-=[]{}|;':\",./<>?");
            assertThat(specialCharQuery.getQuery()).hasSizeGreaterThan(30);
        }
        
        @Test
        @DisplayName("should normalize query text")
        void shouldNormalizeQueryText() {
            // Arrange
            String unnormalizedQuery = "  Query with   multiple   spaces  ";
            
            // Act
            UserQuery query = new UserQuery(unnormalizedQuery, System.currentTimeMillis());
            
            // Assert
            assertThat(query.getQuery()).isEqualTo("Query with   multiple   spaces");
            assertThat(query.getQuery()).doesNotStartWith(" ");
            assertThat(query.getQuery()).doesNotEndWith(" ");
        }
    }
    
    @Nested
    @DisplayName("Query Context")
    class QueryContext {
        
        @Test
        @DisplayName("should set query context correctly")
        void shouldSetQueryContextCorrectly() {
            // Assert
            assertThat(simpleQuery.getQuery()).isEqualTo("What is the error in this test?");
            assertThat(simpleQuery.getTimestamp()).isPositive();
        }
        
        @Test
        @DisplayName("should retrieve query context")
        void shouldRetrieveQueryContext() {
            // Assert
            assertThat(simpleQuery.getQuery()).isNotNull();
            assertThat(simpleQuery.getTimestamp()).isGreaterThan(0L);
        }
        
        @Test
        @DisplayName("should handle context validation")
        void shouldHandleContextValidation() {
            // Assert
            assertThat(simpleQuery.getQuery()).isNotNull().isNotEmpty();
            assertThat(simpleQuery.getTimestamp()).isPositive();
            assertThat(simpleQuery.getQuery().length()).isGreaterThan(0);
        }
        
        @Test
        @DisplayName("should preserve timestamp accuracy")
        void shouldPreserveTimestampAccuracy() {
            // Arrange
            long specificTimestamp = 1234567890L;
            UserQuery query = new UserQuery("Test query", specificTimestamp);
            
            // Assert
            assertThat(query.getTimestamp()).isEqualTo(specificTimestamp);
        }
    }
    
    @Nested
    @DisplayName("Query Properties")
    class QueryProperties {
        
        @Test
        @DisplayName("should get query text correctly")
        void shouldGetQueryTextCorrectly() {
            // Assert
            assertThat(simpleQuery.getQuery()).isEqualTo("What is the error in this test?");
            assertThat(longQuery.getQuery()).contains("very long query");
            assertThat(specialCharQuery.getQuery()).contains("@#$%^&*");
        }
        
        @Test
        @DisplayName("should get timestamp correctly")
        void shouldGetTimestampCorrectly() {
            // Assert
            assertThat(simpleQuery.getTimestamp()).isPositive();
            assertThat(timestampQuery.getTimestamp()).isEqualTo(1234567890L);
        }
        
        @Test
        @DisplayName("should handle different query lengths")
        void shouldHandleDifferentQueryLengths() {
            // Arrange
            UserQuery shortQuery = new UserQuery("Hi", System.currentTimeMillis());
            UserQuery mediumQuery = new UserQuery("This is a medium length query", System.currentTimeMillis());
            
            // Assert
            assertThat(shortQuery.getQuery()).hasSize(2);
            assertThat(mediumQuery.getQuery()).hasSize(29);
            assertThat(longQuery.getQuery()).hasSizeGreaterThan(50);
        }
    }
    
    @Nested
    @DisplayName("Object Methods")
    class ObjectMethods {
        
        @Test
        @DisplayName("should implement equals correctly")
        void shouldImplementEqualsCorrectly() {
            // Arrange
            UserQuery sameQuery = new UserQuery("What is the error in this test?", simpleQuery.getTimestamp());
            UserQuery differentQuery = new UserQuery("Different query", System.currentTimeMillis());
            UserQuery sameTextDifferentTime = new UserQuery("What is the error in this test?", simpleQuery.getTimestamp() + 1000L);
            
            // Act & Assert
            assertThat(simpleQuery).isEqualTo(sameQuery);
            assertThat(simpleQuery).isNotEqualTo(differentQuery);
            assertThat(simpleQuery).isNotEqualTo(sameTextDifferentTime);
            assertThat(simpleQuery).isNotEqualTo(null);
            assertThat(simpleQuery).isNotEqualTo("string");
            assertThat(simpleQuery).isEqualTo(simpleQuery); // Reflexive
        }
        
        @Test
        @DisplayName("should implement hashCode correctly")
        void shouldImplementHashCodeCorrectly() {
            // Arrange
            UserQuery sameQuery = new UserQuery("What is the error in this test?", simpleQuery.getTimestamp());
            
            // Act & Assert
            assertThat(simpleQuery.hashCode()).isEqualTo(sameQuery.hashCode());
        }
        
        @Test
        @DisplayName("should implement toString correctly")
        void shouldImplementToStringCorrectly() {
            // Act
            String toString = simpleQuery.toString();
            
            // Assert
            assertThat(toString).contains("UserQuery{");
            assertThat(toString).contains("query='What is the error in this test?...'");
            assertThat(toString).contains("timestamp=" + simpleQuery.getTimestamp());
        }
        
        @Test
        @DisplayName("should truncate long queries in toString")
        void shouldTruncateLongQueries_inToString() {
            // Arrange
            String veryLongQueryText = "A".repeat(100);
            UserQuery longQuery = new UserQuery(veryLongQueryText, System.currentTimeMillis());
            
            // Act
            String toString = longQuery.toString();
            
            // Assert
            assertThat(toString).contains("query='");
            assertThat(toString).contains("...'");
            // Should truncate to 50 characters + "..."
            assertThat(toString).doesNotContain(veryLongQueryText);
        }
        
        @Test
        @DisplayName("should not truncate short queries in toString")
        void shouldNotTruncateShortQueries_inToString() {
            // Arrange
            String shortQueryText = "Short query";
            UserQuery shortQuery = new UserQuery(shortQueryText, System.currentTimeMillis());
            
            // Act
            String toString = shortQuery.toString();
            
            // Assert
            assertThat(toString).contains("query='" + shortQueryText + "...'");
        }
    }
    
    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {
        
        @Test
        @DisplayName("should handle single character query")
        void shouldHandleSingleCharacterQuery() {
            // Arrange
            UserQuery singleCharQuery = new UserQuery("A", System.currentTimeMillis());
            
            // Assert
            assertThat(singleCharQuery.getQuery()).isEqualTo("A");
            assertThat(singleCharQuery.getQuery()).hasSize(1);
        }
        
        @Test
        @DisplayName("should handle query with only numbers")
        void shouldHandleQuery_withOnlyNumbers() {
            // Arrange
            UserQuery numberQuery = new UserQuery("12345", System.currentTimeMillis());
            
            // Assert
            assertThat(numberQuery.getQuery()).isEqualTo("12345");
        }
        
        @Test
        @DisplayName("should handle query with unicode characters")
        void shouldHandleQuery_withUnicodeCharacters() {
            // Arrange
            String unicodeQuery = "Test query with émojis 🚀 and unicode 中文";
            UserQuery query = new UserQuery(unicodeQuery, System.currentTimeMillis());
            
            // Assert
            assertThat(query.getQuery()).isEqualTo(unicodeQuery);
        }
        
        @Test
        @DisplayName("should handle zero timestamp")
        void shouldHandleZeroTimestamp() {
            // Arrange
            UserQuery zeroTimestampQuery = new UserQuery("Test query", 0L);
            
            // Assert
            assertThat(zeroTimestampQuery.getTimestamp()).isEqualTo(0L);
            assertThat(zeroTimestampQuery.getQuery()).isEqualTo("Test query");
        }
        
        @Test
        @DisplayName("should handle negative timestamp")
        void shouldHandleNegativeTimestamp() {
            // Arrange
            UserQuery negativeTimestampQuery = new UserQuery("Test query", -1234567890L);
            
            // Assert
            assertThat(negativeTimestampQuery.getTimestamp()).isEqualTo(-1234567890L);
            assertThat(negativeTimestampQuery.getQuery()).isEqualTo("Test query");
        }
    }
}
