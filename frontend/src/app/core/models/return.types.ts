export type ReturnStatus =
  | 'REQUESTED'
  | 'APPROVED'
  | 'REJECTED'
  | 'REFUND_PROCESSING'
  | 'REFUND_FAILED'
  | 'REFUNDED'
  | 'INVENTORY_RESTOCKED'
  | 'COMPLETED'
  | 'CANCELLED';

export type ReturnReasonCode =
  | 'BUYER_REQUEST'
  | 'DAMAGED'
  | 'WRONG_ITEM'
  | 'NOT_AS_DESCRIBED'
  | 'DEFECTIVE'
  | 'OTHER';

export interface ReturnItemRequest {
  offerId: string;
  quantity: number;
}

export interface CreateReturnRequest {
  orderId: string;
  reasonCode: ReturnReasonCode;
  reasonDescription?: string;
  items: ReturnItemRequest[];
}

export interface ReturnItemResponse {
  id: string;
  offerId: string;
  productId: string;
  sellerId: string;
  quantity: number;
  unitPrice: number;
  refundAmount: number;
  itemStatus: string;
}

export interface ReturnResponse {
  id: string;
  returnNumber: string;
  orderId: string;
  userId: string;
  paymentId?: string;
  reasonCode: ReturnReasonCode;
  reasonDescription?: string;
  status: ReturnStatus;
  sagaState?: string;
  refundAmount?: number;
  refundId?: string;
  rejectionReason?: string;
  requestedAt: string;
  approvedAt?: string;
  rejectedAt?: string;
  refundedAt?: string;
  completedAt?: string;
  items: ReturnItemResponse[];
}

export interface RejectReturnRequest {
  reason: string;
}
