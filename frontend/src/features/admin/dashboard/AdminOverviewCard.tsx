import {
  Users,
  FileText,
  MessageCircle,
  Heart,
  UserCheck,
  type LucideIcon,
} from "lucide-react";

import type { OverviewMetrics } from "../../../api/types";

interface Props {
  overview: OverviewMetrics;
}

export default function AdminOverviewCards({ overview }: Props) {
  const cards = [
    {
      key: "total-users",
      label: "Total Users",
      value: overview.totalUsers,
      icon: Users,
      accent: "text-sky-400",
    },
    {
      key: "active-users",
      label: "Active Users",
      value: overview.activeUsers,
      icon: UserCheck,
      accent: "text-emerald-400",
    },
    {
      key: "total-posts",
      label: "Total Posts",
      value: overview.totalPosts,
      icon: FileText,
      accent: "text-amber-400",
    },
    {
      key: "total-comments",
      label: "Total Comments",
      value: overview.totalComments,
      icon: MessageCircle,
      accent: "text-indigo-400",
    },
    {
      key: "total-likes",
      label: "Total Likes",
      value: overview.totalLikes,
      icon: Heart,
      accent: "text-pink-400",
    },
  ];

  return (
    <div className="grid gap-4 grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-3">
      {cards.map((card, i) => {
       const Icon: LucideIcon = card.icon;
        const isWide = i < 3; // first row: first 3 cards

        return (
          <div
            key={card.key}
            className={`flex flex-col justify-between ${
              isWide ? "rounded-md p-5" : "rounded-md p-4"
            } border border-slate-800 bg-slate-900/50`}
          >
            <div className="flex items-start justify-between gap-4">
              <div>
                <p className="text-sm text-slate-400">{card.label}</p>
                <p className="mt-3 text-3xl font-semibold text-white">
                  {card.value.toLocaleString()}
                </p>
              </div>

              <div className="flex-shrink-0">
                <div
                  className={`rounded-md bg-slate-800/60 p-2 ${card.accent}`}
                >
                  <Icon size={22} />
                </div>
              </div>
            </div>

            <div className="mt-3 text-xs text-slate-400">{i === 0 ? "All registered users" : i === 1 ? "Users active in range" : i === 2 ? "Posts published" : ""}</div>
          </div>
        );
      })}
    </div>
  );
}