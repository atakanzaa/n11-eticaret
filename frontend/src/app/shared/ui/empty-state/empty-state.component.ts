import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'sc-empty-state',
  standalone: true,
  templateUrl: './empty-state.component.html',
  styleUrls: ['./empty-state.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EmptyStateComponent {
  icon = input<string>('');
  title = input.required<string>();
  description = input<string>('');
}
