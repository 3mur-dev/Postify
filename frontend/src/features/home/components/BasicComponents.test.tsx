import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { trackInteraction } from "../../../api/interactionApi";
import CharacterCounter from "./CharacterCounter";
import FollowButton from "./FollowButton";
import MediaPreview from "./MediaPreview";
import PostButton from "./PostButton";
import StatBadge from "./StatBadge";
import UploadButton from "./UploadButton";
import Avatar from "./Avatar";
import EmptyFeed from "../EmptyFeed";
import FeedLoading from "../FeedLoading";
import HomeSkeleton from "../HomeSkeleton";
import PostContent from "../PostContent";
import PostMenu from "../PostMenu";

vi.mock("../../../api/interactionApi", () => ({
  trackInteraction: vi.fn(),
}));

describe("home feed presentation components", () => {
  it("renders the empty state and loading skeletons", () => {
    const { unmount } = render(<EmptyFeed />);
    expect(screen.getByText("Your feed is quiet right now")).toBeInTheDocument();
    unmount();

    const feed = render(<FeedLoading />);
    expect(feed.container.querySelectorAll("article")).toHaveLength(2);
    feed.unmount();

    const home = render(<HomeSkeleton />);
    expect(home.container.querySelectorAll(".animate-pulse")).toHaveLength(4);
  });

  it("renders avatar fallback, image, and image-load fallback", () => {
    const { rerender } = render(<Avatar name="Ava" />);
    expect(screen.getByRole("img", { name: "Ava avatar" })).toBeInTheDocument();

    rerender(<Avatar name="Ava" imageUrl="https://cdn.example.com/avatar.png" size="xl" />);
    const avatarImage = screen.getByRole("img", { name: "Ava" });
    expect(avatarImage).toHaveAttribute(
      "src",
      "https://cdn.example.com/avatar.png",
    );
    fireEvent.error(avatarImage);
    expect(screen.getByRole("img", { name: "Ava avatar" })).toBeInTheDocument();
  });

  it("renders the character counter with warning and maximum states", () => {
    const { container, rerender } = render(<CharacterCounter current={50} max={100} />);
    expect(container.querySelector("circle[stroke='#38bdf8']")).toBeInTheDocument();
    expect(screen.queryByText("50")).not.toBeInTheDocument();

    rerender(<CharacterCounter current={80} max={100} />);
    expect(container.querySelector("circle[stroke='#f59e0b']")).toBeInTheDocument();
    expect(screen.getByText("20")).toBeInTheDocument();

    rerender(<CharacterCounter current={110} max={100} />);
    expect(container.querySelector("circle[stroke='#ef4444']")).toBeInTheDocument();
    expect(screen.getByText("-10")).toBeInTheDocument();
  });

  it("renders image and video previews and invokes removal", () => {
    const onRemove = vi.fn();
    const { rerender } = render(
      <MediaPreview preview={{ url: "blob:image", type: "IMAGE" }} onRemove={onRemove} />,
    );
    expect(screen.getByAltText("Post preview")).toHaveAttribute("src", "blob:image");
    fireEvent.click(screen.getByRole("button", { name: "Remove" }));
    expect(onRemove).toHaveBeenCalledOnce();

    rerender(
      <MediaPreview preview={{ url: "blob:video", type: "VIDEO" }} onRemove={onRemove} />,
    );
    expect(document.querySelector("video")).toHaveAttribute("src", "blob:video");
    expect(document.querySelector("video")).toHaveAttribute("controls");
  });

  it("renders stat values and supports follow and post button states", () => {
    render(<StatBadge label="posts" value={12} />);
    expect(screen.getByText("12")).toBeInTheDocument();

    render(<FollowButton />);
    fireEvent.click(screen.getByRole("button", { name: "Follow" }));
    expect(screen.getByRole("button", { name: "Following" })).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Following" }));
    expect(screen.getByRole("button", { name: "Follow" })).toBeInTheDocument();

    const onPost = vi.fn();
    const { rerender } = render(<PostButton onClick={onPost} />);
    fireEvent.click(screen.getByRole("button", { name: "Post" }));
    expect(onPost).toHaveBeenCalledOnce();
    rerender(<PostButton loading onClick={onPost} />);
    expect(screen.getByRole("button", { name: "Posting..." })).toBeDisabled();
    rerender(<PostButton disabled onClick={onPost} />);
    expect(screen.getByRole("button", { name: "Post" })).toBeDisabled();
  });

  it("invokes the upload action", () => {
    const onClick = vi.fn();
    render(<UploadButton onClick={onClick} />);
    fireEvent.click(screen.getByRole("button", { name: "Add Photo" }));
    expect(onClick).toHaveBeenCalledOnce();
  });

  it("links URLs in post content and tracks link clicks", async () => {
    const tracker = vi.mocked(trackInteraction);
    tracker.mockResolvedValue(undefined);
    render(
      <PostContent
        content="Read https://example.com/page today"
        postPublicId="post-1"
        source="FEED"
      />,
    );

    const link = screen.getByRole("link", { name: "https://example.com/page" });
    expect(link).toHaveAttribute("target", "_blank");
    expect(link).toHaveAttribute("rel", "noopener noreferrer");
    fireEvent.click(link);
    expect(tracker).toHaveBeenCalledWith({
      postPublicId: "post-1",
      eventType: "LINK_CLICK",
      source: "FEED",
      metadata: "https://example.com/page",
    });
  });

  it("opens the post menu and displays available actions", () => {
    render(<PostMenu />);
    fireEvent.click(screen.getByRole("button"));
    for (const label of ["Edit", "Delete", "Report", "Copy Link", "Hide"]) {
      expect(screen.getByRole("button", { name: label })).toBeInTheDocument();
    }
  });
});
