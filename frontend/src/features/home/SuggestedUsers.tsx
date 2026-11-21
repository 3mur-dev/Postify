import FollowButton from "./components/FollowButton";
import Avatar from "./components/Avatar";

const users = [
  { username: "Ava Chen", handle: "@ava" },
  { username: "Noah Patel", handle: "@noah" },
  { username: "Mina Ortiz", handle: "@mina" },
];

export default function SuggestedUsers() {
  return (
    <div className="rounded-2xl border border-slate-800 bg-slate-900 p-5 shadow-lg">
      <h2 className="mb-4 text-lg font-semibold text-white">Who to follow</h2>

      <div className="space-y-3">
        {users.map((user) => (
          <div key={user.username} className="flex items-center justify-between rounded-xl border border-slate-800 bg-slate-950/70 p-3">
            <div className="flex items-center gap-3">
              <Avatar name={user.username} size="md" />
              <div>
                <p className="font-medium text-white">{user.username}</p>
                <p className="text-sm text-slate-400">{user.handle}</p>
              </div>
            </div>

            <FollowButton />
          </div>
        ))}
      </div>
    </div>
  );
}
