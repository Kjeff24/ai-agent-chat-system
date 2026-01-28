# Implementation Notes

## Key Design Decisions

### Backend (Spring Boot)

1. **Spring AI Integration**: Using Spring AI's unified abstraction allows easy switching between providers (OpenAI, Anthropic, Ollama) without changing business logic.

2. **JPA Entities**: Using JPA with PostgreSQL for persistence. JSON columns for flexible metadata storage.

3. **WebSocket with STOMP**: Using Spring WebSocket with STOMP protocol for real-time communication. This provides better message routing and broker support.

4. **JWT Authentication**: Stateless authentication using JWT tokens. Filter-based authentication for seamless integration.

5. **Service Layer Pattern**: Business logic separated into service classes, keeping controllers thin.

### Frontend (Angular)

1. **Service-Based Architecture**: Centralized state management through services using RxJS observables.

2. **WebSocket Service**: Dedicated service for WebSocket connections using SockJS and STOMP.js.

3. **Component Structure**: Modular components (chat-window, conversation-list) for reusability.

4. **Reactive Programming**: Using RxJS for handling async operations and state updates.

## Code Examples

### Adding a New AI Provider

To add a new AI provider (e.g., Google Gemini):

1. Add dependency to `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-google-gemini-spring-boot-starter</artifactId>
    <version>${spring-ai.version}</version>
</dependency>
```

2. Update `AIModelServiceImpl`:
```java
private final GoogleGeminiChatClient geminiChatClient;

// In constructor
this.geminiChatClient = geminiChatClient;

// In getChatClient method
case gemini -> geminiChatClient;
```

3. Add provider enum value in `ModelConfig.ModelProvider`.

### Adding Conversation Memory

1. Create a memory service:
```java
@Service
public class ConversationMemoryService {
    public void saveSummary(UUID conversationId, String summary) {
        // Save to database or vector store
    }
    
    public String getContext(UUID conversationId) {
        // Retrieve relevant context
    }
}
```

2. Integrate into chat flow:
```java
// Before generating response
String context = memoryService.getContext(conversationId);
messages.add(0, new SystemMessage(context));

// After response
memoryService.saveSummary(conversationId, summary);
```

### Adding Document Retrieval (RAG)

1. Add embedding service:
```java
@Service
public class EmbeddingService {
    public List<Double> generateEmbedding(String text) {
        // Use Spring AI embedding client
    }
}

@Service
public class DocumentService {
    public List<Document> searchSimilar(String query, int limit) {
        // Vector similarity search
    }
}
```

2. Integrate into chat:
```java
// Before generating response
List<Document> relevantDocs = documentService.searchSimilar(userMessage, 5);
String context = buildContextFromDocuments(relevantDocs);
messages.add(0, new SystemMessage(context));
```

## Testing

### Backend Testing

```java
@SpringBootTest
@AutoConfigureMockMvc
class ConversationControllerTest {
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void testCreateConversation() {
        // Test implementation
    }
}
```

### Frontend Testing

```typescript
describe('ChatService', () => {
  let service: ChatService;
  
  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ChatService);
  });
  
  it('should load conversations', () => {
    // Test implementation
  });
});
```

## Performance Considerations

1. **Database Indexing**: Ensure indexes on frequently queried fields (userId, conversationId, createdAt).

2. **Caching**: Use Redis for caching frequently accessed model configs and user sessions.

3. **Connection Pooling**: Configure HikariCP connection pool in `application.yml`.

4. **Async Processing**: Consider using `@Async` for non-blocking AI responses.

5. **Pagination**: Implement pagination for message history in long conversations.

## Security Best Practices

1. **Input Validation**: Always validate user input using Bean Validation.

2. **SQL Injection**: JPA prevents SQL injection, but be careful with native queries.

3. **XSS Prevention**: Angular automatically sanitizes HTML, but be cautious with user-generated content.

4. **Rate Limiting**: Implement rate limiting for API endpoints (consider Spring Cloud Gateway).

5. **API Key Security**: Store API keys in environment variables, never in code.

## Deployment

### Docker

Create `Dockerfile` for backend:
```dockerfile
FROM openjdk:17-jdk-slim
COPY target/chat-system-1.0.0.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Create `docker-compose.yml`:
```yaml
version: '3.8'
services:
  backend:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      - DATABASE_URL=jdbc:postgresql://db:5432/ai_chat
    depends_on:
      - db
  
  db:
    image: postgres:15
    environment:
      - POSTGRES_DB=ai_chat
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=postgres
```

### Environment Variables

Always use environment variables for sensitive configuration:
- API keys
- Database credentials
- JWT secrets
- External service URLs

## Monitoring and Logging

1. **Logging**: Use SLF4J with Logback. Configure log levels in `application.yml`.

2. **Health Checks**: Spring Boot Actuator provides health endpoints.

3. **Metrics**: Consider integrating with Prometheus for metrics collection.

4. **Error Tracking**: Integrate with Sentry or similar for error tracking.

## Future Enhancements

1. **Multi-tenancy**: Add organization/workspace support.

2. **Analytics Dashboard**: Track usage, costs, and performance metrics.

3. **Plugin System**: Allow custom AI providers via plugins.

4. **Export/Import**: Allow users to export conversations.

5. **Search**: Full-text search across conversations and messages.
