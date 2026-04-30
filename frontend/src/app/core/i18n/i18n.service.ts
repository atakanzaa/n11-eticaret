import { Injectable } from '@angular/core';
import { TR } from './tr';

/**
 * Tiny dot-notation lookup over a static dictionary, plus `{{name}}` token
 * interpolation. We deliberately avoid pulling in @angular/localize or ngx-translate
 * — the app is Turkish-only and the dictionary in `tr.ts` is fully typed.
 *
 * Usage from TS: `i18n.t('orderStatus.CONFIRMED')`,
 * Usage in templates: `{{ 'orderStatus.CONFIRMED' | t }}`,
 * with params: `i18n.t('product.lowStock', { count: 3 })`.
 */
@Injectable({ providedIn: 'root' })
export class I18nService {
  private readonly dict: Readonly<Record<string, unknown>> = TR as unknown as Readonly<
    Record<string, unknown>
  >;

  t(path: string, params?: Record<string, string | number>): string {
    const segments = path.split('.');
    let cursor: unknown = this.dict;
    for (const seg of segments) {
      if (cursor && typeof cursor === 'object' && seg in (cursor as object)) {
        cursor = (cursor as Record<string, unknown>)[seg];
      } else {
        cursor = undefined;
        break;
      }
    }
    if (typeof cursor !== 'string') {
      // Surface the missing key so it gets fixed during dev rather than rendering blank.
      return `[missing: ${path}]`;
    }
    return params ? this.interpolate(cursor, params) : cursor;
  }

  private interpolate(template: string, params: Record<string, string | number>): string {
    return template.replace(/\{\{\s*([\w-]+)\s*\}\}/g, (_, key: string) =>
      params[key] === undefined ? `{{${key}}}` : String(params[key]),
    );
  }
}
