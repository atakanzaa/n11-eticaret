import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AiAssistantService } from '@core/ai-assistant.service';
import { TPipe } from '@shared/i18n.pipe';

/**
 * Full-screen chat experience. Same `AiAssistantService` as the floating
 * widget — they share state, so the conversation continues across both
 * surfaces (open the widget, type, then click "tam ekran" → history is there).
 *
 * The mobile pattern is to land directly on `/asistan` instead of the floating
 * bubble, which can be hidden by the on-screen keyboard.
 */
@Component({
  selector: 'sc-ai-chat-fullscreen',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './ai-chat-fullscreen.component.html',
  styleUrls: ['./ai-chat-fullscreen.component.scss'],
  imports: [FormsModule, RouterLink, TPipe],
})
export class AiChatFullscreenComponent {
  protected readonly assistant = inject(AiAssistantService);
  draft = '';

  onSubmit(event: Event): void {
    event.preventDefault();
    const text = this.draft.trim();
    if (!text) return;
    this.draft = '';
    this.assistant.send(text).catch(() => {});
  }

  reset(): void {
    this.assistant.reset();
  }
}
