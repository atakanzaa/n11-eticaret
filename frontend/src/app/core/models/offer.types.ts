export type OfferStatus = 'ACTIVE' | 'PAUSED' | 'INACTIVE' | 'DELISTED';

export type CargoProvider = 'ARAS' | 'YURTICI' | 'MNG' | 'PTT' | 'DEFAULT';

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
