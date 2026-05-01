import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { CurrencyFormatPipe } from '@shared/pipes/currency-format.pipe';

@Component({
  selector: 'sc-price',
  standalone: true,
  imports: [CurrencyFormatPipe],
  templateUrl: './price-display.component.html',
  styleUrls: ['./price-display.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PriceDisplayComponent {
  price = input.required<number>();
  listPrice = input<number | null>(null);
  size = input<'sm' | 'base' | 'lg'>('base');

  discountPct = computed(() => {
    const lp = this.listPrice();
    const p = this.price();
    if (!lp || lp <= p) return null;
    return Math.round(((lp - p) / lp) * 100);
  });
}
