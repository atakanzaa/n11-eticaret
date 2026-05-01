export type OfferStatus = 'ACTIVE' | 'PAUSED' | 'OUT_OF_STOCK' | 'REJECTED';

export type CargoProvider = 'DEFAULT' | 'YURTICI' | 'ARAS' | 'MNG' | 'PTT' | 'UPS' | 'DHL';

export interface OfferResponse {
  id: string;
  productId: string;
  sellerId: string;
  sku: string;
  price: number;
  currency: string;
  listPrice?: number;
  cargoProvider: CargoProvider;
  cargoPrice: number;
  estimatedDeliveryDays: number;
  freeShippingThreshold?: number;
  status: OfferStatus;
  version: number;
}

export interface CreateOfferRequest {
  productId: string;
  sku: string;
  price: number;
  listPrice?: number;
  cargoProvider: CargoProvider;
  cargoPrice: number;
  estimatedDeliveryDays: number;
  freeShippingThreshold?: number;
  initialStock: number;
}

export interface UpdateOfferRequest {
  price?: number;
  listPrice?: number;
  cargoProvider?: CargoProvider;
  cargoPrice?: number;
  estimatedDeliveryDays?: number;
  freeShippingThreshold?: number;
  status?: OfferStatus;
}
