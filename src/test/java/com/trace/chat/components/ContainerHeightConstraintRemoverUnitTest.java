package com.trace.chat.components;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import javax.swing.*;
import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the ContainerHeightConstraintRemover class.
 *
 * <p>These tests verify that the ContainerHeightConstraintRemover correctly identifies
 * and removes height constraints from parent containers while preserving width constraints
 * for proper text wrapping.</p>
 *
 * @author Alex Ibasitas
 * @version 1.0
 * @since 1.0
 */
@DisplayName("ContainerHeightConstraintRemover Unit Tests")
class ContainerHeightConstraintRemoverUnitTest {

    private JPanel testComponent;
    private JPanel parentPanel;
    private JScrollPane scrollPane;
    private JViewport viewport;

    @BeforeEach
    void setUp() {
        // Create a test component hierarchy
        testComponent = new JPanel();
        parentPanel = new JPanel();
        scrollPane = new JScrollPane();
        viewport = new JViewport();
        
        // Set up the hierarchy: testComponent -> parentPanel -> viewport -> scrollPane
        parentPanel.add(testComponent);
        viewport.setView(parentPanel);
        scrollPane.setViewport(viewport);
    }

    @Test
    @DisplayName("should remove height constraints from parent containers")
    void shouldRemoveHeightConstraintsFromParentContainers() {
        // Set height constraints on parent containers
        parentPanel.setMaximumSize(new Dimension(500, 300));
        viewport.setMaximumSize(new Dimension(480, 280));
        scrollPane.setMaximumSize(new Dimension(460, 260));

        // Verify initial constraints are set
        assertEquals(300, parentPanel.getMaximumSize().height);
        assertEquals(280, viewport.getMaximumSize().height);
        assertEquals(260, scrollPane.getMaximumSize().height);

        // Remove height constraints
        ContainerHeightConstraintRemover.removeHeightConstraintsFromParents(testComponent);

        // Verify height constraints have been removed while preserving width
        assertEquals(Integer.MAX_VALUE, parentPanel.getMaximumSize().height);
        assertEquals(500, parentPanel.getMaximumSize().width);
        
        assertEquals(Integer.MAX_VALUE, viewport.getMaximumSize().height);
        assertEquals(480, viewport.getMaximumSize().width);
        
        assertEquals(Integer.MAX_VALUE, scrollPane.getMaximumSize().height);
        assertEquals(460, scrollPane.getMaximumSize().width);
    }

    @Test
    @DisplayName("should handle null component gracefully")
    void shouldHandleNullComponentGracefully() {
        // Should not throw exception when called with null
        assertDoesNotThrow(() -> {
            ContainerHeightConstraintRemover.removeHeightConstraintsFromParents(null);
        });
    }

    @Test
    @DisplayName("should handle component without parent gracefully")
    void shouldHandleComponentWithoutParentGracefully() {
        // Create a component without a parent
        JPanel orphanComponent = new JPanel();
        
        // Should not throw exception
        assertDoesNotThrow(() -> {
            ContainerHeightConstraintRemover.removeHeightConstraintsFromParents(orphanComponent);
        });
    }

    @Test
    @DisplayName("should configure scroll pane for dynamic height behavior")
    void shouldConfigureScrollPaneForDynamicHeightBehavior() {
        // Set initial constraints
        scrollPane.setMaximumSize(new Dimension(500, 300));
        viewport.setMaximumSize(new Dimension(480, 280));

        // Configure scroll pane
        ContainerHeightConstraintRemover.configureScrollPaneForDynamicHeight(scrollPane);

        // Verify scroll pane is configured correctly
        assertEquals(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, scrollPane.getVerticalScrollBarPolicy());
        assertEquals(Integer.MAX_VALUE, scrollPane.getMaximumSize().height);
        assertEquals(500, scrollPane.getMaximumSize().width);
        
        // Verify viewport is configured correctly
        assertEquals(Integer.MAX_VALUE, viewport.getMaximumSize().height);
        assertEquals(480, viewport.getMaximumSize().width);
    }

    @Test
    @DisplayName("should handle null scroll pane gracefully")
    void shouldHandleNullScrollPaneGracefully() {
        // Should not throw exception when called with null
        assertDoesNotThrow(() -> {
            ContainerHeightConstraintRemover.configureScrollPaneForDynamicHeight(null);
        });
    }

    @Test
    @DisplayName("should validate height constraint removal")
    void shouldValidateHeightConstraintRemoval() {
        // Set height constraints initially
        parentPanel.setMaximumSize(new Dimension(500, 300));
        viewport.setMaximumSize(new Dimension(480, 280));

        // Validation should fail before removal
        assertFalse(ContainerHeightConstraintRemover.validateHeightConstraintRemoval(testComponent));

        // Remove height constraints
        ContainerHeightConstraintRemover.removeHeightConstraintsFromParents(testComponent);

        // Validation should pass after removal
        assertTrue(ContainerHeightConstraintRemover.validateHeightConstraintRemoval(testComponent));
    }

    @Test
    @DisplayName("should handle containers with null maximum size")
    void shouldHandleContainersWithNullMaximumSize() {
        // Set some containers to have null maximum size
        parentPanel.setMaximumSize(null);
        viewport.setMaximumSize(new Dimension(480, 280));

        // Should not throw exception and should set proper maximum size
        assertDoesNotThrow(() -> {
            ContainerHeightConstraintRemover.removeHeightConstraintsFromParents(testComponent);
        });

        // Verify that null maximum size was handled correctly
        assertNotNull(parentPanel.getMaximumSize());
        assertEquals(Integer.MAX_VALUE, parentPanel.getMaximumSize().height);
        assertEquals(Integer.MAX_VALUE, parentPanel.getMaximumSize().width);
    }

    @Test
    @DisplayName("should preserve width constraints while removing height constraints")
    void shouldPreserveWidthConstraintsWhileRemovingHeightConstraints() {
        // Set both width and height constraints
        parentPanel.setMaximumSize(new Dimension(500, 300));
        viewport.setMaximumSize(new Dimension(480, 280));

        // Remove height constraints
        ContainerHeightConstraintRemover.removeHeightConstraintsFromParents(testComponent);

        // Verify width constraints are preserved
        assertEquals(500, parentPanel.getMaximumSize().width);
        assertEquals(480, viewport.getMaximumSize().width);
        
        // Verify height constraints are removed
        assertEquals(Integer.MAX_VALUE, parentPanel.getMaximumSize().height);
        assertEquals(Integer.MAX_VALUE, viewport.getMaximumSize().height);
    }

    @Test
    @DisplayName("should handle deep container hierarchy")
    void shouldHandleDeepContainerHierarchy() {
        // Create a deeper hierarchy
        JPanel level1 = new JPanel();
        JPanel level2 = new JPanel();
        JPanel level3 = new JPanel();
        JPanel level4 = new JPanel();
        
        // Set constraints on each level
        level1.setMaximumSize(new Dimension(400, 200));
        level2.setMaximumSize(new Dimension(380, 180));
        level3.setMaximumSize(new Dimension(360, 160));
        level4.setMaximumSize(new Dimension(340, 140));
        
        // Build hierarchy: testComponent -> level4 -> level3 -> level2 -> level1
        level4.add(testComponent);
        level3.add(level4);
        level2.add(level3);
        level1.add(level2);

        // Remove height constraints
        ContainerHeightConstraintRemover.removeHeightConstraintsFromParents(testComponent);

        // Verify all levels have height constraints removed
        assertEquals(Integer.MAX_VALUE, level1.getMaximumSize().height);
        assertEquals(Integer.MAX_VALUE, level2.getMaximumSize().height);
        assertEquals(Integer.MAX_VALUE, level3.getMaximumSize().height);
        assertEquals(Integer.MAX_VALUE, level4.getMaximumSize().height);
        
        // Verify width constraints are preserved
        assertEquals(400, level1.getMaximumSize().width);
        assertEquals(380, level2.getMaximumSize().width);
        assertEquals(360, level3.getMaximumSize().width);
        assertEquals(340, level4.getMaximumSize().width);
    }
}
