import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthApi } from '@core/api/auth.api';
import { AuthStateService } from '@core/auth/auth-state.service';
import { TPipe } from '@shared/i18n.pipe';

@Component({
  selector: 'sc-login',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink, TPipe],
  template: `
    <form class="auth-card" [formGroup]="form" (ngSubmit)="submit()" novalidate>
      <h1>{{ 'auth.loginTitle' | t }}</h1>

      <label class="field">
        <span>{{ 'auth.email' | t }}</span>
        <input type="email" formControlName="email" autocomplete="email" required />
      </label>

      <label class="field">
        <span>{{ 'auth.password' | t }}</span>
        <input type="password" formControlName="password" autocomplete="current-password" required />
      </label>

      @if (errorKey()) {
        <p class="error">{{ errorKey()! | t }}</p>
      }

      <button type="submit" class="primary" [disabled]="form.invalid || submitting()">
        @if (submitting()) {
          <span>{{ 'common.loading' | t }}</span>
        } @else {
          <span>{{ 'auth.loginSubmit' | t }}</span>
        }
      </button>

      <p class="muted">
        {{ 'auth.noAccount' | t }}
        <a [routerLink]="['/kayit']">{{ 'auth.registerSubmit' | t }}</a>
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
        max-width: 400px;
        display: flex;
        flex-direction: column;
        gap: 16px;
      }
      h1 {
        margin: 0 0 8px;
        font-size: 22px;
      }
      .field {
        display: flex;
        flex-direction: column;
        gap: 6px;
        font-size: 14px;
      }
      .field input {
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
export class LoginComponent {
  private readonly fb = inject(FormBuilder).nonNullable;
  private readonly authApi = inject(AuthApi);
  private readonly auth = inject(AuthStateService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly submitting = signal(false);
  readonly errorKey = signal<string | null>(null);

  readonly form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.errorKey.set(null);
    this.authApi.login(this.form.getRawValue()).subscribe({
      next: (response) => {
        this.auth.setSession(response);
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') ?? '/';
        this.router.navigateByUrl(returnUrl);
      },
      error: () => {
        this.submitting.set(false);
        this.errorKey.set('auth.invalidCredentials');
      },
      complete: () => this.submitting.set(false),
    });
  }
}
