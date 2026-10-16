import React, { useState } from "react";
import { createRoot } from "react-dom/client";
import { request } from "./api";
import "./style.css";
type Draft = { id: string; content: string; status: string; latencyMs: number };
function App() {
  const [token, setToken] = useState(""),
    [repo, setRepo] = useState("billing-service"),
    [path, setPath] = useState("src/InvoiceService.java"),
    [basePath, setBase] = useState("before/InvoiceService.java"),
    [task, setTask] = useState("explain"),
    [files, setFiles] = useState<string[]>([]),
    [draft, setDraft] = useState<Draft | null>(null),
    [message, setMessage] = useState(
      "Connect using your operator token. Credentials stay in memory.",
    ),
    [busy, setBusy] = useState(false),
    [analysis, setAnalysis] = useState("");
  async function run(fn: () => Promise<void>) {
    setBusy(true);
    try {
      await fn();
    } catch (e) {
      setMessage(e instanceof Error ? e.message : "Request failed");
    } finally {
      setBusy(false);
    }
  }
  const input = { repo, path };
  return (
    <div className="shell">
      <aside>
        <div className="brand">
          D<span>evAssist AI</span>
        </div>
        <p className="muted">ENGINEERING WORKSPACE</p>
        <nav>
          <b>◈ &nbsp; Code assistant</b>
          <span>▧ &nbsp; Approved knowledge</span>
          <span>✓ &nbsp; Human review</span>
        </nav>
        <div className="aside-bottom">
          <strong>Secure by design</strong>
          <p>Repository allowlist · Secret screening · Review gates</p>
        </div>
      </aside>
      <main>
        <header>
          <div>
            <small>DEVELOPER PRODUCTIVITY / WORKSPACE</small>
            <h1>Your code. A clearer perspective.</h1>
            <p>
              Understand, improve, and document your Java projects with reviewed
              AI drafts.
            </p>
          </div>
          <span className="badge">● Human in the loop</span>
        </header>
        <section className="connection">
          <label>
            Operator token
            <input
              type="password"
              autoComplete="off"
              value={token}
              onChange={(e) => setToken(e.target.value)}
              placeholder="Paste your API token"
            />
          </label>
          <label>
            Approved repository
            <input
              value={repo}
              onChange={(e) => {
                setRepo(e.target.value);
                setFiles([]);
                setDraft(null);
              }}
            />
          </label>
          <button
            disabled={busy || !token}
            onClick={() =>
              run(async () => {
                setFiles(
                  await request(
                    "/repositories/" + encodeURIComponent(repo) + "/files",
                    token,
                  ),
                );
                setMessage("Repository connected. Select a file to begin.");
              })
            }
          >
            Connect repository ↗
          </button>
        </section>
        <div className="grid">
          <section className="card">
            <small>01 / PROJECT CONTEXT</small>
            <h2>Choose your source</h2>
            <label>
              Repository file
              <input
                list="files"
                value={path}
                onChange={(e) => setPath(e.target.value)}
              />
              <datalist id="files">
                {files.map((f) => (
                  <option key={f}>{f}</option>
                ))}
              </datalist>
            </label>
            <div className="file-list">
              {files.map((f) => (
                <button
                  className={f === path ? "selected" : ""}
                  key={f}
                  onClick={() => setPath(f)}
                >
                  {f.endsWith(".java") ? "J" : "M"} &nbsp; {f}
                </button>
              ))}
            </div>
            <button
              className="secondary"
              disabled={busy}
              onClick={() =>
                run(async () => {
                  setAnalysis(
                    JSON.stringify(
                      await request("/analyze", token, input),
                      null,
                      2,
                    ),
                  );
                  setMessage("Java analysis complete.");
                })
              }
            >
              Analyze Java source
            </button>
            <button
              className="secondary"
              disabled={busy || !path.endsWith(".md")}
              onClick={() =>
                run(async () => {
                  await request("/documents/approve", token, input);
                  setMessage(
                    "Document snapshot approved for retrieval. Changes require reapproval.",
                  );
                })
              }
            >
              Approve document for RAG
            </button>
            {analysis && <pre>{analysis}</pre>}
          </section>
          <section className="card">
            <small>02 / AI ASSISTANCE</small>
            <h2>What would you like to explore?</h2>
            <div className="tasks">
              {[
                ["explain", "Code explanation"],
                ["tests", "Unit-test suggestions"],
                ["quality", "Quality issues"],
                ["docs", "Documentation draft"],
                ["review", "PR review summary"],
              ].map(([id, label]) => (
                <button
                  key={id}
                  className={task === id ? "active" : ""}
                  onClick={() => setTask(id)}
                >
                  {label}
                </button>
              ))}
            </div>
            {task === "review" && (
              <label>
                Before snapshot path
                <input
                  value={basePath}
                  onChange={(e) => setBase(e.target.value)}
                />
              </label>
            )}
            <p className="muted">
              Only approved documentation is retrieved. Drafts are never
              executed or committed.
            </p>
            <button
              disabled={busy || !token}
              onClick={() =>
                run(async () => {
                  setDraft(
                    await request("/generate", token, {
                      ...input,
                      task,
                      basePath: task === "review" ? basePath : null,
                    }),
                  );
                  setMessage(
                    "Draft ready. Review its correctness before approval.",
                  );
                })
              }
            >
              {busy ? "Working…" : "Generate draft ✦"}
            </button>
            <div className="output">
              <div className="output-head">
                <strong>Review workspace</strong>
                <span className="badge">{draft?.status ?? "READY"}</span>
              </div>
              {draft ? (
                <>
                  <pre>{draft.content}</pre>
                  <small>
                    {draft.latencyMs} ms · Draft {draft.id.slice(0, 8)}
                  </small>
                  <div className="actions">
                    <button
                      disabled={busy || draft.status !== "PENDING"}
                      onClick={() =>
                        run(async () => {
                          await request(
                            "/drafts/" + draft.id + "/review",
                            token,
                            { decision: "APPROVED" },
                          );
                          setDraft({ ...draft, status: "APPROVED" });
                          setMessage("Approved. Export is now available.");
                        })
                      }
                    >
                      Approve
                    </button>
                    <button
                      className="secondary"
                      disabled={busy || draft.status !== "PENDING"}
                      onClick={() =>
                        run(async () => {
                          await request(
                            "/drafts/" + draft.id + "/review",
                            token,
                            { decision: "REJECTED" },
                          );
                          setDraft({ ...draft, status: "REJECTED" });
                        })
                      }
                    >
                      Reject
                    </button>
                    <button
                      className="secondary"
                      disabled={busy || draft.status !== "APPROVED"}
                      onClick={() =>
                        run(async () => {
                          const result = await request(
                            "/drafts/" + draft.id + "/export",
                            token,
                          );
                          const url = URL.createObjectURL(
                            new Blob([result.content], {
                              type: "text/markdown",
                            }),
                          );
                          const a = document.createElement("a");
                          a.href = url;
                          a.download = "devassist-draft.md";
                          a.click();
                          URL.revokeObjectURL(url);
                        })
                      }
                    >
                      Export Markdown
                    </button>
                  </div>
                </>
              ) : (
                <div className="empty">
                  ✦<h3>Insight starts with context</h3>
                  <p>
                    Select a source file and a task to create your first draft.
                  </p>
                </div>
              )}
            </div>
          </section>
        </div>
        <footer role="status">{message}</footer>
      </main>
    </div>
  );
}
createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);
