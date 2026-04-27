package com.smartcommerce.inventory.domain;

import com.smartcommerce.common.errors.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "offer_id", nullable = false, unique = true)
    private UUID offerId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity;

    @Column(name = "sold_quantity", nullable = false)
    private int soldQuantity;

    @Column(name = "incoming_quantity", nullable = false)
    private int incomingQuantity;

    @Column(name = "low_stock_threshold", nullable = false)
    private int lowStockThreshold;

    @Column(name = "allow_backorder", nullable = false)
    private boolean allowBackorder;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Version
    private Long version;

    public boolean canReserve(int quantity) {
        return allowBackorder || availableQuantity >= quantity;
    }

    public void reserve(int quantity) {
        if (!canReserve(quantity)) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK,
                "Insufficient stock: available=" + availableQuantity + ", requested=" + quantity);
        }
        availableQuantity -= quantity;
        reservedQuantity += quantity;
    }

    public void release(int quantity) {
        reservedQuantity = Math.max(0, reservedQuantity - quantity);
        availableQuantity += quantity;
    }

    public void confirm(int quantity) {
        reservedQuantity = Math.max(0, reservedQuantity - quantity);
        soldQuantity += quantity;
    }

    public boolean isLowStock() {
        return availableQuantity <= lowStockThreshold;
    }
}
