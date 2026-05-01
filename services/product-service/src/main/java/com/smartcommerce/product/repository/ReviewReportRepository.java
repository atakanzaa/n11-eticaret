package com.smartcommerce.product.repository;

import com.smartcommerce.product.domain.ReportStatus;
import com.smartcommerce.product.domain.ReviewReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReviewReportRepository extends JpaRepository<ReviewReport, UUID> {
    boolean existsByReviewIdAndUserId(UUID reviewId, UUID userId);
    Page<ReviewReport> findByStatusOrderByCreatedAtAsc(ReportStatus status, Pageable pageable);
}
