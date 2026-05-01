import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { OrderApi } from '@core/api/order.api';
import { ReturnApi } from '@core/api/return.api';
import { OrderResponse } from '@core/models/order.types';
import { ReturnReasonCode, ReturnResponse, ReturnStatus } from '@core/models/return.types';
import { ToastService } from '@core/toast.service';
import { I18nService } from '@core/i18n/i18n.service';
import { TPipe } from '@shared/i18n.pipe';
import { CurrencyFormatPipe } from '@shared/pipes/currency-format.pipe';
import { SpinnerComponent } from '@shared/ui/spinner/spinner.component';
import { EmptyStateComponent } from '@shared/ui/empty-state/empty-state.component';
import { StatusBadgeComponent } from '@shared/ui/status-badge/status-badge.component';
import { BreadcrumbComponent, BreadcrumbItem } from '@shared/ui/breadcrumb/breadcrumb.component';
import { FormFieldComponent } from '@shared/ui/form-field/form-field.component';

@Component({
  selector: 'sc-return',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './return.component.html',
  styleUrls: ['./return.component.scss'],
  imports: [
    RouterLink,
    FormsModule,
    TPipe,
    CurrencyFormatPipe,
    SpinnerComponent,
    EmptyStateComponent,
    StatusBadgeComponent,
    BreadcrumbComponent,
    FormFieldComponent,
  ],
})
export class ReturnComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly orderApi = inject(OrderApi);
  private readonly returnApi = inject(ReturnApi);
  private readonly toast = inject(ToastService);
  private readonly i18n = inject(I18nService);

  readonly reasons: ReturnReasonCode[] = [
    'BUYER_REQUEST',
    'DAMAGED',
    'WRONG_ITEM',
    'NOT_AS_DESCRIBED',
    'DEFECTIVE',
    'OTHER',
  ];

  readonly order = signal<OrderResponse | null>(null);
  readonly returnRecord = signal<ReturnResponse | null>(null);
  readonly loading = signal(true);
  readonly submitting = signal(false);
  readonly selectedItems = signal<boolean[]>([]);

  selectedReason: ReturnReasonCode | null = null;
  description = '';

  readonly breadcrumbs = computed<BreadcrumbItem[]>(() => {
    const o = this.order();
    return [
      { label: this.i18n.t('nav.account'), route: ['/hesap'] },
      { label: this.i18n.t('nav.orders'), route: ['/hesap/siparislerim'] },
      { label: o ? `#${o.orderNumber}` : '...', route: o ? ['/hesap/siparis', o.id] : undefined },
      { label: this.i18n.t('return.startReturn') },
    ];
  });

  readonly canSubmit = computed(() => {
    const items = this.selectedItems();
    return this.selectedReason !== null && items.some(Boolean);
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
      this.selectedItems.set(order.items.map(() => true));
    } finally {
      this.loading.set(false);
    }
  }

  toggleItem(index: number): void {
    this.selectedItems.update((arr) => {
      const copy = [...arr];
      copy[index] = !copy[index];
      return copy;
    });
  }

  returnStatusVariant(status: ReturnStatus): 'success' | 'warning' | 'danger' | 'info' | 'neutral' {
    const map: Record<ReturnStatus, 'success' | 'warning' | 'danger' | 'info' | 'neutral'> = {
      REQUESTED: 'warning',
      APPROVED: 'info',
      REJECTED: 'danger',
      REFUND_PROCESSING: 'info',
      REFUND_FAILED: 'danger',
      REFUNDED: 'success',
      INVENTORY_RESTOCKED: 'info',
      COMPLETED: 'success',
      CANCELLED: 'danger',
    };
    return map[status] ?? 'neutral';
  }

  async submit(): Promise<void> {
    const order = this.order();
    if (!order || !this.selectedReason) return;

    const selectedFlags = this.selectedItems();
    const items = order.items
      .filter((_, i) => selectedFlags[i])
      .map((item) => ({ offerId: item.offerId, quantity: item.quantity }));

    if (items.length === 0) return;

    this.submitting.set(true);
    try {
      const result = await firstValueFrom(
        this.returnApi.create({
          orderId: order.id,
          reasonCode: this.selectedReason,
          reasonDescription: this.description.trim() || undefined,
          items,
        }),
      );
      this.returnRecord.set(result);
      this.toast.show(this.i18n.t('returnStatus.REQUESTED'), 'success');
    } catch {
      this.toast.show(this.i18n.t('common.error'), 'danger');
    } finally {
      this.submitting.set(false);
    }
  }
}
