import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Page } from '@core/models/common.types';
import {
  CreateReviewRequest,
  CreateReviewReplyRequest,
  CreateReviewReportRequest,
  ReviewReplyResponse,
  ReviewReportResponse,
  ReviewResponse,
  ReviewStatsResponse,
  UpdateReviewRequest,
  UpdateReviewReplyRequest,
} from '@core/models/review.types';

@Injectable({ providedIn: 'root' })
export class ReviewApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api`;

  // ── Public ───────────────────────────────────────────────────────
  listByProduct(productId: string, page = 0, size = 10, params?: Record<string, string>): Observable<Page<ReviewResponse>> {
    let httpParams = new HttpParams().set('page', page).set('size', size).set('sort', 'createdAt,desc');
    if (params) {
      Object.entries(params).forEach(([k, v]) => { if (v) httpParams = httpParams.set(k, v); });
    }
    return this.http.get<Page<ReviewResponse>>(`${this.base}/products/${productId}/reviews`, { params: httpParams });
  }

  stats(productId: string): Observable<ReviewStatsResponse> {
    return this.http.get<ReviewStatsResponse>(`${this.base}/products/${productId}/reviews/stats`);
  }

  byId(id: string): Observable<ReviewResponse> {
    return this.http.get<ReviewResponse>(`${this.base}/reviews/${id}`);
  }

  /**
   * Returns the current user's review for a given product, or `null` if the
   * user hasn't reviewed it yet (backend returns 204 No Content in that case).
   */
  myReviewForProduct(productId: string): Observable<ReviewResponse | null> {
    return this.http
      .get<ReviewResponse | null>(`${this.base}/products/${productId}/reviews/me`, {
        observe: 'response',
      })
      .pipe(map(resp => (resp.status === 204 ? null : resp.body)));
  }

  // ── Authenticated ────────────────────────────────────────────────
  create(productId: string, userDisplayName: string, request: CreateReviewRequest): Observable<ReviewResponse> {
    return this.http.post<ReviewResponse>(`${this.base}/products/${productId}/reviews`, request, {
      headers: { 'X-User-Display-Name': userDisplayName },
    });
  }

  update(id: string, request: UpdateReviewRequest): Observable<ReviewResponse> {
    return this.http.put<ReviewResponse>(`${this.base}/reviews/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/reviews/${id}`);
  }

  vote(id: string, voteType: 'HELPFUL' | 'UNHELPFUL'): Observable<ReviewResponse> {
    return this.http.post<ReviewResponse>(`${this.base}/reviews/${id}/vote`, null, {
      params: new HttpParams().set('voteType', voteType),
    });
  }

  report(id: string, request: CreateReviewReportRequest): Observable<ReviewReportResponse> {
    return this.http.post<ReviewReportResponse>(`${this.base}/reviews/${id}/report`, request);
  }

  // ── Admin ────────────────────────────────────────────────────────
  listPending(page = 0, size = 20): Observable<Page<ReviewResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<ReviewResponse>>(`${this.base}/admin/reviews/pending`, { params });
  }

  approve(id: string): Observable<ReviewResponse> {
    return this.http.post<ReviewResponse>(`${this.base}/admin/reviews/${id}/approve`, {});
  }

  reject(id: string, rejectionReason: string): Observable<ReviewResponse> {
    return this.http.post<ReviewResponse>(`${this.base}/admin/reviews/${id}/reject`, { rejectionReason });
  }

  listReports(page = 0, size = 20): Observable<Page<ReviewReportResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<ReviewReportResponse>>(`${this.base}/admin/reviews/reports`, { params });
  }

  dismissReport(id: string): Observable<ReviewReportResponse> {
    return this.http.post<ReviewReportResponse>(`${this.base}/admin/reviews/reports/${id}/dismiss`, {});
  }

  // ── Seller ───────────────────────────────────────────────────────
  createReply(reviewId: string, sellerId: string, request: CreateReviewReplyRequest): Observable<ReviewReplyResponse> {
    return this.http.post<ReviewReplyResponse>(`${this.base}/reviews/${reviewId}/reply`, request, {
      headers: { 'X-Seller-Id': sellerId },
    });
  }

  updateReply(id: string, sellerId: string, request: UpdateReviewReplyRequest): Observable<ReviewReplyResponse> {
    return this.http.put<ReviewReplyResponse>(`${this.base}/reviews/replies/${id}`, request, {
      headers: { 'X-Seller-Id': sellerId },
    });
  }

  deleteReply(id: string, sellerId: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/reviews/replies/${id}`, {
      headers: { 'X-Seller-Id': sellerId },
    });
  }

  sellerReplies(sellerId: string): Observable<ReviewReplyResponse[]> {
    return this.http.get<ReviewReplyResponse[]>(`${this.base}/reviews/replies/my`, {
      headers: { 'X-Seller-Id': sellerId },
    });
  }
}
