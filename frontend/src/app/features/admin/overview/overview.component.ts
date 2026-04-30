import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { AdminApi } from '@core/api/admin.api';
import { AdminOverviewResponse } from '@core/models/order.types';
import { TPipe } from '@shared/i18n.pipe';

@Component({
  selector: 'sc-admin-overview',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DecimalPipe, TPipe],
  template: `
    <h1>{{ 'admin.overview' | t }}</h1>

    @if (loading()) {
      <p class="muted">{{ 'common.loading' | t }}</p>
    } @else if (data()) {
      <section class="cards">
        <article class="card">
          <span class="label">{{ 'admin.gmv30d' | t }}</span>
          <span class="value">{{ formatPrice(data()!.gmv30d) }}</span>
        </article>
        <article class="card">
          <span class="label">{{ 'admin.orders30d' | t }}</span>
          <span class="value">{{ data()!.ordersCount30d | number }}</span>
        </article>
        <article class="card">
          <span class="label">{{ 'admin.avgBasket' | t }}</span>
          <span class="value">{{ formatPrice(data()!.averageBasket) }}</span>
        </article>
        <article class="card">
          <span class="label">{{ 'admin.activeSellers' | t }}</span>
          <span class="value">{{ data()!.activeSellers30d | number }}</span>
        </article>
      </section>
    }
  `,
  styles: [
    `
      h1 {
        margin: 0 0 24px;
      }
      .cards {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
        gap: 16px;
      }
      .card {
        background: var(--sc-surface);
        border: 1px solid var(--sc-border);
        border-radius: var(--sc-radius);
        padding: 24px;
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
      .muted {
        color: var(--sc-text-muted);
      }
    `,
  ],
})
export class AdminOverviewComponent implements OnInit {
  private readonly adminApi = inject(AdminApi);

  readonly data = signal<AdminOverviewResponse | null>(null);
  readonly loading = signal(true);

  async ngOnInit(): Promise<void> {
    try {
      const data = await firstValueFrom(this.adminApi.overview());
      this.data.set(data);
    } finally {
      this.loading.set(false);
    }
  }

  formatPrice(value: number): string {
    return new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(value);
  }
}
