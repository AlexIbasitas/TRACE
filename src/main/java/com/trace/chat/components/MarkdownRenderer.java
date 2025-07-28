package com.trace.chat.components;

import com.intellij.openapi.diagnostic.Logger;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughSubscriptExtension;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import com.trace.common.constants.TriagePanelConstants;

/**
 * Professional markdown renderer using Flexmark Java library with Swing integration.
 *
 * <p>This class provides high-quality markdown to HTML conversion with professional
 * styling that works perfectly with Swing's JEditorPane. Flexmark Java is the most
 * comprehensive markdown library for Java, supporting all Commonmark features plus
 * extensions like tables, strikethrough, autolinks, and emojis.</p>
 *
 * <p>Uses Swing's JEditorPane with HTML content type for reliable rendering,
 * avoiding the HTML 3.2 limitations by using only supported HTML tags and inline styles.</p>
 *
 * @author Alex Ibasitas
 * @version 3.0
 * @since 1.0
 */
public final class MarkdownRenderer {

    private static final Logger LOG = Logger.getInstance(MarkdownRenderer.class);

    // Flexmark parser with comprehensive extensions
    private static final Parser PARSER = Parser.builder()
            .extensions(Arrays.asList(
                    TablesExtension.create(),
                    AutolinkExtension.create(),
                    StrikethroughSubscriptExtension.create()
            ))
            .build();

    // Flexmark HTML renderer with professional styling
    private static final HtmlRenderer RENDERER = HtmlRenderer.builder()
            .extensions(Arrays.asList(
                    TablesExtension.create(),
                    AutolinkExtension.create(),
                    StrikethroughSubscriptExtension.create()
            ))
            .escapeHtml(true)  // Security: escape HTML in markdown
            .build();

    private MarkdownRenderer() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Creates a JEditorPane with professionally rendered markdown content.
     *
     * <p>This method uses Flexmark Java to parse markdown and convert it to HTML
     * with professional styling that works perfectly with Swing's JEditorPane.
     * The result is a component that displays markdown with proper formatting,
     * typography, and visual hierarchy.</p>
     *
     * @param markdown The markdown text to render
     * @return A configured JEditorPane with styled markdown content
     */
    public static JEditorPane createMarkdownPane(String markdown) {
        if (markdown == null || markdown.trim().isEmpty()) {
            return createEmptyEditorPane();
        }

        try {
            // Parse markdown using Flexmark
            Node document = PARSER.parse(markdown);

            // Render to HTML with professional styling
            String html = RENDERER.render(document);

            // Create JEditorPane with HTML content
            JEditorPane editorPane = new JEditorPane();
            editorPane.setContentType("text/html");
            editorPane.setEditable(false);
            editorPane.setOpaque(false);
            editorPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
            editorPane.setAlignmentX(Component.LEFT_ALIGNMENT);
            editorPane.setAlignmentY(Component.TOP_ALIGNMENT);

            // Apply professional styling
            configureEditorPane(editorPane);

            // Set the styled HTML content
            String styledHtml = wrapHtmlWithStyling(html);
            LOG.info("Setting HTML content with font-size: 14px in CSS");
            LOG.info("HTML content length: " + styledHtml.length());
            LOG.info("HTML contains font-size: " + styledHtml.contains("font-size: 14px"));
            
            // Log a sample of the HTML to see what's being set
            if (styledHtml.length() > 200) {
                LOG.info("HTML sample (first 200 chars): " + styledHtml.substring(0, 200));
            }
            
            editorPane.setText(styledHtml);
            
            // Log the font after setting HTML content
            LOG.info("JEditorPane font after setting HTML: " + editorPane.getFont());
            LOG.info("JEditorPane font size after HTML: " + editorPane.getFont().getSize());
            
            // Also log the actual text content to see if HTML is being rendered
            String actualText = editorPane.getText();
            LOG.info("JEditorPane actual text length: " + actualText.length());
            LOG.info("JEditorPane text sample: " + (actualText.length() > 100 ? actualText.substring(0, 100) : actualText));

            return editorPane;

        } catch (Exception e) {
            LOG.warn("Error creating markdown pane with Flexmark, falling back to plain text: " + e.getMessage());
            return createFallbackEditorPane(markdown);
        }
    }

    /**
     * Configures the JEditorPane for optimal markdown display.
     *
     * @param editorPane The JEditorPane to configure
     */
    private static void configureEditorPane(JEditorPane editorPane) {
        // Apply consistent styling to match other chat elements
        LOG.info("Configuring JEditorPane with smaller font to match IDE");
        
        // Set a smaller font to match IDE's default text size
        Font smallerFont = new Font(TriagePanelConstants.FONT_FAMILY, Font.PLAIN, 11);
        editorPane.setFont(smallerFont);
        
        editorPane.setForeground(TriagePanelConstants.WHITE);
        editorPane.setBackground(TriagePanelConstants.PANEL_BACKGROUND);

        // Enable proper HTML rendering
        editorPane.putClientProperty("JEditorPane.honorDisplayProperties", Boolean.TRUE);

        // Log the final font after configuration
        LOG.info("JEditorPane font after configuration: " + editorPane.getFont());
        LOG.info("JEditorPane font size: " + editorPane.getFont().getSize());

        // JEditorPane doesn't have setLineWrap/setWrapStyleWord - these are JTextArea methods
        // JEditorPane handles wrapping automatically with its layout
    }

    /**
     * Wraps HTML content with professional CSS styling for Swing compatibility.
     *
     * <p>This method adds inline CSS styles that work with Swing's HTML 3.2 support,
     * providing professional typography and spacing while avoiding unsupported CSS features.</p>
     *
     * @param html The HTML content from Flexmark
     * @return HTML content with professional styling
     */
    private static String wrapHtmlWithStyling(String html) {
        LOG.info("Wrapping HTML with styling - input HTML length: " + html.length());
        String styledHtml = """
            <html>
            <head>
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
                        color: #ffffff;
                        background-color: transparent;
                        margin: 0;
                        padding: 0;
                        line-height: 1.6;
                        font-size: 11px;
                    }
                    h1 {
                        font-size: 12px;
                        font-weight: bold;
                        margin: 6px 0 3px 0;
                        color: #ffffff;
                        line-height: 1.3;
                    }
                    h2 {
                        font-size: 11px;
                        font-weight: bold;
                        margin: 4px 0 2px 0;
                        color: #ffffff;
                        line-height: 1.4;
                    }
                    h3 {
                        font-size: 11px;
                        font-weight: bold;
                        margin: 3px 0 2px 0;
                        color: #ffffff;
                        line-height: 1.4;
                    }
                    h4, h5, h6 {
                        font-size: 11px;
                        font-weight: bold;
                        margin: 2px 0 1px 0;
                        color: #ffffff;
                        line-height: 1.4;
                    }
                    strong {
                        font-weight: bold;
                        color: #ffffff;
                    }
                    em {
                        font-style: italic;
                        color: #ffffff;
                    }
                    del {
                        text-decoration: line-through;
                        color: #888888;
                    }
                    ul, ol {
                        margin: 4px 0;
                        padding-left: 20px;
                    }
                    li {
                        margin: 2px 0;
                        color: #ffffff;
                        line-height: 1.5;
                    }
                    code {
                        background-color: #2d2d2d;
                        padding: 1px 3px;
                        border-radius: 2px;
                        font-family: "SF Mono", Monaco, "Cascadia Code", "Roboto Mono", Consolas, "Courier New", monospace;
                        font-size: 10px;
                        color: #e6e6e6;
                        border: 1px solid #404040;
                    }
                    pre {
                        background-color: #1e1e1e;
                        padding: 12px;
                        border-radius: 6px;
                        margin: 8px 0;
                        overflow-x: auto;
                        border-left: 3px solid #4a9eff;
                        border: 1px solid #404040;
                    }
                    pre code {
                        background-color: transparent;
                        padding: 0;
                        font-size: 10px;
                        border: none;
                    }
                    blockquote {
                        border-left: 3px solid #4a9eff;
                        margin: 8px 0;
                        padding: 8px 12px;
                        background-color: #2a2a2a;
                        color: #e6e6e6;
                        border-radius: 0 4px 4px 0;
                    }
                    p {
                        margin: 4px 0;
                        line-height: 1.6;
                        color: #ffffff;
                    }
                    a {
                        color: #4a9eff;
                        text-decoration: none;
                    }
                    a:hover {
                        text-decoration: underline;
                    }
                    table {
                        border-collapse: collapse;
                        margin: 8px 0;
                        width: 100%;
                        border: 1px solid #404040;
                    }
                    th {
                        border: 1px solid #404040;
                        padding: 8px 12px;
                        text-align: left;
                        background-color: #2a2a2a;
                        font-weight: bold;
                        color: #ffffff;
                    }
                    td {
                        border: 1px solid #404040;
                        padding: 8px 12px;
                        text-align: left;
                        background-color: #1e1e1e;
                        color: #e6e6e6;
                    }
                    hr {
                        border: none;
                        border-top: 1px solid #404040;
                        margin: 16px 0;
                    }
                </style>
            </head>
            <body>
            """ + html + """
            </body>
            </html>
            """;
        
        LOG.info("Final styled HTML length: " + styledHtml.length());
        LOG.info("Final HTML contains font-size: " + styledHtml.contains("font-size: 14px"));
        return styledHtml;
    }

    /**
     * Creates an empty JEditorPane for fallback scenarios.
     *
     * @return A configured empty JEditorPane
     */
    private static JEditorPane createEmptyEditorPane() {
        JEditorPane editorPane = new JEditorPane();
        configureEditorPane(editorPane);
        return editorPane;
    }

    /**
     * Creates a fallback JEditorPane with plain text.
     *
     * @param text The text to display
     * @return A configured JEditorPane with plain text
     */
    private static JEditorPane createFallbackEditorPane(String text) {
        JEditorPane editorPane = new JEditorPane();
        configureEditorPane(editorPane);
        editorPane.setContentType("text/plain");
        editorPane.setText(text);
        return editorPane;
    }

    /**
     * Checks if the given text contains markdown patterns.
     *
     * <p>This method detects various markdown patterns including headers, emphasis,
     * lists, code blocks, links, tables, and other Flexmark features.</p>
     *
     * @param text The text to check
     * @return true if the text contains markdown patterns, false otherwise
     */
    public static boolean containsMarkdown(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        return text.contains("#") ||           // Headers
               text.contains("**") ||          // Bold text
               text.contains("*") ||           // Italic text
               text.contains("~~") ||          // Strikethrough
               text.contains("- ") ||          // Unordered lists
               text.contains("```") ||         // Code blocks
               text.contains("`") ||           // Inline code
               text.contains(">") ||           // Blockquotes
               text.contains("|") ||           // Tables
               text.contains("[") ||           // Links
               text.contains("!") ||           // Images
               text.contains("1.") ||          // Ordered lists
               text.contains("---") ||         // Horizontal rules
               text.contains("==") ||          // Highlighting
               text.contains("++") ||          // Underline
               text.contains("http://") ||     // Auto-links
               text.contains("https://") ||    // Auto-links
                               text.contains("~~"); // Strikethrough
    }
} 