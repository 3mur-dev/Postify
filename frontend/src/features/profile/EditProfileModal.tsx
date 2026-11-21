
import { useEffect, useMemo, useState } from "react";
import { X, Camera } from "lucide-react";
import type { ProfileResponse } from "../../api/profileApi";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { updateProfile } from "../../api/profileApi";
import Avatar from "../home/components/Avatar";

type EditProfileModalProps = {
  open: boolean;
  onClose: () => void;
  profile: ProfileResponse;
};

export function EditProfileModal(props: EditProfileModalProps) {
  if (!props.open) {
    return null;
  }

  return <EditProfileModalContent {...props} />;
}

function EditProfileModalContent({
  onClose,
  profile,
}: EditProfileModalProps) {
  const [bio, setBio] = useState(profile.bio ?? "");
  const [avatar, setAvatar] = useState<File | null>(null);

  const queryClient = useQueryClient();

  const mutation = useMutation({
    mutationFn: () => updateProfile(bio, avatar),

    onSuccess: (updatedProfile) => {
      queryClient.setQueryData(["myProfile"], updatedProfile);
      queryClient.setQueryData(
        ["profile", profile.username],
        updatedProfile,
      );

      queryClient.invalidateQueries({
        queryKey: ["myProfile"],
      });

      queryClient.invalidateQueries({
        queryKey: ["profile", profile.username],
      });

      onClose();
    },

    onError: (error) => {
      console.error("Failed to update profile:", error);
    },
  });

  const hasChanges =
    bio !== (profile.bio ?? "") || avatar !== null;

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape" && !mutation.isPending) {
        onClose();
      }
    };

    window.addEventListener("keydown", handleKeyDown);

    return () => {
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [onClose, mutation.isPending]);

  const previewUrl = useMemo(() => {
    if (avatar) {
      return URL.createObjectURL(avatar);
    }

    return profile.avatarUrl;
  }, [avatar, profile.avatarUrl]);

  useEffect(() => {
    if (!avatar || !previewUrl) {
      return;
    }

    return () => {
      URL.revokeObjectURL(previewUrl);
    };
  }, [avatar, previewUrl]);

  return (
    <div className="fixed inset-0 z-[100] flex items-end justify-center bg-black/70 p-3 backdrop-blur-sm sm:items-center sm:p-6">
      <div className="flex max-h-[95vh] w-full max-w-lg flex-col overflow-hidden rounded-2xl border border-white/10 bg-slate-950 shadow-2xl sm:rounded-3xl">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-white/10 px-6 py-5">
          <h2 className="text-lg font-semibold text-white">
            Edit Profile
          </h2>

          <button
            type="button"
            onClick={onClose}
            disabled={mutation.isPending}
            aria-label="Close edit profile modal"
            className="rounded-lg p-2 text-slate-400 transition hover:bg-slate-900 hover:text-white disabled:opacity-50"
          >
            <X size={18} />
          </button>
        </div>

        {/* Body */}
        <div className="flex-1 space-y-6 overflow-y-auto p-4 sm:p-6">
          {/* Avatar */}
          <div className="flex flex-col items-center gap-4">
            <Avatar
              imageUrl={previewUrl}
              name={profile.username}
              className="h-24 w-24 border-4 border-slate-800 sm:h-28 sm:w-28"
            />

            <label className="inline-flex cursor-pointer items-center gap-2 rounded-xl bg-fuchsia-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-fuchsia-500">
              <Camera size={16} />
              Change Photo

              <input
                hidden
                type="file"
                accept="image/*"
                disabled={mutation.isPending}
                onChange={(event) => {
                  setAvatar(event.target.files?.[0] ?? null);
                  event.target.value = "";
                }}
              />
            </label>
          </div>

          {/* Bio */}
          <div>
            <label
              htmlFor="edit-profile-bio"
              className="mb-2 block text-sm font-medium text-slate-300"
            >
              Bio
            </label>

            <textarea
              id="edit-profile-bio"
              rows={4}
              maxLength={500}
              value={bio}
              disabled={mutation.isPending}
              onChange={(event) => setBio(event.target.value)}
              className="w-full resize-none rounded-2xl border border-slate-800 bg-slate-900 p-4 text-sm text-white outline-none transition focus:border-fuchsia-500 disabled:opacity-50"
            />

            <div className="mt-2 text-right text-xs text-slate-500">
              {bio.length}/500
            </div>
          </div>

          {mutation.isError && (
            <p role="alert" className="text-sm text-red-400">
              Failed to update your profile. Please try again.
            </p>
          )}
        </div>

        {/* Footer */}
        <div className="flex flex-col-reverse gap-3 border-t border-white/10 p-4 sm:flex-row sm:justify-end sm:px-6 sm:py-5">
          <button
            type="button"
            onClick={onClose}
            disabled={mutation.isPending}
            className="w-full rounded-xl border border-slate-700 px-4 py-2.5 text-sm text-white transition hover:bg-slate-900 disabled:opacity-50 sm:w-auto"
          >
            Cancel
          </button>

          <button
            type="button"
            disabled={!hasChanges || mutation.isPending}
            onClick={() => mutation.mutate()}
            className="w-full rounded-xl bg-fuchsia-600 px-5 py-2.5 text-sm font-medium text-white transition hover:bg-fuchsia-500 disabled:cursor-not-allowed disabled:opacity-50 sm:w-auto"
          >
            {mutation.isPending ? "Saving..." : "Save Changes"}
          </button>
        </div>
      </div>
    </div>
  );
}
