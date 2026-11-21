import type { SystemMetrics } from "../../../api/types";

interface Props {
  system: SystemMetrics;
}

function formatUptime(seconds: number) {
  const days = Math.floor(seconds / 86400);
  const hours = Math.floor((seconds % 86400) / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);

  return `${days}d ${hours}h ${minutes}m`;
}

function ProgressBar({ value }: { value: number }) {
  return (
    <div className="w-full rounded bg-slate-800/60 h-2 overflow-hidden">
      <div
        className={`h-2 ${value > 85 ? "bg-rose-500" : value > 65 ? "bg-amber-500" : "bg-emerald-400"}`}
        style={{ width: `${Math.min(100, Math.max(0, value))}%` }}
      />
    </div>
  );
}

export default function AdminSystemMetrics({ system }: Props) {
  return (
    <section className="rounded-md border border-slate-800 bg-slate-900/40 p-5">
      <div>
        <h2 className="text-lg font-semibold text-white">System Health</h2>
        <p className="text-sm text-slate-400">Current server health and resource usage.</p>
      </div>

      <div className="mt-4 grid gap-4 grid-cols-1 sm:grid-cols-2 lg:grid-cols-4">
        <div className="space-y-2">
          <div className="flex items-center justify-between">
            <p className="text-sm text-slate-300">CPU Usage</p>
            <p className="text-sm font-medium text-white">{system.cpuUsage.toFixed(1)}%</p>
          </div>
          <ProgressBar value={system.cpuUsage} />
        </div>

        <div className="space-y-2">
          <div className="flex items-center justify-between">
            <p className="text-sm text-slate-300">Memory Usage</p>
            <p className="text-sm font-medium text-white">{system.memoryUsage.toFixed(1)}%</p>
          </div>
          <ProgressBar value={system.memoryUsage} />
        </div>

        <div className="space-y-2">
          <div className="flex items-center justify-between">
            <p className="text-sm text-slate-300">Disk Usage</p>
            <p className="text-sm font-medium text-white">{system.diskUsage.toFixed(1)}%</p>
          </div>
          <ProgressBar value={system.diskUsage} />
        </div>

        <div className="space-y-2">
          <div className="flex items-center justify-between">
            <p className="text-sm text-slate-300">Uptime</p>
            <p className="text-sm font-medium text-white">{formatUptime(system.uptimeSeconds)}</p>
          </div>
          <div className="text-xs text-slate-400">Server status: <span className="text-emerald-400">Online</span></div>
        </div>
      </div>
    </section>
  );
}