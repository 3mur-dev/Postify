import { useQuery } from "@tanstack/react-query";

import { getSearchResults } from "../api/searchApi";

export function useGetSearchResults(query: string) {
  const normalizedQuery = query.trim();

  return useQuery({
    queryKey: ["searchResults", normalizedQuery],
    queryFn: () => getSearchResults(normalizedQuery),
    enabled: normalizedQuery.length > 0,
    staleTime: 1000 * 60,
    retry: 1,
    refetchOnWindowFocus: false,
    throwOnError: false,
  });
}