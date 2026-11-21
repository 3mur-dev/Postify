interface MediaPreviewProps {
  preview: {
    url: string;
    type: "IMAGE" | "VIDEO";
  };
  onRemove: () => void;
}

export default function MediaPreview({
  preview,
  onRemove,
}: MediaPreviewProps) {
  return (
    <div className="relative overflow-hidden rounded-xl">
      {preview.type === "IMAGE" ? (
        <img
          src={preview.url}
          alt="Post preview"
          className="max-h-[400px] w-full object-cover"
        />
      ) : (
        <video
          src={preview.url}
          controls
          className="max-h-[400px] w-full object-cover"
        />
      )}

      <button
        type="button"
        onClick={onRemove}
        className="absolute right-2 top-2 rounded-full bg-black/70 px-3 py-1 text-white"
      >
        Remove
      </button>
    </div>
  );
}