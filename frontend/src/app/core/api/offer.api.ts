import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SKIP_ERROR_TOAST } from '@core/http/error.interceptor';
import {
  CreateOfferRequest,
  OfferResponse,
  UpdateOfferRequest,
} from '@core/models/offer.types';

@Injectable({ providedIn: 'root' })
export class OfferApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api`;

  myOffers(): Observable<OfferResponse[]> {
    return this.http.get<OfferResponse[]>(`${this.base}/offers/my`);
  }

  byId(id: string): Observable<OfferResponse> {
    return this.http.get<OfferResponse>(`${this.base}/offers/${id}`);
  }

  byProduct(productId: string): Observable<OfferResponse[]> {
    return this.http.get<OfferResponse[]>(`${this.base}/products/${productId}/offers`);
  }

  create(request: CreateOfferRequest, skipErrorToast = false): Observable<OfferResponse> {
    const context = skipErrorToast ? new HttpContext().set(SKIP_ERROR_TOAST, true) : undefined;
    return this.http.post<OfferResponse>(`${this.base}/offers`, request, { context });
  }

  update(id: string, request: UpdateOfferRequest): Observable<OfferResponse> {
    return this.http.put<OfferResponse>(`${this.base}/offers/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/offers/${id}`);
  }
}
