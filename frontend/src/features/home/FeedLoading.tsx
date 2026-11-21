export default function FeedLoading() {
  return (
    <div className="mt-6 space-y-6">
      {[1, 2].map((item) => (
        <article
          key={item}
          className="animate-pulse overflow-hidden rounded-2xl border border-slate-800 bg-slate-900"
        >
          {/* Header */}
          <div className="flex items-center gap-3 p-5">
            <div className="h-11 w-11 rounded-full bg-slate-800" />

            <div className="flex-1 space-y-2">
              <div className="h-4 w-32 rounded bg-slate-800" />
              <div className="h-3 w-20 rounded bg-slate-800" />
            </div>
          </div>

          {/* Content */}
          <div className="space-y-2 px-5 pb-4">
            <div className="h-4 w-full rounded bg-slate-800" />
            <div className="h-4 w-5/6 rounded bg-slate-800" />
            <div className="h-4 w-2/3 rounded bg-slate-800" />
          </div>

          {/* Image Placeholder */}
          <div className="aspect-square w-full bg-slate-800" />

          {/* Actions */}
          <div className="flex items-center justify-between px-5 py-4">
            <div className="flex gap-6">
              <div className="h-5 w-12 rounded bg-slate-800" />
              <div className="h-5 w-12 rounded bg-slate-800" />
            </div>

            <div className="h-5 w-5 rounded bg-slate-800" />
          </div>
        </article>
      ))}
    </div>
  );
}