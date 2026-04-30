export type OrderStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'EXPIRED';

export interface OrderItemResponse {
  id: string;
  offerId: string;
  productId: string;
  sellerId: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
  productTitle: string;
  productImageUrl?: string;
  sellerName?: string;
}

export interface OrderResponse {
  id: string;
  orderNumber: string;
  userId: string;
  status: OrderStatus;
  grandTotal: number;
  currency: string;
  sagaState?: string;
  expiresAt?: string;
  items: OrderItemResponse[];
  createdAt?: string;
}

export interface CheckoutRequest {
  addressId: string;
  paymentMethod?: string;
  couponCode?: string;
}

export interface CheckoutResponse {
  orderId: string;
  orderNumber: string;
  status: string;
  grandTotal: number;
  expiresAt?: string;
}

export interface AdminOverviewResponse {
  gmv30d: number;
  ordersCount30d: number;
  averageBasket: number;
  activeSellers30d: number;
}
