import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'sc-form-field',
  standalone: true,
  templateUrl: './form-field.component.html',
  styleUrls: ['./form-field.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FormFieldComponent {
  label = input.required<string>();
  required = input<boolean>(false);
  error = input<string>('');
  hint = input<string>('');
}
