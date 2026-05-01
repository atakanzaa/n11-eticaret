export type SellerStatus = 'PENDING' | 'ACTIVE' | 'SUSPENDED' | 'REJECTED';

export interface SellerDto {
  id: string;
  userId: string;
  storeName: string;
  description?: string;
  rating: number;
  ratingCount: number;
  status: SellerStatus;
  commissionRate: number;
  createdAt: string;
}

export interface UpdateSellerRequest {
  storeName?: string;
  description?: string;
}

/**
 * Per-seller order-side stats from order-service.
 * GET /api/orders/seller/{sellerId}/kpi
 */
export interface SellerOrderKpiResponse {
  todayRevenue: number;
  last7DaysRevenue: number;
  pendingOrdersCount: number;
}

/**
 * Per-seller inventory stats from inventory-service.
 * GET /api/inventory/seller/{sellerId}/stats
 */
export interface SellerInventoryStatsResponse {
  totalOffers: number;
  lowStockCount: number;
}

/**
 * Single point on the seller revenue line chart.
 * GET /api/orders/seller/{sellerId}/revenue?days=30
 */
export interface RevenuePoint {
  date: string;
  amount: number;
  orderCount: number;
}
