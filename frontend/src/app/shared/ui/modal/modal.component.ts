import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

@Component({
  selector: 'sc-modal',
  standalone: true,
  templateUrl: './modal.component.html',
  styleUrls: ['./modal.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ModalComponent {
  open = input.required<boolean>();
  title = input<string>('');
  size = input<'sm' | 'base' | 'lg'>('base');
  closable = input<boolean>(true);

  closed = output<void>();

  close(): void {
    if (this.closable()) this.closed.emit();
  }

  onBackdropClick(): void {
    this.close();
  }
}
