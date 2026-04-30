import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { TPipe } from '@shared/i18n.pipe';

@Component({
  selector: 'sc-account-shell',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, TPipe],
  template: `
    <div class="layout">
      <aside class="sidebar">
        <h2>{{ 'account.title' | t }}</h2>
        <nav>
          <a [routerLink]="['/hesap/siparislerim']" routerLinkActive="active">{{ 'nav.orders' | t }}</a>
          <a [routerLink]="['/hesap/profil']" routerLinkActive="active">{{ 'nav.profile' | t }}</a>
        </nav>
      </aside>
      <section class="content">
        <router-outlet />
      </section>
    </div>
  `,
  styles: [
    `
      .layout {
        display: grid;
        grid-template-columns: 240px 1fr;
        gap: 24px;
      }
      .sidebar {
        background: var(--sc-surface);
        border: 1px solid var(--sc-border);
        border-radius: var(--sc-radius);
        padding: 20px;
        align-self: start;
        position: sticky;
        top: 144px;
      }
      .sidebar h2 {
        margin: 0 0 12px;
        font-size: 16px;
      }
      nav {
        display: flex;
        flex-direction: column;
      }
      nav a {
        padding: 8px 12px;
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
    `,
  ],
})
export class AccountShellComponent {}
