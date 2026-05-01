import { Component, ChangeDetectionStrategy } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { BuyerHeaderComponent } from '@shared/ui/buyer-header/buyer-header.component';
import { BuyerFooterComponent } from '@shared/ui/buyer-footer/buyer-footer.component';
import { AiChatComponent } from '@shared/ui/ai-chat/ai-chat.component';

@Component({
  selector: 'sc-buyer-shell',
  standalone: true,
  imports: [RouterOutlet, BuyerHeaderComponent, BuyerFooterComponent, AiChatComponent],
  templateUrl: './buyer-shell.component.html',
  styleUrls: ['./buyer-shell.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BuyerShellComponent {}
