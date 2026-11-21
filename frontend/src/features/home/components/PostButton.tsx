import { Loader2, SendHorizontal } from "lucide-react";

interface PostButtonProps {
  disabled?: boolean;
  loading?: boolean;
  onClick?: () => void;
}

export default function PostButton({
  disabled,
  loading,
  onClick,
}: PostButtonProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled || loading}
      className="flex items-center gap-2 rounded-full bg-sky-500 px-5 py-2 text-sm font-semibold text-white transition hover:bg-sky-400 disabled:cursor-not-allowed disabled:opacity-50"
    >
      {loading ? (
        <Loader2 className="h-4 w-4 animate-spin" />
      ) : (
        <SendHorizontal className="h-4 w-4" />
      )}

      {loading ? "Posting..." : "Post"}
    </button>
  );
}