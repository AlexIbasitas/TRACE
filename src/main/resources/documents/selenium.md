### Title: NoSuchElementException (Selenium)
**Summary**: Locator failed to find element in DOM.
**Root Causes**:
- Wrong selector (typo, wrong scope)
- Element not rendered yet
**Resolution Steps**:
1. Verify selector correctness
2. Add explicit wait (e.g. wait until presence)
3. Ensure element is in correct frame or page context
---
### Title: TimeoutException (Selenium)
**Summary**: Operation exceeded wait time.
**Root Causes**:
- Wait too short
- Page slow to load
**Resolution Steps**:
1. Increase timeout
2. Use fluent/exponential wait
3. Verify network conditions
---
### Title: ElementNotVisibleException (Selenium)
**Summary**: Element in DOM but not visible.
**Root Causes**:
- Element hidden via CSS
- Overlapping UI
**Resolution Steps**:
1. Wait until visible
2. Scroll element into view
3. Use correct locator
---
### Title: ElementNotInteractableException (Selenium)
**Summary**: Element cannot be interacted with.
**Root Causes**:
- Hidden or disabled element
- Wrong element type
**Resolution Steps**:
1. Use elementToBeClickable
2. Scroll into viewport
3. Retry locating element before interacting  [oai_citation:0‡PSI](https://www.thepsi.com/what-are-the-common-selenium-exceptions-and-how-to-handle-them/?utm_source=chatgpt.com) [oai_citation:1‡Epic Gardening](https://www.epicgardening.com/cucumber-problems/?utm_source=chatgpt.com) [oai_citation:2‡Selenium](https://www.selenium.dev/selenium/docs/api/py/common/selenium.common.exceptions.html?utm_source=chatgpt.com) [oai_citation:3‡Selenium](https://www.selenium.dev/documentation/webdriver/troubleshooting/errors/?utm_source=chatgpt.com) [oai_citation:4‡arXiv](https://arxiv.org/abs/2208.01106?utm_source=chatgpt.com)
---
### Title: StaleElementReferenceException (Selenium)
**Summary**: Element is detached from DOM.
**Root Causes**:
- DOM updated between find & use
- Page re-render
**Resolution Steps**:
1. Re-find element before use
2. Wait for stability/reload
3. Avoid long-lived WebElement refs  [oai_citation:5‡Selenium](https://www.selenium.dev/selenium/docs/api/py/common/selenium.common.exceptions.html?utm_source=chatgpt.com)
---
### Title: InvalidSessionIdException (Selenium)
**Summary**: Session is invalid or ended.
**Root Causes**:
- Browser closed or crashed
- Session timeout
**Resolution Steps**:
1. Restart WebDriver session
2. Handle session cleanup on failure  [oai_citation:6‡Selenium](https://www.selenium.dev/documentation/webdriver/troubleshooting/errors/?utm_source=chatgpt.com) [oai_citation:7‡BrowserStack](https://www.browserstack.com/guide/exceptions-in-selenium-webdriver?utm_source=chatgpt.com)
---
### Title: NoSuchFrameException (Selenium)
**Summary**: Frame or window not found.
**Root Causes**:
- Switching to wrong frame name
- Frame not yet present
**Resolution Steps**:
1. Validate frame locator
2. Wait for frame availability
3. Use switchTo().frame only after frame load  [oai_citation:8‡Java Guides](https://www.javaguides.net/2018/08/junit-assertfail-method-example.html?utm_source=chatgpt.com) [oai_citation:9‡PSI](https://www.thepsi.com/what-are-the-common-selenium-exceptions-and-how-to-handle-them/?utm_source=chatgpt.com) [oai_citation:10‡testgrid.io](https://testgrid.io/blog/how-to-handle-common-exceptions-in-selenium/?utm_source=chatgpt.com) [oai_citation:11‡Eclipse Foundation](https://www.eclipse.org/forums/index.php/t/156167/?utm_source=chatgpt.com)
---
### Title: NoSuchWindowException (Selenium)
**Summary**: Attempted to focus non-existing window.
**Root Causes**:
- Window already closed
- Incorrect window handle
**Resolution Steps**:
1. Use correct window handle list
2. Validate handle before switching  [oai_citation:12‡arXiv](https://arxiv.org/abs/2401.15788?utm_source=chatgpt.com) [oai_citation:13‡PSI](https://www.thepsi.com/what-are-the-common-selenium-exceptions-and-how-to-handle-them/?utm_source=chatgpt.com)
---
### Title: NoAlertPresentException (Selenium)
**Summary**: Alert not found when attempting switch.
**Root Causes**:
- Alert not generated yet
- Wrong timing
**Resolution Steps**:
1. Use wait for alert presence
2. Validate triggering condition first  [oai_citation:14‡The Spruce](https://www.thespruce.com/cucumber-problems-bacterial-wilt-1402985?utm_source=chatgpt.com) [oai_citation:15‡PSI](https://www.thepsi.com/what-are-the-common-selenium-exceptions-and-how-to-handle-them/?utm_source=chatgpt.com) [oai_citation:16‡Wikipedia](https://en.wikipedia.org/wiki/Gummy_stem_blight?utm_source=chatgpt.com)
---
### Title: TimeoutException vs NoSuchElementException (Selenium)
**Summary**: Wait vs find behavior can cause confusion.
**Root Causes**:
- Using wrong wait condition
**Resolution Steps**:
1. Use presenceOfElement vs visibility/wait correctly
2. Adjust wait strategies  [oai_citation:17‡testquality.com](https://testquality.com/different-types-of-selenium-webdriver-common-exceptions/?utm_source=chatgpt.com) [oai_citation:18‡BrowserStack](https://www.browserstack.com/guide/exceptions-in-selenium-webdriver?utm_source=chatgpt.com)