import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { CartService } from '@core/cart.service';
import { ToastService } from '@core/toast.service';
import { I18nService } from '@core/i18n/i18n.service';
import { TPipe } from '@shared/i18n.pipe';

@Component({
  selector: 'sc-cart',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, FormsModule, DecimalPipe, TPipe],
  template: `
    <h1>{{ 'cart.title' | t }}</h1>

    @if (cart.loading()) {
      <p class="muted">{{ 'common.loading' | t }}</p>
    } @else if (!cart.cart() || cart.cart()!.items.length === 0) {
      <div class="empty">
        <p>{{ 'cart.empty' | t }}</p>
        <a [routerLink]="['/']" class="primary">{{ 'cart.keepShopping' | t }}</a>
      </div>
    } @else {
      <div class="layout">
        <section class="lines">
          @for (item of cart.cart()!.items; track item.id) {
            <article class="line">
              @if (item.productImageSnapshot) {
                <img [src]="item.productImageSnapshot" [alt]="item.productTitleSnapshot" />
              } @else {
                <div class="ph"></div>
              }
              <div class="info">
                <h3>{{ item.productTitleSnapshot }}</h3>
                <span class="muted">{{ item.sellerNameSnapshot }}</span>
                <div class="qty">
                  <button type="button" (click)="dec(item.offerId, item.quantity)" [disabled]="item.quantity <= 1">−</button>
                  <span>{{ item.quantity }}</span>
                  <button type="button" (click)="inc(item.offerId, item.quantity)">+</button>
                </div>
                <button type="button" class="link" (click)="remove(item.offerId)">{{ 'cart.remove' | t }}</button>
              </div>
              <div class="line-total">{{ formatPrice(item.subtotal) }}</div>
            </article>
          }
        </section>

        <aside class="summary">
          <div class="row">
            <span>{{ 'common.subtotal' | t }}</span>
            <span>{{ formatPrice(cart.subtotal()) }}</span>
          </div>
          @if (cart.discount() > 0) {
            <div class="row">
              <span>{{ 'common.discount' | t }}</span>
              <span class="discount">−{{ formatPrice(cart.discount()) }}</span>
            </div>
          }
          <div class="row total">
            <span>{{ 'common.total' | t }}</span>
            <span>{{ formatPrice(cart.subtotal() - cart.discount()) }}</span>
          </div>

          <div class="coupon">
            <input type="text" [(ngModel)]="couponCode" [placeholder]="'cart.couponCode' | t" />
            <button type="button" (click)="applyCoupon()">{{ 'cart.couponApply' | t }}</button>
          </div>

          <button type="button" class="primary" (click)="proceed()">{{ 'cart.proceedCheckout' | t }}</button>
        </aside>
      </div>
    }
  `,
  styles: [
    `
      h1 {
        margin: 0 0 24px;
      }
      .empty {
        text-align: center;
        padding: 64px 0;
      }
      .empty .primary {
        display: inline-block;
        margin-top: 16px;
        background: var(--sc-primary);
        color: white;
        padding: 12px 24px;
        border-radius: var(--sc-radius);
        text-decoration: none;
        font-weight: 600;
      }
      .layout {
        display: grid;
        grid-template-columns: 1fr 320px;
        gap: 24px;
      }
      .lines {
        display: flex;
        flex-direction: column;
        gap: 12px;
      }
      .line {
        display: grid;
        grid-template-columns: 100px 1fr auto;
        gap: 16px;
        background: var(--sc-surface);
        border: 1px solid var(--sc-border);
        border-radius: var(--sc-radius);
        padding: 16px;
        align-items: center;
      }
      .line img,
      .line .ph {
        width: 100px;
        height: 100px;
        object-fit: cover;
        border-radius: var(--sc-radius-sm);
        background: var(--sc-surface-2);
      }
      .info h3 {
        margin: 0 0 4px;
        font-size: 15px;
      }
      .qty {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-top: 8px;
      }
      .qty button {
        width: 28px;
        height: 28px;
        border: 1px solid var(--sc-border);
        background: white;
        border-radius: var(--sc-radius-sm);
        cursor: pointer;
      }
      .link {
        background: none;
        border: 0;
        color: var(--sc-danger);
        cursor: pointer;
        padding: 0;
        margin-top: 8px;
        font-size: 13px;
      }
      .line-total {
        font-weight: 700;
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
      .row {
        display: flex;
        justify-content: space-between;
        margin-bottom: 8px;
      }
      .total {
        margin-top: 12px;
        padding-top: 12px;
        border-top: 1px solid var(--sc-border);
        font-weight: 700;
        font-size: 18px;
      }
      .discount {
        color: var(--sc-success);
      }
      .coupon {
        display: flex;
        gap: 8px;
        margin: 16px 0;
      }
      .coupon input {
        flex: 1;
        padding: 10px;
        border: 1px solid var(--sc-border);
        border-radius: var(--sc-radius-sm);
      }
      .coupon button {
        background: white;
        border: 1px solid var(--sc-border);
        padding: 0 16px;
        border-radius: var(--sc-radius-sm);
        cursor: pointer;
      }
      .primary {
        width: 100%;
        background: var(--sc-primary);
        color: white;
        border: 0;
        padding: 14px;
        border-radius: var(--sc-radius);
        font-weight: 600;
        cursor: pointer;
      }
      .muted {
        color: var(--sc-text-muted);
      }
    `,
  ],
})
export class CartComponent implements OnInit {
  protected readonly cart = inject(CartService);
  private readonly toast = inject(ToastService);
  private readonly i18n = inject(I18nService);
  private readonly router = inject(Router);

  couponCode = '';

  ngOnInit(): void {
    this.cart.refresh().catch(() => {});
  }

  async inc(offerId: string, current: number): Promise<void> {
    await this.cart.setQuantity(offerId, current + 1);
  }

  async dec(offerId: string, current: number): Promise<void> {
    await this.cart.setQuantity(offerId, current - 1);
  }

  async remove(offerId: string): Promise<void> {
    await this.cart.removeItem(offerId);
  }

  async applyCoupon(): Promise<void> {
    if (!this.couponCode.trim()) return;
    const ok = await this.cart.applyCoupon(this.couponCode);
    this.toast.show(
      ok
        ? this.i18n.t('cart.couponApplied', { discount: this.formatPrice(this.cart.discount()) })
        : this.i18n.t('cart.couponInvalid'),
      ok ? 'success' : 'danger',
    );
  }

  proceed(): void {
    this.router.navigate(['/odeme']);
  }

  formatPrice(value: number): string {
    return new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(value);
  }
}
