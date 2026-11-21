
  import FeedLoading from "../features/home/FeedLoading";
  import HomeSkeleton from "../features/home/HomeSkeleton";
  import CreatePostCard from "../features/home/components/CreatePostCard";
  import HomeFeed from "../features/home/HomeFeed";
  import EmptyFeed from "../features/home/EmptyFeed";
  import SuggestedUsers from "../features/home/SuggestedUsers";
  import { useEffect, useRef } from "react";
  import { useInfiniteFeed } from "../hooks/useInfiniteFeed";
  import { getMyProfile } from "../api/profileApi";
  import { useQuery } from "@tanstack/react-query";
  import { hasAccessToken } from "../lib/auth";
  
  export default function HomePage() {
    const loadMoreRef = useRef<HTMLDivElement>(null);

    const { data: profile } = useQuery({
      enabled: hasAccessToken(),
      queryKey: ["myProfile"],
      queryFn: getMyProfile,
    });

    const {
    data,
    error,
    isLoading,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
  } = useInfiniteFeed();

  useEffect(() => {
    const observer = new IntersectionObserver(
    ([entry]) => {
      if (
        entry.isIntersecting &&
        hasNextPage &&
        !isFetchingNextPage
      ) {
        fetchNextPage();
      }
    },
    {
      rootMargin: "400px",
    }
  );

    const current = loadMoreRef.current;

    if (current) {
      observer.observe(current);
    }

    return () => observer.disconnect();
  }, [fetchNextPage, hasNextPage, isFetchingNextPage]);

     if (isLoading) {
      return (
        <main className="min-h-screen bg-slate-950 text-white">
          <HomeSkeleton />
        </main>
      ); }


    if (error) {
      return (
        
        <main className="flex min-h-screen items-center justify-center bg-slate-950 text-white">
          <div className="rounded-xl border border-red-500/30 bg-red-500/10 p-6">
            <h2 className="text-xl font-bold">Error</h2>
            <p>
              {error instanceof Error
                ? error.message
                : "Failed to load feed."}
            </p>
          </div>
        </main>
      );
    }

        const posts = Array.from(
          new Map(
            (data?.pages.flatMap((page) => page.content) ?? []).map((post) => [
              post.publicId,
              post,
            ]),
          ).values(),
        );

    return (
      
      <main className="min-h-screen bg-slate-950 text-white">
        <div className="mx-auto flex max-w-7xl flex-col gap-6 px-3 py-4 lg:flex-row lg:px-4 lg:py-5">
          <section className="mx-auto w-full max-w-2xl flex-1">
    {profile && <CreatePostCard user={profile} />}

    <HomeFeed posts={posts} />

    {isFetchingNextPage && <FeedLoading />}

    <div ref={loadMoreRef} />

    {!hasNextPage && posts.length > 0 && (
        <p className="py-6 text-center text-slate-500">
            You're all caught up.
        </p>
    )}

    

    {!posts.length && <EmptyFeed />}
</section>
          <aside className="hidden w-full lg:block lg:w-72 xl:w-80">
            <SuggestedUsers />
          </aside>
        </div>
      </main>
    );
  }