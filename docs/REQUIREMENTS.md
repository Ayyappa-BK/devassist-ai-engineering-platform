# Requirement coverage

| # | Requirement | Implementation / evidence |
| --- | --- | --- |
| 1 | Spring Boot backend | backend, Java 21 / Spring Boot 3.5.16 |
| 2 | React / TypeScript | frontend, production build and client test |
| 3 | Secure ingestion | RepositoryService; read-only administrative mount, bounded safe Java/Markdown reads |
| 4 | Java analysis | JavaParser configured for Java 21, AST method metadata and quality heuristics |
| 5 | Explanations | explain task, deterministic demo and live LLM adapter |
| 6 | Unit-test suggestions | tests task, JUnit boundary-case guidance |
| 7 | Code quality | quality task plus AST/string heuristics |
| 8 | Documentation | docs task, reviewable Markdown draft |
| 9 | PR summaries | review task comparing supplied before/after snapshots; no GitHub PR fetch |
| 10 | Approved-document RAG | SHA-256 approvals, same-repository context, chunking/ranking/citations |
| 11 | Human review | Atomic PENDING → APPROVED/REJECTED transitions and approval-gated export |
| 12 | Injection defenses | Input/output screening, untrusted evidence boundary, fixed policy, no tools |
| 13 | Repository authorization | Operator authentication and administrator-selected repository mount |
| 14 | Secret avoidance | Whole-request secret screening before model call; live mode opt-in; heuristic limitations documented |
| 15 | Validation / limiting | Bean/Pydantic validation, size bounds and 60 authenticated requests/minute |
| 16 | Logs / tracing | Structured JSON and correlated server-generated trace IDs |
| 17 | Unit / integration tests | JUnit, pytest, Vitest, API smoke, Playwright test |
| 18 | Evaluation data | Versioned JSONL dataset across tasks and adversarial examples |
| 19 | Quality / latency | Regression scoring, per-case timings and percentile report; human quality rubric |
| 20 | Compose | Four services, PostgreSQL volume, network separation, non-root app containers |
| 21 | CI/CD | Component/full-stack CI and CI-gated GHCR release images |
| 22 | Documentation | Architecture, security, API, operations, evaluation and verification records |
| 23 | Demonstration projects | Synthetic billing and inventory Java repositories with contracts and before snapshot |
| 24 | End-to-end verification | Five-task cross-service smoke passed using H2; UI approval gate observed; Docker/PostgreSQL/standalone-browser/hosted-CI checks pending as documented |

Publication fallback: exact repository creation and CI-watch commands are in PUBLISH.md. No hosted CI pass is asserted without a run.
