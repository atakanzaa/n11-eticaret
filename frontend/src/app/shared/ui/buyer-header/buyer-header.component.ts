import { ChangeDetectionStrategy, Component, ElementRef, HostListener, OnInit, inject, signal } from '@angular/core';
import { Router, NavigationStart, RouterLink, RouterLinkActive } from '@angular/router';
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
  imports: [RouterLink, RouterLinkActive, FormsModule, TPipe],
  templateUrl: './buyer-header.component.html',
  styleUrls: ['./buyer-header.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BuyerHeaderComponent implements OnInit {
  private readonly categoryApi = inject(CategoryApi);
  private readonly authApi = inject(AuthApi);
  private readonly router = inject(Router);
  private readonly host = inject(ElementRef<HTMLElement>);
  protected readonly auth = inject(AuthStateService);
  protected readonly cart = inject(CartService);

  readonly categories = signal<CategoryResponse[]>([]);
  searchQuery = '';
  searchOpen = signal(false);
  userMenuOpen = signal(false);

  protected readonly isSeller = this.auth.hasRole('SELLER');
  protected readonly isAdmin = this.auth.hasRole('ADMIN');

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as Node | null;
    if (this.userMenuOpen() && target && !this.host.nativeElement.contains(target)) {
      this.userMenuOpen.set(false);
    }
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.userMenuOpen.set(false);
    this.searchOpen.set(false);
  }

  toggleUserMenu(event: Event): void {
    event.stopPropagation();
    this.userMenuOpen.update(v => !v);
  }

  closeUserMenu(): void {
    this.userMenuOpen.set(false);
  }

  async ngOnInit(): Promise<void> {
    try {
      const cats = await firstValueFrom(this.categoryApi.list());
      this.categories.set(cats);
    } catch { /* empty nav is acceptable */ }

    if (this.auth.isAuthenticated()) {
      this.cart.refresh().catch(() => {});
    }

    this.router.events.subscribe(e => {
      if (e instanceof NavigationStart) this.userMenuOpen.set(false);
    });
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
