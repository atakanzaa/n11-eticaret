export interface CartItemDto {
  id: string;
  offerId: string;
  productId: string;
  sellerId: string;
  quantity: number;
  unitPriceSnapshot: number;
  currencySnapshot: string;
  productTitleSnapshot: string;
  productImageSnapshot?: string;
  sellerNameSnapshot?: string;
  cargoPriceSnapshot: number;
  subtotal: number;
}

export interface CartResponse {
  id: string;
  userId: string;
  status: 'ACTIVE' | 'CHECKED_OUT' | 'EXPIRED';
  items: CartItemDto[];
  total: number;
  itemCount: number;
}

export interface AddItemRequest {
  offerId: string;
  quantity: number;
}

export interface CartValidationIssue {
  offerId: string;
  code: string;
  message: string;
}

export interface CartValidationResponse {
  cart: CartResponse;
  issues: CartValidationIssue[];
}
