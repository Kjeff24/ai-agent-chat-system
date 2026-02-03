# AI Agent Chat System — Backend

Spring Boot backend for the AI Agent Chat System: configurable AI models (OpenAI, Anthropic, Ollama), real-time chat over WebSocket, and MCP (Model Context Protocol) integration for model-driven tool use.

## Tech Stack

- **Java 21**
- **Spring Boot 4.x**
- **Spring AI 2.x** — OpenAI, Anthropic, Ollama
- **Spring WebSocket (STOMP)** — real-time messages
- **Spring Security** — JWT authentication
- **Spring Data JPA** — PostgreSQL
- **Spring Data Redis** — caching/session
- **MCP Java SDK** — Streamable HTTP MCP clients
- **SpringDoc OpenAPI** — Swagger UI

## Prerequisites

- Java 21+
- Maven 3.8+
- PostgreSQL 12+
- Redis (optional; used if configured)

## Quick Start

1. **Configure database** in `src/main/resources/application.yml` (or env):

   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/ai_chat
       username: postgres
       password: postgres
   ```

2. **Build and run:**

   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

   Server runs at **http://localhost:8080**.

3. **Optional env vars** (see `application.yml`):

   - `OPENAI_API_KEY`, `ANTHROPIC_API_KEY` — for cloud models
   - `JWT_SECRET` — JWT signing key
   - `DATABASE_URL`, `DB_USERNAME`, `DB_PASSWORD`
   - `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`
   - `GITHUB_PAT` — for GitHub MCP (if used)

## Configuration

- **Main config:** `src/main/resources/application.yml`
- **Env override:** `.env` (optional; see `application.yml` `spring.config.import`)
- **Dev example:** `application-dev.yml.example`

Key sections:

- `spring.datasource` — PostgreSQL
- `spring.data.redis` — Redis
- `spring.ai` — default model options per provider
- `jwt` — secret and token expiration
- `cors` — allowed origins (e.g. `http://localhost:4200`)
- `mcp` — MCP servers (URLs, optional headers, request timeout; tools discovered at runtime)

## API Overview

- **Swagger UI:** http://localhost:8080/swagger-ui.html (when running)
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs

| Area            | Endpoints |
|-----------------|-----------|
| Auth            | `POST /api/auth/register`, `POST /api/auth/login`, `POST /api/auth/refresh` |
| Conversations   | `GET/POST /api/conversations`, `GET/PATCH/DELETE /api/conversations/{id}`, `GET/POST /api/conversations/{id}/messages` |
| Models          | `GET/POST /api/models`, `GET/PATCH/DELETE /api/models/{id}` |
| Model registry  | `GET /api/models/registry`, `POST /api/models/registry` (register provider) |
| MCP servers     | `GET /api/mcp/servers`, `GET/POST/PUT/DELETE /api/mcp/servers/{name}` |

All API calls (except auth) require a valid JWT in the `Authorization` header.

## WebSocket

- **Endpoint:** `ws://localhost:8080/ws` (SockJS + STOMP)
- **Subscribe:** `/topic/conversation/{conversationId}` — receive new messages for a conversation
- **Send:** `/app/conversation/{conversationId}/send` — send message (body and headers as per frontend)

## JavaDoc

Generated API documentation for the backend source:

```bash
mvn javadoc:javadoc
```

Output: `target/site/apidocs/`. Open `target/site/apidocs/index.html` in a browser.

To attach JavaDoc to the built artifact (e.g. for IDE or distribution):

```bash
mvn clean package javadoc:jar
```

## Project Structure

```
backend/
├── README.md                 # This file
├── docs/
│   └── BACKEND.md            # Backend code explanation (architecture & flow)
├── pom.xml
└── src/main/
    ├── java/com/aiagent/chatsystem/
    │   ├── ChatSystemApplication.java
    │   ├── config/           # Security, WebSocket, MCP props, OpenAPI, etc.
    │   ├── controller/       # REST: auth, conversations, models, MCP
    │   ├── dto/              # Request/response DTOs
    │   ├── exception/        # Custom exceptions + global handler
    │   ├── model/            # JPA entities (User, Conversation, Message, ModelConfig, …)
    │   ├── repository/       # JPA repositories
    │   └── service/          # Business logic; impl/ for implementations
    └── resources/
        ├── application.yml
        └── application-dev.yml.example
```

For a **detailed explanation of the backend code** (layers, key classes, request flow, MCP integration), see **[docs/BACKEND.md](docs/BACKEND.md)**.

## Build & Run

| Command              | Description                    |
|----------------------|--------------------------------|
| `mvn clean install`  | Compile, run tests, package    |
| `mvn spring-boot:run`| Run the application           |
| `mvn test`           | Run tests                     |
| `mvn javadoc:javadoc`| Generate JavaDoc only        |

## License

MIT (or as per project root).
