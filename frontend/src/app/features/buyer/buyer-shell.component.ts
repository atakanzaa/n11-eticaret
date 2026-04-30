import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { BuyerHeaderComponent } from '@shared/ui/buyer-header.component';
import { BuyerFooterComponent } from '@shared/ui/buyer-footer.component';
import { AiChatComponent } from '@shared/ui/ai-chat.component';

@Component({
  selector: 'sc-buyer-shell',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, BuyerHeaderComponent, BuyerFooterComponent, AiChatComponent],
  template: `
    <sc-buyer-header />
    <main class="main">
      <router-outlet />
    </main>
    <sc-buyer-footer />
    <sc-ai-chat />
  `,
  styles: [
    `
      :host {
        display: block;
      }
      .main {
        max-width: var(--sc-container);
        margin: 0 auto;
        padding: 24px;
        min-height: calc(100dvh - 200px);
      }
    `,
  ],
})
export class BuyerShellComponent {}
