import { useInfiniteQuery } from "@tanstack/react-query";
import { getPostFeed } from "../api/feedApi";

export function useInfiniteFeed() {
  return useInfiniteQuery({
    queryKey: ["postFeed"],
    queryFn: ({ pageParam }) =>
      getPostFeed({
        page: pageParam,
        size: 5,
      }),
    initialPageParam: 0,
    getNextPageParam: (lastPage) =>
      lastPage.last ? undefined : lastPage.page + 1,
  });
}