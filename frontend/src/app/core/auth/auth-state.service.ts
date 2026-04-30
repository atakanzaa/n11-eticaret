import { Injectable, computed, inject, signal } from '@angular/core';
import { AuthResponse, RoleName, UserDto } from '@core/models/auth.types';
import { TokenStorageService } from './token-storage.service';

/**
 * Single source of truth for "who is logged in right now". Exposes signals so
 * UI bits can react with zero RxJS, and exposes write methods that auth flows
 * (login, register, refresh, logout, 401-recovery) call.
 *
 * The constructor rehydrates state from localStorage so a page reload doesn't
 * dump the user back to the login screen.
 */
@Injectable({ providedIn: 'root' })
export class AuthStateService {
  private readonly storage = inject(TokenStorageService);

  private readonly _currentUser = signal<UserDto | null>(this.readPersistedUser());
  readonly currentUser = this._currentUser.asReadonly();
  readonly isAuthenticated = computed(() => this._currentUser() !== null);
  readonly roles = computed<RoleName[]>(() => this._currentUser()?.roles ?? []);

  /** Convenience: `auth.hasRole('SELLER')()` returns boolean. */
  hasRole(role: RoleName) {
    return computed(() => this.roles().includes(role));
  }

  /** Convenience for guards / interceptors that need the access token sync. */
  getAccessToken(): string | null {
    return this.storage.getAccessToken();
  }

  getRefreshToken(): string | null {
    return this.storage.getRefreshToken();
  }

  setSession(response: AuthResponse): void {
    this.storage.saveTokens(response.accessToken, response.refreshToken, JSON.stringify(response.user));
    this._currentUser.set(response.user);
  }

  clearSession(): void {
    this.storage.clear();
    this._currentUser.set(null);
  }

  private readPersistedUser(): UserDto | null {
    const raw = this.storage.getUserJson();
    if (!raw) return null;
    try {
      return JSON.parse(raw) as UserDto;
    } catch {
      this.storage.clear();
      return null;
    }
  }
}
