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
      holderName: ['', [Validators.required, Validators.minLength(3)]],
      number: ['', [Validators.required, Validators.pattern(/^\d{13,19}$/)]],
      expireMonth: ['', [Validators.required, Validators.pattern(/^(0[1-9]|1[0-2])$/)]],
      expireYear: ['', [Validators.required, Validators.pattern(/^\d{2}$|^\d{4}$/)]],
      cvc: ['', [Validators.required, Validators.pattern(/^\d{3,4}$/)]],
    }),
  });

  // Toast'a takılmadan, kart formu üstünde gösterilen detay hata mesajı
  readonly paymentError = signal<string | null>(null);
  // Backend'den gelen canonical hata kodu (PAYMENT_BANK_CARD_NO_INSTALLMENT vb.)
  readonly paymentErrorCode = signal<string | null>(null);
  // Yeni order yaratıldıysa orderId burada tutulur — payment fail olursa retry
  // sırasında aynı order üzerinden initiate edilir, yeni order yaratılmaz.
  readonly activeOrderId = signal<string | null>(null);
  readonly cancellingOrder = signal(false);

  readonly cardHolderError = computed(() => {
    const c = this.form.get('card.holderName');
    if (!c || !c.touched || c.valid) return '';
    if (c.hasError('required')) return this.i18n.t('validation.required');
    if (c.hasError('minlength')) return this.i18n.t('checkout.cardHolderInvalid');
    return '';
  });

  readonly cardNumberError = computed(() => {
    const c = this.form.get('card.number');
    if (!c || !c.touched || c.valid) return '';
    if (c.hasError('required')) return this.i18n.t('validation.required');
    return this.i18n.t('checkout.cardNumberInvalid');
  });

  readonly cardExpireError = computed(() => {
    const m = this.form.get('card.expireMonth');
    const y = this.form.get('card.expireYear');
    if (!m || !y) return '';
    const monthInvalid = m.touched && !m.valid;
    const yearInvalid = y.touched && !y.valid;
    if (!monthInvalid && !yearInvalid) return '';
    if (m.hasError('required') || y.hasError('required')) return this.i18n.t('validation.required');
    return this.i18n.t('checkout.expireInvalid');
  });

  readonly cardCvcError = computed(() => {
    const c = this.form.get('card.cvc');
    if (!c || !c.touched || c.valid) return '';
    if (c.hasError('required')) return this.i18n.t('validation.required');
    return this.i18n.t('checkout.cvcInvalid');
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

  /** Per-button monthly amount preview — total / n (n = button's installment count). */
  monthlyFor(n: number): number {
    const total = this.cart.subtotal() - this.cart.discount();
    return n > 0 ? total / n : total;
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
    this.paymentError.set(null);
    this.paymentErrorCode.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    try {
      const v = this.form.getRawValue();

      // Yalnızca aktif order yoksa yeni Order yarat. Payment fail olunca
      // activeOrderId silinmiyor → retry aynı order üzerinden initiate çağırır.
      let orderId = this.activeOrderId();
      if (!orderId) {
        // crypto.randomUUID() is only available on secure (HTTPS) contexts.
        // Demo runs on plain HTTP, so we fall back to a manual v4 UUID.
        const idempotencyKey = (typeof crypto !== 'undefined' && crypto.randomUUID)
          ? crypto.randomUUID()
          : ('xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
              const r = Math.random() * 16 | 0;
              return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16);
            }));
        const checkoutResponse = await firstValueFrom(
          this.orderApi.checkout(
            {
              addressId: v.addressId,
              couponCode: this.cart.coupon()?.code,
            },
            idempotencyKey,
          ),
        );
        orderId = checkoutResponse.orderId;
        this.activeOrderId.set(orderId);
      }

      const card = { ...v.card };
      if (card.expireYear && /^\d{2}$/.test(card.expireYear)) {
        card.expireYear = '20' + card.expireYear;
      }
      const paymentResponse = await firstValueFrom(
        this.paymentApi.initiate({
          orderId,
          card,
          installment: v.installment,
        }),
      );

      this.router.navigate(['/odeme/3ds', paymentResponse.paymentId], {
        state: { html: paymentResponse.threeDsHtmlContent ?? '' },
      });
    } catch (err: unknown) {
      // error.interceptor toast'ı gösterse de kart formu üstünde detay göstermek isteriz.
      const errorBody = (err as { error?: { error?: { code?: string; message?: string } } })?.error?.error;
      const code = errorBody?.code;
      const message = errorBody?.message;

      // Backend canonical ERR_xxxx kodu → kullanıcı dostu Türkçe mesaj.
      // Bilinmeyen kodlarda backend'in raw mesajına düşeriz.
      this.paymentErrorCode.set(code ?? null);
      const i18nKey = code && /^ERR_\d{4}$/.test(code) ? `errors.${code}` : null;
      const localized = i18nKey ? this.i18n.t(i18nKey) : null;
      this.paymentError.set(
        localized && localized !== i18nKey
          ? localized
          : message ?? this.i18n.t('checkout.paymentFailed'),
      );
    } finally {
      this.submitting.set(false);
    }
  }

  /** Banka kartı taksit reddi sonrası tek tıkla "Tek çekim'e geç" CTA. */
  switchToSinglePayment(): void {
    this.selectInstallment(1);
    this.paymentError.set(null);
    this.paymentErrorCode.set(null);
  }

  /**
   * Aktif order'ı iptal et + state temizle. Kullanıcı kart/adres tamamen
   * değiştirmek isterse bu butonu kullanır → bir sonraki submit'te yeni
   * Order yaratılır.
   */
  async cancelActiveOrder(): Promise<void> {
    const orderId = this.activeOrderId();
    if (!orderId) return;
    this.cancellingOrder.set(true);
    try {
      await firstValueFrom(this.orderApi.cancelOrder(orderId));
      this.activeOrderId.set(null);
      this.paymentError.set(null);
      this.paymentErrorCode.set(null);
      this.toast.show(this.i18n.t('checkout.orderCancelled'), 'success');
    } catch {
      // Order zaten cancel veya pencere kapandı — sessiz geç, state temizle.
      this.activeOrderId.set(null);
      this.paymentError.set(null);
      this.paymentErrorCode.set(null);
    } finally {
      this.cancellingOrder.set(false);
    }
  }

  /** Banka kartı + taksit reddedildiyse hızlı CTA göster (ERR_4002). */
  readonly showSwitchToSingle = computed(() =>
    this.paymentErrorCode() === 'ERR_4002' &&
    this.selectedInstallment() !== 1,
  );
}
