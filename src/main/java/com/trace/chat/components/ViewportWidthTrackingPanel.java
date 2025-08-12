package com.trace.chat.components;

import javax.swing.*;
import java.awt.*;
import com.trace.common.constants.TriagePanelConstants;

/**
 * A panel that tracks the width of its enclosing viewport, ensuring children
 * wrap to the visible width rather than expanding horizontally.
 */
public class ViewportWidthTrackingPanel extends JPanel implements Scrollable {

    private int minWidthBeforeHorizontalScroll = TriagePanelConstants.MIN_CHAT_WIDTH_BEFORE_SCROLL;

    public ViewportWidthTrackingPanel() {
        super();
        setOpaque(false);
    }

    /**
     * Allows callers to override the cutoff where the panel stops tracking the viewport width
     * and allows horizontal scrolling.
     */
    public void setMinWidthBeforeHorizontalScroll(int minWidth) {
        this.minWidthBeforeHorizontalScroll = Math.max(1, minWidth);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 16;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return visibleRect.height;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        Container parent = getParent();
        if (parent instanceof JViewport) {
            int viewportWidth = ((JViewport) parent).getExtentSize().width;
            // Track (wrap) only when the viewport is at or above the cutoff.
            return viewportWidth >= minWidthBeforeHorizontalScroll;
        }
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension pref = super.getPreferredSize();
        // Ensure the content does not report a preferred width smaller than the cutoff.
        return new Dimension(Math.max(pref.width, minWidthBeforeHorizontalScroll), pref.height);
    }
}

