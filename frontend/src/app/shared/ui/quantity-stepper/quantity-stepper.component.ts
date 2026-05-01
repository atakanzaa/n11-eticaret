import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

@Component({
  selector: 'sc-quantity-stepper',
  standalone: true,
  templateUrl: './quantity-stepper.component.html',
  styleUrls: ['./quantity-stepper.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuantityStepperComponent {
  value = input.required<number>();
  min = input<number>(1);
  max = input<number>(99);
  disabled = input<boolean>(false);

  valueChange = output<number>();

  decrement(): void {
    const v = this.value();
    if (v > this.min()) this.valueChange.emit(v - 1);
  }

  increment(): void {
    const v = this.value();
    if (v < this.max()) this.valueChange.emit(v + 1);
  }
}
