import { useQuery, useQueryClient } from "@tanstack/react-query";
import { getPostById, deletePostApi, editPostApi, createPostApi } from "../api/postApi";
import { useMutation } from "@tanstack/react-query";
import { toast } from "sonner";
import { getApiErrorMessage } from "../utils/apiError";
import type { PagedResponse, PostFeedDto, DeletePostVariables, EditPostVariables } from "../api/types";
import { togglePostLike } from "../api/likeApi";
import type { InfiniteData } from "@tanstack/react-query";

export function usePost(publicId: string) {
  return useQuery<PostFeedDto>({
    queryKey: ["post", publicId],
    queryFn: () => getPostById(publicId),
    enabled: !!publicId,
    staleTime: 1000 * 60 * 5,
  });
}

export function useCreatePost() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      content,
      media,
    }: {
      content: string;
      media: File[]
    }) => createPostApi(content, media),

    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["postFeed"],
      });

      queryClient.invalidateQueries({
        queryKey: ["profile"],
      });
    },

    onError: (error) => {
      toast.error(getApiErrorMessage(error));
    },
  });
}

export function useLikePost() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: togglePostLike,

    onMutate: async ({ publicId }) => {
      await queryClient.cancelQueries({
        queryKey: ["postFeed"],
      });

      await queryClient.cancelQueries({
        queryKey: ["post", publicId],
      });

      const previousFeed = queryClient.getQueryData(["postFeed"]);

      const previousPost =
        queryClient.getQueryData<PostFeedDto>(["post", publicId]);

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
                      likedByCurrentUser: !post.likedByCurrentUser,
                      likeCount:
                        post.likeCount + (post.likedByCurrentUser ? -1 : 1),
                    }
                  : post
              ),
            })),
          };
        });

      queryClient.setQueryData(
        ["post", publicId],
        (old: PostFeedDto | undefined) => {
          if (!old) return old;

          return {
            ...old,
            likedByCurrentUser: !old.likedByCurrentUser,
            likeCount:
              old.likeCount + (old.likedByCurrentUser ? -1 : 1),
          };
        }
      );

      return { previousFeed, previousPost };
    },

    onError: (error, { publicId }, context) => {
      queryClient.setQueryData(
        ["postFeed"],
        context?.previousFeed
      );

      queryClient.setQueryData(
        ["post", publicId],
        context?.previousPost
      );

      toast.error(getApiErrorMessage(error));
    },
  });
}

export function useDeletePost() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ publicId }: DeletePostVariables) =>
      deletePostApi(publicId),

    onSuccess: (_, { profileQueryUsername }) => {
      queryClient.invalidateQueries({
        queryKey: ["postFeed"],
      });

      queryClient.invalidateQueries({
        queryKey: ["profile", profileQueryUsername],
      });
    },

    onError: (error) => {
      toast.error(getApiErrorMessage(error));
    },
  });
}

export function useEditPost() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ publicId, content }: EditPostVariables) => {
      await editPostApi(publicId, content);
    },

    onSuccess: (_, { profileQueryUsername }) => {
      queryClient.invalidateQueries({
        queryKey: ["postFeed"],
      });

      queryClient.invalidateQueries({
        queryKey: ["profile", profileQueryUsername],
      });
    },

    onError: (error) => {
      toast.error(getApiErrorMessage(error));
    },
  });
}