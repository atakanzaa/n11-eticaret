import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
  signal,
  computed,
  DestroyRef,
} from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ProductApi } from '@core/api/product.api';
import { OfferApi } from '@core/api/offer.api';
import { CategoryApi } from '@core/api/category.api';
import { BrandApi } from '@core/api/brand.api';
import { CategoryResponse } from '@core/models/category.types';
import { BrandResponse } from '@core/models/brand.types';
import { ProductResponse } from '@core/models/product.types';
import { OfferResponse } from '@core/models/offer.types';
import { Page } from '@core/models/common.types';
import { ProductCardComponent } from '@shared/ui/product-card/product-card.component';
import { SpinnerComponent } from '@shared/ui/spinner/spinner.component';
import { EmptyStateComponent } from '@shared/ui/empty-state/empty-state.component';
import { PaginationComponent } from '@shared/ui/pagination/pagination.component';
import { BreadcrumbComponent, BreadcrumbItem } from '@shared/ui/breadcrumb/breadcrumb.component';

export interface ProductWithOffer {
  product: ProductResponse;
  offer: OfferResponse | null;
}

@Component({
  selector: 'sc-search',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormsModule,
    RouterLink,
    DecimalPipe,
    ProductCardComponent,
    SpinnerComponent,
    EmptyStateComponent,
    PaginationComponent,
    BreadcrumbComponent,
  ],
  templateUrl: './search.component.html',
  styleUrls: ['./search.component.scss'],
})
export class SearchComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly productApi = inject(ProductApi);
  private readonly offerApi = inject(OfferApi);
  private readonly categoryApi = inject(CategoryApi);
  private readonly brandApi = inject(BrandApi);
  private readonly destroyRef = inject(DestroyRef);

  readonly categories = signal<CategoryResponse[]>([]);
  readonly brands = signal<BrandResponse[]>([]);
  readonly items = signal<ProductWithOffer[]>([]);
  readonly pageData = signal<Page<ProductResponse> | null>(null);
  readonly loading = signal(true);
  readonly heading = signal('Tum urunler');

  readonly selectedCategoryId = signal<string | undefined>(undefined);
  readonly selectedBrandId = signal<string | undefined>(undefined);
  readonly sort = signal('');
  readonly view = signal<'grid' | 'list'>('grid');
  readonly priceMin = signal<number | null>(null);
  readonly priceMax = signal<number | null>(null);
  readonly inStockOnly = signal(false);
  readonly selectedMinRating = signal<number | null>(null);

  readonly currentPage = signal(0);

  readonly breadcrumbs = computed<BreadcrumbItem[]>(() => {
    const items: BreadcrumbItem[] = [{ label: 'Anasayfa', route: ['/'] }];
    items.push({ label: this.heading() });
    return items;
  });

  readonly totalPages = computed(() => this.pageData()?.totalPages ?? 0);
  readonly totalElements = computed(() => this.pageData()?.totalElements ?? 0);
  readonly isFirst = computed(() => this.pageData()?.first ?? true);
  readonly isLast = computed(() => this.pageData()?.last ?? true);

  readonly activeFilterCount = computed(() => {
    let count = 0;
    if (this.selectedCategoryId()) count++;
    if (this.selectedBrandId()) count++;
    if (this.priceMin() !== null) count++;
    if (this.priceMax() !== null) count++;
    if (this.inStockOnly()) count++;
    if (this.selectedMinRating() !== null) count++;
    return count;
  });

  onMinRatingChange(value: number | null): void {
    this.selectedMinRating.set(value);
    this.onFilterChange();
  }

  async ngOnInit(): Promise<void> {
    const [cats, brs] = await Promise.all([
      firstValueFrom(this.categoryApi.list()).catch(() => []),
      firstValueFrom(this.brandApi.list()).catch(() => []),
    ]);
    this.categories.set(cats);
    this.brands.set(brs);

    this.route.params
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(async (params) => {
        const slug = params['slug'];
        if (slug) {
          try {
            const cat = await firstValueFrom(this.categoryApi.bySlug(slug));
            this.selectedCategoryId.set(cat.id);
            this.heading.set(cat.name);
          } catch {
            /* category not found, fallthrough */
          }
        }
        this.loadResults();
      });

    this.route.queryParams
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((qp) => {
        if (qp['q']) {
          this.heading.set(`"${qp['q']}" icin sonuclar`);
        }
        this.loadResults();
      });
  }

  onFilterChange(): void {
    this.currentPage.set(0);
    this.loadResults();
  }

  onPageChange(page: number): void {
    this.currentPage.set(page);
    this.loadResults();
  }

  onCategoryChange(id: string | undefined): void {
    this.selectedCategoryId.set(id || undefined);
    this.onFilterChange();
  }

  onBrandChange(id: string | undefined): void {
    this.selectedBrandId.set(id || undefined);
    this.onFilterChange();
  }

  onSortChange(value: string): void {
    this.sort.set(value);
    this.onFilterChange();
  }

  onPriceMinChange(value: string): void {
    this.priceMin.set(value ? Number(value) : null);
    this.onFilterChange();
  }

  onPriceMaxChange(value: string): void {
    this.priceMax.set(value ? Number(value) : null);
    this.onFilterChange();
  }

  onInStockChange(value: boolean): void {
    this.inStockOnly.set(value);
    this.onFilterChange();
  }

  clearFilters(): void {
    this.selectedCategoryId.set(undefined);
    this.selectedBrandId.set(undefined);
    this.priceMin.set(null);
    this.priceMax.set(null);
    this.inStockOnly.set(false);
    this.selectedMinRating.set(null);
    this.sort.set('');
    this.onFilterChange();
  }

  getCategoryName(id: string | undefined): string {
    if (!id) return '';
    return this.categories().find((c) => c.id === id)?.name ?? '';
  }

  getBrandName(id: string | undefined): string {
    if (!id) return '';
    return this.brands().find((b) => b.id === id)?.name ?? '';
  }

  private async loadResults(): Promise<void> {
    this.loading.set(true);
    try {
      const queryText = this.route.snapshot.queryParams['q'] as string | undefined;
      const result = await firstValueFrom(
        this.productApi.list({
          query: queryText,
          categoryId: this.selectedCategoryId(),
          brandId: this.selectedBrandId(),
          minRating: this.selectedMinRating() ?? undefined,
          page: this.currentPage(),
          size: 24,
          sort: this.sort() || undefined,
        }),
      );
      this.pageData.set(result);

      const enriched = await Promise.all(
        result.content.map(async (product) => {
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
      this.items.set(enriched);
    } finally {
      this.loading.set(false);
    }
  }
}
