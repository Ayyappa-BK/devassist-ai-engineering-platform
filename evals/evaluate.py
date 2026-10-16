"""Deterministic regression evaluation. Does not claim model accuracy."""

import sys, json, time, statistics, os
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "worker"))
os.environ.setdefault(
    "WORKER_TOKEN", "evaluation-only-token-abcdefghijklmnopqrstuvwxyz"
)
from fastapi.testclient import TestClient
from app import app

client = TestClient(app)
results = []
for line in Path(__file__).with_name("dataset.jsonl").read_text().splitlines():
    case = json.loads(line)
    start = time.perf_counter()
    response = client.post(
        "/generate",
        headers={"Authorization": "Bearer " + os.environ["WORKER_TOKEN"]},
        json={k: v for k, v in case.items() if k not in ("id", "expected", "reject")},
    )
    ms = (time.perf_counter() - start) * 1000
    content = response.json().get("content", "").lower()
    passed = (
        (response.status_code == 422)
        if case["reject"]
        else response.status_code == 200
        and all(x.lower() in content for x in case["expected"])
    )
    results.append({"id": case["id"], "passed": passed, "latencyMs": round(ms, 2)})
latencies = sorted(r["latencyMs"] for r in results)
report = {
    "mode": os.environ.get("LLM_MODE", "demo"),
    "passRate": sum(r["passed"] for r in results) / len(results),
    "p50Ms": statistics.median(latencies),
    "p95Ms": latencies[min(len(latencies) - 1, int(len(latencies) * 0.95))],
    "cases": results,
}
print(json.dumps(report, indent=2))
if not all(r["passed"] for r in results):
    sys.exit(1)
