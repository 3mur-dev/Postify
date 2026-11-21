import { useEffect, useMemo, useRef, useState } from "react";

import Avatar from "../components/Avatar";
import Dialog from "../../../components/ui/Dialog";
import type { ProfileResponse } from "../../../api/profileApi";

import MediaPreview from "./MediaPreview";
import CharacterCounter from "./CharacterCounter";
import UploadButton from "./UploadButton";
import PostButton from "./PostButton";
import { useCreatePost } from "../../../hooks/usePost";

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  user: ProfileResponse;
}

export function CreatePostModal({
  open,
  onOpenChange,
  user,
}: Props) {
  const maxLength = 1000;
  const maxMedia = 10;

  const createPost = useCreatePost();

  const [content, setContent] = useState("");
  const [media, setMedia] = useState<File[]>([]);

  const inputRef = useRef<HTMLInputElement>(null);

  const previews = useMemo(() => {
    return media.map((file) => ({
      url: URL.createObjectURL(file),
      type: file.type.startsWith("video/")
        ? ("VIDEO" as const)
        : ("IMAGE" as const),
    }));
  }, [media]);

  useEffect(() => {
    return () => {
      previews.forEach((preview) => {
        URL.revokeObjectURL(preview.url);
      });
    };
  }, [previews]);

  function handleMediaSelected(
    event: React.ChangeEvent<HTMLInputElement>
  ) {
    const files = Array.from(event.target.files ?? []);

    if (files.length === 0) return;

    setMedia((current) => {
      const remaining = maxMedia - current.length;

      return [
        ...current,
        ...files.slice(0, remaining),
      ];
    });

    event.target.value = "";
  }

  function removeMedia(index: number) {
    setMedia((current) =>
      current.filter((_, i) => i !== index)
    );
  }

  function close() {
    setContent("");
    setMedia([]);
    onOpenChange(false);
  }

  function handlePost() {
    createPost.mutate(
      {
        content,
        media,
      },
      {
        onSuccess: () => {
          close();
        },
      }
    );
  }

  return (
    <Dialog
      open={open}
      onOpenChange={onOpenChange}
      title="Create Post"
    >
      <div className="flex flex-col gap-4">
        <div className="flex gap-3">
          <Avatar
            imageUrl={user.avatarUrl}
            name={user.username}
            size="md"
          />

          <div className="min-w-0 flex-1">
            <textarea
              value={content}
              onChange={(event) =>
                setContent(
                  event.target.value.slice(0, maxLength)
                )
              }
              rows={6}
              placeholder="What's happening?"
              autoFocus
              className="
                w-full
                resize-none
                bg-transparent
                text-sm
                leading-6
                text-white
                outline-none
                placeholder:text-slate-500
              "
            />

            {previews.length > 0 && (
              <div className="mt-3 grid grid-cols-2 gap-2">
                {previews.map((preview, index) => (
                  <MediaPreview
                    key={preview.url}
                    preview={preview}
                    onRemove={() => removeMedia(index)}
                  />
                ))}
              </div>
            )}
          </div>
        </div>

        <input
          ref={inputRef}
          type="file"
          accept="image/*,video/*"
          multiple
          hidden
          onChange={handleMediaSelected}
        />

        <div className="flex items-center justify-between border-t border-slate-800 pt-4">
          <div className="flex items-center gap-2">
            <UploadButton
              onClick={() => inputRef.current?.click()}
            />

            {media.length > 0 && (
              <span className="text-xs text-slate-500">
                {media.length}/{maxMedia}
              </span>
            )}
          </div>

          <div className="flex items-center gap-4">
            <CharacterCounter
              current={content.length}
              max={maxLength}
            />

            <PostButton
              disabled={
                (!content.trim() && media.length === 0) ||
                createPost.isPending
              }
              onClick={handlePost}
            />
          </div>
        </div>
      </div>
    </Dialog>
  );
}