export interface ReviewResponse {
  id: string;
  productId: string;
  userDisplayName: string;
  rating: number;
  title?: string;
  comment?: string;
  variantInfo?: string;
  helpfulCount: number;
  createdAt: string;
}

export interface CreateReviewRequest {
  rating: number;
  title?: string;
  comment?: string;
  variantInfo?: string;
  orderId?: string;
}
