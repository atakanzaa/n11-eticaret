import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';

/**
 * Minimal frame for the public auth pages (login, register, unauthorized).
 * Deliberately *not* the full BuyerShell — those screens shouldn't show the
 * shopping header/footer because the user has no shopping context yet.
 */
@Component({
  selector: 'sc-auth-shell',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, RouterLink],
  template: `
    <div class="auth-shell">
      <header class="auth-header">
        <a [routerLink]="['/']" class="brand">SmartCommerce</a>
      </header>
      <main class="auth-main">
        <router-outlet />
      </main>
      <footer class="auth-footer">© SmartCommerce</footer>
    </div>
  `,
  styles: [
    `
      :host {
        display: block;
        min-height: 100dvh;
      }
      .auth-shell {
        display: flex;
        flex-direction: column;
        min-height: 100dvh;
        background: var(--sc-bg);
      }
      .auth-header {
        padding: 24px 40px;
      }
      .brand {
        color: var(--sc-primary);
        font-weight: 700;
        font-size: 20px;
        text-decoration: none;
      }
      .auth-main {
        flex: 1;
        display: grid;
        place-items: center;
        padding: 24px;
      }
      .auth-footer {
        padding: 24px 40px;
        color: var(--sc-text-muted);
        font-size: 13px;
        text-align: center;
      }
    `,
  ],
})
export class AuthShellComponent {}
