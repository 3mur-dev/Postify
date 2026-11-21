import { useEffect, useMemo, useState } from "react";
import { ProfileSkeleton } from "../features/profile/ProfileSkeleton";
import { ProfileCard } from "../features/profile/ProfileCard";
import { ProfilePosts } from "../features/profile/ProfilePosts";
import { SavedPosts } from "../features/profile/SavedPosts";
import { EditProfileModal } from "../features/profile/EditProfileModal";
import { EditPostModal } from "../features/profile/EditPostModal";
import { useNavigate, useParams } from "react-router-dom";
import { useDeletePost, useEditPost, useLikePost } from "../hooks/usePost";
import { useGetProfile } from "../hooks/useGetProfile";

export default function ProfilePage() {
  const [activeTab, setActiveTab] = useState<"posts" | "saved">("posts");
  const [editOpen, setEditOpen] = useState(false);
  const [editingPost, setEditingPost] = useState<{
  publicId: string;
  content: string;
} | null>(null);
  const { username } = useParams();
  const navigate = useNavigate();
  const isAuthenticated = Boolean(localStorage.getItem("token"));
  const likeMutation = useLikePost(); 
  const deleteMutation = useDeletePost();
  const editMutation = useEditPost();

  const currentUsername = useMemo(() => {
    if (!isAuthenticated) return null;

    try {
      const token = localStorage.getItem("token");
      if (!token) return null;

      const payload = JSON.parse(atob(token.split(".")[1]));
      return payload.username || payload.sub || null;
    } catch {
      return null;
    }
  }, [isAuthenticated]);

  const profileUsername = username ?? currentUsername;

  useEffect(() => {
    if (!username && profileUsername && isAuthenticated) {
      navigate(`/profile/${profileUsername}`, { replace: true });
    }
  }, [isAuthenticated, username, profileUsername, navigate]);

  const {
    data: profile,
    isLoading,
    error,
  } = useGetProfile(profileUsername);

  const isOwner = Boolean(
    isAuthenticated &&
      currentUsername &&
      profile?.username &&
      profile.username === currentUsername
  );

  if (!isAuthenticated && !username) {
    return null;
  }

  if (isLoading) {
  return (
    <main className="min-h-screen bg-slate-950 pb-24 text-white md:pb-0">
      <ProfileSkeleton />
    </main>
  );
}

if (error) {
  return (
    <main className="flex min-h-screen items-center justify-center bg-slate-950 pb-24 text-white md:pb-0">
      <div className="rounded-xl border border-red-500/30 bg-red-500/10 p-6">
        <h2 className="text-xl font-bold">Error</h2>
<p>{error instanceof Error ? error.message : "Failed to load profile."}</p>
      </div>
    </main>
  );
}

if (!profile) {
  return null;
}

  return (
    <main className="min-h-screen bg-slate-950 pb-24 text-white md:pb-0">
      <ProfileCard
        profile={profile}
        profileQueryUsername={profileUsername ?? profile.username}
        isAuthenticated={isAuthenticated}
        activeTab={activeTab}
        onTabChange={setActiveTab}
        isOwner={isOwner}
        onEditProfile={() => setEditOpen(true)}
      />

      <EditProfileModal
        open={editOpen}
        onClose={() => setEditOpen(false)}
        profile={profile}
      />

      {activeTab === "posts" ? (
<ProfilePosts
  posts={profile.posts}
  currentUsername={currentUsername}
  onDeletePost={(publicId) => {
    deleteMutation.mutate({
      publicId,
      profileQueryUsername: profileUsername ?? profile.username,
    });
  }}
  onEditPost={(publicId, content) => {
    setEditingPost({
      publicId,
      content,
    });
  }}
  onReportPost={(publicId) => {
    console.log("Report post:", publicId);
  }}
/>
      ) : (
        <SavedPosts
          onLike={(publicId) =>
            likeMutation.mutateAsync({
              publicId,
              source: "SAVED_POSTS",
            })
          }
        />
      )}

      <EditPostModal
      key={editingPost?.publicId}
  open={editingPost !== null}
  post={editingPost}
  isPending={editMutation.isPending}
  onClose={() => setEditingPost(null)}
  onSave={(content) => {
    if (!editingPost) return;

    editMutation.mutate(
      {
        publicId: editingPost.publicId,
        content,
        profileQueryUsername: profileUsername ?? profile.username,
      },
      {
        onSuccess: () => {
          setEditingPost(null);
        },
      }
    );
  }}
/>

    </main>
  );
}