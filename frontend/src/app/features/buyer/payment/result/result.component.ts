import { ChangeDetectionStrategy, Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { PaymentApi } from '@core/api/payment.api';
import { PaymentResponse } from '@core/models/payment.types';
import { TPipe } from '@shared/i18n.pipe';
import { CurrencyFormatPipe } from '@shared/pipes/currency-format.pipe';
import { SpinnerComponent } from '@shared/ui/spinner/spinner.component';

/**
 * Landing page after Iyzico callback. The backend's webhook handler issues a
 * 303 redirect to `/odeme/sonuc/:paymentId?status=success|failure`, but the
 * source-of-truth is `GET /api/payments/{id}` -- query string is just a hint.
 *
 * If we land while status is still `INITIATED` / `PENDING` (e.g. the user
 * scrolled back to a stale tab), we poll for up to a minute before giving up.
 */
@Component({
  selector: 'sc-payment-result',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, TPipe, CurrencyFormatPipe, SpinnerComponent],
  templateUrl: './result.component.html',
  styleUrls: ['./result.component.scss'],
})
export class PaymentResultComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
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
