import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TPipe } from '@shared/i18n.pipe';

@Component({
  selector: 'sc-unauthorized',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, TPipe],
  template: `
    <section class="card">
      <h1>403</h1>
      <p>{{ 'auth.unauthorized' | t }}</p>
      <a class="link" [routerLink]="['/']">{{ 'nav.home' | t }}</a>
    </section>
  `,
  styles: [
    `
      .card {
        background: var(--sc-surface);
        border-radius: var(--sc-radius-lg);
        box-shadow: var(--sc-shadow);
        padding: 40px 48px;
        text-align: center;
      }
      h1 {
        font-size: 64px;
        margin: 0 0 8px;
        color: var(--sc-danger);
      }
      p {
        margin: 0 0 24px;
        color: var(--sc-text-muted);
      }
      .link {
        display: inline-block;
        background: var(--sc-primary);
        color: white;
        padding: 12px 24px;
        border-radius: var(--sc-radius);
        text-decoration: none;
        font-weight: 600;
      }
    `,
  ],
})
export class UnauthorizedComponent {}
