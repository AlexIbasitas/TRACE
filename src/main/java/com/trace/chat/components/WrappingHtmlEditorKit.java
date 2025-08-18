package com.trace.chat.components;

import javax.swing.text.*;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.HTML;
import javax.swing.text.html.InlineView;
import javax.swing.text.LabelView;

/**
 * HTMLEditorKit that enables soft-wrapping for all inline text, including content inside
 * <pre> and <code> blocks. This prevents horizontal overflow in Swing's HTML renderer.
 *
 * Scope: ONLY wrapping behavior. No styling, no business logic changes.
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
                int p = getGlyphPainter().getBoundedPosition(this, p0, pos, len);
                if (p == getStartOffset()) {
                    // Ensure forward progress
                    p = Math.min(getEndOffset(), p0 + 1);
                }
                return createFragment(p0, p);
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
                int p = getGlyphPainter().getBoundedPosition(this, p0, pos, len);
                if (p == getStartOffset()) {
                    p = Math.min(getEndOffset(), p0 + 1);
                }
                return createFragment(p0, p);
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

    // Removed ParagraphView override for PRE/CODE to preserve true preformatted behavior
}

