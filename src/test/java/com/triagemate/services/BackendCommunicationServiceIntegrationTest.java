package com.triagemate.services;

import com.triagemate.models.FailureInfo;
import com.triagemate.models.GherkinScenarioInfo;
import com.triagemate.models.StepDefinitionInfo;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for BackendCommunicationService using a mock HTTP server.
 * 
 * <p>These tests verify the actual HTTP communication behavior without requiring
 * a real backend service. They test the complete request/response cycle and
 * various error scenarios.</p>
 */
class BackendCommunicationServiceIntegrationTest {

    private BackendCommunicationService service;
    private HttpServer mockServer;
    private int serverPort;

    @BeforeEach
    void setUp() throws IOException {
        service = new BackendCommunicationService();
        
        // Create mock HTTP server
        mockServer = HttpServer.create(new InetSocketAddress(0), 0);
        serverPort = mockServer.getAddress().getPort();
        mockServer.setExecutor(Executors.newFixedThreadPool(1));
        mockServer.start();
        
        // Set service to use mock server
        service.setBackendUrl("http://localhost:" + serverPort);
    }

    @AfterEach
    void tearDown() {
        if (mockServer != null) {
            mockServer.stop(0);
        }
    }

    @Test
    void testSuccessfulAnalysisRequest() throws Exception {
        // Setup mock server response
        mockServer.createContext("/api/v1/analyze", exchange -> {
            if ("POST".equals(exchange.getRequestMethod())) {
                // Verify request headers
                String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
                assertEquals("application/json", contentType);
                
                // Send successful response
                String responseBody = """
                    {
                        "analysis": "This appears to be a WebDriver element location issue",
                        "suggestions": ["Check element selector", "Add explicit wait"],
                        "confidence": "High",
                        "modelUsed": "GPT-4",
                        "processingTime": 1500,
                        "promptUsed": "Test prompt"
                    }
                    """;
                
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, responseBody.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBody.getBytes());
                }
            } else {
                exchange.sendResponseHeaders(405, -1); // Method not allowed
            }
        });

        // Create test data
        FailureInfo failureInfo = createTestFailureInfo();

        // Execute test
        BackendCommunicationService.AnalysisResponse response = service.analyzeFailure(failureInfo);

        // Verify response
        assertNotNull(response);
        assertEquals("This appears to be a WebDriver element location issue", response.getAnalysis());
        assertEquals(Arrays.asList("Check element selector", "Add explicit wait"), response.getSuggestions());
        assertEquals("High", response.getConfidence());
        assertEquals("GPT-4", response.getModelUsed());
        assertEquals(1500L, response.getProcessingTime());
    }

    @Test
    void testBackendErrorResponse() {
        // Setup mock server to return error
        mockServer.createContext("/api/v1/analyze", exchange -> {
            String errorBody = "{\"error\": \"Invalid request data\"}";
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(400, errorBody.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(errorBody.getBytes());
            }
        });

        // Create test data
        FailureInfo failureInfo = createTestFailureInfo();

        // Execute test and verify exception
        BackendCommunicationService.BackendCommunicationException exception = 
            assertThrows(BackendCommunicationService.BackendCommunicationException.class, 
                () -> service.analyzeFailure(failureInfo));
        
        // Fix: Check for the actual error message format from the logs
        String errorMessage = exception.getMessage();
        assertTrue(errorMessage.contains("400"), 
                  "Exception message should contain error status 400. Actual message: " + errorMessage);
    }

    @Test
    void testBackendUnavailable() {
        // Stop server to simulate backend being down
        mockServer.stop(0);

        // Create test data
        FailureInfo failureInfo = createTestFailureInfo();

        // Execute test and verify exception
        assertThrows(BackendCommunicationService.BackendCommunicationException.class, 
            () -> service.analyzeFailure(failureInfo));
    }

    @Test
    void testHealthCheckEndpoint() throws Exception {
        // Setup mock health endpoint
        mockServer.createContext("/api/v1/health", exchange -> {
            if ("GET".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, 0);
                exchange.close();
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        });

        // Test health check
        assertTrue(service.isBackendAvailable());
    }

    @Test
    void testHealthCheckEndpointUnavailable() {
        // Stop server to simulate backend being down
        mockServer.stop(0);

        // Test health check
        assertFalse(service.isBackendAvailable());
    }

    @Test
    void testAsyncAnalysisRequest() throws Exception {
        // Setup mock server response
        mockServer.createContext("/api/v1/analyze", exchange -> {
            String responseBody = """
                {
                    "analysis": "Async analysis result",
                    "suggestions": ["Async suggestion"],
                    "confidence": "Medium",
                    "modelUsed": "GPT-4",
                    "processingTime": 2000,
                    "promptUsed": "Async prompt"
                }
                """;
            
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBody.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBody.getBytes());
            }
        });

        // Create test data
        FailureInfo failureInfo = createTestFailureInfo();

        // Execute async test
        var future = service.analyzeFailureAsync(failureInfo);
        BackendCommunicationService.AnalysisResponse response = future.get();

        // Verify response
        assertNotNull(response);
        assertEquals("Async analysis result", response.getAnalysis());
        assertEquals(Arrays.asList("Async suggestion"), response.getSuggestions());
    }

    private FailureInfo createTestFailureInfo() {
        GherkinScenarioInfo scenarioInfo = new GherkinScenarioInfo.Builder()
            .withFeatureName("Login Feature")
            .withScenarioName("Successful Login")
            .withSteps(Arrays.asList("Given user is on login page", "When user enters credentials", "Then user should be logged in"))
            .withTags(Arrays.asList("@smoke", "@login"))
            .build();
            
        StepDefinitionInfo stepDefInfo = new StepDefinitionInfo.Builder()
            .withClassName("LoginStepDefinitions")
            .withMethodName("userEntersCredentials")
            .withStepPattern("user enters credentials")
            .withParameters(Arrays.asList("username", "password"))
            .withMethodText("@When(\"user enters credentials\")\npublic void userEntersCredentials(String username, String password) { }")
            .withSourceFilePath("/src/test/java/LoginStepDefinitions.java")
            .withLineNumber(25)
            .build();
            
        return new FailureInfo.Builder()
            .withScenarioName("Successful Login")
            .withFailedStepText("When user enters credentials")
            .withStackTrace("org.openqa.selenium.NoSuchElementException: Unable to locate element")
            .withSourceFilePath("/src/test/java/LoginStepDefinitions.java")
            .withLineNumber(25)
            .withStepDefinitionInfo(stepDefInfo)
            .withGherkinScenarioInfo(scenarioInfo)
            .withExpectedValue("true")
            .withActualValue("false")
            .withAssertionType("WEBDRIVER_ERROR")
            .withErrorMessage("Unable to locate element")
            .withParsingStrategy("WebDriverErrorStrategy")
            .withParsingTime(150L)
            .build();
    }
} 