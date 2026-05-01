import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

@Component({
  selector: 'sc-address-card',
  standalone: true,
  templateUrl: './address-card.component.html',
  styleUrls: ['./address-card.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AddressCardComponent {
  label = input.required<string>();
  fullName = input.required<string>();
  phone = input<string>('');
  address = input.required<string>();
  district = input.required<string>();
  city = input.required<string>();
  selected = input<boolean>(false);
  selectable = input<boolean>(false);
  showActions = input<boolean>(true);

  select = output<void>();
  edit = output<void>();
  delete = output<void>();
}
