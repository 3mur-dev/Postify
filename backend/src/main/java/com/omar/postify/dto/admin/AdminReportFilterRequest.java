package com.omar.postify.dto.admin;

import com.omar.postify.entities.enums.ReportReason;
import com.omar.postify.entities.enums.ReportStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public record AdminReportFilterRequest(

        Integer page,
        Integer size,
        String search,
        ReportStatus status,
        ReportReason reason

) {

    public int pageOrDefault() {
        return page != null ? page : 0;
    }

    public int sizeOrDefault() {
        return size != null ? size : 20;
    }

    public Pageable pageableOrDefault() {
        return PageRequest.of(
                pageOrDefault(),
                sizeOrDefault(),
                Sort.by("createdAt").descending()
        );
    }
}