import FeedPost from "./FeedPost";
import type { PostFeedDto } from "../../api/feedApi";
import { useState } from "react";
import CommentsModal from "./CommentsModal";

interface Props {
  posts: PostFeedDto[];
}

export default function HomeFeed({ posts }: Props) {
  const [commentsPublicId, setCommentsPublicId] = useState<string | null>(null);
  
  return (
<>
  <section className="space-y-6">
    {posts.map((post) => (
      <FeedPost
        key={post.publicId}
        post={post}
        commentsPublicId={commentsPublicId}
        setCommentsPublicId={setCommentsPublicId}
        source="FEED"
      />
    ))}
  </section>

  {commentsPublicId !== null && (
    <CommentsModal
      publicId={commentsPublicId}
      onClose={() => setCommentsPublicId(null)}
      source="FEED"
    />
  )}
</>

    
  );
}