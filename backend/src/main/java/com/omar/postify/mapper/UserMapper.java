package com.omar.postify.mapper;

import com.omar.postify.dto.UserSearchDto;
import com.omar.postify.entities.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserSearchDto toUserSearchDto(User user) {
        return new UserSearchDto(
                user.getId(),
                user.getPublicId(),
                user.getUsername(),
                user.getAvatarUrl(),
                user.getBio()
        );
    }
}