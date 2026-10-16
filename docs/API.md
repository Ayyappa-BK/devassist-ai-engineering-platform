# API

All API requests require `Authorization: Bearer <API_TOKEN>` including health. JSON only; unknown request fields are rejected. A server-generated `X-Trace-Id` correlates backend and worker events. Paths refer to the mounted repository root, not arbitrary host paths.

| Method | Path | Body / behavior |
| --- | --- | --- |
| GET | /api/health | Checks database connectivity |
| GET | /api/repositories/{repo}/files | Up to 200 Java/Markdown paths, depth at most 12 |
| POST | /api/analyze | `{ "repo":"billing-service", "path":"src/InvoiceService.java" }` |
| POST | /api/documents/approve | Same shape, Markdown path; operator approves snapshot for retrieval |
| POST | /api/generate | repo, path, task, optional basePath; returns id/content/status/latencyMs |
| GET | /api/drafts | Last 100 persisted drafts for operator |
| POST | /api/drafts/{id}/review | `{ "decision":"APPROVED" }` or REJECTED; PENDING only |
| GET | /api/drafts/{id}/export | content only when approved |

Tasks: `explain`, `tests`, `quality`, `docs`, `review`. A review requires a safe `basePath` containing the before snapshot in the same repository.

```sh
curl -fsS http://localhost:3000/api/generate  -H "Authorization: Bearer $API_TOKEN" -H 'Content-Type: application/json'  -d '{"repo":"billing-service","path":"src/InvoiceService.java","task":"review","basePath":"before/InvoiceService.java"}'
```

401 means missing/invalid authentication. 400 means invalid, unsafe or unavailable input, or disallowed state transition. 413 means excessive request size. 429 includes Retry-After. 502 means worker/model unavailable. Errors intentionally omit repository content and upstream details. No CORS cross-origin policy is enabled; use the same-origin proxy.
