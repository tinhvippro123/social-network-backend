package com.vietblog.infrastructure.repository;

import com.vietblog.domain.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, String> {
    Page<Report> findByStatus(Report.ReportStatus status, Pageable pageable);
    long countByStatus(Report.ReportStatus status);
    java.util.List<Report> findTop5ByOrderByCreatedAtDesc();
    boolean existsByReporterIdAndTargetIdAndTypeAndStatus(String reporterId, String targetId,
                                                        Report.TargetType type, Report.ReportStatus status);

    @Query("SELECT r.targetId, COUNT(r) FROM Report r WHERE r.type = :type AND r.targetId IN :targetIds GROUP BY r.targetId")
    java.util.List<Object[]> countByTargetIds(@Param("type") Report.TargetType type, @Param("targetIds") java.util.Collection<String> targetIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Report r where r.id = :id")
    Optional<Report> findLockedById(@Param("id") String id);
}
