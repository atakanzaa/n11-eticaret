import { HttpContextToken, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { ToastService } from '@core/toast.service';
import { I18nService } from '@core/i18n/i18n.service';
import { BackendErrorEnvelope } from '@core/models/common.types';

export const SKIP_ERROR_TOAST = new HttpContextToken<boolean>(() => false);

/**
 * Surfaces backend `ProblemDetail` errors as toasts. Skips 401 because the
 * refresh interceptor handles that flow without surfacing noise to the user.
 *
 * Always re-throws — components can still subscribe to `error` themselves if
 * they need a per-form decoration on top of the toast.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const toast = inject(ToastService);
  const i18n = inject(I18nService);

  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      if (req.context.get(SKIP_ERROR_TOAST)) {
        return throwError(() => err);
      }
      if (err.status === 0) {
        toast.show(i18n.t('common.error'), 'danger');
      } else if (err.status === 401) {
        // refresh.interceptor handles redirect; no toast.
      } else if (err.status === 403) {
        toast.show(i18n.t('auth.unauthorized'), 'warn');
      } else {
        const message = extractMessage(err, i18n) ?? i18n.t('common.error');
        toast.show(message, 'danger');
      }
      return throwError(() => err);
    }),
  );
};

function extractMessage(err: HttpErrorResponse, i18n: I18nService): string | null {
  const body = err.error as BackendErrorEnvelope | string | undefined;
  if (!body) return null;
  if (typeof body === 'string') return body;
  // Backend canonical ERR_xxxx kodu → i18n çevirisi varsa onu göster.
  const code = body.error?.code;
  if (code && /^ERR_\d{4}$/.test(code)) {
    const key = `errors.${code}`;
    const translated = i18n.t(key);
    if (translated && translated !== key) return translated;
  }
  return body.error?.message ?? null;
}
