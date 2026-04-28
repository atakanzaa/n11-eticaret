package com.smartcommerce.payment.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CardDto(
    @NotBlank String holderName,
    @NotBlank @Pattern(regexp = "\\d{12,19}") String number,
    @NotBlank @Pattern(regexp = "\\d{2}") String expireMonth,
    @NotBlank @Pattern(regexp = "\\d{4}") String expireYear,
    @NotBlank @Pattern(regexp = "\\d{3,4}") String cvc
) {
}
