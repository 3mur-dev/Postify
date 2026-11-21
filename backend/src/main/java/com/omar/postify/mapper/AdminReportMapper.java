package com.omar.postify.mapper;

import com.omar.postify.dto.admin.AdminReportDto;
import com.omar.postify.entities.Report;
import org.springframework.stereotype.Component;

@Component
public class AdminReportMapper {

    public AdminReportDto toDto(Report report) {

        return new AdminReportDto(
                report.getPublicId(),
                report.getPost().getPublicId(),
                report.getReporter().getUsername(),
                report.getReason(),
                report.getStatus(),
                report.getCreatedAt()
        );
    }
}