import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, renderHook } from "@testing-library/react";
import type { ReactNode } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { addComment } from "../api/commentApi";
import { toggleFollow } from "../api/followApi";
import { togglePostLike } from "../api/likeApi";
import { deletePostApi, editPostApi, createPostApi } from "../api/postApi";
import { savePost, unsavePost } from "../api/saveApi";
import { toast } from "sonner";
import type { CommentDto, PostFeedDto, ProfileResponse } from "../api/types";
import { useAddComment } from "./useAddComments";
import {
  useCreatePost,
  useDeletePost,
  useEditPost,
  useLikePost,
} from "./usePost";
import { useSavePost } from "./useSavePost";
import { useToggleFollow } from "./useToggleFollow";

vi.mock("../api/commentApi", () => ({ addComment: vi.fn() }));
vi.mock("../api/postApi", () => ({
  createPostApi: vi.fn(),
  deletePostApi: vi.fn(),
  editPostApi: vi.fn(),
}));
vi.mock("../api/likeApi", () => ({ togglePostLike: vi.fn() }));
vi.mock("../api/saveApi", () => ({
  savePost: vi.fn(),
  unsavePost: vi.fn(),
}));
vi.mock("../api/followApi", () => ({ toggleFollow: vi.fn() }));
vi.mock("sonner", () => ({ toast: { error: vi.fn() } }));

const mockAddComment = vi.mocked(addComment);
const mockCreatePost = vi.mocked(createPostApi);
const mockDeletePost = vi.mocked(deletePostApi);
const mockEditPost = vi.mocked(editPostApi);
const mockSavePost = vi.mocked(savePost);
const mockUnsavePost = vi.mocked(unsavePost);
const mockToggleLike = vi.mocked(togglePostLike);
const mockToggleFollow = vi.mocked(toggleFollow);
const post = {
  id: 1,
  publicId: "post-1",
  content: "Post content",
  media: [],
  createdAt: "2025-01-01T00:00:00Z",
  username: "ava",
  avatarUrl: null,
  likeCount: 10,
  commentCount: 2,
  likedByCurrentUser: false,
  savedByCurrentUser: false,
} satisfies PostFeedDto;

function createQueryProvider() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: Infinity },
      mutations: { retry: false },
    },
  });
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  return { queryClient, wrapper };
}

describe("post and comment mutations", () => {
  beforeEach(() => vi.clearAllMocks());

  it("creates a post and invalidates feed and profile caches", async () => {
    mockCreatePost.mockResolvedValue(post);
    const { queryClient, wrapper } = createQueryProvider();
    const invalidate = vi.spyOn(queryClient, "invalidateQueries");
    const create = renderHook(() => useCreatePost(), { wrapper });

    await act(async () =>
      create.result.current.mutateAsync({ content: "Post content", media: [] }),
    );

    expect(mockCreatePost).toHaveBeenCalledWith("Post content", []);
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ["postFeed"] });
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ["profile"] });
  });

  it("deletes and edits posts then invalidates their feed and profile caches", async () => {
    mockDeletePost.mockResolvedValue(undefined);
    mockEditPost.mockResolvedValue(undefined);
    const { queryClient, wrapper } = createQueryProvider();
    const invalidate = vi.spyOn(queryClient, "invalidateQueries");
    const deletion = renderHook(() => useDeletePost(), { wrapper });
    await act(async () =>
      deletion.result.current.mutateAsync({
        publicId: "post-1",
        profileQueryUsername: "ava",
      }),
    );
    expect(mockDeletePost).toHaveBeenCalledWith("post-1");
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ["postFeed"] });
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ["profile", "ava"] });

    invalidate.mockClear();
    const edit = renderHook(() => useEditPost(), { wrapper });
    await act(async () =>
      edit.result.current.mutateAsync({
        publicId: "post-1",
        content: "Changed",
        profileQueryUsername: "ava",
      }),
    );
    expect(mockEditPost).toHaveBeenCalledWith("post-1", "Changed");
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ["postFeed"] });
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ["profile", "ava"] });
  });

  it("reports post mutation errors", async () => {
    mockCreatePost.mockRejectedValue(new Error("Create failed"));
    const { wrapper } = createQueryProvider();
    const create = renderHook(() => useCreatePost(), { wrapper });

    await act(async () => {
      await expect(
        create.result.current.mutateAsync({ content: "Post content", media: [] }),
      ).rejects.toThrow("Create failed");
    });
    expect(toast.error).toHaveBeenCalledWith("Create failed");
  });

  it("optimistically updates and rolls back post likes", async () => {
    mockToggleLike.mockResolvedValue(undefined);
    const { queryClient, wrapper } = createQueryProvider();
    queryClient.setQueryData(["postFeed"], {
      pages: [{ content: [post] }, { content: [{ ...post }] }],
      pageParams: [0, 1],
    });
    queryClient.setQueryData(["post", post.publicId], post);
    const like = renderHook(() => useLikePost(), { wrapper });

    await act(async () =>
      like.result.current.mutateAsync({ publicId: post.publicId, source: "FEED" }),
    );
    const likedPost = queryClient.getQueryData<PostFeedDto>(["post", post.publicId]);
    expect(likedPost?.likedByCurrentUser).toBe(true);
    expect(likedPost?.likeCount).toBe(11);
    const likedFeed = queryClient.getQueryData<{
      pages: Array<{ content: PostFeedDto[] }>;
    }>(["postFeed"]);
    expect(likedFeed?.pages.flatMap((page) => page.content)).toEqual([
      expect.objectContaining({ likedByCurrentUser: true, likeCount: 11 }),
      expect.objectContaining({ likedByCurrentUser: true, likeCount: 11 }),
    ]);

    mockToggleLike.mockRejectedValue(new Error("Like failed"));
    await act(async () => {
      await expect(
        like.result.current.mutateAsync({ publicId: post.publicId, source: "FEED" }),
      ).rejects.toThrow("Like failed");
    });
    expect(queryClient.getQueryData(["post", post.publicId])).toEqual(likedPost);
    expect(queryClient.getQueryData(["postFeed"])).toEqual(likedFeed);
    expect(toast.error).toHaveBeenCalledWith("Like failed");
  });

  it("adds a temporary comment, replaces it on success, and rolls back on failure", async () => {
    const realComment: CommentDto = {
      id: 10,
      content: "Hello",
      createdAt: "2025-01-02T00:00:00Z",
      userPublicId: "user-1",
      username: "ava",
      userAvatarUrl: null,
      postPublicId: post.publicId,
    };
    mockAddComment.mockResolvedValue(realComment);
    const { queryClient, wrapper } = createQueryProvider();
    queryClient.setQueryData(["postFeed"], {
      pages: [{ content: [post] }],
      pageParams: [0],
    });
    queryClient.setQueryData(["post", post.publicId], post);
    queryClient.setQueryData(["comments", post.publicId], []);
    const comments = renderHook(() => useAddComment(), { wrapper });

    await act(async () =>
      comments.result.current.mutateAsync({
        publicId: post.publicId,
        content: "Hello",
        source: "FEED",
      }),
    );
    expect(queryClient.getQueryData(["comments", post.publicId])).toEqual([
      realComment,
    ]);
    expect(
      queryClient.getQueryData<PostFeedDto>(["post", post.publicId])?.commentCount,
    ).toBe(3);

    mockAddComment.mockRejectedValue(new Error("Comment failed"));
    await act(async () => {
      await expect(
        comments.result.current.mutateAsync({
          publicId: post.publicId,
          content: "Nope",
          source: "FEED",
        }),
      ).rejects.toThrow("Comment failed");
    });
    expect(queryClient.getQueryData(["comments", post.publicId])).toEqual([
      realComment,
    ]);
    expect(toast.error).toHaveBeenCalledWith("Comment failed");
  });
});

describe("save and follow mutations", () => {
  beforeEach(() => vi.clearAllMocks());

  it("optimistically saves a post and restores cached state if saving fails", async () => {
    mockSavePost.mockResolvedValue(undefined);
    const { queryClient, wrapper } = createQueryProvider();
    queryClient.setQueryData(["postFeed"], {
      pages: [{ content: [post] }],
      pageParams: [0],
    });
    queryClient.setQueryData(["post", post.publicId], post);
    const save = renderHook(() => useSavePost(), { wrapper });

    await act(async () =>
      save.result.current.mutateAsync({
        publicId: post.publicId,
        saved: false,
        source: "FEED",
      }),
    );
    expect(mockSavePost).toHaveBeenCalledWith(post.publicId, "FEED");
    expect(queryClient.getQueryData<PostFeedDto>(["post", post.publicId])?.savedByCurrentUser)
      .toBe(true);

    mockUnsavePost.mockRejectedValue(new Error("Unsave failed"));
    await act(async () => {
      await expect(
        save.result.current.mutateAsync({
          publicId: post.publicId,
          saved: true,
          source: "FEED",
        }),
      ).rejects.toThrow("Unsave failed");
    });
    expect(mockUnsavePost).toHaveBeenCalledWith(post.publicId, "FEED");
    expect(
      queryClient.getQueryData<PostFeedDto>(["post", post.publicId])?.savedByCurrentUser,
    ).toBe(true);
    expect(toast.error).toHaveBeenCalledWith("Unsave failed");
  });

  it("rolls back failed saves and reports post deletion errors", async () => {
    mockSavePost.mockRejectedValue(new Error("Save failed"));
    mockDeletePost.mockRejectedValue(new Error("Delete failed"));
    const { queryClient, wrapper } = createQueryProvider();
    queryClient.setQueryData(["postFeed"], {
      pages: [{ content: [post] }],
      pageParams: [0],
    });
    queryClient.setQueryData(["post", post.publicId], post);
    const save = renderHook(() => useSavePost(), { wrapper });

    await act(async () => {
      await expect(
        save.result.current.mutateAsync({
          publicId: post.publicId,
          saved: false,
          source: "FEED",
        }),
      ).rejects.toThrow("Save failed");
    });
    expect(queryClient.getQueryData<PostFeedDto>(["post", post.publicId]))
      .toMatchObject({ savedByCurrentUser: false });
    expect(toast.error).toHaveBeenCalledWith("Save failed");

    const deletion = renderHook(() => useDeletePost(), { wrapper });
    await act(async () => {
      await expect(
        deletion.result.current.mutateAsync({
          publicId: post.publicId,
          profileQueryUsername: "ava",
        }),
      ).rejects.toThrow("Delete failed");
    });
    expect(toast.error).toHaveBeenCalledWith("Delete failed");
  });

  it("optimistically follows a user and reconciles with the server response", async () => {
    mockToggleFollow.mockResolvedValue(true as never);
    const { queryClient, wrapper } = createQueryProvider();
    const profile = {
      username: "ava",
      following: false,
      followersCount: 5,
    } as ProfileResponse;
    queryClient.setQueryData(["profile", "ava"], profile);
    const follow = renderHook(() => useToggleFollow(), { wrapper });

    await act(async () =>
      follow.result.current.mutateAsync({
        username: "ava",
        profileQueryUsername: "ava",
      }),
    );

    expect(mockToggleFollow).toHaveBeenCalledWith("ava");
    expect(queryClient.getQueryData<ProfileResponse>(["profile", "ava"])).toMatchObject({
      following: true,
      followersCount: 6,
    });
  });

  it("rolls back failed follows and reports the error", async () => {
    mockToggleFollow.mockRejectedValue(new Error("Follow failed"));
    const { queryClient, wrapper } = createQueryProvider();
    const profile = {
      username: "ava",
      following: false,
      followersCount: 5,
    } as ProfileResponse;
    queryClient.setQueryData(["profile", "ava"], profile);
    const follow = renderHook(() => useToggleFollow(), { wrapper });

    await act(async () => {
      await expect(
        follow.result.current.mutateAsync({
          username: "ava",
          profileQueryUsername: "ava",
        }),
      ).rejects.toThrow("Follow failed");
    });

    expect(queryClient.getQueryData(["profile", "ava"])).toEqual(profile);
    expect(toast.error).toHaveBeenCalledWith("Follow failed");
  });
});
