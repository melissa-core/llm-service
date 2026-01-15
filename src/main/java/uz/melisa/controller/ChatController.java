package uz.melisa.controller;

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
import uz.melisa.dto.chat.CreateChatRequestDTO;
import uz.melisa.dto.common.CommonResponse;
import uz.melisa.service.ChatService;
import uz.melisa.util.ResponseUtil;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<CommonResponse<ResponseMessageDTO>> createChat(
            @RequestBody @Valid CreateChatRequestDTO createChatRequestDTO
    ) {
        log.info("REST request to create chat : {}", createChatRequestDTO);
        return ResponseUtil.buildResponseDTO(chatService.createChat(createChatRequestDTO));
    }

    @GetMapping
    public Page<ChatDTO> getChatPages(
            @PageableDefault(sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        log.info("REST request to get chat pages");
        return chatService.getChatPages(pageable);
    }

    @GetMapping("/{id}/messages")
    public Page<ChatMessagesDTO> getChatMessages(
            @PathVariable("id") Long id,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        log.info("REST request to get chat messages");
        return chatService.getChatMessages(id, pageable);
    }
}
