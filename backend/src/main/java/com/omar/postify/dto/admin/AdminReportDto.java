package com.omar.postify.dto.admin;

import com.omar.postify.entities.enums.ReportReason;
import com.omar.postify.entities.enums.ReportStatus;

import java.time.LocalDateTime;

public record AdminReportDto(

        String publicId,

        String postPublicId,

        String reporterUsername,

        ReportReason reason,

        ReportStatus status,

        LocalDateTime createdAt

) {}