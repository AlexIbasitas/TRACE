package com.trace.chat.components;

import javax.swing.*;
import java.awt.*;

/**
 * A panel that tracks the width of its enclosing viewport, ensuring children
 * wrap to the visible width rather than expanding horizontally.
 */
public class ViewportWidthTrackingPanel extends JPanel implements Scrollable {

    public ViewportWidthTrackingPanel() {
        super();
        setOpaque(false);
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
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
}

