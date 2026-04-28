package com.smartcommerce.fraud.api;

import com.smartcommerce.common.errors.ErrorCode;
import com.smartcommerce.common.errors.ResourceNotFoundException;
import com.smartcommerce.fraud.api.dto.CreateBlacklistRequest;
import com.smartcommerce.fraud.api.dto.FraudCheckRequest;
import com.smartcommerce.fraud.api.dto.FraudCheckResult;
import com.smartcommerce.fraud.domain.FraudBlacklist;
import com.smartcommerce.fraud.domain.FraudCheck;
import com.smartcommerce.fraud.repository.FraudCheckRepository;
import com.smartcommerce.fraud.service.BlacklistService;
import com.smartcommerce.fraud.service.FraudDetectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/fraud")
@RequiredArgsConstructor
public class FraudController {

    private final FraudDetectionService fraudDetectionService;
    private final BlacklistService blacklistService;
    private final FraudCheckRepository fraudCheckRepository;

    @PostMapping("/check")
    public FraudCheckResult check(@Valid @RequestBody FraudCheckRequest request) {
        return fraudDetectionService.check(request);
    }

    @GetMapping("/checks/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    public FraudCheck getCheck(@PathVariable UUID orderId) {
        return fraudCheckRepository.findByOrderId(orderId)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Fraud check not found"));
    }

    @GetMapping("/checks")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<FraudCheck> listChecks(@RequestParam(required = false) String decision,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size);
        if (decision != null) {
            return fraudCheckRepository.findByDecision(decision, pageable);
        }
        return fraudCheckRepository.findAll(pageable);
    }

    @PostMapping("/blacklist")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public FraudBlacklist addBlacklist(@Valid @RequestBody CreateBlacklistRequest request) {
        return blacklistService.add(request);
    }

    @DeleteMapping("/blacklist/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeBlacklist(@PathVariable UUID id) {
        blacklistService.remove(id);
    }

    @GetMapping("/blacklist")
    @PreAuthorize("hasRole('ADMIN')")
    public List<FraudBlacklist> listBlacklist() {
        return blacklistService.listAll();
    }
}
