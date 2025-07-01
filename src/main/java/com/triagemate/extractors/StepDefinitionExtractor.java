package com.triagemate.extractors;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiSearchHelper;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.triagemate.models.StepDefinitionInfo;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Extracts step definition method information using IntelliJ's PSI.
 * 
 * <p>This class uses PSI-based analysis to find Cucumber step definitions by:</p>
 * <ul>
 *   <li>Searching for @Given, @When, @Then annotations in the project</li>
 *   <li>Matching failed step text with step definition patterns</li>
 *   <li>Extracting method implementation and parameters</li>
 *   <li>Providing accurate source file and line number information</li>
 * </ul>
 * 
 * <p>Follows JetBrains best practices for PSI-based code analysis and thread safety.</p>
 */
public class StepDefinitionExtractor {
    private static final Logger LOG = Logger.getInstance(StepDefinitionExtractor.class);
    private final Project project;

    // Cucumber annotation patterns
    private static final String[] CUCUMBER_ANNOTATIONS = {
        "io.cucumber.java.en.Given",
        "io.cucumber.java.en.When", 
        "io.cucumber.java.en.Then",
        "io.cucumber.java.en.And",
        "io.cucumber.java.en.But"
    };

    /**
     * Constructor for StepDefinitionExtractor
     *
     * @param project The current IntelliJ project
     */
    public StepDefinitionExtractor(Project project) {
        this.project = project;
    }

    /**
     * Extracts step definition information for a failed step.
     * 
     * <p>This method performs a comprehensive search to find the step definition
     * that matches the failed step text, using PSI-based analysis for accuracy.</p>
     *
     * @param failedStepText The text of the failed step (e.g., "Given I am on the login page")
     * @return StepDefinitionInfo containing method details, or null if not found
     */
    public StepDefinitionInfo extractStepDefinition(String failedStepText) {
        if (failedStepText == null || failedStepText.trim().isEmpty()) {
            LOG.warn("Failed step text is null or empty");
            return null;
        }

        try {
            // Strip step keyword from failed step text for pattern matching
            String stepTextWithoutKeyword = stripStepKeyword(failedStepText);
            LOG.debug("Original step text: '" + failedStepText + "', stripped: '" + stepTextWithoutKeyword + "'");
            
            // Use read action for PSI operations (JetBrains best practice)
            return ApplicationManager.getApplication().<StepDefinitionInfo>runReadAction(() -> {
                try {
                    return findStepDefinitionByText(stepTextWithoutKeyword);
                } catch (Exception e) {
                    LOG.warn("Failed to extract step definition for: " + failedStepText, e);
                    return null;
                }
            });
        } catch (Exception e) {
            LOG.warn("Failed to execute read action for step definition extraction", e);
            return null;
        }
    }

    /**
     * Strips the step keyword (Given/When/Then/And/But) from step text.
     * 
     * @param stepText The step text that may contain a keyword
     * @return The step text without the keyword
     */
    private String stripStepKeyword(String stepText) {
        if (stepText == null) {
            return null;
        }
        
        // Remove leading step keywords (Given, When, Then, And, But) followed by whitespace
        return stepText.replaceFirst("^(Given|When|Then|And|But)\\s+", "");
    }

    /**
     * Finds step definition by searching for matching annotations and patterns.
     *
     * @param failedStepText The failed step text to match
     * @return StepDefinitionInfo if found, null otherwise
     */
    private StepDefinitionInfo findStepDefinitionByText(String failedStepText) {
        LOG.debug("Searching for step definition for: " + failedStepText);
        
        // Search for all Cucumber annotations in the project
        for (String annotationName : CUCUMBER_ANNOTATIONS) {
            LOG.debug("Searching for annotation: " + annotationName);
            PsiClass annotationClass = JavaPsiFacade.getInstance(project)
                    .findClass(annotationName, GlobalSearchScope.allScope(project));
            
            if (annotationClass != null) {
                LOG.debug("Found annotation class: " + annotationClass.getName());
                // Find all usages of this annotation
                List<PsiAnnotation> annotations = findAnnotationUsages(annotationClass);
                LOG.debug("Found " + annotations.size() + " annotation usages");
                
                for (PsiAnnotation annotation : annotations) {
                    StepDefinitionInfo info = checkAnnotationMatch(annotation, failedStepText);
                    if (info != null) {
                        LOG.debug("Found matching step definition: " + info.getMethodName());
                        return info;
                    }
                }
            } else {
                LOG.debug("Annotation class not found: " + annotationName);
            }
        }
        
        LOG.debug("No matching step definition found for: " + failedStepText);
        return null;
    }

    /**
     * Finds all usages of a specific annotation in the project.
     * 
     * @param annotationClass The annotation class to search for
     * @return List of annotation usages
     */
    private List<PsiAnnotation> findAnnotationUsages(PsiClass annotationClass) {
        List<PsiAnnotation> annotations = new ArrayList<>();
        try {
            ProgressManager.getInstance().runProcess(() -> {
                // Use ReferencesSearch to find references (JetBrains best practice)
                com.intellij.psi.search.searches.ReferencesSearch
                    .search(annotationClass, GlobalSearchScope.projectScope(project))
                    .forEach(psiReference -> {
                        PsiElement element = psiReference.getElement();
                        PsiAnnotation annotation = PsiTreeUtil.getParentOfType(element, PsiAnnotation.class);
                        if (annotation != null) {
                            annotations.add(annotation);
                        }
                    });
            }, new EmptyProgressIndicator());
        } catch (Exception e) {
            LOG.warn("Failed to find annotation usages for: " + annotationClass.getName(), e);
        }
        return annotations;
    }

    /**
     * Checks if an annotation matches the failed step text.
     * 
     * @param annotation The annotation to check
     * @param failedStepText The failed step text to match
     * @return StepDefinitionInfo if match found, null otherwise
     */
    private StepDefinitionInfo checkAnnotationMatch(PsiAnnotation annotation, String failedStepText) {
        try {
            // Get the annotation's value (the step pattern)
            PsiAnnotationMemberValue value = annotation.findAttributeValue("value");
            if (value == null) {
                LOG.debug("No value attribute found in annotation");
                return null;
            }
            
            String stepPattern = extractStringValue(value);
            if (stepPattern == null) {
                LOG.debug("Could not extract string value from annotation");
                return null;
            }
            
            LOG.debug("Checking pattern: '" + stepPattern + "' against text: '" + failedStepText + "'");
            
            // Check if the failed step text matches this pattern
            if (matchesStepPattern(failedStepText, stepPattern)) {
                LOG.debug("Pattern matched! Extracting step definition info");
                return extractStepDefinitionInfo(annotation, stepPattern);
            } else {
                LOG.debug("Pattern did not match");
            }
            
        } catch (Exception e) {
            LOG.debug("Failed to check annotation match", e);
        }
        
        return null;
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
     * Checks if failed step text matches a step definition pattern.
     * 
     * <p>This method handles Cucumber's pattern matching, including:
     * - Exact text matches
     * - Parameter placeholders (e.g., {string}, {int})
     * - Regular expressions</p>
     * 
     * @param failedStepText The failed step text
     * @param stepPattern The step definition pattern
     * @return true if match found, false otherwise
     */
    private boolean matchesStepPattern(String failedStepText, String stepPattern) {
        try {
            // Convert Cucumber pattern to regex
            String regexPattern = convertCucumberPatternToRegex(stepPattern);
            LOG.debug("Converted pattern '" + stepPattern + "' to regex: '" + regexPattern + "'");
            
            Pattern pattern = Pattern.compile(regexPattern, Pattern.CASE_INSENSITIVE);
            boolean matches = pattern.matcher(failedStepText).matches();
            LOG.debug("Regex match result: " + matches);
            
            return matches;
            
        } catch (Exception e) {
            LOG.debug("Failed to match step pattern: " + stepPattern + " with text: " + failedStepText, e);
            return false;
        }
    }

    /**
     * Converts Cucumber step pattern to regex for matching.
     * 
     * @param stepPattern The Cucumber step pattern
     * @return The equivalent regex pattern
     */
    private String convertCucumberPatternToRegex(String stepPattern) {
        // First escape regex special characters (but not curly braces for parameters)
        String regex = stepPattern.replaceAll("([\\\\^\\[\\]\\{\\}\\(\\)\\+\\*\\.\\?\\|])", "\\\\$1");
        
        // Then handle common Cucumber parameter types
        regex = regex
            // {string} should match a quoted string or an unquoted word
            .replaceAll("\\\\\\{string\\\\\\}", "(?:\\\"([^\\\"]*)\\\"|([^\\s]+))")
            .replaceAll("\\\\\\{int\\\\\\}", "(\\d+)")
            .replaceAll("\\\\\\{float\\\\\\}", "(\\d+\\.\\d+)")
            .replaceAll("\\\\\\{word\\\\\\}", "(\\w+)")
            .replaceAll("\\\\\\{([^}]+)\\\\\\}", "([^\\s]+)"); // Generic parameter
        return "^" + regex + "$";
    }

    /**
     * Extracts step definition information from a matching annotation.
     * 
     * @param annotation The matching annotation
     * @param stepPattern The step pattern that matched
     * @return StepDefinitionInfo with extracted details
     */
    private StepDefinitionInfo extractStepDefinitionInfo(PsiAnnotation annotation, String stepPattern) {
        try {
            // Find the method containing this annotation
            PsiMethod method = PsiTreeUtil.getParentOfType(annotation, PsiMethod.class);
            if (method == null) {
                return null;
    }

            // Get the class containing the method
            PsiClass containingClass = method.getContainingClass();
            if (containingClass == null) {
                return null;
            }
            
            // Get the file containing the class
            PsiFile containingFile = containingClass.getContainingFile();
            if (containingFile == null) {
                return null;
            }
            
            // Extract method parameters
            List<String> parameters = extractMethodParameters(method);
            
            // Get line number
            int lineNumber = getLineNumber(method);

            // Extract package name from the file's package statement
            String packageName = null;
            if (containingFile instanceof PsiJavaFile) {
                packageName = ((PsiJavaFile) containingFile).getPackageName();
            } else {
                // fallback: try to parse from qualified class name
                String qualifiedName = containingClass.getQualifiedName();
                if (qualifiedName != null && containingClass.getName() != null && qualifiedName.endsWith(containingClass.getName())) {
                    int idx = qualifiedName.lastIndexOf('.');
                    if (idx > 0) {
                        packageName = qualifiedName.substring(0, idx);
                    }
                }
            }

            return new StepDefinitionInfo.Builder()
                    .withMethodName(method.getName())
                    .withClassName(containingClass.getName())
                    .withPackageName(packageName)
                    .withSourceFilePath(containingFile.getName())
                    .withLineNumber(lineNumber)
                    .withStepPattern(stepPattern)
                    .withParameters(parameters)
                    .withMethodText(method.getText())
                    .build();
                    
        } catch (Exception e) {
            LOG.warn("Failed to extract step definition info", e);
            return null;
        }
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
     * Gets the line number of a PSI element.
     *
     * @param element The PSI element
     * @return The line number, or -1 if not found
     */
    private int getLineNumber(PsiElement element) {
        try {
            PsiFile file = element.getContainingFile();
            if (file != null) {
                String text = file.getText();
                int offset = element.getTextOffset();
                return text.substring(0, offset).split("\n").length;
            }
        } catch (Exception e) {
            LOG.debug("Failed to get line number for element", e);
        }
        
        return -1;
    }
} 