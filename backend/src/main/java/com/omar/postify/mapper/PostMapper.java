package com.omar.postify.mapper;

import com.omar.postify.dto.PostFeedBaseDto;
import com.omar.postify.dto.PostFeedDto;
import com.omar.postify.dto.PostMediaDto;
import com.omar.postify.dto.PostMediaProjection;
import com.omar.postify.entities.Post;
import com.omar.postify.entities.PostMedia;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PostMapper {

    public PostFeedDto toPostFeedDto(
            Post post,
            boolean likedByCurrentUser,
            boolean savedByCurrentUser
    ) {
        List<PostMediaDto> media = post.getMedia()
                .stream()
                .map(this::toMediaDto)
                .toList();

        return new PostFeedDto(
                post.getId(),
                post.getPublicId(),
                post.getContent(),
                media,
                post.getCreatedAt(),
                post.getUser().getUsername(),
                post.getUser().getAvatarUrl(),
                post.getLikeCount(),
                post.getCommentCount(),
                likedByCurrentUser,
                savedByCurrentUser
        );
    }

    public PostFeedDto toPostFeedDto(
            PostFeedBaseDto post,
            List<PostMediaDto> media,
            long likeCount,
            long commentCount,
            boolean likedByCurrentUser,
            boolean savedByCurrentUser
    ) {
        return new PostFeedDto(
                post.id(),
                post.publicId(),
                post.content(),
                media,
                post.createdAt(),
                post.username(),
                post.avatarUrl(),
                likeCount,
                commentCount,
                likedByCurrentUser,
                savedByCurrentUser
        );
    }

    private PostMediaDto toMediaDto(PostMedia media) {
        return new PostMediaDto(
                media.getId(),
                "/api/post-media/" + media.getId(),
                media.getType(),
                media.getPosition(),
                media.getWidth(),
                media.getHeight(),
                media.getDurationMs()
        );
    }
}