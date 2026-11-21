import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { CommentDto } from "../../api/types";
import { hasAccessToken } from "../../lib/auth";
import { useAddComment } from "../../hooks/useAddComments";
import { useComments } from "../../hooks/useComments";
import CommentsModal from "./CommentsModal";
import FeedActions from "./FeedActions";
import HomeFeed from "./HomeFeed";

vi.mock("../../lib/auth", () => ({ hasAccessToken: vi.fn() }));
vi.mock("../../hooks/useAddComments", () => ({ useAddComment: vi.fn() }));
vi.mock("../../hooks/useComments", () => ({ useComments: vi.fn() }));
vi.mock("./FeedPost", () => ({
  default: (props: { post: { publicId: string }; setCommentsPublicId: (id: string) => void }) => (
    <button type="button" onClick={() => props.setCommentsPublicId(props.post.publicId)}>
      Open {props.post.publicId}
    </button>
  ),
}));

const mockHasAccessToken = vi.mocked(hasAccessToken);
const mockUseAddComment = vi.mocked(useAddComment);
const mockUseComments = vi.mocked(useComments);
const mutateAsync = vi.fn();
const comment: CommentDto = {
  id: 1,
  content: "Nice post",
  createdAt: "2025-06-15T11:00:00Z",
  userPublicId: "user-1",
  username: "ava",
  userAvatarUrl: null,
  postPublicId: "post-1",
};

describe("FeedActions", () => {
  it("renders counts and dispatches optional action callbacks", () => {
    const onLike = vi.fn();
    const onComment = vi.fn();
    const onShare = vi.fn();
    const onSave = vi.fn();
    render(
      <FeedActions
        publicId="post-1"
        likes={1234}
        comments={5}
        liked
        saved
        commentsOpen
        onLike={onLike}
        onComment={onComment}
        onShare={onShare}
        onSave={onSave}
      />,
    );

    expect(screen.getByText("1234")).toBeInTheDocument();
    expect(screen.getByText("5")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Like" })).toHaveAttribute(
      "aria-pressed",
      "true",
    );
    expect(screen.getByRole("button", { name: "Comment" })).toHaveAttribute(
      "aria-pressed",
      "true",
    );

    fireEvent.click(screen.getByRole("button", { name: "Like" }));
    fireEvent.click(screen.getByRole("button", { name: "Comment" }));
    fireEvent.click(screen.getByRole("button", { name: "Share" }));
    fireEvent.click(screen.getByRole("button", { name: "Save" }));
    expect(onLike).toHaveBeenCalledWith("post-1");
    expect(onComment).toHaveBeenCalledWith("post-1");
    expect(onShare).toHaveBeenCalledWith("post-1");
    expect(onSave).toHaveBeenCalledWith("post-1");

    const second = render(
      <FeedActions publicId="post-2" likes={0} comments={0} liked={false} />,
    );
    fireEvent.click(within(second.container).getByRole("button", { name: "Share" }));
  });
});

describe("HomeFeed", () => {
  it("renders posts and manages the selected comments modal", () => {
    mockUseComments.mockReturnValue({
      data: [],
      isPending: false,
      isSuccess: true,
      isError: false,
      error: null,
    } as never);
    mockUseAddComment.mockReturnValue({
      mutateAsync,
      isPending: false,
    } as never);
    mockHasAccessToken.mockReturnValue(false);
    render(<HomeFeed posts={[{ publicId: "post-1" }, { publicId: "post-2" }] as never} />);
    expect(screen.getByRole("button", { name: "Open post-1" })).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Open post-2" }));
    expect(screen.getByRole("dialog", { name: "Comments" })).toBeInTheDocument();
  });
});

describe("CommentsModal", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockHasAccessToken.mockReturnValue(true);
    mockUseAddComment.mockReturnValue({
      mutateAsync,
      isPending: false,
    } as never);
    mockUseComments.mockReturnValue({
      data: [],
      isPending: false,
      isSuccess: true,
      isError: false,
      error: null,
    } as never);
  });

  it("shows comment loading and error states with an accessible fallback", () => {
    mockUseComments.mockReturnValue({
      data: undefined,
      isPending: true,
      isSuccess: false,
      isError: false,
      error: null,
    } as never);
    const { rerender } = render(
      <CommentsModal publicId="post-1" onClose={vi.fn()} source="FEED" />,
    );
    expect(screen.getByText("Loading comments...")).toBeInTheDocument();

    mockUseComments.mockReturnValue({
      data: undefined,
      isPending: false,
      isSuccess: false,
      isError: true,
      error: new Error("Unable to reach server"),
    } as never);
    rerender(<CommentsModal publicId="post-1" onClose={vi.fn()} source="FEED" />);
    expect(screen.getByText("Unable to load comments")).toBeInTheDocument();
    expect(screen.getByText("Unable to reach server")).toBeInTheDocument();

    mockUseComments.mockReturnValue({
      data: undefined,
      isPending: false,
      isSuccess: false,
      isError: true,
      error: "failure" as never,
    } as never);
    rerender(<CommentsModal publicId="post-1" onClose={vi.fn()} source="FEED" />);
    expect(screen.getByText("Please try again shortly.")).toBeInTheDocument();
  });

  it("shows an empty state, renders comment details, and closes on Escape or backdrop", () => {
    const onClose = vi.fn();
    const { rerender } = render(
      <CommentsModal publicId="post-1" onClose={onClose} source="FEED" />,
    );
    expect(screen.getByText("No comments yet")).toBeInTheDocument();

    mockUseComments.mockReturnValue({
      data: [comment],
      isPending: false,
      isSuccess: true,
      isError: false,
      error: null,
    } as never);
    rerender(<CommentsModal publicId="post-1" onClose={onClose} source="FEED" />);
    expect(screen.getByText("Nice post")).toBeInTheDocument();
    expect(screen.getByText("ava")).toBeInTheDocument();
    fireEvent.keyDown(window, { key: "Escape" });
    expect(onClose).toHaveBeenCalledOnce();

    fireEvent.click(screen.getByRole("dialog", { name: "Comments" }).parentElement!);
    expect(onClose).toHaveBeenCalledTimes(2);
  });

  it("posts trimmed comments, clears the composer, and reports submission errors", async () => {
    mutateAsync.mockResolvedValue(undefined);
    render(<CommentsModal publicId="post-1" onClose={vi.fn()} source="PROFILE" />);
    const input = screen.getByRole("textbox", { name: "Write a comment" });
    fireEvent.change(input, { target: { value: "  Hello there  " } });
    fireEvent.keyDown(input, { key: "Enter" });

    await waitFor(() => {
      expect(mutateAsync).toHaveBeenCalledWith({
        publicId: "post-1",
        content: "Hello there",
        source: "PROFILE",
      });
    });
    await waitFor(() => expect(input).toHaveValue(""));

    mutateAsync.mockRejectedValueOnce(new Error("Network error"));
    fireEvent.change(input, { target: { value: "Retry me" } });
    fireEvent.click(screen.getByRole("button", { name: "Send comment" }));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "We couldn't post your comment. Please try again.",
    );
  });

  it("disables comment submission for guests and keeps blank submissions inactive", () => {
    mockHasAccessToken.mockReturnValue(false);
    render(<CommentsModal publicId="post-1" onClose={vi.fn()} source="FEED" />);

    expect(screen.getByRole("textbox", { name: "Write a comment" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Send comment" })).toBeDisabled();
    expect(mutateAsync).not.toHaveBeenCalled();
  });
});
