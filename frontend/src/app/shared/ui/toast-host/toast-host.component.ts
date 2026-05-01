import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ToastService } from '@core/toast.service';

@Component({
  selector: 'sc-toast-host',
  standalone: true,
  templateUrl: './toast-host.component.html',
  styleUrls: ['./toast-host.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ToastHostComponent {
  protected readonly toast = inject(ToastService);
}
