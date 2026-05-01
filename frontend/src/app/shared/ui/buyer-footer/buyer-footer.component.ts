import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'sc-buyer-footer',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './buyer-footer.component.html',
  styleUrls: ['./buyer-footer.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BuyerFooterComponent {}
