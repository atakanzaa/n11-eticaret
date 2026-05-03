import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, distinctUntilChanged, filter, firstValueFrom, from, map, merge, of, switchMap } from 'rxjs';
import { OrderApi } from '@core/api/order.api';
import { ShipmentApi } from '@core/api/shipment.api';
import { PaymentApi } from '@core/api/payment.api';
import { OrderResponse, OrderStatus } from '@core/models/order.types';
import { ShipmentResponse } from '@core/models/shipment.types';
import { PaymentResponse, PaymentStatus } from '@core/models/payment.types';
import { ToastService } from '@core/toast.service';
import { I18nService } from '@core/i18n/i18n.service';
import { TPipe } from '@shared/i18n.pipe';
import { CurrencyFormatPipe } from '@shared/pipes/currency-format.pipe';
import { SpinnerComponent } from '@shared/ui/spinner/spinner.component';
import { EmptyStateComponent } from '@shared/ui/empty-state/empty-state.component';
import { StatusBadgeComponent } from '@shared/ui/status-badge/status-badge.component';
import { BreadcrumbComponent, BreadcrumbItem } from '@shared/ui/breadcrumb/breadcrumb.component';
import { TimelineComponent, TimelineEvent } from '@shared/ui/timeline/timeline.component';
import { ConfirmDialogComponent } from '@shared/ui/confirm-dialog/confirm-dialog.component';

type OrderDetailViewState = {
  loading: boolean;
  order: OrderResponse | null;
  shipments: ShipmentResponse[];
  payment: PaymentResponse | null;
  error: boolean;
};

const EMPTY_STATE: OrderDetailViewState = {
  loading: true,
  order: null,
  shipments: [],
  payment: null,
  error: false,
};

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
    ConfirmDialogComponent,
  ],
})
export class OrderDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly orderApi = inject(OrderApi);
  private readonly shipmentApi = inject(ShipmentApi);
  private readonly paymentApi = inject(PaymentApi);
  private readonly toast = inject(ToastService);
  private readonly i18n = inject(I18nService);

  readonly viewState = signal<OrderDetailViewState>(EMPTY_STATE);
  readonly order = computed(() => this.viewState().order);
  readonly shipments = computed(() => this.viewState().shipments);
  readonly payment = computed(() => this.viewState().payment);
  readonly loading = computed(() => this.viewState().loading);
  readonly hasLoadError = computed(() => this.viewState().error);
  readonly cancelling = signal(false);
  readonly showCancelConfirm = signal(false);
  readonly confirmingReceipt = signal(false);
  readonly showReceiveConfirm = signal(false);

  constructor() {
    this.route.paramMap.pipe(
      map(params => params.get('id')),
      filter((id): id is string => !!id),
      distinctUntilChanged(),
      switchMap(id =>
        merge(
          of<OrderDetailViewState>({ ...EMPTY_STATE }),
          from(this.loadState(id)).pipe(
            catchError(() => of<OrderDetailViewState>({
              ...EMPTY_STATE,
              loading: false,
              error: true,
            })),
          ),
        ),
      ),
      takeUntilDestroyed(inject(DestroyRef)),
    ).subscribe(state => this.viewState.set(state));
  }

  private async loadState(id: string): Promise<OrderDetailViewState> {
    const order = await firstValueFrom(this.orderApi.getOrder(id));
    const [shipments, payment] = await Promise.allSettled([
      firstValueFrom(this.shipmentApi.byOrder(id)),
      firstValueFrom(this.paymentApi.byOrder(id)),
    ]);

    return {
      loading: false,
      order,
      shipments: shipments.status === 'fulfilled' ? shipments.value : [],
      payment: payment.status === 'fulfilled' ? payment.value : null,
      error: false,
    };
  }

  readonly displayShipments = computed<ShipmentResponse[]>(() =>
    this.shipments().filter(s => !!(s.recipientFullName || s.city || s.district)),
  );

  readonly estimatedEta = computed<string | null>(() => {
    const list = this.shipments();
    return list.length > 0 ? list[0].estimatedDeliveryDate ?? null : null;
  });

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

    // Cancelled orders get their own minimal timeline
    if (o.status === 'CANCELLED' || o.status === 'EXPIRED' || o.status === 'PAYMENT_FAILED') {
      return [
        {
          label: this.i18n.t('orderStatus.CREATED'),
          date: o.createdAt ?? '',
          status: 'done',
        },
        {
          label: this.i18n.t('orderStatus.' + o.status),
          date: '',
          status: 'done',
        },
      ];
    }

    // Status progression buckets: an order has reached step N if its status
    // matches any in the corresponding bucket.
    const confirmedStates = new Set<OrderStatus>([
      'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'COMPLETED',
      'RETURN_REQUESTED', 'REFUNDED',
    ]);
    const dispatchedStates = new Set<OrderStatus>([
      'SHIPPED', 'DELIVERED', 'COMPLETED', 'RETURN_REQUESTED', 'REFUNDED',
    ]);
    const deliveredStates = new Set<OrderStatus>([
      'DELIVERED', 'COMPLETED', 'RETURN_REQUESTED', 'REFUNDED',
    ]);

    const isConfirmed = confirmedStates.has(o.status);
    const isDispatched = dispatchedStates.has(o.status) || this.shipments().some(s =>
      s.status === 'DISPATCHED' || s.status === 'IN_TRANSIT' ||
      s.status === 'OUT_FOR_DELIVERY' || s.status === 'DELIVERED');
    const isDelivered = deliveredStates.has(o.status) || this.shipments().some(s => s.status === 'DELIVERED');

    const ship = this.shipments()[0];

    // Always show 4-step timeline so users always see the full journey,
    // even when the order is freshly created or shipment data is missing.
    return [
      {
        label: this.i18n.t('orderStatus.CREATED'),
        date: o.createdAt ?? '',
        status: 'done',
      },
      {
        label: this.i18n.t('orderStatus.CONFIRMED'),
        date: '',
        status: isConfirmed ? 'done' : 'active',
      },
      {
        label: this.i18n.t('shipmentStatus.DISPATCHED'),
        detail: ship?.cargoProvider,
        date: ship?.dispatchedAt ?? '',
        status: isDispatched ? 'done' : isConfirmed ? 'active' : 'pending',
      },
      {
        label: this.i18n.t('shipmentStatus.DELIVERED'),
        date: ship?.deliveredAt ?? '',
        status: isDelivered ? 'done' : isDispatched ? 'active' : 'pending',
      },
    ];
  });

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

  paymentVariant(status: PaymentStatus): 'success' | 'warning' | 'danger' | 'info' | 'neutral' {
    const map: Record<PaymentStatus, 'success' | 'warning' | 'danger' | 'info' | 'neutral'> = {
      INITIATED: 'info',
      THREEDS_PENDING: 'warning',
      THREEDS_AUTHENTICATED: 'info',
      CAPTURING: 'info',
      SUCCEEDED: 'success',
      FAILED: 'danger',
      CANCELLED: 'danger',
      REFUND_REQUESTED: 'warning',
      PARTIALLY_REFUNDED: 'neutral',
      REFUNDED: 'neutral',
    };
    return map[status] ?? 'neutral';
  }

  canReturn(): boolean {
    const s = this.order()?.status;
    return s === 'DELIVERED' || s === 'COMPLETED';
  }

  canReviewItem(): boolean {
    const s = this.order()?.status;
    return s === 'DELIVERED' || s === 'COMPLETED';
  }

  hasShippingAddress(): boolean {
    return this.displayShipments().length > 0;
  }

  /** Only show payment card when there's something concrete beyond status to display. */
  hasPaymentInfo(): boolean {
    const p = this.payment();
    if (!p) return false;
    return !!(p.cardLastFour || (p.installment && p.installment > 1) || p.paidAmount);
  }

  canCancel(): boolean {
    const s = this.order()?.status;
    return s === 'CREATED' || s === 'PAYMENT_PENDING' || s === 'CONFIRMED';
  }

  /**
   * Customer-driven delivery confirmation. Cargo is mocked, so the user has
   * to manually mark the order received before they can leave a review.
   * Shows for CONFIRMED/PROCESSING/SHIPPED — covers the case where the mock
   * shipment never reaches SHIPPED on its own.
   */
  canMarkReceived(): boolean {
    const s = this.order()?.status;
    return s === 'CONFIRMED' || s === 'PROCESSING' || s === 'SHIPPED';
  }

  openCancelConfirm(): void {
    this.showCancelConfirm.set(true);
  }

  async cancelOrder(): Promise<void> {
    const o = this.order();
    if (!o) {
      this.showCancelConfirm.set(false);
      return;
    }
    this.showCancelConfirm.set(false);
    this.cancelling.set(true);
    try {
      const updated = await firstValueFrom(this.orderApi.cancelOrder(o.id));
      this.viewState.update(state => ({ ...state, order: updated }));
      this.toast.show(this.i18n.t('orders.cancelledSuccess'), 'success');
    } catch (err: unknown) {
      const status = (err as { status?: number })?.status;
      if (status === 409) {
        this.toast.show(this.i18n.t('orders.cancelNotAllowed'), 'danger');
      } else {
        this.toast.show(this.i18n.t('common.error'), 'danger');
      }
    } finally {
      this.cancelling.set(false);
    }
  }

  openReceiveConfirm(): void {
    this.showReceiveConfirm.set(true);
  }

  async confirmReceived(): Promise<void> {
    const o = this.order();
    if (!o) {
      this.showReceiveConfirm.set(false);
      return;
    }
    this.showReceiveConfirm.set(false);
    this.confirmingReceipt.set(true);
    try {
      const updated = await firstValueFrom(this.orderApi.confirmReceived(o.id));
      this.viewState.update(state => ({ ...state, order: updated }));
      this.toast.show(this.i18n.t('orders.receivedSuccess'), 'success');
    } catch (err: unknown) {
      const status = (err as { status?: number })?.status;
      if (status === 409) {
        this.toast.show(this.i18n.t('orders.receiveNotAllowed'), 'danger');
      } else {
        this.toast.show(this.i18n.t('common.error'), 'danger');
      }
    } finally {
      this.confirmingReceipt.set(false);
    }
  }
}
