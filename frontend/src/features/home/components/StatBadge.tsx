interface StatBadgeProps {
  label: string;
  value: number | string;
}

export default function StatBadge({ label, value }: StatBadgeProps) {
  return (
    <div className="rounded-full border border-slate-800 bg-slate-800/70 px-3 py-1 text-sm text-slate-300">
      <span className="mr-1 font-semibold text-white">{value}</span>
      {label}
    </div>
  );
}
