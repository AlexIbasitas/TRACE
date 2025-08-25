package com.trace.test.extractors;

import com.intellij.execution.testframework.sm.runner.SMTestProxy;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.PsiDocumentManager;
import com.trace.test.models.FailureInfo;

/**
 * Extracts failure information and step definitions using PSI-based analysis.
 * 
 * <p>This class uses IntelliJ's PSI (Program Structure Interface) to navigate to the exact
 * location of test failures and extract step definitions reliably, following JetBrains best practices.
 * It provides comprehensive error information capture and direct file navigation to failure locations.</p>
 * 
 * <p>The extractor uses PSI-based step text extraction for reliability and provides
 * comprehensive error information for test failure analysis.</p>
 * 
 * @author Alex Ibasitas
 * @since 1.0.0
 */
public class StackTraceExtractor {
    private static final Logger LOG = Logger.getInstance(StackTraceExtractor.class);
    private final Project project;

    // Cucumber annotation keywords for step definition detection
    private static final String[] CUCUMBER_ANNOTATIONS = {
        "@Given", "@When", "@Then", "@Step"
    };

    // Gherkin keywords for feature file parsing
    private static final String[] GHERKIN_KEYWORDS = {
        "Given ", "When ", "Then ", "And ", "But "
    };

    /**
     * Constructor for StackTraceExtractor.
     * 
     * @param project The IntelliJ project context for PSI operations
     * @throws NullPointerException if project is null
     */
    public StackTraceExtractor(Project project) {
        if (project == null) {
            throw new NullPointerException("Project cannot be null");
        }
        this.project = project;
    }

    /**
     * Extracts comprehensive failure information from a test proxy using PSI-based analysis.
     * This is the main entry point used by the CucumberTestExecutionListener.
     * 
     * @param test The test proxy to extract failure information from
     * @return FailureInfo containing extracted failure details
     * @throws IllegalArgumentException if test is null
     */
    public FailureInfo extractFailureInfo(SMTestProxy test) {
        if (test == null) {
            throw new IllegalArgumentException("Test cannot be null");
        }
        
        LOG.debug("Starting PSI-based extraction for test: " + test.getName());
        
        try {
            // Extract step text using PSI navigation
            String failedStepText = extractFailedStepText(test);
            
            // Get comprehensive error information
            String stackTrace = test.getStacktrace();
            String errorMessage = test.getErrorMessage();
            
            // Extract structured information from stack trace
            String expectedValue = extractExpectedValue(stackTrace);
            String actualValue = extractActualValue(stackTrace);
            
            // Get source file and line number from test location
            String sourceFilePath = getSourceFilePathFromTest(test);
            int lineNumber = getLineNumberFromTest(test);
            
            // Extract proper scenario name
            String scenarioName = extractScenarioName(test, failedStepText);
            
            // Build comprehensive failure info
            return new FailureInfo.Builder()
                    .withScenarioName(scenarioName)
                    .withFailedStepText(failedStepText)
                    .withErrorMessage(errorMessage != null ? errorMessage : "Test failed")
                    .withExpectedValue(expectedValue)
                    .withActualValue(actualValue)
                    .withStackTrace(stackTrace != null ? stackTrace : "Stack trace not available")
                    .withSourceFilePath(sourceFilePath)
                    .withLineNumber(lineNumber)
                    .withParsingTime(System.currentTimeMillis())
                    .build();
                    
        } catch (Exception e) {
            LOG.warn("Error during failure extraction for test: " + test.getName(), e);
            return createMinimalFailureInfo(test);
        }
    }

    /**
     * Extracts the text of the failed step using PSI navigation to the actual failure location.
     * This is the core of the reliable extraction approach.
     *
     * @param test The failed test proxy
     * @return The text of the failed step, or null if not found
     */
    public String extractFailedStepText(SMTestProxy test) {
        if (test == null) {
            LOG.warn("Test proxy is null");
            return null;
        }
        
        try {
            // Get the source file and line number from the test location
            String sourceFilePath = getSourceFilePathFromTest(test);
            int lineNumber = getLineNumberFromTest(test);
            
            if (sourceFilePath == null || lineNumber <= 0) {
                LOG.debug("Could not determine source file or line number for test: " + test.getName());
                return fallbackToTestName(test);
            }
            
            // Use PSI to navigate to the exact line and extract step text
            String stepText = extractStepTextFromPSI(sourceFilePath, lineNumber);
            
            if (stepText != null) {
                return stepText;
            }
            
            LOG.debug("PSI extraction failed, falling back to test name for: " + test.getName());
            return fallbackToTestName(test);
            
        } catch (Exception e) {
            LOG.warn("Error during PSI extraction for test: " + test.getName(), e);
            return fallbackToTestName(test);
        }
    }
    
    /**
     * Gets the source file path from the test proxy using IntelliJ's location system.
     * 
     * @param test The test proxy to extract location from
     * @return The source file path, or null if not found
     */
    private String getSourceFilePathFromTest(SMTestProxy test) {
        try {
            // Try to get location from test proxy
            if (test.getLocationUrl() != null) {
                String locationUrl = test.getLocationUrl();
                
                // Handle file:// URLs
                if (locationUrl.startsWith("file://")) {
                    return locationUrl.substring(7); // Remove "file://" prefix
                }
                
                // Handle other URL formats
                if (locationUrl.contains(":")) {
                    String[] parts = locationUrl.split(":");
                    if (parts.length >= 2) {
                        return parts[0] + ":" + parts[1];
                    }
                }
            }
            
            // Fallback: try to get from stack trace
            String stackTrace = test.getStacktrace();
            if (stackTrace != null) {
                return extractSourceFileFromStackTrace(stackTrace);
            }
            
        } catch (Exception e) {
            LOG.debug("Error getting source file path from test", e);
        }
        
        return null;
    }
    
    /**
     * Gets the line number from the test proxy.
     * 
     * @param test The test proxy to extract line number from
     * @return The line number, or -1 if not found
     */
    private int getLineNumberFromTest(SMTestProxy test) {
        try {
            // Try to get from location URL first
            if (test.getLocationUrl() != null) {
                String locationUrl = test.getLocationUrl();
                if (locationUrl.contains(":")) {
                    String[] parts = locationUrl.split(":");
                    if (parts.length >= 3) {
                        try {
                            return Integer.parseInt(parts[2]);
                        } catch (NumberFormatException e) {
                            LOG.debug("Could not parse line number from location URL", e);
                        }
                    }
                }
            }
            
            // Fallback: try to get from stack trace
            String stackTrace = test.getStacktrace();
            if (stackTrace != null) {
                return extractLineNumberFromStackTrace(stackTrace);
            }
            
        } catch (Exception e) {
            LOG.debug("Error getting line number from test", e);
        }
        
        return -1;
    }
    
    /**
     * Uses PSI to navigate to the exact line and extract step text.
     * This is the core of the reliable extraction approach.
     * 
     * @param sourceFilePath The path to the source file
     * @param lineNumber The line number to navigate to
     * @return The extracted step text, or null if not found
     */
    private String extractStepTextFromPSI(String sourceFilePath, int lineNumber) {
        try {
            // Get the virtual file
            VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath(sourceFilePath);
            if (virtualFile == null) {
                LOG.debug("Virtual file not found: " + sourceFilePath);
                return null;
            }
            
            // Get PSI file
            PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
            if (psiFile == null) {
                LOG.debug("PSI file not found for: " + sourceFilePath);
                return null;
            }
            
            // Navigate to the specific line
            Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
            if (document == null) {
                LOG.debug("Document not found for PSI file");
                return null;
            }
            
            // Get the line start offset
            int lineStartOffset = document.getLineStartOffset(lineNumber - 1); // Convert to 0-based
            int lineEndOffset = document.getLineEndOffset(lineNumber - 1);
            
            // Get the element at that position
            PsiElement element = psiFile.findElementAt(lineStartOffset);
            if (element == null) {
                LOG.debug("No PSI element found at line " + lineNumber);
                return null;
            }
            
            // Extract step text based on the type of element
            String stepText = extractStepTextFromElement(element, document, lineStartOffset, lineEndOffset);
            
            if (stepText != null) {
                return stepText;
            }
            
            // If direct extraction failed, try to find the step definition method
            return findStepDefinitionMethod(element);
            
        } catch (Exception e) {
            LOG.warn("Error during PSI navigation for file: " + sourceFilePath, e);
            return null;
        }
    }
    
    /**
     * Extracts step text from a PSI element at the failure location.
     * 
     * @param element The PSI element at the failure location
     * @param document The document containing the element
     * @param lineStartOffset The start offset of the line
     * @param lineEndOffset The end offset of the line
     * @return The extracted step text, or null if not found
     */
    private String extractStepTextFromElement(PsiElement element, Document document, 
                                            int lineStartOffset, int lineEndOffset) {
        try {
            // Get the text of the current line
            String lineText = document.getText(new TextRange(lineStartOffset, lineEndOffset)).trim();
            
            // Look for Cucumber step annotations
            if (element instanceof PsiMethod) {
                return extractStepTextFromMethod((PsiMethod) element);
            }
            
            // Look for step annotations in the parent method
            PsiMethod parentMethod = PsiTreeUtil.getParentOfType(element, PsiMethod.class);
            if (parentMethod != null) {
                return extractStepTextFromMethod(parentMethod);
            }
            
            // Look for step annotations in the same class
            PsiClass parentClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
            if (parentClass != null) {
                return extractStepTextFromClass(parentClass, lineText);
            }
            
            // If it's a feature file, extract the step text directly
            if (element.getContainingFile().getName().endsWith(".feature")) {
                return extractStepTextFromFeatureFile(lineText);
            }
            
        } catch (Exception e) {
            LOG.debug("Error extracting step text from element", e);
        }
        
        return null;
    }
    
    /**
     * Extracts step text from a method with Cucumber annotations.
     * 
     * @param method The method to extract step text from
     * @return The extracted step text, or null if not found
     */
    private String extractStepTextFromMethod(PsiMethod method) {
        try {
            // Look for Cucumber step annotations
            PsiAnnotation[] annotations = method.getModifierList().getAnnotations();
            for (PsiAnnotation annotation : annotations) {
                String annotationText = annotation.getText();
                if (containsCucumberAnnotation(annotationText)) {
                    String stepText = extractStepTextFromAnnotation(annotation);
                    if (stepText != null) {
                        return stepText;
                    }
                }
            }
            
        } catch (Exception e) {
            LOG.debug("Error extracting step text from method", e);
        }
        
        return null;
    }
    
    /**
     * Checks if an annotation text contains a Cucumber step annotation.
     * 
     * @param annotationText The annotation text to check
     * @return true if it contains a Cucumber annotation, false otherwise
     */
    private boolean containsCucumberAnnotation(String annotationText) {
        for (String annotation : CUCUMBER_ANNOTATIONS) {
            if (annotationText.contains(annotation)) {
                return true;
            }
        }
        return false;
    }
        
    /**
     * Extracts step text from a Cucumber annotation.
     * 
     * @param annotation The annotation to extract step text from
     * @return The extracted step text, or null if not found
     */
    private String extractStepTextFromAnnotation(PsiAnnotation annotation) {
        try {
            String annotationText = annotation.getText();
            
            // Look for quoted strings in the annotation
            int startQuote = annotationText.indexOf("\"");
            if (startQuote > 0) {
                int endQuote = annotationText.indexOf("\"", startQuote + 1);
                if (endQuote > startQuote) {
                    return annotationText.substring(startQuote + 1, endQuote);
                }
            }
            
        } catch (Exception e) {
            LOG.debug("Error extracting step text from annotation", e);
        }
        
        return null;
    }
        
    /**
     * Extracts step text from a feature file line.
     * 
     * @param lineText The line text from the feature file
     * @return The extracted step text, or null if not found
     */
    private String extractStepTextFromFeatureFile(String lineText) {
        try {
            // Remove leading Gherkin keywords
            for (String keyword : GHERKIN_KEYWORDS) {
                if (lineText.startsWith(keyword)) {
                    return lineText.substring(keyword.length()).trim();
                }
            }
            
            // If no keyword found, return the line as-is
            return lineText;
            
        } catch (Exception e) {
            LOG.debug("Error extracting step text from feature file", e);
        }
        
        return null;
    }

    /**
     * Finds step definition method in the class.
     * 
     * @param element The PSI element to search from
     * @return The step text from a step definition method, or null if not found
     */
    private String findStepDefinitionMethod(PsiElement element) {
        try {
            PsiClass parentClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
            if (parentClass != null) {
                PsiMethod[] methods = parentClass.getMethods();
                for (PsiMethod method : methods) {
                    String stepText = extractStepTextFromMethod(method);
                    if (stepText != null) {
                        return stepText;
                    }
                }
            }
        } catch (Exception e) {
            LOG.debug("Error finding step definition method", e);
        }
        
        return null;
    }
    
    /**
     * Extracts step text from a class by matching the line text to a method with a Cucumber annotation.
     * 
     * @param psiClass The PSI class to search in
     * @param lineText The line text to match against
     * @return The matching step text, or null if not found
     */
    private String extractStepTextFromClass(PsiClass psiClass, String lineText) {
        if (psiClass == null || lineText == null) {
            return null;
        }
        
        for (PsiMethod method : psiClass.getMethods()) {
            String stepText = extractStepTextFromMethod(method);
            if (stepText != null && lineText.contains(stepText)) {
                return stepText;
            }
        }
        return null;
    }
    
    /**
     * Extracts the proper scenario name from the test, not the step text.
     * For Cucumber tests, this should be the scenario name from the feature file.
     * For scenario outlines, it combines the scenario title with the example identifier.
     * 
     * @param test The test proxy to extract scenario name from
     * @param failedStepText The failed step text for fallback
     * @return The extracted scenario name
     */
    private String extractScenarioName(SMTestProxy test, String failedStepText) {
        try {
            // First try to get the parent test name (which might be the scenario)
            SMTestProxy parent = test.getParent();
            if (parent != null && parent != test) {
                String parentName = parent.getName();
                if (isValidScenarioName(parentName, failedStepText)) {
                    return formatScenarioName(parentName, test.getName());
                }
            }
            
            // If parent name is the same as step text, try to find a more descriptive name
            String testName = test.getName();
            if (isValidScenarioName(testName, failedStepText)) {
                return formatScenarioName(testName, null);
            }
            
            // Fallback: try to extract from stack trace
            String stackTrace = test.getStacktrace();
            if (stackTrace != null) {
                String scenarioFromStack = extractScenarioFromStackTrace(stackTrace);
                if (scenarioFromStack != null) {
                    return formatScenarioName(scenarioFromStack, test.getName());
                }
            }
            
            // Last resort: use a descriptive name based on the step
            return createDescriptiveScenarioName(failedStepText);
            
        } catch (Exception e) {
            LOG.debug("Error extracting scenario name", e);
            return "Cucumber Test";
        }
    }
    
    /**
     * Formats the scenario name to include both scenario outline title and example identifier.
     * For scenario outlines, it formats as "Scenario Title (Example #1.1)".
     * For regular scenarios, it returns the scenario name as is.
     * 
     * @param scenarioName The base scenario name
     * @param testName The test name (which might contain example identifier)
     * @return The formatted scenario name
     */
    String formatScenarioName(String scenarioName, String testName) {
        if (scenarioName == null || scenarioName.isEmpty()) {
            return testName != null ? testName : "Unknown Scenario";
        }
        
        // Check if this is a scenario outline (test name contains "Example #")
        if (testName != null && testName.matches("Example #\\d+\\.\\d+")) {
            return scenarioName + " (" + testName + ")";
        }
        
        // Regular scenario, return as is
        return scenarioName;
    }
    
    /**
     * Checks if a scenario name is valid and different from the failed step text.
     * 
     * @param scenarioName The scenario name to validate
     * @param failedStepText The failed step text to compare against
     * @return true if the scenario name is valid, false otherwise
     */
    private boolean isValidScenarioName(String scenarioName, String failedStepText) {
        return scenarioName != null && !scenarioName.isEmpty() && !scenarioName.equals(failedStepText);
    }
    
    /**
     * Creates a descriptive scenario name based on the failed step text.
     * 
     * @param failedStepText The failed step text
     * @return A descriptive scenario name
     */
    private String createDescriptiveScenarioName(String failedStepText) {
        if (failedStepText != null && !failedStepText.isEmpty()) {
            String truncatedStep = failedStepText.length() > 50 
                ? failedStepText.substring(0, 50) + "..." 
                : failedStepText;
            return "Cucumber Test - " + truncatedStep;
        }
        return "Cucumber Test - Unknown Step";
    }
    
    /**
     * Extracts scenario name from stack trace by looking for feature file references.
     * 
     * @param stackTrace The stack trace to search in
     * @return The extracted scenario name, or null if not found
     */
    private String extractScenarioFromStackTrace(String stackTrace) {
        try {
            String[] lines = stackTrace.split("\n");
            for (String line : lines) {
                // Look for lines that reference feature files
                if (line.contains(".feature:") && line.contains("✽")) {
                    // Extract the step text from the feature file reference
                    int stepStart = line.indexOf("✽");
                    if (stepStart > 0) {
                        String stepPart = line.substring(stepStart + 1).trim();
                        // Remove the file reference part
                        if (stepPart.contains("(file://")) {
                            stepPart = stepPart.substring(0, stepPart.indexOf("(file://")).trim();
                        }
                        if (!stepPart.isEmpty()) {
                            return stepPart;
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.debug("Error extracting scenario from stack trace", e);
        }
        return null;
    }
    
    /**
     * Fallback method to extract step text from test name.
     * 
     * @param test The test proxy to extract name from
     * @return The test name as step text, or null if not available
     */
    private String fallbackToTestName(SMTestProxy test) {
        try {
            String testName = test.getName();
            if (testName != null && !testName.isEmpty()) {
                return testName;
            }
        } catch (Exception e) {
            LOG.debug("Error in fallback to test name", e);
        }
        
        LOG.warn("No fallback text available for test");
        return null;
    }

    /**
     * Extracts expected value from stack trace.
     * 
     * @param stackTrace The stack trace to search in
     * @return The expected value, or null if not found
     */
    private String extractExpectedValue(String stackTrace) {
        if (stackTrace == null) {
            return null;
        }
        
        String[] lines = stackTrace.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("Expected:")) {
                return line.substring("Expected:".length()).trim();
            }
        }
        return null;
    }

    /**
     * Extracts actual value from stack trace.
     * 
     * @param stackTrace The stack trace to search in
     * @return The actual value, or null if not found
     */
    private String extractActualValue(String stackTrace) {
        if (stackTrace == null) {
            return null;
        }
        
        String[] lines = stackTrace.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("but: was")) {
                return line.substring("but: was".length()).trim();
            }
        }
        return null;
    }
    
    /**
     * Extracts source file from stack trace (fallback method).
     * 
     * @param stackTrace The stack trace to search in
     * @return The source file path, or null if not found
     */
    private String extractSourceFileFromStackTrace(String stackTrace) {
        try {
            String[] lines = stackTrace.split("\n");
            for (String line : lines) {
                if (line.contains(".java:") && !line.contains("cucumber") && !line.contains("junit")) {
                    String[] parts = line.split(":");
                    if (parts.length >= 2) {
                        return parts[0] + ":" + parts[1];
                    }
                }
            }
        } catch (Exception e) {
            LOG.debug("Error extracting source file from stack trace", e);
        }
        
        return null;
    }
    
    /**
     * Extracts line number from stack trace (fallback method).
     * 
     * @param stackTrace The stack trace to search in
     * @return The line number, or -1 if not found
     */
    private int extractLineNumberFromStackTrace(String stackTrace) {
        try {
            String[] lines = stackTrace.split("\n");
            for (String line : lines) {
                if (line.contains(".java:") && !line.contains("cucumber") && !line.contains("junit")) {
                    String[] parts = line.split(":");
                    if (parts.length >= 3) {
                        try {
                            return Integer.parseInt(parts[2]);
                        } catch (NumberFormatException e) {
                            LOG.debug("Could not parse line number from stack trace", e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.debug("Error extracting line number from stack trace", e);
        }
        
        return -1;
    }
    
    /**
     * Creates a minimal failure info when extraction fails.
     * 
     * @param test The test proxy to create minimal info for
     * @return A minimal FailureInfo object
     */
    private FailureInfo createMinimalFailureInfo(SMTestProxy test) {
        return new FailureInfo.Builder()
                .withScenarioName(test.getName())
                .withErrorMessage("Failed to extract detailed failure information")
                .withStackTrace("Stack trace extraction failed")
                .withParsingTime(System.currentTimeMillis())
                .build();
    }
} 