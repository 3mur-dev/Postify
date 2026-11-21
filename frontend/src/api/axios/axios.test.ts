import { AxiosHeaders } from "axios";
import type { InternalAxiosRequestConfig } from "axios";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { createClient } from "./createClient";
import { attachAccessToken } from "./interceptors/authRequest";
import { redirectOnUnauthorized } from "./interceptors/authResponse";

describe("createClient", () => {
  it("sets API defaults and respects caller-provided configuration", () => {
    const client = createClient();
    expect(client.defaults.baseURL).toBe("http://localhost:8080/api");
    expect(client.defaults.timeout).toBe(15_000);
    expect(client.defaults.withCredentials).toBe(true);
    expect(client.defaults.headers.common.Accept).toContain("application/json");

    const customClient = createClient({
      baseURL: "https://api.example.com",
      timeout: 5_000,
      headers: { "X-Client": "web" },
    });
    expect(customClient.defaults.baseURL).toBe("https://api.example.com");
    expect(customClient.defaults.timeout).toBe(5_000);
    expect(JSON.stringify(customClient.defaults.headers)).toContain("web");
  });
});

describe("attachAccessToken", () => {
  beforeEach(() => localStorage.clear());

  it("returns the request unchanged when there is no token", () => {
    const config = {
      headers: new AxiosHeaders({ "X-Request": "present" }),
    } as InternalAxiosRequestConfig;

    expect(attachAccessToken(config)).toBe(config);
    expect(config.headers.get("Authorization")).toBeUndefined();
  });

  it("adds the bearer token without discarding existing headers", () => {
    localStorage.setItem("token", "test-access-token");
    const config = {
      headers: new AxiosHeaders({ "X-Request": "present" }),
    } as InternalAxiosRequestConfig;

    const result = attachAccessToken(config);

    expect(result.headers.get("Authorization")).toBe("Bearer test-access-token");
    expect(result.headers.get("X-Request")).toBe("present");
  });
});

describe("redirectOnUnauthorized", () => {
  beforeEach(() => localStorage.clear());

  it("clears the token on unauthorized errors and rejects with the same error", async () => {
    localStorage.setItem("token", "expired");
    const error = {
      response: { status: 401 },
      config: {
        headers: new AxiosHeaders({
          Authorization: "Bearer expired",
        }),
      },
    } as never;

    await expect(redirectOnUnauthorized(error)).rejects.toBe(error);
    expect(localStorage.getItem("token")).toBeNull();
  });

  it("expires a session once when concurrent authenticated requests return 401", async () => {
    localStorage.setItem("token", "expired");
    const dispatch = vi.spyOn(window, "dispatchEvent");
    const firstError = {
      response: { status: 401 },
      config: {
        headers: new AxiosHeaders({
          Authorization: "Bearer expired",
        }),
      },
    } as never;
    const secondError = {
      response: { status: 401 },
      config: {
        headers: new AxiosHeaders({
          Authorization: "Bearer expired",
        }),
      },
    } as never;

    await expect(redirectOnUnauthorized(firstError)).rejects.toBe(firstError);
    await expect(redirectOnUnauthorized(secondError)).rejects.toBe(secondError);

    expect(localStorage.getItem("token")).toBeNull();
    expect(
      dispatch.mock.calls.filter(
        ([event]) => event.type === "postify:auth-session-expired",
      ),
    ).toHaveLength(1);
  });

  it("does not expire a newer session for a stale request or a public 401", async () => {
    localStorage.setItem("token", "new-session");
    const staleError = {
      response: { status: 401 },
      config: {
        headers: new AxiosHeaders({
          Authorization: "Bearer old-session",
        }),
      },
    } as never;
    const publicError = {
      response: { status: 401 },
      config: { headers: new AxiosHeaders() },
    } as never;

    await expect(redirectOnUnauthorized(staleError)).rejects.toBe(staleError);
    await expect(redirectOnUnauthorized(publicError)).rejects.toBe(publicError);

    expect(localStorage.getItem("token")).toBe("new-session");
  });

  it("preserves the token for non-401 errors and rejects with the same error", async () => {
    localStorage.setItem("token", "valid");
    const error = {
      response: { status: 403 },
    } as never;

    await expect(redirectOnUnauthorized(error)).rejects.toBe(error);
    expect(localStorage.getItem("token")).toBe("valid");
  });

  it("preserves the token when an error has no response", async () => {
    localStorage.setItem("token", "valid");
    const error = new Error("Network error") as never;

    await expect(redirectOnUnauthorized(error)).rejects.toBe(error);
    expect(localStorage.getItem("token")).toBe("valid");
  });
});
