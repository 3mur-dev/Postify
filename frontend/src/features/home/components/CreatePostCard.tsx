import { ImagePlus } from "lucide-react";
import { useState } from "react";
import Avatar from "../components/Avatar";
import { CreatePostModal } from "./CreatePostModal";
import type { ProfileResponse } from "../../../api/profileApi";


interface CreatePostCardProps {
  user: ProfileResponse;
}

export default function CreatePostCard({ user }: CreatePostCardProps) {
  const [open, setOpen] = useState(false);

  
  return (
    <>
      <section className="mb-6 rounded-2xl border border-slate-800 bg-slate-900">
        <div className="p-3 sm:p-4">
          <div
            onClick={() => setOpen(true)}
            className="flex cursor-text items-center gap-3"
          >
            <Avatar
              imageUrl={user.avatarUrl}
              name={user.username}
            />

            <button
              type="button"
              className="flex h-11 min-w-0 flex-1 items-center rounded-full bg-slate-800 px-4 text-left text-sm text-slate-400 transition hover:bg-slate-700"
            >
              What's happening?
            </button>
          </div>

          <div className="mt-4 border-t border-slate-800 pt-4">
            <button
              type="button"
              onClick={() => setOpen(true)}
              className="flex items-center gap-2 rounded-full px-3 py-2 text-sm text-sky-400 transition hover:bg-slate-800"
            >
              <ImagePlus className="h-5 w-5" />
              Add photo
            </button>
          </div>
        </div>
      </section>

      <CreatePostModal open={open} onOpenChange={setOpen} user={user} />
    </>
  );
}