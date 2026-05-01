import { Pipe, PipeTransform } from '@angular/core';

/**
 * Formats a number as Turkish Lira (TRY) currency.
 *
 * Usage:
 *   {{ 149.90 | scCurrency }}            → ₺149,90
 *   {{ 149.90 | scCurrency:'USD' }}      → $149.90
 *
 * Replaces the 12+ duplicated `formatPrice()` methods across components.
 */
@Pipe({
  name: 'scCurrency',
  standalone: true,
})
export class CurrencyFormatPipe implements PipeTransform {
  private readonly formatters = new Map<string, Intl.NumberFormat>();

  transform(value: number | null | undefined, currency = 'TRY'): string {
    if (value == null) return '';

    let formatter = this.formatters.get(currency);
    if (!formatter) {
      formatter = new Intl.NumberFormat('tr-TR', {
        style: 'currency',
        currency,
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      });
      this.formatters.set(currency, formatter);
    }

    return formatter.format(value);
  }
}
