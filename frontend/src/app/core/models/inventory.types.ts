export type ReservationStatus = 'ACTIVE' | 'CONFIRMED' | 'RELEASED' | 'EXPIRED';

export interface InventoryItemResponse {
  id: string;
  offerId: string;
  productId: string;
  sellerId: string;
  availableQuantity: number;
  reservedQuantity: number;
  soldQuantity: number;
  incomingQuantity: number;
  lowStockThreshold: number;
  allowBackorder: boolean;
  version: number;
}

export interface ReservationResponse {
  id: string;
  reservationKey: string;
  orderId: string;
  offerId: string;
  quantity: number;
  status: ReservationStatus;
  expiresAt: string;
}

export interface AdjustStockRequest {
  newAvailableQuantity: number;
  lowStockThreshold?: number;
}
