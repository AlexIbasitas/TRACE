### Title: AssertionFailure (JUnit)
**Summary**: Test assertion mismatch.
**Root Causes**:
- Expected vs actual difference
- Incorrect expected value
**Resolution Steps**:
1. Verify test expectation
2. Fix either code or test logic  [oai_citation:19‡GitHub](https://github.com/junit-team/junit5/issues/2723?utm_source=chatgpt.com) [oai_citation:20‡arXiv](https://arxiv.org/abs/2401.15788?utm_source=chatgpt.com) [oai_citation:21‡PSI](https://www.thepsi.com/what-are-the-common-selenium-exceptions-and-how-to-handle-them/?utm_source=chatgpt.com) [oai_citation:22‡Plantura](https://plantura.garden/uk/vegetables/cucumbers/cucumber-diseases?utm_source=chatgpt.com) [oai_citation:23‡Java Guides](https://www.javaguides.net/2018/08/junit-assertfail-method-example.html?utm_source=chatgpt.com) [oai_citation:24‡YouTube](https://www.youtube.com/watch?v=f5_e1XPHCrQ&utm_source=chatgpt.com) [oai_citation:25‡Better Homes & Gardens](https://www.bhg.com/cucumbers-are-yellow-7555779?utm_source=chatgpt.com) [oai_citation:26‡BrowserStack](https://www.browserstack.com/guide/exceptions-in-selenium-webdriver?utm_source=chatgpt.com) [oai_citation:27‡GeeksforGeeks](https://www.geeksforgeeks.org/advance-java/difference-between-failure-and-error-in-junit/?utm_source=chatgpt.com)
---
### Title: NullPointerException in @BeforeEach (JUnit)
**Summary**: Setup step references null.
**Root Causes**:
- Field not initialized
- Incorrect use of static/shared state
**Resolution Steps**:
1. Initialize required fields
2. Avoid static state across tests  [oai_citation:28‡Selenium](https://www.selenium.dev/documentation/webdriver/troubleshooting/errors/?utm_source=chatgpt.com)
---
### Title: Test Timeout Failure (JUnit)
**Summary**: Test exceeded allowed time.
**Root Causes**:
- Long-running operations
**Resolution Steps**:
1. Use @Timeout annotation
2. Split test logic into smaller units  [oai_citation:29‡Programmingempire](https://www.programmingempire.com/troubleshooting-junit-tests/?utm_source=chatgpt.com) [oai_citation:30‡Reddit](https://www.reddit.com/r/java/comments/7487nc/what_is_the_most_common_unit_testing_mistake_you/?utm_source=chatgpt.com)
---
### Title: Test Passes in Debug but Fails in Run (JUnit)
**Summary**: Race or timing impacts logic.
**Root Causes**:
- Uninitialized or concurrency issue
**Resolution Steps**:
1. Add synchronization or waits
2. Use consistent environment between run & debug  [oai_citation:31‡Eclipse Foundation](https://www.eclipse.org/forums/index.php/t/156167/?utm_source=chatgpt.com)
---
### Title: Using assertThrows vs ExpectedException (JUnit)
**Summary**: Legacy exception assertions are error-prone.
**Root Causes**:
- Using JUnit 4-style annotations or try/catch
**Resolution Steps**:
1. Use assertThrows in JUnit5
2. Use assertThatThrownBy for detailed checks  [oai_citation:32‡Reddit](https://www.reddit.com/r/java/comments/7487nc/what_is_the_most_common_unit_testing_mistake_you/?utm_source=chatgpt.com)
---
### Title: Misuse of Assert.fail() (JUnit)
**Summary**: Tests fail prematurely without assertion context.
**Root Causes**:
- Using fail() instead of proper assertion
**Resolution Steps**:
1. Prefer assertEquals or assertThrows
2. Use fail() only for unreachable code checks  [oai_citation:33‡Java Guides](https://www.javaguides.net/2018/08/junit-assertfail-method-example.html?utm_source=chatgpt.com) [oai_citation:34‡Reddit](https://www.reddit.com/r/java/comments/7487nc/what_is_the_most_common_unit_testing_mistake_you/?utm_source=chatgpt.com)
---
### Title: Test Depends on External Resource (JUnit)
**Summary**: Failure when external dependency unavailable.
**Root Causes**:
- DB or service connectivity
**Resolution Steps**:
1. Use mocking/stubbing frameworks like Mockito
2. Isolate test from external systems  [oai_citation:35‡Programmingempire](https://www.programmingempire.com/troubleshooting-junit-tests/?utm_source=chatgpt.com)
---
### Title: Shared State Between Tests (JUnit)
**Summary**: Tests affect each other due to shared state.
**Root Causes**:
- Static fields or reused objects
**Resolution Steps**:
1. Use fresh instances per test
2. Use @BeforeEach to reset state  [oai_citation:36‡Code Ranch](https://coderanch.com/t/94915/engineering/common-mistake-JUnit?utm_source=chatgpt.com)
---
### Title: Incorrect Use of Test Lifecycle (@BeforeAll, @AfterAll)
**Summary**: Lifecycle methods misused across tests.
**Root Causes**:
- Global setup not appropriate for all cases
**Resolution Steps**:
1. Use BeforeEach when test-specific setup needed
2. Use BeforeAll only for heavy one-time setups