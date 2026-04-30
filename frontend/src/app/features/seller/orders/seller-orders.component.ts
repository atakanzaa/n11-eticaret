import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { SellerApi } from '@core/api/seller.api';
import { OrderApi } from '@core/api/order.api';
import { SellerDto, SellerOrderKpiResponse, RevenuePoint } from '@core/models/seller.types';
import { TPipe } from '@shared/i18n.pipe';

/**
 * Seller's own orders view. The backend doesn't ship a dedicated
 * "list-orders-for-this-seller" endpoint (a marketplace order has multiple
 * sellers per line), so this view re-uses the seller-side KPI/revenue
 * endpoints we already built and shows a per-day breakdown. For a richer
 * list we'd add `GET /api/orders/seller/{id}/items` later.
 */
@Component({
  selector: 'sc-seller-orders',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, TPipe],
  template: `
    <h1>{{ 'seller.orders' | t }}</h1>

    @if (loading()) {
      <p class="muted">{{ 'common.loading' | t }}</p>
    } @else {
      <section class="cards">
        <article class="card">
          <span class="label">{{ 'seller.todayRevenue' | t }}</span>
          <span class="value">{{ formatPrice(kpi()?.todayRevenue ?? 0) }}</span>
        </article>
        <article class="card">
          <span class="label">{{ 'seller.pendingOrders' | t }}</span>
          <span class="value">{{ kpi()?.pendingOrdersCount ?? 0 }}</span>
        </article>
        <article class="card">
          <span class="label">Son 7 Gün Cirosu</span>
          <span class="value">{{ formatPrice(kpi()?.last7DaysRevenue ?? 0) }}</span>
        </article>
      </section>

      @if (revenue().length > 0) {
        <table class="table">
          <thead>
            <tr>
              <th>Tarih</th>
              <th>Ciro</th>
              <th>Sipariş</th>
            </tr>
          </thead>
          <tbody>
            @for (p of revenue(); track p.date) {
              <tr>
                <td>{{ p.date | date: 'mediumDate' }}</td>
                <td>{{ formatPrice(p.amount) }}</td>
                <td>{{ p.orderCount }}</td>
              </tr>
            }
          </tbody>
        </table>
      }
    }
  `,
  styles: [
    `
      h1 {
        margin: 0 0 24px;
      }
      .cards {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
        gap: 16px;
        margin-bottom: 32px;
      }
      .card {
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
        font-size: 24px;
        font-weight: 700;
        color: var(--sc-primary);
      }
      .table {
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
      }
      th {
        background: var(--sc-surface-2);
      }
      tr:last-child td {
        border-bottom: 0;
      }
      .muted {
        color: var(--sc-text-muted);
      }
    `,
  ],
})
export class SellerOrdersComponent implements OnInit {
  private readonly sellerApi = inject(SellerApi);
  private readonly orderApi = inject(OrderApi);

  readonly seller = signal<SellerDto | null>(null);
  readonly kpi = signal<SellerOrderKpiResponse | null>(null);
  readonly revenue = signal<RevenuePoint[]>([]);
  readonly loading = signal(true);

  async ngOnInit(): Promise<void> {
    try {
      const seller = await firstValueFrom(this.sellerApi.me());
      this.seller.set(seller);
      const [kpi, revenue] = await Promise.allSettled([
        firstValueFrom(this.orderApi.sellerKpi(seller.id)),
        firstValueFrom(this.orderApi.sellerRevenue(seller.id, 14)),
      ]);
      if (kpi.status === 'fulfilled') this.kpi.set(kpi.value);
      if (revenue.status === 'fulfilled') {
        this.revenue.set(revenue.value.slice().reverse());
      }
    } finally {
      this.loading.set(false);
    }
  }

  formatPrice(value: number): string {
    return new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(value);
  }
}
