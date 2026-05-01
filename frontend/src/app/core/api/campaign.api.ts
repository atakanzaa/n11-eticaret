import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { CampaignResponse, CreateCampaignRequest } from '@core/models/campaign.types';

@Injectable({ providedIn: 'root' })
export class CampaignApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/campaigns`;

  myCampaigns(): Observable<CampaignResponse[]> {
    return this.http.get<CampaignResponse[]>(`${this.base}/my`);
  }

  activeByOffer(offerId: string): Observable<CampaignResponse | null> {
    return this.http.get<CampaignResponse>(`${this.base}/active-by-offer/${offerId}`).pipe(
      catchError(() => of(null)),
    );
  }

  create(request: CreateCampaignRequest): Observable<CampaignResponse> {
    return this.http.post<CampaignResponse>(this.base, request);
  }

  deactivate(id: string): Observable<CampaignResponse> {
    return this.http.patch<CampaignResponse>(`${this.base}/${id}/deactivate`, {});
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
