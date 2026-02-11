# Request: Option to skip GET/SSE stream (batch-only / request-response mode)

## Summary

When using `HttpClientStreamableHttpTransport` to connect to MCP servers that **only support POST** (e.g. [GitHub Copilot MCP](https://api.githubcopilot.com/mcp/)), the client sends a **GET** request after the first successful POST (via `reconnect()`). The server correctly returns **405 Method Not Allowed**. The SDK is designed to handle this and fall back to request-response mode, but the **body subscriber** (`ResponseSubscribers.sseToBodySubscriber`) receives the 405 response body and throws `McpTransportException: Invalid SSE response. Status code: 405 Line: Method Not Allowed` before the status-code handling in `reconnect()` can run. This results in noisy logs and potential session disruption even though tool calls over POST continue to work.

## Environment

- **SDK:** `io.modelcontextprotocol.sdk:mcp` (BOM 0.17.2)
- **Transport:** `HttpClientStreamableHttpTransport.builder(serverUrl).build()`
- **Server:** Streamable HTTP endpoint that only allows POST (e.g. `https://api.githubcopilot.com/mcp/`)

## Current behavior

1. First **POST** (e.g. initialize) succeeds; session ID is set.
2. Transport calls **`reconnect(null)`** to open a GET stream for server→client SSE.
3. Server returns **405 Method Not Allowed** (no GET support).
4. The GET response is passed to **`ResponseSubscribers.sseToBodySubscriber()`**, which expects SSE. The first "line" is the status/body (e.g. "Method Not Allowed"), so it throws:
   ```text
   io.modelcontextprotocol.spec.McpTransportException: Invalid SSE response. Status code: 405 Line: Method Not Allowed
       at io.modelcontextprotocol.client.transport.ResponseSubscribers$SseLineSubscriber.hookOnNext(...)
   ```
5. This is logged by **`LifecycleInitializer`** as "Handling exception". The 405 handling in `reconnect()` (`statusCode == METHOD_NOT_ALLOWED` → `Flux.empty()`) never runs because the exception occurs in the body subscriber first.
6. Tool calls over **POST** still work; the connection is established. So the 405 is effectively "handled" at the transport level, but the exception path is taken and logs are noisy.

## Desired behavior

- **Option A (preferred):** A **builder option** to disable the GET/SSE stream entirely (e.g. `openSseStreamAfterConnect(false)` or `requestResponseOnly(true)`). When set, the transport would not call `reconnect()` after the first POST, so no GET is sent. This suits servers that only support POST (batch/request-response only).
- **Option B:** When the server returns 405 for the GET, **check the response status before** delegating to `sseToBodySubscriber`, and return `Flux.empty()` (or equivalent) without parsing the body as SSE, so no exception is thrown and "request-response mode" is entered cleanly.

## Relevance to spec

The [Streamable HTTP spec](https://modelcontextprotocol.io/specification/2025-03-26/basic/transports#streamable-http) allows both POST (messages) and GET (SSE stream). Some servers only implement POST. A client option to use POST-only (no GET) would align with such deployments without changing behavior for servers that do support GET.

## Workaround (current)

- Set `logging.level.io.modelcontextprotocol.client.LifecycleInitializer=ERROR` to suppress the 405 WARN and stack trace. Tool calls continue to work over POST.

Thank you for maintaining the Java SDK.
