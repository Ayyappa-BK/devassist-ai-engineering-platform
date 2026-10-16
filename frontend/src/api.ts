export async function request(path: string, token: string, body?: unknown) {
  const r = await fetch("/api" + path, {
    method: body === undefined ? "GET" : "POST",
    headers: {
      Authorization: "Bearer " + token,
      "Content-Type": "application/json",
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  if (!r.ok)
    throw new Error(
      `Request failed (${r.status}). Check authorization, input safety, and service availability.`,
    );
  return r.json();
}
