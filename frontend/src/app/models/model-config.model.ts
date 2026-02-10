export interface ModelConfig {
  id: string;
  name: string;
  provider: 'openai' | 'anthropic' | 'ollama' | 'bedrock' | 'custom';
  model: string;
  parameters?: {
    temperature?: number;
    maxTokens?: number;
    topP?: number;
    frequencyPenalty?: number;
    presencePenalty?: number;
    providerKey?: string;
  };
  isDefault: boolean;
  isActive: boolean;
}

/** Request body for POST /api/models (create model config) */
export interface CreateModelConfigRequest {
  name: string;
  provider: string;
  model: string;
  parameters?: Record<string, unknown>;
  isDefault?: boolean;
  isActive?: boolean;
}
