import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { OrderApi } from '@core/api/order.api';
import { ShipmentApi } from '@core/api/shipment.api';
import { CartService } from '@core/cart.service';
import { OrderResponse } from '@core/models/order.types';
import { ShipmentResponse } from '@core/models/shipment.types';
import { TPipe } from '@shared/i18n.pipe';
import { CurrencyFormatPipe } from '@shared/pipes/currency-format.pipe';
import { SpinnerComponent } from '@shared/ui/spinner/spinner.component';

@Component({
  selector: 'sc-order-success',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, TPipe, CurrencyFormatPipe, SpinnerComponent],
  templateUrl: './order-success.component.html',
  styleUrls: ['./order-success.component.scss'],
})
export class OrderSuccessComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly orderApi = inject(OrderApi);
  private readonly shipmentApi = inject(ShipmentApi);
  private readonly cart = inject(CartService);

  readonly order = signal<OrderResponse | null>(null);
  readonly shipments = signal<ShipmentResponse[]>([]);
  readonly loading = signal(true);

  async ngOnInit(): Promise<void> {
    const id = this.route.snapshot.params['id'] as string;
    if (!id) {
      this.loading.set(false);
      return;
    }
    try {
      const order = await firstValueFrom(this.orderApi.getOrder(id));
      this.order.set(order);
      this.cart.refresh().catch(() => {});
      try {
        const shipments = await firstValueFrom(this.shipmentApi.byOrder(id));
        this.shipments.set(shipments);
      } catch {
        /* shipments may not exist yet */
      }
    } finally {
      this.loading.set(false);
    }
  }
}
