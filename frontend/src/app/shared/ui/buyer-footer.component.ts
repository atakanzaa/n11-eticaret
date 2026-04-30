import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'sc-buyer-footer',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <footer class="footer">
      <div class="row">
        <span class="brand">SmartCommerce</span>
        <span class="muted">© 2026 — Tüm hakları saklıdır.</span>
      </div>
    </footer>
  `,
  styles: [
    `
      .footer {
        background: var(--sc-surface);
        border-top: 1px solid var(--sc-border);
        margin-top: 64px;
      }
      .row {
        max-width: var(--sc-container);
        margin: 0 auto;
        padding: 32px 24px;
        display: flex;
        justify-content: space-between;
        align-items: center;
      }
      .brand {
        color: var(--sc-primary);
        font-weight: 700;
      }
      .muted {
        color: var(--sc-text-muted);
        font-size: 13px;
      }
    `,
  ],
})
export class BuyerFooterComponent {}
