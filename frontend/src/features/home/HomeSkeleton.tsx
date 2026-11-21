export default function HomeSkeleton() {
  return (
    <div className="max-w-2xl mx-auto space-y-6">
      {Array.from({ length: 4 }).map((_, index) => (
        <div
          key={index}
          className="bg-white dark:bg-neutral-900 border border-neutral-200 dark:border-neutral-800 rounded-2xl p-5 animate-pulse"
        >
          {/* Header */}
          <div className="flex items-center gap-3">
            <div className="h-11 w-11 rounded-full bg-neutral-200 dark:bg-neutral-800" />

            <div className="flex-1 space-y-2">
              <div className="h-4 w-32 rounded bg-neutral-200 dark:bg-neutral-800" />
              <div className="h-3 w-20 rounded bg-neutral-200 dark:bg-neutral-800" />
            </div>
          </div>

          {/* Content */}
          <div className="mt-5 space-y-3">
            <div className="h-4 w-full rounded bg-neutral-200 dark:bg-neutral-800" />
            <div className="h-4 w-11/12 rounded bg-neutral-200 dark:bg-neutral-800" />
            <div className="h-4 w-3/4 rounded bg-neutral-200 dark:bg-neutral-800" />
          </div>

          {/* Image */}
          <div className="mt-5 h-80 w-full rounded-xl bg-neutral-200 dark:bg-neutral-800" />

          {/* Actions */}
          <div className="mt-5 flex items-center gap-6">
            <div className="h-5 w-16 rounded bg-neutral-200 dark:bg-neutral-800" />
            <div className="h-5 w-16 rounded bg-neutral-200 dark:bg-neutral-800" />
            <div className="h-5 w-16 rounded bg-neutral-200 dark:bg-neutral-800" />
          </div>
        </div>
      ))}
    </div>
  );
}