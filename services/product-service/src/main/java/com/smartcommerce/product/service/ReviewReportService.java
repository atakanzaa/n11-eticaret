package com.smartcommerce.product.service;

import com.smartcommerce.common.errors.ResourceNotFoundException;
import com.smartcommerce.common.errors.ErrorCode;
import com.smartcommerce.product.api.dto.ReviewReportResponse;
import com.smartcommerce.product.api.mapper.ReviewMapper;
import com.smartcommerce.product.domain.ReportStatus;
import com.smartcommerce.product.repository.ReviewReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewReportService {
    private final ReviewReportRepository reportRepository;
    private final ReviewMapper reviewMapper;

    @Transactional(readOnly = true)
    public Page<ReviewReportResponse> listPending(Pageable pageable) {
        return reportRepository.findByStatusOrderByCreatedAtAsc(ReportStatus.PENDING, pageable)
            .map(reviewMapper::toReportResponse);
    }

    @Transactional
    public ReviewReportResponse dismiss(UUID reportId) {
        var report = reportRepository.findById(reportId)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Report not found: " + reportId));
        report.setStatus(ReportStatus.DISMISSED);
        return reviewMapper.toReportResponse(reportRepository.save(report));
    }
}
