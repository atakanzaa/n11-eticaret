export type FraudDecision = 'APPROVE' | 'REVIEW' | 'BLOCK';

export type BlacklistType = 'USER_ID' | 'EMAIL' | 'IP' | 'CARD_BIN';

export interface FraudCheckRequest {
  orderId: string;
  userId: string;
  amount: number;
  currency: string;
  itemCount: number;
  userIp?: string;
  firstOrder?: boolean;
}

export interface FraudCheckResult {
  flagged: boolean;
  decision: FraudDecision;
  riskScore: number;
  reason?: string;
  triggeredRules: string[];
}

export interface FraudCheck {
  id: string;
  orderId: string;
  userId: string;
  amount: number;
  riskScore: number;
  decision: FraudDecision;
  triggeredRules: string[];
  reason?: string;
  createdAt: string;
}

export interface FraudBlacklist {
  id: string;
  blacklistType: BlacklistType;
  value: string;
  reason: string;
  createdAt: string;
  active: boolean;
}

export interface CreateBlacklistRequest {
  blacklistType: BlacklistType;
  value: string;
  reason: string;
}
