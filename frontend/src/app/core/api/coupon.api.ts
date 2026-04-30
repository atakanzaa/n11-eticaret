import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CouponResponse,
  CouponValidationResponse,
  CreateCouponRequest,
  ValidateCouponRequest,
} from '@core/models/coupon.types';

@Injectable({ providedIn: 'root' })
export class CouponApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/coupons`;

  validate(request: ValidateCouponRequest): Observable<CouponValidationResponse> {
    return this.http.post<CouponValidationResponse>(`${this.base}/validate`, request);
  }

  list(): Observable<CouponResponse[]> {
    return this.http.get<CouponResponse[]>(this.base);
  }

  create(request: CreateCouponRequest): Observable<CouponResponse> {
    return this.http.post<CouponResponse>(this.base, request);
  }

  deactivate(id: string): Observable<CouponResponse> {
    return this.http.patch<CouponResponse>(`${this.base}/${id}/deactivate`, {});
  }
}
