# Deployment and operations

1. Generate independent credentials and set `.env` permissions to 600. Review repository snapshots and mount only approved projects read-only under the configured repository root.
2. Start `docker compose up -d --build`. Compose waits for PostgreSQL readiness. Run `python scripts/smoke.py` with API_TOKEN exported; browser smoke is `cd frontend && npm run e2e`.
3. Keep the UI localhost-only or add an authenticated TLS reverse proxy. Do not publish database or worker ports. Restrict outbound model traffic using host network policy if required.
4. Back up the named PostgreSQL volume using `pg_dump`; test restoration before upgrades. Draft content is sensitive. Define retention and use SQL administrative deletion according to policy.
5. Rotate credentials by updating environment secrets and recreating backend/worker. Operator token rotation invalidates active browser sessions; drafts retain the deployment's operator identity.
6. Inspect structured logs via `docker compose logs backend worker`. Use X-Trace-Id from a failed request. Never enable request-body logging. Monitor status counts, 429s, upstream failures and generation durations.
7. Stop with `docker compose down`; omit `-v` to preserve data. `down -v` destroys database storage.

For release, run CI successfully, then tag the same commit `v1.0.0` and push the tag. The release workflow waits for CI and publishes backend, worker and frontend images under `ghcr.io/<owner>/devassist-ai-engineering-platform/<service>:<tag>`. It does not deploy a host or claim a production rollout. Set organization package access policy before consuming private GHCR images.

Rollback by deploying the previous image tag with the existing database volume. Current schema initialization is additive only; use explicit reviewed migrations for future schema changes. Single-instance rate limiting must be replaced with centralized enforcement before horizontal scaling. Keep Java, Spring Boot, Python and npm dependencies updated and audit them before production use.

Native development uses port 18080 for the backend; Compose uses internal 8080. Vite proxies API requests to 18080. Tests should run against a dedicated disposable database to avoid altering real approvals and draft records.
