import { useState } from "react";

interface FollowButtonProps {
  label?: string;
}

export default function FollowButton({ label = "Follow" }: FollowButtonProps) {
  const [following, setFollowing] = useState(false);

  return (
    <button
      type="button"
      onClick={() => setFollowing((value) => !value)}
      className={`rounded-full px-3 py-2 text-sm font-semibold transition-all duration-300 ${
        following
          ? "bg-slate-800 text-slate-200 hover:bg-slate-700"
          : "bg-sky-500 text-white hover:bg-sky-400"
      }`}
    >
      {following ? "Following" : label}
    </button>
  );
}
