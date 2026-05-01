package com.smartcommerce.product.api;

import com.smartcommerce.product.api.dto.ModerationActionRequest;
import com.smartcommerce.product.api.dto.ReviewReportResponse;
import com.smartcommerce.product.api.dto.ReviewResponse;
import com.smartcommerce.product.service.ReviewReportService;
import com.smartcommerce.product.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/reviews")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ReviewAdminController {
    private final ReviewService reviewService;
    private final ReviewReportService reviewReportService;

    @GetMapping("/pending")
    public Page<ReviewResponse> listPending(Pageable pageable) {
        return reviewService.listPending(pageable);
    }

    @PostMapping("/{id}/approve")
    public ReviewResponse approve(@PathVariable UUID id,
                                  @AuthenticationPrincipal String userId) {
        return reviewService.approve(id, UUID.fromString(userId));
    }

    @PostMapping("/{id}/reject")
    public ReviewResponse reject(@PathVariable UUID id,
                                 @AuthenticationPrincipal String userId,
                                 @Valid @RequestBody ModerationActionRequest request) {
        return reviewService.reject(id, UUID.fromString(userId), request.rejectionReason());
    }

    @GetMapping("/reports")
    public Page<ReviewReportResponse> listReports(Pageable pageable) {
        return reviewReportService.listPending(pageable);
    }

    @PostMapping("/reports/{id}/dismiss")
    public ReviewReportResponse dismissReport(@PathVariable UUID id) {
        return reviewReportService.dismiss(id);
    }
}
