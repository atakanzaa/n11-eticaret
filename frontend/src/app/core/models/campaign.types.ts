export interface CampaignResponse {
  id: string;
  offerId: string;
  sellerId: string;
  productId: string;
  discountedPrice: number;
  startsAt: string;
  endsAt: string;
  active: boolean;
}

export interface CreateCampaignRequest {
  offerId: string;
  productId: string;
  discountedPrice: number;
  startsAt: string;
  endsAt: string;
}
