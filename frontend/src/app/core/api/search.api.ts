import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface SearchQuery {
  q?: string;
  categoryId?: string;
  brandId?: string;
  sellerId?: string;
  minPrice?: number;
  maxPrice?: number;
  /** Minimum average product rating, 1-5 inclusive. */
  minRating?: number;
  inStockOnly?: boolean;
  sort?: string;
  page?: number;
  size?: number;
}

export interface SearchResultResponse<T = unknown> {
  hits: T[];
  total: number;
  page: number;
  size: number;
  facets?: Record<string, unknown>;
}

@Injectable({ providedIn: 'root' })
export class SearchApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/search`;

  search<T = unknown>(query: SearchQuery): Observable<SearchResultResponse<T>> {
    let params = new HttpParams();
    Object.entries(query).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, String(value));
      }
    });
    return this.http.get<SearchResultResponse<T>>(this.base, { params });
  }
}
