import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { AiApi } from '@core/api/ai.api';
import { AiUsageBreakdownResponse } from '@core/models/ai.types';
import { TPipe } from '@shared/i18n.pipe';

@Component({
  selector: 'sc-admin-ai-usage',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DecimalPipe, DatePipe, TPipe],
  template: `
    <h1>{{ 'admin.aiUsage' | t }}</h1>

    @if (loading()) {
      <p class="muted">{{ 'common.loading' | t }}</p>
    } @else if (data()) {
      <section class="budget">
        <article class="card">
          <span class="label">{{ 'ai.todayUsed' | t }}</span>
          <span class="value">{{ formatUsd(data()!.todayUsedUsd) }}</span>
        </article>
        <article class="card">
          <span class="label">{{ 'ai.dailyBudget' | t }}</span>
          <span class="value">{{ formatUsd(data()!.dailyBudgetUsd) }}</span>
        </article>
        <article class="card">
          <span class="label">{{ 'ai.remaining' | t }}</span>
          <span class="value">{{ formatUsd(data()!.remainingUsd) }}</span>
        </article>
      </section>

      <section class="card">
        <h3>{{ 'ai.providerDistribution' | t }}</h3>
        @if (data()!.byProvider.length === 0) {
          <p class="muted">{{ 'common.empty' | t }}</p>
        } @else {
          <ul class="providers">
            @for (p of data()!.byProvider; track p.provider) {
              <li>
                <strong>{{ p.provider }}</strong>
                <span>{{ p.requestCount | number }} istek</span>
                <span>{{ formatUsd(p.costUsd) }}</span>
                <div class="bar-container">
                  <div class="bar" [style.width.%]="percent(p.costUsd)"></div>
                </div>
              </li>
            }
          </ul>
        }
      </section>

      <section class="card">
        <h3>{{ 'ai.dailySpend' | t }}</h3>
        @if (data()!.dailyUsage.length === 0) {
          <p class="muted">{{ 'common.empty' | t }}</p>
        } @else {
          <table>
            <thead>
              <tr>
                <th>Tarih</th>
                <th>Maliyet</th>
                <th>İstek</th>
              </tr>
            </thead>
            <tbody>
              @for (d of data()!.dailyUsage; track d.date) {
                <tr>
                  <td>{{ d.date | date: 'mediumDate' }}</td>
                  <td>{{ formatUsd(d.costUsd) }}</td>
                  <td>{{ d.requestCount | number }}</td>
                </tr>
              }
            </tbody>
          </table>
        }
      </section>
    }
  `,
  styles: [
    `
      h1 {
        margin: 0 0 24px;
      }
      .budget {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
        gap: 16px;
        margin-bottom: 24px;
      }
      .card {
        background: var(--sc-surface);
        border: 1px solid var(--sc-border);
        border-radius: var(--sc-radius);
        padding: 24px;
        margin-bottom: 16px;
      }
      .budget .card {
        margin-bottom: 0;
        display: flex;
        flex-direction: column;
        gap: 8px;
      }
      .label {
        color: var(--sc-text-muted);
        font-size: 13px;
        text-transform: uppercase;
        letter-spacing: 0.05em;
      }
      .value {
        font-size: 28px;
        font-weight: 700;
        color: var(--sc-primary);
      }
      .card h3 {
        margin: 0 0 16px;
      }
      .providers {
        list-style: none;
        padding: 0;
        margin: 0;
        display: flex;
        flex-direction: column;
        gap: 12px;
      }
      .providers li {
        display: grid;
        grid-template-columns: 1fr 1fr 1fr 200px;
        gap: 12px;
        align-items: center;
        padding: 8px 0;
        border-bottom: 1px solid var(--sc-border);
      }
      .providers li:last-child {
        border-bottom: 0;
      }
      .bar-container {
        background: var(--sc-surface-2);
        height: 8px;
        border-radius: 4px;
        overflow: hidden;
      }
      .bar {
        height: 100%;
        background: var(--sc-primary);
      }
      table {
        width: 100%;
        border-collapse: collapse;
      }
      th,
      td {
        text-align: left;
        padding: 8px 12px;
        border-bottom: 1px solid var(--sc-border);
      }
      tr:last-child td {
        border-bottom: 0;
      }
      .muted {
        color: var(--sc-text-muted);
      }
    `,
  ],
})
export class AdminAiUsageComponent implements OnInit {
  private readonly aiApi = inject(AiApi);

  readonly data = signal<AiUsageBreakdownResponse | null>(null);
  readonly loading = signal(true);

  readonly totalCost = computed(() => {
    const d = this.data();
    if (!d) return 0;
    return d.byProvider.reduce((sum, p) => sum + p.costUsd, 0);
  });

  async ngOnInit(): Promise<void> {
    try {
      const data = await firstValueFrom(this.aiApi.usageBreakdown(7));
      this.data.set(data);
    } finally {
      this.loading.set(false);
    }
  }

  percent(cost: number): number {
    const total = this.totalCost();
    return total > 0 ? Math.round((cost / total) * 100) : 0;
  }

  formatUsd(value: number): string {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 4,
      maximumFractionDigits: 4,
    }).format(value);
  }
}
