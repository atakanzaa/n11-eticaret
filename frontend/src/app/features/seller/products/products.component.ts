import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
  signal,
  computed,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { OfferApi } from '@core/api/offer.api';
import { ProductApi } from '@core/api/product.api';
import { InventoryApi } from '@core/api/inventory.api';
import { CategoryApi } from '@core/api/category.api';
import { CampaignApi } from '@core/api/campaign.api';
import {
  CreateOfferRequest,
  OfferResponse,
  OfferStatus,
  UpdateOfferRequest,
} from '@core/models/offer.types';
import { CreateCampaignRequest } from '@core/models/campaign.types';
import { ProductResponse, CreateProductRequest } from '@core/models/product.types';
import { CategoryResponse } from '@core/models/category.types';
import { ToastService } from '@core/toast.service';
import { TPipe } from '@shared/i18n.pipe';
import { CurrencyFormatPipe } from '@shared/pipes/currency-format.pipe';
import { SpinnerComponent } from '@shared/ui/spinner/spinner.component';
import { EmptyStateComponent } from '@shared/ui/empty-state/empty-state.component';
import { StatusBadgeComponent } from '@shared/ui/status-badge/status-badge.component';
import { DataTableComponent, TableColumn } from '@shared/ui/data-table/data-table.component';
import { ScTableCellDirective } from '@shared/ui/data-table/table-cell.directive';
import { ModalComponent } from '@shared/ui/modal/modal.component';
import { ConfirmDialogComponent } from '@shared/ui/confirm-dialog/confirm-dialog.component';
import { FormFieldComponent } from '@shared/ui/form-field/form-field.component';

interface OfferRow {
  id: string;
  offer: OfferResponse;
  product: ProductResponse | null;
  available: number | null;
  title: string;
  price: number;
  listPrice: number | null;
  stock: number | null;
  status: string;
  categoryName: string;
}

@Component({
  selector: 'sc-seller-products',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './products.component.html',
  styleUrls: ['./products.component.scss'],
  imports: [
    FormsModule,
    TPipe,
    CurrencyFormatPipe,
    SpinnerComponent,
    EmptyStateComponent,
    StatusBadgeComponent,
    DataTableComponent,
    ScTableCellDirective,
    ModalComponent,
    ConfirmDialogComponent,
    FormFieldComponent,
  ],
})
export class SellerProductsComponent implements OnInit {
  private readonly offerApi = inject(OfferApi);
  private readonly productApi = inject(ProductApi);
  private readonly inventoryApi = inject(InventoryApi);
  private readonly categoryApi = inject(CategoryApi);
  private readonly campaignApi = inject(CampaignApi);
  private readonly toast = inject(ToastService);

  readonly rows = signal<OfferRow[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);

  // Categories from admin (for dropdown)
  readonly categories = signal<CategoryResponse[]>([]);

  // Modal state
  readonly showCreateModal = signal(false);
  readonly showEditModal = signal(false);
  readonly showDeleteConfirm = signal(false);
  readonly showCampaignModal = signal(false);
  readonly campaignTarget = signal<OfferRow | null>(null);
  readonly savingCampaign = signal(false);
  campaignForm = {
    discountedPrice: 0,
    startsAt: '',
    endsAt: '',
  };
  readonly editingOffer = signal<OfferRow | null>(null);
  readonly deletingOffer = signal<OfferRow | null>(null);

  // Create form — Step 1: Product details
  productForm = {
    title: '',
    description: '',
    shortDescription: '',
    categoryId: '',
    brandId: '',
  };

  // Create form — Step 2: Offer details
  offerForm = {
    sku: '',
    price: 0,
    listPrice: undefined as number | undefined,
    stock: 0,
    cargoProvider: 'DEFAULT' as string,
    cargoPrice: 0,
    estimatedDeliveryDays: 3,
    freeShippingThreshold: undefined as number | undefined,
  };

  // Create wizard step
  readonly createStep = signal<1 | 2>(1);

  // Edit form
  readonly editForm = signal<UpdateOfferRequest>({
    price: 0,
    listPrice: undefined,
    cargoProvider: undefined,
    cargoPrice: undefined,
    estimatedDeliveryDays: undefined,
    status: undefined,
  });

  readonly columns: TableColumn[] = [
    { key: 'title', label: 'Urun' },
    { key: 'price', label: 'Fiyat', width: '120px' },
    { key: 'listPrice', label: 'Liste Fiyati', width: '120px' },
    { key: 'stock', label: 'Stok', width: '80px' },
    { key: 'status', label: 'Durum', width: '100px' },
    { key: 'actions', label: '', width: '120px' },
  ];

  readonly rowCount = computed(() => this.rows().length);

  async ngOnInit(): Promise<void> {
    await Promise.all([this.refresh(), this.loadCategories()]);
  }

  private async loadCategories(): Promise<void> {
    try {
      const cats = await firstValueFrom(this.categoryApi.list());
      this.categories.set(cats);
    } catch {
      /* silent — categories just won't be available */
    }
  }

  async refresh(): Promise<void> {
    this.loading.set(true);
    try {
      const offers = await firstValueFrom(this.offerApi.myOffers());
      const cats = this.categories();
      const catMap = new Map(cats.map(c => [c.id, c.name]));

      const enriched: OfferRow[] = await Promise.all(
        offers.map(async (offer) => {
          const [product, inv] = await Promise.allSettled([
            firstValueFrom(this.productApi.byId(offer.productId)),
            firstValueFrom(this.inventoryApi.byOffer(offer.id)),
          ]);
          const prod = product.status === 'fulfilled' ? product.value : null;
          const avail = inv.status === 'fulfilled' ? inv.value.availableQuantity : null;
          return {
            id: offer.id,
            offer,
            product: prod,
            available: avail,
            title: prod?.title ?? offer.productId,
            price: offer.price,
            listPrice: offer.listPrice ?? null,
            stock: avail,
            status: offer.status,
            categoryName: prod?.categoryId ? (catMap.get(prod.categoryId) ?? '') : '',
          };
        }),
      );
      this.rows.set(enriched);
    } finally {
      this.loading.set(false);
    }
  }

  statusVariant(status: string): 'success' | 'warning' | 'danger' | 'neutral' {
    const map: Record<string, 'success' | 'warning' | 'danger' | 'neutral'> = {
      ACTIVE: 'success',
      PAUSED: 'warning',
      INACTIVE: 'neutral',
      DELISTED: 'danger',
    };
    return map[status] ?? 'neutral';
  }

  // Helper: get category name by id
  getCategoryName(categoryId: string): string {
    const cat = this.categories().find(c => c.id === categoryId);
    return cat?.name ?? '';
  }

  // ── Create (2-step wizard) ────────────────────────────
  openCreateModal(): void {
    this.productForm = {
      title: '',
      description: '',
      shortDescription: '',
      categoryId: '',
      brandId: '',
    };
    this.offerForm = {
      sku: '',
      price: 0,
      listPrice: undefined,
      stock: 0,
      cargoProvider: 'DEFAULT',
      cargoPrice: 0,
      estimatedDeliveryDays: 3,
      freeShippingThreshold: undefined,
    };
    this.createStep.set(1);
    this.showCreateModal.set(true);
  }

  goToStep2(): void {
    this.createStep.set(2);
  }

  goToStep1(): void {
    this.createStep.set(1);
  }

  canProceedStep1(): boolean {
    return !!this.productForm.title && !!this.productForm.categoryId;
  }

  async submitCreate(): Promise<void> {
    this.saving.set(true);
    try {
      // Step 1: Create the product
      const productReq: CreateProductRequest = {
        title: this.productForm.title,
        description: this.productForm.description || undefined,
        shortDescription: this.productForm.shortDescription || undefined,
        categoryId: this.productForm.categoryId,
        brandId: this.productForm.brandId || undefined,
      };
      const product = await firstValueFrom(this.productApi.create(productReq));

      // Step 2: Create the offer on the new product
      const offerReq: CreateOfferRequest = {
        productId: product.id,
        sku: this.offerForm.sku || `SKU-${Date.now()}`,
        price: this.offerForm.price,
        listPrice: this.offerForm.listPrice,
        cargoProvider: this.offerForm.cargoProvider as any,
        cargoPrice: this.offerForm.cargoPrice,
        estimatedDeliveryDays: this.offerForm.estimatedDeliveryDays,
        freeShippingThreshold: this.offerForm.freeShippingThreshold,
        initialStock: this.offerForm.stock,
      };
      await firstValueFrom(this.offerApi.create(offerReq));

      this.toast.show('Urun ve teklif olusturuldu', 'success');
      this.showCreateModal.set(false);
      await this.refresh();
    } catch {
      /* error.interceptor handles toast */
    } finally {
      this.saving.set(false);
    }
  }

  // ── Edit ──────────────────────────────────────────────
  openEditModal(row: OfferRow): void {
    this.editingOffer.set(row);
    this.editForm.set({
      price: row.offer.price,
      listPrice: row.offer.listPrice,
      cargoProvider: row.offer.cargoProvider,
      cargoPrice: row.offer.cargoPrice,
      estimatedDeliveryDays: row.offer.estimatedDeliveryDays,
      status: row.offer.status,
    });
    this.showEditModal.set(true);
  }

  async submitEdit(): Promise<void> {
    const row = this.editingOffer();
    if (!row) return;
    this.saving.set(true);
    try {
      await firstValueFrom(this.offerApi.update(row.offer.id, this.editForm()));
      this.toast.show('Teklif guncellendi', 'success');
      this.showEditModal.set(false);
      await this.refresh();
    } catch {
      /* error.interceptor handles toast */
    } finally {
      this.saving.set(false);
    }
  }

  // ── Delete ────────────────────────────────────────────
  openDeleteConfirm(row: OfferRow): void {
    this.deletingOffer.set(row);
    this.showDeleteConfirm.set(true);
  }

  async confirmDelete(): Promise<void> {
    const row = this.deletingOffer();
    if (!row) return;
    try {
      await firstValueFrom(this.offerApi.delete(row.offer.id));
      this.toast.show('Teklif silindi', 'success');
      this.showDeleteConfirm.set(false);
      await this.refresh();
    } catch {
      /* error.interceptor handles toast */
    }
  }

  cancelDelete(): void {
    this.showDeleteConfirm.set(false);
    this.deletingOffer.set(null);
  }

  // ── Campaign (Kısa Süreli Teklif) ─────────────────────
  openCampaignModal(row: OfferRow): void {
    this.campaignTarget.set(row);
    const now = new Date();
    const inWeek = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000);
    this.campaignForm = {
      discountedPrice: Math.max(0, +(row.offer.price * 0.9).toFixed(2)),
      startsAt: this.toLocalInputValue(now),
      endsAt: this.toLocalInputValue(inWeek),
    };
    this.showCampaignModal.set(true);
  }

  closeCampaignModal(): void {
    this.showCampaignModal.set(false);
    this.campaignTarget.set(null);
  }

  async submitCampaign(): Promise<void> {
    const target = this.campaignTarget();
    if (!target) return;
    if (this.campaignForm.discountedPrice <= 0) {
      this.toast.show('İndirimli fiyat 0\'dan büyük olmalı', 'warn');
      return;
    }
    if (this.campaignForm.discountedPrice >= target.offer.price) {
      this.toast.show('İndirimli fiyat normal fiyattan düşük olmalı', 'warn');
      return;
    }
    this.savingCampaign.set(true);
    try {
      const req: CreateCampaignRequest = {
        offerId: target.offer.id,
        productId: target.offer.productId,
        discountedPrice: this.campaignForm.discountedPrice,
        startsAt: new Date(this.campaignForm.startsAt).toISOString(),
        endsAt: new Date(this.campaignForm.endsAt).toISOString(),
      };
      await firstValueFrom(this.campaignApi.create(req));
      this.toast.show('Kısa süreli teklif oluşturuldu', 'success');
      this.closeCampaignModal();
    } catch {
      /* error.interceptor handles toast */
    } finally {
      this.savingCampaign.set(false);
    }
  }

  private toLocalInputValue(d: Date): string {
    const pad = (n: number) => n.toString().padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
  }

  // ── Template helpers (arrow fns not allowed in templates) ──
  updateEditField(field: string, value: unknown): void {
    this.editForm.update(f => ({ ...f, [field]: value }));
  }
}
