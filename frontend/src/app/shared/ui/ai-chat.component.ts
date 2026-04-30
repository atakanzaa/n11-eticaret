import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AiAssistantService } from '@core/ai-assistant.service';
import { AuthStateService } from '@core/auth/auth-state.service';
import { TPipe } from '@shared/i18n.pipe';

/**
 * Floating chat widget. Real backend AI calls — every message round-trips
 * through `/api/ai/chat`, the orchestrator picks the configured provider
 * (Anthropic / OpenAI / Gemini), and the conversation id persists across
 * page reloads via `AiAssistantService`.
 */
@Component({
  selector: 'sc-ai-chat',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, TPipe],
  template: `
    @if (auth.isAuthenticated()) {
      <div class="widget" [class.open]="open()">
        @if (open()) {
          <header>
            <span>{{ 'ai.assistant' | t }}</span>
            <button type="button" (click)="toggle()" class="close">×</button>
          </header>
          <div class="messages">
            @if (assistant.messages().length === 0) {
              <p class="welcome">{{ 'ai.welcome' | t }}</p>
            }
            @for (m of assistant.messages(); track $index) {
              <div class="msg" [class]="m.role">{{ m.content }}</div>
            }
            @if (assistant.loading()) {
              <div class="msg assistant typing">…</div>
            }
          </div>
          <form class="composer" (submit)="onSubmit($event)">
            <input
              type="text"
              [(ngModel)]="draft"
              name="msg"
              [placeholder]="'ai.placeholder' | t"
              [disabled]="assistant.loading()"
            />
            <button type="submit" [disabled]="!draft.trim() || assistant.loading()">→</button>
          </form>
        } @else {
          <button type="button" class="bubble" (click)="toggle()">💬</button>
        }
      </div>
    }
  `,
  styles: [
    `
      .widget {
        position: fixed;
        bottom: 24px;
        right: 24px;
        z-index: 90;
      }
      .bubble {
        width: 56px;
        height: 56px;
        border-radius: 50%;
        background: var(--sc-primary);
        color: white;
        font-size: 24px;
        border: 0;
        cursor: pointer;
        box-shadow: var(--sc-shadow-lg);
      }
      .open {
        width: 360px;
        background: white;
        border-radius: var(--sc-radius-lg);
        box-shadow: var(--sc-shadow-lg);
        display: flex;
        flex-direction: column;
        max-height: 520px;
      }
      header {
        background: var(--sc-primary);
        color: white;
        padding: 12px 16px;
        border-radius: var(--sc-radius-lg) var(--sc-radius-lg) 0 0;
        display: flex;
        justify-content: space-between;
        font-weight: 600;
      }
      .close {
        background: none;
        border: 0;
        color: white;
        font-size: 24px;
        cursor: pointer;
      }
      .messages {
        flex: 1;
        overflow-y: auto;
        padding: 16px;
        display: flex;
        flex-direction: column;
        gap: 8px;
        max-height: 360px;
      }
      .welcome {
        color: var(--sc-text-muted);
        font-size: 14px;
        margin: 0;
      }
      .msg {
        padding: 10px 14px;
        border-radius: var(--sc-radius);
        font-size: 14px;
        line-height: 1.4;
        max-width: 85%;
      }
      .msg.user {
        background: var(--sc-primary-50);
        align-self: flex-end;
      }
      .msg.assistant {
        background: var(--sc-surface-2);
        align-self: flex-start;
      }
      .typing {
        opacity: 0.6;
      }
      .composer {
        display: flex;
        gap: 8px;
        padding: 12px;
        border-top: 1px solid var(--sc-border);
      }
      .composer input {
        flex: 1;
        padding: 10px 12px;
        border: 1px solid var(--sc-border);
        border-radius: var(--sc-radius-sm);
      }
      .composer button {
        background: var(--sc-primary);
        color: white;
        border: 0;
        padding: 0 16px;
        border-radius: var(--sc-radius-sm);
        cursor: pointer;
      }
      .composer button:disabled {
        opacity: 0.5;
      }
    `,
  ],
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
