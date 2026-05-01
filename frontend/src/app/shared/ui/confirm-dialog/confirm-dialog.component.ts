import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { ModalComponent } from '../modal/modal.component';

@Component({
  selector: 'sc-confirm-dialog',
  standalone: true,
  imports: [ModalComponent],
  templateUrl: './confirm-dialog.component.html',
  styleUrls: ['./confirm-dialog.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConfirmDialogComponent {
  open = input.required<boolean>();
  title = input<string>('Emin misiniz?');
  message = input<string>('');
  confirmLabel = input<string>('Onayla');
  cancelLabel = input<string>('Vazgec');
  variant = input<'danger' | 'primary'>('primary');

  confirmed = output<void>();
  cancelled = output<void>();
}
