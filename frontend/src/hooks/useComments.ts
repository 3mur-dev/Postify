import { useQuery } from "@tanstack/react-query";
import { getComments } from "../api/commentApi";

export function useComments(publicId: string) {
  return useQuery({
    queryKey: ["comments", publicId],
    queryFn: () => getComments(publicId),
    enabled: !!publicId,
    staleTime: 1000 * 60,
    retry: 1,
    refetchOnWindowFocus: false,
    throwOnError: false,
  });
}



