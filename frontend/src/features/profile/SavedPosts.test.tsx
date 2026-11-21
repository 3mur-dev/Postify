import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { getSavedPosts } from "../../api/saveApi";
import type { SavedPostResponse } from "../../api/types";
import { SavedPosts } from "./SavedPosts";

vi.mock("../../api/saveApi", () => ({ getSavedPosts: vi.fn() }));
vi.mock("../../api/interactionApi", () => ({ trackInteraction: vi.fn() }));
vi.mock("../home/components/Avatar", () => ({
  default: () => <span>Avatar</span>,
}));
vi.mock("../home/PostContent", () => ({
  default: ({ content }: { content: string }) => <p>{content}</p>,
}));

const mockGetSavedPosts = vi.mocked(getSavedPosts);
const postResponse: SavedPostResponse = {
  savedAt: "2025-01-02T00:00:00.000Z",
  post: {
    id: 1,
    publicId: "post-1",
    content: "Saved content",
    media: [],
    createdAt: "2025-01-01T00:00:00.000Z",
    username: "sam",
    avatarUrl: null,
    likeCount: 10,
    commentCount: 2,
    likedByCurrentUser: false,
    savedByCurrentUser: true,
  },
};

function renderSavedPosts(onLike = vi.fn<() => Promise<void>>()) {
  return render(
    <MemoryRouter initialEntries={["/profile/ava/saved"]}>
      <Routes>
        <Route
          path="/profile/:username/saved"
          element={<SavedPosts onLike={onLike} />}
        />
        <Route
          path="/:username/posts/:publicId"
          element={<div>Post detail route</div>}
        />
      </Routes>
    </MemoryRouter>,
  );
}

describe("SavedPosts", () => {
  beforeEach(() => vi.clearAllMocks());

  it("loads saved posts and renders the empty state", async () => {
    mockGetSavedPosts.mockResolvedValue({ content: [] } as never);
    const { container } = renderSavedPosts();

    expect(container.querySelector(".animate-spin")).toBeInTheDocument();
    expect(await screen.findByText("No saved posts")).toBeInTheDocument();
    expect(mockGetSavedPosts).toHaveBeenCalledWith("ava");
  });

  it("shows a retry action after loading errors and recovers on retry", async () => {
    mockGetSavedPosts
      .mockRejectedValueOnce(new Error("Network unavailable"))
      .mockResolvedValueOnce({ content: [postResponse] } as never);
    renderSavedPosts();

    fireEvent.click(
      await screen.findByRole("button", { name: "Try again" }),
    );
    expect(await screen.findByText("Saved content")).toBeInTheDocument();
    expect(mockGetSavedPosts).toHaveBeenCalledTimes(2);
  });

  it("updates likes optimistically and rolls them back when the request fails", async () => {
    mockGetSavedPosts.mockResolvedValue({ content: [postResponse] } as never);
    let rejectLike!: (reason: Error) => void;
    const onLike = vi.fn(
      () =>
        new Promise<void>((_, reject) => {
          rejectLike = reject;
        }),
    );
    renderSavedPosts(onLike);

    await screen.findByText("Saved content");
    fireEvent.click(screen.getByRole("button", { name: "Like post" }));
    expect(screen.getByText("11")).toBeInTheDocument();
    expect(onLike).toHaveBeenCalledWith("post-1");

    rejectLike(new Error("Like failed"));
    await waitFor(() => expect(screen.getByText("10")).toBeInTheDocument());
  });
});
