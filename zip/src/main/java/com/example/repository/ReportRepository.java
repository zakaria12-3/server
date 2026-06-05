package com.example.repository;

import com.example.model.Report;
import com.example.model.ReportStatus;
import com.example.model.ReportTargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findAllByOrderByCreatedAtDesc();
    List<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status);
    long countByTargetTypeAndTargetId(ReportTargetType targetType, Long targetId);
    long countByStatus(ReportStatus status);
}
