import { publicApi } from './axios';
import type { ApiResponse, SearchResponse } from './types';

export async function getSearchResults(query: string): Promise<SearchResponse> {
  const normalizedQuery = query.trim();

  if (!normalizedQuery) {
    return { users: [], posts: [] };
  }

  const response = await publicApi.get<ApiResponse<SearchResponse>>("/search", {
    params: { q: normalizedQuery },
  });

  return response.data.data;
}

export const getsearchResults = getSearchResults;