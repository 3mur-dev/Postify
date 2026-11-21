import { hasAccessToken } from "../lib/auth";
import { privateApi, publicApi } from "./axios";
import type { PostFeedDto, PagedResponse } from "./types";

export type { PagedResponse, PostFeedDto } from "./types";

interface FeedParams {
  keyword?: string;
  page?: number;
  size?: number;
}

export async function getPostFeed({
  keyword,
  page = 0,
  size = 10,
}: FeedParams = {}): Promise<PagedResponse<PostFeedDto>> {
  const isLoggedIn = hasAccessToken();
  const api = isLoggedIn ? privateApi : publicApi;

  const { data } = await api.get<PagedResponse<PostFeedDto>>("/feed", {
    withCredentials: true,
    params: {
      q: keyword || undefined,
      page,
      size,
    },
  });

  return data;
}