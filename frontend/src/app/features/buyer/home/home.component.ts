import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { CategoryApi } from '@core/api/category.api';
import { ProductApi } from '@core/api/product.api';
import { OfferApi } from '@core/api/offer.api';
import { CategoryResponse } from '@core/models/category.types';
import { ProductResponse } from '@core/models/product.types';
import { OfferResponse } from '@core/models/offer.types';
import { ProductCardComponent } from '@shared/ui/product-card.component';
import { TPipe } from '@shared/i18n.pipe';

interface ProductWithOffer {
  product: ProductResponse;
  offer: OfferResponse | null;
}

/**
 * Storefront home page. Pulls categories, a featured product list and the
 * cheapest active offer per featured product so the cards render with prices.
 *
 * Recommendation rails (`/api/recommendations/me`, `/api/recommendations/popular`)
 * are folded in opportunistically — if the calls fail (e.g. recommendation
 * service down), the home page still loads.
 */
@Component({
  selector: 'sc-home',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, ProductCardComponent, TPipe],
  template: `
    <section class="categories">
      @for (c of categories(); track c.id) {
        <a [routerLink]="['/kategori', c.slug]" class="category">
          <span class="name">{{ c.name }}</span>
          <span class="count">{{ c.productCount }} ürün</span>
        </a>
      }
    </section>

    <section class="rail">
      <h2>Öne Çıkan Ürünler</h2>
      @if (loading()) {
        <p class="muted">{{ 'common.loading' | t }}</p>
      } @else if (featured().length === 0) {
        <p class="muted">{{ 'common.empty' | t }}</p>
      } @else {
        <div class="grid">
          @for (item of featured(); track item.product.id) {
            <sc-product-card [product]="item.product" [offer]="item.offer" />
          }
        </div>
      }
    </section>
  `,
  styles: [
    `
      .categories {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
        gap: 12px;
        margin-bottom: 32px;
      }
      .category {
        display: flex;
        flex-direction: column;
        align-items: center;
        padding: 24px 12px;
        background: var(--sc-surface);
        border: 1px solid var(--sc-border);
        border-radius: var(--sc-radius);
        text-decoration: none;
        color: inherit;
        text-align: center;
        transition: transform 0.15s, box-shadow 0.15s;
      }
      .category:hover {
        transform: translateY(-2px);
        box-shadow: var(--sc-shadow);
      }
      .name {
        font-weight: 600;
        margin-bottom: 4px;
      }
      .count {
        font-size: 12px;
        color: var(--sc-text-muted);
      }
      .rail {
        margin-bottom: 48px;
      }
      .rail h2 {
        font-size: 22px;
        margin: 0 0 16px;
      }
      .grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
        gap: 16px;
      }
      .muted {
        color: var(--sc-text-muted);
      }
    `,
  ],
})
export class HomeComponent implements OnInit {
  private readonly categoryApi = inject(CategoryApi);
  private readonly productApi = inject(ProductApi);
  private readonly offerApi = inject(OfferApi);

  readonly categories = signal<CategoryResponse[]>([]);
  readonly featured = signal<ProductWithOffer[]>([]);
  readonly loading = signal(true);

  async ngOnInit(): Promise<void> {
    try {
      const [cats, productsPage] = await Promise.all([
        firstValueFrom(this.categoryApi.list()),
        firstValueFrom(this.productApi.list({ size: 12 })),
      ]);
      this.categories.set(cats);

      const items = await Promise.all(
        productsPage.content.map(async (product) => {
          try {
            const offers = await firstValueFrom(this.offerApi.byProduct(product.id));
            const cheapest =
              offers.find((o) => o.status === 'ACTIVE') ?? offers[0] ?? null;
            return { product, offer: cheapest };
          } catch {
            return { product, offer: null };
          }
        }),
      );
      this.featured.set(items);
    } finally {
      this.loading.set(false);
    }
  }
}
