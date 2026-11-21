import { useQuery } from "@tanstack/react-query";
import { getProfile, type ProfileResponse } from "../api/profileApi";

export function useGetProfile(username: string | null) {
  return useQuery<ProfileResponse>({
    queryKey: ["profile", username],
    queryFn: () => {
      if (!username) {
        throw new Error("A username is required to load a profile.");
      }

      return getProfile(username);
    },
    enabled: Boolean(username),
    staleTime: 1000 * 60 * 5,
  });
}
