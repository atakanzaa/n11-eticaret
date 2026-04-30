export interface ChatRequest {
  conversationId?: string;
  message: string;
}

export interface ChatResponse {
  conversationId: string;
  message: string;
}

export interface AiConversation {
  id: string;
  userId: string;
  title?: string;
  createdAt: string;
  updatedAt: string;
}

export interface AiMessage {
  id: string;
  conversationId: string;
  role: 'USER' | 'ASSISTANT' | 'SYSTEM' | 'TOOL';
  content: string;
  toolName?: string;
  toolArgs?: string;
  createdAt: string;
}

export interface EnrichmentResult {
  description: string;
  bullets: string[];
  keywords: string[];
}

export interface ProviderUsage {
  provider: string;
  requestCount: number;
  costUsd: number;
  inputTokens: number;
  outputTokens: number;
}

export interface PurposeUsage {
  purpose: string;
  requestCount: number;
  costUsd: number;
}

export interface DailyUsage {
  date: string;
  costUsd: number;
  requestCount: number;
}

export interface AiUsageBreakdownResponse {
  byProvider: ProviderUsage[];
  byPurpose: PurposeUsage[];
  dailyUsage: DailyUsage[];
  dailyBudgetUsd: number;
  todayUsedUsd: number;
  remainingUsd: number;
}
