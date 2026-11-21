import {
  UserPlus,
  UserX,
  UserMinus,
  TrendingUp,
} from "lucide-react";

import type { UserMetrics } from "../../../api/types";

interface Props {
  users: UserMetrics;
}

export default function AdminUserMetrics({ users }: Props) {
  return (
    <section className="rounded-md border border-slate-800 bg-slate-900/40 p-5">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-lg font-semibold text-white">User Activity</h2>
          <p className="text-sm text-slate-400">User activity and growth for the selected period.</p>
        </div>
      </div>

      <div className="mt-4 divide-y divide-slate-800">
        <div className="flex items-center justify-between py-3">
          <div className="flex items-center gap-3">
            <div className="rounded-md bg-slate-800/60 p-2 text-sky-400">
              <UserPlus size={18} />
            </div>
            <div>
              <p className="text-sm text-slate-300">New Users</p>
              <p className="text-lg font-semibold text-white">{users.newUsersInPeriod.toLocaleString()}</p>
            </div>
          </div>

          <div className="text-sm text-slate-400">&nbsp;</div>
        </div>

        <div className="flex items-center justify-between py-3">
          <div className="flex items-center gap-3">
            <div className="rounded-md bg-slate-800/60 p-2 text-rose-400">
              <UserX size={18} />
            </div>
            <div>
              <p className="text-sm text-slate-300">Banned Users</p>
              <p className="text-lg font-semibold text-white">{users.bannedUsers.toLocaleString()}</p>
            </div>
          </div>

          <div className="text-sm text-slate-400">&nbsp;</div>
        </div>

        <div className="flex items-center justify-between py-3">
          <div className="flex items-center gap-3">
            <div className="rounded-md bg-slate-800/60 p-2 text-amber-400">
              <UserMinus size={18} />
            </div>
            <div>
              <p className="text-sm text-slate-300">Suspended Users</p>
              <p className="text-lg font-semibold text-white">{users.suspendedUsers.toLocaleString()}</p>
            </div>
          </div>

          <div className="text-sm text-slate-400">&nbsp;</div>
        </div>

        <div className="flex items-center justify-between py-3">
          <div className="flex items-center gap-3">
            <div className="rounded-md bg-slate-800/60 p-2 text-emerald-400">
              <TrendingUp size={18} />
            </div>
            <div>
              <p className="text-sm text-slate-300">User Growth</p>
              <p className="text-lg font-semibold text-white">{users.userGrowthPercentage.toFixed(1)}%</p>
            </div>
          </div>

          <div className="text-sm text-slate-400">Compared to previous period</div>
        </div>
      </div>
    </section>
  );
}