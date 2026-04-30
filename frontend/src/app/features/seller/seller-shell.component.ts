import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthApi } from '@core/api/auth.api';
import { AuthStateService } from '@core/auth/auth-state.service';
import { TPipe } from '@shared/i18n.pipe';

@Component({
  selector: 'sc-seller-shell',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, TPipe],
  template: `
    <div class="layout">
      <aside class="sidebar">
        <a class="brand" [routerLink]="['/satici']">{{ 'nav.seller' | t }}</a>
        <nav>
          <a [routerLink]="['/satici']" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">{{ 'seller.dashboard' | t }}</a>
          <a [routerLink]="['/satici/urunler']" routerLinkActive="active">{{ 'seller.products' | t }}</a>
          <a [routerLink]="['/satici/siparisler']" routerLinkActive="active">{{ 'seller.orders' | t }}</a>
        </nav>
        <div class="bottom">
          <a [routerLink]="['/']">← {{ 'nav.home' | t }}</a>
          <button type="button" (click)="logout()">{{ 'nav.logout' | t }}</button>
        </div>
      </aside>
      <main class="content">
        <router-outlet />
      </main>
    </div>
  `,
  styles: [
    `
      .layout {
        display: grid;
        grid-template-columns: 240px 1fr;
        min-height: 100dvh;
      }
      .sidebar {
        background: var(--sc-surface);
        border-right: 1px solid var(--sc-border);
        padding: 24px 16px;
        display: flex;
        flex-direction: column;
        gap: 24px;
      }
      .brand {
        color: var(--sc-primary);
        font-weight: 700;
        font-size: 18px;
        text-decoration: none;
      }
      nav {
        display: flex;
        flex-direction: column;
      }
      nav a {
        padding: 10px 12px;
        color: var(--sc-text-muted);
        text-decoration: none;
        border-radius: var(--sc-radius-sm);
        font-size: 14px;
      }
      nav a.active,
      nav a:hover {
        background: var(--sc-surface-2);
        color: var(--sc-text);
      }
      .bottom {
        margin-top: auto;
        display: flex;
        flex-direction: column;
        gap: 8px;
      }
      .bottom a {
        color: var(--sc-text-muted);
        text-decoration: none;
        font-size: 13px;
      }
      .bottom button {
        background: none;
        border: 0;
        color: var(--sc-danger);
        font-size: 13px;
        text-align: left;
        cursor: pointer;
        padding: 0;
      }
      .content {
        padding: 32px;
        background: var(--sc-bg);
      }
    `,
  ],
})
export class SellerShellComponent {
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
