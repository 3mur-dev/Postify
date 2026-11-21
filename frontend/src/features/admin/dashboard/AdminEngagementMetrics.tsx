import {
  Heart,
  MessageCircle,
  HeartHandshake,
  Activity,
} from "lucide-react";

import type { EngagementMetrics } from "../../../api/types";

interface Props {
  engagement: EngagementMetrics;
}

export default function AdminEngagementMetrics({ engagement }: Props) {
  return (
    <section className="rounded-md border border-slate-800 bg-slate-900/40 p-5">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-lg font-semibold text-white">Engagement</h2>
          <p className="text-sm text-slate-400">Engagement activity across the platform.</p>
        </div>
      </div>

      <div className="mt-4 grid gap-4 grid-cols-1 sm:grid-cols-2">
        <div className="flex items-center justify-between rounded-sm bg-slate-900/60 p-3">
          <div className="flex items-center gap-3">
            <div className="rounded-md bg-slate-800/60 p-2 text-pink-400">
              <Heart size={18} />
            </div>
            <div>
              <p className="text-sm text-slate-300">Likes Received</p>
              <p className="text-lg font-semibold text-white">{engagement.likesReceived.toLocaleString()}</p>
            </div>
          </div>

          <div className="text-sm text-slate-400">&nbsp;</div>
        </div>

        <div className="flex items-center justify-between rounded-sm bg-slate-900/60 p-3">
          <div className="flex items-center gap-3">
            <div className="rounded-md bg-slate-800/60 p-2 text-indigo-400">
              <MessageCircle size={18} />
            </div>
            <div>
              <p className="text-sm text-slate-300">Comments Received</p>
              <p className="text-lg font-semibold text-white">{engagement.commentsReceived.toLocaleString()}</p>
            </div>
          </div>

          <div className="text-sm text-slate-400">&nbsp;</div>
        </div>

        <div className="flex items-center justify-between rounded-sm bg-slate-900/60 p-3">
          <div className="flex items-center gap-3">
            <div className="rounded-md bg-slate-800/60 p-2 text-amber-400">
              <HeartHandshake size={18} />
            </div>
            <div>
              <p className="text-sm text-slate-300">Average Likes / Post</p>
              <p className="text-lg font-semibold text-white">{engagement.averageLikesPerPost.toFixed(2)}</p>
            </div>
          </div>

          <div className="text-sm text-slate-400">Avg</div>
        </div>

        <div className="flex items-center justify-between rounded-sm bg-slate-900/60 p-3">
          <div className="flex items-center gap-3">
            <div className="rounded-md bg-slate-800/60 p-2 text-sky-400">
              <MessageCircle size={18} />
            </div>
            <div>
              <p className="text-sm text-slate-300">Average Comments / Post</p>
              <p className="text-lg font-semibold text-white">{engagement.averageCommentsPerPost.toFixed(2)}</p>
            </div>
          </div>

          <div className="text-sm text-slate-400">Avg</div>
        </div>

        <div className="col-span-1 sm:col-span-2 flex items-center justify-between rounded-sm bg-slate-900/60 p-3">
          <div className="flex items-center gap-3">
            <div className="rounded-md bg-slate-800/60 p-2 text-emerald-400">
              <Activity size={18} />
            </div>
            <div>
              <p className="text-sm text-slate-300">Engagement Rate</p>
              <p className="text-lg font-semibold text-white">{engagement.engagementRate.toFixed(2)}%</p>
            </div>
          </div>

          <div className="text-sm text-slate-400">Overall</div>
        </div>
      </div>
    </section>
  );
}