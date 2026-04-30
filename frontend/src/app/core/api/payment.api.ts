import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  InitiatePaymentRequest,
  InitiatePaymentResponse,
  PaymentResponse,
  RefundRequest,
  RefundResponse,
} from '@core/models/payment.types';

@Injectable({ providedIn: 'root' })
export class PaymentApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/payments`;

  initiate(request: InitiatePaymentRequest): Observable<InitiatePaymentResponse> {
    return this.http.post<InitiatePaymentResponse>(`${this.base}/initiate`, request);
  }

  byId(id: string): Observable<PaymentResponse> {
    return this.http.get<PaymentResponse>(`${this.base}/${id}`);
  }

  byOrder(orderId: string): Observable<PaymentResponse> {
    return this.http.get<PaymentResponse>(`${this.base}/by-order/${orderId}`);
  }

  refund(id: string, request: RefundRequest): Observable<RefundResponse> {
    return this.http.post<RefundResponse>(`${this.base}/${id}/refund`, request);
  }
}
