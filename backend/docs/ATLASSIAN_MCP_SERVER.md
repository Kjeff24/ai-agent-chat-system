# Atlassian MCP Server — Setup and OAuth Flow

This guide walks through registering as an OAuth client, configuring the application, connecting to the Atlassian MCP server, obtaining tokens, and using refresh tokens.

---

## 1. OAuth discovery

Atlassian’s MCP OAuth metadata is available at:

**Discovery URL:** [https://mcp.atlassian.com/.well-known/oauth-authorization-server](https://mcp.atlassian.com/.well-known/oauth-authorization-server)

Example response:

```json
{
  "issuer": "https://cf.mcp.atlassian.com",
  "authorization_endpoint": "https://mcp.atlassian.com/v1/authorize",
  "token_endpoint": "https://cf.mcp.atlassian.com/v1/token",
  "registration_endpoint": "https://cf.mcp.atlassian.com/v1/register",
  "response_types_supported": ["code"],
  "response_modes_supported": ["query"],
  "grant_types_supported": ["authorization_code", "refresh_token"],
  "token_endpoint_auth_methods_supported": ["client_secret_basic", "client_secret_post", "none"],
  "revocation_endpoint": "https://cf.mcp.atlassian.com/v1/token",
  "code_challenge_methods_supported": ["plain", "S256"]
}
```

Use these values for configuration:

- **Authorize:** `https://mcp.atlassian.com/v1/authorize`
- **Token (exchange code / refresh):** `https://cf.mcp.atlassian.com/v1/token`

---

## 2. Registering as a client

1. **Register your app** with Atlassian’s MCP OAuth (e.g. via [Atlassian Developer](https://developer.atlassian.com/) or the Rovo/MCP registration flow that uses the discovery document).
2. **Redirect URI:** Set the redirect URI to your backend callback, e.g.  
   `https://your-backend-host/api/mcp/servers/oauth/callback`  
   (for local dev, e.g. `http://localhost:8080/api/mcp/servers/oauth/callback`). It must match exactly what the app sends in the authorize request.
3. **Your domains (if required):** In Atlassian Administration → Rovo MCP server → “Your domains”, add the origin of your backend (e.g. `http://localhost:8080` or `https://your-backend-host`) so MCP requests with that `Origin` header are accepted.
4. **Credentials:** Obtain **client ID** and **client secret** and keep them secret (e.g. in environment variables).

---

## 3. Application configuration

### 3.1 OAuth provider (app.mcp.oauth)

In `application.yml` (or via environment variables), configure the Atlassian MCP OAuth provider using the discovery endpoints:

```yaml
app:
  mcp:
    oauth:
      callback-uri: https://your-backend-host/api/mcp/servers/oauth/callback
      # For local dev: http://localhost:8080/api/mcp/servers/oauth/callback
      providers:
        atlassian:
          authorization-uri: https://mcp.atlassian.com/v1/authorize
          token-uri: https://cf.mcp.atlassian.com/v1/token
          client-id: ${ATLASSIAN_MCP_CLIENT_ID}
          client-secret: ${ATLASSIAN_MCP_CLIENT_SECRET}
          scopes: "openid email profile"
```

Use the same scopes as Claude Desktop (`openid email profile`) for compatibility.

### 3.2 MCP server entry

Add the Atlassian MCP server so the app knows its URL and to use OAuth for that server:

**Static config (application.yml):**

```yaml
mcp:
  servers:
    atlassian:
      url: https://mcp.atlassian.com/v1/mcp
      request-timeout-seconds: 30
      oauth-provider: atlassian
```

**Or register dynamically** via API:

```http
POST /api/mcp/servers
Content-Type: application/json

{
  "name": "atlassian",
  "url": "https://mcp.atlassian.com/v1/mcp",
  "requestTimeoutSeconds": 30,
  "oauthProvider": "atlassian"
}
```

The `oauth-provider` / `oauthProvider` value must match the key under `app.mcp.oauth.providers` (e.g. `atlassian`).

---

## 4. Connecting and getting tokens

### 4.1 Authorize (start OAuth)

1. User is logged in to the app (JWT).
2. Frontend opens or redirects to:  
   `GET /api/mcp/servers/atlassian/oauth/authorize`  
   with the JWT (e.g. in `Authorization` header or cookie).
3. Backend builds the authorization URL using:
   - `authorization_uri`: `https://mcp.atlassian.com/v1/authorize`
   - `client_id`, `redirect_uri`, `scope`, `state` (signed state ties the callback to the user and server).
4. Backend **redirects the browser** to Atlassian’s authorize page.
5. User signs in at Atlassian (if needed) and consents; Atlassian redirects back to your **callback** with `?code=...&state=...`.

### 4.2 Callback (exchange code for tokens)

1. Backend receives:  
   `GET /api/mcp/servers/oauth/callback?code=...&state=...`
2. Validates `state`, recovers `userId` and `serverName`.
3. Exchanges the `code` for tokens:  
   `POST https://cf.mcp.atlassian.com/v1/token`  
   with body (e.g. `application/x-www-form-urlencoded`):
   - `grant_type=authorization_code`
   - `code=...`
   - `redirect_uri=...` (same as in authorize)
   - `client_id=...`
   - `client_secret=...`
4. Saves **access_token**, **refresh_token**, **expires_at** (and optional scope) per `(userId, serverName)` and stores **provider_id** (e.g. `atlassian`) for refresh.
5. Redirects the user to the frontend (e.g. `/settings?mcp_oauth=success` or `failed`).

After this, the server appears as “connected” for that user when the app has a valid (or refreshable) token.

### 4.3 Using the token for MCP

- When creating an MCP client for an OAuth-enabled server, the backend looks up the stored token for the current user and server.
- If the access token is expired, it uses the **refresh token** to get a new access (and optionally refresh) token from `https://cf.mcp.atlassian.com/v1/token` and saves the new values.
- MCP requests to `https://mcp.atlassian.com/v1/mcp` are sent with `Authorization: Bearer <access_token>` and, if configured, the callback origin as `Origin` header.

---

## 5. Refresh token flow

The app refreshes tokens automatically when:

- A valid access token is needed (e.g. for listing tools or calling tools), and
- The stored access token is expired, and
- A refresh token and `provider_id` are stored.

Refresh request (same as in the app and testable in Postman):

- **URL:** `https://cf.mcp.atlassian.com/v1/token`
- **Method:** `POST`
- **Headers:** `Content-Type: application/x-www-form-urlencoded`
- **Body (form):**
  - `grant_type=refresh_token`
  - `refresh_token=<stored_refresh_token>`
  - `client_id=<your client id>`
  - `client_secret=<your client secret>`

Success response includes `access_token`, `token_type`, `expires_in`, and often a new `refresh_token` and `scope`. The app saves the new access token, the new refresh token (if present), and `expires_at` (from `expires_in`).

---

## 6. Testing refresh with Postman

1. Get a **refresh_token** from your database (`mcp_oauth_token.refresh_token` for the desired user/server).
2. **POST** `https://cf.mcp.atlassian.com/v1/token`
3. **Body** → **x-www-form-urlencoded**:
   - `grant_type` = `refresh_token`
   - `refresh_token` = &lt;value from DB&gt;
   - `client_id` = &lt;from config&gt;
   - `client_secret` = &lt;from config&gt;
4. Send the request. A 200 response with `access_token` and `expires_in` confirms refresh works; the app uses the same request and then persists the new tokens.

---

## 7. Summary

| Step | What |
|------|------|
| Discovery | [https://mcp.atlassian.com/.well-known/oauth-authorization-server](https://mcp.atlassian.com/.well-known/oauth-authorization-server) |
| Register client | Redirect URI = backend callback; add your domain if required; get client_id and client_secret. |
| Configure app | `app.mcp.oauth.providers.atlassian` (authorization-uri, token-uri, client-id, client-secret, scopes) and `mcp.servers.atlassian` (url, oauth-provider). |
| Connect | User hits authorize → callback → code exchange → tokens stored (with provider_id for refresh). |
| Use | MCP client uses stored access token; if expired, app refreshes via token endpoint and saves new tokens. |

For the general OAuth-for-MCP design, see [MCP_OAUTH_DESIGN.md](./MCP_OAUTH_DESIGN.md).
