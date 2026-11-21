import { useInfiniteQuery, useQuery } from "@tanstack/react-query";

import {
  getConversationMessages,
  getConversations,
} from "../api/conversationApi";

export const conversationsQueryKey = ["conversations"] as const;

export const conversationMessagesQueryKey = (conversationPublicId: string) =>
  ["conversationMessages", conversationPublicId] as const;

export function useConversations(enabled: boolean) {
  return useQuery({
    queryKey: conversationsQueryKey,
    queryFn: getConversations,
    enabled,
    staleTime: 15_000,
    refetchOnWindowFocus: true,
  });
}

export function useConversationMessages(
  conversationPublicId: string | undefined,
) {
  return useInfiniteQuery({
    queryKey: conversationMessagesQueryKey(conversationPublicId ?? ""),
    queryFn: ({ pageParam }) =>
      getConversationMessages(conversationPublicId ?? "", pageParam),
    initialPageParam: 0,
    getNextPageParam: (lastPage) =>
      lastPage.last ? undefined : lastPage.number + 1,
    enabled: !!conversationPublicId,
    staleTime: 0,
    refetchInterval: 2_000,
    refetchIntervalInBackground: false,
  });
}
