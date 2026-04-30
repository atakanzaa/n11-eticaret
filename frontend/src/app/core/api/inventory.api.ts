import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AdjustStockRequest, InventoryItemResponse } from '@core/models/inventory.types';
import { SellerInventoryStatsResponse } from '@core/models/seller.types';

@Injectable({ providedIn: 'root' })
export class InventoryApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/inventory`;

  byOffer(offerId: string): Observable<InventoryItemResponse> {
    return this.http.get<InventoryItemResponse>(`${this.base}/offers/${offerId}`);
  }

  adjustStock(offerId: string, request: AdjustStockRequest): Observable<InventoryItemResponse> {
    return this.http.put<InventoryItemResponse>(`${this.base}/offers/${offerId}/stock`, request);
  }

  sellerStats(sellerId: string): Observable<SellerInventoryStatsResponse> {
    return this.http.get<SellerInventoryStatsResponse>(`${this.base}/seller/${sellerId}/stats`);
  }
}
