import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { DecimalPipe, DatePipe } from '@angular/common';
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
import { CurrencyFormatPipe } from '@shared/pipes/currency-format.pipe';
import { KpiCardComponent } from '@shared/ui/kpi-card/kpi-card.component';
import { SpinnerComponent } from '@shared/ui/spinner/spinner.component';
import { EmptyStateComponent } from '@shared/ui/empty-state/empty-state.component';
import { StatusBadgeComponent } from '@shared/ui/status-badge/status-badge.component';

@Component({
  selector: 'sc-seller-dashboard',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  imports: [
    DecimalPipe,
    DatePipe,
    TPipe,
    CurrencyFormatPipe,
    KpiCardComponent,
    SpinnerComponent,
    EmptyStateComponent,
    StatusBadgeComponent,
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

  readonly areaPoints = computed(() => {
    const lp = this.linePoints();
    return lp ? lp + ' 600,200 0,200' : '';
  });

  readonly recentRevenue = computed(() => {
    return this.revenue().slice(0, 5);
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
}
