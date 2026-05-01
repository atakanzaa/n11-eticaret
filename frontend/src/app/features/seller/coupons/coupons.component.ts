import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe, DecimalPipe } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { CouponApi } from '@core/api/coupon.api';
import {
  CouponResponse,
  CreateCouponRequest,
  DiscountType,
} from '@core/models/coupon.types';
import { ToastService } from '@core/toast.service';
import { SpinnerComponent } from '@shared/ui/spinner/spinner.component';
import { EmptyStateComponent } from '@shared/ui/empty-state/empty-state.component';
import { ModalComponent } from '@shared/ui/modal/modal.component';
import { FormFieldComponent } from '@shared/ui/form-field/form-field.component';

@Component({
  selector: 'sc-seller-coupons',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './coupons.component.html',
  styleUrls: ['./coupons.component.scss'],
  imports: [
    FormsModule,
    DatePipe,
    DecimalPipe,
    SpinnerComponent,
    EmptyStateComponent,
    ModalComponent,
    FormFieldComponent,
  ],
})
export class SellerCouponsComponent implements OnInit {
  private readonly couponApi = inject(CouponApi);
  private readonly toast = inject(ToastService);

  readonly coupons = signal<CouponResponse[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly showCreateModal = signal(false);

  form: CreateCouponRequest = this.empty();

  async ngOnInit(): Promise<void> {
    await this.refresh();
  }

  async refresh(): Promise<void> {
    this.loading.set(true);
    try {
      const list = await firstValueFrom(this.couponApi.myCoupons());
      this.coupons.set(list);
    } catch {
      /* error.interceptor handles toast */
    } finally {
      this.loading.set(false);
    }
  }

  openCreateModal(): void {
    this.form = this.empty();
    this.showCreateModal.set(true);
  }

  closeCreateModal(): void {
    this.showCreateModal.set(false);
  }

  async submitCreate(): Promise<void> {
    if (!this.form.code || !this.form.name) {
      this.toast.show('Kod ve isim zorunlu', 'warn');
      return;
    }
    if (this.form.discountValue <= 0) {
      this.toast.show('İndirim değeri 0\'dan büyük olmalı', 'warn');
      return;
    }
    if (!this.form.validFrom || !this.form.validUntil) {
      this.toast.show('Geçerlilik tarihleri gerekli', 'warn');
      return;
    }
    this.saving.set(true);
    try {
      const payload: CreateCouponRequest = {
        ...this.form,
        validFrom: new Date(this.form.validFrom).toISOString(),
        validUntil: new Date(this.form.validUntil).toISOString(),
      };
      await firstValueFrom(this.couponApi.create(payload));
      this.toast.show('Kupon oluşturuldu', 'success');
      this.closeCreateModal();
      await this.refresh();
    } catch {
      /* error.interceptor handles toast */
    } finally {
      this.saving.set(false);
    }
  }

  async deactivate(coupon: CouponResponse): Promise<void> {
    if (!coupon.active) return;
    try {
      await firstValueFrom(this.couponApi.deactivate(coupon.id));
      this.toast.show('Kupon devre dışı bırakıldı', 'success');
      await this.refresh();
    } catch {
      /* error.interceptor handles toast */
    }
  }

  formatDiscount(c: CouponResponse): string {
    if (c.discountType === 'PERCENTAGE') return `%${c.discountValue}`;
    if (c.discountType === 'FIXED_AMOUNT') return `₺${c.discountValue}`;
    return 'Ücretsiz Kargo';
  }

  setDiscountType(value: string): void {
    this.form.discountType = value as DiscountType;
  }

  private empty(): CreateCouponRequest {
    const now = new Date();
    const end = new Date(now.getTime() + 30 * 24 * 60 * 60 * 1000);
    const pad = (n: number) => n.toString().padStart(2, '0');
    const local = (d: Date) =>
      `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
    return {
      code: '',
      name: '',
      description: '',
      discountType: 'PERCENTAGE',
      discountValue: 10,
      maxDiscountAmount: undefined,
      minimumOrderAmount: undefined,
      totalUsageLimit: undefined,
      perUserLimit: 1,
      validFrom: local(now),
      validUntil: local(end),
      firstOrderOnly: false,
      stackable: false,
    };
  }
}
