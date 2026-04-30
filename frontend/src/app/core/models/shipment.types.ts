export type ShipmentStatus = 'PENDING' | 'DISPATCHED' | 'IN_TRANSIT' | 'DELIVERED' | 'FAILED';

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
