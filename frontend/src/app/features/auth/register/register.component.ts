import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthApi } from '@core/api/auth.api';
import { AuthStateService } from '@core/auth/auth-state.service';
import { RoleName } from '@core/models/auth.types';
import { TPipe } from '@shared/i18n.pipe';

@Component({
  selector: 'sc-register',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink, TPipe],
  template: `
    <form class="auth-card" [formGroup]="form" (ngSubmit)="submit()" novalidate>
      <h1>{{ 'auth.registerTitle' | t }}</h1>

      <div class="row">
        <label class="field">
          <span>{{ 'auth.firstName' | t }}</span>
          <input type="text" formControlName="firstName" autocomplete="given-name" required />
        </label>
        <label class="field">
          <span>{{ 'auth.lastName' | t }}</span>
          <input type="text" formControlName="lastName" autocomplete="family-name" required />
        </label>
      </div>

      <label class="field">
        <span>{{ 'auth.email' | t }}</span>
        <input type="email" formControlName="email" autocomplete="email" required />
      </label>

      <label class="field">
        <span>{{ 'auth.phone' | t }} ({{ 'common.optional' | t }})</span>
        <input type="tel" formControlName="phone" autocomplete="tel" />
      </label>

      <label class="field">
        <span>{{ 'auth.password' | t }}</span>
        <input type="password" formControlName="password" autocomplete="new-password" required minlength="8" />
      </label>

      <label class="field">
        <span>{{ 'auth.role' | t }}</span>
        <select formControlName="role">
          <option value="CUSTOMER">{{ 'nav.account' | t }}</option>
          <option value="SELLER">{{ 'nav.seller' | t }}</option>
        </select>
      </label>

      @if (errorMessage()) {
        <p class="error">{{ errorMessage() }}</p>
      }

      <button type="submit" class="primary" [disabled]="form.invalid || submitting()">
        @if (submitting()) {
          <span>{{ 'common.loading' | t }}</span>
        } @else {
          <span>{{ 'auth.registerSubmit' | t }}</span>
        }
      </button>

      <p class="muted">
        {{ 'auth.haveAccount' | t }}
        <a [routerLink]="['/giris']">{{ 'auth.loginSubmit' | t }}</a>
      </p>
    </form>
  `,
  styles: [
    `
      .auth-card {
        background: var(--sc-surface);
        border-radius: var(--sc-radius-lg);
        box-shadow: var(--sc-shadow);
        padding: 32px;
        width: 100%;
        max-width: 460px;
        display: flex;
        flex-direction: column;
        gap: 14px;
      }
      h1 {
        margin: 0 0 8px;
        font-size: 22px;
      }
      .row {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 12px;
      }
      .field {
        display: flex;
        flex-direction: column;
        gap: 6px;
        font-size: 14px;
      }
      .field input,
      .field select {
        padding: 12px 14px;
        border: 1px solid var(--sc-border);
        border-radius: var(--sc-radius-sm);
        font-size: 15px;
      }
      .primary {
        background: var(--sc-primary);
        color: white;
        border: 0;
        padding: 14px;
        border-radius: var(--sc-radius);
        font-weight: 600;
        cursor: pointer;
      }
      .primary:disabled {
        opacity: 0.6;
        cursor: not-allowed;
      }
      .error {
        margin: 0;
        color: var(--sc-danger);
        font-size: 13px;
      }
      .muted {
        margin: 0;
        color: var(--sc-text-muted);
        font-size: 13px;
        text-align: center;
      }
      a {
        color: var(--sc-primary);
        text-decoration: none;
      }
    `,
  ],
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder).nonNullable;
  private readonly authApi = inject(AuthApi);
  private readonly auth = inject(AuthStateService);
  private readonly router = inject(Router);

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    phone: [''],
    password: ['', [Validators.required, Validators.minLength(8)]],
    role: ['CUSTOMER' as RoleName, Validators.required],
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const v = this.form.getRawValue();
    this.authApi
      .register({
        firstName: v.firstName,
        lastName: v.lastName,
        email: v.email,
        phone: v.phone || undefined,
        password: v.password,
        roles: [v.role],
      })
      .subscribe({
        next: (response) => {
          this.auth.setSession(response);
          this.router.navigateByUrl('/');
        },
        error: (err) => {
          this.submitting.set(false);
          this.errorMessage.set(err?.error?.error?.message ?? null);
        },
        complete: () => this.submitting.set(false),
      });
  }
}
