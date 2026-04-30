import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AddItemRequest,
  CartResponse,
  CartValidationResponse,
} from '@core/models/cart.types';

@Injectable({ providedIn: 'root' })
export class CartApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/cart`;

  get(): Observable<CartResponse> {
    return this.http.get<CartResponse>(this.base);
  }

  addItem(request: AddItemRequest): Observable<CartResponse> {
    return this.http.post<CartResponse>(`${this.base}/items`, request);
  }

  updateQuantity(offerId: string, quantity: number): Observable<CartResponse> {
    return this.http.put<CartResponse>(`${this.base}/items/${offerId}`, null, {
      params: new HttpParams().set('quantity', quantity),
    });
  }

  removeItem(offerId: string): Observable<CartResponse> {
    return this.http.delete<CartResponse>(`${this.base}/items/${offerId}`);
  }

  clear(): Observable<void> {
    return this.http.delete<void>(this.base);
  }

  validate(): Observable<CartValidationResponse> {
    return this.http.post<CartValidationResponse>(`${this.base}/validate`, {});
  }
}
