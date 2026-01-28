# Chat System Architecture Plan

## Overview
A scalable, extensible chat system with configurable AI models, real-time communication, and multi-client platform support.

## Tech Stack

### Backend
- **Runtime**: Java 21
- **Framework**: Spring Boot 3.x
- **AI Integration**: Spring AI (OpenAI, Anthropic, Ollama support)
- **Real-time**: Spring WebSocket (STOMP protocol)
- **Database**: PostgreSQL (primary), Redis (caching/sessions)
- **ORM**: Spring Data JPA (Hibernate)
- **Validation**: Bean Validation (Jakarta Validation)
- **Authentication**: Spring Security with JWT tokens
- **Build Tool**: Maven or Gradle
- **AI Providers Supported**:
  - OpenAI (GPT-4, GPT-3.5) via Spring AI
  - Anthropic (Claude) via Spring AI
  - Ollama (local models) via Spring AI
  - Custom providers via Spring AI abstraction

### Frontend
- **Framework**: Angular 17+ with TypeScript
- **State Management**: Angular Services + RxJS
- **Real-time**: WebSocket (SockJS + STOMP)
- **UI Library**: Angular Material or PrimeNG
- **Build Tool**: Angular CLI
- **HTTP Client**: Angular HttpClient with RxJS

### Infrastructure
- **Containerization**: Docker & Docker Compose
- **Message Queue**: Redis (for future async processing)
- **Monitoring**: Winston for logging

## System Architecture

```
┌─────────────────┐
│   Web Client    │
│  (Angular App)  │
└────────┬────────┘
         │ HTTP/WebSocket (STOMP)
         │
┌────────▼─────────────────────────────────┐
│      Spring Boot Application             │
│  ┌──────────────┐  ┌──────────────────┐ │
│  │ REST API     │  │ WebSocket Server │ │
│  │ (Spring MVC) │  │  (STOMP)         │ │
│  └──────┬───────┘  └────────┬─────────┘ │
│         │                   │            │
│  ┌──────▼───────────────────▼──────────┐ │
│  │  Chat Service                      │ │
│  │  - Conversation Management         │ │
│  │  - Message History                 │ │
│  └──────┬─────────────────────────────┘ │
│         │                                │
│  ┌──────▼──────────────────────────────┐ │
│  │  Spring AI Integration              │ │
│  │  - Model Manager                    │ │
│  │  - Provider Abstraction             │ │
│  │  - Configuration Management         │ │
│  └──────┬──────────────────────────────┘ │
└─────────┼────────────────────────────────┘
          │
┌─────────▼───────────────────▼──────────────┐
│         Data Layer                          │
│  ┌──────────────┐  ┌──────────────────┐   │
│  │ PostgreSQL   │  │      Redis       │   │
│  │ (JPA/Hibernate)│  │  (Cache/Queue)   │   │
│  └──────────────┘  └──────────────────┘   │
└────────────────────────────────────────────┘
          │
┌─────────▼──────────────────────────────────┐
│      External Integrations                 │
│  ┌──────────────┐  ┌──────────────────┐   │
│  │ MCP Clients  │  │  AI Providers    │   │
│  │ (Protocol)   │  │  (Spring AI)     │   │
│  └──────────────┘  └──────────────────┘   │
└────────────────────────────────────────────┘
```

## Database Schema

### Core Tables

#### Users
- `id` (UUID, PK)
- `email` (String, unique)
- `name` (String)
- `createdAt` (DateTime)
- `updatedAt` (DateTime)

#### Conversations
- `id` (UUID, PK)
- `userId` (UUID, FK → Users)
- `title` (String, nullable)
- `modelConfigId` (UUID, FK → ModelConfigs)
- `metadata` (JSON)
- `createdAt` (DateTime)
- `updatedAt` (DateTime)

#### Messages
- `id` (UUID, PK)
- `conversationId` (UUID, FK → Conversations)
- `role` (Enum: user, assistant, system)
- `content` (Text)
- `metadata` (JSON)
- `createdAt` (DateTime)

#### ModelConfigs
- `id` (UUID, PK)
- `name` (String, unique)
- `provider` (Enum: openai, anthropic, custom)
- `model` (String) - e.g., "gpt-4", "claude-3-opus"
- `parameters` (JSON) - temperature, max_tokens, etc.
- `isDefault` (Boolean)
- `isActive` (Boolean)
- `createdAt` (DateTime)
- `updatedAt` (DateTime)

#### ConversationMemory (Future)
- `id` (UUID, PK)
- `conversationId` (UUID, FK)
- `type` (Enum: summary, embedding, context)
- `data` (JSON)
- `createdAt` (DateTime)

## API Structure

### REST Endpoints

#### Authentication
- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - User login
- `POST /api/auth/refresh` - Refresh JWT token

#### Conversations
- `GET /api/conversations` - List user conversations
- `POST /api/conversations` - Create new conversation
- `GET /api/conversations/:id` - Get conversation details
- `PATCH /api/conversations/:id` - Update conversation
- `DELETE /api/conversations/:id` - Delete conversation

#### Messages
- `GET /api/conversations/:id/messages` - Get conversation messages
- `POST /api/conversations/:id/messages` - Send message (async)

#### Model Configuration
- `GET /api/models` - List available model configs
- `POST /api/models` - Create model config
- `PATCH /api/models/:id` - Update model config
- `DELETE /api/models/:id` - Delete model config
- `GET /api/models/:id` - Get model config details

#### MCP Integration
- `POST /api/mcp/connect` - Connect MCP client
- `GET /api/mcp/status` - Get MCP connection status
- `POST /api/mcp/execute` - Execute MCP command

### WebSocket Events

#### Client → Server
- `join:conversation` - Join conversation room
- `send:message` - Send chat message
- `typing:start` - User started typing
- `typing:stop` - User stopped typing

#### Server → Client
- `message:new` - New message received
- `message:stream` - Streaming AI response chunk
- `message:complete` - AI response complete
- `typing:update` - Typing indicator update
- `error` - Error occurred

## AI Model Integration Approach

### Spring AI Provider Abstraction
Spring AI provides a unified abstraction for AI models. We'll use:
- `ChatClient` interface for chat interactions
- `ChatModel` for model-specific implementations
- `ChatOptions` for configuration (temperature, maxTokens, etc.)
- `Prompt` and `ChatResponse` for request/response handling

```java
public interface AIModelService {
    Flux<String> generateStream(List<Message> messages, ModelConfig config);
    Mono<String> generate(List<Message> messages, ModelConfig config);
    boolean validateConfig(ModelConfig config);
}
```

### Configuration Management
- Model configs stored in database
- Runtime switching without restart
- Parameter validation
- Fallback to default model on error
- Rate limiting per model/provider

## Frontend Architecture

### Angular Component Structure
```
src/
├── app/
│   ├── components/
│   │   ├── chat/
│   │   │   ├── chat-window/
│   │   │   │   ├── chat-window.component.ts
│   │   │   │   ├── chat-window.component.html
│   │   │   │   └── chat-window.component.css
│   │   │   ├── message-list/
│   │   │   ├── message-input/
│   │   │   └── typing-indicator/
│   │   ├── conversation/
│   │   │   ├── conversation-list/
│   │   │   └── conversation-item/
│   │   └── settings/
│   │       └── model-selector/
│   ├── services/
│   │   ├── chat.service.ts
│   │   ├── conversation.service.ts
│   │   ├── websocket.service.ts
│   │   ├── model-config.service.ts
│   │   └── auth.service.ts
│   ├── models/
│   │   ├── message.model.ts
│   │   ├── conversation.model.ts
│   │   └── model-config.model.ts
│   ├── guards/
│   │   └── auth.guard.ts
│   └── interceptors/
│       └── auth.interceptor.ts
```

## Extensibility Points

### 1. Conversation Memory
- Plugin interface for memory providers
- Support for vector databases (Pinecone, Weaviate)
- Automatic summarization
- Context window management

### 2. Document Retrieval (RAG)
- Document upload and processing
- Embedding generation
- Semantic search integration
- Context injection into prompts

### 3. Analytics
- Message analytics
- Model performance metrics
- User behavior tracking
- Cost tracking per model/provider

### 4. Multi-tenancy
- Organization/workspace support
- Role-based access control
- Resource quotas

## Security Considerations

- JWT authentication with refresh tokens
- Rate limiting (per user, per IP)
- Input sanitization
- CORS configuration
- API key encryption for model providers
- WebSocket authentication

## Scalability Considerations

- Horizontal scaling with Redis pub/sub
- Database connection pooling
- Caching frequently accessed data
- Async message processing
- Load balancing for WebSocket connections
- CDN for static assets

## Deployment

- Docker containers for all services
- Environment-based configuration
- Health check endpoints
- Graceful shutdown handling
- Logging and monitoring setup
