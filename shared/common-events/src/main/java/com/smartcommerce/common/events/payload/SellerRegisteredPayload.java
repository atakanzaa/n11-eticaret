package com.smartcommerce.common.events.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerRegisteredPayload {
    private UUID sellerId;
    private UUID userId;
    private String storeName;
}
