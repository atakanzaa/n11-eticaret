import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { OrdersService } from '@core/orders.service';
import { OrderStatus } from '@core/models/order.types';
import { TPipe } from '@shared/i18n.pipe';

@Component({
  selector: 'sc-orders',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, DatePipe, TPipe],
  template: `
    <h2>{{ 'nav.orders' | t }}</h2>

    @if (orders.loading()) {
      <p class="muted">{{ 'common.loading' | t }}</p>
    } @else if (orders.orders().length === 0) {
      <p class="muted">{{ 'common.empty' | t }}</p>
    } @else {
      <ul class="list">
        @for (o of orders.orders(); track o.id) {
          <li>
            <a [routerLink]="['/hesap/siparis', o.id]">
              <header>
                <strong>#{{ o.orderNumber }}</strong>
                <span class="pill" [class]="pillClass(o.status)">{{ statusKey(o.status) | t }}</span>
              </header>
              <div class="meta">
                <span>{{ o.createdAt | date: 'short' }}</span>
                <span class="total">{{ formatPrice(o.grandTotal) }}</span>
              </div>
              <div class="muted">{{ o.items.length }} ürün</div>
            </a>
          </li>
        }
      </ul>
    }
  `,
  styles: [
    `
      h2 {
        margin: 0 0 16px;
      }
      .list {
        list-style: none;
        padding: 0;
        margin: 0;
        display: flex;
        flex-direction: column;
        gap: 8px;
      }
      .list a {
        display: block;
        background: var(--sc-surface);
        border: 1px solid var(--sc-border);
        border-radius: var(--sc-radius);
        padding: 16px;
        text-decoration: none;
        color: inherit;
      }
      .list a:hover {
        box-shadow: var(--sc-shadow);
      }
      header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 8px;
      }
      .pill {
        font-size: 12px;
        padding: 4px 10px;
        border-radius: 999px;
        font-weight: 600;
      }
      .pill-info {
        background: #dbeafe;
        color: #1d4ed8;
      }
      .pill-warn {
        background: #fef3c7;
        color: #b45309;
      }
      .pill-danger {
        background: #fee2e2;
        color: #b91c1c;
      }
      .pill-success {
        background: #d1fae5;
        color: #047857;
      }
      .meta {
        display: flex;
        justify-content: space-between;
        margin-bottom: 4px;
      }
      .total {
        font-weight: 700;
      }
      .muted {
        color: var(--sc-text-muted);
        font-size: 13px;
      }
    `,
  ],
})
export class OrdersComponent implements OnInit {
  protected readonly orders = inject(OrdersService);

  ngOnInit(): void {
    this.orders.refresh().catch(() => {});
  }

  statusKey(status: OrderStatus): string {
    return `orderStatus.${status}`;
  }

  pillClass(status: OrderStatus): string {
    return {
      PENDING: 'pill-warn',
      CONFIRMED: 'pill-info',
      CANCELLED: 'pill-danger',
      EXPIRED: 'pill-danger',
    }[status];
  }

  formatPrice(value: number): string {
    return new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(value);
  }
}
