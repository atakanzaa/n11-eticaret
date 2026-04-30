import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ImgPlaceholderComponent } from './img-placeholder.component';
import { ProductResponse } from '@core/models/product.types';
import { OfferResponse } from '@core/models/offer.types';
import { CartService } from '@core/cart.service';
import { ToastService } from '@core/toast.service';
import { I18nService } from '@core/i18n/i18n.service';

@Component({
  selector: 'sc-product-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, DecimalPipe, ImgPlaceholderComponent],
  template: `
    <a class="card" [routerLink]="['/urun', product().id]">
      @if (product().primaryImageUrl) {
        <img [src]="product().primaryImageUrl" [alt]="product().title" class="img" loading="lazy" />
      } @else {
        <sc-img-placeholder [label]="product().title" />
      }

      <div class="meta">
        <h3 class="title">{{ product().title }}</h3>

        @if (offer()) {
          <div class="price-row">
            @if (offer()!.listPrice && offer()!.listPrice! > offer()!.price) {
              <span class="strike">{{ formatPrice(offer()!.listPrice!) }}</span>
              <span class="discount">%{{ discountPercent() }}</span>
            }
            <span class="price">{{ formatPrice(offer()!.price) }}</span>
          </div>
        }

        @if (product().reviewCount > 0) {
          <div class="rating">★ {{ product().averageRating | number: '1.1-1' }} ({{ product().reviewCount }})</div>
        }
      </div>

      @if (offer()) {
        <button type="button" class="add" (click)="onAdd($event)" [disabled]="adding">+ Sepet</button>
      }
    </a>
  `,
  styles: [
    `
      .card {
        display: block;
        background: var(--sc-surface);
        border: 1px solid var(--sc-border);
        border-radius: var(--sc-radius);
        text-decoration: none;
        color: inherit;
        position: relative;
        overflow: hidden;
        transition: box-shadow 0.15s, transform 0.15s;
      }
      .card:hover {
        box-shadow: var(--sc-shadow);
        transform: translateY(-1px);
      }
      .img {
        width: 100%;
        aspect-ratio: 1 / 1;
        object-fit: cover;
        display: block;
      }
      .meta {
        padding: 12px 14px;
      }
      .title {
        font-size: 14px;
        margin: 0 0 8px;
        line-height: 1.3;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
      }
      .price-row {
        display: flex;
        align-items: baseline;
        gap: 8px;
        flex-wrap: wrap;
      }
      .price {
        font-weight: 700;
        font-size: 18px;
        color: var(--sc-text);
      }
      .strike {
        text-decoration: line-through;
        color: var(--sc-text-faint);
        font-size: 13px;
      }
      .discount {
        background: var(--sc-primary-50);
        color: var(--sc-primary);
        font-size: 12px;
        padding: 2px 6px;
        border-radius: 4px;
        font-weight: 600;
      }
      .rating {
        font-size: 12px;
        color: var(--sc-text-muted);
        margin-top: 6px;
      }
      .add {
        position: absolute;
        bottom: 12px;
        right: 12px;
        background: var(--sc-primary);
        color: white;
        border: 0;
        padding: 8px 12px;
        border-radius: var(--sc-radius-sm);
        font-size: 12px;
        font-weight: 600;
        cursor: pointer;
        opacity: 0;
        transition: opacity 0.15s;
      }
      .add:disabled {
        opacity: 0.6;
      }
      .card:hover .add {
        opacity: 1;
      }
    `,
  ],
})
export class ProductCardComponent {
  product = input.required<ProductResponse>();
  offer = input<OfferResponse | null>(null);

  private readonly cart = inject(CartService);
  private readonly toast = inject(ToastService);
  private readonly i18n = inject(I18nService);

  adding = false;

  discountPercent = computed(() => {
    const o = this.offer();
    if (!o?.listPrice || o.listPrice <= o.price) return 0;
    return Math.round(((o.listPrice - o.price) / o.listPrice) * 100);
  });

  async onAdd(event: Event): Promise<void> {
    event.preventDefault();
    event.stopPropagation();
    const offer = this.offer();
    if (!offer) return;
    this.adding = true;
    try {
      await this.cart.addItem(offer.id, 1);
      this.toast.show(this.i18n.t('common.addToCart') + ' ✓', 'success');
    } catch {
      /* error.interceptor surfaces a toast */
    } finally {
      this.adding = false;
    }
  }

  formatPrice(value: number): string {
    return new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(value);
  }
}
