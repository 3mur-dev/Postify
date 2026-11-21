import { useState } from "react";
import Dialog from "../../components/ui/Dialog";

interface EditPostModalProps {
  open: boolean;
  post: {
    publicId: string;
    content: string;
  } | null;
  isPending: boolean;
  onClose: () => void;
  onSave: (content: string) => void;
}

export function EditPostModal({
  open,
  post,
  isPending,
  onClose,
  onSave,
}: EditPostModalProps) {

  const [content, setContent] = useState(post?.content ?? "");

  const maxLength = 220;

  function handleSave() {
    const trimmedContent = content.trim();

    if (!trimmedContent) {
      return;
    }

    onSave(trimmedContent);
  }

  return (
    <Dialog open={open} onOpenChange={(value) => {
        if(!value) {
            onClose()
        }
    }}
        title="Edit Post"
        >
      <div className="w-full max-w-lg rounded-2xl border border-white/10 bg-slate-950 p-6 shadow-2xl">
        <div className="mb-5">
          <h2 className="text-lg font-semibold text-white">
            Edit post
          </h2>

          <p className="mt-1 text-sm text-slate-500">
            Update your caption.
          </p>
        </div>

        <textarea
          value={content}
          onChange={(event) =>
            setContent(event.target.value.slice(0, maxLength))
          }
          rows={6}
          autoFocus
          disabled={isPending}
          placeholder="What's happening?"
          className="
            w-full
            resize-none
            rounded-xl
            border border-white/10
            bg-white/5
            p-4
            text-sm
            leading-6
            text-white
            outline-none
            placeholder:text-slate-500
            focus:border-white/20
            disabled:opacity-50
          "
        />

        <div className="mt-2 flex justify-end">
          <span className="text-xs text-slate-500">
            {content.length}/{maxLength}
          </span>
        </div>

        <div className="mt-6 flex justify-end gap-3">
          <button
            type="button"
            onClick={onClose}
            disabled={isPending}
            className="
              rounded-xl
              px-4
              py-2
              text-sm
              font-medium
              text-slate-300
              transition
              hover:bg-white/10
              disabled:cursor-not-allowed
              disabled:opacity-50
            "
          >
            Cancel
          </button>

          <button
            type="button"
            onClick={handleSave}
            disabled={!content.trim() || isPending}
            className="
              rounded-xl
              bg-white
              px-4
              py-2
              text-sm
              font-semibold
              text-slate-950
              transition
              hover:bg-slate-200
              disabled:cursor-not-allowed
              disabled:opacity-50
            "
          >
            {isPending ? "Saving..." : "Save"}
          </button>
        </div>
      </div>
    </Dialog>
  );
}