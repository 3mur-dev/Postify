import { privateApi } from './axios';
import type { ApiResponse, PagedResponse } from './types';

export async function toggleFollow(username: string): Promise<boolean> {
  const response = await privateApi.post<ApiResponse<boolean>>(
    `/follow/${username}`
  );

  return response.data.data;
}

export type FollowListType = "followers" | "following";

export interface FollowListUser {
  id: number;
  username: string;
  avatarUrl: string | null;
  followedByCurrentUser: boolean;
}

export async function getProfileFollowList(
  username: string,
  listType: FollowListType,
  page: number
): Promise<PagedResponse<FollowListUser>> {
  const response = await privateApi.get<PagedResponse<FollowListUser>>(
    `/profile/${encodeURIComponent(username)}/${listType}`,
    { params: { page, size: 20 } }
  );

  return response.data;
}