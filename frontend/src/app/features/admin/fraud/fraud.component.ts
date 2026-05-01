import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { FraudApi } from '@core/api/fraud.api';
import { FraudBlacklist, FraudCheck, FraudDecision } from '@core/models/fraud.types';
import { TPipe } from '@shared/i18n.pipe';
import { SpinnerComponent } from '@shared/ui/spinner/spinner.component';
import { StatusBadgeComponent } from '@shared/ui/status-badge/status-badge.component';
import { EmptyStateComponent } from '@shared/ui/empty-state/empty-state.component';
import { CurrencyFormatPipe } from '@shared/pipes/currency-format.pipe';

@Component({
  selector: 'sc-admin-fraud',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, TPipe, SpinnerComponent, StatusBadgeComponent, EmptyStateComponent, CurrencyFormatPipe],
  templateUrl: './fraud.component.html',
  styleUrls: ['./fraud.component.scss'],
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
}
