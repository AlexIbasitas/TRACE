### Title: Structured Triage Workflow
**Summary**: Recommended process for triaging test failures.
**Root Causes**:
- No formal workflow leads to missed diagnosis
**Resolution Steps**:
1. Reproduce failure manually
2. Examine logs and stack trace
3. Search for similar past failures
4. Check recent code changes
5. Retry or isolate test
---
### Title: Flaky vs True Failure Distinction
**Summary**: Determine if failure is intermittent or consistent.
**Root Causes**:
- No rerun logic to detect flakiness
**Resolution Steps**:
1. Rerun failed test immediately
2. Classify as flaky if passes on rerun
3. Mark or skip flaky tests in CI
---
### Title: Use of Logs and Stack Trace
**Summary**: Logs help identify root cause quickly.
**Root Causes**:
- Log stderr not captured
**Resolution Steps**:
1. Capture full stacktrace and logs
2. Highlight exception messages clearly
3. Attach relevant logs in summary prompt
---
### Title: Minimal Prompt Generation for AI
**Summary**: Provide only essential context to language model.
**Root Causes**:
- Full logs overloaded prompt tokens
**Resolution Steps**:
1. Extract failed step, exception, stack trace
2. Omit irrelevant logs
3. Structure prompt with bullet headers
---
### Title: Embedding Similar Cases for Context
**Summary**: Use past cases to enhance triage.
**Root Causes**:
- No reuse of prior knowledge
**Resolution Steps**:
1. Retrieve top-k similar failures
2. Inject them into prompt
3. Ask AI to produce summary + possible fix
---
### Title: Prune or Archive History
**Summary**: Keep corpus manageable and relevant.
**Root Causes**:
- Growing RAG corpus slows retrieval
**Resolution Steps**:
1. Remove records older than N entries or timeframe
2. Keep only high-frequency patterns
3. Archive older entries in separate store
---
### Title: Validate Embedding Quality
**Summary**: Ensure embeddings reflect document meaning.
**Root Causes**:
- Too generic or too short text
**Resolution Steps**:
1. Include title + summary in chunk
2. Keep each doc focused (~200 words)
3. Test similarity outputs manually
---
### Title: Regex-based Markdown Parsing
**Summary**: Predictable format aids ingestion scripts.
**Root Causes**:
- Inconsistent chunk delimiters
**Resolution Steps**:
1. Use `### Title:` boundary
2. Separate chunks with `---`
3. Require each section present
---
### Title: Prompt Hygiene and Truncation
**Summary**: Proper prompt sizes prevent OOM or truncation.
**Root Causes**:
- Too many similar cases or very long content in prompt
**Resolution Steps**:
1. Limit injected context to top‑k (e.g. 3)
2. Truncate case content if >500 tokens
3. Prioritize concise summaries
---
### Title: Privacy & Data Isolation
**Summary**: Keep user code/data private in RAG.
**Root Causes**:
- Adding internal user data into statically curated corpus
**Resolution Steps**:
1. Use only generic public‑domain docs
2. Never include user-specific stack trace into static corpus
3. Ensure embedding and retrieval logic sanitize sensitive text