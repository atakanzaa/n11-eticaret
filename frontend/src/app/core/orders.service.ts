import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { OrderApi } from '@core/api/order.api';
import { OrderResponse } from '@core/models/order.types';
import { Page } from '@core/models/common.types';

/**
 * Orders facade. Same pattern as `CartService`: HTTP-backed signal cache,
 * components consume the cache (or trigger a refresh) instead of subscribing.
 *
 * Order placement itself does NOT live here — that's `CheckoutComponent`'s
 * orchestration job (checkout → payment initiate → 3DS → result). All this
 * service does is read the user's order list and details.
 */
@Injectable({ providedIn: 'root' })
export class OrdersService {
  private readonly orderApi = inject(OrderApi);

  private readonly _orders = signal<OrderResponse[]>([]);
  private readonly _page = signal<Page<OrderResponse> | null>(null);
  private readonly _loading = signal(false);

  readonly orders = this._orders.asReadonly();
  readonly page = this._page.asReadonly();
  readonly loading = this._loading.asReadonly();

  async refresh(page = 0, size = 20): Promise<void> {
    this._loading.set(true);
    try {
      const result = await firstValueFrom(this.orderApi.listMyOrders(page, size));
      this._page.set(result);
      this._orders.set(result.content);
    } finally {
      this._loading.set(false);
    }
  }

  async getById(orderId: string): Promise<OrderResponse> {
    return firstValueFrom(this.orderApi.getOrder(orderId));
  }
}
