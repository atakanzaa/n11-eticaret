export type PaymentStatus =
  | 'INITIATED'
  | 'PENDING'
  | 'THREEDS_PENDING'
  | 'SUCCEEDED'
  | 'FAILED'
  | 'REFUNDED';

export interface CardDto {
  holderName: string;
  number: string;
  expireMonth: string;
  expireYear: string;
  cvc: string;
}

export interface InitiatePaymentRequest {
  orderId: string;
  card: CardDto;
  installment?: number;
  userIp?: string;
}

export interface InitiatePaymentResponse {
  paymentId: string;
  threeDsHtmlContent?: string;
  status: PaymentStatus;
}

export interface PaymentResponse {
  id: string;
  orderId: string;
  userId: string;
  amount: number;
  paidAmount?: number;
  refundedAmount?: number;
  currency: string;
  installment: number;
  provider: string;
  status: PaymentStatus;
  cardLastFour?: string;
  cardBrand?: string;
  initiatedAt?: string;
  succeededAt?: string;
  failedAt?: string;
  failureCode?: string;
  failureMessage?: string;
}

export interface RefundRequest {
  amount: number;
  reason: string;
}

export interface RefundResponse {
  id: string;
  amount: number;
  status: string;
}
