package com.trace.chat.components;

import com.trace.common.constants.TriagePanelConstants;
import com.trace.common.utils.ThemeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;

/**
 * Lightweight row that displays an animated "assistant is typing" indicator.
 *
 * - Styled like an assistant message row
 * - Uses javax.swing.Timer for low-cost animation
 * - Respects theme colors via UIManager
 * - Starts/stops based on visibility and lifecycle events
 */
public class TypingIndicatorRow extends JPanel {

    private static final Logger LOG = LoggerFactory.getLogger(TypingIndicatorRow.class);

    private static final int DOT_COUNT = 3;
    private static final int TICK_MS = 80; // ~12.5 FPS; low CPU
    private static final int DOT_DIAMETER = 6;  // slightly smaller
    private static final int DOT_SPACING = 8;   // slightly tighter
    private static final int BASE_HEIGHT = 24;  // slightly shorter row

    private Timer animationTimer;
    private int tickCounter = 0;

    public TypingIndicatorRow() {
        setOpaque(true);
        setBackground(ThemeUtils.panelBackground());
        setBorder(TriagePanelConstants.MESSAGE_COMPONENT_BORDER);
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setAlignmentY(Component.TOP_ALIGNMENT);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, getPreferredSize().height + TriagePanelConstants.COMPONENT_SPACING));

        getAccessibleContext().setAccessibleName("Assistant is typing");
        getAccessibleContext().setAccessibleDescription("Animated typing indicator");

        // Prepare timer but do not start until showing
        animationTimer = new Timer(TICK_MS, e -> {
            // Only repaint if showing to avoid unnecessary work
            if (isShowing()) {
                tickCounter++;
                repaint();
            }
        });
        animationTimer.setRepeats(true);

        // Start/stop on showing changes
        addHierarchyListener(new HierarchyListener() {
            @Override
            public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                    if (isShowing()) {
                        startAnimation();
                    } else {
                        pauseAnimation();
                    }
                }
            }
        });
    }

    @Override
    public Dimension getPreferredSize() {
        Insets insets = getInsets();
        int h = BASE_HEIGHT + (insets != null ? insets.top + insets.bottom : 0);
        return new Dimension(200, h);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        startAnimation();
    }

    @Override
    public void removeNotify() {
        stopAnimation();
        super.removeNotify();
    }

    public void startAnimation() {
        try {
            if (!animationTimer.isRunning()) {
                animationTimer.start();
                LOG.debug("TypingIndicator: animation started");
            }
        } catch (Exception ignore) {
        }
    }

    public void pauseAnimation() {
        try {
            if (animationTimer.isRunning()) {
                animationTimer.stop();
                LOG.debug("TypingIndicator: animation paused (not showing)");
            }
        } catch (Exception ignore) {
        }
    }

    public void stopAnimation() {
        try {
            if (animationTimer != null && animationTimer.isRunning()) {
                animationTimer.stop();
                LOG.debug("TypingIndicator: animation stopped");
            }
        } catch (Exception ignore) {
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Colors from theme
            Color fg = UIManager.getColor("Label.foreground");
            if (fg == null) fg = new Color(0xBBBBBB);
            Color bg = UIManager.getColor("Panel.background");
            if (bg == null) bg = getBackground();

            int width = getWidth();
            int height = getHeight();
            Insets insets = getInsets();
            int contentW = width - (insets != null ? insets.left + insets.right : 0);
            int contentH = height - (insets != null ? insets.top + insets.bottom : 0);

            // Layout: draw 3 dots left-aligned within content area
            int dotsWidth = DOT_COUNT * DOT_DIAMETER + (DOT_COUNT - 1) * DOT_SPACING;
            int startX = (insets != null ? insets.left : 0) + 6; // gentle left margin
            int baseY = (contentH) / 2 + (insets != null ? insets.top : 0);

            for (int i = 0; i < DOT_COUNT; i++) {
                int phase = (tickCounter + i * 3) % 24; // staggered phase
                double t = phase / 24.0; // 0..1
                double bob = Math.sin(t * Math.PI * 2) * 3.0; // vertical bobbing
                float alpha = (float) (0.4 + 0.6 * (0.5 + 0.5 * Math.sin(t * Math.PI * 2))); // 0.4..1.0

                int x = startX + i * (DOT_DIAMETER + DOT_SPACING);
                int y = (int) Math.round(baseY - DOT_DIAMETER / 2 - bob);

                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g2.setColor(fg);
                g2.fillOval(x, y, DOT_DIAMETER, DOT_DIAMETER);
            }

            // Restore
            g2.setComposite(AlphaComposite.SrcOver);
        } finally {
            g2.dispose();
        }
    }
}

