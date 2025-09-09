package com.trace.ai.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

import com.trace.ai.services.embedding.OpenAIEmbeddingService;
import com.trace.ai.services.embedding.GeminiEmbeddingService;
import com.trace.ai.configuration.AISettings;
import com.trace.ai.configuration.AIServiceType;

import java.sql.SQLException;

@DisplayName("Document Retrieval Service Unit Tests")
@ExtendWith(MockitoExtension.class)
class DocumentRetrievalServiceUnitTest {
    
    @Mock private DocumentDatabaseService mockDatabaseService;
    @Mock private OpenAIEmbeddingService mockOpenAIEmbeddingService;
    @Mock private GeminiEmbeddingService mockGeminiEmbeddingService;
    @Mock private AISettings mockAISettings;
    
    private DocumentRetrievalService documentRetrievalService;
    
    @BeforeEach
    void setUp() {
        documentRetrievalService = new DocumentRetrievalService(
            mockDatabaseService,
            mockOpenAIEmbeddingService,
            mockGeminiEmbeddingService,
            mockAISettings
        );
    }
    
    @Nested
    @DisplayName("Constructor Validation")
    class ConstructorValidation {
        
        @Test
        @DisplayName("should throw exception when database service is null")
        void shouldThrowException_whenDatabaseServiceIsNull() {
            // Act & Assert
            assertThatThrownBy(() -> 
                new DocumentRetrievalService(
                    null, mockOpenAIEmbeddingService, mockGeminiEmbeddingService, mockAISettings
                )
            )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Argument for @NotNull parameter 'databaseService'");
        }
        
        @Test
        @DisplayName("should throw exception when OpenAI embedding service is null")
        void shouldThrowException_whenOpenAIEmbeddingServiceIsNull() {
            // Act & Assert
            assertThatThrownBy(() -> 
                new DocumentRetrievalService(
                    mockDatabaseService, null, mockGeminiEmbeddingService, mockAISettings
                )
            )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Argument for @NotNull parameter 'openAIEmbeddingService'");
        }
        
        @Test
        @DisplayName("should throw exception when Gemini embedding service is null")
        void shouldThrowException_whenGeminiEmbeddingServiceIsNull() {
            // Act & Assert
            assertThatThrownBy(() -> 
                new DocumentRetrievalService(
                    mockDatabaseService, mockOpenAIEmbeddingService, null, mockAISettings
                )
            )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Argument for @NotNull parameter 'geminiEmbeddingService'");
        }
        
        @Test
        @DisplayName("should throw exception when AI settings is null")
        void shouldThrowException_whenAISettingsIsNull() {
            // Act & Assert
            assertThatThrownBy(() -> 
                new DocumentRetrievalService(
                    mockDatabaseService, mockOpenAIEmbeddingService, mockGeminiEmbeddingService, null
                )
            )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Argument for @NotNull parameter 'aiSettings'");
        }
        
        @Test
        @DisplayName("should create service successfully when all dependencies are provided")
        void shouldCreateServiceSuccessfully_whenAllDependenciesAreProvided() {
            // Act & Assert
            assertThat(documentRetrievalService).isNotNull();
        }
    }
    
    @Nested
    @DisplayName("Input Validation")
    class InputValidation {
        
        @Test
        @DisplayName("should throw exception when query text is null")
        void shouldThrowException_whenQueryTextIsNull() {
            // Act & Assert
            assertThatThrownBy(() -> 
                documentRetrievalService.retrieveRelevantDocuments(
                    null, "failure_analysis", null, "detailed"
                )
            )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Argument for @NotNull parameter 'queryText'");
        }
        
        @Test
        @DisplayName("should throw exception when query text is empty")
        void shouldThrowException_whenQueryTextIsEmpty() {
            // Act & Assert
            assertThatThrownBy(() -> 
                documentRetrievalService.retrieveRelevantDocuments(
                    "", "failure_analysis", null, "detailed"
                )
            )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Query text cannot be null or empty");
        }
        
        @Test
        @DisplayName("should throw exception when query text is whitespace only")
        void shouldThrowException_whenQueryTextIsWhitespaceOnly() {
            // Act & Assert
            assertThatThrownBy(() -> 
                documentRetrievalService.retrieveRelevantDocuments(
                    "   ", "failure_analysis", null, "detailed"
                )
            )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Query text cannot be null or empty");
        }
    }
    
    @Nested
    @DisplayName("Database Operations")
    class DatabaseOperations {
        
        @Test
        @DisplayName("should return correct document count for OpenAI")
        void shouldReturnCorrectDocumentCountForOpenAI() throws Exception {
            // Arrange
            when(mockAISettings.getPreferredAIService()).thenReturn(AIServiceType.OPENAI);
            when(mockDatabaseService.getDocumentCountWithEmbeddings(DocumentDatabaseService.EmbeddingType.OPENAI))
                .thenReturn(42);
            
            // Act
            int documentCount = documentRetrievalService.getDocumentCount();
            
            // Assert
            assertThat(documentCount).isEqualTo(42);
            verify(mockDatabaseService).getDocumentCountWithEmbeddings(DocumentDatabaseService.EmbeddingType.OPENAI);
        }
        
        @Test
        @DisplayName("should return correct document count for Gemini")
        void shouldReturnCorrectDocumentCountForGemini() throws Exception {
            // Arrange
            when(mockAISettings.getPreferredAIService()).thenReturn(AIServiceType.GEMINI);
            when(mockDatabaseService.getDocumentCountWithEmbeddings(DocumentDatabaseService.EmbeddingType.GEMINI))
                .thenReturn(15);
            
            // Act
            int documentCount = documentRetrievalService.getDocumentCount();
            
            // Assert
            assertThat(documentCount).isEqualTo(15);
            verify(mockDatabaseService).getDocumentCountWithEmbeddings(DocumentDatabaseService.EmbeddingType.GEMINI);
        }
        
        @Test
        @DisplayName("should return zero document count when database error occurs")
        void shouldReturnZeroDocumentCount_whenDatabaseErrorOccurs() throws Exception {
            // Arrange
            when(mockAISettings.getPreferredAIService()).thenReturn(AIServiceType.OPENAI);
            when(mockDatabaseService.getDocumentCountWithEmbeddings(any()))
                .thenThrow(new SQLException("Database error"));
            
            // Act
            int documentCount = documentRetrievalService.getDocumentCount();
            
            // Assert
            assertThat(documentCount).isEqualTo(0);
            verify(mockDatabaseService).getDocumentCountWithEmbeddings(DocumentDatabaseService.EmbeddingType.OPENAI);
        }
        
        @Test
        @DisplayName("should handle runtime exception during document count")
        void shouldHandleRuntimeExceptionDuringDocumentCount() throws Exception {
            // Arrange
            when(mockAISettings.getPreferredAIService()).thenReturn(AIServiceType.OPENAI);
            when(mockDatabaseService.getDocumentCountWithEmbeddings(any()))
                .thenThrow(new RuntimeException("Unexpected error"));
            
            // Act & Assert
            assertThatThrownBy(() -> documentRetrievalService.getDocumentCount())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Unexpected error");
            verify(mockDatabaseService).getDocumentCountWithEmbeddings(DocumentDatabaseService.EmbeddingType.OPENAI);
        }
    }
    
    @Nested
    @DisplayName("Database Validation")
    class DatabaseValidation {
        
        @Test
        @DisplayName("should return true when database is ready with documents")
        void shouldReturnTrue_whenDatabaseIsReadyWithDocuments() throws Exception {
            // Arrange
            when(mockAISettings.getPreferredAIService()).thenReturn(AIServiceType.OPENAI);
            when(mockDatabaseService.getDocumentCountWithEmbeddings(DocumentDatabaseService.EmbeddingType.OPENAI))
                .thenReturn(5);
            
            // Act
            boolean isReady = documentRetrievalService.isDatabaseReady();
            
            // Assert
            assertThat(isReady).isTrue();
            verify(mockDatabaseService).getDocumentCountWithEmbeddings(DocumentDatabaseService.EmbeddingType.OPENAI);
        }
        
        @Test
        @DisplayName("should return false when database has no documents")
        void shouldReturnFalse_whenDatabaseHasNoDocuments() throws Exception {
            // Arrange
            when(mockAISettings.getPreferredAIService()).thenReturn(AIServiceType.OPENAI);
            when(mockDatabaseService.getDocumentCountWithEmbeddings(DocumentDatabaseService.EmbeddingType.OPENAI))
                .thenReturn(0);
            
            // Act
            boolean isReady = documentRetrievalService.isDatabaseReady();
            
            // Assert
            assertThat(isReady).isFalse();
            verify(mockDatabaseService).getDocumentCountWithEmbeddings(DocumentDatabaseService.EmbeddingType.OPENAI);
        }
        
        @Test
        @DisplayName("should return false when database validation fails with SQLException")
        void shouldReturnFalse_whenDatabaseValidationFailsWithSQLException() throws Exception {
            // Arrange
            when(mockAISettings.getPreferredAIService()).thenReturn(AIServiceType.OPENAI);
            when(mockDatabaseService.getDocumentCountWithEmbeddings(any()))
                .thenThrow(new SQLException("Database connection failed"));
            
            // Act
            boolean isReady = documentRetrievalService.isDatabaseReady();
            
            // Assert
            assertThat(isReady).isFalse();
            verify(mockDatabaseService).getDocumentCountWithEmbeddings(DocumentDatabaseService.EmbeddingType.OPENAI);
        }
        
        @Test
        @DisplayName("should return false when database validation fails with RuntimeException")
        void shouldReturnFalse_whenDatabaseValidationFailsWithRuntimeException() throws Exception {
            // Arrange
            when(mockAISettings.getPreferredAIService()).thenReturn(AIServiceType.OPENAI);
            when(mockDatabaseService.getDocumentCountWithEmbeddings(any()))
                .thenThrow(new RuntimeException("Validation error"));
            
            // Act
            boolean isReady = documentRetrievalService.isDatabaseReady();
            
            // Assert
            assertThat(isReady).isFalse();
            verify(mockDatabaseService).getDocumentCountWithEmbeddings(DocumentDatabaseService.EmbeddingType.OPENAI);
        }
    }
    
    @Nested
    @DisplayName("Service Configuration")
    class ServiceConfiguration {
        
        @Test
        @DisplayName("should use OpenAI when OpenAI is preferred service")
        void shouldUseOpenAI_whenOpenAIIsPreferredService() throws Exception {
            // Arrange
            when(mockAISettings.getPreferredAIService()).thenReturn(AIServiceType.OPENAI);
            when(mockDatabaseService.getDocumentCountWithEmbeddings(DocumentDatabaseService.EmbeddingType.OPENAI))
                .thenReturn(10);
            
            // Act
            int documentCount = documentRetrievalService.getDocumentCount();
            
            // Assert
            assertThat(documentCount).isEqualTo(10);
            verify(mockAISettings).getPreferredAIService();
            verify(mockDatabaseService).getDocumentCountWithEmbeddings(DocumentDatabaseService.EmbeddingType.OPENAI);
        }
        
        @Test
        @DisplayName("should use Gemini when Gemini is preferred service")
        void shouldUseGemini_whenGeminiIsPreferredService() throws Exception {
            // Arrange
            when(mockAISettings.getPreferredAIService()).thenReturn(AIServiceType.GEMINI);
            when(mockDatabaseService.getDocumentCountWithEmbeddings(DocumentDatabaseService.EmbeddingType.GEMINI))
                .thenReturn(20);
            
            // Act
            int documentCount = documentRetrievalService.getDocumentCount();
            
            // Assert
            assertThat(documentCount).isEqualTo(20);
            verify(mockAISettings).getPreferredAIService();
            verify(mockDatabaseService).getDocumentCountWithEmbeddings(DocumentDatabaseService.EmbeddingType.GEMINI);
        }
    }
    
    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {
        
        @Test
        @DisplayName("should handle very large document count")
        void shouldHandleVeryLargeDocumentCount() throws Exception {
            // Arrange
            when(mockAISettings.getPreferredAIService()).thenReturn(AIServiceType.OPENAI);
            when(mockDatabaseService.getDocumentCountWithEmbeddings(DocumentDatabaseService.EmbeddingType.OPENAI))
                .thenReturn(Integer.MAX_VALUE);
            
            // Act
            int documentCount = documentRetrievalService.getDocumentCount();
            
            // Assert
            assertThat(documentCount).isEqualTo(Integer.MAX_VALUE);
            verify(mockDatabaseService).getDocumentCountWithEmbeddings(DocumentDatabaseService.EmbeddingType.OPENAI);
        }
        
        @Test
        @DisplayName("should handle negative document count from database")
        void shouldHandleNegativeDocumentCountFromDatabase() throws Exception {
            // Arrange
            when(mockAISettings.getPreferredAIService()).thenReturn(AIServiceType.OPENAI);
            when(mockDatabaseService.getDocumentCountWithEmbeddings(DocumentDatabaseService.EmbeddingType.OPENAI))
                .thenReturn(-5);
            
            // Act
            int documentCount = documentRetrievalService.getDocumentCount();
            
            // Assert
            assertThat(documentCount).isEqualTo(-5);
            verify(mockDatabaseService).getDocumentCountWithEmbeddings(DocumentDatabaseService.EmbeddingType.OPENAI);
        }
        

    }
}
