import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { FavouriteApi } from '@core/api/favourite.api';
import { ProductApi } from '@core/api/product.api';
import { OfferApi } from '@core/api/offer.api';
import { ProductResponse } from '@core/models/product.types';
import { OfferResponse } from '@core/models/offer.types';
import { Page } from '@core/models/common.types';
import { ToastService } from '@core/toast.service';
import { I18nService } from '@core/i18n/i18n.service';
import { TPipe } from '@shared/i18n.pipe';
import { SpinnerComponent } from '@shared/ui/spinner/spinner.component';
import { EmptyStateComponent } from '@shared/ui/empty-state/empty-state.component';
import { PaginationComponent } from '@shared/ui/pagination/pagination.component';
import { ProductCardComponent } from '@shared/ui/product-card/product-card.component';

type FavouriteRow = {
  favouriteId: string;
  product: ProductResponse;
  offer: OfferResponse | null;
};

@Component({
  selector: 'sc-favourites',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TPipe,
    SpinnerComponent,
    EmptyStateComponent,
    PaginationComponent,
    ProductCardComponent,
  ],
  templateUrl: './favourites.component.html',
  styleUrls: ['./favourites.component.scss'],
})
export class FavouritesComponent implements OnInit {
  private readonly favouriteApi = inject(FavouriteApi);
  private readonly productApi = inject(ProductApi);
  private readonly offerApi = inject(OfferApi);
  private readonly toast = inject(ToastService);
  private readonly i18n = inject(I18nService);

  readonly rows = signal<FavouriteRow[]>([]);
  readonly loading = signal(true);
  readonly removing = signal<string | null>(null); // productId being removed
  readonly currentPage = signal(0);
  readonly totalPages = signal(0);
  readonly isFirst = signal(true);
  readonly isLast = signal(true);

  readonly hasItems = computed(() => this.rows().length > 0);

  async ngOnInit(): Promise<void> {
    await this.loadPage(0);
  }

  async loadPage(page: number): Promise<void> {
    this.loading.set(true);
    try {
      const result: Page<{ id: string; productId: string; addedAt: string }> = await firstValueFrom(
        this.favouriteApi.list(page, 24),
      );
      this.currentPage.set(result.number);
      this.totalPages.set(result.totalPages);
      this.isFirst.set(result.first);
      this.isLast.set(result.last);

      const rows: (FavouriteRow | null)[] = await Promise.all(
        result.content.map(async (f): Promise<FavouriteRow | null> => {
          try {
            const product = await firstValueFrom(this.productApi.byId(f.productId));
            const offers = await firstValueFrom(this.offerApi.byProduct(f.productId)).catch(() => []);
            const offer = offers.find(o => o.status === 'ACTIVE') ?? offers[0] ?? null;
            return { favouriteId: f.id, product, offer };
          } catch {
            return null;
          }
        }),
      );
      this.rows.set(rows.filter((r): r is FavouriteRow => r !== null));
    } finally {
      this.loading.set(false);
    }
  }

  async remove(productId: string): Promise<void> {
    if (this.removing()) return;
    this.removing.set(productId);
    try {
      await firstValueFrom(this.favouriteApi.remove(productId));
      this.rows.update(arr => arr.filter(r => r.product.id !== productId));
      this.toast.show(this.i18n.t('product.removedFromFavourites'), 'success');
    } catch {
      // error.interceptor handles toast
    } finally {
      this.removing.set(null);
    }
  }

  onPageChange(page: number): void {
    this.loadPage(page);
  }
}
