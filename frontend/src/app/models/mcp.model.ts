/** MCP server summary from GET /api/mcp/servers */
export interface McpServerSummary {
  name: string;
  url: string;
  source: 'static' | 'dynamic';
  /** Connection status: "unknown", "connected", "failed", "not_authorized" (OAuth server, user has not authorized) */
  status?: 'unknown' | 'connected' | 'failed' | 'not_authorized';
  /** OAuth provider id when server uses OAuth (e.g. "atlassian") */
  oauthProvider?: string;
}

/** Request body for POST /api/mcp/servers. Tools are discovered from the server at runtime. */
export interface RegisterMcpServerRequest {
  name: string;
  url: string;
  requestTimeoutSeconds?: number;
  headers?: Record<string, string>;
  /** OAuth provider id (e.g. "atlassian") when server uses OAuth */
  oauthProvider?: string;
}

/** Request body for PUT /api/mcp/servers/{name}. All fields optional (partial update). */
export interface UpdateMcpServerRequest {
  url?: string;
  requestTimeoutSeconds?: number;
  headers?: Record<string, string>;
  oauthProvider?: string;
}

/** Full server details from GET /api/mcp/servers/{name} (for edit). */
export interface McpServerDetail extends McpServerSummary {
  requestTimeoutSeconds?: number;
  headers?: Record<string, string>;
}
