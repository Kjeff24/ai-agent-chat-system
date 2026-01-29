package com.aiagent.chatsystem.controller;

import com.aiagent.chatsystem.dto.ConversationDTO;
import com.aiagent.chatsystem.dto.CreateConversationRequest;
import com.aiagent.chatsystem.dto.MessageDTO;
import com.aiagent.chatsystem.dto.SendMessageRequest;
import com.aiagent.chatsystem.dto.UpdateConversationRequest;
import com.aiagent.chatsystem.service.ConversationService;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/conversations")
@Tag(name = "Conversations", description = "Create and manage chat conversations and messages (JWT required)")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping
    @Operation(summary = "List conversations", description = "Get all conversations for the authenticated user")
    @ApiResponse(responseCode = "200", description = "Success", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ConversationDTO.class))))
    public List<ConversationDTO> getUserConversations(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return conversationService.getConversations(userId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get conversation", description = "Get a single conversation by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success", content = @Content(schema = @Schema(implementation = ConversationDTO.class))),
            @ApiResponse(responseCode = "404", description = "Conversation not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ConversationDTO getConversation(
            @Parameter(description = "Conversation UUID") @PathVariable UUID id,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return conversationService.getConversation(id, userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create conversation", description = "Create a new conversation. Body may be empty.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created", content = @Content(schema = @Schema(implementation = ConversationDTO.class))),
            @ApiResponse(responseCode = "503", description = "No default model config")
    })
    public ConversationDTO createConversation(
            @RequestBody(required = false) CreateConversationRequest request,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return conversationService.createConversation(request, userId);
    }

    @GetMapping("/{id}/messages")
    @Operation(summary = "List messages", description = "Get all messages in a conversation")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success", content = @Content(array = @ArraySchema(schema = @Schema(implementation = MessageDTO.class)))),
            @ApiResponse(responseCode = "404", description = "Conversation not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public List<MessageDTO> getMessages(
            @Parameter(description = "Conversation UUID") @PathVariable UUID id,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return conversationService.getMessages(id, userId);
    }

    @PostMapping("/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Send message", description = "Send a user message and receive the AI reply. Also broadcast via WebSocket.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "AI reply", content = @Content(schema = @Schema(implementation = MessageDTO.class))),
            @ApiResponse(responseCode = "404", description = "Conversation not found"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "503", description = "AI model error (e.g. Ollama model not found)")
    })
    public MessageDTO sendMessage(
            @Parameter(description = "Conversation UUID") @PathVariable UUID id,
            @Valid @RequestBody SendMessageRequest request,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return conversationService.sendMessage(id, request, userId);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update conversation", description = "Update conversation title or other fields")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success", content = @Content(schema = @Schema(implementation = ConversationDTO.class))),
            @ApiResponse(responseCode = "404", description = "Conversation not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ConversationDTO updateConversation(
            @Parameter(description = "Conversation UUID") @PathVariable UUID id,
            @RequestBody(required = false) UpdateConversationRequest request,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return conversationService.updateConversation(id, request, userId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete conversation", description = "Permanently delete a conversation and its messages")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "404", description = "Conversation not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public void deleteConversation(
            @Parameter(description = "Conversation UUID") @PathVariable UUID id,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        conversationService.deleteConversation(id, userId);
    }
}
