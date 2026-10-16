import { test, expect, vi } from "vitest";
import { request } from "./api";
test("passes authorization and rejects unsuccessful responses", async () => {
  globalThis.fetch = vi.fn().mockResolvedValue({ ok: false, status: 401 });
  await expect(request("/health", "abc")).rejects.toThrow("401");
  expect(fetch).toHaveBeenCalledWith(
    "/api/health",
    expect.objectContaining({
      headers: expect.objectContaining({ Authorization: "Bearer abc" }),
    }),
  );
});
