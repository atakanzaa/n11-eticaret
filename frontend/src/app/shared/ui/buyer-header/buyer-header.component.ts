import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { CategoryApi } from '@core/api/category.api';
import { AuthApi } from '@core/api/auth.api';
import { AuthStateService } from '@core/auth/auth-state.service';
import { CartService } from '@core/cart.service';
import { CategoryResponse } from '@core/models/category.types';

@Component({
  selector: 'sc-buyer-header',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, FormsModule],
  templateUrl: './buyer-header.component.html',
  styleUrls: ['./buyer-header.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BuyerHeaderComponent implements OnInit {
  private readonly categoryApi = inject(CategoryApi);
  private readonly authApi = inject(AuthApi);
  private readonly router = inject(Router);
  protected readonly auth = inject(AuthStateService);
  protected readonly cart = inject(CartService);

  readonly categories = signal<CategoryResponse[]>([]);
  searchQuery = '';
  searchOpen = signal(false);

  protected readonly isSeller = this.auth.hasRole('SELLER');
  protected readonly isAdmin = this.auth.hasRole('ADMIN');

  async ngOnInit(): Promise<void> {
    try {
      const cats = await firstValueFrom(this.categoryApi.list());
      this.categories.set(cats);
    } catch { /* empty nav is acceptable */ }

    if (this.auth.isAuthenticated()) {
      this.cart.refresh().catch(() => {});
    }
  }

  onSearch(event: Event): void {
    event.preventDefault();
    this.searchOpen.set(false);
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
