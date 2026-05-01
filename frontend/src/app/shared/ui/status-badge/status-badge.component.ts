import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'sc-status-badge',
  standalone: true,
  templateUrl: './status-badge.component.html',
  styleUrls: ['./status-badge.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StatusBadgeComponent {
  label = input.required<string>();
  variant = input<'success' | 'warning' | 'danger' | 'info' | 'neutral' | 'primary'>('neutral');
  size = input<'sm' | 'base'>('base');
}
