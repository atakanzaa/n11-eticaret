import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthStateService } from './auth-state.service';

/**
 * Stamps every outgoing request with `Authorization: Bearer <token>`, except
 * the unauthenticated auth endpoints (login/register/refresh) where attaching
 * a (potentially stale) token would actually confuse the backend.
 */
const SKIP_PATTERNS = ['/api/auth/login', '/api/auth/register', '/api/auth/refresh'];

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (SKIP_PATTERNS.some((p) => req.url.includes(p))) {
    return next(req);
  }
  const token = inject(AuthStateService).getAccessToken();
  if (!token) {
    return next(req);
  }
  return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
};
