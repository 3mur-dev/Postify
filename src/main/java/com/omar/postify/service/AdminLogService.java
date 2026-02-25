package com.omar.postify.service;

import com.omar.postify.entities.AdminLog;
import com.omar.postify.repository.AdminLogRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;


@Service
@AllArgsConstructor
public class AdminLogService {

    private final AdminLogRepository logRepository;

    public void logAction(String adminUsername, String action, String target) {
        AdminLog log = new AdminLog();
        log.setAdminUsername(adminUsername);
        log.setAction(action);
        log.setTarget(target);
        logRepository.save(log);
    }
    public Page<AdminLog> getLogs(String keyword, int page) {

        Pageable pageable = PageRequest.of(page, 10, Sort.by("createdAt").descending());

        if (keyword == null || keyword.isEmpty()) {
            return logRepository.findAll(pageable);
        }

        return logRepository
                .findByAdminUsernameContainingIgnoreCaseOrActionContainingIgnoreCaseOrTargetContainingIgnoreCase(
                        keyword, keyword, keyword, pageable);
    }
}