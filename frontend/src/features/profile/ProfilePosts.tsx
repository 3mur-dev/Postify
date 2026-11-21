import {
  Flag,
  Heart,
  MessageCircle,
  MoreVertical,
  Pencil,
  Trash2,
} from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";

import type { ProfilePostDto } from "../../api/types";
import PostContent from "../home/PostContent";
import { resolveImageUrl } from "../../utils/image";

interface Props {
  posts: ProfilePostDto[];
  currentUsername: string | null;
  onDeletePost: (publicId: string) => void;
  onEditPost: (publicId: string, content: string) => void;
  onReportPost: (publicId: string) => void;
}

export function ProfilePosts({
  posts,
  currentUsername,
  onDeletePost,
  onEditPost,
  onReportPost,
}: Props) {
  
  const navigate = useNavigate();

  const [openMenu, setOpenMenu] = useState<string | null>(null);

  const menuRef = useRef<HTMLDivElement | null>(null);

  // Close menu when clicking anywhere outside of it
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (
        menuRef.current &&
        !menuRef.current.contains(event.target as Node)
      ) {
        setOpenMenu(null);
      }
    }

    document.addEventListener("mousedown", handleClickOutside);

    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, []);

  if (posts.length === 0) {
    return (
      <div className="mt-8 rounded-2xl border border-white/10 bg-white/5 p-8 text-center">
        <p className="text-sm font-medium text-slate-300">No posts yet</p>

        <p className="mt-1 text-xs text-slate-500">
          Posts will appear here when you share something.
        </p>
      </div>
    );
  }

  function openPost(post: ProfilePostDto) {
    navigate(`/${post.username}/posts/${post.publicId}`);
  }

  return (
    <div className="mt-8 grid grid-cols-2 gap-2 sm:grid-cols-3 md:gap-3">
      {posts.map((post) => {
        const isOwner = currentUsername === post.username;
        const firstMedia = post.media?.[0];
        if (!post.content && !firstMedia) {
          return null;
        }

        return (
          <article
            key={post.publicId}
            role="button"
            tabIndex={0}
            onClick={(event) => {
  const target = event.target;

  if (
    target instanceof HTMLElement &&
    target.closest("a, button, [data-stop-navigation='true']")
  ) {
    return;
  }

  openPost(post);
}}
            onKeyDown={(event) => {
              if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                openPost(post);
              }
            }}
            className="group relative aspect-square cursor-pointer overflow-hidden rounded-xl border border-white/10 bg-slate-900 outline-none focus:ring-2 focus:ring-white/40"
          >
            {firstMedia ? (
  firstMedia.type === "VIDEO" ? (
    <video
  src={resolveImageUrl(firstMedia.url)}
  className="h-full w-full object-cover transition-transform duration-500 ease-out group-hover:scale-105"
  muted
  playsInline
  autoPlay
  loop
  preload="metadata"
/>
  ) : (
    <img
      src={resolveImageUrl(firstMedia.url)}
      alt={post.content || "Post image"}
      className="h-full w-full object-cover transition-transform duration-500 ease-out group-hover:scale-105"
    />
  )
) : (
<div
  className="flex h-full w-full items-center justify-center bg-gradient-to-br from-slate-800 to-slate-950 p-5"
>
  <PostContent
    content={post.content ?? ""}
    postPublicId={post.publicId}
    source="PROFILE"
  />
</div>
)}

            {/* More button + menu */}
            <div
              ref={openMenu === post.publicId ? menuRef : null}
              className="absolute right-2 top-2 opacity-0 transition-opacity duration-200 group-hover:opacity-100 group-focus:opacity-100"
            >
              <button
                type="button"
                onClick={(event) => {
                  event.stopPropagation();

                  setOpenMenu(
                    openMenu === post.publicId ? null : post.publicId
                  );
                }}
                className="flex h-8 w-8 items-center justify-center rounded-full bg-black/60 text-white backdrop-blur-sm transition hover:bg-black/80"
                aria-label="Post options"
                aria-expanded={openMenu === post.publicId}
              >
                <MoreVertical className="h-5 w-5" />
              </button>

              {/* Menu */}
              {openMenu === post.publicId && (
                <div
                  onClick={(event) => event.stopPropagation()}
                  className="absolute right-0 top-10 z-20 w-36 overflow-hidden rounded-xl border border-white/10 bg-slate-900/95 p-1 shadow-xl backdrop-blur-md"
                >
                  {isOwner ? (
                    <>
                      {/* Edit */}
                      <button
                        type="button"
                        className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left text-sm text-slate-200 transition hover:bg-white/10"
                        onClick={() => {
                          onEditPost(
                            post.publicId,
                            post.content ?? ""
                          );

                          setOpenMenu(null);
                        }}
                      >
                        <Pencil className="h-4 w-4" />
                        Edit
                      </button>

                      {/* Delete */}
                      <button
                        type="button"
                        className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left text-sm text-red-400 transition hover:bg-red-500/10"
                        onClick={() => {
                          onDeletePost(post.publicId);
                          setOpenMenu(null);
                        }}
                      >
                        <Trash2 className="h-4 w-4" />
                        Delete
                      </button>
                    </>
                  ) : (
                    /* Report */
                    <button
                      type="button"
                      className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left text-sm text-slate-200 transition hover:bg-white/10"
                      onClick={() => {
                        onReportPost(post.publicId);
                        setOpenMenu(null);
                      }}
                    >
                      <Flag className="h-4 w-4" />
                      Report
                    </button>
                  )}
                </div>
              )}
            </div>

            {/* Hover stats */}
            <div className="pointer-events-none absolute inset-0 flex items-end bg-gradient-to-t from-black/80 via-black/20 to-transparent opacity-0 transition-opacity duration-200 group-hover:opacity-100 group-focus:opacity-100">
              <div className="flex w-full items-center gap-5 p-4 text-sm font-medium text-white">
                <span className="flex items-center gap-1.5">
                  <Heart className="h-4 w-4 fill-white" />
                  {post.likeCount}
                </span>

                <span className="flex items-center gap-1.5">
                  <MessageCircle className="h-4 w-4 fill-white" />
                  {post.commentCount}
                </span>
              </div>
            </div>
          </article>
        );
      })}
    </div>
  );
}