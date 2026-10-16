"""Exercise real API -> worker -> database. Run against Compose or native services."""

import os, json, urllib.request, urllib.error, time

base = os.environ.get("BASE_URL", "http://localhost:3000")
token = os.environ["API_TOKEN"]


def call(path, body=None, expected=200, auth=True):
    headers = {"Content-Type": "application/json"}
    if auth:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(
        base + "/api" + path,
        headers=headers,
        data=json.dumps(body).encode() if body is not None else None,
    )
    try:
        with urllib.request.urlopen(req, timeout=45) as r:
            status = r.status
            data = json.load(r)
    except urllib.error.HTTPError as e:
        status = e.code
        data = {}
    assert status == expected, (path, status, expected)
    return data


call("/health", expected=401, auth=False)
call("/health")
files = call("/repositories/billing-service/files")
assert "src/InvoiceService.java" in files
source = {"repo": "billing-service", "path": "src/InvoiceService.java"}
assert call("/analyze", source)["methodCount"] == 1
call("/analyze", {**source, "path": "../README.md"}, 400)
call("/repositories/not-authorized/files", expected=400)
call("/documents/approve", {**source, "path": "README.md"})
results = []
for task in ["explain", "tests", "quality", "docs", "review"]:
    start = time.perf_counter()
    draft = call(
        "/generate",
        {
            **source,
            "task": task,
            "basePath": "before/InvoiceService.java" if task == "review" else None,
        },
    )
    assert draft["status"] == "PENDING" and "[README.md]" in draft["content"]
    call("/drafts/" + draft["id"] + "/export", expected=400)
    decision = "REJECTED" if task == "quality" else "APPROVED"
    call("/drafts/" + draft["id"] + "/review", {"decision": decision})
    call(
        "/drafts/" + draft["id"] + "/export",
        expected=200 if decision == "APPROVED" else 400,
    )
    results.append(
        {"task": task, "latencyMs": round((time.perf_counter() - start) * 1000, 2)}
    )
assert len(call("/drafts")) >= 5
print(json.dumps({"workflow": "PASS", "results": results}, indent=2))
