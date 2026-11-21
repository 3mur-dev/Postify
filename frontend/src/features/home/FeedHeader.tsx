import { CalendarDays, BadgeCheck } from "lucide-react";
import type { PostFeedDto, InteractionSource } from "../../api/types";
import Avatar from "./components/Avatar";
import PostMenu from "./PostMenu";
import { trackInteraction } from "../../api/interactionApi";

interface FeedHeaderProps {
  post: PostFeedDto;
  formattedDate: string;
  source: InteractionSource;
}

export default function FeedHeader({ post, formattedDate, source }: FeedHeaderProps) {
  return (
    <div className="flex items-start justify-between px-3 pt-3">
      <div className="flex items-start gap-2.5">
        
        <button
          type="button"
          className="flex-shrink-0 rounded-full transition hover:opacity-80 cursor-pointer"
          onClick={() => {
                  void trackInteraction({
                    postPublicId: post.publicId,
                    eventType: "PROFILE_OPEN",
                    source
                  })
            window.location.href = `/profile/${post.username}`;
          }}
        >
        <Avatar imageUrl={post.avatarUrl} name={post.username} size="md" />
      </button>

        <div>
          <div className="flex items-center gap-2">
            <h3 className="text-[13px] font-semibold text-white">{post.username}</h3>
            <BadgeCheck className="h-3.5 w-3.5 text-sky-400" />
          </div>
          

          <div className="mt-0.5 flex items-center gap-1 text-[11px] text-slate-400">
            <CalendarDays className="h-3 w-3" />
            {formattedDate}
          </div>
        </div>
      </div>

      <PostMenu />
    </div>
  );
}
