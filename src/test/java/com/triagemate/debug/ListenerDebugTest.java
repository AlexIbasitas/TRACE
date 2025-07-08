package com.triagemate.debug;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.triagemate.listeners.CucumberTestExecutionListener;
import com.triagemate.ui.TriagePanelToolWindowFactory;
import com.triagemate.ui.TriagePanelView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Debug test to verify listener and panel communication
 */
public class ListenerDebugTest extends BasePlatformTestCase {

    private CucumberTestExecutionListener listener;
    private TriagePanelView panel;

    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        listener = new CucumberTestExecutionListener(getProject());
        panel = new TriagePanelView(getProject());
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            // Clean up panel instances
            TriagePanelToolWindowFactory.removePanelForProject(getProject());
        } catch (Exception e) {
            addSuppressedException(e);
        } finally {
            super.tearDown();
        }
    }

    @Test
    public void testListenerCreation() {
        System.out.println("TriageMate Debug: Testing listener creation");
        assertNotNull(listener);
        System.out.println("TriageMate Debug: Listener created successfully");
    }

    @Test
    public void testPanelCreation() {
        System.out.println("TriageMate Debug: Testing panel creation");
        assertNotNull(panel);
        System.out.println("TriageMate Debug: Panel created successfully");
    }

    @Test
    public void testPanelCommunication() {
        System.out.println("TriageMate Debug: Testing panel communication");
        
        // Manually store the panel for testing (simulating what the factory does)
        // We'll use reflection to access the private static map
        try {
            java.lang.reflect.Field field = TriagePanelToolWindowFactory.class.getDeclaredField("panelInstances");
            field.setAccessible(true);
            java.util.concurrent.ConcurrentHashMap<com.intellij.openapi.project.Project, TriagePanelView> map = 
                (java.util.concurrent.ConcurrentHashMap<com.intellij.openapi.project.Project, TriagePanelView>) field.get(null);
            map.put(getProject(), panel);
            
            // Test if we can retrieve the panel
            TriagePanelView retrievedPanel = TriagePanelToolWindowFactory.getPanelForProject(getProject());
            assertNotNull(retrievedPanel);
            System.out.println("TriageMate Debug: Panel communication test passed");
            
            // Clean up
            map.remove(getProject());
        } catch (Exception e) {
            fail("Failed to test panel communication: " + e.getMessage());
        }
    }
} 