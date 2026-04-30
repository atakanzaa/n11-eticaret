import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { DecimalPipe, DatePipe } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { ProductApi } from '@core/api/product.api';
import { OfferApi } from '@core/api/offer.api';
import { InventoryApi } from '@core/api/inventory.api';
import { ReviewApi } from '@core/api/review.api';
import { RecommendationApi } from '@core/api/recommendation.api';
import { CartService } from '@core/cart.service';
import { ToastService } from '@core/toast.service';
import { I18nService } from '@core/i18n/i18n.service';
import { ProductResponse } from '@core/models/product.types';
import { OfferResponse } from '@core/models/offer.types';
import { InventoryItemResponse } from '@core/models/inventory.types';
import { ReviewResponse } from '@core/models/review.types';
import { TPipe } from '@shared/i18n.pipe';
import { ImgPlaceholderComponent } from '@shared/ui/img-placeholder.component';

@Component({
  selector: 'sc-product',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, DecimalPipe, DatePipe, TPipe, ImgPlaceholderComponent],
  template: `
    @if (loading()) {
      <p class="muted">{{ 'common.loading' | t }}</p>
    } @else if (product()) {
      <div class="layout">
        <div class="gallery">
          @if (product()!.primaryImageUrl) {
            <img [src]="product()!.primaryImageUrl" [alt]="product()!.title" />
          } @else {
            <sc-img-placeholder [label]="product()!.title" ratio="4 / 3" />
          }
        </div>

        <div class="info">
          <h1>{{ product()!.title }}</h1>

          @if (product()!.reviewCount > 0) {
            <div class="rating">
              ★ {{ product()!.averageRating | number: '1.1-1' }}
              <span class="muted">({{ product()!.reviewCount }} değerlendirme)</span>
            </div>
          }

          @if (offer()) {
            <div class="price-block">
              @if (offer()!.listPrice && offer()!.listPrice! > offer()!.price) {
                <span class="strike">{{ formatPrice(offer()!.listPrice!) }}</span>
              }
              <span class="price">{{ formatPrice(offer()!.price) }}</span>
            </div>

            <div class="stock" [class]="stockClass()">{{ stockLabel() }}</div>

            <div class="actions">
              <button
                type="button"
                class="primary"
                (click)="addToCart()"
                [disabled]="adding() || stockClass() === 'stk-out'"
              >
                {{ 'common.addToCart' | t }}
              </button>
              <button
                type="button"
                class="secondary"
                (click)="buyNow()"
                [disabled]="adding() || stockClass() === 'stk-out'"
              >
                {{ 'common.buyNow' | t }}
              </button>
            </div>
          } @else {
            <p class="muted">Bu ürün için aktif satıcı bulunamadı.</p>
          }

          @if (product()!.description) {
            <section class="desc">
              <h3>{{ 'product.description' | t }}</h3>
              <p>{{ product()!.description }}</p>
            </section>
          }

          @if (reviews().length > 0) {
            <section class="reviews">
              <h3>{{ 'product.reviews' | t }}</h3>
              @for (r of reviews(); track r.id) {
                <article class="review">
                  <header>
                    <strong>{{ r.userDisplayName }}</strong>
                    <span class="stars">★ {{ r.rating }}</span>
                    <span class="muted">{{ r.createdAt | date: 'mediumDate' }}</span>
                  </header>
                  @if (r.title) {
                    <h4>{{ r.title }}</h4>
                  }
                  @if (r.comment) {
                    <p>{{ r.comment }}</p>
                  }
                </article>
              }
            </section>
          } @else {
            <section class="reviews">
              <h3>{{ 'product.reviews' | t }}</h3>
              <p class="muted">{{ 'product.noReviews' | t }}</p>
            </section>
          }
        </div>
      </div>
    } @else {
      <p class="muted">Ürün bulunamadı.</p>
    }
  `,
  styles: [
    `
      .layout {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 32px;
      }
      .gallery img {
        width: 100%;
        aspect-ratio: 4 / 3;
        object-fit: cover;
        border-radius: var(--sc-radius);
      }
      .info h1 {
        font-size: 28px;
        margin: 0 0 8px;
      }
      .rating {
        margin-bottom: 16px;
        font-size: 15px;
      }
      .price-block {
        display: flex;
        align-items: baseline;
        gap: 12px;
        margin: 24px 0 12px;
      }
      .strike {
        text-decoration: line-through;
        color: var(--sc-text-faint);
        font-size: 16px;
      }
      .price {
        font-size: 32px;
        font-weight: 700;
        color: var(--sc-primary);
      }
      .stock {
        margin-bottom: 24px;
        font-size: 14px;
        font-weight: 600;
      }
      .stk-ok {
        color: var(--sc-success);
      }
      .stk-low {
        color: var(--sc-warn);
      }
      .stk-out {
        color: var(--sc-danger);
      }
      .actions {
        display: flex;
        gap: 12px;
        margin-bottom: 32px;
      }
      .primary,
      .secondary {
        flex: 1;
        padding: 14px;
        border: 0;
        border-radius: var(--sc-radius);
        font-weight: 600;
        font-size: 15px;
        cursor: pointer;
      }
      .primary {
        background: var(--sc-primary);
        color: white;
      }
      .secondary {
        background: var(--sc-text);
        color: white;
      }
      .primary:disabled,
      .secondary:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }
      section h3 {
        margin: 24px 0 12px;
        font-size: 18px;
      }
      .review {
        border-top: 1px solid var(--sc-border);
        padding: 16px 0;
      }
      .review header {
        display: flex;
        gap: 12px;
        align-items: baseline;
        margin-bottom: 4px;
      }
      .stars {
        color: var(--sc-warn);
        font-weight: 600;
      }
      .muted {
        color: var(--sc-text-muted);
      }
    `,
  ],
})
export class ProductComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly productApi = inject(ProductApi);
  private readonly offerApi = inject(OfferApi);
  private readonly inventoryApi = inject(InventoryApi);
  private readonly reviewApi = inject(ReviewApi);
  private readonly cart = inject(CartService);
  private readonly toast = inject(ToastService);
  private readonly i18n = inject(I18nService);

  readonly product = signal<ProductResponse | null>(null);
  readonly offer = signal<OfferResponse | null>(null);
  readonly inventory = signal<InventoryItemResponse | null>(null);
  readonly reviews = signal<ReviewResponse[]>([]);
  readonly loading = signal(true);
  readonly adding = signal(false);

  async ngOnInit(): Promise<void> {
    const id = this.route.snapshot.params['id'] as string;
    if (!id) {
      this.loading.set(false);
      return;
    }
    try {
      const product = await firstValueFrom(this.productApi.byId(id));
      this.product.set(product);

      const offers = await firstValueFrom(this.offerApi.byProduct(product.id));
      const chosen = offers.find((o) => o.status === 'ACTIVE') ?? offers[0] ?? null;
      this.offer.set(chosen);

      if (chosen) {
        try {
          const inv = await firstValueFrom(this.inventoryApi.byOffer(chosen.id));
          this.inventory.set(inv);
        } catch {
          /* inventory missing — render without stock badge */
        }
      }

      const reviewsPage = await firstValueFrom(this.reviewApi.listByProduct(product.id, 0, 5));
      this.reviews.set(reviewsPage.content);
    } finally {
      this.loading.set(false);
    }
  }

  stockClass(): string {
    const inv = this.inventory();
    if (!inv) return 'stk-ok';
    if (inv.availableQuantity <= 0) return 'stk-out';
    if (inv.availableQuantity <= inv.lowStockThreshold) return 'stk-low';
    return 'stk-ok';
  }

  stockLabel(): string {
    const inv = this.inventory();
    if (!inv) return this.i18n.t('product.inStock');
    if (inv.availableQuantity <= 0) return this.i18n.t('product.outOfStock');
    if (inv.availableQuantity <= inv.lowStockThreshold) {
      return this.i18n.t('product.lowStock', { count: inv.availableQuantity });
    }
    return this.i18n.t('product.inStock');
  }

  async addToCart(): Promise<void> {
    const offer = this.offer();
    if (!offer) return;
    this.adding.set(true);
    try {
      await this.cart.addItem(offer.id, 1);
      this.toast.show(this.i18n.t('common.addToCart') + ' ✓', 'success');
    } finally {
      this.adding.set(false);
    }
  }

  async buyNow(): Promise<void> {
    await this.addToCart();
    this.router.navigate(['/sepet']);
  }

  formatPrice(value: number): string {
    return new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(value);
  }
}
