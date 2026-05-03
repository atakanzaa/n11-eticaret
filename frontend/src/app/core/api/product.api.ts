import { HttpClient, HttpContext, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SKIP_ERROR_TOAST } from '@core/http/error.interceptor';
import { Page } from '@core/models/common.types';
import {
  CreateProductRequest,
  ProductResponse,
  UpdateProductRequest,
} from '@core/models/product.types';

export interface ProductSearchParams {
  query?: string;
  categoryId?: string;
  brandId?: string;
  /** Minimum average rating, 1-5 inclusive. */
  minRating?: number;
  page?: number;
  size?: number;
  sort?: string;
}

@Injectable({ providedIn: 'root' })
export class ProductApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/products`;

  list(params: ProductSearchParams = {}): Observable<Page<ProductResponse>> {
    let httpParams = new HttpParams();
    if (params.query) httpParams = httpParams.set('query', params.query);
    if (params.categoryId) httpParams = httpParams.set('categoryId', params.categoryId);
    if (params.brandId) httpParams = httpParams.set('brandId', params.brandId);
    if (params.minRating !== undefined && params.minRating !== null) {
      httpParams = httpParams.set('minRating', params.minRating);
    }
    if (params.page !== undefined) httpParams = httpParams.set('page', params.page);
    if (params.size !== undefined) httpParams = httpParams.set('size', params.size);
    if (params.sort) httpParams = httpParams.set('sort', params.sort);
    return this.http.get<Page<ProductResponse>>(this.base, { params: httpParams });
  }

  byId(id: string): Observable<ProductResponse> {
    return this.http.get<ProductResponse>(`${this.base}/${id}`);
  }

  /**
   * Pre-create dedupe lookup. 200 → product already exists with this barcode;
   * 404 → safe to create. Used by the seller "ürün ekle" form to redirect to
   * "add an offer" instead of duplicating the catalog entry.
   */
  byBarcode(barcode: string, skipErrorToast = true): Observable<ProductResponse> {
    const context = skipErrorToast ? new HttpContext().set(SKIP_ERROR_TOAST, true) : undefined;
    return this.http.get<ProductResponse>(`${this.base}/by-barcode/${encodeURIComponent(barcode)}`, { context });
  }

  create(request: CreateProductRequest, skipErrorToast = false): Observable<ProductResponse> {
    const context = skipErrorToast ? new HttpContext().set(SKIP_ERROR_TOAST, true) : undefined;
    return this.http.post<ProductResponse>(this.base, request, { context });
  }

  update(id: string, request: UpdateProductRequest): Observable<ProductResponse> {
    return this.http.put<ProductResponse>(`${this.base}/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
