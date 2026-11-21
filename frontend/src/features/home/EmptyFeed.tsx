export default function EmptyFeed() {
  return (
    <div className="rounded-2xl border border-slate-800 bg-slate-900 p-10 text-center shadow-lg">
      <h2 className="text-xl font-semibold text-white">Your feed is quiet right now</h2>
      <p className="mt-2 text-sm text-slate-400">
        Follow more people or create the first post to get things started.
      </p>
    </div>
  );
}
