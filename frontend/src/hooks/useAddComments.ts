import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";

import { addComment } from "../api/commentApi";
import { getApiErrorMessage } from "../utils/apiError";

import type {
  InteractionSource,
  PagedResponse,
  PostFeedDto,
  CommentDto,
} from "../api/types";

interface AddCommentInput {
  publicId: string;
  content: string;
  source: InteractionSource;
}

export function useAddComment() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ publicId, content, source }: AddCommentInput) =>
      addComment(publicId, content, source),

    onMutate: async ({ publicId, content }) => {
      // Cancel active requests so they don't overwrite
      // the optimistic state.
      await Promise.all([
        queryClient.cancelQueries({
          queryKey: ["postFeed"],
        }),

        queryClient.cancelQueries({
          queryKey: ["post", publicId],
        }),

        queryClient.cancelQueries({
          queryKey: ["comments", publicId],
        }),
      ]);

      // Save previous cache state for rollback.
      const previousFeed =
        queryClient.getQueryData(["postFeed"]);

      const previousPost =
        queryClient.getQueryData<PostFeedDto>([
          "post",
          publicId,
        ]);

      const previousComments =
        queryClient.getQueryData<CommentDto[]>([
          "comments",
          publicId,
        ]);

      // Temporary optimistic comment.
      const tempComment: CommentDto = {
        id: Date.now(),
        content,
        createdAt: new Date().toISOString(),
        userPublicId: "",
        username: "You",
        userAvatarUrl: null,
        postPublicId: publicId,
      };

      // Add optimistic comment immediately.
      queryClient.setQueryData<CommentDto[]>(
        ["comments", publicId],
        (old = []) => [tempComment, ...old]
      );

      // Optimistically increment comment count in the feed.
      queryClient.setQueryData(
        ["postFeed"],
        (
          old:
            | {
                pages: PagedResponse<PostFeedDto>[];
                pageParams: unknown[];
              }
            | undefined
        ) => {
          if (!old) return old;

          return {
            ...old,

            pages: old.pages.map((page) => ({
              ...page,

              content: page.content.map((post) =>
                post.publicId === publicId
                  ? {
                      ...post,
                      commentCount:
                        post.commentCount + 1,
                    }
                  : post
              ),
            })),
          };
        }
      );

      // Optimistically increment the individual post count.
      queryClient.setQueryData(
        ["post", publicId],
        (old: PostFeedDto | undefined) => {
          if (!old) return old;

          return {
            ...old,
            commentCount: old.commentCount + 1,
          };
        }
      );

      return {
        previousFeed,
        previousPost,
        previousComments,
        tempCommentId: tempComment.id,
      };
    },

    onSuccess: (newComment, { publicId }, context) => {
      // Replace the temporary optimistic comment
      // with the real server-generated comment.
      queryClient.setQueryData<CommentDto[]>(
        ["comments", publicId],
        (old = []) => [
          newComment,
          ...old.filter(
            (comment) =>
              comment.id !== context?.tempCommentId
          ),
        ]
      );
    },

    onError: (error, { publicId }, context) => {
      // Roll back the feed.
      queryClient.setQueryData(
        ["postFeed"],
        context?.previousFeed
      );

      // Roll back the individual post.
      queryClient.setQueryData(
        ["post", publicId],
        context?.previousPost
      );

      // Roll back comments.
      queryClient.setQueryData(
        ["comments", publicId],
        context?.previousComments
      );

      toast.error(getApiErrorMessage(error));
    },

    onSettled: (_, __, { publicId }) => {
      // Server is the final source of truth.
      queryClient.invalidateQueries({
        queryKey: ["comments", publicId],
      });

      queryClient.invalidateQueries({
        queryKey: ["post", publicId],
      });

      queryClient.invalidateQueries({
        queryKey: ["postFeed"],
      });
    },
  });
}