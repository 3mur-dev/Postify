import { publicApi, privateApi } from "./axios";
import type { ProfileResponse } from "./types";
import { hasAccessToken } from "../lib/auth";

export type { ProfileResponse, SavedPostResponse } from "./types";

export const getMyProfile = async (): Promise<ProfileResponse> => {
  const response = await privateApi.get<{ data: ProfileResponse }>(
    "/profile/me"
  );

  return response.data.data;
};


export const getProfile = async (
  username: string
): Promise<ProfileResponse> => {
  const api = hasAccessToken() ? privateApi : publicApi;
  const response = await api.get<{ data: ProfileResponse }>(
    `/profile/${username}`
  );

  return response.data.data;
};



export async function updateProfile(
  bio: string,
  avatar?: File | null
) {
  const formData = new FormData();

  formData.append("bio", bio);

  if (avatar) {
    formData.append("avatar", avatar);
  }

  const response = await privateApi.put("/profile/me", formData, {
    headers: {
      "Content-Type": "multipart/form-data",
    },
  });

  return response.data.data;
}