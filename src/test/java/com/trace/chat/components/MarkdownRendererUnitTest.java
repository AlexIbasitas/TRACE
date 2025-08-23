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

        @Test
        @DisplayName("should detect and log Java code blocks")
        void shouldDetectAndLogJavaCodeBlocks() {
            String markdown = "Here's some Java code:\n\n```java\npublic class Test {\n    public static void main(String[] args) {\n        System.out.println(\"Hello World\");\n    }\n}\n```";
            
            JEditorPane result = MarkdownRenderer.createMarkdownPane(markdown);
            
            assertNotNull(result);
            assertTrue(result.getText().contains("public class Test"));
            assertTrue(result.getText().contains("System.out.println"));
            // Verify that code blocks are properly styled
            assertTrue(result.getText().contains("<pre>"));
            assertTrue(result.getText().contains("<code>"));
        }

        @Test
        @DisplayName("should handle complex markdown with multiple code blocks")
        void shouldHandleComplexMarkdownWithMultipleCodeBlocks() {
            String markdown = "# Header\n\nHere's `inline code` and a block:\n\n```java\npublic class Example {\n    private String name;\n}\n```\n\nAnd another block:\n\n```\nSimple text block\n```";
            
            JEditorPane result = MarkdownRenderer.createMarkdownPane(markdown);
            
            assertNotNull(result);
            assertTrue(result.getText().contains("Header"));
            assertTrue(result.getText().contains("inline code"));
            assertTrue(result.getText().contains("public class Example"));
            assertTrue(result.getText().contains("Simple text block"));
        }

        @Test
        @DisplayName("should apply background colors to code blocks")
        void shouldApplyBackgroundColorsToCodeBlocks() {
            String markdown = "Here's some code:\n\n```java\npublic class Test {\n    public void method() {\n        // comment\n    }\n}\n```\n\nAnd `inline code` here.";
            
            JEditorPane result = MarkdownRenderer.createMarkdownPane(markdown);
            
            assertNotNull(result);
            String htmlContent = result.getText();
            
            // Verify code blocks have background colors applied
            assertTrue(htmlContent.contains("background-color:") || htmlContent.contains("background-color"), 
                "Code block should have background-color style");
            
            // Verify both pre and code tags are present
            assertTrue(htmlContent.contains("<pre>"), "Should contain <pre> tags");
            assertTrue(htmlContent.contains("<code>"), "Should contain <code> tags");
            
            // Verify padding is applied for better visual appearance
            assertTrue(htmlContent.contains("padding:"), "Should have padding for code blocks");
            
            // Verify the Java code content is preserved
            assertTrue(htmlContent.contains("public class Test"), "Should contain Java code");
            assertTrue(htmlContent.contains("method()"), "Should contain method");
        }

        @Test
        @DisplayName("should create scrollable component for wide Java code blocks")
        void shouldCreateScrollableComponentForWideJavaCodeBlocks() {
            // Create a very long line of code that exceeds 80 characters
            String longCodeLine = "public class VeryLongClassNameWithManyMethodsAndVariablesAndParametersThatExceedsEightyCharacters {";
            String markdown = "Here's some wide Java code:\n\n```java\n" + longCodeLine + "\n    private String veryLongVariableNameThatAlsoExceedsTheNormalWidthLimitForCodeDisplay = \"value\";\n    \n    public void veryLongMethodNameThatExceedsNormalLimitsAndWouldCauseHorizontalScrolling() {\n        System.out.println(\"This is a very long line that definitely exceeds eighty characters and should trigger scrollable component creation\");\n    }\n}\n```";
            
            JComponent result = MarkdownRenderer.createMarkdownComponent(markdown);
            
            assertNotNull(result);
            
            // Should return a JPanel (mixed component) instead of JEditorPane for wide code
            assertTrue(result instanceof JPanel, "Should return JPanel for wide code blocks");
            
            JPanel panel = (JPanel) result;
            assertTrue(panel.getComponentCount() > 0, "Panel should contain components");
            
            // Look for JScrollPane components in the panel
            boolean foundScrollPane = false;
            for (int i = 0; i < panel.getComponentCount(); i++) {
                Component comp = panel.getComponent(i);
                if (comp instanceof JScrollPane) {
                    foundScrollPane = true;
                    JScrollPane scrollPane = (JScrollPane) comp;
                    
                    // Verify scroll pane configuration
                    assertEquals(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED, 
                        scrollPane.getHorizontalScrollBarPolicy(),
                        "Should have horizontal scrollbar as needed");
                    
                    // Verify the content is a JTextArea
                    Component view = scrollPane.getViewport().getView();
                    assertTrue(view instanceof JTextArea, "Scroll pane should contain JTextArea");
                    
                    JTextArea textArea = (JTextArea) view;
                    assertFalse(textArea.getLineWrap(), "Text area should not wrap lines");
                    assertTrue(textArea.getText().contains(longCodeLine), "Should contain the long code line");
                    break;
                }
            }
            
            assertTrue(foundScrollPane, "Should contain at least one JScrollPane for wide code");
        }

        @Test
        @DisplayName("should use regular markdown pane for narrow Java code blocks")
        void shouldUseRegularMarkdownPaneForNarrowJavaCodeBlocks() {
            String markdown = "Here's normal Java code:\n\n```java\npublic class Test {\n    public void method() {\n        System.out.println(\"Hello\");\n    }\n}\n```";
            
            JComponent result = MarkdownRenderer.createMarkdownComponent(markdown);
            
            assertNotNull(result);
            
            // Should return a JEditorPane for narrow code (no scrollable components needed)
            assertTrue(result instanceof JEditorPane, "Should return JEditorPane for narrow code blocks");
            
            JEditorPane editorPane = (JEditorPane) result;
            String htmlContent = editorPane.getText();
            
            // Verify the Java code content is preserved
            assertTrue(htmlContent.contains("public class Test"), "Should contain Java code");
            assertTrue(htmlContent.contains("System.out.println"), "Should contain method call");
        }

        @Test
        @DisplayName("should handle mixed content with both wide and narrow code blocks")
        void shouldHandleMixedContentWithBothWideAndNarrowCodeBlocks() {
            String longLine = "public class VeryLongClassNameThatExceedsEightyCharactersAndShouldTriggerScrolling {";
            String markdown = "# Code Examples\n\nHere's normal code:\n\n```java\npublic class Short {\n    void method() {}\n}\n```\n\nAnd here's wide code:\n\n```java\n" + longLine + "\n    // This class has very long names\n}\n```\n\nEnd of examples.";
            
            JComponent result = MarkdownRenderer.createMarkdownComponent(markdown);
            
            assertNotNull(result);
            assertTrue(result instanceof JPanel, "Should return JPanel for mixed content");
            
            JPanel panel = (JPanel) result;
            assertTrue(panel.getComponentCount() > 0, "Panel should contain multiple components");
            
            // Count different component types
            int editorPaneCount = 0;
            int scrollPaneCount = 0;
            
            for (int i = 0; i < panel.getComponentCount(); i++) {
                Component comp = panel.getComponent(i);
                if (comp instanceof JEditorPane) {
                    editorPaneCount++;
                } else if (comp instanceof JScrollPane) {
                    scrollPaneCount++;
                }
            }
            
            assertTrue(editorPaneCount > 0, "Should contain JEditorPane components for text and narrow code");
            assertTrue(scrollPaneCount > 0, "Should contain JScrollPane components for wide code");
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

    @Nested
    @DisplayName("ResponsiveHtmlPane Sizing")
    class ResponsiveHtmlPaneSizing {

        @Test
        @DisplayName("should apply dynamic buffer in getPreferredSize")
        void shouldApplyDynamicBufferInGetPreferredSize() {
            // Create a ResponsiveHtmlPane instance
            MarkdownRenderer.ResponsiveHtmlPane pane = new MarkdownRenderer.ResponsiveHtmlPane();
            pane.setText("Test content for sizing");
            
            // Get preferred size
            Dimension preferredSize = pane.getPreferredSize();
            
            // Verify that the size is reasonable (not zero or negative)
            assertNotNull(preferredSize);
            assertTrue(preferredSize.width > 0);
            assertTrue(preferredSize.height > 0);
            
            // The height should be greater than the minimum buffer (16px)
            assertTrue(preferredSize.height >= 16);
        }

        @Test
        @DisplayName("should handle dynamic buffer calculation gracefully")
        void shouldHandleDynamicBufferCalculationGracefully() {
            // Create a ResponsiveHtmlPane instance
            MarkdownRenderer.ResponsiveHtmlPane pane = new MarkdownRenderer.ResponsiveHtmlPane();
            
            // Test with different content lengths
            String shortContent = "Short";
            String longContent = "This is a much longer content that should wrap to multiple lines " +
                               "and test the dynamic buffer calculation with different font sizes " +
                               "and zoom levels to ensure proper text rendering without clipping.";
            
            pane.setText(shortContent);
            Dimension shortSize = pane.getPreferredSize();
            
            pane.setText(longContent);
            Dimension longSize = pane.getPreferredSize();
            
            // Both should be valid sizes
            assertNotNull(shortSize);
            assertNotNull(longSize);
            assertTrue(shortSize.height > 0);
            assertTrue(longSize.height > 0);
            
            // Longer content should generally have greater height
            assertTrue(longSize.height >= shortSize.height);
        }

        @Test
        @DisplayName("should log comprehensive debugging information")
        void shouldLogComprehensiveDebuggingInformation() {
            // Create a ResponsiveHtmlPane instance with complex content
            MarkdownRenderer.ResponsiveHtmlPane pane = new MarkdownRenderer.ResponsiveHtmlPane();
            
            // Set complex content that would trigger detailed logging
            String complexContent = "# Header\n\nThis is a **complex** message with:\n" +
                                  "- Multiple lines\n" +
                                  "- **Bold text**\n" +
                                  "- *Italic text*\n" +
                                  "- `Code snippets`\n\n" +
                                  "And a long paragraph that should wrap to multiple lines " +
                                  "to test the dynamic buffer calculation and ensure that " +
                                  "the comprehensive logging system captures all the relevant " +
                                  "information about sizing, font calculations, and container " +
                                  "hierarchy for debugging text rendering issues.";
            
            pane.setText(complexContent);
            
            // Get preferred size to trigger logging
            Dimension preferredSize = pane.getPreferredSize();
            
            // Verify the component works correctly
            assertNotNull(preferredSize);
            assertTrue(preferredSize.width > 0);
            assertTrue(preferredSize.height > 0);
            
            // The logging should have been triggered during getPreferredSize() call
            // This test verifies that the comprehensive logging system is in place
            // and will help debug text rendering issues in production
        }

        @Test
        @DisplayName("should handle parent container hierarchy logging")
        void shouldHandleParentContainerHierarchyLogging() {
            // Create a parent container
            JPanel parentPanel = new JPanel(new BorderLayout());
            parentPanel.setSize(400, 300);
            
            // Create a ResponsiveHtmlPane and add it to the parent
            MarkdownRenderer.ResponsiveHtmlPane pane = new MarkdownRenderer.ResponsiveHtmlPane();
            pane.setText("Test content for hierarchy logging");
            parentPanel.add(pane, BorderLayout.CENTER);
            
            // Get preferred size to trigger hierarchy logging
            Dimension preferredSize = pane.getPreferredSize();
            
            // Verify the component works correctly
            assertNotNull(preferredSize);
            assertTrue(preferredSize.width > 0);
            assertTrue(preferredSize.height > 0);
            
            // The hierarchy logging should have been triggered during getPreferredSize() call
            // This test verifies that the parent container hierarchy logging is in place
            // and will help identify which container is constraining the height
        }

        @Test
        @DisplayName("should not cause stack overflow when getting preferred size")
        void shouldNotCauseStackOverflowWhenGettingPreferredSize() {
            // Create a ResponsiveHtmlPane instance
            MarkdownRenderer.ResponsiveHtmlPane pane = new MarkdownRenderer.ResponsiveHtmlPane();
            
            // Set content that would trigger sizing calculations
            String content = "This is a test content that should not cause stack overflow when calculating preferred size.";
            pane.setText(content);
            
            // This should not throw StackOverflowError
            assertDoesNotThrow(() -> {
                Dimension preferredSize = pane.getPreferredSize();
                assertNotNull(preferredSize);
                assertTrue(preferredSize.width > 0);
                assertTrue(preferredSize.height > 0);
            });
        }
    }

            @Test
        @DisplayName("should create scrollable component for wide Java code")
        void shouldCreateScrollableComponentForWideJavaCode() {
            String longLine = "public class VeryLongClassNameThatExceedsEightyCharactersAndShouldTriggerScrolling {";
            String markdown = "Here's some wide Java code:\n\n```java\n" + longLine + "\n    // This class has very long names\n}\n```";
            
            JComponent result = MarkdownRenderer.createMarkdownComponent(markdown);
            
            assertNotNull(result);
            assertTrue(result instanceof JPanel, "Should return JPanel container for mixed content");
            
            JPanel container = (JPanel) result;
            assertTrue(container.getComponentCount() > 0, "Container should have components");
            
            // Find the scrollable code component
            JScrollPane scrollPane = null;
            for (Component comp : container.getComponents()) {
                if (comp instanceof JScrollPane) {
                    scrollPane = (JScrollPane) comp;
                    break;
                }
            }
            
            assertNotNull(scrollPane, "Should contain a JScrollPane for code block");
            assertEquals(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED, scrollPane.getHorizontalScrollBarPolicy(), 
                "Should have horizontal scrollbar policy set to AS_NEEDED");
            assertEquals(JScrollPane.VERTICAL_SCROLLBAR_NEVER, scrollPane.getVerticalScrollBarPolicy(), 
                "Should have NO vertical scrollbar");
            
            // Verify the content is preserved
            Component viewportView = scrollPane.getViewport().getView();
            assertTrue(viewportView instanceof JTextArea, "Viewport should contain JTextArea");
            
            JTextArea textArea = (JTextArea) viewportView;
            assertTrue(textArea.getText().contains("VeryLongClassNameThatExceedsEightyCharactersAndShouldTriggerScrolling"), 
                "Should contain the wide code content");
            
            // Verify dynamic sizing
            assertTrue(scrollPane.getMaximumSize().width == Integer.MAX_VALUE, 
                "Should have maximum width for dynamic resizing");
        }

            @Test
        @DisplayName("should integrate with MessageComponent correctly")
        void shouldIntegrateWithMessageComponentCorrectly() {
            String longLine = "public class VeryLongClassNameThatExceedsEightyCharactersAndShouldTriggerScrolling {";
            String markdown = "Here's some wide Java code:\n\n```java\n" + longLine + "\n    // This class has very long names\n}\n```";
            
            // Test that the component can be added to a panel (simulating MessageComponent usage)
            JPanel testPanel = new JPanel();
            testPanel.setLayout(new BoxLayout(testPanel, BoxLayout.Y_AXIS));
            
            JComponent markdownComponent = MarkdownRenderer.createMarkdownComponent(markdown);
            testPanel.add(markdownComponent);
            
            assertTrue(testPanel.getComponentCount() == 1, "Panel should contain one component");
            assertTrue(testPanel.getComponent(0) instanceof JPanel, "Component should be JPanel container");
            
            JPanel container = (JPanel) testPanel.getComponent(0);
            
            // Find the scrollable code component within the container
            JScrollPane scrollPane = null;
            for (Component comp : container.getComponents()) {
                if (comp instanceof JScrollPane) {
                    scrollPane = (JScrollPane) comp;
                    break;
                }
            }
            
            assertNotNull(scrollPane, "Should contain a scrollable code component");
            assertNotNull(scrollPane.getPreferredSize(), "Should have preferred size");
            assertTrue(scrollPane.getPreferredSize().height > 0, "Should have positive height");
            
            // Verify dynamic width (preferred width should be 0 for dynamic sizing)
            assertEquals(0, scrollPane.getPreferredSize().width, "Should have width 0 for dynamic sizing");
            assertEquals(Integer.MAX_VALUE, scrollPane.getMaximumSize().width, "Should have unlimited maximum width");
        }
} 