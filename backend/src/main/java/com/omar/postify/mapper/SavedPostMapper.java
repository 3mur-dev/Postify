package com.omar.postify.mapper;

import com.omar.postify.dto.PostFeedDto;
import com.omar.postify.dto.SavedPostResponse;
import com.omar.postify.entities.SavedPost;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class SavedPostMapper {

    private final PostMapper postMapper;

    public SavedPostResponse toSavedPostResponse(
            SavedPost savedPost,
            boolean likedByCurrentUser
    ) {

        PostFeedDto postDto = postMapper.toPostFeedDto(
                savedPost.getPost(),
                likedByCurrentUser,
                true
        );

        return SavedPostResponse.builder()
                .savedAt(savedPost.getSavedAt())
                .post(postDto)
                .build();
    }
}
