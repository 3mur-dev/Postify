package com.omar.postify.dto.admin;

import com.omar.postify.entities.PostMedia;
import com.omar.postify.entities.enums.PostStatus;
import com.omar.postify.entities.enums.ReportReason;

import java.time.LocalDateTime;
import java.util.List;

public record AdminPostDto(

        String publicId,

        String userPublicId,
        String username,

        String content,
        List<PostMedia> media,

        PostStatus status,

        int likeCount,
        int commentCount,
        int reportCount,

        ReportReason lastReportReason,

        LocalDateTime createdAt,
        LocalDateTime updatedAt,

        boolean deleted

) {}
