package com.trace.ai.services;

import com.intellij.openapi.diagnostic.Logger;
import com.trace.ai.models.DocumentEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for parsing markdown documents into DocumentEntry objects using regex.
 * 
 * <p>This service uses simple regex patterns to extract structured data from
 * test failure documentation. It follows IntelliJ plugin best practices and
 * provides comprehensive error handling.</p>
 * 
 * <p>Supported markdown features:</p>
 * <ul>
 *   <li>Headers for title extraction</li>
 *   <li>Lists for root causes and resolution steps</li>
 *   <li>Metadata fields (Category, Tags, Summary)</li>
 *   <li>Content extraction and processing</li>
 * </ul>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
public class DocumentParserService {
    
    private static final Logger LOG = Logger.getInstance(DocumentParserService.class);
    
    // Regex patterns for extracting metadata from the new format
    private static final Pattern TITLE_PATTERN = Pattern.compile("^### Title:\\s*(.+)$", Pattern.MULTILINE);
    private static final Pattern SUMMARY_PATTERN = Pattern.compile("\\*\\*Summary\\*\\*:\\s*(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ROOT_CAUSES_PATTERN = Pattern.compile("(?s)\\*\\*Root Causes\\*\\*:\\s*\\n((?:[-*]\\s*.+\\n?)+?)(?=\\*\\*Resolution Steps|\\*\\*Solution|$)", Pattern.CASE_INSENSITIVE);
    private static final Pattern RESOLUTION_PATTERN = Pattern.compile("(?s)\\*\\*Resolution Steps\\*\\*:\\s*\\n((?:[0-9]+\\.\\s*.+\\n?)+?)(?=\\*\\*|$)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SOLUTION_PATTERN = Pattern.compile("(?s)\\*\\*Solution\\*\\*:\\s*\\n((?:[0-9]+\\.\\s*.+\\n?)+?)(?=\\*\\*|$)", Pattern.CASE_INSENSITIVE);
    
    public DocumentParserService() {
        // Simple constructor - no external dependencies
    }
    
    /**
     * Parses a markdown file into a DocumentEntry object.
     * 
     * @param file the markdown file to parse
     * @return the parsed DocumentEntry, or null if parsing fails
     */
    @Nullable
    public DocumentEntry parseDocument(@NotNull File file) {
        try {
            LOG.info("Parsing document: " + file.getName());
            
            String content = Files.readString(file.toPath());
            return parseDocumentContent(content, file.getName());
            
        } catch (IOException e) {
            LOG.warn("Failed to read file: " + file.getName(), e);
            return null;
        } catch (Exception e) {
            LOG.warn("Failed to parse document: " + file.getName(), e);
            return null;
        }
    }
    
    /**
     * Parses multiple markdown files in a directory.
     * 
     * @param directory the directory containing markdown files
     * @return list of parsed DocumentEntry objects
     */
    @NotNull
    public List<DocumentEntry> parseDocumentsFromDirectory(@NotNull File directory) {
        List<DocumentEntry> documents = new ArrayList<>();
        
        if (!directory.exists() || !directory.isDirectory()) {
            LOG.warn("Directory does not exist or is not a directory: " + directory.getPath());
            return documents;
        }
        
        File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".md"));
        
        if (files == null) {
            LOG.warn("No markdown files found in directory: " + directory.getPath());
            return documents;
        }
        
        LOG.info("Found " + files.length + " markdown files to parse");
        
        for (File file : files) {
            List<DocumentEntry> fileDocuments = parseDocumentWithMultipleEntries(file);
            documents.addAll(fileDocuments);
            LOG.info("Successfully parsed " + fileDocuments.size() + " documents from: " + file.getName());
        }
        
        LOG.info("Parsed " + documents.size() + " documents successfully");
        return documents;
    }
    
    /**
     * Parses a markdown file that may contain multiple document entries separated by "---".
     * 
     * @param file the markdown file to parse
     * @return list of parsed DocumentEntry objects
     */
    @NotNull
    public List<DocumentEntry> parseDocumentWithMultipleEntries(@NotNull File file) {
        List<DocumentEntry> documents = new ArrayList<>();
        
        try {
            String content = Files.readString(file.toPath());
            String[] sections = content.split("\\n\\s*---\\s*\\n");
            
            LOG.info("Found " + sections.length + " document sections in: " + file.getName());
            
            for (int i = 0; i < sections.length; i++) {
                String section = sections[i].trim();
                if (!section.isEmpty()) {
                    DocumentEntry document = parseDocumentContent(section, file.getName() + "_" + (i + 1));
                    if (document != null && validateDocument(document)) {
                        documents.add(document);
                        LOG.info("Successfully parsed section " + (i + 1) + " - Title: " + document.getTitle());
                    } else {
                        LOG.warn("Failed to parse section " + (i + 1) + " in: " + file.getName());
                    }
                }
            }
            
        } catch (IOException e) {
            LOG.warn("Failed to read file: " + file.getName(), e);
        } catch (Exception e) {
            LOG.warn("Failed to parse document: " + file.getName(), e);
        }
        
        return documents;
    }
    
    /**
     * Parses document content into a DocumentEntry object.
     * 
     * @param content the markdown content to parse
     * @param fileName the original file name
     * @return the parsed DocumentEntry
     */
    @NotNull
    private DocumentEntry parseDocumentContent(@NotNull String content, @NotNull String fileName) {
        // Extract metadata using regex patterns
        String title = extractTitle(content);
        String summary = extractSummary(content);
        String rootCauses = extractRootCauses(content);
        String resolutionSteps = extractResolutionSteps(content);
        
        // Extract category from filename
        String category = extractCategoryFromFilename(fileName);
        
        // Use filename as fallback for title
        if (title == null || title.trim().isEmpty()) {
            title = fileName.replace(".md", "");
        }
        
        // Clean up content (remove metadata fields for embedding)
        String cleanContent = cleanContentForEmbedding(content);
        
        LOG.info("Parsed document - Title: " + title + ", Category: " + category);
        
        return new DocumentEntry(category, title, cleanContent, summary, rootCauses, resolutionSteps, null);
    }
    
    /**
     * Extracts the title from the ### Title: pattern.
     */
    @Nullable
    private String extractTitle(@NotNull String content) {
        Matcher matcher = TITLE_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }
    
    /**
     * Extracts category from filename.
     */
    @NotNull
    private String extractCategoryFromFilename(@NotNull String fileName) {
        // Extract category from filename (e.g., "selenium.md" -> "selenium")
        String name = fileName.replace(".md", "");
        return name.toLowerCase();
    }
    
    /**
     * Extracts summary from **Summary**: pattern.
     */
    @Nullable
    private String extractSummary(@NotNull String content) {
        Matcher matcher = SUMMARY_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }
    
    /**
     * Extracts root causes from **Root Causes**: pattern.
     */
    @Nullable
    private String extractRootCauses(@NotNull String content) {
        // Find the start of Root Causes section
        int startIndex = content.indexOf("**Root Causes**:");
        if (startIndex == -1) {
            return null;
        }
        
        // Find the end of Root Causes section (next ** section or end of content)
        int endIndex = content.length();
        String[] sections = {"**Resolution Steps**:", "**Solution**:", "**Summary**:", "### Title:"};
        
        for (String section : sections) {
            int sectionIndex = content.indexOf(section, startIndex + 1);
            if (sectionIndex != -1 && sectionIndex < endIndex) {
                endIndex = sectionIndex;
            }
        }
        
        // Extract the content between Root Causes and the next section
        String rootCausesSection = content.substring(startIndex, endIndex);
        
        // Extract only the list items (lines starting with - or *)
        String[] lines = rootCausesSection.split("\n");
        List<String> rootCauses = new ArrayList<>();
        
        boolean inRootCauses = false;
        for (String line : lines) {
            if (line.contains("**Root Causes**:") || line.contains("**Root Causes**:")) {
                inRootCauses = true;
                continue;
            }
            if (inRootCauses && (line.trim().startsWith("-") || line.trim().startsWith("*"))) {
                rootCauses.add(line.trim());
            } else if (inRootCauses && !line.trim().isEmpty() && !line.trim().startsWith("-") && !line.trim().startsWith("*")) {
                // Stop when we hit non-list content
                break;
            }
        }
        
        // Clean the list items (remove bullet points)
        return cleanListItems(String.join("\n", rootCauses));
    }
    
    /**
     * Extracts resolution steps from **Resolution Steps**: pattern.
     */
    @Nullable
    private String extractResolutionSteps(@NotNull String content) {
        // Find the start of Resolution Steps section
        int startIndex = content.indexOf("**Resolution Steps**:");
        if (startIndex == -1) {
            // Try Solution as fallback
            startIndex = content.indexOf("**Solution**:");
            if (startIndex == -1) {
                return null;
            }
        }
        
        // Find the end of Resolution Steps section (next ** section or end of content)
        int endIndex = content.length();
        String[] sections = {"**Root Causes**:", "**Summary**:", "### Title:"};
        
        for (String section : sections) {
            int sectionIndex = content.indexOf(section, startIndex + 1);
            if (sectionIndex != -1 && sectionIndex < endIndex) {
                endIndex = sectionIndex;
            }
        }
        
        // Extract the content between Resolution Steps and the next section
        String resolutionSection = content.substring(startIndex, endIndex);
        
        // Extract only the numbered list items (lines starting with 1., 2., etc.)
        String[] lines = resolutionSection.split("\n");
        List<String> resolutionSteps = new ArrayList<>();
        
        boolean inResolutionSteps = false;
        for (String line : lines) {
            if (line.contains("**Resolution Steps**:") || line.contains("**Solution**:") || 
                line.contains("**Resolution Steps**") || line.contains("**Solution**")) {
                inResolutionSteps = true;
                continue;
            }
            if (inResolutionSteps && line.trim().matches("^[0-9]+\\..*")) {
                resolutionSteps.add(line.trim());
            } else if (inResolutionSteps && !line.trim().isEmpty() && !line.trim().matches("^[0-9]+\\..*")) {
                // Stop when we hit non-numbered content
                break;
            }
        }
        
        // Clean the list items (remove numbering)
        return cleanListItems(String.join("\n", resolutionSteps));
    }
    
    /**
     * Cleans list items by removing bullet points and extra whitespace.
     */
    @NotNull
    private String cleanListItems(@NotNull String listText) {
        String[] lines = listText.split("\n");
        List<String> cleanedItems = new ArrayList<>();
        
        for (String line : lines) {
            // Remove bullet points (-, *) or numbered items (1., 2., etc.)
            String cleaned = line.replaceAll("^[-*]\\s*", "").replaceAll("^[0-9]+\\.\\s*", "").trim();
            if (!cleaned.isEmpty()) {
                cleanedItems.add(cleaned);
            }
        }
        
        return String.join("\n", cleanedItems);
    }
    
    /**
     * Cleans content for embedding by removing metadata fields.
     */
    @NotNull
    private String cleanContentForEmbedding(@NotNull String content) {
        // Keep the original content for embedding - don't remove metadata
        // This ensures we have content for embedding while keeping metadata separate
        return content.trim();
    }
    
    /**
     * Validates that a parsed document has the required fields.
     * 
     * @param document the document to validate
     * @return true if the document is valid
     */
    public boolean validateDocument(@NotNull DocumentEntry document) {
        if (document.getTitle() == null || document.getTitle().trim().isEmpty()) {
            LOG.warn("Document missing title");
            return false;
        }
        
        if (document.getContent() == null || document.getContent().trim().isEmpty()) {
            LOG.warn("Document missing content");
            return false;
        }
        
        if (document.getCategory() == null || document.getCategory().trim().isEmpty()) {
            LOG.warn("Document missing category");
            return false;
        }
        
        return true;
    }
} 