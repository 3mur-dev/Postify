import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes, useLocation } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { PostFeedDto } from "../api/types";
import { useLikePost, usePost } from "../hooks/usePost";
import { useSavePost } from "../hooks/useSavePost";
import PostPage from "./PostPage";

vi.mock("../hooks/usePost", () => ({
  usePost: vi.fn(),
  useLikePost: vi.fn(),
}));
vi.mock("../hooks/useSavePost", () => ({ useSavePost: vi.fn() }));
vi.mock("../features/home/FeedHeader", () => ({
  default: () => <div>Post author header</div>,
}));
vi.mock("../features/home/FeedMedia", () => ({
  default: () => <div>Post media</div>,
}));
vi.mock("../features/home/PostContent", () => ({
  default: ({ content }: { content: string }) => <div>{content}</div>,
}));
vi.mock("../features/home/FeedActions", () => ({
  default: (props: {
    publicId: string;
    onLike: () => void;
    onSave: (publicId: string) => void;
    onComment: () => void;
  }) => (
    <div>
      <button type="button" onClick={props.onLike}>Like post</button>
      <button type="button" onClick={() => props.onSave(props.publicId)}>Save post</button>
      <button type="button" onClick={props.onComment}>Open comments</button>
    </div>
  ),
}));
vi.mock("../features/home/CommentsModal", () => ({
  default: (props: { onClose: () => void; publicId: string }) => (
    <div>
      Comments for {props.publicId}
      <button type="button" onClick={props.onClose}>Close comments</button>
    </div>
  ),
}));

const mockUsePost = vi.mocked(usePost);
const mockUseLikePost = vi.mocked(useLikePost);
const mockUseSavePost = vi.mocked(useSavePost);
const mutateLike = vi.fn();
const mutateSave = vi.fn();
const post = {
  id: 1,
  publicId: "post-1",
  content: "Post body",
  createdAt: "2025-01-01T00:00:00Z",
  media: [],
  username: "ava",
  avatarUrl: null,
  likeCount: 10,
  commentCount: 2,
  likedByCurrentUser: false,
  savedByCurrentUser: false,
} satisfies PostFeedDto;

function LocationDisplay() {
  const location = useLocation();
  return <output data-testid="location">{location.pathname}</output>;
}

function renderPostPage() {
  return render(
    <MemoryRouter initialEntries={["/", "/ava/posts/post-1"]} initialIndex={1}>
      <Routes>
        <Route path="/:username/posts/:publicId" element={<PostPage />} />
        <Route path="/" element={<LocationDisplay />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe("PostPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUsePost.mockReturnValue({
      data: post,
      isLoading: false,
      isError: false,
    } as never);
    mockUseLikePost.mockReturnValue({ mutateAsync: mutateLike } as never);
    mockUseSavePost.mockReturnValue({ mutateAsync: mutateSave } as never);
  });

  it("shows a loading placeholder while the post is fetched", () => {
    mockUsePost.mockReturnValue({
      data: undefined,
      isLoading: true,
      isError: false,
    } as never);
    renderPostPage();
    expect(document.querySelector(".animate-pulse")).toBeInTheDocument();
  });

  it("shows a recoverable error state when the post is unavailable", () => {
    mockUsePost.mockReturnValue({
      data: undefined,
      isLoading: false,
      isError: true,
    } as never);
    renderPostPage();
    expect(screen.getByText("something went wrong")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("link", { name: "Go Home" }));
    expect(screen.getByTestId("location")).toHaveTextContent("/");
  });

  it("renders post content and actions, and opens and closes comments", () => {
    renderPostPage();

    expect(mockUsePost).toHaveBeenCalledWith("post-1");
    expect(screen.getByText("Post author header")).toBeInTheDocument();
    expect(screen.getByText("Post body")).toBeInTheDocument();
    expect(screen.getByText("Post media")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Like post" }));
    expect(mutateLike).toHaveBeenCalledWith({
      publicId: "post-1",
      source: "POST_DETAIL",
    });

    fireEvent.click(screen.getByRole("button", { name: "Save post" }));
    expect(mutateSave).toHaveBeenCalledWith({
      publicId: "post-1",
      saved: false,
      source: "POST_DETAIL",
    });

    fireEvent.click(screen.getByRole("button", { name: "Open comments" }));
    expect(screen.getByText("Comments for post-1")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Close comments" }));
    expect(screen.queryByText("Comments for post-1")).not.toBeInTheDocument();
  });

  it("omits empty post text and navigates back", () => {
    mockUsePost.mockReturnValue({
      data: { ...post, content: null },
      isLoading: false,
      isError: false,
    } as never);
    renderPostPage();

    expect(screen.queryByText("Post body")).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Back" }));
    expect(screen.getByTestId("location")).toHaveTextContent("/");
  });
});
