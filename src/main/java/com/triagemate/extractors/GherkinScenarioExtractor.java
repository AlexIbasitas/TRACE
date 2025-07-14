package com.triagemate.extractors;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.openapi.application.ApplicationManager;
import com.triagemate.models.GherkinScenarioInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Extracts Gherkin scenario information using IntelliJ's PSI.
 * 
 * <p>This class uses PSI-based analysis to find and parse Gherkin feature files
 * to extract scenario context, including:</p>
 * <ul>
 *   <li>Scenario name and description</li>
 *   <li>Given/When/Then steps</li>
 *   <li>Scenario outline examples</li>
 *   <li>Tags and metadata</li>
 *   <li>Feature file context</li>
 * </ul>
 * 
 * <p>Follows JetBrains best practices for PSI-based file parsing and thread safety.</p>
 */
public class GherkinScenarioExtractor {
    private static final Logger LOG = Logger.getInstance(GherkinScenarioExtractor.class);
    private final Project project;

    // Gherkin keywords for parsing
    private static final String[] GHERKIN_KEYWORDS = {
        "Given", "When", "Then", "And", "But"
    };

    /**
     * Data structure to track examples table for scenario outlines.
     * 
     * <p>This class encapsulates the parsing and formatting of Cucumber examples tables,
     * providing methods to format the table for display and find matching rows based on
     * failed step text.</p>
     */
    private static class ExamplesTable {
        private final List<String> headers;
        private final List<List<String>> rows;
        
        /**
         * Creates a new ExamplesTable with the specified headers and data rows.
         * 
         * @param headers The column headers for the examples table
         * @param rows The data rows containing example values
         */
        public ExamplesTable(List<String> headers, List<List<String>> rows) {
            this.headers = headers;
            this.rows = rows;
        }
        
        /**
         * Formats the examples table as a Gherkin string for inclusion in scenario text.
         * 
         * @return Formatted examples table as a string
         */
        public String getFormattedTable() {
            StringBuilder sb = new StringBuilder();
            sb.append("Examples:\n");
            sb.append("  | ").append(String.join(" | ", headers)).append(" |\n");
            
            for (List<String> row : rows) {
                sb.append("  | ").append(String.join(" | ", row)).append(" |\n");
            }
            
            return sb.toString();
        }
        
        /**
         * Finds the example row that matches the failed step text.
         * 
         * @param failedStepText The text of the failed step to match against
         * @return The matching row as a list of values, or null if no match found
         */
        public List<String> findMatchingRow(String failedStepText) {
            // Try to find which example row matches the failed step
            for (List<String> row : rows) {
                if (rowMatchesStep(row, headers, failedStepText)) {
                    return row;
                }
            }
            return null;
        }
        
        /**
         * Checks if a row contains values that match the failed step text.
         * 
         * @param row The data row to check
         * @param headers The column headers
         * @param failedStepText The failed step text to match against
         * @return true if the row contains matching values, false otherwise
         */
        private boolean rowMatchesStep(List<String> row, List<String> headers, String failedStepText) {
            // Check if any parameter in this row could match the failed step
            for (int i = 0; i < Math.min(headers.size(), row.size()); i++) {
                String value = row.get(i);
                if (failedStepText.contains(value)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Constructor for GherkinScenarioExtractor
     *
     * @param project The current IntelliJ project
     */
    public GherkinScenarioExtractor(Project project) {
        this.project = project;
    }

    /**
     * Extracts Gherkin scenario information for a failed step.
     * 
     * <p>This method searches for the scenario containing the failed step
     * and extracts comprehensive context information.</p>
     *
     * @param failedStepText The text of the failed step
     * @param scenarioName The name of the scenario (if known)
     * @return GherkinScenarioInfo containing scenario details, or null if not found
     */
    public GherkinScenarioInfo extractScenarioInfo(String failedStepText, String scenarioName) {
        if (failedStepText == null || failedStepText.trim().isEmpty()) {
            LOG.warn("Failed step text is null or empty");
            return null;
        }

        try {
            // Use read action for PSI operations (JetBrains best practice)
            return ApplicationManager.getApplication().<GherkinScenarioInfo>runReadAction(() -> {
                try {
                    return findScenarioByStep(failedStepText, scenarioName);
                } catch (Exception e) {
                    LOG.warn("Failed to extract scenario info for: " + failedStepText, e);
                    return null;
                }
            });
        } catch (Exception e) {
            LOG.warn("Failed to execute read action for scenario extraction", e);
            return null;
        }
    }

    /**
     * Finds the scenario containing the failed step by searching through all feature files.
     * 
     * <p>This method iterates through all .feature files in the project and attempts to
     * find a scenario that contains the specified failed step text.</p>
     * 
     * @param failedStepText The failed step text to search for
     * @param scenarioName The scenario name (optional, for more precise matching)
     * @return GherkinScenarioInfo if found, null otherwise
     */
    private GherkinScenarioInfo findScenarioByStep(String failedStepText, String scenarioName) {
        // Find all .feature files in the project
        List<PsiFile> featureFiles = findFeatureFiles();
        
        for (PsiFile featureFile : featureFiles) {
            GherkinScenarioInfo scenarioInfo = parseFeatureFile(featureFile, failedStepText, scenarioName);
            if (scenarioInfo != null) {
                return scenarioInfo;
            }
        }
        
        LOG.debug("No matching scenario found for step: " + failedStepText);
        return null;
    }

    /**
     * Finds all .feature files in the project by recursively searching the project structure.
     * 
     * <p>This method traverses the project directory structure to locate all files with
     * the .feature extension, which are Cucumber feature files.</p>
     * 
     * @return List of PSI files representing the found feature files
     */
    private List<PsiFile> findFeatureFiles() {
        List<PsiFile> featureFiles = new ArrayList<>();
        
        try {
            PsiManager psiManager = PsiManager.getInstance(project);
            com.intellij.openapi.vfs.VirtualFile[] roots = 
                com.intellij.openapi.project.ProjectUtil.guessProjectDir(project).getChildren();
            
            for (com.intellij.openapi.vfs.VirtualFile root : roots) {
                findFeatureFilesRecursively(root, featureFiles, psiManager);
            }
        } catch (Exception e) {
            LOG.warn("Failed to find feature files", e);
        }
        
        return featureFiles;
    }

    /**
     * Recursively finds .feature files in a directory and its subdirectories.
     * 
     * <p>This method performs a depth-first search through the directory structure,
     * identifying files with the .feature extension and converting them to PSI files
     * for further processing.</p>
     * 
     * @param directory The directory to search recursively
     * @param featureFiles List to accumulate found feature files
     * @param psiManager The PSI manager for file conversion
     */
    private void findFeatureFilesRecursively(com.intellij.openapi.vfs.VirtualFile directory, 
                                           List<PsiFile> featureFiles, 
                                           PsiManager psiManager) {
        if (directory == null || !directory.isDirectory()) {
            return;
        }
        
        for (com.intellij.openapi.vfs.VirtualFile child : directory.getChildren()) {
            if (child.isDirectory()) {
                findFeatureFilesRecursively(child, featureFiles, psiManager);
            } else if (child.getName().endsWith(".feature")) {
                PsiFile psiFile = psiManager.findFile(child);
                if (psiFile != null) {
                    featureFiles.add(psiFile);
                }
            }
        }
    }

    /**
     * Parses a feature file to find the scenario containing the failed step.
     * 
     * <p>This method performs comprehensive parsing of a Gherkin feature file, handling:</p>
     * <ul>
     *   <li>Background sections and their steps</li>
     *   <li>Regular scenarios and scenario outlines</li>
     *   <li>Examples tables for scenario outlines</li>
     *   <li>Data tables within scenarios</li>
     *   <li>Tags and metadata</li>
     * </ul>
     * 
     * <p>The parsing is done line-by-line, maintaining state to properly handle
     * nested structures and complex Gherkin syntax.</p>
     * 
     * @param featureFile The PSI file representing the feature file to parse
     * @param failedStepText The failed step text to search for within scenarios
     * @param scenarioName The scenario name for more precise matching (optional)
     * @return GherkinScenarioInfo if a matching scenario is found, null otherwise
     */
    private GherkinScenarioInfo parseFeatureFile(PsiFile featureFile, String failedStepText, String scenarioName) {
        try {
            String fileContent = featureFile.getText();
            String[] lines = fileContent.split("\n");
            
            String featureName = extractFeatureName(lines);
            List<String> backgroundSteps = new ArrayList<>();
            List<String> currentScenarioTags = new ArrayList<>();
            String currentScenario = null;
            List<String> currentSteps = new ArrayList<>();
            List<String> currentDataTable = new ArrayList<>();
            ExamplesTable currentExamplesTable = null;
            boolean inBackground = false;
            boolean inScenario = false;
            boolean inScenarioOutline = false;
            boolean inExamples = false;
            int scenarioStartLine = 0;
            
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();
                String originalLine = lines[i];
                
                // Handle Background section
                if (line.startsWith("Background:")) {
                    inBackground = true;
                    inScenario = false;
                    inScenarioOutline = false;
                    backgroundSteps.clear();
                    continue;
                }
                
                // Handle Scenario/Scenario Outline start
                if (line.startsWith("Scenario:") || line.startsWith("Scenario Outline:")) {
                    // Check if previous scenario contains our failed step
                    if (inScenario && currentScenario != null) {
                        if (scenarioContainsFailedStep(currentScenario, currentSteps, backgroundSteps, 
                                                      currentDataTable, currentExamplesTable, failedStepText, scenarioName, inScenarioOutline)) {
                            return createEnhancedScenarioInfo(featureFile, featureName, currentScenario, 
                                                            currentSteps, backgroundSteps, currentScenarioTags, 
                                                            currentDataTable, currentExamplesTable, scenarioStartLine, inScenarioOutline);
                        }
                    }
                    
                    // Start new scenario
                    inBackground = false;
                    inScenario = true;
                    inScenarioOutline = line.startsWith("Scenario Outline:");
                    inExamples = false;
                    currentScenario = extractScenarioName(line);
                    currentSteps.clear();
                    currentDataTable.clear();
                    currentScenarioTags.clear();
                    scenarioStartLine = i + 1;
                    continue;
                }
                
                // Handle Examples section in Scenario Outline
                if (line.startsWith("Examples:") && inScenarioOutline) {
                    inExamples = true;
                    currentExamplesTable = parseExamplesTable(lines, i);
                    continue;
                }
                
                // Handle data table rows (but not examples table rows)
                if (line.startsWith("|") && (inScenario || inBackground) && !inExamples) {
                    currentDataTable.add(originalLine);
                    continue;
                }
                
                // Handle step lines
                if (isStepLine(line) && (inScenario || inBackground)) {
                    if (inBackground) {
                        backgroundSteps.add(line);
                    } else if (inScenario) {
                        currentSteps.add(line);
                    }
                    continue;
                }
                
                // Handle scenario tags
                if (line.startsWith("@") && !inScenario && !inBackground) {
                    currentScenarioTags.addAll(extractTags(line));
                    continue;
                }
                
                // Handle empty lines and comments
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
            }
            
            // Check the last scenario
            if (inScenario && currentScenario != null) {
                if (scenarioContainsFailedStep(currentScenario, currentSteps, backgroundSteps, 
                                              currentDataTable, currentExamplesTable, failedStepText, scenarioName, inScenarioOutline)) {
                    return createEnhancedScenarioInfo(featureFile, featureName, currentScenario, 
                                                    currentSteps, backgroundSteps, currentScenarioTags, 
                                                    currentDataTable, currentExamplesTable, scenarioStartLine, inScenarioOutline);
                }
            }
            
        } catch (Exception e) {
            LOG.warn("Failed to parse feature file: " + featureFile.getName(), e);
        }
        
        return null;
    }

    /**
     * Parses the examples table from a scenario outline.
     * 
     * <p>This method extracts the table structure from a Cucumber examples section,
     * parsing both headers and data rows. It handles comments and empty lines
     * within the table structure.</p>
     * 
     * @param lines The lines of the feature file
     * @param examplesStartLine The line number where the "Examples:" keyword starts
     * @return ExamplesTable object containing the parsed headers and rows
     */
    private ExamplesTable parseExamplesTable(String[] lines, int examplesStartLine) {
        List<String> headers = new ArrayList<>();
        List<List<String>> rows = new ArrayList<>();
        
        try {
            // Find the header row
            int headerLine = examplesStartLine + 1;
            while (headerLine < lines.length && lines[headerLine].trim().isEmpty()) {
                headerLine++;
            }
            
            if (headerLine < lines.length && lines[headerLine].trim().startsWith("|")) {
                String headerRow = lines[headerLine].trim();
                headers = parseTableRow(headerRow);
                
                // Parse data rows
                for (int i = headerLine + 1; i < lines.length; i++) {
                    String line = lines[i].trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    if (!line.startsWith("|")) {
                        break; // End of table
                    }
                    
                    List<String> row = parseTableRow(line);
                    if (!row.isEmpty()) {
                        rows.add(row);
                    }
                }
            }
            
        } catch (Exception e) {
            LOG.warn("Failed to parse examples table", e);
        }
        
        return new ExamplesTable(headers, rows);
    }
    
    /**
     * Parses a single table row into a list of cell values.
     * 
     * <p>This method splits a table row string (e.g., "| header1 | header2 |") into
     * individual cell values, trimming whitespace and filtering out empty cells.</p>
     * 
     * @param tableRow The table row string to parse
     * @return List of cell values extracted from the row
     */
    private List<String> parseTableRow(String tableRow) {
        List<String> cells = new ArrayList<>();
        String[] parts = tableRow.split("\\|");
        
        for (String part : parts) {
            String cell = part.trim();
            if (!cell.isEmpty()) {
                cells.add(cell);
            }
        }
        
        return cells;
    }

    /**
     * Checks if a scenario contains the failed step by examining all its components.
     * 
     * <p>This method performs a comprehensive search through all parts of a scenario:</p>
     * <ul>
     *   <li>Background steps (if any)</li>
     *   <li>Scenario steps</li>
     *   <li>Data tables within the scenario</li>
     *   <li>Examples table rows (for scenario outlines)</li>
     * </ul>
     * 
     * <p>For scenario outlines, it uses flexible name matching since Cucumber generates
     * different names for example executions.</p>
     * 
     * @param scenarioName The name of the scenario being checked
     * @param scenarioSteps The list of steps in the scenario
     * @param backgroundSteps The list of background steps (if any)
     * @param dataTable The data table rows associated with the scenario
     * @param examplesTable The examples table for scenario outlines (if any)
     * @param failedStepText The failed step text to search for
     * @param requiredScenarioName The expected scenario name for matching (optional)
     * @param isScenarioOutline Whether this is a scenario outline
     * @return true if the failed step is found in the scenario, false otherwise
     */
    private boolean scenarioContainsFailedStep(String scenarioName, List<String> scenarioSteps, 
                                             List<String> backgroundSteps, List<String> dataTable,
                                             ExamplesTable examplesTable, String failedStepText, 
                                             String requiredScenarioName, boolean isScenarioOutline) {
        
        // Check if scenario name matches (if provided)
        if (requiredScenarioName != null && !requiredScenarioName.trim().isEmpty()) {
            // For scenario outlines, be more flexible with name matching
            if (!isScenarioOutline && !scenarioName.equals(requiredScenarioName.trim())) {
                return false;
            }
        }
        
        // Check background steps
        for (String backgroundStep : backgroundSteps) {
            if (stepMatches(backgroundStep, failedStepText)) {
                return true;
            }
        }
        
        // Check scenario steps
        for (String scenarioStep : scenarioSteps) {
            if (stepMatches(scenarioStep, failedStepText)) {
                return true;
            }
        }
        
        // Check data table steps
        if (!dataTable.isEmpty()) {
            for (String dataTableRow : dataTable) {
                if (dataTableRow.contains(failedStepText)) {
                    return true;
                }
            }
        }
        
        // Check examples table for scenario outlines
        if (examplesTable != null) {
            List<String> matchingRow = examplesTable.findMatchingRow(failedStepText);
            if (matchingRow != null) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Checks if a step matches the failed step text, handling both regular and parameterized steps.
     * 
     * <p>This method performs step matching by:</p>
     * <ul>
     *   <li>Removing Gherkin keywords (Given/When/Then/And/But) for comparison</li>
     *   <li>Performing direct string matching</li>
     *   <li>Handling parameterized steps for scenario outlines</li>
     * </ul>
     * 
     * @param step The step text to check against
     * @param failedStepText The failed step text to match
     * @return true if the step matches the failed step text, false otherwise
     */
    private boolean stepMatches(String step, String failedStepText) {
        String stepText = removeKeywordPrefix(step);
        String failedStepWithoutKeyword = removeKeywordPrefix(failedStepText);
        
        // Direct comparison
        boolean directMatch = stepText.equals(failedStepWithoutKeyword) || 
                            stepText.contains(failedStepWithoutKeyword) ||
                            step.equals(failedStepText) || 
                            step.contains(failedStepText);
        
        // Parameterized step matching for scenario outlines
        boolean parameterizedMatch = matchesParameterizedStep(stepText, failedStepText);
        
        return directMatch || parameterizedMatch;
    }
    
    /**
     * Creates an enhanced GherkinScenarioInfo with comprehensive scenario context.
     * 
     * <p>This method constructs a complete GherkinScenarioInfo object that includes:</p>
     * <ul>
     *   <li>Combined background and scenario steps</li>
     *   <li>Full scenario text with proper Gherkin formatting</li>
     *   <li>Examples table for scenario outlines</li>
     *   <li>Data tables and metadata</li>
     *   <li>File location and content information</li>
     * </ul>
     * 
     * @param featureFile The PSI file containing the scenario
     * @param featureName The name of the feature
     * @param scenarioName The name of the scenario
     * @param scenarioSteps The steps within the scenario
     * @param backgroundSteps The background steps (if any)
     * @param tags The tags associated with the scenario
     * @param dataTable The data table rows (if any)
     * @param examplesTable The examples table for scenario outlines (if any)
     * @param startLine The starting line number of the scenario
     * @param isScenarioOutline Whether this is a scenario outline
     * @return Enhanced GherkinScenarioInfo object with complete context
     */
    private GherkinScenarioInfo createEnhancedScenarioInfo(PsiFile featureFile, String featureName, 
                                                          String scenarioName, List<String> scenarioSteps,
                                                          List<String> backgroundSteps, List<String> tags,
                                                          List<String> dataTable, ExamplesTable examplesTable, 
                                                          int startLine, boolean isScenarioOutline) {
        // Combine background and scenario steps
        List<String> allSteps = new ArrayList<>();
        if (!backgroundSteps.isEmpty()) {
            allSteps.addAll(backgroundSteps);
        }
        allSteps.addAll(scenarioSteps);
        
        // Create full scenario text
        StringBuilder fullScenarioText = new StringBuilder();
        fullScenarioText.append("Feature: ").append(featureName).append("\n\n");
        
        if (!tags.isEmpty()) {
            fullScenarioText.append(String.join(" ", tags)).append("\n");
        }
        
        if (!backgroundSteps.isEmpty()) {
            fullScenarioText.append("Background:\n");
            for (String backgroundStep : backgroundSteps) {
                fullScenarioText.append("  ").append(backgroundStep).append("\n");
            }
            fullScenarioText.append("\n");
        }
        
        fullScenarioText.append(isScenarioOutline ? "Scenario Outline: " : "Scenario: ")
                       .append(scenarioName).append("\n");
        
        for (String step : scenarioSteps) {
            fullScenarioText.append("  ").append(step).append("\n");
        }
        
        if (!dataTable.isEmpty()) {
            fullScenarioText.append("\n");
            for (String dataTableRow : dataTable) {
                fullScenarioText.append("  ").append(dataTableRow).append("\n");
            }
        }
        
        // Add examples table for scenario outlines
        if (isScenarioOutline && examplesTable != null) {
            fullScenarioText.append("\n");
            fullScenarioText.append(examplesTable.getFormattedTable());
        }
        
        // Convert examples table to data table format for backward compatibility
        List<String> examplesDataTable = new ArrayList<>();
        if (examplesTable != null) {
            examplesDataTable.addAll(dataTable);
            examplesDataTable.add("");
            examplesDataTable.add(examplesTable.getFormattedTable());
        }
        
        return new GherkinScenarioInfo.Builder()
                .withFeatureName(featureName)
                .withScenarioName(scenarioName)
                .withSteps(allSteps)
                .withTags(tags)
                .withSourceFilePath(featureFile.getName())
                .withLineNumber(startLine)
                .withFeatureFileContent(featureFile.getText())
                .withFullScenarioText(fullScenarioText.toString())
                .withBackgroundSteps(backgroundSteps)
                .withDataTable(examplesDataTable.isEmpty() ? dataTable : examplesDataTable)
                .withIsScenarioOutline(isScenarioOutline)
                .build();
    }

    /**
     * Extracts the feature name from the feature file lines.
     * 
     * <p>This method searches for the "Feature:" keyword at the beginning of a line
     * and extracts the feature name that follows it.</p>
     * 
     * @param lines The lines of the feature file
     * @return The feature name, or "Unknown Feature" if not found
     */
    private String extractFeatureName(String[] lines) {
        for (String line : lines) {
            if (line.trim().startsWith("Feature:")) {
                return line.trim().substring("Feature:".length()).trim();
            }
        }
        return "Unknown Feature";
    }

    /**
     * Extracts Gherkin tags from a line.
     * 
     * <p>This method parses a line containing Gherkin tags (e.g., "@smoke @regression")
     * and extracts all tag names that start with the "@" symbol.</p>
     * 
     * @param line The line containing tags
     * @return List of tag names found in the line
     */
    private List<String> extractTags(String line) {
        List<String> tags = new ArrayList<>();
        String[] parts = line.split("\\s+");
        
        for (String part : parts) {
            if (part.startsWith("@")) {
                tags.add(part);
            }
        }
        
        return tags;
    }

    /**
     * Extracts the scenario name from a scenario or scenario outline line.
     * 
     * <p>This method handles both "Scenario:" and "Scenario Outline:" keywords
     * and extracts the scenario name that follows the keyword.</p>
     * 
     * @param line The scenario line to parse
     * @return The scenario name, or "Unknown Scenario" if not found
     */
    private String extractScenarioName(String line) {
        if (line.startsWith("Scenario:")) {
            return line.substring("Scenario:".length()).trim();
        } else if (line.startsWith("Scenario Outline:")) {
            return line.substring("Scenario Outline:".length()).trim();
        }
        return "Unknown Scenario";
    }

    /**
     * Checks if a line represents a Gherkin step.
     * 
     * <p>This method determines if a line starts with a Gherkin keyword
     * (Given, When, Then, And, But) followed by a space, indicating it's a step.</p>
     * 
     * @param line The line to check
     * @return true if the line is a step, false otherwise
     */
    private boolean isStepLine(String line) {
        for (String keyword : GHERKIN_KEYWORDS) {
            if (line.startsWith(keyword + " ")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a parameterized step matches a concrete step text using regex pattern matching.
     * 
     * <p>This method handles scenario outline parameter matching by converting parameterized
     * steps (e.g., "I enter \"<email>\" in the email field") into regex patterns that can
     * match concrete steps (e.g., "I enter \"valid@test.com\" in the email field").</p>
     * 
     * @param parameterizedStep The step with parameters (e.g., "I enter \"<email>\" in the email field")
     * @param concreteStep The concrete step text (e.g., "When I enter \"valid@test.com\" in the email field")
     * @return true if the parameterized step matches the concrete step, false otherwise
     */
    private boolean matchesParameterizedStep(String parameterizedStep, String concreteStep) {
        if (!parameterizedStep.contains("<") || !parameterizedStep.contains(">")) {
            return false;
        }
        
        String concreteStepWithoutKeyword = removeKeywordPrefix(concreteStep);
        
        // Create a regex pattern by replacing parameters with wildcards
        String regexPattern = parameterizedStep
            .replaceAll("<[^>]+>", "[^\"]*")
            .replaceAll("\"", "\\\"");
        
        try {
            Pattern pattern = Pattern.compile(regexPattern);
            Matcher matcher = pattern.matcher(concreteStepWithoutKeyword);
            return matcher.matches();
        } catch (Exception e) {
            LOG.debug("Failed to compile regex pattern for parameterized step matching", e);
            return false;
        }
    }

    /**
     * Removes the Gherkin keyword prefix from a step line.
     * 
     * <p>This method strips the leading keyword (Given, When, Then, And, But) from a step
     * to facilitate step matching without keyword interference.</p>
     * 
     * @param stepLine The step line that may contain a keyword prefix
     * @return The step text without the keyword prefix
     */
    private String removeKeywordPrefix(String stepLine) {
        for (String keyword : GHERKIN_KEYWORDS) {
            if (stepLine.startsWith(keyword + " ")) {
                return stepLine.substring(keyword.length() + 1).trim();
            }
        }
        return stepLine;
    }
} 