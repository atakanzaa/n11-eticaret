import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { OfferApi } from '@core/api/offer.api';
import { ProductApi } from '@core/api/product.api';
import { InventoryApi } from '@core/api/inventory.api';
import { OfferResponse } from '@core/models/offer.types';
import { ProductResponse } from '@core/models/product.types';
import { TPipe } from '@shared/i18n.pipe';

interface OfferRow {
  offer: OfferResponse;
  product: ProductResponse | null;
  available: number | null;
}

@Component({
  selector: 'sc-seller-products',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TPipe],
  template: `
    <header class="head">
      <h1>{{ 'seller.products' | t }}</h1>
    </header>

    @if (loading()) {
      <p class="muted">{{ 'common.loading' | t }}</p>
    } @else if (rows().length === 0) {
      <p class="muted">{{ 'common.empty' | t }}</p>
    } @else {
      <table class="grid">
        <thead>
          <tr>
            <th>Ürün</th>
            <th>SKU</th>
            <th>Fiyat</th>
            <th>Stok</th>
            <th>Durum</th>
          </tr>
        </thead>
        <tbody>
          @for (row of rows(); track row.offer.id) {
            <tr>
              <td>{{ row.product?.title ?? row.offer.productId }}</td>
              <td><code>{{ row.offer.sku }}</code></td>
              <td>{{ formatPrice(row.offer.price) }}</td>
              <td [class.low]="(row.available ?? 0) <= 5">
                {{ row.available ?? '—' }}
              </td>
              <td><span class="pill" [class]="pillClass(row.offer.status)">{{ row.offer.status }}</span></td>
            </tr>
          }
        </tbody>
      </table>
    }
  `,
  styles: [
    `
      .head {
        display: flex;
        justify-content: space-between;
        margin-bottom: 16px;
      }
      h1 {
        margin: 0;
      }
      .grid {
        width: 100%;
        background: var(--sc-surface);
        border: 1px solid var(--sc-border);
        border-radius: var(--sc-radius);
        border-collapse: collapse;
        overflow: hidden;
      }
      th,
      td {
        text-align: left;
        padding: 12px 16px;
        border-bottom: 1px solid var(--sc-border);
        font-size: 14px;
      }
      th {
        background: var(--sc-surface-2);
        font-weight: 600;
      }
      tr:last-child td {
        border-bottom: 0;
      }
      .low {
        color: var(--sc-warn);
        font-weight: 600;
      }
      code {
        font-family: var(--sc-mono, monospace);
        font-size: 12px;
        background: var(--sc-surface-2);
        padding: 2px 6px;
        border-radius: 4px;
      }
      .pill {
        font-size: 12px;
        padding: 4px 10px;
        border-radius: 999px;
        font-weight: 600;
        background: var(--sc-surface-2);
      }
      .pill-active {
        background: #d1fae5;
        color: #047857;
      }
      .pill-paused {
        background: #fef3c7;
        color: #b45309;
      }
      .pill-inactive {
        background: #e5e7eb;
        color: #4b5563;
      }
      .muted {
        color: var(--sc-text-muted);
      }
    `,
  ],
})
export class SellerProductsComponent implements OnInit {
  private readonly offerApi = inject(OfferApi);
  private readonly productApi = inject(ProductApi);
  private readonly inventoryApi = inject(InventoryApi);

  readonly rows = signal<OfferRow[]>([]);
  readonly loading = signal(true);

  async ngOnInit(): Promise<void> {
    try {
      const offers = await firstValueFrom(this.offerApi.myOffers());
      const enriched: OfferRow[] = await Promise.all(
        offers.map(async (offer) => {
          const [product, inv] = await Promise.allSettled([
            firstValueFrom(this.productApi.byId(offer.productId)),
            firstValueFrom(this.inventoryApi.byOffer(offer.id)),
          ]);
          return {
            offer,
            product: product.status === 'fulfilled' ? product.value : null,
            available: inv.status === 'fulfilled' ? inv.value.availableQuantity : null,
          };
        }),
      );
      this.rows.set(enriched);
    } finally {
      this.loading.set(false);
    }
  }

  pillClass(status: string): string {
    return {
      ACTIVE: 'pill-active',
      PAUSED: 'pill-paused',
      INACTIVE: 'pill-inactive',
      DELISTED: 'pill-inactive',
    }[status] ?? '';
  }

  formatPrice(value: number): string {
    return new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(value);
  }
}
