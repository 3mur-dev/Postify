import { privateApi, publicApi } from "./axios";
import { hasAccessToken } from "../lib/auth";
import type { CommentDto , ApiResponse, InteractionSource } from "./types";

export async function getComments(publicId: string): Promise<CommentDto[]> {
  const isLoggedIn = hasAccessToken();
  const api = isLoggedIn ? privateApi : publicApi;

  const response = await api.get<ApiResponse<CommentDto[]>>(
    `/posts/${publicId}/comments`
  );
  return response.data.data;
}

export async function addComment(
  publicId: string,
  content: string,
  source: InteractionSource
): Promise<CommentDto> {
  if (!hasAccessToken()) {
    throw new Error("Authentication required to comment.");
  }

  const response = await privateApi.post<ApiResponse<CommentDto>>(
    `/posts/${publicId}/comments`,
    { content },
    {
      params:{
        source
      }
      
    }
  );

  return response.data.data;
}