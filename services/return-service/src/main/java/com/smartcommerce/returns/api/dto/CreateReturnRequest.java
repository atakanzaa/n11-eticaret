package com.smartcommerce.returns.api.dto;

import com.smartcommerce.returns.domain.ReturnReason;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateReturnRequest(
    @NotNull UUID orderId,
    @NotNull ReturnReason reasonCode,
    String reasonDescription,
    @NotEmpty @Valid List<ReturnItemRequest> items
) {
    public record ReturnItemRequest(
        @NotNull UUID offerId,
        @Min(1) int quantity
    ) {}
}
