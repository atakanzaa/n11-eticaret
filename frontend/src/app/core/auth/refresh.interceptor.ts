import {
  HttpErrorResponse,
  HttpEvent,
  HttpHandlerFn,
  HttpInterceptorFn,
  HttpRequest,
} from '@angular/common/http';
import { Router } from '@angular/router';
import { inject } from '@angular/core';
import { Observable, catchError, from, switchMap, throwError } from 'rxjs';
import { AuthApi } from '@core/api/auth.api';
import { AuthStateService } from './auth-state.service';

/**
 * Catches 401 from any non-auth request, calls `/api/auth/refresh` once with
 * the stored refresh token, persists the new pair, and replays the original
 * request with the new access token.
 *
 * Only one refresh runs at a time even when multiple requests fire 401s
 * concurrently — they all wait on the same in-flight Promise. This matters
 * because the backend's refresh-token-reuse detection will revoke the entire
 * token family if the same refresh token is presented twice.
 */
let refreshInFlight: Promise<string> | null = null;

const SKIP_REFRESH_FOR = [
  '/api/auth/login',
  '/api/auth/register',
  '/api/auth/refresh',
  '/api/auth/logout',
];

export const refreshInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthStateService);
  const authApi = inject(AuthApi);
  const router = inject(Router);

  if (SKIP_REFRESH_FOR.some((p) => req.url.includes(p))) {
    return next(req);
  }

  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status !== 401) {
        return throwError(() => err);
      }
      const refreshToken = auth.getRefreshToken();
      if (!refreshToken) {
        auth.clearSession();
        router.navigate(['/giris'], { queryParams: { returnUrl: router.url } });
        return throwError(() => err);
      }
      const refresh$ = ensureRefreshPromise(refreshToken, authApi, auth, router);
      return from(refresh$).pipe(switchMap((newAccess) => retryWithToken(req, next, newAccess)));
    }),
  );
};

function ensureRefreshPromise(
  refreshToken: string,
  authApi: AuthApi,
  auth: AuthStateService,
  router: Router,
): Promise<string> {
  if (refreshInFlight) {
    return refreshInFlight;
  }
  refreshInFlight = new Promise<string>((resolve, reject) => {
    authApi.refresh({ refreshToken }).subscribe({
      next: (response) => {
        auth.setSession(response);
        refreshInFlight = null;
        resolve(response.accessToken);
      },
      error: (err) => {
        refreshInFlight = null;
        auth.clearSession();
        router.navigate(['/giris'], { queryParams: { returnUrl: router.url } });
        reject(err);
      },
    });
  });
  return refreshInFlight;
}

function retryWithToken(
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
  token: string,
): Observable<HttpEvent<unknown>> {
  return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
}
