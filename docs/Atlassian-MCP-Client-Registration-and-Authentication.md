# Registering Your Application as an Atlassian MCP Client

This guide explains how to register your application as an OAuth client for the Atlassian Rovo MCP Server, configure authentication, and connect to Jira, Confluence, and Compass via the Model Context Protocol (MCP).

---

## Overview

The **Atlassian Rovo MCP Server** exposes Jira, Confluence, and Compass tools over the Model Context Protocol. To integrate your application, you must:

1. Register as an OAuth 2.0 / 2.1 client
2. Configure OAuth endpoints in your application
3. Implement the authorization flow (authorize → callback → token exchange)
4. Use the access token for MCP requests
5. Handle token refresh when access tokens expire

---

## Prerequisites

Before you begin, ensure you have:

- An **Atlassian Cloud site** with Jira, Compass, and/or Confluence
- Access to **Atlassian Administration** (or Atlassian Developer) to register your app
- A backend application that can handle OAuth redirects and store tokens
- A modern browser for the OAuth authorization flow

> **Note:** As of late 2025, Atlassian's public MCP server is in beta and may restrict custom OAuth client registration to approved partners or whitelisted applications. If registration is not available in your Atlassian admin, check [Atlassian Community](https://community.atlassian.com/forums/Rovo-questions/Can-I-register-my-own-MCP-OAuth-client-for-Atlassian-Remote-MCP/qaq-p/3105807) for the latest status.

---

## 1. OAuth Discovery

Atlassian exposes OAuth metadata at a well-known URL. Use this to obtain endpoint URLs and supported features.

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

Key endpoints:

| Purpose | URL |
|--------|-----|
| Authorize (user consent) | `https://mcp.atlassian.com/v1/authorize` |
| Token exchange & refresh | `https://cf.mcp.atlassian.com/v1/token` |
| MCP server endpoint | `https://mcp.atlassian.com/v1/mcp` |

---

## 2. Registering Your Application

### Step 1: Create an OAuth App

1. Go to **[Atlassian Developer](https://developer.atlassian.com/)** or your **Atlassian Administration** → **Rovo MCP Server**.
2. Create a new OAuth 2.0 application (or use the MCP registration flow if available).
3. Note the **Client ID** and **Client Secret** — store these securely (e.g., in environment variables).

### Step 2: Configure Redirect URI

Set the **Redirect URI** to your backend callback endpoint. It must match exactly what your app sends in the authorize request.

**Examples:**
- Production: `https://your-backend-host/api/mcp/servers/oauth/callback`
- Local development: `http://localhost:8080/api/mcp/servers/oauth/callback`
- Desktop apps: `http://localhost:3334/oauth/callback` (common for localhost-based clients)

> ⚠️ **Important:** Atlassian may restrict redirect URIs to localhost for some clients. Check the latest [Atlassian documentation](https://support.atlassian.com/atlassian-rovo-mcp-server/docs/authentication-and-authorization/) for current restrictions.

### Step 3: Add Your Domains

In **Atlassian Administration** → **Rovo MCP Server** → **Your domains**, add the origin of your backend so MCP requests with that `Origin` header are accepted.

Examples:
- `http://localhost:8080` (local dev)
- `https://your-backend-host` (production)

---

## 3. Application Configuration

### OAuth Provider Configuration

Configure your OAuth provider using the discovery endpoints. Example (YAML):

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

**Scopes:** Use `openid email profile` for compatibility with standard MCP clients (e.g., Claude Desktop).

### MCP Server Configuration

Register the Atlassian MCP server in your application:

```yaml
mcp:
  servers:
    atlassian:
      url: https://mcp.atlassian.com/v1/mcp
      request-timeout-seconds: 30
      oauth-provider: atlassian
```

The `oauth-provider` value must match the key under `app.mcp.oauth.providers`.

---

## 4. Authentication Flow

### 4.1 How OAuth Works

1. User clicks **Authorize** or **Connect** in your app.
2. Backend redirects the browser to Atlassian's authorization page.
3. User signs in at Atlassian (if needed) and approves the requested scopes.
4. Atlassian redirects back to your callback URL with `?code=...&state=...`.
5. Backend exchanges the authorization code for access and refresh tokens.
6. Tokens are stored per user and per server.
7. MCP requests use `Authorization: Bearer <access_token>`.

### 4.2 Authorize (Start OAuth)

1. User is authenticated in your app (e.g., via JWT).
2. Frontend opens or redirects to: `GET /api/mcp/servers/atlassian/oauth/authorize` (with user's auth).
3. Backend builds the authorization URL with:
   - `authorization_uri`: `https://mcp.atlassian.com/v1/authorize`
   - `client_id`, `redirect_uri`, `scope`, `state` (signed state binds callback to user + server)
4. Backend **redirects the browser** to Atlassian's authorize page.
5. User completes consent; Atlassian redirects to your callback with `?code=...&state=...`.

### 4.3 Callback (Exchange Code for Tokens)

1. Backend receives: `GET /api/mcp/servers/oauth/callback?code=...&state=...`
2. Validate `state` and recover `userId` and `serverName`.
3. Exchange the code for tokens:

   **Request:**
   - **Method:** `POST`
   - **URL:** `https://cf.mcp.atlassian.com/v1/token`
   - **Headers:** `Content-Type: application/x-www-form-urlencoded`
   - **Body (form):**
     - `grant_type=authorization_code`
     - `code=<authorization_code>`
     - `redirect_uri=<same as in authorize>`
     - `client_id=<your client id>`
     - `client_secret=<your client secret>`

4. Save `access_token`, `refresh_token`, `expires_at` per `(userId, serverName)`.
5. Redirect user to frontend (e.g., `/settings?mcp_oauth=success` or `failed`).

### 4.4 Using the Token for MCP

- When creating an MCP client, look up the stored token for the current user and server.
- Add `Authorization: Bearer <access_token>` to all MCP requests to `https://mcp.atlassian.com/v1/mcp`.
- Optionally add the callback origin as `Origin` header if required.

---

## 5. Token Refresh

Tokens expire. Refresh them when the access token is expired and a refresh token exists.

**Refresh request:**
- **URL:** `https://cf.mcp.atlassian.com/v1/token`
- **Method:** `POST`
- **Headers:** `Content-Type: application/x-www-form-urlencoded`
- **Body (form):**
  - `grant_type=refresh_token`
  - `refresh_token=<stored_refresh_token>`
  - `client_id=<your client id>`
  - `client_secret=<your client secret>`

**Success response** includes `access_token`, `token_type`, `expires_in`, and often a new `refresh_token`. Save the new values and use the updated access token for subsequent MCP requests.

---

## 6. Token Behavior & Security

| Aspect | Details |
|--------|---------|
| **Scope** | Tokens are scoped to a specific cloud site (e.g., `example.atlassian.net`). |
| **Permissions** | Tokens inherit the user's existing Jira, Compass, and Confluence permissions. |
| **Validity** | Sessions are time-limited; re-authentication may be required after expiry or revocation. |
| **Sharing** | Tokens are never shared between users. |
| **Storage** | Store access and refresh tokens encrypted at rest. |

**Best practices:**
- Use a cryptographically random `state` and bind it to `userId` and `serverName`.
- Validate redirect URIs strictly (no open redirects).
- Use HTTPS in production for authorize and callback URLs.
- Use least privilege and monitor audit logs.

---

## 7. Testing

### Test Token Refresh with Postman

1. Get a `refresh_token` from your database for the desired user/server.
2. **POST** `https://cf.mcp.atlassian.com/v1/token`
3. Set body to **x-www-form-urlencoded**:
   - `grant_type` = `refresh_token`
   - `refresh_token` = `<value from DB>`
   - `client_id` = `<from config>`
   - `client_secret` = `<from config>`
4. A 200 response with `access_token` and `expires_in` confirms refresh works.

### Common Authentication Issues

| Issue | Possible cause | Resolution |
|-------|----------------|------------|
| Flow doesn't launch | Pop-up blocker or CLI error | Re-run, disable pop-up blockers |
| Redirect fails | Blocked localhost or wrong redirect URI | Allowlist callback URI, check network |
| Access denied | Insufficient Jira/Confluence permissions | Verify product access with site admin |
| No data returned | Token expired or wrong scopes | Re-authenticate or check granted scopes |
| Token exchange fails (400) | 3LO client not recognized by MCP auth | Ensure you're using an MCP-registered client; 3LO apps may not work |

---

## 8. Quick Reference

| Step | Action |
|------|--------|
| Discovery | [https://mcp.atlassian.com/.well-known/oauth-authorization-server](https://mcp.atlassian.com/.well-known/oauth-authorization-server) |
| Register | Redirect URI = backend callback; add domain; get client_id and client_secret |
| Configure | `app.mcp.oauth.providers.atlassian` and `mcp.servers.atlassian` |
| Connect | User hits authorize → callback → code exchange → tokens stored |
| Use | MCP client uses `Authorization: Bearer <access_token>` |
| Refresh | POST to token endpoint with `grant_type=refresh_token` when expired |

---

## Related Resources

- [Atlassian Rovo MCP Server - Getting Started](https://support.atlassian.com/atlassian-rovo-mcp-server/docs/getting-started-with-the-atlassian-remote-mcp-server/)
- [Authentication and Authorization](https://support.atlassian.com/atlassian-rovo-mcp-server/docs/authentication-and-authorization/)
- [Setting up clients](https://support.atlassian.com/atlassian-rovo-mcp-server/docs/setting-up-clients/)
- [MCP Clients - Security risks](https://www.atlassian.com/blog/artificial-intelligence/mcp-risk-awareness)
- [OAuth Discovery](https://mcp.atlassian.com/.well-known/oauth-authorization-server)
