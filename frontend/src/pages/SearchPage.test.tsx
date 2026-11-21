import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes, useLocation } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { PostFeedDto, SearchResponse } from "../api/types";
import { createConversation } from "../api/conversationApi";
import { useAuth } from "../context/useAuth";
import { useGetSearchResults } from "../hooks/useGetSearchResults";
import SearchPage from "./SearchPage";

vi.mock("../api/conversationApi", () => ({ createConversation: vi.fn() }));
vi.mock("../context/useAuth", () => ({ useAuth: vi.fn() }));
vi.mock("../hooks/useGetSearchResults", () => ({
  useGetSearchResults: vi.fn(),
}));

const mockCreateConversation = vi.mocked(createConversation);
const mockUseAuth = vi.mocked(useAuth);
const mockUseGetSearchResults = vi.mocked(useGetSearchResults);
const currentUser = { username: "ava" } as never;
const posts = [
  {
    id: 1,
    publicId: "post-1",
    username: "sam",
    content: "Text post",
    media: [],
    createdAt: "2025-01-01T00:00:00Z",
    avatarUrl: null,
    likeCount: 0,
    commentCount: 0,
    likedByCurrentUser: false,
    savedByCurrentUser: false,
  },
  {
    id: 2,
    publicId: "post-2",
    username: "lee",
    content: null,
    media: [
      {
        id: 2,
        url: "/uploads/photo.jpg",
        type: "IMAGE" as const,
        position: 0,
        width: null,
        height: null,
        durationMs: null,
      },
    ],
    createdAt: "2025-01-01T00:00:00Z",
    avatarUrl: null,
    likeCount: 0,
    commentCount: 0,
    likedByCurrentUser: false,
    savedByCurrentUser: false,
  },
] satisfies PostFeedDto[];
const users = [
  { id: 1, publicId: "ava-id", username: "ava", displayName: "Ava", avatarUrl: null },
  { id: 2, publicId: "sam-id", username: "sam", displayName: "Sam", avatarUrl: null },
] satisfies SearchResponse["users"];

function LocationDisplay() {
  const location = useLocation();
  return <output data-testid="location">{location.pathname}</output>;
}

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={["/search"]}>
        <Routes>
          <Route path="/search" element={<SearchPage />} />
          <Route path="*" element={<LocationDisplay />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

function setSearchResult(
  data: SearchResponse,
  state: { isLoading?: boolean; isError?: boolean } = {},
) {
  mockUseGetSearchResults.mockReturnValue({
    data,
    isLoading: false,
    isError: false,
    ...state,
  } as never);
}

describe("SearchPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    setSearchResult({ posts: [], users: [] });
    mockUseAuth.mockReturnValue({
      user: currentUser,
      loading: false,
      isAuthenticated: true,
      refreshUser: vi.fn(),
      logout: vi.fn(),
    });
  });

  it("renders the default posts tab and provides clear-search behavior", () => {
    renderPage();
    expect(screen.getByText("No posts found")).toBeInTheDocument();

    const search = screen.getByPlaceholderText("Search posts and people");
    fireEvent.change(search, { target: { value: "postify" } });
    expect(screen.getByRole("button", { name: "Clear search" })).toBeInTheDocument();
    expect(mockUseGetSearchResults).toHaveBeenLastCalledWith("postify");
    fireEvent.click(screen.getByRole("button", { name: "Clear search" }));
    expect(search).toHaveValue("");
    expect(mockUseGetSearchResults).toHaveBeenLastCalledWith("");
  });

  it("shows loading skeletons for posts and people", () => {
    setSearchResult({ posts: [], users: [] }, { isLoading: true });
    const { unmount } = renderPage();
    expect(document.querySelectorAll(".animate-pulse")).toHaveLength(9);
    unmount();

    renderPage();
    fireEvent.click(screen.getByRole("button", { name: "People" }));
    expect(document.querySelectorAll(".animate-pulse")).toHaveLength(15);
  });

  it("shows search errors and empty people results", () => {
    setSearchResult({ posts: [], users: [] }, { isError: true });
    const { rerender } = renderPage();
    expect(screen.getByText("Something went wrong")).toBeInTheDocument();
    expect(screen.getByText("Try searching again.")).toBeInTheDocument();

    setSearchResult({ posts: [], users: [] });
    rerender(
      <QueryClientProvider client={new QueryClient()}>
        <MemoryRouter initialEntries={["/search"]}>
          <Routes>
            <Route path="/search" element={<SearchPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );
    fireEvent.click(screen.getByRole("button", { name: "People" }));
    expect(screen.getByText("No people found")).toBeInTheDocument();
  });

  it("renders text and image post tiles and navigates to the selected post", () => {
    setSearchResult({ posts, users: [] });
    renderPage();

    expect(screen.getByText("Text post")).toBeInTheDocument();
    const imagePost = screen.getByRole("img", { name: "Post" });
    expect(imagePost).toHaveAttribute("src", "http://localhost:8080/uploads/photo.jpg");
    fireEvent.click(screen.getByRole("button", { name: "Text post" }));
    expect(screen.getByTestId("location")).toHaveTextContent("/sam/posts/post-1");
  });

  it("renders people, hides self-messaging, and navigates to profiles", () => {
    setSearchResult({ posts: [], users });
    mockUseAuth.mockReturnValue({
      user: currentUser,
      loading: false,
      isAuthenticated: true,
      refreshUser: vi.fn(),
      logout: vi.fn(),
    });
    renderPage();
    fireEvent.click(screen.getByRole("button", { name: "People" }));

    expect(screen.queryByRole("button", { name: "Message ava" })).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Message sam" })).toBeInTheDocument();
    fireEvent.click(screen.getByText("sam"));
    expect(screen.getByTestId("location")).toHaveTextContent("/profile/sam");
  });

  it("starts a conversation and navigates to its encoded route", async () => {
    setSearchResult({ posts: [], users });
    mockCreateConversation.mockResolvedValue({ publicId: "thread/one" });
    renderPage();
    fireEvent.click(screen.getByRole("button", { name: "People" }));
    fireEvent.click(screen.getByRole("button", { name: "Message sam" }));

    await waitFor(() => {
      expect(mockCreateConversation.mock.calls[0][0]).toBe("sam-id");
      expect(screen.getByTestId("location")).toHaveTextContent(
        "/messages/thread%2Fone",
      );
    });
  });

  it("does not show message actions for unauthenticated users", () => {
    setSearchResult({ posts: [], users });
    mockUseAuth.mockReturnValue({
      user: null,
      loading: false,
      isAuthenticated: false,
      refreshUser: vi.fn(),
      logout: vi.fn(),
    });
    renderPage();
    fireEvent.click(screen.getByRole("button", { name: "People" }));

    expect(screen.getByText("sam")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Message sam" })).not.toBeInTheDocument();
  });
});
