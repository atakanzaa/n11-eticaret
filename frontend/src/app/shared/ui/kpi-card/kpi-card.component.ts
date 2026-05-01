import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'sc-kpi-card',
  standalone: true,
  templateUrl: './kpi-card.component.html',
  styleUrls: ['./kpi-card.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class KpiCardComponent {
  label = input.required<string>();
  value = input.required<string | number>();
  trend = input<'up' | 'down' | 'flat' | null>(null);
  trendLabel = input<string>('');
}
