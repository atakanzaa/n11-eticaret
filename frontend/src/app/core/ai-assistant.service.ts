import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AiApi } from '@core/api/ai.api';

const STORAGE_KEY = 'sc_ai_conversation_id';

export interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
}

/**
 * AI shopping assistant facade. Calls the backend AI orchestrator (which
 * fans out to Anthropic / OpenAI / Gemini based on the `ai.provider` env
 * setting), and remembers the conversation id in localStorage so reloading
 * the chat preserves context across sessions.
 */
@Injectable({ providedIn: 'root' })
export class AiAssistantService {
  private readonly aiApi = inject(AiApi);

  private readonly _messages = signal<ChatMessage[]>([]);
  private readonly _loading = signal(false);
  private conversationId: string | undefined = this.loadConversationId();

  readonly messages = this._messages.asReadonly();
  readonly loading = this._loading.asReadonly();

  async send(userMessage: string): Promise<void> {
    const trimmed = userMessage.trim();
    if (!trimmed || this._loading()) return;

    this._messages.update((arr) => [...arr, { role: 'user', content: trimmed }]);
    this._loading.set(true);

    try {
      const response = await firstValueFrom(
        this.aiApi.chat({ conversationId: this.conversationId, message: trimmed }),
      );
      this.conversationId = response.conversationId;
      this.persistConversationId(response.conversationId);
      this._messages.update((arr) => [...arr, { role: 'assistant', content: response.message }]);
    } catch {
      this._messages.update((arr) => [
        ...arr,
        { role: 'assistant', content: '[Bir hata oluştu, lütfen tekrar deneyin.]' },
      ]);
    } finally {
      this._loading.set(false);
    }
  }

  reset(): void {
    this._messages.set([]);
    this.conversationId = undefined;
    try {
      localStorage.removeItem(STORAGE_KEY);
    } catch {
      /* swallow */
    }
  }

  private loadConversationId(): string | undefined {
    try {
      return localStorage.getItem(STORAGE_KEY) ?? undefined;
    } catch {
      return undefined;
    }
  }

  private persistConversationId(id: string): void {
    try {
      localStorage.setItem(STORAGE_KEY, id);
    } catch {
      /* swallow */
    }
  }
}
