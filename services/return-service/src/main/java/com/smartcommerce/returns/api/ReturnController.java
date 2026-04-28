package com.smartcommerce.returns.api;

import com.smartcommerce.returns.api.dto.CreateReturnRequest;
import com.smartcommerce.returns.api.dto.RejectReturnRequest;
import com.smartcommerce.returns.api.dto.ReturnResponse;
import com.smartcommerce.returns.service.ReturnOrchestrator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/returns")
@RequiredArgsConstructor
public class ReturnController {

    private final ReturnOrchestrator orchestrator;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReturnResponse requestReturn(@AuthenticationPrincipal String userId,
                                        @Valid @RequestBody CreateReturnRequest request) {
        return orchestrator.requestReturn(UUID.fromString(userId), request);
    }

    @GetMapping("/me")
    public List<ReturnResponse> myReturns(@AuthenticationPrincipal String userId) {
        return orchestrator.listForUser(UUID.fromString(userId));
    }

    @GetMapping("/{id}")
    public ReturnResponse getReturn(@PathVariable UUID id) {
        return orchestrator.get(id);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPPORT')")
    public ReturnResponse approve(@PathVariable UUID id) {
        return orchestrator.approve(id);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPPORT')")
    public ReturnResponse reject(@PathVariable UUID id, @Valid @RequestBody RejectReturnRequest request) {
        return orchestrator.reject(id, request.reason());
    }

    @PostMapping("/{id}/cancel")
    public ReturnResponse cancel(@AuthenticationPrincipal String userId, @PathVariable UUID id) {
        return orchestrator.cancel(id, UUID.fromString(userId));
    }
}
