import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AdminOverviewResponse } from '@core/models/order.types';

@Injectable({ providedIn: 'root' })
export class AdminApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/admin`;

  overview(): Observable<AdminOverviewResponse> {
    return this.http.get<AdminOverviewResponse>(`${this.base}/overview`);
  }
}
