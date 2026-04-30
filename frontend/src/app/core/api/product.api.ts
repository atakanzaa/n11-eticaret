import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
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
    if (params.page !== undefined) httpParams = httpParams.set('page', params.page);
    if (params.size !== undefined) httpParams = httpParams.set('size', params.size);
    if (params.sort) httpParams = httpParams.set('sort', params.sort);
    return this.http.get<Page<ProductResponse>>(this.base, { params: httpParams });
  }

  byId(id: string): Observable<ProductResponse> {
    return this.http.get<ProductResponse>(`${this.base}/${id}`);
  }

  create(request: CreateProductRequest): Observable<ProductResponse> {
    return this.http.post<ProductResponse>(this.base, request);
  }

  update(id: string, request: UpdateProductRequest): Observable<ProductResponse> {
    return this.http.put<ProductResponse>(`${this.base}/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
