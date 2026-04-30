import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthStateService } from './auth-state.service';

/** Sends unauthenticated visitors to `/giris?returnUrl=…`. */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthStateService);
  const router = inject(Router);
  if (auth.isAuthenticated()) {
    return true;
  }
  return router.createUrlTree(['/giris'], { queryParams: { returnUrl: state.url } });
};
