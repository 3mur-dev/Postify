package com.omar.postify.dto.admin;

import com.omar.postify.dto.enums.PostSortField;
import com.omar.postify.entities.enums.PostStatus;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;

public record AdminPostFilterRequest(

        Integer  page,
        Integer  size,

        String search,

        PostStatus status,
        Boolean reported,

        Long userId,

        LocalDateTime from,
        LocalDateTime to,

        PostSortField sortField,
        Sort.Direction direction

) {

    public int pageOrDefault() {
        return page != null ? page : 0;
    }

    public int sizeOrDefault() {
        return size != null ? size : 20;
    }

    public PostSortField sortFieldOrDefault() {
        return sortField != null ? sortField : PostSortField.CREATED_AT;
    }

    public Sort.Direction directionOrDefault() {
        return direction != null ? direction : Sort.Direction.DESC;
    }
}