import { privateApi, publicApi } from './axios';
import { hasAccessToken } from '../lib/auth';
import type { ApiResponse, PostFeedDto } from './types';

export async function getPostById(
  publicId: string
): Promise<PostFeedDto> {
  const api = hasAccessToken() ? privateApi : publicApi;

  const response = await api.get<ApiResponse<PostFeedDto>>(
    `/posts/${publicId}`
  );

  return response.data.data;
}

export async function deletePostApi(publicId: string): Promise<void> {
  await privateApi.delete(`/posts/${publicId}`);
}

export async function editPostApi(publicId: string, content: string): Promise<void> {
 const response = await privateApi.put(
    `/posts/${publicId}`,
    null,
    {
      params: {
        content,
      },
    }
  );

  return response.data.data;
}

export async function createPostApi(
  content: string,
  media: File[]
): Promise<PostFeedDto> {
  const formData = new FormData();

  if (content.trim()) {
    formData.append("content", content.trim());
  }

for (const file of media) {
    formData.append("media", file);
  }

  const response = await privateApi.post<ApiResponse<PostFeedDto>>(
    "/posts",
    formData
  );

  return response.data.data;
}