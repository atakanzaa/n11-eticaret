import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { OrderApi } from '@core/api/order.api';
import { ReturnApi } from '@core/api/return.api';
import { OrderResponse } from '@core/models/order.types';
import { ReturnReasonCode, ReturnResponse } from '@core/models/return.types';
import { ToastService } from '@core/toast.service';
import { I18nService } from '@core/i18n/i18n.service';
import { TPipe } from '@shared/i18n.pipe';

@Component({
  selector: 'sc-return',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, FormsModule, TPipe],
  template: `
    @if (loading()) {
      <p class="muted">{{ 'common.loading' | t }}</p>
    } @else if (returnRecord()) {
      <article class="card">
        <h2>İade Talebi #{{ returnRecord()!.returnNumber }}</h2>
        <p class="status">{{ 'returnStatus.' + returnRecord()!.status | t }}</p>
        <a [routerLink]="['/hesap/siparis', returnRecord()!.orderId]">← Siparişe dön</a>
      </article>
    } @else if (order()) {
      <article class="card">
        <h2>İade Başlat</h2>
        <p class="muted">Sipariş #{{ order()!.orderNumber }}</p>

        <fieldset>
          <legend>{{ 'product.reviews' | t }}</legend>
          @for (reason of reasons; track reason) {
            <label>
              <input type="radio" [name]="'reason'" [value]="reason" [(ngModel)]="selectedReason" />
              <span>{{ 'returnReason.' + reason | t }}</span>
            </label>
          }
        </fieldset>

        <button type="button" class="primary" (click)="submit()" [disabled]="!selectedReason || submitting()">
          @if (submitting()) {
            <span>{{ 'common.loading' | t }}</span>
          } @else {
            <span>{{ 'common.confirm' | t }}</span>
          }
        </button>
      </article>
    } @else {
      <p class="muted">Sipariş bulunamadı.</p>
    }
  `,
  styles: [
    `
      .card {
        background: var(--sc-surface);
        border: 1px solid var(--sc-border);
        border-radius: var(--sc-radius);
        padding: 24px;
      }
      .card h2 {
        margin: 0 0 8px;
      }
      .muted {
        color: var(--sc-text-muted);
      }
      fieldset {
        border: 0;
        padding: 0;
        margin: 16px 0;
      }
      legend {
        font-weight: 600;
        margin-bottom: 8px;
      }
      label {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 8px 0;
      }
      .status {
        background: var(--sc-primary-50);
        color: var(--sc-primary);
        padding: 8px 12px;
        border-radius: var(--sc-radius-sm);
        margin: 16px 0;
        font-weight: 600;
      }
      .primary {
        background: var(--sc-primary);
        color: white;
        border: 0;
        padding: 12px 24px;
        border-radius: var(--sc-radius);
        font-weight: 600;
        cursor: pointer;
      }
      .primary:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }
      a {
        color: var(--sc-primary);
        text-decoration: none;
      }
    `,
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
  selectedReason: ReturnReasonCode | null = null;

  async ngOnInit(): Promise<void> {
    const id = this.route.snapshot.params['id'] as string;
    if (!id) {
      this.loading.set(false);
      return;
    }
    try {
      const order = await firstValueFrom(this.orderApi.getOrder(id));
      this.order.set(order);
    } finally {
      this.loading.set(false);
    }
  }

  async submit(): Promise<void> {
    const order = this.order();
    if (!order || !this.selectedReason) return;
    this.submitting.set(true);
    try {
      const result = await firstValueFrom(
        this.returnApi.create({
          orderId: order.id,
          reasonCode: this.selectedReason,
          items: order.items.map((i) => ({ offerId: i.offerId, quantity: i.quantity })),
        }),
      );
      this.returnRecord.set(result);
      this.toast.show(this.i18n.t('returnStatus.REQUESTED'), 'success');
    } finally {
      this.submitting.set(false);
    }
  }
}
