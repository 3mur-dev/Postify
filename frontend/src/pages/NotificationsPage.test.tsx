import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes, useLocation } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { NotificationResponse, PagedResponse } from "../api/types";
import { getNotifications, markAsRead } from "../api/notification";
import { NotificationsPage } from "./NotificationsPage";

vi.mock("../api/notification", () => ({
  getNotifications: vi.fn(),
  markAsRead: vi.fn(),
}));

const mockGetNotifications = vi.mocked(getNotifications);
const mockMarkAsRead = vi.mocked(markAsRead);

function notification(
  overrides: Partial<NotificationResponse> = {},
): NotificationResponse {
  return {
    publicId: "notice-1",
    message: "A notification",
    type: "LIKE",
    actorPublicId: "actor-1",
    actorUsername: "ava",
    postPublicId: "post-1",
    commentId: null,
    isRead: false,
    createdAt: "2025-06-15T11:59:59.000Z",
    ...overrides,
  };
}

function LocationDisplay() {
  const location = useLocation();
  return <output data-testid="location">{location.pathname}</output>;
}

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: 0 } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={["/notifications"]}>
        <Routes>
          <Route path="/notifications" element={<NotificationsPage />} />
          <Route path="*" element={<LocationDisplay />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("NotificationsPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockGetNotifications.mockResolvedValue({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0, last: false } as PagedResponse<NotificationResponse>);
    mockMarkAsRead.mockResolvedValue(undefined);
  });

  it("shows the loading skeleton", async () => {
    mockGetNotifications.mockReturnValue(new Promise(() => {}));
    renderPage();

    expect(screen.queryByRole("heading", { name: "Notifications" })).not.toBeInTheDocument();
    expect(document.querySelectorAll(".animate-pulse")).toHaveLength(19);
  });

  it("shows an error state when notifications cannot be loaded", async () => {
    mockGetNotifications.mockRejectedValue(new Error("Request failed"));
    renderPage();

    expect(await screen.findByRole("heading", { name: "Notifications" })).toBeInTheDocument();
    expect(await screen.findByText("Failed to load notifications.")).toBeInTheDocument();
  });

  it("renders the empty state when there are no notifications", async () => {
    renderPage();
    expect(await screen.findByText("No notifications yet")).toBeInTheDocument();
  });

  it("renders notification types and formats relative timestamps", async () => {
    const now = Date.now();
    const ago = (seconds: number) =>
      new Date(now - seconds * 1000).toISOString();
    const data = [
      notification({ publicId: "n1", message: "Recent", createdAt: ago(30) }),
      notification({ publicId: "n2", message: "Minutes", createdAt: ago(300), type: "COMMENT" }),
      notification({ publicId: "n3", message: "Hours", createdAt: ago(7200), type: "FOLLOW" }),
      notification({ publicId: "n4", message: "Day", createdAt: ago(86400), type: "SYSTEM" }),
      notification({ publicId: "n5", message: "Days", createdAt: ago(259200), type: "OTHER" as never }),
      notification({ publicId: "n6", message: "Older", createdAt: ago(691200), isRead: true }),
    ];
    mockGetNotifications.mockResolvedValue({ content: data, page: 0, size: 20, totalElements: data.length, totalPages: 1, last: true } as PagedResponse<NotificationResponse>);
    renderPage();

    expect(await screen.findByText("6 notifications")).toBeInTheDocument();
    expect(screen.getByText("6 notifications")).toBeInTheDocument();
    expect(screen.getByText("Just now")).toBeInTheDocument();
    expect(screen.getByText("Minutes")).toBeInTheDocument();
    expect(screen.getByText("5m")).toBeInTheDocument();
    expect(screen.getByText("2h")).toBeInTheDocument();
    expect(screen.getByText("Yesterday")).toBeInTheDocument();
    expect(screen.getByText("3d")).toBeInTheDocument();
    expect(screen.getByText(new Date(ago(691200)).toLocaleDateString()))
      .toBeInTheDocument();
    expect(screen.getAllByRole("button")).toHaveLength(6);
  });

  it("marks unread items as read and navigates to related posts and profiles", async () => {
    const data = [
      notification({ publicId: "like", type: "LIKE" }),
      notification({ publicId: "comment", type: "COMMENT", isRead: true }),
      notification({ publicId: "follow", type: "FOLLOW", postPublicId: null }),
      notification({ publicId: "system", type: "SYSTEM", actorUsername: null, postPublicId: null }),
    ];
    mockGetNotifications.mockResolvedValue({ content: data, page: 0, size: 20, totalElements: data.length, totalPages: 1, last: true } as PagedResponse<NotificationResponse>);
    const { unmount } = renderPage();

    await screen.findAllByRole("button");
    fireEvent.click(screen.getAllByRole("button")[0]);
    await waitFor(() => expect(mockMarkAsRead).toHaveBeenCalledWith("like"));
    expect(screen.getByTestId("location")).toHaveTextContent("/ava/posts/post-1");
    unmount();

    mockMarkAsRead.mockClear();
    const comment = notification({ publicId: "comment", type: "COMMENT", isRead: true });
    mockGetNotifications.mockResolvedValue({ content: [comment], page: 0, size: 20, totalElements: 1, totalPages: 1, last: true } as PagedResponse<NotificationResponse>);
    const commentPage = renderPage();
    await screen.findByRole("button");
    fireEvent.click(screen.getByRole("button", { name: /A notification/ }));
    expect(mockMarkAsRead).not.toHaveBeenCalled();
    expect(screen.getByTestId("location")).toHaveTextContent("/ava/posts/post-1");
    commentPage.unmount();

    mockMarkAsRead.mockClear();
    const follow = notification({
      publicId: "follow",
      type: "FOLLOW",
      postPublicId: null,
    });
    mockGetNotifications.mockResolvedValue({ content: [follow], page: 0, size: 20, totalElements: 1, totalPages: 1, last: true } as PagedResponse<NotificationResponse>);
    const followPage = renderPage();
    await screen.findByRole("button");
    fireEvent.click(screen.getByRole("button", { name: /A notification/ }));
    await waitFor(() => expect(mockMarkAsRead).toHaveBeenCalledWith("follow"));
    expect(screen.getByTestId("location")).toHaveTextContent("/profile/ava");
    followPage.unmount();

    mockMarkAsRead.mockClear();
    const system = notification({
      publicId: "system",
      type: "SYSTEM",
      actorUsername: null,
      postPublicId: null,
    });
    mockGetNotifications.mockResolvedValue({ content: [system], page: 0, size: 20, totalElements: 1, totalPages: 1, last: true } as PagedResponse<NotificationResponse>);
    renderPage();
    await screen.findByRole("button");
    fireEvent.click(screen.getByRole("button", { name: /A notification/ }));
    await waitFor(() => expect(mockMarkAsRead).toHaveBeenCalledWith("system"));
    expect(screen.queryByTestId("location")).not.toBeInTheDocument();
  });
});
