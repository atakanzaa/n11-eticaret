import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { OrderApi } from '@core/api/order.api';
import { ShipmentApi } from '@core/api/shipment.api';
import { OrderResponse } from '@core/models/order.types';
import { ShipmentResponse } from '@core/models/shipment.types';
import { TPipe } from '@shared/i18n.pipe';

@Component({
  selector: 'sc-order-success',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, TPipe],
  template: `
    <div class="card">
      @if (loading()) {
        <p class="muted">{{ 'common.loading' | t }}</p>
      } @else if (order()) {
        <h2>✓ {{ 'checkout.paymentSuccess' | t }}</h2>
        <p>{{ 'checkout.orderNumber' | t }}: <strong>#{{ order()!.orderNumber }}</strong></p>
        <p class="total">{{ formatPrice(order()!.grandTotal) }}</p>

        @if (shipments().length > 0) {
          <p class="muted">
            {{ 'checkout.estimatedDelivery' | t }}: {{ shipments()[0].cargoProvider }}
            @if (shipments()[0].trackingNumber) {
              · {{ shipments()[0].trackingNumber }}
            }
          </p>
        }

        <div class="actions">
          <a [routerLink]="['/hesap/siparis', order()!.id]" class="primary">{{ 'nav.orders' | t }}</a>
          <a [routerLink]="['/']" class="secondary">{{ 'checkout.backToShopping' | t }}</a>
        </div>
      }
    </div>
  `,
  styles: [
    `
      .card {
        max-width: 540px;
        margin: 64px auto;
        background: var(--sc-surface);
        border: 1px solid var(--sc-border);
        border-radius: var(--sc-radius);
        padding: 40px;
        text-align: center;
      }
      h2 {
        color: var(--sc-success);
        margin: 0 0 24px;
      }
      .total {
        font-size: 24px;
        font-weight: 700;
        color: var(--sc-primary);
      }
      .muted {
        color: var(--sc-text-muted);
      }
      .actions {
        display: flex;
        gap: 12px;
        justify-content: center;
        margin-top: 24px;
      }
      .primary,
      .secondary {
        padding: 12px 20px;
        border-radius: var(--sc-radius);
        text-decoration: none;
        font-weight: 600;
      }
      .primary {
        background: var(--sc-primary);
        color: white;
      }
      .secondary {
        background: var(--sc-surface-2);
        color: var(--sc-text);
      }
    `,
  ],
})
export class OrderSuccessComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly orderApi = inject(OrderApi);
  private readonly shipmentApi = inject(ShipmentApi);

  readonly order = signal<OrderResponse | null>(null);
  readonly shipments = signal<ShipmentResponse[]>([]);
  readonly loading = signal(true);

  async ngOnInit(): Promise<void> {
    const id = this.route.snapshot.params['id'] as string;
    if (!id) {
      this.loading.set(false);
      return;
    }
    try {
      const order = await firstValueFrom(this.orderApi.getOrder(id));
      this.order.set(order);
      try {
        const shipments = await firstValueFrom(this.shipmentApi.byOrder(id));
        this.shipments.set(shipments);
      } catch {
        /* shipments may not exist yet */
      }
    } finally {
      this.loading.set(false);
    }
  }

  formatPrice(value: number): string {
    return new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(value);
  }
}
