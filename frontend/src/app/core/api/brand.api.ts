import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { BrandResponse } from '@core/models/brand.types';

@Injectable({ providedIn: 'root' })
export class BrandApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/brands`;

  list(): Observable<BrandResponse[]> {
    return this.http.get<BrandResponse[]>(this.base);
  }

  bySlug(slug: string): Observable<BrandResponse> {
    return this.http.get<BrandResponse>(`${this.base}/${slug}`);
  }
}
