import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { FraudApi } from '@core/api/fraud.api';
import { FraudBlacklist, FraudCheck, FraudDecision } from '@core/models/fraud.types';
import { TPipe } from '@shared/i18n.pipe';

@Component({
  selector: 'sc-admin-fraud',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, TPipe],
  template: `
    <h1>{{ 'admin.fraud' | t }}</h1>

    @if (loading()) {
      <p class="muted">{{ 'common.loading' | t }}</p>
    } @else {
      <section>
        <h3>Son Kontroller</h3>
        @if (checks().length === 0) {
          <p class="muted">{{ 'common.empty' | t }}</p>
        } @else {
          <table>
            <thead>
              <tr>
                <th>Sipariş</th>
                <th>Tutar</th>
                <th>{{ 'fraud.riskScore' | t }}</th>
                <th>Karar</th>
                <th>Tarih</th>
              </tr>
            </thead>
            <tbody>
              @for (c of checks(); track c.id) {
                <tr>
                  <td><code>{{ c.orderId }}</code></td>
                  <td>{{ formatPrice(c.amount) }}</td>
                  <td>
                    <div class="risk">
                      <div class="bar" [style.width.%]="c.riskScore" [style.background]="riskColor(c.riskScore)"></div>
                      <span>{{ c.riskScore }}</span>
                    </div>
                  </td>
                  <td><span class="pill" [class]="pillClass(c.decision)">{{ 'fraudDecision.' + c.decision | t }}</span></td>
                  <td>{{ c.createdAt | date: 'short' }}</td>
                </tr>
              }
            </tbody>
          </table>
        }
      </section>

      <section>
        <h3>{{ 'fraud.blacklist' | t }} ({{ blacklist().length }})</h3>
        @if (blacklist().length === 0) {
          <p class="muted">{{ 'common.empty' | t }}</p>
        } @else {
          <ul class="bl">
            @for (b of blacklist(); track b.id) {
              <li>
                <span class="pill">{{ 'blacklistType.' + b.blacklistType | t }}</span>
                <code>{{ b.value }}</code>
                <span class="muted">{{ b.reason }}</span>
              </li>
            }
          </ul>
        }
      </section>
    }
  `,
  styles: [
    `
      h1 {
        margin: 0 0 24px;
      }
      section {
        background: var(--sc-surface);
        border: 1px solid var(--sc-border);
        border-radius: var(--sc-radius);
        padding: 24px;
        margin-bottom: 24px;
      }
      section h3 {
        margin: 0 0 16px;
      }
      table {
        width: 100%;
        border-collapse: collapse;
      }
      th,
      td {
        text-align: left;
        padding: 12px 8px;
        border-bottom: 1px solid var(--sc-border);
        font-size: 14px;
      }
      th {
        font-weight: 600;
      }
      tr:last-child td {
        border-bottom: 0;
      }
      code {
        font-size: 12px;
        background: var(--sc-surface-2);
        padding: 2px 6px;
        border-radius: 4px;
      }
      .risk {
        display: grid;
        grid-template-columns: 100px auto;
        gap: 8px;
        align-items: center;
      }
      .bar {
        height: 8px;
        border-radius: 4px;
      }
      .pill {
        font-size: 12px;
        padding: 4px 10px;
        border-radius: 999px;
        font-weight: 600;
        background: var(--sc-surface-2);
      }
      .pill-approve {
        background: #d1fae5;
        color: #047857;
      }
      .pill-review {
        background: #fef3c7;
        color: #b45309;
      }
      .pill-block {
        background: #fee2e2;
        color: #b91c1c;
      }
      .bl {
        list-style: none;
        padding: 0;
        margin: 0;
        display: flex;
        flex-direction: column;
        gap: 8px;
      }
      .bl li {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 8px 0;
        border-bottom: 1px solid var(--sc-border);
      }
      .bl li:last-child {
        border-bottom: 0;
      }
      .muted {
        color: var(--sc-text-muted);
      }
    `,
  ],
})
export class AdminFraudComponent implements OnInit {
  private readonly fraudApi = inject(FraudApi);

  readonly checks = signal<FraudCheck[]>([]);
  readonly blacklist = signal<FraudBlacklist[]>([]);
  readonly loading = signal(true);

  async ngOnInit(): Promise<void> {
    try {
      const [checksPage, blacklist] = await Promise.allSettled([
        firstValueFrom(this.fraudApi.listChecks(undefined, 0, 50)),
        firstValueFrom(this.fraudApi.listBlacklist()),
      ]);
      if (checksPage.status === 'fulfilled') this.checks.set(checksPage.value.content);
      if (blacklist.status === 'fulfilled') this.blacklist.set(blacklist.value);
    } finally {
      this.loading.set(false);
    }
  }

  riskColor(score: number): string {
    if (score >= 80) return 'var(--sc-danger)';
    if (score >= 60) return 'var(--sc-warn)';
    return 'var(--sc-success)';
  }

  pillClass(decision: FraudDecision): string {
    return {
      APPROVE: 'pill-approve',
      REVIEW: 'pill-review',
      BLOCK: 'pill-block',
    }[decision];
  }

  formatPrice(value: number): string {
    return new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(value);
  }
}
