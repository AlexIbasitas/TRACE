package com.trace.chat.ui;

import javax.swing.*;
import java.awt.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.trace.chat.components.MessageComponent;
import com.trace.chat.components.ChatMessage;
import com.trace.chat.components.TypingIndicatorRow;

/**
 * Helper class for scroll-related functionality in the TriagePanelView.
 * Provides methods for smooth scrolling, scroll position calculations, and scroll behavior management.
 * 
 * <p>This class encapsulates all scroll-related operations to reduce the complexity
 * of the main TriagePanelView class and improve code organization.</p>
 * 
 * @author Alex Ibasitas
 * @version 1.0
 * @since 1.0
 */
public class ScrollHelper {
    
    private static final Logger LOG = Logger.getInstance(ScrollHelper.class);
    
    // Scroll/align tuning constants
    private static final int NEAR_TARGET_THRESHOLD_PX = 120; // widen near-window
    private static final int RESCROLL_DEBOUNCE_MS = 80;      // 60–100 ms debounce
    private static final int SMOOTH_SCROLL_DURATION_MS = 220; // slightly longer ease-out
    private static final int LAYOUT_SETTLE_DELAY_MS = 40; // Small delay to allow layout/HTML wrap to settle before computing scroll target
    
    // Scroll state management
    private javax.swing.Timer smoothScrollTimer;
    private boolean isSmoothScrolling = false;
    private boolean isProgrammaticScroll = false;
    private javax.swing.Timer rescrollDebounceTimer;
    private boolean maintainAlignAfterAppend = false;
    private boolean anchorActiveForAppend = false;
    private int anchorUserTopYBeforeAppend = 0;
    private int anchorScrollValueBeforeAppend = 0;
    private boolean wasNearBottomBeforeUserSend = false;
    
    // Component references (will be set by TriagePanelView)
    private JScrollPane chatScrollPane;
    private JPanel messageContainer;
    private JPanel bottomSpacer;
    private JComponent latestUserMessageComponent;
    private JButton newMessagesChip;
    private JPanel chatOverlayPanel;
    
    /**
     * Sets the scroll pane reference for this helper.
     * 
     * @param chatScrollPane The scroll pane to manage
     */
    public void setChatScrollPane(JScrollPane chatScrollPane) {
        this.chatScrollPane = chatScrollPane;
    }
    
    /**
     * Sets the message container reference for this helper.
     * 
     * @param messageContainer The message container to manage
     */
    public void setMessageContainer(JPanel messageContainer) {
        this.messageContainer = messageContainer;
    }
    
    /**
     * Sets the bottom spacer reference for this helper.
     * 
     * @param bottomSpacer The bottom spacer to manage
     */
    public void setBottomSpacer(JPanel bottomSpacer) {
        this.bottomSpacer = bottomSpacer;
    }
    
    /**
     * Sets the latest user message component reference for this helper.
     * 
     * @param latestUserMessageComponent The latest user message component
     */
    public void setLatestUserMessageComponent(JComponent latestUserMessageComponent) {
        this.latestUserMessageComponent = latestUserMessageComponent;
    }
    
    /**
     * Sets the new messages chip reference for this helper.
     * 
     * @param newMessagesChip The new messages chip button
     */
    public void setNewMessagesChip(JButton newMessagesChip) {
        this.newMessagesChip = newMessagesChip;
    }
    
    /**
     * Sets the chat overlay panel reference for this helper.
     * 
     * @param chatOverlayPanel The chat overlay panel
     */
    public void setChatOverlayPanel(JPanel chatOverlayPanel) {
        this.chatOverlayPanel = chatOverlayPanel;
    }
    
    /**
     * Returns true if the viewport is at the very bottom (distance == 0).
     */
    public boolean isAtBottom(JScrollPane sp) {
        if (sp == null) return false;
        JScrollBar sb = sp.getVerticalScrollBar();
        int value = sb.getValue();
        int extent = sb.getModel().getExtent();
        int max = sb.getMaximum();
        int distance = Math.max(0, max - (value + extent));
        return distance == 0;
    }

    /** Returns true if the viewport is within thresholdPx of the bottom. */
    public boolean isNearBottom(JScrollPane sp, int thresholdPx) {
        if (sp == null) return false;
        JScrollBar sb = sp.getVerticalScrollBar();
        int value = sb.getValue();
        int extent = sb.getModel().getExtent();
        int max = sb.getMaximum();
        int distance = Math.max(0, max - (value + extent));
        return distance <= Math.max(0, thresholdPx);
    }

    // --- Align-newest-to-top helpers ---

    public int computeAlignTopTarget(JScrollPane sp, JComponent row) {
        if (sp == null || row == null) return 0;
        JScrollBar sb = sp.getVerticalScrollBar();
        int extent = sb.getModel().getExtent();
        int max = sb.getMaximum();
        int maxScroll = Math.max(0, max - extent);
        int y = row.getY();
        LOG.debug("alignTop: rowY=" + y + " max=" + max + " extent=" + extent + " maxScroll=" + maxScroll);
        return Math.max(0, Math.min(y, maxScroll));
    }

    public boolean isNearTarget(JScrollPane sp, int target, int thresholdPx) {
        if (sp == null) return false;
        JScrollBar sb = sp.getVerticalScrollBar();
        int value = sb.getValue();
        return Math.abs(value - Math.max(0, target)) <= Math.max(0, thresholdPx);
    }

    public void requestAlignNewestIfNear(JScrollPane sp) {
        if (sp == null || latestUserMessageComponent == null) return;
        // Ensure spacer is up-to-date before computing target
        recomputeBottomSpacer();
        int target = computeAlignTopTarget(sp, latestUserMessageComponent);
        if (isNearTarget(sp, target, NEAR_TARGET_THRESHOLD_PX)) {
            scrollToComponentTopSmooth(sp, latestUserMessageComponent, SMOOTH_SCROLL_DURATION_MS);
        } else {
            showNewMessagesChip();
        }
    }

    public void scrollToComponentTopSmooth(JScrollPane sp, JComponent row, int durationMs) {
        if (sp == null || row == null) return;
        final int duration = (durationMs <= 0) ? 200 : durationMs;
        JScrollBar sb = sp.getVerticalScrollBar();
        int start = sb.getValue();
        int target = computeAlignTopTarget(sp, row);
        LOG.debug("smoothStart: start=" + start + " target=" + target + " durationMs=" + duration);
        if (Math.abs(start - target) <= 1) {
            isProgrammaticScroll = true;
            sb.setValue(target);
            isProgrammaticScroll = false;
            hideNewMessagesChip();
            return;
        }

        cancelSmoothScroll();
        final long startTime = System.currentTimeMillis();
        isSmoothScrolling = true;
        smoothScrollTimer = new javax.swing.Timer(15, null);
        smoothScrollTimer.addActionListener((java.awt.event.ActionEvent e) -> {
            long elapsed = System.currentTimeMillis() - startTime;
            double t = Math.min(1.0, (double) elapsed / duration);
            double p = 1 - Math.pow(1 - t, 3); // easeOutCubic

            int dynamicTarget = computeAlignTopTarget(sp, row);
            int value = start + (int) Math.round((dynamicTarget - start) * p);
            // Reduce noise: only log occasionally
            if ((elapsed / 45) % 3 == 0) {
                LOG.debug("smoothTick: t=" + String.format("%.3f", t) +
                    " p=" + String.format("%.3f", p) +
                    " value=" + value +
                    " dynTarget=" + dynamicTarget);
            }

            isProgrammaticScroll = true;
            sb.setValue(value);
            isProgrammaticScroll = false;

            if (t >= 1.0) {
                LOG.debug("smoothDone: value=" + sb.getValue() +
                    " target=" + dynamicTarget +
                    " atTop?=" + isNearTarget(sp, dynamicTarget, 2));
                cancelSmoothScroll();
                hideNewMessagesChip();
            }
        });
        smoothScrollTimer.start();
    }

    public void cancelSmoothScroll() {
        if (smoothScrollTimer != null) {
            smoothScrollTimer.stop();
            smoothScrollTimer = null;
        }
        isSmoothScrolling = false;
    }

    /** Immediately align a row's top to the viewport top without animation. */
    public void alignTopImmediate(JScrollPane sp, JComponent row) {
        if (sp == null || row == null) return;
        int target = computeAlignTopTarget(sp, row);
        isProgrammaticScroll = true;
        sp.getVerticalScrollBar().setValue(target);
        isProgrammaticScroll = false;
        hideNewMessagesChip();
    }

    public void showNewMessagesChip() {
        if (newMessagesChip != null && !newMessagesChip.isVisible()) {
            newMessagesChip.setVisible(true);
            if (chatOverlayPanel != null) {
                chatOverlayPanel.revalidate();
                chatOverlayPanel.repaint();
            }
        }
    }

    public void hideNewMessagesChip() {
        if (newMessagesChip != null && newMessagesChip.isVisible()) {
            newMessagesChip.setVisible(false);
            if (chatOverlayPanel != null) {
                chatOverlayPanel.revalidate();
                chatOverlayPanel.repaint();
            }
        }
    }

    public void scheduleDebouncedReScroll() {
        if (rescrollDebounceTimer != null && rescrollDebounceTimer.isRunning()) {
            rescrollDebounceTimer.stop();
        }
        rescrollDebounceTimer = new javax.swing.Timer(RESCROLL_DEBOUNCE_MS, evt -> {
            if (chatScrollPane != null && latestUserMessageComponent != null) {
                recomputeBottomSpacer();
                requestAlignNewestIfNear(chatScrollPane);
            }
        });
        rescrollDebounceTimer.setRepeats(false);
        rescrollDebounceTimer.start();
    }

    public void recomputeBottomSpacer() {
        if (chatScrollPane == null || bottomSpacer == null) return;
        int viewportH = chatScrollPane.getViewport().getExtentSize().height;
        if (latestUserMessageComponent == null) {
            // No user messages yet; avoid introducing artificial bottom whitespace
            bottomSpacer.setPreferredSize(new Dimension(1, 0));
            messageContainer.revalidate();
            messageContainer.repaint();
            return;
        }

        int rowH = latestUserMessageComponent.getHeight() > 0
            ? latestUserMessageComponent.getHeight()
            : latestUserMessageComponent.getPreferredSize().height;
        int rowY = latestUserMessageComponent.getY();

        // If the latest user row is taller than the viewport, spacer should be 0
        if (rowH >= viewportH) {
            bottomSpacer.setPreferredSize(new Dimension(1, 0));
            messageContainer.revalidate();
            messageContainer.repaint();
            return;
        }

        // Make after-row height equal to the viewport height so rowY is always reachable as a scroll target
        int currentSpacerH = bottomSpacer.getPreferredSize() != null ? bottomSpacer.getPreferredSize().height : 0;
        int totalHNoSpacer = messageContainer.getPreferredSize().height - currentSpacerH;
        int heightBelowRow = Math.max(0, totalHNoSpacer - rowY);
        int spacerH = Math.max(0, viewportH - heightBelowRow);
        bottomSpacer.setPreferredSize(new Dimension(1, spacerH));

        // Log scroll metrics
        JScrollBar sb = chatScrollPane.getVerticalScrollBar();
        int extent = sb.getModel().getExtent();
        int max = sb.getMaximum();
        int maxScroll = Math.max(0, max - extent);
        LOG.debug("spacer: h=" + spacerH + " rowY=" + rowY + " max=" + max + " extent=" + extent + " maxScroll=" + maxScroll);

        messageContainer.revalidate();
        messageContainer.repaint();
    }
    
    /**
     * Restores the viewport so the latest user row remains at the same viewport position
     * as before the AI append, avoiding any jump to bottom or top.
     */
    public void restoreAnchorAfterAppend() {
        if (!anchorActiveForAppend || chatScrollPane == null) {
            maintainAlignAfterAppend = false;
            return;
        }
        try {
            JScrollBar sb = chatScrollPane.getVerticalScrollBar();
            int extent = sb.getModel().getExtent();
            int maxScroll = Math.max(0, sb.getMaximum() - extent);
            int newY = latestUserMessageComponent != null ? latestUserMessageComponent.getY() : anchorUserTopYBeforeAppend;
            int delta = newY - anchorUserTopYBeforeAppend;
            int newValue = Math.max(0, Math.min(maxScroll, anchorScrollValueBeforeAppend + delta));
            isProgrammaticScroll = true;
            sb.setValue(newValue);
            isProgrammaticScroll = false;
        } catch (Exception ignore) {
        } finally {
            anchorActiveForAppend = false;
            maintainAlignAfterAppend = false;
        }
    }
    
    /**
     * Replaces the typing indicator row with the provided AI message component in-place
     * to avoid visual jumping. Keeps spacer as last child, and avoids any animated scroll.
     */
    public void replaceTypingIndicatorWithMessageComponent(MessageComponent aiComponent, TypingIndicatorRow typingIndicatorRow) {
        if (messageContainer == null || aiComponent == null) return;

        int count = messageContainer.getComponentCount();
        int idx = -1;
        for (int i = 0; i < count; i++) {
            if (messageContainer.getComponent(i) == typingIndicatorRow) {
                idx = i;
                break;
            }
        }
        if (idx >= 0) {
            // Stop animation and clear indicator state but do not trigger rebuild
            try { if (typingIndicatorRow != null) typingIndicatorRow.stopAnimation(); } catch (Exception ignore) {}

            // Replace indicator with AI component at the same index
            messageContainer.remove(idx);
            aiComponent.setAlignmentY(Component.TOP_ALIGNMENT);
            messageContainer.add(aiComponent, idx);

            // Ensure bottom spacer is the last child
            if (bottomSpacer != null) {
                // Remove and re-add to guarantee it is last
                messageContainer.remove(bottomSpacer);
                messageContainer.add(bottomSpacer);
            }

            // Refresh layout and recompute spacer to keep maxScroll tight
            messageContainer.revalidate();
            messageContainer.repaint();
            ApplicationManager.getApplication().invokeLater(() -> {
                recomputeBottomSpacer();
                restoreAnchorAfterAppend();
            });
        }
    }
    
    // Getters for state variables that TriagePanelView needs to access
    public boolean isSmoothScrolling() {
        return isSmoothScrolling;
    }
    
    public boolean isProgrammaticScroll() {
        return isProgrammaticScroll;
    }
    
    public void setProgrammaticScroll(boolean programmaticScroll) {
        isProgrammaticScroll = programmaticScroll;
    }
    
    public boolean isMaintainAlignAfterAppend() {
        return maintainAlignAfterAppend;
    }
    
    public void setMaintainAlignAfterAppend(boolean maintainAlignAfterAppend) {
        this.maintainAlignAfterAppend = maintainAlignAfterAppend;
    }
    
    public boolean isAnchorActiveForAppend() {
        return anchorActiveForAppend;
    }
    
    public void setAnchorActiveForAppend(boolean anchorActiveForAppend) {
        this.anchorActiveForAppend = anchorActiveForAppend;
    }
    
    public int getAnchorUserTopYBeforeAppend() {
        return anchorUserTopYBeforeAppend;
    }
    
    public void setAnchorUserTopYBeforeAppend(int anchorUserTopYBeforeAppend) {
        this.anchorUserTopYBeforeAppend = anchorUserTopYBeforeAppend;
    }
    
    public int getAnchorScrollValueBeforeAppend() {
        return anchorScrollValueBeforeAppend;
    }
    
    public void setAnchorScrollValueBeforeAppend(int anchorScrollValueBeforeAppend) {
        this.anchorScrollValueBeforeAppend = anchorScrollValueBeforeAppend;
    }
    
    public boolean isWasNearBottomBeforeUserSend() {
        return wasNearBottomBeforeUserSend;
    }
    
    public void setWasNearBottomBeforeUserSend(boolean wasNearBottomBeforeUserSend) {
        this.wasNearBottomBeforeUserSend = wasNearBottomBeforeUserSend;
    }
    
    public int getNearTargetThresholdPx() {
        return NEAR_TARGET_THRESHOLD_PX;
    }
    
    public int getSmoothScrollDurationMs() {
        return SMOOTH_SCROLL_DURATION_MS;
    }
    
    public int getLayoutSettleDelayMs() {
        return LAYOUT_SETTLE_DELAY_MS;
    }
}
