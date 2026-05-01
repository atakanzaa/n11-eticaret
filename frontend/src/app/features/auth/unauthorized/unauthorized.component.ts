import { Component, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TPipe } from '@shared/i18n.pipe';

@Component({
  selector: 'sc-unauthorized',
  standalone: true,
  imports: [RouterLink, TPipe],
  templateUrl: './unauthorized.component.html',
  styleUrls: ['./unauthorized.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UnauthorizedComponent {}
