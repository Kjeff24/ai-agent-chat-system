# AI Agent Chat System — Usage Guide

This guide explains how to use the application: signing in, chatting, and configuring **model providers**, **model configs**, and **MCP servers** via the UI (and optionally the API).

---

## Prerequisites

- **Backend** and **frontend** running (see [README.md](README.md) for setup).
- Backend: `http://localhost:8080`
- Frontend: `http://localhost:4200`

---

## 1. Getting started

### 1.1 Sign in or register

1. Open **http://localhost:4200** in your browser.
2. Use the **Log in** / **Register** tabs:
   - **Log in:** Email and password (at least 6 characters).
   - **Register:** Name, email, and password.
3. After login you are taken to the main chat view.

### 1.2 Main screen

- **Left sidebar:** List of conversations. You can collapse/expand it (desktop).
- **Center:** Chat area for the selected conversation (or empty state if none).
- **Top:** Header with **New conversation** and **Settings** (gear icon).

---

## 2. Chat

### 2.1 Create a conversation

- Click **New conversation** in the header (or the **+** button in the sidebar).
- A new conversation is created using the **default model config** (see [Model configs](#4-model-configs) below).

### 2.2 Send messages

- Select a conversation from the sidebar (or use the one just created).
- Type in the message box at the bottom and press **Send** (or Enter).
- Your message appears on the right; the AI reply appears on the left when it’s ready.
- Messages are persisted and shown when you return to the conversation.

### 2.3 Conversation list

- **Click** a conversation to open it and load its messages.
- The list is ordered by last activity.
- New conversations get a title from the first message you send.

---

## 3. Model providers

Model providers supply the AI backends (OpenAI, Anthropic, Ollama, or custom). You add **providers** first, then pick one when creating a **model config**.

### 3.1 Open providers settings

- Click the **Settings** (gear) icon in the header.
- Open the **Model providers** tab.

### 3.2 Add a provider

1. Click **Add provider**.
2. Fill in:
   - **Provider name** — Unique key (e.g. `openai`, `openrouter`, `my-ollama`). No spaces. This is what you’ll select in model configs.
   - **Type** — `OpenAI / OpenRouter (openai)`, `Anthropic (anthropic)`, or `Ollama (ollama)`.
   - **API key** — Required for OpenAI and Anthropic. Leave blank when **editing** to keep the current key.
   - **Base URL (optional)** — Default is the official API. For **OpenRouter** use `https://openrouter.ai/api`.
   - **Models** — Comma- or newline-separated model IDs (e.g. `gpt-4`, `gpt-3.5-turbo`, `claude-3-opus`).
   - **Default model** — Optional; otherwise the first model in the list is used.
3. Click **Register** (or **Update** when editing).

### 3.3 Edit or remove a provider

- **Edit:** Click **Edit** next to a provider (only **Dynamic** ones are editable).
- **Remove:** Click **Remove** next to a **Dynamic** provider. Providers that come from backend config (e.g. built-in OpenAI, Anthropic, Ollama) cannot be removed from the UI.

### 3.4 Import provider from JSON

- When adding (not editing), click **Import JSON** and choose a JSON file. Example:

**OpenAI provider:**

```json
{
  "provider": "openai",
  "type": "openai",
  "apiKey": "sk-your-openai-api-key",
  "baseUrl": "https://api.openai.com",
  "models": ["gpt-4", "gpt-4o", "gpt-3.5-turbo"],
  "defaultModel": "gpt-4"
}
```

**OpenRouter provider:**

```json
{
  "provider": "openrouter",
  "type": "openai",
  "apiKey": "sk-or-your-openrouter-key",
  "baseUrl": "https://openrouter.ai/api",
  "models": ["anthropic/claude-3.5-sonnet", "openai/gpt-4o"],
  "defaultModel": "anthropic/claude-3.5-sonnet"
}
```

**Ollama (local) provider:**

```json
{
  "provider": "my-ollama",
  "type": "ollama",
  "baseUrl": "http://localhost:11434",
  "models": ["llama2", "mistral", "codellama"],
  "defaultModel": "llama2"
}
```

---

## 4. Model configs

Model configs define **which provider and model** to use for conversations. Each new conversation uses the **default** config until you change it.

### 4.1 Open model configs

- **Settings** → **Model configs** tab.

### 4.2 Add a model config

1. Click **Add model config**.
2. Fill in:
   - **Name** — Display name (e.g. “GPT-4 Chat”, “OpenRouter Claude”).
   - **Provider** — Choose a **registered** provider from the list (add one under Model providers first if the list is empty).
   - **Model** — Choose a model from that provider (or type a model ID if the list is empty).
   - **Set as default config** — Check to use this config for new conversations.
3. Click **Create**.

### 4.3 Set default or delete

- **Set default:** Click **Set default** next to a config that isn’t already default.
- **Delete:** Click **Delete** next to a config (only non-default configs can be deleted in the UI; you need at least one default).

### 4.4 Import config from JSON

- When adding a config, click **Import JSON** and choose a file. Example:

```json
{
  "name": "GPT-4 Chat",
  "provider": "openai",
  "model": "gpt-4",
  "parameters": {
    "temperature": 0.7,
    "maxTokens": 2000
  },
  "isDefault": true,
  "isActive": true
}
```

Another example (OpenRouter Claude, not default):

```json
{
  "name": "Claude via OpenRouter",
  "provider": "openrouter",
  "model": "anthropic/claude-3.5-sonnet",
  "parameters": {
    "temperature": 0.5,
    "maxTokens": 4096
  },
  "isDefault": false,
  "isActive": true
}
```

---

## 5. MCP servers

MCP (Model Context Protocol) servers expose **tools** the model can call when it decides they’re needed (e.g. search, files). You configure servers so the model sees their tools; the model then chooses when to call them.

### 5.1 Open MCP settings

- **Settings** → **MCP servers** tab.

### 5.2 Add an MCP server

1. Click **Add MCP server**.
2. Fill in:
   - **Name** — Unique label (e.g. `github`, `filesystem`). Read-only when editing.
   - **URL (Streamable HTTP)** — The MCP server’s Streamable HTTP endpoint (e.g. `https://api.githubcopilot.com/mcp/`).
   - **Request timeout (seconds)** — Optional (e.g. 30).
   Tools are discovered from the server at runtime; you don’t configure tool names or input schema.
3. Click **Register** (or **Update** when editing).

### 5.3 Edit or remove

- **Edit:** Click **Edit** next to a **dynamic** server (added via UI or API).
- **Remove:** Click **Remove** next to a **dynamic** server. Servers defined in backend config (static) cannot be removed from the UI.

### 5.4 Import MCP server from JSON

- When adding (not editing), click **Import JSON** and choose a file. Example:

```json
{
  "name": "my-mcp",
  "url": "https://your-mcp-server.example.com/sse",
  "requestTimeoutSeconds": 30
}
```

Example with headers (e.g. for GitHub MCP):

```json
{
  "name": "github",
  "url": "https://api.githubcopilot.com/mcp/",
  "requestTimeoutSeconds": 30,
  "headers": {
    "Authorization": "Bearer YOUR_GITHUB_PAT"
  }
}
```

Save as a `.json` file (e.g. `mcp-server.json`) and use **Import JSON** when adding a new MCP server. Replace `YOUR_GITHUB_PAT` with your token as needed.

### 5.5 Static vs dynamic

- **Static** — Defined in backend `application.yml` (e.g. a preconfigured GitHub MCP). Shown in the list but not editable/removable from the UI.
- **Dynamic** — Added via Settings or API. Can be edited and removed.

### 5.6 Connection status

Each server in the list shows a **status** badge after the backend has tried to connect (e.g. after you send a message in chat):

- **unknown** — Not yet probed.
- **connected** — Backend connected; tools from this server are available to the model.
- **failed** — Connection failed (wrong URL, 404, or auth rejected). No tools from this server. Check URL and auth (e.g. headers); remove the server if you don’t need it.

If a server shows **failed**, the model will not have its tools. For example, if Atlassian fails and GitHub succeeds, the model only sees GitHub tools and will correctly say it cannot access Atlassian.

### 5.7 Auth and server-specific notes

- **GitHub MCP** (`https://api.githubcopilot.com/mcp/`) — Uses a **Bearer token** in headers. Set `Authorization: Bearer YOUR_GITHUB_PAT` when adding the server (or in backend `application.yml`). Works with this app.
- **Atlassian Rovo MCP** (`https://mcp.atlassian.com/v1/mcp`) — Uses **OAuth 2.1** (browser-based login). This app supports OAuth: add an OAuth provider (e.g. `atlassian`) in Settings → MCP servers → OAuth providers, then add the Atlassian MCP server with that provider and click **Authorize**. If you see **404** after authorizing (token stored but “Server Not Found” in logs), Atlassian is likely rejecting the connection: they require your **domain** (and sometimes **IP**) to be allowlisted in **Atlassian Administration** → Rovo MCP Server settings. See [Available Atlassian Rovo MCP server domains](https://support.atlassian.com/security-and-access-policies/docs/available-atlassian-rovo-mcp-server-domains/) and [Control Atlassian Rovo MCP server settings](https://support.atlassian.com/security-and-access-policies/docs/control-atlassian-rovo-mcp-server-settings/). Running the app on `localhost` may not be allowed by your org’s policy.

---

## 6. Using the application end-to-end

1. **Start backend and frontend** (see [README.md](README.md)).
2. **Register or log in** at http://localhost:4200.
3. **Add a model provider** (Settings → Model providers): e.g. OpenAI with API key and models `gpt-4`, `gpt-3.5-turbo`.
4. **Add a model config** (Settings → Model configs): name “GPT-4”, provider = that provider, model = `gpt-4`, check “Set as default config”.
5. **Optional:** Add an MCP server (Settings → MCP servers) if you want the model to use tools (e.g. GitHub search).
6. **New conversation** → type a message and send. The model responds using the default config; if MCP is configured, the model may call tools when relevant.

---

## 7. API usage (optional)

If you prefer to configure providers, configs, or MCP via the API:

- **Swagger UI:** http://localhost:8080/swagger-ui.html (when the backend is running).
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs.

All API calls (except auth) require a valid JWT in the `Authorization` header:

```http
Authorization: Bearer <your-jwt>
```

### 7.1 Auth

- `POST /api/auth/register` — Register a new user.
- `POST /api/auth/login` — Log in; returns JWT and user info.

**Example — Register:**

```json
{
  "name": "Jane Doe",
  "email": "jane@example.com",
  "password": "securePassword123"
}
```

**Example — Login:**

```json
{
  "email": "jane@example.com",
  "password": "securePassword123"
}
```

### 7.2 Model providers (registry)

- `GET /api/models/registry` — List registered providers.
- `POST /api/models/registry` — Register a provider (type: `openai`, `anthropic`, `ollama`).

**Example — Register provider:**

```json
{
  "provider": "openrouter",
  "type": "openai",
  "apiKey": "sk-or-your-key",
  "baseUrl": "https://openrouter.ai/api",
  "models": ["anthropic/claude-3.5-sonnet", "openai/gpt-4o"],
  "defaultModel": "anthropic/claude-3.5-sonnet"
}
```

### 7.3 Model configs

- `GET /api/models` — List model configs.
- `POST /api/models` — Create config.
- `PATCH /api/models/{id}` — Update config (e.g. set default).
- `DELETE /api/models/{id}` — Delete config.

**Example — Create model config:**

```json
{
  "name": "GPT-4 Default",
  "provider": "openai",
  "model": "gpt-4",
  "parameters": {
    "temperature": 0.7,
    "maxTokens": 2000
  },
  "isDefault": true,
  "isActive": true
}
```

### 7.4 MCP servers

- `GET /api/mcp/servers` — List MCP servers (summary).
- `GET /api/mcp/servers/{name}` — Get full server details (for editing).
- `POST /api/mcp/servers` — Register server.
- `PUT /api/mcp/servers/{name}` — Update a **dynamic** server.
- `DELETE /api/mcp/servers/{name}` — Remove a **dynamic** server.

**Example — Register MCP server:**

```json
{
  "name": "github",
  "url": "https://api.githubcopilot.com/mcp/",
  "requestTimeoutSeconds": 30,
  "headers": {
    "Authorization": "Bearer YOUR_GITHUB_PAT"
  }
}
```

### 7.5 Conversations and messages

- `GET /api/conversations` — List your conversations.
- `POST /api/conversations` — Create conversation (body optional).
- `GET /api/conversations/{id}/messages` — List messages.
- `POST /api/conversations/{id}/messages` — Send message.

**Example — Create conversation (optional title):**

```json
{
  "title": "My first chat"
}
```

**Example — Send message:**

```json
{
  "content": "What is the weather like today?"
}
```

Use the JWT from login in the `Authorization` header for these requests.

---

## 8. Tips

- **No providers?** Add at least one under **Model providers** before creating a model config.
- **No configs?** Add at least one **model config** and set it as default so new conversations have a model.
- **MCP tools:** The model sees MCP tools and decides when to call them; you don’t need to trigger tools manually.
- **OpenRouter:** Add a provider with type **OpenAI**, base URL `https://openrouter.ai/api`, and your OpenRouter API key; then create a model config using that provider and the desired model ID.
- **Theme:** Use the sun/moon icon on the login page to toggle light/dark mode.

For setup and run instructions, see [README.md](README.md). For backend code structure, see [backend/docs/BACKEND.md](backend/docs/BACKEND.md).
