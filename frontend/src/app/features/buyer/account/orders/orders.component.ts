import { ChangeDetectionStrategy, Component, OnInit, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { OrdersService } from '@core/orders.service';
import { OrderStatus } from '@core/models/order.types';
import { TPipe } from '@shared/i18n.pipe';
import { CurrencyFormatPipe } from '@shared/pipes/currency-format.pipe';
import { SpinnerComponent } from '@shared/ui/spinner/spinner.component';
import { EmptyStateComponent } from '@shared/ui/empty-state/empty-state.component';
import { StatusBadgeComponent } from '@shared/ui/status-badge/status-badge.component';
import { PaginationComponent } from '@shared/ui/pagination/pagination.component';

@Component({
  selector: 'sc-orders',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './orders.component.html',
  styleUrls: ['./orders.component.scss'],
  imports: [
    RouterLink,
    DatePipe,
    TPipe,
    CurrencyFormatPipe,
    SpinnerComponent,
    EmptyStateComponent,
    StatusBadgeComponent,
    PaginationComponent,
  ],
})
export class OrdersComponent implements OnInit {
  protected readonly orders = inject(OrdersService);

  readonly pageData = computed(() => this.orders.page());

  ngOnInit(): void {
    this.orders.refresh().catch(() => {});
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

  onPageChange(page: number): void {
    this.orders.refresh(page).catch(() => {});
  }
}
