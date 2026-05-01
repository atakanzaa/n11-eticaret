import { Component, ChangeDetectionStrategy } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { TPipe } from '@shared/i18n.pipe';

@Component({
  selector: 'sc-account-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, TPipe],
  templateUrl: './account-shell.component.html',
  styleUrls: ['./account-shell.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AccountShellComponent {}
