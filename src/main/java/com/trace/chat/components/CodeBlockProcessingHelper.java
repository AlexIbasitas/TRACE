package com.trace.chat.components;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.ui.UIUtil;
import com.trace.common.utils.ThemeUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseWheelEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class for code block processing operations in markdown rendering.
 * 
 * <p>This class provides methods for processing code blocks in markdown content,
 * including detection, layout creation, and scrollable component generation. It handles
 * code block processing operations to ensure proper display of wide code content
 * with horizontal scrolling capabilities.</p>
 * 
 * <p>The helper encapsulates all code block processing logic to keep the main markdown
 * renderer focused on core rendering while delegating code block processing to this
 * specialized helper.</p>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
public class CodeBlockProcessingHelper {
    
    private static final Logger LOG = Logger.getInstance(CodeBlockProcessingHelper.class);
    
    private CodeBlockProcessingHelper() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Checks if the markdown contains Java code blocks that should use scrollable components.
     * 
     * @param markdown The markdown text to check
     * @return true if Java code blocks are found
     */
    public static boolean containsJavaCodeBlocks(String markdown) {
        if (markdown == null || markdown.trim().isEmpty()) {
            return false;
        }

        // Look for Java code blocks with ```java
        Pattern javaCodePattern = Pattern.compile("```java\\s*\\n(.*?)```", Pattern.DOTALL);
        Matcher matcher = javaCodePattern.matcher(markdown);
        
        boolean found = matcher.find();
        if (found) {
            LOG.info("Found Java code blocks - will use scrollable components");
        }
        
        return found;
    }

    /**
     * Creates a mixed component layout with scrollable code blocks for wide content.
     * 
     * @param markdown The markdown text containing wide code blocks
     * @return A JPanel with mixed content layout
     */
    public static JComponent createMixedMarkdownComponent(String markdown) {
        LOG.info("Creating mixed markdown component with scrollable code blocks");
        
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setOpaque(false);
        container.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Split the markdown by Java code blocks and process each part
        Pattern javaCodePattern = Pattern.compile("(```java\\s*\\n.*?```)", Pattern.DOTALL);
        String[] parts = javaCodePattern.split(markdown);
        
        // Find all Java code blocks
        Matcher matcher = javaCodePattern.matcher(markdown);
        java.util.List<String> codeBlocks = new java.util.ArrayList<>();
        while (matcher.find()) {
            codeBlocks.add(matcher.group(1));
        }

        // Process each part alternating between text and code
        int codeBlockIndex = 0;
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            
            // Add text part if not empty
            if (!part.trim().isEmpty()) {
                JEditorPane textPane = MarkdownRenderer.createMarkdownPane(part);
                textPane.setAlignmentX(Component.LEFT_ALIGNMENT);
                container.add(textPane);
                
                // Add some spacing
                container.add(Box.createVerticalStrut(5));
            }
            
            // Add code block if available
            if (codeBlockIndex < codeBlocks.size()) {
                String codeBlock = codeBlocks.get(codeBlockIndex);
                
                // Extract just the code content (remove ```java and ```)
                String codeContent = codeBlock.replaceFirst("```java\\s*\\n", "").replaceFirst("```$", "");
                
                // Create scrollable component for ALL Java code blocks
                JScrollPane scrollableCode = createScrollableCodeComponent(codeContent);
                scrollableCode.setAlignmentX(Component.LEFT_ALIGNMENT);
                container.add(scrollableCode);
                
                codeBlockIndex++;
                
                // Add some spacing after code block
                container.add(Box.createVerticalStrut(5));
            }
        }

        LOG.info("Created mixed component with " + parts.length + " text parts and " + codeBlocks.size() + " code blocks");
        return container;
    }

    /**
     * Detects if a code block contains content that would be too wide for normal display.
     * Checks for long lines that would benefit from horizontal scrolling.
     * 
     * @param codeContent The content of the code block
     * @return true if the code block contains wide content
     */
    public static boolean isCodeBlockWide(String codeContent) {
        if (codeContent == null || codeContent.trim().isEmpty()) {
            return false;
        }
        
        // Split into lines and check each line length
        String[] lines = codeContent.split("\n");
        for (String line : lines) {
            // Consider a line "wide" if it's longer than 80 characters
            // This is a reasonable threshold for code readability
            if (line.length() > 80) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Detected wide code line (" + line.length() + " chars): " + 
                            (line.length() > 50 ? line.substring(0, 50) + "..." : line));
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Creates a horizontally scrollable component for wide code blocks.
     * Uses JScrollPane with JTextArea to provide native horizontal scrolling.
     * 
     * @param codeContent The code content to display
     * @return A JScrollPane containing the code with horizontal scrollbar
     */
    public static JScrollPane createScrollableCodeComponent(String codeContent) {
        LOG.info("Creating scrollable code component for content length: " + codeContent.length());
        
        // Create JTextArea for code content
        JTextArea codeArea = new JTextArea(codeContent);
        codeArea.setEditable(false);
        codeArea.setLineWrap(false); // Critical: no line wrapping for code
        codeArea.setWrapStyleWord(false);
        
        // Use monospace font for code
        Font codeFont = new Font(Font.MONOSPACED, Font.PLAIN, UIUtil.getLabelFont().getSize());
        codeArea.setFont(codeFont);
        
        // Apply theme-aware colors
        codeArea.setForeground(ThemeUtils.codeForeground()); // Theme-aware text color
        codeArea.setBackground(ThemeUtils.codeBackground());
        codeArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        
        // Calculate preferred size based on content
        FontMetrics fm = codeArea.getFontMetrics(codeFont);
        String[] lines = codeContent.split("\n");
        int maxLineWidth = 0;
        
        for (String line : lines) {
            int lineWidth = fm.stringWidth(line);
            maxLineWidth = Math.max(maxLineWidth, lineWidth);
        }
        
        // Add padding to width and height
        int contentWidth = maxLineWidth + 40; // Extra padding for scrollbar
        int contentHeight = lines.length * fm.getHeight() + 20;
        
        codeArea.setPreferredSize(new Dimension(contentWidth, contentHeight));
        
        // Create scroll pane with ONLY horizontal scrolling
        JScrollPane scrollPane = new JScrollPane(codeArea);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER); // NO vertical scrollbar
        
        // Dynamic width sizing - resize with container, height matches content
        int actualContentHeight = contentHeight + 10; // Small buffer for border
        
        scrollPane.setPreferredSize(new Dimension(0, actualContentHeight)); // Width 0 = use available space
        scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, actualContentHeight)); // Dynamic width, fixed height
        scrollPane.setMinimumSize(new Dimension(200, actualContentHeight)); // Reasonable minimum width
        
        // Enable proper alignment for container layout
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Forward mouse wheel events to parent when no vertical scrolling is needed
        // This allows parent chat container to handle scrolling when hovering over code blocks
        codeArea.addMouseWheelListener(e -> {
            // Check if we have vertical scrollbar disabled (which we do)
            if (scrollPane.getVerticalScrollBarPolicy() == JScrollPane.VERTICAL_SCROLLBAR_NEVER) {
                // Forward the event to parent container for chat scrolling
                Container parent = scrollPane.getParent();
                while (parent != null) {
                    if (parent instanceof JScrollPane) {
                        // Found a parent scroll pane - forward the event
                        parent.dispatchEvent(new MouseWheelEvent(
                            (Component) parent,
                            e.getID(),
                            e.getWhen(),
                            e.getModifiersEx(), // Updated from deprecated getModifiers() to getModifiersEx()
                            e.getX(),
                            e.getY(),
                            e.getXOnScreen(),
                            e.getYOnScreen(),
                            e.getClickCount(),
                            e.isPopupTrigger(),
                            e.getScrollType(),
                            e.getScrollAmount(),
                            e.getWheelRotation()
                        ));
                        break;
                    }
                    parent = parent.getParent();
                }
            }
            // If we have vertical scrolling enabled, let the default behavior handle it
        });
        
        // Style the scroll pane border
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeUtils.borderColor(), 1),
            BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));
        
        LOG.info("Created scrollable code component with dynamic width x " + actualContentHeight + 
                ", content: " + contentWidth + "x" + contentHeight + ", lines: " + lines.length);
        
        return scrollPane;
    }
}
