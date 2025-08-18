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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
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
            editorPane.setOpaque(true);
            editorPane.setBackground(ThemeUtils.panelBackground());
            // Increase bottom inset to prevent last line clipping in dynamic wrap scenarios
            editorPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));
            editorPane.setAlignmentX(Component.LEFT_ALIGNMENT);
            editorPane.setAlignmentY(Component.TOP_ALIGNMENT);

            // Apply professional styling
            configureEditorPane(editorPane);

            // Force wrapping behavior and a theme-aware stylesheet
            try {
                HTMLEditorKit kit = new WrappingHtmlEditorKit();
                editorPane.setEditorKit(kit);
                // Create a fresh HTMLDocument and apply rules at the document level for max precedence
                HTMLDocument doc = (HTMLDocument) kit.createDefaultDocument();
                StyleSheet docSheet = doc.getStyleSheet();
                String textFg = ThemeUtils.toHex(ThemeUtils.textForeground());
                String panelBg = ThemeUtils.toHex(ThemeUtils.panelBackground());
                String codeBg = ThemeUtils.toHex(ThemeUtils.codeBackground());
                String codeFg = ThemeUtils.toHex(ThemeUtils.codeForeground());
                String inlineBg = ThemeUtils.toHex(ThemeUtils.inlineCodeBackground());

                docSheet.addRule("body, p, li, ul, ol, h1, h2, h3, h4, h5, h6, span, div, td, th, a, b, i { color:" + textFg + "; }");
                docSheet.addRule("body { background-color:" + panelBg + "; padding-bottom:12px; }");
                // Monospace font and themed background for code
                docSheet.addRule("pre { font-family:'Monospaced'; background:" + codeBg + "; color:" + codeFg + "; padding:6px; margin-top:4px; margin-bottom:4px; }");
                docSheet.addRule("code { font-family:'Monospaced'; background:" + inlineBg + "; color:" + codeFg + "; padding:0 3px; }");
                // Set base body text size (reduce by 1px from previous 12px → 11px)
                docSheet.addRule("body, p, li { font-size:11px; }");
                // Tighten vertical spacing globally (supported subset of CSS in Swing)
                docSheet.addRule("p { margin-top:2px; margin-bottom:2px; }");
                docSheet.addRule("ul, ol { margin-top:2px; margin-bottom:2px; }");
                docSheet.addRule("li { margin-top:0px; margin-bottom:2px; }");
                docSheet.addRule("pre { margin-top:3px; margin-bottom:3px; }");
                editorPane.setDocument(doc);
                logHtmlKitAndStyles(editorPane, kit, docSheet);
            } catch (Exception ex) {
                LOG.warn("Failed to set custom HTMLEditorKit stylesheet: " + ex.getMessage());
            }

            // Set the styled HTML content (with safe post-processing)
            String styledHtml = wrapHtmlWithStyling(html);
            LOG.info("Setting HTML content with font-size: 14px in CSS");
            LOG.info("HTML content length: " + styledHtml.length());
            LOG.info("HTML contains font-size: " + styledHtml.contains("font-size: 14px"));
            
            // Log a sample of the HTML to see what's being set
            if (styledHtml.length() > 200) {
                LOG.info("HTML sample (first 200 chars): " + styledHtml.substring(0, 200));
            }
            
            editorPane.setText(styledHtml);
            // Trigger reflow after text is set
            SwingUtilities.invokeLater(() -> {
                if (editorPane instanceof ResponsiveHtmlPane) {
                    ((ResponsiveHtmlPane) editorPane).applyWidthFromParent();
                }
                try {
                    logDocumentStyles(editorPane);
                } catch (Exception e) {
                    LOG.warn("Post-setText style logging failed: " + e.getMessage());
                }
            });
            
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
        
        editorPane.setForeground(ThemeUtils.textForeground());
        editorPane.setBackground(ThemeUtils.panelBackground());

        // Enable proper HTML rendering
        editorPane.putClientProperty("JEditorPane.honorDisplayProperties", Boolean.TRUE);

        // Log the final font after configuration
        LOG.info("JEditorPane font after configuration: " + editorPane.getFont());
        LOG.info("JEditorPane font size: " + editorPane.getFont().getSize());

        // JEditorPane doesn't have setLineWrap/setWrapStyleWord - these are JTextArea methods
        // JEditorPane handles wrapping automatically with its layout
    }

    private static void logHtmlKitAndStyles(JEditorPane pane, HTMLEditorKit kit, StyleSheet ss) {
        try {
            LOG.info("HTML Kit class: " + kit.getClass().getName());
            LOG.info("Pane EditorKit class: " + (pane.getEditorKit() != null ? pane.getEditorKit().getClass().getName() : "null"));
            LOG.info("Custom StyleSheet rules applied: theme-aware body/text colors and code styles");
        } catch (Exception e) {
            LOG.warn("Failed logging kit/styles: " + e.getMessage());
        }
    }

    private static void logDocumentStyles(JEditorPane pane) {
        try {
            javax.swing.text.Document doc = pane.getDocument();
            LOG.info("Document class: " + (doc != null ? doc.getClass().getName() : "null"));
            LOG.info("Foreground color of pane: " + pane.getForeground());
            // Dump a small sample of HTML actually stored to verify tags
            String text = pane.getText();
            LOG.info("Pane HTML length: " + (text != null ? text.length() : -1));
            if (text != null) {
                LOG.info("Pane HTML sample: " + text.substring(0, Math.min(200, text.length())));
            }
        } catch (Exception e) {
            LOG.warn("Failed logging document styles: " + e.getMessage());
        }
    }

    /**
     * Re-applies the current theme stylesheet to an existing HTML JEditorPane.
     * Keeps the pane's current HTML content while rebuilding the document/styles.
     */
    public static void reapplyThemeStyles(JEditorPane pane) {
        if (pane == null) return;
        try {
            String currentHtml = pane.getText();
            HTMLEditorKit kit = new WrappingHtmlEditorKit();
            pane.setEditorKit(kit);
            HTMLDocument doc = (HTMLDocument) kit.createDefaultDocument();
            StyleSheet ss = doc.getStyleSheet();
            String textFg = ThemeUtils.toHex(ThemeUtils.textForeground());
            String panelBg = ThemeUtils.toHex(ThemeUtils.panelBackground());
            String codeBg = ThemeUtils.toHex(ThemeUtils.codeBackground());
            String codeFg = ThemeUtils.toHex(ThemeUtils.codeForeground());
            String inlineBg = ThemeUtils.toHex(ThemeUtils.inlineCodeBackground());
            ss.addRule("body, p, li, ul, ol, h1, h2, h3, h4, h5, h6, span, div, td, th, a, b, i { color:" + textFg + "; }");
            ss.addRule("body { background-color:" + panelBg + "; padding-bottom:12px; }");
            ss.addRule("pre { font-family:'Monospaced'; background:" + codeBg + "; color:" + codeFg + "; padding:6px; margin-top:4px; margin-bottom:4px; }");
            ss.addRule("code { font-family:'Monospaced'; background:" + inlineBg + "; color:" + codeFg + "; padding:0 3px; }");
            ss.addRule("body, p, li { font-size:11px; }");
            ss.addRule("p { margin-top:2px; margin-bottom:2px; }");
            ss.addRule("ul, ol { margin-top:2px; margin-bottom:2px; }");
            ss.addRule("li { margin-top:0px; margin-bottom:2px; }");
            ss.addRule("pre { margin-top:3px; margin-bottom:3px; }");
            pane.setDocument(doc);
            pane.setBackground(ThemeUtils.panelBackground());
            pane.setForeground(ThemeUtils.textForeground());
            pane.setText(currentHtml);
        } catch (Exception ignore) {
        }
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
        // Keep HTML minimal and safe for Swing; prefer global stylesheet for colors
        String safeHtml = convertToLegacyHtmlTags(html);
        safeHtml = escapeHtmlCommentOpeners(safeHtml);
        // Transform PRE/CODE blocks into div-per-line markup (no syntax highlighting)
        safeHtml = transformPreBlocksToDivLines(safeHtml);
        // Ensure inline styles on PRE and CODE with theme-aware values for maximum precedence
        String codeBg = ThemeUtils.toHex(ThemeUtils.codeBackground());
        String codeFg = ThemeUtils.toHex(ThemeUtils.codeForeground());
        String inlineBg = ThemeUtils.toHex(ThemeUtils.inlineCodeBackground());
        safeHtml = ensureInlineStyleOnTag(safeHtml, "pre", "color:" + codeFg + "; font-family:Monospaced; background:" + codeBg + "; padding:6px; margin-top:4px; margin-bottom:4px");
        safeHtml = ensureInlineStyleOnTag(safeHtml, "code", "color:" + codeFg + "; font-family:Monospaced; background:" + inlineBg + "; padding:0 3px");
        // Apply inline heading styles (h1–h6) so headings render visually as headings in Swing
        safeHtml = applyHeadingStylesInline(safeHtml);
        // Replace heading tags with styled divs to bypass Swing's default heading sizing
        safeHtml = replaceHeadingsWithStyledDivs(safeHtml);

        String textFg = ThemeUtils.toHex(ThemeUtils.textForeground());
        String panelBg = ThemeUtils.toHex(ThemeUtils.panelBackground());
        String styledHtml = """
            <html>
            <head></head>
            <body style="color:%s; background-color:%s;">
            %s
            </body>
            </html>
            """.formatted(textFg, panelBg, safeHtml);
        
        LOG.info("Final styled HTML length: " + styledHtml.length());
        LOG.info("Final HTML contains font-size: " + styledHtml.contains("font-size: 14px"));
        return styledHtml;
    }

    private static String applyBasicSyntaxHighlighting(String html) {
        try {
            String updated = html;
            // Only attempt if language class is present to avoid false positives
            updated = highlightLanguageBlock(updated, "java",
                new String[]{
                    "public","private","protected","class","interface","enum","void","int","long","double","float","boolean","char","new","return","if","else","switch","case","break","continue","try","catch","finally","throw","throws","static","final","abstract","synchronized","volatile","transient","this","super","extends","implements","package","import"
                }
            );
            updated = highlightLanguageBlock(updated, "json",
                new String[]{
                    "true","false","null"
                }
            );
            updated = highlightLanguageBlock(updated, "sql",
                new String[]{
                    "select","from","where","and","or","join","left","right","inner","outer","on","group","by","order","limit","insert","into","values","update","set","delete"
                }
            );
            return updated;
        } catch (Exception e) {
            return html;
        }
    }

    /**
     * Swing-friendly replacement for PRE/CODE blocks without syntax highlighting.
     */
    private static String transformPreBlocksToDivLines(String html) {
        try {
            Pattern p = Pattern.compile("(?is)<pre>\\s*(?:<code([^>]*)>)?(.*?)(?:</code>)?\\s*</pre>");
            Matcher m = p.matcher(html);
            StringBuffer out = new StringBuffer();
            while (m.find()) {
                String codeAttrs = m.group(1);
                String innerHtml = m.group(2) != null ? m.group(2) : "";
                String rawCode = htmlUnescape(innerHtml);
                String[] lines = rawCode.split("\\r?\\n", -1);
                String codeBg = ThemeUtils.toHex(ThemeUtils.codeBackground());
                String codeFg = ThemeUtils.toHex(ThemeUtils.codeForeground());
                StringBuilder block = new StringBuilder();
                block.append("<div style=\"font-family:Monospaced; background:" + codeBg + 
                             "; padding:6px; margin-top:4px; margin-bottom:4px; color:" + codeFg + ";\">");
                for (String line : lines) {
                    String lineHtml = htmlEscape(line.replace("\t", "    "));
                    lineHtml = lineHtml.replace(" ", "&nbsp;");
                    block.append("<div>").append(lineHtml).append("</div>");
                }
                block.append("</div>");
                m.appendReplacement(out, Matcher.quoteReplacement(block.toString()));
            }
            m.appendTail(out);
            return out.toString();
        } catch (Exception e) {
            return html;
        }
    }

    private static String extractLanguageFromClassAttr(String attrs) {
        if (attrs == null) return null;
        try {
            Matcher m = Pattern.compile("(?i)class\\s*=\\s*\"([^\"]*)\"").matcher(attrs);
            if (m.find()) {
                String cls = m.group(1);
                Matcher lm = Pattern.compile("(?i)language-([A-Za-z0-9_+-]+)").matcher(cls);
                if (lm.find()) {
                    return lm.group(1).toLowerCase();
                }
            }
        } catch (Exception ignore) {}
        return null;
    }

    private static String highlightEscapedLine(String escapedLine, String language) {
        try {
            String s = escapedLine;
            if ("java".equals(language)) {
                s = replaceRegex(s, "//.*$", "#6A9955", Pattern.MULTILINE);
                s = replaceRegex(s, "(?<![A-Za-z0-9_])(abstract|assert|boolean|break|byte|case|catch|char|class|const|continue|default|do|double|else|enum|extends|final|finally|float|for|goto|if|implements|import|instanceof|int|interface|long|native|new|package|private|protected|public|return|short|static|strictfp|super|switch|synchronized|this|throw|throws|transient|try|void|volatile|while)(?![A-Za-z0-9_])", "#4FC1FF", 0);
                s = replaceRegex(s, "(?<![A-Za-z0-9_])(true|false|null)(?![A-Za-z0-9_])", "#569CD6", Pattern.CASE_INSENSITIVE);
            } else if ("json".equals(language)) {
                s = replaceRegex(s, "(?<![A-Za-z0-9_])(true|false|null)(?![A-Za-z0-9_])", "#569CD6", Pattern.CASE_INSENSITIVE);
                // JSON keys inside quotes: color strings
            } else if ("sql".equals(language)) {
                s = replaceRegex(s, "(?<![A-Za-z0-9_])(select|from|where|and|or|join|left|right|inner|outer|on|group|by|order|limit|insert|into|values|update|set|delete)(?![A-Za-z0-9_])", "#C586C0", Pattern.CASE_INSENSITIVE);
                s = replaceRegex(s, "--.*$", "#6A9955", Pattern.MULTILINE);
            }
            s = replaceRegex(s, "(?<![A-Za-z0-9_])\\d+(?![A-Za-z0-9_])", "#CE9178", 0);
            // Strings in quotes for JSON/Java/SQL
            s = replaceRegex(s, "\"([^\"]*)\"", "#CE9178", 0);
            return s;
        } catch (Exception e) {
            return escapedLine;
        }
    }

    private static String replaceRegex(String input, String regex, String color, int flags) {
        Pattern pattern = (flags == 0) ? Pattern.compile(regex) : Pattern.compile(regex, flags);
        Matcher m = pattern.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            // Use <font color> which is supported by Swing HTMLEditorKit; style attributes on span are not reliable
            String repl = "<font color=\"" + color + "\">" + m.group(0) + "</font>";
            m.appendReplacement(sb, Matcher.quoteReplacement(repl));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String htmlUnescape(String s) {
        if (s == null) return null;
        String out = s;
        try { out = out.replace("&lt;", "<"); } catch (Exception ignore) {}
        try { out = out.replace("&gt;", ">"); } catch (Exception ignore) {}
        try { out = out.replace("&quot;", "\""); } catch (Exception ignore) {}
        try { out = out.replace("&#39;", "'"); } catch (Exception ignore) {}
        try { out = out.replace("&amp;", "&"); } catch (Exception ignore) {}
        return out;
    }

    private static String htmlEscape(String s) {
        if (s == null) return "";
        StringBuilder out = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&': out.append("&amp;"); break;
                case '<': out.append("&lt;"); break;
                case '>': out.append("&gt;"); break;
                case '"': out.append("&quot;"); break;
                case '\'': out.append("&#39;"); break;
                default: out.append(c);
            }
        }
        return out.toString();
    }

    private static String highlightLanguageBlock(String html, String language, String[] keywords) {
        try {
            // Match <code class="language-xxx">...</code> (optionally wrapped in <pre>)
            Pattern p = Pattern.compile("(?is)(<code[^>]*class=\\\"[^\\\"]*language-" + language + "[^\\\"]*\\\"[^>]*>)(.*?)(</code>)");
            Matcher m = p.matcher(html);
            StringBuffer out = new StringBuffer();
            while (m.find()) {
                String open = m.group(1);
                String body = m.group(2);
                String close = m.group(3);
                String highlighted = highlightKeywordsInCode(body, keywords);
                m.appendReplacement(out, Matcher.quoteReplacement(open + highlighted + close));
            }
            m.appendTail(out);
            return out.toString();
        } catch (Exception e) {
            return html;
        }
    }

    private static String highlightKeywordsInCode(String codeHtml, String[] keywords) {
        try {
            String result = codeHtml;
            for (String kw : keywords) {
                // Highlight whole-word keywords; keep case-insensitive for SQL/JSON
                result = result.replaceAll("(?i)(?<![A-Za-z0-9_])(" + java.util.regex.Pattern.quote(kw) + ")(?![A-Za-z0-9_])",
                        "<span style=\"color:#4FC1FF\">$1</span>");
            }
            // Numbers
            result = result.replaceAll("(?<![A-Za-z0-9_])(\\d+)(?![A-Za-z0-9_])", "<span style=\"color:#CE9178\">$1</span>");
            // Strings in double quotes
            result = result.replaceAll("\"([^\"]*)\"", "<span style=\"color:#CE9178\">\"$1\"</span>");
            return result;
        } catch (Exception e) {
            return codeHtml;
        }
    }

    private static String ensureInlineStyleOnTag(String html, String tag, String styleToMerge) {
        try {
            Pattern openTag = Pattern.compile("(?is)<" + tag + "(\\b[^>]*)>");
            Matcher m = openTag.matcher(html);
            StringBuffer out = new StringBuffer();
            while (m.find()) {
                String attrs = m.group(1) != null ? m.group(1) : "";
                String newAttrs = attrs;
                try {
                    Pattern styleAttr = Pattern.compile("(?i)style\\s*=\\s*\"([^\"]*)\"");
                    Matcher sm = styleAttr.matcher(attrs);
                    if (sm.find()) {
                        String existing = sm.group(1) != null ? sm.group(1) : "";
                        String combined = existing.trim();
                        if (!combined.endsWith(";") && combined.length() > 0) {
                            combined = combined + "; ";
                        }
                        combined = combined + styleToMerge;
                        newAttrs = sm.replaceFirst("style=\"" + Matcher.quoteReplacement(combined) + "\"");
                    } else {
                        newAttrs = (newAttrs == null || newAttrs.trim().isEmpty()) ?
                                " style=\"" + styleToMerge + "\"" : newAttrs + " style=\"" + styleToMerge + "\"";
                    }
                } catch (Exception ignore) {
                    newAttrs = (newAttrs == null || newAttrs.trim().isEmpty()) ?
                            " style=\"" + styleToMerge + "\"" : newAttrs + " style=\"" + styleToMerge + "\"";
                }
                String replacement = "<" + tag + newAttrs + ">";
                m.appendReplacement(out, Matcher.quoteReplacement(replacement));
            }
            m.appendTail(out);
            return out.toString();
        } catch (Exception e) {
            return html;
        }
    }

    /**
     * Applies inline color to content inside <pre> and <code> to keep them slightly dimmer than body text.
     * Uses element-level attributes for maximum precedence in Swing's HTML renderer.
     */
    private static String dimCodeAndPreColorsInline(String html) { return html; }

    /**
     * Adds inline styles to heading tags h1–h6 to ensure they render with larger font and bold
     * using element-level attributes which have the highest precedence per Swing's StyleSheet docs.
     */
    private static String applyHeadingStylesInline(String html) {
        try {
            String result = html;
            // Make headings close to body size (~11px) but still distinct
            result = applyHeadingStyleForLevel(result, 1, "font-size:13px; font-weight:bold; margin-top:6px; margin-bottom:4px");
            result = applyHeadingStyleForLevel(result, 2, "font-size:12px; font-weight:bold; margin-top:6px; margin-bottom:4px");
            result = applyHeadingStyleForLevel(result, 3, "font-size:12px; font-weight:bold; margin-top:5px; margin-bottom:3px");
            result = applyHeadingStyleForLevel(result, 4, "font-size:11px; font-weight:bold; margin-top:5px; margin-bottom:3px");
            result = applyHeadingStyleForLevel(result, 5, "font-size:11px; font-weight:bold; margin-top:4px; margin-bottom:2px");
            result = applyHeadingStyleForLevel(result, 6, "font-size:11px; font-weight:bold; margin-top:4px; margin-bottom:2px");
            return result;
        } catch (Exception e) {
            return html;
        }
    }

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
            // One pass per level for simplicity and clarity
            result = replaceSingleHeadingLevel(result, 1, "font-size:12px; font-weight:bold; margin-top:6px; margin-bottom:4px");
            result = replaceSingleHeadingLevel(result, 2, "font-size:12px; font-weight:bold; margin-top:6px; margin-bottom:4px");
            result = replaceSingleHeadingLevel(result, 3, "font-size:11px; font-weight:bold; margin-top:5px; margin-bottom:3px");
            result = replaceSingleHeadingLevel(result, 4, "font-size:11px; font-weight:bold; margin-top:5px; margin-bottom:3px");
            result = replaceSingleHeadingLevel(result, 5, "font-size:11px; font-weight:bold; margin-top:4px; margin-bottom:2px");
            result = replaceSingleHeadingLevel(result, 6, "font-size:11px; font-weight:bold; margin-top:4px; margin-bottom:2px");
            return result;
        } catch (Exception e) {
            return html;
        }
    }

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

    private static String escapeHtmlCommentOpeners(String html) {
        // Replace <!-- with &lt;!-- to avoid Swing parser "Unclosed comment" errors
        try {
            return html.replace("<!--", "&lt;!--");
        } catch (Exception e) {
            return html;
        }
    }

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
                        SwingUtilities.invokeLater(ResponsiveHtmlPane.this::applyWidthFromParent);
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
                    // Add an extra safety buffer to height to prevent last-line clipping during wraps
                    return new Dimension(targetWidth, pref.height + 16);
                }
            } catch (Exception ignore) {
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
            SwingUtilities.invokeLater(this::applyWidthFromParent);
        }

        void applyWidthFromParent() {
            try {
                int targetWidth = computeTargetWidth();
                if (targetWidth <= 0) {
                    return;
                }
                if (targetWidth != lastAppliedWidth) {
                    // First set an arbitrarily large height to allow proper preferred size computation
                    super.setSize(new Dimension(targetWidth, Integer.MAX_VALUE));
                    Dimension pref = getPreferredSize();
                    // Apply an extra height buffer to account for reflow after wraps
                    super.setSize(new Dimension(targetWidth, pref.height + 16));
                    revalidate();
                    lastAppliedWidth = targetWidth;
                }
            } catch (Exception ex) {
                LOG.warn("applyWidthFromParent failed: " + ex.getMessage());
            }
        }

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
                               text.contains("~~"); // Strikethrough
    }
} 