package com.omar.postify.mapper;

import com.omar.postify.dto.PostMediaDto;
import com.omar.postify.dto.PostMediaProjection;
import com.omar.postify.entities.PostMedia;
import org.springframework.stereotype.Component;

@Component
public class PostMediaMapper {

    public PostMediaDto toDto(PostMediaProjection media) {
        return new PostMediaDto(
                media.id(),
                media.url(),
                media.type(),
                media.position(),
                media.width(),
                media.height(),
                media.durationMs()
        );
    }
}
