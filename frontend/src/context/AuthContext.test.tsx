import { act, fireEvent, render, renderHook, screen, waitFor } from "@testing-library/react";
import { AxiosHeaders } from "axios";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { ReactNode } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { getMyProfile } from "../api/profileApi";
import type { ProfileResponse } from "../api/types";
import { redirectOnUnauthorized } from "../api/axios/interceptors/authResponse";
import { AuthProvider } from "./AuthContext";
import { useAuth } from "./useAuth";

vi.mock("../api/profileApi", () => ({ getMyProfile: vi.fn() }));

const mockGetMyProfile = vi.mocked(getMyProfile);
const profile = {
  publicId: "user-1",
  username: "ava",
  role: "USER",
} as ProfileResponse;
const secondProfile = {
  publicId: "user-2",
  username: "sam",
  role: "USER",
} as ProfileResponse;

let queryClient: QueryClient;

function AuthState() {
  const { user, loading, isAuthenticated, refreshUser, logout } = useAuth();

  return (
    <div>
      <output data-testid="username">{user?.username ?? "anonymous"}</output>
      <output data-testid="loading">{String(loading)}</output>
      <output data-testid="authenticated">{String(isAuthenticated)}</output>
      <button type="button" onClick={() => void refreshUser()}>
        Refresh
      </button>
      <button type="button" onClick={logout}>
        Log out
      </button>
    </div>
  );
}

function wrapper({ children }: { children: ReactNode }) {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>{children}</AuthProvider>
    </QueryClientProvider>
  );
}

describe("AuthProvider and useAuth", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false, gcTime: Infinity },
        mutations: { retry: false },
      },
    });
  });

  it("requires consumers to be rendered inside the provider", () => {
    expect(() => renderHook(() => useAuth())).toThrow(
      "useAuth must be used inside AuthProvider",
    );
  });

  it("starts anonymous without a token and refresh remains anonymous", async () => {
    render(<AuthState />, { wrapper });

    expect(screen.getByTestId("username")).toHaveTextContent("anonymous");
    expect(screen.getByTestId("loading")).toHaveTextContent("false");
    expect(screen.getByTestId("authenticated")).toHaveTextContent("false");
    expect(mockGetMyProfile).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole("button", { name: "Refresh" }));
    await waitFor(() =>
      expect(screen.getByTestId("loading")).toHaveTextContent("false"),
    );
    expect(mockGetMyProfile).not.toHaveBeenCalled();
  });

  it("loads an authenticated profile and supports refresh and logout", async () => {
    localStorage.setItem("token", "access-token");
    mockGetMyProfile.mockResolvedValue(profile);
    render(<AuthState />, { wrapper });

    await waitFor(() => {
      expect(screen.getByTestId("username")).toHaveTextContent("ava");
      expect(screen.getByTestId("authenticated")).toHaveTextContent("true");
      expect(screen.getByTestId("loading")).toHaveTextContent("false");
    });
    expect(mockGetMyProfile).toHaveBeenCalledOnce();

    fireEvent.click(screen.getByRole("button", { name: "Refresh" }));
    await waitFor(() => expect(mockGetMyProfile).toHaveBeenCalledTimes(2));

    queryClient.setQueryData(["postFeed"], ["private post"]);
    fireEvent.click(screen.getByRole("button", { name: "Log out" }));
    expect(localStorage.getItem("token")).toBeNull();
    expect(screen.getByTestId("username")).toHaveTextContent("anonymous");
    expect(screen.getByTestId("authenticated")).toHaveTextContent("false");
    expect(queryClient.getQueryData(["postFeed"])).toBeUndefined();
  });

  it("clears cached private data when the authenticated account changes", async () => {
    localStorage.setItem("token", "first-account-token");
    let resolveSecondProfile!: (value: ProfileResponse) => void;
    mockGetMyProfile
      .mockResolvedValueOnce(profile)
      .mockImplementationOnce(
        () =>
          new Promise<ProfileResponse>((resolve) => {
            resolveSecondProfile = resolve;
          }),
      );
    queryClient.setQueryData(["postFeed"], ["first account private post"]);
    render(<AuthState />, { wrapper });

    await waitFor(() =>
      expect(screen.getByTestId("username")).toHaveTextContent("ava"),
    );
    localStorage.setItem("token", "second-account-token");
    fireEvent.click(screen.getByRole("button", { name: "Refresh" }));

    await waitFor(() => expect(mockGetMyProfile).toHaveBeenCalledTimes(2));
    expect(screen.getByTestId("username")).toHaveTextContent("anonymous");
    expect(queryClient.getQueryData(["postFeed"])).toBeUndefined();

    await act(async () => resolveSecondProfile(secondProfile));
    await waitFor(() =>
      expect(screen.getByTestId("username")).toHaveTextContent("sam"),
    );
  });

  it("clears an invalid token when profile loading fails", async () => {
    localStorage.setItem("token", "expired-token");
    queryClient.setQueryData(["postFeed"], ["private post"]);
    mockGetMyProfile.mockRejectedValue(new Error("Unauthorized"));
    render(<AuthState />, { wrapper });

    await waitFor(() => {
      expect(localStorage.getItem("token")).toBeNull();
      expect(screen.getByTestId("loading")).toHaveTextContent("false");
      expect(screen.getByTestId("username")).toHaveTextContent("anonymous");
    });
    expect(queryClient.getQueryData(["postFeed"])).toBeUndefined();
  });

  it("transitions to guest and clears private cache on an authenticated 401", async () => {
    localStorage.setItem("token", "access-token");
    mockGetMyProfile.mockResolvedValue(profile);
    render(<AuthState />, { wrapper });
    await waitFor(() =>
      expect(screen.getByTestId("username")).toHaveTextContent("ava"),
    );
    queryClient.setQueryData(["postFeed"], ["private post"]);

    const error = {
      response: { status: 401 },
      config: {
        headers: new AxiosHeaders({
          Authorization: "Bearer access-token",
        }),
      },
    } as never;
    await expect(redirectOnUnauthorized(error)).rejects.toBe(error);

    await waitFor(() => {
      expect(screen.getByTestId("username")).toHaveTextContent("anonymous");
      expect(screen.getByTestId("authenticated")).toHaveTextContent("false");
    });
    expect(localStorage.getItem("token")).toBeNull();
    expect(queryClient.getQueryData(["postFeed"])).toBeUndefined();
  });

  it("does not update auth state after the provider is unmounted", async () => {
    localStorage.setItem("token", "access-token");
    let resolveProfile!: (value: ProfileResponse) => void;
    mockGetMyProfile.mockReturnValue(
      new Promise<ProfileResponse>((resolve) => {
        resolveProfile = resolve;
      }),
    );
    const { unmount } = render(<AuthState />, { wrapper });
    unmount();

    await act(async () => resolveProfile(profile));
    expect(mockGetMyProfile).toHaveBeenCalledOnce();
  });

  // ---- Cross-tab synchronization tests ----

  it("clears auth state when the token is removed in another tab", async () => {
    localStorage.setItem("token", "access-token");
    mockGetMyProfile.mockResolvedValue(profile);
    queryClient.setQueryData(["postFeed"], ["private post"]);
    render(<AuthState />, { wrapper });

    await waitFor(() =>
      expect(screen.getByTestId("username")).toHaveTextContent("ava"),
    );

    // Simulate another tab removing the token via the storage event.
    await act(async () => {
      window.dispatchEvent(
        new StorageEvent("storage", {
          key: "token",
          newValue: null,
          oldValue: "access-token",
          url: window.location.href,
          storageArea: localStorage,
        }),
      );
    });

    await waitFor(() => {
      expect(screen.getByTestId("username")).toHaveTextContent("anonymous");
      expect(screen.getByTestId("authenticated")).toHaveTextContent("false");
      expect(screen.getByTestId("loading")).toHaveTextContent("false");
    });
    expect(queryClient.getQueryData(["postFeed"])).toBeUndefined();
  });

  it("syncs a new login from another tab by refreshing the profile", async () => {
    localStorage.setItem("token", "old-token");
    mockGetMyProfile.mockResolvedValue(profile);
    render(<AuthState />, { wrapper });

    await waitFor(() =>
      expect(screen.getByTestId("username")).toHaveTextContent("ava"),
    );

    // Simulate another tab logging in as a different user.
    localStorage.setItem("token", "new-token");
    mockGetMyProfile.mockResolvedValue(secondProfile);

    await act(async () => {
      window.dispatchEvent(
        new StorageEvent("storage", {
          key: "token",
          newValue: "new-token",
          oldValue: "old-token",
          url: window.location.href,
          storageArea: localStorage,
        }),
      );
    });

    await waitFor(() => {
      expect(screen.getByTestId("username")).toHaveTextContent("sam");
      expect(screen.getByTestId("authenticated")).toHaveTextContent("true");
      expect(screen.getByTestId("loading")).toHaveTextContent("false");
    });
    expect(mockGetMyProfile).toHaveBeenCalledTimes(2);
  });

  it("does not clear auth state on a stale 401 after a newer login", async () => {
    localStorage.setItem("token", "old-token");
    mockGetMyProfile.mockResolvedValue(profile);
    render(<AuthState />, { wrapper });

    await waitFor(() =>
      expect(screen.getByTestId("username")).toHaveTextContent("ava"),
    );

    // Simulate the user logging in with a new token in the same tab.
    localStorage.setItem("token", "new-token");
    mockGetMyProfile.mockResolvedValue(secondProfile);
    fireEvent.click(screen.getByRole("button", { name: "Refresh" }));

    await waitFor(() =>
      expect(screen.getByTestId("username")).toHaveTextContent("sam"),
    );

    // A stale 401 arrives for the old token.
    const staleError = {
      response: { status: 401 },
      config: {
        headers: new AxiosHeaders({
          Authorization: "Bearer old-token",
        }),
      },
    } as never;
    await expect(redirectOnUnauthorized(staleError)).rejects.toBe(staleError);

    // Auth state should remain intact — the user is still logged in with the new token.
    expect(screen.getByTestId("username")).toHaveTextContent("sam");
    expect(screen.getByTestId("authenticated")).toHaveTextContent("true");
    expect(localStorage.getItem("token")).toBe("new-token");
  });

  it("ignores storage events for keys other than token", async () => {
    localStorage.setItem("token", "access-token");
    mockGetMyProfile.mockResolvedValue(profile);
    render(<AuthState />, { wrapper });

    await waitFor(() =>
      expect(screen.getByTestId("username")).toHaveTextContent("ava"),
    );

    // Simulate a storage event for an unrelated key.
    await act(async () => {
      window.dispatchEvent(
        new StorageEvent("storage", {
          key: "some-other-key",
          newValue: "value",
          oldValue: null,
          url: window.location.href,
          storageArea: localStorage,
        }),
      );
    });

    // Auth state should remain unchanged.
    expect(screen.getByTestId("username")).toHaveTextContent("ava");
    expect(screen.getByTestId("authenticated")).toHaveTextContent("true");
  });
});
