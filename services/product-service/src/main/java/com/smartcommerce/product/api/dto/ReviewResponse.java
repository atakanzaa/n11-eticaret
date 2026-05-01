package com.smartcommerce.product.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReviewResponse(
    UUID id,
    UUID productId,
    String userDisplayName,
    int rating,
    String title,
    String comment,
    String variantInfo,
    int helpfulCount,
    int unhelpfulCount,
    boolean verifiedPurchase,
    List<String> imageUrls,
    ReviewReplyResponse sellerReply,
    Instant createdAt,
    Instant updatedAt
) {}
