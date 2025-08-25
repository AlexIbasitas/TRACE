package com.trace.ai.services;

import com.trace.ai.configuration.AIServiceType;
import com.trace.ai.configuration.AISettings;
import com.trace.ai.models.AIAnalysisResult;
import com.trace.ai.models.AIModel;
import com.trace.ai.prompts.InitialPromptFailureAnalysisService;
import com.trace.ai.services.providers.AIServiceProvider;
import com.trace.security.SecureAPIKeyManager;
import com.trace.test.models.FailureInfo;
import com.trace.test.models.GherkinScenarioInfo;
import com.trace.test.models.StepDefinitionInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AI Network Service Unit Tests")
class AINetworkServiceUnitTest {
    
    @Mock private AISettings mockAISettings;
    @Mock private AIModelService mockModelService;
    @Mock private AIServiceProvider mockProvider;
    @Mock private AIModel mockModel;
    @Mock private InitialPromptFailureAnalysisService mockPromptService;
    
    private FailureInfo testFailureInfo;
    
    @BeforeEach
    void setUp() {
        // Create test failure info
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
    @DisplayName("FailureInfo Validation")
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
    @DisplayName("AIServiceProvider Interface Testing")
    class AIServiceProviderInterfaceTesting {
        
        @Test
        @DisplayName("should mock provider interface correctly")
        void shouldMockProviderInterfaceCorrectly() {
            // Arrange
            when(mockProvider.getServiceType()).thenReturn(AIServiceType.OPENAI);
            when(mockProvider.getDisplayName()).thenReturn("OpenAI Provider");
            
            // Act & Assert
            assertThat(mockProvider.getServiceType()).isEqualTo(AIServiceType.OPENAI);
            assertThat(mockProvider.getDisplayName()).isEqualTo("OpenAI Provider");
        }
        
        @Test
        @DisplayName("should handle provider validation")
        void shouldHandleProviderValidation() throws ExecutionException, InterruptedException {
            // Arrange
            when(mockProvider.validateConnection("test-api-key"))
                .thenReturn(CompletableFuture.completedFuture(true));
            
            // Act
            CompletableFuture<Boolean> future = mockProvider.validateConnection("test-api-key");
            Boolean result = future.get();
            
            // Assert
            assertThat(result).isTrue();
            verify(mockProvider).validateConnection("test-api-key");
        }
        
        @Test
        @DisplayName("should handle provider analysis")
        void shouldHandleProviderAnalysis() throws ExecutionException, InterruptedException {
            // Arrange
            AIAnalysisResult expectedResult = new AIAnalysisResult(
                "Test analysis",
                AIServiceType.OPENAI,
                "gpt-4",
                System.currentTimeMillis(),
                1000L
            );
            when(mockProvider.analyze("test prompt", "gpt-4", "test-api-key"))
                .thenReturn(CompletableFuture.completedFuture(expectedResult));
            
            // Act
            CompletableFuture<AIAnalysisResult> future = mockProvider.analyze("test prompt", "gpt-4", "test-api-key");
            AIAnalysisResult result = future.get();
            
            // Assert
            assertThat(result).isEqualTo(expectedResult);
            assertThat(result.getAnalysis()).isEqualTo("Test analysis");
            verify(mockProvider).analyze("test prompt", "gpt-4", "test-api-key");
        }
    }
    
    @Nested
    @DisplayName("AIServiceFactory Testing")
    class AIServiceFactoryTesting {
        
        @Test
        @DisplayName("should mock factory provider retrieval")
        void shouldMockFactoryProviderRetrieval() {
            // Arrange
            try (var mockedFactory = org.mockito.Mockito.mockStatic(AIServiceFactory.class)) {
                mockedFactory.when(() -> AIServiceFactory.getProvider(AIServiceType.OPENAI)).thenReturn(mockProvider);
                mockedFactory.when(() -> AIServiceFactory.getProvider(AIServiceType.GEMINI)).thenReturn(null);
                
                // Act & Assert
                assertThat(AIServiceFactory.getProvider(AIServiceType.OPENAI)).isEqualTo(mockProvider);
                assertThat(AIServiceFactory.getProvider(AIServiceType.GEMINI)).isNull();
            }
        }
        
        @Test
        @DisplayName("should mock factory provider existence check")
        void shouldMockFactoryProviderExistenceCheck() {
            // Arrange
            try (var mockedFactory = org.mockito.Mockito.mockStatic(AIServiceFactory.class)) {
                mockedFactory.when(() -> AIServiceFactory.hasProvider(AIServiceType.OPENAI)).thenReturn(true);
                mockedFactory.when(() -> AIServiceFactory.hasProvider(AIServiceType.GEMINI)).thenReturn(false);
                
                // Act & Assert
                assertThat(AIServiceFactory.hasProvider(AIServiceType.OPENAI)).isTrue();
                assertThat(AIServiceFactory.hasProvider(AIServiceType.GEMINI)).isFalse();
            }
        }
        
        @Test
        @DisplayName("should mock factory registered service types")
        void shouldMockFactoryRegisteredServiceTypes() {
            // Arrange
            AIServiceType[] expectedTypes = {AIServiceType.OPENAI, AIServiceType.GEMINI};
            try (var mockedFactory = org.mockito.Mockito.mockStatic(AIServiceFactory.class)) {
                mockedFactory.when(() -> AIServiceFactory.getRegisteredServiceTypes()).thenReturn(expectedTypes);
                
                // Act
                AIServiceType[] result = AIServiceFactory.getRegisteredServiceTypes();
                
                // Assert
                assertThat(result).isEqualTo(expectedTypes);
                assertThat(result).hasSize(2);
                assertThat(result).contains(AIServiceType.OPENAI, AIServiceType.GEMINI);
            }
        }
    }
    
    @Nested
    @DisplayName("SecureAPIKeyManager Testing")
    class SecureAPIKeyManagerTesting {
        
        @Test
        @DisplayName("should mock API key retrieval")
        void shouldMockAPIKeyRetrieval() {
            // Arrange
            try (var mockedKeyManager = org.mockito.Mockito.mockStatic(SecureAPIKeyManager.class)) {
                mockedKeyManager.when(() -> SecureAPIKeyManager.getAPIKey(AIServiceType.OPENAI)).thenReturn("openai-key");
                mockedKeyManager.when(() -> SecureAPIKeyManager.getAPIKey(AIServiceType.GEMINI)).thenReturn("gemini-key");
                
                // Act & Assert
                assertThat(SecureAPIKeyManager.getAPIKey(AIServiceType.OPENAI)).isEqualTo("openai-key");
                assertThat(SecureAPIKeyManager.getAPIKey(AIServiceType.GEMINI)).isEqualTo("gemini-key");
            }
        }
        
        @Test
        @DisplayName("should handle null API key")
        void shouldHandleNullAPIKey() {
            // Arrange
            try (var mockedKeyManager = org.mockito.Mockito.mockStatic(SecureAPIKeyManager.class)) {
                mockedKeyManager.when(() -> SecureAPIKeyManager.getAPIKey(AIServiceType.OPENAI)).thenReturn(null);
                
                // Act & Assert
                assertThat(SecureAPIKeyManager.getAPIKey(AIServiceType.OPENAI)).isNull();
            }
        }
    }
    
    @Nested
    @DisplayName("AIAnalysisResult Testing")
    class AIAnalysisResultTesting {
        
        @Test
        @DisplayName("should create analysis result with basic constructor")
        void shouldCreateAnalysisResultWithBasicConstructor() {
            // Arrange
            String analysis = "Test analysis result";
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
            String analysis = "Test analysis result";
            String prompt = "Test prompt";
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
        @DisplayName("should handle error analysis result")
        void shouldHandleErrorAnalysisResult() {
            // Arrange
            String errorAnalysis = "AI Analysis Failed\n\nError Details\n\nThe AI service encountered an error";
            
            // Act
            AIAnalysisResult result = new AIAnalysisResult(
                errorAnalysis,
                AIServiceType.GEMINI,
                "error",
                System.currentTimeMillis(),
                0L
            );
            
            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getAnalysis()).contains("AI Analysis Failed");
            assertThat(result.getModelId()).isEqualTo("error");
            assertThat(result.getProcessingTimeMs()).isEqualTo(0L);
        }
    }
}
