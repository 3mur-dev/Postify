import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toggleFollow } from "../api/followApi";
import type { ProfileResponse } from "../api/profileApi";
import { toast } from "sonner";
import { getApiErrorMessage } from "../utils/apiError";

interface ToggleFollowVariables {
  username: string;
  profileQueryUsername: string;
}

export function useToggleFollow() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ username }: ToggleFollowVariables) => toggleFollow(username),

    onMutate: async ({ profileQueryUsername }) => {
      const queryKey = ["profile", profileQueryUsername] as const;

      await queryClient.cancelQueries({ queryKey });

      const previousProfile =
        queryClient.getQueryData<ProfileResponse>(queryKey);

      queryClient.setQueryData<ProfileResponse>(queryKey, (old) => {
        if (!old) return old;

        const following = !old.following;

        return {
          ...old,
          following,
          followersCount: old.followersCount + (following ? 1 : -1),
        };
      });

      return { previousProfile, queryKey };
    },

    onSuccess: (isFollowing, _username, context) => {
      queryClient.setQueryData<ProfileResponse>(
        context.queryKey,
        (old) => {
          if (!old) return old;

          const previousProfile = context.previousProfile;
          const followersCount = previousProfile
            ? previousProfile.followersCount +
              (isFollowing === previousProfile.following
                ? 0
                : isFollowing
                  ? 1
                  : -1)
            : old.followersCount;

          return {
            ...old,
            following: isFollowing,
            followersCount,
          };
        }
      );
    },

    onError: (error, _username, context) => {
      if (context?.previousProfile) {
        queryClient.setQueryData(context.queryKey, context.previousProfile);
      }

      toast.error(getApiErrorMessage(error));
    },

    onSettled: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["profile"] }),
        queryClient.invalidateQueries({ queryKey: ["myProfile"] }),
      ]);
    },
  });
}