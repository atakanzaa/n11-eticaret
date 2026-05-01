export type ReviewStatus = 'APPROVED' | 'PENDING' | 'REJECTED';

export type VoteType = 'HELPFUL' | 'UNHELPFUL';

export interface ReviewResponse {
  id: string;
  productId: string;
  userId: string;
  userDisplayName: string;
  rating: number;
  title?: string;
  comment?: string;
  variantInfo?: string;
  helpfulCount: number;
  unhelpfulCount: number;
  verifiedPurchase: boolean;
  imageUrls?: string[];
  sellerReply?: ReviewReplyResponse;
  status: ReviewStatus;
  createdAt: string;
  updatedAt?: string;
}

export interface CreateReviewRequest {
  rating: number;
  title?: string;
  comment?: string;
  variantInfo?: string;
  orderId?: string;
  imageUrls?: string[];
}

export interface UpdateReviewRequest {
  rating?: number;
  title?: string;
  comment?: string;
}

export interface ReviewStatsResponse {
  averageRating: number;
  totalCount: number;
  distribution: Record<number, number>;
}

export interface ReviewReplyResponse {
  id: string;
  reviewId: string;
  sellerId: string;
  content: string;
  createdAt: string;
  updatedAt?: string;
}

export interface CreateReviewReplyRequest {
  content: string;
}

export interface UpdateReviewReplyRequest {
  content: string;
}

export interface CreateReviewReportRequest {
  reason: string;
  description?: string;
}

export interface ReviewReportResponse {
  id: string;
  reviewId: string;
  userId: string;
  reason: string;
  description?: string;
  status: string;
  createdAt: string;
}
