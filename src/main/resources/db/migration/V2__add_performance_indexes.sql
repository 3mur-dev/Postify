CREATE INDEX idx_posts_user_id
    ON posts (user_id);

CREATE INDEX idx_posts_created_at
    ON posts (created_at);

CREATE INDEX idx_comments_post_id
    ON comments (post_id);

CREATE INDEX idx_comments_user_id
    ON comments (user_id);

CREATE INDEX idx_likes_post_id
    ON likes (post_id);

CREATE INDEX idx_likes_user_id
    ON likes (user_id);

CREATE INDEX idx_follows_follower_id
    ON follows (follower_id);

CREATE INDEX idx_follows_following_id
    ON follows (following_id);

CREATE INDEX idx_admin_logs_created_at
    ON admin_logs (created_at);
