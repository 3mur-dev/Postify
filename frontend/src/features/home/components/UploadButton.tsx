import { ImagePlus } from "lucide-react";

interface UploadButtonProps {
  onClick: () => void;
}

export default function UploadButton({
  onClick,
}: UploadButtonProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="flex items-center gap-2 rounded-full px-3 py-2 text-sky-400 transition hover:bg-slate-800"
    >
      <ImagePlus className="h-5 w-5" />

      <span className="text-sm font-medium">
        Add Photo
      </span>
    </button>
  );
}