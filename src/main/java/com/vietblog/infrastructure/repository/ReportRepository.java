package com.vietblog.infrastructure.repository;

import com.vietblog.domain.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, String> {
    Page<Report> findByStatus(Report.ReportStatus status, Pageable pageable);
}
