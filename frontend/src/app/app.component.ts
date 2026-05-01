import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ToastHostComponent } from './shared/ui/toast-host/toast-host.component';

@Component({
  selector: 'sc-root',
  standalone: true,
  imports: [RouterOutlet, ToastHostComponent],
  template: `
    <router-outlet />
    <sc-toast-host />
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppComponent {}
