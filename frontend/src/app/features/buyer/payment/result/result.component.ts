import { ChangeDetectionStrategy, Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { PaymentApi } from '@core/api/payment.api';
import { PaymentResponse } from '@core/models/payment.types';
import { TPipe } from '@shared/i18n.pipe';

/**
 * Landing page after Iyzico callback. The backend's webhook handler issues a
 * 303 redirect to `/odeme/sonuc/:paymentId?status=success|failure`, but the
 * source-of-truth is `GET /api/payments/{id}` — query string is just a hint.
 *
 * If we land while status is still `INITIATED` / `PENDING` (e.g. the user
 * scrolled back to a stale tab), we poll for up to a minute before giving up.
 */
@Component({
  selector: 'sc-payment-result',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, TPipe],
  template: `
    <div class="card">
      @if (state() === 'pending') {
        <h2>{{ 'common.loading' | t }}</h2>
        <p class="muted">Banka onayı bekleniyor…</p>
      } @else if (state() === 'success' && payment()) {
        <h2 class="success">✓ {{ 'checkout.paymentSuccess' | t }}</h2>
        <p>Sipariş No: <strong>#{{ payment()!.orderId }}</strong></p>
        <a [routerLink]="['/siparis-tamamlandi', payment()!.orderId]" class="primary">
          {{ 'common.next' | t }}
        </a>
      } @else if (state() === 'failure') {
        <h2 class="danger">✗ {{ 'checkout.paymentFailure' | t }}</h2>
        @if (payment()?.failureMessage) {
          <p class="muted">{{ payment()!.failureMessage }}</p>
        }
        <a [routerLink]="['/sepet']" class="primary">{{ 'common.back' | t }}</a>
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
        margin: 0 0 16px;
      }
      .success {
        color: var(--sc-success);
      }
      .danger {
        color: var(--sc-danger);
      }
      .muted {
        color: var(--sc-text-muted);
      }
      .primary {
        display: inline-block;
        margin-top: 24px;
        background: var(--sc-primary);
        color: white;
        padding: 12px 24px;
        border-radius: var(--sc-radius);
        text-decoration: none;
        font-weight: 600;
      }
    `,
  ],
})
export class PaymentResultComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly paymentApi = inject(PaymentApi);

  readonly payment = signal<PaymentResponse | null>(null);
  readonly state = signal<'pending' | 'success' | 'failure'>('pending');

  private pollHandle: ReturnType<typeof setInterval> | null = null;
  private attempts = 0;

  ngOnInit(): void {
    const id = this.route.snapshot.params['paymentId'] as string;
    if (!id) {
      this.state.set('failure');
      return;
    }
    this.checkOnce(id);
    this.pollHandle = setInterval(() => this.checkOnce(id), 2000);
  }

  ngOnDestroy(): void {
    if (this.pollHandle) clearInterval(this.pollHandle);
  }

  private async checkOnce(paymentId: string): Promise<void> {
    this.attempts++;
    try {
      const payment = await firstValueFrom(this.paymentApi.byId(paymentId));
      this.payment.set(payment);
      if (payment.status === 'SUCCEEDED') {
        this.state.set('success');
        this.stopPolling();
      } else if (payment.status === 'FAILED') {
        this.state.set('failure');
        this.stopPolling();
      } else if (this.attempts >= 30) {
        // give up after a minute — banker probably timed out
        this.state.set('failure');
        this.stopPolling();
      }
    } catch {
      if (this.attempts >= 30) {
        this.state.set('failure');
        this.stopPolling();
      }
    }
  }

  private stopPolling(): void {
    if (this.pollHandle) {
      clearInterval(this.pollHandle);
      this.pollHandle = null;
    }
  }
}
