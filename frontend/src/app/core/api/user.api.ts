import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AddressDto,
  CreateAddressRequest,
  UpdateAddressRequest,
  UpdateProfileRequest,
  UserProfileDto,
} from '@core/models/user.types';

@Injectable({ providedIn: 'root' })
export class UserApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/users`;

  me(): Observable<UserProfileDto> {
    return this.http.get<UserProfileDto>(`${this.base}/me`);
  }

  updateProfile(request: UpdateProfileRequest): Observable<UserProfileDto> {
    return this.http.put<UserProfileDto>(`${this.base}/me`, request);
  }

  giveKvkkConsent(): Observable<void> {
    return this.http.post<void>(`${this.base}/me/kvkk-consent`, {});
  }

  deleteAccount(): Observable<void> {
    return this.http.delete<void>(`${this.base}/me`);
  }

  listAddresses(): Observable<AddressDto[]> {
    return this.http.get<AddressDto[]>(`${this.base}/me/addresses`);
  }

  addAddress(request: CreateAddressRequest): Observable<AddressDto> {
    return this.http.post<AddressDto>(`${this.base}/me/addresses`, request);
  }

  updateAddress(id: string, request: UpdateAddressRequest): Observable<AddressDto> {
    return this.http.put<AddressDto>(`${this.base}/me/addresses/${id}`, request);
  }

  deleteAddress(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/me/addresses/${id}`);
  }
}
