import { publicApi, privateApi } from "./axios";
import { hasAccessToken } from "../lib/auth";
import type { PagedResponse, SavedPostResponse, InteractionSource } from "./types";

export async function getSavedPosts(username: string) {
  const isLoggedIn = hasAccessToken();
  const api = isLoggedIn ? privateApi : publicApi;
  
  const response = await api.get<PagedResponse<SavedPostResponse>>(
    `/profile/${username}/saved`
  );

  return response.data;
}

export async function savePost(publicId: string, source: InteractionSource) {
  await privateApi.post(`/posts/${publicId}/save`, null, {
    params: { source },
  });
}

export async function unsavePost(publicId: string, source: InteractionSource) {
  await privateApi.delete(`/posts/${publicId}/unsave`, {
    params: { source },
  });
}



