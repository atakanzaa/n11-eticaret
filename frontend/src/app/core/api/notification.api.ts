import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Page } from '@core/models/common.types';
import { NotificationLogResponse } from '@core/models/notification.types';

@Injectable({ providedIn: 'root' })
export class NotificationApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/notifications`;

  myLogs(page = 0, size = 20): Observable<Page<NotificationLogResponse>> {
    const params = new HttpParams().set('page', page).set('size', size).set('sort', 'createdAt,desc');
    return this.http.get<Page<NotificationLogResponse>>(`${this.base}/me`, { params });
  }

  allLogs(page = 0, size = 50): Observable<Page<NotificationLogResponse>> {
    const params = new HttpParams().set('page', page).set('size', size).set('sort', 'createdAt,desc');
    return this.http.get<Page<NotificationLogResponse>>(`${this.base}/logs`, { params });
  }
}
