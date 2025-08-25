package com.trace.chat.components;

import javax.swing.*;
import java.awt.*;
import com.trace.common.constants.TriagePanelConstants;

/**
 * A panel that tracks the width of its enclosing viewport, ensuring children
 * wrap to the visible width rather than expanding horizontally.
 * 
 * <p>This component implements the Scrollable interface to provide intelligent
 * width tracking behavior. It monitors the viewport width and adjusts its
 * preferred size accordingly to prevent horizontal scrolling when the viewport
 * is wide enough, while allowing horizontal scrolling when the viewport is
 * too narrow.</p>
 * 
 * <p>The panel is designed to work with JScrollPane components and provides
 * smooth transitions between wrapping and scrolling behavior based on the
 * available viewport width.</p>
 * 
 * @author Alex Ibasitas
 * @version 1.0
 * @since 1.0
 */
public class ViewportWidthTrackingPanel extends JPanel implements Scrollable {

    private int minWidthBeforeHorizontalScroll = TriagePanelConstants.MIN_CHAT_WIDTH_BEFORE_SCROLL;

    /**
     * Creates a new viewport width tracking panel.
     * 
     * <p>The panel is initialized with a transparent background and uses the
     * default minimum width before horizontal scrolling from the constants.</p>
     */
    public ViewportWidthTrackingPanel() {
        super();
        setOpaque(false);
    }

    /**
     * Sets the minimum width threshold before horizontal scrolling is enabled.
     * 
     * <p>When the viewport width is at or above this threshold, the panel will
     * track the viewport width and wrap content. When below this threshold,
     * horizontal scrolling will be allowed.</p>
     * 
     * @param minWidth The minimum width in pixels (must be at least 1)
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

