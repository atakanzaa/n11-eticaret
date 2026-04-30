import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { CategoryApi } from '@core/api/category.api';
import { AuthApi } from '@core/api/auth.api';
import { AuthStateService } from '@core/auth/auth-state.service';
import { CartService } from '@core/cart.service';
import { CategoryResponse } from '@core/models/category.types';
import { TPipe } from '@shared/i18n.pipe';

@Component({
  selector: 'sc-buyer-header',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, RouterLinkActive, FormsModule, TPipe],
  template: `
    <header class="header">
      <div class="bar">
        <a [routerLink]="['/']" class="brand">SmartCommerce</a>

        <form class="search" (submit)="onSearch($event)">
          <input
            type="search"
            [(ngModel)]="searchQuery"
            name="q"
            [placeholder]="'common.search' | t"
            autocomplete="off"
          />
          <button type="submit">{{ 'common.search' | t }}</button>
        </form>

        <nav class="actions">
          @if (auth.isAuthenticated()) {
            <a [routerLink]="['/sepet']" class="cart">
              {{ 'nav.cart' | t }}
              @if (cart.itemCount() > 0) {
                <span class="badge">{{ cart.itemCount() }}</span>
              }
            </a>
            <details class="user">
              <summary>{{ auth.currentUser()?.firstName ?? ('nav.account' | t) }}</summary>
              <div class="menu">
                <a [routerLink]="['/hesap/profil']">{{ 'nav.profile' | t }}</a>
                <a [routerLink]="['/hesap/siparislerim']">{{ 'nav.orders' | t }}</a>
                @if (isSeller()) {
                  <a [routerLink]="['/satici']">{{ 'nav.seller' | t }}</a>
                }
                @if (isAdmin()) {
                  <a [routerLink]="['/admin']">{{ 'nav.admin' | t }}</a>
                }
                <button type="button" (click)="onLogout()">{{ 'nav.logout' | t }}</button>
              </div>
            </details>
          } @else {
            <a [routerLink]="['/giris']">{{ 'nav.login' | t }}</a>
            <a [routerLink]="['/kayit']" class="primary">{{ 'nav.register' | t }}</a>
          }
        </nav>
      </div>

      <nav class="categories">
        @for (c of categories(); track c.id) {
          <a [routerLink]="['/kategori', c.slug]" routerLinkActive="active">{{ c.name }}</a>
        }
      </nav>
    </header>
  `,
  styles: [
    `
      .header {
        background: var(--sc-surface);
        border-bottom: 1px solid var(--sc-border);
        position: sticky;
        top: 0;
        z-index: 100;
      }
      .bar {
        max-width: var(--sc-container);
        margin: 0 auto;
        padding: 16px 24px;
        display: grid;
        grid-template-columns: auto 1fr auto;
        gap: 24px;
        align-items: center;
      }
      .brand {
        color: var(--sc-primary);
        font-weight: 700;
        font-size: 20px;
        text-decoration: none;
      }
      .search {
        display: flex;
      }
      .search input {
        flex: 1;
        padding: 12px 16px;
        border: 1px solid var(--sc-border);
        border-right: 0;
        border-radius: var(--sc-radius) 0 0 var(--sc-radius);
        font-size: 15px;
      }
      .search button {
        background: var(--sc-primary);
        color: white;
        border: 0;
        padding: 0 20px;
        border-radius: 0 var(--sc-radius) var(--sc-radius) 0;
        cursor: pointer;
        font-weight: 600;
      }
      .actions {
        display: flex;
        gap: 12px;
        align-items: center;
      }
      .actions a {
        text-decoration: none;
        color: var(--sc-text);
        padding: 8px 12px;
        border-radius: var(--sc-radius-sm);
      }
      .actions a.primary {
        background: var(--sc-primary);
        color: white;
      }
      .cart {
        position: relative;
      }
      .badge {
        background: var(--sc-primary);
        color: white;
        font-size: 11px;
        padding: 2px 6px;
        border-radius: 10px;
        margin-left: 4px;
      }
      .user {
        position: relative;
      }
      .user summary {
        list-style: none;
        cursor: pointer;
        padding: 8px 12px;
      }
      .user summary::-webkit-details-marker {
        display: none;
      }
      .menu {
        position: absolute;
        right: 0;
        top: calc(100% + 4px);
        background: white;
        border: 1px solid var(--sc-border);
        border-radius: var(--sc-radius);
        box-shadow: var(--sc-shadow-lg);
        min-width: 180px;
        display: flex;
        flex-direction: column;
      }
      .menu a,
      .menu button {
        text-align: left;
        padding: 10px 14px;
        background: none;
        border: 0;
        color: var(--sc-text);
        cursor: pointer;
        text-decoration: none;
        font: inherit;
      }
      .menu a:hover,
      .menu button:hover {
        background: var(--sc-surface-2);
      }
      .categories {
        max-width: var(--sc-container);
        margin: 0 auto;
        padding: 0 24px 12px;
        display: flex;
        gap: 24px;
        overflow-x: auto;
      }
      .categories a {
        color: var(--sc-text-muted);
        text-decoration: none;
        font-size: 14px;
        white-space: nowrap;
        padding: 4px 0;
      }
      .categories a.active,
      .categories a:hover {
        color: var(--sc-primary);
      }
    `,
  ],
})
export class BuyerHeaderComponent implements OnInit {
  private readonly categoryApi = inject(CategoryApi);
  private readonly authApi = inject(AuthApi);
  private readonly router = inject(Router);
  protected readonly auth = inject(AuthStateService);
  protected readonly cart = inject(CartService);

  readonly categories = signal<CategoryResponse[]>([]);
  searchQuery = '';

  protected readonly isSeller = this.auth.hasRole('SELLER');
  protected readonly isAdmin = this.auth.hasRole('ADMIN');

  async ngOnInit(): Promise<void> {
    try {
      const cats = await firstValueFrom(this.categoryApi.list());
      this.categories.set(cats);
    } catch {
      /* swallow — empty header nav is fine */
    }
    if (this.auth.isAuthenticated()) {
      this.cart.refresh().catch(() => {});
    }
  }

  onSearch(event: Event): void {
    event.preventDefault();
    const q = this.searchQuery.trim();
    this.router.navigate(['/arama'], { queryParams: q ? { q } : {} });
  }

  async onLogout(): Promise<void> {
    const refreshToken = this.auth.getRefreshToken();
    try {
      if (refreshToken) {
        await firstValueFrom(this.authApi.logout(refreshToken));
      }
    } finally {
      this.auth.clearSession();
      this.router.navigate(['/']);
    }
  }
}
