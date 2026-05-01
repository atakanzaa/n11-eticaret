import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { OrderApi } from '@core/api/order.api';
import { ShipmentApi } from '@core/api/shipment.api';
import { PaymentApi } from '@core/api/payment.api';
import { OrderResponse, OrderStatus } from '@core/models/order.types';
import { ShipmentResponse } from '@core/models/shipment.types';
import { PaymentResponse } from '@core/models/payment.types';
import { ToastService } from '@core/toast.service';
import { I18nService } from '@core/i18n/i18n.service';
import { TPipe } from '@shared/i18n.pipe';
import { CurrencyFormatPipe } from '@shared/pipes/currency-format.pipe';
import { SpinnerComponent } from '@shared/ui/spinner/spinner.component';
import { EmptyStateComponent } from '@shared/ui/empty-state/empty-state.component';
import { StatusBadgeComponent } from '@shared/ui/status-badge/status-badge.component';
import { BreadcrumbComponent, BreadcrumbItem } from '@shared/ui/breadcrumb/breadcrumb.component';
import { TimelineComponent, TimelineEvent } from '@shared/ui/timeline/timeline.component';

@Component({
  selector: 'sc-order-detail',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './order-detail.component.html',
  styleUrls: ['./order-detail.component.scss'],
  imports: [
    RouterLink,
    DatePipe,
    TPipe,
    CurrencyFormatPipe,
    SpinnerComponent,
    EmptyStateComponent,
    StatusBadgeComponent,
    BreadcrumbComponent,
    TimelineComponent,
  ],
})
export class OrderDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly orderApi = inject(OrderApi);
  private readonly shipmentApi = inject(ShipmentApi);
  private readonly paymentApi = inject(PaymentApi);
  private readonly toast = inject(ToastService);
  private readonly i18n = inject(I18nService);

  readonly order = signal<OrderResponse | null>(null);
  readonly shipments = signal<ShipmentResponse[]>([]);
  readonly payment = signal<PaymentResponse | null>(null);
  readonly loading = signal(true);
  readonly cancelling = signal(false);

  readonly breadcrumbs = computed<BreadcrumbItem[]>(() => {
    const o = this.order();
    return [
      { label: this.i18n.t('nav.account'), route: ['/hesap'] },
      { label: this.i18n.t('nav.orders'), route: ['/hesap/siparislerim'] },
      { label: o ? `#${o.orderNumber}` : '...' },
    ];
  });

  readonly timelineEvents = computed<TimelineEvent[]>(() => {
    const o = this.order();
    if (!o) return [];

    const earlyStates: OrderStatus[] = ['CREATED', 'FRAUD_FLAGGED', 'PAYMENT_PENDING', 'PAYMENT_FAILED'];
    const confirmedOrLater: OrderStatus[] = [
      'CONFIRMED',
      'PROCESSING',
      'SHIPPED',
      'DELIVERED',
      'COMPLETED',
      'RETURN_REQUESTED',
      'REFUNDED',
    ];
    const isEarly = earlyStates.includes(o.status);
    const isConfirmed = confirmedOrLater.includes(o.status);

    const events: TimelineEvent[] = [
      {
        label: this.i18n.t('orderStatus.CREATED'),
        date: o.createdAt ?? '',
        status: 'done',
      },
      {
        label: this.i18n.t('orderStatus.CONFIRMED'),
        date: '',
        status: isConfirmed ? 'done' : isEarly ? 'active' : 'pending',
      },
    ];

    // Add shipment events if available
    const ships = this.shipments();
    if (ships.length > 0) {
      const s = ships[0];
      events.push({
        label: this.i18n.t('shipmentStatus.DISPATCHED'),
        detail: s.cargoProvider,
        date: s.dispatchedAt ?? '',
        status: s.status === 'DISPATCHED' || s.status === 'IN_TRANSIT' || s.status === 'DELIVERED' ? 'done' : 'pending',
      });
      events.push({
        label: this.i18n.t('shipmentStatus.DELIVERED'),
        date: s.deliveredAt ?? '',
        status: s.status === 'DELIVERED' ? 'done' : s.status === 'IN_TRANSIT' ? 'active' : 'pending',
      });
    }

    if (o.status === 'CANCELLED') {
      events.push({
        label: this.i18n.t('orderStatus.CANCELLED'),
        date: '',
        status: 'done',
      });
    }

    return events;
  });

  async ngOnInit(): Promise<void> {
    const id = this.route.snapshot.params['id'] as string;
    if (!id) {
      this.loading.set(false);
      return;
    }
    try {
      const order = await firstValueFrom(this.orderApi.getOrder(id));
      this.order.set(order);

      const [shipments, payment] = await Promise.allSettled([
        firstValueFrom(this.shipmentApi.byOrder(id)),
        firstValueFrom(this.paymentApi.byOrder(id)),
      ]);
      if (shipments.status === 'fulfilled') this.shipments.set(shipments.value);
      if (payment.status === 'fulfilled') this.payment.set(payment.value);
    } finally {
      this.loading.set(false);
    }
  }

  statusVariant(status: OrderStatus): 'success' | 'warning' | 'danger' | 'info' | 'neutral' {
    const map: Record<OrderStatus, 'success' | 'warning' | 'danger' | 'info' | 'neutral'> = {
      CREATED: 'info',
      FRAUD_FLAGGED: 'warning',
      PAYMENT_PENDING: 'warning',
      PAYMENT_FAILED: 'danger',
      EXPIRED: 'danger',
      CANCELLED: 'danger',
      CONFIRMED: 'info',
      PROCESSING: 'info',
      SHIPPED: 'info',
      DELIVERED: 'success',
      COMPLETED: 'success',
      RETURN_REQUESTED: 'warning',
      REFUNDED: 'neutral',
    };
    return map[status] ?? 'neutral';
  }

  canReturn(): boolean {
    const s = this.order()?.status;
    return s === 'DELIVERED' || s === 'COMPLETED';
  }

  canCancel(): boolean {
    const s = this.order()?.status;
    return s === 'CREATED' || s === 'PAYMENT_PENDING' || s === 'CONFIRMED';
  }

  async cancelOrder(): Promise<void> {
    const o = this.order();
    if (!o) return;
    this.cancelling.set(true);
    try {
      // Reload to get updated status after cancel
      const updated = await firstValueFrom(this.orderApi.getOrder(o.id));
      this.order.set(updated);
      this.toast.show(this.i18n.t('orders.cancelledSuccess'), 'success');
    } catch {
      this.toast.show(this.i18n.t('common.error'), 'danger');
    } finally {
      this.cancelling.set(false);
    }
  }
}
