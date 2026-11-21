package com.omar.postify.mapper;
import com.omar.postify.dto.FollowerResponse;
import com.omar.postify.entities.User;
import org.springframework.stereotype.Component;

@Component
public class FollowMapper {

    public FollowerResponse toFollowerResponse(
            User user,
            boolean followedByCurrentUser
    ) {
        return FollowerResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .avatarUrl(user.getAvatarUrl())
                .followedByCurrentUser(followedByCurrentUser)
                .build();
    }
}
