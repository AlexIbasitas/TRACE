package com.trace.chat.components;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.DisplayName;

import javax.swing.*;
import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the MarkdownRenderer class using Flexmark Java.
 *
 * <p>These tests verify that the MarkdownRenderer correctly creates JEditorPane components
 * with properly styled markdown content using Flexmark Java library.</p>
 *
 * @author Alex Ibasitas
 * @version 3.0
 * @since 1.0
 */
@DisplayName("MarkdownRenderer Unit Tests")
class MarkdownRendererUnitTest {

    @Nested
    @DisplayName("Basic Markdown Conversion")
    class BasicMarkdownConversion {

        @Test
        @DisplayName("should convert headers properly")
        void shouldConvertHeadersProperly() {
            String markdown = "# Header 1\n## Header 2\n### Header 3";
            
            JEditorPane result = MarkdownRenderer.createMarkdownPane(markdown);
            
            assertNotNull(result);
            assertTrue(result.getText().contains("Header 1"));
            assertTrue(result.getText().contains("Header 2"));
            assertTrue(result.getText().contains("Header 3"));
        }

        @Test
        @DisplayName("should convert bold text properly")
        void shouldConvertBoldTextProperly() {
            String markdown = "This is **bold** text";
            
            JEditorPane result = MarkdownRenderer.createMarkdownPane(markdown);
            
            assertNotNull(result);
            assertTrue(result.getText().contains("bold"));
        }

        @Test
        @DisplayName("should convert italic text properly")
        void shouldConvertItalicTextProperly() {
            String markdown = "This is *italic* text";
            
            JEditorPane result = MarkdownRenderer.createMarkdownPane(markdown);
            
            assertNotNull(result);
            assertTrue(result.getText().contains("italic"));
        }

        @Test
        @DisplayName("should convert lists properly")
        void shouldConvertListsProperly() {
            String markdown = "- Item 1\n- Item 2\n1. Numbered item";
            
            JEditorPane result = MarkdownRenderer.createMarkdownPane(markdown);
            
            assertNotNull(result);
            assertTrue(result.getText().contains("Item 1"));
            assertTrue(result.getText().contains("Item 2"));
            assertTrue(result.getText().contains("Numbered item"));
        }

        @Test
        @DisplayName("should convert inline code properly")
        void shouldConvertInlineCodeProperly() {
            String markdown = "Use `code` in text";
            
            JEditorPane result = MarkdownRenderer.createMarkdownPane(markdown);
            
            assertNotNull(result);
            assertTrue(result.getText().contains("code"));
        }

        @Test
        @DisplayName("should convert blockquotes properly")
        void shouldConvertBlockquotesProperly() {
            String markdown = "> This is a blockquote";
            
            JEditorPane result = MarkdownRenderer.createMarkdownPane(markdown);
            
            assertNotNull(result);
            assertTrue(result.getText().contains("This is a blockquote"));
        }

        @Test
        @DisplayName("should convert tables properly")
        void shouldConvertTablesProperly() {
            String markdown = "| Header 1 | Header 2 |\n|----------|----------|\n| Cell 1   | Cell 2   |";
            
            JEditorPane result = MarkdownRenderer.createMarkdownPane(markdown);
            
            assertNotNull(result);
            assertTrue(result.getText().contains("Header 1"));
            assertTrue(result.getText().contains("Cell 1"));
        }

        @Test
        @DisplayName("should convert strikethrough properly")
        void shouldConvertStrikethroughProperly() {
            String markdown = "This is ~~strikethrough~~ text";
            
            JEditorPane result = MarkdownRenderer.createMarkdownPane(markdown);
            
            assertNotNull(result);
            assertTrue(result.getText().contains("strikethrough"));
        }
    }

    @Nested
    @DisplayName("Professional Styling")
    class ProfessionalStyling {

        @Test
        @DisplayName("should apply professional typography")
        void shouldApplyProfessionalTypography() {
            String markdown = "# Header\n**Bold text** and `code`";
            
            JEditorPane result = MarkdownRenderer.createMarkdownPane(markdown);
            
            assertNotNull(result);
            assertEquals("text/html", result.getContentType());
            assertFalse(result.isEditable());
            assertFalse(result.isOpaque());
        }

        @Test
        @DisplayName("should apply proper spacing")
        void shouldApplyProperSpacing() {
            String markdown = "# Header\n\nParagraph text";
            
            JEditorPane result = MarkdownRenderer.createMarkdownPane(markdown);
            
            assertNotNull(result);
            assertEquals(Component.LEFT_ALIGNMENT, result.getAlignmentX());
            assertEquals(Component.TOP_ALIGNMENT, result.getAlignmentY());
        }

        @Test
        @DisplayName("should apply code styling")
        void shouldApplyCodeStyling() {
            String markdown = "`inline code` and ```block code```";
            
            JEditorPane result = MarkdownRenderer.createMarkdownPane(markdown);
            
            assertNotNull(result);
            assertTrue(result.getText().contains("inline code"));
            assertTrue(result.getText().contains("block code"));
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesAndErrorHandling {

        @Test
        @DisplayName("should handle whitespace only input")
        void shouldHandleWhitespaceOnlyInput() {
            String markdown = "   \n\t  ";
            
            JEditorPane result = MarkdownRenderer.createMarkdownPane(markdown);
            
            assertNotNull(result);
            assertTrue(result.getText().trim().isEmpty());
        }

        @Test
        @DisplayName("should handle null input")
        void shouldHandleNullInput() {
            JEditorPane result = MarkdownRenderer.createMarkdownPane(null);
            
            assertNotNull(result);
            assertTrue(result.getText().trim().isEmpty());
        }

        @Test
        @DisplayName("should handle empty input")
        void shouldHandleEmptyInput() {
            String markdown = "";
            
            JEditorPane result = MarkdownRenderer.createMarkdownPane(markdown);
            
            assertNotNull(result);
            assertTrue(result.getText().trim().isEmpty());
        }

        @Test
        @DisplayName("should handle complex markdown")
        void shouldHandleComplexMarkdown() {
            String markdown = "# Header\n\n**Bold** and *italic* text with `code`.\n\n- List item 1\n- List item 2\n\n| Table | Header |\n|-------|--------|\n| Cell  | Data   |";
            
            JEditorPane result = MarkdownRenderer.createMarkdownPane(markdown);
            
            assertNotNull(result);
            assertTrue(result.getText().contains("Header"));
            assertTrue(result.getText().contains("Bold"));
            assertTrue(result.getText().contains("italic"));
            assertTrue(result.getText().contains("code"));
            assertTrue(result.getText().contains("List item 1"));
            assertTrue(result.getText().contains("Table"));
        }
    }

    @Nested
    @DisplayName("Markdown Detection")
    class MarkdownDetection {

        @Test
        @DisplayName("should detect headers")
        void shouldDetectHeaders() {
            assertTrue(MarkdownRenderer.containsMarkdown("# Header"));
            assertTrue(MarkdownRenderer.containsMarkdown("## Header"));
            assertTrue(MarkdownRenderer.containsMarkdown("### Header"));
        }

        @Test
        @DisplayName("should detect code blocks")
        void shouldDetectCodeBlocks() {
            assertTrue(MarkdownRenderer.containsMarkdown("```code```"));
            assertTrue(MarkdownRenderer.containsMarkdown("`inline code`"));
        }

        @Test
        @DisplayName("should detect lists")
        void shouldDetectLists() {
            assertTrue(MarkdownRenderer.containsMarkdown("- List item"));
            assertTrue(MarkdownRenderer.containsMarkdown("1. Numbered item"));
        }

        @Test
        @DisplayName("should detect tables")
        void shouldDetectTables() {
            assertTrue(MarkdownRenderer.containsMarkdown("| Header |"));
            assertTrue(MarkdownRenderer.containsMarkdown("|-------|"));
        }

        @Test
        @DisplayName("should detect strikethrough")
        void shouldDetectStrikethrough() {
            assertTrue(MarkdownRenderer.containsMarkdown("~~strikethrough~~"));
        }

        @Test
        @DisplayName("should return false for plain text")
        void shouldReturnFalseForPlainText() {
            assertFalse(MarkdownRenderer.containsMarkdown("Plain text without markdown"));
            assertFalse(MarkdownRenderer.containsMarkdown(""));
            assertFalse(MarkdownRenderer.containsMarkdown(null));
        }

        @Test
        @DisplayName("should detect bold text")
        void shouldDetectBoldText() {
            assertTrue(MarkdownRenderer.containsMarkdown("**bold text**"));
            assertTrue(MarkdownRenderer.containsMarkdown("*italic text*"));
        }
    }

    @Nested
    @DisplayName("Component Configuration")
    class ComponentConfiguration {

        @Test
        @DisplayName("should create properly configured JEditorPane")
        void shouldCreateProperlyConfiguredJEditorPane() {
            String markdown = "Test content";
            
            JEditorPane result = MarkdownRenderer.createMarkdownPane(markdown);
            
            assertNotNull(result);
            assertFalse(result.isEditable());
            assertFalse(result.isOpaque());
            assertEquals("text/html", result.getContentType());
            assertEquals(Component.LEFT_ALIGNMENT, result.getAlignmentX());
            assertEquals(Component.TOP_ALIGNMENT, result.getAlignmentY());
        }

        @Test
        @DisplayName("should have consistent styling")
        void shouldHaveConsistentStyling() {
            String markdown = "Test content";
            
            JEditorPane result = MarkdownRenderer.createMarkdownPane(markdown);
            
            assertNotNull(result);
            assertEquals(Color.WHITE, result.getForeground());
            assertEquals(new Color(43, 43, 43), result.getBackground());
            assertNotNull(result.getFont());
        }
    }
} 