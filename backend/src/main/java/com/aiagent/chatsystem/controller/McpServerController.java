package com.aiagent.chatsystem.controller;

import com.aiagent.chatsystem.dto.McpServerDetailDTO;
import com.aiagent.chatsystem.dto.McpServerSummaryDTO;
import com.aiagent.chatsystem.dto.RegisterMcpServerRequest;
import com.aiagent.chatsystem.dto.UpdateMcpServerRequest;
import com.aiagent.chatsystem.exception.McpServerNotFoundException;
import com.aiagent.chatsystem.service.McpClientService;
import com.aiagent.chatsystem.service.McpOAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/mcp/servers")
@Tag(name = "MCP servers", description = "List and dynamically register/remove MCP servers (Streamable HTTP). OAuth for supported servers. JWT required except callback.")
public class McpServerController {

    private final McpClientService mcpClientService;
    private final McpOAuthService mcpOAuthService;

    @Value("${cors.allowed-origins:http://localhost:4200}")
    private String frontendOrigin;

    public McpServerController(McpClientService mcpClientService, McpOAuthService mcpOAuthService) {
        this.mcpClientService = mcpClientService;
        this.mcpOAuthService = mcpOAuthService;
    }

    @GetMapping
    @Operation(summary = "List servers", description = "List all MCP servers (static + dynamic) with status for the current user (OAuth: connected when authorized)")
    @ApiResponse(responseCode = "200", description = "Success", content = @Content(array = @ArraySchema(schema = @Schema(implementation = McpServerSummaryDTO.class))))
    public List<McpServerSummaryDTO> listServers(Authentication authentication) {
        UUID userId = authentication != null ? UUID.fromString(authentication.getName()) : null;
        return mcpClientService.listServers(userId);
    }

    @GetMapping("/{name}")
    @Operation(summary = "Get server", description = "Get full details of an MCP server by name (for edit), with OAuth status for current user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success", content = @Content(schema = @Schema(implementation = McpServerDetailDTO.class))),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    public McpServerDetailDTO getServer(
            @Parameter(description = "Server name") @PathVariable String name,
            Authentication authentication) {
        UUID userId = authentication != null ? UUID.fromString(authentication.getName()) : null;
        McpServerDetailDTO server = mcpClientService.getServer(name, userId);
        if (server == null) {
            throw new McpServerNotFoundException("MCP server not found: " + name);
        }
        return server;
    }

    @GetMapping("/{name}/oauth/authorize-url")
    @Operation(summary = "Get OAuth authorize URL", description = "Returns the URL to redirect the user to for OAuth. SPA calls this with JWT, then sets window.location to the returned URL.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success", content = @Content(schema = @Schema(implementation = java.util.Map.class))),
            @ApiResponse(responseCode = "400", description = "Server not OAuth-enabled or provider config missing"),
            @ApiResponse(responseCode = "404", description = "Server not found")
    })
    public java.util.Map<String, String> getOAuthAuthorizeUrl(
            @Parameter(description = "Server name") @PathVariable String name,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        if (mcpClientService.getServer(name, userId) == null) {
            throw new McpServerNotFoundException("MCP server not found: " + name);
        }
        String providerId = mcpClientService.getOAuthProviderForServer(name);
        if (providerId == null || providerId.isBlank()) {
            throw new IllegalArgumentException("Server is not OAuth-enabled: " + name);
        }
        String url = mcpOAuthService.buildAuthorizeUrl(name, userId, providerId);
        if (url == null) {
            throw new IllegalArgumentException("OAuth provider config missing for: " + providerId);
        }
        return java.util.Map.of("authorizeUrl", url);
    }

    @GetMapping("/{name}/test")
    @Operation(summary = "Test MCP connection", description = "Calls the MCP server's initialize method with the current user's OAuth token. Returns the HTTP status and response body. Use to verify the server accepts the token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Test result (check body for MCP response or error)"),
            @ApiResponse(responseCode = "401", description = "Not authorized (no or expired OAuth token)"),
            @ApiResponse(responseCode = "404", description = "Server not found")
    })
    public Map<String, Object> testMcpConnection(
            @Parameter(description = "Server name (e.g. atlassian)") @PathVariable String name,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        McpServerDetailDTO server = mcpClientService.getServer(name, userId);
        if (server == null) {
            throw new McpServerNotFoundException("MCP server not found: " + name);
        }
        String token = mcpOAuthService.getValidAccessToken(userId, name);
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "No OAuth token for this server. Authorize first via " + name + " OAuth.");
        }
        String url = server.getUrl();
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("Server has no URL: " + name);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/json, text/event-stream");
        headers.setBearerAuth(token);
        String body = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}";
        RestTemplate rest = new RestTemplate();
        try {
            ResponseEntity<String> response = rest.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
            return Map.of(
                    "statusCode", response.getStatusCode().value(),
                    "body", response.getBody() != null ? response.getBody() : ""
            );
        } catch (HttpServerErrorException e) {
            int code = e.getStatusCode().value();
            String message = code == 524
                    ? "Timeout (Cloudflare): MCP origin did not respond in time. Retry or check Atlassian status."
                    : "MCP server error: " + e.getStatusCode() + (e.getResponseBodyAsString() != null && !e.getResponseBodyAsString().isBlank() ? " " + e.getResponseBodyAsString() : "");
            return Map.of(
                    "statusCode", code,
                    "body", message
            );
        }
    }

    @DeleteMapping("/{name}/oauth/token")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoke OAuth token", description = "Remove the stored OAuth token for this server and current user. Server will show as not authorized until user authorizes again.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Token revoked"),
            @ApiResponse(responseCode = "404", description = "Server not found or not OAuth-enabled")
    })
    public void revokeOAuthToken(
            @Parameter(description = "Server name") @PathVariable String name,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        if (mcpClientService.getServer(name, userId) == null) {
            throw new McpServerNotFoundException("MCP server not found: " + name);
        }
        String providerId = mcpClientService.getOAuthProviderForServer(name);
        if (providerId == null || providerId.isBlank()) {
            throw new McpServerNotFoundException("Server is not OAuth-enabled: " + name);
        }
        mcpOAuthService.revokeToken(userId, name);
    }

    @GetMapping("/oauth/callback")
    @Operation(summary = "OAuth callback", description = "Called by the OAuth provider after user authorizes. No JWT. Exchanges code for token and redirects to frontend.")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Redirect to frontend with query param mcp_oauth=success or failed"),
            @ApiResponse(responseCode = "400", description = "Invalid state or code")
    })
    public RedirectView oauthCallback(
            @RequestParam(name = "code", required = false) String code,
            @RequestParam(name = "state", required = false) String state,
            @RequestParam(name = "error", required = false) String error) {
        String redirect = frontendOrigin + "/settings";
        if (error != null || code == null || state == null) {
            return new RedirectView(redirect + "?mcp_oauth=failed");
        }
        UUID userId = mcpOAuthService.exchangeCodeAndStoreToken(code, state);
        if (userId == null) {
            return new RedirectView(redirect + "?mcp_oauth=failed");
        }
        return new RedirectView(redirect + "?mcp_oauth=success");
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register server", description = "Add an MCP server at runtime. Overwrites if name exists. Use Streamable HTTP URL. Optional: headers, requestTimeoutSeconds. Tools are discovered from the server at runtime.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registered"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public void registerServer(@Valid @RequestBody RegisterMcpServerRequest request) {
        mcpClientService.addServer(request);
    }

    @PutMapping("/{name}")
    @Operation(summary = "Update server", description = "Update a dynamically added MCP server. Only provided fields are updated. Fails if server is from static config.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "404", description = "Not found or not dynamic")
    })
    public void updateServer(
            @Parameter(description = "Server name") @PathVariable String name,
            @Valid @RequestBody UpdateMcpServerRequest request) {
        if (!mcpClientService.updateServer(name, request)) {
            throw new McpServerNotFoundException("Server not found or not dynamic: " + name);
        }
    }

    @DeleteMapping("/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove server", description = "Remove a dynamically added MCP server. Fails if server is from static config only.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Removed"),
            @ApiResponse(responseCode = "404", description = "Not found or not dynamic (cannot remove static config)")
    })
    public void removeServer(@Parameter(description = "Server name") @PathVariable String name) {
        if (!mcpClientService.removeServer(name)) {
            throw new McpServerNotFoundException("Server not found or not dynamic: " + name + ". Only dynamically added servers can be removed via API.");
        }
    }
}
