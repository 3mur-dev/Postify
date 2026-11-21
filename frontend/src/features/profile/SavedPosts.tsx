import { useEffect, useRef, useState } from "react";
import {
  Heart,
  MessageCircle,
  Bookmark,
  CalendarDays,
} from "lucide-react";
import { useNavigate, useParams } from "react-router-dom";

import { type SavedPostResponse } from "../../api/profileApi";
import { getSavedPosts } from "../../api/saveApi";
import type { ProfilePostDto } from "../../api/types";
import { resolveImageUrl } from "../../utils/image";
import { trackInteraction } from "../../api/interactionApi";
import Avatar from "../home/components/Avatar";
import PostContent from "../home/PostContent";

type SavedPostsProps = {
  onLike: (publicId: string) => Promise<void>;
};

function SavedVideo({ url }: { url: string }) {
  const videoRef = useRef<HTMLVideoElement | null>(null);

  useEffect(() => {
    const video = videoRef.current;

    if (!video) return;

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          video.play().catch(() => {});
        } else {
          video.pause();
        }
      },
      {
        threshold: 0.6,
      }
    );

    observer.observe(video);

    return () => {
      observer.disconnect();
    };
  }, []);

  return (
    <video
      ref={videoRef}
      src={resolveImageUrl(url)}
      className="h-72 w-full object-cover"
      muted
      playsInline
      loop
      preload="metadata"
    />
  );
}

export function SavedPosts({ onLike }: SavedPostsProps) {
  const [posts, setPosts] = useState<SavedPostResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(false);
  const [reloadKey, setReloadKey] = useState(0);

  const { username } = useParams<{ username: string }>();
  const navigate = useNavigate();

  useEffect(() => {
    if (!username) return;

    let isMounted = true;

    const load = async () => {
      setLoading(true);
      setLoadError(false);

      try {
        const response = await getSavedPosts(username);

        if (isMounted) {
          setPosts(response.content);
        }
      } catch {
        if (isMounted) {
          setLoadError(true);
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    };

    load();

    return () => {
      isMounted = false;
    };
  }, [reloadKey, username]);

  const handleLike = async (publicId: string) => {
    const previousPosts = posts;

    setPosts((currentPosts) =>
      currentPosts.map((item) => {
        if (item.post.publicId !== publicId) {
          return item;
        }

        const isLiked = item.post.likedByCurrentUser;

        return {
          ...item,
          post: {
            ...item.post,
            likedByCurrentUser: !isLiked,
            likeCount: item.post.likeCount + (isLiked ? -1 : 1),
          },
        };
      })
    );

    try {
      await onLike(publicId);
    } catch {
      setPosts(previousPosts);
    }
  };

  function openPost(post: ProfilePostDto) {
    navigate(`/${post.username}/posts/${post.publicId}`);
  }

  if (loading) {
    return (
      <div className="mt-8 flex justify-center">
        <div className="h-10 w-10 animate-spin rounded-full border-4 border-fuchsia-500 border-t-transparent" />
      </div>
    );
  }

  if (loadError) {
    return (
      <div className="mt-8 rounded-3xl border border-rose-900/50 bg-rose-950/20 p-12 text-center">
        <h2 className="text-xl font-bold">Saved posts couldn’t load</h2>
        <p className="mt-2 text-slate-400">
          Check your connection and try again.
        </p>
        <button
          type="button"
          onClick={() => setReloadKey((current) => current + 1)}
          className="mt-4 rounded-lg border border-slate-700 px-4 py-2 text-sm text-white"
        >
          Try again
        </button>
      </div>
    );
  }

  if (posts.length === 0) {
    return (
      <div className="mt-8 rounded-3xl border border-white/10 bg-white/5 p-12 text-center backdrop-blur-xl">
        <Bookmark className="mx-auto mb-4 h-14 w-14 text-slate-500" />

        <h2 className="text-xl font-bold">
          No saved posts
        </h2>

        <p className="mt-2 text-slate-400">
          Posts you save will appear here.
        </p>
      </div>
    );
  }

  return (
    <div className="mt-8 space-y-6">
      {posts.map(({ post, savedAt }) => {
        const firstMedia = post.media?.[0];

        return (
          <article
            key={post.publicId}
            onClick={() => openPost(post)}
            className="group overflow-hidden rounded-3xl border border-white/10 bg-white/5 shadow-xl shadow-black/20 backdrop-blur-xl transition-all duration-300 hover:-translate-y-1 hover:border-fuchsia-500/40 hover:bg-white/10"
          >
            {/* Media */}
            {firstMedia && (
              <div className="relative overflow-hidden">
                {firstMedia.type === "VIDEO" ? (
                  <SavedVideo url={firstMedia.url} />
                ) : (
                  <img
                    src={resolveImageUrl(firstMedia.url)}
                    alt={post.content || "Post image"}
                    className="h-72 w-full object-cover transition duration-500 group-hover:scale-105"
                  />
                )}

                {firstMedia.type === "VIDEO" && (
                  <div className="pointer-events-none absolute right-4 top-4 rounded-full bg-black/60 px-3 py-1 text-xs font-medium text-white backdrop-blur-sm">
                    Video
                  </div>
                )}
              </div>
            )}

            {/* Content */}
            <div className="p-6">
              {/* Header */}
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-4">
                  <button
                    type="button"
                    className="flex-shrink-0 cursor-pointer rounded-full transition hover:opacity-80"
                    onClick={(event) => {
                      event.stopPropagation();

                  void trackInteraction({
                    postPublicId: post.publicId,
                    eventType: "PROFILE_OPEN",
                    source: "SAVED_POSTS"
                  })

                      navigate(`/profile/${post.username}`);
                    }}
                  >
                    <Avatar imageUrl={post.avatarUrl} name={post.username} size="xl" />
                  </button>

                  <div>
                    <h3 className="font-semibold">
                      {post.username}
                    </h3>

                    <div className="mt-1 flex items-center gap-2 text-xs text-slate-400">
                      <CalendarDays size={13} />

                      Saved{" "}
                      {new Date(savedAt).toLocaleDateString(
                        undefined,
                        {
                          month: "short",
                          day: "numeric",
                          year: "numeric",
                        }
                      )}
                    </div>
                  </div>
                </div>

                <Bookmark
                  size={20}
                  className="fill-fuchsia-500 text-fuchsia-500"
                />
              </div>

              {/* Text */}
{post.content && (
  <div className="px-4 pb-2 pt-1">
    <PostContent
      content={post.content}
      postPublicId={post.publicId}
      source="SAVED_POSTS"
    />
  </div>
)}

              {/* Actions */}
              <div
                className="mt-6 flex items-center gap-6 border-t border-white/10 pt-4 text-slate-400"
                onClick={(event) => event.stopPropagation()}
              >
                {/* Like */}
                <div className="flex items-center gap-2 transition hover:text-pink-400">
                  <button
                    type="button"
                    onClick={() => void handleLike(post.publicId)}
                    aria-label="Like post"
                  >
                    <Heart
                      size={18}
                      className={
                        post.likedByCurrentUser
                          ? "fill-pink-500 text-pink-500"
                          : ""
                      }
                    />
                  </button>

                  <span>{post.likeCount}</span>
                </div>

                {/* Comments */}
                <div className="flex items-center gap-2 transition hover:text-cyan-400">
                  <MessageCircle size={18} />

                  <span>{post.commentCount}</span>
                </div>
              </div>
            </div>
          </article>
        );
      })}
    </div>
  );
}