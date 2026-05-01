import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AiAssistantService } from '@core/ai-assistant.service';
import { AuthStateService } from '@core/auth/auth-state.service';
import { TPipe } from '@shared/i18n.pipe';

/**
 * Floating chat widget. Real backend AI calls -- every message round-trips
 * through `/api/ai/chat`, the orchestrator picks the configured provider
 * (Anthropic / OpenAI / Gemini), and the conversation id persists across
 * page reloads via `AiAssistantService`.
 */
@Component({
  selector: 'sc-ai-chat',
  standalone: true,
  imports: [FormsModule, TPipe],
  templateUrl: './ai-chat.component.html',
  styleUrls: ['./ai-chat.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AiChatComponent {
  protected readonly assistant = inject(AiAssistantService);
  protected readonly auth = inject(AuthStateService);

  readonly open = signal(false);
  draft = '';

  toggle(): void {
    this.open.update((v) => !v);
  }

  onSubmit(event: Event): void {
    event.preventDefault();
    const text = this.draft.trim();
    if (!text) return;
    this.draft = '';
    this.assistant.send(text).catch(() => {});
  }
}
