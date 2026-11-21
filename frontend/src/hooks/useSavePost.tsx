import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { savePost, unsavePost } from "../api/saveApi";
import { getApiErrorMessage } from "../utils/apiError";
import type { PagedResponse, PostFeedDto } from "../api/feedApi";
import type { InteractionSource } from "../api/types";
import type { InfiniteData } from "@tanstack/react-query";

interface SaveInput {
  publicId: string;
  saved: boolean;
  source: InteractionSource;
}

export function useSavePost() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ publicId, saved, source }: SaveInput) =>
      saved ? unsavePost(publicId, source) : savePost(publicId, source),

    onMutate: async ({ publicId }) => {
      await queryClient.cancelQueries({ queryKey: ["postFeed"] });
      await queryClient.cancelQueries({ queryKey: ["post", publicId] });

      const previousFeed = queryClient.getQueryData(["postFeed"]);
      const previousPost = queryClient.getQueryData<PostFeedDto>(["post", publicId]);

      queryClient.setQueryData<
      InfiniteData<PagedResponse<PostFeedDto>> | undefined
      >(["postFeed"], (old) => {
        
        if (!old) return old;

        return {
          ...old,
          pages: old.pages.map((page) => ({
            ...page,
            content: page.content.map((post) =>
              post.publicId === publicId
                ? {
                    ...post,
                    savedByCurrentUser: !post.savedByCurrentUser,
                  }
                : post
            ),
          })),
        };
      });

      queryClient.setQueryData(["post", publicId], (old: PostFeedDto | undefined) => {
        if (!old) return old;

        return {
          ...old,
          savedByCurrentUser: !old.savedByCurrentUser,
        };
      });

      return { previousFeed, previousPost };
    },

    onError: (error, variables, context) => {
      queryClient.setQueryData(["postFeed"], context?.previousFeed);
      queryClient.setQueryData(["post", variables.publicId], context?.previousPost);
      toast.error(getApiErrorMessage(error));
    },
  });
}