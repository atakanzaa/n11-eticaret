import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { SellerApi } from '@core/api/seller.api';
import { OrderApi } from '@core/api/order.api';
import { SellerDto, SellerOrderKpiResponse, RevenuePoint } from '@core/models/seller.types';
import { OrderResponse } from '@core/models/order.types';
import { ToastService } from '@core/toast.service';
import { TPipe } from '@shared/i18n.pipe';
import { SpinnerComponent } from '@shared/ui/spinner/spinner.component';
import { KpiCardComponent } from '@shared/ui/kpi-card/kpi-card.component';
import { CurrencyFormatPipe } from '@shared/pipes/currency-format.pipe';
import { StatusBadgeComponent } from '@shared/ui/status-badge/status-badge.component';

@Component({
  selector: 'sc-seller-orders',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './seller-orders.component.html',
  styleUrls: ['./seller-orders.component.scss'],
  imports: [
    DatePipe, TPipe, SpinnerComponent, KpiCardComponent, CurrencyFormatPipe, StatusBadgeComponent,
  ],
})
export class SellerOrdersComponent implements OnInit {
  private readonly sellerApi = inject(SellerApi);
  private readonly orderApi = inject(OrderApi);
  private readonly toast = inject(ToastService);

  readonly seller = signal<SellerDto | null>(null);
  readonly kpi = signal<SellerOrderKpiResponse | null>(null);
  readonly revenue = signal<RevenuePoint[]>([]);
  readonly orders = signal<OrderResponse[]>([]);
  readonly loading = signal(true);
  readonly shippingId = signal<string | null>(null);

  async ngOnInit(): Promise<void> {
    try {
      const seller = await firstValueFrom(this.sellerApi.me());
      this.seller.set(seller);
      const [kpi, orders] = await Promise.allSettled([
        firstValueFrom(this.orderApi.sellerKpi(seller.id)),
        firstValueFrom(this.orderApi.sellerOrders(seller.id, 0, 50)),
      ]);
      if (kpi.status === 'fulfilled') this.kpi.set(kpi.value);
      if (orders.status === 'fulfilled') {
        this.orders.set(orders.value.content);
      }
    } finally {
      this.loading.set(false);
    }
  }

  formatPrice(value: number): string {
    return new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(value);
  }

  canShip(status: string): boolean {
    return status === 'CONFIRMED' || status === 'PROCESSING';
  }

  statusVariant(status: string): 'success' | 'warning' | 'danger' | 'neutral' {
    const map: Record<string, 'success' | 'warning' | 'danger' | 'neutral'> = {
      CONFIRMED: 'success',
      PROCESSING: 'warning',
      SHIPPED: 'success',
      DELIVERED: 'success',
      COMPLETED: 'success',
      CANCELLED: 'danger',
      EXPIRED: 'danger',
      PAYMENT_FAILED: 'danger',
      PAYMENT_PENDING: 'warning',
      FRAUD_FLAGGED: 'danger',
    };
    return map[status] ?? 'neutral';
  }

  async ship(order: OrderResponse): Promise<void> {
    const sellerId = this.seller()?.id;
    if (!sellerId) return;
    this.shippingId.set(order.id);
    try {
      const updated = await firstValueFrom(this.orderApi.markShipped(order.id, sellerId));
      this.orders.update(list => list.map(o => o.id === order.id ? updated : o));
      this.toast.show('Sipariş kargoya verildi olarak işaretlendi', 'success');
    } catch {
      /* error.interceptor handles toast */
    } finally {
      this.shippingId.set(null);
    }
  }
}
