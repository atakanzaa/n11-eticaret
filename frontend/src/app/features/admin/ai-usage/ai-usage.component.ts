import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { AiApi } from '@core/api/ai.api';
import { AiUsageBreakdownResponse } from '@core/models/ai.types';
import { TPipe } from '@shared/i18n.pipe';
import { KpiCardComponent } from '@shared/ui/kpi-card/kpi-card.component';
import { SpinnerComponent } from '@shared/ui/spinner/spinner.component';

@Component({
  selector: 'sc-admin-ai-usage',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DecimalPipe, DatePipe, TPipe, KpiCardComponent, SpinnerComponent],
  templateUrl: './ai-usage.component.html',
  styleUrls: ['./ai-usage.component.scss'],
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
