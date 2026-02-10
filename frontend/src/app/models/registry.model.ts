/** Single provider entry from GET /api/models/registry */
export interface ProviderWithMeta {
  name: string;
  dynamic: boolean;
  models?: string[];
  defaultModel?: string;
}

/** Response from GET /api/models/registry/{provider} when dynamic (edit details) */
export interface ProviderDetailsResponse {
  provider: string;
  registered: boolean;
  dynamic: boolean;
  type?: string;
  baseUrl?: string;
  models?: string[];
  defaultModel?: string;
  /** Placeholder when API key is set (e.g. "••••••••"); leave blank in form to keep current. */
  apiKeyMasked?: string;
  /** Placeholder when AWS secret key is set (bedrock only). */
  secretKeyMasked?: string;
}

/** Request body for PATCH /api/models/registry/{provider}. Omit fields to keep current. */
export interface UpdateProviderRequest {
  apiKey?: string;
  secretKey?: string;
  baseUrl?: string;
  models?: string[];
  defaultModel?: string;
}

/** Response from GET /api/models/registry */
export interface RegistryResponse {
  providers: string[];
  count: number;
  providersWithMeta: ProviderWithMeta[];
}

/** Request body for POST /api/models/registry/discover */
export interface DiscoverModelsRequest {
  type: 'openai' | 'anthropic' | 'ollama' | 'bedrock';
  apiKey?: string;
  secretKey?: string;
  baseUrl?: string;
}

/** Response from POST /api/models/registry/discover */
export interface DiscoverModelsResponse {
  models: string[];
}

/** Request body for POST /api/models/registry */
export interface RegisterModelRequest {
  provider: string;
  type: 'openai' | 'anthropic' | 'ollama' | 'bedrock';
  apiKey?: string;
  secretKey?: string;
  baseUrl?: string;
  models?: string[];
  defaultModel?: string;
}
