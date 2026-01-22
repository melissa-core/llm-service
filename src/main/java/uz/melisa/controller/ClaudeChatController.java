package uz.melisa.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.melisa.dto.claude.ClaudeChatRequest;
import uz.melisa.dto.common.CommonResponse;
import uz.melisa.dto.guest.GuestMessageResponseDTO;
import uz.melisa.service.GuestService;
import uz.melisa.util.ResponseUtil;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/guest")
public class ClaudeChatController {

    private final GuestService guestService;

    @PostMapping(value = "/free-chat", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CommonResponse<GuestMessageResponseDTO>> chat(@Valid @RequestBody ClaudeChatRequest request) {
        log.info("REST request to free chat : {}", request);
        return ResponseUtil.buildResponseDTO(guestService.chatWithClaude(request));
    }
}
