import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes, useLocation } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { ConversationSummary, ProfileResponse } from "../api/types";
import { useAuth } from "../context/useAuth";
import { useConversations } from "../hooks/useConversations";
import DirectMessagesPage from "./DirectMessagesPage";

vi.mock("../context/useAuth", () => ({ useAuth: vi.fn() }));
vi.mock("../hooks/useConversations", () => ({ useConversations: vi.fn() }));
vi.mock("../features/messaging/ConversationInbox", () => ({
  default: (props: {
    currentUsername: string;
    isLoading: boolean;
    isError: boolean;
    onRetry: () => void;
  }) => (
    <div>
      Inbox for {props.currentUsername} loading={String(props.isLoading)} error={String(props.isError)}
      <button type="button" onClick={props.onRetry}>Retry inbox</button>
    </div>
  ),
}));
vi.mock("../features/messaging/ConversationPanel", () => ({
  default: (props: { conversationPublicId: string; currentUsername: string }) => (
    <div>
      Panel {props.conversationPublicId} for {props.currentUsername}
    </div>
  ),
}));

const mockUseAuth = vi.mocked(useAuth);
const mockUseConversations = vi.mocked(useConversations);
const refetch = vi.fn();
const profile = { username: "ava" } as ProfileResponse;
const conversation = {
  publicId: "thread-1",
  participant: { publicId: "user-2", username: "sam", avatarUrl: null },
  latestMessage: null,
  updatedAt: "2025-01-01T00:00:00Z",
} satisfies ConversationSummary;

function LocationDisplay() {
  const location = useLocation();
  return <output data-testid="location">{location.pathname}</output>;
}

function renderPage(path = "/messages") {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/messages" element={<DirectMessagesPage />} />
        <Route path="/messages/:conversationPublicId" element={<DirectMessagesPage />} />
        <Route path="/login" element={<LocationDisplay />} />
        <Route path="/search" element={<LocationDisplay />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe("DirectMessagesPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUseAuth.mockReturnValue({
      user: profile,
      loading: false,
      isAuthenticated: true,
      refreshUser: vi.fn(),
      logout: vi.fn(),
    });
    mockUseConversations.mockReturnValue({
      data: [conversation],
      isLoading: false,
      isError: false,
      refetch,
      } as never);
  });

  it("waits for authentication before rendering message routes", () => {
    mockUseAuth.mockReturnValue({
      user: null,
      loading: true,
      isAuthenticated: false,
      refreshUser: vi.fn(),
      logout: vi.fn(),
    });
    renderPage();
    expect(screen.getByText("Checking your session")).toBeInTheDocument();
    expect(screen.queryByText("Sign in to see your messages")).not.toBeInTheDocument();
  });

  it("prompts guests to sign in and preserves their messages location", () => {
    mockUseAuth.mockReturnValue({
      user: null,
      loading: false,
      isAuthenticated: false,
      refreshUser: vi.fn(),
      logout: vi.fn(),
    });
    renderPage();

    expect(screen.getByRole("heading", { name: "Sign in to see your messages" }))
      .toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Sign in" }));
    expect(screen.getByTestId("location")).toHaveTextContent("/login");
  });

  it("renders the inbox and empty conversation prompt for an authenticated user", () => {
    renderPage();

    expect(screen.getByText("Inbox for ava loading=false error=false")).toBeInTheDocument();
    expect(screen.getByRole("heading", {
      name: "Your conversations, all in one place",
    })).toBeInTheDocument();
    fireEvent.click(screen.getByRole("link", { name: "New conversation" }));
    expect(screen.getByTestId("location")).toHaveTextContent("/search");
  });

  it("renders the selected conversation panel and inbox error/loading props", () => {
    mockUseConversations.mockReturnValue({
      data: [conversation],
      isLoading: true,
      isError: true,
      refetch,
    } as never);
    renderPage("/messages/thread-1");

    expect(screen.getByText("Inbox for ava loading=true error=true")).toBeInTheDocument();
    expect(screen.getByText("Panel thread-1 for ava")).toBeInTheDocument();
    expect(mockUseConversations).toHaveBeenCalledWith(true);
  });

  it("supports retry from the inbox", () => {
    mockUseConversations.mockReturnValue({
      data: [],
      isLoading: false,
      isError: true,
      refetch,
    } as never);
    renderPage();

    expect(screen.getByText("Inbox for ava loading=false error=true")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Retry inbox" }));
    expect(refetch).toHaveBeenCalledOnce();
  });
});
