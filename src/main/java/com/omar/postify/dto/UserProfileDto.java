package com.omar.postify.dto;

import lombok.Data;

@Data
public class UserProfileDto {
    private String username;
    private String email;
    private String bio;
    private String avatarUrl;
    private int followersCount;
    private int followingCount;
}
