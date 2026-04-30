export type NotificationChannel = 'EMAIL' | 'SMS' | 'PUSH';
export type NotificationStatus = 'PENDING' | 'SENT' | 'FAILED';

export interface NotificationLogResponse {
  id: string;
  userId?: string;
  channel: NotificationChannel;
  templateCode: string;
  recipient: string;
  subject?: string;
  status: NotificationStatus;
  correlationId?: string;
  errorMessage?: string;
  sentAt?: string;
  createdAt: string;
}
