package com.trace.test.extractors;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.editor.Document;
import com.trace.test.models.StepDefinitionInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts step definition method information using IntelliJ's PSI.
 * 
 * <p>This class uses PSI-based navigation to find Cucumber step definitions by:</p>
 * <ul>
 *   <li>Extracting Java step definition file path from stack trace</li>
 *   <li>Navigating directly to the failure location using file path and line number</li>
 *   <li>Finding the containing method at the failure location</li>
 *   <li>Extracting method implementation and parameters</li>
 *   <li>Providing accurate source file and line number information</li>
 * </ul>
 * 
 * <p>Follows JetBrains best practices for PSI-based code analysis and thread safety.</p>
 */
public class StepDefinitionExtractor {
    private static final Logger LOG = Logger.getInstance(StepDefinitionExtractor.class);
    private final Project project;

    // Cucumber annotation patterns for step definition detection
    private static final String[] CUCUMBER_ANNOTATIONS = {
        "io.cucumber.java.en.Given",
        "io.cucumber.java.en.When", 
        "io.cucumber.java.en.Then",
        "io.cucumber.java.en.And",
        "io.cucumber.java.en.But"
    };

    // Common step definition class name patterns
    private static final String[] STEP_CLASS_PATTERNS = {
        "step", "test", "steps", "stepdefinitions"
    };

    // Common step definition method name patterns
    private static final String[] STEP_METHOD_PATTERNS = {
        "should", "given", "when", "then", "and", "but", "i_", "user_"
    };

    // Standard Java source directory patterns
    private static final String[] SOURCE_PATHS = {
        "src/test/java/",
        "src/main/java/",
        "test/java/",
        "main/java/"
    };

    /**
     * Constructor for StepDefinitionExtractor.
     *
     * @param project The current IntelliJ project
     * @throws NullPointerException if project is null
     */
    public StepDefinitionExtractor(Project project) {
        if (project == null) {
            throw new NullPointerException("Project cannot be null");
        }
        this.project = project;
    }

    /**
     * Extracts step definition information for a failed step using direct PSI navigation.
     * This method extracts the Java step definition file path from the stack trace
     * and navigates to the actual method that contains the step definition.
     * 
     * <p>This method follows IntelliJ Platform best practices by:</p>
     * <ul>
     *   <li>Extracting Java file path from stack trace (not feature file path)</li>
     *   <li>Using PSI navigation to the exact failure location</li>
     *   <li>Extracting step definition from the actual method that failed</li>
     *   <li>Avoiding fragile pattern matching</li>
     *   <li>Using IntelliJ's built-in code analysis</li>
     * </ul>
     *
     * @param stackTrace The stack trace containing the failure information
     * @return StepDefinitionInfo containing method details, or null if not found
     */
    public StepDefinitionInfo extractStepDefinition(String stackTrace) {
        if (stackTrace == null || stackTrace.trim().isEmpty()) {
            LOG.debug("Stack trace is null or empty");
            return null;
        }
        
        try {
            // Extract Java step definition file path and line number from stack trace
            StepLocation stepLocation = extractStepLocationFromStackTrace(stackTrace);
            if (stepLocation == null) {
                LOG.debug("Could not extract step location from stack trace");
                return null;
            }
            
            // Use read action for PSI operations (JetBrains best practice)
            return ApplicationManager.getApplication().<StepDefinitionInfo>runReadAction(() -> {
                try {
                    // Step 1: Navigate to the exact file and line using PSI
                    PsiFile psiFile = navigateToFile(stepLocation.filePath);
                    if (psiFile == null) {
                        LOG.debug("Could not navigate to file: " + stepLocation.filePath);
                        return null;
                    }
                    
                    // Step 2: Navigate to the exact line number
                    PsiElement elementAtLine = navigateToLine(psiFile, stepLocation.lineNumber);
                    if (elementAtLine == null) {
                        LOG.debug("Could not navigate to line: " + stepLocation.lineNumber);
                        return null;
                    }
                    
                    // Step 3: Find the method containing this element
                    PsiMethod containingMethod = findContainingMethod(elementAtLine);
                    if (containingMethod == null) {
                        LOG.debug("Could not find containing method");
                        return null;
                    }
                    
                    // Step 4: Extract step definition information from the method
                    return extractStepDefinitionFromMethod(containingMethod);
                    
                } catch (Exception e) {
                    LOG.warn("Failed to extract step definition from location", e);
                    return null;
                }
            });
            
        } catch (Exception e) {
            LOG.warn("Exception during step definition extraction", e);
            return null;
        }
    }
    
    /**
     * Extracts step definition file path and line number from stack trace.
     * This looks for the first Java file in the stack trace that contains step definitions.
     * 
     * @param stackTrace The stack trace to analyze
     * @return StepLocation containing file path and line number, or null if not found
     */
    private StepLocation extractStepLocationFromStackTrace(String stackTrace) {
        try {
            // Pattern to match Java files in stack trace: com.example.steps.ClassName.methodName(ClassName.java:lineNumber)
            Pattern pattern = Pattern.compile("at\\s+([\\w\\.]+)\\.([\\w]+)\\(([\\w]+\\.java):(\\d+)\\)");
            Matcher matcher = pattern.matcher(stackTrace);
            
            while (matcher.find()) {
                String className = matcher.group(1);
                String methodName = matcher.group(2);
                String fileName = matcher.group(3);
                int lineNumber = Integer.parseInt(matcher.group(4));
                
                // Check if this looks like a step definition class
                if (isStepDefinitionClass(className, methodName)) {
                    // Convert class name to file path
                    String filePath = convertClassNameToFilePath(className, fileName);
                    if (filePath != null) {
                        return new StepLocation(filePath, lineNumber);
                    }
                }
            }
            
            LOG.debug("No step definition location found in stack trace");
            return null;
            
        } catch (Exception e) {
            LOG.warn("Error extracting step location from stack trace", e);
            return null;
        }
    }
    
    /**
     * Checks if a class and method look like a step definition.
     * 
     * @param className The class name
     * @param methodName The method name
     * @return true if it looks like a step definition, false otherwise
     */
    private boolean isStepDefinitionClass(String className, String methodName) {
        String lowerClassName = className.toLowerCase();
        String lowerMethodName = methodName.toLowerCase();
        
        // Check for step definition class patterns
        boolean isStepClass = containsAnyPattern(lowerClassName, STEP_CLASS_PATTERNS);
        
        // Check for step definition method patterns
        boolean isStepMethod = containsAnyPattern(lowerMethodName, STEP_METHOD_PATTERNS);
        
        return isStepClass || isStepMethod;
    }
    
    /**
     * Checks if a string contains any of the specified patterns.
     * 
     * @param text The text to check
     * @param patterns The patterns to search for
     * @return true if any pattern is found, false otherwise
     */
    private boolean containsAnyPattern(String text, String[] patterns) {
        for (String pattern : patterns) {
            if (text.contains(pattern)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Converts a class name to a file path by searching in the project's file system.
     * This method works with both real file systems and test fixture virtual file systems.
     * 
     * @param className The fully qualified class name
     * @param fileName The file name (e.g., "ClassName.java")
     * @return The file path if found, or null if not found
     */
    private String convertClassNameToFilePath(String className, String fileName) {
        try {
            // Convert class name to package path
            String packagePath = className.replace('.', '/');
            
            // Look for the file in the project using the project's file system
            for (String sourcePath : SOURCE_PATHS) {
                String fullPath = sourcePath + packagePath + ".java";
                
                // Use the project's base directory to search for files
                VirtualFile baseDir = ProjectUtil.guessProjectDir(project);
                if (baseDir != null) {
                    VirtualFile virtualFile = baseDir.findFileByRelativePath(fullPath);
                    if (virtualFile != null && virtualFile.exists()) {
                        return virtualFile.getPath();
                    }
                }
                
                // Fallback: try using LocalFileSystem for absolute paths
                VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath(baseDir != null ? baseDir.getPath() + "/" + fullPath : fullPath);
                if (virtualFile != null && virtualFile.exists()) {
                    return virtualFile.getPath();
                }
            }
            
            LOG.debug("Could not find file for class: " + className);
            return null;
            
        } catch (Exception e) {
            LOG.warn("Error converting class name to file path", e);
            return null;
        }
    }
    
    /**
     * Simple data class to hold step location information.
     */
    private static class StepLocation {
        final String filePath;
        final int lineNumber;
        
        StepLocation(String filePath, int lineNumber) {
            this.filePath = filePath;
            this.lineNumber = lineNumber;
        }
    }
    
    /**
     * Navigates to a file using PSI.
     * 
     * @param sourceFilePath The file path to navigate to
     * @return The PSI file, or null if not found
     */
    private PsiFile navigateToFile(String sourceFilePath) {
        try {
            // Convert file path to VirtualFile
            VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath(sourceFilePath);
            if (virtualFile == null) {
                LOG.debug("VirtualFile not found for: " + sourceFilePath);
                return null;
            }
            
            // Get PSI file from VirtualFile
            PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
            if (psiFile == null) {
                LOG.debug("PSI file not found for: " + sourceFilePath);
                return null;
            }
            
            return psiFile;
            
        } catch (Exception e) {
            LOG.warn("Error navigating to file: " + sourceFilePath, e);
            return null;
        }
    }
    
    /**
     * Navigates to a specific line in a PSI file.
     * 
     * @param psiFile The PSI file
     * @param lineNumber The line number (1-indexed)
     * @return The PSI element at that line, or null if not found
     */
    private PsiElement navigateToLine(PsiFile psiFile, int lineNumber) {
        try {
            // Get the document for line-based navigation
            Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
            if (document == null) {
                LOG.debug("Could not get document for file");
                return null;
            }
            
            // Convert line number to offset
            int lineIndex = lineNumber - 1; // Convert to 0-indexed
            if (lineIndex < 0 || lineIndex >= document.getLineCount()) {
                LOG.debug("Line number out of bounds: " + lineNumber);
                return null;
            }
            
            int lineStartOffset = document.getLineStartOffset(lineIndex);
            
            // Find the PSI element at this offset
            PsiElement element = psiFile.findElementAt(lineStartOffset);
            if (element == null) {
                LOG.debug("No PSI element found at line: " + lineNumber);
                return null;
            }
            
            return element;
            
        } catch (Exception e) {
            LOG.warn("Error navigating to line: " + lineNumber, e);
            return null;
        }
    }
    
    /**
     * Finds the method containing a PSI element.
     * 
     * @param element The PSI element
     * @return The containing method, or null if not found
     */
    private PsiMethod findContainingMethod(PsiElement element) {
        try {
            // Use PsiTreeUtil to find the containing method
            PsiMethod method = PsiTreeUtil.getParentOfType(element, PsiMethod.class);
            if (method == null) {
                LOG.debug("No containing method found");
                return null;
            }
            
            return method;
            
        } catch (Exception e) {
            LOG.warn("Error finding containing method", e);
            return null;
        }
    }
    
    /**
     * Extracts step definition information from a method.
     * 
     * @param method The method to extract information from
     * @return StepDefinitionInfo with extracted details
     */
    private StepDefinitionInfo extractStepDefinitionFromMethod(PsiMethod method) {
        try {
            // Get the class containing the method
            PsiClass containingClass = method.getContainingClass();
            if (containingClass == null) {
                LOG.debug("Could not find containing class");
                return null;
            }
            
            // Get the file containing the class
            PsiFile containingFile = containingClass.getContainingFile();
            if (containingFile == null) {
                LOG.debug("Could not find containing file");
                return null;
            }
            
            // Extract step pattern from method annotations
            String stepPattern = extractStepPatternFromMethod(method);
            if (stepPattern == null) {
                LOG.debug("Could not extract step pattern from method");
                return null;
            }
            
            // Extract method parameters
            List<String> parameters = extractMethodParameters(method);
            
            // Get accurate line number
            int lineNumber = getAccurateLineNumber(method);
            
            // Extract package name
            String packageName = extractPackageName(containingFile, containingClass);
            
            // Extract method text
            String methodText = method.getText();
            
            return new StepDefinitionInfo.Builder()
                    .withMethodName(method.getName())
                    .withClassName(containingClass.getName())
                    .withPackageName(packageName)
                    .withSourceFilePath(containingFile.getName())
                    .withLineNumber(lineNumber)
                    .withStepPattern(stepPattern)
                    .withParameters(parameters)
                    .withMethodText(methodText)
                    .build();
            
        } catch (Exception e) {
            LOG.warn("Failed to extract step definition from method", e);
            return null;
        }
    }
    
    /**
     * Extracts the step pattern from a method's annotations.
     * 
     * @param method The method to extract pattern from
     * @return The step pattern, or null if not found
     */
    private String extractStepPatternFromMethod(PsiMethod method) {
        try {
            PsiAnnotation[] annotations = method.getModifierList().getAnnotations();
            for (PsiAnnotation annotation : annotations) {
                String annotationName = annotation.getQualifiedName();
                if (annotationName != null && isCucumberAnnotation(annotationName)) {
                    // Extract the value from the annotation
                    PsiAnnotationMemberValue value = annotation.findAttributeValue("value");
                    if (value != null) {
                        String pattern = extractStringValue(value);
                        if (pattern != null) {
                            return pattern;
                        }
                    }
                }
            }
            
            LOG.debug("No Cucumber annotation found on method");
            return null;
            
        } catch (Exception e) {
            LOG.warn("Error extracting step pattern from method", e);
            return null;
        }
    }
    
    /**
     * Checks if an annotation is a Cucumber annotation.
     * 
     * @param annotationName The annotation name to check
     * @return true if it's a Cucumber annotation, false otherwise
     */
    private boolean isCucumberAnnotation(String annotationName) {
        for (String cucumberAnnotation : CUCUMBER_ANNOTATIONS) {
            if (cucumberAnnotation.equals(annotationName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extracts string value from a PSI annotation member value.
     * 
     * @param value The annotation member value
     * @return The string value, or null if not a string
     */
    private String extractStringValue(PsiAnnotationMemberValue value) {
        if (value instanceof PsiLiteralExpression) {
            Object literalValue = ((PsiLiteralExpression) value).getValue();
            if (literalValue instanceof String) {
                return (String) literalValue;
            }
        }
        return null;
    }

    /**
     * Extracts method parameters as a list of strings.
     * 
     * @param method The method to extract parameters from
     * @return List of parameter names
     */
    private List<String> extractMethodParameters(PsiMethod method) {
        List<String> parameters = new ArrayList<>();
        
        PsiParameter[] psiParameters = method.getParameterList().getParameters();
        for (PsiParameter parameter : psiParameters) {
            parameters.add(parameter.getName());
        }
        
        return parameters;
    }

    /**
     * Gets the accurate line number of a PSI element using TextRange.
     * This is the recommended JetBrains approach for accurate line number extraction.
     *
     * @param element The PSI element
     * @return The line number (1-indexed), or -1 if not found
     */
    private int getAccurateLineNumber(PsiElement element) {
        try {
            // Use TextRange for accurate line number calculation (JetBrains best practice)
            TextRange textRange = element.getTextRange();
            if (textRange != null) {
                PsiFile file = element.getContainingFile();
                if (file != null) {
                    // Get the document for accurate line calculation
                    Document document = 
                        com.intellij.openapi.fileEditor.FileDocumentManager.getInstance()
                            .getDocument(file.getVirtualFile());
                    if (document != null) {
                        int lineNumber = document.getLineNumber(textRange.getStartOffset()) + 1; // Convert to 1-indexed
                        return lineNumber;
                    }
                }
            }
        } catch (Exception e) {
            LOG.debug("Failed to get accurate line number for element", e);
        }
        
        // Fallback to manual calculation if Document approach fails
        return getLineNumberFallback(element);
    }

    /**
     * Fallback method for line number calculation when Document approach fails.
     * 
     * @param element The PSI element
     * @return The line number (1-indexed), or -1 if not found
     */
    private int getLineNumberFallback(PsiElement element) {
        try {
            PsiFile file = element.getContainingFile();
            if (file != null) {
                String text = file.getText();
                int offset = element.getTextOffset();
                int lineNumber = text.substring(0, offset).split("\n").length;
                return lineNumber;
            }
        } catch (Exception e) {
            LOG.debug("Failed to get fallback line number for element", e);
        }
        
        return -1;
    }

    /**
     * Extracts package name from the file or class.
     * 
     * @param containingFile The file containing the class
     * @param containingClass The class
     * @return The package name, or null if not found
     */
    private String extractPackageName(PsiFile containingFile, PsiClass containingClass) {
        // First try to get from the file's package statement
        if (containingFile instanceof PsiJavaFile) {
            String packageName = ((PsiJavaFile) containingFile).getPackageName();
            if (packageName != null && !packageName.isEmpty()) {
                return packageName;
            }
        }
        
        // Fallback: try to parse from qualified class name
        String qualifiedName = containingClass.getQualifiedName();
        if (qualifiedName != null && containingClass.getName() != null && 
            qualifiedName.endsWith(containingClass.getName())) {
            int idx = qualifiedName.lastIndexOf('.');
            if (idx > 0) {
                return qualifiedName.substring(0, idx);
            }
        }
        
        return null;
    }
} 