package com.omar.postify.repository;

import com.omar.postify.entities.Post;
import com.omar.postify.entities.PostTopic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface PostTopicRepository extends JpaRepository<PostTopic, Long> {

    List<PostTopic> findAllByPost(Post post);

    List<PostTopic> findByTopicIdIn(Set topicId);

    List<PostTopic> findByPostId(Long postId);

    @Query("""
        SELECT p
        FROM Post p
        WHERE NOT EXISTS (
            SELECT 1
            FROM PostTopic pt
            WHERE pt.post = p
        )
        """)
    List<Post> findPostsWithoutTopics();
}
