import axios from "axios";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { getApiErrorMessage } from "./apiError";
import { resolveImageUrl } from "./image";
import { formatRelativeTime } from "./time";
import { getToken, hasToken, removeToken, setToken } from "./token";
import { getAccessToken, hasAccessToken } from "../lib/auth";

describe("authentication token utilities", () => {
  beforeEach(() => localStorage.clear());

  it("stores, reads, checks, and removes the access token", () => {
    expect(getToken()).toBeNull();
    expect(hasToken()).toBe(false);
    expect(getAccessToken()).toBeNull();
    expect(hasAccessToken()).toBe(false);

    setToken("session-token");

    expect(getToken()).toBe("session-token");
    expect(hasToken()).toBe(true);
    expect(getAccessToken()).toBe("session-token");
    expect(hasAccessToken()).toBe(true);

    removeToken();

    expect(getToken()).toBeNull();
    expect(hasToken()).toBe(false);
  });
});

describe("getApiErrorMessage", () => {
  it("returns a readable message for standard errors", () => {
    expect(getApiErrorMessage(new Error("Request failed"))).toBe("Request failed");
  });

  it("uses the configured fallback for unknown errors", () => {
    expect(getApiErrorMessage("failure")).toBe(
      "Something went wrong. Please try again.",
    );
    expect(getApiErrorMessage(null, "Try later")).toBe("Try later");
  });

  it("uses API detail for Axios errors and the fallback when detail is absent", () => {
    const detailedError = Object.assign(new Error("Request failed"), {
      isAxiosError: true,
      response: { data: { detail: "Post not found" } },
    });
    const emptyError = Object.assign(new Error("Request failed"), {
      isAxiosError: true,
      response: { data: {} },
    });

    expect(axios.isAxiosError(detailedError)).toBe(true);
    expect(getApiErrorMessage(detailedError)).toBe("Post not found");
    expect(getApiErrorMessage(emptyError, "Custom fallback")).toBe(
      "Custom fallback",
    );
  });
});

describe("resolveImageUrl", () => {
  it("returns undefined for empty image paths", () => {
    expect(resolveImageUrl(null)).toBeUndefined();
    expect(resolveImageUrl(undefined)).toBeUndefined();
    expect(resolveImageUrl("")).toBeUndefined();
  });

  it("preserves absolute URLs and resolves API-root-relative paths", () => {
    expect(resolveImageUrl("https://cdn.example.com/photo.jpg")).toBe(
      "https://cdn.example.com/photo.jpg",
    );
    expect(resolveImageUrl("HTTP://cdn.example.com/photo.jpg")).toBe(
      "HTTP://cdn.example.com/photo.jpg",
    );
    expect(resolveImageUrl("/uploads/photo.jpg")).toBe(
      "http://localhost:8080/uploads/photo.jpg",
    );
  });

  it("preserves relative paths", () => {
    expect(resolveImageUrl("uploads/photo.jpg")).toBe("uploads/photo.jpg");
  });
});

describe("formatRelativeTime", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date("2025-06-15T12:00:00.000Z"));
  });

  it("handles invalid dates, future dates, and minute/hour boundaries", () => {
    expect(formatRelativeTime("not-a-date")).toBe("Unknown time");
    expect(formatRelativeTime("2025-06-15T12:01:00.000Z")).toBe("now");
    expect(formatRelativeTime("2025-06-15T11:59:00.000Z")).toBe("1 min ago");
    expect(formatRelativeTime("2025-06-15T11:00:00.000Z")).toBe("1 hr ago");
  });

  it("formats yesterday, weekdays, and dates with or without a year", () => {
    expect(formatRelativeTime("2025-06-14T12:00:00.000Z")).toBe("Yesterday");
    expect(formatRelativeTime("2025-06-12T12:00:00.000Z")).toBe(
      new Date("2025-06-12T12:00:00.000Z").toLocaleDateString([], {
        weekday: "short",
      }),
    );
    expect(formatRelativeTime("2025-06-01T12:00:00.000Z")).toBe(
      new Date("2025-06-01T12:00:00.000Z").toLocaleDateString([], {
        month: "short",
        day: "numeric",
      }),
    );
    expect(formatRelativeTime("2024-06-01T12:00:00.000Z")).toBe(
      new Date("2024-06-01T12:00:00.000Z").toLocaleDateString([], {
        month: "short",
        day: "numeric",
        year: "numeric",
      }),
    );
  });
});
