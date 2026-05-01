import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'sc-spinner',
  standalone: true,
  templateUrl: './spinner.component.html',
  styleUrls: ['./spinner.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SpinnerComponent {
  size = input<'sm' | 'base' | 'lg'>('base');
  label = input<string>('');
}
