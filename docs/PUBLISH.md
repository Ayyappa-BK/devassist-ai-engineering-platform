# Publish the separate GitHub repository

GitHub publication was unavailable during implementation: `gh auth status` reported the saved credential invalid. No remote repository or hosted CI run is claimed. From this repository directory execute:

```sh
gh auth login --hostname github.com --git-protocol https --web
if [ ! -d .git ]; then git init -b main; fi
git add .
git diff --cached --quiet || git commit -m "Build DevAssist AI engineering platform"
gh repo create devassist-ai-engineering-platform --private --source=. --remote=origin --push
# Wait for the push workflow to appear, then inspect and wait for its result:
gh run list --workflow ci.yml --limit 5
RUN_ID=$(gh run list --workflow ci.yml --limit 1 --json databaseId --jq '.[0].databaseId')
gh run watch "$RUN_ID" --exit-status
# If a check fails:
gh run view "$RUN_ID" --log-failed
```

Use `--public` instead of `--private` only if the full source and examples are intended for public publication. The example repositories contain synthetic business code, not actual customer repositories. `.env`, build products and local test state are ignored.

After CI succeeds and you want a container release:

```sh
git tag v1.0.0
git push origin v1.0.0
gh run list --workflow release.yml --limit 5
```

The CI workflow builds all components, evaluates deterministic outputs, starts real PostgreSQL through Compose, runs the API smoke script and tests the browser approval/export flow. Verify hosted checks before treating the release as deployable.
