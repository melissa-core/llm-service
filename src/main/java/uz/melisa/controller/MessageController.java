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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import uz.melisa.dto.ResponseMessageDTO;
import uz.melisa.dto.common.CommonResponse;
import uz.melisa.dto.message.MessageResponseDTO;
import uz.melisa.dto.message.MessageSendRequestDTO;
import uz.melisa.service.MessageService;
import uz.melisa.util.ResponseUtil;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/message")
@Tag(
        name = "Chat - Messages",
        description = "Send chat messages to the AI assistant and delete previously stored messages. "
                + "All endpoints require an authenticated user (valid JWT); no specific permission code is enforced."
)
public class MessageController {

    private final MessageService messageService;

    @Operation(
            summary = "Send chat message",
            description = "Persists the user message, runs it through the AI assistant and returns the model reply "
                    + "(general text or a product-based suggestion). The message is bound to the authenticated user; "
                    + "requires a valid JWT (no specific permission code)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Model reply generated and returned"),
            @ApiResponse(responseCode = "400", description = "Request body validation failed"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated (missing or invalid JWT)"),
            @ApiResponse(responseCode = "404", description = "Referenced chat or related entity not found"),
            @ApiResponse(responseCode = "500", description = "Unexpected error while generating the reply")
    })
    @PostMapping("/send")
    public ResponseEntity<CommonResponse<MessageResponseDTO>> sendMessage(
            @RequestBody @Valid MessageSendRequestDTO messageSendRequestDTO
    ) {
        log.info("REST request to send message : {}", messageSendRequestDTO);
        return ResponseUtil.buildResponseDTO(messageService.sendMessage(messageSendRequestDTO));
    }

    @Operation(
            summary = "Stream chat message reply",
            description = "Internal API. Server-Sent Events variant of /send that streams the AI reply as 'delta' "
                    + "chunks, a final 'done' event, or an in-stream 'error' event. Requires a valid JWT. "
                    + "Hidden from the public OpenAPI document."
    )
    @Hidden
    @PostMapping(value = "/send-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> sendMessageStream(
            @RequestBody @Valid MessageSendRequestDTO messageSendRequestDTO
    ) {
        log.info("REST request to stream message, chatId={}", messageSendRequestDTO.getChatId());
        return messageService.sendMessageStream(messageSendRequestDTO);
    }

    @Operation(
            summary = "Delete chat message",
            description = "Soft-deletes a message owned by the authenticated user. Only the owner's own, "
                    + "not-yet-deleted message can be removed. Requires a valid JWT (no specific permission code)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Message deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated (missing or invalid JWT)"),
            @ApiResponse(responseCode = "404", description = "Message not found for the current user"),
            @ApiResponse(responseCode = "500", description = "Unexpected error while deleting the message")
    })
    @PostMapping("/delete/{id}")
    public ResponseEntity<CommonResponse<ResponseMessageDTO>> deleteMessage(
            @Parameter(description = "Id of the message to delete", required = true, example = "42")
            @PathVariable("id") long id
    ) {
        log.info("REST request to delete message : {}", id);
        return ResponseUtil.buildResponseDTO(messageService.deleteMessage(id));
    }
}
