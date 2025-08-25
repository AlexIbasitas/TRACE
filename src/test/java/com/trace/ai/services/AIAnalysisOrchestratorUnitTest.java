package com.trace.ai.services;

import com.trace.ai.configuration.AIServiceType;
import com.trace.ai.models.AIAnalysisResult;
import com.trace.test.models.FailureInfo;
import com.trace.test.models.GherkinScenarioInfo;
import com.trace.test.models.StepDefinitionInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("AI Analysis Orchestrator Unit Tests")
class AIAnalysisOrchestratorUnitTest {
    
    private FailureInfo testFailureInfo;
    
    @BeforeEach
    void setUp() {
        // Create test failure info with proper constructors
        List<String> stepParams = new ArrayList<>();
        stepParams.add("buttonName");
        StepDefinitionInfo stepInfo = new StepDefinitionInfo(
            "clickLoginButton", "LoginStepDefinitions", "com.example.steps",
            "/src/test/java/LoginTest.java", 42, "^I click the (.*?) button$",
            stepParams, "public void clickLoginButton(String buttonName) { /* implementation */ }"
        );
        
        List<String> scenarioSteps = new ArrayList<>();
        scenarioSteps.add("Given I am on the login page");
        scenarioSteps.add("When I click the login button");
        scenarioSteps.add("Then I should be logged in");
        
        List<String> tags = new ArrayList<>();
        tags.add("@smoke");
        tags.add("@login");
        
        GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo(
            "User Authentication", "User login with valid credentials",
            scenarioSteps, tags, new ArrayList<>(), new ArrayList<>(),
            "Scenario: User login with valid credentials\nGiven I am on the login page\nWhen I click the login button\nThen I should be logged in",
            false, "/src/test/features/login.feature", 5, "Feature: User Authentication\nScenario: User login with valid credentials"
        );
        
        testFailureInfo = new FailureInfo(
            "Test Login Scenario",
            "I click the login button",
            "java.lang.AssertionError: Expected true but was false",
            "/src/test/java/LoginTest.java",
            42,
            stepInfo,
            scenarioInfo,
            "true",
            "false",
            "Login button click failed",
            System.currentTimeMillis()
        );
    }
    
    @Nested
    @DisplayName("Failure Info Validation")
    class FailureInfoValidation {
        
        @Test
        @DisplayName("should create valid failure info")
        void shouldCreateValidFailureInfo() {
            // Assert
            assertThat(testFailureInfo).isNotNull();
            assertThat(testFailureInfo.getScenarioName()).isEqualTo("Test Login Scenario");
            assertThat(testFailureInfo.getFailedStepText()).isEqualTo("I click the login button");
            assertThat(testFailureInfo.getErrorMessage()).isEqualTo("Login button click failed");
            assertThat(testFailureInfo.getExpectedValue()).isEqualTo("true");
            assertThat(testFailureInfo.getActualValue()).isEqualTo("false");
        }
        
        @Test
        @DisplayName("should have valid step definition info")
        void shouldHaveValidStepDefinitionInfo() {
            // Assert
            StepDefinitionInfo stepInfo = testFailureInfo.getStepDefinitionInfo();
            assertThat(stepInfo).isNotNull();
            assertThat(stepInfo.getMethodName()).isEqualTo("clickLoginButton");
            assertThat(stepInfo.getClassName()).isEqualTo("LoginStepDefinitions");
            assertThat(stepInfo.getPackageName()).isEqualTo("com.example.steps");
            assertThat(stepInfo.getSourceFilePath()).isEqualTo("/src/test/java/LoginTest.java");
            assertThat(stepInfo.getLineNumber()).isEqualTo(42);
        }
        
        @Test
        @DisplayName("should have valid gherkin scenario info")
        void shouldHaveValidGherkinScenarioInfo() {
            // Assert
            GherkinScenarioInfo scenarioInfo = testFailureInfo.getGherkinScenarioInfo();
            assertThat(scenarioInfo).isNotNull();
            assertThat(scenarioInfo.getFeatureName()).isEqualTo("User Authentication");
            assertThat(scenarioInfo.getScenarioName()).isEqualTo("User login with valid credentials");
            assertThat(scenarioInfo.getSteps()).hasSize(3);
            assertThat(scenarioInfo.getTags()).hasSize(2);
            assertThat(scenarioInfo.getTags()).contains("@smoke", "@login");
        }
    }
    
    @Nested
    @DisplayName("AIAnalysisResult Creation")
    class AIAnalysisResultCreation {
        
        @Test
        @DisplayName("should create analysis result with basic constructor")
        void shouldCreateAnalysisResultWithBasicConstructor() {
            // Arrange
            String analysis = "The login button click failed because the element was not found";
            long timestamp = System.currentTimeMillis();
            
            // Act
            AIAnalysisResult result = new AIAnalysisResult(
                analysis,
                AIServiceType.OPENAI,
                "gpt-4",
                timestamp,
                1500L
            );
            
            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getAnalysis()).isEqualTo(analysis);
            assertThat(result.getServiceType()).isEqualTo(AIServiceType.OPENAI);
            assertThat(result.getModelId()).isEqualTo("gpt-4");
            assertThat(result.getTimestamp()).isEqualTo(timestamp);
            assertThat(result.getProcessingTimeMs()).isEqualTo(1500L);
            assertThat(result.getPrompt()).isNull();
            assertThat(result.hasPrompt()).isFalse();
        }
        
        @Test
        @DisplayName("should create analysis result with prompt")
        void shouldCreateAnalysisResultWithPrompt() {
            // Arrange
            String analysis = "The login button click failed because the element was not found";
            String prompt = "Analyze this test failure: Login button click failed";
            long timestamp = System.currentTimeMillis();
            
            // Act
            AIAnalysisResult result = new AIAnalysisResult(
                analysis,
                prompt,
                AIServiceType.OPENAI,
                "gpt-4",
                timestamp,
                1500L
            );
            
            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getAnalysis()).isEqualTo(analysis);
            assertThat(result.getPrompt()).isEqualTo(prompt);
            assertThat(result.hasPrompt()).isTrue();
            assertThat(result.getServiceType()).isEqualTo(AIServiceType.OPENAI);
            assertThat(result.getModelId()).isEqualTo("gpt-4");
        }
        
        @Test
        @DisplayName("should handle empty prompt correctly")
        void shouldHandleEmptyPromptCorrectly() {
            // Arrange
            String analysis = "Analysis result";
            String emptyPrompt = "";
            
            // Act
            AIAnalysisResult result = new AIAnalysisResult(
                analysis,
                emptyPrompt,
                AIServiceType.OPENAI,
                "gpt-4",
                System.currentTimeMillis(),
                1000L
            );
            
            // Assert
            assertThat(result.getPrompt()).isEqualTo("");
            assertThat(result.hasPrompt()).isFalse();
        }
        
        @Test
        @DisplayName("should handle whitespace-only prompt correctly")
        void shouldHandleWhitespaceOnlyPromptCorrectly() {
            // Arrange
            String analysis = "Analysis result";
            String whitespacePrompt = "   ";
            
            // Act
            AIAnalysisResult result = new AIAnalysisResult(
                analysis,
                whitespacePrompt,
                AIServiceType.OPENAI,
                "gpt-4",
                System.currentTimeMillis(),
                1000L
            );
            
            // Assert
            assertThat(result.getPrompt()).isEqualTo("   ");
            assertThat(result.hasPrompt()).isFalse();
        }
    }
    
    @Nested
    @DisplayName("Analysis Mode Validation")
    class AnalysisModeValidation {
        
        @Test
        @DisplayName("should have correct analysis mode values")
        void shouldHaveCorrectAnalysisModeValues() {
            // Assert
            assertThat(AnalysisMode.OVERVIEW).isNotNull();
            assertThat(AnalysisMode.FULL).isNotNull();
            assertThat(AnalysisMode.values()).hasSize(2);
        }
        
        @Test
        @DisplayName("should convert analysis mode to string correctly")
        void shouldConvertAnalysisModeToStringCorrectly() {
            // Assert
            assertThat(AnalysisMode.OVERVIEW.toString()).isEqualTo("OVERVIEW");
            assertThat(AnalysisMode.FULL.toString()).isEqualTo("FULL");
        }
    }
    
    @Nested
    @DisplayName("CompletableFuture Handling")
    class CompletableFutureHandling {
        
        @Test
        @DisplayName("should create completed future with result")
        void shouldCreateCompletedFutureWithResult() throws ExecutionException, InterruptedException {
            // Arrange
            AIAnalysisResult expectedResult = new AIAnalysisResult(
                "Test analysis",
                AIServiceType.OPENAI,
                "gpt-4",
                System.currentTimeMillis(),
                1000L
            );
            
            // Act
            CompletableFuture<AIAnalysisResult> future = CompletableFuture.completedFuture(expectedResult);
            AIAnalysisResult result = future.get();
            
            // Assert
            assertThat(result).isEqualTo(expectedResult);
            assertThat(future.isDone()).isTrue();
            assertThat(future.isCompletedExceptionally()).isFalse();
        }
        
        @Test
        @DisplayName("should handle failed future gracefully")
        void shouldHandleFailedFutureGracefully() {
            // Arrange
            RuntimeException exception = new RuntimeException("Test error");
            CompletableFuture<AIAnalysisResult> future = CompletableFuture.failedFuture(exception);
            
            // Assert
            assertThat(future.isDone()).isTrue();
            assertThat(future.isCompletedExceptionally()).isTrue();
            assertThatThrownBy(() -> future.get())
                .isInstanceOf(ExecutionException.class)
                .hasCause(exception);
        }
        
        @Test
        @DisplayName("should create future with exception handling")
        void shouldCreateFutureWithExceptionHandling() throws ExecutionException, InterruptedException {
            // Arrange
            CompletableFuture<AIAnalysisResult> future = CompletableFuture.supplyAsync(() -> {
                throw new RuntimeException("Simulated error");
            });
            
            // Act & Assert
            assertThatThrownBy(() -> future.get())
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining("Simulated error");
            
            assertThat(future.isDone()).isTrue();
            assertThat(future.isCompletedExceptionally()).isTrue();
        }
    }
    
    @Nested
    @DisplayName("Error Handling Patterns")
    class ErrorHandlingPatterns {
        
        @Test
        @DisplayName("should create error result for disabled service")
        void shouldCreateErrorResultForDisabledService() {
            // Act
            AIAnalysisResult result = new AIAnalysisResult(
                "TRACE is OFF. Turn TRACE ON to enable context extraction and AI features.",
                AIServiceType.OPENAI,
                "disabled",
                System.currentTimeMillis(),
                0L
            );
            
            // Assert
            assertThat(result.getAnalysis()).contains("TRACE is OFF");
            assertThat(result.getModelId()).isEqualTo("disabled");
            assertThat(result.getProcessingTimeMs()).isEqualTo(0L);
        }
        
        @Test
        @DisplayName("should create error result for prompt-only mode")
        void shouldCreateErrorResultForPromptOnlyMode() {
            // Arrange
            String prompt = "Test prompt for analysis";
            
            // Act
            AIAnalysisResult result = new AIAnalysisResult(
                "Prompt preview only. AI Analysis is disabled.",
                prompt,
                AIServiceType.OPENAI,
                "disabled",
                System.currentTimeMillis(),
                0L
            );
            
            // Assert
            assertThat(result.getAnalysis()).contains("Prompt preview only");
            assertThat(result.getPrompt()).isEqualTo(prompt);
            assertThat(result.getModelId()).isEqualTo("disabled");
        }
        
        @Test
        @DisplayName("should create error result for network failure")
        void shouldCreateErrorResultForNetworkFailure() {
            // Act
            AIAnalysisResult result = new AIAnalysisResult(
                "Analysis failed due to an error: Network timeout",
                AIServiceType.OPENAI,
                "error",
                System.currentTimeMillis(),
                0L
            );
            
            // Assert
            assertThat(result.getAnalysis()).contains("Analysis failed due to an error");
            assertThat(result.getModelId()).isEqualTo("error");
        }
    }
}
