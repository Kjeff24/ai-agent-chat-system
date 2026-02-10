/** OAuth provider summary from GET /api/mcp/oauth/providers */
export interface OAuthProviderSummary {
  id: string;
  authorizationUri: string;
  tokenUri: string;
  clientIdMasked?: string;
  scopes?: string;
  source: 'static' | 'dynamic';
}

/** OAuth provider detail from GET /api/mcp/oauth/providers/{id} (for edit). No clientSecret. */
export interface OAuthProviderDetail {
  id: string;
  authorizationUri: string;
  tokenUri: string;
  clientId?: string;
  scopes?: string;
  source: 'static' | 'dynamic';
}

/** Request body for POST /api/mcp/oauth/providers */
export interface RegisterOAuthProviderRequest {
  id: string;
  authorizationUri: string;
  tokenUri: string;
  clientId?: string;
  clientSecret?: string;
  scopes?: string;
}

/** Request body for PUT /api/mcp/oauth/providers/{id}. All fields optional. */
export interface UpdateOAuthProviderRequest {
  authorizationUri?: string;
  tokenUri?: string;
  clientId?: string;
  clientSecret?: string;
  scopes?: string;
}
