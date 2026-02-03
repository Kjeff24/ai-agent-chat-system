package com.aiagent.chatsystem.controller;

import com.aiagent.chatsystem.config.AppMcpOAuthProperties;
import com.aiagent.chatsystem.dto.*;
import com.aiagent.chatsystem.exception.McpOAuthProviderNotFoundException;
import com.aiagent.chatsystem.service.McpOAuthProviderRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/mcp/oauth/providers")
@Tag(name = "MCP OAuth providers", description = "List and add/update/remove OAuth providers for MCP servers. Static providers (from yaml) cannot be removed.")
public class McpOAuthProviderController {

    private final McpOAuthProviderRegistry registry;

    public McpOAuthProviderController(McpOAuthProviderRegistry registry) {
        this.registry = registry;
    }

    @GetMapping
    @Operation(summary = "List OAuth providers", description = "List all OAuth providers (static + dynamic) with masked client id and source.")
    @ApiResponse(responseCode = "200", description = "Success", content = @Content(array = @ArraySchema(schema = @Schema(implementation = OAuthProviderSummaryDTO.class))))
    public List<OAuthProviderSummaryDTO> listProviders() {
        Map<String, AppMcpOAuthProperties.OAuthProviderConfig> effective = registry.getEffectiveProviders();
        return effective.entrySet().stream()
                .map(e -> toSummary(e.getKey(), e.getValue(), registry.getSource(e.getKey())))
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get OAuth provider", description = "Get provider detail by id (for edit). Does not return client secret.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success", content = @Content(schema = @Schema(implementation = OAuthProviderDetailDTO.class))),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    public OAuthProviderDetailDTO getProvider(
            @Parameter(description = "Provider id") @PathVariable String id) {
        AppMcpOAuthProperties.OAuthProviderConfig config = registry.getProvider(id);
        if (config == null) {
            throw new McpOAuthProviderNotFoundException("OAuth provider not found: " + id);
        }
        return toDetail(id, config, registry.getSource(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add OAuth provider", description = "Add a dynamic OAuth provider. Overwrites if id exists.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public void registerProvider(@Valid @RequestBody RegisterOAuthProviderRequest request) {
        AppMcpOAuthProperties.OAuthProviderConfig config = new AppMcpOAuthProperties.OAuthProviderConfig();
        config.setAuthorizationUri(request.getAuthorizationUri());
        config.setTokenUri(request.getTokenUri());
        config.setClientId(request.getClientId());
        config.setClientSecret(request.getClientSecret());
        config.setScopes(request.getScopes());
        registry.addProvider(request.getId().trim(), config);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update OAuth provider", description = "Update a dynamic OAuth provider. Only provided fields are updated. Fails if provider is static.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "404", description = "Not found or not dynamic")
    })
    public void updateProvider(
            @Parameter(description = "Provider id") @PathVariable String id,
            @Valid @RequestBody UpdateOAuthProviderRequest request) {
        if (!"dynamic".equals(registry.getSource(id))) {
            throw new McpOAuthProviderNotFoundException("OAuth provider is static and cannot be updated: " + id);
        }
        AppMcpOAuthProperties.OAuthProviderConfig existing = registry.getProvider(id);
        if (existing == null) {
            throw new McpOAuthProviderNotFoundException("OAuth provider not found: " + id);
        }
        AppMcpOAuthProperties.OAuthProviderConfig config = new AppMcpOAuthProperties.OAuthProviderConfig();
        config.setAuthorizationUri(request.getAuthorizationUri() != null ? request.getAuthorizationUri() : existing.getAuthorizationUri());
        config.setTokenUri(request.getTokenUri() != null ? request.getTokenUri() : existing.getTokenUri());
        config.setClientId(request.getClientId() != null ? request.getClientId() : existing.getClientId());
        config.setClientSecret(request.getClientSecret() != null ? request.getClientSecret() : existing.getClientSecret());
        config.setScopes(request.getScopes() != null ? request.getScopes() : existing.getScopes());
        registry.updateProvider(id, config);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove OAuth provider", description = "Remove a dynamic OAuth provider. Fails if provider is static.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Removed"),
            @ApiResponse(responseCode = "404", description = "Not found or not dynamic")
    })
    public void removeProvider(@Parameter(description = "Provider id") @PathVariable String id) {
        if (!registry.removeProvider(id)) {
            throw new McpOAuthProviderNotFoundException("OAuth provider not found or is static (cannot remove): " + id);
        }
    }

    private static String maskClientId(String clientId) {
        if (clientId == null || clientId.length() <= 4) {
            return clientId != null && !clientId.isEmpty() ? "****" : null;
        }
        return "****" + clientId.substring(clientId.length() - 4);
    }

    private static OAuthProviderSummaryDTO toSummary(String id, AppMcpOAuthProperties.OAuthProviderConfig config, String source) {
        OAuthProviderSummaryDTO dto = new OAuthProviderSummaryDTO();
        dto.setId(id);
        dto.setAuthorizationUri(config.getAuthorizationUri());
        dto.setTokenUri(config.getTokenUri());
        dto.setClientIdMasked(maskClientId(config.getClientId()));
        dto.setScopes(config.getScopes());
        dto.setSource(source);
        return dto;
    }

    private static OAuthProviderDetailDTO toDetail(String id, AppMcpOAuthProperties.OAuthProviderConfig config, String source) {
        OAuthProviderDetailDTO dto = new OAuthProviderDetailDTO();
        dto.setId(id);
        dto.setAuthorizationUri(config.getAuthorizationUri());
        dto.setTokenUri(config.getTokenUri());
        dto.setClientId(config.getClientId());
        dto.setScopes(config.getScopes());
        dto.setSource(source);
        return dto;
    }
}
