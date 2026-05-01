import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CartService } from '@core/cart.service';
import { ToastService } from '@core/toast.service';
import { I18nService } from '@core/i18n/i18n.service';
import { TPipe } from '@shared/i18n.pipe';
import { CurrencyFormatPipe } from '@shared/pipes/currency-format.pipe';
import { SpinnerComponent } from '@shared/ui/spinner/spinner.component';
import { EmptyStateComponent } from '@shared/ui/empty-state/empty-state.component';
import { QuantityStepperComponent } from '@shared/ui/quantity-stepper/quantity-stepper.component';
import { PriceDisplayComponent } from '@shared/ui/price-display/price-display.component';

@Component({
  selector: 'sc-cart',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    FormsModule,
    TPipe,
    CurrencyFormatPipe,
    SpinnerComponent,
    EmptyStateComponent,
    QuantityStepperComponent,
    PriceDisplayComponent,
  ],
  templateUrl: './cart.component.html',
  styleUrls: ['./cart.component.scss'],
})
export class CartComponent implements OnInit {
  protected readonly cart = inject(CartService);
  private readonly toast = inject(ToastService);
  private readonly i18n = inject(I18nService);
  private readonly router = inject(Router);

  readonly couponCode = signal('');

  ngOnInit(): void {
    this.cart.refresh().catch(() => {});
  }

  async onQuantityChange(offerId: string, qty: number): Promise<void> {
    await this.cart.setQuantity(offerId, qty);
  }

  async remove(offerId: string): Promise<void> {
    await this.cart.removeItem(offerId);
  }

  async applyCoupon(): Promise<void> {
    const code = this.couponCode().trim();
    if (!code) return;
    const ok = await this.cart.applyCoupon(code);
    this.toast.show(
      ok
        ? this.i18n.t('cart.couponApplied', { discount: '' })
        : this.i18n.t('cart.couponInvalid'),
      ok ? 'success' : 'danger',
    );
  }

  proceed(): void {
    this.router.navigate(['/odeme']);
  }
}
