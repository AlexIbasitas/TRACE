package com.trace.ai.services;

import com.trace.ai.models.DocumentEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Document Database Service Unit Tests")
class DocumentDatabaseServiceUnitTest {
    
    private DocumentDatabaseService documentDatabaseService;
    
    @BeforeEach
    void setUp() {
        documentDatabaseService = new DocumentDatabaseService();
    }
    
    @Nested
    @DisplayName("Database Path Management")
    class DatabasePathManagement {
        
        @Test
        @DisplayName("should return consistent database path")
        void shouldReturnConsistentDatabasePath() {
            // Act
            String dbPath = documentDatabaseService.getDatabasePath();
            
            // Assert
            assertThat(dbPath).isNotNull();
            assertThat(dbPath).contains("trace-documents.db");
            assertThat(dbPath).contains(".trace/documents");
        }
        
        @Test
        @DisplayName("should return same path on multiple calls")
        void shouldReturnSamePathOnMultipleCalls() {
            // Act
            String path1 = documentDatabaseService.getDatabasePath();
            String path2 = documentDatabaseService.getDatabasePath();
            
            // Assert
            assertThat(path1).isEqualTo(path2);
        }
    }
    
    @Nested
    @DisplayName("Cosine Similarity Calculation")
    class CosineSimilarityCalculation {
        
        @Test
        @DisplayName("should calculate perfect similarity for identical embeddings")
        void shouldCalculatePerfectSimilarityForIdenticalEmbeddings() {
            // Arrange
            float[] embedding1 = {1.0f, 2.0f, 3.0f};
            float[] embedding2 = {1.0f, 2.0f, 3.0f};
            
            // Act
            double similarity = documentDatabaseService.calculateCosineSimilarity(embedding1, embedding2);
            
            // Assert
            assertThat(similarity).isEqualTo(1.0);
        }
        
        @Test
        @DisplayName("should calculate zero similarity for orthogonal embeddings")
        void shouldCalculateZeroSimilarityForOrthogonalEmbeddings() {
            // Arrange
            float[] embedding1 = {1.0f, 0.0f, 0.0f};
            float[] embedding2 = {0.0f, 1.0f, 0.0f};
            
            // Act
            double similarity = documentDatabaseService.calculateCosineSimilarity(embedding1, embedding2);
            
            // Assert
            assertThat(similarity).isEqualTo(0.0);
        }
        
        @Test
        @DisplayName("should calculate negative similarity for opposite embeddings")
        void shouldCalculateNegativeSimilarityForOppositeEmbeddings() {
            // Arrange
            float[] embedding1 = {1.0f, 2.0f, 3.0f};
            float[] embedding2 = {-1.0f, -2.0f, -3.0f};
            
            // Act
            double similarity = documentDatabaseService.calculateCosineSimilarity(embedding1, embedding2);
            
            // Assert
            assertThat(similarity).isEqualTo(-1.0);
        }
        
        @Test
        @DisplayName("should calculate partial similarity for different embeddings")
        void shouldCalculatePartialSimilarityForDifferentEmbeddings() {
            // Arrange
            float[] embedding1 = {1.0f, 0.0f, 0.0f};
            float[] embedding2 = {0.5f, 0.5f, 0.0f};
            
            // Act
            double similarity = documentDatabaseService.calculateCosineSimilarity(embedding1, embedding2);
            
            // Assert
            assertThat(similarity).isBetween(0.0, 1.0);
            assertThat(similarity).isGreaterThan(0.0);
        }
        
        @Test
        @DisplayName("should handle zero embeddings")
        void shouldHandleZeroEmbeddings() {
            // Arrange
            float[] embedding1 = {0.0f, 0.0f, 0.0f};
            float[] embedding2 = {0.0f, 0.0f, 0.0f};
            
            // Act
            double similarity = documentDatabaseService.calculateCosineSimilarity(embedding1, embedding2);
            
            // Assert
            assertThat(similarity).isEqualTo(0.0);
        }
        
        @Test
        @DisplayName("should throw exception when embeddings have different dimensions")
        void shouldThrowException_whenEmbeddingsHaveDifferentDimensions() {
            // Arrange
            float[] embedding1 = {1.0f, 2.0f, 3.0f};
            float[] embedding2 = {1.0f, 2.0f};
            
            // Act & Assert
            assertThatThrownBy(() -> documentDatabaseService.calculateCosineSimilarity(embedding1, embedding2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Embeddings must have same dimensions");
        }
        
        @Test
        @DisplayName("should handle single dimension embeddings")
        void shouldHandleSingleDimensionEmbeddings() {
            // Arrange
            float[] embedding1 = {1.0f};
            float[] embedding2 = {0.5f};
            
            // Act
            double similarity = documentDatabaseService.calculateCosineSimilarity(embedding1, embedding2);
            
            // Assert
            assertThat(similarity).isBetween(0.0, 1.0);
        }
        
        @Test
        @DisplayName("should handle large dimension embeddings")
        void shouldHandleLargeDimensionEmbeddings() {
            // Arrange
            float[] embedding1 = new float[1000];
            float[] embedding2 = new float[1000];
            
            for (int i = 0; i < 1000; i++) {
                embedding1[i] = (float) Math.random();
                embedding2[i] = (float) Math.random();
            }
            
            // Act
            double similarity = documentDatabaseService.calculateCosineSimilarity(embedding1, embedding2);
            
            // Assert
            assertThat(similarity).isBetween(-1.0, 1.0);
        }
    }
    
    @Nested
    @DisplayName("Document Entry Creation")
    class DocumentEntryCreation {
        
        @Test
        @DisplayName("should create document entry with all fields")
        void shouldCreateDocumentEntryWithAllFields() {
            // Arrange
            String category = "selenium";
            String title = "WebDriver Timeout Issue";
            String content = "Full content here";
            String summary = "Summary of the issue";
            String rootCauses = "Root cause analysis";
            String resolutionSteps = "Step by step resolution";
            String tags = "webdriver,timeout,selenium";
            
            // Act
            DocumentEntry entry = new DocumentEntry(category, title, content, summary, rootCauses, resolutionSteps, tags);
            
            // Assert
            assertThat(entry.getCategory()).isEqualTo(category);
            assertThat(entry.getTitle()).isEqualTo(title);
            assertThat(entry.getContent()).isEqualTo(content);
            assertThat(entry.getSummary()).isEqualTo(summary);
            assertThat(entry.getRootCauses()).isEqualTo(rootCauses);
            assertThat(entry.getResolutionSteps()).isEqualTo(resolutionSteps);
            assertThat(entry.getTags()).isEqualTo(tags);
        }
        
        @Test
        @DisplayName("should create document entry with null optional fields")
        void shouldCreateDocumentEntryWithNullOptionalFields() {
            // Arrange
            String category = "cucumber";
            String title = "Cucumber Test Failure";
            String content = "Full content here";
            
            // Act
            DocumentEntry entry = new DocumentEntry(category, title, content, null, null, null, null);
            
            // Assert
            assertThat(entry.getCategory()).isEqualTo(category);
            assertThat(entry.getTitle()).isEqualTo(title);
            assertThat(entry.getContent()).isEqualTo(content);
            assertThat(entry.getSummary()).isNull();
            assertThat(entry.getRootCauses()).isNull();
            assertThat(entry.getResolutionSteps()).isNull();
            assertThat(entry.getTags()).isNull();
        }
        
        @Test
        @DisplayName("should build embedding content correctly")
        void shouldBuildEmbeddingContentCorrectly() {
            // Arrange
            DocumentEntry entry = new DocumentEntry(
                "junit", "JUnit Test Failure", "Full content",
                "Test summary", "Root causes", "Resolution steps", "junit,test"
            );
            
            // Act
            String embeddingContent = entry.buildEmbeddingContent();
            
            // Assert
            assertThat(embeddingContent).contains("Title: JUnit Test Failure");
            assertThat(embeddingContent).contains("Summary: Test summary");
            assertThat(embeddingContent).contains("Root Causes: Root causes");
            assertThat(embeddingContent).contains("Resolution Steps: Resolution steps");
        }
        
        @Test
        @DisplayName("should build search content correctly")
        void shouldBuildSearchContentCorrectly() {
            // Arrange
            DocumentEntry entry = new DocumentEntry(
                "selenium", "Selenium Issue", "Full content",
                "Summary", "Root causes", "Resolution", "selenium,webdriver"
            );
            
            // Act
            String searchContent = entry.buildSearchContent();
            
            // Assert
            assertThat(searchContent).contains("Selenium Issue");
            assertThat(searchContent).contains("Summary");
            assertThat(searchContent).contains("Root causes");
            assertThat(searchContent).contains("Resolution");
            assertThat(searchContent).contains("selenium,webdriver");
        }
        
        @Test
        @DisplayName("should handle empty optional fields in embedding content")
        void shouldHandleEmptyOptionalFieldsInEmbeddingContent() {
            // Arrange
            DocumentEntry entry = new DocumentEntry(
                "test", "Test Title", "Content", "", "", "", ""
            );
            
            // Act
            String embeddingContent = entry.buildEmbeddingContent();
            
            // Assert
            assertThat(embeddingContent).contains("Title: Test Title");
            assertThat(embeddingContent).doesNotContain("Summary:");
            assertThat(embeddingContent).doesNotContain("Root Causes:");
            assertThat(embeddingContent).doesNotContain("Resolution Steps:");
        }
        
        @Test
        @DisplayName("should handle null optional fields in search content")
        void shouldHandleNullOptionalFieldsInSearchContent() {
            // Arrange
            DocumentEntry entry = new DocumentEntry(
                "test", "Test Title", "Content", null, null, null, null
            );
            
            // Act
            String searchContent = entry.buildSearchContent();
            
            // Assert
            assertThat(searchContent).isEqualTo("Test Title");
        }
    }
    
    @Nested
    @DisplayName("Document Entry Properties")
    class DocumentEntryProperties {
        
        @Test
        @DisplayName("should set and get document ID")
        void shouldSetAndGetDocumentId() {
            // Arrange
            DocumentEntry entry = new DocumentEntry("test", "title", "content", null, null, null, null);
            long expectedId = 123L;
            
            // Act
            entry.setId(expectedId);
            
            // Assert
            assertThat(entry.getId()).isEqualTo(expectedId);
        }
        
        @Test
        @DisplayName("should set and get category")
        void shouldSetAndGetCategory() {
            // Arrange
            DocumentEntry entry = new DocumentEntry("test", "title", "content", null, null, null, null);
            String newCategory = "new-category";
            
            // Act
            entry.setCategory(newCategory);
            
            // Assert
            assertThat(entry.getCategory()).isEqualTo(newCategory);
        }
        
        @Test
        @DisplayName("should set and get title")
        void shouldSetAndGetTitle() {
            // Arrange
            DocumentEntry entry = new DocumentEntry("test", "title", "content", null, null, null, null);
            String newTitle = "New Title";
            
            // Act
            entry.setTitle(newTitle);
            
            // Assert
            assertThat(entry.getTitle()).isEqualTo(newTitle);
        }
        
        @Test
        @DisplayName("should set and get content")
        void shouldSetAndGetContent() {
            // Arrange
            DocumentEntry entry = new DocumentEntry("test", "title", "content", null, null, null, null);
            String newContent = "New content";
            
            // Act
            entry.setContent(newContent);
            
            // Assert
            assertThat(entry.getContent()).isEqualTo(newContent);
        }
        
        @Test
        @DisplayName("should set and get summary")
        void shouldSetAndGetSummary() {
            // Arrange
            DocumentEntry entry = new DocumentEntry("test", "title", "content", null, null, null, null);
            String summary = "Test summary";
            
            // Act
            entry.setSummary(summary);
            
            // Assert
            assertThat(entry.getSummary()).isEqualTo(summary);
        }
        
        @Test
        @DisplayName("should set and get root causes")
        void shouldSetAndGetRootCauses() {
            // Arrange
            DocumentEntry entry = new DocumentEntry("test", "title", "content", null, null, null, null);
            String rootCauses = "Test root causes";
            
            // Act
            entry.setRootCauses(rootCauses);
            
            // Assert
            assertThat(entry.getRootCauses()).isEqualTo(rootCauses);
        }
        
        @Test
        @DisplayName("should set and get resolution steps")
        void shouldSetAndGetResolutionSteps() {
            // Arrange
            DocumentEntry entry = new DocumentEntry("test", "title", "content", null, null, null, null);
            String resolutionSteps = "Test resolution steps";
            
            // Act
            entry.setResolutionSteps(resolutionSteps);
            
            // Assert
            assertThat(entry.getResolutionSteps()).isEqualTo(resolutionSteps);
        }
        
        @Test
        @DisplayName("should set and get tags")
        void shouldSetAndGetTags() {
            // Arrange
            DocumentEntry entry = new DocumentEntry("test", "title", "content", null, null, null, null);
            String tags = "test,tags,example";
            
            // Act
            entry.setTags(tags);
            
            // Assert
            assertThat(entry.getTags()).isEqualTo(tags);
        }
    }
    
    @Nested
    @DisplayName("Document Entry Object Methods")
    class DocumentEntryObjectMethods {
        
        @Test
        @DisplayName("should implement toString correctly")
        void shouldImplementToStringCorrectly() {
            // Arrange
            DocumentEntry entry = new DocumentEntry("test", "Test Title", "content", "summary", null, null, null);
            entry.setId(123L);
            
            // Act
            String toString = entry.toString();
            
            // Assert
            assertThat(toString).contains("DocumentEntry{");
            assertThat(toString).contains("id=123");
            assertThat(toString).contains("category='test'");
            assertThat(toString).contains("title='Test Title'");
            assertThat(toString).contains("summary='summary'");
        }
        
        @Test
        @DisplayName("should implement equals correctly")
        void shouldImplementEqualsCorrectly() {
            // Arrange
            DocumentEntry entry1 = new DocumentEntry("test", "title", "content", null, null, null, null);
            DocumentEntry entry2 = new DocumentEntry("test", "title", "content", null, null, null, null);
            DocumentEntry entry3 = new DocumentEntry("different", "title", "content", null, null, null, null);
            
            entry1.setId(1L);
            entry2.setId(1L);
            entry3.setId(1L);
            
            // Act & Assert
            assertThat(entry1).isEqualTo(entry1); // Reflexive
            assertThat(entry1).isEqualTo(entry2); // Same values
            assertThat(entry2).isEqualTo(entry1); // Symmetric
            assertThat(entry1).isNotEqualTo(entry3); // Different values
            assertThat(entry1).isNotEqualTo(null); // Not null
            assertThat(entry1).isNotEqualTo("string"); // Different type
        }
        
        @Test
        @DisplayName("should implement hashCode correctly")
        void shouldImplementHashCodeCorrectly() {
            // Arrange
            DocumentEntry entry1 = new DocumentEntry("test", "title", "content", null, null, null, null);
            DocumentEntry entry2 = new DocumentEntry("test", "title", "content", null, null, null, null);
            
            entry1.setId(1L);
            entry2.setId(1L);
            
            // Act & Assert
            assertThat(entry1.hashCode()).isEqualTo(entry2.hashCode());
        }
    }
    
    @Nested
    @DisplayName("Embedding Type Enum")
    class EmbeddingTypeEnum {
        
        @Test
        @DisplayName("should have OPENAI and GEMINI values")
        void shouldHaveOpenAIAndGeminiValues() {
            // Act & Assert
            assertThat(DocumentDatabaseService.EmbeddingType.OPENAI).isNotNull();
            assertThat(DocumentDatabaseService.EmbeddingType.GEMINI).isNotNull();
        }
        
        @Test
        @DisplayName("should have correct enum values")
        void shouldHaveCorrectEnumValues() {
            // Act
            DocumentDatabaseService.EmbeddingType[] values = DocumentDatabaseService.EmbeddingType.values();
            
            // Assert
            assertThat(values).hasSize(2);
            assertThat(values).contains(DocumentDatabaseService.EmbeddingType.OPENAI);
            assertThat(values).contains(DocumentDatabaseService.EmbeddingType.GEMINI);
        }
    }
    
    @Nested
    @DisplayName("Document With Embedding Data Class")
    class DocumentWithEmbeddingDataClass {
        
        @Test
        @DisplayName("should set and get embedding")
        void shouldSetAndGetEmbedding() {
            // Arrange
            DocumentDatabaseService.DocumentWithEmbedding doc = new DocumentDatabaseService.DocumentWithEmbedding();
            float[] embedding = {1.0f, 2.0f, 3.0f};
            
            // Act
            doc.setEmbedding(embedding);
            
            // Assert
            assertThat(doc.getEmbedding()).isEqualTo(embedding);
        }
        
        @Test
        @DisplayName("should inherit from DocumentEntry")
        void shouldInheritFromDocumentEntry() {
            // Arrange
            DocumentDatabaseService.DocumentWithEmbedding doc = new DocumentDatabaseService.DocumentWithEmbedding();
            
            // Act & Assert
            assertThat(doc).isInstanceOf(DocumentEntry.class);
        }
    }
    
    @Nested
    @DisplayName("Document With Similarity Data Class")
    class DocumentWithSimilarityDataClass {
        
        @Test
        @DisplayName("should set and get similarity score")
        void shouldSetAndGetSimilarityScore() {
            // Arrange
            DocumentDatabaseService.DocumentWithSimilarity doc = new DocumentDatabaseService.DocumentWithSimilarity();
            double similarityScore = 0.85;
            
            // Act
            doc.setSimilarityScore(similarityScore);
            
            // Assert
            assertThat(doc.getSimilarityScore()).isEqualTo(similarityScore);
        }
        
        @Test
        @DisplayName("should inherit from DocumentEntry")
        void shouldInheritFromDocumentEntry() {
            // Arrange
            DocumentDatabaseService.DocumentWithSimilarity doc = new DocumentDatabaseService.DocumentWithSimilarity();
            
            // Act & Assert
            assertThat(doc).isInstanceOf(DocumentEntry.class);
        }
    }
    
    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesAndErrorHandling {
        
        @Test
        @DisplayName("should handle very long document content")
        void shouldHandleVeryLongDocumentContent() {
            // Arrange
            StringBuilder longContent = new StringBuilder();
            for (int i = 0; i < 10000; i++) {
                longContent.append("Very long document content line ").append(i).append(" ");
            }
            
            DocumentEntry entry = new DocumentEntry(
                "test", "Long Document", longContent.toString(), null, null, null, null
            );
            
            // Act
            String searchContent = entry.buildSearchContent();
            
            // Assert
            assertThat(searchContent).isNotNull();
            assertThat(searchContent).contains("Long Document");
        }
        
        @Test
        @DisplayName("should handle special characters in document content")
        void shouldHandleSpecialCharactersInDocumentContent() {
            // Arrange
            String specialContent = "Content with special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?";
            DocumentEntry entry = new DocumentEntry(
                "test", "Special Title", specialContent, null, null, null, null
            );
            
            // Act
            String searchContent = entry.buildSearchContent();
            
            // Assert
            assertThat(searchContent).contains("Special Title");
            // Note: buildSearchContent() only includes title and optional fields, not the main content
            assertThat(searchContent).isEqualTo("Special Title");
        }
        
        @Test
        @DisplayName("should handle unicode characters in document content")
        void shouldHandleUnicodeCharactersInDocumentContent() {
            // Arrange
            String unicodeContent = "Content with unicode: 你好世界 🌍 🚀";
            DocumentEntry entry = new DocumentEntry(
                "test", "Unicode Title", unicodeContent, null, null, null, null
            );
            
            // Act
            String searchContent = entry.buildSearchContent();
            
            // Assert
            assertThat(searchContent).contains("Unicode Title");
            // Note: buildSearchContent() only includes title and optional fields, not the main content
            assertThat(searchContent).isEqualTo("Unicode Title");
        }
        
        @Test
        @DisplayName("should handle empty strings in document fields")
        void shouldHandleEmptyStringsInDocumentFields() {
            // Arrange
            DocumentEntry entry = new DocumentEntry("", "", "", "", "", "", "");
            
            // Act
            String embeddingContent = entry.buildEmbeddingContent();
            String searchContent = entry.buildSearchContent();
            
            // Assert
            assertThat(embeddingContent).contains("Title: ");
            assertThat(searchContent).isEmpty();
        }
        
        @Test
        @DisplayName("should handle whitespace-only strings in document fields")
        void shouldHandleWhitespaceOnlyStringsInDocumentFields() {
            // Arrange
            DocumentEntry entry = new DocumentEntry("   ", "   ", "   ", "   ", "   ", "   ", "   ");
            
            // Act
            String embeddingContent = entry.buildEmbeddingContent();
            String searchContent = entry.buildSearchContent();
            
            // Assert
            assertThat(embeddingContent).contains("Title:    ");
            // Note: buildSearchContent() trims the result, so when all fields are whitespace-only, it becomes empty
            assertThat(searchContent).isEqualTo("");
        }
    }
}
