package com.smartcommerce.returns.api.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectReturnRequest(@NotBlank String reason) {
}
