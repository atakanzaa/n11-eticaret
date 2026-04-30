import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { SellerApi } from '@core/api/seller.api';
import { OrderApi } from '@core/api/order.api';
import { InventoryApi } from '@core/api/inventory.api';
import {
  RevenuePoint,
  SellerDto,
  SellerInventoryStatsResponse,
  SellerOrderKpiResponse,
} from '@core/models/seller.types';
import { TPipe } from '@shared/i18n.pipe';

/**
 * Seller dashboard. Reads four real backend endpoints in parallel and
 * displays KPI cards plus a 30-day revenue line chart drawn as inline SVG
 * (no chart library — keeps the bundle thin and lets us hand-tune the look).
 */
@Component({
  selector: 'sc-seller-dashboard',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DecimalPipe, TPipe],
  template: `
    @if (loading()) {
      <p class="muted">{{ 'common.loading' | t }}</p>
    } @else {
      <header>
        <h1>{{ seller()?.storeName ?? ('seller.dashboard' | t) }}</h1>
        <span class="muted">★ {{ seller()?.rating ?? 0 | number }} · {{ seller()?.ratingCount ?? 0 }} değerlendirme</span>
      </header>

      <section class="kpi-grid">
        <article class="kpi">
          <span class="label">{{ 'seller.todayRevenue' | t }}</span>
          <span class="value">{{ formatPrice(orderKpi()?.todayRevenue ?? 0) }}</span>
        </article>
        <article class="kpi">
          <span class="label">{{ 'seller.pendingOrders' | t }}</span>
          <span class="value">{{ orderKpi()?.pendingOrdersCount ?? 0 }}</span>
        </article>
        <article class="kpi">
          <span class="label">{{ 'seller.lowStock' | t }}</span>
          <span class="value">{{ inventoryStats()?.lowStockCount ?? 0 }}</span>
        </article>
        <article class="kpi">
          <span class="label">{{ 'seller.rating' | t }}</span>
          <span class="value">★ {{ seller()?.rating ?? 0 | number: '1.1-1' }}</span>
        </article>
      </section>

      @if (revenue().length > 0) {
        <section class="chart">
          <h3>{{ 'seller.last30DaysRevenue' | t }}</h3>
          <svg viewBox="0 0 600 200" class="line">
            <polyline [attr.points]="linePoints()" fill="none" stroke="var(--sc-primary)" stroke-width="2" />
          </svg>
        </section>
      }
    }
  `,
  styles: [
    `
      header {
        display: flex;
        justify-content: space-between;
        align-items: baseline;
        margin-bottom: 24px;
      }
      h1 {
        margin: 0;
      }
      .kpi-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
        gap: 16px;
        margin-bottom: 32px;
      }
      .kpi {
        background: var(--sc-surface);
        border: 1px solid var(--sc-border);
        border-radius: var(--sc-radius);
        padding: 20px;
        display: flex;
        flex-direction: column;
        gap: 8px;
      }
      .label {
        color: var(--sc-text-muted);
        font-size: 13px;
        text-transform: uppercase;
        letter-spacing: 0.05em;
      }
      .value {
        font-size: 28px;
        font-weight: 700;
        color: var(--sc-primary);
      }
      .chart {
        background: var(--sc-surface);
        border: 1px solid var(--sc-border);
        border-radius: var(--sc-radius);
        padding: 20px;
      }
      .chart h3 {
        margin: 0 0 16px;
      }
      .line {
        width: 100%;
        height: auto;
      }
      .muted {
        color: var(--sc-text-muted);
      }
    `,
  ],
})
export class SellerDashboardComponent implements OnInit {
  private readonly sellerApi = inject(SellerApi);
  private readonly orderApi = inject(OrderApi);
  private readonly inventoryApi = inject(InventoryApi);

  readonly seller = signal<SellerDto | null>(null);
  readonly orderKpi = signal<SellerOrderKpiResponse | null>(null);
  readonly inventoryStats = signal<SellerInventoryStatsResponse | null>(null);
  readonly revenue = signal<RevenuePoint[]>([]);
  readonly loading = signal(true);

  readonly linePoints = computed(() => {
    const data = this.revenue();
    if (data.length === 0) return '';
    const max = Math.max(...data.map((p) => p.amount), 1);
    return data
      .map((p, i) => {
        const x = (i / Math.max(data.length - 1, 1)) * 600;
        const y = 200 - (p.amount / max) * 180 - 10;
        return `${x.toFixed(0)},${y.toFixed(0)}`;
      })
      .join(' ');
  });

  async ngOnInit(): Promise<void> {
    try {
      const seller = await firstValueFrom(this.sellerApi.me());
      this.seller.set(seller);

      const [orderKpi, invStats, revenue] = await Promise.allSettled([
        firstValueFrom(this.orderApi.sellerKpi(seller.id)),
        firstValueFrom(this.inventoryApi.sellerStats(seller.id)),
        firstValueFrom(this.orderApi.sellerRevenue(seller.id, 30)),
      ]);
      if (orderKpi.status === 'fulfilled') this.orderKpi.set(orderKpi.value);
      if (invStats.status === 'fulfilled') this.inventoryStats.set(invStats.value);
      if (revenue.status === 'fulfilled') this.revenue.set(revenue.value);
    } finally {
      this.loading.set(false);
    }
  }

  formatPrice(value: number): string {
    return new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(value);
  }
}
