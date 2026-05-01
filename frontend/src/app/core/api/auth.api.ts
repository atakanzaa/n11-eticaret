import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AuthResponse,
  LoginRequest,
  RefreshRequest,
  RegisterRequest,
  UserDto,
} from '@core/models/auth.types';

/**
 * Thin HTTP wrapper for `/api/auth/**`. Lives entirely in the request layer:
 * no state, no caching, no signal manipulation — that's `AuthStateService`'s job.
 */
@Injectable({ providedIn: 'root' })
export class AuthApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/auth`;

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.base}/login`, request);
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.base}/register`, request);
  }

  refresh(request: RefreshRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.base}/refresh`, request);
  }

  logout(refreshToken: string): Observable<void> {
    return this.http.post<void>(`${this.base}/logout`, { refreshToken });
  }

  me(): Observable<UserDto> {
    return this.http.get<UserDto>(`${this.base}/me`);
  }

  changePassword(request: { currentPassword: string; newPassword: string }): Observable<void> {
    return this.http.post<void>(`${this.base}/me/password`, request);
  }

  becomeSeller(): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.base}/become-seller`, {});
  }
}
