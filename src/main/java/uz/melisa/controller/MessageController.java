package uz.melisa.controller;

import io.swagger.v3.oas.annotations.Hidden;
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
public class MessageController {

    private final MessageService messageService;

    @PostMapping("/send")
    public ResponseEntity<CommonResponse<MessageResponseDTO>> sendMessage(
            @RequestBody @Valid MessageSendRequestDTO messageSendRequestDTO
    ) {
        log.info("REST request to send message : {}", messageSendRequestDTO);
        return ResponseUtil.buildResponseDTO(messageService.sendMessage(messageSendRequestDTO));
    }

    @Hidden
    @PostMapping(value = "/send-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> sendMessageStream(
            @RequestBody @Valid MessageSendRequestDTO messageSendRequestDTO
    ) {
        log.info("REST request to stream message, chatId={}", messageSendRequestDTO.getChatId());
        return messageService.sendMessageStream(messageSendRequestDTO);
    }

    @PostMapping("/delete/{id}")
    public ResponseEntity<CommonResponse<ResponseMessageDTO>> deleteMessage(
            @PathVariable("id") long id
    ) {
        log.info("REST request to delete message : {}", id);
        return ResponseUtil.buildResponseDTO(messageService.deleteMessage(id));
    }
}
