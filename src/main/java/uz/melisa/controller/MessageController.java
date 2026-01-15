package uz.melisa.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.melisa.dto.common.CommonResponse;
import uz.melisa.dto.message.MessageResponseDTO;
import uz.melisa.dto.message.MessageSendRequestDTO;
import uz.melisa.service.MessageService;
import uz.melisa.util.ResponseUtil;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/message")
public class MessageController {

    private final MessageService messageService;

    @PreAuthorize("hasRole('ROLE_USER')")
    @PostMapping("/send")
    public ResponseEntity<CommonResponse<MessageResponseDTO>> sendMessage(
            @RequestBody @Valid MessageSendRequestDTO messageSendRequestDTO
    ) {
        log.info("REST request to send message : {}", messageSendRequestDTO);
        return ResponseUtil.buildResponseDTO(messageService.sendMessage(messageSendRequestDTO));
    }
}
