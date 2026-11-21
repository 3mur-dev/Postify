import type { AdminUser, UserStatus } from "../../../api/types";

interface AdminUserTableProps {
  users: AdminUser[];
  onSelectUser: (publicId: string) => void;
}

function getStatusClasses(status: UserStatus) {
  switch (status) {
    case "ACTIVE":
      return "bg-emerald-500/10 text-emerald-400 border-emerald-500/20";

    case "SUSPENDED":
      return "bg-yellow-500/10 text-yellow-400 border-yellow-500/20";

    case "BANNED":
      return "bg-red-500/10 text-red-400 border-red-500/20";

    default:
      return "bg-slate-500/10 text-slate-400 border-slate-500/20";
  }
}

export default function AdminUserTable({
  users,
  onSelectUser,
}: AdminUserTableProps) {
  return (
    <div className="overflow-hidden rounded-xl border border-slate-800 bg-slate-900">
      <div className="hidden sm:block">
        <table className="w-full">
          <thead className="border-b border-slate-800 bg-slate-950/50">
            <tr>
              <th className="px-6 py-4 text-left text-xs font-medium uppercase tracking-wider text-slate-400">
                User
              </th>

              <th className="px-6 py-4 text-left text-xs font-medium uppercase tracking-wider text-slate-400">
                Email
              </th>

              <th className="px-6 py-4 text-left text-xs font-medium uppercase tracking-wider text-slate-400">
                Status
              </th>

              <th className="px-6 py-4 text-left text-xs font-medium uppercase tracking-wider text-slate-400">
                Joined
              </th>

              <th className="px-6 py-4 text-right text-xs font-medium uppercase tracking-wider text-slate-400">
                Action
              </th>
            </tr>
          </thead>

          <tbody className="divide-y divide-slate-800">
            {users.map((user) => (
              <tr
                key={user.publicId}
                className="transition hover:bg-slate-800/40"
              >
                <td className="px-6 py-4">
                  <div>
                    <p className="font-medium text-white">@{user.username}</p>
                    <p className="mt-1 text-xs text-slate-500">{user.publicId}</p>
                  </div>
                </td>

                <td className="px-6 py-4 text-sm text-slate-300">{user.email}</td>

                <td className="px-6 py-4">
                  <span
                    className={`inline-flex rounded-full border px-2.5 py-1 text-xs font-medium ${getStatusClasses(
                      user.status
                    )}`}
                  >
                    {user.status}
                  </span>
                </td>

                <td className="px-6 py-4 text-sm text-slate-400">
                  {new Date(user.createdAt).toLocaleDateString()}
                </td>

                <td className="px-6 py-4 text-right">
                  <button
                    onClick={() => onSelectUser(user.publicId)}
                    className="rounded-lg px-3 py-2 text-sm font-medium text-fuchsia-400 transition hover:bg-fuchsia-500/10 hover:text-fuchsia-300"
                  >
                    View
                  </button>
                </td>
              </tr>
            ))}

            {users.length === 0 && (
              <tr>
                <td
                  colSpan={5}
                  className="px-6 py-12 text-center text-sm text-slate-500"
                >
                  No users found.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <div className="space-y-4 px-4 py-4 sm:hidden">
        {users.length === 0 && (
          <div className="rounded-2xl border border-slate-800 bg-slate-950/70 p-6 text-center text-sm text-slate-500">
            No users found.
          </div>
        )}

        {users.map((user) => (
          <article
            key={user.publicId}
            className="rounded-2xl border border-slate-800 bg-slate-950/70 p-4"
          >
            <div className="flex flex-col gap-4">
              <div className="flex items-start justify-between gap-4">
                <div className="min-w-0">
                  <p className="truncate text-base font-medium text-white">@{user.username}</p>
                  <p className="mt-2 truncate text-sm text-slate-400">{user.email}</p>
                </div>

                <span
                  className={`inline-flex rounded-full border px-2.5 py-1 text-xs font-medium ${getStatusClasses(
                    user.status
                  )}`}
                >
                  {user.status}
                </span>
              </div>

              <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <p className="text-sm text-slate-400">
                  Joined {new Date(user.createdAt).toLocaleDateString()}
                </p>

                <button
                  onClick={() => onSelectUser(user.publicId)}
                  className="self-start rounded-lg border border-fuchsia-500/30 bg-fuchsia-500/10 px-3 py-2 text-sm font-medium text-fuchsia-300 transition hover:bg-fuchsia-500/20 sm:self-auto"
                >
                  View
                </button>
              </div>
            </div>
          </article>
        ))}
      </div>
    </div>
  );
}
