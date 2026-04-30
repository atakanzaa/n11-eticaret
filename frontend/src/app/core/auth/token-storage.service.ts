import { Injectable } from '@angular/core';

/**
 * Wraps localStorage access for the access/refresh token pair so the rest of
 * the auth code never speaks to `window.localStorage` directly. That makes it
 * trivially mockable in tests and gives us a single seam for tightening this
 * down later (httpOnly cookie, encrypted local store, etc.).
 *
 * Keys are namespaced with `sc_` to avoid colliding with anything else the
 * browser might be storing for `localhost:4200`.
 */
@Injectable({ providedIn: 'root' })
export class TokenStorageService {
  private static readonly ACCESS = 'sc_access_token';
  private static readonly REFRESH = 'sc_refresh_token';
  private static readonly USER = 'sc_user';

  getAccessToken(): string | null {
    return this.read(TokenStorageService.ACCESS);
  }

  getRefreshToken(): string | null {
    return this.read(TokenStorageService.REFRESH);
  }

  getUserJson(): string | null {
    return this.read(TokenStorageService.USER);
  }

  saveTokens(accessToken: string, refreshToken: string, userJson: string): void {
    this.write(TokenStorageService.ACCESS, accessToken);
    this.write(TokenStorageService.REFRESH, refreshToken);
    this.write(TokenStorageService.USER, userJson);
  }

  clear(): void {
    this.remove(TokenStorageService.ACCESS);
    this.remove(TokenStorageService.REFRESH);
    this.remove(TokenStorageService.USER);
  }

  private read(key: string): string | null {
    try {
      return localStorage.getItem(key);
    } catch {
      return null;
    }
  }

  private write(key: string, value: string): void {
    try {
      localStorage.setItem(key, value);
    } catch {
      /* SSR or storage quota — silent fallback to in-memory only for this session. */
    }
  }

  private remove(key: string): void {
    try {
      localStorage.removeItem(key);
    } catch {
      /* swallow */
    }
  }
}
