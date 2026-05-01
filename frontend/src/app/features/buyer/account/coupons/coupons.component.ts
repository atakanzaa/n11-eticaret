import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { CouponApi } from '@core/api/coupon.api';
import { CouponResponse } from '@core/models/coupon.types';
import { ToastService } from '@core/toast.service';
import { SpinnerComponent } from '@shared/ui/spinner/spinner.component';
import { EmptyStateComponent } from '@shared/ui/empty-state/empty-state.component';

@Component({
  selector: 'sc-buyer-coupons',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './coupons.component.html',
  styleUrls: ['./coupons.component.scss'],
  imports: [DatePipe, SpinnerComponent, EmptyStateComponent],
})
export class BuyerCouponsComponent implements OnInit {
  private readonly couponApi = inject(CouponApi);
  private readonly toast = inject(ToastService);

  readonly coupons = signal<CouponResponse[]>([]);
  readonly loading = signal(true);

  async ngOnInit(): Promise<void> {
    this.loading.set(true);
    try {
      const list = await firstValueFrom(this.couponApi.active());
      this.coupons.set(list);
    } catch {
      /* error.interceptor handles toast */
    } finally {
      this.loading.set(false);
    }
  }

  async copyCode(code: string): Promise<void> {
    try {
      await navigator.clipboard.writeText(code);
      this.toast.show(`"${code}" panoya kopyalandı`, 'success');
    } catch {
      this.toast.show('Kopyalanamadı', 'warn');
    }
  }

  formatDiscount(c: CouponResponse): string {
    if (c.discountType === 'PERCENTAGE') return `%${c.discountValue} indirim`;
    if (c.discountType === 'FIXED_AMOUNT') return `₺${c.discountValue} indirim`;
    return 'Ücretsiz Kargo';
  }
}
