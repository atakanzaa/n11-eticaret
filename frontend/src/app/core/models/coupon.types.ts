export type DiscountType = 'PERCENTAGE' | 'FIXED_AMOUNT' | 'FREE_SHIPPING';

export interface ValidateCouponRequest {
  code: string;
  userId: string;
  cartTotal: number;
  shippingCost: number;
  productIds?: string[];
  categoryIds?: string[];
  sellerIds?: string[];
  firstOrder?: boolean;
}

export interface CouponValidationResponse {
  valid: boolean;
  code: string;
  discountType?: DiscountType;
  discountAmount: number;
  errorCode?: string;
  errorMessage?: string;
}

export interface CouponResponse {
  id: string;
  code: string;
  name: string;
  description?: string;
  discountType: DiscountType;
  discountValue: number;
  maxDiscountAmount?: number;
  minimumOrderAmount?: number;
  totalUsageLimit?: number;
  perUserLimit: number;
  timesUsed: number;
  validFrom: string;
  validUntil: string;
  active: boolean;
  firstOrderOnly: boolean;
  stackable: boolean;
}

export interface CreateCouponRequest {
  code: string;
  name: string;
  description?: string;
  discountType: DiscountType;
  discountValue: number;
  maxDiscountAmount?: number;
  minimumOrderAmount?: number;
  totalUsageLimit?: number;
  perUserLimit?: number;
  validFrom: string;
  validUntil: string;
  firstOrderOnly?: boolean;
  stackable?: boolean;
}
