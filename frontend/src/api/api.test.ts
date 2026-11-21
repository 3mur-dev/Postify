import { beforeEach, describe, expect, it, vi } from "vitest";
import { hasAccessToken } from "../lib/auth";
import { privateApi, publicApi } from "./axios";
import {
  getAdminDashboard,
  getUserDetails,
  getUsers,
} from "./adminApi";
import { login, register } from "./authApi";
import { addComment, getComments } from "./commentApi";
import {
  createConversation,
  getConversationMessages,
  getConversations,
  markMessagesAsRead,
} from "./conversationApi";
import { getPostFeed } from "./feedApi";
import { getProfileFollowList, toggleFollow } from "./followApi";
import { trackInteraction } from "./interactionApi";
import { togglePostLike } from "./likeApi";
import { getNotifications, markAsRead } from "./notification";
import {
  createPostApi,
  deletePostApi,
  editPostApi,
  getPostById,
} from "./postApi";
import { getMyProfile, getProfile, updateProfile } from "./profileApi";
import { getSavedPosts, savePost, unsavePost } from "./saveApi";
import { getSearchResults } from "./searchApi";

vi.mock("./axios", () => ({
  privateApi: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
  publicApi: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

vi.mock("./axios/publicApi", () => ({
  default: { post: vi.fn() },
}));

vi.mock("../lib/auth", () => ({ hasAccessToken: vi.fn() }));

const mockPrivate = vi.mocked(privateApi);
const mockPublic = vi.mocked(publicApi);
const mockHasAccessToken = vi.mocked(hasAccessToken);
const response = <T,>(data: T) => ({ data: { data } });

describe("API clients", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockHasAccessToken.mockReturnValue(true);
  });

  it("logs in and registers through the public authentication client", async () => {
    const authClient = (await import("./axios/publicApi")).default;
    vi.mocked(authClient.post).mockResolvedValueOnce(
      response({ token: "access-token" }) as never,
    );

    await expect(login({ username: "ava", password: "secret" })).resolves.toEqual({
      token: "access-token",
    });
    expect(authClient.post).toHaveBeenCalledWith("/auth/login", {
      username: "ava",
      password: "secret",
    });

    vi.mocked(authClient.post).mockResolvedValueOnce({ data: { data: null } } as never);
    await expect(
      register({
        username: "ava",
        email: "ava@example.com",
        password: "secret",
        confirmPassword: "secret",
      }),
    ).resolves.toBeUndefined();
  });

  it("fetches users with defaults and explicit filters and retrieves user details", async () => {
    mockPrivate.get
      .mockResolvedValueOnce({ data: { content: [] } } as never)
      .mockResolvedValueOnce({ data: { content: [] } } as never)
      .mockResolvedValueOnce(response({ publicId: "user-1" }) as never);

    await getUsers();
    expect(mockPrivate.get).toHaveBeenNthCalledWith(1, "/admin/users", {
      params: {
        page: 0,
        size: 10,
        sortField: "CREATED_AT",
        direction: "DESC",
        search: undefined,
      },
    });

    await getUsers({
      page: 2,
      size: 25,
      sortField: "USERNAME",
      direction: "ASC",
      search: "ava",
    });
    expect(mockPrivate.get).toHaveBeenNthCalledWith(2, "/admin/users", {
      params: {
        page: 2,
        size: 25,
        sortField: "USERNAME",
        direction: "ASC",
        search: "ava",
      },
    });

    await expect(getUserDetails("user-1")).resolves.toEqual({ publicId: "user-1" });
    expect(mockPrivate.get).toHaveBeenLastCalledWith("/admin/users/user-1");
  });

  it("serializes dashboard range, timezone, optional dates, and repeated sections", async () => {
    const dashboard = { overview: { totalUsers: 1 } };
    mockPrivate.get.mockResolvedValueOnce(response(dashboard) as never);

    await expect(
      getAdminDashboard({
        rangePreset: "CUSTOM",
        timezone: "UTC",
        fromDate: "2025-01-01",
        toDate: "2025-01-31",
        sections: ["OVERVIEW", "CHARTS"],
      }),
    ).resolves.toEqual(dashboard);

    const [, config] = mockPrivate.get.mock.calls[0];
    expect(config?.params).toBeInstanceOf(URLSearchParams);
    expect((config?.params as URLSearchParams).getAll("sections")).toEqual([
      "OVERVIEW",
      "CHARTS",
    ]);
    expect((config?.params as URLSearchParams).get("fromDate")).toBe("2025-01-01");
    expect((config?.params as URLSearchParams).get("toDate")).toBe("2025-01-31");

    mockPrivate.get.mockResolvedValueOnce(response(dashboard) as never);
    await getAdminDashboard({
      rangePreset: "TODAY",
      timezone: "UTC",
      sections: [],
    });
    const [, defaultConfig] = mockPrivate.get.mock.calls[1];
    expect((defaultConfig?.params as URLSearchParams).has("fromDate")).toBe(false);
    expect((defaultConfig?.params as URLSearchParams).has("toDate")).toBe(false);
  });

  it("routes comment requests according to authentication and requires auth to add comments", async () => {
    mockPrivate.get.mockResolvedValueOnce(response([]) as never);
    await getComments("post-1");
    expect(mockPrivate.get).toHaveBeenCalledWith("/posts/post-1/comments");

    mockHasAccessToken.mockReturnValueOnce(false);
    mockPublic.get.mockResolvedValueOnce(response([]) as never);
    await getComments("post-2");
    expect(mockPublic.get).toHaveBeenCalledWith("/posts/post-2/comments");

    mockHasAccessToken.mockReturnValueOnce(false);
    await expect(addComment("post-2", "Hi", "FEED")).rejects.toThrow(
      "Authentication required to comment.",
    );

    mockPrivate.post.mockResolvedValueOnce(response({ id: 1 }) as never);
    await expect(addComment("post-1", "Hi", "FEED")).resolves.toEqual({ id: 1 });
    expect(mockPrivate.post).toHaveBeenCalledWith(
      "/posts/post-1/comments",
      { content: "Hi" },
      { params: { source: "FEED" } },
    );
  });

  it("routes feed, post, and saved-post reads for authenticated and anonymous users", async () => {
    mockPrivate.get.mockResolvedValueOnce({ data: { content: [] } } as never);
    await getPostFeed({ keyword: " cat ", page: 1, size: 5 });
    expect(mockPrivate.get).toHaveBeenCalledWith("/feed", {
      withCredentials: true,
      params: { q: " cat ", page: 1, size: 5 },
    });

    mockHasAccessToken.mockReturnValueOnce(false);
    mockPublic.get.mockResolvedValueOnce({ data: { content: [] } } as never);
    await getPostFeed();
    expect(mockPublic.get).toHaveBeenCalledWith("/feed", {
      withCredentials: true,
      params: { q: undefined, page: 0, size: 10 },
    });

    mockPrivate.get.mockResolvedValueOnce(response({ publicId: "post-1" }) as never);
    await getPostById("post-1");
    expect(mockPrivate.get).toHaveBeenCalledWith("/posts/post-1");

    mockHasAccessToken.mockReturnValueOnce(false);
    mockPublic.get.mockResolvedValueOnce(response({ publicId: "post-2" }) as never);
    await getPostById("post-2");
    expect(mockPublic.get).toHaveBeenCalledWith("/posts/post-2");

    mockPrivate.get.mockResolvedValueOnce({ data: [] } as never);
    await getSavedPosts("ava");
    mockHasAccessToken.mockReturnValueOnce(false);
    mockPublic.get.mockResolvedValueOnce({ data: [] } as never);
    await getSavedPosts("sam");
  });

  it("creates, edits, and deletes posts while preserving trimmed text and media", async () => {
    await deletePostApi("post-1");
    expect(mockPrivate.delete).toHaveBeenCalledWith("/posts/post-1");

    mockPrivate.put.mockResolvedValueOnce(response(null) as never);
    await editPostApi("post-1", "Updated");
    expect(mockPrivate.put).toHaveBeenCalledWith("/posts/post-1", null, {
      params: { content: "Updated" },
    });

    const image = new File(["image"], "photo.png", { type: "image/png" });
    mockPrivate.post.mockResolvedValueOnce(response({ publicId: "new-post" }) as never);
    await expect(createPostApi("  caption  ", [image])).resolves.toEqual({
      publicId: "new-post",
    });
    const formData = mockPrivate.post.mock.calls.at(-1)?.[1] as FormData;
    expect(formData.get("content")).toBe("caption");
    expect(formData.get("media")).toBe(image);

    mockPrivate.post.mockResolvedValueOnce(response({ publicId: "empty-post" }) as never);
    await createPostApi("   ", []);
    const emptyFormData = mockPrivate.post.mock.calls.at(-1)?.[1] as FormData;
    expect(emptyFormData.has("content")).toBe(false);
    expect(emptyFormData.has("media")).toBe(false);
  });

  it("routes profile reads and uploads profile updates", async () => {
    mockPrivate.get.mockResolvedValueOnce(response({ username: "me" }) as never);
    await getMyProfile();
    expect(mockPrivate.get).toHaveBeenCalledWith("/profile/me");

    mockPrivate.get.mockResolvedValueOnce(response({ username: "ava" }) as never);
    await getProfile("ava");
    expect(mockPrivate.get).toHaveBeenCalledWith("/profile/ava");

    mockHasAccessToken.mockReturnValueOnce(false);
    mockPublic.get.mockResolvedValueOnce(response({ username: "public" }) as never);
    await getProfile("public");
    expect(mockPublic.get).toHaveBeenCalledWith("/profile/public");

    const avatar = new File(["avatar"], "avatar.png", { type: "image/png" });
    mockPrivate.put.mockResolvedValueOnce(response({ username: "me" }) as never);
    await updateProfile("Bio", avatar);
    const [url, formData, config] = mockPrivate.put.mock.calls.at(-1)!;
    expect(url).toBe("/profile/me");
    expect((formData as FormData).get("bio")).toBe("Bio");
    expect((formData as FormData).get("avatar")).toBe(avatar);
    expect(config).toEqual({ headers: { "Content-Type": "multipart/form-data" } });

    mockPrivate.put.mockResolvedValueOnce(response({ username: "me" }) as never);
    await updateProfile("Bio");
    expect((mockPrivate.put.mock.calls.at(-1)?.[1] as FormData).has("avatar")).toBe(
      false,
    );
  });

  it("trims search input, skips empty searches, and preserves the compatibility alias", async () => {
    await expect(getSearchResults("   ")).resolves.toEqual({ users: [], posts: [] });
    expect(mockPublic.get).not.toHaveBeenCalled();

    mockPublic.get.mockResolvedValueOnce(response({ users: [], posts: [] }) as never);
    await getSearchResults("  postify  ");
    expect(mockPublic.get).toHaveBeenCalledWith("/search", {
      params: { q: "postify" },
    });
  });

  it("supports follow and paginated encoded follower lists", async () => {
    mockPrivate.post.mockResolvedValueOnce({ data: { success: true } } as never);
    await toggleFollow("ava");
    expect(mockPrivate.post).toHaveBeenCalledWith("/follow/ava");

    mockPrivate.get.mockResolvedValueOnce({ data: { content: [] } } as never);
    await getProfileFollowList("ava smith", "followers", 3);
    expect(mockPrivate.get).toHaveBeenCalledWith(
      "/profile/ava%20smith/followers",
      { params: { page: 3, size: 20 } },
    );
  });

  it("sends interactions and like/save toggles with their source", async () => {
    const interaction = {
      postPublicId: "post-1",
      eventType: "VIEW" as const,
      source: "FEED" as const,
    };
    await trackInteraction(interaction);
    expect(mockPrivate.post).toHaveBeenCalledWith("/interactions", interaction);

    await togglePostLike({ publicId: "post-1", source: "PROFILE" });
    expect(mockPrivate.post).toHaveBeenCalledWith("/posts/post-1/like", null, {
      params: { source: "PROFILE" },
    });

    await savePost("post-1", "FEED");
    expect(mockPrivate.post).toHaveBeenCalledWith("/posts/post-1/save", null, {
      params: { source: "FEED" },
    });
    await unsavePost("post-1", "PROFILE");
    expect(mockPrivate.delete).toHaveBeenCalledWith("/posts/post-1/unsave", {
      params: { source: "PROFILE" },
    });
  });

  it("loads conversations and encodes conversation identifiers for messages and read state", async () => {
    mockPrivate.get.mockResolvedValueOnce(response([]) as never);
    await getConversations();
    expect(mockPrivate.get).toHaveBeenCalledWith("/conversations");

    mockPrivate.get.mockResolvedValueOnce(response({ content: [] }) as never);
    await getConversationMessages("thread/one", 2, 15);
    expect(mockPrivate.get).toHaveBeenCalledWith(
      "/conversations/thread%2Fone/messages",
      { params: { page: 2, size: 15 } },
    );

    mockPrivate.post.mockResolvedValueOnce(response({ publicId: "thread-1" }) as never);
    await createConversation("user/one");
    expect(mockPrivate.post).toHaveBeenCalledWith("/conversations/user%2Fone");

    await markMessagesAsRead("thread/one");
    expect(mockPrivate.post).toHaveBeenCalledWith(
      "/conversations/thread%2Fone/read",
    );
  });

  it("loads notifications and marks individual items as read", async () => {
    mockPrivate.get.mockResolvedValueOnce(response({ content: [] }) as never);
    await getNotifications();
    expect(mockPrivate.get).toHaveBeenCalledWith("/notifications", {
      params: { page: 0, size: 20 },
    });

    await markAsRead("notice-1");
    expect(mockPrivate.patch).toHaveBeenCalledWith("/notifications/notice-1/read");
  });
});
