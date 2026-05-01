import { Component, ChangeDetectionStrategy } from '@angular/core';
import { RouterOutlet, RouterLink } from '@angular/router';

/**
 * Minimal frame for the public auth pages (login, register, unauthorized).
 * Deliberately *not* the full BuyerShell -- those screens should not show the
 * shopping header/footer because the user has no shopping context yet.
 */
@Component({
  selector: 'sc-auth-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink],
  templateUrl: './auth-shell.component.html',
  styleUrls: ['./auth-shell.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AuthShellComponent {}
