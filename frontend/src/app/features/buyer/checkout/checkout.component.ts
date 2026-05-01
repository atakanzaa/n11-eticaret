import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
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
import { CurrencyFormatPipe } from '@shared/pipes/currency-format.pipe';
import { SpinnerComponent } from '@shared/ui/spinner/spinner.component';
import { StepIndicatorComponent } from '@shared/ui/step-indicator/step-indicator.component';
import { AddressCardComponent } from '@shared/ui/address-card/address-card.component';
import { FormFieldComponent } from '@shared/ui/form-field/form-field.component';

@Component({
  selector: 'sc-checkout',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    TPipe,
    CurrencyFormatPipe,
    SpinnerComponent,
    StepIndicatorComponent,
    AddressCardComponent,
    FormFieldComponent,
  ],
  templateUrl: './checkout.component.html',
  styleUrls: ['./checkout.component.scss'],
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
  readonly loading = signal(true);
  readonly submitting = signal(false);
  readonly currentStep = signal(0);
  readonly selectedAddressId = signal('');
  readonly installments = environment.installments;
  readonly stepLabels = ['Adres', 'Odeme', 'Onay'];

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

  readonly selectedAddress = computed(() => {
    const id = this.selectedAddressId();
    return this.addresses().find(a => a.id === id) ?? null;
  });

  readonly canProceedFromAddress = computed(() => !!this.selectedAddressId());

  readonly canProceedFromPayment = computed(() => {
    const card = this.form.get('card');
    return card ? card.valid : false;
  });

  readonly selectedInstallment = signal(1);

  readonly monthlyPayment = computed(() => {
    const total = this.cart.subtotal() - this.cart.discount();
    const inst = this.selectedInstallment();
    return inst > 0 ? total / inst : total;
  });

  async ngOnInit(): Promise<void> {
    await this.cart.refresh();
    try {
      const addrs = await firstValueFrom(this.userApi.listAddresses());
      this.addresses.set(addrs);
      const def = addrs.find(a => a.defaultShipping) ?? addrs[0];
      if (def) {
        this.selectedAddressId.set(def.id);
        this.form.patchValue({ addressId: def.id });
      }
    } catch {
      /* shown as inline empty state */
    } finally {
      this.loading.set(false);
    }
  }

  selectAddress(id: string): void {
    this.selectedAddressId.set(id);
    this.form.patchValue({ addressId: id });
  }

  selectInstallment(n: number): void {
    this.selectedInstallment.set(n);
    this.form.patchValue({ installment: n });
  }

  nextStep(): void {
    const step = this.currentStep();
    if (step < 2) this.currentStep.set(step + 1);
  }

  prevStep(): void {
    const step = this.currentStep();
    if (step > 0) this.currentStep.set(step - 1);
  }

  formatCardNumber(event: Event): void {
    const input = event.target as HTMLInputElement;
    let value = input.value.replace(/\D/g, '');
    if (value.length > 16) value = value.slice(0, 16);
    const parts: string[] = [];
    for (let i = 0; i < value.length; i += 4) {
      parts.push(value.slice(i, i + 4));
    }
    input.value = parts.join(' ');
    this.form.get('card.number')?.setValue(value);
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

      const card = { ...v.card };
      if (card.expireYear && /^\d{2}$/.test(card.expireYear)) {
        card.expireYear = '20' + card.expireYear;
      }
      const paymentResponse = await firstValueFrom(
        this.paymentApi.initiate({
          orderId: checkoutResponse.orderId,
          card,
          installment: v.installment,
        }),
      );

      this.router.navigate(['/odeme/3ds', paymentResponse.paymentId], {
        state: { html: paymentResponse.threeDsHtmlContent ?? '' },
      });
    } catch {
      // error.interceptor surfaced a toast already
    } finally {
      this.submitting.set(false);
    }
  }
}
