import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ImgPlaceholderComponent } from '@shared/ui/img-placeholder/img-placeholder.component';
import { PriceDisplayComponent } from '@shared/ui/price-display/price-display.component';
import { StarRatingComponent } from '@shared/ui/star-rating/star-rating.component';
import { ProductResponse } from '@core/models/product.types';
import { OfferResponse } from '@core/models/offer.types';
import { CartService } from '@core/cart.service';
import { ToastService } from '@core/toast.service';
import { I18nService } from '@core/i18n/i18n.service';

@Component({
  selector: 'sc-product-card',
  standalone: true,
  imports: [RouterLink, ImgPlaceholderComponent, PriceDisplayComponent, StarRatingComponent],
  templateUrl: './product-card.component.html',
  styleUrls: ['./product-card.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
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
}
