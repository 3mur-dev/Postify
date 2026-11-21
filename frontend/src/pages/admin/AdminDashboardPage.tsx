import { useState } from "react";
import type { RangePreset } from "../../api/types";
import { useGetAdminDashboard } from "../../hooks/admin/dashboard/useGetAdminDashboard";

import AdminUserMetrics from "../../features/admin/dashboard/AdminUserMetrics";
import AdminOverviewCards from "../../features/admin/dashboard/AdminOverviewCard";
import AdminSystemMetrics from "../../features/admin/dashboard/AdminSystemMetrics";
import AdminEngagementMetrics from "../../features/admin/dashboard/AdminEngagementMetrics";
import AdminChart from "../../features/admin/dashboard/AdminChart";

const RANGE_OPTIONS = [
  { value: "TODAY", label: "Today" },
  { value: "LAST_7_DAYS", label: "Last 7 days" },
  { value: "LAST_30_DAYS", label: "Last 30 days" },
  { value: "LAST_90_DAYS", label: "Last 90 days" },
];

export default function AdminDashboard() {
  const [rangePreset, setRangePreset] = useState<RangePreset>("LAST_7_DAYS");

  const { data, isLoading, isError } = useGetAdminDashboard({
    rangePreset,
    timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
    sections: [
      "OVERVIEW",
      "CHARTS",
      "SYSTEM_METRICS",
      "USER_METRICS",
      "ENGAGEMENT_METRICS",
    ],
  });

  if (isLoading) return <div className="text-slate-400">Loading dashboard...</div>;
  if (isError || !data) return <div className="text-red-400">Failed to load dashboard.</div>;

  return (
    <div className="space-y-8">
      <div className="flex flex-col gap-4 rounded-2xl border border-slate-800 bg-slate-900/40 p-5 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-white">Dashboard</h1>
          <p className="mt-1 text-sm text-slate-400">Overview of your Postify platform.</p>
        </div>

        <div className="flex flex-col gap-2 sm:flex-row sm:items-center">
          <label className="text-sm text-slate-300">Range</label>
          <select
            value={rangePreset}
            onChange={(e) => setRangePreset(e.target.value as RangePreset )}
            className="rounded-md bg-slate-800/60 px-3 py-1 text-sm text-white border border-slate-700"
            aria-label="Dashboard range"
          >
            {RANGE_OPTIONS.map((o) => (
              <option key={o.value} value={o.value}>
                {o.label}
              </option>
            ))}
          </select>
        </div>
      </div>

      <AdminOverviewCards overview={data.overview} />

      <div className="grid gap-6 lg:grid-cols-2">
        <AdminUserMetrics users={data.users} />
        <AdminEngagementMetrics engagement={data.engagement} />
      </div>

      <AdminSystemMetrics system={data.system} />

      <div className="space-y-6">
        <h3 className="text-sm font-medium text-slate-300">Analytics</h3>

        <div className="grid gap-6 lg:grid-cols-2">
          <AdminChart
            title="User Growth"
            description="New users over the selected period."
            data={data.userGrowthChart}
            valueLabel="Users"
          />

          <AdminChart
            title="Post Growth"
            description="Posts created over the selected period."
            data={data.postGrowthChart}
            valueLabel="Posts"
          />
        </div>

        <div>
          <AdminChart
            title="Engagement"
            description="Engagement activity over the selected period."
            data={data.engagementChart}
            valueLabel="Engagement"
          />
        </div>
      </div>
    </div>
  );
}