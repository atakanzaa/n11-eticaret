import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Page } from '@core/models/common.types';
import {
  CheckoutRequest,
  CheckoutResponse,
  OrderResponse,
} from '@core/models/order.types';
import { RevenuePoint, SellerOrderKpiResponse } from '@core/models/seller.types';

@Injectable({ providedIn: 'root' })
export class OrderApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api`;

  /**
   * Checkout. The Idempotency-Key header lets the user safely retry a failed
   * submit (network blip, lost response) without creating a duplicate order.
   */
  checkout(request: CheckoutRequest, idempotencyKey: string): Observable<CheckoutResponse> {
    const headers = new HttpHeaders({ 'Idempotency-Key': idempotencyKey });
    return this.http.post<CheckoutResponse>(`${this.base}/checkout`, request, { headers });
  }

  listMyOrders(page = 0, size = 20, sort = 'createdAt,desc'): Observable<Page<OrderResponse>> {
    const params = new HttpParams().set('page', page).set('size', size).set('sort', sort);
    return this.http.get<Page<OrderResponse>>(`${this.base}/orders`, { params });
  }

  getOrder(orderId: string): Observable<OrderResponse> {
    return this.http.get<OrderResponse>(`${this.base}/orders/${orderId}`);
  }

  /**
   * User-initiated cancellation. Backend rejects with 409 if the order has
   * already shipped or completed.
   */
  cancelOrder(orderId: string): Observable<OrderResponse> {
    return this.http.post<OrderResponse>(`${this.base}/orders/${orderId}/cancel`, {});
  }

  /**
   * Customer confirms manual delivery (cargo is mocked). Idempotent — replays
   * return the current state instead of erroring, so spam-clicks are safe.
   */
  confirmReceived(orderId: string): Observable<OrderResponse> {
    return this.http.post<OrderResponse>(`${this.base}/orders/${orderId}/confirm-received`, {});
  }

  sellerKpi(sellerId: string): Observable<SellerOrderKpiResponse> {
    return this.http.get<SellerOrderKpiResponse>(`${this.base}/orders/seller/${sellerId}/kpi`);
  }

  sellerRevenue(sellerId: string, days = 30): Observable<RevenuePoint[]> {
    return this.http.get<RevenuePoint[]>(`${this.base}/orders/seller/${sellerId}/revenue`, {
      params: new HttpParams().set('days', days),
    });
  }

  sellerOrders(sellerId: string, page = 0, size = 20): Observable<Page<OrderResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<OrderResponse>>(`${this.base}/orders/seller/${sellerId}`, { params });
  }

  markShipped(orderId: string, sellerId: string): Observable<OrderResponse> {
    return this.http.patch<OrderResponse>(
      `${this.base}/orders/${orderId}/ship`,
      {},
      { params: new HttpParams().set('sellerId', sellerId) },
    );
  }
}
