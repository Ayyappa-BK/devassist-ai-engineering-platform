# Verification record — 2026-10-16

## Executed successfully

| Check | Result |
| --- | --- |
| Java 21 / Spring Boot 3.5.16 Maven verify | 7 tests, zero failures/errors |
| Python worker pytest | 14 tests passed |
| Frontend TypeScript + production Vite build | Passed |
| Frontend Vitest | 1 test passed |
| Deterministic evaluation dataset | 9/9 cases passed; timings in evaluation-report.json |
| Real HTTP API → Python worker → H2 PostgreSQL compatibility database | All five tasks passed; source analysis, approved-context citations, pending export denial, approval/export and rejection verified |
| App browser interaction | Repository connection, Java analysis, documentation approval, PENDING generation, disabled export, APPROVED state and enabled export observed |
| YAML parsing | Compose and both GitHub workflows parsed |
| npm audit | Zero known vulnerabilities reported for resolved dependency tree |
| pip-audit | Zero known vulnerabilities reported for resolved worker requirements |

See [smoke-report.json](smoke-report.json), [evaluation-report.json](evaluation-report.json), [dependency-audit.json](dependency-audit.json) and [workspace screenshot](workspace.png). Smoke task timings include review/export API calls, not just generation. Evaluation timings are deterministic local demo measurements, not model-quality or provider-latency claims. Tests used Java 21, Python 3.13 and Node 24 locally; CI targets Java 21, Python 3.12 and Node 22.

## Pending or blocked

- **GitHub publication and hosted CI:** saved GitHub authentication was invalid. No hosted repository URL or green CI run is claimed. Follow PUBLISH.md to publish and wait for checks.
- **Docker Compose runtime and image builds:** Docker is not installed locally. Configuration was inspected and YAML parsed; this does not establish container correctness.
- **Real PostgreSQL execution:** native PostgreSQL initdb failed because sandbox permissions prohibit shared-memory creation. H2 compatibility tests are not proof of PostgreSQL behavior. CI runs real PostgreSQL 17 through Compose.
- **Standalone Playwright run:** Chromium launch was blocked by macOS sandbox Mach-port permissions. The app browser verified the UI flow as listed above. Its attempted download event wait timed out, so browser-download completion is unverified; the approved export API was verified. The Playwright test remains in CI for a full browser check.
- **Live model semantic quality:** no real model credential was supplied. Live adapter policy, endpoint, tool absence and output/error boundaries are tested through fixtures; actual provider generation and quality need an opt-in evaluation run.

This record intentionally distinguishes implemented checks from checks executed in this environment. The project is a single-operator self-hosted implementation with heuristic secret/injection filtering; see SECURITY.md before using confidential source or enabling external models.
