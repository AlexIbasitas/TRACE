package com.trace.chat.components;

import javax.swing.text.*;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.HTML;
import javax.swing.text.html.InlineView;
import javax.swing.text.html.ParagraphView;
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
                // Only apply aggressive wrapping to PRE/CODE blocks and their inline/text children
                if (t == HTML.Tag.PRE || t == HTML.Tag.CODE) {
                    return new WrappingParagraphView(elem);
                }
                if (t == HTML.Tag.CONTENT) {
                    if (isInsidePreOrCode(elem)) {
                        return new WrappingLabelView(elem);
                    }
                    return super.create(elem);
                }
                if (t == HTML.Tag.SPAN) {
                    if (isInsidePreOrCode(elem)) {
                        return new WrappingInlineView(elem);
                    }
                    return super.create(elem);
                }
            }
            return super.create(elem);
        }

        private boolean isInsidePreOrCode(Element elem) {
            Element e = elem;
            while (e != null) {
                Object name = e.getAttributes().getAttribute(StyleConstants.NameAttribute);
                if (name instanceof HTML.Tag) {
                    HTML.Tag tag = (HTML.Tag) name;
                    if (tag == HTML.Tag.PRE || tag == HTML.Tag.CODE) {
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

    /**
     * Paragraph view that allows wrapping even for pre/code containers.
     */
    private static class WrappingParagraphView extends ParagraphView {
        WrappingParagraphView(Element elem) { super(elem); }

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
                return super.breakView(axis, p0, pos, len);
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
}

