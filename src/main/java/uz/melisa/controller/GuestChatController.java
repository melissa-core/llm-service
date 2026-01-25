package uz.melisa.controller;

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
import uz.melisa.dto.claude.ClaudeChatRequest;
import uz.melisa.dto.common.CommonResponse;
import uz.melisa.dto.guest.GuestMessageResponseDTO;
import uz.melisa.service.GuestService;
import uz.melisa.util.ResponseUtil;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/guest")
public class GuestChatController {

    private final GuestService guestService;

    @PostMapping(value = "/free-chat", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CommonResponse<GuestMessageResponseDTO>> chat(
            @RequestHeader(value = "X-Device-Id") String deviceId,
            @Valid @RequestBody ClaudeChatRequest request) {
        log.info("REST request to free chat : {}", request);
        return ResponseUtil.buildResponseDTO(guestService.guestSendMessage(deviceId, request));
    }

    @GetMapping("/messages")
    public ResponseEntity<CommonResponse<Page<ChatMessagesDTO>>> getMessages(
            @RequestHeader(value = "X-Device-Id") String deviceId,
            @PageableDefault(sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        log.info("REST request to get messages : {}", deviceId);
        return ResponseUtil.buildResponseDTO(guestService.getMessages(deviceId, pageable));
    }
}
