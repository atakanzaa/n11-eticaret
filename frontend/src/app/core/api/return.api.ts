import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CreateReturnRequest,
  RejectReturnRequest,
  ReturnResponse,
} from '@core/models/return.types';

@Injectable({ providedIn: 'root' })
export class ReturnApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/returns`;

  create(request: CreateReturnRequest): Observable<ReturnResponse> {
    return this.http.post<ReturnResponse>(this.base, request);
  }

  myReturns(): Observable<ReturnResponse[]> {
    return this.http.get<ReturnResponse[]>(`${this.base}/me`);
  }

  byId(id: string): Observable<ReturnResponse> {
    return this.http.get<ReturnResponse>(`${this.base}/${id}`);
  }

  approve(id: string): Observable<ReturnResponse> {
    return this.http.post<ReturnResponse>(`${this.base}/${id}/approve`, {});
  }

  reject(id: string, request: RejectReturnRequest): Observable<ReturnResponse> {
    return this.http.post<ReturnResponse>(`${this.base}/${id}/reject`, request);
  }

  cancel(id: string): Observable<ReturnResponse> {
    return this.http.post<ReturnResponse>(`${this.base}/${id}/cancel`, {});
  }
}
