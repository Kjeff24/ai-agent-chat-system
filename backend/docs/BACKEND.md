# Backend Code Overview

This document explains the structure and flow of the AI Agent Chat System backend so you can navigate and extend the codebase.

## Overview

The backend is a Spring Boot 4 application that provides:

- **REST API** for auth, conversations, messages, model configs, and MCP servers
- **WebSocket (STOMP)** for real-time delivery of new messages
- **AI integration** via Spring AI (OpenAI, Anthropic, Ollama) with configurable models per conversation
- **MCP (Model Context Protocol)** integration so the model can call external tools when it decides they are needed

All API endpoints except `/api/auth/**` and `/ws/**` require a valid JWT. The frontend sends the JWT in the `Authorization` header and uses it for WebSocket handshake as well.

---

## Package Structure and Layers

```
com.aiagent.chatsystem
├── ChatSystemApplication.java   # Entry point; excludes some Spring AI auto-configs
├── config/                       # Security, WebSocket, MCP properties, OpenAPI, global exception handler
├── controller/                   # REST: auth, conversations, models, model registry, MCP servers
├── dto/                          # Request/response DTOs (no entities exposed)
├── exception/                    # Custom exceptions + GlobalExceptionHandler
├── model/                        # JPA entities (User, Conversation, Message, ModelConfig, …)
├── repository/                  # Spring Data JPA repositories
└── service/                      # Business logic
    └── impl/                     # Service implementations
```

- **Controllers** only validate input, resolve the current user (from JWT), and call services; they do not contain business logic.
- **Services** implement use cases and call repositories, `AIModelService`, and `McpClientService` as needed.
- **DTOs** are used for all API request/response bodies; entities are not serialized directly.
- **Exceptions** are thrown by services and mapped to HTTP status and body in `GlobalExceptionHandler`.

---

## Request Flow: Sending a Message

Typical path when a user sends a chat message:

1. **ConversationController**  
   `POST /api/conversations/{id}/messages` receives the body (e.g. `SendMessageRequest`), resolves `userId` from `Authentication` (JWT), and calls `ConversationService.sendMessage(conversationId, request, userId)`.

2. **ConversationServiceImpl.sendMessage**  
   - Loads the conversation and checks ownership.  
   - Persists the user message and notifies over WebSocket (`/topic/conversation/{id}`) so the UI can show it immediately.  
   - Optionally sets the conversation title from the first message.  
   - Loads the conversation’s `ModelConfig` and builds the list of Spring AI `Message` instances from stored messages (user/assistant only).  
   - **If MCP is enabled:** calls `McpClientService.getToolCallbacks()` to get all MCP tools as Spring AI `ToolCallback`s, then calls `AIModelService.generate(messages, modelConfig, toolCallbacks)`.  
   - **Otherwise:** calls `AIModelService.generate(messages, modelConfig)` (no tools).  
   - The model may request tool calls; the framework executes them via the callbacks and continues until a final text response.  
   - Persists the assistant message and broadcasts it over WebSocket.  
   - Returns the assistant message as DTO.

3. **AIModelServiceImpl.generate**  
   - Resolves the `ChatModel` from `ModelRegistry` using the conversation’s model config (provider/model).  
   - If `toolCallbacks` are provided, builds a `Prompt` with `ToolCallingChatOptions` so the model can request tool calls.  
   - Calls `ChatModel.call(prompt)`; Spring AI runs the tool-call loop (model → tool execution → model) and returns the final text.  
   - Returns that text as the assistant reply.

4. **McpClientService / McpToolCallbackAdapter**  
   - `getToolCallbacks()` aggregates tools from all configured MCP servers (from `application.yml` and dynamically registered servers).  
   - For each MCP tool, it creates an `McpToolCallbackAdapter` that implements Spring AI’s `ToolCallback`: name/description/inputSchema come from the MCP server; `call(toolInput)` parses JSON and delegates to `McpClientService.executeTool(serverName, toolName, args)`.  
   - Tool names are prefixed (e.g. `mcp_{serverName}__{toolName}`) so they are unique across servers.

So: **Controller → ConversationService → AIModelService (+ optional MCP tool callbacks) → ChatModel**. MCP tools are only invoked when the model requests them (model-driven tool use).

---

## Key Classes

### Entry and configuration

- **ChatSystemApplication**  
  Boot application; excludes some Spring AI auto-configurations so the app can start without OpenAI/Anthropic API keys; those providers are enabled when configured at runtime.

- **config/SecurityConfig**  
  JWT filter, CORS, stateless session; permits `/api/auth/**`, `/ws/**`, and Swagger; requires authentication for all other API paths.

- **config/WebSocketConfig**  
  STOMP over SockJS; destination prefix `/app`; broker prefix `/topic`; conversation messages on `/topic/conversation/{id}`.

- **config/McpProperties**  
  Binds `mcp.servers` and related options (URLs, optional headers, request timeout) from `application.yml`. Tools are discovered from each server at runtime; no tool list or input schema is configured.

### Controllers

- **AuthController**  
  Register, login, refresh; returns JWT and user info.

- **ConversationController**  
  CRUD for conversations and messages; all methods take the authenticated user and delegate to `ConversationService`.

- **ModelManagementController**  
  Registry of providers (list, register); used to add OpenAI/Anthropic/Ollama/custom at runtime.

- **McpServerController**  
  List MCP servers; get by name; register (POST); update (PUT) and delete (DELETE) for dynamically added servers only.

### Services

- **ConversationService / ConversationServiceImpl**  
  Conversation and message persistence; loading history; calling `AIModelService` (with or without MCP tools) and broadcasting new messages via WebSocket.

- **AIModelService / AIModelServiceImpl**  
  Resolves `ChatModel` from `ModelRegistry` by provider key (and optional model id); builds prompts; supports optional `List<ToolCallback>` for model-driven tool use; returns generated text.

- **ModelRegistry**  
  Holds per-provider `ChatModel` instances (from `ModelFactory` and dynamic registration). Used by `AIModelServiceImpl` to get the right model for each request.

- **ModelFactory**  
  Builds Spring AI `ChatModel` instances from `RegisterModelRequest` (OpenAI, Anthropic, Ollama).

- **McpClientService / McpClientServiceImpl**  
  Manages MCP server configs (static + dynamic); creates MCP clients (Streamable HTTP) on demand; lists tools; executes tools; exposes `getToolCallbacks()` for Spring AI tool use.

- **McpToolCallbackAdapter**  
  Implements Spring AI `ToolCallback` for one MCP tool: `getToolDefinition()` (name, description, inputSchema); `call(toolInput)` parses JSON and calls `McpClientService.executeTool(serverName, toolName, args)`.

### Model and persistence

- **model/**  
  JPA entities: `User`, `Conversation`, `Message`, `ModelConfig`, `DynamicProviderRegistration`, etc.  
  Repositories in **repository/** provide standard and custom queries (e.g. by user, by conversation, default model config).

---

## MCP Integration (Model-Driven Tools)

- MCP servers are configured in `application.yml` under `mcp.servers` or added at runtime via `POST /api/mcp/servers`.
- For each server, the backend uses the MCP Java SDK (Streamable HTTP) to connect, list tools, and call tools.
- When a user sends a message and MCP is enabled, `ConversationServiceImpl` gets `List<ToolCallback>` from `McpClientService.getToolCallbacks()` and passes it to `AIModelService.generate(..., toolCallbacks)`.
- The model sees tool definitions (name, description, input schema) and can request tool calls; the framework invokes the matching `ToolCallback.call(toolInput)`, which runs `McpClientService.executeTool(...)` and returns the result to the model.
- So tools are **not** called on every message; they are called only when the model decides to use them (similar to Claude Desktop).

---

## Configuration

- **application.yml**  
  Data source, JPA, Redis, Spring AI defaults (per provider), JWT, CORS, MCP servers.  
  Many values can be overridden by environment variables (see comments and placeholders in the file).

- **Optional .env**  
  Loaded via `spring.config.import: optional:file:.env[.properties]` for local overrides.

---

## JavaDoc

Generated API docs for the backend:

```bash
mvn javadoc:javadoc
```

Output: `target/site/apidocs/`. Open `target/site/apidocs/index.html` in a browser.

For a JAR of the JavaDoc (e.g. for distribution or IDE):

```bash
mvn javadoc:jar
```

See [README.md](../README.md) for full backend setup, run, and build instructions.
