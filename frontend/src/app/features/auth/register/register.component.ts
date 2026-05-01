import { Component, ChangeDetectionStrategy, inject, signal, computed } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthApi } from '@core/api/auth.api';
import { AuthStateService } from '@core/auth/auth-state.service';
import { ToastService } from '@core/toast.service';
import { FormFieldComponent } from '@shared/ui/form-field/form-field.component';
import { TPipe } from '@shared/i18n.pipe';

@Component({
  selector: 'sc-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, FormFieldComponent, TPipe],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder).nonNullable;
  private readonly authApi = inject(AuthApi);
  private readonly auth = inject(AuthStateService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    phone: [''],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  readonly firstNameError = computed(() => {
    const ctrl = this.form.controls.firstName;
    if (!ctrl.touched || ctrl.valid) return '';
    if (ctrl.hasError('required')) return 'Ad zorunludur';
    return '';
  });

  readonly lastNameError = computed(() => {
    const ctrl = this.form.controls.lastName;
    if (!ctrl.touched || ctrl.valid) return '';
    if (ctrl.hasError('required')) return 'Soyad zorunludur';
    return '';
  });

  readonly emailError = computed(() => {
    const ctrl = this.form.controls.email;
    if (!ctrl.touched || ctrl.valid) return '';
    if (ctrl.hasError('required')) return 'E-posta zorunludur';
    if (ctrl.hasError('email')) return 'Gecerli bir e-posta giriniz';
    return '';
  });

  readonly phoneError = computed(() => {
    const ctrl = this.form.controls.phone;
    if (!ctrl.touched || ctrl.valid) return '';
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
    this.errorMessage.set(null);
    const v = this.form.getRawValue();
    try {
      const response = await firstValueFrom(
        this.authApi.register({
          firstName: v.firstName,
          lastName: v.lastName,
          email: v.email,
          phone: v.phone || undefined,
          password: v.password,
          roles: ['CUSTOMER'],
        }),
      );
      this.auth.setSession(response);
      this.router.navigateByUrl('/');
    } catch (err: unknown) {
      const message = (err as { error?: { error?: { message?: string } } })?.error?.error?.message ?? null;
      this.errorMessage.set(message);
      this.toast.show('Kayit basarisiz', 'danger');
    } finally {
      this.submitting.set(false);
    }
  }
}
