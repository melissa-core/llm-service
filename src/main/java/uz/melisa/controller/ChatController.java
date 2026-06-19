package uz.melisa.controller;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.melisa.dto.ResponseMessageDTO;
import uz.melisa.dto.chat.ChatDTO;
import uz.melisa.dto.chat.ChatMessagesDTO;
import uz.melisa.dto.chat.ChatPageDTO;
import uz.melisa.dto.chat.CreateChatRequestDTO;
import uz.melisa.dto.common.CommonResponse;
import uz.melisa.service.ChatService;
import uz.melisa.util.ResponseUtil;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/chat")
@Tag(name = "Chat - Conversations", description = "Authenticated user chat conversations: create, rename, list, fetch, read messages and delete. Requires an authenticated user (the current user id is resolved from the security context).")
public class ChatController {

    private final ChatService chatService;

    @Operation(
            summary = "Create chat",
            description = "Creates a new chat for the current authenticated user. The chat is owned by the user resolved from the security context. Requires authentication."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chat created"),
            @ApiResponse(responseCode = "400", description = "Invalid request body (validation failure)"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated or invalid session")
    })
    @PostMapping
    public ResponseEntity<CommonResponse<ResponseMessageDTO>> createChat(
            @RequestBody @Valid CreateChatRequestDTO createChatRequestDTO
    ) {
        log.info("REST request to create chat : {}", createChatRequestDTO);
        return ResponseUtil.buildResponseDTO(chatService.createChat(createChatRequestDTO));
    }

    @Operation(
            summary = "Update chat",
            description = "Renames an existing chat owned by the current authenticated user. Only chats owned by the current user (resolved from the security context) and not deleted can be updated. Requires authentication."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chat updated"),
            @ApiResponse(responseCode = "400", description = "Invalid request body (validation failure)"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated or invalid session"),
            @ApiResponse(responseCode = "404", description = "Chat not found for the current user")
    })
    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<ResponseMessageDTO>> updateChat(
            @Parameter(description = "Id of the chat to update", required = true, example = "10")
            @PathVariable("id") Long id,
            @RequestBody @Valid CreateChatRequestDTO createChatRequestDTO
    ) {
        log.info("REST request to update chat : {}, {}", createChatRequestDTO, id);
        return ResponseUtil.buildResponseDTO(chatService.updateChat(id, createChatRequestDTO));
    }

    @Operation(
            summary = "List chats",
            description = "Returns a page of the current authenticated user's chats, ordered by last update (newest first by default). Only the current user's chats are returned. Requires authentication."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of chats returned"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated or invalid session")
    })
    @GetMapping
    public ResponseEntity<CommonResponse<Page<ChatPageDTO>>> getChatPages(
            @PageableDefault(sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        log.info("REST request to get chat pages");
        return ResponseUtil.buildResponseDTO(chatService.getChatPages(pageable));
    }

    @Operation(
            summary = "Get chat by id",
            description = "Returns a single chat owned by the current authenticated user. Only a non-deleted chat owned by the current user (resolved from the security context) is returned. Requires authentication."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chat returned"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated or invalid session"),
            @ApiResponse(responseCode = "404", description = "Chat not found for the current user")
    })
    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<ChatDTO>> getChatById(
            @Parameter(description = "Id of the chat to fetch", required = true, example = "10")
            @PathVariable("id") Long id) {
        log.info("REST request to get chat : {}", id);
        return ResponseUtil.buildResponseDTO(chatService.getChatById(id));
    }

    @Operation(
            summary = "List chat messages",
            description = "Returns a page of messages for a chat owned by the current authenticated user, ordered by creation time (newest first by default). The chat must be a non-deleted chat owned by the current user. Requires authentication."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of chat messages returned"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated or invalid session"),
            @ApiResponse(responseCode = "404", description = "Chat not found for the current user")
    })
    @GetMapping("/{id}/messages")
    public ResponseEntity<CommonResponse<Page<ChatMessagesDTO>>> getChatMessages(
            @Parameter(description = "Id of the chat whose messages are listed", required = true, example = "10")
            @PathVariable("id") Long id,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        log.info("REST request to get chat messages");
        return ResponseUtil.buildResponseDTO(chatService.getChatMessages(id, pageable));
    }

    @Operation(
            summary = "Delete chat",
            description = "Soft-deletes a chat owned by the current authenticated user. Only a non-deleted chat owned by the current user (resolved from the security context) can be deleted. Requires authentication."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chat deleted"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated or invalid session"),
            @ApiResponse(responseCode = "404", description = "Chat not found for the current user")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<CommonResponse<ResponseMessageDTO>> deleteChat(
            @Parameter(description = "Id of the chat to delete", required = true, example = "10")
            @PathVariable("id") Long id) {
        log.info("REST request to delete chat : {}", id);
        return ResponseUtil.buildResponseDTO(chatService.deleteChat(id));
    }

    @Operation(
            summary = "Activate temporary chat (internal)",
            description = "Internal API. Claims a temporary (device-bound) chat for the current authenticated user by its device key, reassigning the chat and its messages to the user. Hidden from Swagger UI. Returns a success message even when no temporary chat matches the key. Requires authentication."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Temporary chat activated, or no matching temporary chat found"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated or invalid session")
    })
    @Hidden
    @PostMapping("/activate-chat/{key}")
    public ResponseEntity<CommonResponse<ResponseMessageDTO>> activateChat(
            @Parameter(description = "Device key of the temporary chat to activate", required = true, example = "device-abc-123")
            @PathVariable("key") String key
    ) {
        log.info("REST request to activate chat : {}", key);
        return ResponseUtil.buildResponseDTO(chatService.activateChat(key));
    }
}
