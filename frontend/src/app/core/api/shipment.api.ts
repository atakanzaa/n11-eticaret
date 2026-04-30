import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ShipmentEventResponse, ShipmentResponse } from '@core/models/shipment.types';

@Injectable({ providedIn: 'root' })
export class ShipmentApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/shipments`;

  byId(id: string): Observable<ShipmentResponse> {
    return this.http.get<ShipmentResponse>(`${this.base}/${id}`);
  }

  byOrder(orderId: string): Observable<ShipmentResponse[]> {
    return this.http.get<ShipmentResponse[]>(`${this.base}/by-order/${orderId}`);
  }

  track(trackingNumber: string): Observable<ShipmentResponse> {
    return this.http.get<ShipmentResponse>(`${this.base}/track/${trackingNumber}`);
  }

  events(shipmentId: string): Observable<ShipmentEventResponse[]> {
    return this.http.get<ShipmentEventResponse[]>(`${this.base}/${shipmentId}/events`);
  }
}
