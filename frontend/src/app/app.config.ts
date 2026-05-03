import { ApplicationConfig, LOCALE_ID, provideZoneChangeDetection } from '@angular/core';
import { provideRouter, withComponentInputBinding, withInMemoryScrolling } from '@angular/router';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { registerLocaleData } from '@angular/common';
import localeTr from '@angular/common/locales/tr';
import localeTrExtra from '@angular/common/locales/extra/tr';
import { routes } from './app.routes';
import { authInterceptor } from '@core/auth/auth.interceptor';
import { refreshInterceptor } from '@core/auth/refresh.interceptor';
import { errorInterceptor } from '@core/http/error.interceptor';

registerLocaleData(localeTr, 'tr-TR', localeTrExtra);

export const appConfig: ApplicationConfig = {
  providers: [
    { provide: LOCALE_ID, useValue: 'tr-TR' },
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(
      routes,
      withComponentInputBinding(),
      withInMemoryScrolling({ scrollPositionRestoration: 'top', anchorScrolling: 'enabled' })
    ),
    // Interceptor order matters: refresh runs first so a successful refresh
    // gets a fresh Authorization header on retry, then auth attaches headers
    // for the original request, then errors bubble up to the toast layer.
    provideHttpClient(
      withFetch(),
      withInterceptors([refreshInterceptor, authInterceptor, errorInterceptor]),
    ),
  ],
};
