package com.omar.postify.repository;

import com.omar.postify.entities.AdminLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminLogRepository extends JpaRepository<AdminLog, Long> {

    Page<AdminLog> findByAdminUsernameContainingIgnoreCaseOrActionContainingIgnoreCaseOrTargetContainingIgnoreCase(
            String admin,
            String action,
            String target,
            Pageable pageable
    );
}