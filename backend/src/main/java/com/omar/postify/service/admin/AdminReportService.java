package com.omar.postify.service.admin;

import com.omar.postify.dto.admin.AdminReportDto;
import com.omar.postify.dto.admin.AdminReportFilterRequest;
import com.omar.postify.entities.Report;
import com.omar.postify.entities.enums.ReportStatus;
import com.omar.postify.exception.ResourceNotFoundException;
import com.omar.postify.mapper.AdminReportMapper;
import com.omar.postify.repository.ReportRepository;
import com.omar.postify.util.ReportSpecification;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AdminReportService {

    private final ReportRepository reportRepository;
    private final AdminReportMapper adminReportMapper;

    public Page<AdminReportDto> getReports(
            AdminReportFilterRequest request
    ) {

        return reportRepository.findAll(
                        ReportSpecification.withFilters(
                                request.search(),
                                request.status(),
                                request.reason()
                        ),
                        request.pageableOrDefault()
                )
                .map(adminReportMapper::toDto);
    }

    public AdminReportDto getReport(String publicId) {

        Report report = reportRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        return adminReportMapper.toDto(report);
    }

    @Transactional
    public AdminReportDto resolveReport(String publicId) {

        return reportRepository.findByPublicId(publicId)
                .map(report -> {
                            report.setStatus(ReportStatus.RESOLVED);
                            return adminReportMapper.toDto(reportRepository.save(report));
                        }
                ).orElseThrow(() -> new ResourceNotFoundException("Report not found"));

    }

    @Transactional
    public AdminReportDto rejectReport(String publicId) {

        return reportRepository.findByPublicId(publicId)
                .map(report -> {
                            report.setStatus(ReportStatus.REJECTED);
                            return adminReportMapper.toDto(reportRepository.save(report));
                        }
                ).orElseThrow(() -> new ResourceNotFoundException("Report not found"));

    }
}
