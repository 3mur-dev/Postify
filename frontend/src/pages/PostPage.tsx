import { ArrowLeft } from "lucide-react";
import { Link, useParams } from "react-router-dom";

import { usePost } from "../hooks/usePost";
import { useLikePost } from "../hooks/usePost";
import { useSavePost } from "../hooks/useSavePost";

import FeedHeader from "../features/home/FeedHeader";
import FeedMedia from "../features/home/FeedMedia";
import FeedActions from "../features/home/FeedActions";
import CommentsModal from "../features/home/CommentsModal";
import PostContent from "../features/home/PostContent";
import { useState } from "react";
import { useNavigate } from "react-router-dom";

export default function PostPage() {
  const { publicId } = useParams();
  const resolvedPublicId = publicId ?? "";
  const navigate = useNavigate();

  const { data: post, isLoading, isError } = usePost(resolvedPublicId);

  const likeMutation = useLikePost();
  const saveMutation = useSavePost();

  const [commentsOpen, setCommentsOpen] = useState(false);

  if (isLoading) {
    return (
      <div className="mx-auto max-w-3xl py-10">
        <div className="h-[650px] animate-pulse rounded-3xl bg-slate-900" />
      </div>
    );
  }

  if (isError || !post) {
    return (
      <div className="flex min-h-[60vh] flex-col items-center justify-center gap-4">
        <h1 className="text-3xl font-bold text-white">
        something went wrong
        </h1>

        <Link
          to="/"
          className="rounded-xl bg-blue-600 px-5 py-2 text-white transition hover:bg-blue-700"
        >
          Go Home
        </Link>
      </div>
    );
  }

  const formattedDate = new Date(post.createdAt).toLocaleString(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  });

  return (
    <>
      <div className="mx-auto max-w-3xl px-4 py-8">

        <button
          type="button"
          onClick={() => navigate(-1)}
          className="mb-6 inline-flex items-center gap-2 text-slate-300 transition hover:text-white"
        >
          <ArrowLeft className="h-5 w-5" />
          Back

        </button>

        <article className="overflow-hidden rounded-3xl border border-slate-800 bg-slate-900 shadow-xl">
          <FeedHeader
            post={post}
            formattedDate={formattedDate}
            source="POST_DETAIL"
          />

{post.content && (
  <div className="px-4 pb-2 pt-1">
    <PostContent
      content={post.content}
      postPublicId={post.publicId}
      source={"POST_DETAIL"}
    />
  </div>
)}

<FeedMedia
  media={post.media}
  postPublicId={post.publicId}
  source="POST_DETAIL"
/>

          <FeedActions
            publicId={post.publicId}
            likes={post.likeCount}
            comments={post.commentCount}
            liked={post.likedByCurrentUser}
            saved={post.savedByCurrentUser}
            commentsOpen={commentsOpen}
            onLike={()=>
              likeMutation.mutateAsync({
                publicId: post.publicId,
                source: "POST_DETAIL",
              }
            )}
            onSave={(publicId) =>
              saveMutation.mutateAsync({
                publicId,
                saved: post.savedByCurrentUser,
                source: "POST_DETAIL",
              })
            }
            onComment={() => setCommentsOpen(true)}
          />
        </article>
      </div>

      {commentsOpen ? (
        <CommentsModal
          publicId={post.publicId}
          onClose={() => setCommentsOpen(false)}
          source="POST_DETAIL"
        />
      ) : null}
    </>
  );
}