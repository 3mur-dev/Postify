import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useDeletePost, useEditPost, useLikePost } from "../hooks/usePost";
import { useGetProfile } from "../hooks/useGetProfile";
import ProfilePage from "./ProfilePage";

vi.mock("../hooks/usePost", () => ({
  useDeletePost: vi.fn(),
  useEditPost: vi.fn(),
  useLikePost: vi.fn(),
}));
vi.mock("../hooks/useGetProfile", () => ({ useGetProfile: vi.fn() }));
vi.mock("../features/profile/ProfileSkeleton", () => ({
  ProfileSkeleton: () => <div>Profile loading</div>,
}));
vi.mock("../features/profile/ProfileCard", () => ({
  ProfileCard: (props: {
    isOwner: boolean;
    onTabChange: (tab: "posts" | "saved") => void;
    onEditProfile: () => void;
  }) => (
    <div>
      <span>Owner: {String(props.isOwner)}</span>
      <button type="button" onClick={() => props.onTabChange("posts")}>Posts tab</button>
      <button type="button" onClick={() => props.onTabChange("saved")}>Saved tab</button>
      <button type="button" onClick={props.onEditProfile}>Edit profile</button>
    </div>
  ),
}));
vi.mock("../features/profile/ProfilePosts", () => ({
  ProfilePosts: (props: {
    onDeletePost: (publicId: string) => void;
    onEditPost: (publicId: string, content: string) => void;
    onReportPost: (publicId: string) => void;
  }) => (
    <div>
      <button type="button" onClick={() => props.onDeletePost("post-1")}>Delete post</button>
      <button type="button" onClick={() => props.onEditPost("post-1", "Current")}>Edit post</button>
      <button type="button" onClick={() => props.onReportPost("post-1")}>Report post</button>
    </div>
  ),
}));
vi.mock("../features/profile/SavedPosts", () => ({
  SavedPosts: (props: { onLike: (publicId: string) => void }) => (
    <button type="button" onClick={() => props.onLike("saved-1")}>Like saved post</button>
  ),
}));
vi.mock("../features/profile/EditProfileModal", () => ({
  EditProfileModal: (props: { open: boolean; onClose: () => void }) =>
    props.open ? <button type="button" onClick={props.onClose}>Close profile editor</button> : null,
}));
vi.mock("../features/profile/EditPostModal", () => ({
  EditPostModal: (props: {
    open: boolean;
    isPending: boolean;
    onClose: () => void;
    onSave: (content: string) => void;
  }) =>
    props.open ? (
      <div>
        <span>Editing post pending={String(props.isPending)}</span>
        <button type="button" onClick={() => props.onSave("Updated content")}>Save edit</button>
        <button type="button" onClick={props.onClose}>Close post editor</button>
      </div>
    ) : null,
}));

const mockUseGetProfile = vi.mocked(useGetProfile);
const mockUseLikePost = vi.mocked(useLikePost);
const mockUseDeletePost = vi.mocked(useDeletePost);
const mockUseEditPost = vi.mocked(useEditPost);
const mutateDelete = vi.fn();
const mutateEdit = vi.fn();
const mutateLike = vi.fn();
const profile = {
  publicId: "user-1",
  username: "ava",
  avatarUrl: null,
  posts: [],
} as never;

function renderProfile(path = "/profile/ava") {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/profile" element={<ProfilePage />} />
        <Route path="/profile/:username" element={<ProfilePage />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe("ProfilePage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    mockUseGetProfile.mockReturnValue({
      data: profile,
      isLoading: false,
      error: null,
    } as never);
    mockUseLikePost.mockReturnValue({
      mutateAsync: mutateLike,
    } as never);
    mockUseDeletePost.mockReturnValue({
      mutate: mutateDelete,
    } as never);
    mockUseEditPost.mockReturnValue({
      mutate: mutateEdit,
      isPending: false,
    } as never);
  });

  it("renders nothing when a guest opens their own profile route", () => {
    const { container } = renderProfile("/profile");
    expect(container).toBeEmptyDOMElement();
    expect(mockUseGetProfile).toHaveBeenCalledWith(null);
  });

  it("shows profile loading, error, and missing-profile states", () => {
    mockUseGetProfile.mockReturnValue({
      data: undefined,
      isLoading: true,
      error: null,
    } as never);
    const { rerender, container } = renderProfile();
    expect(screen.getByText("Profile loading")).toBeInTheDocument();

    mockUseGetProfile.mockReturnValue({
      data: undefined,
      isLoading: false,
      error: new Error("Profile unavailable"),
    } as never);
    rerender(
      <MemoryRouter initialEntries={["/profile/ava"]}>
        <Routes>
          <Route path="/profile/:username" element={<ProfilePage />} />
        </Routes>
      </MemoryRouter>,
    );
    expect(screen.getByText("Profile unavailable")).toBeInTheDocument();

    mockUseGetProfile.mockReturnValue({
      data: undefined,
      isLoading: false,
      error: null,
    } as never);
    rerender(
      <MemoryRouter initialEntries={["/profile/ava"]}>
        <Routes>
          <Route path="/profile/:username" element={<ProfilePage />} />
        </Routes>
      </MemoryRouter>,
    );
    expect(container).toBeEmptyDOMElement();
  });

  it("decodes the current user from a token and redirects the profile shortcut", async () => {
    localStorage.setItem("token", `header.${btoa(JSON.stringify({ username: "ava" }))}.signature`);
    renderProfile("/profile");

    await waitFor(() => expect(mockUseGetProfile).toHaveBeenCalledWith("ava"));
    expect(screen.getByText("Owner: true")).toBeInTheDocument();
  });

  it("handles invalid tokens and supports profile tabs and editing actions", () => {
    localStorage.setItem("token", "not-a-jwt");
    renderProfile();

    expect(screen.getByText("Owner: false")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Edit profile" }));
    expect(screen.getByRole("button", { name: "Close profile editor" })).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Close profile editor" }));

    fireEvent.click(screen.getByRole("button", { name: "Delete post" }));
    expect(mutateDelete).toHaveBeenCalledWith({
      publicId: "post-1",
      profileQueryUsername: "ava",
    });
    fireEvent.click(screen.getByRole("button", { name: "Edit post" }));
    expect(screen.getByText("Editing post pending=false")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Save edit" }));
    expect(mutateEdit).toHaveBeenCalledWith(
      {
        publicId: "post-1",
        content: "Updated content",
        profileQueryUsername: "ava",
      },
      expect.objectContaining({ onSuccess: expect.any(Function) }),
    );
    const onSuccess = mutateEdit.mock.calls[0][1]?.onSuccess;
    act(() => onSuccess?.());
    expect(screen.queryByText("Editing post pending=false")).not.toBeInTheDocument();
  });

  it("renders saved posts and routes likes through the saved-post interaction source", () => {
    renderProfile();
    fireEvent.click(screen.getByRole("button", { name: "Saved tab" }));
    fireEvent.click(screen.getByRole("button", { name: "Like saved post" }));

    expect(mutateLike).toHaveBeenCalledWith({
      publicId: "saved-1",
      source: "SAVED_POSTS",
    });
  });
});
