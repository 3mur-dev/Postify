import {
  ResponsiveContainer,
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
} from "recharts";

import type { ChartDataPoint } from "../../../api/types";

interface Props {
  title: string;
  description?: string;
  data: ChartDataPoint[];
  valueLabel?: string;
}

export default function AdminChart({ title, description, data, valueLabel }: Props) {
  const hasData = Array.isArray(data) && data.length > 0;

  return (
    <section className="rounded-md border border-slate-800 bg-slate-900/40 p-4">
      <div className="mb-3">
        <h4 className="text-sm font-semibold text-white">{title}</h4>
        {description ? <p className="text-xs text-slate-400">{description}</p> : null}
      </div>

      <div style={{ height: 320 }} className="w-full">
        {hasData ? (
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={data} margin={{ top: 10, right: 20, left: -10, bottom: 5 }}>
              <CartesianGrid stroke="rgba(148,163,184,0.06)" strokeDasharray="3 3" />
              <XAxis dataKey="label" tick={{ fill: "#94A3B8", fontSize: 12 }} />
              <YAxis tick={{ fill: "#94A3B8", fontSize: 12 }} />
              <Tooltip
                contentStyle={{ background: "#0f1724", borderColor: "#1f2937" }}
                formatter={(value: number | string) => [value, valueLabel ?? "Value"]}
                labelStyle={{ color: "#94A3B8" }}
              />
              <Line type="monotone" dataKey="value" stroke="#60A5FA" strokeWidth={2} dot={{ r: 2 }} />
            </LineChart>
          </ResponsiveContainer>
        ) : (
          <div className="flex h-full w-full items-center justify-center">
            <div className="text-center text-slate-400">
              <div className="text-sm">No data for the selected range.</div>
            </div>
          </div>
        )}
      </div>
    </section>
  );
}
