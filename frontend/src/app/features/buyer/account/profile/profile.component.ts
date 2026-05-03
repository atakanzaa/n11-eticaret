import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { UserApi } from '@core/api/user.api';
import { AuthApi } from '@core/api/auth.api';
import { AddressDto, CreateAddressRequest, UpdateAddressRequest, UserProfileDto } from '@core/models/user.types';
import { ToastService } from '@core/toast.service';
import { I18nService } from '@core/i18n/i18n.service';
import { TPipe } from '@shared/i18n.pipe';
import { SpinnerComponent } from '@shared/ui/spinner/spinner.component';
import { EmptyStateComponent } from '@shared/ui/empty-state/empty-state.component';
import { FormFieldComponent } from '@shared/ui/form-field/form-field.component';
import { AddressCardComponent } from '@shared/ui/address-card/address-card.component';
import { ModalComponent } from '@shared/ui/modal/modal.component';
import { ConfirmDialogComponent } from '@shared/ui/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'sc-profile',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss'],
  imports: [
    ReactiveFormsModule,
    TPipe,
    SpinnerComponent,
    EmptyStateComponent,
    FormFieldComponent,
    AddressCardComponent,
    ModalComponent,
    ConfirmDialogComponent,
  ],
})
export class ProfileComponent implements OnInit {
  private readonly fb = inject(FormBuilder).nonNullable;
  private readonly userApi = inject(UserApi);
  private readonly authApi = inject(AuthApi);
  private readonly toast = inject(ToastService);
  private readonly i18n = inject(I18nService);

  readonly profile = signal<UserProfileDto | null>(null);
  readonly addresses = signal<AddressDto[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);

  // Password change
  readonly currentPassword = signal('');
  readonly newPassword = signal('');
  readonly confirmPassword = signal('');
  readonly savingPassword = signal(false);
  readonly passwordError = signal('');

  // Address modal
  readonly showAddressModal = signal(false);
  readonly editingAddress = signal<AddressDto | null>(null);
  readonly savingAddress = signal(false);
  readonly addressForm = signal<CreateAddressRequest>(this.emptyAddress());
  readonly addressErrors = signal<Record<string, string>>({});

  // Delete address
  readonly showDeleteAddressConfirm = signal(false);
  readonly deletingAddress = signal<AddressDto | null>(null);

  readonly form = this.fb.group({
    firstName: [''],
    lastName: [''],
    phone: [''],
  });

  async ngOnInit(): Promise<void> {
    try {
      const [profile, addresses] = await Promise.all([
        firstValueFrom(this.userApi.me()),
        firstValueFrom(this.userApi.listAddresses()).catch(() => []),
      ]);
      this.profile.set(profile);
      this.addresses.set(addresses);
      this.form.patchValue({
        firstName: profile.firstName,
        lastName: profile.lastName,
        phone: profile.phone ?? '',
      });
    } finally {
      this.loading.set(false);
    }
  }

  async save(): Promise<void> {
    this.saving.set(true);
    try {
      const updated = await firstValueFrom(this.userApi.updateProfile(this.form.getRawValue()));
      this.profile.set(updated);
      this.toast.show(this.i18n.t('account.profileUpdated'), 'success');
    } finally {
      this.saving.set(false);
    }
  }

  // ── Password ──────────────────────────────────────────
  async submitPasswordChange(): Promise<void> {
    this.passwordError.set('');

    if (!this.currentPassword() || !this.newPassword()) {
      this.passwordError.set(this.i18n.t('account.passwordRequired'));
      return;
    }
    if (this.newPassword().length < 8) {
      this.passwordError.set(this.i18n.t('account.passwordMinLength'));
      return;
    }
    if (this.newPassword() !== this.confirmPassword()) {
      this.passwordError.set(this.i18n.t('account.passwordMismatch'));
      return;
    }

    this.savingPassword.set(true);
    try {
      await firstValueFrom(
        this.authApi.changePassword({
          currentPassword: this.currentPassword(),
          newPassword: this.newPassword(),
        }),
      );
      this.toast.show(this.i18n.t('account.passwordChanged'), 'success');
      this.currentPassword.set('');
      this.newPassword.set('');
      this.confirmPassword.set('');
    } catch (err: unknown) {
      const status = (err as { status?: number })?.status;
      if (status === 401) {
        this.passwordError.set(this.i18n.t('account.currentPasswordWrong'));
      } else if (status === 400) {
        this.passwordError.set(this.i18n.t('account.passwordSameAsOld'));
      } else {
        this.passwordError.set(this.i18n.t('common.error'));
      }
    } finally {
      this.savingPassword.set(false);
    }
  }

  // ── Address CRUD ──────────────────────────────────────
  openAddressModal(): void {
    this.editingAddress.set(null);
    this.addressForm.set(this.emptyAddress());
    this.addressErrors.set({});
    this.showAddressModal.set(true);
  }

  openEditAddressModal(address: AddressDto): void {
    this.editingAddress.set(address);
    this.addressForm.set({ ...address });
    this.addressErrors.set({});
    this.showAddressModal.set(true);
  }

  updateAddressForm(key: string, value: string): void {
    this.addressForm.update(f => ({ ...f, [key]: value }));
    if (this.addressErrors()[key]) {
      this.addressErrors.update(errs => {
        const copy = { ...errs };
        delete copy[key];
        return copy;
      });
    }
  }

  private validateAddress(): Record<string, string> {
    const f = this.addressForm();
    const errs: Record<string, string> = {};
    const reqMsg = this.i18n.t('validation.required');
    if (!f.label?.trim()) errs['label'] = reqMsg;
    if (!f.fullName?.trim()) errs['fullName'] = reqMsg;
    if (!f.phone?.trim()) {
      errs['phone'] = reqMsg;
    } else if (!/^(\+90|0)?\s*5\d{2}\s*\d{3}\s*\d{2}\s*\d{2}$/.test(f.phone)) {
      errs['phone'] = this.i18n.t('auth.phoneInvalid');
    }
    if (!f.city?.trim()) errs['city'] = reqMsg;
    if (!f.district?.trim()) errs['district'] = reqMsg;
    if (!f.fullAddress?.trim()) errs['fullAddress'] = reqMsg;
    return errs;
  }

  async submitAddress(): Promise<void> {
    const errs = this.validateAddress();
    this.addressErrors.set(errs);
    if (Object.keys(errs).length > 0) return;

    this.savingAddress.set(true);
    try {
      const form = this.addressForm();
      const payload: CreateAddressRequest = {
        ...form,
        street: (form.fullAddress ?? '').slice(0, 255),
      };
      const editing = this.editingAddress();
      if (editing) {
        const updated = await firstValueFrom(
          this.userApi.updateAddress(editing.id, payload as UpdateAddressRequest),
        );
        this.addresses.update(list => list.map(a => (a.id === editing.id ? updated : a)));
        this.toast.show(this.i18n.t('account.addressUpdated'), 'success');
      } else {
        const created = await firstValueFrom(this.userApi.addAddress(payload));
        this.addresses.update(list => [...list, created]);
        this.toast.show(this.i18n.t('account.addressAdded'), 'success');
      }
      this.showAddressModal.set(false);
    } finally {
      this.savingAddress.set(false);
    }
  }

  openDeleteAddressConfirm(address: AddressDto): void {
    this.deletingAddress.set(address);
    this.showDeleteAddressConfirm.set(true);
  }

  async confirmDeleteAddress(): Promise<void> {
    const address = this.deletingAddress();
    if (!address) return;
    try {
      await firstValueFrom(this.userApi.deleteAddress(address.id));
      this.addresses.update(list => list.filter(a => a.id !== address.id));
      this.toast.show(this.i18n.t('account.addressDeleted'), 'success');
      this.showDeleteAddressConfirm.set(false);
    } catch {
      /* error.interceptor handles toast */
    }
  }

  private emptyAddress(): CreateAddressRequest {
    return {
      label: '',
      fullName: '',
      phone: '',
      country: 'TR',
      city: '',
      district: '',
      neighborhood: '',
      street: '',
      buildingNo: '',
      apartmentNo: '',
      postalCode: '',
      fullAddress: '',
      defaultShipping: false,
      defaultBilling: false,
      addressType: 'HOME',
    };
  }
}
