# Security controls and threat model

## Trust boundaries

The deployment operator controls repository mounts, environment credentials and approval decisions. Repositories and generated output are untrusted. The model has no tools, shell, database access, git credentials or write access. The browser is a display/review client. Never expose the development server publicly.

| Threat | Implemented control | Residual limit |
| --- | --- | --- |
| Unauthorized repository use | Constant-time bearer comparison; server-selected mount; repository name allowlist syntax; no remote cloning | Single operator token grants all mounted projects; not multi-tenant RBAC |
| Traversal / links / large files | Real-path containment, symlink checks on every path component, hidden-component rejection, .java/.md allowlist, 64 KiB files | Operator must provide immutable, trusted mount snapshots to prevent filesystem races and hard-link surprises |
| Request flooding | 60 requests/minute/operator, 1 MiB backend body cap including chunked requests, bounded worker body, network isolation | In-memory limit applies per backend process; deploy one replica or use central ingress enforcement |
| Credential leakage | Secret-pattern screening in backend and worker before external calls, output screening, no body logging, fixed HTTPS provider, redirects disabled | Pattern matching cannot identify every unknown secret; only externally approved, scrubbed snapshots should use live mode |
| Prompt injection | Suspicious instruction-pattern rejection, fixed system instructions, JSON evidence boundary, approved-doc hash checks, no tools | Detection is heuristic; semantic/obfuscated attacks can bypass it; model output remains untrusted |
| Unreviewed action | PENDING/APPROVED/REJECTED database transitions; export requires approval; no autonomous execution or commits | Human may approve incorrect output; same operator may generate and approve |
| Browser script injection | React text escaping, no raw HTML, NGINX CSP, memory-only credential, no-store API | Protect the host and browser; TLS required beyond localhost |
| Worker misuse | Independent worker credential; no published worker port in Compose | An operator with host access can access service credentials |
| Secret logging | JSON logs contain status/timing/task/count; validation errors return generic messages | Avoid enabling framework HTTP body/debug logging |

Supported secret patterns include common AWS access IDs, GitHub tokens, `sk-` model keys, private-key headers and credential assignments. A secret-like source is rejected as a whole rather than redacted piecemeal. Repository access is read-only. Configure live-mode egress restrictions at the host/proxy if policy requires enforcement outside application code.

Compose binds the UI to localhost and isolates the database network. Backend and worker run non-root, read-only with temporary filesystems, dropped capabilities and no privilege escalation. Store `.env` outside Git with restrictive permissions. Production deployments need TLS termination and secret injection from a secret manager.

Approval records are scoped to the configured operator and repository. This release does not claim separate approver identity, comprehensive audit retention, formal DLP, complete prompt-injection prevention, or tenant isolation. Add those controls before adopting shared enterprise use.

Report security issues privately to the repository owner; do not include live credentials or proprietary source in public issues.
