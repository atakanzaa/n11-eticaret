import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { OrderApi } from '@core/api/order.api';
import { ShipmentApi } from '@core/api/shipment.api';
import { PaymentApi } from '@core/api/payment.api';
import { OrderResponse } from '@core/models/order.types';
import { ShipmentResponse } from '@core/models/shipment.types';
import { PaymentResponse } from '@core/models/payment.types';
import { TPipe } from '@shared/i18n.pipe';

@Component({
  selector: 'sc-order-detail',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, DatePipe, TPipe],
  template: `
    @if (loading()) {
      <p class="muted">{{ 'common.loading' | t }}</p>
    } @else if (order()) {
      <header class="head">
        <a [routerLink]="['/hesap/siparislerim']" class="back">← {{ 'common.back' | t }}</a>
        <h1>#{{ order()!.orderNumber }}</h1>
        <span class="pill">{{ 'orderStatus.' + order()!.status | t }}</span>
      </header>

      <section class="card">
        <h3>Ürünler</h3>
        @for (item of order()!.items; track item.id) {
          <div class="line">
            <span>{{ item.productTitle }}</span>
            <span class="muted">{{ item.quantity }} adet</span>
            <span class="total">{{ formatPrice(item.lineTotal) }}</span>
          </div>
        }
        <div class="grand">
          <strong>{{ 'common.total' | t }}</strong>
          <strong>{{ formatPrice(order()!.grandTotal) }}</strong>
        </div>
      </section>

      @if (shipments().length > 0) {
        <section class="card">
          <h3>Kargo</h3>
          @for (s of shipments(); track s.id) {
            <article class="shipment">
              <header>
                <strong>{{ s.cargoProvider }}</strong>
                @if (s.trackingNumber) {
                  <code>{{ s.trackingNumber }}</code>
                }
                <span class="pill" [class]="shipmentPill(s.status)">{{ 'shipmentStatus.' + s.status | t }}</span>
              </header>
              <div class="muted">
                {{ s.recipientFullName }} — {{ s.district }}, {{ s.city }}
              </div>
              @if (s.estimatedDeliveryDate) {
                <div class="muted">Tahmini Teslimat: {{ s.estimatedDeliveryDate | date: 'mediumDate' }}</div>
              }
            </article>
          }
        </section>
      }

      @if (payment()) {
        <section class="card">
          <h3>Ödeme</h3>
          <div class="muted">{{ 'paymentStatus.' + payment()!.status | t }}</div>
          @if (payment()!.cardLastFour) {
            <div class="muted">{{ payment()!.cardBrand }} **** {{ payment()!.cardLastFour }}</div>
          }
        </section>
      }

      @if (canReturn()) {
        <a [routerLink]="['/hesap/iade', order()!.id]" class="return-link">İade Başlat →</a>
      }
    } @else {
      <p class="muted">Sipariş bulunamadı.</p>
    }
  `,
  styles: [
    `
      .head {
        display: flex;
        align-items: center;
        gap: 16px;
        margin-bottom: 24px;
      }
      .back {
        color: var(--sc-text-muted);
        text-decoration: none;
      }
      .head h1 {
        margin: 0;
        font-size: 22px;
        flex: 1;
      }
      .pill {
        font-size: 12px;
        padding: 4px 10px;
        border-radius: 999px;
        font-weight: 600;
        background: var(--sc-surface-2);
      }
      .pill-success {
        background: #d1fae5;
        color: #047857;
      }
      .pill-warn {
        background: #fef3c7;
        color: #b45309;
      }
      .pill-info {
        background: #dbeafe;
        color: #1d4ed8;
      }
      .card {
        background: var(--sc-surface);
        border: 1px solid var(--sc-border);
        border-radius: var(--sc-radius);
        padding: 20px;
        margin-bottom: 16px;
      }
      .card h3 {
        margin: 0 0 12px;
      }
      .line {
        display: grid;
        grid-template-columns: 1fr auto auto;
        gap: 16px;
        padding: 8px 0;
        border-bottom: 1px solid var(--sc-border);
      }
      .line:last-of-type {
        border-bottom: 0;
      }
      .total {
        font-weight: 700;
      }
      .grand {
        display: flex;
        justify-content: space-between;
        margin-top: 12px;
        padding-top: 12px;
        border-top: 2px solid var(--sc-border);
        font-size: 18px;
      }
      .shipment {
        padding: 12px 0;
        border-bottom: 1px solid var(--sc-border);
      }
      .shipment:last-child {
        border-bottom: 0;
      }
      .shipment header {
        display: flex;
        gap: 12px;
        align-items: center;
        margin-bottom: 4px;
      }
      code {
        background: var(--sc-surface-2);
        padding: 2px 6px;
        border-radius: 4px;
        font-size: 12px;
      }
      .return-link {
        display: inline-block;
        margin-top: 16px;
        color: var(--sc-primary);
        text-decoration: none;
        font-weight: 600;
      }
      .muted {
        color: var(--sc-text-muted);
      }
    `,
  ],
})
export class OrderDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly orderApi = inject(OrderApi);
  private readonly shipmentApi = inject(ShipmentApi);
  private readonly paymentApi = inject(PaymentApi);

  readonly order = signal<OrderResponse | null>(null);
  readonly shipments = signal<ShipmentResponse[]>([]);
  readonly payment = signal<PaymentResponse | null>(null);
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

      // Shipments + payment can each fail independently — surface what we can.
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

  canReturn(): boolean {
    const o = this.order();
    return o?.status === 'CONFIRMED';
  }

  shipmentPill(status: string): string {
    return {
      PENDING: 'pill-warn',
      DISPATCHED: 'pill-info',
      IN_TRANSIT: 'pill-info',
      DELIVERED: 'pill-success',
      FAILED: 'pill-warn',
    }[status] ?? '';
  }

  formatPrice(value: number): string {
    return new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(value);
  }
}
