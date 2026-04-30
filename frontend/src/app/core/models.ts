/**
 * Barrel re-export. New code should import from `core/models/<domain>.types`
 * directly; this file exists so older imports keep working until they're cut over.
 *
 * IMPORTANT: types here mirror the backend DTOs one-to-one. Mock data, view-models,
 * and computed UI helpers do NOT belong here — they live next to the components
 * that own them.
 */
export * from './models/common.types';
export * from './models/auth.types';
export * from './models/user.types';
export * from './models/seller.types';
export * from './models/category.types';
export * from './models/brand.types';
export * from './models/product.types';
export * from './models/offer.types';
export * from './models/inventory.types';
export * from './models/cart.types';
export * from './models/order.types';
export * from './models/payment.types';
export * from './models/shipment.types';
export * from './models/return.types';
export * from './models/coupon.types';
export * from './models/recommendation.types';
export * from './models/notification.types';
export * from './models/fraud.types';
export * from './models/review.types';
export * from './models/ai.types';
