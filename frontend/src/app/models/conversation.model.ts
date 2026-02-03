export interface Conversation {
  id: string;
  userId: string;
  title?: string;
  /** Provider key from registry (e.g. openai, bedrock). */
  providerKey?: string;
  /** Model id for the provider (e.g. gpt-4o). */
  model?: string;
  /** @deprecated Prefer providerKey + model. */
  modelConfigId?: string;
  metadata?: Record<string, any>;
  createdAt: string;
  updatedAt: string;
}
