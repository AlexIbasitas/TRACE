### Title: AssertionFailure (JUnit)
**Summary**: Test assertion mismatch.
**Root Causes**:
- Expected vs actual difference
- Incorrect expected value
**Resolution Steps**:
1. Verify test expectation
2. Fix either code or test logic
---
### Title: NullPointerException in @BeforeEach (JUnit)
**Summary**: Setup step references null.
**Root Causes**:
- Field not initialized
- Incorrect use of static/shared state
**Resolution Steps**:
1. Initialize required fields
2. Avoid static state across tests
---
### Title: Test Timeout Failure (JUnit)
**Summary**: Test exceeded allowed time.
**Root Causes**:
- Long-running operations
**Resolution Steps**:
1. Use @Timeout annotation
2. Split test logic into smaller units
---
### Title: Test Passes in Debug but Fails in Run (JUnit)
**Summary**: Race or timing impacts logic.
**Root Causes**:
- Uninitialized or concurrency issue
**Resolution Steps**:
1. Add synchronization or waits
2. Use consistent environment between run & debug
---
### Title: Using assertThrows vs ExpectedException (JUnit)
**Summary**: Legacy exception assertions are error-prone.
**Root Causes**:
- Using JUnit 4-style annotations or try/catch
**Resolution Steps**:
1. Use assertThrows in JUnit5
2. Use assertThatThrownBy for detailed checks
---
### Title: Misuse of Assert.fail() (JUnit)
**Summary**: Tests fail prematurely without assertion context.
**Root Causes**:
- Using fail() instead of proper assertion
**Resolution Steps**:
1. Prefer assertEquals or assertThrows
2. Use fail() only for unreachable code checks
---
### Title: Test Depends on External Resource (JUnit)
**Summary**: Failure when external dependency unavailable.
**Root Causes**:
- DB or service connectivity
**Resolution Steps**:
1. Use mocking/stubbing frameworks like Mockito
2. Isolate test from external systems
---
### Title: Shared State Between Tests (JUnit)
**Summary**: Tests affect each other due to shared state.
**Root Causes**:
- Static fields or reused objects
**Resolution Steps**:
1. Use fresh instances per test
2. Use @BeforeEach to reset state
---
### Title: Incorrect Use of Test Lifecycle (@BeforeAll, @AfterAll)
**Summary**: Lifecycle methods misused across tests.
**Root Causes**:
- Global setup not appropriate for all cases
**Resolution Steps**:
1. Use BeforeEach when test-specific setup needed
2. Use BeforeAll only for heavy one-time setups
### Title: Ignoring Exceptions in Tests (JUnit)
**Summary**: Exceptions swallowed, test passes incorrectly.
**Root Causes**:
- Try/catch without rethrowing
- Missing assertions inside catch block
**Resolution Steps**:
1. Avoid empty catch blocks in tests
2. Use `assertThrows` to explicitly verify exceptions
