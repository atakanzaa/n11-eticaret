import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthStateService } from './auth-state.service';
import { RoleName } from '@core/models/auth.types';

/**
 * Builds a CanActivateFn that requires the user to hold one of `allowedRoles`.
 * Unauthenticated visitors land on `/giris`; authenticated-but-wrong-role
 * visitors land on `/yetkisiz`.
 *
 * Usage:
 *   { path: 'admin', canActivate: [roleGuard(['ADMIN'])], … }
 */
export function roleGuard(allowedRoles: RoleName[]): CanActivateFn {
  return (_route, state) => {
    const auth = inject(AuthStateService);
    const router = inject(Router);

    if (!auth.isAuthenticated()) {
      return router.createUrlTree(['/giris'], { queryParams: { returnUrl: state.url } });
    }
    const userRoles = auth.roles();
    if (allowedRoles.some((r) => userRoles.includes(r))) {
      return true;
    }
    return router.createUrlTree(['/yetkisiz']);
  };
}
