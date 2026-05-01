import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { AdminApi } from '@core/api/admin.api';
import { AdminOverviewResponse } from '@core/models/order.types';
import { TPipe } from '@shared/i18n.pipe';
import { KpiCardComponent } from '@shared/ui/kpi-card/kpi-card.component';
import { SpinnerComponent } from '@shared/ui/spinner/spinner.component';
import { CurrencyFormatPipe } from '@shared/pipes/currency-format.pipe';

@Component({
  selector: 'sc-admin-overview',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DecimalPipe, TPipe, KpiCardComponent, SpinnerComponent, CurrencyFormatPipe],
  templateUrl: './overview.component.html',
  styleUrls: ['./overview.component.scss'],
})
export class AdminOverviewComponent implements OnInit {
  private readonly adminApi = inject(AdminApi);

  readonly data = signal<AdminOverviewResponse | null>(null);
  readonly loading = signal(true);

  async ngOnInit(): Promise<void> {
    try {
      const data = await firstValueFrom(this.adminApi.overview());
      this.data.set(data);
    } finally {
      this.loading.set(false);
    }
  }
}
