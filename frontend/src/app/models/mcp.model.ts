/** MCP server summary from GET /api/mcp/servers */
export interface McpServerSummary {
  name: string;
  url: string;
  source: 'static' | 'dynamic';
}

/** Request body for POST /api/mcp/servers */
export interface RegisterMcpServerRequest {
  name: string;
  url: string;
  requestTimeoutSeconds?: number;
  headers?: Record<string, string>;
}
