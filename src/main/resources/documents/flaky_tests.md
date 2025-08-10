### Title: Flaky Test due to Timing (General)
**Summary**: Test sometimes fails, passes on retry.
**Root Causes**:
- Async behavior, network latency
**Resolution Steps**:
1. Add explicit wait or retry logic
2. Increase timeout margins
3. Log timing to diagnose
---
### Title: Environment-sensitive Failure (General)
**Summary**: Fails only on CI or specific machines.
**Root Causes**:
- Config differences
- Missing environment variable
**Resolution Steps**:
1. Sync environment configs
2. Standardize test environment variables
---
### Title: Data-dependent Flakiness (General)
**Summary**: Depends on seed data state.
**Root Causes**:
- Shared database or state not reset
**Resolution Steps**:
1. Reset database between tests
2. Use fixture isolation
---
### Title: Randomness-induced Flakiness (General)
**Summary**: Test uses random values causing unpredictability.
**Root Causes**:
- Uncontrolled random seeds
**Resolution Steps**:
1. Seed randomness for repeatability
2. Avoid random logic in assertion
---
### Title: Network Dependency in Test (General)
**Summary**: Fails due to network timeout or availability.
**Root Causes**:
- External API not mocked
**Resolution Steps**:
1. Mock network calls
2. Use local stub service
3. Add fallback assumptions
---
### Title: External Service Rate Limit (General)
**Summary**: Test fails due to throttling.
**Root Causes**:
- Calling service repeatedly in CI
**Resolution Steps**:
1. Add retry/backoff
2. Use mock services in CI
---
### Title: UI Flakiness (General)
**Summary**: UI tests fail intermittently due to rendering delays.
**Root Causes**:
- Element load timing mismatch
**Resolution Steps**:
1. Use explicit waits
2. Validate element readiness state
3. Separate UI from fast unit test logic
---
### Title: Threading Race Condition (General)
**Summary**: Failure due to concurrency behavior.
**Root Causes**:
- Non-deterministic thread scheduling
**Resolution Steps**:
1. Add synchronization or countdown latches
2. Use deterministic scheduling in tests
---
### Title: Port/Resource Conflict (General)
**Summary**: Tests crash when port or file resource in use.
**Root Causes**:
- Shared port or leftover resource
**Resolution Steps**:
1. Randomize ports per test
2. Clean up resources in teardown
---
### Title: Test Order Dependency (General)
**Summary**: Order affects test pass/fail.
**Root Causes**:
- Shared state or dependencies between tests
**Resolution Steps**:
1. Make each test independent
2. Randomize test execution order
3. Reset static data between tests