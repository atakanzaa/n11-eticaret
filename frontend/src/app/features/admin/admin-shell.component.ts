import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { Router, RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthApi } from '@core/api/auth.api';
import { AuthStateService } from '@core/auth/auth-state.service';
import { TPipe } from '@shared/i18n.pipe';

@Component({
  selector: 'sc-admin-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, TPipe],
  templateUrl: './admin-shell.component.html',
  styleUrls: ['./admin-shell.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminShellComponent {
  private readonly authApi = inject(AuthApi);
  private readonly auth = inject(AuthStateService);
  private readonly router = inject(Router);

  async logout(): Promise<void> {
    const refreshToken = this.auth.getRefreshToken();
    try {
      if (refreshToken) await firstValueFrom(this.authApi.logout(refreshToken));
    } finally {
      this.auth.clearSession();
      this.router.navigate(['/']);
    }
  }
}
