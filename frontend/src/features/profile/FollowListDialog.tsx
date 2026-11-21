import { useInfiniteQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { getProfileFollowList, type FollowListType } from "../../api/followApi";
import Dialog from "../../components/ui/Dialog";
import Avatar from "../home/components/Avatar";

interface FollowListDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  username: string;
  listType: FollowListType;
  canView: boolean;
  isAuthenticated: boolean;
  isFollowPending: boolean;
  onFollow: () => void;
}

export function FollowListDialog({
  open,
  onOpenChange,
  username,
  listType,
  canView,
  isAuthenticated,
  isFollowPending,
  onFollow,
}: FollowListDialogProps) {
  const query = useInfiniteQuery({
    queryKey: ["profile", username, listType],
    queryFn: ({ pageParam }) =>
      getProfileFollowList(username, listType, pageParam),
    initialPageParam: 0,
    getNextPageParam: (lastPage) =>
      lastPage.last ? undefined : lastPage.page + 1,
    enabled: open && canView,
    staleTime: 0,
    refetchOnMount: "always",
  });

  const users = query.data?.pages.flatMap((page) => page.content) ?? [];
  const totalUsers = query.data?.pages[0]?.totalElements;
  const title = listType === "followers" ? "Followers" : "Following";

  return (
    <Dialog open={open} onOpenChange={onOpenChange} title={title}>
      {!canView ? (
        <div className="py-8 text-center">
          <p className="text-sm text-slate-300">
            {isAuthenticated
              ? `Follow @${username} to see their ${listType}.`
              : `Log in and follow @${username} to see their ${listType}.`}
          </p>
          <button
            type="button"
            onClick={onFollow}
            disabled={isFollowPending}
            className="mt-4 rounded-xl bg-fuchsia-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-fuchsia-500 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {isFollowPending
              ? "Following..."
              : isAuthenticated
                ? `Follow @${username}`
                : "Log in"}
          </button>
        </div>
      ) : query.isLoading ||
        (query.isFetching && !query.isFetchingNextPage) ? (
        <p className="py-8 text-center text-sm text-slate-400">
          {query.isLoading ? "Loading" : "Refreshing"} {listType}...
        </p>
      ) : query.isError ? (
        <div className="py-8 text-center">
          <p className="text-sm text-red-300">
            Could not load {listType}. Please try again.
          </p>
          <button
            type="button"
            onClick={() => void query.refetch()}
            className="mt-3 text-sm font-medium text-fuchsia-300 hover:text-fuchsia-200"
          >
            Retry
          </button>
        </div>
      ) : users.length === 0 ? (
        <p className="py-8 text-center text-sm text-slate-400">
          No {listType} yet.
        </p>
      ) : (
        <div className="max-h-[60vh] overflow-y-auto">
          <p className="mb-2 px-2 text-xs text-slate-400">
            Showing {users.length}
            {totalUsers === undefined ? "" : ` of ${totalUsers}`}
          </p>
          <div className="space-y-2">
            {users.map((user) => (
              <Link
                key={user.id}
                to={`/profile/${user.username}`}
                onClick={() => onOpenChange(false)}
                className="flex items-center gap-3 rounded-xl px-2 py-2 transition hover:bg-slate-800"
              >
                <Avatar
                  imageUrl={user.avatarUrl}
                  name={user.username}
                  size="md"
                />
                <span className="truncate text-sm font-medium text-white">
                  {user.username}
                </span>
              </Link>
            ))}
          </div>
          {query.hasNextPage && (
            <button
              type="button"
              onClick={() => void query.fetchNextPage()}
              disabled={query.isFetchingNextPage}
              className="w-full rounded-xl py-2 text-sm font-medium text-fuchsia-300 transition hover:bg-slate-800 disabled:opacity-60"
            >
              {query.isFetchingNextPage ? "Loading..." : "Load more"}
            </button>
          )}
        </div>
      )}
    </Dialog>
  );
}
