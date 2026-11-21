export function ProfileSkeleton() {
  return (
    <div className="mx-auto w-full max-w-5xl animate-pulse px-4 py-8 sm:px-6 lg:px-8">
      <div className="overflow-hidden rounded-3xl border border-white/10 bg-white/5 shadow-2xl backdrop-blur-xl">
        <div className="h-56 bg-gradient-to-br from-fuchsia-500/30 via-purple-500/20 to-cyan-500/20 sm:h-64" />
        <div className="px-6 pb-8 sm:px-8">
          <div className="-mt-16 flex flex-col gap-6 sm:-mt-20 sm:flex-row sm:items-end sm:justify-between">
            <div className="flex items-end gap-5">
              <div className="h-32 w-32 rounded-full border-4 border-slate-950 bg-slate-700 sm:h-40 sm:w-40" />
              <div className="space-y-3 pb-3">
                <div className="h-6 w-40 rounded-lg bg-white/10" />
                <div className="h-4 w-28 rounded-lg bg-white/10" />
                <div className="h-4 w-64 rounded-lg bg-white/10" />
              </div>
            </div>
          </div>

          <div className="mt-8 grid grid-cols-3 gap-3 sm:gap-4">
            <div className="h-24 rounded-2xl bg-white/5" />
            <div className="h-24 rounded-2xl bg-white/5" />
            <div className="h-24 rounded-2xl bg-white/5" />
          </div>

          <div className="mt-8 h-20 rounded-3xl bg-white/5" />
        </div>
      </div>
    </div>
  );
}