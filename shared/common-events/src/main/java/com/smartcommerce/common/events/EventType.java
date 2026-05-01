package com.smartcommerce.common.events;

public final class EventType {
    private EventType() {}

    // User & Auth
    public static final String USER_REGISTERED = "USER_REGISTERED";
    public static final String USER_PROFILE_UPDATED = "USER_PROFILE_UPDATED";
    public static final String USER_DELETED = "USER_DELETED";

    // Seller
    public static final String SELLER_REGISTERED = "SELLER_REGISTERED";
    public static final String SELLER_STATUS_CHANGED = "SELLER_STATUS_CHANGED";

    // Product & Offer
    public static final String PRODUCT_CREATED = "PRODUCT_CREATED";
    public static final String PRODUCT_UPDATED = "PRODUCT_UPDATED";
    public static final String PRODUCT_DELETED = "PRODUCT_DELETED";
    public static final String OFFER_CREATED = "OFFER_CREATED";
    public static final String OFFER_PRICE_CHANGED = "OFFER_PRICE_CHANGED";
    public static final String OFFER_STOCK_CHANGED = "OFFER_STOCK_CHANGED";
    public static final String OFFER_STATUS_CHANGED = "OFFER_STATUS_CHANGED";

    // Inventory
    public static final String INVENTORY_RESERVED = "INVENTORY_RESERVED";
    public static final String INVENTORY_RESERVATION_FAILED = "INVENTORY_RESERVATION_FAILED";
    public static final String INVENTORY_RELEASED = "INVENTORY_RELEASED";
    public static final String INVENTORY_CONFIRMED = "INVENTORY_CONFIRMED";
    public static final String INVENTORY_LOW_STOCK = "INVENTORY_LOW_STOCK";

    // Order
    public static final String ORDER_CREATED = "ORDER_CREATED";
    public static final String ORDER_CONFIRMED = "ORDER_CONFIRMED";
    public static final String ORDER_CANCELLED = "ORDER_CANCELLED";
    public static final String ORDER_EXPIRED = "ORDER_EXPIRED";
    public static final String ORDER_FRAUD_FLAGGED = "ORDER_FRAUD_FLAGGED";

    // Payment
    public static final String PAYMENT_INITIATED = "PAYMENT_INITIATED";
    public static final String PAYMENT_SUCCEEDED = "PAYMENT_SUCCEEDED";
    public static final String PAYMENT_FAILED = "PAYMENT_FAILED";
    public static final String PAYMENT_REFUNDED = "PAYMENT_REFUNDED";

    // Shipment
    public static final String SHIPMENT_CREATED = "SHIPMENT_CREATED";
    public static final String SHIPMENT_DISPATCHED = "SHIPMENT_DISPATCHED";
    public static final String SHIPMENT_DELIVERED = "SHIPMENT_DELIVERED";
    public static final String SHIPMENT_FAILED = "SHIPMENT_FAILED";

    // Return
    public static final String RETURN_REQUESTED = "RETURN_REQUESTED";
    public static final String RETURN_APPROVED = "RETURN_APPROVED";
    public static final String RETURN_REJECTED = "RETURN_REJECTED";
    public static final String RETURN_REFUNDED = "RETURN_REFUNDED";
    public static final String RETURN_COMPLETED = "RETURN_COMPLETED";

    // Cart
    public static final String CART_ITEM_ADDED = "CART_ITEM_ADDED";
    public static final String CART_CHECKOUT_STARTED = "CART_CHECKOUT_STARTED";
    public static final String CART_ABANDONED = "CART_ABANDONED";

    // Review
    public static final String REVIEW_CREATED = "REVIEW_CREATED";
    public static final String REVIEW_UPDATED = "REVIEW_UPDATED";
    public static final String REVIEW_DELETED = "REVIEW_DELETED";
    public static final String REVIEW_APPROVED = "REVIEW_APPROVED";
    public static final String REVIEW_REJECTED = "REVIEW_REJECTED";
}
