import { Component, ChangeDetectionStrategy, inject, signal, computed } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthApi } from '@core/api/auth.api';
import { SellerApi } from '@core/api/seller.api';
import { AuthStateService } from '@core/auth/auth-state.service';
import { ToastService } from '@core/toast.service';
import { I18nService } from '@core/i18n/i18n.service';
import { FormFieldComponent } from '@shared/ui/form-field/form-field.component';
import { SpinnerComponent } from '@shared/ui/spinner/spinner.component';
import { TPipe } from '@shared/i18n.pipe';

@Component({
  selector: 'sc-become-seller',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, FormFieldComponent, SpinnerComponent, TPipe],
  templateUrl: './become-seller.component.html',
  styleUrls: ['./become-seller.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BecomeSellerComponent {
  private readonly fb = inject(FormBuilder).nonNullable;
  private readonly authApi = inject(AuthApi);
  private readonly sellerApi = inject(SellerApi);
  private readonly auth = inject(AuthStateService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly i18n = inject(I18nService);

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly alreadySeller = computed(() => this.auth.roles().includes('SELLER'));

  readonly form = this.fb.group({
    storeName: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(100)]],
    description: ['', [Validators.maxLength(500)]],
    agreement: [false, Validators.requiredTrue],
  });

  readonly storeNameError = computed(() => {
    const ctrl = this.form.controls.storeName;
    if (!ctrl.touched || ctrl.valid) return '';
    if (ctrl.hasError('required')) return this.i18n.t('seller.storeNameRequired');
    if (ctrl.hasError('minlength')) return this.i18n.t('seller.storeNameMinLength');
    if (ctrl.hasError('maxlength')) return this.i18n.t('seller.storeNameMaxLength');
    return '';
  });

  readonly descriptionError = computed(() => {
    const ctrl = this.form.controls.description;
    if (!ctrl.touched || ctrl.valid) return '';
    if (ctrl.hasError('maxlength')) return this.i18n.t('seller.descriptionMaxLength');
    return '';
  });

  async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set(null);
    const v = this.form.getRawValue();

    try {
      const refreshed = await firstValueFrom(this.authApi.becomeSeller());
      this.auth.setSession(refreshed);

      await firstValueFrom(
        this.sellerApi.updateMe({
          storeName: v.storeName.trim(),
          description: v.description.trim() || undefined,
        }),
      );

      this.toast.show(this.i18n.t('seller.onboardSuccess'), 'success');
      this.router.navigateByUrl('/satici');
    } catch (err: unknown) {
      const status = (err as { status?: number })?.status;
      if (status === 409) {
        this.errorMessage.set(this.i18n.t('seller.alreadySellerError'));
      } else {
        const message = (err as { error?: { error?: { message?: string } } })?.error?.error?.message;
        this.errorMessage.set(message ?? this.i18n.t('common.error'));
      }
    } finally {
      this.submitting.set(false);
    }
  }
}
