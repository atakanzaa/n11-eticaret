import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { CartService } from '@core/cart.service';
import { UserApi } from '@core/api/user.api';
import { OrderApi } from '@core/api/order.api';
import { PaymentApi } from '@core/api/payment.api';
import { AddressDto } from '@core/models/user.types';
import { ToastService } from '@core/toast.service';
import { I18nService } from '@core/i18n/i18n.service';
import { environment } from '../../../../environments/environment';
import { TPipe } from '@shared/i18n.pipe';

/**
 * Checkout orchestrator: address → payment → confirm. Submitting fires two
 * backend calls in sequence:
 *
 *  1. POST /api/checkout       → orderId
 *  2. POST /api/payments/initiate → 3DS HTML payload
 *
 * The 3DS HTML form is then rendered on the next route (`/odeme/3ds/:paymentId`).
 *
 * Idempotency-Key is generated client-side so a network retry on step 1 doesn't
 * create a duplicate order.
 */
@Component({
  selector: 'sc-checkout',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink, TPipe],
  template: `
    <div class="layout">
      <section class="card">
        <h2>{{ 'checkout.title' | t }}</h2>

        <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
          <fieldset>
            <legend>{{ 'checkout.stepAddress' | t }}</legend>
            @if (addresses().length === 0) {
              <p class="muted">Önce profil sayfasından bir adres ekleyin.</p>
              <a [routerLink]="['/hesap/profil']">{{ 'account.title' | t }} →</a>
            } @else {
              <select formControlName="addressId">
                @for (a of addresses(); track a.id) {
                  <option [value]="a.id">{{ a.label }} — {{ a.fullAddress }}, {{ a.district }}/{{ a.city }}</option>
                }
              </select>
            }
          </fieldset>

          <fieldset formGroupName="card">
            <legend>{{ 'checkout.stepPayment' | t }}</legend>
            <label>
              <span>{{ 'checkout.cardHolder' | t }}</span>
              <input formControlName="holderName" autocomplete="cc-name" />
            </label>
            <label>
              <span>{{ 'checkout.cardNumber' | t }}</span>
              <input formControlName="number" autocomplete="cc-number" maxlength="19" />
            </label>
            <div class="row">
              <label>
                <span>{{ 'checkout.cardExpire' | t }}</span>
                <div class="expire">
                  <input formControlName="expireMonth" placeholder="AA" maxlength="2" />
                  <input formControlName="expireYear" placeholder="YYYY" maxlength="4" />
                </div>
              </label>
              <label>
                <span>{{ 'checkout.cardCvc' | t }}</span>
                <input formControlName="cvc" autocomplete="cc-csc" maxlength="4" />
              </label>
            </div>
          </fieldset>

          <fieldset>
            <legend>{{ 'checkout.installment' | t }}</legend>
            <select formControlName="installment">
              @for (n of installments; track n) {
                <option [value]="n">
                  {{ n === 1 ? ('checkout.installmentSingle' | t) : ('checkout.installmentMonths' | t: { n: n }) }}
                </option>
              }
            </select>
          </fieldset>

          <fieldset>
            <legend>Kupon</legend>
            <div class="row">
              <input [value]="cart.coupon()?.code ?? ''" placeholder="Kupon Kodu" disabled />
              @if (cart.discount() > 0) {
                <span class="muted">−{{ formatPrice(cart.discount()) }}</span>
              }
            </div>
          </fieldset>

          <label class="check">
            <input type="checkbox" formControlName="terms" />
            <span>{{ 'checkout.termsAccept' | t }}</span>
          </label>

          <button type="submit" class="primary" [disabled]="form.invalid || submitting()">
            @if (submitting()) {
              <span>{{ 'common.loading' | t }}</span>
            } @else {
              <span>{{ 'checkout.placeOrder' | t }}</span>
            }
          </button>
        </form>
      </section>

      <aside class="summary">
        <h3>{{ 'common.total' | t }}</h3>
        @if (cart.cart()) {
          <div class="row">
            <span>{{ 'common.subtotal' | t }}</span>
            <span>{{ formatPrice(cart.subtotal()) }}</span>
          </div>
          @if (cart.discount() > 0) {
            <div class="row">
              <span>{{ 'common.discount' | t }}</span>
              <span>−{{ formatPrice(cart.discount()) }}</span>
            </div>
          }
          <div class="row total">
            <strong>{{ 'common.total' | t }}</strong>
            <strong>{{ formatPrice(cart.subtotal() - cart.discount()) }}</strong>
          </div>
        }
      </aside>
    </div>
  `,
  styles: [
    `
      .layout {
        display: grid;
        grid-template-columns: 1fr 320px;
        gap: 24px;
      }
      .card {
        background: var(--sc-surface);
        border: 1px solid var(--sc-border);
        border-radius: var(--sc-radius);
        padding: 24px;
      }
      h2 {
        margin: 0 0 16px;
      }
      fieldset {
        border: 1px solid var(--sc-border);
        border-radius: var(--sc-radius-sm);
        padding: 16px;
        margin-bottom: 16px;
      }
      legend {
        font-weight: 600;
        padding: 0 8px;
      }
      label {
        display: flex;
        flex-direction: column;
        gap: 4px;
        margin-bottom: 12px;
      }
      .row {
        display: grid;
        grid-template-columns: 2fr 1fr;
        gap: 12px;
      }
      .expire {
        display: flex;
        gap: 8px;
      }
      .expire input {
        width: 60px;
      }
      input,
      select {
        padding: 10px 12px;
        border: 1px solid var(--sc-border);
        border-radius: var(--sc-radius-sm);
        font-size: 14px;
      }
      .check {
        flex-direction: row;
        align-items: center;
        gap: 8px;
        margin: 16px 0;
      }
      .primary {
        background: var(--sc-primary);
        color: white;
        border: 0;
        padding: 14px 24px;
        border-radius: var(--sc-radius);
        font-weight: 600;
        font-size: 15px;
        cursor: pointer;
        width: 100%;
      }
      .primary:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }
      .summary {
        background: var(--sc-surface);
        border: 1px solid var(--sc-border);
        border-radius: var(--sc-radius);
        padding: 20px;
        align-self: start;
        position: sticky;
        top: 144px;
      }
      .summary .row {
        display: flex;
        justify-content: space-between;
        margin-bottom: 8px;
        grid-template-columns: none;
      }
      .total {
        margin-top: 12px;
        padding-top: 12px;
        border-top: 1px solid var(--sc-border);
        font-size: 18px;
      }
      .muted {
        color: var(--sc-text-muted);
      }
    `,
  ],
})
export class CheckoutComponent implements OnInit {
  private readonly fb = inject(FormBuilder).nonNullable;
  protected readonly cart = inject(CartService);
  private readonly userApi = inject(UserApi);
  private readonly orderApi = inject(OrderApi);
  private readonly paymentApi = inject(PaymentApi);
  private readonly toast = inject(ToastService);
  private readonly i18n = inject(I18nService);
  private readonly router = inject(Router);

  readonly addresses = signal<AddressDto[]>([]);
  readonly submitting = signal(false);
  readonly installments = environment.installments;

  readonly form = this.fb.group({
    addressId: ['', Validators.required],
    installment: [1, Validators.required],
    terms: [false, Validators.requiredTrue],
    card: this.fb.group({
      holderName: ['', Validators.required],
      number: ['', [Validators.required, Validators.minLength(12)]],
      expireMonth: ['', Validators.required],
      expireYear: ['', Validators.required],
      cvc: ['', [Validators.required, Validators.minLength(3)]],
    }),
  });

  async ngOnInit(): Promise<void> {
    await this.cart.refresh();
    try {
      const addrs = await firstValueFrom(this.userApi.listAddresses());
      this.addresses.set(addrs);
      const def = addrs.find((a) => a.defaultShipping) ?? addrs[0];
      if (def) {
        this.form.patchValue({ addressId: def.id });
      }
    } catch {
      /* shown as inline empty state above */
    }
  }

  async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    try {
      const v = this.form.getRawValue();
      const idempotencyKey = crypto.randomUUID();

      const checkoutResponse = await firstValueFrom(
        this.orderApi.checkout(
          {
            addressId: v.addressId,
            couponCode: this.cart.coupon()?.code,
          },
          idempotencyKey,
        ),
      );

      const paymentResponse = await firstValueFrom(
        this.paymentApi.initiate({
          orderId: checkoutResponse.orderId,
          card: v.card,
          installment: v.installment,
        }),
      );

      // Stash the 3DS HTML payload on history.state so the challenge route
      // can render it without an extra GET. PaymentApi response only includes
      // it once.
      this.router.navigate(['/odeme/3ds', paymentResponse.paymentId], {
        state: { html: paymentResponse.threeDsHtmlContent ?? '' },
      });
    } catch {
      // error.interceptor surfaced a toast already
    } finally {
      this.submitting.set(false);
    }
  }

  formatPrice(value: number): string {
    return new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(value);
  }
}
