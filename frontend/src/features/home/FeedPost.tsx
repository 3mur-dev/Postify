import type { MouseEvent } from "react";

import { useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";

import type { InteractionSource, PostFeedDto } from "../../api/types";
import FeedActions from "./FeedActions";
import FeedHeader from "./FeedHeader";
import FeedMedia from "./FeedMedia";
import PostContent from "./PostContent";

import { useLikePost } from "../../hooks/usePost";
import { useSavePost } from "../../hooks/useSavePost";
import { trackInteraction } from "../../api/interactionApi";
interface Props {
  post: PostFeedDto;
  source: InteractionSource;
  commentsPublicId: string | null;
  setCommentsPublicId: React.Dispatch<React.SetStateAction<string | null>>;
}

export default function FeedPost({
  post,
  commentsPublicId,
  setCommentsPublicId,
  source,
}: Props) {
  const navigate = useNavigate();
  const postRef = useRef<HTMLElement | null>(null);
  const hasImpressed = useRef(false);
  const viewStartTime = useRef<number | null>(null);
  const isViewing = useRef(false);

  const formattedDate = new Date(post.createdAt).toLocaleString(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  });

  const likeMutation = useLikePost();
  const saveMutation = useSavePost();

useEffect(() => {
  const element = postRef.current;

  if (!element) {
    return;
  }

  const observer = new IntersectionObserver(
    ([entry]) => {
      const isVisible =
        entry.isIntersecting &&
        entry.intersectionRatio >= 0.5;

      // VIEW START
      if (isVisible && !isViewing.current) {

        // IMPRESSION — only once
        if (!hasImpressed.current) {
          hasImpressed.current = true;

          void trackInteraction({
            postPublicId: post.publicId,
            eventType: "IMPRESSION",
            source,
          })
            .then(() => {
            })
            .catch(() => {
            // Silently fail — impression tracking is non-critical
          });
        }

        isViewing.current = true;
        viewStartTime.current = performance.now();

        void trackInteraction({
          postPublicId: post.publicId,
          eventType: "VIEW_START",
          source,
        })
          .then(() => {
          })
          .catch(() => {
            // Silently fail — view tracking is non-critical
          });

        return;
      }

      // VIEW END
      if (!isVisible && isViewing.current) {
        isViewing.current = false;

        const startTime = viewStartTime.current;

        if (startTime !== null) {
          const durationMs = Math.round(
            performance.now() - startTime
          );

          void trackInteraction({
            postPublicId: post.publicId,
            eventType: "VIEW_END",
            source,
            durationMs,
          })
            .then(() => {
          
            })
            .catch(() => {
              // Silently fail — view end tracking is non-critical
            });
        }

        viewStartTime.current = null;
      }
    },
    {
      threshold: 0.5,
    }
  );

  observer.observe(element);

  return () => {
    observer.disconnect();

    // Close an active viewing session on unmount
    if (
      isViewing.current &&
      viewStartTime.current !== null
    ) {
      const durationMs = Math.round(
        performance.now() - viewStartTime.current
      );

      void trackInteraction({
        postPublicId: post.publicId,
        eventType: "VIEW_END",
        source,
        durationMs,
      })
        .then(() => {
          
        })
        .catch(() => {
          // Silently fail — cleanup tracking is non-critical
        });

      isViewing.current = false;
      viewStartTime.current = null;
    }
  };
}, [post.publicId, source]);

  const handleOpenPost = (event: MouseEvent<HTMLElement>) => {
    const target = event.target;

    if (
      target instanceof HTMLElement &&
      target.closest(
        "button, a, input, textarea, select, [role='button'], [data-stop-navigation='true']"
      )
    ) {
      return;
    }

    void trackInteraction({
        postPublicId: post.publicId,
        eventType: "POST_OPEN",
        source,
      }).catch(() => {
        // Silently fail — interaction tracking is non-critical
      });

    navigate(`/${post.username}/posts/${post.publicId}`);
  };

  return (
    <article
    ref={postRef}
      onClick={handleOpenPost}
      className="cursor-pointer overflow-hidden rounded-xl border border-slate-800 bg-slate-900/95 shadow-[0_8px_30px_rgba(2,6,23,0.3)] transition-all duration-300 hover:-translate-y-0.5 hover:border-slate-700 hover:shadow-[0_10px_36px_rgba(2,6,23,0.38)]"
    >
      <FeedHeader
        post={post}
        formattedDate={formattedDate}
        source={source}
      />

{post.content && (
  <div className="px-4 pb-2 pt-1">
    <PostContent
      content={post.content}
      postPublicId={post.publicId}
      source={source}
    />
  </div>
)}

      <FeedMedia
       media={post.media} 
       postPublicId={post.publicId}
       source={source}
       />

      <FeedActions
        publicId={post.publicId}
        likes={post.likeCount}
        comments={post.commentCount}
        liked={post.likedByCurrentUser}
        saved={post.savedByCurrentUser}
        commentsOpen={commentsPublicId === post.publicId}
        onLike={() =>
          likeMutation.mutateAsync({ 
            publicId: post.publicId ,
            source: source 
          })
      }
        onSave={() =>
          saveMutation.mutateAsync({
            publicId: post.publicId,
            saved: post.savedByCurrentUser,
            source,
          })
        }
        onComment={(publicId) => setCommentsPublicId(publicId)}
      />
    </article>
  );
}