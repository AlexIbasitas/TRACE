package com.triagemate.extractors;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
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
        "Feature:", "Scenario:", "Scenario Outline:", "Given", "When", "Then", "And", "But"
    };

    /**
     * Constructor for GherkinScenarioExtractor
     *
     * @param project The current IntelliJ project
     */
    public GherkinScenarioExtractor(Project project) {
        this.project = project;
    }

    /**
     * Gets the project instance.
     * Protected access for subclasses.
     *
     * @return The project instance
     */
    protected Project getProject() {
        return project;
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
     * Finds the scenario containing the failed step.
     * 
     * @param failedStepText The failed step text to search for
     * @param scenarioName The scenario name (optional)
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
     * Finds all .feature files in the project.
     * 
     * @return List of feature files
     */
    protected List<PsiFile> findFeatureFiles() {
        List<PsiFile> featureFiles = new ArrayList<>();
        
        try {
            // Search for .feature files in the project
            GlobalSearchScope scope = GlobalSearchScope.projectScope(project);
            PsiManager psiManager = PsiManager.getInstance(project);
            
            // Use VirtualFileManager to find .feature files
            com.intellij.openapi.vfs.VirtualFileManager vfm = com.intellij.openapi.vfs.VirtualFileManager.getInstance();
            com.intellij.openapi.vfs.VirtualFile[] roots = com.intellij.openapi.project.ProjectUtil.guessProjectDir(project).getChildren();
            
            for (com.intellij.openapi.vfs.VirtualFile root : roots) {
                findFeatureFilesRecursively(root, featureFiles, psiManager);
            }
            
        } catch (Exception e) {
            LOG.warn("Failed to find feature files", e);
        }
        
        return featureFiles;
    }

    /**
     * Recursively finds .feature files in a directory.
     * 
     * @param directory The directory to search
     * @param featureFiles List to add found feature files to
     * @param psiManager The PSI manager
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
     * @param featureFile The feature file to parse
     * @param failedStepText The failed step text to search for
     * @param scenarioName The scenario name (optional)
     * @return GherkinScenarioInfo if found, null otherwise
     */
    private GherkinScenarioInfo parseFeatureFile(PsiFile featureFile, String failedStepText, String scenarioName) {
        try {
            String fileContent = featureFile.getText();
            String[] lines = fileContent.split("\n");
            
            String featureName = extractFeatureName(lines);
            List<String> tags = new ArrayList<>();
            String currentScenario = null;
            List<String> currentSteps = new ArrayList<>();
            boolean inScenario = false;
            int scenarioStartLine = 0;
            
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();
                
                // Extract tags
                if (line.startsWith("@")) {
                    tags.addAll(extractTags(line));
                }
                
                // Check for scenario start
                if (line.startsWith("Scenario:") || line.startsWith("Scenario Outline:")) {
                    // If we were in a scenario, check if it contains our failed step
                    if (inScenario && currentScenario != null) {
                        System.out.println("[DEBUG] Checking scenario: " + currentScenario);
                        System.out.println("[DEBUG] Steps: " + currentSteps);
                        System.out.println("[DEBUG] Failed step text: " + failedStepText);
                        System.out.println("[DEBUG] Required scenario name: " + scenarioName);
                        
                        // Check if scenario name matches (if provided)
                        boolean scenarioNameMatches = scenarioName == null || scenarioName.trim().isEmpty() || 
                                                    currentScenario.equals(scenarioName.trim());
                        
                        if (scenarioNameMatches && containsStep(currentSteps, failedStepText)) {
                            return createScenarioInfo(featureFile, featureName, currentScenario, 
                                                   currentSteps, tags, scenarioStartLine);
                        }
                    }
                    // Start new scenario
                    currentScenario = extractScenarioName(line);
                    currentSteps.clear();
                    inScenario = true;
                    scenarioStartLine = i + 1; // steps start after scenario line
                    continue; // do not add scenario line as a step
                }
                
                // Collect steps
                if (inScenario && isStepLine(line)) {
                    // Explicitly skip 'Examples:' and table rows
                    if (!line.startsWith("Examples:") && !line.startsWith("|")) {
                        currentSteps.add(line);
                    }
                }
            }
            
            // Check the last scenario
            if (inScenario && currentScenario != null) {
                System.out.println("[DEBUG] Checking scenario: " + currentScenario);
                System.out.println("[DEBUG] Steps: " + currentSteps);
                System.out.println("[DEBUG] Failed step text: " + failedStepText);
                System.out.println("[DEBUG] Required scenario name: " + scenarioName);
                
                // Check if scenario name matches (if provided)
                boolean scenarioNameMatches = scenarioName == null || scenarioName.trim().isEmpty() || 
                                            currentScenario.equals(scenarioName.trim());
                
                if (scenarioNameMatches && containsStep(currentSteps, failedStepText)) {
                    return createScenarioInfo(featureFile, featureName, currentScenario, 
                                           currentSteps, tags, scenarioStartLine);
                }
            }
            
        } catch (Exception e) {
            LOG.warn("Failed to parse feature file: " + featureFile.getName(), e);
        }
        
        return null;
    }

    /**
     * Extracts the feature name from the feature file.
     * 
     * @param lines The lines of the feature file
     * @return The feature name
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
     * Extracts tags from a line.
     * 
     * @param line The line containing tags
     * @return List of tags
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
     * Extracts the scenario name from a scenario line.
     * 
     * @param line The scenario line
     * @return The scenario name
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
     * Checks if a line is a step line.
     * 
     * @param line The line to check
     * @return true if it's a step line, false otherwise
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
     * Checks if a list of steps contains the failed step.
     * 
     * @param steps The list of steps
     * @param failedStepText The failed step text
     * @return true if the step is found, false otherwise
     */
    private boolean containsStep(List<String> steps, String failedStepText) {
        for (String step : steps) {
            // Remove the keyword prefix for comparison
            String stepText = removeKeywordPrefix(step);
            
            // Direct comparison
            boolean directMatch = stepText.equals(failedStepText) || stepText.contains(failedStepText)
                || step.equals(failedStepText) || step.contains(failedStepText);
            
            // Parameterized step matching for scenario outlines
            boolean parameterizedMatch = matchesParameterizedStep(stepText, failedStepText);
            
            boolean match = directMatch || parameterizedMatch;
            System.out.println("[DEBUG] Comparing step: [" + stepText + "] and [" + step + "] to failedStepText: [" + failedStepText + "] => " + match);
            if (match) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a parameterized step matches a concrete step text.
     * This handles scenario outline parameter matching.
     * 
     * @param parameterizedStep The step with parameters (e.g., "I enter \"<email>\" in the email field")
     * @param concreteStep The concrete step text (e.g., "When I enter \"valid@test.com\" in the email field")
     * @return true if they match, false otherwise
     */
    private boolean matchesParameterizedStep(String parameterizedStep, String concreteStep) {
        // If the parameterized step doesn't contain parameters, use direct comparison
        if (!parameterizedStep.contains("<") || !parameterizedStep.contains(">")) {
            return false;
        }
        
        // Remove keyword prefix from concrete step for comparison
        String concreteStepWithoutKeyword = removeKeywordPrefix(concreteStep);
        
        // Create a regex pattern by replacing parameters with wildcards
        String regexPattern = parameterizedStep
            .replaceAll("<[^>]+>", "[^\"]*")  // Replace <param> with [^"]* to match any non-quote content
            .replaceAll("\"", "\\\"");        // Escape quotes for regex
        
        System.out.println("[DEBUG] Parameterized step: [" + parameterizedStep + "]");
        System.out.println("[DEBUG] Concrete step (with keyword): [" + concreteStep + "]");
        System.out.println("[DEBUG] Concrete step (without keyword): [" + concreteStepWithoutKeyword + "]");
        System.out.println("[DEBUG] Regex pattern: [" + regexPattern + "]");
        
        try {
            Pattern pattern = Pattern.compile(regexPattern);
            Matcher matcher = pattern.matcher(concreteStepWithoutKeyword);
            boolean matches = matcher.matches();
            System.out.println("[DEBUG] Parameterized match result: " + matches);
            return matches;
        } catch (Exception e) {
            // If regex compilation fails, fall back to simple string comparison
            LOG.debug("Failed to compile regex pattern for parameterized step matching", e);
            System.out.println("[DEBUG] Regex compilation failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Removes the keyword prefix from a step line.
     * 
     * @param stepLine The step line
     * @return The step text without keyword
     */
    private String removeKeywordPrefix(String stepLine) {
        for (String keyword : GHERKIN_KEYWORDS) {
            if (stepLine.startsWith(keyword + " ")) {
                return stepLine.substring(keyword.length() + 1).trim();
            }
        }
        return stepLine;
    }

    /**
     * Creates a GherkinScenarioInfo object from parsed data.
     * 
     * @param featureFile The feature file
     * @param featureName The feature name
     * @param scenarioName The scenario name
     * @param steps The scenario steps
     * @param tags The scenario tags
     * @param startLine The starting line number
     * @return GherkinScenarioInfo object
     */
    private GherkinScenarioInfo createScenarioInfo(PsiFile featureFile, String featureName, 
                                                  String scenarioName, List<String> steps, 
                                                  List<String> tags, int startLine) {
        return new GherkinScenarioInfo.Builder()
                .withFeatureName(featureName)
                .withScenarioName(scenarioName)
                .withSteps(steps)
                .withTags(tags)
                .withSourceFilePath(featureFile.getName())
                .withLineNumber(startLine)
                .withFeatureFileContent(featureFile.getText())
                .build();
    }
} 