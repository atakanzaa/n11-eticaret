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
  imports: [FormsModule, RouterLink, TPipe],
  template: `
    <div class="page">
      <header class="head">
        <a [routerLink]="['/']" class="back">←</a>
        <h1>{{ 'ai.assistant' | t }}</h1>
        <button type="button" class="reset" (click)="reset()">{{ 'common.cancel' | t }}</button>
      </header>

      <main class="messages">
        @if (assistant.messages().length === 0) {
          <div class="welcome">
            <h2>{{ 'ai.welcome' | t }}</h2>
            <p class="muted">Ürün önerileri, hediye fikirleri veya kategori karşılaştırmaları için sorabilirsiniz.</p>
          </div>
        }
        @for (m of assistant.messages(); track $index) {
          <div class="msg" [class]="m.role">
            <div class="bubble">{{ m.content }}</div>
          </div>
        }
        @if (assistant.loading()) {
          <div class="msg assistant">
            <div class="bubble typing">…</div>
          </div>
        }
      </main>

      <form class="composer" (submit)="onSubmit($event)">
        <input
          type="text"
          [(ngModel)]="draft"
          name="msg"
          [placeholder]="'ai.placeholder' | t"
          [disabled]="assistant.loading()"
          autofocus
        />
        <button type="submit" class="send" [disabled]="!draft.trim() || assistant.loading()">
          Gönder →
        </button>
      </form>
    </div>
  `,
  styles: [
    `
      .page {
        display: flex;
        flex-direction: column;
        height: calc(100dvh - 200px);
        max-width: 800px;
        margin: 0 auto;
        background: var(--sc-surface);
        border: 1px solid var(--sc-border);
        border-radius: var(--sc-radius-lg);
        overflow: hidden;
      }
      .head {
        display: flex;
        align-items: center;
        gap: 16px;
        padding: 16px 24px;
        border-bottom: 1px solid var(--sc-border);
      }
      .back {
        font-size: 20px;
        text-decoration: none;
        color: var(--sc-text);
        padding: 4px 12px;
        border-radius: var(--sc-radius-sm);
      }
      .back:hover {
        background: var(--sc-surface-2);
      }
      .head h1 {
        margin: 0;
        font-size: 18px;
        flex: 1;
      }
      .reset {
        background: none;
        border: 1px solid var(--sc-border);
        padding: 6px 12px;
        border-radius: var(--sc-radius-sm);
        cursor: pointer;
      }
      .messages {
        flex: 1;
        overflow-y: auto;
        padding: 24px;
        display: flex;
        flex-direction: column;
        gap: 12px;
      }
      .welcome {
        text-align: center;
        margin: auto;
      }
      .welcome h2 {
        font-size: 24px;
        margin: 0 0 8px;
        color: var(--sc-primary);
      }
      .msg {
        display: flex;
      }
      .msg.user {
        justify-content: flex-end;
      }
      .bubble {
        padding: 12px 16px;
        border-radius: var(--sc-radius);
        max-width: 70%;
        line-height: 1.4;
      }
      .user .bubble {
        background: var(--sc-primary);
        color: white;
      }
      .assistant .bubble {
        background: var(--sc-surface-2);
      }
      .typing {
        opacity: 0.6;
      }
      .composer {
        display: flex;
        gap: 8px;
        padding: 16px;
        border-top: 1px solid var(--sc-border);
      }
      .composer input {
        flex: 1;
        padding: 12px 16px;
        border: 1px solid var(--sc-border);
        border-radius: var(--sc-radius);
        font-size: 15px;
      }
      .send {
        background: var(--sc-primary);
        color: white;
        border: 0;
        padding: 0 20px;
        border-radius: var(--sc-radius);
        cursor: pointer;
        font-weight: 600;
      }
      .send:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }
      .muted {
        color: var(--sc-text-muted);
      }
    `,
  ],
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
