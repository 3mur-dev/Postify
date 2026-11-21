import { useState } from "react";
import { useNavigate } from "react-router-dom";

import AdminUserTable from "../../features/admin/users/AdminUserTable";
import { useGetUsers } from "../../hooks/admin/users/useGetUsers";

export default function AdminUsersPage() {
  const navigate = useNavigate();

  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);

  const { data, isLoading, isError } = useGetUsers({
    search,
    page,
    size: 10,
  });

  const users = data?.content ?? [];

  function handleSearch(value: string) {
    setSearch(value);
    setPage(0);
  }

  function handleSelectUser(publicId: string) {
    navigate(`/admin/users/${publicId}`);
  }

  return (
    <div className="space-y-6 p-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-white">
          Users
        </h1>

        <p className="mt-1 text-sm text-slate-400">
          Manage Postify users and their account status.
        </p>
      </div>

      {/* Search */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <input
          value={search}
          onChange={(event) =>
            handleSearch(event.target.value)
          }
          placeholder="Search users..."
          className="w-full max-w-md rounded-lg border border-slate-800 bg-slate-900 px-4 py-2.5 text-sm text-white outline-none placeholder:text-slate-500 focus:border-fuchsia-500"
        />

        <p className="whitespace-nowrap text-sm text-slate-500">
          {data?.totalElements ?? 0} users
        </p>
      </div>

      {/* Loading */}
      {isLoading && (
        <div className="rounded-xl border border-slate-800 bg-slate-900 px-6 py-12 text-center text-sm text-slate-400">
          Loading users...
        </div>
      )}

      {/* Error */}
      {isError && (
        <div className="rounded-xl border border-red-500/20 bg-red-500/5 px-6 py-12 text-center text-sm text-red-400">
          Failed to load users.
        </div>
      )}

      {/* Table */}
      {!isLoading && !isError && (
        <AdminUserTable
          users={users}
          onSelectUser={handleSelectUser}
        />
      )}

      {/* Pagination */}
      {!isLoading && !isError && data && data.totalPages > 0 && (
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <button
            disabled={page === 0}
            onClick={() => setPage((current) => current - 1)}
            className="rounded-lg border border-slate-800 px-4 py-2 text-sm text-slate-300 transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-40"
          >
            Previous
          </button>

          <span className="text-sm text-slate-400">
            Page {page + 1} of {data.totalPages}
          </span>

          <button
            disabled={data.last}
            onClick={() => setPage((current) => current + 1)}
            className="rounded-lg border border-slate-800 px-4 py-2 text-sm text-slate-300 transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-40"
          >
            Next
          </button>
        </div>
      )}
    </div>
  );
}