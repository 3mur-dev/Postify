import { Bookmark, Heart, MessageCircle, Share2 } from "lucide-react";

interface FeedActionsProps {
  publicId: string;

  likes: number;
  comments: number;

  liked: boolean;
  saved?: boolean;
  commentsOpen?: boolean;

  onLike?: (publicId: string) => Promise<void>;
  onComment?: (publicId: string) => void;
  onShare?: (publicId: string) => void;
  onSave?: (publicId: string) => Promise<void>;
}

export default function FeedActions({
  publicId,
  likes,
  comments,
  liked,
  commentsOpen,
  saved,
  onLike,
  onComment,
  onShare,
  onSave,
}: FeedActionsProps) {

  

  const actions = [
    {
      label: "Like",
      icon: Heart,
      onClick: onLike,
      className: "hover:text-rose-400",
    },
    {
      label: "Comment",
      icon: MessageCircle,
      onClick: onComment,
      className: "hover:text-sky-400",
    },
    {
      label: "Share",
      icon: Share2,
      onClick: onShare,
      className: "hover:text-emerald-400",
    },
    {
      label: "Save",
      icon: Bookmark,
      onClick: onSave,
      className: "hover:text-amber-400",
    },
  ];

const handleClick = (
  onClick?: (publicId: string) => Promise<void> | void
) => {
  onClick?.(publicId);
};

  return (
    <div className="space-y-2 px-3 py-2.5">
      <div className="flex flex-wrap gap-1.5">
        <span className="rounded-full border border-slate-800 bg-slate-800/70 px-2 py-1 text-[11px] text-slate-300">
          <span className="mr-1 font-semibold text-white">{likes}</span>
          likes
        </span>
        <span className="rounded-full border border-slate-800 bg-slate-800/70 px-2.5 py-1 text-xs text-slate-300">
          <span className="mr-1 font-semibold text-white">{comments}</span>
          comments
        </span>
      </div>

      <div className="grid grid-cols-4 gap-1.5">
{actions.map(({ label, icon: Icon, onClick, className }) => (
  <button
    key={label}
    type="button"
    onClick={() => handleClick(onClick)}
    aria-label={label}
    aria-pressed={label === "Like" ? liked : commentsOpen}
    className={`flex items-center justify-center rounded-xl border border-slate-800 bg-slate-950/70 p-1.5 transition-all duration-300 hover:border-slate-700 hover:bg-slate-800 ${className}`}
  >
{label === "Like" ? (
  <Heart
    className={`h-3.5 w-3.5 transition-colors ${
      liked
        ? "fill-rose-500 text-rose-500"
        : "text-slate-300"
    }`}
  />
) : label === "Comment" ? (
  <MessageCircle
    className={`h-4 w-4 transition-colors ${
      commentsOpen ? "text-blue-500" : "text-slate-300"
    }`}
  />
) : label === "Save" ? (
  <Bookmark
    className={`h-4 w-4 transition-colors ${
      saved
        ? "fill-amber-400 text-amber-400"
        : "text-slate-300"
    }`}
  />
) : (
  <Icon className="h-4 w-4 text-slate-300" />
)}
  </button>
))}
      </div>
    </div>
  );
}