import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SellerDto, UpdateSellerRequest } from '@core/models/seller.types';

@Injectable({ providedIn: 'root' })
export class SellerApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/sellers`;

  me(): Observable<SellerDto> {
    return this.http.get<SellerDto>(`${this.base}/me`);
  }

  updateMe(request: UpdateSellerRequest): Observable<SellerDto> {
    return this.http.put<SellerDto>(`${this.base}/me`, request);
  }

  byId(id: string): Observable<SellerDto> {
    return this.http.get<SellerDto>(`${this.base}/${id}`);
  }

  byUser(userId: string): Observable<SellerDto> {
    return this.http.get<SellerDto>(`${this.base}/by-user/${userId}`);
  }
}
