package uz.melisa.controller;

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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.melisa.dto.chat.ChatMessagesDTO;
import uz.melisa.dto.claude.GuestChatRequest;
import uz.melisa.dto.common.CommonResponse;
import uz.melisa.dto.guest.GuestMessageResponseDTO;
import uz.melisa.service.GuestService;
import uz.melisa.util.ResponseUtil;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/guest")
@Tag(name = "Mobile - Guest Chat", description = "Anonymous guest chat with Melissa, keyed by device id. Public endpoints - no authentication required.")
public class GuestChatController {

    private final GuestService guestService;

    @Operation(
            summary = "Send guest chat message",
            description = "Sends a free-chat message as an anonymous guest identified by the X-Device-Id header and returns Melissa's reply. "
                    + "Public endpoint - no token or permission required. Client: MOBILE."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Message processed, model reply returned"),
            @ApiResponse(responseCode = "400", description = "Missing X-Device-Id, blank/empty message, or request body validation failed"),
            @ApiResponse(responseCode = "500", description = "Unexpected error while generating the reply")
    })
    @PostMapping(value = "/free-chat", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CommonResponse<GuestMessageResponseDTO>> chat(
            @Parameter(description = "Anonymous device identifier used to scope the guest chat session", required = true, example = "9f8b2c1a-3d4e-4f56-8a90-1b2c3d4e5f60")
            @RequestHeader(value = "X-Device-Id") String deviceId,
            @Valid @RequestBody GuestChatRequest request) {
        log.info("REST request to free chat : {}", request);
        return ResponseUtil.buildResponseDTO(guestService.guestSendMessage(deviceId, request));
    }

    @Operation(
            summary = "Get guest chat messages",
            description = "Returns a paginated history of the guest chat for the device given in the X-Device-Id header; "
                    + "an empty page is returned when no chat exists for the device. "
                    + "Public endpoint - no token or permission required. Client: MOBILE."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated guest messages returned (empty page when no chat exists)"),
            @ApiResponse(responseCode = "400", description = "Missing X-Device-Id or invalid pagination parameters"),
            @ApiResponse(responseCode = "500", description = "Unexpected error while loading messages")
    })
    @GetMapping("/messages")
    public ResponseEntity<CommonResponse<Page<ChatMessagesDTO>>> getMessages(
            @Parameter(description = "Anonymous device identifier whose guest chat history is requested", required = true, example = "9f8b2c1a-3d4e-4f56-8a90-1b2c3d4e5f60")
            @RequestHeader(value = "X-Device-Id") String deviceId,
            @PageableDefault(sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        log.info("REST request to get messages : {}", deviceId);
        return ResponseUtil.buildResponseDTO(guestService.getMessages(deviceId, pageable));
    }
}
