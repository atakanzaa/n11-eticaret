import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { UserApi } from '@core/api/user.api';
import { AddressDto, UserProfileDto } from '@core/models/user.types';
import { ToastService } from '@core/toast.service';
import { I18nService } from '@core/i18n/i18n.service';
import { TPipe } from '@shared/i18n.pipe';

@Component({
  selector: 'sc-profile',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, TPipe],
  template: `
    <h2>{{ 'nav.profile' | t }}</h2>

    @if (loading()) {
      <p class="muted">{{ 'common.loading' | t }}</p>
    } @else if (profile()) {
      <section class="card">
        <h3>Kişisel Bilgiler</h3>
        <form [formGroup]="form" (ngSubmit)="save()">
          <div class="row">
            <label>
              <span>{{ 'auth.firstName' | t }}</span>
              <input formControlName="firstName" />
            </label>
            <label>
              <span>{{ 'auth.lastName' | t }}</span>
              <input formControlName="lastName" />
            </label>
          </div>
          <label>
            <span>{{ 'auth.phone' | t }}</span>
            <input formControlName="phone" />
          </label>
          <button type="submit" class="primary" [disabled]="saving()">{{ 'common.save' | t }}</button>
        </form>
      </section>

      <section class="card">
        <h3>{{ 'nav.addresses' | t }}</h3>
        @if (addresses().length === 0) {
          <p class="muted">{{ 'common.empty' | t }}</p>
        } @else {
          <ul class="addresses">
            @for (a of addresses(); track a.id) {
              <li>
                <strong>{{ a.label }}</strong>
                <span>{{ a.fullName }}, {{ a.phone }}</span>
                <span class="muted">{{ a.fullAddress }}, {{ a.district }}/{{ a.city }}</span>
              </li>
            }
          </ul>
        }
      </section>
    }
  `,
  styles: [
    `
      h2 {
        margin: 0 0 16px;
      }
      .card {
        background: var(--sc-surface);
        border: 1px solid var(--sc-border);
        border-radius: var(--sc-radius);
        padding: 24px;
        margin-bottom: 16px;
      }
      .card h3 {
        margin: 0 0 16px;
      }
      .row {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 12px;
      }
      label {
        display: flex;
        flex-direction: column;
        gap: 4px;
        margin-bottom: 12px;
        font-size: 14px;
      }
      input {
        padding: 10px 12px;
        border: 1px solid var(--sc-border);
        border-radius: var(--sc-radius-sm);
      }
      .primary {
        background: var(--sc-primary);
        color: white;
        border: 0;
        padding: 10px 24px;
        border-radius: var(--sc-radius);
        font-weight: 600;
        cursor: pointer;
      }
      .primary:disabled {
        opacity: 0.5;
      }
      .addresses {
        list-style: none;
        padding: 0;
        margin: 0;
        display: flex;
        flex-direction: column;
        gap: 12px;
      }
      .addresses li {
        display: flex;
        flex-direction: column;
        padding: 12px;
        background: var(--sc-surface-2);
        border-radius: var(--sc-radius-sm);
      }
      .muted {
        color: var(--sc-text-muted);
      }
    `,
  ],
})
export class ProfileComponent implements OnInit {
  private readonly fb = inject(FormBuilder).nonNullable;
  private readonly userApi = inject(UserApi);
  private readonly toast = inject(ToastService);
  private readonly i18n = inject(I18nService);

  readonly profile = signal<UserProfileDto | null>(null);
  readonly addresses = signal<AddressDto[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);

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
}
