import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, renderHook, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { getAdminDashboard, getUsers } from "../api/adminApi";
import { getComments } from "../api/commentApi";
import { getConversationMessages, getConversations } from "../api/conversationApi";
import { getPostFeed } from "../api/feedApi";
import { getNotifications, markAsRead } from "../api/notification";
import { getProfile } from "../api/profileApi";
import { getSearchResults } from "../api/searchApi";
import type { DashboardSection, NotificationResponse, PagedResponse } from "../api/types";
import { useMessaging } from "./useMessaging";
import { useConversations, useConversationMessages } from "./useConversations";
import { useComments } from "./useComments";
import { useGetNotifications } from "./useGetNotification";
import { useGetProfile } from "./useGetProfile";
import { useGetSearchResults } from "./useGetSearchResults";
import { useInfiniteFeed } from "./useInfiniteFeed";
import { useMarkAsRead } from "./useMarkAsRead";
import { useGetAdminDashboard } from "./admin/dashboard/useGetAdminDashboard";
import { useGetUsers } from "./admin/users/useGetUsers";
import { MessagingContext } from "../context/messagingContextStore";

vi.mock("../api/adminApi", () => ({
  getAdminDashboard: vi.fn(),
  getUsers: vi.fn(),
}));
vi.mock("../api/commentApi", () => ({ getComments: vi.fn() }));
vi.mock("../api/conversationApi", () => ({
  getConversationMessages: vi.fn(),
  getConversations: vi.fn(),
}));
vi.mock("../api/feedApi", () => ({ getPostFeed: vi.fn() }));
vi.mock("../api/notification", () => ({
  getNotifications: vi.fn(),
  markAsRead: vi.fn(),
}));
vi.mock("../api/profileApi", () => ({ getProfile: vi.fn() }));
vi.mock("../api/searchApi", () => ({ getSearchResults: vi.fn() }));

const api = {
  getAdminDashboard: vi.mocked(getAdminDashboard),
  getUsers: vi.mocked(getUsers),
  getComments: vi.mocked(getComments),
  getConversationMessages: vi.mocked(getConversationMessages),
  getConversations: vi.mocked(getConversations),
  getPostFeed: vi.mocked(getPostFeed),
  getNotifications: vi.mocked(getNotifications),
  markAsRead: vi.mocked(markAsRead),
  getProfile: vi.mocked(getProfile),
  getSearchResults: vi.mocked(getSearchResults),
};

function createQueryProvider() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: 0 },
      mutations: { retry: false },
    },
  });
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );

  return { queryClient, wrapper };
}

describe("query hooks", () => {
  beforeEach(() => vi.clearAllMocks());

  it("loads comments and profiles only when their identifiers are available", async () => {
    api.getComments.mockResolvedValue([]);
    api.getProfile.mockResolvedValue({ username: "ava" } as never);
    const { wrapper } = createQueryProvider();

    const comments = renderHook(() => useComments("post-1"), { wrapper });
    await waitFor(() => expect(comments.result.current.isSuccess).toBe(true));
    expect(api.getComments).toHaveBeenCalledWith("post-1");

    const missingProfile = renderHook(() => useGetProfile(null), { wrapper });
    expect(missingProfile.result.current.fetchStatus).toBe("idle");
    expect(api.getProfile).not.toHaveBeenCalled();

    const profile = renderHook(() => useGetProfile("ava"), { wrapper });
    await waitFor(() => expect(profile.result.current.data?.username).toBe("ava"));
    expect(api.getProfile).toHaveBeenCalledWith("ava");
  });

  it("normalizes search input and skips empty queries", async () => {
    api.getSearchResults.mockResolvedValue({ users: [], posts: [] });
    const { wrapper } = createQueryProvider();

    const emptySearch = renderHook(() => useGetSearchResults("   "), { wrapper });
    expect(emptySearch.result.current.fetchStatus).toBe("idle");
    expect(api.getSearchResults).not.toHaveBeenCalled();

    const search = renderHook(() => useGetSearchResults("  postify  "), { wrapper });
    await waitFor(() => expect(search.result.current.isSuccess).toBe(true));
    expect(api.getSearchResults).toHaveBeenCalledWith("postify");
  });

  it("loads feed pages using the configured page size and next-page cursor", async () => {
    api.getPostFeed
      .mockResolvedValueOnce({ content: [], page: 0, last: false } as never)
      .mockResolvedValueOnce({ content: [], page: 1, last: true } as never);
    const { wrapper } = createQueryProvider();
    const feed = renderHook(() => useInfiniteFeed(), { wrapper });

    await waitFor(() => expect(feed.result.current.isSuccess).toBe(true));
    expect(api.getPostFeed).toHaveBeenNthCalledWith(1, { page: 0, size: 5 });
    await act(async () => feed.result.current.fetchNextPage());
    expect(api.getPostFeed).toHaveBeenNthCalledWith(2, { page: 1, size: 5 });
  });

  it("loads conversations only when enabled and pages conversation messages", async () => {
    api.getConversations.mockResolvedValue([]);
    api.getConversationMessages
      .mockResolvedValueOnce({ content: [], number: 0, last: false } as never)
      .mockResolvedValueOnce({ content: [], number: 1, last: true } as never);
    const { wrapper } = createQueryProvider();

    const disabled = renderHook(() => useConversations(false), { wrapper });
    expect(disabled.result.current.fetchStatus).toBe("idle");
    expect(api.getConversations).not.toHaveBeenCalled();

    const conversations = renderHook(() => useConversations(true), { wrapper });
    await waitFor(() => expect(conversations.result.current.isSuccess).toBe(true));
    expect(api.getConversations).toHaveBeenCalledOnce();

    const noConversation = renderHook(() => useConversationMessages(undefined), {
      wrapper,
    });
    expect(noConversation.result.current.fetchStatus).toBe("idle");
    expect(api.getConversationMessages).not.toHaveBeenCalled();

    const messages = renderHook(() => useConversationMessages("thread-1"), {
      wrapper,
    });
    await waitFor(() => expect(messages.result.current.isSuccess).toBe(true));
    expect(api.getConversationMessages).toHaveBeenCalledWith("thread-1", 0);
    await act(async () => messages.result.current.fetchNextPage());
    expect(api.getConversationMessages).toHaveBeenCalledWith("thread-1", 1);
  });

  it("loads notifications, dashboard, and admin users", async () => {
    api.getNotifications.mockResolvedValue({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0, last: false } as PagedResponse<NotificationResponse>);
    api.getAdminDashboard.mockResolvedValue({ overview: {} } as never);
    api.getUsers.mockResolvedValue({ content: [] } as never);
    const { wrapper } = createQueryProvider();

    const notifications = renderHook(() => useGetNotifications(), { wrapper });
    await waitFor(() => expect(notifications.result.current.isSuccess).toBe(true));

    const dashboardParams = {
      rangePreset: "TODAY" as const,
      timezone: "UTC",
      sections: ["OVERVIEW"] as DashboardSection[],
    };
    const dashboard = renderHook(
      () => useGetAdminDashboard(dashboardParams),
      { wrapper },
    );
    await waitFor(() => expect(dashboard.result.current.isSuccess).toBe(true));
    expect(api.getAdminDashboard).toHaveBeenCalledWith(dashboardParams);

    const usersParams = { page: 2, size: 15 };
    const users = renderHook(() => useGetUsers(usersParams), { wrapper });
    await waitFor(() => expect(users.result.current.isSuccess).toBe(true));
    expect(api.getUsers).toHaveBeenCalledWith(usersParams);
  });

  it("marks a cached notification as read after the mutation succeeds", async () => {
    api.markAsRead.mockResolvedValue(undefined);
    const { queryClient, wrapper } = createQueryProvider();
    const notifications = [
      { publicId: "n1", isRead: false },
      { publicId: "n2", isRead: false },
    ] as NotificationResponse[];
    queryClient.setQueryData(["notifications"], { content: notifications, page: 0, size: 20, totalElements: 2, totalPages: 1, last: true } as PagedResponse<NotificationResponse>);
    const mutation = renderHook(() => useMarkAsRead(), { wrapper });

    await act(async () => mutation.result.current.mutateAsync("n1"));

    expect(api.markAsRead).toHaveBeenCalledWith("n1");
    expect(queryClient.getQueryData<PagedResponse<NotificationResponse>>(["notifications"])?.content).toEqual([
      { publicId: "n1", isRead: true },
      { publicId: "n2", isRead: false },
    ]);
  });

  it("requires a messaging provider and exposes its context value", () => {
    expect(() => renderHook(() => useMessaging())).toThrow(
      "useMessaging must be used inside MessagingProvider",
    );

    const value = {
      connectionState: "connected" as const,
      connectionError: null,
      pendingMessages: [],
      participantPresence: {},
      sendMessage: vi.fn(),
      requestPresence: vi.fn(),
      retryMessage: vi.fn(),
      resolvePendingMessage: vi.fn(),
    };
    const { result } = renderHook(() => useMessaging(), {
      wrapper: ({ children }) => (
        <MessagingContext.Provider value={value}>{children}</MessagingContext.Provider>
      ),
    });
    expect(result.current).toBe(value);
  });
});
