import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ProductRecommendation } from '@core/models/recommendation.types';

@Injectable({ providedIn: 'root' })
export class RecommendationApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/recommendations`;

  forMe(limit = 10): Observable<ProductRecommendation[]> {
    return this.http.get<ProductRecommendation[]>(`${this.base}/me`, {
      params: new HttpParams().set('limit', limit),
    });
  }

  popular(limit = 10): Observable<ProductRecommendation[]> {
    return this.http.get<ProductRecommendation[]>(`${this.base}/popular`, {
      params: new HttpParams().set('limit', limit),
    });
  }

  recentlyViewed(limit = 10): Observable<ProductRecommendation[]> {
    return this.http.get<ProductRecommendation[]>(`${this.base}/recently-viewed`, {
      params: new HttpParams().set('limit', limit),
    });
  }

  related(productId: string, limit = 10): Observable<ProductRecommendation[]> {
    return this.http.get<ProductRecommendation[]>(`${this.base}/products/${productId}/related`, {
      params: new HttpParams().set('limit', limit),
    });
  }

  trackView(productId: string): Observable<void> {
    return this.http.post<void>(`${this.base}/track/view?productId=${productId}`, null);
  }
}
