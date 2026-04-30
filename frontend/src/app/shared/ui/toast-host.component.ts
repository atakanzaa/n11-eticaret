import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ToastService } from '@core/toast.service';

@Component({
  selector: 'sc-toast-host',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="host" aria-live="polite">
      @for (t of toast.toasts(); track t.id) {
        <div class="toast" [class]="t.kind" (click)="toast.dismiss(t.id)">{{ t.message }}</div>
      }
    </div>
  `,
  styles: [
    `
      .host {
        position: fixed;
        bottom: 24px;
        right: 24px;
        display: flex;
        flex-direction: column;
        gap: 8px;
        z-index: 9999;
      }
      .toast {
        background: var(--sc-text);
        color: white;
        padding: 12px 16px;
        border-radius: var(--sc-radius);
        box-shadow: var(--sc-shadow-lg);
        font-size: 14px;
        max-width: 360px;
        cursor: pointer;
      }
      .success {
        background: var(--sc-success);
      }
      .danger {
        background: var(--sc-danger);
      }
      .warn {
        background: var(--sc-warn);
        color: var(--sc-text);
      }
    `,
  ],
})
export class ToastHostComponent {
  protected toast = inject(ToastService);
}
