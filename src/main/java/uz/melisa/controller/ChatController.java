package uz.melisa.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.melisa.dto.ResponseMessageDTO;
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
}
