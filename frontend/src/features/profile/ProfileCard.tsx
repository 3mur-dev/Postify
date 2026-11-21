import type { ProfileResponse } from "../../api/profileApi";
import { Grid3X3, Bookmark, MessageCircle } from "lucide-react";
import Avatar from "../home/components/Avatar";
import { useToggleFollow } from "../../hooks/useToggleFollow";
import { useLocation, useNavigate } from "react-router-dom";
import { useState } from "react";
import { FollowListDialog } from "./FollowListDialog";
import type { FollowListType } from "../../api/followApi";
import { createConversation } from "../../api/conversationApi";
import { useMutation, useQueryClient } from "@tanstack/react-query";


interface Props {
  profile: ProfileResponse;
  profileQueryUsername: string;
  isAuthenticated: boolean;
  activeTab: "posts" | "saved";
  onTabChange: (tab: "posts" | "saved") => void;
  isOwner: boolean;
  onEditProfile: () => void;
}

export function ProfileCard({ profile, profileQueryUsername, isAuthenticated, activeTab, onTabChange, isOwner, onEditProfile }: Props) {
  const followMutation = useToggleFollow();
  const navigate = useNavigate();
  const location = useLocation();
  const queryClient = useQueryClient();
  const [followList, setFollowList] = useState<FollowListType | null>(null);
  const canViewFollowLists =
    isOwner || (profile.following && !followMutation.isPending);
    
  const startConversation = useMutation({
     mutationFn: createConversation,
     onSuccess: async ({ publicId }) => {
       await queryClient.invalidateQueries({ queryKey: ["conversations"] });
       navigate(`/messages/${encodeURIComponent(publicId)}`);
     },
   });

  const followProfile = () => {
    if (!isAuthenticated) {
      navigate("/login", {
        state: {
          from: {
            pathname: location.pathname,
            search: location.search,
            hash: location.hash,
          },
        },
      });
      return;
    }

    followMutation.mutate({
      username: profile.username,
      profileQueryUsername,
    });
  };

  return (
    <div className="mx-auto w-full max-w-5xl px-2 py-2 sm:px-5 lg:px-6 lg:py-6">
      <div className="overflow-hidden rounded-2xl border border-white/10 bg-white/5 shadow-2xl shadow-black/40 backdrop-blur-xl sm:rounded-3xl">
        <div className="relative h-36 sm:h-56">
            <div className="absolute inset-0 bg-gradient-to-br from-fuchsia-500 via-purple-600 to-cyan-500 opacity-90" />
            <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_left,rgba(255,255,255,0.28),transparent_30%),radial-gradient(circle_at_bottom_right,rgba(255,255,255,0.10),transparent_28%)]" />
            <div className="absolute inset-x-0 bottom-0 h-24 bg-gradient-to-t from-slate-950/85 to-transparent" />
          </div>

          <div className="px-3 pb-5 sm:px-8 sm:pb-8">
            <div className="-mt-14 flex flex-col gap-4 sm:-mt-20 sm:flex-row sm:items-end sm:justify-between sm:gap-6">
              <div className="flex flex-col items-center gap-4 sm:flex-row sm:items-end sm:gap-5">
                <div className="relative mx-auto sm:mx-0">
                  <div className="absolute -inset-1 rounded-full bg-gradient-to-r from-fuchsia-500 via-purple-500 to-cyan-500 blur-md" />
                  <Avatar
                    imageUrl={profile.avatarUrl}
                    name={profile.username}
                    className="relative h-20 w-20 border-4 border-slate-950 shadow-xl sm:h-28 sm:w-28"
                  />
                </div>

                <div className="text-center sm:text-left">
                  <div className="flex flex-col items-center gap-2 sm:flex-row sm:items-center sm:gap-3">
                    <div className="flex flex-wrap items-center justify-center gap-2 sm:justify-start">
                      <h1 className="text-lg font-bold tracking-tight sm:text-xl">{profile.username}</h1>
                      {isOwner && (
                        <button
                          onClick={onEditProfile}
                          type="button"
                          className="rounded-xl border border-white/10 bg-white/5 px-3 py-1.5 text-sm font-medium transition hover:border-fuchsia-500/40 hover:bg-fuchsia-500/10"
                        >
                          Edit Profile
                        </button>
                      )}
                      {!isOwner && (
                        <button
                          onClick={followProfile}
                          type="button"
                          disabled={followMutation.isPending}
                          className="rounded-xl border border-white/10 bg-white/5 px-3 py-1.5 text-sm font-medium transition hover:border-fuchsia-500/40 hover:bg-fuchsia-500/10 disabled:cursor-not-allowed disabled:opacity-60"
                        >
                          {profile.following ? "Unfollow" : "Follow"}
                        </button>

                        
                      )}
                      {!isOwner && (
                        <button
                          type="button"
                          aria-label={`Message ${profile.username}`}
                          title={`Message ${profile.username}`}
                          disabled={startConversation.isPending}
                          onClick={() => startConversation.mutate(profile.publicId)}
                          className="flex size-10 shrink-0 items-center justify-center rounded-full text-muted-foreground transition hover:bg-background hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:opacity-50"
                        >
                          <MessageCircle aria-hidden="true" className="size-4" />
                        </button>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div className="mt-4 grid grid-cols-3 gap-2 sm:gap-3">
                {[
                  { label: "Posts", value: profile.posts.length },
                  { label: "Followers", value: profile.followersCount, listType: "followers" as const },
                  { label: "Following", value: profile.followingCount, listType: "following" as const },
                ].map(({ label, value, listType }) => (
                  <div
                    key={label}
                    className="rounded-xl border border-white/10 bg-white/5 px-2 py-2.5 text-center shadow-lg shadow-black/20 sm:rounded-2xl sm:px-3 sm:py-3"
                  >
                    {listType ? (
                      <button
                        type="button"
                        onClick={() => setFollowList(listType)}
                        aria-label={`View ${profile.username}'s ${label.toLowerCase()}`}
                        className="w-full cursor-pointer rounded-lg transition hover:bg-white/5 focus:outline-none focus:ring-2 focus:ring-fuchsia-500/50"
                      >
                        <div className="text-base font-extrabold text-white sm:text-xl">{value}</div>
                        <div className="mt-1 text-[10px] uppercase tracking-[0.2em] text-slate-400 sm:text-xs">
                          {label}
                        </div>
                      </button>
                    ) : (
                      <>
                        <div className="text-base font-extrabold sm:text-xl">{value}</div>
                        <div className="mt-1 text-[10px] uppercase tracking-[0.2em] text-slate-400 sm:text-xs">
                          {label}
                        </div>
                      </>
                    )}
                  </div>
                ))}
              </div>

            <div className="mt-4 sm:mt-6">
              <div className="rounded-2xl border border-white/10 bg-white/5 p-3 sm:rounded-3xl sm:p-4">
                <p className="text-sm leading-6 text-slate-300 sm:text-[15px]">
                  {profile.bio || "No bio yet."}
                </p>
              </div>
            </div>
<div className="mt-4 border-t border-white/10 sm:mt-6">
  <div className="flex">
    <button
      onClick={() => onTabChange("posts")}
      className={`flex flex-1 items-center justify-center gap-2 border-b-2 py-3 text-sm transition sm:py-4 sm:text-base ${
        activeTab === "posts"
          ? "border-fuchsia-500 text-white"
          : "border-transparent text-slate-400 hover:text-white"
      }`}
    >
      <Grid3X3 size={18} />
      <span>Posts</span>
    </button>

    <button
      onClick={() => onTabChange("saved")}
      className={`flex flex-1 items-center justify-center gap-2 border-b-2 py-3 text-sm transition sm:py-4 sm:text-base ${
        activeTab === "saved"
          ? "border-fuchsia-500 text-white"
          : "border-transparent text-slate-400 hover:text-white"
      }`}
    >
      <Bookmark size={18} />
      <span>Saved</span>
    </button>
  </div>
</div>
<FollowListDialog
  open={followList !== null}
  onOpenChange={(open) => {
    if (!open) setFollowList(null);
  }}
  username={profile.username}
  listType={followList ?? "followers"}
  canView={canViewFollowLists}
  isAuthenticated={isAuthenticated}
  isFollowPending={followMutation.isPending}
  onFollow={followProfile}
/>
        </div>
      </div>
    </div>
  );
}