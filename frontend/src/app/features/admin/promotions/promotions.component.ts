import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { CouponApi } from '@core/api/coupon.api';
import { CouponResponse, CreateCouponRequest, DiscountType } from '@core/models/coupon.types';
import { ToastService } from '@core/toast.service';
import { I18nService } from '@core/i18n/i18n.service';
import { TPipe } from '@shared/i18n.pipe';
import { SpinnerComponent } from '@shared/ui/spinner/spinner.component';
import { EmptyStateComponent } from '@shared/ui/empty-state/empty-state.component';
import { StatusBadgeComponent } from '@shared/ui/status-badge/status-badge.component';
import { ConfirmDialogComponent } from '@shared/ui/confirm-dialog/confirm-dialog.component';
import { ModalComponent } from '@shared/ui/modal/modal.component';
import { FormFieldComponent } from '@shared/ui/form-field/form-field.component';
import { PaginationComponent } from '@shared/ui/pagination/pagination.component';
import { CurrencyFormatPipe } from '@shared/pipes/currency-format.pipe';

@Component({
  selector: 'sc-admin-promotions',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    FormsModule,
    TPipe,
    SpinnerComponent,
    EmptyStateComponent,
    StatusBadgeComponent,
    ConfirmDialogComponent,
    ModalComponent,
    FormFieldComponent,
    PaginationComponent,
    CurrencyFormatPipe,
  ],
  templateUrl: './promotions.component.html',
  styleUrls: ['./promotions.component.scss'],
})
export class AdminPromotionsComponent implements OnInit {
  private readonly couponApi = inject(CouponApi);
  private readonly toast = inject(ToastService);
  private readonly i18n = inject(I18nService);

  readonly coupons = signal<CouponResponse[]>([]);
  readonly loading = signal(true);
  readonly creating = signal(false);
  readonly showCreateModal = signal(false);
  readonly deactivateDialogOpen = signal(false);

  // Client-side pagination
  readonly pageSize = 20;
  readonly currentPage = signal(0);

  readonly sortedCoupons = computed(() => {
    return [...this.coupons()].sort((a, b) => {
      const aTime = a.validUntil ? new Date(a.validUntil).getTime() : 0;
      const bTime = b.validUntil ? new Date(b.validUntil).getTime() : 0;
      return bTime - aTime;
    });
  });

  readonly totalPages = computed(() =>
    Math.max(1, Math.ceil(this.sortedCoupons().length / this.pageSize)),
  );

  readonly pagedCoupons = computed(() => {
    const start = this.currentPage() * this.pageSize;
    return this.sortedCoupons().slice(start, start + this.pageSize);
  });

  readonly isFirstPage = computed(() => this.currentPage() === 0);
  readonly isLastPage = computed(() => this.currentPage() >= this.totalPages() - 1);

  onPageChange(page: number): void {
    this.currentPage.set(Math.max(0, Math.min(page, this.totalPages() - 1)));
  }

  private deactivateTargetId: string | null = null;

  newCoupon: Partial<CreateCouponRequest> & { code: string; name: string; discountType: DiscountType; discountValue: number; validFrom: string; validUntil: string } = {
    code: '',
    name: '',
    discountType: 'PERCENTAGE',
    discountValue: 0,
    validFrom: '',
    validUntil: '',
  };

  async ngOnInit(): Promise<void> {
    await this.refresh();
  }

  private async refresh(): Promise<void> {
    this.loading.set(true);
    try {
      const list = await firstValueFrom(this.couponApi.list());
      this.coupons.set(list);
    } finally {
      this.loading.set(false);
    }
  }

  confirmDeactivate(id: string): void {
    this.deactivateTargetId = id;
    this.deactivateDialogOpen.set(true);
  }

  async onDeactivateConfirmed(): Promise<void> {
    this.deactivateDialogOpen.set(false);
    if (!this.deactivateTargetId) return;
    try {
      const updated = await firstValueFrom(this.couponApi.deactivate(this.deactivateTargetId));
      this.coupons.update((arr) => arr.map((c) => (c.id === this.deactivateTargetId ? updated : c)));
      this.toast.show(this.i18n.t('admin.couponDeactivated'), 'success');
    } catch {
      /* error.interceptor handles toast */
    } finally {
      this.deactivateTargetId = null;
    }
  }

  async createCoupon(): Promise<void> {
    this.creating.set(true);
    try {
      const request: CreateCouponRequest = {
        code: this.newCoupon.code,
        name: this.newCoupon.name,
        discountType: this.newCoupon.discountType,
        discountValue: this.newCoupon.discountValue,
        // Backend java.time.Instant — date input "2026-05-03" must become ISO
        // instant; midnight UTC of the chosen day is fine for coupon windows.
        validFrom: this.toIsoInstant(this.newCoupon.validFrom),
        validUntil: this.toIsoInstant(this.newCoupon.validUntil, /*endOfDay*/ true),
        totalUsageLimit: this.newCoupon.totalUsageLimit,
        perUserLimit: this.newCoupon.perUserLimit,
        minimumOrderAmount: this.newCoupon.minimumOrderAmount,
      };
      const created = await firstValueFrom(this.couponApi.create(request));
      this.coupons.update((arr) => [created, ...arr]);
      this.toast.show(this.i18n.t('admin.couponCreated'), 'success');
      this.showCreateModal.set(false);
      this.resetForm();
    } catch {
      /* error.interceptor handles toast */
    } finally {
      this.creating.set(false);
    }
  }

  /**
   * `<input type="date">` produces "YYYY-MM-DD" with no time/zone, but the
   * backend DTO is java.time.Instant. Convert to a full ISO instant —
   * midnight UTC for validFrom, end-of-day UTC for validUntil so the coupon
   * stays valid through the selected day.
   */
  private toIsoInstant(date: string, endOfDay = false): string {
    if (!date) return date;
    if (date.includes('T')) return date; // already ISO
    return endOfDay ? `${date}T23:59:59Z` : `${date}T00:00:00Z`;
  }

  formatValue(c: CouponResponse): string {
    if (c.discountType === 'PERCENTAGE') return `%${c.discountValue}`;
    if (c.discountType === 'FIXED_AMOUNT') {
      return new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(c.discountValue);
    }
    return 'Ucretsiz Kargo';
  }

  private resetForm(): void {
    this.newCoupon = {
      code: '',
      name: '',
      discountType: 'PERCENTAGE',
      discountValue: 0,
      validFrom: '',
      validUntil: '',
    };
  }
}
