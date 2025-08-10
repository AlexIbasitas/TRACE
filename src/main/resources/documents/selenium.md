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
3. Retry locating element before interacting
---
### Title: StaleElementReferenceException (Selenium)
**Summary**: Element is detached from DOM.
**Root Causes**:
- DOM updated between find & use
- Page re-render
**Resolution Steps**:
1. Re-find element before use
2. Wait for stability/reload
3. Avoid long-lived WebElement refs
---
### Title: InvalidSessionIdException (Selenium)
**Summary**: Session is invalid or ended.
**Root Causes**:
- Browser closed or crashed
- Session timeout
**Resolution Steps**:
1. Restart WebDriver session
2. Handle session cleanup on failure
---
### Title: NoSuchFrameException (Selenium)
**Summary**: Frame or window not found.
**Root Causes**:
- Switching to wrong frame name
- Frame not yet present
**Resolution Steps**:
1. Validate frame locator
2. Wait for frame availability
3. Use switchTo().frame only after frame load
---
### Title: NoSuchWindowException (Selenium)
**Summary**: Attempted to focus non-existing window.
**Root Causes**:
- Window already closed
- Incorrect window handle
**Resolution Steps**:
1. Use correct window handle list
2. Validate handle before switching
---
### Title: NoAlertPresentException (Selenium)
**Summary**: Alert not found when attempting switch.
**Root Causes**:
- Alert not generated yet
- Wrong timing
**Resolution Steps**:
1. Use wait for alert presence
2. Validate triggering condition first
---
### Title: TimeoutException vs NoSuchElementException (Selenium)
**Summary**: Wait vs find behavior can cause confusion.
**Root Causes**:
- Using wrong wait condition
**Resolution Steps**:
1. Use presenceOfElement vs visibility/wait correctly
2. Adjust wait strategies