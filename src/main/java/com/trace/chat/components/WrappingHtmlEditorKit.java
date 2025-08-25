package com.trace.chat.components;

import javax.swing.text.*;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.HTML;
import javax.swing.text.html.InlineView;
import javax.swing.text.LabelView;
import javax.swing.text.Segment;

/**
 * HTMLEditorKit that enables soft-wrapping for all inline text, including content inside
 * <pre> and <code> blocks. This prevents horizontal overflow in Swing's HTML renderer.
 *
 * <p>This class extends HTMLEditorKit to provide intelligent text wrapping behavior
 * while preserving the formatting of preformatted content. It implements a custom
 * ViewFactory that creates wrappable views for inline content while maintaining
 * the original behavior for PRE and CODE blocks.</p>
 *
 * <p>The wrapping behavior is designed to prevent horizontal scrolling in chat
 * interfaces while maintaining readability and proper formatting.</p>
 *
 * <p>Scope: ONLY wrapping behavior. No styling, no business logic changes.</p>
 *
 * @author Alex Ibasitas
 * @version 1.0
 * @since 1.0
 */
public class WrappingHtmlEditorKit extends HTMLEditorKit {

    private final ViewFactory wrappingFactory = new WrappingHTMLFactory();

    @Override
    public ViewFactory getViewFactory() {
        return wrappingFactory;
    }

    /**
     * Factory that returns wrappable views for inline/label content, and ensures
     * content under PRE/CODE can break lines as needed.
     * 
     * <p>This factory creates appropriate view implementations based on the HTML
     * element type, ensuring that text content can wrap while preserving
     * preformatted behavior for PRE and CODE blocks.</p>
     */
    private static class WrappingHTMLFactory extends HTMLEditorKit.HTMLFactory {
        @Override
        public View create(Element elem) {
            Object o = elem.getAttributes().getAttribute(StyleConstants.NameAttribute);
            if (o instanceof HTML.Tag) {
                HTML.Tag t = (HTML.Tag) o;
                // 1) Preserve true preformatted behavior for PRE containers
                if (t == HTML.Tag.PRE) {
                    return super.create(elem);
                }
                // 2) If CODE is inside PRE, treat it as part of the block (no overrides)
                if (t == HTML.Tag.CODE && isInside(elem, HTML.Tag.PRE)) {
                    return super.create(elem);
                }
                // 3) For plain text content, only wrap outside of PRE
                if (t == HTML.Tag.CONTENT) {
                    return isInside(elem, HTML.Tag.PRE) ? super.create(elem) : new WrappingLabelView(elem);
                }
                // 4) For inline spans, only wrap outside of PRE
                if (t == HTML.Tag.SPAN) {
                    return isInside(elem, HTML.Tag.PRE) ? super.create(elem) : new WrappingInlineView(elem);
                }
            }
            return super.create(elem);
        }

        /**
         * Checks if an element is inside a specific ancestor tag.
         * 
         * <p>This method traverses up the element hierarchy to determine if
         * the given element is contained within an element of the specified tag type.</p>
         * 
         * @param elem The element to check
         * @param ancestor The ancestor tag to look for
         * @return true if the element is inside the specified ancestor tag
         */
        private static boolean isInside(Element elem, HTML.Tag ancestor) {
            Element e = elem;
            while (e != null) {
                Object name = e.getAttributes().getAttribute(StyleConstants.NameAttribute);
                if (name instanceof HTML.Tag) {
                    HTML.Tag tag = (HTML.Tag) name;
                    if (tag == ancestor) {
                        return true;
                    }
                }
                e = e.getParentElement();
            }
            return false;
        }
    }

    /**
     * Wrappable label view for plain text content nodes.
     * 
     * <p>This view implementation provides word-aware text wrapping for plain
     * text content while maintaining proper text rendering and selection behavior.</p>
     */
    private static class WrappingLabelView extends LabelView {
        WrappingLabelView(Element elem) { super(elem); }

        @Override
        public int getBreakWeight(int axis, float pos, float len) {
            if (axis == View.X_AXIS) {
                return GoodBreakWeight;
            }
            return super.getBreakWeight(axis, pos, len);
        }

        @Override
        public View breakView(int axis, int p0, float pos, float len) {
            if (axis == View.X_AXIS) {
                checkPainter();
                // Initial char-level break suggestion
                int bounded = getGlyphPainter().getBoundedPosition(this, p0, pos, len);
                // Try to move the break back to the last word boundary before 'bounded'
                int breakAt = findWordBreak(getDocument(), p0, bounded);
                if (breakAt <= p0) {
                    // Ensure forward progress if we couldn't find a boundary
                    breakAt = Math.min(getEndOffset(), Math.max(bounded, p0 + 1));
                }
                return createFragment(p0, breakAt);
            }
            return super.breakView(axis, p0, pos, len);
        }

        @Override
        public float getMinimumSpan(int axis) {
            if (axis == View.X_AXIS) {
                return 0f;
            }
            return super.getMinimumSpan(axis);
        }
    }

    /**
     * Wrappable inline view for inline elements like SPAN.
     * 
     * <p>This view implementation provides word-aware text wrapping for inline
     * HTML elements while maintaining proper styling and layout behavior.</p>
     */
    private static class WrappingInlineView extends InlineView {
        WrappingInlineView(Element elem) { super(elem); }

        @Override
        public int getBreakWeight(int axis, float pos, float len) {
            if (axis == View.X_AXIS) {
                return GoodBreakWeight;
            }
            return super.getBreakWeight(axis, pos, len);
        }

        @Override
        public View breakView(int axis, int p0, float pos, float len) {
            if (axis == View.X_AXIS) {
                checkPainter();
                int bounded = getGlyphPainter().getBoundedPosition(this, p0, pos, len);
                int breakAt = findWordBreak(getDocument(), p0, bounded);
                if (breakAt <= p0) {
                    breakAt = Math.min(getEndOffset(), Math.max(bounded, p0 + 1));
                }
                return createFragment(p0, breakAt);
            }
            return super.breakView(axis, p0, pos, len);
        }

        @Override
        public float getMinimumSpan(int axis) {
            if (axis == View.X_AXIS) {
                return 0f;
            }
            return super.getMinimumSpan(axis);
        }
    }

    /**
     * Returns a position at or before 'end' to break the line, preferring whitespace
     * and common delimiters. Falls back to the provided end position if no boundary exists.
     * 
     * <p>This method implements intelligent word breaking by looking for natural
     * break points such as whitespace, hyphens, slashes, underscores, and periods.
     * It scans backwards from the end position to find the best break point.</p>
     * 
     * @param doc The document containing the text
     * @param start The starting position in the document
     * @param end The ending position in the document
     * @return The best position to break the line at
     */
    private static int findWordBreak(Document doc, int start, int end) {
        if (end <= start) {
            return start;
        }
        try {
            Segment seg = new Segment();
            doc.getText(start, end - start, seg);
            // Scan backwards for whitespace or natural delimiters
            for (int i = seg.count - 1; i >= 0; i--) {
                char ch = seg.array[seg.offset + i];
                if (Character.isWhitespace(ch) || ch == '-' || ch == '/' || ch == '_' || ch == '.') {
                    return start + i + 1; // include the delimiter/space in the first fragment
                }
            }
        } catch (BadLocationException ignored) {
            return end;
        }
        return end; // no better break point found
    }

    // Removed ParagraphView override for PRE/CODE to preserve true preformatted behavior
}

