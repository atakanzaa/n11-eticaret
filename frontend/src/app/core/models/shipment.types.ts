export type ShipmentStatus =
  | 'CREATED'
  | 'READY_FOR_PICKUP'
  | 'DISPATCHED'
  | 'IN_TRANSIT'
  | 'OUT_FOR_DELIVERY'
  | 'DELIVERED'
  | 'FAILED_DELIVERY'
  | 'RETURNED_TO_SENDER'
  | 'CANCELLED';

export interface ShipmentEventResponse {
  id: string;
  eventType: string;
  description?: string;
  location?: string;
  occurredAt: string;
}

export interface ShipmentResponse {
  id: string;
  orderId: string;
  sellerId: string;
  cargoProvider: string;
  trackingNumber?: string;
  trackingUrl?: string;
  recipientFullName: string;
  city: string;
  district: string;
  status: ShipmentStatus;
  estimatedDeliveryDate?: string;
  dispatchedAt?: string;
  deliveredAt?: string;
  createdAt: string;
}
