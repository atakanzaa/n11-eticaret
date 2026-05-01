import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { DecimalPipe, DatePipe, KeyValuePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';

import { ProductApi } from '@core/api/product.api';
import { OfferApi } from '@core/api/offer.api';
import { InventoryApi } from '@core/api/inventory.api';
import { ReviewApi } from '@core/api/review.api';
import { RecommendationApi } from '@core/api/recommendation.api';
import { SellerApi } from '@core/api/seller.api';
import { CampaignApi } from '@core/api/campaign.api';
import { CampaignResponse } from '@core/models/campaign.types';
import { CartService } from '@core/cart.service';
import { ToastService } from '@core/toast.service';
import { I18nService } from '@core/i18n/i18n.service';
import { AuthStateService } from '@core/auth/auth-state.service';

import { ProductResponse } from '@core/models/product.types';
import { OfferResponse } from '@core/models/offer.types';
import { InventoryItemResponse } from '@core/models/inventory.types';
import { SellerDto } from '@core/models/seller.types';
import {
  ReviewResponse,
  ReviewStatsResponse,
  CreateReviewRequest,
  UpdateReviewRequest,
} from '@core/models/review.types';
import { Page } from '@core/models/common.types';

import { BreadcrumbComponent, BreadcrumbItem } from '@shared/ui/breadcrumb/breadcrumb.component';
import { SpinnerComponent } from '@shared/ui/spinner/spinner.component';
import { EmptyStateComponent } from '@shared/ui/empty-state/empty-state.component';
import { StarRatingComponent } from '@shared/ui/star-rating/star-rating.component';
import { PriceDisplayComponent } from '@shared/ui/price-display/price-display.component';
import { ReviewCardComponent } from '@shared/ui/review-card/review-card.component';
import { TabsComponent } from '@shared/ui/tabs/tabs.component';
import { TabPanelComponent } from '@shared/ui/tabs/tab-panel.component';
import { ImgPlaceholderComponent } from '@shared/ui/img-placeholder/img-placeholder.component';
import { PaginationComponent } from '@shared/ui/pagination/pagination.component';
import { ProductCardComponent } from '@shared/ui/product-card/product-card.component';
import { TPipe } from '@shared/i18n.pipe';

@Component({
  selector: 'sc-product',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './product.component.html',
  styleUrls: ['./product.component.scss'],
  imports: [
    RouterLink,
    DecimalPipe,
    DatePipe,
    KeyValuePipe,
    FormsModule,
    BreadcrumbComponent,
    SpinnerComponent,
    EmptyStateComponent,
    StarRatingComponent,
    PriceDisplayComponent,
    ReviewCardComponent,
    TabsComponent,
    TabPanelComponent,
    ImgPlaceholderComponent,
    PaginationComponent,
    ProductCardComponent,
    TPipe,
  ],
})
export class ProductComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly productApi = inject(ProductApi);
  private readonly offerApi = inject(OfferApi);
  private readonly inventoryApi = inject(InventoryApi);
  private readonly reviewApi = inject(ReviewApi);
  private readonly recommendationApi = inject(RecommendationApi);
  private readonly sellerApi = inject(SellerApi);
  private readonly campaignApi = inject(CampaignApi);
  private readonly cart = inject(CartService);
  private readonly toast = inject(ToastService);
  private readonly i18n = inject(I18nService);
  readonly auth = inject(AuthStateService);

  // ── Core data signals ───────────────────────────────────────────
  readonly product = signal<ProductResponse | null>(null);
  readonly offer = signal<OfferResponse | null>(null);
  readonly inventory = signal<InventoryItemResponse | null>(null);
  readonly seller = signal<SellerDto | null>(null);
  readonly campaign = signal<CampaignResponse | null>(null);
  readonly loading = signal(true);
  readonly adding = signal(false);

  // ── Gallery ─────────────────────────────────────────────────────
  readonly imgIdx = signal(0);

  readonly imageUrls = computed(() => {
    const p = this.product();
    if (!p) return [];
    if (p.images && p.images.length > 0) {
      return p.images
        .sort((a, b) => a.displayOrder - b.displayOrder)
        .map(img => img.url);
    }
    return p.primaryImageUrl ? [p.primaryImageUrl] : [];
  });

  readonly mainImage = computed(() => {
    const urls = this.imageUrls();
    const idx = this.imgIdx();
    return urls[idx] ?? null;
  });

  // ── Variant selection ───────────────────────────────────────────
  readonly selectedVariant = signal<string | null>(null);

  // ── Price / discount ────────────────────────────────────────────
  readonly discountPct = computed(() => {
    const o = this.offer();
    if (!o?.listPrice || o.listPrice <= o.price) return null;
    return Math.round(((o.listPrice - o.price) / o.listPrice) * 100);
  });

  // ── Stock status ────────────────────────────────────────────────
  readonly stockClass = computed(() => {
    const inv = this.inventory();
    if (!inv) return 'stk-ok';
    if (inv.availableQuantity <= 0) return 'stk-out';
    if (inv.availableQuantity <= inv.lowStockThreshold) return 'stk-low';
    return 'stk-ok';
  });

  readonly stockLabel = computed(() => {
    const inv = this.inventory();
    if (!inv) return this.i18n.t('product.inStock');
    if (inv.availableQuantity <= 0) return this.i18n.t('product.outOfStock');
    if (inv.availableQuantity <= inv.lowStockThreshold) {
      return this.i18n.t('product.lowStock', { count: inv.availableQuantity });
    }
    return this.i18n.t('product.inStock');
  });

  readonly isOutOfStock = computed(() => this.stockClass() === 'stk-out');

  // ── Breadcrumb ──────────────────────────────────────────────────
  readonly breadcrumbs = computed<BreadcrumbItem[]>(() => {
    const p = this.product();
    if (!p) return [];
    return [
      { label: 'Anasayfa', route: ['/'] },
      { label: 'Urunler', route: ['/arama'] },
      { label: p.title },
    ];
  });

  // ── Attributes ──────────────────────────────────────────────────
  readonly attributeEntries = computed(() => {
    const p = this.product();
    if (!p?.attributes) return [];
    return Object.entries(p.attributes).map(([key, value]) => ({
      key,
      value: String(value ?? ''),
    }));
  });

  // ── Reviews ─────────────────────────────────────────────────────
  readonly reviewStats = signal<ReviewStatsResponse | null>(null);
  readonly reviewsPage = signal<Page<ReviewResponse> | null>(null);
  readonly reviews = computed(() => this.reviewsPage()?.content ?? []);
  readonly reviewCurrentPage = signal(0);
  readonly reviewPageSize = 5;

  readonly ratingDistribution = computed(() => {
    const stats = this.reviewStats();
    if (!stats) return [];
    const total = stats.totalCount || 1;
    return [5, 4, 3, 2, 1].map(star => ({
      star,
      count: stats.distribution[star] ?? 0,
      pct: Math.round(((stats.distribution[star] ?? 0) / total) * 100),
    }));
  });

  // ── Review form ─────────────────────────────────────────────────
  readonly myReview = signal<ReviewResponse | null>(null);
  readonly showReviewForm = signal(false);
  readonly reviewFormRating = signal(0);
  readonly reviewFormTitle = signal('');
  readonly reviewFormComment = signal('');
  readonly submittingReview = signal(false);

  // ── Similar products ────────────────────────────────────────────
  readonly similarProducts = signal<ProductResponse[]>([]);

  // ── Favourite ───────────────────────────────────────────────────
  readonly fav = signal(false);

  // ── Quantity ─────────────────────────────────────────────────────
  readonly quantity = signal(1);

  // ── Init ────────────────────────────────────────────────────────
  async ngOnInit(): Promise<void> {
    const id = this.route.snapshot.params['id'] as string;
    if (!id) {
      this.loading.set(false);
      return;
    }

    try {
      const product = await firstValueFrom(this.productApi.byId(id));
      this.product.set(product);

      // Parallel fetches: offers, review stats, recommendations, seller, track view
      const [offers, stats] = await Promise.all([
        firstValueFrom(this.offerApi.byProduct(product.id)),
        firstValueFrom(this.reviewApi.stats(product.id)).catch(() => null),
      ]);

      this.reviewStats.set(stats);

      const chosen = offers.find(o => o.status === 'ACTIVE') ?? offers[0] ?? null;
      this.offer.set(chosen);

      // Inventory + seller + reviews + recommendations in parallel
      const parallel: Promise<void>[] = [];

      if (chosen) {
        parallel.push(
          firstValueFrom(this.inventoryApi.byOffer(chosen.id))
            .then(inv => this.inventory.set(inv))
            .catch(() => { /* inventory missing */ }),
        );
        parallel.push(
          firstValueFrom(this.sellerApi.byId(chosen.sellerId))
            .then(s => this.seller.set(s))
            .catch(() => { /* seller fetch failed */ }),
        );
        parallel.push(
          firstValueFrom(this.campaignApi.activeByOffer(chosen.id))
            .then(c => this.campaign.set(c))
            .catch(() => { /* no active campaign */ }),
        );
      }

      parallel.push(this.loadReviews(0));
      parallel.push(this.loadSimilarProducts(product.id));
      parallel.push(
        firstValueFrom(this.recommendationApi.trackView(product.id)).catch(() => {}),
      );
      if (this.auth.isAuthenticated()) {
        parallel.push(
          firstValueFrom(this.reviewApi.myReviewForProduct(product.id))
            .then(r => this.myReview.set(r))
            .catch(() => { /* not reviewed yet */ }),
        );
      }

      await Promise.all(parallel);
    } finally {
      this.loading.set(false);
    }
  }

  // ── Review loading ──────────────────────────────────────────────
  async loadReviews(page: number): Promise<void> {
    const p = this.product();
    if (!p) return;
    try {
      const result = await firstValueFrom(
        this.reviewApi.listByProduct(p.id, page, this.reviewPageSize),
      );
      this.reviewsPage.set(result);
      this.reviewCurrentPage.set(page);
    } catch {
      /* reviews fetch failed silently */
    }
  }

  onReviewPageChange(page: number): void {
    this.loadReviews(page);
  }

  // ── Similar products ────────────────────────────────────────────
  private async loadSimilarProducts(productId: string): Promise<void> {
    try {
      const recs = await firstValueFrom(this.recommendationApi.related(productId, 4));
      const products = await Promise.all(
        recs.map(r =>
          firstValueFrom(this.productApi.byId(r.productId)).catch(() => null),
        ),
      );
      this.similarProducts.set(products.filter((p): p is ProductResponse => p !== null));
    } catch {
      /* recommendations unavailable */
    }
  }

  // ── Cart actions ────────────────────────────────────────────────
  async addToCart(): Promise<void> {
    const offer = this.offer();
    if (!offer) return;
    this.adding.set(true);
    try {
      await this.cart.addItem(offer.id, this.quantity());
      this.toast.show(this.i18n.t('common.addToCart') + ' ✓', 'success');
    } finally {
      this.adding.set(false);
    }
  }

  async buyNow(): Promise<void> {
    await this.addToCart();
    this.router.navigate(['/sepet']);
  }

  // ── Quantity control ────────────────────────────────────────────
  decrementQty(): void {
    const q = this.quantity();
    if (q > 1) this.quantity.set(q - 1);
  }

  incrementQty(): void {
    const inv = this.inventory();
    const max = inv?.availableQuantity ?? 99;
    const q = this.quantity();
    if (q < max) this.quantity.set(q + 1);
  }

  // ── Gallery ─────────────────────────────────────────────────────
  selectImage(index: number): void {
    this.imgIdx.set(index);
  }

  // ── Review form ─────────────────────────────────────────────────
  toggleReviewForm(): void {
    const willShow = !this.showReviewForm();
    if (willShow) {
      const existing = this.myReview();
      this.reviewFormRating.set(existing?.rating ?? 0);
      this.reviewFormTitle.set(existing?.title ?? '');
      this.reviewFormComment.set(existing?.comment ?? '');
    }
    this.showReviewForm.set(willShow);
  }

  onNewRating(rating: number): void {
    this.reviewFormRating.set(rating);
  }

  async submitReview(): Promise<void> {
    const p = this.product();
    const user = this.auth.currentUser();
    if (!p || !user) return;

    const rating = this.reviewFormRating();
    if (rating === 0) {
      this.toast.show('Lutfen bir puan secin', 'warn');
      return;
    }

    this.submittingReview.set(true);
    try {
      const existing = this.myReview();
      let saved: ReviewResponse;
      if (existing) {
        const updateRequest: UpdateReviewRequest = {
          rating,
          title: this.reviewFormTitle() || undefined,
          comment: this.reviewFormComment() || undefined,
        };
        saved = await firstValueFrom(this.reviewApi.update(existing.id, updateRequest));
        this.toast.show(this.i18n.t('product.reviewUpdated'), 'success');
      } else {
        const createRequest: CreateReviewRequest = {
          rating,
          title: this.reviewFormTitle() || undefined,
          comment: this.reviewFormComment() || undefined,
        };
        const displayName = `${user.firstName} ${user.lastName}`.trim() || user.email;
        saved = await firstValueFrom(
          this.reviewApi.create(p.id, displayName, createRequest),
        );
        this.toast.show(this.i18n.t('product.reviewSubmitted'), 'success');
      }

      this.myReview.set(saved);
      this.showReviewForm.set(false);

      // Reload reviews and stats
      await Promise.all([
        this.loadReviews(0),
        firstValueFrom(this.reviewApi.stats(p.id))
          .then(s => this.reviewStats.set(s))
          .catch(() => {}),
      ]);
    } catch {
      this.toast.show(this.i18n.t('product.reviewFailed'), 'danger');
    } finally {
      this.submittingReview.set(false);
    }
  }

  // ── Review voting ───────────────────────────────────────────────
  async voteHelpful(reviewId: string): Promise<void> {
    try {
      const updated = await firstValueFrom(this.reviewApi.vote(reviewId, 'HELPFUL'));
      const page = this.reviewsPage();
      if (page) {
        const content = page.content.map(r => (r.id === updated.id ? updated : r));
        this.reviewsPage.set({ ...page, content });
      }
    } catch {
      /* vote failed */
    }
  }

  // ── Favourite ───────────────────────────────────────────────────
  toggleFavourite(): void {
    this.fav.set(!this.fav());
  }
}
