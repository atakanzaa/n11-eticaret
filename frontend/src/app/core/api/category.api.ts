import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CategoryResponse,
  CreateCategoryRequest,
  UpdateCategoryRequest,
} from '@core/models/category.types';
import { Page } from '@core/models/common.types';
import { ProductResponse } from '@core/models/product.types';

@Injectable({ providedIn: 'root' })
export class CategoryApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/categories`;

  /** Public — only active categories (storefront). */
  list(): Observable<CategoryResponse[]> {
    return this.http.get<CategoryResponse[]>(this.base);
  }

  /** Admin — all categories including inactive. */
  listForAdmin(): Observable<CategoryResponse[]> {
    return this.http.get<CategoryResponse[]>(`${this.base}/admin`);
  }

  bySlug(slug: string): Observable<CategoryResponse> {
    return this.http.get<CategoryResponse>(`${this.base}/${slug}`);
  }

  productsBySlug(slug: string, page = 0, size = 24): Observable<Page<ProductResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<ProductResponse>>(`${this.base}/${slug}/products`, { params });
  }

  create(request: CreateCategoryRequest): Observable<CategoryResponse> {
    return this.http.post<CategoryResponse>(this.base, request);
  }

  update(id: string, request: UpdateCategoryRequest): Observable<CategoryResponse> {
    return this.http.put<CategoryResponse>(`${this.base}/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
