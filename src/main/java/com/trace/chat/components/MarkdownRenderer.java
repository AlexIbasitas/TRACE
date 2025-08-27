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
            if (containsJavaCodeBlocks(markdown)) {
                return createMixedMarkdownComponent(markdown);
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
            String styledHtml = wrapHtmlWithStyling(html);
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
     * Wraps HTML content with professional CSS styling for Swing compatibility.
     *
     * <p>This method adds inline CSS styles that work with Swing's HTML 3.2 support,
     * providing professional typography and spacing while avoiding unsupported CSS features.</p>
     *
     * @param html The HTML content from Flexmark
     * @return HTML content with professional styling
     */
    private static String wrapHtmlWithStyling(String html) {
        // Keep HTML minimal and safe for Swing; prefer global stylesheet for colors
        String safeHtml = convertToLegacyHtmlTags(html);
        safeHtml = escapeHtmlCommentOpeners(safeHtml);
        // Optionally dim code/pre using inline element-level styles which Swing honors with highest precedence
        safeHtml = dimCodeAndPreColorsInline(safeHtml);
        // Apply inline heading styles (h1–h6) so headings render visually as headings in Swing
        safeHtml = applyHeadingStylesInline(safeHtml);
        // Replace heading tags with styled divs to bypass Swing's default heading sizing
        safeHtml = replaceHeadingsWithStyledDivs(safeHtml);

        // Use theme-aware colors instead of hardcoded white
        String textColor = ThemeUtils.toHex(ThemeUtils.textForeground());
        
        // Use dynamic font size based on IDE's default
        int baseFontSize = UIUtil.getLabelFont().getSize();
        String styledHtml = "<html><head></head><body style=\"color:" + textColor + "; font-size:" + baseFontSize + "px;\">" + safeHtml + "</body></html>";
        
        return styledHtml;
    }

    /**
     * Applies inline color to content inside <pre> and <code> to keep them slightly dimmer than body text.
     * Uses element-level attributes for maximum precedence in Swing's HTML renderer.
     */
    private static String dimCodeAndPreColorsInline(String html) {
        try {
            String updated = html;
            // Wrap <pre>...</pre> contents
            try {
                Pattern pre = Pattern.compile("(?is)<pre>(.*?)</pre>");
                Matcher m = pre.matcher(updated);
                StringBuffer sb = new StringBuffer();
                while (m.find()) {
                    String inner = m.group(1);
                    String replacement = "<pre><span style=\"color:#e6e6e6\">" + inner + "</span></pre>";
                    m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
                }
                m.appendTail(sb);
                updated = sb.toString();
            } catch (Exception ignore) { /* best-effort */ }

            // Wrap <code>...</code> contents
            try {
                Pattern code = Pattern.compile("(?is)<code>(.*?)</code>");
                Matcher m = code.matcher(updated);
                StringBuffer sb = new StringBuffer();
                while (m.find()) {
                    String inner = m.group(1);
                    String replacement = "<code><span style=\"color:#e6e6e6\">" + inner + "</span></code>";
                    m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
                }
                m.appendTail(sb);
                updated = sb.toString();
            } catch (Exception ignore) { /* best-effort */ }

            return updated;
        } catch (Exception e) {
            return html;
        }
    }

    /**
     * Adds inline styles to heading tags h1–h6 to ensure they render with larger font and bold
     * using element-level attributes which have the highest precedence per Swing's StyleSheet docs.
     */
    private static String applyHeadingStylesInline(String html) {
        try {
            String result = html;
            // Use proper markdown heading hierarchy based on IDE's default font size
            int baseFontSize = UIUtil.getLabelFont().getSize();
            result = applyHeadingStyleForLevel(result, 1, "font-size:" + (baseFontSize + 3) + "px; font-weight:bold; margin-top:6px; margin-bottom:4px");
            result = applyHeadingStyleForLevel(result, 2, "font-size:" + (baseFontSize + 2) + "px; font-weight:bold; margin-top:6px; margin-bottom:4px");
            result = applyHeadingStyleForLevel(result, 3, "font-size:" + (baseFontSize + 1) + "px; font-weight:bold; margin-top:5px; margin-bottom:3px");
            result = applyHeadingStyleForLevel(result, 4, "font-size:" + baseFontSize + "px; font-weight:bold; margin-top:5px; margin-bottom:3px");
            result = applyHeadingStyleForLevel(result, 5, "font-size:" + (baseFontSize - 1) + "px; font-weight:bold; margin-top:4px; margin-bottom:2px");
            result = applyHeadingStyleForLevel(result, 6, "font-size:" + (baseFontSize - 2) + "px; font-weight:bold; margin-top:4px; margin-bottom:2px");
            return result;
        } catch (Exception e) {
            return html;
        }
    }

    /**
     * Applies a specific style to heading tags of a given level.
     *
     * @param html The HTML content to process
     * @param level The heading level (1-6)
     * @param styleToAppend The CSS style to append
     * @return The HTML with applied heading styles
     */
    private static String applyHeadingStyleForLevel(String html, int level, String styleToAppend) {
        String tag = "h" + level;
        // Match the opening tag with any attributes: <hN ...>
        Pattern openTag = Pattern.compile("(?i)<" + tag + "\\b([^>]*)>");
        Matcher matcher = openTag.matcher(html);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String attrs = matcher.group(1) != null ? matcher.group(1) : ""; // includes leading space if present
            String newAttrs = attrs;
            try {
                // Find existing style="..."
                Pattern styleAttr = Pattern.compile("(?i)style\\s*=\\s*\"([^\"]*)\"");
                Matcher sm = styleAttr.matcher(attrs);
                if (sm.find()) {
                    String existing = sm.group(1) != null ? sm.group(1) : "";
                    String combined = existing.trim();
                    if (!combined.endsWith(";") && combined.length() > 0) {
                        combined = combined + "; ";
                    }
                    combined = combined + styleToAppend;
                    // Replace only the first style attribute occurrence
                    newAttrs = sm.replaceFirst("style=\"" + Matcher.quoteReplacement(combined) + "\"");
                } else {
                    // No style attribute: append one, preserving spacing
                    if (newAttrs == null || newAttrs.trim().isEmpty()) {
                        newAttrs = " style=\"" + styleToAppend + "\"";
                    } else {
                        newAttrs = newAttrs + " style=\"" + styleToAppend + "\"";
                    }
                }
            } catch (Exception ignore) {
                // Best-effort: if anything fails, fall back to adding a style attribute
                if (newAttrs == null || newAttrs.trim().isEmpty()) {
                    newAttrs = " style=\"" + styleToAppend + "\"";
                } else {
                    newAttrs = newAttrs + " style=\"" + styleToAppend + "\"";
                }
            }
            String replacement = "<" + tag + newAttrs + ">";
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    /**
     * Replaces <h1>..</h1> .. <h6>..</h6> with <div style="...">..</div> to avoid HTMLEditorKit's
     * built-in heading size boosts. Keeps headings close to body size but bold.
     */
    private static String replaceHeadingsWithStyledDivs(String html) {
        try {
            String result = html;
            // Use proper markdown heading hierarchy based on IDE's default font size
            int baseFontSize = UIUtil.getLabelFont().getSize();
            result = replaceSingleHeadingLevel(result, 1, "font-size:" + (baseFontSize + 3) + "px; font-weight:bold; margin-top:6px; margin-bottom:4px");
            result = replaceSingleHeadingLevel(result, 2, "font-size:" + (baseFontSize + 2) + "px; font-weight:bold; margin-top:6px; margin-bottom:4px");
            result = replaceSingleHeadingLevel(result, 3, "font-size:" + (baseFontSize + 1) + "px; font-weight:bold; margin-top:5px; margin-bottom:3px");
            result = replaceSingleHeadingLevel(result, 4, "font-size:" + baseFontSize + "px; font-weight:bold; margin-top:5px; margin-bottom:3px");
            result = replaceSingleHeadingLevel(result, 5, "font-size:" + (baseFontSize - 1) + "px; font-weight:bold; margin-top:4px; margin-bottom:2px");
            result = replaceSingleHeadingLevel(result, 6, "font-size:" + (baseFontSize - 2) + "px; font-weight:bold; margin-top:4px; margin-bottom:2px");
            return result;
        } catch (Exception e) {
            return html;
        }
    }

    /**
     * Replaces a single heading level with styled divs.
     *
     * @param html The HTML content to process
     * @param level The heading level to replace
     * @param divStyle The CSS style for the div
     * @return The HTML with replaced headings
     */
    private static String replaceSingleHeadingLevel(String html, int level, String divStyle) {
        String tag = "h" + level;
        Pattern pattern = Pattern.compile("(?is)<" + tag + "\\b[^>]*>(.*?)</" + tag + ">" );
        Matcher matcher = pattern.matcher(html);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String inner = matcher.group(1);
            String replacement = "<div style=\"" + divStyle + "\">" + inner + "</div>";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Inserts zero-width break opportunities inside <pre> blocks to avoid extremely long tokens
     * expanding the layout width.
     */
    private static String insertSoftBreaksInPreBlocks(String html) {
        try {
            Pattern prePattern = Pattern.compile("(?is)<pre>(.*?)</pre>");
            Matcher matcher = prePattern.matcher(html);
            StringBuffer result = new StringBuffer();
            while (matcher.find()) {
                String originalContent = matcher.group(1);
                String safeContent = breakLongTokens(originalContent, 80, 40);
                // Re-escape backreferences safely
                matcher.appendReplacement(result, Matcher.quoteReplacement("<pre>" + safeContent + "</pre>"));
            }
            matcher.appendTail(result);
            return result.toString();
        } catch (Exception e) {
            LOG.warn("Soft-break insertion failed: " + e.getMessage());
            return html;
        }
    }

    /**
     * Transforms <pre> blocks to a wrapped div structure so lines can wrap in HTMLEditorKit.
     * Also inserts soft breaks inside long tokens to prevent width overflow.
     */
    private static String transformPreBlocksToWrappedDivs(String html) {
        try {
            Pattern prePattern = Pattern.compile("(?is)<pre>(.*?)</pre>");
            Matcher matcher = prePattern.matcher(html);
            StringBuffer result = new StringBuffer();
            while (matcher.find()) {
                String originalContent = matcher.group(1);
                String safeContent = breakLongTokens(originalContent, 80, 40);
                safeContent = escapeHtmlCommentOpeners(safeContent);
                String replacement = "<div class=\"code-block\"><div class=\"code-text\">" + safeContent + "</div></div>";
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(result);
            return result.toString();
        } catch (Exception e) {
            LOG.warn("Pre-to-div transform failed: " + e.getMessage());
            // Fallback to soft-break inside pre
            return insertSoftBreaksInPreBlocks(html);
        }
    }

    /**
     * Escapes HTML comment openers to avoid Swing parser errors.
     *
     * @param html The HTML content to process
     * @return The HTML with escaped comment openers
     */
    private static String escapeHtmlCommentOpeners(String html) {
        // Replace <!-- with &lt;!-- to avoid Swing parser "Unclosed comment" errors
        try {
            return html.replace("<!--", "&lt;!--");
        } catch (Exception e) {
            return html;
        }
    }

    /**
     * Converts modern HTML tags to legacy tags for Swing compatibility.
     *
     * @param html The HTML content to convert
     * @return The HTML with legacy tags
     */
    private static String convertToLegacyHtmlTags(String html) {
        try {
            String result = html;
            // Strong/em to b/i to ensure Swing applies weight/style consistently
            result = result.replace("<strong>", "<b>")
                           .replace("</strong>", "</b>")
                           .replace("<em>", "<i>")
                           .replace("</em>", "</i>");
            return result;
        } catch (Exception e) {
            return html;
        }
    }

    /**
     * Breaks long non-whitespace tokens by inserting zero-width spaces (&#8203;) at safe intervals.
     *
     * @param text The text to process
     * @param longTokenThreshold The threshold for considering a token long
     * @param insertEvery The interval for inserting breaks
     * @return The text with inserted breaks
     */
    private static String breakLongTokens(String text, int longTokenThreshold, int insertEvery) {
        StringBuilder output = new StringBuilder(text.length() + 32);
        int currentTokenLength = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            output.append(c);
            if (Character.isWhitespace(c)) {
                currentTokenLength = 0;
                continue;
            }
            currentTokenLength++;
            boolean isSoftBoundary = (c == '_' || c == '-' || c == '/' || c == '.' || c == ',' || c == ';' || c == ':');
            if (isSoftBoundary) {
                output.append("&#8203;");
                currentTokenLength = 0;
                continue;
            }
            if (currentTokenLength >= longTokenThreshold) {
                output.append("&#8203;");
                currentTokenLength = 0;
                continue;
            }
            if (currentTokenLength % insertEvery == 0) {
                output.append("&#8203;");
            }
        }
        return output.toString();
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
                    String newHtml = wrapHtmlWithStyling(bodyContent);
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

    /**
     * Checks if the markdown contains Java code blocks that should use scrollable components.
     * 
     * @param markdown The markdown text to check
     * @return true if Java code blocks are found
     */
    private static boolean containsJavaCodeBlocks(String markdown) {
        if (markdown == null || markdown.trim().isEmpty()) {
            return false;
        }

        // Look for Java code blocks with ```java
        Pattern javaCodePattern = Pattern.compile("```java\\s*\\n(.*?)```", Pattern.DOTALL);
        Matcher matcher = javaCodePattern.matcher(markdown);
        
        boolean found = matcher.find();
        if (found) {
            LOG.info("Found Java code blocks - will use scrollable components");
        }
        
        return found;
    }

    /**
     * Creates a mixed component layout with scrollable code blocks for wide content.
     * 
     * @param markdown The markdown text containing wide code blocks
     * @return A JPanel with mixed content layout
     */
    private static JComponent createMixedMarkdownComponent(String markdown) {
        LOG.info("Creating mixed markdown component with scrollable code blocks");
        
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setOpaque(false);
        container.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Split the markdown by Java code blocks and process each part
        Pattern javaCodePattern = Pattern.compile("(```java\\s*\\n.*?```)", Pattern.DOTALL);
        String[] parts = javaCodePattern.split(markdown);
        
        // Find all Java code blocks
        Matcher matcher = javaCodePattern.matcher(markdown);
        java.util.List<String> codeBlocks = new java.util.ArrayList<>();
        while (matcher.find()) {
            codeBlocks.add(matcher.group(1));
        }

        // Process each part alternating between text and code
        int codeBlockIndex = 0;
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            
            // Add text part if not empty
            if (!part.trim().isEmpty()) {
                JEditorPane textPane = createMarkdownPane(part);
                textPane.setAlignmentX(Component.LEFT_ALIGNMENT);
                container.add(textPane);
                
                // Add some spacing
                container.add(Box.createVerticalStrut(5));
            }
            
            // Add code block if available
            if (codeBlockIndex < codeBlocks.size()) {
                String codeBlock = codeBlocks.get(codeBlockIndex);
                
                // Extract just the code content (remove ```java and ```)
                String codeContent = codeBlock.replaceFirst("```java\\s*\\n", "").replaceFirst("```$", "");
                
                // Create scrollable component for ALL Java code blocks
                JScrollPane scrollableCode = createScrollableCodeComponent(codeContent);
                scrollableCode.setAlignmentX(Component.LEFT_ALIGNMENT);
                container.add(scrollableCode);
                
                codeBlockIndex++;
                
                // Add some spacing after code block
                container.add(Box.createVerticalStrut(5));
            }
        }

        LOG.info("Created mixed component with " + parts.length + " text parts and " + codeBlocks.size() + " code blocks");
        return container;
    }

    /**
     * Detects if a code block contains content that would be too wide for normal display.
     * Checks for long lines that would benefit from horizontal scrolling.
     * 
     * @param codeContent The content of the code block
     * @return true if the code block contains wide content
     */
    private static boolean isCodeBlockWide(String codeContent) {
        if (codeContent == null || codeContent.trim().isEmpty()) {
            return false;
        }
        
        // Split into lines and check each line length
        String[] lines = codeContent.split("\n");
        for (String line : lines) {
            // Consider a line "wide" if it's longer than 80 characters
            // This is a reasonable threshold for code readability
            if (line.length() > 80) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Detected wide code line (" + line.length() + " chars): " + 
                            (line.length() > 50 ? line.substring(0, 50) + "..." : line));
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Creates a horizontally scrollable component for wide code blocks.
     * Uses JScrollPane with JTextArea to provide native horizontal scrolling.
     * 
     * @param codeContent The code content to display
     * @return A JScrollPane containing the code with horizontal scrollbar
     */
    private static JScrollPane createScrollableCodeComponent(String codeContent) {
        LOG.info("Creating scrollable code component for content length: " + codeContent.length());
        
        // Create JTextArea for code content
        JTextArea codeArea = new JTextArea(codeContent);
        codeArea.setEditable(false);
        codeArea.setLineWrap(false); // Critical: no line wrapping for code
        codeArea.setWrapStyleWord(false);
        
        // Use monospace font for code
        Font codeFont = new Font(Font.MONOSPACED, Font.PLAIN, UIUtil.getLabelFont().getSize());
        codeArea.setFont(codeFont);
        
        // Apply theme-aware colors
        codeArea.setForeground(ThemeUtils.codeForeground()); // Theme-aware text color
        codeArea.setBackground(ThemeUtils.codeBackground());
        codeArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        
        // Calculate preferred size based on content
        FontMetrics fm = codeArea.getFontMetrics(codeFont);
        String[] lines = codeContent.split("\n");
        int maxLineWidth = 0;
        
        for (String line : lines) {
            int lineWidth = fm.stringWidth(line);
            maxLineWidth = Math.max(maxLineWidth, lineWidth);
        }
        
        // Add padding to width and height
        int contentWidth = maxLineWidth + 40; // Extra padding for scrollbar
        int contentHeight = lines.length * fm.getHeight() + 20;
        
        codeArea.setPreferredSize(new Dimension(contentWidth, contentHeight));
        
        // Create scroll pane with ONLY horizontal scrolling
        JScrollPane scrollPane = new JScrollPane(codeArea);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER); // NO vertical scrollbar
        
        // Dynamic width sizing - resize with container, height matches content
        int actualContentHeight = contentHeight + 10; // Small buffer for border
        
        scrollPane.setPreferredSize(new Dimension(0, actualContentHeight)); // Width 0 = use available space
        scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, actualContentHeight)); // Dynamic width, fixed height
        scrollPane.setMinimumSize(new Dimension(200, actualContentHeight)); // Reasonable minimum width
        
        // Enable proper alignment for container layout
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Forward mouse wheel events to parent when no vertical scrolling is needed
        // This allows parent chat container to handle scrolling when hovering over code blocks
        codeArea.addMouseWheelListener(e -> {
            // Check if we have vertical scrollbar disabled (which we do)
            if (scrollPane.getVerticalScrollBarPolicy() == JScrollPane.VERTICAL_SCROLLBAR_NEVER) {
                // Forward the event to parent container for chat scrolling
                Container parent = scrollPane.getParent();
                while (parent != null) {
                    if (parent instanceof JScrollPane) {
                        // Found a parent scroll pane - forward the event
                        parent.dispatchEvent(new MouseWheelEvent(
                            (Component) parent,
                            e.getID(),
                            e.getWhen(),
                            e.getModifiersEx(), // Updated from deprecated getModifiers() to getModifiersEx()
                            e.getX(),
                            e.getY(),
                            e.getXOnScreen(),
                            e.getYOnScreen(),
                            e.getClickCount(),
                            e.isPopupTrigger(),
                            e.getScrollType(),
                            e.getScrollAmount(),
                            e.getWheelRotation()
                        ));
                        break;
                    }
                    parent = parent.getParent();
                }
            }
            // If we have vertical scrolling enabled, let the default behavior handle it
        });
        
        // Style the scroll pane border
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeUtils.borderColor(), 1),
            BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));
        
        LOG.info("Created scrollable code component with dynamic width x " + actualContentHeight + 
                ", content: " + contentWidth + "x" + contentHeight + ", lines: " + lines.length);
        
        return scrollPane;
    }
} 