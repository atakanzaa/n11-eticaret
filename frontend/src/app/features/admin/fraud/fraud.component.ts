import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { FraudApi } from '@core/api/fraud.api';
import {
  BlacklistType,
  CreateBlacklistRequest,
  FraudBlacklist,
  FraudCheck,
} from '@core/models/fraud.types';
import { ToastService } from '@core/toast.service';
import { I18nService } from '@core/i18n/i18n.service';
import { TPipe } from '@shared/i18n.pipe';
import { SpinnerComponent } from '@shared/ui/spinner/spinner.component';
import { StatusBadgeComponent } from '@shared/ui/status-badge/status-badge.component';
import { EmptyStateComponent } from '@shared/ui/empty-state/empty-state.component';
import { ModalComponent } from '@shared/ui/modal/modal.component';
import { ConfirmDialogComponent } from '@shared/ui/confirm-dialog/confirm-dialog.component';
import { FormFieldComponent } from '@shared/ui/form-field/form-field.component';
import { CurrencyFormatPipe } from '@shared/pipes/currency-format.pipe';

@Component({
  selector: 'sc-admin-fraud',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    FormsModule,
    TPipe,
    SpinnerComponent,
    StatusBadgeComponent,
    EmptyStateComponent,
    ModalComponent,
    ConfirmDialogComponent,
    FormFieldComponent,
    CurrencyFormatPipe,
  ],
  templateUrl: './fraud.component.html',
  styleUrls: ['./fraud.component.scss'],
})
export class AdminFraudComponent implements OnInit {
  private readonly fraudApi = inject(FraudApi);
  private readonly toast = inject(ToastService);
  private readonly i18n = inject(I18nService);

  readonly checks = signal<FraudCheck[]>([]);
  readonly blacklist = signal<FraudBlacklist[]>([]);
  readonly loading = signal(true);

  // Add modal
  readonly showAddModal = signal(false);
  readonly saving = signal(false);
  readonly form = signal<CreateBlacklistRequest>({
    blacklistType: 'EMAIL',
    value: '',
    reason: '',
  });
  readonly formErrors = signal<Record<string, string>>({});

  readonly blacklistTypes: BlacklistType[] = ['USER_ID', 'EMAIL', 'IP', 'CARD_BIN'];

  // Delete confirm
  readonly deletingItem = signal<FraudBlacklist | null>(null);
  readonly showDeleteConfirm = signal(false);

  async ngOnInit(): Promise<void> {
    await this.refresh();
  }

  private async refresh(): Promise<void> {
    this.loading.set(true);
    try {
      const [checksPage, blacklist] = await Promise.allSettled([
        firstValueFrom(this.fraudApi.listChecks(undefined, 0, 50)),
        firstValueFrom(this.fraudApi.listBlacklist()),
      ]);
      if (checksPage.status === 'fulfilled') this.checks.set(checksPage.value.content);
      if (blacklist.status === 'fulfilled') this.blacklist.set(blacklist.value);
    } finally {
      this.loading.set(false);
    }
  }

  openAddModal(): void {
    this.form.set({ blacklistType: 'EMAIL', value: '', reason: '' });
    this.formErrors.set({});
    this.showAddModal.set(true);
  }

  updateForm(key: keyof CreateBlacklistRequest, value: string): void {
    this.form.update(f => ({ ...f, [key]: value }));
    if (this.formErrors()[key]) {
      this.formErrors.update(errs => {
        const copy = { ...errs };
        delete copy[key];
        return copy;
      });
    }
  }

  private validate(): Record<string, string> {
    const f = this.form();
    const errs: Record<string, string> = {};
    const reqMsg = this.i18n.t('validation.required');
    if (!f.value?.trim()) errs['value'] = reqMsg;
    if (!f.reason?.trim()) errs['reason'] = reqMsg;
    if (f.blacklistType === 'EMAIL' && f.value && !/^.+@.+\..+$/.test(f.value)) {
      errs['value'] = this.i18n.t('validation.email');
    }
    if (f.blacklistType === 'IP' && f.value && !/^\d{1,3}(\.\d{1,3}){3}$/.test(f.value)) {
      errs['value'] = this.i18n.t('fraud.ipInvalid');
    }
    return errs;
  }

  async submitAdd(): Promise<void> {
    const errs = this.validate();
    this.formErrors.set(errs);
    if (Object.keys(errs).length > 0) return;

    this.saving.set(true);
    try {
      await firstValueFrom(this.fraudApi.addToBlacklist(this.form()));
      this.toast.show(this.i18n.t('fraud.blacklistAdded'), 'success');
      this.showAddModal.set(false);
      await this.refresh();
    } catch {
      // error.interceptor handles toast
    } finally {
      this.saving.set(false);
    }
  }

  openDeleteConfirm(item: FraudBlacklist): void {
    this.deletingItem.set(item);
    this.showDeleteConfirm.set(true);
  }

  async confirmDelete(): Promise<void> {
    const item = this.deletingItem();
    if (!item) return;
    try {
      await firstValueFrom(this.fraudApi.removeFromBlacklist(item.id));
      this.toast.show(this.i18n.t('fraud.blacklistRemoved'), 'success');
      this.showDeleteConfirm.set(false);
      this.deletingItem.set(null);
      await this.refresh();
    } catch {
      // error.interceptor handles toast
    }
  }

  riskColor(score: number): string {
    if (score >= 80) return 'var(--sc-danger)';
    if (score >= 60) return 'var(--sc-warn)';
    return 'var(--sc-success)';
  }
}
