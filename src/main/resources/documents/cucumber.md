### Title: Undefined Step (Cucumber)
**Summary**: No matching step definition found.
**Root Causes**:
- Regex mismatch
- Typos in feature vs code
**Resolution Steps**:
1. Verify step text matches definition regex
2. Refresh glue path & rebuild
---
### Title: Ambiguous Step Definition (Cucumber)
**Summary**: Multiple definitions match a single step.
**Root Causes**:
- Duplicate regex patterns
**Resolution Steps**:
1. Remove or consolidate defs
2. Make regex patterns more specific
---
### Title: Hook Execution Failed (Cucumber)
**Summary**: Error within @Before/@After hooks.
**Root Causes**:
- Unhandled exceptions in setup/teardown
**Resolution Steps**:
1. Add try-catch in hook code
2. Validate setup state
---
### Title: Retry on Flaky Step (Cucumber)
**Summary**: Step fails intermittently, passes on retry.
**Root Causes**:
- Race conditions or timing issues
**Resolution Steps**:
1. Enable retry flags
2. Add explicit waits around async behavior
---
### Title: Missing Feature File (Cucumber)
**Summary**: Feature path not found or unnamed.
**Root Causes**:
- Incorrect file path or package
- Feature file not recognized
**Resolution Steps**:
1. Validate feature location and extension
2. Rebuild test runner config
---
### Title: Data Table Mismatch (Cucumber)
**Summary**: Step table format doesn't match method signature.
**Root Causes**:
- Incorrect column count
- Missing header mapping
**Resolution Steps**:
1. Align table columns with method params
2. Add headers or DataTable mappings
---
### Title: Scenario Outline Parameter Missing (Cucumber)
**Summary**: Placeholder in outline not provided in example row.
**Root Causes**:
- Case mismatch in headers
- Missing column
**Resolution Steps**:
1. Ensure example row includes all placeholders
2. Adjust header-case matching
---
### Title: @Given vs @When Annotation Error
**Summary**: Using wrong annotation for step type.
**Root Causes**:
- Cucumber distinguishes Given/When/Then semantics
**Resolution Steps**:
1. Use correct annotation per step
2. Update step regex accordingly
---
### Title: Background Step Not Running (Cucumber)
**Summary**: Background not executed before each scenario.
**Root Causes**:
- Indentation or formatting error
**Resolution Steps**:
1. Check background indentation
2. Ensure syntax matches Gherkin spec
---
### Title: Tag Expression Failure (Cucumber)
**Summary**: Tests filtered out by tag expressions.
**Root Causes**:
- Incorrect tag syntax
**Resolution Steps**:
1. Validate tag logic (AND/OR)
2. Run with simple include tags to debug