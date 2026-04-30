import { Pipe, PipeTransform, inject } from '@angular/core';
import { I18nService } from '@core/i18n/i18n.service';

/**
 * Template-side translation. Pure pipe so Angular caches by inputs — re-renders
 * only when key or params reference change.
 *
 * Examples:
 *   {{ 'orderStatus.CONFIRMED' | t }}
 *   {{ 'product.lowStock' | t: { count: stock } }}
 *
 * Standalone import: add `TPipe` to the component's `imports` array.
 */
@Pipe({ name: 't', standalone: true, pure: true })
export class TPipe implements PipeTransform {
  private readonly i18n = inject(I18nService);

  transform(key: string, params?: Record<string, string | number>): string {
    return this.i18n.t(key, params);
  }
}
