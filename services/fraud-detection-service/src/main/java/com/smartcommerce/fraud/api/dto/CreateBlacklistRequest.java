package com.smartcommerce.fraud.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateBlacklistRequest(
    @NotBlank @Pattern(regexp = "USER_ID|EMAIL|IP|CARD_BIN") String blacklistType,
    @NotBlank String value,
    String reason
) {
}
