import { Injectable, signal } from '@angular/core';

export interface Toast {
  id: number;
  message: string;
  kind: 'info' | 'success' | 'danger' | 'warn';
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private _toasts = signal<Toast[]>([]);
  readonly toasts = this._toasts.asReadonly();
  private nextId = 1;

  show(message: string, kind: Toast['kind'] = 'info', duration = 2800) {
    const t: Toast = { id: this.nextId++, message, kind };
    this._toasts.update(arr => [...arr, t]);
    setTimeout(() => this.dismiss(t.id), duration);
  }

  dismiss(id: number) {
    this._toasts.update(arr => arr.filter(t => t.id !== id));
  }
}
