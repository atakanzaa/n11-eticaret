import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom, tap } from 'rxjs';
import { CartApi } from '@core/api/cart.api';
import { CouponApi } from '@core/api/coupon.api';
import { CartResponse, CartValidationIssue } from '@core/models/cart.types';
import { CouponValidationResponse } from '@core/models/coupon.types';
import { AuthStateService } from '@core/auth/auth-state.service';

/**
 * Cart facade. Components only ever talk to this service — never directly to
 * `CartApi`. The facade keeps a signal-backed cache of the current cart so the
 * cart-icon badge in the header refreshes the moment a "Add to Cart" succeeds,
 * without anyone having to subscribe to anything.
 *
 * Backend is the source of truth: there's no localStorage, no guest cart, no
 * client-side discount computation. The user has to be logged in for the cart
 * to exist (the route guard enforces that).
 */
@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly cartApi = inject(CartApi);
  private readonly couponApi = inject(CouponApi);
  private readonly auth = inject(AuthStateService);

  private readonly _cart = signal<CartResponse | null>(null);
  private readonly _validation = signal<CartValidationIssue[]>([]);
  private readonly _coupon = signal<CouponValidationResponse | null>(null);
  private readonly _loading = signal(false);

  readonly cart = this._cart.asReadonly();
  readonly validation = this._validation.asReadonly();
  readonly coupon = this._coupon.asReadonly();
  readonly loading = this._loading.asReadonly();

  readonly itemCount = computed(() => this._cart()?.itemCount ?? 0);
  readonly subtotal = computed(() => this._cart()?.total ?? 0);
  readonly discount = computed(() => this._coupon()?.discountAmount ?? 0);

  /**
   * Sum of `cargoPriceSnapshot` across distinct sellers in the cart. Each
   * seller charges their own cargo once regardless of how many items the user
   * buys from them, so we only count the cargo price per seller (not per item
   * quantity).
   */
  readonly shippingTotal = computed(() => {
    const items = this._cart()?.items ?? [];
    const cargoPerSeller = new Map<string, number>();
    for (const it of items) {
      // Take the highest cargo price per seller (defensive — usually constant)
      const current = cargoPerSeller.get(it.sellerId) ?? 0;
      if (it.cargoPriceSnapshot > current) {
        cargoPerSeller.set(it.sellerId, it.cargoPriceSnapshot);
      }
    }
    let sum = 0;
    cargoPerSeller.forEach(v => sum += v);
    return sum;
  });

  readonly grandTotal = computed(() =>
    Math.max(0, this.subtotal() + this.shippingTotal() - this.discount()),
  );

  async refresh(): Promise<void> {
    if (!this.auth.isAuthenticated()) {
      this._cart.set(null);
      return;
    }
    this._loading.set(true);
    try {
      const cart = await firstValueFrom(this.cartApi.get());
      this._cart.set(cart);
    } finally {
      this._loading.set(false);
    }
  }

  async addItem(offerId: string, quantity = 1): Promise<void> {
    const cart = await firstValueFrom(this.cartApi.addItem({ offerId, quantity }));
    this._cart.set(cart);
  }

  async setQuantity(offerId: string, quantity: number): Promise<void> {
    if (quantity <= 0) {
      return this.removeItem(offerId);
    }
    const cart = await firstValueFrom(this.cartApi.updateQuantity(offerId, quantity));
    this._cart.set(cart);
  }

  async removeItem(offerId: string): Promise<void> {
    const cart = await firstValueFrom(this.cartApi.removeItem(offerId));
    this._cart.set(cart);
  }

  async clear(): Promise<void> {
    await firstValueFrom(this.cartApi.clear());
    this._cart.set(null);
    this._coupon.set(null);
  }

  async validate(): Promise<void> {
    const result = await firstValueFrom(this.cartApi.validate());
    this._cart.set(result.cart);
    this._validation.set(result.issues);
  }

  /**
   * Validates a coupon code against the current cart's totals on the server.
   * The discount is *not* applied locally — it's a hint we show in the UI so
   * the user knows what their final total will look like once checkout fires
   * the same call inside `OrderApi.checkout`.
   */
  async applyCoupon(code: string): Promise<boolean> {
    const cart = this._cart();
    const userId = this.auth.currentUser()?.id;
    if (!cart || !userId) return false;
    const trimmedCode = code.trim().toUpperCase();
    const result = await firstValueFrom(
      this.couponApi.validate({
        code: trimmedCode,
        userId,
        cartTotal: cart.total,
        shippingCost: 0,
      }),
    );
    if (result.valid) {
      this._coupon.set(result);
      return true;
    }
    this._coupon.set(null);
    return false;
  }

  clearCoupon(): void {
    this._coupon.set(null);
  }
}
