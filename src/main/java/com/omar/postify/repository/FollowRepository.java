package com.omar.postify.repository;

import com.omar.postify.entities.Follow;
import com.omar.postify.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {

    boolean existsByFollowerAndFollowing(User follower, User following);

    Follow findByFollowerAndFollowing(User follower, User following);

    default void deleteByFollowerAndFollowing(User follower, User following) {
        Follow follow = findByFollowerAndFollowing(follower, following);
        if (follow != null) {
            delete(follow);
        }
    }

    long countByFollower(User follower);
    long countByFollowing(User following);
}
