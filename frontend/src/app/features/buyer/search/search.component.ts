import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { ProductApi } from '@core/api/product.api';
import { OfferApi } from '@core/api/offer.api';
import { CategoryApi } from '@core/api/category.api';
import { BrandApi } from '@core/api/brand.api';
import { CategoryResponse } from '@core/models/category.types';
import { BrandResponse } from '@core/models/brand.types';
import { ProductResponse } from '@core/models/product.types';
import { OfferResponse } from '@core/models/offer.types';
import { Page } from '@core/models/common.types';
import { ProductCardComponent } from '@shared/ui/product-card.component';
import { TPipe } from '@shared/i18n.pipe';

interface ProductWithOffer {
  product: ProductResponse;
  offer: OfferResponse | null;
}

@Component({
  selector: 'sc-search',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, ProductCardComponent, TPipe],
  template: `
    <div class="layout">
      <aside class="filters">
        <h3>{{ 'common.filter' | t }}</h3>

        <div class="group">
          <h4>Kategori</h4>
          <select [(ngModel)]="selectedCategoryId" (change)="onFilterChange()">
            <option [ngValue]="undefined">Tümü</option>
            @for (c of categories(); track c.id) {
              <option [ngValue]="c.id">{{ c.name }}</option>
            }
          </select>
        </div>

        <div class="group">
          <h4>Marka</h4>
          <select [(ngModel)]="selectedBrandId" (change)="onFilterChange()">
            <option [ngValue]="undefined">Tümü</option>
            @for (b of brands(); track b.id) {
              <option [ngValue]="b.id">{{ b.name }}</option>
            }
          </select>
        </div>

        <div class="group">
          <h4>{{ 'common.sort' | t }}</h4>
          <select [(ngModel)]="sort" (change)="onFilterChange()">
            <option value="">Önerilen</option>
            <option value="title,asc">İsim (A-Z)</option>
          </select>
        </div>
      </aside>

      <section class="results">
        <header>
          <h1>{{ heading() }}</h1>
          @if (page()) {
            <span class="count">{{ page()!.totalElements }} sonuç</span>
          }
        </header>

        @if (loading()) {
          <p class="muted">{{ 'common.loading' | t }}</p>
        } @else if (items().length === 0) {
          <p class="muted">{{ 'common.empty' | t }}</p>
        } @else {
          <div class="grid">
            @for (item of items(); track item.product.id) {
              <sc-product-card [product]="item.product" [offer]="item.offer" />
            }
          </div>
          @if (page()!.totalPages > 1) {
            <div class="pager">
              <button type="button" (click)="prevPage()" [disabled]="page()!.first">{{ 'common.back' | t }}</button>
              <span>{{ page()!.number + 1 }} / {{ page()!.totalPages }}</span>
              <button type="button" (click)="nextPage()" [disabled]="page()!.last">{{ 'common.next' | t }}</button>
            </div>
          }
        }
      </section>
    </div>
  `,
  styles: [
    `
      .layout {
        display: grid;
        grid-template-columns: 240px 1fr;
        gap: 24px;
      }
      .filters {
        background: var(--sc-surface);
        border: 1px solid var(--sc-border);
        border-radius: var(--sc-radius);
        padding: 20px;
        align-self: start;
        position: sticky;
        top: 144px;
      }
      .filters h3 {
        margin: 0 0 16px;
      }
      .group {
        margin-bottom: 16px;
      }
      .group h4 {
        margin: 0 0 8px;
        font-size: 13px;
        color: var(--sc-text-muted);
        text-transform: uppercase;
        letter-spacing: 0.05em;
      }
      .group select {
        width: 100%;
        padding: 8px 10px;
        border: 1px solid var(--sc-border);
        border-radius: var(--sc-radius-sm);
      }
      .results header {
        display: flex;
        justify-content: space-between;
        align-items: baseline;
        margin-bottom: 16px;
      }
      .results h1 {
        font-size: 22px;
        margin: 0;
      }
      .count {
        color: var(--sc-text-muted);
        font-size: 14px;
      }
      .grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
        gap: 16px;
      }
      .muted {
        color: var(--sc-text-muted);
      }
      .pager {
        display: flex;
        gap: 16px;
        justify-content: center;
        align-items: center;
        margin-top: 24px;
      }
      .pager button {
        padding: 8px 16px;
        border: 1px solid var(--sc-border);
        background: white;
        border-radius: var(--sc-radius-sm);
        cursor: pointer;
      }
      .pager button:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }
    `,
  ],
})
export class SearchComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly productApi = inject(ProductApi);
  private readonly offerApi = inject(OfferApi);
  private readonly categoryApi = inject(CategoryApi);
  private readonly brandApi = inject(BrandApi);

  readonly categories = signal<CategoryResponse[]>([]);
  readonly brands = signal<BrandResponse[]>([]);
  readonly items = signal<ProductWithOffer[]>([]);
  readonly page = signal<Page<ProductResponse> | null>(null);
  readonly loading = signal(true);
  readonly heading = signal('Tüm ürünler');

  selectedCategoryId: string | undefined;
  selectedBrandId: string | undefined;
  sort = '';

  private currentPage = 0;

  async ngOnInit(): Promise<void> {
    const [cats, brs] = await Promise.all([
      firstValueFrom(this.categoryApi.list()).catch(() => []),
      firstValueFrom(this.brandApi.list()).catch(() => []),
    ]);
    this.categories.set(cats);
    this.brands.set(brs);

    this.route.params.subscribe(async (params) => {
      const slug = params['slug'];
      if (slug) {
        try {
          const cat = await firstValueFrom(this.categoryApi.bySlug(slug));
          this.selectedCategoryId = cat.id;
          this.heading.set(cat.name);
        } catch {
          /* category not found, fallthrough */
        }
      }
      this.loadResults();
    });

    this.route.queryParams.subscribe((qp) => {
      if (qp['q']) {
        this.heading.set(`"${qp['q']}" için sonuçlar`);
      }
      this.loadResults();
    });
  }

  onFilterChange(): void {
    this.currentPage = 0;
    this.loadResults();
  }

  prevPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.loadResults();
    }
  }

  nextPage(): void {
    const p = this.page();
    if (p && !p.last) {
      this.currentPage++;
      this.loadResults();
    }
  }

  private async loadResults(): Promise<void> {
    this.loading.set(true);
    try {
      const queryText = this.route.snapshot.queryParams['q'] as string | undefined;
      const result = await firstValueFrom(
        this.productApi.list({
          query: queryText,
          categoryId: this.selectedCategoryId,
          brandId: this.selectedBrandId,
          page: this.currentPage,
          size: 24,
          sort: this.sort || undefined,
        }),
      );
      this.page.set(result);

      const enriched = await Promise.all(
        result.content.map(async (product) => {
          try {
            const offers = await firstValueFrom(this.offerApi.byProduct(product.id));
            const cheapest = offers.find((o) => o.status === 'ACTIVE') ?? offers[0] ?? null;
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
