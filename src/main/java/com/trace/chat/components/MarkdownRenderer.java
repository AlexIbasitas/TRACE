package com.trace.chat.components;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.ui.UIUtil;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughSubscriptExtension;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.MouseWheelEvent;
import java.util.Arrays;
import com.trace.common.constants.TriagePanelConstants;
import com.trace.common.utils.ThemeUtils;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import javax.swing.text.html.HTMLDocument;

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
     * Creates a component for markdown content, with scrollable code blocks for wide Java code.
     * 
     * <p>This method detects wide Java code blocks and creates a mixed component layout:
     * - Wide code blocks become horizontally scrollable JScrollPane components
     * - All other content uses the standard JEditorPane with HTML rendering</p>
     * 
     * @param markdown The markdown text to render
     * @return A JComponent containing the markdown content with appropriate scrolling
     */
    public static JComponent createMarkdownComponent(String markdown) {
        if (markdown == null || markdown.trim().isEmpty()) {
            return createEmptyEditorPane();
        }

        try {
            // Check if the markdown contains Java code blocks
            if (CodeBlockProcessingHelper.containsJavaCodeBlocks(markdown)) {
                return CodeBlockProcessingHelper.createMixedMarkdownComponent(markdown);
            } else {
                // Use standard markdown pane for normal content
                return createMarkdownPane(markdown);
            }
        } catch (Exception e) {
            LOG.warn("Error creating markdown component, falling back to plain text: " + e.getMessage());
            return createFallbackEditorPane(markdown);
        }
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

            // Create responsive JEditorPane with HTML content
            JEditorPane editorPane = new ResponsiveHtmlPane();
            editorPane.setContentType("text/html");
            editorPane.setEditable(false);
            editorPane.setOpaque(false);
            // Increase bottom inset to prevent last line clipping in dynamic wrap scenarios
            editorPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));
            editorPane.setAlignmentX(Component.LEFT_ALIGNMENT);
            editorPane.setAlignmentY(Component.TOP_ALIGNMENT);

            // Apply professional styling
            configureEditorPane(editorPane);

            // Force wrapping behavior and a stylesheet that sets text color to white
            try {
                HTMLEditorKit kit = new WrappingHtmlEditorKit();
                editorPane.setEditorKit(kit);
                // Create a fresh HTMLDocument and apply rules at the document level for max precedence
                HTMLDocument doc = (HTMLDocument) kit.createDefaultDocument();
                StyleSheet docSheet = doc.getStyleSheet();
                docSheet.addRule("body, p, li, ul, ol, h1, h2, h3, h4, h5, h6, span, div, td, th, a, b, i { color:#ffffff; }");
                docSheet.addRule("code, pre { color:#e6e6e6; }");
                
                // Add background color for code blocks using theme-aware colors
                String codeBackgroundColor = ThemeUtils.toHex(ThemeUtils.codeBackground());
                docSheet.addRule("pre { background-color:" + codeBackgroundColor + "; padding:8px; border-radius:4px; }");
                
                // Add background color for inline code blocks
                String inlineCodeBackgroundColor = ThemeUtils.toHex(ThemeUtils.inlineCodeBackground());
                docSheet.addRule("code { background-color:" + inlineCodeBackgroundColor + "; padding:2px 4px; border-radius:2px; }");
                
                // Set base body text size to use IDE's default font size
                int baseFontSize = UIUtil.getLabelFont().getSize();
                docSheet.addRule("body, p, li, ul, ol, span, div, td, th, a, b, i { font-size:" + baseFontSize + "px; }");
                // Tighten vertical spacing globally (supported subset of CSS in Swing)
                docSheet.addRule("p { margin-top:2px; margin-bottom:2px; }");
                docSheet.addRule("ul, ol { margin-top:2px; margin-bottom:2px; }");
                docSheet.addRule("li { margin-top:0px; margin-bottom:2px; }");
                docSheet.addRule("pre { margin-top:3px; margin-bottom:3px; }");
                // Add slightly larger bottom padding to avoid last-line clipping during dynamic wrap
                docSheet.addRule("body { padding-bottom:12px; }");
                editorPane.setDocument(doc);
            } catch (Exception ex) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("HTMLEditorKit stylesheet initialization issue (non-critical): " + ex.getMessage());
                }
            }

            // Set the styled HTML content (with safe post-processing)
            String styledHtml = HTMLProcessingHelper.wrapHtmlWithStyling(html);
            editorPane.setText(styledHtml);
            
            // Trigger reflow after text is set
            ApplicationManager.getApplication().invokeLater(() -> {
                if (editorPane instanceof ResponsiveHtmlPane) {
                    ((ResponsiveHtmlPane) editorPane).applyWidthFromParent();
                }
            });

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
        // Use JetBrains standard font that responds to IDE font size changes
        Font dynamicFont = UIUtil.getLabelFont();
        editorPane.setFont(dynamicFont);
        
        editorPane.setForeground(ThemeUtils.textForeground());
        editorPane.setBackground(ThemeUtils.panelBackground());

        // Enable proper HTML rendering
        editorPane.putClientProperty("JEditorPane.honorDisplayProperties", Boolean.TRUE);
    }

    /**
     * Creates an empty JEditorPane for fallback scenarios.
     *
     * @return A configured empty JEditorPane
     */
    private static JEditorPane createEmptyEditorPane() {
        JEditorPane editorPane = new ResponsiveHtmlPane();
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
        JEditorPane editorPane = new ResponsiveHtmlPane();
        configureEditorPane(editorPane);
        editorPane.setContentType("text/plain");
        editorPane.setText(text);
        return editorPane;
    }

    /**
     * A responsive HTML pane that tracks parent width and reflows HTML content accordingly.
     */
    static final class ResponsiveHtmlPane extends JEditorPane {
        private int lastAppliedWidth = -1;

        ResponsiveHtmlPane() {
            super();
            setEditable(false);
            setOpaque(false);
            putClientProperty("JEditorPane.honorDisplayProperties", Boolean.TRUE);

            // Re-apply sizing on hierarchy changes
            addHierarchyListener(new HierarchyListener() {
                @Override
                public void hierarchyChanged(HierarchyEvent e) {
                    if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0
                        || (e.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED) != 0
                        || (e.getChangeFlags() & HierarchyEvent.PARENT_CHANGED) != 0) {
                        ApplicationManager.getApplication().invokeLater(ResponsiveHtmlPane.this::applyWidthFromParent);
                    }
                }
            });

            // Also react to our own resize
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    applyWidthFromParent();
                }
            });
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            // If we ever end up directly inside a viewport, track its width
            return true;
        }

        @Override
        public Dimension getPreferredSize() {
            try {
                int targetWidth = computeTargetWidth();
                if (targetWidth > 0) {
                    super.setSize(new Dimension(targetWidth, Integer.MAX_VALUE));
                    Dimension pref = super.getPreferredSize();
                    
                    // Calculate dynamic buffer based on current font size
                    int baseFontSize = UIUtil.getLabelFont().getSize();
                    int dynamicBuffer = Math.max(16, baseFontSize);
                    
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("ResponsiveHtmlPane.getPreferredSize() - targetWidth: " + targetWidth + 
                                 ", baseFontSize: " + baseFontSize + ", dynamicBuffer: " + dynamicBuffer + 
                                 ", final height: " + (pref.height + dynamicBuffer));
                    }
                    
                    // Add dynamic buffer to prevent last-line clipping during text wrapping
                    return new Dimension(targetWidth, pref.height + dynamicBuffer);
                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("ResponsiveHtmlPane.getPreferredSize() - targetWidth <= 0: " + targetWidth);
                    }
                }
            } catch (Exception ex) {
                LOG.warn("ResponsiveHtmlPane.getPreferredSize() - Exception: " + ex.getMessage(), ex);
                // fall back to default behavior
            }
            return super.getPreferredSize();
        }
        
        @Override
        public Dimension getMaximumSize() {
            Dimension pref = getPreferredSize();
            return new Dimension(Integer.MAX_VALUE, pref.height);
        }

        @Override
        public void setText(String t) {
            super.setText(t);
            ApplicationManager.getApplication().invokeLater(this::applyWidthFromParent);
        }

        /**
         * Applies width from parent container and recalculates preferred size.
         */
        void applyWidthFromParent() {
            try {
                int targetWidth = computeTargetWidth();
                
                if (targetWidth <= 0) {
                    return;
                }
                
                if (targetWidth != lastAppliedWidth) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("ResponsiveHtmlPane.applyWidthFromParent() - width change: " + lastAppliedWidth + " -> " + targetWidth);
                    }
                    
                    // First set an arbitrarily large height to allow proper preferred size computation
                    super.setSize(new Dimension(targetWidth, Integer.MAX_VALUE));
                    Dimension pref = getPreferredSize();
                    
                    // Calculate dynamic buffer based on current font size for consistent sizing
                    int baseFontSize = UIUtil.getLabelFont().getSize();
                    int dynamicBuffer = Math.max(16, baseFontSize);
                    
                    // Apply dynamic height buffer to account for reflow after wraps
                    Dimension finalSize = new Dimension(targetWidth, pref.height + dynamicBuffer);
                    super.setSize(finalSize);
                    
                    revalidate();
                    lastAppliedWidth = targetWidth;
                }
            } catch (Exception ex) {
                LOG.warn("ResponsiveHtmlPane.applyWidthFromParent() - Exception: " + ex.getMessage(), ex);
            }
        }

        /**
         * Computes the target width based on parent container constraints.
         *
         * @return The computed target width, or -1 if unable to determine
         */
        private int computeTargetWidth() {
            // Prefer enclosing viewport width and subtract cumulative insets of ancestor containers
            Container c = this;
            int safety = 0;
            JViewport viewport = null;
            while (c != null && safety++ < 20) {
                if (c instanceof JViewport) {
                    viewport = (JViewport) c;
                    break;
                }
                c = c.getParent();
            }
            if (viewport != null) {
                Component view = viewport.getView();
                int baseWidth = viewport.getExtentSize().width;
                int cumulativeInsets = 0;
                // Sum insets of all ancestors between the viewport view and this pane
                Container walker = this;
                int guard = 0;
                while (walker != null && walker != view && guard++ < 30) {
                    if (walker instanceof JComponent) {
                        Insets ins = ((JComponent) walker).getInsets();
                        if (ins != null) {
                            cumulativeInsets += Math.max(0, ins.left) + Math.max(0, ins.right);
                        }
                    }
                    walker = walker.getParent();
                }
                // Also subtract our own insets
                Insets selfIns = getInsets();
                if (selfIns != null) {
                    cumulativeInsets += Math.max(0, selfIns.left) + Math.max(0, selfIns.right);
                }
                int width = baseWidth - cumulativeInsets;
                return Math.max(width, -1);
            }
            // Fallback to nearest parent with non-zero width
            Container parent = getParent();
            safety = 0;
            while (parent != null && parent.getWidth() == 0 && safety++ < 10) {
                parent = parent.getParent();
            }
            if (parent == null) {
                return -1;
            }
            int width = parent.getWidth();
            Insets selfIns = getInsets();
            if (selfIns != null) {
                width -= Math.max(0, selfIns.left) + Math.max(0, selfIns.right);
            }
            return Math.max(width, -1);
        }
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
               text.contains("~~");            // Strikethrough
    }

    /**
     * Re-applies the current theme stylesheet to an existing HTML JEditorPane.
     * Uses dynamic theme-aware colors instead of hardcoded values.
     *
     * @param pane The JEditorPane to update
     */
    public static void reapplyThemeStyles(JEditorPane pane) {
        if (pane == null) return;
        try {
            LOG.info("Reapplying theme styles to JEditorPane");
            
            // DON'T set component colors - they override HTML CSS
            // Let HTML CSS handle all color styling
            pane.setOpaque(false); // Prevent component background from showing through
            
            // Get current HTML content and re-wrap it with new theme colors
            String currentHtml = pane.getText();
            if (currentHtml != null && currentHtml.contains("<html>")) {
                // Extract the content between <body> tags
                String bodyContent = extractBodyContent(currentHtml);
                if (bodyContent != null) {
                    // Re-wrap with new theme colors
                    String newHtml = HTMLProcessingHelper.wrapHtmlWithStyling(bodyContent);
                    pane.setText(newHtml);
                    LOG.info("Re-wrapped HTML content with new theme colors");
                }
            }
            
            // Force a repaint to apply the new colors
            pane.revalidate();
            pane.repaint();
            
            LOG.info("Theme styles reapplied successfully");
        } catch (Exception e) {
            LOG.warn("Error reapplying theme styles: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts the content between <body> tags from HTML.
     *
     * @param html The HTML content to parse
     * @return The body content, or null if not found
     */
    private static String extractBodyContent(String html) {
        try {
            int bodyStart = html.indexOf("<body>");
            int bodyEnd = html.indexOf("</body>");
            if (bodyStart != -1 && bodyEnd != -1 && bodyEnd > bodyStart) {
                return html.substring(bodyStart + 6, bodyEnd).trim();
            }
        } catch (Exception e) {
            LOG.warn("Error extracting body content: " + e.getMessage());
        }
        return null;
    }
}