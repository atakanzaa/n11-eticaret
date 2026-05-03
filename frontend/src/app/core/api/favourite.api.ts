import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Page } from '@core/models/common.types';
import { FavouriteResponse } from '@core/models/favourite.types';

@Injectable({ providedIn: 'root' })
export class FavouriteApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/users/me/favourites`;

  list(page = 0, size = 24): Observable<Page<FavouriteResponse>> {
    const params = new HttpParams().set('page', page).set('size', size).set('sort', 'addedAt,desc');
    return this.http.get<Page<FavouriteResponse>>(this.base, { params });
  }

  contains(productId: string): Observable<{ favourited: boolean }> {
    return this.http.get<{ favourited: boolean }>(`${this.base}/contains/${productId}`);
  }

  add(productId: string): Observable<FavouriteResponse> {
    return this.http.post<FavouriteResponse>(`${this.base}/${productId}`, {});
  }

  remove(productId: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${productId}`);
  }
}
