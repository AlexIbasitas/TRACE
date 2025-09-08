package com.trace.chat.components;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.ui.UIUtil;
import com.trace.common.utils.ThemeUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class for HTML processing operations in markdown rendering.
 * 
 * <p>This class provides methods for processing HTML content generated from markdown,
 * including styling, tag conversion, and text manipulation. It handles HTML processing
 * operations to ensure compatibility with Swing's HTML renderer while maintaining
 * professional styling and typography.</p>
 * 
 * <p>The helper encapsulates all HTML processing logic to keep the main markdown
 * renderer focused on core rendering while delegating HTML processing to this
 * specialized helper.</p>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
public class HTMLProcessingHelper {
    
    private static final Logger LOG = Logger.getInstance(HTMLProcessingHelper.class);
    
    private HTMLProcessingHelper() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
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
    public static String wrapHtmlWithStyling(String html) {
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
    public static String dimCodeAndPreColorsInline(String html) {
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
    public static String applyHeadingStylesInline(String html) {
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
    public static String applyHeadingStyleForLevel(String html, int level, String styleToAppend) {
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
    public static String replaceHeadingsWithStyledDivs(String html) {
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
    public static String replaceSingleHeadingLevel(String html, int level, String divStyle) {
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
    public static String insertSoftBreaksInPreBlocks(String html) {
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
    public static String transformPreBlocksToWrappedDivs(String html) {
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
    public static String escapeHtmlCommentOpeners(String html) {
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
    public static String convertToLegacyHtmlTags(String html) {
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
    public static String breakLongTokens(String text, int longTokenThreshold, int insertEvery) {
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
}
