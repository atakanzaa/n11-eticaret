import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { CouponApi } from '@core/api/coupon.api';
import { CouponResponse } from '@core/models/coupon.types';
import { ToastService } from '@core/toast.service';
import { TPipe } from '@shared/i18n.pipe';

@Component({
  selector: 'sc-admin-promotions',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, TPipe],
  template: `
    <h1>{{ 'admin.promotions' | t }}</h1>

    @if (loading()) {
      <p class="muted">{{ 'common.loading' | t }}</p>
    } @else if (coupons().length === 0) {
      <p class="muted">{{ 'common.empty' | t }}</p>
    } @else {
      <table>
        <thead>
          <tr>
            <th>Kod</th>
            <th>Ad</th>
            <th>Tip</th>
            <th>Değer</th>
            <th>Geçerli</th>
            <th>Kullanım</th>
            <th>Durum</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          @for (c of coupons(); track c.id) {
            <tr>
              <td><code>{{ c.code }}</code></td>
              <td>{{ c.name }}</td>
              <td>{{ c.discountType }}</td>
              <td>{{ formatValue(c) }}</td>
              <td>
                <span class="muted">{{ c.validFrom | date: 'shortDate' }} →</span>
                <br />
                <span class="muted">{{ c.validUntil | date: 'shortDate' }}</span>
              </td>
              <td>{{ c.timesUsed }} / {{ c.totalUsageLimit ?? '∞' }}</td>
              <td>
                <span class="pill" [class]="c.active ? 'pill-success' : 'pill-inactive'">
                  {{ c.active ? 'Aktif' : 'Pasif' }}
                </span>
              </td>
              <td>
                @if (c.active) {
                  <button type="button" (click)="deactivate(c.id)">Kapat</button>
                }
              </td>
            </tr>
          }
        </tbody>
      </table>
    }
  `,
  styles: [
    `
      h1 {
        margin: 0 0 24px;
      }
      table {
        width: 100%;
        background: var(--sc-surface);
        border: 1px solid var(--sc-border);
        border-radius: var(--sc-radius);
        border-collapse: collapse;
      }
      th,
      td {
        text-align: left;
        padding: 12px 16px;
        border-bottom: 1px solid var(--sc-border);
        font-size: 14px;
      }
      th {
        background: var(--sc-surface-2);
      }
      tr:last-child td {
        border-bottom: 0;
      }
      code {
        font-size: 12px;
        background: var(--sc-surface-2);
        padding: 2px 6px;
        border-radius: 4px;
      }
      .pill {
        font-size: 12px;
        padding: 4px 10px;
        border-radius: 999px;
        font-weight: 600;
      }
      .pill-success {
        background: #d1fae5;
        color: #047857;
      }
      .pill-inactive {
        background: #e5e7eb;
        color: #4b5563;
      }
      button {
        background: white;
        border: 1px solid var(--sc-border);
        padding: 6px 12px;
        border-radius: var(--sc-radius-sm);
        cursor: pointer;
        font-size: 13px;
      }
      button:hover {
        background: var(--sc-danger);
        color: white;
        border-color: var(--sc-danger);
      }
      .muted {
        color: var(--sc-text-muted);
        font-size: 12px;
      }
    `,
  ],
})
export class AdminPromotionsComponent implements OnInit {
  private readonly couponApi = inject(CouponApi);
  private readonly toast = inject(ToastService);

  readonly coupons = signal<CouponResponse[]>([]);
  readonly loading = signal(true);

  async ngOnInit(): Promise<void> {
    await this.refresh();
  }

  private async refresh(): Promise<void> {
    this.loading.set(true);
    try {
      const list = await firstValueFrom(this.couponApi.list());
      this.coupons.set(list);
    } finally {
      this.loading.set(false);
    }
  }

  async deactivate(id: string): Promise<void> {
    try {
      const updated = await firstValueFrom(this.couponApi.deactivate(id));
      this.coupons.update((arr) => arr.map((c) => (c.id === id ? updated : c)));
      this.toast.show('Kupon kapatıldı', 'success');
    } catch {
      /* error.interceptor handles toast */
    }
  }

  formatValue(c: CouponResponse): string {
    if (c.discountType === 'PERCENTAGE') return `%${c.discountValue}`;
    if (c.discountType === 'FIXED_AMOUNT') {
      return new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(c.discountValue);
    }
    return 'Ücretsiz Kargo';
  }
}
