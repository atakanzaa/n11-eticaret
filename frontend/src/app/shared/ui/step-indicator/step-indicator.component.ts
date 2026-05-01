import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'sc-step-indicator',
  standalone: true,
  templateUrl: './step-indicator.component.html',
  styleUrls: ['./step-indicator.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StepIndicatorComponent {
  steps = input.required<string[]>();
  /** 0-based index of the active step */
  currentStep = input.required<number>();
}
