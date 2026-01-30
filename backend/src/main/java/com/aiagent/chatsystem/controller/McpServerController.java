package com.aiagent.chatsystem.controller;

import com.aiagent.chatsystem.dto.McpServerSummaryDTO;
import com.aiagent.chatsystem.dto.RegisterMcpServerRequest;
import com.aiagent.chatsystem.exception.McpServerNotFoundException;
import com.aiagent.chatsystem.service.McpClientService;
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

@RestController
@RequestMapping("/api/mcp/servers")
@Tag(name = "MCP servers", description = "List and dynamically register/remove MCP servers (Streamable HTTP). JWT required.")
public class McpServerController {

    private final McpClientService mcpClientService;

    public McpServerController(McpClientService mcpClientService) {
        this.mcpClientService = mcpClientService;
    }

    @GetMapping
    @Operation(summary = "List servers", description = "List all MCP servers (static from config + dynamically added)")
    @ApiResponse(responseCode = "200", description = "Success", content = @Content(array = @ArraySchema(schema = @Schema(implementation = McpServerSummaryDTO.class))))
    public List<McpServerSummaryDTO> listServers() {
        return mcpClientService.listServers();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register server", description = "Add an MCP server at runtime. Overwrites if name exists. Use Streamable HTTP URL.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registered"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public void registerServer(@Valid @RequestBody RegisterMcpServerRequest request) {
        mcpClientService.addServer(request);
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
