import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Page } from '@core/models/common.types';
import { CreateReviewRequest, ReviewResponse } from '@core/models/review.types';

@Injectable({ providedIn: 'root' })
export class ReviewApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api`;

  listByProduct(productId: string, page = 0, size = 10): Observable<Page<ReviewResponse>> {
    const params = new HttpParams().set('page', page).set('size', size).set('sort', 'createdAt,desc');
    return this.http.get<Page<ReviewResponse>>(`${this.base}/products/${productId}/reviews`, { params });
  }

  create(productId: string, userDisplayName: string, request: CreateReviewRequest): Observable<ReviewResponse> {
    return this.http.post<ReviewResponse>(`${this.base}/products/${productId}/reviews`, request, {
      headers: { 'X-User-Display-Name': userDisplayName },
    });
  }

  byId(id: string): Observable<ReviewResponse> {
    return this.http.get<ReviewResponse>(`${this.base}/reviews/${id}`);
  }

  markHelpful(id: string): Observable<ReviewResponse> {
    return this.http.post<ReviewResponse>(`${this.base}/reviews/${id}/helpful`, {});
  }
}
