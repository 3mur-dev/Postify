import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { updateProfile } from "../../api/profileApi";
import type { ProfileResponse } from "../../api/types";
import { EditProfileModal } from "./EditProfileModal";

vi.mock("../../api/profileApi", () => ({ updateProfile: vi.fn() }));
vi.mock("../home/components/Avatar", () => ({
  default: () => <div>Profile avatar</div>,
}));

const mockUpdateProfile = vi.mocked(updateProfile);
const profile: ProfileResponse = {
  id: 1,
  publicId: "user-1",
  username: "ava",
  bio: "Existing bio",
  avatarUrl: "https://example.test/avatar.png",
  ownProfile: true,
  following: false,
  followersCount: 0,
  followingCount: 0,
  posts: [],
  role: "USER",
};

Object.defineProperty(URL, "createObjectURL", {
  configurable: true,
  value: vi.fn(() => "blob:avatar-preview"),
});
Object.defineProperty(URL, "revokeObjectURL", {
  configurable: true,
  value: vi.fn(),
});

function createQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: Infinity },
      mutations: { retry: false },
    },
  });
}

function renderModal(queryClient: QueryClient, onClose = vi.fn()) {
  function Wrapper({ children }: { children: ReactNode }) {
    return (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );
  }
  return {
    onClose,
    ...render(
      <EditProfileModal open onClose={onClose} profile={profile} />,
      { wrapper: Wrapper },
    ),
  };
}

describe("EditProfileModal", () => {
  beforeEach(() => vi.clearAllMocks());

  it("requires a change, uploads an avatar, and refreshes profile caches on success", async () => {
    const queryClient = createQueryClient();
    const updatedProfile = { ...profile, bio: "Updated bio" };
    mockUpdateProfile.mockResolvedValue(updatedProfile);
    queryClient.setQueryData(["myProfile"], profile);
    queryClient.setQueryData(["profile", profile.username], profile);
    const invalidateQueries = vi.spyOn(queryClient, "invalidateQueries");
    const onClose = vi.fn();
    const view = renderModal(queryClient, onClose);

    expect(screen.getByRole("button", { name: "Save Changes" })).toBeDisabled();
    fireEvent.change(screen.getByLabelText("Bio"), {
      target: { value: "Updated bio" },
    });
    const avatar = new File(["image-bytes"], "avatar.png", {
      type: "image/png",
    });
    fireEvent.change(screen.getByLabelText("Change Photo"), {
      target: { files: [avatar] },
    });
    fireEvent.click(screen.getByRole("button", { name: "Save Changes" }));

    await waitFor(() =>
      expect(mockUpdateProfile).toHaveBeenCalledWith("Updated bio", avatar),
    );
    await waitFor(() => expect(onClose).toHaveBeenCalledOnce());
    expect(queryClient.getQueryData(["myProfile"])).toEqual(updatedProfile);
    expect(
      queryClient.getQueryData(["profile", profile.username]),
    ).toEqual(updatedProfile);
    expect(invalidateQueries).toHaveBeenCalledWith({
      queryKey: ["myProfile"],
    });
    expect(invalidateQueries).toHaveBeenCalledWith({
      queryKey: ["profile", profile.username],
    });

    view.unmount();
    expect(URL.revokeObjectURL).toHaveBeenCalledWith("blob:avatar-preview");
  });

  it("shows a recoverable error and closes on Escape", async () => {
    mockUpdateProfile.mockRejectedValue(new Error("Upload failed"));
    const onClose = vi.fn();
    renderModal(createQueryClient(), onClose);
    fireEvent.change(screen.getByLabelText("Bio"), {
      target: { value: "Changed bio" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Save Changes" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Failed to update your profile. Please try again.",
    );
    fireEvent.keyDown(window, { key: "Escape" });
    expect(onClose).toHaveBeenCalledOnce();
  });
});
