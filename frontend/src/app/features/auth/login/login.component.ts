import { Component, ChangeDetectionStrategy, inject, signal, computed } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthApi } from '@core/api/auth.api';
import { AuthStateService } from '@core/auth/auth-state.service';
import { ToastService } from '@core/toast.service';
import { I18nService } from '@core/i18n/i18n.service';
import { FormFieldComponent } from '@shared/ui/form-field/form-field.component';
import { TPipe } from '@shared/i18n.pipe';

@Component({
  selector: 'sc-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, FormFieldComponent, TPipe],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder).nonNullable;
  private readonly authApi = inject(AuthApi);
  private readonly auth = inject(AuthStateService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly toast = inject(ToastService);
  private readonly i18n = inject(I18nService);

  readonly submitting = signal(false);
  readonly errorKey = signal<string | null>(null);

  readonly form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  readonly emailError = computed(() => {
    const ctrl = this.form.controls.email;
    if (!ctrl.touched || ctrl.valid) return '';
    if (ctrl.hasError('required')) return 'E-posta zorunludur';
    if (ctrl.hasError('email')) return 'Gecerli bir e-posta giriniz';
    return '';
  });

  readonly passwordError = computed(() => {
    const ctrl = this.form.controls.password;
    if (!ctrl.touched || ctrl.valid) return '';
    if (ctrl.hasError('required')) return 'Sifre zorunludur';
    if (ctrl.hasError('minlength')) return 'Sifre en az 8 karakter olmalidir';
    return '';
  });

  async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.errorKey.set(null);
    try {
      const response = await firstValueFrom(this.authApi.login(this.form.getRawValue()));
      this.auth.setSession(response);
      const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') ?? '/';
      this.router.navigateByUrl(returnUrl);
    } catch {
      this.errorKey.set('auth.invalidCredentials');
      this.toast.show(this.i18n.t('auth.loginFailed'), 'danger');
    } finally {
      this.submitting.set(false);
    }
  }
}
