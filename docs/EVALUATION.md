# Evaluation methodology

`evals/dataset.jsonl` contains versioned examples for explanations, JUnit suggestions, quality findings, documentation, PR snapshot summaries, retrieval citations, injection and secret rejection. `python evals/evaluate.py` records per-case status and latency, pass rate, median and nearest-rank p95. Failed assertions return nonzero for CI. Component tests cover invalid tasks, oversized source, authorization, provider configuration failure, unsafe output, traversal, symlinks and approval transitions. The smoke script traverses the real API/worker/database; Playwright exercises the browser.

Demo evaluations are deterministic regression checks. Their keyword checks and millisecond timings measure local plumbing, not LLM quality or network latency. Do not use them to claim generation accuracy. Live-mode runs require opt-in credentials and incur model costs; the same small dataset is a starting point, not a statistically representative benchmark.

## Human rubric for live outputs

Sample at least 30 representative files per language/framework category, with held-out projects and confidential data removed. Have two engineers independently score each draft 0–4 for factual correctness, source grounding, task completeness, actionable specificity and safety. Report agreement, adjudicated scores and confidence intervals. Assess citations against actual approved text, not their mere presence. For generated tests, execute reviewed tests in a resource-isolated environment and measure compilation rate, assertion validity and mutation/branch coverage. This platform never executes model code itself.

For PR review, label actual changed lines and expected defects; score precision and recall with false positives separately. For documentation, compare claimed contracts and exceptions to source and maintained docs. For prompt injection, include obfuscated, multilingual, embedded-comment and retrieved-document attacks. For secrets, include synthetic credential formats, split literals and false-positive benign identifiers; never put real secrets in the dataset.

Measure end-to-end p50/p95 and failures under stated concurrency, cache, provider/model version, source/context sizes, timeout and hardware conditions. Keep rejected requests separate from successful generations. Reviewers approve deployment only after project-specific quality and safety thresholds are met. No universal quality threshold is implied by the demo baseline.
