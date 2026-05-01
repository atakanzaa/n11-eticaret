import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  OnDestroy,
  inject,
  signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { CategoryApi } from '@core/api/category.api';
import { ProductApi } from '@core/api/product.api';
import { OfferApi } from '@core/api/offer.api';
import { RecommendationApi } from '@core/api/recommendation.api';
import { CategoryResponse } from '@core/models/category.types';
import { ProductResponse } from '@core/models/product.types';
import { OfferResponse } from '@core/models/offer.types';
import { ProductCardComponent } from '@shared/ui/product-card/product-card.component';
import { SpinnerComponent } from '@shared/ui/spinner/spinner.component';
import { EmptyStateComponent } from '@shared/ui/empty-state/empty-state.component';

export interface ProductWithOffer {
  product: ProductResponse;
  offer: OfferResponse | null;
}

interface HeroSlide {
  bg: string;
  kicker: string;
  title: string;
  sub: string;
  link: string;
  art: string;
}

@Component({
  selector: 'sc-home',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, ProductCardComponent, SpinnerComponent, EmptyStateComponent],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss'],
})
export class HomeComponent implements OnInit, OnDestroy {
  private readonly categoryApi = inject(CategoryApi);
  private readonly productApi = inject(ProductApi);
  private readonly offerApi = inject(OfferApi);
  private readonly recommendationApi = inject(RecommendationApi);

  readonly categories = signal<CategoryResponse[]>([]);
  readonly featured = signal<ProductWithOffer[]>([]);
  readonly personalized = signal<ProductWithOffer[]>([]);
  readonly bestSellers = signal<ProductWithOffer[]>([]);
  readonly loading = signal(true);
  readonly heroIdx = signal(0);

  private heroTimer: ReturnType<typeof setInterval> | null = null;

  readonly slides: HeroSlide[] = [
    {
      bg: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
      kicker: 'Yeni Sezon',
      title: 'Yaza ozel firsatlar basliyor',
      sub: 'En yeni urunler, en uygun fiyatlarla seni bekliyor.',
      link: '/arama',
      art: 'SUMMER 2026',
    },
    {
      bg: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
      kicker: 'Kampanya',
      title: 'Secili urunlerde buyuk indirim',
      sub: 'Binlerce urunde kacirılmayacak firsatlar.',
      link: '/arama',
      art: 'MEGA SALE',
    },
    {
      bg: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
      kicker: 'Ucretsiz Kargo',
      title: 'Tum siparislerde ucretsiz kargo',
      sub: 'Sinir yok, kosul yok. Hemen alisverise basla.',
      link: '/arama',
      art: 'FREE SHIPPING',
    },
  ];

  async ngOnInit(): Promise<void> {
    this.heroTimer = setInterval(() => {
      this.heroIdx.set((this.heroIdx() + 1) % this.slides.length);
    }, 5000);

    try {
      const [cats, productsPage] = await Promise.all([
        firstValueFrom(this.categoryApi.list()),
        firstValueFrom(this.productApi.list({ size: 8 })),
      ]);
      this.categories.set(cats);

      const items = await this.enrichWithOffers(productsPage.content);
      this.featured.set(items);

      // Load recommendation rails opportunistically
      this.loadPersonalized();
      this.loadBestSellers();
    } catch {
      /* core data failed — page still renders */
    } finally {
      this.loading.set(false);
    }
  }

  ngOnDestroy(): void {
    if (this.heroTimer) {
      clearInterval(this.heroTimer);
    }
  }

  private async loadPersonalized(): Promise<void> {
    try {
      const recs = await firstValueFrom(this.recommendationApi.forMe(4));
      const products = await Promise.all(
        recs.map((r) => firstValueFrom(this.productApi.byId(r.productId)).catch(() => null)),
      );
      const valid = products.filter((p): p is ProductResponse => p !== null);
      const items = await this.enrichWithOffers(valid);
      this.personalized.set(items);
    } catch {
      /* recommendation service unavailable */
    }
  }

  private async loadBestSellers(): Promise<void> {
    try {
      const recs = await firstValueFrom(this.recommendationApi.popular(4));
      const products = await Promise.all(
        recs.map((r) => firstValueFrom(this.productApi.byId(r.productId)).catch(() => null)),
      );
      const valid = products.filter((p): p is ProductResponse => p !== null);
      const items = await this.enrichWithOffers(valid);
      this.bestSellers.set(items);
    } catch {
      /* recommendation service unavailable */
    }
  }

  private async enrichWithOffers(products: ProductResponse[]): Promise<ProductWithOffer[]> {
    return Promise.all(
      products.map(async (product) => {
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
  }

  /** Get a category tint color based on index */
  catTint(index: number): string {
    const tints = [
      'linear-gradient(135deg, #e0f7fa 0%, #b2ebf2 100%)',
      'linear-gradient(135deg, #fce4ec 0%, #f8bbd0 100%)',
      'linear-gradient(135deg, #e8f5e9 0%, #c8e6c9 100%)',
      'linear-gradient(135deg, #fff3e0 0%, #ffe0b2 100%)',
      'linear-gradient(135deg, #ede7f6 0%, #d1c4e9 100%)',
      'linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%)',
      'linear-gradient(135deg, #fff8e1 0%, #ffecb3 100%)',
      'linear-gradient(135deg, #fbe9e7 0%, #ffccbc 100%)',
    ];
    return tints[index % tints.length];
  }
}
