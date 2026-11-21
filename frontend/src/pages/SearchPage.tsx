import { useState } from "react";
import { MessageCircle, Search, X } from "lucide-react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";

import { createConversation } from "../api/conversationApi";
import { useGetSearchResults } from "../hooks/useGetSearchResults";
import Avatar from "../features/home/components/Avatar";
import { useAuth } from "../context/useAuth";
import { resolveImageUrl } from "../utils/image";

export default function SearchPage() {
  const [query, setQuery] = useState("");
  const [activeTab, setActiveTab] = useState<"posts" | "people">("posts");

  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { isAuthenticated, user: currentUser } = useAuth();
  const startConversation = useMutation({
    mutationFn: createConversation,
    onSuccess: async ({ publicId }) => {
      await queryClient.invalidateQueries({ queryKey: ["conversations"] });
      navigate(`/messages/${encodeURIComponent(publicId)}`);
    },
  });

  const { data, isLoading, isError } = useGetSearchResults(query);

  const posts = data?.posts ?? [];
  const users = data?.users ?? [];

  return (
    <div className="min-h-screen bg-background">
      <div className="mx-auto w-full max-w-3xl px-4 py-6 sm:px-6 sm:py-8">
        {/* Search */}
        <div className="relative">
          <Search
            className="
              pointer-events-none
              absolute left-3.5 top-1/2
              size-4
              -translate-y-1/2
              text-muted-foreground
            "
          />

          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            type="text"
            placeholder="Search posts and people"
            className="
              h-11 w-full
              rounded-xl
              border border-border/60
              bg-muted/60
              pl-10 pr-10
              text-sm
              text-foreground
              outline-none
              transition
              placeholder:text-muted-foreground
              hover:bg-muted
              focus:border-border
              focus:bg-background
              focus:ring-2
              focus:ring-ring/10
            "
          />

          {query && (
            <button
              type="button"
              aria-label="Clear search"
              onClick={() => setQuery("")}
              className="
                absolute right-2.5 top-1/2
                flex size-6
                -translate-y-1/2
                items-center justify-center
                rounded-full
                text-muted-foreground
                transition
                hover:bg-background
                hover:text-foreground
              "
            >
              <X className="size-3.5" />
            </button>
          )}
        </div>

        {/* Tabs */}
        <div className="mt-6 border-b border-border">
          <div className="flex">
            {(["posts", "people"] as const).map((tab) => {
              const isActive = activeTab === tab;
              return (
                <button
                  key={tab}
                  type="button"
                  onClick={() => setActiveTab(tab)}
                  className={`
                    relative flex-1 py-3
                    text-sm font-medium
                    transition-all
                    border-transparent text-slate-400 hover:text-white border-fuchsia-500
                    ${
                      isActive
                        ? "text-foreground"
                        : "text-muted-foreground hover:text-foreground "
                    }
                  `}
                >
                  {tab === "posts" ? "Posts" : "People"}
                  {isActive && (
                    <span
                      className="
                        absolute inset-x-0 -bottom-px
                        h-0.5 bg-foreground
                        transition-all
                      "
                    />
                  )}
                </button>
              );
            })}
          </div>
        </div>

        {/* Results */}
        <div className="mt-6">
          {/* Loading */}
          {isLoading && (
            <div className="space-y-3">
              {activeTab === "posts" ? (
                <div className="grid grid-cols-3 gap-1.5">
                  {Array.from({ length: 9 }).map((_, index) => (
                    <div
                      key={index}
                      className="aspect-square animate-pulse rounded-md bg-muted"
                    />
                  ))}
                </div>
              ) : (
                Array.from({ length: 5 }).map((_, index) => (
                  <div
                    key={index}
                    className="flex items-center gap-3 rounded-xl px-3 py-2.5"
                  >
                    <div className="size-11 animate-pulse rounded-full bg-muted" />

                    <div className="space-y-2">
                      <div className="h-3 w-24 animate-pulse rounded bg-muted" />
                      <div className="h-2.5 w-16 animate-pulse rounded bg-muted" />
                    </div>
                  </div>
                ))
              )}
            </div>
          )}

          {/* Error */}
          {isError && !isLoading && (
            <div className="flex min-h-40 flex-col items-center justify-center text-center">
              <p className="text-sm font-medium text-foreground">
                Something went wrong
              </p>

              <p className="mt-1 text-sm text-muted-foreground">
                Try searching again.
              </p>
            </div>
          )}

          {/* Posts */}
          {!isLoading && !isError && activeTab === "posts" && (
            <>
              {posts.length > 0 ? (
                <div className="grid grid-cols-3 gap-1.5">
                  {posts.map((post) => {
                    const hasMedia = !!post.media[0]?.url;
                    return (
                      <button
                        key={post.id}
                        type="button"
                        onClick={() => {
                          navigate(`/${post.username}/posts/${post.publicId}`);
                        }}
                        className="
                          group
                          relative
                          aspect-square
                          overflow-hidden
                          rounded-md
                          bg-muted
                          focus:outline-none
                          focus:ring-2
                          focus:ring-ring
                          focus:ring-offset-2
                        "
                      >
                        {hasMedia ? (
                          <>
                            <img
                              src={resolveImageUrl(post.media[0].url)}
                              alt={post.content || "Post"}
                              className="
                                size-full
                                object-cover
                                transition-transform
                                duration-300
                                group-hover:scale-105
                              "
                            />
                            <div
                              className="
                                pointer-events-none
                                absolute inset-0
                                bg-black/0
                                transition-colors
                                group-hover:bg-black/10
                              "
                            />
                          </>
                        ) : (
                          <div
                            className="
                              flex size-full
                              flex-col justify-center
                              p-3
                              text-left
                            "
                          >
                            <p
                              className="
                                line-clamp-3
                                text-xs
                                font-medium
                                leading-snug
                                text-foreground
                              "
                            >
                              {post.content}
                            </p>
                          </div>
                        )}
                      </button>
                    );
                  })}
                </div>
              ) : (
                <div className="flex min-h-52 flex-col items-center justify-center text-center">
                  <div className="flex size-12 items-center justify-center rounded-full bg-muted">
                    <Search className="size-5 text-muted-foreground" />
                  </div>

                  <p className="mt-3 text-sm font-medium">No posts found</p>

                  <p className="mt-1 text-sm text-muted-foreground">
                    Try searching for something else.
                  </p>
                </div>
              )}
            </>
          )}

          {/* People */}
          {!isLoading && !isError && activeTab === "people" && (
            <>
              {users.length > 0 ? (
                <div className="space-y-1">
                  {users.map((user) => (
                    <div
                      key={user.publicId}
                      className="group flex items-center gap-2 rounded-xl px-3 py-2.5 transition-colors hover:bg-muted"
                    >
                      <button
                        type="button"
                        onClick={() => navigate(`/profile/${user.username}`)}
                        className="flex min-w-0 flex-1 items-center gap-3 rounded-lg text-left focus:outline-none focus-visible:ring-2 focus-visible:ring-ring/30"
                      >
                        <div className="size-12 shrink-0 overflow-hidden rounded-full transition-transform duration-200 group-hover:scale-105">
                          <Avatar
                            imageUrl={user.avatarUrl}
                            name={user.username}
                            size="xl"
                            className="border-2 border-white/30"
                          />
                        </div>
                        <div className="min-w-0">
                          <p className="truncate text-sm font-semibold text-foreground">
                            {user.username}
                          </p>
                        </div>
                      </button>
                      {isAuthenticated &&
                        user.username !== currentUser?.username && (
                        <button
                          type="button"
                          aria-label={`Message ${user.username}`}
                          title={`Message ${user.username}`}
                          disabled={startConversation.isPending}
                          onClick={() => startConversation.mutate(user.publicId)}
                          className="flex size-10 shrink-0 items-center justify-center rounded-full text-muted-foreground transition hover:bg-background hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:opacity-50"
                        >
                          <MessageCircle aria-hidden="true" className="size-4" />
                        </button>
                      )}
                    </div>
                  ))}
                </div>
              ) : (
                <div className="flex min-h-52 flex-col items-center justify-center text-center">
                  <div className="flex size-12 items-center justify-center rounded-full bg-muted">
                    <Search className="size-5 text-muted-foreground" />
                  </div>

                  <p className="mt-3 text-sm font-medium">No people found</p>

                  <p className="mt-1 text-sm text-muted-foreground">
                    Try searching for another username.
                  </p>
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}