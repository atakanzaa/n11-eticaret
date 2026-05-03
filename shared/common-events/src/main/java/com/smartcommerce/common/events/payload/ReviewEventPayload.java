package com.smartcommerce.common.events.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewEventPayload {
    private UUID reviewId;
    private UUID productId;
    private UUID userId;
    private UUID orderId;
    private int rating;
    private String title;
    private String status;
    private String rejectionReason;
    private boolean verifiedPurchase;
    private Instant occurredAt;
    /** Optional previous rating, populated for UPDATE/DELETE events so seller-rating
     *  consumers can apply differential aggregation without re-querying. */
    private Integer previousRating;
}
