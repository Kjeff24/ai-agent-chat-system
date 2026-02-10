# OAuth for MCP Servers — Design

This document describes how to support MCP servers that require **OAuth 2.x** (e.g. Atlassian Rovo MCP) instead of static Bearer tokens. Today the app only supports header-based auth (e.g. `Authorization: Bearer <token>`); OAuth requires a browser redirect flow and **per-user** tokens.

---

## 1. High-level flow

1. **Configure an OAuth-enabled MCP server**  
   Admin configures which servers use OAuth (e.g. by server name) and the provider’s endpoints (authorize URL, token URL, client ID, client secret, scopes). Example: Atlassian.

2. **User authorizes**  
   User adds the MCP server (e.g. “atlassian”) in Settings. For OAuth servers, the UI shows an **“Authorize”** (or “Connect”) button. Clicking it:
   - Frontend opens or redirects to a backend URL: `GET /api/mcp/servers/{name}/oauth/authorize` (with JWT).
   - Backend builds the provider’s authorization URL (with `client_id`, `redirect_uri`, `scope`, `state`), stores `state → (userId, serverName)` (e.g. in a short-lived cache or signed cookie), and **redirects the browser** to the provider.

3. **User logs in at the provider**  
   User signs in at the provider (e.g. Atlassian), consents to scopes, and the provider **redirects back** to your callback with `?code=...&state=...`.

4. **Callback**  
   Backend endpoint: `GET /api/mcp/servers/oauth/callback?code=...&state=...`
   - Validate `state`, recover `userId` and `serverName`.
   - Exchange `code` for `access_token` (and optionally `refresh_token`) via the provider’s token URL.
   - **Store tokens** per user and per server (e.g. in DB, encrypted).
   - Redirect the user back to the frontend (e.g. `/settings?mcp_oauth=success` or `failed`).

5. **Using the token for MCP**  
   When the backend builds an MCP client for a given server and **current user** (e.g. when handling `sendMessage`):
   - If the server is **OAuth-enabled**: look up the stored access token for `(userId, serverName)`. If present and not expired, add `Authorization: Bearer <access_token>` to MCP requests. If expired and you have a refresh token, refresh and store the new access token, then use it.
   - If the server is **not** OAuth-enabled: use existing static headers from config (current behavior).

So: **OAuth tokens are per user and per server**; MCP client creation (or request headers) must be **user-aware** when the server uses OAuth.

---

## 2. What you need to implement

### 2.1 Backend

- **OAuth provider config**  
  For each OAuth-enabled MCP server (e.g. “atlassian”):  
  `authorization_uri`, `token_uri`, `client_id`, `client_secret` (from env or config), `scope`.  
  Example in `application.yml`:
  ```yaml
  app:
    mcp:
      oauth-servers:
        atlassian:
          authorization-uri: https://auth.atlassian.com/authorize
          token-uri: https://auth.atlassian.com/oauth/token
          client-id: ${ATLASSIAN_MCP_CLIENT_ID}
          client-secret: ${ATLASSIAN_MCP_CLIENT_SECRET}
          scopes: "read:jira-work read:confluence-content.all offline_access"
  ```
  (Use the real Atlassian OAuth URLs and scopes from their docs.)

- **Token storage**  
  New entity, e.g. `McpOAuthToken`:  
  `user_id`, `server_name`, `access_token`, `refresh_token` (nullable), `expires_at`.  
  Encrypt tokens at rest.  
  Repository: e.g. `findByUserIdAndServerName(UUID userId, String serverName)`.

- **Endpoints**  
  - `GET /api/mcp/servers/{name}/oauth/authorize`  
    - Require JWT; get `userId` from `Authentication`.  
    - If server is not OAuth-enabled, return 400.  
    - Build `state` (e.g. signed JWT or random ID stored in cache with `userId` + `serverName`).  
    - Redirect to provider’s `authorization_uri` with `response_type=code`, `client_id`, `redirect_uri`, `scope`, `state`.  
  - `GET /api/mcp/servers/oauth/callback`  
    - Query params: `code`, `state`.  
    - Validate `state`, recover `userId` and `serverName`.  
    - Exchange `code` for tokens (POST to `token_uri` with `grant_type=authorization_code`, `code`, `redirect_uri`, `client_id`, `client_secret`).  
    - Save or update `McpOAuthToken` for that user and server.  
    - Redirect to frontend, e.g. `https://your-frontend/settings?mcp_oauth=success` or `failed`.

- **MCP client creation (user-aware)**  
  - Today: `McpClientService.getToolCallbacks()` has no user; clients are global per server.  
  - Change: add an overload that takes `UUID userId`, e.g. `getToolCallbacks(UUID userId)`.  
  - In the implementation, when creating a client for a given server:  
    - If the server is OAuth-enabled: load `McpOAuthToken` for `(userId, serverName)`. If none or expired (and refresh fails), skip this server or mark it failed. Otherwise set `Authorization: Bearer <access_token>` on the transport (and optionally refresh token in background).  
    - If not OAuth-enabled: use existing static headers from config.  
  - **Call site**: In `ConversationServiceImpl.sendMessage(..., userId)`, call `getToolCallbacks(userId)` instead of `getToolCallbacks()` so chat uses the current user’s OAuth tokens.

- **Caching / per-user clients**  
  Currently there is one MCP client per server (global). For OAuth you need **per-user** (or per-user-per-server) clients because the Bearer token is per user. Options:  
  - Cache key `(serverName, userId)` for OAuth servers and keep a single global client for non-OAuth servers.  
  - Or build the client (or at least the request headers) per request for OAuth servers using the stored token, and keep the current cache for non-OAuth.

- **List servers / status**  
  For OAuth servers, “connected” can mean “this user has a valid (or refreshable) token”. So when listing servers for the current user, you can set status to “connected” if a valid `McpOAuthToken` exists for `(userId, serverName)`, and “failed” or “not_authorized” if not. That may require passing `userId` into `listServers` (or having the controller add a per-server “oauth_connected” flag from a new method that checks token existence).

### 2.2 Frontend

- **Authorize button**  
  For servers that are configured as OAuth-enabled (e.g. from a new API field or from a list of known OAuth server names), show an **“Authorize”** or **“Connect”** button.  
  Clicking it: open or redirect to `GET /api/mcp/servers/{name}/oauth/authorize` (with JWT in header if using fetch, or open in same window so the redirect goes to the provider).  
  After the user completes the flow, the callback redirects back to the frontend (e.g. `/settings?mcp_oauth=success`). The frontend can show a toast and refresh the MCP server list (and status).

- **Callback handling**  
  Optional: dedicated route `/settings/oauth/callback` that the backend redirects to with query params, so the frontend can show “Authorization successful” and refresh; or the backend redirects directly to `/settings?mcp_oauth=success` and the frontend reads the query param and refreshes.

### 2.3 Provider-specific setup (e.g. Atlassian)

- Register an OAuth app in the provider’s developer console (e.g. Atlassian Developer).
- Get **client ID** and **client secret**.
- Set **redirect URI** to your callback, e.g. `https://your-backend/api/mcp/servers/oauth/callback`.
- Use the provider’s documented **authorization** and **token** URLs and **scopes** in your `app.mcp.oauth-servers.atlassian` config.

---

## 3. Security considerations

- **State**: Always use a cryptographically random `state` and bind it to `userId` and `serverName` so the callback cannot be used to attach a token to another user.
- **Tokens**: Store access (and refresh) tokens encrypted at rest; restrict DB and backups.
- **Redirect URI**: Validate exactly in the provider’s app config (no open redirects).
- **HTTPS**: Use HTTPS in production for authorize and callback URLs.

---

## 4. Phasing

- **Phase 1**: Token storage + authorize + callback for one provider (e.g. Atlassian), and user-aware `getToolCallbacks(userId)` using that token. No refresh token yet.
- **Phase 2**: Refresh token support and per-user “connected” status in the list.
- **Phase 3**: Generalize to multiple OAuth-enabled MCP servers via config (same code, different `oauth-servers` entries).

This design lets you support any MCP server that uses OAuth: configure its endpoints and credentials, then users authorize once per server; their tokens are used for MCP calls in chat.
