# AI Agent Chat System

A scalable, extensible chat system with configurable AI models, real-time communication, and multi-client platform support.

## Tech Stack

### Backend
- **Java 21** with **Spring Boot 3.2**
- **Spring AI** for AI model integration (OpenAI, Anthropic, Ollama)
- **Spring WebSocket** (STOMP) for real-time communication
- **PostgreSQL** for data persistence
- **Redis** for caching and session management
- **Spring Security** with JWT authentication
- **Maven** for dependency management

### Frontend
- **Angular 17+** with TypeScript
- **SockJS + STOMP** for WebSocket communication
- **RxJS** for reactive programming
- **Angular Material** for UI components

## Architecture

See [ARCHITECTURE.md](./ARCHITECTURE.md) for detailed architecture documentation.

## Prerequisites

- Java 21 or higher
- Maven 3.6+
- Node.js 18+ and npm
- PostgreSQL 12+
- Redis (optional, for caching)

## Setup Instructions

### 1. Database Setup

Create a PostgreSQL database:

```sql
CREATE DATABASE ai_chat;
```

Update the database connection in `backend/src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ai_chat
    username: your_username
    password: your_password
```

### 2. Backend Setup

1. Navigate to the backend directory:
```bash
cd backend
```

2. Configure environment variables in `application.yml` or set them as environment variables:
   - `OPENAI_API_KEY` - Your OpenAI API key (optional)
   - `ANTHROPIC_API_KEY` - Your Anthropic API key (optional)
   - `JWT_SECRET` - Secret key for JWT tokens
   - `DATABASE_URL` - PostgreSQL connection string

3. Build and run the backend:
```bash
mvn clean install
mvn spring-boot:run
```

The backend will start on `http://localhost:8080`

### 3. Frontend Setup

1. Navigate to the frontend directory:
```bash
cd frontend
```

2. Install dependencies:
```bash
npm install
```

3. Start the development server:
```bash
npm start
```

The frontend will start on `http://localhost:4200`

## API Endpoints

### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login user
- `POST /api/auth/refresh` - Refresh JWT token

### Conversations
- `GET /api/conversations` - List user conversations
- `POST /api/conversations` - Create new conversation
- `GET /api/conversations/:id` - Get conversation details
- `GET /api/conversations/:id/messages` - Get conversation messages
- `POST /api/conversations/:id/messages` - Send message

### Model Configuration
- `GET /api/models` - List available model configs
- `POST /api/models` - Create model config
- `PATCH /api/models/:id` - Update model config
- `DELETE /api/models/:id` - Delete model config

## WebSocket Endpoints

- Connect: `ws://localhost:8080/ws`
- Subscribe to conversation: `/topic/conversation/{conversationId}`
- Send message: `/app/conversation/{conversationId}/send`

## Configuration

### AI Model Configuration

Model configurations are stored in the database and can be managed via the API. Example configuration:

```json
{
  "name": "GPT-4 Default",
  "provider": "openai",
  "model": "gpt-4",
  "parameters": {
    "temperature": 0.7,
    "maxTokens": 2000,
    "topP": 1.0
  },
  "isDefault": true,
  "isActive": true
}
```

### Supported Providers

1. **OpenAI**: GPT-4, GPT-3.5-turbo, etc.
2. **Anthropic**: Claude 3 Opus, Claude 3 Sonnet, etc.
3. **Ollama**: Local models (llama2, mistral, etc.)

## Development

### Backend Development

- Run tests: `mvn test`
- Build JAR: `mvn clean package`
- Run JAR: `java -jar target/chat-system-1.0.0.jar`

### Frontend Development

- Run development server: `npm start`
- Build for production: `npm run build`
- Run tests: `npm test`

## Project Structure

```
ai-agent/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/aiagent/chatsystem/
│   │   │   │   ├── config/          # Configuration classes
│   │   │   │   ├── controller/      # REST controllers
│   │   │   │   ├── dto/             # Data Transfer Objects
│   │   │   │   ├── model/           # JPA entities
│   │   │   │   ├── repository/      # JPA repositories
│   │   │   │   └── service/         # Business logic
│   │   │   └── resources/
│   │   │       └── application.yml  # Configuration
│   │   └── test/                    # Tests
│   └── pom.xml                      # Maven dependencies
├── frontend/
│   ├── src/
│   │   ├── app/
│   │   │   ├── components/          # Angular components
│   │   │   ├── models/              # TypeScript models
│   │   │   └── services/             # Angular services
│   │   └── main.ts
│   └── package.json
└── ARCHITECTURE.md                   # Architecture documentation
```

## Best Practices

### Backend

1. **Service Layer**: Business logic should be in service classes, not controllers
2. **DTOs**: Use DTOs for API requests/responses, not entities directly
3. **Exception Handling**: Implement global exception handlers
4. **Validation**: Use Bean Validation annotations
5. **Security**: Always validate user permissions in service layer

### Frontend

1. **Services**: Use services for API calls and state management
2. **Observables**: Use RxJS observables for async operations
3. **Components**: Keep components focused and reusable
4. **Error Handling**: Implement proper error handling in services
5. **Type Safety**: Use TypeScript interfaces for all data models

## Extensibility

The system is designed for easy extension:

1. **New AI Providers**: Implement `AIModelService` interface
2. **Conversation Memory**: Add memory service and integrate with vector DB
3. **Document Retrieval**: Add document processing and embedding services
4. **Analytics**: Add analytics service and integrate with monitoring tools
5. **MCP Integration**: Add MCP client service for protocol support

## Security Considerations

- JWT tokens for authentication
- Password hashing with BCrypt
- CORS configuration
- Input validation
- SQL injection prevention (JPA)
- XSS prevention (Angular sanitization)

## Troubleshooting

### Backend won't start
- Check PostgreSQL is running
- Verify database credentials
- Check port 8080 is available

### Frontend can't connect
- Verify backend is running on port 8080
- Check CORS configuration
- Verify WebSocket endpoint is accessible

### AI models not working
- Verify API keys are set correctly
- Check model configuration in database
- Review Spring AI logs for errors

## License

MIT License

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.
