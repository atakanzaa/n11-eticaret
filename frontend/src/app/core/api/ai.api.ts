import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AiConversation,
  AiMessage,
  AiUsageBreakdownResponse,
  ChatRequest,
  ChatResponse,
  EnrichmentResult,
} from '@core/models/ai.types';

@Injectable({ providedIn: 'root' })
export class AiApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/ai`;

  chat(request: ChatRequest): Observable<ChatResponse> {
    return this.http.post<ChatResponse>(`${this.base}/chat`, request);
  }

  myConversations(): Observable<AiConversation[]> {
    return this.http.get<AiConversation[]>(`${this.base}/conversations/me`);
  }

  conversationMessages(id: string): Observable<AiMessage[]> {
    return this.http.get<AiMessage[]>(`${this.base}/conversations/${id}/messages`);
  }

  enrichProduct(productId: string): Observable<EnrichmentResult> {
    return this.http.post<EnrichmentResult>(`${this.base}/products/${productId}/enrich`, {});
  }

  todayUsage(): Observable<{ spent: number; remaining: number }> {
    return this.http.get<{ spent: number; remaining: number }>(`${this.base}/usage/today`);
  }

  usageBreakdown(days = 7): Observable<AiUsageBreakdownResponse> {
    return this.http.get<AiUsageBreakdownResponse>(`${this.base}/usage/breakdown`, {
      params: new HttpParams().set('days', days),
    });
  }
}
