import { X, Send } from "lucide-react";
import { useEffect, useState } from "react";
import { useAddComment } from "../../hooks/useAddComments";
import { useComments } from "../../hooks/useComments";
import { hasAccessToken } from "../../lib/auth";
import { formatRelativeTime } from "../../utils/time";
import type { InteractionSource } from "../../api/types";
import Avatar from "./components/Avatar";

interface Props {
  publicId: string;
  onClose: () => void;
  source: InteractionSource;
}

export default function CommentsModal({
  publicId,
  onClose,
  source,
}: Props) {
  const [content, setContent] = useState("");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const {
    data: comments,
    isPending,
    isSuccess,
    isError,
    error,
  } = useComments(publicId);
  const addCommentMutation = useAddComment();
  const resolvedComments = comments ?? [];

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        onClose();
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [onClose]);

  async function handleSubmit() {
    if (!content.trim() || addCommentMutation.isPending) return;

    if (!hasAccessToken()) {
      setErrorMessage("Please sign in to comment.");
      return;
    }

    setErrorMessage(null);

    try {
      await addCommentMutation.mutateAsync({
        publicId,
        content: content.trim(),
        source: source,
      });

      setContent("");
    } catch {
      setErrorMessage("We couldn't post your comment. Please try again.");
    }
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-end justify-center bg-black/60 backdrop-blur-sm"
      onClick={onClose}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-label="Comments"
        className="flex h-[72vh] w-full max-w-2xl flex-col rounded-t-3xl border border-slate-800 bg-slate-950"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="flex items-center justify-between border-b border-slate-800 p-3">
          <h2 className="text-lg font-semibold text-white">Comments</h2>

          <button
            type="button"
            onClick={onClose}
            aria-label="Close comments"
            className="rounded-lg p-2 transition hover:bg-slate-800"
          >
            <X className="h-5 w-5 text-slate-300" />
          </button>
        </div>

        <div className="flex-1 space-y-3 overflow-y-auto p-3" aria-busy={isPending}>
          {isPending ? (
            <p className="text-center text-slate-400">Loading comments...</p>
          ) : isError ? (
            <div className="mt-16 text-center">
              <p className="text-lg font-medium text-white">Unable to load comments</p>
              <p className="mt-2 text-sm text-slate-400">
                {error instanceof Error ? error.message : "Please try again shortly."}
              </p>
            </div>
          ) : isSuccess && resolvedComments.length === 0 ? (
            <div className="mt-16 text-center">
              <p className="text-lg font-medium text-white">No comments yet</p>
              <p className="mt-2 text-sm text-slate-400">Be the first to comment.</p>
            </div>
          ) : (
            resolvedComments.map((comment) => (
              <div key={comment.id} className="flex gap-3">

                <button
                  type="button"
                  className="flex-shrink-0 rounded-full transition hover:opacity-80 cursor-pointer"
                  onClick={() => {
                    window.location.href = `/profile/${comment.username}`;
                  }}
                >
                  <Avatar
                    imageUrl={comment.userAvatarUrl}
                    name={comment.username}
                    size="md"
                  />
                </button>

                <div className="min-w-0 flex-1">
                  <div className="rounded-2xl bg-slate-900 p-2.5">
                    <p className="font-medium text-white">{comment.username}</p>
                    <p className="mt-1 break-words text-sm text-slate-300">
                      {comment.content}
                    </p>
                  </div>

<p className="mt-1 text-xs text-slate-400">
  <time dateTime={comment.createdAt}>
    {formatRelativeTime(comment.createdAt)}
  </time>
</p>
                </div>
              </div>
            ))
          )}
        </div>

        <div className="border-t border-slate-800 p-3">
          {errorMessage ? (
            <p role="alert" className="mb-3 text-sm text-rose-400">
              {errorMessage}
            </p>
          ) : null}

          <div className="flex items-center gap-3">
            <input
              value={content}
              onChange={(event) => setContent(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === "Enter") {
                  event.preventDefault();
                  void handleSubmit();
                }
              }}
              placeholder={hasAccessToken() ? "Write a comment..." : "Sign in to comment"}
              disabled={!hasAccessToken()}
              aria-label="Write a comment"
              className="flex-1 rounded-full border border-slate-700 bg-slate-900 px-4 py-2 text-white outline-none transition focus:border-blue-500 disabled:cursor-not-allowed disabled:opacity-50"
            />

            <button
              type="button"
              disabled={!content.trim() || addCommentMutation.isPending || !hasAccessToken()}
              onClick={() => void handleSubmit()}
              className="rounded-full bg-blue-600 p-3 transition hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-50"
              aria-label="Send comment"
            >
              <Send className="h-4 w-4 text-white" />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
