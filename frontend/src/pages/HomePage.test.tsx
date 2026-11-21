import { render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type { ProfileResponse, PostFeedDto } from "../api/types";
import { useInfiniteFeed } from "../hooks/useInfiniteFeed";
import HomePage from "./HomePage";

vi.mock("@tanstack/react-query", () => ({
  useQuery: vi.fn(() => ({ data: { username: "ava" } })),
}));
vi.mock("../lib/auth", () => ({ hasAccessToken: vi.fn(() => true) }));
vi.mock("../hooks/useInfiniteFeed", () => ({ useInfiniteFeed: vi.fn() }));
vi.mock("../features/home/FeedLoading", () => ({
  default: () => <div>Loading more posts</div>,
}));
vi.mock("../features/home/HomeSkeleton", () => ({
  default: () => <div>Home skeleton</div>,
}));
vi.mock("../features/home/components/CreatePostCard", () => ({
  default: ({ user }: { user: ProfileResponse }) => (
    <div>Create post for {user.username}</div>
  ),
}));
vi.mock("../features/home/HomeFeed", () => ({
  default: ({ posts }: { posts: PostFeedDto[] }) => (
    <div>Post count: {posts.length}</div>
  ),
}));
vi.mock("../features/home/EmptyFeed", () => ({
  default: () => <div>Empty feed</div>,
}));
vi.mock("../features/home/SuggestedUsers", () => ({
  default: () => <div>Suggested users</div>,
}));

const mockUseInfiniteFeed = vi.mocked(useInfiniteFeed);
const post = { publicId: "post-1" } as PostFeedDto;
let observerCallback: IntersectionObserverCallback;
const observers: MockIntersectionObserver[] = [];

class MockIntersectionObserver {
  constructor(callback: IntersectionObserverCallback) {
    observerCallback = callback;
    observers.push(this);
  }

  observe = vi.fn();
  disconnect = vi.fn();
  unobserve = vi.fn();
  takeRecords = vi.fn(() => []);
  root = null;
  rootMargin = "400px";
  thresholds = [];
}

function setFeed(
  result: Partial<ReturnType<typeof useInfiniteFeed>>,
) {
  mockUseInfiniteFeed.mockReturnValue({
    data: { pages: [{ content: [post] }] },
    error: null,
    isLoading: false,
    fetchNextPage: vi.fn(),
    hasNextPage: false,
    isFetchingNextPage: false,
    ...result,
  } as ReturnType<typeof useInfiniteFeed>);
}

describe("HomePage", () => {
  beforeEach(() => {
    observers.length = 0;
    vi.stubGlobal("IntersectionObserver", MockIntersectionObserver);
    vi.spyOn(console, "log").mockImplementation(() => undefined);
    setFeed({});
  });

  afterEach(() => vi.unstubAllGlobals());

  it("shows the loading skeleton", () => {
    setFeed({ isLoading: true, data: undefined });
    render(<HomePage />);
    expect(screen.getByText("Home skeleton")).toBeInTheDocument();
  });

  it("shows an error message from Error objects and a fallback for other values", () => {
    const { rerender } = render(
      <HomePageWithFeed error={new Error("Feed unavailable")} />,
    );
    expect(screen.getByText("Feed unavailable")).toBeInTheDocument();

    setFeed({ error: "failure" as never });
    rerender(<HomePage />);
    expect(screen.getByText("Failed to load feed.")).toBeInTheDocument();
  });

  it("deduplicates overlapping pages and shows the caught-up state", () => {
    setFeed({
      data: { pages: [{ content: [post] }, { content: [post] }] } as never,
      hasNextPage: false,
    });
    render(<HomePage />);

    expect(screen.getByText("Create post for ava")).toBeInTheDocument();
    expect(screen.getByText("Post count: 1")).toBeInTheDocument();
    expect(screen.getByText("You're all caught up.")).toBeInTheDocument();
    expect(screen.getByText("Suggested users")).toBeInTheDocument();
  });

  it("shows the empty feed and loading indicator for the next page", () => {
    setFeed({
      data: { pages: [{ content: [] }] } as never,
      hasNextPage: true,
      isFetchingNextPage: true,
    });
    render(<HomePage />);

    expect(screen.getByText("Empty feed")).toBeInTheDocument();
    expect(screen.getByText("Loading more posts")).toBeInTheDocument();
    expect(screen.queryByText("You're all caught up.")).not.toBeInTheDocument();
  });

  it("fetches the next page only when the sentinel intersects and no page is already loading", () => {
    const fetchNextPage = vi.fn();
    setFeed({ fetchNextPage, hasNextPage: true });
    const { unmount, container } = render(<HomePage />);
    const sentinel = container.querySelector("section > div:last-child");

    expect(sentinel).not.toBeNull();
    expect(observerCallback).toBeTypeOf("function");
    expect(observers[0].observe).toHaveBeenCalledWith(sentinel);

    observerCallback(
      [{ isIntersecting: true } as IntersectionObserverEntry],
      {} as IntersectionObserver,
    );
    expect(fetchNextPage).toHaveBeenCalledOnce();

    setFeed({ fetchNextPage, hasNextPage: false });
    unmount();
    render(<HomePage />);
    observerCallback(
      [{ isIntersecting: true } as IntersectionObserverEntry],
      {} as IntersectionObserver,
    );
    expect(fetchNextPage).toHaveBeenCalledOnce();

    expect(observers[0].disconnect).toHaveBeenCalledOnce();
  });
});

function HomePageWithFeed({ error }: { error: Error }) {
  setFeed({ error });
  return <HomePage />;
}
