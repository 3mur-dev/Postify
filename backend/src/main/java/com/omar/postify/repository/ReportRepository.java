package com.omar.postify.repository;

import com.omar.postify.entities.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

@Repository
public interface ReportRepository extends
        JpaRepository<Report, Long>,
        JpaSpecificationExecutor<Report> {

    Optional<Report> findByPublicId(String publicId);

}
