export interface Conversation {
  id: string;
  userId: string;
  title?: string;
  modelConfigId: string;
  metadata?: Record<string, any>;
  createdAt: string;
  updatedAt: string;
}
