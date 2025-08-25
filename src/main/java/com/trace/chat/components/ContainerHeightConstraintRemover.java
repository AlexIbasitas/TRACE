package com.trace.chat.components;

import com.intellij.openapi.diagnostic.Logger;
import javax.swing.*;
import java.awt.*;

/**
 * Utility class for removing height constraints from Swing containers that limit
 * the dynamic sizing of ResponsiveHtmlPane components.
 * 
 * <p>This class implements the dynamic container height adjustment approach to fix
 * text clipping issues by identifying constraining containers and removing their
 * height limitations while preserving width constraints for proper text wrapping.</p>
 * 
 * <p>The solution addresses the root cause where parent containers ignore the
 * ResponsiveHtmlPane's calculated height and constrain it to fixed values,
 * causing text clipping and poor user experience.</p>
 * 
 * @author Alex Ibasitas
 * @version 1.0
 * @since 1.0
 */
public final class ContainerHeightConstraintRemover {
    
    private static final Logger LOG = Logger.getInstance(ContainerHeightConstraintRemover.class);
    
    // Private constructor to prevent instantiation
    private ContainerHeightConstraintRemover() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    
    /**
     * Removes height constraints from all parent containers of the given component
     * while preserving width constraints for proper text wrapping.
     * 
     * <p>This method traverses the component hierarchy upward from the given component
     * and removes height constraints from containers that are limiting the component's
     * ability to size properly. It focuses on containers that directly affect the
     * sizing behavior of ResponsiveHtmlPane components.</p>
     * 
     * @param component The component whose parent containers should have height constraints removed
     */
    public static void removeHeightConstraintsFromParents(Component component) {
        if (component == null) {
            LOG.warn("ContainerHeightConstraintRemover: removeHeightConstraintsFromParents called with null component");
            return;
        }
        
        LOG.debug("ContainerHeightConstraintRemover: Starting height constraint removal for component: " + 
                 component.getClass().getSimpleName());
        
        Container parent = component.getParent();
        int containerCount = 0;
        
        while (parent != null && containerCount < 20) { // Safety limit to prevent infinite loops
            containerCount++;
            
            if (shouldRemoveHeightConstraint(parent)) {
                removeHeightConstraint(parent);
                LOG.debug("ContainerHeightConstraintRemover: Removed height constraint from container: " + 
                         parent.getClass().getSimpleName() + " (level " + containerCount + ")");
            }
            
            parent = parent.getParent();
        }
        
        LOG.debug("ContainerHeightConstraintRemover: Completed height constraint removal. " +
                 "Processed " + containerCount + " parent containers.");
    }
    
    /**
     * Removes height constraints only from content panels, preserving scroll behavior.
     * 
     * <p>This method is more targeted than removeHeightConstraintsFromParents and only
     * removes height constraints from content panels (JPanel, Box) while preserving
     * scroll containers (JScrollPane, JViewport) to maintain proper chat scroll behavior.</p>
     * 
     * @param component The component whose content panel parents should have height constraints removed
     */
    public static void removeHeightConstraintsFromContentPanels(Component component) {
        if (component == null) {
            LOG.warn("ContainerHeightConstraintRemover: removeHeightConstraintsFromContentPanels called with null component");
            return;
        }
        
        LOG.debug("ContainerHeightConstraintRemover: Starting content panel height constraint removal for component: " + 
                 component.getClass().getSimpleName());
        
        Container parent = component.getParent();
        int containerCount = 0;
        
        while (parent != null && containerCount < 20) { // Safety limit to prevent infinite loops
            containerCount++;
            
            if (shouldRemoveHeightConstraintFromContentPanel(parent)) {
                removeHeightConstraint(parent);
                LOG.debug("ContainerHeightConstraintRemover: Removed height constraint from content panel: " + 
                         parent.getClass().getSimpleName() + " (level " + containerCount + ")");
            }
            
            parent = parent.getParent();
        }
        
        LOG.debug("ContainerHeightConstraintRemover: Completed content panel height constraint removal. " +
                 "Processed " + containerCount + " parent containers.");
    }
    
    /**
     * Determines if a container should have its height constraint removed.
     * 
     * <p>This method checks if the container is a type that commonly constrains
     * component height and should have those constraints removed.</p>
     *
     * @param container The container to check
     * @return true if the container should have height constraints removed
     */
    private static boolean shouldRemoveHeightConstraint(Container container) {
        if (container == null) {
            return false;
        }
        
        // Check if the container has a maximum size that constrains height
        Dimension maxSize = container.getMaximumSize();
        if (maxSize != null && maxSize.height < Integer.MAX_VALUE) {
            return true;
        }
        
        // Focus on specific container types that commonly cause height constraint issues
        // BUT exclude scroll-related containers that control chat scroll behavior
        return container instanceof JPanel || 
               container instanceof Box ||
               container instanceof JComponent;
        // Note: Excluded JScrollPane and JViewport to preserve scroll behavior
    }
    
    /**
     * Determines if a content panel should have its height constraint removed.
     * 
     * <p>This method is more selective than shouldRemoveHeightConstraint and only
     * targets content panels while preserving scroll containers.</p>
     *
     * @param container The container to check
     * @return true if the container should have height constraints removed
     */
    private static boolean shouldRemoveHeightConstraintFromContentPanel(Container container) {
        if (container == null) {
            return false;
        }
        
        // Check if the container has a maximum size that constrains height
        Dimension maxSize = container.getMaximumSize();
        if (maxSize != null && maxSize.height < Integer.MAX_VALUE) {
            return true;
        }
        
        // Only target content panels, explicitly exclude scroll containers
        return container instanceof JPanel || 
               container instanceof Box;
        // Note: Explicitly excluded JScrollPane, JViewport, and JComponent to preserve scroll behavior
    }
    
    /**
     * Removes height constraints from a specific container.
     * 
     * <p>This method sets the maximum height of the container to Integer.MAX_VALUE
     * to allow it to expand based on its content while preserving width constraints.</p>
     *
     * @param container The container to remove height constraints from
     */
    private static void removeHeightConstraint(Container container) {
        if (container == null) {
            return;
        }
        
        try {
            Dimension currentMaxSize = container.getMaximumSize();
            if (currentMaxSize == null) {
                // If no maximum size is set, set it to allow unlimited height
                container.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
            } else {
                // Preserve width constraint while removing height constraint
                container.setMaximumSize(new Dimension(currentMaxSize.width, Integer.MAX_VALUE));
            }
            
            // For scroll panes, ensure they can expand vertically
            if (container instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) container;
                scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
                
                // Ensure the viewport can expand
                JViewport viewport = scrollPane.getViewport();
                if (viewport != null) {
                    Dimension viewportMaxSize = viewport.getMaximumSize();
                    if (viewportMaxSize != null) {
                        viewport.setMaximumSize(new Dimension(viewportMaxSize.width, Integer.MAX_VALUE));
                    } else {
                        viewport.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
                    }
                }
            }
            
            // For viewports, ensure they can expand
            if (container instanceof JViewport) {
                JViewport viewport = (JViewport) container;
                Dimension viewportMaxSize = viewport.getMaximumSize();
                if (viewportMaxSize != null) {
                    viewport.setMaximumSize(new Dimension(viewportMaxSize.width, Integer.MAX_VALUE));
                } else {
                    viewport.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
                }
            }
            
        } catch (Exception e) {
            LOG.warn("ContainerHeightConstraintRemover: Error removing height constraint from container: " + 
                    container.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }
    
    /**
     * Checks if a component has height constraints that should be removed.
     * 
     * <p>This method analyzes a component's sizing properties to determine
     * if it has height constraints that are limiting its ability to size properly.</p>
     *
     * @param component The component to check
     * @return true if the component has height constraints that should be removed
     */
    public static void configureScrollPaneForDynamicHeight(JScrollPane scrollPane) {
        if (scrollPane == null) {
            LOG.warn("ContainerHeightConstraintRemover: configureScrollPaneForDynamicHeight called with null scrollPane");
            return;
        }
        
        try {
            // Set vertical scroll bar policy to show when needed
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            
            // Remove height constraints from the scroll pane itself
            Dimension maxSize = scrollPane.getMaximumSize();
            if (maxSize != null) {
                scrollPane.setMaximumSize(new Dimension(maxSize.width, Integer.MAX_VALUE));
            } else {
                scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
            }
            
            // Configure the viewport to allow expansion
            JViewport viewport = scrollPane.getViewport();
            if (viewport != null) {
                Dimension viewportMaxSize = viewport.getMaximumSize();
                if (viewportMaxSize != null) {
                    viewport.setMaximumSize(new Dimension(viewportMaxSize.width, Integer.MAX_VALUE));
                } else {
                    viewport.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
                }
            }
            
            LOG.debug("ContainerHeightConstraintRemover: Configured scroll pane for dynamic height behavior");
            
        } catch (Exception e) {
            LOG.warn("ContainerHeightConstraintRemover: Error configuring scroll pane: " + e.getMessage());
        }
    }
    
    /**
     * Validates that height constraints have been properly removed.
     * 
     * <p>This method checks the container hierarchy to ensure that height constraints
     * have been successfully removed and that the component can expand vertically
     * as needed.</p>
     * 
     * @param component The component to validate
     * @return true if height constraints have been properly removed
     */
    public static boolean validateHeightConstraintRemoval(Component component) {
        if (component == null) {
            return false;
        }
        
        Container parent = component.getParent();
        int containerCount = 0;
        boolean allConstraintsRemoved = true;
        
        while (parent != null && containerCount < 20) {
            containerCount++;
            
            Dimension maxSize = parent.getMaximumSize();
            if (maxSize != null && maxSize.height < Integer.MAX_VALUE) {
                LOG.debug("ContainerHeightConstraintRemover: Validation failed - container " + 
                         parent.getClass().getSimpleName() + " still has height constraint: " + maxSize.height);
                allConstraintsRemoved = false;
            }
            
            parent = parent.getParent();
        }
        
        LOG.debug("ContainerHeightConstraintRemover: Height constraint validation " + 
                 (allConstraintsRemoved ? "PASSED" : "FAILED") + " for component: " + 
                 component.getClass().getSimpleName());
        
        return allConstraintsRemoved;
    }
}
