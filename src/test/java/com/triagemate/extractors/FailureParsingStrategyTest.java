package com.triagemate.extractors;

import com.triagemate.models.FailureInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the FailureParsingStrategy interface.
 * 
 * <p>These tests verify the contract defined by the FailureParsingStrategy interface
 * and ensure that implementations follow the expected behavior. The tests use a
 * mock implementation to validate the interface contract.</p>
 */
@DisplayName("FailureParsingStrategy Interface")
class FailureParsingStrategyTest {
    
    /**
     * Mock implementation of FailureParsingStrategy for testing the interface contract.
     */
    private static class MockFailureParsingStrategy implements FailureParsingStrategy {
        private final boolean canHandleResult;
        private final FailureInfo parseResult;
        private final int priority;
        private final String strategyName;
        private final RuntimeException parseException;
        
        public MockFailureParsingStrategy(boolean canHandleResult, FailureInfo parseResult, 
                                        int priority, String strategyName) {
            this.canHandleResult = canHandleResult;
            this.parseResult = parseResult;
            this.priority = priority;
            this.strategyName = strategyName;
            this.parseException = null;
        }
        
        public MockFailureParsingStrategy(boolean canHandleResult, RuntimeException parseException,
                                        int priority, String strategyName) {
            this.canHandleResult = canHandleResult;
            this.parseResult = null;
            this.priority = priority;
            this.strategyName = strategyName;
            this.parseException = parseException;
        }
        
        @Override
        public boolean canHandle(String testOutput) {
            return canHandleResult;
        }
        
        @Override
        public FailureInfo parse(String testOutput) {
            if (testOutput == null) {
                throw new IllegalArgumentException("testOutput cannot be null");
            }
            if (parseException != null) {
                throw parseException;
            }
            return parseResult;
        }
        
        @Override
        public int getPriority() {
            return priority;
        }
        
        @Override
        public String getStrategyName() {
            return strategyName;
        }
    }
    
    @Nested
    @DisplayName("canHandle method")
    class CanHandleMethod {
        
        @Test
        @DisplayName("should return true when strategy can handle the output")
        void shouldReturnTrueWhenStrategyCanHandleOutput() {
            // Arrange
            FailureInfo mockResult = new FailureInfo.Builder()
                .withErrorMessage("Test error")
                .build();
            FailureParsingStrategy strategy = new MockFailureParsingStrategy(true, mockResult, 100, "TestStrategy");
            
            // Act
            boolean result = strategy.canHandle("some test output");
            
            // Assert
            assertTrue(result, "Strategy should return true when it can handle the output");
        }
        
        @Test
        @DisplayName("should return false when strategy cannot handle the output")
        void shouldReturnFalseWhenStrategyCannotHandleOutput() {
            // Arrange
            FailureInfo mockResult = new FailureInfo.Builder()
                .withErrorMessage("Test error")
                .build();
            FailureParsingStrategy strategy = new MockFailureParsingStrategy(false, mockResult, 100, "TestStrategy");
            
            // Act
            boolean result = strategy.canHandle("some test output");
            
            // Assert
            assertFalse(result, "Strategy should return false when it cannot handle the output");
        }
        
        @Test
        @DisplayName("should handle null test output gracefully")
        void shouldHandleNullTestOutputGracefully() {
            // Arrange
            FailureInfo mockResult = new FailureInfo.Builder()
                .withErrorMessage("Test error")
                .build();
            FailureParsingStrategy strategy = new MockFailureParsingStrategy(false, mockResult, 100, "TestStrategy");
            
            // Act & Assert
            assertDoesNotThrow(() -> strategy.canHandle(null), 
                "Strategy should handle null input gracefully");
        }
        
        @Test
        @DisplayName("should handle empty test output gracefully")
        void shouldHandleEmptyTestOutputGracefully() {
            // Arrange
            FailureInfo mockResult = new FailureInfo.Builder()
                .withErrorMessage("Test error")
                .build();
            FailureParsingStrategy strategy = new MockFailureParsingStrategy(false, mockResult, 100, "TestStrategy");
            
            // Act & Assert
            assertDoesNotThrow(() -> strategy.canHandle(""), 
                "Strategy should handle empty input gracefully");
        }
    }
    
    @Nested
    @DisplayName("parse method")
    class ParseMethod {
        
        @Test
        @DisplayName("should return FailureInfo when parsing succeeds")
        void shouldReturnFailureInfoWhenParsingSucceeds() {
            // Arrange
            FailureInfo expectedResult = new FailureInfo.Builder()
                .withErrorMessage("Test error")
                .withAssertionType("TEST")
                .build();
            FailureParsingStrategy strategy = new MockFailureParsingStrategy(true, expectedResult, 100, "TestStrategy");
            
            // Act
            FailureInfo result = strategy.parse("some test output");
            
            // Assert
            assertNotNull(result, "Parse method should return a non-null FailureInfo");
            assertEquals(expectedResult, result, "Parse method should return the expected FailureInfo");
        }
        
        @Test
        @DisplayName("should throw RuntimeException when parsing fails")
        void shouldThrowRuntimeExceptionWhenParsingFails() {
            // Arrange
            RuntimeException expectedException = new RuntimeException("Parsing failed");
            FailureParsingStrategy strategy = new MockFailureParsingStrategy(true, expectedException, 100, "TestStrategy");
            
            // Act & Assert
            RuntimeException thrown = assertThrows(RuntimeException.class, 
                () -> strategy.parse("some test output"),
                "Parse method should throw RuntimeException when parsing fails");
            
            assertEquals("Parsing failed", thrown.getMessage(), 
                "Exception should have the expected message");
        }
        
        @Test
        @DisplayName("should throw IllegalArgumentException when testOutput is null")
        void shouldThrowIllegalArgumentExceptionWhenTestOutputIsNull() {
            // Arrange
            FailureInfo mockResult = new FailureInfo.Builder()
                .withErrorMessage("Test error")
                .build();
            FailureParsingStrategy strategy = new MockFailureParsingStrategy(true, mockResult, 100, "TestStrategy");
            
            // Act & Assert
            assertThrows(IllegalArgumentException.class, 
                () -> strategy.parse(null),
                "Parse method should throw IllegalArgumentException when testOutput is null");
        }
    }
    
    @Nested
    @DisplayName("getPriority method")
    class GetPriorityMethod {
        
        @Test
        @DisplayName("should return the correct priority value")
        void shouldReturnCorrectPriorityValue() {
            // Arrange
            FailureInfo mockResult = new FailureInfo.Builder()
                .withErrorMessage("Test error")
                .build();
            int expectedPriority = 100;
            FailureParsingStrategy strategy = new MockFailureParsingStrategy(true, mockResult, expectedPriority, "TestStrategy");
            
            // Act
            int result = strategy.getPriority();
            
            // Assert
            assertEquals(expectedPriority, result, "Priority should match the expected value");
        }
        
        @Test
        @DisplayName("should return different priority values for different strategies")
        void shouldReturnDifferentPriorityValuesForDifferentStrategies() {
            // Arrange
            FailureInfo mockResult = new FailureInfo.Builder()
                .withErrorMessage("Test error")
                .build();
            FailureParsingStrategy highPriorityStrategy = new MockFailureParsingStrategy(true, mockResult, 100, "HighPriorityStrategy");
            FailureParsingStrategy lowPriorityStrategy = new MockFailureParsingStrategy(true, mockResult, 10, "LowPriorityStrategy");
            
            // Act
            int highPriority = highPriorityStrategy.getPriority();
            int lowPriority = lowPriorityStrategy.getPriority();
            
            // Assert
            assertTrue(highPriority > lowPriority, 
                "High priority strategy should have higher priority value than low priority strategy");
        }
    }
    
    @Nested
    @DisplayName("getStrategyName method")
    class GetStrategyNameMethod {
        
        @Test
        @DisplayName("should return the correct strategy name")
        void shouldReturnCorrectStrategyName() {
            // Arrange
            FailureInfo mockResult = new FailureInfo.Builder()
                .withErrorMessage("Test error")
                .build();
            String expectedName = "TestStrategy";
            FailureParsingStrategy strategy = new MockFailureParsingStrategy(true, mockResult, 100, expectedName);
            
            // Act
            String result = strategy.getStrategyName();
            
            // Assert
            assertEquals(expectedName, result, "Strategy name should match the expected value");
        }
        
        @Test
        @DisplayName("should return non-null strategy name")
        void shouldReturnNonNullStrategyName() {
            // Arrange
            FailureInfo mockResult = new FailureInfo.Builder()
                .withErrorMessage("Test error")
                .build();
            FailureParsingStrategy strategy = new MockFailureParsingStrategy(true, mockResult, 100, "TestStrategy");
            
            // Act
            String result = strategy.getStrategyName();
            
            // Assert
            assertNotNull(result, "Strategy name should not be null");
        }
        
        @Test
        @DisplayName("should return non-empty strategy name")
        void shouldReturnNonEmptyStrategyName() {
            // Arrange
            FailureInfo mockResult = new FailureInfo.Builder()
                .withErrorMessage("Test error")
                .build();
            FailureParsingStrategy strategy = new MockFailureParsingStrategy(true, mockResult, 100, "TestStrategy");
            
            // Act
            String result = strategy.getStrategyName();
            
            // Assert
            assertFalse(result.isEmpty(), "Strategy name should not be empty");
        }
    }
    
    @Nested
    @DisplayName("Interface contract validation")
    class InterfaceContractValidation {
        
        @Test
        @DisplayName("should maintain consistent behavior across multiple calls")
        void shouldMaintainConsistentBehaviorAcrossMultipleCalls() {
            // Arrange
            FailureInfo mockResult = new FailureInfo.Builder()
                .withErrorMessage("Test error")
                .build();
            FailureParsingStrategy strategy = new MockFailureParsingStrategy(true, mockResult, 100, "TestStrategy");
            String testOutput = "some test output";
            
            // Act
            boolean canHandle1 = strategy.canHandle(testOutput);
            boolean canHandle2 = strategy.canHandle(testOutput);
            int priority1 = strategy.getPriority();
            int priority2 = strategy.getPriority();
            String name1 = strategy.getStrategyName();
            String name2 = strategy.getStrategyName();
            
            // Assert
            assertEquals(canHandle1, canHandle2, "canHandle should return consistent results");
            assertEquals(priority1, priority2, "getPriority should return consistent results");
            assertEquals(name1, name2, "getStrategyName should return consistent results");
        }
        
        @Test
        @DisplayName("should handle edge cases gracefully")
        void shouldHandleEdgeCasesGracefully() {
            // Arrange
            FailureInfo mockResult = new FailureInfo.Builder()
                .withErrorMessage("Test error")
                .build();
            FailureParsingStrategy strategy = new MockFailureParsingStrategy(false, mockResult, 100, "TestStrategy");
            
            // Act & Assert
            assertDoesNotThrow(() -> {
                strategy.canHandle("");
                strategy.canHandle("   ");
                strategy.canHandle("\n\t\r");
                strategy.getPriority();
                strategy.getStrategyName();
            }, "Strategy should handle edge cases gracefully");
        }
    }
} 