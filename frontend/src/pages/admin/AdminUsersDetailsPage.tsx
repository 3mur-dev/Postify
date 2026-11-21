import { Link, useParams } from "react-router-dom";
import {
  ArrowLeft,
  CalendarDays,
  FileText,
  Users,
  UserPlus,
  Mail,
  Shield,
  Ban,
  UserX,
} from "lucide-react";

import { useGetUserDetails } from "../../hooks/admin/users/useGetUsersDetails";
import Avatar from "../../features/home/components/Avatar";

export default function AdminUserDetailsPage() {
  const { publicId } = useParams<{ publicId: string }>();

  const {
    data: user,
    isLoading,
    isError,
  } = useGetUserDetails(publicId!);

  if (isLoading) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center text-slate-400">
        Loading user...
      </div>
    );
  }

  if (isError || !user) {
    return (
      <div className="p-6">
        <Link
          to="/admin/users"
          className="mb-6 inline-flex items-center gap-2 text-sm text-slate-400 transition hover:text-white"
        >
          <ArrowLeft size={16} />
          Back to Users
        </Link>

        <div className="rounded-xl border border-red-500/20 bg-red-500/10 p-6 text-red-400">
          Failed to load user.
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6 p-6">
      {/* Header */}
      <div>
        <Link
          to="/admin/users"
          className="mb-4 inline-flex items-center gap-2 text-sm text-slate-400 transition hover:text-white"
        >
          <ArrowLeft size={16} />
          Back to Users
        </Link>

        <h1 className="text-2xl font-semibold text-white">
          User Details
        </h1>

        <p className="mt-1 text-sm text-slate-400">
          Manage and view information about this user.
        </p>
      </div>

      {/* User Header */}
      <section className="rounded-2xl border border-slate-800 bg-slate-900 p-6">
        <div className="flex flex-col gap-6 md:flex-row md:items-center md:justify-between">
          <div className="flex items-center gap-4">
            <Avatar
              imageUrl={user.avatarUrl}
              name={user.username}
              className="h-20 w-20"
            />

            <div>
              <div className="flex items-center gap-3">
                <h2 className="text-xl font-semibold text-white">
                  {user.username}
                </h2>

                <StatusBadge status={user.status} />
              </div>

              <div className="mt-2 flex flex-wrap items-center gap-3 text-sm text-slate-400">
                <span className="inline-flex items-center gap-1.5">
                  <Mail size={14} />
                  {user.email}
                </span>

                <span className="inline-flex items-center gap-1.5">
                  <Shield size={14} />
                  {user.role}
                </span>
              </div>
            </div>
          </div>

          {/* Actions */}
          <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-end">
            {user.status === "ACTIVE" && (
              <>
                <button
                  type="button"
                  className="inline-flex items-center gap-2 rounded-lg border border-yellow-500/30 bg-yellow-500/10 px-4 py-2 text-sm font-medium text-yellow-400 transition hover:bg-yellow-500/20"
                >
                  <UserX size={16} />
                  Suspend
                </button>

                <button
                  type="button"
                  className="inline-flex items-center gap-2 rounded-lg border border-red-500/30 bg-red-500/10 px-4 py-2 text-sm font-medium text-red-400 transition hover:bg-red-500/20"
                >
                  <Ban size={16} />
                  Ban
                </button>
              </>
            )}
          </div>
        </div>
      </section>

      {/* Stats */}
      <div className="grid gap-4 sm:grid-cols-3">
        <StatCard
          icon={<FileText size={20} />}
          label="Posts"
          value={user.postsCount}
        />

        <StatCard
          icon={<Users size={20} />}
          label="Followers"
          value={user.followersCount}
        />

        <StatCard
          icon={<UserPlus size={20} />}
          label="Following"
          value={user.followingCount}
        />
      </div>

      {/* About */}
      <section className="rounded-2xl border border-slate-800 bg-slate-900 p-6">
        <h2 className="text-lg font-semibold text-white">
          About
        </h2>

        <p className="mt-3 text-sm leading-6 text-slate-400">
          {user.bio || "This user has not added a bio."}
        </p>
      </section>

      {/* Account Information */}
      <section className="rounded-2xl border border-slate-800 bg-slate-900 p-6">
        <h2 className="text-lg font-semibold text-white">
          Account Information
        </h2>

        <div className="mt-5 grid gap-5 sm:grid-cols-2">
          <InfoItem
            label="Username"
            value={user.username}
          />

          <InfoItem
            label="Email"
            value={user.email}
          />

          <InfoItem
            label="Role"
            value={user.role}
          />

          <InfoItem
            label="Status"
            value={user.status}
          />

          <InfoItem
            label="Joined"
            value={formatDate(user.createdAt)}
            icon={<CalendarDays size={15} />}
          />

          <InfoItem
            label="Public ID"
            value={user.publicId}
          />
        </div>
      </section>
    </div>
  );
}

function StatCard({
  icon,
  label,
  value,
}: {
  icon: React.ReactNode;
  label: string;
  value: number | undefined
}) {
  return (
    <div className="rounded-2xl border border-slate-800 bg-slate-900 p-5">
      <div className="flex items-center gap-3">
        <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-slate-800 text-slate-400">
          {icon}
        </div>

        <span className="text-sm text-slate-400">
          {label}
        </span>
      </div>

      <div className="mt-4 text-2xl font-semibold text-white">
        <p>{value?.toLocaleString() ?? "0"}</p>
      </div>
    </div>
  );
}

function InfoItem({
  label,
  value,
  icon,
}: {
  label: string;
  value: string;
  icon?: React.ReactNode;
}) {
  return (
    <div>
      <p className="text-xs font-medium uppercase tracking-wide text-slate-500">
        {label}
      </p>

      <p className="mt-1 flex items-center gap-2 break-all text-sm text-slate-200">
        {icon}
        {value}
      </p>
    </div>
  );
}

function StatusBadge({
  status,
}: {
  status: "ACTIVE" | "SUSPENDED" | "BANNED";
}) {
  const styles = {
    ACTIVE:
      "bg-emerald-500/10 text-emerald-400 border-emerald-500/20",
    SUSPENDED:
      "bg-yellow-500/10 text-yellow-400 border-yellow-500/20",
    BANNED:
      "bg-red-500/10 text-red-400 border-red-500/20",
  };

  return (
    <span
      className={`rounded-full border px-2.5 py-1 text-xs font-medium ${styles[status]}`}
    >
      {status}
    </span>
  );
}

function formatDate(date: string) {
  return new Date(date).toLocaleDateString(undefined, {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
}