import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Page } from '@core/models/common.types';
import {
  CreateBlacklistRequest,
  FraudBlacklist,
  FraudCheck,
  FraudDecision,
} from '@core/models/fraud.types';

@Injectable({ providedIn: 'root' })
export class FraudApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/fraud`;

  listChecks(decision?: FraudDecision, page = 0, size = 20): Observable<Page<FraudCheck>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (decision) params = params.set('decision', decision);
    return this.http.get<Page<FraudCheck>>(`${this.base}/checks`, { params });
  }

  getCheck(orderId: string): Observable<FraudCheck> {
    return this.http.get<FraudCheck>(`${this.base}/checks/${orderId}`);
  }

  listBlacklist(): Observable<FraudBlacklist[]> {
    return this.http.get<FraudBlacklist[]>(`${this.base}/blacklist`);
  }

  addToBlacklist(request: CreateBlacklistRequest): Observable<FraudBlacklist> {
    return this.http.post<FraudBlacklist>(`${this.base}/blacklist`, request);
  }

  removeFromBlacklist(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/blacklist/${id}`);
  }
}
