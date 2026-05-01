package com.smartcommerce.common.events;

public final class Topics {
    private Topics() {}

    // User & Auth
    public static final String USER_REGISTERED = "user.registered.v1";
    public static final String USER_PROFILE_UPDATED = "user.profile-updated.v1";
    public static final String USER_DELETED = "user.deleted.v1";

    // Seller
    public static final String SELLER_REGISTERED = "seller.registered.v1";
    public static final String SELLER_STATUS_CHANGED = "seller.status-changed.v1";

    // Product & Offer
    public static final String PRODUCT_CREATED = "product.created.v1";
    public static final String PRODUCT_UPDATED = "product.updated.v1";
    public static final String PRODUCT_DELETED = "product.deleted.v1";
    public static final String OFFER_CREATED = "offer.created.v1";
    public static final String OFFER_PRICE_CHANGED = "offer.price-changed.v1";
    public static final String OFFER_STOCK_CHANGED = "offer.stock-changed.v1";
    public static final String OFFER_STATUS_CHANGED = "offer.status-changed.v1";

    // Inventory
    public static final String INVENTORY_RESERVED = "inventory.reserved.v1";
    public static final String INVENTORY_RESERVATION_FAILED = "inventory.reservation-failed.v1";
    public static final String INVENTORY_RELEASED = "inventory.released.v1";
    public static final String INVENTORY_CONFIRMED = "inventory.confirmed.v1";
    public static final String INVENTORY_LOW_STOCK = "inventory.low-stock.v1";

    // Order
    public static final String ORDER_CREATED = "order.created.v1";
    public static final String ORDER_CONFIRMED = "order.confirmed.v1";
    public static final String ORDER_CANCELLED = "order.cancelled.v1";
    public static final String ORDER_EXPIRED = "order.expired.v1";
    public static final String ORDER_FRAUD_FLAGGED = "order.fraud-flagged.v1";

    // Payment
    public static final String PAYMENT_INITIATED = "payment.initiated.v1";
    public static final String PAYMENT_SUCCEEDED = "payment.succeeded.v1";
    public static final String PAYMENT_FAILED = "payment.failed.v1";
    public static final String PAYMENT_REFUNDED = "payment.refunded.v1";

    // Shipment
    public static final String SHIPMENT_CREATED = "shipment.created.v1";
    public static final String SHIPMENT_DISPATCHED = "shipment.dispatched.v1";
    public static final String SHIPMENT_DELIVERED = "shipment.delivered.v1";
    public static final String SHIPMENT_FAILED = "shipment.failed.v1";

    // Return
    public static final String RETURN_REQUESTED = "return.requested.v1";
    public static final String RETURN_APPROVED = "return.approved.v1";
    public static final String RETURN_REJECTED = "return.rejected.v1";
    public static final String RETURN_REFUNDED = "return.refunded.v1";
    public static final String RETURN_COMPLETED = "return.completed.v1";

    // Cart
    public static final String CART_ITEM_ADDED = "cart.item-added.v1";
    public static final String CART_CHECKOUT_STARTED = "cart.checkout-started.v1";
    public static final String CART_ABANDONED = "cart.abandoned.v1";

    // Review
    public static final String REVIEW_CREATED = "review.created.v1";
    public static final String REVIEW_UPDATED = "review.updated.v1";
    public static final String REVIEW_DELETED = "review.deleted.v1";
    public static final String REVIEW_APPROVED = "review.approved.v1";
    public static final String REVIEW_REJECTED = "review.rejected.v1";

    public static String dlqOf(String topic) {
        return topic + ".dlq";
    }
}
